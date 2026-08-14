package com.aem.bulkauthoring.docadapt;

import com.aem.bulkauthoring.docadapt.bind.BindingEngine;
import com.aem.bulkauthoring.docadapt.bind.BoundSlots;
import com.aem.bulkauthoring.docadapt.catalog.SlotCatalog;
import com.aem.bulkauthoring.docadapt.content.ContentDocument;
import com.aem.bulkauthoring.docadapt.extract.SourceDocumentNormalizer;
import com.aem.bulkauthoring.docadapt.fill.TemplateSlotDiscovery;
import com.aem.bulkauthoring.docadapt.fill.TemplateSlotFiller;
import com.aem.bulkauthoring.docadapt.mapping.SlotBinding;
import com.aem.bulkauthoring.docadapt.mapping.SourceMapping;
import com.aem.bulkauthoring.docadapt.mapping.SourceMappingLoader;
import com.aem.bulkauthoring.docadapt.model.NormalizedDocument;
import com.aem.bulkauthoring.docadapt.recipe.SourceRecipe;
import com.aem.bulkauthoring.docadapt.recipe.SourceRecipeDetector;
import com.aem.bulkauthoring.docadapt.review.AdaptReviewModel;
import com.aem.bulkauthoring.docadapt.strategy.AdaptStrategy;
import com.aem.bulkauthoring.docadapt.strategy.ExtractedSlot;
import com.aem.bulkauthoring.docadapt.strategy.RuleEngineStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Orchestrates batch client-DOCX → filled template adapt + review payloads. */
public final class DocAdaptService {

    public static final String AUTO_MAPPING_ID = "__auto__";
    public static final String TEMPLATES_DIR = "input/templates";

    private final SourceDocumentNormalizer normalizer = new SourceDocumentNormalizer();
    private final TemplateSlotFiller filler = new TemplateSlotFiller();
    private final BindingEngine bindingEngine = new BindingEngine();
    private final SourceRecipeDetector detector = new SourceRecipeDetector();
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final Map<String, AdaptJob> jobs = new ConcurrentHashMap<>();

    public AdaptJobResult run(String mappingId, List<File> sourceFiles) throws IOException {
        if (AUTO_MAPPING_ID.equals(mappingId)) {
            throw new IllegalArgumentException("Auto adapt requires a target template");
        }
        SourceMapping mapping = SourceMappingLoader.loadById(mappingId);
        return run(mapping, sourceFiles);
    }

    public AdaptJobResult runAuto(File template, String recipeId, List<File> sourceFiles)
            throws IOException {
        SourceMapping mapping = new SourceMapping();
        mapping.setId(AUTO_MAPPING_ID);
        mapping.setTargetTemplate(template.getPath().replace('\\', '/'));
        mapping.setSourceRecipe(recipeId == null || recipeId.isBlank() ? "auto" : recipeId);
        mapping.setSourceDir("input/raw-source-docs");
        mapping.setOutputDir("output/adapted-articles");
        return run(mapping, sourceFiles);
    }

    public AdaptJobResult run(SourceMapping mapping, List<File> sourceFiles) throws IOException {
        File template = resolveTemplate(mapping.getTargetTemplate());
        if (!template.isFile()) {
            throw new IllegalArgumentException("Target template not found: " + template.getPath());
        }
        List<String> markerPaths = TemplateSlotDiscovery.discoverPaths(template);
        SlotCatalog catalog = SlotCatalog.fromPaths(markerPaths);

        File outDir = new File(mapping.getOutputDir() == null
                ? "output/adapted-articles"
                : mapping.getOutputDir());
        Files.createDirectories(outDir.toPath());

        String jobId = UUID.randomUUID().toString();
        List<AdaptReviewModel> reviews = new ArrayList<>();
        List<File> adaptedFiles = new ArrayList<>();
        List<ArticleState> articles = new ArrayList<>();

        List<File> sources = sourceFiles;
        if (sources == null || sources.isEmpty()) {
            sources = listDocx(new File(mapping.getSourceDir() == null
                    ? "input/raw-source-docs"
                    : mapping.getSourceDir()));
        }

        boolean legacy = mapping.hasLegacySlots();
        AdaptStrategy legacyStrategy = legacy ? resolveStrategy(mapping.getStrategy()) : null;

        for (File source : sources) {
            if (source == null || !source.isFile()) {
                continue;
            }
            NormalizedDocument doc = normalizer.normalize(source);
            ArticleState article = new ArticleState();
            article.doc = doc;

            Map<String, String> values = new LinkedHashMap<>();
            AdaptReviewModel review = new AdaptReviewModel();
            review.setSourceFile(source.getName());
            review.setSourcePlainText(doc.toPlainText());

            if (legacy) {
                List<ExtractedSlot> extracted = legacyStrategy.apply(doc, mapping);
                for (ExtractedSlot slot : extracted) {
                    values.put(slot.getPath(), slot.getValue());
                    review.getSlots().add(new AdaptReviewModel.SlotReview(
                            slot.getPath(), slot.getValue(), slot.getSourceExcerpt()));
                }
            } else {
                SourceRecipe recipe = detector.resolve(mapping.getSourceRecipe(), doc);
                ContentDocument content = recipe.extract(doc);
                article.content = content;
                BoundSlots bound = bindingEngine.bind(content, catalog, mapping.getBindings());
                values.putAll(bound.getPathToValue());
                applyBoundReview(review, bound, content);
            }

            String stem = stem(source.getName());
            File adapted = new File(outDir, stem + ".docx");
            filler.fill(template, values, adapted);
            review.setAdaptedFile(adapted.getName());

            File reviewJson = new File(outDir, stem + ".review.json");
            mapper.writeValue(reviewJson, review);

            reviews.add(review);
            adaptedFiles.add(adapted);
            article.adaptedFile = adapted;
            article.review = review;
            articles.add(article);
        }

        AdaptJob job = new AdaptJob(
                jobId, mapping.getId(), outDir, reviews, adaptedFiles,
                template, catalog, mapping.getSourceRecipe(), articles);
        jobs.put(jobId, job);
        return toResult(job);
    }

    public AdaptJobResult rebind(String jobId, List<SlotBinding> bindings) throws IOException {
        AdaptJob job = jobs.get(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Unknown adapt job: " + jobId);
        }
        if (job.catalog == null || job.template == null) {
            throw new IllegalArgumentException("This job has no template catalog to rebind");
        }
        List<SlotBinding> overrides = bindings == null ? List.of() : bindings;
        for (ArticleState article : job.articles) {
            if (article.content == null) {
                continue;
            }
            BoundSlots bound = bindingEngine.bind(article.content, job.catalog, overrides);
            filler.fill(job.template, bound.getPathToValue(), article.adaptedFile);
            applyBoundReview(article.review, bound, article.content);
            File reviewJson = new File(job.outDir,
                    article.adaptedFile.getName().replace(".docx", ".review.json"));
            mapper.writeValue(reviewJson, article.review);
        }
        return toResult(job);
    }

    public File saveMapping(String id,
                            String templatePath,
                            String recipeId,
                            String bindStrategy,
                            List<SlotBinding> bindings) throws IOException {
        SourceMapping mapping = new SourceMapping();
        mapping.setId(id);
        mapping.setStrategy("rule-engine");
        mapping.setTargetTemplate(resolveTemplate(templatePath).getPath().replace('\\', '/'));
        mapping.setSourceRecipe(recipeId == null || recipeId.isBlank() ? "auto" : recipeId);
        mapping.setBindStrategy(bindStrategy);
        mapping.setBindings(bindings == null ? List.of() : bindings);
        mapping.setSourceDir("input/raw-source-docs");
        mapping.setOutputDir("output/adapted-articles");
        return SourceMappingLoader.save(mapping);
    }

    public List<TemplateInfo> listTemplates() throws IOException {
        File dir = new File(TEMPLATES_DIR);
        List<TemplateInfo> out = new ArrayList<>();
        if (!dir.isDirectory()) {
            return out;
        }
        File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".docx")
                && !name.startsWith("~"));
        if (files == null) {
            return out;
        }
        for (File f : files) {
            TemplateInfo info = new TemplateInfo();
            info.id = stem(f.getName());
            info.path = f.getPath().replace('\\', '/');
            info.markers = TemplateSlotDiscovery.discoverPaths(f);
            out.add(info);
        }
        out.sort((a, b) -> a.id.compareToIgnoreCase(b.id));
        return out;
    }

    public List<String> listRecipes() {
        return detector.recipeIds();
    }

    public AdaptJobResult getReview(String jobId) {
        AdaptJob job = jobs.get(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Unknown adapt job: " + jobId);
        }
        return toResult(job);
    }

    public File resolveAdaptedFile(String jobId, String fileName) {
        AdaptJob job = jobs.get(jobId);
        if (job == null) {
            File fallback = new File("output/adapted-articles", fileName);
            if (fallback.isFile() && isSafeName(fileName)) {
                return fallback;
            }
            throw new IllegalArgumentException("Unknown adapt job: " + jobId);
        }
        if (!isSafeName(fileName)) {
            throw new IllegalArgumentException("Invalid file name");
        }
        File f = new File(job.outDir, fileName);
        if (!f.isFile()) {
            throw new IllegalArgumentException("Adapted file not found: " + fileName);
        }
        return f;
    }

    public List<String> sendToArticles(String jobId,
                                       List<String> adaptedFileNames,
                                       File articlesDir) throws IOException {
        Files.createDirectories(articlesDir.toPath());
        List<String> copied = new ArrayList<>();
        for (String name : adaptedFileNames) {
            File src = resolveAdaptedFile(jobId, name);
            Path dest = articlesDir.toPath().resolve(src.getName());
            Files.copy(src.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            copied.add(src.getName());
        }
        return copied;
    }

    public List<String> listMappings() throws IOException {
        return SourceMappingLoader.listIds();
    }

    public static File resolveTemplate(String pathOrId) {
        if (pathOrId == null || pathOrId.isBlank()) {
            throw new IllegalArgumentException("Template is required");
        }
        String raw = pathOrId.trim().replace('\\', '/');
        File direct = new File(raw);
        if (direct.isFile()) {
            return direct;
        }
        File underTemplates = new File(TEMPLATES_DIR, raw.endsWith(".docx") ? raw : raw + ".docx");
        if (underTemplates.isFile()) {
            return underTemplates;
        }
        String name = raw.contains("/") ? raw.substring(raw.lastIndexOf('/') + 1) : raw;
        File byName = new File(TEMPLATES_DIR, name.endsWith(".docx") ? name : name + ".docx");
        return byName;
    }

    private static void applyBoundReview(AdaptReviewModel review,
                                         BoundSlots bound,
                                         ContentDocument content) {
        review.setRecipeId(content.getRecipeId());
        review.setBindStrategy(bound.getBindStrategy());
        review.setUnits(content.selectableUnits());
        review.setBindings(bound.getBindings());
        review.setSlots(new ArrayList<>());
        for (ExtractedSlot slot : bound.toExtractedSlots()) {
            review.getSlots().add(new AdaptReviewModel.SlotReview(
                    slot.getPath(),
                    slot.getValue(),
                    slot.getSourceExcerpt(),
                    bound.getPathToUnit().get(slot.getPath())));
        }
    }

    private static AdaptStrategy resolveStrategy(String id) {
        String key = id == null ? "rule-engine" : id.trim().toLowerCase(Locale.ROOT);
        if ("rule-engine".equals(key)) {
            return new RuleEngineStrategy();
        }
        throw new IllegalArgumentException("Unknown strategy: " + id);
    }

    private static List<File> listDocx(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return List.of();
        }
        File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".docx"));
        if (files == null) {
            return List.of();
        }
        List<File> list = new ArrayList<>();
        for (File f : files) {
            if (f.isFile() && !f.getName().startsWith("~")) {
                list.add(f);
            }
        }
        return list;
    }

    private static String stem(String name) {
        if (name.toLowerCase(Locale.ROOT).endsWith(".docx")) {
            return name.substring(0, name.length() - 5);
        }
        return name;
    }

    private static boolean isSafeName(String name) {
        return name != null
                && !name.contains("..")
                && !name.contains("/")
                && !name.contains("\\");
    }

    private static AdaptJobResult toResult(AdaptJob job) {
        String templatePath = job.template == null ? null : job.template.getPath().replace('\\', '/');
        return new AdaptJobResult(job.jobId, job.mappingId, job.reviews, templatePath, job.recipeId);
    }

    public static final class AdaptJobResult {
        public final String jobId;
        public final String mappingId;
        public final List<AdaptReviewModel> reviews;
        public final String templatePath;
        public final String recipeId;

        public AdaptJobResult(String jobId, String mappingId, List<AdaptReviewModel> reviews) {
            this(jobId, mappingId, reviews, null, null);
        }

        public AdaptJobResult(String jobId, String mappingId, List<AdaptReviewModel> reviews,
                              String templatePath, String recipeId) {
            this.jobId = jobId;
            this.mappingId = mappingId;
            this.reviews = reviews;
            this.templatePath = templatePath;
            this.recipeId = recipeId;
        }
    }

    public static final class TemplateInfo {
        public String id;
        public String path;
        public List<String> markers = new ArrayList<>();
    }

    private static final class ArticleState {
        NormalizedDocument doc;
        ContentDocument content;
        File adaptedFile;
        AdaptReviewModel review;
    }

    private static final class AdaptJob {
        final String jobId;
        final String mappingId;
        final File outDir;
        final List<AdaptReviewModel> reviews;
        final List<File> adaptedFiles;
        final File template;
        final SlotCatalog catalog;
        final String recipeId;
        final List<ArticleState> articles;

        AdaptJob(String jobId,
                 String mappingId,
                 File outDir,
                 List<AdaptReviewModel> reviews,
                 List<File> adaptedFiles,
                 File template,
                 SlotCatalog catalog,
                 String recipeId,
                 List<ArticleState> articles) {
            this.jobId = jobId;
            this.mappingId = mappingId;
            this.outDir = outDir;
            this.reviews = reviews;
            this.adaptedFiles = adaptedFiles;
            this.template = template;
            this.catalog = catalog;
            this.recipeId = recipeId;
            this.articles = articles;
        }
    }
}

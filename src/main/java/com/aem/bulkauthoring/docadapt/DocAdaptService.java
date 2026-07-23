package com.aem.bulkauthoring.docadapt;

import com.aem.bulkauthoring.docadapt.extract.SourceDocumentNormalizer;
import com.aem.bulkauthoring.docadapt.fill.TemplateSlotDiscovery;
import com.aem.bulkauthoring.docadapt.fill.TemplateSlotFiller;
import com.aem.bulkauthoring.docadapt.mapping.SourceMapping;
import com.aem.bulkauthoring.docadapt.mapping.SourceMappingLoader;
import com.aem.bulkauthoring.docadapt.model.NormalizedDocument;
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

    private final SourceDocumentNormalizer normalizer = new SourceDocumentNormalizer();
    private final TemplateSlotFiller filler = new TemplateSlotFiller();
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final Map<String, AdaptJob> jobs = new ConcurrentHashMap<>();

    public AdaptJobResult run(String mappingId, List<File> sourceFiles) throws IOException {
        SourceMapping mapping = SourceMappingLoader.loadById(mappingId);
        return run(mapping, sourceFiles);
    }

    public AdaptJobResult run(SourceMapping mapping, List<File> sourceFiles) throws IOException {
        File template = new File(mapping.getTargetTemplate());
        if (!template.isFile()) {
            throw new IllegalArgumentException("Target template not found: " + template.getPath());
        }
        TemplateSlotDiscovery.discoverPaths(template);

        AdaptStrategy strategy = resolveStrategy(mapping.getStrategy());
        File outDir = new File(mapping.getOutputDir() == null
                ? "output/adapted-articles"
                : mapping.getOutputDir());
        Files.createDirectories(outDir.toPath());

        String jobId = UUID.randomUUID().toString();
        List<AdaptReviewModel> reviews = new ArrayList<>();
        List<File> adaptedFiles = new ArrayList<>();

        List<File> sources = sourceFiles;
        if (sources == null || sources.isEmpty()) {
            sources = listDocx(new File(mapping.getSourceDir() == null
                    ? "input/raw-source-docs"
                    : mapping.getSourceDir()));
        }

        for (File source : sources) {
            if (source == null || !source.isFile()) {
                continue;
            }
            NormalizedDocument doc = normalizer.normalize(source);
            List<ExtractedSlot> extracted = strategy.apply(doc, mapping);

            Map<String, String> values = new LinkedHashMap<>();
            AdaptReviewModel review = new AdaptReviewModel();
            review.setSourceFile(source.getName());
            review.setSourcePlainText(doc.toPlainText());

            for (ExtractedSlot slot : extracted) {
                values.put(slot.getPath(), slot.getValue());
                review.getSlots().add(new AdaptReviewModel.SlotReview(
                        slot.getPath(), slot.getValue(), slot.getSourceExcerpt()));
            }

            String stem = stem(source.getName());
            File adapted = new File(outDir, stem + ".docx");
            filler.fill(template, values, adapted);
            review.setAdaptedFile(adapted.getName());

            File reviewJson = new File(outDir, stem + ".review.json");
            mapper.writeValue(reviewJson, review);

            reviews.add(review);
            adaptedFiles.add(adapted);
        }

        AdaptJob job = new AdaptJob(jobId, mapping.getId(), outDir, reviews, adaptedFiles);
        jobs.put(jobId, job);
        return new AdaptJobResult(jobId, mapping.getId(), reviews);
    }

    public AdaptJobResult getReview(String jobId) {
        AdaptJob job = jobs.get(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Unknown adapt job: " + jobId);
        }
        return new AdaptJobResult(job.jobId, job.mappingId, job.reviews);
    }

    public File resolveAdaptedFile(String jobId, String fileName) {
        AdaptJob job = jobs.get(jobId);
        if (job == null) {
            // fall back to output dir scan
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

    public static final class AdaptJobResult {
        public final String jobId;
        public final String mappingId;
        public final List<AdaptReviewModel> reviews;

        public AdaptJobResult(String jobId, String mappingId, List<AdaptReviewModel> reviews) {
            this.jobId = jobId;
            this.mappingId = mappingId;
            this.reviews = reviews;
        }
    }

    private static final class AdaptJob {
        final String jobId;
        final String mappingId;
        final File outDir;
        final List<AdaptReviewModel> reviews;
        final List<File> adaptedFiles;

        AdaptJob(String jobId,
                 String mappingId,
                 File outDir,
                 List<AdaptReviewModel> reviews,
                 List<File> adaptedFiles) {
            this.jobId = jobId;
            this.mappingId = mappingId;
            this.outDir = outDir;
            this.reviews = reviews;
            this.adaptedFiles = adaptedFiles;
        }
    }
}

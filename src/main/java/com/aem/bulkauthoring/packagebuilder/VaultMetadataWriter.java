package com.aem.bulkauthoring.packagebuilder;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class VaultMetadataWriter {

    private static final String GROUP = "my_packages";
    private static final String PACKAGE_NAME = "bulk-articles";
    private static final String ARTICLES_ROOT =
            "/content/my-aem-site53/us/en/articles";

    public void write(File packageRoot, List<PageArtifact> pages) throws IOException {
        List<String> roots = pages.stream()
                .map(p -> ARTICLES_ROOT + "/" + p.getPageName())
                .collect(Collectors.toList());

        writeFilter(packageRoot, roots);
        writeProperties(packageRoot);
        writeManifest(packageRoot, roots);
        writeDefinition(packageRoot, roots);
        writeArticlesStub(packageRoot, pages);
    }

    private void writeFilter(File packageRoot, List<String> roots) throws IOException {
        File filter = new File(packageRoot, "META-INF/vault/filter.xml");
        filter.getParentFile().mkdirs();

        try (Writer writer = Files.newBufferedWriter(filter.toPath(), StandardCharsets.UTF_8)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<workspaceFilter version=\"1.0\">\n");
            for (String root : roots) {
                writer.write("    <filter root=\"");
                writer.write(escapeXml(root));
                writer.write("\"/>\n");
            }
            writer.write("</workspaceFilter>\n");
        }
    }

    private void writeProperties(File packageRoot) throws IOException {
        File props = new File(packageRoot, "META-INF/vault/properties.xml");
        String now = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        try (Writer writer = Files.newBufferedWriter(props.toPath(), StandardCharsets.UTF_8)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">\n");
            writer.write("<properties>\n");
            writer.write("<comment>FileVault Package Properties</comment>\n");
            writer.write("<entry key=\"description\">Bulk authored article pages</entry>\n");
            writer.write("<entry key=\"packageType\">content</entry>\n");
            writer.write("<entry key=\"packageFormatVersion\">2</entry>\n");
            writer.write("<entry key=\"group\">");
            writer.write(GROUP);
            writer.write("</entry>\n");
            writer.write("<entry key=\"name\">");
            writer.write(PACKAGE_NAME);
            writer.write("</entry>\n");
            writer.write("<entry key=\"version\"></entry>\n");
            writer.write("<entry key=\"dependencies\"></entry>\n");
            writer.write("<entry key=\"createdBy\">aem-bulk-authoring</entry>\n");
            writer.write("<entry key=\"created\">");
            writer.write(escapeXml(now));
            writer.write("</entry>\n");
            writer.write("<entry key=\"lastModifiedBy\">aem-bulk-authoring</entry>\n");
            writer.write("<entry key=\"lastModified\">");
            writer.write(escapeXml(now));
            writer.write("</entry>\n");
            writer.write("<entry key=\"buildCount\">1</entry>\n");
            writer.write("</properties>\n");
        }
    }

    private void writeManifest(File packageRoot, List<String> roots) throws IOException {
        File manifest = new File(packageRoot, "META-INF/MANIFEST.MF");
        manifest.getParentFile().mkdirs();

        String rootsValue = String.join(",", roots);

        try (Writer writer = Files.newBufferedWriter(manifest.toPath(), StandardCharsets.UTF_8)) {
            writer.write("Manifest-Version: 1.0\r\n");
            writer.write("Content-Package-Id: ");
            writer.write(GROUP);
            writer.write(":");
            writer.write(PACKAGE_NAME);
            writer.write("\r\n");
            writeManifestHeader(writer, "Content-Package-Roots", rootsValue);
            writer.write("Content-Package-Type: content\r\n");
            writer.write("\r\n");
        }
    }

    private void writeManifestHeader(Writer writer, String name, String value) throws IOException {
        String line = name + ": " + value;
        int max = 70;
        if (line.length() <= max) {
            writer.write(line);
            writer.write("\r\n");
            return;
        }

        writer.write(line, 0, max);
        writer.write("\r\n");
        int index = max;
        while (index < line.length()) {
            int end = Math.min(index + 69, line.length());
            writer.write(' ');
            writer.write(line, index, end - index);
            writer.write("\r\n");
            index = end;
        }
    }

    private void writeDefinition(File packageRoot, List<String> roots) throws IOException {
        File definition = new File(packageRoot, "META-INF/vault/definition/.content.xml");
        definition.getParentFile().mkdirs();
        String now = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        try (Writer writer = Files.newBufferedWriter(definition.toPath(), StandardCharsets.UTF_8)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<jcr:root xmlns:vlt=\"http://www.day.com/jcr/vault/1.0\" ");
            writer.write("xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" ");
            writer.write("xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\"\n");
            writer.write("    jcr:primaryType=\"vlt:PackageDefinition\"\n");
            writer.write("    group=\"");
            writer.write(GROUP);
            writer.write("\"\n");
            writer.write("    name=\"");
            writer.write(PACKAGE_NAME);
            writer.write("\"\n");
            writer.write("    version=\"\"\n");
            writer.write("    buildCount=\"1\"\n");
            writer.write("    jcr:description=\"Bulk authored article pages\"\n");
            writer.write("    jcr:created=\"{Date}");
            writer.write(escapeXml(now));
            writer.write("\"\n");
            writer.write("    jcr:createdBy=\"aem-bulk-authoring\"\n");
            writer.write("    jcr:lastModified=\"{Date}");
            writer.write(escapeXml(now));
            writer.write("\"\n");
            writer.write("    jcr:lastModifiedBy=\"aem-bulk-authoring\">\n");
            writer.write("    <filter jcr:primaryType=\"nt:unstructured\">\n");

            for (int i = 0; i < roots.size(); i++) {
                writer.write("        <f");
                writer.write(Integer.toString(i));
                writer.write("\n");
                writer.write("            jcr:primaryType=\"nt:unstructured\"\n");
                writer.write("            mode=\"replace\"\n");
                writer.write("            root=\"");
                writer.write(escapeXml(roots.get(i)));
                writer.write("\"\n");
                writer.write("            rules=\"[]\"/>\n");
            }

            writer.write("    </filter>\n");
            writer.write("</jcr:root>\n");
        }
    }

    private void writeArticlesStub(File packageRoot, List<PageArtifact> pages) throws IOException {
        File articlesContent = new File(
                packageRoot,
                "jcr_root/content/my-aem-site53/us/en/articles/.content.xml");
        articlesContent.getParentFile().mkdirs();

        try (Writer writer = Files.newBufferedWriter(articlesContent.toPath(), StandardCharsets.UTF_8)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<jcr:root xmlns:cq=\"http://www.day.com/jcr/cq/1.0\" ");
            writer.write("xmlns:jcr=\"http://www.jcp.org/jcr/1.0\"\n");
            writer.write("    jcr:primaryType=\"cq:Page\">\n");
            writer.write("    <jcr:content/>\n");
            for (PageArtifact page : pages) {
                writer.write("    <");
                writer.write(page.getPageName());
                writer.write("/>\n");
            }
            writer.write("</jcr:root>\n");
        }
    }

    private String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public static String packageName() {
        return PACKAGE_NAME;
    }
}

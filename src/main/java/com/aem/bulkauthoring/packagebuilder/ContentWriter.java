package com.aem.bulkauthoring.packagebuilder;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class ContentWriter {

    private static final String PAGE_PATH_PREFIX =
            "jcr_root/content/my-aem-site53/us/en/articles/";

    private final DocViewXmlWriter xmlWriter = new DocViewXmlWriter();

    public void write(List<PageArtifact> pages, File packageRoot) throws IOException {
        for (PageArtifact page : pages) {
            File contentXml = new File(
                    packageRoot,
                    PAGE_PATH_PREFIX + page.getPageName() + "/.content.xml");
            contentXml.getParentFile().mkdirs();

            try (Writer writer = Files.newBufferedWriter(
                    contentXml.toPath(), StandardCharsets.UTF_8)) {
                xmlWriter.write(page.getPageRoot(), writer);
            }
        }
    }
}

package com.aem.bulkauthoring.packagebuilder;

import com.aem.bulkauthoring.blueprint.BlueprintPackageConfig;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class ContentWriter {

    private final DocViewXmlWriter xmlWriter = new DocViewXmlWriter();

    public void write(List<PageArtifact> pages,
                      File packageRoot,
                      BlueprintPackageConfig config) throws IOException {

        String pagePathPrefix = config.jcrRootContentParent() + "/";

        for (PageArtifact page : pages) {
            File contentXml = new File(
                    packageRoot,
                    pagePathPrefix + page.getPageName() + "/.content.xml");
            contentXml.getParentFile().mkdirs();

            try (Writer writer = Files.newBufferedWriter(
                    contentXml.toPath(), StandardCharsets.UTF_8)) {
                xmlWriter.write(page.getPageRoot(), writer);
            }
        }
    }
}

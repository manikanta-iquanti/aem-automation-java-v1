package com.aem.bulkauthoring.packagebuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ContentWriter {

    public void write(File updatedBlueprint,
                      File packageRoot) {

        try {

            File destination = new File(
                    packageRoot,
                    "jcr_root/content/page.json");

            destination.getParentFile().mkdirs();

            Files.copy(
                    updatedBlueprint.toPath(),
                    destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

    }

}
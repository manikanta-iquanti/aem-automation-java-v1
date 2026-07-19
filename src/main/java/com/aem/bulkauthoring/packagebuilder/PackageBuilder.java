package com.aem.bulkauthoring.packagebuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class PackageBuilder {

    public void build(File updatedBlueprint) {

        try {

            File root = new File("output/package");

            delete(root);

            Files.createDirectories(root.toPath());

            Files.createDirectories(
                    new File(root, "jcr_root").toPath());

            Files.createDirectories(
                    new File(root, "META-INF/vault").toPath());

            new ContentWriter().write(updatedBlueprint, root);

            System.out.println();

            System.out.println("Package folder created.");

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

    }

    private void delete(File file) {

        if (!file.exists()) {
            return;
        }

        if (file.isDirectory()) {

            File[] files = file.listFiles();

            if (files != null) {

                for (File child : files) {

                    delete(child);

                }
            }
        }

        file.delete();

    }

}
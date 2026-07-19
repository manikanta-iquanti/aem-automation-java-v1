package com.aem.bulkauthoring.packagebuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public class PackageBuilder {

    private static final File BLUEPRINT_PACKAGE =
            new File("input/blueprint-xml/poc-3");

    private static final String SAMPLE_PAGE =
            "vietnam-central-coast-train";

    public File build(List<PageArtifact> pages) {
        if (pages == null || pages.isEmpty()) {
            throw new IllegalArgumentException("At least one page is required");
        }

        try {
            File packageRoot = new File("output/package");
            delete(packageRoot);
            Files.createDirectories(packageRoot.toPath());

            scaffoldFromBlueprint(packageRoot);
            removeSamplePage(packageRoot);

            new ContentWriter().write(pages, packageRoot);
            new VaultMetadataWriter().write(packageRoot, pages);

            File zipFile = new File(
                    "output",
                    VaultMetadataWriter.packageName() + ".zip");
            new PackageZipper().zip(packageRoot, zipFile);

            System.out.println();
            System.out.println("Package folder: " + packageRoot.getAbsolutePath());
            System.out.println("Installable zip: " + zipFile.getAbsolutePath());

            return zipFile;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void scaffoldFromBlueprint(File packageRoot) throws IOException {
        if (!BLUEPRINT_PACKAGE.isDirectory()) {
            throw new IOException("Blueprint package not found: " + BLUEPRINT_PACKAGE);
        }

        copyTree(BLUEPRINT_PACKAGE.toPath(), packageRoot.toPath());
    }

    private void removeSamplePage(File packageRoot) throws IOException {
        Path samplePage = packageRoot.toPath().resolve(
                "jcr_root/content/my-aem-site53/us/en/articles/" + SAMPLE_PAGE);
        if (Files.exists(samplePage)) {
            delete(samplePage.toFile());
        }
    }

    private void copyTree(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                Path relative = source.relativize(dir);
                Path destination = target.resolve(relative);
                Files.createDirectories(destination);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Path relative = source.relativize(file);
                Path destination = target.resolve(relative);
                Files.createDirectories(destination.getParent());
                Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void delete(File file) {
        if (!file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    delete(child);
                }
            }
        }

        file.delete();
    }
}

package com.aem.bulkauthoring.packagebuilder;

import com.aem.bulkauthoring.blueprint.BlueprintPackageConfig;

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

    public File build(List<PageArtifact> pages, BlueprintPackageConfig config) {
        if (pages == null || pages.isEmpty()) {
            throw new IllegalArgumentException("At least one page is required");
        }
        if (config == null) {
            throw new IllegalArgumentException("Package config is required");
        }

        try {
            File packageRoot = new File("output/package");
            delete(packageRoot);
            Files.createDirectories(packageRoot.toPath());

            scaffoldFromBlueprint(packageRoot, config);
            removeSamplePage(packageRoot, config);

            new ContentWriter().write(pages, packageRoot, config);
            new VaultMetadataWriter().write(packageRoot, pages, config);

            File zipFile = new File(
                    "output",
                    config.getPackageName() + ".zip");
            new PackageZipper().zip(packageRoot, zipFile);

            System.out.println();
            System.out.println("Package folder: " + packageRoot.getAbsolutePath());
            System.out.println("Installable zip: " + zipFile.getAbsolutePath());

            return zipFile;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void scaffoldFromBlueprint(File packageRoot,
                                       BlueprintPackageConfig config) throws IOException {
        File blueprintPackage = config.getVaultPackageDir();
        if (!blueprintPackage.isDirectory()) {
            throw new IOException("Blueprint package not found: " + blueprintPackage);
        }

        copyTree(blueprintPackage.toPath(), packageRoot.toPath());
    }

    private void removeSamplePage(File packageRoot,
                                  BlueprintPackageConfig config) throws IOException {
        Path samplePage = packageRoot.toPath().resolve(config.samplePageJcrRootPath());
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

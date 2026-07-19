package com.aem.bulkauthoring.packagebuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PackageZipper {

    public void zip(File packageDir, File zipFile) throws IOException {
        Path root = packageDir.toPath().toAbsolutePath().normalize();
        zipFile.getParentFile().mkdirs();

        if (zipFile.exists() && !zipFile.delete()) {
            throw new IOException("Could not replace existing zip: " + zipFile);
        }

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
             var paths = Files.walk(root)) {

            paths.filter(path -> !path.equals(root))
                    .forEach(path -> {
                        try {
                            Path relative = root.relativize(path);
                            String entryName = relative.toString().replace('\\', '/');
                            if (Files.isDirectory(path)) {
                                if (!entryName.endsWith("/")) {
                                    entryName = entryName + "/";
                                }
                                zos.putNextEntry(new ZipEntry(entryName));
                                zos.closeEntry();
                            } else {
                                zos.putNextEntry(new ZipEntry(entryName));
                                try (FileInputStream in = new FileInputStream(path.toFile())) {
                                    byte[] buffer = new byte[8192];
                                    int read;
                                    while ((read = in.read(buffer)) != -1) {
                                        zos.write(buffer, 0, read);
                                    }
                                }
                                zos.closeEntry();
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}

package com.aem.bulkauthoring.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Derives {@code samplePageName} and {@code contentParentPath} from a FileVault package folder.
 */
public class VaultPackageInspector {

    private static final Pattern FILTER_ROOT =
            Pattern.compile("root\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern CQ_PAGE =
            Pattern.compile("jcr:primaryType\\s*=\\s*\"cq:Page\"");

    public DerivedVaultConfig inspect(File vaultPackageDir) {
        if (vaultPackageDir == null || !vaultPackageDir.isDirectory()) {
            throw new IllegalArgumentException(
                    "Vault package dir missing: " + vaultPackageDir);
        }

        File filterXml = new File(vaultPackageDir, "META-INF/vault/filter.xml");
        List<String> filterRoots = readFilterRoots(filterXml);
        if (filterRoots.size() == 1) {
            return fromJcrPath(vaultPackageDir, filterRoots.get(0));
        }

        File deepestPage = findBestCqPage(vaultPackageDir);
        if (deepestPage == null) {
            throw new IllegalStateException(
                    "Could not derive sample page from " + vaultPackageDir.getPath()
                            + " (no single filter root and no cq:Page .content.xml)");
        }

        File pageDir = deepestPage.getParentFile();
        String jcrPath = toJcrPath(vaultPackageDir, pageDir);
        return fromJcrPath(vaultPackageDir, jcrPath);
    }

    public DerivedVaultConfig fromJcrPath(File vaultPackageDir, String jcrPath) {
        String normalized = normalizeJcrPath(jcrPath);
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash <= 0) {
            throw new IllegalArgumentException("Invalid JCR path: " + jcrPath);
        }
        String samplePageName = normalized.substring(lastSlash + 1);
        String contentParentPath = normalized.substring(0, lastSlash);
        return new DerivedVaultConfig(
                vaultPackageDir,
                contentParentPath,
                samplePageName
        );
    }

    private static List<String> readFilterRoots(File filterXml) {
        List<String> roots = new ArrayList<>();
        if (filterXml == null || !filterXml.isFile()) {
            return roots;
        }
        try {
            String content = Files.readString(filterXml.toPath());
            Matcher matcher = FILTER_ROOT.matcher(content);
            while (matcher.find()) {
                roots.add(normalizeJcrPath(matcher.group(1)));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + filterXml, e);
        }
        return roots;
    }

    private static File findBestCqPage(File vaultPackageDir) {
        File contentRoot = new File(vaultPackageDir, "jcr_root/content");
        if (!contentRoot.isDirectory()) {
            return null;
        }

        List<File> candidates = new ArrayList<>();
        collectCqPages(contentRoot, candidates);
        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort(Comparator
                .comparingInt((File f) -> pathDepth(f))
                .thenComparingLong(File::length)
                .reversed());
        return candidates.get(0);
    }

    private static void collectCqPages(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectCqPages(child, out);
            } else if (".content.xml".equals(child.getName()) && isCqPage(child)) {
                out.add(child);
            }
        }
    }

    private static boolean isCqPage(File contentXml) {
        try (BufferedReader reader = Files.newBufferedReader(
                contentXml.toPath(), StandardCharsets.UTF_8)) {
            StringBuilder head = new StringBuilder();
            String line;
            int lines = 0;
            while ((line = reader.readLine()) != null && lines < 30) {
                head.append(line).append('\n');
                lines++;
            }
            return CQ_PAGE.matcher(head).find();
        } catch (IOException e) {
            return false;
        }
    }

    private static int pathDepth(File contentXml) {
        String path = contentXml.getPath().replace('\\', '/');
        int depth = 0;
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '/') {
                depth++;
            }
        }
        return depth;
    }

    private static String toJcrPath(File vaultPackageDir, File pageDir) {
        File jcrRoot = new File(vaultPackageDir, "jcr_root");
        String absolutePage = pageDir.getAbsolutePath();
        String absoluteRoot = jcrRoot.getAbsolutePath();
        if (!absolutePage.startsWith(absoluteRoot)) {
            throw new IllegalStateException(
                    "Page dir not under jcr_root: " + pageDir);
        }
        String relative = absolutePage.substring(absoluteRoot.length())
                .replace('\\', '/');
        if (!relative.startsWith("/")) {
            relative = "/" + relative;
        }
        return normalizeJcrPath(relative);
    }

    private static String normalizeJcrPath(String path) {
        String normalized = path.trim().replace('\\', '/');
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public static final class DerivedVaultConfig {
        private final File vaultPackageDir;
        private final String contentParentPath;
        private final String samplePageName;

        public DerivedVaultConfig(File vaultPackageDir,
                                  String contentParentPath,
                                  String samplePageName) {
            this.vaultPackageDir = vaultPackageDir;
            this.contentParentPath = contentParentPath;
            this.samplePageName = samplePageName;
        }

        public File getVaultPackageDir() {
            return vaultPackageDir;
        }

        public String getContentParentPath() {
            return contentParentPath;
        }

        public String getSamplePageName() {
            return samplePageName;
        }
    }
}

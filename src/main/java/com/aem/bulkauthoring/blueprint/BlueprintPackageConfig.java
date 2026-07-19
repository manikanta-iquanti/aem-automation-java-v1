package com.aem.bulkauthoring.blueprint;

import java.io.File;

/**
 * Where generated pages are installed in the repository and which FileVault
 * export to use as the package scaffold.
 */
public class BlueprintPackageConfig {

    private final File vaultPackageDir;
    private final String contentParentPath;
    private final String samplePageName;
    private final String packageName;

    public BlueprintPackageConfig(File vaultPackageDir,
                                  String contentParentPath,
                                  String samplePageName,
                                  String packageName) {
        this.vaultPackageDir = vaultPackageDir;
        this.contentParentPath = normalizeContentParent(contentParentPath);
        this.samplePageName = samplePageName;
        this.packageName = packageName;
    }

    private static String normalizeContentParent(String path) {
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /** Directory of the reference FileVault package (e.g. {@code input/blueprint-xml/normal-page1}). */
    public File getVaultPackageDir() {
        return vaultPackageDir;
    }

    /** JCR parent of generated pages, e.g. {@code /content/my-aem-site53/us/en/articles}. */
    public String getContentParentPath() {
        return contentParentPath;
    }

    /** Sample page folder name to remove from the scaffold after copy. */
    public String getSamplePageName() {
        return samplePageName;
    }

    /** Content package / zip name (without {@code .zip}). */
    public String getPackageName() {
        return packageName;
    }

    /** Path under {@code jcr_root} for the content parent (no leading slash). */
    public String jcrRootContentParent() {
        return "jcr_root" + contentParentPath;
    }

    /** Relative path from {@code jcr_root} to the sample page folder. */
    public String samplePageJcrRootPath() {
        return jcrRootContentParent() + "/" + samplePageName;
    }
}

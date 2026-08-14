package com.aem.bulkauthoring.docadapt.content;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DocTexts {

    private static final Pattern HEADING_STYLE =
            Pattern.compile("(?i)(?:heading|标题)\\s*(\\d)");
    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9]+");

    private DocTexts() {
    }

    public static boolean containsIgnoreCase(String haystack, String needle) {
        if (haystack == null || needle == null || needle.isBlank()) {
            return false;
        }
        return haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    public static String escapeXml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public static int headingLevelFromStyle(String style) {
        if (style == null || style.isBlank()) {
            return 0;
        }
        Matcher m = HEADING_STYLE.matcher(style.trim());
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    public static boolean looksLikeHeadingLine(String text) {
        if (text == null || text.length() > 80) {
            return false;
        }
        String trimmed = text.trim();
        return trimmed.equals(trimmed.toUpperCase(Locale.ROOT)) && trimmed.length() > 2;
    }

    public static String slug(String text) {
        if (text == null) {
            return "";
        }
        String s = text.toLowerCase(Locale.ROOT).trim();
        s = NON_SLUG.matcher(s).replaceAll("-");
        s = s.replaceAll("^-+", "").replaceAll("-+$", "");
        if (s.length() > 60) {
            s = s.substring(0, 60).replaceAll("-+$", "");
        }
        return s;
    }

    public static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim().replace('\n', ' ');
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max) + "…";
    }

    public static String lastSegment(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    public static String parentPath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        int slash = path.lastIndexOf('/');
        if (slash <= 0) {
            return "/";
        }
        return path.substring(0, slash);
    }
}

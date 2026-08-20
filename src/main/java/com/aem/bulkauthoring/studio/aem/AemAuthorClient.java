package com.aem.bulkauthoring.studio.aem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * Fetches page {@code .infinity.json} and a FileVault package zip from local AEM Author
 * via Basic Auth and Package Manager HTTP APIs.
 */
public final class AemAuthorClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PACKAGE_GROUP = "studio";
    /** Must match the group written by VaultMetadataWriter for generated packages. */
    private static final String GENERATED_PACKAGE_GROUP = "my_packages";

    private final String baseUrl;
    private final String basicAuthHeader;
    private final HttpClient http;

    public AemAuthorClient(String baseUrl, String username, String password) {
        this(baseUrl, username, password, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build());
    }

    AemAuthorClient(String baseUrl, String username, String password, HttpClient http) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("AEM base URL is required (Settings → AEM base URL)");
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        this.baseUrl = trimmed;
        String user = username == null ? "" : username;
        String pass = password == null ? "" : password;
        this.basicAuthHeader = "Basic " + Base64.getEncoder()
                .encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
        this.http = http;
    }

    /**
     * Normalizes a page URL or content path to a JCR path (no selectors/extensions).
     * Accepts full Author URLs or paths like {@code /content/.../page.html}.
     */
    public static String normalizeContentPath(String pageUrlOrPath) {
        if (pageUrlOrPath == null || pageUrlOrPath.isBlank()) {
            throw new IllegalArgumentException("Page URL or path is required");
        }
        String raw = pageUrlOrPath.trim();
        String path;
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            URI uri = URI.create(raw);
            path = uri.getPath();
            if (path == null || path.isBlank()) {
                throw new IllegalArgumentException("URL has no path: " + pageUrlOrPath);
            }
        } else {
            path = raw;
            int q = path.indexOf('?');
            if (q >= 0) {
                path = path.substring(0, q);
            }
            int hash = path.indexOf('#');
            if (hash >= 0) {
                path = path.substring(0, hash);
            }
        }
        path = path.replace('\\', '/');
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        // Strip known suffixes / selectors commonly pasted from browsers (longest first)
        path = stripSuffixIgnoreCase(path, ".tidy.infinity.json");
        path = stripSuffixIgnoreCase(path, ".infinity.json");
        path = stripSuffixIgnoreCase(path, ".json");
        path = stripSuffixIgnoreCase(path, ".html");
        while (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        if ("/".equals(path) || path.isBlank()) {
            throw new IllegalArgumentException("Invalid content path from: " + pageUrlOrPath);
        }
        return path;
    }

    private static String stripSuffixIgnoreCase(String path, String suffix) {
        if (path.length() >= suffix.length()
                && path.regionMatches(true, path.length() - suffix.length(), suffix, 0, suffix.length())) {
            return path.substring(0, path.length() - suffix.length());
        }
        return path;
    }

    public byte[] fetchInfinityJson(String pageUrlOrPath) throws IOException, InterruptedException {
        String path = normalizeContentPath(pageUrlOrPath);
        String url = baseUrl + path + ".infinity.json";
        HttpResponse<byte[]> response = send(HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .GET()
                .header("Authorization", basicAuthHeader)
                .header("Accept", "application/json")
                .build());
        ensureOk(response, "Fetch .infinity.json for " + path);
        byte[] body = response.body();
        try {
            MAPPER.readTree(body);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "AEM response is not valid JSON for " + path + ".infinity.json");
        }
        return body;
    }

    /**
     * Creates a temporary Package Manager package filtered to the page, builds it, downloads the zip,
     * then best-effort deletes the temp package.
     */
    public byte[] exportPagePackage(String pageUrlOrPath) throws IOException, InterruptedException {
        String contentPath = normalizeContentPath(pageUrlOrPath);
        String packageName = "studio-fetch-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String packagePath = "/etc/packages/" + PACKAGE_GROUP + "/" + packageName + ".zip";
        try {
            createPackage(packageName);
            updateFilter(packagePath, packageName, contentPath);
            buildPackage(packagePath);
            return downloadPackage(packagePath);
        } finally {
            try {
                deletePackage(packagePath);
            } catch (Exception ignored) {
                // best-effort cleanup
            }
        }
    }

    public InputStream fetchInfinityJsonStream(String pageUrlOrPath)
            throws IOException, InterruptedException {
        return new ByteArrayInputStream(fetchInfinityJson(pageUrlOrPath));
    }

    public InputStream exportPagePackageStream(String pageUrlOrPath)
            throws IOException, InterruptedException {
        return new ByteArrayInputStream(exportPagePackage(pageUrlOrPath));
    }

    /**
     * Uploads a FileVault zip to Package Manager and installs the uploaded package.
     *
     * @return the Package Manager success message, when provided
     */
    public String uploadAndInstall(File zip) throws IOException, InterruptedException {
        if (zip == null || !zip.isFile()) {
            throw new IllegalArgumentException("Package zip not found");
        }
        if (!zip.getName().toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("Package must be a .zip file: " + zip.getName());
        }
        if (!zip.getName().matches("[A-Za-z0-9][A-Za-z0-9._-]*\\.zip")) {
            throw new IllegalArgumentException(
                    "Package filename may contain only letters, numbers, dots, underscores, and hyphens");
        }

        String boundary = "----StudioBoundary" + UUID.randomUUID().toString().replace("-", "");
        String safeFilename = zip.getName()
                .replace("\r", "_")
                .replace("\n", "_")
                .replace("\"", "_");
        byte[] prefix = (
                "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"force\"\r\n\r\n"
                        + "true\r\n"
                        + "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"package\"; filename=\""
                        + safeFilename + "\"\r\n"
                        + "Content-Type: application/zip\r\n\r\n")
                .getBytes(StandardCharsets.ISO_8859_1);
        byte[] suffix = ("\r\n--" + boundary + "--\r\n")
                .getBytes(StandardCharsets.ISO_8859_1);
        ByteArrayOutputStream multipart = new ByteArrayOutputStream();
        multipart.write(prefix);
        try (InputStream zipIn = java.nio.file.Files.newInputStream(zip.toPath())) {
            zipIn.transferTo(multipart);
        }
        multipart.write(suffix);

        HttpResponse<byte[]> upload = send(HttpRequest.newBuilder(
                        URI.create(baseUrl + "/crx/packmgr/service/.json/?cmd=upload"))
                .timeout(Duration.ofMinutes(5))
                .header("Authorization", basicAuthHeader)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipart.toByteArray()))
                .build());
        ensureOk(upload, "Upload package " + zip.getName());
        ensurePackmgrSuccess(upload.body(), "Upload package " + zip.getName());

        String packagePath = "/etc/packages/" + GENERATED_PACKAGE_GROUP + "/" + zip.getName();

        HttpResponse<byte[]> install = send(HttpRequest.newBuilder(
                        URI.create(baseUrl + "/crx/packmgr/service/.json"
                                + packagePath + "?cmd=install"))
                .timeout(Duration.ofMinutes(5))
                .header("Authorization", basicAuthHeader)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build());
        ensureOk(install, "Install package " + packagePath);
        ensurePackmgrSuccess(install.body(), "Install package " + packagePath);

        JsonNode installResult = readPackmgrJson(install.body(), "Install package " + packagePath);
        return installResult.path("msg").asText("Package installed");
    }

    private static JsonNode readPackmgrJson(byte[] body, String action) {
        JsonNode result;
        try {
            result = MAPPER.readTree(body);
        } catch (IOException e) {
            throw new IllegalArgumentException(action + " returned an invalid response");
        }
        if (result == null || !result.isObject()) {
            throw new IllegalArgumentException(action + " returned an invalid response");
        }
        return result;
    }

    private void createPackage(String packageName) throws IOException, InterruptedException {
        String url = baseUrl + "/crx/packmgr/service/.json/etc/packages/"
                + PACKAGE_GROUP + "/" + packageName + ".zip?cmd=create";
        String form = "packageName=" + enc(packageName) + "&groupName=" + enc(PACKAGE_GROUP);
        HttpResponse<byte[]> response = send(HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", basicAuthHeader)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build());
        ensureOk(response, "Create package " + packageName);
        ensurePackmgrSuccess(response.body(), "Create package " + packageName);
    }

    private void updateFilter(String packagePath, String packageName, String contentPath)
            throws IOException, InterruptedException {
        ArrayNode filters = MAPPER.createArrayNode();
        ObjectNode filter = filters.addObject();
        filter.put("root", contentPath);
        filter.set("rules", MAPPER.createArrayNode());
        String filterJson = MAPPER.writeValueAsString(filters);

        String boundary = "----StudioBoundary" + UUID.randomUUID().toString().replace("-", "");
        String body = multipartBody(boundary,
                "path", packagePath,
                "packageName", packageName,
                "groupName", PACKAGE_GROUP,
                "filter", filterJson,
                "_charset_", "UTF-8");

        HttpResponse<byte[]> response = send(HttpRequest.newBuilder(
                        URI.create(baseUrl + "/crx/packmgr/update.jsp"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", basicAuthHeader)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build());
        ensureOk(response, "Update package filter for " + contentPath);
    }

    private void buildPackage(String packagePath) throws IOException, InterruptedException {
        String url = baseUrl + "/crx/packmgr/service/.json" + packagePath + "?cmd=build";
        HttpResponse<byte[]> response = send(HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .header("Authorization", basicAuthHeader)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build());
        ensureOk(response, "Build package " + packagePath);
        ensurePackmgrSuccess(response.body(), "Build package " + packagePath);
    }

    private byte[] downloadPackage(String packagePath) throws IOException, InterruptedException {
        String url = baseUrl + packagePath;
        HttpResponse<byte[]> response = send(HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .header("Authorization", basicAuthHeader)
                .GET()
                .build());
        ensureOk(response, "Download package " + packagePath);
        byte[] body = response.body();
        if (body == null || body.length < 4
                || body[0] != 'P' || body[1] != 'K') {
            throw new IOException("Downloaded package is not a zip: " + packagePath);
        }
        return body;
    }

    private void deletePackage(String packagePath) throws IOException, InterruptedException {
        String url = baseUrl + "/crx/packmgr/service/.json" + packagePath + "?cmd=delete";
        send(HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", basicAuthHeader)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build());
    }

    private HttpResponse<byte[]> send(HttpRequest request) throws IOException, InterruptedException {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new IOException(
                    "Cannot reach AEM at " + baseUrl + " — is Author running? (" + e.getMessage() + ")",
                    e);
        }
    }

    private static void ensureOk(HttpResponse<byte[]> response, String action) {
        int code = response.statusCode();
        if (code == 401 || code == 403) {
            throw new IllegalArgumentException(
                    code + " — " + action + " failed. Check Settings credentials.");
        }
        if (code == 404) {
            throw new IllegalArgumentException(code + " — " + action + " failed (not found).");
        }
        if (code < 200 || code >= 300) {
            String snippet = bodySnippet(response.body());
            throw new IllegalArgumentException(
                    code + " — " + action + " failed" + (snippet.isEmpty() ? "" : ": " + snippet));
        }
    }

    private static void ensurePackmgrSuccess(byte[] body, String action) {
        if (body == null || body.length == 0) {
            return;
        }
        try {
            JsonNode node = MAPPER.readTree(body);
            if (node.has("success") && !node.get("success").asBoolean()) {
                String msg = node.has("msg") ? node.get("msg").asText() : bodySnippet(body);
                throw new IllegalArgumentException(action + " failed: " + msg);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException ignored) {
            // non-JSON success bodies are fine
        }
    }

    private static String bodySnippet(byte[] body) {
        if (body == null || body.length == 0) {
            return "";
        }
        String text = new String(body, StandardCharsets.UTF_8).replaceAll("\\s+", " ").trim();
        if (text.length() > 200) {
            return text.substring(0, 200) + "…";
        }
        return text;
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String multipartBody(String boundary, String... nameValues) {
        if (nameValues.length % 2 != 0) {
            throw new IllegalArgumentException("name/value pairs required");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nameValues.length; i += 2) {
            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Disposition: form-data; name=\"")
                    .append(nameValues[i]).append("\"\r\n\r\n");
            sb.append(nameValues[i + 1]).append("\r\n");
        }
        sb.append("--").append(boundary).append("--\r\n");
        return sb.toString();
    }
}

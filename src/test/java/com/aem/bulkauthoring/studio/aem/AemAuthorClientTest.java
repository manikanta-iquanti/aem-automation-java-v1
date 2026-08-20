package com.aem.bulkauthoring.studio.aem;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AemAuthorClientTest {

    @TempDir
    Path tempDir;

    @Test
    void normalizesFullHtmlUrl() {
        assertEquals(
                "/content/my-aem-site53/us/en/articles/normal-page1",
                AemAuthorClient.normalizeContentPath(
                        "http://localhost:4502/content/my-aem-site53/us/en/articles/normal-page1.html"));
    }

    @Test
    void normalizesPathWithoutHost() {
        assertEquals(
                "/content/site/us/en/page",
                AemAuthorClient.normalizeContentPath("/content/site/us/en/page.html"));
    }

    @Test
    void stripsInfinityJsonSelector() {
        assertEquals(
                "/content/site/page",
                AemAuthorClient.normalizeContentPath(
                        "http://localhost:4502/content/site/page.infinity.json"));
    }

    @Test
    void stripsTidyInfinityJson() {
        assertEquals(
                "/content/site/page",
                AemAuthorClient.normalizeContentPath("/content/site/page.tidy.infinity.json"));
    }

    @Test
    void stripsQueryAndFragment() {
        assertEquals(
                "/content/site/page",
                AemAuthorClient.normalizeContentPath(
                        "http://localhost:4502/content/site/page.html?wcmmode=disabled#foo"));
    }

    @Test
    void addsLeadingSlash() {
        assertEquals(
                "/content/site/page",
                AemAuthorClient.normalizeContentPath("content/site/page"));
    }

    @Test
    void rejectsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> AemAuthorClient.normalizeContentPath("  "));
    }

    @Test
    void uploadsThenInstallsGeneratedPackageWithoutTrustingResponsePath() throws Exception {
        byte[] zipBytes = {'P', 'K', 3, 4, 0, 1, 2};
        File zip = tempDir.resolve("bulk-articles.zip").toFile();
        Files.write(zip.toPath(), zipBytes);
        List<String> requests = new ArrayList<>();
        List<byte[]> bodies = new ArrayList<>();

        HttpServer server = startServer(exchange -> {
            requests.add(exchange.getRequestURI().toString());
            bodies.add(exchange.getRequestBody().readAllBytes());
            if (requests.size() == 1) {
                respond(exchange, 200,
                        "{\"success\":true,\"path\":\"http://untrusted.example/package.zip\"}");
            } else {
                respond(exchange, 200, "{\"success\":true,\"msg\":\"Package installed\"}");
            }
        });
        try {
            AemAuthorClient client = new AemAuthorClient(baseUrl(server), "admin", "admin");

            String message = client.uploadAndInstall(zip);

            assertEquals("Package installed", message);
            assertEquals(List.of(
                    "/crx/packmgr/service/.json/?cmd=upload",
                    "/crx/packmgr/service/.json/etc/packages/my_packages/bulk-articles.zip?cmd=install"),
                    requests);
            String uploadBody = new String(bodies.get(0), StandardCharsets.ISO_8859_1);
            assertTrue(uploadBody.contains("name=\"force\"\r\n\r\ntrue"));
            assertTrue(uploadBody.contains("name=\"package\"; filename=\"bulk-articles.zip\""));
            assertTrue(indexOf(bodies.get(0), zipBytes) >= 0, "multipart body contains zip bytes");
            assertArrayEquals(new byte[0], bodies.get(1));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void reportsPackageManagerInstallFailure() throws Exception {
        File zip = tempDir.resolve("bulk.zip").toFile();
        Files.write(zip.toPath(), new byte[]{'P', 'K', 3, 4});
        HttpServer server = startServer(exchange -> {
            if (exchange.getRequestURI().getQuery().contains("upload")) {
                respond(exchange, 200,
                        "{\"success\":true,\"path\":\"/etc/packages/my_packages/bulk.zip\"}");
            } else {
                respond(exchange, 200,
                        "{\"success\":false,\"msg\":\"Package installation failed\"}");
            }
        });
        try {
            AemAuthorClient client = new AemAuthorClient(baseUrl(server), "admin", "admin");

            IllegalArgumentException error = assertThrows(
                    IllegalArgumentException.class,
                    () -> client.uploadAndInstall(zip));

            assertTrue(error.getMessage().contains("Package installation failed"));
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer startServer(ExchangeHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> handler.handle(exchange));
        server.start();
        return server;
    }

    private static String baseUrl(HttpServer server) {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static int indexOf(byte[] body, byte[] expected) {
        outer:
        for (int i = 0; i <= body.length - expected.length; i++) {
            for (int j = 0; j < expected.length; j++) {
                if (body[i + j] != expected[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}

package dev.ripiters.servercore.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ripiters.servercore.ServerCore;
import dev.ripiters.servercore.config.ServerCoreConfig;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class VersionChecker {
    private static volatile boolean outdated = false;
    private static volatile String latestVersion = "";
    private static volatile String currentVersion = "";

    private static final String GITHUB_VERSION_URL = "https://raw.githubusercontent.com/ripiters/modpack-info/refs/heads/main/version.json";
    private static final String SITE_REPORT_URL = "http://ripiters.ddns.net/api/modpack/client-report";
    private static final String DEFAULT_UPDATE_URL = "http://ripiters.ddns.net/import";

    private static final ExecutorService ASYNC_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ServerCore-VersionChecker");
        t.setDaemon(true);
        return t;
    });

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).executor(ASYNC_EXECUTOR).build();

    private VersionChecker() {}

    public static void checkVersionAsync() {
        currentVersion = safeConfiguredVersion();
        reportVersionAsync(currentVersion);

        CompletableFuture.runAsync(() -> {
            try {
                String url = safeConfiguredCheckUrl();
                if (url.isEmpty()) {
                    url = GITHUB_VERSION_URL;
                }

                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
                        .header("User-Agent", "ServerCore-ModpackChecker")
                        .timeout(Duration.ofSeconds(8))
                        .GET()
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) return;

                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                if (!json.has("latest_version")) return;

                latestVersion = json.get("latest_version").getAsString().trim();
                outdated = compareVersions(latestVersion, currentVersion) > 0;

                if (outdated) {
                    ServerCore.LOGGER.info("Modpack update found! Remote: {}, Installed: {}", latestVersion, currentVersion);
                }
            } catch (Exception e) {
                ServerCore.LOGGER.warn("Failed to check modpack version: {}", e.getMessage());
            }
        }, ASYNC_EXECUTOR);
    }

    private static void reportVersionAsync(String version) {
        if (version == null || version.isBlank()) return;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SITE_REPORT_URL))
                .header("Content-Type", "application/json")
                .header("User-Agent", "ServerCore-VersionReporter")
                .timeout(Duration.ofSeconds(6))
                .POST(HttpRequest.BodyPublishers.ofString("{\"version\":\"" + version + "\"}"))
                .build();

        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .exceptionally(throwable -> {
                    ServerCore.LOGGER.debug("Modpack version report failed: {}", throwable.getMessage());
                    return null;
                });
    }

    private static String safeConfiguredVersion() {
        try {
            String val = ServerCoreConfig.COMMON.modpackVersion.get();
            return val != null ? val.trim() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String safeConfiguredCheckUrl() {
        try {
            String val = ServerCoreConfig.COMMON.versionCheckUrl.get();
            return val != null ? val.trim() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private static int compareVersions(String a, String b) {
        String[] left = a == null ? new String[0] : a.replaceFirst("^[vV]", "").split("\\.");
        String[] right = b == null ? new String[0] : b.replaceFirst("^[vV]", "").split("\\.");
        int count = Math.max(left.length, right.length);
        for (int i = 0; i < count; i++) {
            int l = i < left.length ? parsePart(left[i]) : 0;
            int r = i < right.length ? parsePart(right[i]) : 0;
            if (l != r) return Integer.compare(l, r);
        }
        return 0;
    }

    private static int parsePart(String value) {
        String digits = value == null ? "" : value.replaceAll("[^0-9].*$", "");
        if (digits.isEmpty()) return 0;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public static boolean isOutdated() {
        return outdated;
    }

    // unused
    public static String getCurrentVersion() {
        return currentVersion;
    }
    public static String getLatestVersion() {
        return latestVersion;
    }

    public static String getUpdateUrl() {
        String query = "?v=" + encode(currentVersion) + "&latest=" + encode(latestVersion);
        String baseUrl = DEFAULT_UPDATE_URL;

        if (baseUrl.contains("#")) {
            String[] parts = baseUrl.split("#", 2);
            return parts[0] + query + "#" + parts[1];
        }

        return baseUrl + query;
    }

    private static String encode(String s) {
        return URLEncoder.encode(s != null ? s : "", StandardCharsets.UTF_8);
    }
}
package Clothes_system.cloud;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Small HTTP client used by the desktop sync layer.
 *
 * It deliberately talks to the Cutdown protected API instead of embedding a
 * Supabase service-role credential in the desktop application.
 */
public final class CutdownCloudClient {
    private final HttpClient http;

    public CutdownCloudClient() {
        http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(CutdownCloudConfig.timeoutMillis()))
                .build();
    }

    public boolean isConfigured() {
        return CutdownCloudConfig.configured();
    }

    public String get(String path) throws IOException, InterruptedException {
        HttpRequest request = request(path)
                .GET()
                .build();

        HttpResponse<String> response = http.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(
                    "Cutdown API returned HTTP " + response.statusCode()
                            + ": " + response.body()
            );
        }

        return response.body();
    }

    public String postJson(String path, String json)
            throws IOException, InterruptedException {

        HttpRequest request = request(path)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json == null ? "{}" : json))
                .build();

        HttpResponse<String> response = http.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(
                    "Cutdown API returned HTTP " + response.statusCode()
                            + ": " + response.body()
            );
        }

        return response.body();
    }

    private HttpRequest.Builder request(String path) {
        String base = CutdownCloudConfig.baseUrl();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);

        String normalized = path == null ? "" : path.trim();
        if (!normalized.startsWith("/")) normalized = "/" + normalized;

        return HttpRequest.newBuilder()
                .uri(URI.create(base + normalized))
                .timeout(Duration.ofMillis(CutdownCloudConfig.timeoutMillis()))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + CutdownCloudConfig.syncToken());
    }
}

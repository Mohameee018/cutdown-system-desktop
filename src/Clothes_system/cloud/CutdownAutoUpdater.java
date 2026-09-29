package Clothes_system.cloud;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class CutdownAutoUpdater {
    private CutdownAutoUpdater() {}

    public static void checkAsync(Window owner) {
        Thread t = new Thread(() -> {
            try {
                String response = fetchUpdateMetadata();
                Object parsed = MiniJson.parse(response);
                if (!(parsed instanceof Map<?,?> map)) return;
                String latest = value(map, "version");
                String url = value(map, "download_url");
                String expectedSha256 = value(map, "sha256").trim().toLowerCase();
                boolean mandatory = Boolean.parseBoolean(value(map, "mandatory"));
                String current = currentVersion();
                if (latest.isBlank() || url.isBlank() || expectedSha256.isBlank() || compareVersions(latest, current) <= 0) return;

                SwingUtilities.invokeLater(() -> {
                    String message = "A new Cutdown version (" + latest + ") is available.";
                    int choice = JOptionPane.showConfirmDialog(owner, message + "\n\nUpdate now?", "Cutdown Update",
                            JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    if (mandatory || choice == JOptionPane.YES_OPTION) {
                        downloadAndLaunch(url, expectedSha256, owner);
                    }
                });
            } catch (Exception ignored) {
                // Update checks are non-blocking; the installed version keeps working.
            }
        }, "cutdown-auto-update-check");
        t.setDaemon(true);
        t.start();
    }

    private static String fetchUpdateMetadata() throws Exception {
        try {
            if (CutdownCloudConfig.configured()) {
                return new CutdownCloudClient().get("/api/desktop/update");
            }
        } catch (Exception ignored) {
            // Public GitHub release metadata is the fallback.
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest request = HttpRequest.newBuilder(
                URI.create("https://api.github.com/repos/Mohameee018/cutdown-system-desktop/releases"))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Cutdown-Desktop-Updater")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("GitHub releases metadata returned HTTP " + response.statusCode());
        }

        Object parsed = MiniJson.parse(response.body());
        if (!(parsed instanceof List<?> releases)) throw new IOException("Invalid GitHub releases metadata");

        Map<?,?> bestRelease = null;
        String bestVersion = "";
        for (Object item : releases) {
            if (!(item instanceof Map<?,?> release)) continue;
            if (Boolean.parseBoolean(value(release, "draft"))
                    || Boolean.parseBoolean(value(release, "prerelease"))) continue;

            String tag = value(release, "tag_name").replaceFirst("^[vV]", "").trim();
            if (tag.isBlank() || compareVersions(tag, bestVersion) <= 0) continue;

            Object assetsValue = release.get("assets");
            if (!(assetsValue instanceof List<?> assets)) continue;
            boolean hasExe = false;
            boolean hasChecksum = false;
            for (Object assetValue : assets) {
                if (!(assetValue instanceof Map<?,?> asset)) continue;
                String name = value(asset, "name");
                if (name.toLowerCase().endsWith(".exe")) hasExe = true;
                if ("SHA256SUMS.txt".equalsIgnoreCase(name)) hasChecksum = true;
            }
            if (hasExe && hasChecksum) {
                bestRelease = release;
                bestVersion = tag;
            }
        }

        if (bestRelease == null) throw new IOException("No valid GitHub release found");

        Object assetsValue = bestRelease.get("assets");
        if (!(assetsValue instanceof List<?> assets)) throw new IOException("GitHub release has no assets");

        String downloadUrl = "";
        String sha256 = "";
        for (Object assetValue : assets) {
            if (!(assetValue instanceof Map<?,?> asset)) continue;
            String name = value(asset, "name");
            String browserUrl = value(asset, "browser_download_url");
            if (name.toLowerCase().endsWith(".exe")) {
                downloadUrl = browserUrl;
            } else if ("SHA256SUMS.txt".equalsIgnoreCase(name)) {
                sha256 = readChecksumFile(client, browserUrl);
            }
        }

        if (bestVersion.isBlank() || downloadUrl.isBlank() || sha256.isBlank()) {
            throw new IOException("GitHub release metadata is incomplete");
        }

        return "{\"version\":\"" + escapeJson(bestVersion)
                + "\",\"download_url\":\"" + escapeJson(downloadUrl)
                + "\",\"sha256\":\"" + escapeJson(sha256)
                + "\",\"mandatory\":false}";
    }

    private static String readChecksumFile(HttpClient client, String url) throws Exception {
        if (url.isBlank()) return "";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/octet-stream")
                .header("User-Agent", "Cutdown-Desktop-Updater")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) return "";

        String[] lines = response.body().split("\\R");
        for (String line : lines) {
            String[] parts = line.trim().split("\\s+", 2);
            if (parts.length == 2 && parts[1].toLowerCase().endsWith(".exe")) {
                return parts[0].trim().toLowerCase();
            }
        }
        return "";
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
    }

    private static String value(Map<?,?> map, String key) { Object v = map.get(key); return v == null ? "" : String.valueOf(v); }

    private static String currentVersion() {
        return System.getProperty("cutdown.version",
                System.getenv().getOrDefault("CUTDOWN_DESKTOP_VERSION", "1.0.0"));
    }

    private static int compareVersions(String a, String b) {
        String[] aa = a.replaceFirst("^[vV]", "").split("\\.");
        String[] bb = b.replaceFirst("^[vV]", "").split("\\.");
        for (int i = 0; i < Math.max(aa.length, bb.length); i++) {
            int x = i < aa.length ? number(aa[i]) : 0;
            int y = i < bb.length ? number(bb[i]) : 0;
            if (x != y) return Integer.compare(x, y);
        }
        return 0;
    }

    private static int number(String s) {
        String n = s.replaceAll("[^0-9].*$", "");
        try { return Integer.parseInt(n.isBlank() ? "0" : n); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private static void downloadAndLaunch(String url, String expectedSha256, Window owner) {
        try {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme())) throw new IOException("Update URL must use HTTPS");
            String name = uri.getPath();
            name = name == null ? "Cutdown-Update.exe" : name.substring(name.lastIndexOf('/') + 1);
            if (!name.toLowerCase().endsWith(".exe")) name = "Cutdown-Update.exe";
            Path target = Files.createTempFile("cutdown-update-", "-" + name);
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(5)).GET().build();
            HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(target));
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IOException("Download failed");
            if (expectedSha256.isBlank()) throw new IOException("Update integrity hash is missing");
            String actualSha256 = sha256(target);
            if (!actualSha256.equals(expectedSha256)) {
                Files.deleteIfExists(target);
                throw new IOException("Update integrity check failed");
            }
            new ProcessBuilder(target.toAbsolutePath().toString()).start();
            System.exit(0);
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(owner,
                    "Update could not be downloaded. Please contact support.",
                    "Cutdown Update", JOptionPane.WARNING_MESSAGE));
        }
    }

    private static String sha256(Path file) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(var in=Files.newInputStream(file)){byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)digest.update(buf,0,n);}
        StringBuilder out=new StringBuilder();
        for(byte b:digest.digest())out.append(String.format("%02x",b));
        return out.toString();
    }
}

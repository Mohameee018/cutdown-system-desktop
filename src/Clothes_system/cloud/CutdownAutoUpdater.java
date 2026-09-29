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
import java.time.Duration;
import java.util.Map;

public final class CutdownAutoUpdater {
    private CutdownAutoUpdater() {}

    public static void checkAsync(Window owner) {
        Thread t = new Thread(() -> {
            try {
                String response = new CutdownCloudClient().get("/api/desktop/update");
                Object parsed = MiniJson.parse(response);
                if (!(parsed instanceof Map<?,?> map)) return;
                String latest = String.valueOf(map.getOrDefault("version", ""));
                String url = String.valueOf(map.getOrDefault("download_url", ""));
                boolean mandatory = Boolean.parseBoolean(String.valueOf(map.getOrDefault("mandatory", "false")));
                String current = currentVersion();
                if (latest.isBlank() || url.isBlank() || compareVersions(latest, current) <= 0) return;

                SwingUtilities.invokeLater(() -> {
                    String message = "A new Cutdown version (" + latest + ") is available.";
                    int choice = JOptionPane.showConfirmDialog(owner, message + "\n\nUpdate now?", "Cutdown Update",
                            JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    if (choice == JOptionPane.YES_OPTION || mandatory) {
                        downloadAndLaunch(url, owner);
                    }
                });
            } catch (Exception ignored) {
                // Update checks are non-blocking; the installed version keeps working.
            }
        }, "cutdown-auto-update-check");
        t.setDaemon(true);
        t.start();
    }

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

    private static void downloadAndLaunch(String url, Window owner) {
        try {
            URI uri = URI.create(url);
            String name = uri.getPath();
            name = name == null ? "Cutdown-Update.exe" : name.substring(name.lastIndexOf('/') + 1);
            if (!name.toLowerCase().endsWith(".exe")) name = "Cutdown-Update.exe";
            Path target = Files.createTempFile("cutdown-update-", "-" + name);
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(5)).GET().build();
            HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(target));
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IOException("Download failed");
            new ProcessBuilder(target.toAbsolutePath().toString()).start();
            System.exit(0);
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(owner,
                    "Update could not be downloaded. Please contact support.",
                    "Cutdown Update", JOptionPane.WARNING_MESSAGE));
        }
    }
}

package Clothes_system.cloud;

/**
 * Runtime configuration for the Cutdown cloud connection.
 *
 * The desktop application must never contain the Supabase service-role key.
 * The protected sync API is configured separately and can be supplied by
 * environment variables or JVM system properties.
 */
public final class CutdownCloudConfig {
    private CutdownCloudConfig() {}

    public static String baseUrl() {
        return value("CUTDOWN_API_BASE_URL", "cutdown.api.baseUrl", "");
    }

    public static String syncToken() {
        return value("CUTDOWN_DESKTOP_SYNC_TOKEN", "cutdown.api.syncToken", "");
    }

    public static int timeoutMillis() {
        String raw = value("CUTDOWN_API_TIMEOUT_MS", "cutdown.api.timeoutMs", "8000");
        try {
            return Math.max(1000, Integer.parseInt(raw));
        } catch (NumberFormatException ignored) {
            return 8000;
        }
    }

    public static boolean configured() {
        return !baseUrl().isBlank() && !syncToken().isBlank();
    }

    private static String value(String env, String property, String fallback) {
        String p = System.getProperty(property);
        if (p != null && !p.isBlank()) return p.trim();

        String e = System.getenv(env);
        if (e != null && !e.isBlank()) return e.trim();

        return fallback;
    }
}

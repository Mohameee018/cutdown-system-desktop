package Clothes_system.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Step 0.1 — SQLite Database Foundation.
 *
 * This is the ONLY class in the application allowed to talk to
 * java.sql.* directly. UI panels and business-logic managers must
 * never import java.sql.* themselves; if a future phase needs SQL
 * access it goes through a Data/DAO class in this package, which in
 * turn uses {@link #getConnection()}.
 *
 * Scope of this step:
 *  - Open a single SQLite connection to a fixed local file
 *    (clothes_system.db) next to the application.
 *  - Enable foreign key enforcement.
 *  - Create the schema tables/indexes if they do not already exist.
 *  - Provide a clean shutdown hook.
 *
 * This class owns only the SQLite connection/schema lifecycle. The
 * PersistenceRepository is responsible for loading/saving the application
 * entities that are mirrored in memory by the Swing screens.
 */
public final class DatabaseManager {

    /** Fixed, stable location for the SQLite database file. */
    private static final String DB_FILE_NAME = "clothes_system.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE_NAME;

    private static Connection connection;
    private static boolean initialized = false;

    private DatabaseManager() {}

    /**
     * Opens the SQLite connection (if not already open), enables
     * foreign key constraints, and creates the schema if needed.
     * Safe to call more than once; subsequent calls are no-ops.
     *
     * Intended to be called once at application startup.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        try {
            // Ensure the JDBC driver class is registered even under
            // the Java Platform Module System, regardless of whether
            // ServiceLoader auto-discovery picks it up.
            Class.forName("org.sqlite.JDBC");

            connection = DriverManager.getConnection(DB_URL);

            try (Statement pragma = connection.createStatement()) {
                pragma.execute("PRAGMA foreign_keys = ON");
            }

            migrateLegacyFoundationIfNeeded();
            createSchemaIfNeeded();

            initialized = true;
            System.out.println("[DatabaseManager] SQLite ready at: " + DB_FILE_NAME);

        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                "SQLite JDBC driver not found on the classpath (org.sqlite.JDBC). "
                + "Add the sqlite-jdbc dependency/jar to the project build path.", e);
        } catch (SQLException e) {
            throw new IllegalStateException(
                "Failed to initialize SQLite database '" + DB_FILE_NAME + "': " + e.getMessage(), e);
        }
    }

    /**
     * Returns the shared connection, initializing it first if needed.
     * Package-private-style usage is expected: only Data/DAO classes
     * inside Clothes_system.db should call this.
     */
    public static synchronized Connection getConnection() {
        if (!initialized) {
            initialize();
        }
        return connection;
    }

    /**
     * Closes the SQLite connection cleanly. Safe to call multiple
     * times or even if initialize() was never called.
     */
    public static synchronized void shutdown() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("[DatabaseManager] Error closing SQLite connection: " + e.getMessage());
            } finally {
                connection = null;
                initialized = false;
            }
        }
    }


    /**
     * The original foundation release created tables with INTEGER primary
     * keys, while the live application uses IDs such as PRD-001/ORD-001.
     * That foundation never wrote business data, but we still preserve any
     * old tables rather than dropping them. The current schema is then
     * created alongside them.
     */
    private static void migrateLegacyFoundationIfNeeded() throws SQLException {
        if (!tableExists("products")) return;
        boolean legacy = false;
        try (Statement st=connection.createStatement();
             ResultSet rs=st.executeQuery("PRAGMA table_info(products)")) {
            while(rs.next()) {
                if ("id".equalsIgnoreCase(rs.getString("name"))
                        && rs.getString("type").toUpperCase(java.util.Locale.ROOT).contains("INTEGER")) {
                    legacy = true; break;
                }
            }
        }
        if (!legacy) return;

        String suffix = String.valueOf(System.currentTimeMillis());
        String[] tables = {"product_inventory_history","product_price_history","product_warehouse_stock",
                "product_variants","order_items","returns","orders","customers","expenses","warehouses","products","settings"};
        try (Statement st=connection.createStatement()) {
            for(String t:tables) if(tableExists(t))
                st.executeUpdate("ALTER TABLE \""+t+"\" RENAME TO \"legacy_"+t+"_"+suffix+"\"");
        }
        System.out.println("[DatabaseManager] Preserved legacy foundation tables under legacy_*.");
    }

    private static boolean tableExists(String name) throws SQLException {
        try(PreparedStatement ps=connection.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?")) {
            ps.setString(1,name);
            try(ResultSet rs=ps.executeQuery()){return rs.next();}
        }
    }

    private static void createSchemaIfNeeded() throws SQLException {
        try (Statement st = connection.createStatement()) {
            for (String ddl : DatabaseSchema.CREATE_TABLES) {
                st.execute(ddl);
            }
            for (String ddl : DatabaseSchema.CREATE_INDEXES) {
                st.execute(ddl);
            }
        }
    }

    // =========================================================
    // DATA SAFETY: integrity checks, backup, restore
    // =========================================================

    /** Absolute path to the live SQLite file, for backup/restore callers. */
    public static File getDatabaseFile() {
        return new File(DB_FILE_NAME).getAbsoluteFile();
    }

    /** Runs SQLite's own consistency check on the live connection. */
    public static synchronized boolean integrityCheckOk() {
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery("PRAGMA integrity_check")) {
            return rs.next() && "ok".equalsIgnoreCase(rs.getString(1));
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] integrity_check failed: " + e.getMessage());
            return false;
        }
    }

    /** True when this live SQLite connection has foreign-key enforcement enabled. */
    public static synchronized boolean foreignKeysEnabled() {
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery("PRAGMA foreign_keys")) {
            return rs.next() && rs.getInt(1) == 1;
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] PRAGMA foreign_keys failed: " + e.getMessage());
            return false;
        }
    }

    /** True when PRAGMA foreign_key_check reports zero violation rows. */
    public static synchronized boolean foreignKeyCheckOk() {
        try {
            // foreign_key_check reports relationships even on a connection
            // where enforcement is disabled, but enabling it here makes the
            // validation explicit and keeps this method aligned with the
            // application's safety contract.
            try (Statement enable = getConnection().createStatement()) {
                enable.execute("PRAGMA foreign_keys = ON");
            }
            try (Statement st = getConnection().createStatement();
                 ResultSet rs = st.executeQuery("PRAGMA foreign_key_check")) {
                return !rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] foreign_key_check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Opens a short-lived, independent connection to the given file (NOT
     * the live app connection) and confirms it is a readable, structurally
     * sound SQLite database. Used to validate a backup file right after
     * creating it, and to validate a candidate file before restoring it.
     */
    public static boolean isValidSqliteFile(File f) {
        if (f == null || !f.isFile() || f.length() <= 0) {
            return false;
        }
        String url = "jdbc:sqlite:" + f.getAbsolutePath();
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            return false;
        }
        try (Connection test = DriverManager.getConnection(url)) {
            try (Statement pragma = test.createStatement()) {
                pragma.execute("PRAGMA foreign_keys = ON");
            }
            try (Statement st = test.createStatement();
                 ResultSet rs = st.executeQuery("PRAGMA foreign_keys")) {
                if (!(rs.next() && rs.getInt(1) == 1)) {
                    return false;
                }
            }
            try (Statement st = test.createStatement();
                 ResultSet rs = st.executeQuery("PRAGMA integrity_check")) {
                if (!(rs.next() && "ok".equalsIgnoreCase(rs.getString(1)))) {
                    return false;
                }
            }
            try (Statement st = test.createStatement();
                 ResultSet rs = st.executeQuery("PRAGMA foreign_key_check")) {
                if (rs.next()) {
                    return false;
                }
            }
            try (Statement st = test.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM sqlite_master")) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Writes a consistent, standalone snapshot of the live database to
     * destFile using SQLite's own "VACUUM INTO", which takes an internal
     * read lock and streams a clean copy without blocking the app's own
     * connection or ever touching the live file on disk directly (unlike a
     * plain file copy, which could catch the .db file mid-write). The
     * caller is responsible for flushing pending in-memory state (e.g. via
     * PersistenceRepository.saveAll()) before calling this, so the
     * snapshot reflects the latest data.
     *
     * destFile must not already exist; SQLite refuses to overwrite it.
     */
    public static synchronized File backupTo(File destFile) throws SQLException, IOException {
        if (destFile == null) {
            throw new IllegalArgumentException("Backup destination is required.");
        }
        if (destFile.exists() && !destFile.delete()) {
            throw new IOException("Could not replace existing file: " + destFile);
        }
        File parent = destFile.getAbsoluteFile().getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }
        String escapedPath = destFile.getAbsolutePath().replace("'", "''");
        try (Statement st = getConnection().createStatement()) {
            st.execute("VACUUM INTO '" + escapedPath + "'");
        }
        if (!isValidSqliteFile(destFile)) {
            throw new IllegalStateException(
                "Backup file was written but failed validation: " + destFile);
        }
        return destFile;
    }

    /**
     * Restores the live database from a previously-created backup file.
     * Safety sequence:
     *   1. Validate the backup file is a real, intact SQLite database
     *      BEFORE touching anything live.
     *   2. Copy the current live file aside to a timestamped
     *      "*.pre_restore_<ts>.bak" file, so a failed/aborted restore can
     *      always be recovered from manually.
     *   3. Close the live connection so the file is not open/locked.
     *   4. Copy the backup over the live file location.
     *
     * This does NOT re-open the connection or reload any in-memory Swing
     * state: the running application's managers (ProductManager,
     * OrdersPanel, CustomersPanel, etc.) were all populated from the OLD
     * data at startup and have no generic "reload everything" hook today.
     * Continuing to run against stale in-memory data after swapping the
     * file out from under it — and then letting the normal shutdown-time
     * saveAll() overwrite the just-restored file with that stale data —
     * is a worse data-safety outcome than asking for a restart. The
     * caller (UI) is expected to tell the user to restart the app, and
     * must suppress the shutdown-time autosave for this run (see
     * Clothes_system.suppressSaveOnNextShutdown()) so it does not
     * immediately clobber the restored file on exit.
     */
    public static synchronized File restoreFrom(File backupFile) throws SQLException, IOException {
        if (backupFile == null || !backupFile.isFile()) {
            throw new IllegalArgumentException("Backup file does not exist: " + backupFile);
        }
        if (!isValidSqliteFile(backupFile)) {
            throw new IllegalStateException(
                "Selected file is not a valid, intact SQLite database: " + backupFile);
        }

        File live = getDatabaseFile();
        File safetyCopy = new File(
                live.getParentFile(),
                live.getName() + ".pre_restore_" + System.currentTimeMillis() + ".bak");

        if (live.exists()) {
            Files.copy(live.toPath(), safetyCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        // Close the live connection so the destination file isn't open
        // while we replace it.
        shutdown();

        try {
            Files.copy(backupFile.toPath(), live.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException copyFailure) {
            // Best-effort recovery: put the safety copy back so the app
            // is left with its previous, known-good database rather than
            // a half-written file.
            if (safetyCopy.exists()) {
                try {
                    Files.copy(safetyCopy.toPath(), live.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException recoveryFailure) {
                    System.err.println(
                        "[DatabaseManager] CRITICAL: restore copy failed AND recovery copy failed. "
                        + "Live DB may be missing/corrupt. Safety copy preserved at: " + safetyCopy);
                }
            }
            throw copyFailure;
        }

        if (!isValidSqliteFile(live)) {
            // The copy "succeeded" but what landed on disk is not a sound
            // SQLite file (e.g. a truncated/interrupted copy). Auto-heal
            // by putting the pre-restore safety copy straight back rather
            // than leaving the app pointed at a broken database file.
            if (safetyCopy.exists()) {
                try {
                    Files.copy(safetyCopy.toPath(), live.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException recoveryFailure) {
                    System.err.println(
                        "[DatabaseManager] CRITICAL: restored file failed validation AND recovery copy failed. "
                        + "Live DB may be missing/corrupt. Safety copy preserved at: " + safetyCopy);
                }
            }
            throw new IllegalStateException(
                "Restored file failed post-copy validation: " + live
                + " (previous database has been put back from " + safetyCopy + ")");
        }

        return live;
    }
}

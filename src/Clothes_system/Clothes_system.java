package Clothes_system;

import Clothes_system.db.DatabaseManager;
import Clothes_system.db.PersistenceRepository;
import Clothes_system.cloud.CutdownOnlineGate;

import javax.swing.*;
import java.awt.*;

public class Clothes_system extends JFrame {

    private JPanel mainPanel;
    private CardLayout cardLayout;
    private DashboardPanel dashboardPanel;
    private ProductsPanel productsPanel;
    private InventoryPanel inventoryPanel;
    private OrdersPanel ordersPanel;
    private CustomersPanel customersPanel;
    private ReturnsPanel returnsPanel;
    private ExpensesPanel expensesPanel;
    private ReportsPanel reportsPanel;
    private SettingsPanel settingsPanel;
    private Sidebar sidebar;

    private static Clothes_system currentInstance;

    // Set right before an intentional restart/exit after a successful
    // database Restore, so the normal shutdown-time saveAll() (which
    // would otherwise overwrite the just-restored file with the old,
    // still-in-memory data) is skipped for this run. See
    // PersistenceRepository.restoreDatabase(...).
    private static volatile boolean skipSaveOnShutdown = false;

    public static void suppressSaveOnNextShutdown() {
        skipSaveOnShutdown = true;
    }

    public Clothes_system() {

        currentInstance = this;

        setTitle("VYRA Dashboard");

        setSize(
            1536,
            1024
        );

        setDefaultCloseOperation(
            JFrame.EXIT_ON_CLOSE
        );

        setLocationRelativeTo(null);

        setLayout(
            new BorderLayout()
        );

        // ================= SIDEBAR =================

        sidebar =
            new Sidebar(this);

        add(
            sidebar,
            BorderLayout.WEST
        );

        // ================= CARD LAYOUT =================

        cardLayout =
            new CardLayout();

        mainPanel =
            new JPanel(
                cardLayout
            );

        // ================= PAGES =================

        dashboardPanel = new DashboardPanel();

        mainPanel.add(
            dashboardPanel,
            "Dashboard"
        );

        productsPanel = new ProductsPanel();
        mainPanel.add(productsPanel, "Products");

        inventoryPanel = new InventoryPanel();
        mainPanel.add(inventoryPanel, "Inventory");

        ordersPanel = new OrdersPanel();
        mainPanel.add(ordersPanel, "Orders");

        customersPanel = new CustomersPanel();
        mainPanel.add(customersPanel, "Customers");

        returnsPanel = new ReturnsPanel();
        mainPanel.add(returnsPanel, "Returns");

        expensesPanel = new ExpensesPanel();
        mainPanel.add(expensesPanel, "Expenses");

        reportsPanel = new ReportsPanel();
        mainPanel.add(reportsPanel, "Reports");

        settingsPanel = new SettingsPanel();
        mainPanel.add(settingsPanel, "Settings");

        add(
            mainPanel,
            BorderLayout.CENTER
        );

        // Now that every panel exists, let the dashboard's
        // quick-links jump to the right tab.
        dashboardPanel.wireNavigation(this);

        // Start with Dashboard
        showPage(
            "Dashboard"
        );
    }

    public void globalSearch(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        if (q.isEmpty()) return;
        boolean orderMatch = false;
        for (OrdersPanel.Order o : OrdersPanel.getOrders()) {
            if (String.valueOf(o.id).toLowerCase().contains(q)
                    || String.valueOf(o.customer).toLowerCase().contains(q)
                    || String.valueOf(o.phone).toLowerCase().contains(q)) { orderMatch=true; break; }
            for (OrdersPanel.OrderItem i:o.items) if (String.valueOf(i.name).toLowerCase().contains(q) || String.valueOf(i.sku).toLowerCase().contains(q)) { orderMatch=true; break; }
            if(orderMatch) break;
        }
        if (orderMatch) { ordersPanel.setSearchQuery(query); showPage("Orders"); return; }
        boolean productMatch = !ProductManager.getInstance().search(query).isEmpty();
        if (productMatch) { productsPanel.setSearchQuery(query); showPage("Products"); return; }
        if (CustomersPanel.findCustomer(query)) { customersPanel.setSearchQuery(query); showPage("Customers"); return; }
        JOptionPane.showMessageDialog(this, "No matching order, product or customer was found.", "Search", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void refreshSidebarBranding() {
        if (currentInstance != null && currentInstance.sidebar != null) {
            currentInstance.setTitle(SettingsManager.getStoreName());
            currentInstance.sidebar.refreshBrandingAndTheme();
        }
    }

    public void showPage(
        String page
    ) {

        if (sidebar != null) {
            sidebar.setActivePage(page);
        }

        // Every time the Dashboard is shown, refresh it from
        // real data so it never goes stale (products, orders
        // and customers can all change while on other tabs).
        if ("Dashboard".equals(page) && dashboardPanel != null) {
            dashboardPanel.refreshData();
        }

        cardLayout.show(
            mainPanel,
            page
        );
    }

    /**
     * Refresh all existing data views after an in-memory data change.
     * This is a small synchronization hook for Clothes V1; it is not
     * an architecture refactor and does not introduce a database layer.
     */
    public static void refreshAllDataViews() {
        Clothes_system frame = currentInstance;
        if (frame == null) return;

        Runnable refresh = () -> {
            if (frame.productsPanel != null) frame.productsPanel.refreshData();
            if (frame.inventoryPanel != null) frame.inventoryPanel.refreshData();
            if (frame.ordersPanel != null) frame.ordersPanel.refreshData();
            if (frame.returnsPanel != null) frame.returnsPanel.refreshData();
            if (frame.dashboardPanel != null) frame.dashboardPanel.refreshData();
            if (frame.expensesPanel != null) frame.expensesPanel.refreshData();
            if (frame.reportsPanel != null) frame.reportsPanel.refreshData();
            PersistenceRepository.saveAll();
        };

        if (SwingUtilities.isEventDispatchThread()) refresh.run();
        else SwingUtilities.invokeLater(refresh);
    }

    public static void main(
        String[] args
    ) {

        // Every installation is assigned to exactly one brand account.
        // No desktop UI is exposed until the brand administrator signs in.
        if (!CutdownOnlineGate.requireLogin(null)) return;

        // Cloud is authoritative for this installation. Do not initialize
        // SQLite or expose the UI unless the protected Cutdown cloud API is
        // reachable. This prevents local-only work during an internet outage.
        if (!CutdownOnlineGate.requireOnlineAtStartup(null)) {
            return;
        }

        // ================= SQLITE STARTUP =================
        DatabaseManager.initialize();
        PersistenceRepository.markSeedCompleteIfDatabaseAlreadyHasData();
        InventoryPanel.WarehouseManager.loadFromPersistence();
        PersistenceRepository.saveWarehouses();
        // Products, orders, returns and expenses load their persisted
        // collections before the Swing screens are constructed.
        ProductManager.getInstance();
        OrdersPanel.loadFromPersistence();
        // Returns must be loaded before any ReturnsPanel is constructed so
        // return accounting and processed quantities are available when
        // orders are reconstructed for the UI.
        PersistenceRepository.loadReturns();
        PersistenceRepository.loadExpenses();
        PersistenceRepository.loadSettings();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!skipSaveOnShutdown) {
                PersistenceRepository.saveAll();
            }
            DatabaseManager.shutdown();
        }));

        SwingUtilities.invokeLater(() -> {
            Clothes_system frame = new Clothes_system();
            PersistenceRepository.markInitialSeedComplete();
            PersistenceRepository.saveAll();
            CutdownOnlineGate.installWatchdog(frame);
            frame.setVisible(true);
            CutdownOnlineGate.checkForUpdates(frame);
        });
    }
}
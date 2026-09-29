package Clothes_system;

import Clothes_system.db.PersistenceRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.event.TableModelListener;
import java.awt.*;

public class InventoryPanel extends JPanel {

    private JTable inventoryTable;
    private DefaultTableModel inventoryModel;

    private JLabel totalProductsLabel;
    private JLabel inStockLabel;
    private JLabel lowStockLabel;
    private JLabel outOfStockLabel;

    private TableModelListener productModelListener;

    // null = show combined ("All Warehouses") stock; otherwise a
    // warehouse id from WarehouseManager, to view/edit that
    // warehouse's stock specifically.
    private String selectedWarehouseId = null;
    private JComboBox<String> warehouseFilter;

    public InventoryPanel() {

        setBackground(
            new Color(248, 250, 249)
        );

        setLayout(
            new BorderLayout()
        );


        // =========================
        // HEADER
        // =========================

        JPanel header =
            new JPanel(
                new BorderLayout()
            );

        header.setBackground(
            new Color(248, 250, 249)
        );

        header.setBorder(
            BorderFactory.createEmptyBorder(
                20, 30, 15, 30
            )
        );


        // =========================
        // TITLE
        // =========================

        JPanel titleBox =
            new JPanel();

        titleBox.setBackground(
            new Color(248, 250, 249)
        );

        titleBox.setLayout(
            new BoxLayout(
                titleBox,
                BoxLayout.Y_AXIS
            )
        );


        JLabel title =
            new JLabel("Inventory");

        title.setFont(
            new Font(
                "Arial",
                Font.BOLD,
                24
            )
        );

        title.setForeground(
            new Color(25, 45, 45)
        );


        JLabel subtitle =
            new JLabel(
                "Monitor stock levels and inventory status"
            );

        subtitle.setFont(
            new Font(
                "Arial",
                Font.PLAIN,
                12
            )
        );

        subtitle.setForeground(
            new Color(140, 150, 150)
        );


        titleBox.add(title);

        titleBox.add(
            Box.createVerticalStrut(5)
        );

        titleBox.add(subtitle);


        header.add(
            titleBox,
            BorderLayout.WEST
        );


        // =========================
        // WAREHOUSE CONTROLS
        // =========================

        JPanel warehouseControls =
            new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 10, 0)
            );

        warehouseControls.setBackground(
            new Color(248, 250, 249)
        );

        JLabel warehouseLabel = new JLabel("Viewing:");
        warehouseLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        warehouseLabel.setForeground(new Color(90, 105, 105));
        warehouseControls.add(warehouseLabel);

        warehouseFilter = new JComboBox<>();
        warehouseFilter.setFont(new Font("Arial", Font.PLAIN, 12));
        warehouseFilter.setPreferredSize(new Dimension(200, 32));
        warehouseControls.add(warehouseFilter);

        JButton manageWarehousesBtn = new JButton("Manage Warehouses");
        manageWarehousesBtn.setFont(new Font("Arial", Font.BOLD, 11));
        manageWarehousesBtn.setFocusPainted(false);
        manageWarehousesBtn.setBackground(new Color(25, 115, 105));
        manageWarehousesBtn.setForeground(Color.WHITE);
        manageWarehousesBtn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        manageWarehousesBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        manageWarehousesBtn.addActionListener(e -> openManageWarehousesDialog());
        warehouseControls.add(manageWarehousesBtn);

        header.add(
            warehouseControls,
            BorderLayout.EAST
        );

        refreshWarehouseFilter();

        warehouseFilter.addActionListener(e -> {
            int idx = warehouseFilter.getSelectedIndex();
            selectedWarehouseId = idx <= 0
                    ? null
                    : InventoryPanel.WarehouseManager.getWarehouses().get(idx - 1).getId();
            refreshInventory();
        });


        add(
            header,
            BorderLayout.NORTH
        );


        // =========================
        // CONTENT
        // =========================

        JPanel content =
            new JPanel(
                new BorderLayout()
            );

        content.setBackground(
            new Color(248, 250, 249)
        );

        content.setBorder(
            BorderFactory.createEmptyBorder(
                0, 30, 30, 30
            )
        );


        // =========================
        // SUMMARY CARDS
        // =========================

        JPanel summaryPanel =
            new JPanel(
                new GridLayout(
                    1, 4, 15, 0
                )
            );

        summaryPanel.setBackground(
            new Color(248, 250, 249)
        );


        JPanel totalCard = createSummaryCard("Total Products", "0", new Color(25, 115, 105));
        totalProductsLabel = (JLabel) totalCard.getClientProperty("valueLabel");
        summaryPanel.add(totalCard);


        JPanel inCard = createSummaryCard("In Stock", "0", new Color(55, 130, 85));
        inStockLabel = (JLabel) inCard.getClientProperty("valueLabel");
        summaryPanel.add(inCard);


        JPanel lowCard = createSummaryCard("Low Stock", "0", new Color(210, 145, 45));
        lowStockLabel = (JLabel) lowCard.getClientProperty("valueLabel");
        summaryPanel.add(lowCard);


        JPanel outCard = createSummaryCard("Out of Stock", "0", new Color(190, 75, 75));
        outOfStockLabel = (JLabel) outCard.getClientProperty("valueLabel");
        summaryPanel.add(outCard);


        summaryPanel.setPreferredSize(
            new Dimension(
                0,
                105
            )
        );


        content.add(
            summaryPanel,
            BorderLayout.NORTH
        );


        // =========================
        // TABLE
        // =========================

        String[] columns = {
            "Product ID",
            "Product Name",
            "Category",
            "Size",
            "Stock",
            "Minimum Stock",
            "Status"
        };

        inventoryModel =
            new DefaultTableModel(
                columns,
                0
            ) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

        inventoryTable =
            new JTable(inventoryModel);

        inventoryTable.setRowHeight(50);


        inventoryTable.setFont(
            new Font(
                "Arial",
                Font.PLAIN,
                12
            )
        );


        inventoryTable.setForeground(
            new Color(50, 65, 65)
        );


        inventoryTable.setGridColor(
            new Color(235, 240, 240)
        );


        inventoryTable.setSelectionBackground(
            new Color(230, 243, 241)
        );


        inventoryTable.setSelectionForeground(
            new Color(25, 80, 75)
        );


        // =========================
        // TABLE HEADER
        // =========================

        inventoryTable
            .getTableHeader()
            .setFont(
                new Font(
                    "Arial",
                    Font.BOLD,
                    12
                )
            );


        inventoryTable
            .getTableHeader()
            .setForeground(
                new Color(70, 85, 85)
            );


        inventoryTable
            .getTableHeader()
            .setBackground(
                new Color(245, 248, 248)
            );


        inventoryTable
            .getTableHeader()
            .setPreferredSize(
                new Dimension(
                    0,
                    42
                )
            );


        // =========================
        // STATUS RENDERER
        // =========================

        inventoryTable
            .getColumnModel()
            .getColumn(6)
            .setCellRenderer(
                new StatusRenderer()
            );


        inventoryTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && selectedWarehouseId != null) {
                    int row = inventoryTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        String productId = String.valueOf(inventoryModel.getValueAt(row, 0));
                        openAdjustWarehouseStockDialog(productId);
                    }
                }
            }
        });


        // =========================
        // SCROLL PANE
        // =========================

        JScrollPane scrollPane =
            new JScrollPane(
                inventoryTable
            );


        scrollPane.setBorder(
            BorderFactory.createLineBorder(
                new Color(225, 232, 232)
            )
        );


        JPanel tableContainer =
            new JPanel(
                new BorderLayout()
            );

        tableContainer.setBackground(
            Color.WHITE
        );

        tableContainer.setBorder(
            BorderFactory.createEmptyBorder(
                20, 0, 0, 0
            )
        );


        tableContainer.add(
            scrollPane,
            BorderLayout.CENTER
        );


        content.add(
            tableContainer,
            BorderLayout.CENTER
        );


        add(
            content,
            BorderLayout.CENTER
        );

        refreshInventory();
    }


    // =========================
    // SUMMARY CARD
    // =========================

    private JPanel createSummaryCard(
        String title,
        String value,
        Color accent
    ) {

        JPanel card =
            new JPanel(
                new BorderLayout()
            );

        card.setBackground(
            Color.WHITE
        );


        card.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    new Color(225, 232, 232)
                ),
                BorderFactory.createEmptyBorder(
                    15, 18, 15, 18
                )
            )
        );


        JLabel titleLabel =
            new JLabel(title);

        titleLabel.setFont(
            new Font(
                "Arial",
                Font.PLAIN,
                12
            )
        );

        titleLabel.setForeground(
            new Color(130, 145, 145)
        );


        JLabel valueLabel =
            new JLabel(value);

        valueLabel.setFont(
            new Font(
                "Arial",
                Font.BOLD,
                25
            )
        );

        valueLabel.setForeground(
            accent
        );


        card.add(
            titleLabel,
            BorderLayout.NORTH
        );


        card.add(
            valueLabel,
            BorderLayout.CENTER
        );

        card.putClientProperty(
            "valueLabel",
            valueLabel
        );

        return card;
    }


    // =========================================================
    // REFRESH FROM PRODUCTS
    // =========================================================

    public void refreshData() {
        refreshWarehouseFilter();
        refreshInventory();
    }

    public void refreshInventory() {

        if (inventoryModel == null) {
            return;
        }

        inventoryModel.setRowCount(0);

        String stockColumnName = selectedWarehouseId == null
                ? "Stock (All Warehouses)"
                : "Stock (" + warehouseName(selectedWarehouseId) + ")";

        inventoryModel.setColumnIdentifiers(new Object[]{
                "Product ID", "Product Name", "Category", "Size",
                stockColumnName, "Minimum Stock", "Status"
        });

        for (Product p : ProductManager.getInstance().getAllProducts()) {

            String size = "";
            if (!p.getVariants().isEmpty()) {
                size = p.getVariants().get(0).getSize();
            }

            int stock = selectedWarehouseId == null
                    ? p.getStockQuantity()
                    : p.getWarehouseQuantity(selectedWarehouseId);

            int minimumStock = Math.max(0, p.getMinimumStock());

            String status = stock <= 0 ? "Out of Stock"
                    : stock <= minimumStock ? "Low Stock" : "In Stock";

            inventoryModel.addRow(new Object[]{
                    p.getId(),
                    p.getName(),
                    p.getCategory(),
                    size,
                    stock,
                    minimumStock,
                    status
            });
        }

        updateSummary();
    }

    private String warehouseName(String id) {
        Warehouse w = WarehouseManager.findById(id);
        return w != null ? w.getName() : id;
    }

    private void refreshWarehouseFilter() {

        if (warehouseFilter == null) {
            return;
        }

        warehouseFilter.removeAllItems();
        warehouseFilter.addItem("All Warehouses (Total)");

        for (Warehouse w : WarehouseManager.getWarehouses()) {
            warehouseFilter.addItem(w.getName());
        }
    }

    private void openManageWarehousesDialog() {

        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Manage Warehouses",
                Dialog.ModalityType.APPLICATION_MODAL
        );

        dialog.setSize(420, 420);
        dialog.setLocationRelativeTo(this);

        DefaultListModel<Warehouse> listModel = new DefaultListModel<>();
        for (Warehouse w : WarehouseManager.getWarehouses()) {
            listModel.addElement(w);
        }

        JList<Warehouse> list = new JList<>(listModel);
        list.setFont(new Font("Arial", Font.PLAIN, 13));
        JScrollPane listScroll = new JScrollPane(list);

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        main.add(listScroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

        JButton addBtn = new JButton("+ Add Warehouse");
        addBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(dialog, "Warehouse name:");
            if (name != null && !name.trim().isEmpty()) {
                try {
                    Warehouse w = WarehouseManager.addWarehouse(name.trim());
                    listModel.addElement(w);
                    refreshWarehouseFilter();
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(
                            dialog,
                            ex.getMessage(),
                            "Invalid Warehouse Name",
                            JOptionPane.WARNING_MESSAGE
                    );
                }
            }
        });

        JButton renameBtn = new JButton("Rename");
        renameBtn.addActionListener(e -> {
            Warehouse selected = list.getSelectedValue();
            if (selected == null) return;
            String name = JOptionPane.showInputDialog(dialog, "New name:", selected.getName());
            if (name != null && !name.trim().isEmpty()) {
                selected.setName(name.trim());
                // Persist the rename immediately, the same way
                // addWarehouse()/removeWarehouse() do, so it survives a
                // restart even if the app is closed abnormally rather
                // than through the normal shutdown hook.
                PersistenceRepository.saveWarehouses();
                // saveWarehouses() itself no longer wipes other warehouses'
                // product_warehouse_stock rows (it upserts instead of
                // delete-all-and-reinsert — see saveWarehousesBody()). This
                // extra resave is defense in depth to keep this warehouse's
                // own product rows' updated_at/consistency in step with the
                // rename, matching addWarehouse()/removeWarehouse().
                for (Product p : ProductManager.getInstance().getAllProducts()) PersistenceRepository.saveProduct(p);
                list.repaint();
                refreshWarehouseFilter();
                refreshInventory();
            }
        });

        JButton removeBtn = new JButton("Remove");
        removeBtn.addActionListener(e -> {
            Warehouse selected = list.getSelectedValue();
            if (selected == null) return;
            if (WarehouseManager.DEFAULT_WAREHOUSE_ID.equals(selected.getId())) {
                JOptionPane.showMessageDialog(dialog, "The main warehouse cannot be removed.", "Not Allowed", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (JOptionPane.showConfirmDialog(dialog,
                    "Remove \"" + selected.getName() + "\"? Its stock will move back into the Main Warehouse.",
                    "Remove Warehouse", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                WarehouseManager.removeWarehouse(selected.getId());
                listModel.removeElement(selected);
                if (selected.getId().equals(selectedWarehouseId)) {
                    selectedWarehouseId = null;
                }
                refreshWarehouseFilter();
                refreshInventory();
                Clothes_system.refreshAllDataViews();
            }
        });

        bottom.add(addBtn);
        bottom.add(renameBtn);
        bottom.add(removeBtn);
        main.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(main);
        dialog.setVisible(true);
    }

    private void openAdjustWarehouseStockDialog(String productId) {

        if (selectedWarehouseId == null) {
            return;
        }

        Product p = ProductManager.getInstance().findById(productId);
        if (p == null) return;

        int current = p.getWarehouseQuantity(selectedWarehouseId);

        String input = JOptionPane.showInputDialog(
                this,
                "New quantity for \"" + p.getName() + "\" in " + warehouseName(selectedWarehouseId) + ":",
                current
        );

        if (input == null) return;

        try {
            int qty = Integer.parseInt(input.trim());
            if (qty < 0) {
                JOptionPane.showMessageDialog(this, "Quantity cannot be negative.", "Invalid Quantity", JOptionPane.WARNING_MESSAGE);
                return;
            }
            p.setWarehouseQuantity(selectedWarehouseId, qty, "Manual warehouse adjustment");
            PersistenceRepository.saveProduct(p);
            refreshInventory();
            Clothes_system.refreshAllDataViews();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a whole number.", "Invalid Quantity", JOptionPane.WARNING_MESSAGE);
        }
    }

    private String value(DefaultTableModel m, int row, int col) {
        if (m == null || row < 0 || row >= m.getRowCount() || col < 0 || col >= m.getColumnCount()) {
            return "";
        }
        Object v = m.getValueAt(row, col);
        return v == null ? "" : String.valueOf(v);
    }

    private int parseInt(Object v) {
        try {
            return Integer.parseInt(String.valueOf(v).replace(",", "").trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private void updateSummary() {

        int total = 0;
        int in = 0;
        int low = 0;
        int out = 0;

        for (int row = 0; row < inventoryModel.getRowCount(); row++) {
            total++;
            String status = String.valueOf(inventoryModel.getValueAt(row, 6));
            if ("Out of Stock".equals(status)) out++;
            else if ("Low Stock".equals(status)) low++;
            else in++;
        }

        if (totalProductsLabel != null) totalProductsLabel.setText(String.valueOf(total));
        if (inStockLabel != null) inStockLabel.setText(String.valueOf(in));
        if (lowStockLabel != null) lowStockLabel.setText(String.valueOf(low));
        if (outOfStockLabel != null) outOfStockLabel.setText(String.valueOf(out));
    }

    // =========================
    // STATUS RENDERER
    // =========================

    private static class StatusRenderer
        extends JLabel
        implements TableCellRenderer {

        public StatusRenderer() {

            setOpaque(true);

            setHorizontalAlignment(
                SwingConstants.CENTER
            );

            setFont(
                new Font(
                    "Arial",
                    Font.BOLD,
                    11
                )
            );
        }


        @Override
        public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column
        ) {

            String status =
                value.toString();


            setText(status);


            if (status.equals("In Stock")) {

                setForeground(
                    new Color(45, 125, 75)
                );

                setBackground(
                    new Color(225, 242, 230)
                );

            }

            else if (
                status.equals("Low Stock")
            ) {

                setForeground(
                    new Color(175, 115, 20)
                );

                setBackground(
                    new Color(250, 240, 215)
                );

            }

            else {

                setForeground(
                    new Color(175, 60, 60)
                );

                setBackground(
                    new Color(250, 225, 225)
                );
            }


            return this;
        }
    }

    // =========================================================
    // MULTI-WAREHOUSE SUPPORT
    // Nested here (instead of a new file) so a shop with several
    // storage locations can track how much of each product sits
    // in each one, while every other screen keeps working off the
    // simple combined total (Product.getStockQuantity()).
    // =========================================================

    public static class Warehouse {

        private final String id;
        private String name;
        private String location = "";
        private boolean active = true;

        public Warehouse(String id, String name) {
            this(id, name, "", true);
        }

        public Warehouse(String id, String name, String location, boolean active) {
            this.id = id;
            this.name = name;
            this.location = location == null ? "" : location;
            this.active = active;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location == null ? "" : location; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }

        @Override
        public String toString() {
            return name;
        }
    }

    public static class WarehouseManager {

        public static final String DEFAULT_WAREHOUSE_ID = "WH-MAIN";

        private static final java.util.List<Warehouse> warehouses = new java.util.ArrayList<>();
        private static int counter = 1;

        static {
            warehouses.add(new Warehouse(DEFAULT_WAREHOUSE_ID, "Main Warehouse"));
        }

        public static void clearForPersistence() {
            warehouses.clear();
            counter = 1;
        }

        public static void addPersistedWarehouse(Warehouse w) {
            if (w == null) return;
            warehouses.add(w);
            try {
                if (w.getId().startsWith("WH-")) counter = Math.max(counter, Integer.parseInt(w.getId().substring(3)));
            } catch (Exception ignored) {}
        }

        public static void loadFromPersistence() {
            PersistenceRepository.loadWarehouses();
        }

        public static java.util.List<Warehouse> getWarehouses() {
            return java.util.Collections.unmodifiableList(warehouses);
        }

        public static Warehouse addWarehouse(String name) {

            String cleanName = name == null ? "" : name.trim();

            if (cleanName.isEmpty()) {
                throw new IllegalArgumentException("Warehouse name cannot be empty.");
            }

            // Case-insensitive duplicate check against existing warehouses
            // (trimmed both sides) so "Main Warehouse" and "main warehouse "
            // are treated as the same warehouse. This only prevents
            // creating NEW duplicates — any duplicates already sitting in
            // an old database are left alone, not silently merged/deleted.
            for (Warehouse existing : warehouses) {
                if (existing.getName() != null
                        && existing.getName().trim().equalsIgnoreCase(cleanName)) {
                    throw new IllegalArgumentException(
                            "A warehouse named \"" + existing.getName().trim() + "\" already exists."
                    );
                }
            }

            counter++;
            String id = String.format("WH-%03d", counter);
            Warehouse w = new Warehouse(id, cleanName);
            warehouses.add(w);
            PersistenceRepository.saveWarehouses();
            // saveWarehouses() itself no longer wipes other warehouses'
            // product_warehouse_stock rows (it upserts instead of
            // delete-all-and-reinsert — see saveWarehousesBody()). This
            // extra resave is defense in depth for consistency, matching
            // removeWarehouse() below.
            for (Product p : ProductManager.getInstance().getAllProducts()) PersistenceRepository.saveProduct(p);
            return w;
        }

        public static boolean removeWarehouse(String id) {

            if (DEFAULT_WAREHOUSE_ID.equals(id)) {
                return false;
            }

            Warehouse toRemove = findById(id);

            if (toRemove == null) {
                return false;
            }

            // Fold this warehouse's stock back into the Main
            // Warehouse for every product, so totals never change.
            for (Product p : ProductManager.getInstance().getAllProducts()) {
                p.mergeWarehouseIntoDefault(id);
            }

            warehouses.remove(toRemove);
            PersistenceRepository.saveWarehouses();
            for (Product p : ProductManager.getInstance().getAllProducts()) PersistenceRepository.saveProduct(p);
            return true;
        }

        public static Warehouse findById(String id) {

            if (id == null) {
                return null;
            }

            for (Warehouse w : warehouses) {
                if (w.getId().equals(id)) {
                    return w;
                }
            }

            return null;
        }
    }
}
package Clothes_system;

import Clothes_system.db.PersistenceRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;

public class CustomersPanel extends JPanel {

    private JTable customersTable;
    private JTextField searchField;
    private TableRowSorter<DefaultTableModel> sorter;
    private static DefaultTableModel model;

    private JLabel totalCustomersLabel;
    private JLabel activeCustomersLabel;
    private JLabel newThisMonthLabel;
    private JLabel totalPurchasesLabel;

    // =========================================================
    // SHARED CUSTOMER TABLE MODEL
    // Used by OrdersPanel / DashboardPanel to keep customers
    // linked with real orders instead of separate mock data.
    // =========================================================

    public static DefaultTableModel getCustomerModel() { return model; }

    public static boolean findCustomer(String query) {
        if (model == null || query == null || query.trim().isEmpty()) return false;
        String q=query.trim().toLowerCase();
        for(int r=0;r<model.getRowCount();r++) for(int c=0;c<3;c++) if(String.valueOf(model.getValueAt(r,c)).toLowerCase().contains(q)) return true;
        return false;
    }

    public void setSearchQuery(String query) { if(searchField!=null) searchField.setText(query==null?"":query.trim()); }

    public CustomersPanel() {

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
            new JLabel("Customers");

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
                "Manage customer profiles and purchase history"
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
        // NEW CUSTOMER BUTTON
        // =========================

        JButton newCustomerButton =
            new JButton("+  New Customer");

        newCustomerButton.setFocusPainted(false);

        newCustomerButton.setForeground(
            Color.WHITE
        );

        newCustomerButton.setBackground(
            new Color(25, 105, 100)
        );

        newCustomerButton.setFont(
            new Font(
                "Arial",
                Font.BOLD,
                13
            )
        );

        newCustomerButton.setBorder(
            BorderFactory.createEmptyBorder(
                12, 20, 12, 20
            )
        );

        newCustomerButton.setCursor(
            new Cursor(
                Cursor.HAND_CURSOR
            )
        );

        newCustomerButton.addActionListener(e -> showCustomerDialog(-1));

        header.add(
            newCustomerButton,
            BorderLayout.EAST
        );

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

        JPanel totalCustomersCard =
            createSummaryCard(
                "Total Customers",
                "0",
                new Color(25, 115, 105)
            );
        totalCustomersLabel = (JLabel) totalCustomersCard.getClientProperty("valueLabel");
        summaryPanel.add(totalCustomersCard);

        JPanel activeCustomersCard =
            createSummaryCard(
                "Active Customers",
                "0",
                new Color(65, 120, 180)
            );
        activeCustomersLabel = (JLabel) activeCustomersCard.getClientProperty("valueLabel");
        summaryPanel.add(activeCustomersCard);

        JPanel newThisMonthCard =
            createSummaryCard(
                "New Customers (1 order)",
                "0",
                new Color(210, 145, 45)
            );
        newThisMonthLabel = (JLabel) newThisMonthCard.getClientProperty("valueLabel");
        summaryPanel.add(newThisMonthCard);

        JPanel totalPurchasesCard =
            createSummaryCard(
                "Total Purchases",
                "EGP 0",
                new Color(55, 130, 85)
            );
        totalPurchasesLabel = (JLabel) totalPurchasesCard.getClientProperty("valueLabel");
        summaryPanel.add(totalPurchasesCard);

        summaryPanel.setPreferredSize(
            new Dimension(
                0, 105
            )
        );

        content.add(
            summaryPanel,
            BorderLayout.NORTH
        );

        // =========================
        // SEARCH
        // =========================

        JPanel searchPanel =
            new JPanel(
                new BorderLayout()
            );

        searchPanel.setBackground(
            Color.WHITE
        );

        searchPanel.setBorder(
            BorderFactory.createEmptyBorder(
                18, 15, 15, 15
            )
        );

        JLabel searchLabel =
            new JLabel(
                "Search Customers"
            );

        searchLabel.setFont(
            new Font(
                "Arial",
                Font.BOLD,
                12
            )
        );

        searchLabel.setForeground(
            new Color(70, 85, 85)
        );

        searchField =
            new JTextField();

        searchField.setFont(
            new Font(
                "Arial",
                Font.PLAIN,
                13
            )
        );

        searchField.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    new Color(220, 230, 230)
                ),
                BorderFactory.createEmptyBorder(
                    8, 12, 8, 12
                )
            )
        );

        searchField.setToolTipText(
            "Search by name, phone or customer ID"
        );

        searchPanel.add(
            searchLabel,
            BorderLayout.WEST
        );

        searchPanel.add(
            Box.createHorizontalStrut(15),
            BorderLayout.CENTER
        );

        JPanel searchBox =
            new JPanel(
                new BorderLayout()
            );

        searchBox.setBackground(
            Color.WHITE
        );

        searchBox.add(
            searchField,
            BorderLayout.CENTER
        );

        searchPanel.add(
            searchBox,
            BorderLayout.EAST
        );

        // =========================
        // TABLE DATA
        // =========================

        String[] columns = {
            "Customer ID", "Customer Name", "Phone", "Orders",
            "Total Spent", "Last Order", "Status"
        };

        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        PersistenceRepository.loadCustomers(model);
        if (model.getRowCount() == 0 && PersistenceRepository.isInitialSeedPending()) {
            seedDemoCustomers();
            PersistenceRepository.saveCustomers(model);
        }

        customersTable =
            new JTable(model);

        model.addTableModelListener(e -> refreshSummary());
        refreshSummary();

        customersTable.setRowHeight(50);

        customersTable.setFont(
            new Font(
                "Arial",
                Font.PLAIN,
                12
            )
        );

        customersTable.setForeground(
            new Color(50, 65, 65)
        );

        customersTable.setGridColor(
            new Color(235, 240, 240)
        );

        customersTable.setSelectionBackground(
            new Color(230, 243, 241)
        );

        customersTable.setSelectionForeground(
            new Color(25, 80, 75)
        );

        // =========================
        // TABLE HEADER
        // =========================

        customersTable
            .getTableHeader()
            .setFont(
                new Font(
                    "Arial",
                    Font.BOLD,
                    12
                )
            );

        customersTable
            .getTableHeader()
            .setForeground(
                new Color(70, 85, 85)
            );

        customersTable
            .getTableHeader()
            .setBackground(
                new Color(245, 248, 248)
            );

        customersTable
            .getTableHeader()
            .setPreferredSize(
                new Dimension(
                    0, 42
                )
            );

        // =========================
        // STATUS
        // =========================

        customersTable
            .getColumnModel()
            .getColumn(6)
            .setCellRenderer(
                new StatusRenderer()
            );

        // =========================
        // SEARCH FUNCTION
        // =========================

        TableRowSorter<DefaultTableModel> sorter =
            new TableRowSorter<>(
                model
            );

        customersTable.setRowSorter(
            sorter
        );

        JPopupMenu customerMenu = new JPopupMenu();
        JMenuItem editCustomer = new JMenuItem("Edit Customer");
        JMenuItem deleteCustomer = new JMenuItem("Delete Customer");
        customerMenu.add(editCustomer);
        customerMenu.add(deleteCustomer);
        customersTable.setComponentPopupMenu(customerMenu);
        java.awt.event.MouseAdapter customerMouse = new java.awt.event.MouseAdapter() {
            private void select(java.awt.event.MouseEvent e) {
                int r = customersTable.rowAtPoint(e.getPoint());
                if (r >= 0) customersTable.setRowSelectionInterval(r, r);
            }
            public void mousePressed(java.awt.event.MouseEvent e){ if(e.isPopupTrigger()) select(e); }
            public void mouseReleased(java.awt.event.MouseEvent e){ if(e.isPopupTrigger()) select(e); }
        };
        customersTable.addMouseListener(customerMouse);
        editCustomer.addActionListener(e -> {
            int view = customersTable.getSelectedRow();
            if(view >= 0) showCustomerDialog(customersTable.convertRowIndexToModel(view));
        });
        deleteCustomer.addActionListener(e -> {
            int view = customersTable.getSelectedRow();
            if(view < 0) return;
            int row = customersTable.convertRowIndexToModel(view);
            String id = String.valueOf(model.getValueAt(row,0));
            int ok = JOptionPane.showConfirmDialog(this,
                    "Delete customer " + id + "? Historical orders will remain unchanged.",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if(ok == JOptionPane.YES_OPTION){ model.removeRow(row); refreshSummary(); PersistenceRepository.saveCustomers(model); Clothes_system.refreshAllDataViews(); }
        });

        searchField
            .getDocument()
            .addDocumentListener(
                new javax.swing.event.DocumentListener() {

                    private void filter() {

                        String text =
                            searchField
                                .getText()
                                .trim();

                        if (text.length() == 0) {

                            sorter.setRowFilter(
                                null
                            );

                        } else {

                            sorter.setRowFilter(
                                RowFilter.regexFilter(
                                    "(?i)" + text
                                )
                            );
                        }
                    }

                    @Override
                    public void insertUpdate(
                        javax.swing.event.DocumentEvent e
                    ) {
                        filter();
                    }

                    @Override
                    public void removeUpdate(
                        javax.swing.event.DocumentEvent e
                    ) {
                        filter();
                    }

                    @Override
                    public void changedUpdate(
                        javax.swing.event.DocumentEvent e
                    ) {
                        filter();
                    }
                }
            );

        // =========================
        // SCROLL
        // =========================

        JScrollPane scrollPane =
            new JScrollPane(
                customersTable
            );

        scrollPane.setBorder(
            BorderFactory.createLineBorder(
                new Color(225, 232, 232)
            )
        );

        // =========================
        // TABLE CONTAINER
        // =========================

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
            searchPanel,
            BorderLayout.NORTH
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
    }

    private void showCustomerDialog(int row) {
        boolean edit = row >= 0;
        JTextField name = new JTextField(edit ? String.valueOf(model.getValueAt(row,1)) : "");
        JTextField phone = new JTextField(edit ? String.valueOf(model.getValueAt(row,2)) : "");
        JPanel p = new JPanel(new GridLayout(0,2,8,8));
        p.add(new JLabel("Name")); p.add(name); p.add(new JLabel("Phone")); p.add(phone);
        int result = JOptionPane.showConfirmDialog(this,p,edit?"Edit Customer":"New Customer",
                JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        if(result != JOptionPane.OK_OPTION) return;
        String n=name.getText().trim(), ph=phone.getText().trim();
        if(n.isEmpty() || ph.isEmpty() || !ph.matches("[0-9+ -]{7,20}")){
            JOptionPane.showMessageDialog(this,"Enter a valid name and phone.","Invalid Customer",JOptionPane.ERROR_MESSAGE); return;
        }
        int duplicate=findRowByPhone(ph);
        if(duplicate>=0 && duplicate!=row){
            JOptionPane.showMessageDialog(this,"A customer with this phone already exists.","Duplicate Customer",JOptionPane.ERROR_MESSAGE); return;
        }
        if(edit){
            model.setValueAt(n,row,1); model.setValueAt(ph,row,2);
        } else {
            model.addRow(new Object[]{generateCustomerId(),n,ph,0,"EGP 0","-", "Active"});
        }
        refreshSummary(); Clothes_system.refreshAllDataViews();
    }

    private void seedDemoCustomers() {
        Object[][] seed = {
            {"CUS-001","Ahmed Hassan","01012345678",8,"EGP 12,450","16 Sep 2026","Active"},
            {"CUS-002","Mohamed Ali","01123456789",5,"EGP 7,820","16 Sep 2026","Active"},
            {"CUS-003","Omar Khaled","01234567890",3,"EGP 4,320","15 Sep 2026","Active"},
            {"CUS-004","Youssef Ahmed","01098765432",1,"EGP 950","15 Sep 2026","Inactive"},
            {"CUS-005","Mahmoud Samir","01555555555",6,"EGP 9,180","14 Sep 2026","Active"},
            {"CUS-006","Karim Mostafa","01111111111",2,"EGP 2,560","14 Sep 2026","Active"},
            {"CUS-007","Amr Hassan","01022222222",9,"EGP 15,240","13 Sep 2026","Active"},
            {"CUS-008","Ali Mohamed","01233333333",4,"EGP 6,350","12 Sep 2026","Active"}
        };
        for (Object[] row : seed) model.addRow(row);
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
                22
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
    // REFRESH SUMMARY CARDS FROM REAL TABLE DATA
    // =========================================================

    private void refreshSummary() {

        if (model == null) {
            return;
        }

        int total = model.getRowCount();
        int active = 0;
        int newCustomers = 0;
        double totalPurchases = 0;

        for (int row = 0; row < total; row++) {

            String status = String.valueOf(model.getValueAt(row, 6));
            if ("Active".equalsIgnoreCase(status)) {
                active++;
            }

            int orders = parseIntSafe(model.getValueAt(row, 3));
            if (orders <= 1) {
                newCustomers++;
            }

            totalPurchases += parseMoneySafe(model.getValueAt(row, 4));
        }

        if (totalCustomersLabel != null) totalCustomersLabel.setText(String.valueOf(total));
        if (activeCustomersLabel != null) activeCustomersLabel.setText(String.valueOf(active));
        if (newThisMonthLabel != null) newThisMonthLabel.setText(String.valueOf(newCustomers));
        if (totalPurchasesLabel != null) totalPurchasesLabel.setText(String.format(java.util.Locale.US, "EGP %,.0f", totalPurchases));
    }

    private static int parseIntSafe(Object v) {
        try {
            return Integer.parseInt(String.valueOf(v).replace(",", "").trim());
        } catch (Exception e) {
            return 0;
        }
    }

    // Authoritative money formatter for the "total spent" column, which
    // doubles as storage (parseMoneySafe reads it straight back for the
    // next add/subtract). Rounding to whole EGP here — like the old
    // "EGP %,.0f" everywhere below used to do — would silently drift
    // the stored figure every time an order/return touched it. Whole
    // amounts still display exactly as before ("EGP 300").
    private static String money(double amount) {
        if (Math.abs(amount - Math.round(amount)) < 0.005) {
            return String.format(java.util.Locale.US, "EGP %,.0f", (double) Math.round(amount));
        }
        return String.format(java.util.Locale.US, "EGP %,.2f", amount);
    }

    private static double parseMoneySafe(Object v) {
        try {
            return Double.parseDouble(
                    String.valueOf(v)
                            .replace("EGP", "")
                            .replace(",", "")
                            .trim()
            );
        } catch (Exception e) {
            return 0;
        }
    }

    // =========================================================
    // LINK ORDERS -> CUSTOMERS
    // Called by OrdersPanel whenever a new order is created,
    // so that customer records (orders count, total spent,
    // last order date, status) always reflect real orders
    // instead of staying static/disconnected.
    // =========================================================

    public static synchronized void recordOrderForCustomer(
            String customerName,
            String phone,
            double orderAmount,
            String dateText
    ) {

        if (model == null) {
            // Customers panel has not been built yet
            // (should not normally happen since all panels
            // are created at startup, but this keeps the
            // link crash-proof either way).
            return;
        }

        if (customerName == null || customerName.trim().isEmpty()) {
            return;
        }

        String cleanPhone = phone == null ? "" : phone.trim();
        String cleanName = customerName.trim();

        int foundRow = findRowByPhone(cleanPhone);

        if (foundRow >= 0) {

            int orders = parseIntSafe(model.getValueAt(foundRow, 3)) + 1;
            double spent = parseMoneySafe(model.getValueAt(foundRow, 4)) + orderAmount;

            model.setValueAt(orders, foundRow, 3);
            model.setValueAt(money(spent), foundRow, 4);
            model.setValueAt(dateText, foundRow, 5);
            model.setValueAt("Active", foundRow, 6);

        } else {

            String newId = generateCustomerId();

            model.addRow(new Object[]{
                    newId,
                    cleanName,
                    cleanPhone,
                    1,
                    money(orderAmount),
                    dateText,
                    "Active"
            });
        }
        PersistenceRepository.saveCustomers(model);
    }

    public static synchronized void recordReturnForCustomer(String phone, double refundAmount) {
        if (model == null || refundAmount <= 0) return;
        int row = findRowByPhone(phone);
        if (row < 0) return;
        double spent = Math.max(0, parseMoneySafe(model.getValueAt(row, 4)) - refundAmount);
        model.setValueAt(money(spent), row, 4);
        PersistenceRepository.saveCustomers(model);
    }

    private static int findRowByPhone(String phone) {

        if (phone == null || phone.trim().isEmpty() || model == null) {
            return -1;
        }

        String clean = phone.trim();

        for (int row = 0; row < model.getRowCount(); row++) {

            String existingPhone = String.valueOf(model.getValueAt(row, 2)).trim();

            if (existingPhone.equalsIgnoreCase(clean)) {
                return row;
            }
        }

        return -1;
    }

    // =========================================================
    // Called by OrdersPanel when an EXISTING order is edited,
    // so the customer's total spent stays correct without
    // double-counting the order.
    // =========================================================

    public static synchronized void adjustOrderAmountForCustomer(
            String oldPhone,
            double oldAmount,
            String newName,
            String newPhone,
            double newAmount,
            String dateText
    ) {

        if (model == null) {
            return;
        }

        int oldRow = findRowByPhone(oldPhone);

        if (oldRow >= 0) {

            double spent = parseMoneySafe(model.getValueAt(oldRow, 4)) - oldAmount;

            if (spent < 0) {
                spent = 0;
            }

            model.setValueAt(
                    money(spent),
                    oldRow,
                    4
            );

            // Keep total_orders in sync with total_spent: the order that
            // used to belong to this customer is being removed from them
            // (either reassigned to another customer, or replaced by a
            // freshly-created customer row below), so decrement by
            // exactly one order. Never let it go negative.
            int oldOrders = parseIntSafe(model.getValueAt(oldRow, 3)) - 1;
            if (oldOrders < 0) {
                oldOrders = 0;
            }
            model.setValueAt(oldOrders, oldRow, 3);
        }

        if (newName == null || newName.trim().isEmpty()
                || newPhone == null || newPhone.trim().isEmpty()) {
            return;
        }

        int newRow = findRowByPhone(newPhone);

        if (newRow >= 0) {

            double spent = parseMoneySafe(model.getValueAt(newRow, 4)) + newAmount;

            model.setValueAt(
                    money(spent),
                    newRow,
                    4
            );

            // Mirror the decrement above: this order now belongs to
            // this customer, so their order count goes up by exactly one
            // (this also correctly nets to "no change" when oldRow and
            // newRow are the same customer, i.e. a normal edit that does
            // not reassign the order to someone else).
            int newOrders = parseIntSafe(model.getValueAt(newRow, 3)) + 1;
            model.setValueAt(newOrders, newRow, 3);

            model.setValueAt(dateText, newRow, 5);

        } else {

            String newId = generateCustomerId();

            model.addRow(new Object[]{
                    newId,
                    newName.trim(),
                    newPhone.trim(),
                    1,
                    money(newAmount),
                    dateText,
                    "Active"
            });
        }
    }

    private static String generateCustomerId() {

        int max = 0;

        for (int row = 0; row < model.getRowCount(); row++) {

            String id = String.valueOf(model.getValueAt(row, 0));

            try {
                int n = Integer.parseInt(id.replace("CUS-", "").trim());
                if (n > max) max = n;
            } catch (Exception ignored) {
            }
        }

        return String.format("CUS-%03d", max + 1);
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

            if (
                status.equals("Active")
            ) {

                setForeground(
                    new Color(
                        45, 125, 75
                    )
                );

                setBackground(
                    new Color(
                        225, 242, 230
                    )
                );

            } else {

                setForeground(
                    new Color(
                        175, 60, 60
                    )
                );

                setBackground(
                    new Color(
                        250, 225, 225
                    )
                );
            }

            return this;
        }
    }
}
package Clothes_system;

import Clothes_system.db.PersistenceRepository;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class OrdersPanel extends JPanel {

    // =========================================================
    // PRODUCTS MODEL COLUMN MAP
    // Must match ProductsPanel's shared 12-column model
    // =========================================================

    private static final int PRODUCT_ID_COLUMN = 1;
    private static final int PRODUCT_SKU_COLUMN = 2;
    private static final int PRODUCT_NAME_COLUMN = 3;
    private static final int PRODUCT_CATEGORY_COLUMN = 4;
    private static final int PRODUCT_SIZE_COLUMN = 5;
    private static final int PRODUCT_COLOR_COLUMN = 6;
    private static final int PRODUCT_PRICE_COLUMN = 7;
    private static final int PRODUCT_STOCK_COLUMN = 9;
    private static final int PRODUCT_STATUS_COLUMN = 10;

    // =========================================================
    // COLORS
    // =========================================================

    private final Color BG = new Color(248, 250, 249);
    private final Color WHITE = Color.WHITE;
    private final Color TEXT = new Color(30, 45, 45);
    private final Color MUTED = new Color(135, 150, 150);
    private final Color BORDER = new Color(225, 232, 232);
    private final Color GREEN = new Color(25, 115, 105);

    // =========================================================
    // TABLE
    // =========================================================

    private JTable ordersTable;
    private DefaultTableModel ordersModel;
    private TableRowSorter<DefaultTableModel> sorter;

    // =========================================================
    // SUMMARY
    // =========================================================

    private JLabel totalOrdersLabel;
    private JLabel totalRevenueLabel;
    private JLabel completedOrdersLabel;
    private JLabel deliveryOrdersLabel;
    private JLabel cancelledOrdersLabel;

    // =========================================================
    // FILTERS
    // =========================================================

    private JTextField searchField;
    private final Set<String> selectedOrderIds = new LinkedHashSet<>();
    private JCheckBox selectAllCheckBox;
    private JButton printSelectedButton;

    private JComboBox<String> statusFilter;
    private JComboBox<String> deliveryFilter;

    private JTextField minPriceField;
    private JTextField maxPriceField;

    private JTextField dateFromField;
    private JTextField dateToField;

    private JTextField minProductsField;
    private JTextField maxProductsField;

    private JComboBox<String> sortByBox;
    private JComboBox<String> sortDirectionBox;

    // =========================================================
    // SHARED ORDERS
    // =========================================================

    static final List<Order> orders = new ArrayList<>();

    private static boolean demoOrdersCreated = false;

    private final SimpleDateFormat displayDateFormat =
            new SimpleDateFormat(
                    "dd MMM yyyy HH:mm",
                    Locale.ENGLISH
            );

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public OrdersPanel() {

        setBackground(BG);
        setLayout(new BorderLayout());

        JPanel top = new JPanel();
        top.setBackground(BG);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        top.add(createHeader());
        top.add(createSummaryCards());

        add(top, BorderLayout.NORTH);
        add(createOrdersContent(), BorderLayout.CENTER);

        loadFromPersistence();
        if (orders.isEmpty() && PersistenceRepository.isInitialSeedPending()) {
            createDemoOrders();
            for (Order o : orders) PersistenceRepository.saveOrder(o);
        }

        SwingUtilities.invokeLater(() -> {

            refreshOrdersTable();
            updateSummary();
            applySort();
            applyFilters();

        });
    }

    // =========================================================
    // HEADER
    // =========================================================

    private JPanel createHeader() {

        JPanel panel = new JPanel(new BorderLayout());

        panel.setBackground(BG);

        panel.setBorder(
                new EmptyBorder(
                        25,
                        28,
                        18,
                        28
                )
        );

        JPanel textPanel = new JPanel();

        textPanel.setOpaque(false);

        textPanel.setLayout(
                new BoxLayout(
                        textPanel,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel title = new JLabel("Orders");

        title.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        26
                )
        );

        title.setForeground(TEXT);

        JLabel subtitle =
                new JLabel(
                        "Manage orders, preparation and delivery"
                );

        subtitle.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        13
                )
        );

        subtitle.setForeground(MUTED);

        textPanel.add(title);

        textPanel.add(
                Box.createVerticalStrut(5)
        );

        textPanel.add(subtitle);

        panel.add(
                textPanel,
                BorderLayout.WEST
        );

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerActions.setOpaque(false);

        selectAllCheckBox = new JCheckBox("Select All");
        selectAllCheckBox.setOpaque(false);
        selectAllCheckBox.setForeground(TEXT);
        selectAllCheckBox.setFocusPainted(false);
        selectAllCheckBox.addActionListener(e -> selectAllVisibleOrders(selectAllCheckBox.isSelected()));

        printSelectedButton = createResetButton("PRINT");
        printSelectedButton.addActionListener(e -> printCurrentOrderSelection());

        JButton moreActions = createResetButton("MORE ACTIONS");
        moreActions.addActionListener(e -> showMoreActionsMenu(moreActions));
        JButton newOrderButton = createPrimaryButton("+ New Order");
        newOrderButton.addActionListener(e -> showNewOrderDialog());

        headerActions.add(selectAllCheckBox);
        headerActions.add(printSelectedButton);
        headerActions.add(moreActions);
        headerActions.add(newOrderButton);
        panel.add(headerActions, BorderLayout.EAST);

        return panel;
    }

    // =========================================================
    // SUMMARY
    // =========================================================

    private JPanel createSummaryCards() {

        JPanel panel =
                new JPanel(
                        new GridLayout(
                                1,
                                5,
                                12,
                                0
                        )
                );

        panel.setBackground(BG);

        panel.setBorder(
                new EmptyBorder(
                        0,
                        28,
                        20,
                        28
                )
        );

        totalOrdersLabel = new JLabel("0");
        totalRevenueLabel = new JLabel("EGP 0");
        completedOrdersLabel = new JLabel("0");
        deliveryOrdersLabel = new JLabel("0");
        cancelledOrdersLabel = new JLabel("0");

        panel.add(
                createStatCard(
                        "TOTAL ORDERS",
                        totalOrdersLabel
                )
        );

        panel.add(
                createStatCard(
                        "TOTAL REVENUE",
                        totalRevenueLabel
                )
        );

        panel.add(
                createStatCard(
                        "COMPLETED ORDERS",
                        completedOrdersLabel
                )
        );

        panel.add(
                createStatCard(
                        "IN DELIVERY",
                        deliveryOrdersLabel
                )
        );

        panel.add(
                createStatCard(
                        "CANCELLED ORDERS",
                        cancelledOrdersLabel
                )
        );

        return panel;
    }

    private JPanel createStatCard(
            String title,
            JLabel value
    ) {

        JPanel card =
                new JPanel(
                        new BorderLayout()
                );

        card.setBackground(WHITE);

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER
                        ),
                        BorderFactory.createEmptyBorder(
                                15,
                                16,
                                15,
                                16
                        )
                )
        );

        JLabel titleLabel =
                new JLabel(title);

        titleLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        10
                )
        );

        titleLabel.setForeground(MUTED);

        value.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        21
                )
        );

        value.setForeground(TEXT);

        card.add(
                titleLabel,
                BorderLayout.NORTH
        );

        card.add(
                value,
                BorderLayout.CENTER
        );

        return card;
    }

    // =========================================================
    // CONTENT
    // =========================================================

    private JPanel createOrdersContent() {

        JPanel panel =
                new JPanel(
                        new BorderLayout(
                                0,
                                12
                        )
                );

        panel.setBackground(BG);

        panel.setBorder(
                new EmptyBorder(
                        0,
                        28,
                        25,
                        28
                )
        );

        panel.add(
                createFilters(),
                BorderLayout.NORTH
        );

        panel.add(
                createOrdersTable(),
                BorderLayout.CENTER
        );

        return panel;
    }

    // =========================================================
    // FILTERS
    // =========================================================

    private JPanel createFilters() {

        JPanel outer = new JPanel();

        outer.setBackground(WHITE);

        outer.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER
                        ),
                        BorderFactory.createEmptyBorder(
                                14,
                                15,
                                14,
                                15
                        )
                )
        );

        outer.setLayout(
                new BoxLayout(
                        outer,
                        BoxLayout.Y_AXIS
                )
        );

        JPanel row1 =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                8,
                                0
                        )
                );

        row1.setBackground(WHITE);

        searchField =
                createFilterInput(
                        "Search order, customer or product..."
                );

        searchField.setPreferredSize(
                new Dimension(
                        280,
                        34
                )
        );

        addLabeledFilter(
                row1,
                "SEARCH",
                searchField
        );

        sortByBox =
                new JComboBox<>(
                        new String[]{
                                "Date",
                                "Price",
                                "Customer",
                                "Order ID",
                                "Products",
                                "Order Status",
                                "Delivery Status",
                                "Select"
                        }
                );

        sortByBox.setPreferredSize(
                new Dimension(
                        145,
                        34
                )
        );

        addLabeledFilter(
                row1,
                "SORT BY",
                sortByBox
        );

        sortDirectionBox =
                new JComboBox<>();

        sortDirectionBox.setPreferredSize(
                new Dimension(
                        155,
                        34
                )
        );

        addLabeledFilter(
                row1,
                "DIRECTION",
                sortDirectionBox
        );

        sortByBox.addActionListener(
                e -> updateSortDirection()
        );

        updateSortDirection();

        outer.add(row1);

        outer.add(
                Box.createVerticalStrut(10)
        );

        JPanel row2 =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                8,
                                0
                        )
                );

        row2.setBackground(WHITE);

        statusFilter =
                new JComboBox<>(
                        new String[]{
                                "All Order Status",
                                "Not Prepared",
                                "Preparing",
                                "Prepared",
                                "Completed"
                        }
                );

        deliveryFilter =
                new JComboBox<>(
                        new String[]{
                                "All Delivery Status",
                                "With Shipping Company",
                                "Out for Delivery",
                                "Delivered",
                                "Partially Returned",
                                "Returned"
                        }
                );

        minPriceField =
                createFilterInput("Min price");

        maxPriceField =
                createFilterInput("Max price");

        dateFromField =
                createFilterInput("From date");

        dateToField =
                createFilterInput("To date");

        minProductsField =
                createFilterInput("Min products");

        maxProductsField =
                createFilterInput("Max products");

        statusFilter.setPreferredSize(
                new Dimension(155, 34)
        );

        deliveryFilter.setPreferredSize(
                new Dimension(190, 34)
        );

        minPriceField.setPreferredSize(
                new Dimension(100, 34)
        );

        maxPriceField.setPreferredSize(
                new Dimension(100, 34)
        );

        dateFromField.setPreferredSize(
                new Dimension(105, 34)
        );

        dateToField.setPreferredSize(
                new Dimension(105, 34)
        );

        minProductsField.setPreferredSize(
                new Dimension(105, 34)
        );

        maxProductsField.setPreferredSize(
                new Dimension(105, 34)
        );

        addLabeledFilter(
                row2,
                "ORDER STATUS",
                statusFilter
        );

        addLabeledFilter(
                row2,
                "DELIVERY",
                deliveryFilter
        );

        addLabeledFilter(
                row2,
                "MIN PRICE",
                minPriceField
        );

        addLabeledFilter(
                row2,
                "MAX PRICE",
                maxPriceField
        );

        addLabeledFilter(
                row2,
                "FROM",
                dateFromField
        );

        addLabeledFilter(
                row2,
                "TO",
                dateToField
        );

        addLabeledFilter(
                row2,
                "MIN PRODUCTS",
                minProductsField
        );

        addLabeledFilter(
                row2,
                "MAX PRODUCTS",
                maxProductsField
        );

        JButton applyButton =
                createPrimaryButton(
                        "Apply Filters"
                );

        applyButton.addActionListener(
                e -> applyFilters()
        );

        JButton resetButton =
                createResetButton(
                        "Reset"
                );

        resetButton.addActionListener(
                e -> resetFilters()
        );

        row2.add(applyButton);
        row2.add(resetButton);

        outer.add(row2);

        DocumentListener listener =
                new DocumentListener() {

                    @Override
                    public void insertUpdate(
                            DocumentEvent e
                    ) {
                        applyFilters();
                    }

                    @Override
                    public void removeUpdate(
                            DocumentEvent e
                    ) {
                        applyFilters();
                    }

                    @Override
                    public void changedUpdate(
                            DocumentEvent e
                    ) {
                        applyFilters();
                    }
                };

        searchField
                .getDocument()
                .addDocumentListener(listener);

        return outer;
    }

    private void addLabeledFilter(
            JPanel parent,
            String labelText,
            Component component
    ) {

        JPanel box =
                new JPanel(
                        new BorderLayout(
                                0,
                                3
                        )
                );

        box.setBackground(WHITE);

        JLabel label =
                new JLabel(labelText);

        label.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        9
                )
        );

        label.setForeground(MUTED);

        box.add(
                label,
                BorderLayout.NORTH
        );

        box.add(
                component,
                BorderLayout.CENTER
        );

        parent.add(box);
    }

    private JTextField createFilterInput(
            String placeholder
    ) {

        JTextField field =
                new JTextField();

        field.setToolTipText(placeholder);

        field.putClientProperty(
                "JTextField.placeholderText",
                placeholder
        );

        field.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        12
                )
        );

        field.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER
                        ),
                        BorderFactory.createEmptyBorder(
                                0,
                                8,
                                0,
                                8
                        )
                )
        );

        return field;
    }

    // =========================================================
    // TABLE
    // =========================================================

    private JScrollPane createOrdersTable() {

        ordersModel =
                new DefaultTableModel(
                        new Object[]{
                                "Order ID",
                                "Customer",
                                "Phone",
                                "Date",
                                "Products",
                                "Total",
                                "Order Status",
                                "Delivery Status",
                                "Select"
                        },
                        0
                ) {

                    @Override
                    public boolean isCellEditable(
                            int row,
                            int column
                    ) {
                        return column == 8;
                    }

                    @Override
                    public Class<?> getColumnClass(
                            int column
                    ) {

                        if (column == 5) return Double.class;
                        if (column == 8) return Boolean.class;
                        return String.class;
                    }
                };

        ordersTable =
                new JTable(ordersModel);

        sorter =
                new TableRowSorter<>(
                        ordersModel
                );

        // The Date column holds a formatted string ("10 Jan 2026 10:00"),
        // so the default string comparator ordered rows alphabetically by
        // month name (Feb < Jan < Mar) instead of chronologically. Compare
        // the parsed instants instead. This also fixes the order in which
        // batches of invoices are printed, because the print dataset
        // follows the displayed row order.
        sorter.setComparator(3, (Comparator<Object>) (a, b) -> {

            Date left = parseDisplayDateOrNull(a);
            Date right = parseDisplayDateOrNull(b);

            if (left == null && right == null) {
                return String.valueOf(a).compareTo(String.valueOf(b));
            }

            if (left == null) {
                return -1;
            }

            if (right == null) {
                return 1;
            }

            return left.compareTo(right);
        });

        ordersTable.setRowSorter(sorter);

        ordersTable.setRowHeight(45);

        ordersTable.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        12
                )
        );

        ordersTable.setForeground(TEXT);
        ordersTable.setBackground(WHITE);

        ordersTable.setGridColor(
                new Color(
                        235,
                        240,
                        240
                )
        );

        ordersTable.setSelectionBackground(
                new Color(
                        225,
                        241,
                        238
                )
        );

        ordersTable.setSelectionForeground(TEXT);

        ordersTable.getTableHeader()
                .setFont(
                        new Font(
                                "Arial",
                                Font.BOLD,
                                11
                        )
                );

        ordersTable.getTableHeader()
                .setForeground(TEXT);

        ordersTable.getTableHeader()
                .setBackground(
                        new Color(
                                242,
                                246,
                                245
                        )
                );

        ordersTable.getColumnModel()
                .getColumn(5)
                .setCellRenderer(
                        new MoneyRenderer()
                );

        ordersTable.getColumnModel()
                .getColumn(6)
                .setCellRenderer(
                        new StatusRenderer()
                );

        ordersTable.getColumnModel()
                .getColumn(7)
                .setCellRenderer(
                        new DeliveryRenderer()
                );

        ordersTable.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { handleOrderMouse(e); }
            @Override public void mouseReleased(MouseEvent e) { handleOrderMouse(e); }
        });

        ordersTable.getModel().addTableModelListener(e -> {
            if (ordersModel == null || e.getColumn() != 8) return;
            for (int r=0; r<ordersModel.getRowCount(); r++) {
                String id = String.valueOf(ordersModel.getValueAt(r,0));
                Object v = ordersModel.getValueAt(r,8);
                if (Boolean.TRUE.equals(v)) selectedOrderIds.add(id); else selectedOrderIds.remove(id);
            }
            SwingUtilities.invokeLater(this::updateSelectAllState);
        });

        JScrollPane scrollPane =
                new JScrollPane(
                        ordersTable
                );

        scrollPane.setBorder(
                BorderFactory.createLineBorder(
                        BORDER
                )
        );

        scrollPane.getViewport()
                .setBackground(WHITE);

        return scrollPane;
    }

    // =========================================================
    // FILTER LOGIC
    // =========================================================

    private void applyFilters() {

        if (sorter == null
                || searchField == null) {
            return;
        }

        final String search =
                searchField.getText()
                        .trim()
                        .toLowerCase();

        final String selectedStatus =
                String.valueOf(
                        statusFilter.getSelectedItem()
                );

        final String selectedDelivery =
                String.valueOf(
                        deliveryFilter.getSelectedItem()
                );

        final Double minPrice =
                parseOptionalDouble(
                        minPriceField.getText()
                );

        final Double maxPrice =
                parseOptionalDouble(
                        maxPriceField.getText()
                );

        final Date fromDate =
                parseFilterDate(
                        dateFromField.getText(),
                        false
                );

        final Date toDate =
                parseFilterDate(
                        dateToField.getText(),
                        true
                );

        final Integer minProducts =
                parseOptionalInteger(
                        minProductsField.getText()
                );

        final Integer maxProducts =
                parseOptionalInteger(
                        maxProductsField.getText()
                );

        sorter.setRowFilter(
                new RowFilter<DefaultTableModel, Integer>() {

                    @Override
                    public boolean include(
                            Entry<? extends DefaultTableModel, ? extends Integer> entry
                    ) {

                        int row =
                                entry.getIdentifier();

                        if (row < 0
                                || row >= orders.size()) {
                            return false;
                        }

                        Order order =
                                orders.get(row);

                        if (!search.isEmpty()) {

                            StringBuilder text =
                                    new StringBuilder();

                            text.append(order.id).append(" ");
                            text.append(order.customer).append(" ");
                            text.append(order.phone).append(" ");
                            text.append(order.additionalPhone).append(" ");
                            text.append(order.address).append(" ");
                            text.append(order.notes).append(" ");
                            text.append(order.payment).append(" ");
                            text.append(order.orderStatus).append(" ");
                            text.append(getEffectiveDeliveryStatus(order)).append(" ");

                            for (OrderItem item :
                                    order.items) {

                                text.append(item.name).append(" ");
                                text.append(item.code).append(" ");
                                text.append(item.sku).append(" ");
                                text.append(item.category).append(" ");
                                text.append(item.size).append(" ");
                                text.append(item.color).append(" ");
                            }

                            if (!text.toString()
                                    .toLowerCase()
                                    .contains(search)) {

                                return false;
                            }
                        }

                        if (!"All Order Status".equals(
                                selectedStatus
                        )
                                && !selectedStatus.equals(
                                order.orderStatus
                        )) {

                            return false;
                        }

                        if (!"All Delivery Status".equals(
                                selectedDelivery
                        )
                                && !selectedDelivery.equals(
                                getEffectiveDeliveryStatus(order)
                        )) {

                            return false;
                        }

                        double total =
                                parseNumber(
                                        order.total
                                );

                        if (minPrice != null
                                && total < minPrice) {
                            return false;
                        }

                        if (maxPrice != null
                                && total > maxPrice) {
                            return false;
                        }

                        if (fromDate != null
                                && (order.dateValue == null
                                || order.dateValue.before(
                                fromDate
                        ))) {

                            return false;
                        }

                        if (toDate != null
                                && (order.dateValue == null
                                || order.dateValue.after(
                                toDate
                        ))) {

                            return false;
                        }

                        int productCount =
                                order.items.size();

                        if (minProducts != null
                                && productCount < minProducts) {
                            return false;
                        }

                        if (maxProducts != null
                                && productCount > maxProducts) {
                            return false;
                        }

                        return true;
                    }
                }
        );
    }

    // =========================================================
    // SORTING
    // =========================================================

    private void updateSortDirection() {

        if (sortDirectionBox == null) {
            return;
        }

        String selected =
                String.valueOf(
                        sortByBox.getSelectedItem()
                );

        sortDirectionBox.removeAllItems();

        if ("Date".equals(selected)) {

            sortDirectionBox.addItem(
                    "Newest → Oldest"
            );

            sortDirectionBox.addItem(
                    "Oldest → Newest"
            );

        } else if ("Price".equals(selected)) {

            sortDirectionBox.addItem(
                    "Highest → Lowest"
            );

            sortDirectionBox.addItem(
                    "Lowest → Highest"
            );

        } else if ("Products".equals(selected)) {

            sortDirectionBox.addItem(
                    "Most → Least"
            );

            sortDirectionBox.addItem(
                    "Least → Most"
            );

        } else {

            sortDirectionBox.addItem(
                    "A → Z"
            );

            sortDirectionBox.addItem(
                    "Z → A"
            );
        }

        applySort();
    }

    private void applySort() {

        if (sorter == null
                || sortByBox == null
                || sortDirectionBox == null) {
            return;
        }

        String selected =
                String.valueOf(
                        sortByBox.getSelectedItem()
                );

        int column = 3;

        if ("Price".equals(selected)) {
            column = 5;

        } else if ("Customer".equals(selected)) {
            column = 1;

        } else if ("Order ID".equals(selected)) {
            column = 0;

        } else if ("Products".equals(selected)) {
            column = 4;

        } else if ("Order Status".equals(selected)) {
            column = 6;

        } else if ("Delivery Status".equals(selected)) {
            column = 7;
        }

        boolean descending =
                sortDirectionBox.getSelectedIndex() == 0;

        sorter.setSortKeys(
                Collections.singletonList(
                        new RowSorter.SortKey(
                                column,
                                descending
                                        ? SortOrder.DESCENDING
                                        : SortOrder.ASCENDING
                        )
                )
        );
    }

    private void resetFilters() {

        searchField.setText("");

        statusFilter.setSelectedIndex(0);
        deliveryFilter.setSelectedIndex(0);

        minPriceField.setText("");
        maxPriceField.setText("");

        dateFromField.setText("");
        dateToField.setText("");

        minProductsField.setText("");
        maxProductsField.setText("");

        sortByBox.setSelectedIndex(0);

        updateSortDirection();

        applyFilters();
    }

    // =========================================================
    // NEW ORDER
    // =========================================================

    private void showNewOrderDialog() {

        JDialog dialog =
                new JDialog(
                        SwingUtilities.getWindowAncestor(this),
                        "New Order",
                        Dialog.ModalityType.APPLICATION_MODAL
                );

        dialog.setSize(820, 820);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel content = new JPanel();

        content.setBackground(BG);

        content.setBorder(
                new EmptyBorder(
                        20,
                        22,
                        20,
                        22
                )
        );

        content.setLayout(
                new BoxLayout(
                        content,
                        BoxLayout.Y_AXIS
                )
        );

        JPanel customerPanel =
                new JPanel(
                        new GridLayout(
                                3,
                                2,
                                10,
                                10
                        )
                );

        customerPanel.setBackground(BG);

        JTextField customerField = createInput();
        JTextField phoneField = createInput();
        JTextField additionalPhoneField = createInput();
        JTextField addressField = createInput();

        addFormField(
                customerPanel,
                "Customer *",
                customerField
        );

        addFormField(
                customerPanel,
                "Phone *",
                phoneField
        );

        addFormField(
                customerPanel,
                "Additional Phone (Optional)",
                additionalPhoneField
        );

        addFormField(
                customerPanel,
                "Address *",
                addressField
        );

        JButton selectCustomer = new JButton("Select Existing Customer");
        selectCustomer.addActionListener(e -> {
            DefaultTableModel cm = CustomersPanel.getCustomerModel();
            if (cm == null || cm.getRowCount() == 0) {
                JOptionPane.showMessageDialog(dialog, "No customers available.", "Customer", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String[] choices = new String[cm.getRowCount()];
            for (int r = 0; r < cm.getRowCount(); r++)
                choices[r] = String.valueOf(cm.getValueAt(r, 1)) + " | " + String.valueOf(cm.getValueAt(r, 2));
            String selected = (String) JOptionPane.showInputDialog(dialog, "Select Customer:", "Customer",
                    JOptionPane.PLAIN_MESSAGE, null, choices, choices[0]);
            if (selected != null) {
                int sep = selected.lastIndexOf(" | ");
                customerField.setText(sep > 0 ? selected.substring(0, sep) : selected);
                if (sep > 0) phoneField.setText(selected.substring(sep + 3));
            }
        });
        customerPanel.add(selectCustomer);
        customerPanel.add(new JLabel(""));

        content.add(customerPanel);

        content.add(
                Box.createVerticalStrut(10)
        );

        JTextArea notesArea =
                createNotesArea();

        content.add(
                createTextAreaField(
                        "Order Details / Notes (Optional)",
                        notesArea
                )
        );

        content.add(
                Box.createVerticalStrut(15)
        );

        JPanel statusPanel =
                new JPanel(
                        new GridLayout(
                                2,
                                2,
                                10,
                                10
                        )
                );

        statusPanel.setBackground(BG);

        JComboBox<String> paymentBox =
                new JComboBox<>(
                        new String[]{
                                "Cash",
                                "Visa",
                                "Bank Transfer"
                        }
                );

        JComboBox<String> orderStatusBox =
                new JComboBox<>(
                        new String[]{
                                "Not Prepared",
                                "Preparing",
                                "Prepared",
                                "Completed"
                        }
                );

        JComboBox<String> deliveryBox =
                new JComboBox<>(
                        new String[]{
                                "With Shipping Company",
                                "Out for Delivery",
                                "Delivered",
                                "Partially Returned",
                                "Returned"
                        }
                );

        JTextField shippingField = createInput();
        JTextField discountField = createInput();

        shippingField.setText("0");
        discountField.setText("0");

        addFormField(
                statusPanel,
                "Payment",
                paymentBox
        );

        addFormField(
                statusPanel,
                "Order Status",
                orderStatusBox
        );

        addFormField(
                statusPanel,
                "Delivery Status",
                deliveryBox
        );

        addFormField(
                statusPanel,
                "Shipping",
                shippingField
        );

        content.add(statusPanel);

        content.add(
                Box.createVerticalStrut(10)
        );

        JPanel discountPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT
                        )
                );

        discountPanel.setBackground(BG);

        addFormField(
                discountPanel,
                "Discount",
                discountField
        );

        content.add(discountPanel);

        content.add(
                Box.createVerticalStrut(15)
        );

        JLabel productsTitle =
                new JLabel("Products");

        productsTitle.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        14
                )
        );

        productsTitle.setForeground(TEXT);

        content.add(productsTitle);

        content.add(
                Box.createVerticalStrut(8)
        );

        JPanel productRowsPanel =
                new JPanel();

        productRowsPanel.setBackground(BG);

        productRowsPanel.setLayout(
                new BoxLayout(
                        productRowsPanel,
                        BoxLayout.Y_AXIS
                )
        );

        List<ProductRowUI> rowUIs =
                new ArrayList<>();

        addProductRow(
                productRowsPanel,
                rowUIs,
                false,
                null
        );

        JScrollPane productScroll =
                new JScrollPane(
                        productRowsPanel
                );

        productScroll.setPreferredSize(
                new Dimension(
                        730,
                        190
                )
        );

        productScroll.setBorder(
                BorderFactory.createLineBorder(
                        BORDER
                )
        );

        content.add(productScroll);

        JButton addProductButton =
                createResetButton(
                        "+ Add Product"
                );

        addProductButton.addActionListener(
                e -> {

                    addProductRow(
                            productRowsPanel,
                            rowUIs,
                            false,
                            null
                    );

                    refreshAllProductRows(
                            rowUIs
                    );

                    productRowsPanel.revalidate();
                    productRowsPanel.repaint();
                }
        );

        content.add(
                Box.createVerticalStrut(8)
        );

        content.add(addProductButton);

        JPanel buttons =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT
                        )
                );

        buttons.setBackground(BG);

        JButton cancel =
                createResetButton(
                        "Cancel"
                );

        cancel.addActionListener(
                e -> dialog.dispose()
        );

        JButton save =
                createPrimaryButton(
                        "Create Order"
                );

        save.addActionListener(
                e -> {

                    String customer =
                            customerField.getText()
                                    .trim();

                    String phone =
                            phoneField.getText()
                                    .trim();

                    String address =
                            addressField.getText()
                                    .trim();

                    if (phone.isEmpty()) {

                        showError(
                                dialog,
                                "Phone number is required.\n"
                                        + "You cannot create an order without a phone number."
                        );

                        phoneField.requestFocus();
                        return;
                    }

                    if (address.isEmpty()) {

                        showError(
                                dialog,
                                "Address is required.\n"
                                        + "You cannot create an order without an address."
                        );

                        addressField.requestFocus();
                        return;
                    }

                    if (customer.isEmpty()) {

                        showError(
                                dialog,
                                "Please enter customer name."
                        );

                        customerField.requestFocus();
                        return;
                    }

                    double shipping =
                            parseNumber(
                                    shippingField.getText()
                            );

                    double discount =
                            parseNumber(
                                    discountField.getText()
                            );

                    if (shipping < 0
                            || discount < 0) {

                        showError(
                                dialog,
                                "Shipping and discount cannot be negative."
                        );

                        return;
                    }

                    List<OrderLine> lines =
                            collectProductLines(
                                    rowUIs
                            );

                    if (lines.isEmpty()) {

                        showError(
                                dialog,
                                "Please add at least one product."
                        );

                        return;
                    }

                    if (!checkCombinedStock(
                            lines,
                            dialog
                    )) {

                        return;
                    }

                    String delivery =
                            String.valueOf(
                                    deliveryBox.getSelectedItem()
                            );

                    /*
                     * A brand-new order cannot already be returned: no
                     * return record can exist for an order that has not
                     * been created yet. "Returned" / "Partially Returned"
                     * are accounting-backed states owned exclusively by
                     * the Returns workflow.
                     */
                    if ("Returned".equals(delivery)
                            || "Partially Returned".equals(delivery)) {

                        showError(
                                dialog,
                                "A new order cannot start with delivery "
                                        + "status \"" + delivery + "\".\n\n"
                                        + "That state is only produced by "
                                        + "processing an actual return "
                                        + "through the Returns screen after "
                                        + "the order exists."
                        );

                        return;
                    }

                    String orderStatus =
                            String.valueOf(
                                    orderStatusBox.getSelectedItem()
                            );

                    if ("Delivered".equals(delivery)) {
                        orderStatus = "Completed";
                    }

                    if ("Returned".equals(delivery)
                            && "Completed".equals(orderStatus)) {

                        orderStatus = "Not Prepared";
                    }

                    Order order =
                            new Order(
                                    generateOrderId(),
                                    customer,
                                    phone,
                                    additionalPhoneField
                                            .getText()
                                            .trim(),
                                    address,
                                    displayDateFormat.format(
                                            new Date()
                                    ),
                                    String.valueOf(
                                            paymentBox.getSelectedItem()
                                    ),
                                    orderStatus,
                                    delivery,
                                    shipping,
                                    discount,
                                    notesArea.getText()
                                            .trim()
                            );

                    for (OrderLine line :
                            lines) {

                        order.addItem(
                                line.product.name,
                                line.product.code,
                                line.product.sku,
                                line.product.category,
                                line.product.size,
                                line.product.color,
                                line.product.price,
                                line.quantity
                        );
                    }

                    order.calculateTotal(
                            order.getSubtotal()
                    );

                    if ("Delivered".equals(delivery)) {

                        order.deliveredAt =
                                new Date();
                    }

                    deductStock(lines);

                    orders.add(order);

                    // Link this order to the Customers screen:
                    // updates existing customer stats (orders count,
                    // total spent, last order date) or creates a new
                    // customer record automatically.
                    CustomersPanel.recordOrderForCustomer(
                            customer,
                            phone,
                            parseNumber(order.total),
                            order.date
                    );

                    refreshOrdersTable();
                    updateSummary();
                    applySort();
                    applyFilters();

                    Clothes_system.refreshAllDataViews();

                    dialog.dispose();
                }
        );

        buttons.add(cancel);
        buttons.add(save);

        dialog.add(
                new JScrollPane(content),
                BorderLayout.CENTER
        );

        dialog.add(
                buttons,
                BorderLayout.SOUTH
        );

        dialog.setVisible(true);
    }

    // =========================================================
    // DETAILS
    // =========================================================

    private void showOrderDetails(
            Order order
    ) {

        JDialog dialog =
                new JDialog(
                        SwingUtilities.getWindowAncestor(this),
                        "Order Details - " + order.id,
                        Dialog.ModalityType.APPLICATION_MODAL
                );

        dialog.setSize(
                920,
                760
        );

        dialog.setLocationRelativeTo(this);

        dialog.setLayout(
                new BorderLayout()
        );

        JPanel content =
                new JPanel();

        content.setBackground(BG);

        content.setBorder(
                new EmptyBorder(
                        20,
                        22,
                        20,
                        22
                )
        );

        content.setLayout(
                new BoxLayout(
                        content,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel title =
                new JLabel(order.id);

        title.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        22
                )
        );

        title.setForeground(TEXT);

        content.add(title);

        content.add(
                Box.createVerticalStrut(15)
        );

        JPanel infoGrid =
                new JPanel(
                        new GridLayout(
                                4,
                                4,
                                8,
                                8
                        )
                );

        infoGrid.setBackground(BG);

        addInfo(infoGrid, "Customer", order.customer);
        addInfo(infoGrid, "Phone", order.phone);
        addInfo(
                infoGrid,
                "Additional Phone",
                emptyAsDash(order.additionalPhone)
        );
        addInfo(infoGrid, "Date", order.date);
        addInfo(infoGrid, "Payment", order.payment);
        addInfo(infoGrid, "Address", order.address);
        addInfo(infoGrid, "Order Status", order.orderStatus);
        addInfo(infoGrid, "Delivery Status", getEffectiveDeliveryStatus(order));

        addInfo(
                infoGrid,
                "Delivered At",
                order.deliveredAt == null
                        ? "-"
                        : displayDateFormat.format(
                                order.deliveredAt
                        )
        );

        addInfo(
                infoGrid,
                "Shipping",
                formatMoney(order.shipping)
        );

        addInfo(
                infoGrid,
                "Discount",
                formatMoney(order.discount)
        );

        addInfo(
                infoGrid,
                "Total",
                order.total
        );

        content.add(infoGrid);

        content.add(
                Box.createVerticalStrut(12)
        );

        JPanel notesBox =
                new JPanel(
                        new BorderLayout()
                );

        notesBox.setBackground(WHITE);

        notesBox.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER
                        ),
                        BorderFactory.createEmptyBorder(
                                10,
                                12,
                                10,
                                12
                        )
                )
        );

        JLabel notesTitle =
                new JLabel(
                        "Order Details / Notes"
                );

        notesTitle.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        10
                )
        );

        notesTitle.setForeground(MUTED);

        JTextArea notes =
                new JTextArea(
                        emptyAsDash(order.notes)
                );

        notes.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        12
                )
        );

        notes.setForeground(TEXT);
        notes.setEditable(false);
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);
        notes.setRows(3);
        notes.setBackground(WHITE);

        notesBox.add(
                notesTitle,
                BorderLayout.NORTH
        );

        notesBox.add(
                notes,
                BorderLayout.CENTER
        );

        content.add(notesBox);

        content.add(
                Box.createVerticalStrut(15)
        );

        JLabel productsLabel =
                new JLabel(
                        "Ordered Products"
                );

        productsLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        14
                )
        );

        productsLabel.setForeground(TEXT);

        content.add(productsLabel);

        content.add(
                Box.createVerticalStrut(8)
        );

        DefaultTableModel model =
                new DefaultTableModel(
                        new Object[]{
                                "Product Name",
                                "Code",
                                "Category",
                                "Size",
                                "Color",
                                "Price",
                                "Qty",
                                "Product Total"
                        },
                        0
                ) {

                    @Override
                    public boolean isCellEditable(
                            int row,
                            int column
                    ) {
                        return false;
                    }
                };

        for (OrderItem item :
                order.items) {

            model.addRow(
                    new Object[]{
                            item.name,
                            item.code,
                            item.category,
                            item.size,
                            item.color,
                            formatMoney(item.price),
                            item.quantity,
                            formatMoney(
                                    item.price
                                            * item.quantity
                            )
                    }
            );
        }

        JTable table =
                new JTable(model);

        table.setRowHeight(38);

        table.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        12
                )
        );

        table.getTableHeader()
                .setFont(
                        new Font(
                                "Arial",
                                Font.BOLD,
                                11
                        )
                );

        JScrollPane tableScroll =
                new JScrollPane(table);

        tableScroll.setPreferredSize(
                new Dimension(
                        850,
                        200
                )
        );

        content.add(tableScroll);

        content.add(
                Box.createVerticalStrut(15)
        );

        JPanel totals =
                new JPanel(
                        new GridLayout(
                                4,
                                2,
                                8,
                                8
                        )
                );

        totals.setBackground(WHITE);

        totals.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER
                        ),
                        BorderFactory.createEmptyBorder(
                                12,
                                15,
                                12,
                                15
                        )
                )
        );

        addTotal(
                totals,
                "Subtotal",
                formatMoney(
                        order.getSubtotal()
                )
        );

        addTotal(
                totals,
                "Shipping",
                formatMoney(
                        order.shipping
                )
        );

        addTotal(
                totals,
                "Discount",
                formatMoney(
                        order.discount
                )
        );

        addTotal(
                totals,
                "Grand Total",
                order.total
        );

        content.add(totals);

        JPanel buttons =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT
                        )
                );

        buttons.setBackground(BG);

        JButton close =
                createResetButton("Close");

        close.addActionListener(
                e -> dialog.dispose()
        );

        JButton edit =
                createPrimaryButton(
                        "Edit Order"
                );

        edit.addActionListener(
                e -> {

                    dialog.dispose();

                    showEditOrderDialog(order);
                }
        );

        JButton returnWhole = createResetButton("Return Entire Order");
        returnWhole.addActionListener(e -> ReturnsPanel.processWholeOrderReturn(order, dialog));
        buttons.add(returnWhole);
        buttons.add(close);
        buttons.add(edit);

        dialog.add(
                new JScrollPane(content),
                BorderLayout.CENTER
        );

        dialog.add(
                buttons,
                BorderLayout.SOUTH
        );

        dialog.setVisible(true);
    }

    // =========================================================
    // EDIT ORDER
    // =========================================================

    private void showEditOrderDialog(
            Order order
    ) {

        JDialog dialog =
                new JDialog(
                        SwingUtilities.getWindowAncestor(this),
                        "Edit Order - " + order.id,
                        Dialog.ModalityType.APPLICATION_MODAL
                );

        dialog.setSize(
                820,
                830
        );

        dialog.setLocationRelativeTo(this);

        dialog.setLayout(
                new BorderLayout()
        );

        JPanel content =
                new JPanel();

        content.setBackground(BG);

        content.setBorder(
                new EmptyBorder(
                        20,
                        22,
                        20,
                        22
                )
        );

        content.setLayout(
                new BoxLayout(
                        content,
                        BoxLayout.Y_AXIS
                )
        );

        // =====================================================
        // CUSTOMER
        // =====================================================

        JPanel customerPanel =
                new JPanel(
                        new GridLayout(
                                2,
                                2,
                                10,
                                10
                        )
                );

        customerPanel.setBackground(BG);

        JTextField customerField = createInput();
        customerField.setText(order.customer);

        JTextField phoneField = createInput();
        phoneField.setText(order.phone);

        JTextField additionalPhoneField = createInput();
        additionalPhoneField.setText(order.additionalPhone);

        JTextField addressField = createInput();
        addressField.setText(order.address);

        addFormField(
                customerPanel,
                "Customer *",
                customerField
        );

        addFormField(
                customerPanel,
                "Phone *",
                phoneField
        );

        addFormField(
                customerPanel,
                "Additional Phone (Optional)",
                additionalPhoneField
        );

        addFormField(
                customerPanel,
                "Address *",
                addressField
        );

        content.add(customerPanel);

        content.add(
                Box.createVerticalStrut(10)
        );

        JTextArea notesArea =
                createNotesArea();

        notesArea.setText(order.notes);

        content.add(
                createTextAreaField(
                        "Order Details / Notes (Optional)",
                        notesArea
                )
        );

        content.add(
                Box.createVerticalStrut(15)
        );

        // =====================================================
        // SETTINGS
        // =====================================================

        JPanel statusPanel =
                new JPanel(
                        new GridLayout(
                                2,
                                2,
                                10,
                                10
                        )
                );

        statusPanel.setBackground(BG);

        JComboBox<String> paymentBox =
                new JComboBox<>(
                        new String[]{
                                "Cash",
                                "Visa",
                                "Bank Transfer"
                        }
                );

        paymentBox.setSelectedItem(
                order.payment
        );

        JComboBox<String> orderStatusBox =
                new JComboBox<>(
                        new String[]{
                                "Not Prepared",
                                "Preparing",
                                "Prepared",
                                "Completed"
                        }
                );

        orderStatusBox.setSelectedItem(
                order.orderStatus
        );

        JComboBox<String> deliveryBox =
                new JComboBox<>(
                        new String[]{
                                "With Shipping Company",
                                "Out for Delivery",
                                "Delivered",
                                "Partially Returned",
                                "Returned"
                        }
                );

        deliveryBox.setSelectedItem(
                order.deliveryStatus
        );

        JTextField shippingField = createInput();

        shippingField.setText(
                String.valueOf(
                        order.shipping
                )
        );

        JTextField discountField = createInput();

        discountField.setText(
                String.valueOf(
                        order.discount
                )
        );

        addFormField(
                statusPanel,
                "Payment",
                paymentBox
        );

        addFormField(
                statusPanel,
                "Order Status",
                orderStatusBox
        );

        addFormField(
                statusPanel,
                "Delivery Status",
                deliveryBox
        );

        addFormField(
                statusPanel,
                "Shipping",
                shippingField
        );

        content.add(statusPanel);

        JPanel discountPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT
                        )
                );

        discountPanel.setBackground(BG);

        addFormField(
                discountPanel,
                "Discount",
                discountField
        );

        content.add(discountPanel);

        content.add(
                Box.createVerticalStrut(15)
        );

        JLabel productsTitle =
                new JLabel("Products");

        productsTitle.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        14
                )
        );

        productsTitle.setForeground(TEXT);

        content.add(productsTitle);

        content.add(
                Box.createVerticalStrut(8)
        );

        JPanel productRowsPanel =
                new JPanel();

        productRowsPanel.setBackground(BG);

        productRowsPanel.setLayout(
                new BoxLayout(
                        productRowsPanel,
                        BoxLayout.Y_AXIS
                )
        );

        List<ProductRowUI> rowUIs =
                new ArrayList<>();

        Map<String, Integer> allowance =
                new HashMap<>();

        for (OrderItem item :
                order.items) {

            allowance.put(
                    item.code,
                    allowance.getOrDefault(
                            item.code,
                            0
                    ) + item.quantity
            );
        }

        for (OrderItem item :
                order.items) {

            Product productModel =
                    ProductManager.getInstance().findById(item.code);

            if (productModel != null) {

                ProductItem product =
                        createProductItem(
                                productModel
                        );

                OrderLine line =
                        new OrderLine();

                line.product = product;
                line.quantity = item.quantity;

                addProductRow(
                        productRowsPanel,
                        rowUIs,
                        true,
                        new ExistingProductData(
                                line,
                                allowance
                        )
                );
            }
        }

        if (rowUIs.isEmpty()) {

            addProductRow(
                    productRowsPanel,
                    rowUIs,
                    true,
                    new ExistingProductData(
                            null,
                            allowance
                    )
            );
        }

        refreshAllProductRows(rowUIs);

        JScrollPane productScroll =
                new JScrollPane(
                        productRowsPanel
                );

        productScroll.setPreferredSize(
                new Dimension(
                        730,
                        220
                )
        );

        productScroll.setBorder(
                BorderFactory.createLineBorder(
                        BORDER
                )
        );

        content.add(productScroll);

        JButton addProductButton =
                createResetButton(
                        "+ Add Product"
                );

        addProductButton.addActionListener(
                e -> {

                    addProductRow(
                            productRowsPanel,
                            rowUIs,
                            true,
                            new ExistingProductData(
                                    null,
                                    allowance
                            )
                    );

                    refreshAllProductRows(
                            rowUIs
                    );

                    productRowsPanel.revalidate();
                    productRowsPanel.repaint();
                }
        );

        content.add(
                Box.createVerticalStrut(8)
        );

        content.add(addProductButton);

        // =====================================================
        // SAVE
        // =====================================================

        JPanel buttons =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT
                        )
                );

        buttons.setBackground(BG);

        JButton cancel =
                createResetButton(
                        "Cancel"
                );

        cancel.addActionListener(
                e -> dialog.dispose()
        );

        JButton save =
                createPrimaryButton(
                        "Save Changes"
                );

        save.addActionListener(
                e -> {

                    String customer =
                            customerField.getText()
                                    .trim();

                    String phone =
                            phoneField.getText()
                                    .trim();

                    String address =
                            addressField.getText()
                                    .trim();

                    if (phone.isEmpty()) {

                        showError(
                                dialog,
                                "Phone number is required.\n"
                                        + "You cannot save an order without a phone number."
                        );

                        phoneField.requestFocus();
                        return;
                    }

                    if (address.isEmpty()) {

                        showError(
                                dialog,
                                "Address is required.\n"
                                        + "You cannot save an order without an address."
                        );

                        addressField.requestFocus();
                        return;
                    }

                    if (customer.isEmpty()) {

                        showError(
                                dialog,
                                "Please enter customer name."
                        );

                        customerField.requestFocus();
                        return;
                    }

                    double shipping =
                            parseNumber(
                                    shippingField.getText()
                            );

                    double discount =
                            parseNumber(
                                    discountField.getText()
                            );

                    if (shipping < 0
                            || discount < 0) {

                        showError(
                                dialog,
                                "Shipping and discount cannot be negative."
                        );

                        return;
                    }

                    /*
                     * Make sure the UI is synchronized before saving.
                     */
                    refreshAllProductRows(rowUIs);

                    List<OrderLine> lines =
                            collectProductLines(
                                    rowUIs
                            );

                    if (lines.isEmpty()) {

                        showError(
                                dialog,
                                "Please add at least one product."
                        );

                        return;
                    }

                    /*
                     * An edit must never leave an order holding fewer
                     * units than have already been returned: that would
                     * make the returned quantity impossible and push
                     * inventory above its true level. Constrain the
                     * edit instead of corrupting the return history.
                     */
                    Map<String, Integer> alreadyReturned =
                            ReturnsPanel.getReturnedUnitsByProduct(order);

                    if (!alreadyReturned.isEmpty()) {

                        Map<String, Integer> requested = new HashMap<>();

                        for (OrderLine line : lines) {
                            if (line == null || line.product == null) continue;
                            requested.put(line.product.code,
                                    requested.getOrDefault(line.product.code, 0)
                                            + line.quantity);
                        }

                        for (Map.Entry<String, Integer> entry :
                                alreadyReturned.entrySet()) {

                            int nowOrdered =
                                    requested.getOrDefault(entry.getKey(), 0);

                            if (nowOrdered < entry.getValue()) {

                                Product returnedProduct =
                                        ProductManager.getInstance()
                                                .findById(entry.getKey());

                                showError(
                                        dialog,
                                        "This order already has "
                                                + entry.getValue()
                                                + " returned unit(s) of\n\n"
                                                + (returnedProduct == null
                                                        ? entry.getKey()
                                                        : returnedProduct.getName())
                                                + "\n\nThe quantity cannot be reduced below "
                                                + entry.getValue()
                                                + ", and the product cannot be removed,\n"
                                                + "because that would invalidate the existing"
                                                + " return record."
                                );

                                return;
                            }
                        }
                    }

                    /*
                     * Temporarily restore the original order stock.
                     */
                    restoreOrderStock(order);

                    if (!checkCombinedStock(
                            lines,
                            dialog
                    )) {

                        reapplyOrderStock(order);

                        refreshAllProductRows(rowUIs);

                        return;
                    }

                    String oldDelivery =
                            order.deliveryStatus;

                    String delivery =
                            String.valueOf(
                                    deliveryBox.getSelectedItem()
                            );

                    /*
                     * Order Edit must never be able to invent or discard a
                     * return. "Returned" and "Partially Returned" are
                     * accounting-backed states owned exclusively by the
                     * Returns workflow (see ReturnsPanel.syncOrderReturnStatus).
                     * A raw status pick from this dialog is only accepted
                     * when it actually matches the order's current return
                     * accounting; otherwise the edit is rejected so the
                     * user is sent to the Returns screen instead.
                     */
                    if ("Returned".equals(delivery)
                            || "Partially Returned".equals(delivery)) {

                        boolean hasReturn =
                                ReturnsPanel.hasProcessedReturn(order.id);

                        int remaining =
                                ReturnsPanel.getRemainingReturnableUnits(order);

                        boolean matchesReturnedState =
                                hasReturn && remaining == 0
                                        && "Returned".equals(delivery);

                        boolean matchesPartialState =
                                hasReturn && remaining > 0
                                        && "Partially Returned".equals(delivery);

                        if (!matchesReturnedState && !matchesPartialState) {

                            reapplyOrderStock(order);

                            showError(
                                    dialog,
                                    "Delivery status cannot be set to \""
                                            + delivery
                                            + "\" here.\n\n"
                                            + "This state is only produced "
                                            + "automatically when a return is "
                                            + "processed through the Returns "
                                            + "screen, and it must match the "
                                            + "order's actual returned "
                                            + "quantity."
                            );

                            deliveryBox.setSelectedItem(oldDelivery);

                            return;
                        }
                    }

                    String newOrderStatus =
                            String.valueOf(
                                    orderStatusBox.getSelectedItem()
                            );

                    if ("Delivered".equals(delivery)) {

                        newOrderStatus =
                                "Completed";
                    }

                    if ("Returned".equals(delivery)
                            && "Completed".equals(
                            newOrderStatus
                    )) {

                        newOrderStatus =
                                "Not Prepared";
                    }

                    deductStock(lines);

                    String oldCustomerPhone = order.phone;
                    double oldOrderTotal = parseNumber(order.total);

                    order.customer = customer;
                    order.phone = phone;

                    order.additionalPhone =
                            additionalPhoneField
                                    .getText()
                                    .trim();

                    order.address = address;

                    order.payment =
                            String.valueOf(
                                    paymentBox.getSelectedItem()
                            );

                    order.orderStatus =
                            newOrderStatus;

                    order.deliveryStatus =
                            delivery;

                    order.shipping = shipping;
                    order.discount = discount;

                    order.notes =
                            notesArea.getText()
                                    .trim();

                    if ("Delivered".equals(delivery)) {

                        if (!"Delivered".equals(
                                oldDelivery
                        )
                                || order.deliveredAt == null) {

                            order.deliveredAt =
                                    new Date();
                        }

                    } else {

                        order.deliveredAt = null;
                    }

                    List<OrderItem> oldItems =
                            new ArrayList<>(order.items);

                    order.items.clear();

                    for (OrderLine line : lines) {

                        OrderItem snapshot = null;

                        for (OrderItem oldItem : oldItems) {
                            if (oldItem == null) continue;
                            if (line.product.code.equals(oldItem.code)
                                    && safeEquals(line.product.size, oldItem.size)
                                    && safeEquals(line.product.color, oldItem.color)) {
                                snapshot = oldItem;
                                break;
                            }
                        }

                        if (snapshot != null) {
                            order.items.add(new OrderItem(
                                    line.product.name,
                                    line.product.code,
                                    line.product.sku,
                                    line.product.category,
                                    line.product.size,
                                    line.product.color,
                                    snapshot.price,
                                    snapshot.costPrice,
                                    line.quantity
                            ));
                        } else {
                            order.addItem(
                                    line.product.name,
                                    line.product.code,
                                    line.product.sku,
                                    line.product.category,
                                    line.product.size,
                                    line.product.color,
                                    line.product.price,
                                    line.quantity
                            );
                        }
                    }

                    order.calculateTotal(
                            order.getSubtotal()
                    );

                    // Keep the linked customer's stats correct:
                    // remove the old amount from the old phone,
                    // then add the new (possibly edited) amount.
                    CustomersPanel.adjustOrderAmountForCustomer(
                            oldCustomerPhone,
                            oldOrderTotal,
                            order.customer,
                            order.phone,
                            parseNumber(order.total),
                            order.date
                    );

                    refreshOrdersTable();
                    updateSummary();
                    applySort();
                    applyFilters();

                    Clothes_system.refreshAllDataViews();

                    dialog.dispose();
                }
        );

        buttons.add(cancel);
        buttons.add(save);

        dialog.add(
                new JScrollPane(content),
                BorderLayout.CENTER
        );

        dialog.add(
                buttons,
                BorderLayout.SOUTH
        );

        dialog.setVisible(true);
    }

    // =========================================================
    // PRODUCT ROW
    // =========================================================

    private void addProductRow(
            JPanel parent,
            List<ProductRowUI> rowUIs,
            boolean editMode,
            ExistingProductData existingData
    ) {

        JPanel row =
                new JPanel(
                        new BorderLayout(
                                8,
                                0
                        )
                );

        row.setBackground(WHITE);

        row.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER
                        ),
                        BorderFactory.createEmptyBorder(
                                7,
                                7,
                                7,
                                7
                        )
                )
        );

        row.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        58
                )
        );

        JComboBox<ProductItem> productBox =
                new JComboBox<>();

        productBox.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        11
                )
        );

        productBox.setPreferredSize(
                new Dimension(
                        470,
                        35
                )
        );

        JSpinner spinner =
                new JSpinner();

        spinner.setPreferredSize(
                new Dimension(
                        75,
                        35
                )
        );

        JButton remove =
                createResetButton(
                        "Remove"
                );

        ProductRowUI ui =
                new ProductRowUI();

        ui.row = row;
        ui.productBox = productBox;
        ui.spinner = spinner;
        ui.removeButton = remove;
        ui.line = new OrderLine();
        ui.editMode = editMode;

        if (existingData != null) {

            ui.allowance =
                    existingData.allowance;

            if (existingData.line != null) {

                ui.desiredProductCode =
                        existingData.line.product == null
                                ? null
                                : existingData.line.product.code;

                ui.desiredQuantity =
                        Math.max(
                                1,
                                existingData.line.quantity
                        );
            }
        }

        rowUIs.add(ui);

        // Initial population.
        refreshAllProductRows(rowUIs);

        productBox.addActionListener(
                e -> {

                    if (ui.updating) {
                        return;
                    }

                    ProductItem product =
                            (ProductItem)
                                    productBox.getSelectedItem();

                    if (product == null) {
                        return;
                    }

                    ui.line.product =
                            product;

                    ui.desiredProductCode =
                            product.code;

                    refreshAllProductRows(rowUIs);
                }
        );

        spinner.addChangeListener(
                e -> {

                    if (ui.updating) {
                        return;
                    }

                    ProductItem product =
                            (ProductItem)
                                    productBox.getSelectedItem();

                    if (product == null) {
                        return;
                    }

                    int value =
                            ((Number)
                                    spinner.getValue())
                                    .intValue();

                    ui.desiredQuantity =
                            Math.max(
                                    1,
                                    value
                            );

                    ui.line.product =
                            product;

                    ui.line.quantity =
                            ui.desiredQuantity;

                    refreshAllProductRows(rowUIs);
                }
        );

        remove.addActionListener(
                e -> {

                    if (rowUIs.size() == 1) {

                        ui.updating = true;

                        ui.desiredProductCode = null;
                        ui.desiredQuantity = 1;

                        ui.line.product = null;
                        ui.line.quantity = 1;

                        refreshAllProductRows(rowUIs);

                        ui.updating = false;

                    } else {

                        rowUIs.remove(ui);

                        parent.remove(row);

                        refreshAllProductRows(rowUIs);

                        parent.revalidate();
                        parent.repaint();
                    }
                }
        );

        JPanel qtyPanel =
                new JPanel(
                        new BorderLayout()
                );

        qtyPanel.setBackground(WHITE);

        JLabel qtyLabel =
                new JLabel("Qty");

        qtyLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        10
                )
        );

        qtyLabel.setForeground(MUTED);

        qtyPanel.add(
                qtyLabel,
                BorderLayout.NORTH
        );

        qtyPanel.add(
                spinner,
                BorderLayout.CENTER
        );

        row.add(
                remove,
                BorderLayout.WEST
        );

        row.add(
                productBox,
                BorderLayout.CENTER
        );

        row.add(
                qtyPanel,
                BorderLayout.EAST
        );

        parent.add(row);
    }

    // =========================================================
    // DYNAMIC PRODUCT AVAILABILITY
    // =========================================================

    private void refreshAllProductRows(
            List<ProductRowUI> rowUIs
    ) {

        if (rowUIs == null || rowUIs.isEmpty()) {
            return;
        }

        for (ProductRowUI ui : rowUIs) {
            if (ui == null) continue;

            if (ui.desiredProductCode == null && ui.productBox != null) {
                ProductItem selected = (ProductItem) ui.productBox.getSelectedItem();
                if (selected != null) ui.desiredProductCode = selected.code;
            }

            if (ui.productBox != null && ui.spinner != null
                    && ui.productBox.getSelectedItem() != null) {
                try {
                    ui.desiredQuantity = Math.max(1,
                            ((Number) ui.spinner.getValue()).intValue());
                } catch (Exception ignored) {}
            }
        }

        for (ProductRowUI target : rowUIs) {
            if (target == null || target.productBox == null) continue;

            String wantedCode = target.desiredProductCode;
            int wantedQuantity = Math.max(1, target.desiredQuantity);

            target.updating = true;
            target.productBox.removeAllItems();

            for (Product product : ProductManager.getInstance().getAllProducts()) {
                if (product == null) continue;

                int available = getAvailableForRow(product.getId(), target, rowUIs);
                boolean active = product.isActive();
                boolean currentSelection = wantedCode != null && wantedCode.equals(product.getId());

                if ((available > 0 && active) || currentSelection) {
                    target.productBox.addItem(createProductItem(product));
                }
            }

            ProductItem selected = wantedCode == null
                    ? null
                    : findComboProduct(target.productBox, wantedCode);

            if (selected == null && target.productBox.getItemCount() > 0) {
                selected = target.productBox.getItemAt(0);
                target.desiredProductCode = selected.code;
            } else if (selected == null) {
                target.desiredProductCode = null;
                target.line.product = null;
                target.line.quantity = 1;
                target.spinner.setModel(new SpinnerNumberModel(1, 1, 1, 1));
                target.updating = false;
                continue;
            }

            target.productBox.setSelectedItem(selected);

            int maximum = getAvailableForRow(selected.code, target, rowUIs);
            maximum = Math.max(maximum, wantedQuantity);
            maximum = Math.max(1, maximum);

            int finalQuantity = Math.max(1, Math.min(wantedQuantity, maximum));
            target.spinner.setModel(new SpinnerNumberModel(finalQuantity, 1, maximum, 1));
            target.desiredQuantity = finalQuantity;
            target.line.product = selected;
            target.line.quantity = finalQuantity;
            target.updating = false;
        }
    }

    private int getAvailableForRow(
            String code,
            ProductRowUI target,
            List<ProductRowUI> rowUIs
    ) {

        if (code == null) return 0;

        Product product = ProductManager.getInstance().findById(code);
        if (product == null) return 0;

        int available = product.getStockQuantity();

        if (target != null && target.editMode && target.allowance != null) {
            available += target.allowance.getOrDefault(code, 0);
        }

        if (rowUIs != null) {
            for (ProductRowUI ui : rowUIs) {
                if (ui == null || ui == target || ui.productBox == null || ui.spinner == null) continue;

                ProductItem other = (ProductItem) ui.productBox.getSelectedItem();
                if (other == null || !code.equals(other.code)) continue;

                try {
                    available -= Math.max(0, ((Number) ui.spinner.getValue()).intValue());
                } catch (Exception ignored) {}
            }
        }

        return Math.max(0, available);
    }

    private ProductItem findComboProduct(JComboBox<ProductItem> box, String code) {
        for (int i = 0; i < box.getItemCount(); i++) {
            ProductItem item = box.getItemAt(i);
            if (item != null && code.equals(item.code)) return item;
        }
        return null;
    }

    private List<OrderLine> collectProductLines(List<ProductRowUI> rowUIs) {
        List<OrderLine> lines = new ArrayList<>();
        for (ProductRowUI ui : rowUIs) {
            if (ui == null || ui.productBox == null) continue;
            ProductItem product = (ProductItem) ui.productBox.getSelectedItem();
            if (product == null) continue;
            int quantity = ((Number) ui.spinner.getValue()).intValue();
            if (quantity <= 0) continue;
            OrderLine line = new OrderLine();
            line.product = product;
            line.quantity = quantity;
            lines.add(line);
        }
        return lines;
    }

    private boolean checkCombinedStock(List<OrderLine> lines, Component parent) {
        Map<String, Integer> requested = new HashMap<>();
        Map<String, ProductItem> products = new HashMap<>();

        for (OrderLine line : lines) {
            if (line == null || line.product == null) continue;
            String code = line.product.code;
            requested.put(code, requested.getOrDefault(code, 0) + line.quantity);
            products.put(code, line.product);
        }

        for (String code : requested.keySet()) {
            Product product = ProductManager.getInstance().findById(code);
            ProductItem item = products.get(code);
            if (product == null) {
                showError(parent, "Product not found:\n" + (item == null ? code : item.name));
                return false;
            }

            int available = product.getStockQuantity();
            int required = requested.get(code);
            if (required > available) {
                showError(parent, "Not enough stock for:\n\n" + product.getName()
                        + "\n\nAvailable: " + available + "\nRequested: " + required);
                return false;
            }
        }
        return true;
    }

    private void deductStock(List<OrderLine> lines) {
        Map<String, Integer> quantities = new HashMap<>();
        for (OrderLine line : lines) {
            if (line != null && line.product != null) {
                quantities.put(line.product.code,
                        quantities.getOrDefault(line.product.code, 0) + line.quantity);
            }
        }

        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            Product product = ProductManager.getInstance().findById(entry.getKey());
            if (product == null) continue;
            product.removeStock(entry.getValue(), "Order created/updated");
        }
    }

    private void restoreOrderStock(Order order) {
        if (order == null || order.items == null) return;
        Map<String, Integer> quantities = new HashMap<>();
        for (OrderItem item : order.items) {
            if (item != null) quantities.put(item.code,
                    quantities.getOrDefault(item.code, 0) + item.quantity);
        }
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            Product product = ProductManager.getInstance().findById(entry.getKey());
            if (product != null) product.addStock(entry.getValue(), "Order stock temporarily restored for editing");
        }
    }

    private void reapplyOrderStock(Order order) {
        if (order == null || order.items == null) return;
        Map<String, Integer> quantities = new HashMap<>();
        for (OrderItem item : order.items) {
            if (item != null) quantities.put(item.code,
                    quantities.getOrDefault(item.code, 0) + item.quantity);
        }
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            Product product = ProductManager.getInstance().findById(entry.getKey());
            if (product != null) product.removeStock(entry.getValue(), "Order edit cancelled, stock reapplied");
        }
    }

    private ProductItem createProductItem(Product product) {
        String size = "";
        String color = "";
        if (product != null && !product.getVariants().isEmpty()) {
            Product.ProductVariant v = product.getVariants().get(0);
            size = v.getSize();
            color = v.getColor();
        }

        return new ProductItem(
                -1,
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getCategory(),
                size,
                color,
                product.getSellingPrice(),
                product.getStockQuantity()
        );
    }

    public void setSearchQuery(String query) {
        if (searchField == null) return;
        searchField.setText(query == null ? "" : query.trim());
        applyFilters();
    }

    private Order orderAtPoint(MouseEvent e) {
        int viewRow = ordersTable.rowAtPoint(e.getPoint());
        if (viewRow < 0) return null;
        int modelRow = ordersTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= orders.size()) return null;
        ordersTable.setRowSelectionInterval(viewRow, viewRow);
        return orders.get(modelRow);
    }

    private void handleOrderMouse(MouseEvent e) {
        Order order = orderAtPoint(e);
        if (order == null) return;
        if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem details = new JMenuItem("View Details");
            JMenuItem edit = new JMenuItem("Edit Order");
            JMenuItem invoice = new JMenuItem("Print Invoice");
            JMenuItem wholeReturn = new JMenuItem("Mark Entire Order Returned");
            details.addActionListener(x -> showOrderDetails(order));
            edit.addActionListener(x -> showEditOrderDialog(order));
            invoice.addActionListener(x -> InvoicePanel.printOrder(order, SettingsManager.getPaperSize(), this));
            wholeReturn.addActionListener(x -> ReturnsPanel.processWholeOrderReturn(order, this));
            menu.add(details); menu.add(edit); menu.addSeparator(); menu.add(invoice); menu.addSeparator(); menu.add(wholeReturn);
            menu.show(ordersTable, e.getX(), e.getY());
            return;
        }
        if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) showOrderDetails(order);
    }

    private void showMoreActionsMenu(Component invoker) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem print = new JMenuItem("Print packing slips");
        JMenuItem selectNew = new JMenuItem("Select all new orders");
        JMenuItem clear = new JMenuItem("Clear selection");
        print.addActionListener(e -> printSelectedPackingSlips());
        selectNew.addActionListener(e -> {
            // Selection follows the visible/filtered dataset, exactly
            // like Select All. Hidden orders are never selected, and
            // selections already made on hidden rows are left alone.
            for (Order o : getVisibleOrdersInTableOrder()) {
                if (isNewOrderForPacking(o)) selectedOrderIds.add(o.id);
                else selectedOrderIds.remove(o.id);
            }
            refreshOrdersTable(); applyFilters(); updateSelectAllState();
        });
        clear.addActionListener(e -> { selectedOrderIds.clear(); refreshOrdersTable(); applyFilters(); });
        menu.add(print); menu.addSeparator(); menu.add(selectNew); menu.add(clear);
        menu.show(invoker, 0, invoker.getHeight());
    }

    private boolean isNewOrderForPacking(Order o) {
        if (o == null) return false;
        if ("Completed".equalsIgnoreCase(o.orderStatus)) return false;
        String d = o.deliveryStatus == null ? "" : o.deliveryStatus;
        return !"With Shipping Company".equalsIgnoreCase(d)
                && !"Out for Delivery".equalsIgnoreCase(d)
                && !"Delivered".equalsIgnoreCase(d)
                && !"Returned".equalsIgnoreCase(d)
                && !"Partially Returned".equalsIgnoreCase(d);
    }

    private List<Order> getVisibleOrdersInTableOrder() {
        List<Order> result = new ArrayList<>();
        if (ordersTable == null) return result;
        for (int viewRow = 0; viewRow < ordersTable.getRowCount(); viewRow++) {
            int modelRow = ordersTable.convertRowIndexToModel(viewRow);
            if (modelRow < 0 || modelRow >= ordersModel.getRowCount()) continue;
            String id = String.valueOf(ordersModel.getValueAt(modelRow, 0));
            for (Order o : orders) {
                if (o != null && o.id.equals(id)) {
                    result.add(o);
                    break;
                }
            }
        }
        return result;
    }

    private List<Order> getSelectedVisibleOrders() {
        List<Order> result = new ArrayList<>();
        for (Order o : getVisibleOrdersInTableOrder()) {
            if (selectedOrderIds.contains(o.id)) result.add(o);
        }
        return result;
    }

    private void selectAllVisibleOrders(boolean selected) {
        List<Order> visible = getVisibleOrdersInTableOrder();
        if (selected) {
            for (Order o : visible) selectedOrderIds.add(o.id);
        } else {
            for (Order o : visible) selectedOrderIds.remove(o.id);
        }
        refreshOrdersTable();
        applyFilters();
        updateSelectAllState();
    }

    private void updateSelectAllState() {
        if (selectAllCheckBox == null) return;
        List<Order> visible = getVisibleOrdersInTableOrder();
        boolean all = !visible.isEmpty();
        for (Order o : visible) {
            if (!selectedOrderIds.contains(o.id)) { all = false; break; }
        }
        selectAllCheckBox.setSelected(all);
        selectAllCheckBox.setEnabled(!visible.isEmpty());
    }

    private void printCurrentOrderSelection() {
        List<Order> visible = getVisibleOrdersInTableOrder();
        if (visible.isEmpty()) {
            JOptionPane.showMessageDialog(this, "There are no visible orders to print.", "Print Orders", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<Order> selected = getSelectedVisibleOrders();
        List<Order> toPrint = selected.isEmpty() ? visible : selected;

        InvoicePanel.printInvoices(toPrint, SettingsManager.getPaperSize(), this);
    }

    private void printSelectedPackingSlips() {
        List<Order> selected = getSelectedVisibleOrders();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this,"Select at least one visible order first.","Packing Slips",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<Order> eligible = new ArrayList<>();
        for (Order o : selected) if (isNewOrderForPacking(o)) eligible.add(o);
        if (eligible.isEmpty()) {
            JOptionPane.showMessageDialog(this,"The selected orders are not new packing orders. Completed, shipping and delivered orders are excluded.","Packing Slips",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        InvoicePanel.printPackingSlips(eligible, SettingsManager.getPaperSize(), this);
    }

    public void refreshData() {
        refreshOrdersTable();
        updateSummary();
        applySort();
        applyFilters();
    }

    /**
     * A Returned delivery status is only shown as an actual returned
     * order after ReturnsPanel has processed at least one return record.
     * This prevents demo/legacy delivery values from making a normal
     * Completed order look returned.
     */
    public static String getEffectiveDeliveryStatus(Order order) {
        if (order == null) return "";

        if ("Returned".equals(order.deliveryStatus)
                && !ReturnsPanel.hasProcessedReturn(order.id)) {
            return "Delivered";
        }

        return order.deliveryStatus;
    }

    private void refreshOrdersTable() {

        if (ordersModel == null) {
            return;
        }

        ordersModel.setRowCount(0);

        for (Order order :
                orders) {

            ordersModel.addRow(
                    new Object[]{
                            order.id,
                            order.customer,
                            order.phone,
                            order.date,
                            getProductNames(order),
                            parseNumber(
                                    order.total
                            ),
                            order.orderStatus,
                            getEffectiveDeliveryStatus(order),
                            selectedOrderIds.contains(order.id)
                    }
            );
        }
    }

    private String getProductNames(
            Order order
    ) {

        StringBuilder result =
                new StringBuilder();

        for (int i = 0;
             i < order.items.size();
             i++) {

            OrderItem item =
                    order.items.get(i);

            if (i > 0) {
                result.append(", ");
            }

            result.append(item.name);
            result.append(" × ");
            result.append(item.quantity);
        }

        return result.toString();
    }

    // =========================================================
    // SUMMARY
    // =========================================================

    private void updateSummary() {

        int totalOrders =
                orders.size();

        double revenue = 0;
        int completed = 0;
        int inDelivery = 0;
        int cancelled = 0;

        for (Order order :
                orders) {

            if ("Returned".equals(getEffectiveDeliveryStatus(order))) {
                cancelled++;
            }

            // Cancelled orders are not revenue. Dashboard and Reports
            // both exclude them; this summary previously did not.
            if (!"Cancelled".equalsIgnoreCase(order.orderStatus)) {
                revenue += Math.max(0, parseNumber(order.total)
                        - ReturnsPanel.getReturnedSalesAmountForOrder(order.id));
            }

            if ("Completed".equals(
                    order.orderStatus
            )) {

                completed++;
            }

            if ("With Shipping Company".equals(
                    order.deliveryStatus
            )
                    || "Out for Delivery".equals(
                    order.deliveryStatus
            )) {

                inDelivery++;
            }
        }

        totalOrdersLabel.setText(
                String.valueOf(totalOrders)
        );

        totalRevenueLabel.setText(
                formatMoney(revenue)
        );

        completedOrdersLabel.setText(
                String.valueOf(completed)
        );

        deliveryOrdersLabel.setText(
                String.valueOf(inDelivery)
        );

        cancelledOrdersLabel.setText(
                String.valueOf(cancelled)
        );
    }

    // =========================================================
    // DELIVERY API
    // =========================================================

    public static void updateDeliveryStatus(
            String orderId,
            String newStatus
    ) {

        for (Order order :
                orders) {

            if (!order.id.equals(orderId)) {
                continue;
            }

            /*
             * This is a generic delivery-status setter. It must never
             * be able to invent a return: marking an order Returned
             * here would flip the accounting fields without creating a
             * return record or restoring inventory, and the grid would
             * still display it as Delivered. A real return has to go
             * through the Returns screen or "Mark Entire Order
             * Returned", both of which do the accounting. So a raw
             * Returned request with no return behind it is downgraded
             * to the delivered state it actually still is.
             */
            if ("Returned".equals(newStatus)
                    && !(ReturnsPanel.hasProcessedReturn(order.id)
                            && ReturnsPanel.getRemainingReturnableUnits(order) == 0)) {

                newStatus = "Delivered";
            }

            /*
             * A raw request must not invent a partial return either: it
             * is only valid when the order's own accounting backs it.
             */
            if ("Partially Returned".equals(newStatus)
                    && !(ReturnsPanel.hasProcessedReturn(order.id)
                            && ReturnsPanel.getRemainingReturnableUnits(order) > 0)) {

                newStatus = "Delivered";
            }

            /*
             * Equally, a raw request must not erase a partial return.
             */
            if (!"Returned".equals(newStatus)
                    && ReturnsPanel.hasProcessedReturn(order.id)
                    && ReturnsPanel.getRemainingReturnableUnits(order) > 0
                    && "Delivered".equals(newStatus)) {

                newStatus = "Partially Returned";
            }

            order.deliveryStatus =
                    newStatus;

            if ("Delivered".equals(
                    newStatus
            )) {

                order.deliveredAt =
                        new Date();

                order.orderStatus =
                        "Completed";
            }

            if ("Returned".equals(
                    newStatus
            )) {

                order.deliveredAt =
                        null;

                if ("Completed".equals(
                        order.orderStatus
                )) {

                    order.orderStatus =
                            "Not Prepared";
                }
            }

            if (!"Delivered".equals(
                    newStatus
            )
                    && !"Returned".equals(
                    newStatus
            )) {

                order.deliveredAt =
                        null;
            }

            Clothes_system.refreshAllDataViews();
            return;
        }
    }

    // =========================================================
    // PUBLIC DATA FOR RETURNS
    // =========================================================

    public static synchronized void loadFromPersistence() {
        if (!orders.isEmpty()) return;
        orders.addAll(PersistenceRepository.readOrders());
    }

    public static List<Order> getOrders() {

        return orders;
    }

    public static List<Order> getReturnedOrders() {

        List<Order> result =
                new ArrayList<>();

        for (Order order :
                orders) {

            // Same source of truth as the Orders grid: an order counts
            // as returned only when return accounting actually backs it.
            if ("Returned".equals(
                    getEffectiveDeliveryStatus(order)
            )) {

                result.add(order);
            }
        }

        return result;
    }

    // =========================================================
    // ID
    // =========================================================

    private String generateOrderId() {

        int max = 0;

        for (Order order :
                orders) {

            try {

                String number =
                        order.id.replace(
                                "ORD-",
                                ""
                        );

                max =
                        Math.max(
                                max,
                                Integer.parseInt(
                                        number
                                )
                        );

            } catch (Exception ignored) {
            }
        }

        return String.format(
                Locale.US,
                "ORD-%03d",
                max + 1
        );
    }

    // =========================================================
    // DEMO DATA
    // =========================================================

    private void createDemoOrders() {

        if (demoOrdersCreated) {
            return;
        }

        demoOrdersCreated = true;

        if (!orders.isEmpty()) {
            return;
        }

        Order order1 =
                new Order(
                        "ORD-001",
                        "Ahmed Mohamed",
                        "01012345678",
                        "",
                        "Nasr City, Cairo",
                        "15 Sep 2026 18:20",
                        "Cash",
                        "Completed",
                        "Delivered",
                        50,
                        0,
                        "Please call before delivery."
                );

        order1.addItem(
                "Classic Hoodie",
                "PRD-001",
                "Hoodies",
                "M",
                1250,
                2
        );

        order1.addItem(
                "Basic T-Shirt",
                "PRD-003",
                "T-Shirts",
                "XL",
                450,
                2
        );

        order1.calculateTotal(
                order1.getSubtotal()
        );

        order1.deliveredAt =
                parseDateStatic(
                        "15 Sep 2026 20:10"
                );

        orders.add(order1);

        Order order2 =
                new Order(
                        "ORD-002",
                        "Omar Ali",
                        "01123456789",
                        "01000000002",
                        "Dokki, Giza",
                        "16 Sep 2026 10:15",
                        "Visa",
                        "Prepared",
                        "Out for Delivery",
                        70,
                        100,
                        "Leave the package with security if unavailable."
                );

        order2.addItem(
                "Slim Jeans",
                "PRD-002",
                "Jeans",
                "L",
                950,
                1
        );

        order2.calculateTotal(
                order2.getSubtotal()
        );

        orders.add(order2);

        Order order3 =
                new Order(
                        "ORD-003",
                        "Mohamed Hassan",
                        "01234567890",
                        "",
                        "Faisal, Giza",
                        "16 Sep 2026 13:40",
                        "Cash",
                        "Preparing",
                        "With Shipping Company",
                        0,
                        0,
                        "Customer requested size confirmation."
                );

        order3.addItem(
                "Cargo Pants",
                "PRD-005",
                "Pants",
                "L",
                1100,
                2
        );

        order3.calculateTotal(
                order3.getSubtotal()
        );

        orders.add(order3);

        Order order4 =
                new Order(
                        "ORD-004",
                        "Karim Adel",
                        "01598765432",
                        "",
                        "October, Giza",
                        "16 Sep 2026 15:30",
                        "Cash",
                        "Not Prepared",
                        "Returned",
                        50,
                        0,
                        "Customer was unavailable."
                );

        order4.addItem(
                "Classic Hoodie",
                "PRD-001",
                "Hoodies",
                "M",
                1250,
                1
        );

        order4.addItem(
                "Basic T-Shirt",
                "PRD-003",
                "T-Shirts",
                "XL",
                450,
                1
        );

        order4.calculateTotal(
                order4.getSubtotal()
        );

        orders.add(order4);
    }

    // =========================================================
    // UI HELPERS
    // =========================================================

    private JButton createPrimaryButton(
            String text
    ) {

        JButton button =
                new JButton(text);

        button.setFocusPainted(false);

        button.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        11
                )
        );

        button.setForeground(Color.WHITE);

        button.setBackground(GREEN);

        button.setBorder(
                BorderFactory.createEmptyBorder(
                        8,
                        12,
                        8,
                        12
                )
        );

        button.setCursor(
                new Cursor(
                        Cursor.HAND_CURSOR
                )
        );

        return button;
    }

    private JButton createResetButton(
            String text
    ) {

        JButton button =
                new JButton(text);

        button.setFocusPainted(false);

        button.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        11
                )
        );

        button.setForeground(
                new Color(
                        70,
                        85,
                        85
                )
        );

        button.setBackground(WHITE);

        button.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER
                        ),
                        BorderFactory.createEmptyBorder(
                                8,
                                12,
                                8,
                                12
                        )
                )
        );

        button.setCursor(
                new Cursor(
                        Cursor.HAND_CURSOR
                )
        );

        return button;
    }

    private JTextField createInput() {

        JTextField field =
                new JTextField();

        field.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        12
                )
        );

        field.setPreferredSize(
                new Dimension(
                        200,
                        35
                )
        );

        field.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(
                                        220,
                                        228,
                                        228
                                )
                        ),
                        BorderFactory.createEmptyBorder(
                                0,
                                8,
                                0,
                                8
                        )
                )
        );

        return field;
    }

    private JTextArea createNotesArea() {

        JTextArea area =
                new JTextArea();

        area.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        12
                )
        );

        area.setRows(3);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);

        area.setBorder(
                BorderFactory.createEmptyBorder(
                        8,
                        8,
                        8,
                        8
                )
        );

        return area;
    }

    private JPanel createTextAreaField(
            String labelText,
            JTextArea area
    ) {

        JPanel panel =
                new JPanel(
                        new BorderLayout(
                                0,
                                4
                        )
                );

        panel.setBackground(BG);

        JLabel label =
                new JLabel(labelText);

        label.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        10
                )
        );

        label.setForeground(MUTED);

        JScrollPane scroll =
                new JScrollPane(area);

        scroll.setBorder(
                BorderFactory.createLineBorder(
                        BORDER
                )
        );

        panel.add(
                label,
                BorderLayout.NORTH
        );

        panel.add(
                scroll,
                BorderLayout.CENTER
        );

        return panel;
    }

    private void addFormField(
            JPanel parent,
            String labelText,
            Component component
    ) {

        JPanel box =
                new JPanel(
                        new BorderLayout(
                                0,
                                4
                        )
                );

        box.setBackground(BG);

        JLabel label =
                new JLabel(labelText);

        label.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        10
                )
        );

        label.setForeground(MUTED);

        box.add(
                label,
                BorderLayout.NORTH
        );

        box.add(
                component,
                BorderLayout.CENTER
        );

        parent.add(box);
    }

    private void addInfo(
            JPanel parent,
            String title,
            String value
    ) {

        JPanel box =
                new JPanel(
                        new BorderLayout(
                                0,
                                4
                        )
                );

        box.setBackground(WHITE);

        box.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER
                        ),
                        BorderFactory.createEmptyBorder(
                                8,
                                10,
                                8,
                                10
                        )
                )
        );

        JLabel titleLabel =
                new JLabel(title);

        titleLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        9
                )
        );

        titleLabel.setForeground(MUTED);

        JLabel valueLabel =
                new JLabel(
                        value == null
                                ? "-"
                                : value
                );

        valueLabel.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        11
                )
        );

        valueLabel.setForeground(TEXT);

        box.add(
                titleLabel,
                BorderLayout.NORTH
        );

        box.add(
                valueLabel,
                BorderLayout.CENTER
        );

        parent.add(box);
    }

    private void addTotal(
            JPanel parent,
            String title,
            String value
    ) {

        JLabel titleLabel =
                new JLabel(title);

        titleLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        11
                )
        );

        titleLabel.setForeground(MUTED);

        JLabel valueLabel =
                new JLabel(value);

        valueLabel.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        12
                )
        );

        valueLabel.setForeground(TEXT);

        parent.add(titleLabel);
        parent.add(valueLabel);
    }

    private void showError(
            Component parent,
            String message
    ) {

        JOptionPane.showMessageDialog(
                parent,
                message,
                "Invalid Order",
                JOptionPane.ERROR_MESSAGE
        );
    }

    // =========================================================
    // PARSING
    // =========================================================

    private double parseNumber(
            String value
    ) {

        if (value == null
                || value.trim().isEmpty()) {

            return 0;
        }

        try {

            String cleaned =
                    value.replace(
                            "EGP",
                            ""
                    )
                    .replace(
                            ",",
                            ""
                    )
                    .trim();

            return Double.parseDouble(
                    cleaned
            );

        } catch (Exception e) {

            return 0;
        }
    }

    private Double parseOptionalDouble(
            String value
    ) {

        if (value == null
                || value.trim().isEmpty()) {

            return null;
        }

        try {

            return Double.parseDouble(
                    value.trim()
                            .replace(
                                    ",",
                                    ""
                            )
            );

        } catch (Exception e) {

            return null;
        }
    }

    private Integer parseOptionalInteger(
            String value
    ) {

        if (value == null
                || value.trim().isEmpty()) {

            return null;
        }

        try {

            return Integer.parseInt(
                    value.trim()
            );

        } catch (Exception e) {

            return null;
        }
    }

    private Date parseFilterDate(
            String value,
            boolean endOfDay
    ) {

        if (value == null
                || value.trim().isEmpty()) {

            return null;
        }

        String[] formats = {
                "dd MMM yyyy",
                "dd/MM/yyyy",
                "yyyy-MM-dd"
        };

        for (String format :
                formats) {

            try {

                SimpleDateFormat sdf =
                        new SimpleDateFormat(
                                format,
                                Locale.ENGLISH
                        );

                sdf.setLenient(false);

                Date date =
                        sdf.parse(
                                value.trim()
                        );

                if (endOfDay) {

                    Calendar calendar =
                            Calendar.getInstance();

                    calendar.setTime(date);

                    calendar.set(
                            Calendar.HOUR_OF_DAY,
                            23
                    );

                    calendar.set(
                            Calendar.MINUTE,
                            59
                    );

                    calendar.set(
                            Calendar.SECOND,
                            59
                    );

                    calendar.set(
                            Calendar.MILLISECOND,
                            999
                    );

                    return calendar.getTime();
                }

                return date;

            } catch (ParseException ignored) {
            }
        }

        return null;
    }

    /**
     * Strict variant of parseDateStatic: returns null instead of "now"
     * when the value is not a real display date, so sorting can keep
     * unparseable rows together instead of scattering them.
     */
    private static Date parseDisplayDateOrNull(Object value) {

        if (value == null) {
            return null;
        }

        try {

            return new SimpleDateFormat(
                    "dd MMM yyyy HH:mm",
                    Locale.ENGLISH
            ).parse(String.valueOf(value));

        } catch (Exception e) {

            return null;
        }
    }

    private static Date parseDateStatic(
            String value
    ) {

        try {

            SimpleDateFormat sdf = new SimpleDateFormat(
                    "dd MMM yyyy HH:mm",
                    Locale.ENGLISH
            );
            sdf.setLenient(false);

            return sdf.parse(value);

        } catch (Exception e) {

            // Do NOT silently turn corrupted/unparseable order data into
            // "now" - that would make an old, broken-date order suddenly
            // reappear as if it were placed today in every date-filtered
            // view. Every known reader of Order.dateValue (this file's own
            // date-range filter below, DashboardPanel, ReportsPanel,
            // RecentOrders) already treats a null dateValue as "unknown
            // date" rather than assuming non-null, so returning null here
            // is safe.
            return null;
        }
    }

    private String formatMoney(
            double amount
    ) {

        return formatMoneyStatic(amount);
    }

    // Single authoritative money-formatting rule shared by both the
    // instance-side UI code and the static Order/OrderItem model classes
    // (which cannot call the instance formatMoney above). Whole-number
    // amounts keep displaying exactly as before ("EGP 300"), but amounts
    // with real cents are no longer silently rounded away ("EGP 299.97"
    // instead of "EGP 300") — that silent rounding is what let a fully
    // returned order leave a residual in Dashboard/Reports.
    private static String formatMoneyStatic(
            double amount
    ) {

        if (Math.abs(amount - Math.round(amount)) < 0.005) {
            return String.format(
                    Locale.US,
                    "EGP %,.0f",
                    (double) Math.round(amount)
            );
        }

        return String.format(
                Locale.US,
                "EGP %,.2f",
                amount
        );
    }

    private String emptyAsDash(
            String value
    ) {

        if (value == null
                || value.trim().isEmpty()) {

            return "-";
        }

        return value;
    }

    // =========================================================
    // DATA CLASSES
    // =========================================================

    private static class ExistingProductData {

        OrderLine line;

        Map<String, Integer> allowance;

        ExistingProductData(
                OrderLine line,
                Map<String, Integer> allowance
        ) {

            this.line = line;
            this.allowance = allowance;
        }
    }

    public static class ProductItem {

        int modelRow;
        String code;
        String sku;
        String name;
        String category;
        String size;
        String color;
        double price;
        int stock;

        ProductItem(
                int modelRow,
                String code,
                String sku,
                String name,
                String category,
                String size,
                String color,
                double price,
                int stock
        ) {

            this.modelRow = modelRow;
            this.code = code;
            this.sku = sku;
            this.name = name;
            this.category = category;
            this.size = size;
            this.color = color;
            this.price = price;
            this.stock = stock;
        }

        @Override
        public String toString() {

            return name
                    + " | "
                    + size
                    + (color == null || color.isEmpty()
                            ? ""
                            : " / " + color)
                    + " | "
                    + String.format(
                    Locale.US,
                    "EGP %,.0f",
                    price
            )
                    + " | Stock: "
                    + stock;
        }
    }

    private static class OrderLine {

        ProductItem product;

        int quantity = 1;
    }
    
    private static class ProductRowUI {

        JPanel row;

        JComboBox<ProductItem> productBox;

        JSpinner spinner;

        JButton removeButton;

        OrderLine line;

        boolean updating;

        /*
         * New dynamic availability data.
         */
        boolean editMode;

        Map<String, Integer> allowance;

        String desiredProductCode;

        int desiredQuantity = 1;
    }
    
    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    public static class Order {

        String id;
        String customer;
        String phone;
        String additionalPhone;
        String address;
        String date;
        String payment;
        String orderStatus;
        String deliveryStatus;
        double shipping;
        double discount;
        String notes;

        String total = "EGP 0";

        Date dateValue;
        Date deliveredAt;

        List<OrderItem> items =
                new ArrayList<>();

        Order(
                String id,
                String customer,
                String phone,
                String additionalPhone,
                String address,
                String date,
                String payment,
                String orderStatus,
                String deliveryStatus,
                double shipping,
                double discount,
                String notes
        ) {

            this.id = id;
            this.customer = customer;
            this.phone = phone;
            this.additionalPhone = additionalPhone;
            this.address = address;
            this.date = date;
            this.payment = payment;
            this.orderStatus = orderStatus;
            this.deliveryStatus = deliveryStatus;
            this.shipping = shipping;
            this.discount = discount;
            this.notes = notes;

            this.dateValue =
                    parseDateStatic(date);
        }

        public String getId(){ return id; }
        public String getCustomer(){ return customer; }
        public String getPhone(){ return phone; }
        public String getAdditionalPhone(){ return additionalPhone; }
        public String getAddress(){ return address; }
        public String getDate(){ return date; }
        public String getPayment(){ return payment; }
        public String getOrderStatus(){ return orderStatus; }
        public String getDeliveryStatus(){ return deliveryStatus; }
        public double getShipping(){ return shipping; }
        public double getDiscount(){ return discount; }
        public String getNotes(){ return notes; }
        public String getTotal(){ return total; }
        public Date getDeliveredAt(){ return deliveredAt; }
        public List<OrderItem> getItems(){ return Collections.unmodifiableList(items); }
        public void persistenceSetTotal(String value){ total = value == null ? "EGP 0" : value; }
        public void persistenceSetDeliveredAt(Date value){ deliveredAt = value; }
        public void persistenceAddItem(OrderItem item){ if(item!=null) items.add(item); }

        public static Order persistenceCreate(String id, String customer, String phone, String additionalPhone,
                                      String address, String date, String payment, String orderStatus,
                                      String deliveryStatus, double shipping, double discount, String notes) {
            return new Order(id, customer, phone, additionalPhone, address, date, payment,
                    orderStatus, deliveryStatus, shipping, discount, notes);
        }

        void addItem(
                String name,
                String code,
                String category,
                String size,
                double price,
                int quantity
        ) {
            addItem(name, code, "", category, size, "", price, quantity);
        }

        void addItem(
                String name,
                String code,
                String sku,
                String category,
                String size,
                String color,
                double price,
                int quantity
        ) {

            // Snapshot the product's cost price at the moment of the
            // sale (not just the selling price), so that if the cost
            // price changes later, past orders keep the true profit
            // they were actually sold at instead of being recomputed
            // with today's cost.
            double costAtSale = 0;

            try {
                Product p = ProductManager.getInstance().findById(code);
                if (p != null) {
                    costAtSale = p.getCostPrice();
                }
            } catch (Exception ignored) {
            }

            items.add(
                    new OrderItem(
                            name,
                            code,
                            sku,
                            category,
                            size,
                            color,
                            price,
                            costAtSale,
                            quantity
                    )
            );
        }

        double getSubtotal() {

            double subtotal = 0;

            for (OrderItem item :
                    items) {

                subtotal +=
                        item.price
                                * item.quantity;
            }

            return subtotal;
        }

        void calculateTotal(
                double subtotal
        ) {

            double finalTotal =
                    subtotal
                            + shipping
                            - discount;

            if (finalTotal < 0) {
                finalTotal = 0;
            }

            // Keep 2-decimal precision when the total isn't a whole
            // number (e.g. 3 x 99.99 = 299.97) instead of rounding to
            // "EGP 300" here and then reconciling against the precise
            // 299.97 figure everywhere else (Returns, Dashboard,
            // Reports). This is the authoritative stored total.
            total = formatMoneyStatic(finalTotal);
        }
    }

    public static class OrderItem {

        // All fields below are a SNAPSHOT of the product as it was
        // at the moment of sale. They must never be re-derived from
        // the live Product/ProductManager afterwards — that is what
        // let past orders, invoices, and profit reports silently
        // change whenever someone edited a product later.
        String name;
        String code;
        String sku;
        String category;
        String size;
        String color;
        double price;
        double costPrice;
        int quantity;

        public String getName(){ return name; }
        public String getCode(){ return code; }
        public String getSku(){ return sku; }
        public String getCategory(){ return category; }
        public String getSize(){ return size; }
        public String getColor(){ return color; }
        public double getPrice(){ return price; }
        public double getCostPrice(){ return costPrice; }
        public int getQuantity(){ return quantity; }

        public static OrderItem persistenceCreate(String name, String code, String sku, String category,
                                              String size, String color, double price, double costPrice, int quantity) {
            return new OrderItem(name, code, sku, category, size, color, price, costPrice, quantity);
        }

        OrderItem(
                String name,
                String code,
                String category,
                String size,
                double price,
                double costPrice,
                int quantity
        ) {
            this(name, code, "", category, size, "", price, costPrice, quantity);
        }

        OrderItem(
                String name,
                String code,
                String sku,
                String category,
                String size,
                String color,
                double price,
                double costPrice,
                int quantity
        ) {

            this.name = name;
            this.code = code;
            this.sku = sku == null ? "" : sku;
            this.category = category;
            this.size = size;
            this.color = color == null ? "" : color;
            this.price = price;
            this.costPrice = costPrice;
            this.quantity = quantity;
        }
    }

    // =========================================================
    // RENDERERS
    // =========================================================

    private static class StatusRenderer
            extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {

            Component c =
                    super.getTableCellRendererComponent(
                            table,
                            value,
                            isSelected,
                            hasFocus,
                            row,
                            column
                    );

            setHorizontalAlignment(
                    SwingConstants.CENTER
            );

            if (!isSelected) {

                String status =
                        String.valueOf(value);

                if ("Completed".equals(status)) {

                    setForeground(
                            new Color(
                                    30,
                                    120,
                                    80
                            )
                    );

                } else if (
                        "Prepared".equals(status)
                ) {

                    setForeground(
                            new Color(
                                    35,
                                    105,
                                    120
                            )
                    );

                } else if (
                        "Preparing".equals(status)
                ) {

                    setForeground(
                            new Color(
                                    190,
                                    130,
                                    30
                            )
                    );

                } else {

                    setForeground(
                            new Color(
                                    130,
                                    140,
                                    140
                            )
                    );
                }
            }

            return c;
        }
    }

    private static class DeliveryRenderer
            extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {

            Component c =
                    super.getTableCellRendererComponent(
                            table,
                            value,
                            isSelected,
                            hasFocus,
                            row,
                            column
                    );

            setHorizontalAlignment(
                    SwingConstants.CENTER
            );

            if (!isSelected) {

                String status =
                        String.valueOf(value);

                if ("Delivered".equals(status)) {

                    setForeground(
                            new Color(
                                    30,
                                    125,
                                    80
                            )
                    );

                } else if (
                        "Out for Delivery"
                                .equals(status)
                ) {

                    setForeground(
                            new Color(
                                    35,
                                    100,
                                    170
                            )
                    );

                } else if (
                        "With Shipping Company"
                                .equals(status)
                ) {

                    setForeground(
                            new Color(
                                    180,
                                    125,
                                    30
                            )
                    );

                } else if (
                        "Returned".equals(status)
                ) {

                    setForeground(
                            new Color(
                                    190,
                                    65,
                                    65
                            )
                    );

                } else {

                    setForeground(
                            new Color(
                                    130,
                                    140,
                                    140
                            )
                    );
                }
            }

            return c;
        }
    }

    private static class MoneyRenderer
            extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {

            Component c =
                    super.getTableCellRendererComponent(
                            table,
                            value,
                            isSelected,
                            hasFocus,
                            row,
                            column
                    );

            setHorizontalAlignment(
                    SwingConstants.LEFT
            );

            if (value instanceof Number) {

                setText(
                        String.format(
                                Locale.US,
                                "EGP %,.0f",
                                ((Number) value)
                                        .doubleValue()
                        )
                );
            }

            return c;
        }
    }
}
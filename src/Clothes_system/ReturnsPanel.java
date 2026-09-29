package Clothes_system;

import Clothes_system.db.PersistenceRepository;
import Clothes_system.cloud.CutdownCloudReturnSyncService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import java.awt.*;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReturnsPanel extends JPanel {

    // =========================================================
    // TABLE
    // =========================================================

    private JTable returnsTable;
    private DefaultTableModel returnModel;

    // =========================================================
    // SUMMARY
    // =========================================================

    private JLabel totalReturnsValue;
    private JLabel pendingValue;
    private JLabel returnedStockValue;
    private JLabel lossValue;

    // =========================================================
    // RETURN COUNTER
    // =========================================================

    private static int returnCounter = 1;

    // =========================================================
    // SHARED RETURN RECORDS
    // =========================================================
    /*
     * Static so that opening ReturnsPanel again does not
     * immediately destroy the current return history.
     *
     * This is still in-memory only.
     */

    private static final List<ReturnRecord> returnRecords =
            new ArrayList<>();

    /*
     * Tracks how many returned units have already been
     * processed for each Order + Product.
     *
     * Example:
     *
     * Order ORD-001
     * Product Hoodie
     *
     * Returned by Item Status = 4
     * Already processed = 2
     * Available = 2
     */

    private static final Map<String, Integer> processedReturnQuantities =
            new HashMap<>();

    // =========================================================
    // COLORS
    // =========================================================

    private final Color BG =
            new Color(
                    248,
                    250,
                    249
            );

    private final Color DARK =
            new Color(
                    25,
                    45,
                    45
            );

    private final Color TEAL =
            new Color(
                    25,
                    105,
                    100
            );

    // =========================================================
    // RETURN RECORD
    // =========================================================

    private static class ReturnRecord {

        String returnId;
        String orderId;
        String customer;
        String product;
        String productCode;
        String size;
        String color;
        int quantity;
        String date;
        double amount;
        String reason;
        String disposition;
        double loss;

        ReturnRecord(
                String returnId,
                String orderId,
                String customer,
                String product,
                String productCode,
                String size,
                String color,
                int quantity,
                String date,
                double amount,
                String reason,
                String disposition,
                double loss
        ) {

            this.returnId = returnId;
            this.orderId = orderId;
            this.customer = customer;
            this.product = product;
            this.productCode = productCode;
            this.size = size == null ? "" : size;
            this.color = color == null ? "" : color;
            this.quantity = quantity;
            this.date = date;
            this.amount = amount;
            this.reason = reason;
            this.disposition = disposition;
            this.loss = loss;
        }
    }

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ReturnsPanel() {

        setBackground(BG);

        setLayout(
                new BorderLayout()
        );

        // =====================================================
        // HEADER
        // =====================================================

        JPanel header =
                new JPanel(
                        new BorderLayout()
                );

        header.setBackground(BG);

        header.setBorder(
                new EmptyBorder(
                        20,
                        30,
                        15,
                        30
                )
        );

        JPanel titleBox =
                new JPanel();

        titleBox.setBackground(BG);

        titleBox.setLayout(
                new BoxLayout(
                        titleBox,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel title =
                new JLabel(
                        "Returns"
                );

        title.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        24
                )
        );

        title.setForeground(DARK);

        JLabel subtitle =
                new JLabel(
                        "Manage returned products, reasons, stock recovery and losses"
                );

        subtitle.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        12
                )
        );

        subtitle.setForeground(
                new Color(
                        140,
                        150,
                        150
                )
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



        add(
                header,
                BorderLayout.NORTH
        );

        // =====================================================
        // CONTENT
        // =====================================================

        JPanel content =
                new JPanel(
                        new BorderLayout()
                );

        content.setBackground(BG);

        content.setBorder(
                new EmptyBorder(
                        0,
                        30,
                        30,
                        30
                )
        );

        // =====================================================
        // SUMMARY
        // =====================================================

        JPanel summaryPanel =
                new JPanel(
                        new GridLayout(
                                1,
                                4,
                                15,
                                0
                        )
                );

        summaryPanel.setBackground(BG);

        summaryPanel.setPreferredSize(
                new Dimension(
                        0,
                        105
                )
        );

        JPanel totalCard =
                createSummaryCard(
                        "Total Returns",
                        "0",
                        new Color(
                                25,
                                115,
                                105
                        )
                );

        JPanel pendingCard =
                createSummaryCard(
                        "Pending",
                        "0",
                        new Color(
                                210,
                                145,
                                45
                        )
                );

        JPanel stockCard =
                createSummaryCard(
                        "Returned to Stock",
                        "0",
                        new Color(
                                55,
                                130,
                                85
                        )
                );

        JPanel lossCard =
                createSummaryCard(
                        "Total Loss",
                        "EGP 0",
                        new Color(
                                190,
                                75,
                                65
                        )
                );

        totalReturnsValue =
                (JLabel)
                        totalCard.getClientProperty(
                                "valueLabel"
                        );

        pendingValue =
                (JLabel)
                        pendingCard.getClientProperty(
                                "valueLabel"
                        );

        returnedStockValue =
                (JLabel)
                        stockCard.getClientProperty(
                                "valueLabel"
                        );

        lossValue =
                (JLabel)
                        lossCard.getClientProperty(
                                "valueLabel"
                        );

        summaryPanel.add(totalCard);
        summaryPanel.add(pendingCard);
        summaryPanel.add(stockCard);
        summaryPanel.add(lossCard);

        content.add(
                summaryPanel,
                BorderLayout.NORTH
        );

        // =====================================================
        // TABLE
        // =====================================================

        String[] columns = {
                "Return ID",
                "Order ID",
                "Customer",
                "Product",
                "Variant",
                "Qty",
                "Date",
                "Amount",
                "Reason",
                "Disposition",
                "Loss"
        };

        returnModel =
                new DefaultTableModel(
                        columns,
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

        returnsTable =
                new JTable(
                        returnModel
                );

        returnsTable.setRowHeight(48);

        returnsTable.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        12
                )
        );

        returnsTable.setForeground(
                new Color(
                        50,
                        65,
                        65
                )
        );

        returnsTable.setGridColor(
                new Color(
                        235,
                        240,
                        240
                )
        );

        returnsTable.setSelectionBackground(
                new Color(
                        230,
                        243,
                        241
                )
        );

        returnsTable.setSelectionForeground(
                new Color(
                        25,
                        80,
                        75
                )
        );

        returnsTable.getTableHeader()
                .setFont(
                        new Font(
                                "Arial",
                                Font.BOLD,
                                12
                        )
                );

        returnsTable.getTableHeader()
                .setForeground(
                        new Color(
                                70,
                                85,
                                85
                        )
                );

        returnsTable.getTableHeader()
                .setBackground(
                        new Color(
                                245,
                                248,
                                248
                        )
                );

        returnsTable.getTableHeader()
                .setPreferredSize(
                        new Dimension(
                                0,
                                42
                        )
                );

        // =====================================================
        // COLUMN WIDTHS
        // =====================================================

        int[] widths = {
                85,
                85,
                120,
                150,
                55,
                100,
                100,
                120,
                145,
                95
        };

        for (
                int i = 0;
                i < widths.length;
                i++
        ) {

            returnsTable
                    .getColumnModel()
                    .getColumn(i)
                    .setPreferredWidth(
                            widths[i]
                    );
        }

        returnsTable
                .getColumnModel()
                .getColumn(8)
                .setCellRenderer(
                        new DispositionRenderer()
                );

        // =====================================================
        // DOUBLE CLICK DETAILS
        // =====================================================

        returnsTable.addMouseListener(
                new java.awt.event.MouseAdapter() {

                    @Override
                    public void mouseClicked(
                            java.awt.event.MouseEvent e
                    ) {

                        if (
                                e.getClickCount() == 2
                                        && SwingUtilities
                                        .isLeftMouseButton(e)
                        ) {

                            int row =
                                    returnsTable
                                            .getSelectedRow();

                            if (row >= 0) {

                                showReturnDetails(row);
                            }
                        }
                    }
                }
        );

        // =====================================================
        // SCROLL
        // =====================================================

        JScrollPane scrollPane =
                new JScrollPane(
                        returnsTable
                );

        scrollPane.setBorder(
                BorderFactory.createLineBorder(
                        new Color(
                                225,
                                232,
                                232
                        )
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
                new EmptyBorder(
                        20,
                        0,
                        0,
                        0
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

        // Returns are persisted data, not UI-only state. The application
        // loads them before the table is first rendered; this also rebuilds
        // processedReturnQuantities from the persisted records.
        loadFromPersistence();

        refreshReturnTable();

        updateSummary();
    }

    // =========================================================
    // REFRESH RETURN TABLE
    // =========================================================

    public static synchronized void clearPersistenceData() {
        returnRecords.clear();
        processedReturnQuantities.clear();
        returnCounter = 1;
    }

    public static synchronized void addPersistedReturn(String id, String orderId, String customer,
                                                        String product, String productId, String size, String color,
                                                        int quantity, String date, double amount, String reason,
                                                        String disposition, double loss) {
        ReturnRecord r = new ReturnRecord(id, orderId, customer, product, productId, size, color,
                quantity, date, amount, reason, disposition, loss);
        returnRecords.add(r);
        String key = String.valueOf(orderId)+"||"+String.valueOf(productId)+"||"+String.valueOf(product)+"||"+
                String.valueOf(size)+"||"+String.valueOf(color);
        processedReturnQuantities.put(key, processedReturnQuantities.getOrDefault(key,0)+Math.max(0,quantity));
        try { if(id != null && id.startsWith("RET-")) returnCounter=Math.max(returnCounter,Integer.parseInt(id.substring(4))+1); }
        catch(Exception ignored){}
    }

    public static synchronized void loadFromPersistence() {
        if (!returnRecords.isEmpty()) return;
        PersistenceRepository.loadReturns();
    }

    public record ReturnSnapshot(String id, String orderId, String customer, String product, String productId,
                                  String size, String color, int quantity, String date, double amount,
                                  String reason, String disposition, double loss) {}

    /**
     * Reconcile order delivery state after persisted return records have
     * been loaded. This is intentionally based only on persisted return
     * accounting, so an old database cannot make a returned order appear as
     * a normal Delivered order simply because its delivery_status column was
     * stale.
     */
    public static synchronized void reconcileLoadedOrderStatuses() {
        for (OrdersPanel.Order order : OrdersPanel.getOrders()) {
            if (order != null && hasProcessedReturn(order.id)) {
                syncOrderReturnStatus(order);
            }
        }
    }

    public static synchronized List<ReturnSnapshot> getReturnSnapshotsForOrder(String orderId) {
        List<ReturnSnapshot> out = new ArrayList<>();
        if (orderId == null) return out;
        for (ReturnRecord r : returnRecords) {
            if (r != null && orderId.equals(r.orderId)) {
                out.add(new ReturnSnapshot(r.returnId, r.orderId, r.customer, r.product, r.productCode,
                        r.size, r.color, r.quantity, r.date, r.amount, r.reason, r.disposition, r.loss));
            }
        }
        return out;
    }

    public static synchronized List<ReturnSnapshot> getReturnSnapshots() {
        List<ReturnSnapshot> out = new ArrayList<>();
        for (ReturnRecord r : returnRecords)
            out.add(new ReturnSnapshot(r.returnId,r.orderId,r.customer,r.product,r.productCode,r.size,r.color,
                    r.quantity,r.date,r.amount,r.reason,r.disposition,r.loss));
        return out;
    }



    public void refreshData() {
        refreshReturnTable();
        updateSummary();
    }

    private void refreshReturnTable() {

        if (returnModel == null) {
            return;
        }

        returnModel.setRowCount(0);

        for (
                ReturnRecord record :
                returnRecords
        ) {

            returnModel.addRow(
                    new Object[]{
                            record.returnId,
                            record.orderId,
                            record.customer,
                            record.product,
                            record.size
                                    + (record.color == null || record.color.isEmpty()
                                            ? ""
                                            : " / " + record.color),
                            record.quantity,
                            record.date,
                            "EGP "
                                    + formatMoney(
                                    record.amount
                            ),
                            record.reason,
                            record.disposition,
                            "EGP "
                                    + formatMoney(
                                    record.loss
                            )
                    }
            );
        }
    }

    // =========================================================
    // NEW RETURN DIALOG
    // =========================================================

    private void showNewReturnDialog() {

        /*
         * Returns are intentionally whole-order only.
         *
         * The previous dialog allowed selecting a product and quantity,
         * which created a partial-return workflow with extra accounting
         * and stock edge cases. The Returns screen keeps the same main
         * layout/table; creating a return now means returning the entire
         * eligible order through the single, existing whole-order path.
         */

        List<OrdersPanel.Order> availableOrders =
                getOrdersWithAvailableReturns();

        if (availableOrders == null || availableOrders.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "There are no returnable orders available.\\n\\n"
                            + "Cancelled or already returned orders cannot be processed.",
                    "No Returns Available",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        JComboBox<String> orderCombo = new JComboBox<>();
        orderCombo.setFont(new Font("Arial", Font.PLAIN, 12));
        orderCombo.setBackground(Color.WHITE);

        for (OrdersPanel.Order order : availableOrders) {
            orderCombo.addItem(
                    order.id + " - " + order.customer
                            + " - " + order.items.size() + " item(s)"
            );
        }

        JTextArea details = new JTextArea();
        details.setEditable(false);
        details.setOpaque(false);
        details.setFont(new Font("Arial", Font.PLAIN, 12));
        details.setForeground(new Color(80, 95, 95));
        details.setLineWrap(true);
        details.setWrapStyleWord(true);

        Runnable refreshDetails = () -> {
            int index = orderCombo.getSelectedIndex();
            if (index < 0 || index >= availableOrders.size()) {
                details.setText("");
                return;
            }

            OrdersPanel.Order order = availableOrders.get(index);
            StringBuilder text = new StringBuilder();
            text.append("Customer: ").append(order.customer).append("\\n");
            text.append("Phone: ").append(order.phone).append("\\n");
            text.append("Items: ").append(order.items.size()).append("\\n\\n");
            text.append("This action returns the entire order.\\n");
            text.append("All remaining quantities will be processed together.");
            details.setText(text.toString());
        };

        orderCombo.addActionListener(e -> refreshDetails.run());
        refreshDetails.run();

        JPanel form = new JPanel(new BorderLayout(0, 10));
        form.setBackground(Color.WHITE);

        JPanel selector = new JPanel(new BorderLayout(0, 6));
        selector.setBackground(Color.WHITE);

        JLabel label = new JLabel("Returned Order");
        label.setFont(new Font("Arial", Font.BOLD, 12));
        label.setForeground(DARK);

        selector.add(label, BorderLayout.NORTH);
        selector.add(orderCombo, BorderLayout.CENTER);

        form.add(selector, BorderLayout.NORTH);
        form.add(details, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
                this,
                form,
                "Create New Return",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        int index = orderCombo.getSelectedIndex();
        if (index < 0 || index >= availableOrders.size()) {
            return;
        }

        /*
         * Delegate the actual mutation to the existing whole-order
         * implementation so stock restoration, return records,
         * customer totals, order status and persistence remain in one
         * authoritative path.
         */
        processWholeOrderReturn(availableOrders.get(index), this);
    }

    /**
     * Determines whether an order can enter the return workflow.
     *
     * The application has two independent status fields: orderStatus
     * describes preparation/completion, while deliveryStatus describes
     * the delivery lifecycle. Return eligibility must not confuse either
     * field with the other, and it must not infer "Delivered" from a
     * non-delivery status such as Preparing.
     *
     * Returns are a stock/accounting operation in this application, so
     * they are valid for active orders as well as delivered orders. The
     * actual guard against an invalid duplicate return is the remaining
     * returnable quantity check. Cancelled orders and orders with no
     * items are never eligible.
     */
    public static synchronized boolean isOrderReturnEligible(OrdersPanel.Order order) {
        if (order == null || order.items == null || order.items.isEmpty()) return false;

        String orderStatus = normalizeStatus(order.orderStatus);
        String deliveryStatus = normalizeStatus(order.deliveryStatus);

        // A cancelled order is not a return transaction.
        if ("cancelled".equals(orderStatus) || "cancelled".equals(deliveryStatus)) return false;

        // Fully returned orders are never eligible again.
        if (getRemainingReturnableUnits(order) <= 0) return false;

        // Do not manufacture or reinterpret status values here.
        // Preparing / Prepared / Completed and the delivery states
        // (including Out for Delivery / Delivered) are all legitimate
        // lifecycle states for an order that still has returnable units.
        return true;
    }

    private static String normalizeStatus(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT);
    }

    // =========================================================
    // GET ORDERS WITH AVAILABLE RETURNS
    // =========================================================

    private List<OrdersPanel.Order>
    getOrdersWithAvailableReturns() {

        List<OrdersPanel.Order> result =
                new ArrayList<>();

        if (
                OrdersPanel.orders == null
        ) {

            return result;
        }

        for (
                OrdersPanel.Order order :
                OrdersPanel.orders
        ) {

            if (order == null || order.items == null) continue;
            if (!isOrderReturnEligible(order)) continue;

            boolean hasAvailable = false;

            for (
                    OrdersPanel.OrderItem item :
                    order.items
            ) {

                if (
                        getAvailableReturnQuantity(
                                order,
                                item
                        ) > 0
                ) {

                    hasAvailable = true;

                    break;
                }
            }

            if (hasAvailable) {

                result.add(
                        order
                );
            }
        }

        return result;
    }

    // =========================================================
    // GET AVAILABLE RETURN ITEMS
    // =========================================================

    private List<OrdersPanel.OrderItem>
    getAvailableReturnItems(
            OrdersPanel.Order order
    ) {

        List<OrdersPanel.OrderItem> result =
                new ArrayList<>();

        if (
                order == null
                        || order.items == null
        ) {

            return result;
        }

        for (
                OrdersPanel.OrderItem item :
                order.items
        ) {

            if (
                    getAvailableReturnQuantity(
                            order,
                            item
                    ) > 0
            ) {

                result.add(
                        item
                );
            }
        }

        return result;
    }

    // =========================================================
    // GET AVAILABLE RETURN QUANTITY
    // =========================================================

    private int getAvailableReturnQuantity(
            OrdersPanel.Order order,
            OrdersPanel.OrderItem item
    ) {

        if (
                order == null
                        || item == null
        ) {

            return 0;
        }

        if (!isOrderReturnEligible(order)) return 0;

        // A delivered item can be returned even though the order itself
        // has not previously been marked Returned. The Returns screen is
        // the action that starts the return.
        //
        // The processed-quantity counter is keyed by order + product +
        // size + colour, so two identical lines in the same order share
        // one counter. Availability must therefore be measured against
        // the COMBINED ordered quantity for that key, not against this
        // single line, otherwise returning one line silently consumes
        // its identical sibling.
        int returnableQuantity = getOrderedQuantityForKey(order, item);
        int processed = getProcessedReturnQuantity(order, item);
        return Math.max(0, returnableQuantity - processed);
    }

    /**
     * Total quantity ordered across every line of this order that maps
     * to the same return key as the given item.
     */
    private int getOrderedQuantityForKey(
            OrdersPanel.Order order,
            OrdersPanel.OrderItem item
    ) {

        if (order == null || order.items == null || item == null) {
            return 0;
        }

        String key = createReturnKey(order, item);

        int total = 0;

        for (OrdersPanel.OrderItem other : order.items) {

            if (other == null) {
                continue;
            }

            if (key.equals(createReturnKey(order, other))) {
                total += Math.max(0, other.quantity);
            }
        }

        return total;
    }

    // =========================================================
    // READ ITEM RETURNED QUANTITY
    // =========================================================

    private int getItemReturnedQuantity(
            OrdersPanel.OrderItem item
    ) {

        if (item == null) {
            return 0;
        }

        /*
         * We use reflection here intentionally.
         *
         * This makes ReturnsPanel compatible with both:
         *
         * 1. Old OrdersPanel
         * 2. New OrdersPanel containing:
         *      getReturnedQuantity()
         *
         * So ReturnsPanel itself does not force a compile
         * dependency on a method that may not exist yet.
         */

        try {

            Method method =
                    item.getClass()
                            .getMethod(
                                    "getReturnedQuantity"
                            );

            Object result =
                    method.invoke(
                            item
                    );

            if (
                    result instanceof Number
            ) {

                return Math.max(
                        0,
                        ((Number) result)
                                .intValue()
                );
            }

        } catch (Exception ignored) {
        }

        /*
         * Second fallback:
         *
         * Try reading statusRecords directly.
         *
         * This works when the new OrderItem has the
         * statusRecords field but method visibility differs.
         */

        try {

            java.lang.reflect.Field field =
                    item.getClass()
                            .getDeclaredField(
                                    "statusRecords"
                            );

            field.setAccessible(
                    true
            );

            Object value =
                    field.get(
                            item
                    );

            if (
                    value instanceof List
            ) {

                int total = 0;

                List<?> records =
                        (List<?>) value;

                for (
                        Object record :
                        records
                ) {

                    if (record == null) {
                        continue;
                    }

                    java.lang.reflect.Field statusField =
                            record.getClass()
                                    .getDeclaredField(
                                            "status"
                                    );

                    java.lang.reflect.Field quantityField =
                            record.getClass()
                                    .getDeclaredField(
                                            "quantity"
                                    );

                    statusField.setAccessible(
                            true
                    );

                    quantityField.setAccessible(
                            true
                    );

                    Object status =
                            statusField.get(
                                    record
                            );

                    Object quantity =
                            quantityField.get(
                                    record
                            );

                    if (
                            "Returned".equals(
                                    String.valueOf(
                                            status
                                    )
                            )
                    ) {

                        if (
                                quantity instanceof Number
                        ) {

                            total +=
                                    Math.max(
                                            0,
                                            ((Number)
                                                    quantity)
                                                    .intValue()
                                    );
                        }
                    }
                }

                return total;
            }

        } catch (Exception ignored) {
        }

        return 0;
    }

    // =========================================================
    // CHECK STATUS HISTORY
    // =========================================================

    private boolean hasNoItemStatusHistory(
            OrdersPanel.OrderItem item
    ) {

        if (item == null) {
            return true;
        }

        try {

            java.lang.reflect.Field field =
                    item.getClass()
                            .getDeclaredField(
                                    "statusRecords"
                            );

            field.setAccessible(
                    true
            );

            Object value =
                    field.get(
                            item
                    );

            if (
                    value instanceof List
            ) {

                return ((List<?>) value)
                        .isEmpty();
            }

        } catch (Exception ignored) {
        }

        return true;
    }

    // =========================================================
    // PROCESSED RETURN QUANTITY
    // =========================================================

    private int getProcessedReturnQuantity(
            OrdersPanel.Order order,
            OrdersPanel.OrderItem item
    ) {

        String key =
                createReturnKey(
                        order,
                        item
                );

        return processedReturnQuantities
                .getOrDefault(
                        key,
                        0
                );
    }

    // =========================================================
    // RETURN KEY
    // =========================================================

    private String createReturnKey(
            OrdersPanel.Order order,
            OrdersPanel.OrderItem item
    ) {

        String orderId =
                order == null
                        ? ""
                        : String.valueOf(
                                order.id
                        );

        String code =
                item == null
                        ? ""
                        : String.valueOf(
                                item.code
                        );

        String name =
                item == null
                        ? ""
                        : String.valueOf(
                                item.name
                        );

        String size =
                item == null
                        ? ""
                        : String.valueOf(
                                item.size
                        );

        String color =
                item == null
                        ? ""
                        : String.valueOf(
                                item.color
                        );

        return orderId
                + "||"
                + code
                + "||"
                + name
                + "||"
                + size
                + "||"
                + color;
    }

    // =========================================================
    // UPDATE QUANTITY MODEL
    // =========================================================

    private void updateQuantityModel(
            OrdersPanel.Order order,
            JComboBox<String> productCombo,
            JSpinner quantitySpinner
    ) {

        List<OrdersPanel.OrderItem>
                availableItems =
                getAvailableReturnItems(
                        order
                );

        int index =
                productCombo.getSelectedIndex();

        if (
                index < 0
                        || index
                        >= availableItems.size()
        ) {

            quantitySpinner.setModel(
                    new SpinnerNumberModel(
                            1,
                            1,
                            1,
                            1
                    )
            );

            return;
        }

        OrdersPanel.OrderItem item =
                availableItems.get(
                        index
                );

        int max =
                Math.max(
                        1,
                        getAvailableReturnQuantity(
                                order,
                                item
                        )
                );

        quantitySpinner.setModel(
                new SpinnerNumberModel(
                        1,
                        1,
                        max,
                        1
                )
        );
    }

    // =========================================================
    // UPDATE AMOUNTS
    // =========================================================

    private void updateReturnAmounts(
            OrdersPanel.Order order,
            JComboBox<String> productCombo,
            JSpinner quantitySpinner,
            JComboBox<String> dispositionCombo,
            JLabel amountLabel,
            JLabel lossLabel
    ) {

        List<OrdersPanel.OrderItem>
                availableItems =
                getAvailableReturnItems(
                        order
                );

        int index =
                productCombo.getSelectedIndex();

        if (
                index < 0
                        || index
                        >= availableItems.size()
        ) {

            amountLabel.setText(
                    "EGP 0"
            );

            lossLabel.setText(
                    "EGP 0"
            );

            return;
        }

        OrdersPanel.OrderItem item =
                availableItems.get(
                        index
                );

        int quantity =
                ((Number)
                        quantitySpinner.getValue())
                        .intValue();

        int maxAvailable =
                getAvailableReturnQuantity(
                        order,
                        item
                );

        if (
                quantity > maxAvailable
        ) {

            quantity =
                    Math.max(
                            1,
                            maxAvailable
                    );

            quantitySpinner.setValue(
                    quantity
            );
        }

        double amount =
                computeReturnAmount(
                        order,
                        item,
                        quantity
                );

        amountLabel.setText(
                "EGP "
                        + formatMoney(
                        amount
                )
        );

        if (
                "Scrap / Damaged"
                        .equals(
                                dispositionCombo
                                        .getSelectedItem()
                        )
        ) {

            lossLabel.setText(
                    "EGP "
                            + formatMoney(
                            amount
                    )
            );

        } else {

            lossLabel.setText(
                    "EGP 0"
            );
        }
    }

    // =========================================================
    // FORM ROW
    // =========================================================

    private void addFormRow(
            JPanel panel,
            GridBagConstraints gbc,
            int row,
            String labelText,
            Component component
    ) {

        gbc.gridx = 0;

        gbc.gridy = row;

        gbc.weightx = 0;

        gbc.fill =
                GridBagConstraints.HORIZONTAL;

        gbc.insets =
                new Insets(
                        8,
                        0,
                        8,
                        10
                );

        JLabel label =
                new JLabel(
                        labelText
                );

        label.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        12
                )
        );

        label.setForeground(
                new Color(
                        75,
                        90,
                        90
                )
        );

        panel.add(
                label,
                gbc
        );

        gbc.gridx = 1;

        gbc.weightx = 1;

        gbc.insets =
                new Insets(
                        8,
                        0,
                        8,
                        0
                );

        panel.add(
                component,
                gbc
        );

        gbc.insets =
                new Insets(
                        8,
                        0,
                        8,
                        10
                );
    }

    // =========================================================
    // RETURN DETAILS
    // =========================================================

    private void showReturnDetails(
            int row
    ) {

        if (
                row < 0
                        || row >= returnModel
                        .getRowCount()
        ) {

            return;
        }

        String returnId =
                String.valueOf(
                        returnModel.getValueAt(
                                row,
                                0
                        )
                );

        String orderId =
                String.valueOf(
                        returnModel.getValueAt(
                                row,
                                1
                        )
                );

        String customer =
                String.valueOf(
                        returnModel.getValueAt(
                                row,
                                2
                        )
                );

        String product =
                String.valueOf(
                        returnModel.getValueAt(
                                row,
                                3
                        )
                );

        String quantity =
                String.valueOf(
                        returnModel.getValueAt(
                                row,
                                4
                        )
                );

        String date =
                String.valueOf(
                        returnModel.getValueAt(
                                row,
                                5
                        )
                );

        String amount =
                String.valueOf(
                        returnModel.getValueAt(
                                row,
                                6
                        )
                );

        String reason =
                String.valueOf(
                        returnModel.getValueAt(
                                row,
                                7
                        )
                );

        String disposition =
                String.valueOf(
                        returnModel.getValueAt(
                                row,
                                8
                        )
                );

        String loss =
                String.valueOf(
                        returnModel.getValueAt(
                                row,
                                9
                        )
                );

        String message =
                "Return ID: "
                        + returnId
                        + "\nOrder ID: "
                        + orderId
                        + "\nCustomer: "
                        + customer
                        + "\nProduct: "
                        + product
                        + "\nQuantity: "
                        + quantity
                        + "\nDate: "
                        + date
                        + "\nRefund Amount: "
                        + amount
                        + "\nReason: "
                        + reason
                        + "\nCondition: "
                        + disposition
                        + "\nLoss: "
                        + loss;

        JOptionPane.showMessageDialog(
                this,
                message,
                "Return Details",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

 // =========================================================
 // SUMMARY
 // =========================================================

 private void updateSummary() {
     int totalReturns = 0;
     int pending = 0;
     int returnedToStock = 0;
     double totalLoss = 0;

     for (ReturnRecord r : returnRecords) {
         if (r == null) continue;
         totalReturns += r.quantity;
         if ("Return to Stock".equals(r.disposition)) returnedToStock += r.quantity;
         if ("Scrap / Damaged".equals(r.disposition)) totalLoss += r.loss;
     }

     // Pending = a delivered order that has been partially/fully marked
     // for return but still has units not processed by Returns.
     for (OrdersPanel.Order order : OrdersPanel.orders) {
         if (order == null || order.items == null) continue;
         if (!"Returned".equals(order.deliveryStatus)) continue;
         for (OrdersPanel.OrderItem item : order.items) {
             if (item == null) continue;
             pending += Math.max(0, item.quantity - getProcessedReturnQuantity(order, item));
         }
     }

     if (totalReturnsValue != null) totalReturnsValue.setText(String.valueOf(totalReturns));
     if (pendingValue != null) pendingValue.setText(String.valueOf(pending));
     if (returnedStockValue != null) returnedStockValue.setText(String.valueOf(returnedToStock));
     if (lossValue != null) lossValue.setText("EGP " + formatMoney(totalLoss));
 }

    public static synchronized void processWholeOrderReturn(OrdersPanel.Order order, Component parent) {
        if (order == null || order.items == null || order.items.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Order is missing or has no items.", "Whole Return", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!isOrderReturnEligible(order)) {
            JOptionPane.showMessageDialog(parent,
                    "This order cannot be returned because it has no returnable quantities or is already fully returned.",
                    "Whole Return", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<OrdersPanel.OrderItem> remaining = new ArrayList<>();
        java.util.Set<String> seenKeys = new java.util.HashSet<>();
        for (OrdersPanel.OrderItem item : order.items) {
            // Identical lines share one return counter, so collapse them
            // to a single entry measured against the combined quantity.
            String k = createReturnKeyStatic(order, item);
            if (!seenKeys.add(k)) continue;
            int qty = Math.max(0, getOrderedQuantityForKeyStatic(order, item)
                    - getProcessedReturnQuantityStatic(order, item));
            if (qty > 0) remaining.add(item);
        }
        if (remaining.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "All items in this order have already been returned.", "Whole Return", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JComboBox<String> disposition = new JComboBox<>(new String[]{"Return to Stock", "Scrap / Damaged"});
        JComboBox<String> reason = new JComboBox<>(new String[]{"Customer Return", "Wrong Size", "Wrong Color", "Defective", "Damaged", "Wrong Product", "Other"});
        JPanel form = new JPanel(new GridLayout(2,2,8,8));
        form.add(new JLabel("Reason")); form.add(reason); form.add(new JLabel("Decision")); form.add(disposition);
        int ok = JOptionPane.showConfirmDialog(parent, form, "Return Entire Order - " + order.id, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        String disp = String.valueOf(disposition.getSelectedItem());
        String why = String.valueOf(reason.getSelectedItem());
        for (OrdersPanel.OrderItem item : remaining) {
            int qty = getOrderedQuantityForKeyStatic(order, item)
                    - getProcessedReturnQuantityStatic(order, item);
            double amount = computeReturnAmount(order, item, qty);
            double loss = "Scrap / Damaged".equals(disp) ? amount : 0;
            if ("Return to Stock".equals(disp)) {
                Product product = ProductManager.getInstance().findById(item.code);
                if (product != null) product.addStock(qty, "Whole order return - restocked");
            }
            String key = createReturnKeyStatic(order, item);
            processedReturnQuantities.put(key, processedReturnQuantities.getOrDefault(key,0) + qty);
            returnRecords.add(new ReturnRecord(String.format("RET-%04d", returnCounter++), order.id, order.customer, item.name, item.code, item.size, item.color, qty, new SimpleDateFormat("dd MMM yyyy HH:mm").format(new Date()), amount, why, disp, loss));
            CustomersPanel.recordReturnForCustomer(order.phone, amount);
        }
        syncOrderReturnStatus(order);
        // Whole-order returns are mutations too; make them durable before
        // repainting the screens so a restart cannot lose the return.
        PersistenceRepository.saveAll();
        CutdownCloudReturnSyncService.syncReturnAsync(order);
        Clothes_system.refreshAllDataViews();
        JOptionPane.showMessageDialog(parent, "Entire order " + order.id + " was returned successfully.", "Whole Return", JOptionPane.INFORMATION_MESSAGE);
    }

    // =========================================================
    // RETURN VALUE (ORDER-LEVEL DISCOUNT AWARE)
    // =========================================================
    /*
     * A return refunds what the customer was actually charged for the
     * goods, not the sum of list prices. An order-level discount is
     * therefore spread across the lines in proportion to each line's
     * share of the subtotal:
     *
     *     subtotal 1000, discount 200
     *     line A 600 -> 480
     *     line B 400 -> 320
     *
     * Shipping is deliberately NOT refunded; it is a carriage charge,
     * not goods value. So a fully returned order gives back exactly
     *
     *     max(0, subtotal - discount)
     *
     * Because per-line rounding can drift by a cent or two, the return
     * that finally empties the order is booked as the exact remainder
     * instead of a freshly rounded figure. That guarantees the running
     * total of an order's returns lands exactly on its net goods value,
     * whether it got there in one return or in five.
     */

    static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** Net goods value of an order: subtotal less the order discount. */
    public static double getNetGoodsValue(OrdersPanel.Order order) {

        if (order == null) {
            return 0;
        }

        return round2(Math.max(0, order.getSubtotal() - order.discount));
    }

    /** Units of this order that have not yet been returned. */
    public static int getRemainingReturnableUnits(OrdersPanel.Order order) {

        if (order == null || order.items == null) {
            return 0;
        }

        // Identical lines share one return counter, so count each key once.
        java.util.Set<String> seen = new java.util.HashSet<>();

        int remaining = 0;

        for (OrdersPanel.OrderItem item : order.items) {

            if (item == null) {
                continue;
            }

            String key = createReturnKeyStatic(order, item);

            if (!seen.add(key)) {
                continue;
            }

            remaining += Math.max(0,
                    getOrderedQuantityForKeyStatic(order, item)
                            - getProcessedReturnQuantityStatic(order, item));
        }

        return remaining;
    }

    /**
     * Amount to refund for returning {@code quantity} units of
     * {@code item}, with the order-level discount applied pro rata.
     */
    public static synchronized double computeReturnAmount(
            OrdersPanel.Order order,
            OrdersPanel.OrderItem item,
            int quantity
    ) {

        if (order == null || item == null || quantity <= 0) {
            return 0;
        }

        double subtotal = order.getSubtotal();

        if (subtotal <= 0) {
            return 0;
        }

        double net = getNetGoodsValue(order);

        // Does this return empty the order? If so, book the exact
        // remainder so accumulated returns equal the net order value.
        int remainingAfter = getRemainingReturnableUnits(order) - quantity;

        if (remainingAfter <= 0) {

            double already = getReturnedSalesAmountForOrder(order.id);

            return round2(Math.max(0, net - already));
        }

        return round2(item.price * quantity * (net / subtotal));
    }

    /**
     * Brings the order's delivery status back in line with its return
     * accounting. Single source of truth for both the partial-return
     * and whole-order-return paths.
     */
    public static synchronized void syncOrderReturnStatus(OrdersPanel.Order order) {

        if (order == null) {
            return;
        }

        if (getRemainingReturnableUnits(order) == 0) {

            order.deliveryStatus = "Returned";
            order.orderStatus = "Not Prepared";
            order.deliveredAt = null;

        }
    }

    /** Returned units per product code, for order-edit validation. */
    public static synchronized java.util.Map<String, Integer>
    getReturnedUnitsByProduct(OrdersPanel.Order order) {

        java.util.Map<String, Integer> result = new HashMap<>();

        if (order == null || order.items == null) {
            return result;
        }

        java.util.Set<String> seen = new java.util.HashSet<>();

        for (OrdersPanel.OrderItem item : order.items) {

            if (item == null) {
                continue;
            }

            String key = createReturnKeyStatic(order, item);

            if (!seen.add(key)) {
                continue;
            }

            int processed = getProcessedReturnQuantityStatic(order, item);

            if (processed <= 0) {
                continue;
            }

            String code = String.valueOf(item.code);

            result.put(code, result.getOrDefault(code, 0) + processed);
        }

        return result;
    }

    private static String createReturnKeyStatic(OrdersPanel.Order order, OrdersPanel.OrderItem item) {
        return String.valueOf(order.id)+"||"+String.valueOf(item.code)+"||"+String.valueOf(item.name)+"||"+String.valueOf(item.size)+"||"+String.valueOf(item.color);
    }
    private static int getProcessedReturnQuantityStatic(OrdersPanel.Order order, OrdersPanel.OrderItem item) {
        return processedReturnQuantities.getOrDefault(createReturnKeyStatic(order,item),0);
    }

    /**
     * Total quantity ordered across every line of this order that shares
     * the given item's return key. Mirrors getOrderedQuantityForKey().
     */
    private static int getOrderedQuantityForKeyStatic(OrdersPanel.Order order, OrdersPanel.OrderItem item) {
        if (order == null || order.items == null || item == null) return 0;
        String key = createReturnKeyStatic(order, item);
        int total = 0;
        for (OrdersPanel.OrderItem other : order.items) {
            if (other == null) continue;
            if (key.equals(createReturnKeyStatic(order, other))) total += Math.max(0, other.quantity);
        }
        return total;
    }

    /**
     * Returns true only when a real return record has been processed
     * for the given order. A delivery status of "Returned" alone is
     * not enough to make an order appear as actually returned.
     */
    public static boolean hasProcessedReturn(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) return false;

        for (ReturnRecord record : returnRecords) {
            if (record != null && orderId.equals(record.orderId)
                    && record.quantity > 0) {
                return true;
            }
        }
        return false;
    }

    // =========================================================
    // SUMMARY CARD
    // =========================================================

    public static synchronized double getReturnedSalesAmountForOrder(String orderId) {
        double total=0;
        for(ReturnRecord r:returnRecords) if(r!=null && String.valueOf(orderId).equals(r.orderId)) total+=r.amount;
        return total;
    }

    public static synchronized double getReturnedCostAmountForOrder(String orderId) {
        double total=0;
        if(orderId==null) return 0;
        for(ReturnRecord r:returnRecords) {
            if(r==null || !orderId.equals(r.orderId) || !"Return to Stock".equals(r.disposition)) continue;
            for(OrdersPanel.Order o:OrdersPanel.getOrders()) if(o!=null && orderId.equals(o.id)) for(OrdersPanel.OrderItem i:o.items) {
                if(i!=null && i.code.equals(r.productCode) && java.util.Objects.equals(i.size,r.size) && java.util.Objects.equals(i.color,r.color)) { total += i.costPrice*r.quantity; break; }
            }
        }
        return total;
    }

    public static synchronized int getReturnCountBetween(Date from, Date to) {
        int n=0; SimpleDateFormat f=new SimpleDateFormat("dd MMM yyyy HH:mm",Locale.ENGLISH);
        for(ReturnRecord r:returnRecords){try{Date d=f.parse(r.date);if((from==null||!d.before(from))&&(to==null||!d.after(to)))n++;}catch(Exception ignored){}}
        return n;
    }

    public static synchronized double getReturnedSalesAmountBetween(Date from, Date to) {
        double n=0; SimpleDateFormat f=new SimpleDateFormat("dd MMM yyyy HH:mm",Locale.ENGLISH);
        for(ReturnRecord r:returnRecords){try{Date d=f.parse(r.date);if((from==null||!d.before(from))&&(to==null||!d.after(to)))n+=r.amount;}catch(Exception ignored){}}
        return n;
    }

    public static synchronized double getReturnedCostAmountBetween(Date from, Date to) {
        double n=0; SimpleDateFormat f=new SimpleDateFormat("dd MMM yyyy HH:mm",Locale.ENGLISH);
        for(ReturnRecord r:returnRecords){try{Date d=f.parse(r.date);if((from==null||!d.before(from))&&(to==null||!d.after(to)) && "Return to Stock".equals(r.disposition)) {
            for(OrdersPanel.Order o:OrdersPanel.getOrders()) if(o!=null && r.orderId.equals(o.id)) for(OrdersPanel.OrderItem i:o.items) if(i!=null && i.code.equals(r.productCode) && java.util.Objects.equals(i.size,r.size) && java.util.Objects.equals(i.color,r.color)){n+=i.costPrice*r.quantity;break;}
        }}catch(Exception ignored){}}
        return n;
    }

    public static synchronized int getReturnCount() {
        return returnRecords.size();
    }

    public static synchronized double getReturnedSalesAmount() {
        double total = 0;
        for (ReturnRecord r : returnRecords) total += r.amount;
        return total;
    }

    public static synchronized double getReturnLoss() {
        double total = 0;
        for (ReturnRecord r : returnRecords) total += r.loss;
        return total;
    }

    public static synchronized int getReturnedQuantity() {
        int total = 0;
        for (ReturnRecord r : returnRecords) total += r.quantity;
        return total;
    }

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
                                new Color(
                                        225,
                                        232,
                                        232
                                )
                        ),
                        BorderFactory.createEmptyBorder(
                                15,
                                18,
                                15,
                                18
                        )
                )
        );

        JLabel titleLabel =
                new JLabel(
                        title
                );

        titleLabel.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        12
                )
        );

        titleLabel.setForeground(
                new Color(
                        130,
                        145,
                        145
                )
        );

        JLabel valueLabel =
                new JLabel(
                        value
                );

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
    // DISPOSITION RENDERER
    // =========================================================

    private static class DispositionRenderer
            extends JLabel
            implements TableCellRenderer {

        public DispositionRenderer() {

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
        public Component
        getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {

            String text =
                    value == null
                            ? ""
                            : value.toString();

            setText(
                    text
            );

            if (
                    "Return to Stock"
                            .equals(
                                    text
                            )
            ) {

                setForeground(
                        new Color(
                                45,
                                125,
                                75
                        )
                );

                setBackground(
                        new Color(
                                225,
                                242,
                                230
                        )
                );

            } else if (
                    "Scrap / Damaged"
                            .equals(
                                    text
                            )
            ) {

                setForeground(
                        new Color(
                                175,
                                65,
                                55
                        )
                );

                setBackground(
                        new Color(
                                250,
                                225,
                                220
                        )
                );

            } else {

                setForeground(
                        new Color(
                                175,
                                115,
                                20
                        )
                );

                setBackground(
                        new Color(
                                250,
                                240,
                                215
                        )
                );
            }

            return this;
        }
    }

    // =========================================================
    // TEXT FIELD
    // =========================================================

    private JTextField createTextField() {

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
                        0,
                        36
                )
        );

        field.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(
                                        215,
                                        225,
                                        225
                                )
                        ),
                        BorderFactory.createEmptyBorder(
                                0,
                                10,
                                0,
                                10
                        )
                )
        );

        return field;
    }

    // =========================================================
    // PARSE DOUBLE
    // =========================================================

    private double parseDouble(
            Object value
    ) {

        if (value == null) {
            return 0;
        }

        String text =
                value.toString()
                        .replace(
                                "EGP",
                                ""
                        )
                        .replace(
                                ",",
                                ""
                        )
                        .trim();

        try {

            return Double.parseDouble(
                    text
            );

        } catch (Exception e) {

            return 0;
        }
    }

    // =========================================================
    // PARSE INT
    // =========================================================

    private int parseInt(
            Object value
    ) {

        if (value == null) {
            return 0;
        }

        String text =
                value.toString()
                        .replace(
                                ",",
                                ""
                        )
                        .trim();

        try {

            return Integer.parseInt(
                    text
            );

        } catch (Exception e) {

            return 0;
        }
    }

    // =========================================================
    // FORMAT MONEY
    // =========================================================

    private String formatMoney(
            double value
    ) {

        /*
         * Locale.US is forced here on purpose.
         *
         * String.format() without an explicit Locale uses
         * the JVM's default locale for digit glyphs. On a
         * machine set to Arabic, that silently renders
         * numbers using Eastern Arabic-Indic digits
         * (e.g. "450" becomes "٤٥٠").
         *
         * Double.parseDouble() (used in parseDouble()) only
         * understands plain ASCII 0-9 digits, no matter the
         * locale. So a value we format here and later try to
         * parse back (e.g. summing the Loss column in
         * updateSummary) would fail to parse and silently
         * fall back to 0 - which is exactly why Total Loss
         * could show EGP 0 even with a real loss in the
         * table.
         *
         * Forcing Locale.US keeps every money string in this
         * panel round-trip safe between formatMoney() and
         * parseDouble().
         */

        if (
                value
                        == Math.floor(value)
        ) {

            return String.format(
                    Locale.US,
                    "%.0f",
                    value
            );
        }

        return String.format(
                Locale.US,
                "%.2f",
                value
        );
    }
}
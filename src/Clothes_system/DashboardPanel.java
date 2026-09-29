package Clothes_system;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DashboardPanel extends JPanel {

    private StatCard totalSalesCard;
    private StatCard totalOrdersCard;
    private StatCard customersCard;
    private StatCard netProfitCard;

    private SalesChart chart;
    private LowStockPanel lowStock;
    private RecentOrders recentOrders;
    private BestSellingProducts bestSelling;

    public DashboardPanel() {

        setBackground(
            new Color(248, 250, 249)
        );

        setLayout(
            new BorderLayout()
        );


        // =========================
        // HEADER
        // =========================

        Header header =
            new Header();

        add(
            header,
            BorderLayout.NORTH
        );


        // =========================
        // DASHBOARD CONTENT
        // =========================

        JPanel dashboardContent =
            new JPanel();

        dashboardContent.setBackground(
            new Color(248, 250, 249)
        );

        dashboardContent.setLayout(
            new BoxLayout(
                dashboardContent,
                BoxLayout.Y_AXIS
            )
        );

        dashboardContent.setBorder(
            BorderFactory.createEmptyBorder(
                5, 30, 30, 30
            )
        );


        // =========================
        // STAT CARDS
        // =========================

        JPanel cardsPanel =
            new JPanel(
                new GridLayout(
                    1, 4, 15, 0
                )
            );

        cardsPanel.setBackground(
            new Color(248, 250, 249)
        );

        cardsPanel.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                130
            )
        );


        totalSalesCard = new StatCard(
                "Total Sales",
                "EGP 0",
                "All completed orders"
        );
        cardsPanel.add(totalSalesCard);

        totalOrdersCard = new StatCard(
                "Total Orders",
                "0",
                "All orders placed"
        );
        cardsPanel.add(totalOrdersCard);

        customersCard = new StatCard(
                "Customers",
                "0",
                "Registered customers"
        );
        cardsPanel.add(customersCard);

        netProfitCard = new StatCard(
                "Net Profit",
                "EGP 0",
                "Revenue minus cost & expenses"
        );
        cardsPanel.add(netProfitCard);


        dashboardContent.add(
            cardsPanel
        );


        // Space

        dashboardContent.add(
            Box.createVerticalStrut(20)
        );


        // =========================
        // MIDDLE SECTION
        // =========================

        JPanel middlePanel =
            new JPanel(
                new GridLayout(
                    1, 2, 20, 0
                )
            );

        middlePanel.setBackground(
            new Color(248, 250, 249)
        );

        middlePanel.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                390
            )
        );


        // Sales Chart

        chart =
            new SalesChart();

        middlePanel.add(chart);


        // Low Stock

        lowStock =
            new LowStockPanel();

        middlePanel.add(lowStock);


        dashboardContent.add(
            middlePanel
        );
        
     // =========================
     // BOTTOM SECTION
     // =========================

     dashboardContent.add(
         Box.createVerticalStrut(20)
     );


     JPanel bottomPanel =
         new JPanel(
             new GridLayout(
                 1, 2, 20, 0
             )
         );

     bottomPanel.setBackground(
         new Color(248, 250, 249)
     );

     bottomPanel.setMaximumSize(
         new Dimension(
             Integer.MAX_VALUE,
             300
         )
     );


     // Recent Orders

     recentOrders =
         new RecentOrders();

     bottomPanel.add(
         recentOrders
     );


     // Best Selling Products

     bestSelling =
         new BestSellingProducts();

     bottomPanel.add(
         bestSelling
     );


     dashboardContent.add(
         bottomPanel
     );

        add(
            dashboardContent,
            BorderLayout.CENTER
        );

        refreshData();
    }

    // =========================================================
    // NAVIGATION
    // Lets the "View Inventory" / "View All" links jump to the
    // right tab instead of doing nothing.
    // =========================================================

    public void wireNavigation(Clothes_system frame) {
        if (frame == null) return;
        lowStock.setOnViewInventory(() -> frame.showPage("Inventory"));
        recentOrders.setOnViewAll(() -> frame.showPage("Orders"));
        // Dashboard header search is a real global search.
        // It routes to the matching existing panel instead of being a decorative field.
        Header h = findHeader();
        if (h != null) h.setSearchListener(e -> frame.globalSearch(h.getSearchText()));
    }

    private Header findHeader() {
        for (Component c : getComponents()) if (c instanceof Header) return (Header)c;
        return null;
    }

    // =========================================================
    // REFRESH ALL DASHBOARD DATA FROM THE REAL SYSTEM
    // Call this every time the Dashboard tab is shown so it
    // never goes stale, no matter what order panels/products/
    // orders were created in.
    // =========================================================

    public void refreshData() {

        List<Product> products =
                ProductManager.getInstance().getAllProducts();

        List<OrdersPanel.Order> orders =
                OrdersPanel.getOrders();

        // ---------------------------------------------------
        // TOTAL SALES + NET PROFIT (completed orders only)
        // ---------------------------------------------------

        double totalSales = 0;
        double totalCost = 0;

        for (OrdersPanel.Order order : orders) {

            if ("Cancelled".equalsIgnoreCase(order.orderStatus)) {
                continue;
            }

            totalSales += Math.max(0, parseMoney(order.total) - ReturnsPanel.getReturnedSalesAmountForOrder(order.id));

            for (OrdersPanel.OrderItem item : order.items) {
                // Use the cost price snapshotted at the time of sale
                // (item.costPrice), not today's live product cost —
                // otherwise editing a product's cost later would
                // silently rewrite the profit of past orders.
                totalCost += item.costPrice * item.quantity;
            }
            totalCost -= ReturnsPanel.getReturnedCostAmountForOrder(order.id);
        }

        // Net profit uses net sales, cost of units still sold, and actual expenses.
        totalCost = Math.max(0, totalCost);
        double totalExpenses = ExpenseManager.total();
        double netProfit = totalSales - totalCost - totalExpenses;

        if (totalSalesCard != null) {
            totalSalesCard.setValue(String.format(Locale.US, "EGP %,.0f", totalSales));
        }

        if (netProfitCard != null) {
            netProfitCard.setValue(String.format(Locale.US, "EGP %,.0f", netProfit));
        }

        // ---------------------------------------------------
        // TOTAL ORDERS
        // ---------------------------------------------------
        //
        // Business rule (must match ReportsPanel.metrics()):
        // Total Orders = non-cancelled orders. Dashboard is an all-time
        // KPI (it has no period selector), so this counts non-cancelled
        // orders across all time, while Reports applies the same
        // non-cancelled rule within the user-selected period. Cancelled
        // orders never count toward this KPI in either screen.

        if (totalOrdersCard != null) {
            int activeOrderCount = 0;
            for (OrdersPanel.Order order : orders) {
                if (!"Cancelled".equalsIgnoreCase(order.orderStatus)) {
                    activeOrderCount++;
                }
            }
            totalOrdersCard.setValue(String.valueOf(activeOrderCount));
        }

        // ---------------------------------------------------
        // CUSTOMERS
        // ---------------------------------------------------

        javax.swing.table.DefaultTableModel customerModel =
                CustomersPanel.getCustomerModel();

        if (customersCard != null) {
            customersCard.setValue(
                    String.valueOf(
                            customerModel != null ? customerModel.getRowCount() : 0
                    )
            );
        }

        // ---------------------------------------------------
        // SALES CHART (last 6 months, oldest to newest)
        // ---------------------------------------------------

        if (chart != null) {

            int months = 6;
            int[] monthlySales = new int[months];
            String[] monthLabels = new String[months];

            Calendar cal = Calendar.getInstance();
            SimpleDateFormat labelFormat = new SimpleDateFormat("MMM", Locale.ENGLISH);
            SimpleDateFormat keyFormat = new SimpleDateFormat("yyyyMM", Locale.ENGLISH);

            String[] monthKeys = new String[months];

            // Build the last N month buckets ending this month.
            cal.add(Calendar.MONTH, -(months - 1));

            for (int i = 0; i < months; i++) {
                monthLabels[i] = labelFormat.format(cal.getTime());
                monthKeys[i] = keyFormat.format(cal.getTime());
                cal.add(Calendar.MONTH, 1);
            }

            // Gross sales are bucketed by ORDER date.
            for (OrdersPanel.Order order : orders) {

                if (order.dateValue == null
                        || "Cancelled".equalsIgnoreCase(order.orderStatus)) {
                    continue;
                }

                String key = keyFormat.format(order.dateValue);

                for (int i = 0; i < months; i++) {
                    if (monthKeys[i].equals(key)) {
                        monthlySales[i] += (int) Math.round(parseMoney(order.total));
                        break;
                    }
                }
            }

            // Returns are bucketed by the month the return was PROCESSED,
            // which is the rule Reports and Monthly Performance already
            // use. Previously the chart deducted an order's returns from
            // the order's own month, so a return that crossed a month
            // boundary made the chart and the Reports table disagree.
            Calendar bucket = Calendar.getInstance();
            bucket.add(Calendar.MONTH, -(months - 1));

            for (int i = 0; i < months; i++) {

                Calendar monthStart = (Calendar) bucket.clone();
                monthStart.set(Calendar.DAY_OF_MONTH, 1);
                monthStart.set(Calendar.HOUR_OF_DAY, 0);
                monthStart.set(Calendar.MINUTE, 0);
                monthStart.set(Calendar.SECOND, 0);
                monthStart.set(Calendar.MILLISECOND, 0);

                Calendar monthEnd = (Calendar) monthStart.clone();
                monthEnd.add(Calendar.MONTH, 1);
                monthEnd.add(Calendar.DAY_OF_MONTH, -1);
                monthEnd.set(Calendar.HOUR_OF_DAY, 23);
                monthEnd.set(Calendar.MINUTE, 59);
                monthEnd.set(Calendar.SECOND, 59);
                monthEnd.set(Calendar.MILLISECOND, 999);

                double returned = ReturnsPanel.getReturnedSalesAmountBetween(
                        monthStart.getTime(), monthEnd.getTime());

                monthlySales[i] = (int) Math.max(0,
                        Math.round(monthlySales[i] - returned));

                bucket.add(Calendar.MONTH, 1);
            }

            chart.setData(monthlySales, monthLabels);
        }

        // ---------------------------------------------------
        // LOW STOCK / RECENT ORDERS / BEST SELLING
        // ---------------------------------------------------

        if (lowStock != null) {
            lowStock.refreshData();
        }

        if (recentOrders != null) {
            recentOrders.refreshData();
        }

        if (bestSelling != null) {
            bestSelling.refreshData();
        }
    }

    private double parseMoney(String value) {

        if (value == null || value.trim().isEmpty()) {
            return 0;
        }

        try {
            return Double.parseDouble(
                    value.replace("EGP", "")
                            .replace(",", "")
                            .trim()
            );
        } catch (Exception e) {
            return 0;
        }
    }
}
package Clothes_system;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class RecentOrders extends JPanel {

    private JPanel ordersList;
    private Runnable onViewAll;

    public void setOnViewAll(Runnable callback) {
        this.onViewAll = callback;
    }

    public RecentOrders() {

        setBackground(Color.WHITE);

        setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    new Color(225, 232, 232)
                ),
                BorderFactory.createEmptyBorder(
                    20, 20, 15, 20
                )
            )
        );

        setLayout(new BorderLayout());


        // =========================
        // HEADER
        // =========================

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);

        JPanel titleBox = new JPanel();
        titleBox.setBackground(Color.WHITE);

        titleBox.setLayout(
            new BoxLayout(
                titleBox,
                BoxLayout.Y_AXIS
            )
        );

        JLabel title = new JLabel("Recent Orders");

        title.setFont(
            new Font(
                "Arial",
                Font.BOLD,
                17
            )
        );

        title.setForeground(
            new Color(25, 45, 45)
        );


        JLabel subtitle =
            new JLabel("Latest customer orders");

        subtitle.setFont(
            new Font(
                "Arial",
                Font.PLAIN,
                11
            )
        );

        subtitle.setForeground(
            new Color(140, 150, 150)
        );


        titleBox.add(title);

        titleBox.add(
            Box.createVerticalStrut(4)
        );

        titleBox.add(subtitle);


        header.add(
            titleBox,
            BorderLayout.WEST
        );


        // View all button

        JButton viewAll =
            new JButton("View All →");

        viewAll.setFocusPainted(false);
        viewAll.setBorderPainted(false);
        viewAll.setContentAreaFilled(false);

        viewAll.setForeground(
            new Color(25, 115, 105)
        );

        viewAll.setFont(
            new Font(
                "Arial",
                Font.BOLD,
                11
            )
        );


        viewAll.setCursor(
            new Cursor(Cursor.HAND_CURSOR)
        );

        viewAll.addActionListener(e -> {
            if (onViewAll != null) {
                onViewAll.run();
            }
        });

        header.add(
            viewAll,
            BorderLayout.EAST
        );


        add(
            header,
            BorderLayout.NORTH
        );


        // =========================
        // ORDERS
        // =========================

        ordersList = new JPanel();

        ordersList.setBackground(Color.WHITE);

        ordersList.setLayout(
            new BoxLayout(
                ordersList,
                BoxLayout.Y_AXIS
            )
        );

        refreshData();

        add(
            ordersList,
            BorderLayout.CENTER
        );
    }

    // =========================================================
    // REFRESH FROM REAL ORDERS
    // =========================================================

    public void refreshData() {

        if (ordersList == null) {
            return;
        }

        ordersList.removeAll();

        List<OrdersPanel.Order> recent =
                OrdersPanel.getOrders().stream()
                        .sorted(
                                Comparator.comparing(
                                        (OrdersPanel.Order o) -> o.dateValue,
                                        Comparator.nullsFirst(Comparator.naturalOrder())
                                ).reversed()
                        )
                        .limit(5)
                        .collect(Collectors.toList());

        if (recent.isEmpty()) {

            JLabel empty = new JLabel("No orders yet.");
            empty.setFont(new Font("Arial", Font.PLAIN, 11));
            empty.setForeground(new Color(140, 150, 150));
            empty.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
            ordersList.add(empty);

        } else {

            for (OrdersPanel.Order order : recent) {

                String productSummary = "-";

                if (!order.items.isEmpty()) {
                    OrdersPanel.OrderItem first = order.items.get(0);
                    productSummary = order.items.size() > 1
                            ? first.name + " +" + (order.items.size() - 1) + " more"
                            : first.name;
                }

                ordersList.add(
                        createOrder(
                                "#" + order.id,
                                order.customer,
                                productSummary,
                                order.total,
                                order.orderStatus
                        )
                );
            }
        }

        ordersList.revalidate();
        ordersList.repaint();
    }


    private JPanel createOrder(
            String orderId,
            String customer,
            String product,
            String price,
            String status) {


        JPanel order =
            new JPanel(
                new BorderLayout()
            );

        order.setBackground(Color.WHITE);

        order.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                58
            )
        );

        order.setBorder(
            BorderFactory.createMatteBorder(
                0,
                0,
                1,
                0,
                new Color(240, 243, 243)
            )
        );


        // =========================
        // LEFT
        // =========================

        JPanel left = new JPanel();

        left.setBackground(Color.WHITE);

        left.setLayout(
            new BoxLayout(
                left,
                BoxLayout.Y_AXIS
            )
        );


        JLabel id =
            new JLabel(orderId);

        id.setFont(
            new Font(
                "Arial",
                Font.BOLD,
                12
            )
        );

        id.setForeground(
            new Color(45, 60, 60)
        );


        JLabel customerLabel =
            new JLabel(customer);

        customerLabel.setFont(
            new Font(
                "Arial",
                Font.PLAIN,
                10
            )
        );

        customerLabel.setForeground(
            new Color(145, 155, 155)
        );


        left.add(id);

        left.add(
            Box.createVerticalStrut(3)
        );

        left.add(customerLabel);


        // =========================
        // CENTER
        // =========================

        JLabel productLabel =
            new JLabel(product);

        productLabel.setFont(
            new Font(
                "Arial",
                Font.PLAIN,
                11
            )
        );

        productLabel.setForeground(
            new Color(80, 90, 90)
        );


        // =========================
        // RIGHT
        // =========================

        JPanel right = new JPanel();

        right.setBackground(Color.WHITE);

        right.setLayout(
            new BoxLayout(
                right,
                BoxLayout.Y_AXIS
            )
        );


        JLabel priceLabel =
            new JLabel(price);

        priceLabel.setFont(
            new Font(
                "Arial",
                Font.BOLD,
                11
            )
        );

        priceLabel.setForeground(
            new Color(40, 55, 55)
        );


        JLabel statusLabel =
            new JLabel(status);

        statusLabel.setFont(
            new Font(
                "Arial",
                Font.BOLD,
                9
            )
        );


        if (status.equals("Completed")) {

            statusLabel.setForeground(
                new Color(40, 145, 100)
            );

        } else if (status.equals("Pending")) {

            statusLabel.setForeground(
                new Color(210, 140, 40)
            );

        } else {

            statusLabel.setForeground(
                new Color(70, 120, 190)
            );
        }


        right.add(priceLabel);

        right.add(
            Box.createVerticalStrut(3)
        );

        right.add(statusLabel);


        order.add(
            left,
            BorderLayout.WEST
        );

        order.add(
            productLabel,
            BorderLayout.CENTER
        );

        order.add(
            right,
            BorderLayout.EAST
        );


        return order;
    }
}
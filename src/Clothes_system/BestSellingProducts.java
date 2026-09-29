package Clothes_system;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class BestSellingProducts extends JPanel {

    private JPanel productsList;

    public BestSellingProducts() {

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

        setLayout(
            new BorderLayout()
        );


        // =========================
        // HEADER
        // =========================

        JPanel header =
            new JPanel();

        header.setBackground(Color.WHITE);

        header.setLayout(
            new BorderLayout()
        );


        JLabel title =
            new JLabel("Best Selling Products");

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
            new JLabel("Top products this month");

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


        JPanel titleBox =
            new JPanel();

        titleBox.setBackground(Color.WHITE);

        titleBox.setLayout(
            new BoxLayout(
                titleBox,
                BoxLayout.Y_AXIS
            )
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


        add(
            header,
            BorderLayout.NORTH
        );


        // =========================
        // PRODUCTS
        // =========================

        productsList =
            new JPanel();

        productsList.setBackground(Color.WHITE);

        productsList.setLayout(
            new BoxLayout(
                productsList,
                BoxLayout.Y_AXIS
            )
        );

        refreshData();

        add(
            productsList,
            BorderLayout.CENTER
        );
    }

    // =========================================================
    // REFRESH FROM REAL ORDER DATA
    // =========================================================

    private static class Aggregate {
        int quantity;
        double revenue;
    }

    public void refreshData() {

        if (productsList == null) {
            return;
        }

        productsList.removeAll();

        Map<String, Aggregate> byProduct = new LinkedHashMap<>();

        for (OrdersPanel.Order order : OrdersPanel.getOrders()) {

            for (OrdersPanel.OrderItem item : order.items) {

                Aggregate agg = byProduct.computeIfAbsent(item.name, k -> new Aggregate());
                agg.quantity += item.quantity;
                agg.revenue += item.price * item.quantity;
            }
        }

        List<Map.Entry<String, Aggregate>> top =
                byProduct.entrySet().stream()
                        .sorted((a, b) -> Integer.compare(b.getValue().quantity, a.getValue().quantity))
                        .limit(4)
                        .collect(Collectors.toList());

        if (top.isEmpty()) {

            JLabel empty = new JLabel("No sales recorded yet.");
            empty.setFont(new Font("Arial", Font.PLAIN, 11));
            empty.setForeground(new Color(140, 150, 150));
            empty.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
            productsList.add(empty);

        } else {

            int rank = 1;

            for (Map.Entry<String, Aggregate> entry : top) {

                productsList.add(
                        createProduct(
                                rank,
                                entry.getKey(),
                                entry.getValue().quantity + " sold",
                                formatRevenue(entry.getValue().revenue)
                        )
                );

                rank++;
            }
        }

        productsList.revalidate();
        productsList.repaint();
    }

    private String formatRevenue(double revenue) {

        if (revenue >= 1000) {
            return String.format(Locale.US, "EGP %.0fK", revenue / 1000.0);
        }

        return String.format(Locale.US, "EGP %,.0f", revenue);
    }


    private JPanel createProduct(
            int rank,
            String name,
            String sold,
            String revenue) {


        JPanel product =
            new JPanel(
                new BorderLayout()
            );

        product.setBackground(Color.WHITE);

        product.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                58
            )
        );

        product.setBorder(
            BorderFactory.createMatteBorder(
                0,
                0,
                1,
                0,
                new Color(240, 243, 243)
            )
        );


        // =========================
        // NUMBER
        // =========================

        JLabel number =
            new JLabel(
                String.valueOf(rank)
            );

        number.setPreferredSize(
            new Dimension(
                30,
                40
            )
        );

        number.setFont(
            new Font(
                "Arial",
                Font.BOLD,
                13
            )
        );

        number.setForeground(
            new Color(25, 115, 105)
        );


        // =========================
        // INFO
        // =========================

        JPanel info =
            new JPanel();

        info.setBackground(Color.WHITE);

        info.setLayout(
            new BoxLayout(
                info,
                BoxLayout.Y_AXIS
            )
        );


        JLabel nameLabel =
            new JLabel(name);

        nameLabel.setFont(
            new Font(
                "Arial",
                Font.BOLD,
                12
            )
        );

        nameLabel.setForeground(
            new Color(45, 60, 60)
        );


        JLabel soldLabel =
            new JLabel(sold);

        soldLabel.setFont(
            new Font(
                "Arial",
                Font.PLAIN,
                10
            )
        );

        soldLabel.setForeground(
            new Color(145, 155, 155)
        );


        info.add(nameLabel);

        info.add(
            Box.createVerticalStrut(3)
        );

        info.add(soldLabel);


        // =========================
        // REVENUE
        // =========================

        JLabel revenueLabel =
            new JLabel(revenue);

        revenueLabel.setFont(
            new Font(
                "Arial",
                Font.BOLD,
                11
            )
        );

        revenueLabel.setForeground(
            new Color(40, 55, 55)
        );


        product.add(
            number,
            BorderLayout.WEST
        );

        product.add(
            info,
            BorderLayout.CENTER
        );

        product.add(
            revenueLabel,
            BorderLayout.EAST
        );


        return product;
    }
}
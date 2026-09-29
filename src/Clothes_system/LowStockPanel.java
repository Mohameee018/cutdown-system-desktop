package Clothes_system;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class LowStockPanel extends JPanel {

    private JPanel productsList;
    private Runnable onViewInventory;

    public void setOnViewInventory(Runnable callback) {
        this.onViewInventory = callback;
    }

    public LowStockPanel() {

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
        // TITLE
        // =========================

        JPanel header =
            new JPanel();

        header.setBackground(Color.WHITE);

        header.setLayout(
            new BorderLayout()
        );


        JLabel title =
            new JLabel("Low Stock");

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
            new JLabel("Products running low");

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


        // =========================
        // VIEW INVENTORY
        // =========================

        JButton viewButton =
            new JButton("View Inventory →");

        viewButton.setFocusPainted(false);

        viewButton.setBorderPainted(false);

        viewButton.setContentAreaFilled(false);

        viewButton.setForeground(
            new Color(25, 115, 105)
        );

        viewButton.setFont(
            new Font(
                "Arial",
                Font.BOLD,
                12
            )
        );

        viewButton.setCursor(
            new Cursor(Cursor.HAND_CURSOR)
        );

        viewButton.addActionListener(e -> {
            if (onViewInventory != null) {
                onViewInventory.run();
            }
        });


        JPanel bottom =
            new JPanel(
                new FlowLayout(
                    FlowLayout.LEFT,
                    0,
                    0
                )
            );

        bottom.setBackground(Color.WHITE);

        bottom.add(viewButton);


        add(
            bottom,
            BorderLayout.SOUTH
        );
    }


    // =========================================================
    // REFRESH FROM REAL PRODUCT DATA
    // =========================================================

    public void refreshData() {

        if (productsList == null) {
            return;
        }

        productsList.removeAll();

        List<Product> lowStock =
                ProductManager.getInstance().getLowStockProducts();

        if (lowStock.isEmpty()) {

            JLabel empty = new JLabel("No products are running low right now.");
            empty.setFont(new Font("Arial", Font.PLAIN, 11));
            empty.setForeground(new Color(140, 150, 150));
            empty.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
            productsList.add(empty);

        } else {

            int shown = 0;

            for (Product p : lowStock) {

                if (shown >= 5) {
                    break;
                }

                String sizeText = "All sizes";

                if (!p.getVariants().isEmpty()) {
                    String sz = p.getVariants().get(0).getSize();
                    if (sz != null && !sz.isEmpty()) {
                        sizeText = "Size " + sz;
                    }
                }

                String stockText =
                        p.getStockQuantity() <= 0
                                ? "Out of stock"
                                : p.getStockQuantity() + " left";

                productsList.add(
                        createProduct(
                                p.getName(),
                                sizeText,
                                stockText
                        )
                );

                shown++;
            }
        }

        productsList.revalidate();
        productsList.repaint();
    }

    private JPanel createProduct(
            String name,
            String size,
            String stock) {


        JPanel product =
            new JPanel(
                new BorderLayout()
            );

        product.setBackground(Color.WHITE);

        product.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                55
            )
        );

        product.setBorder(
            BorderFactory.createMatteBorder(
                0, 0, 1, 0,
                new Color(240, 243, 243)
            )
        );


        // Product information

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


        JLabel sizeLabel =
            new JLabel(size);

        sizeLabel.setFont(
            new Font(
                "Arial",
                Font.PLAIN,
                10
            )
        );

        sizeLabel.setForeground(
            new Color(145, 155, 155)
        );


        info.add(nameLabel);

        info.add(
            Box.createVerticalStrut(3)
        );

        info.add(sizeLabel);


        // Stock

        JLabel stockLabel =
            new JLabel(stock);

        stockLabel.setFont(
            new Font(
                "Arial",
                Font.BOLD,
                11
            )
        );

        stockLabel.setForeground(
            new Color(210, 100, 70)
        );


        product.add(
            info,
            BorderLayout.CENTER
        );

        product.add(
            stockLabel,
            BorderLayout.EAST
        );


        return product;
    }
}
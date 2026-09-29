package Clothes_system;

import javax.swing.*;
import java.awt.*;

public class SalesChart extends JPanel {

    // Starts empty; DashboardPanel supplies actual values.
    private int[] sales = {0,0,0,0,0,0};

    private String[] months = {"","","","","",""};

    // =========================================================
    // REAL DATA WIRING
    // Called by DashboardPanel with actual monthly revenue
    // computed from OrdersPanel.getOrders() instead of the
    // static demo numbers above.
    // =========================================================

    public void setData(int[] salesData, String[] monthLabels) {

        if (salesData == null || monthLabels == null
                || salesData.length == 0
                || salesData.length != monthLabels.length) {
            return;
        }

        this.sales = salesData;
        this.months = monthLabels;

        repaint();
    }

    public SalesChart() {

        setBackground(Color.WHITE);

        setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    new Color(225, 232, 232)
                ),
                BorderFactory.createEmptyBorder(
                    20, 20, 20, 20
                )
            )
        );
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2 =
            (Graphics2D) g.create();

        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );


        // =========================
        // TITLE
        // =========================

        g2.setColor(
            new Color(25, 45, 45)
        );

        g2.setFont(
            new Font(
                "Arial",
                Font.BOLD,
                17
            )
        );

        g2.drawString(
            "Sales Overview",
            20,
            30
        );


        // =========================
        // SUBTITLE
        // =========================

        g2.setColor(
            new Color(130, 145, 145)
        );

        g2.setFont(
            new Font(
                "Arial",
                Font.PLAIN,
                11
            )
        );

        g2.drawString(
            "Monthly sales performance",
            20,
            48
        );


        // =========================
        // CHART AREA
        // =========================

        int left = 70;
        int top = 80;

        int right = getWidth() - 25;
        int bottom = getHeight() - 45;

        int chartWidth = right - left;
        int chartHeight = bottom - top;


        // =========================
        // GRID LINES
        // =========================

        g2.setColor(
            new Color(235, 240, 240)
        );

        for (int i = 0; i <= 4; i++) {

            int y =
                top +
                (chartHeight * i / 4);

            g2.drawLine(
                left,
                y,
                right,
                y
            );
        }


        // =========================
        // Y AXIS LABELS
        // =========================

        g2.setColor(
            new Color(140, 150, 150)
        );

        g2.setFont(
            new Font(
                "Arial",
                Font.PLAIN,
                10
            )
        );

        int maxSale = 1;

        for (int v : sales) {
            if (v > maxSale) {
                maxSale = v;
            }
        }

        // Round the scale up a bit so the top point isn't glued
        // to the ceiling of the chart.
        double axisMax = maxSale * 1.15;

        String[] values = new String[5];

        for (int i = 0; i < 5; i++) {

            double v = axisMax * (4 - i) / 4.0;

            values[i] = v >= 1000
                    ? String.format("%.0fK", v / 1000.0)
                    : String.format("%.0f", v);
        }

        for (int i = 0; i < values.length; i++) {

            int y =
                top +
                (chartHeight * i / 4);

            g2.drawString(
                values[i],
                25,
                y + 4
            );
        }


        // =========================
        // POINTS
        // =========================

        int[] xPoints =
            new int[sales.length];

        int[] yPoints =
            new int[sales.length];


        for (int i = 0; i < sales.length; i++) {

            xPoints[i] =
                sales.length > 1
                    ? left + (chartWidth * i / (sales.length - 1))
                    : left + (chartWidth / 2);

            yPoints[i] =
                bottom -
                (int) (sales[i] * chartHeight / axisMax);
        }


        // =========================
        // LINE
        // =========================

        g2.setColor(
            new Color(25, 115, 105)
        );

        g2.setStroke(
            new BasicStroke(
                3f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND
            )
        );


        for (int i = 0; i < sales.length - 1; i++) {

            g2.drawLine(
                xPoints[i],
                yPoints[i],
                xPoints[i + 1],
                yPoints[i + 1]
            );
        }


        // =========================
        // POINT CIRCLES
        // =========================

        for (int i = 0; i < sales.length; i++) {

            g2.setColor(Color.WHITE);

            g2.fillOval(
                xPoints[i] - 5,
                yPoints[i] - 5,
                10,
                10
            );

            g2.setColor(
                new Color(25, 115, 105)
            );

            g2.fillOval(
                xPoints[i] - 3,
                yPoints[i] - 3,
                6,
                6
            );
        }


        // =========================
        // MONTHS
        // =========================

        g2.setColor(
            new Color(140, 150, 150)
        );

        for (int i = 0; i < months.length; i++) {

            g2.drawString(
                months[i],
                xPoints[i] - 10,
                bottom + 25
            );
        }


        g2.dispose();
    }
}
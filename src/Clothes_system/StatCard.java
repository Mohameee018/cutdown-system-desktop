package Clothes_system;

import javax.swing.*;
import java.awt.*;

public class StatCard extends JPanel {

    private JLabel valueLabel;
    private JLabel changeLabel;

    public StatCard(
            String title,
            String value,
            String change) {

        setBackground(Color.WHITE);

        setLayout(
            new BorderLayout()
        );

        setBorder(
            BorderFactory.createCompoundBorder(

                BorderFactory.createLineBorder(
                    new Color(225, 232, 232)
                ),

                BorderFactory.createEmptyBorder(
                    18, 20, 18, 20
                )
            )
        );


        // =========================
        // TOP
        // =========================

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
            new Color(110, 130, 130)
        );


        // =========================
        // VALUE
        // =========================

        valueLabel =
            new JLabel(value);

        valueLabel.setFont(
            new Font(
                "Arial",
                Font.BOLD,
                25
            )
        );

        valueLabel.setForeground(
            new Color(25, 45, 45)
        );


        // =========================
        // CHANGE
        // =========================

        changeLabel =
            new JLabel(change);

        changeLabel.setFont(
            new Font(
                "Arial",
                Font.PLAIN,
                11
            )
        );

        changeLabel.setForeground(
            new Color(40, 145, 100)
        );


        // =========================
        // TEXT PANEL
        // =========================

        JPanel textPanel = new JPanel();

        textPanel.setBackground(Color.WHITE);

        textPanel.setLayout(
            new BoxLayout(
                textPanel,
                BoxLayout.Y_AXIS
            )
        );


        textPanel.add(titleLabel);

        textPanel.add(
            Box.createVerticalStrut(8)
        );

        textPanel.add(valueLabel);

        textPanel.add(
            Box.createVerticalStrut(6)
        );

        textPanel.add(changeLabel);


        add(
            textPanel,
            BorderLayout.CENTER
        );
    }

    public void setValue(String value) {
        if (valueLabel != null) {
            valueLabel.setText(value);
        }
    }

    public void setChange(String change) {
        if (changeLabel != null) {
            changeLabel.setText(change);
        }
    }

    public void setChangeColor(Color color) {
        if (changeLabel != null) {
            changeLabel.setForeground(color);
        }
    }
}
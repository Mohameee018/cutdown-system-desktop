package Clothes_system;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class Header extends JPanel {

    private final JTextField searchField = new JTextField();

    public Header() {

        setBackground(new Color(248, 250, 249));

        setPreferredSize(new Dimension(0, 85));

        setLayout(new BorderLayout());

        setBorder(
            BorderFactory.createEmptyBorder(
                15, 30, 15, 30
            )
        );


        // =========================
        // LEFT SIDE
        // =========================

        JPanel titlePanel = new JPanel();

        titlePanel.setBackground(
            new Color(248, 250, 249)
        );

        titlePanel.setLayout(
            new BoxLayout(
                titlePanel,
                BoxLayout.Y_AXIS
            )
        );


        JLabel title = new JLabel("Dashboard");

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
                "Overview of your clothing store"
            );

        subtitle.setFont(
            new Font(
                "Arial",
                Font.PLAIN,
                12
            )
        );

        subtitle.setForeground(
            new Color(120, 140, 140)
        );


        titlePanel.add(title);

        titlePanel.add(
            Box.createVerticalStrut(4)
        );

        titlePanel.add(subtitle);


        // =========================
        // RIGHT SIDE
        // =========================

        JPanel rightPanel = new JPanel(
            new FlowLayout(
                FlowLayout.RIGHT,
                12,
                8
            )
        );

        rightPanel.setBackground(
            new Color(248, 250, 249)
        );


        // Search

        JTextField search = searchField;

        search.setPreferredSize(
            new Dimension(190, 38)
        );

        search.setText("Search...");
        search.setForeground(new Color(140, 155, 155));
        search.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if ("Search...".equals(search.getText())) { search.selectAll(); search.setForeground(new Color(35,55,55)); }
            }
            @Override public void focusLost(FocusEvent e) {
                if (search.getText().trim().isEmpty()) { search.setText("Search..."); search.setForeground(new Color(140,155,155)); }
            }
        });


        // Notification

        JButton notification =
            new JButton("🔔");

        notification.setPreferredSize(
            new Dimension(42, 38)
        );

        notification.setFocusPainted(false);

        notification.setBorderPainted(false);

        notification.setBackground(Color.WHITE);


        // User

        JLabel user =
            new JLabel("Mohamed Ahmed");

        user.setFont(
            new Font(
                "Arial",
                Font.BOLD,
                13
            )
        );

        user.setForeground(
            new Color(35, 55, 55)
        );


        rightPanel.add(search);

        rightPanel.add(notification);

        rightPanel.add(user);


        add(
            titlePanel,
            BorderLayout.WEST
        );

        add(
            rightPanel,
            BorderLayout.EAST
        );
    }

    public void setSearchListener(ActionListener listener) {
        for (ActionListener old : searchField.getActionListeners()) searchField.removeActionListener(old);
        if (listener != null) searchField.addActionListener(listener);
    }

    public String getSearchText() { return searchField.getText().trim(); }

    public void clearSearch() { searchField.setText(""); }
}
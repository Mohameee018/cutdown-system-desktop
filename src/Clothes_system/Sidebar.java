package Clothes_system;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

public class Sidebar extends JPanel {

    private final Clothes_system mainFrame;

    private Color SIDEBAR =
        new Color(10, 45, 45);

    private Color ACTIVE =
        new Color(28, 83, 83);

    private final Color ICON_BG =
        new Color(20, 66, 66);

    private final Color TEXT =
        new Color(235, 242, 240);

    private Color MUTED =
        new Color(145, 175, 170);

    private final List<JButton> menuButtons =
        new ArrayList<>();

    private JLabel storeNameLabel;
    private JLabel storeTypeLabel;
    private String activePage = "Dashboard";

    public Sidebar(
        Clothes_system mainFrame
    ) {

        this.mainFrame = mainFrame;

        setPreferredSize(
            new Dimension(272, 0)
        );

        setBackground(SIDEBAR);

        setLayout(
            new BorderLayout()
        );

        // ================= TOP =================

        JPanel topPanel = new JPanel();

        topPanel.setLayout(
            new BoxLayout(
                topPanel,
                BoxLayout.Y_AXIS
            )
        );

        topPanel.setBackground(SIDEBAR);

        topPanel.setBorder(
            BorderFactory.createEmptyBorder(
                30, 18, 20, 18
            )
        );

        // ================= LOGO =================

        JLabel logo =
            new JLabel(SettingsManager.getStoreName());

        logo.setFont(
            new Font(
                "Segoe UI",
                Font.BOLD,
                30
            )
        );

        logo.setForeground(Color.WHITE);

        JLabel storeName =
            new JLabel(SettingsManager.getStoreType());

        storeNameLabel = logo;
        storeTypeLabel = storeName;

        storeName.setFont(
            new Font(
                "Segoe UI",
                Font.PLAIN,
                10
            )
        );

        storeName.setForeground(MUTED);

        topPanel.add(logo);

        topPanel.add(
            Box.createVerticalStrut(2)
        );

        topPanel.add(storeName);

        topPanel.add(
            Box.createVerticalStrut(30)
        );

        // ================= MAIN =================

        addSectionTitle(
            topPanel,
            "MAIN"
        );

        addMenuButton(
            topPanel,
            "Dashboard",
            "Dashboard",
            "⌂"
        );

        addMenuButton(
            topPanel,
            "Products",
            "Products",
            "▣"
        );

        addMenuButton(
            topPanel,
            "Inventory",
            "Inventory",
            "▤"
        );

        addMenuButton(
            topPanel,
            "Orders",
            "Orders",
            "🛒"
        );

        addMenuButton(
            topPanel,
            "Customers",
            "Customers",
            "♙"
        );

        topPanel.add(
            Box.createVerticalStrut(24)
        );

        // ================= MANAGEMENT =================

        addSectionTitle(
            topPanel,
            "MANAGEMENT"
        );

        addMenuButton(
            topPanel,
            "Returns",
            "Returns",
            "↩"
        );

        addMenuButton(
            topPanel,
            "Expenses",
            "Expenses",
            "₤"
        );

        addMenuButton(
            topPanel,
            "Reports",
            "Reports",
            "▥"
        );

        addMenuButton(
            topPanel,
            "Settings",
            "Settings",
            "⚙"
        );

        add(
            topPanel,
            BorderLayout.NORTH
        );

        // ================= PROFILE =================

        JPanel profilePanel =
            new JPanel(
                new BorderLayout()
            );

        profilePanel.setBackground(
            new Color(8, 38, 38)
        );

        profilePanel.setBorder(
            BorderFactory.createEmptyBorder(
                17, 18, 17, 18
            )
        );

        JPanel avatar =
            new JPanel() {

                @Override
                protected void paintComponent(
                    Graphics g
                ) {

                    super.paintComponent(g);

                    Graphics2D g2 =
                        (Graphics2D) g.create();

                    g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                    );

                    g2.setColor(ACTIVE);

                    g2.fill(
                        new RoundRectangle2D.Double(
                            0,
                            0,
                            42,
                            42,
                            12,
                            12
                        )
                    );

                    g2.setColor(Color.WHITE);

                    g2.setFont(
                        new Font(
                            "Segoe UI",
                            Font.BOLD,
                            12
                        )
                    );

                    FontMetrics fm =
                        g2.getFontMetrics();

                    String text = "MA";

                    int x =
                        (42
                        - fm.stringWidth(text))
                        / 2;

                    int y =
                        (42
                        - fm.getHeight())
                        / 2
                        + fm.getAscent();

                    g2.drawString(
                        text,
                        x,
                        y
                    );

                    g2.dispose();
                }
            };

        avatar.setOpaque(false);

        avatar.setPreferredSize(
            new Dimension(42, 42)
        );

        JPanel userInfo =
            new JPanel();

        userInfo.setLayout(
            new BoxLayout(
                userInfo,
                BoxLayout.Y_AXIS
            )
        );

        userInfo.setBackground(
            new Color(8, 38, 38)
        );

        userInfo.setBorder(
            BorderFactory.createEmptyBorder(
                2, 12, 0, 0
            )
        );

        JLabel name =
            new JLabel("Mohamed Ahmed");

        name.setFont(
            new Font(
                "Segoe UI",
                Font.BOLD,
                13
            )
        );

        name.setForeground(Color.WHITE);

        JLabel role =
            new JLabel("Administrator");

        role.setFont(
            new Font(
                "Segoe UI",
                Font.PLAIN,
                11
            )
        );

        role.setForeground(MUTED);

        userInfo.add(name);

        userInfo.add(
            Box.createVerticalStrut(3)
        );

        userInfo.add(role);

        profilePanel.add(
            avatar,
            BorderLayout.WEST
        );

        profilePanel.add(
            userInfo,
            BorderLayout.CENTER
        );

        add(
            profilePanel,
            BorderLayout.SOUTH
        );
    }

    // =========================================================
    // SECTION TITLE
    // =========================================================

    private void addSectionTitle(
        JPanel parent,
        String text
    ) {

        JLabel label =
            new JLabel(text);

        label.setFont(
            new Font(
                "Segoe UI",
                Font.BOLD,
                10
            )
        );

        label.setForeground(
            new Color(125, 160, 155)
        );

        label.setBorder(
            BorderFactory.createEmptyBorder(
                0, 10, 8, 0
            )
        );

        parent.add(label);
    }

    // =========================================================
    // MENU BUTTON
    // =========================================================

    private void addMenuButton(
        JPanel parent,
        String text,
        String page,
        String iconText
    ) {

        JButton button =
            new JButton();

        button.setLayout(
            new BorderLayout()
        );

        button.setBackground(SIDEBAR);

        button.setForeground(TEXT);

        button.setFocusPainted(false);

        button.setBorderPainted(false);

        button.setContentAreaFilled(true);

        button.setOpaque(true);

        button.setCursor(
            new Cursor(
                Cursor.HAND_CURSOR
            )
        );

        button.setPreferredSize(
            new Dimension(236, 50)
        );

        button.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                50
            )
        );

        button.setBorder(
            BorderFactory.createEmptyBorder(
                4, 8, 4, 8
            )
        );

        // ================= ICON =================

        JPanel iconBox =
            new JPanel() {

                @Override
                protected void paintComponent(
                    Graphics g
                ) {

                    super.paintComponent(g);

                    Graphics2D g2 =
                        (Graphics2D) g.create();

                    g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                    );

                    g2.setColor(ICON_BG);

                    g2.fill(
                        new RoundRectangle2D.Double(
                            0,
                            0,
                            34,
                            34,
                            9,
                            9
                        )
                    );

                    g2.setColor(
                        new Color(
                            215,
                            232,
                            228
                        )
                    );

                    g2.setFont(
                        new Font(
                            "Segoe UI Symbol",
                            Font.PLAIN,
                            17
                        )
                    );

                    FontMetrics fm =
                        g2.getFontMetrics();

                    int x =
                        (34
                        - fm.stringWidth(iconText))
                        / 2;

                    int y =
                        (34
                        - fm.getHeight())
                        / 2
                        + fm.getAscent();

                    g2.drawString(
                        iconText,
                        x,
                        y
                    );

                    g2.dispose();
                }
            };

        iconBox.setOpaque(false);

        iconBox.setPreferredSize(
            new Dimension(34, 34)
        );

        // ================= TEXT =================

        JLabel label =
            new JLabel(text);

        label.setFont(
            new Font(
                "Segoe UI",
                Font.PLAIN,
                14
            )
        );

        label.setForeground(TEXT);

        label.setBorder(
            BorderFactory.createEmptyBorder(
                0, 13, 0, 0
            )
        );

        button.add(
            iconBox,
            BorderLayout.WEST
        );

        button.add(
            label,
            BorderLayout.CENTER
        );

        button.putClientProperty("page", page);

        menuButtons.add(button);

        // ================= ACTION =================

        button.addActionListener(
            e -> {

                for (
                    JButton b :
                    menuButtons
                ) {

                    b.setBackground(SIDEBAR);
                }

                button.setBackground(ACTIVE);

                mainFrame.showPage(page);
            }
        );

        // ================= HOVER =================

        button.addMouseListener(
            new java.awt.event.MouseAdapter() {

                @Override
                public void mouseEntered(
                    java.awt.event.MouseEvent e
                ) {

                    if (
                        button.getBackground()
                            .equals(SIDEBAR)
                    ) {

                        button.setBackground(
                            new Color(
                                16,
                                57,
                                57
                            )
                        );
                    }
                }

                @Override
                public void mouseExited(
                    java.awt.event.MouseEvent e
                ) {

                    if (
                        !button.getBackground()
                            .equals(ACTIVE)
                    ) {

                        button.setBackground(
                            SIDEBAR
                        );
                    }
                }
            }
        );

        parent.add(button);

        parent.add(
            Box.createVerticalStrut(4)
        );

        // Dashboard active initially
        if (
            page.equals("Dashboard")
        ) {

            button.setBackground(ACTIVE);
        }
    }
    /** Refresh store branding and the user-selected sidebar colors. */
    public void refreshBrandingAndTheme() {
        SIDEBAR = SettingsManager.getSidebarColor();
        ACTIVE = SettingsManager.getPrimaryColor();
        if (storeNameLabel != null) storeNameLabel.setText(SettingsManager.getStoreName());
        if (storeTypeLabel != null) storeTypeLabel.setText(SettingsManager.getStoreType());
        setBackground(SIDEBAR);
        for (JButton b : menuButtons) {
            Object page = b.getClientProperty("page");
            b.setBackground(activePage.equals(page) ? ACTIVE : SIDEBAR);
        }
        repaint();
    }

    /**
     * Synchronize the highlighted sidebar button with the actual page
     * shown by Clothes_system. This also handles navigation triggered
     * from Dashboard quick-links and other internal navigation paths.
     */
    public void setActivePage(String page) {
        if (page == null) return;
        activePage = page;
        SIDEBAR = SettingsManager.getSidebarColor();
        ACTIVE = SettingsManager.getPrimaryColor();
        for (JButton b : menuButtons) {
            Object value = b.getClientProperty("page");
            b.setBackground(page.equals(value) ? ACTIVE : SIDEBAR);
        }
    }


}
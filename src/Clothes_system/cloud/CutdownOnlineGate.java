package Clothes_system.cloud;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CutdownOnlineGate {
    private CutdownOnlineGate() {}

    public static boolean requireOnlineAtStartup(Component parent) {
        while (true) {
            if (isOnline()) return true;
            int choice = JOptionPane.showOptionDialog(
                    parent,
                    "Cutdown requires an active internet connection and cloud server connection.\n"
                            + "The system will not start while the cloud is unreachable.",
                    "Internet Connection Required",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    new Object[]{"Retry", "Exit"},
                    "Retry"
            );
            if (choice != 0) return false;
        }
    }

    public static boolean isOnline() {
        if (!CutdownCloudConfig.configured()) return false;
        try {
            String body = new CutdownCloudClient().get("/api/health");
            return body != null && body.contains("\"ok\":true");
        } catch (Exception ignored) {
            return false;
        }
    }

    public static void installWatchdog(JFrame frame) {
        if (frame == null) return;

        JPanel blocker = new JPanel(new GridBagLayout());
        blocker.setOpaque(true);
        blocker.setBackground(new Color(248, 250, 249));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 220, 218)),
                BorderFactory.createEmptyBorder(35, 55, 35, 55)
        ));

        JLabel title = new JLabel("Internet connection required");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(new Color(25, 45, 45));

        JLabel message = new JLabel(
                "<html><div style='text-align:center'>"
                        + "The system is temporarily locked because the Cutdown cloud cannot be reached."
                        + "<br>Please restore the internet connection. The system will unlock automatically."
                        + "</div></html>"
        );
        message.setAlignmentX(Component.CENTER_ALIGNMENT);
        message.setBorder(BorderFactory.createEmptyBorder(12, 0, 20, 0));

        JLabel status = new JLabel("Checking connection...");
        status.setAlignmentX(Component.CENTER_ALIGNMENT);
        status.setForeground(new Color(120, 130, 130));

        card.add(title);
        card.add(message);
        card.add(status);
        blocker.add(card);

        JPanel glass = new JPanel(new BorderLayout());
        glass.add(blocker, BorderLayout.CENTER);
        glass.setOpaque(true);
        glass.setVisible(false);
        frame.setGlassPane(glass);

        AtomicBoolean checking = new AtomicBoolean(false);
        Timer timer = new Timer(3000, e -> {
            if (!checking.compareAndSet(false, true)) return;
            new Thread(() -> {
                boolean online = isOnline();
                SwingUtilities.invokeLater(() -> {
                    glass.setVisible(!online);
                    status.setText(online ? "Connected" : "Waiting for cloud connection...");
                    frame.revalidate();
                    frame.repaint();
                    checking.set(false);
                });
            }, "cutdown-online-check").start();
        });
        timer.setRepeats(true);
        timer.setInitialDelay(0);
        timer.start();

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                timer.stop();
            }
        });
    }
}

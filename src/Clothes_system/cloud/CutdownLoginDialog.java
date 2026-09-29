package Clothes_system.cloud;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public final class CutdownLoginDialog extends JDialog {
    private boolean authenticated;
    private final JTextField email = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JLabel message = new JLabel(" ");
    private final JButton login = new JButton("Sign in");

    private CutdownLoginDialog(Window owner) {
        super(owner, "Cutdown Sign in", ModalityType.APPLICATION_MODAL);
        setSize(430, 285);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(16,16));
        root.setBorder(BorderFactory.createEmptyBorder(28,30,24,30));
        root.setBackground(Color.WHITE);

        JLabel title = new JLabel("Cutdown");
        title.setFont(new Font("Arial", Font.BOLD, 28));
        JLabel subtitle = new JLabel("Sign in to your brand account");
        subtitle.setForeground(new Color(100,100,100));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(subtitle);

        JPanel form = new JPanel(new GridLayout(4,1,0,6));
        form.setOpaque(false);
        form.add(new JLabel("Email"));
        form.add(email);
        form.add(new JLabel("Password"));
        form.add(password);

        message.setForeground(new Color(180,40,40));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(message, BorderLayout.CENTER);
        login.setPreferredSize(new Dimension(110,38));
        bottom.add(login, BorderLayout.EAST);

        root.add(heading, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);

        getRootPane().setDefaultButton(login);
        login.addActionListener(e -> doLogin());
        password.addActionListener(e -> doLogin());
    }

    public static boolean showAndLogin(Window owner) {
        CutdownLoginDialog dialog = new CutdownLoginDialog(owner);
        dialog.setVisible(true);
        return dialog.authenticated;
    }

    private void doLogin() {
        String e = email.getText().trim();
        String p = new String(password.getPassword());
        if (e.isBlank() || p.isBlank()) {
            message.setText("Enter your email and password.");
            return;
        }
        login.setEnabled(false);
        message.setText("Signing in...");
        new Thread(() -> {
            try {
                String response = new CutdownCloudClient().login(e, p);
                Object parsed = MiniJson.parse(response);
                if (!(parsed instanceof Map<?,?> map)) throw new IllegalStateException("Invalid login response.");
                Object token = map.get("access_token");
                Object brand = map.get("brand");
                if (token == null || String.valueOf(token).isBlank() || !(brand instanceof Map<?,?>)) {
                    throw new IllegalStateException("This account is not assigned to a brand.");
                }
                CutdownCloudConfig.setAccessToken(String.valueOf(token));
                authenticated = true;
                SwingUtilities.invokeLater(this::dispose);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    login.setEnabled(true);
                    message.setText("Sign in failed. Check the account and try again.");
                });
            }
        }, "cutdown-login").start();
    }
}

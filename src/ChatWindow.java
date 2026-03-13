import javax.swing.*;
import java.awt.*;

public class ChatWindow extends JFrame implements NetworkListener {
    private final NetworkController net;
    private JTextArea chatArea;
    private JTextField inputField;
    private JTextField userField;
    private JPasswordField passField;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    
    public ChatWindow(NetworkController net) {
        this.net = net;
        this.net.setListener(this);
        initUI();
    }

    private void initUI() {
        setTitle("Discord Clone - UPV/EHU");
        setSize(500, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(createLoginPanel(), "LOGIN");
        mainPanel.add(createChatPanel(), "CHAT");

        add(mainPanel);
        setLocationRelativeTo(null);
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        userField = new JTextField(15);
        passField = new JPasswordField(15);
        JButton btnLogin = new JButton("Login");
        JButton btnReg = new JButton("Register");

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Usuario:"), gbc);
        gbc.gridx = 1; panel.add(userField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Contraseña:"), gbc);
        gbc.gridx = 1; panel.add(passField, gbc);

        JPanel btnPanel = new JPanel();
        btnPanel.add(btnLogin);
        btnPanel.add(btnReg);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; panel.add(btnPanel, gbc);

        btnLogin.addActionListener(e -> net.login(userField.getText(), new String(passField.getPassword())));
        btnReg.addActionListener(e -> net.register(userField.getText(), new String(passField.getPassword())));

        return panel;
    }

    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        panel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        inputField = new JTextField();
        JButton btnSend = new JButton("Send");
        JButton btnVoice = new JButton("Toggle Voice");

        bottom.add(btnVoice, BorderLayout.WEST);
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(btnSend, BorderLayout.EAST);
        panel.add(bottom, BorderLayout.SOUTH);

        btnSend.addActionListener(e -> sendChat());
        inputField.addActionListener(e -> sendChat());
        btnVoice.addActionListener(e -> net.sendMessage("/voice"));

        return panel;
    }

    private void sendChat() {
        String msg = inputField.getText().trim();
        if (!msg.isEmpty()) {
            net.sendMessage(msg);
            inputField.setText("");
        }
    }

    // Cruce de frontera hacia el Event Dispatch Thread (EDT)
    @Override
    public void onMessageReceived(String user, String message) {
        SwingUtilities.invokeLater(() -> chatArea.append(user + ": " + message + "\n"));
    }

    @Override
    public void onAuthResult(boolean success, String message) {
        SwingUtilities.invokeLater(() -> {
            if (success) {
                cardLayout.show(mainPanel, "CHAT");
                chatArea.append("=== " + message + " ===\n");
            } else {
                JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    @Override
    public void onSystemMessage(String message) {
        SwingUtilities.invokeLater(() -> chatArea.append("=== " + message + " ===\n"));
    }
}

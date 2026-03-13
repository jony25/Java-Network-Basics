import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ChatWindow extends JFrame implements NetworkListener {
    private final NetworkController net;
    private JTextPane chatArea;
    private JTextField inputField;
    private JTextField userField;
    private JPasswordField passField;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JButton btnVoice;

    // Discord Palette
    private final Color BG_DARK = new Color(54, 57, 63);       // #36393f (Main Chat)
    private final Color BG_DARKER = new Color(47, 49, 54);     // #2f3136 (Sidebar)
    private final Color BG_DARKEST = new Color(32, 34, 37);    // #202225 (Inputs/Server list)
    private final Color TEXT_NORMAL = new Color(220, 221, 222); // #dcddde
    private final Color BLURPLE = new Color(88, 101, 242);     // #5865F2
    private final Color GREEN = new Color(67, 181, 129);       // #43b581

    // User state
    private final DefaultListModel<String> onlineUsersModel = new DefaultListModel<>();
    private final DefaultListModel<String> voiceUsersModel = new DefaultListModel<>();
    private JList<String> listOnlineUsers;
    private JPanel voiceUsersPanel;

    public ChatWindow(NetworkController net) {
        this.net = net;
        this.net.setListener(this);
        initUI();
    }

    private void initUI() {
        setTitle("Discord Clone");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setBackground(BG_DARK);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(createLoginPanel(), "LOGIN");
        mainPanel.add(createChatPanel(), "CHAT");

        add(mainPanel);
        setLocationRelativeTo(null);
    }

    private JPanel createLoginPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(BG_DARK);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_DARK);
        card.setBorder(new EmptyBorder(32, 32, 32, 32));

        JLabel title = new JLabel("¡Te damos la bienvenida de nuevo!");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("¡Nos alegramos mucho de volver a verte!");
        subtitle.setForeground(new Color(185, 187, 190));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        userField = createStyledTextField("CORREO O NÚMERO DE TELÉFONO");
        passField = createStyledPasswordField("CONTRASEÑA");

        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.Y_AXIS));
        btnPanel.setBackground(BG_DARK);
        
        JButton btnLogin = createStyledButton("Iniciar sesión", BLURPLE);
        JButton btnReg = createStyledButton("Registrarse", BG_DARKER);

        btnLogin.addActionListener(e -> net.login(userField.getText(), new String(passField.getPassword())));
        btnReg.addActionListener(e -> net.register(userField.getText(), new String(passField.getPassword())));

        // Wrap login in centered FlowLayout
        JPanel loginWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        loginWrap.setBackground(BG_DARK);
        loginWrap.add(btnLogin);
        
        // Wrap register in centered FlowLayout
        JPanel regWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        regWrap.setBackground(BG_DARK);
        regWrap.add(btnReg);

        btnPanel.add(loginWrap);
        btnPanel.add(Box.createVerticalStrut(8));
        btnPanel.add(regWrap);

        card.add(title);
        card.add(Box.createVerticalStrut(8));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(24));
        card.add(userField);
        card.add(Box.createVerticalStrut(16));
        card.add(passField);
        card.add(Box.createVerticalStrut(24));
        card.add(btnPanel);

        wrapper.add(card);
        return wrapper;
    }

    // UI State Context
    private String currentServer = "Servidor UPV";
    private String currentTextChannel = "general";
    private JButton activeTextChannelBtn;
    
    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Left Sidebar: Server List
        JPanel serverList = new JPanel();
        serverList.setLayout(new BoxLayout(serverList, BoxLayout.Y_AXIS));
        serverList.setBackground(BG_DARKEST);
        serverList.setPreferredSize(new Dimension(72, 0));
        serverList.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JButton srv1 = createServerIcon("U");
        serverList.add(srv1);
        serverList.add(Box.createVerticalStrut(10));
        JButton srv2 = createServerIcon("G");
        serverList.add(srv2);

        // Middle Sidebar: Channel List
        JPanel channelList = new JPanel(new BorderLayout());
        channelList.setBackground(BG_DARKER);
        channelList.setPreferredSize(new Dimension(240, 0));
        
        JPanel channels = new JPanel();
        channels.setLayout(new BoxLayout(channels, BoxLayout.Y_AXIS));
        channels.setBackground(BG_DARKER);
        channels.setBorder(new EmptyBorder(15, 10, 10, 10));
        
        JLabel srvTitle = new JLabel("Servidor UPV");
        srvTitle.setName("serverTitleLabel");
        srvTitle.setForeground(Color.WHITE);
        srvTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        channels.add(srvTitle);
        channels.add(Box.createVerticalStrut(20));
        
        // Text Channels
        channels.add(createCategoryLabel("CANALES DE TEXTO"));
        JButton txtGen = createChannelBtn("# general", true);
        JButton txtOff = createChannelBtn("# off-topic", true);
        channels.add(txtGen);
        channels.add(txtOff);
        
        channels.add(Box.createVerticalStrut(20));
        
        // Voice Channels
        channels.add(createCategoryLabel("CANALES DE VOZ"));
        JButton vcGen = createChannelBtn("[Voz] voz-general", false);
        channels.add(vcGen);
        
        // Voice Users Container underneath the channel
        voiceUsersPanel = new JPanel();
        voiceUsersPanel.setLayout(new BoxLayout(voiceUsersPanel, BoxLayout.Y_AXIS));
        voiceUsersPanel.setBackground(BG_DARKER);
        voiceUsersPanel.setBorder(new EmptyBorder(0, 20, 0, 0));
        channels.add(voiceUsersPanel);

        channelList.add(channels, BorderLayout.WEST);

        setActiveChannelBtn(txtGen);

        // Text Channel Click Listeners
        txtGen.addActionListener(e -> { setActiveChannelBtn(txtGen); currentTextChannel = "general"; net.joinTextChannel(currentServer, "general"); updateChatHeader(); });
        txtOff.addActionListener(e -> { setActiveChannelBtn(txtOff); currentTextChannel = "off-topic"; net.joinTextChannel(currentServer, "off-topic"); updateChatHeader(); });
        
        // Voice Channel Click Listeners
        vcGen.addActionListener(e -> net.joinVoiceChannel(currentServer, "voz-general"));
        
        // Server click handlers to swap contexts
        srv1.addActionListener(e -> switchServer("Servidor UPV"));
        srv2.addActionListener(e -> switchServer("Gaming"));

        // Voice controls at bottom of Channel List
        JPanel userControls = new JPanel(new BorderLayout());
        userControls.setBackground(new Color(41, 43, 47)); // #292b2f
        userControls.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        btnVoice = new JButton("📞 Desconectado");
        btnVoice.setBackground(BG_DARKER);
        btnVoice.setForeground(TEXT_NORMAL);
        btnVoice.setFocusPainted(false);
        btnVoice.setBorderPainted(false);
        btnVoice.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVoice.addActionListener(e -> net.leaveVoiceChannel()); // Now it only disconnects
        
        userControls.add(btnVoice, BorderLayout.CENTER);
        channelList.add(userControls, BorderLayout.SOUTH);

        // Main Chat Area
        JPanel mainChat = new JPanel(new BorderLayout());
        mainChat.setBackground(BG_DARK);

        // Chat Header
        JPanel chatHeader = new JPanel(new BorderLayout());
        chatHeader.setBackground(BG_DARK);
        chatHeader.setBorder(new MatteBorder(0, 0, 1, 0, BG_DARKEST));
        JLabel headerTitle = new JLabel(" # general");
        headerTitle.setName("chatHeaderTitle");
        headerTitle.setForeground(Color.WHITE);
        headerTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        headerTitle.setBorder(new EmptyBorder(15, 15, 15, 15));
        chatHeader.add(headerTitle, BorderLayout.WEST);
        mainChat.add(chatHeader, BorderLayout.NORTH);

        // Messages
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setBackground(BG_DARK);
        chatArea.setForeground(TEXT_NORMAL);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JScrollPane scroll = new JScrollPane(chatArea);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        mainChat.add(scroll, BorderLayout.CENTER);

        // Input Area
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(BG_DARK);
        inputPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        inputField = new JTextField();
        inputField.setBackground(new Color(64, 68, 75)); // #40444b
        inputField.setForeground(Color.WHITE);
        inputField.setCaretColor(Color.WHITE);
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(64, 68, 75), 10, true),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        inputField.addActionListener(e -> sendChat());
        
        inputPanel.add(inputField, BorderLayout.CENTER);
        mainChat.add(inputPanel, BorderLayout.SOUTH);

        // Right Sidebar: Online Users
        JPanel userListPanel = new JPanel(new BorderLayout());
        userListPanel.setBackground(BG_DARKER);
        userListPanel.setPreferredSize(new Dimension(240, 0));
        userListPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        JLabel usersTitle = createCategoryLabel("USUARIOS CONECTADOS");
        userListPanel.add(usersTitle, BorderLayout.NORTH);
        
        listOnlineUsers = new JList<>(onlineUsersModel);
        listOnlineUsers.setBackground(BG_DARKER);
        listOnlineUsers.setForeground(TEXT_NORMAL);
        listOnlineUsers.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        listOnlineUsers.setSelectionBackground(new Color(52, 55, 60));
        listOnlineUsers.setSelectionForeground(Color.WHITE);
        listOnlineUsers.setBorder(null);
        
        JScrollPane userScroll = new JScrollPane(listOnlineUsers);
        userScroll.setBorder(null);
        userListPanel.add(userScroll, BorderLayout.CENTER);

        // Split layout essentially
        JPanel rightSide = new JPanel(new BorderLayout());
        rightSide.add(channelList, BorderLayout.WEST);
        rightSide.add(mainChat, BorderLayout.CENTER);
        rightSide.add(userListPanel, BorderLayout.EAST);
        
        panel.add(serverList, BorderLayout.WEST);
        panel.add(rightSide, BorderLayout.CENTER);

        return panel;
    }

    private JLabel createCategoryLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(new Color(142, 146, 151));
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setBorder(new EmptyBorder(5, 0, 5, 0));
        return lbl;
    }
    
    private JButton createChannelBtn(String text, boolean isText) {
        JButton btn = new JButton(text);
        btn.setForeground(new Color(142, 146, 151));
        btn.setBackground(BG_DARKER);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(220, 35));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                if (btn != activeTextChannelBtn) {
                    btn.setBackground(new Color(52, 55, 60)); // Hover highlight
                    btn.setForeground(Color.WHITE);
                }
            }
            public void mouseExited(MouseEvent evt) {
                if (btn != activeTextChannelBtn) {
                    btn.setBackground(BG_DARKER);
                    btn.setForeground(new Color(142, 146, 151));
                }
            }
        });
        return btn;
    }
    
    private void setActiveChannelBtn(JButton btn) {
        if (activeTextChannelBtn != null) {
            activeTextChannelBtn.setBackground(BG_DARKER);
            activeTextChannelBtn.setForeground(new Color(142, 146, 151));
        }
        activeTextChannelBtn = btn;
        if (btn != null) {
            btn.setBackground(new Color(66, 70, 77)); // active highlight
            btn.setForeground(Color.WHITE);
        }
    }

    private JButton createServerIcon(String init) {
        JButton btn = new JButton(init);
        btn.setBackground(BG_DARK);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(48, 48));
        btn.setMaximumSize(new Dimension(48, 48));
        // Simple rounded feel via padding
        btn.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                btn.setBackground(BLURPLE);
            }
            public void mouseExited(MouseEvent evt) {
                btn.setBackground(BG_DARK);
            }
        });
        return btn;
    }
    
    private void updateChatHeader() {
        chatArea.setText(""); // clear chat on channel switch
        JLabel header = (JLabel) findComponentByName(mainPanel, "chatHeaderTitle");
        if (header != null) {
            header.setText(" # " + currentTextChannel);
        }
    }
    
    private void switchServer(String newServer) {
        if (!this.currentServer.equals(newServer)) {
            this.currentServer = newServer;
            JLabel title = (JLabel) findComponentByName(mainPanel, "serverTitleLabel");
            if (title != null) title.setText(newServer);
            net.joinTextChannel(this.currentServer, this.currentTextChannel); // Join to trigger history
            updateChatHeader();
        }
    }

    private Component findComponentByName(Container container, String name) {
        for (Component component : container.getComponents()) {
            if (name.equals(component.getName())) return component;
            if (component instanceof Container) {
                Component found = findComponentByName((Container) component, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private JTextField createStyledTextField(String placeholder) {
        JTextField tf = new JTextField(20);
        styleInput(tf);
        return tf;
    }

    private JPasswordField createStyledPasswordField(String placeholder) {
        JPasswordField pf = new JPasswordField(20);
        styleInput(pf);
        return pf;
    }

    private void styleInput(JTextField tf) {
        tf.setBackground(BG_DARKEST);
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BG_DARKEST, 8),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));
    }

    private JButton createStyledButton(String text, Color baseColor) {
        JButton btn = new JButton(text);
        btn.setBackground(baseColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setPreferredSize(new Dimension(160, 45));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                btn.setBackground(baseColor.brighter());
            }
            public void mouseExited(MouseEvent evt) {
                btn.setBackground(baseColor);
            }
        });
        return btn;
    }

    private void sendChat() {
        String msg = inputField.getText().trim();
        if (!msg.isEmpty()) {
            net.sendMessage(currentServer, currentTextChannel, msg);
            inputField.setText("");
        }
    }

    private void appendText(String text, Color color, boolean bold) {
        try {
            javax.swing.text.StyleContext sc = javax.swing.text.StyleContext.getDefaultStyleContext();
            javax.swing.text.AttributeSet aset = sc.addAttribute(javax.swing.text.SimpleAttributeSet.EMPTY, javax.swing.text.StyleConstants.Foreground, color);
            
            aset = sc.addAttribute(aset, javax.swing.text.StyleConstants.FontFamily, "Segoe UI");
            aset = sc.addAttribute(aset, javax.swing.text.StyleConstants.Alignment, javax.swing.text.StyleConstants.ALIGN_JUSTIFIED);
            aset = sc.addAttribute(aset, javax.swing.text.StyleConstants.Bold, bold);

            int len = chatArea.getDocument().getLength();
            chatArea.getDocument().insertString(len, text, aset);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void onMessageReceived(String user, String message) {
        SwingUtilities.invokeLater(() -> {
            boolean isMe = user.equals(userField.getText());
            Color nameColor = isMe ? BLURPLE : new Color(67, 181, 129); // green for others
            
            try {
                String initial = user.isEmpty() ? "?" : user.substring(0, 1).toUpperCase();
                Icon avatar = new AvatarIcon(initial, nameColor);
                
                int len = chatArea.getDocument().getLength();
                chatArea.setCaretPosition(len);
                chatArea.insertIcon(avatar);

                appendText("  " + user + "  ", nameColor, true);
                String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                appendText(time + "\n", new Color(114, 118, 125), false);
                
                appendText("      " + message + "\n\n", TEXT_NORMAL, false);
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    @Override
    public void onAuthResult(boolean success, String message) {
        SwingUtilities.invokeLater(() -> {
            if (success) {
                cardLayout.show(mainPanel, "CHAT");
                net.joinTextChannel(currentServer, currentTextChannel);
            } else {
                JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    @Override
    public void onSystemMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (message.equals("[VOZ ACTIVA]")) {
                btnVoice.setText("📞 Voz Conectada");
                btnVoice.setBackground(GREEN);
            } else if (message.equals("[VOZ OFF]")) {
                btnVoice.setText("📞 Desconectado");
                btnVoice.setBackground(BG_DARKER);
            } else {
                appendText("\n" + message + "\n", new Color(114, 137, 218), false);
            }
        });
    }

    @Override
    public void onUserPresence(String user, boolean isOnline) {
        SwingUtilities.invokeLater(() -> {
            if (isOnline) {
                if (!onlineUsersModel.contains(user)) {
                    onlineUsersModel.addElement(user);
                }
            } else {
                onlineUsersModel.removeElement(user);
            }
        });
    }

    @Override
    public void onVoicePresence(String channel, String user, boolean joined) {
        SwingUtilities.invokeLater(() -> {
            if (joined) {
                if (!voiceUsersModel.contains(user)) voiceUsersModel.addElement(user);
            } else {
                voiceUsersModel.removeElement(user);
            }
            refreshVoiceUsersUI();
        });
    }
    
    private void refreshVoiceUsersUI() {
        voiceUsersPanel.removeAll();
        for (int i = 0; i < voiceUsersModel.size(); i++) {
            JLabel lbl = new JLabel("  ╰ " + voiceUsersModel.get(i));
            lbl.setForeground(TEXT_NORMAL);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            voiceUsersPanel.add(lbl);
            voiceUsersPanel.add(Box.createVerticalStrut(4));
        }
        voiceUsersPanel.revalidate();
        voiceUsersPanel.repaint();
    }

    class AvatarIcon implements Icon {
        private String letter;
        private Color color;
        public AvatarIcon(String letter, Color color) {
            this.letter = letter;
            this.color = color;
        }
        public int getIconWidth() { return 36; }
        public int getIconHeight() { return 36; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x, y, 36, 36);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
            FontMetrics fm = g2.getFontMetrics();
            int sw = fm.stringWidth(letter);
            int sh = fm.getAscent();
            g2.drawString(letter, x + (36 - sw) / 2, y + (36 + sh) / 2 - 2);
            g2.dispose();
        }
    }
}

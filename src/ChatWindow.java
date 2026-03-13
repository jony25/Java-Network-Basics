import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private JButton activeServerBtn;
    private JPanel serverListPanel;
    private JPanel channelsPanel;
    private String currentServerOwner = "";
    private final Map<String, Image> customAvatars = new ConcurrentHashMap<>();
    
    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Left Sidebar: Server List
        serverListPanel = new JPanel();
        serverListPanel.setLayout(new BoxLayout(serverListPanel, BoxLayout.Y_AXIS));
        serverListPanel.setBackground(BG_DARKEST);
        serverListPanel.setPreferredSize(new Dimension(72, 0));
        serverListPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Middle Sidebar: Channel List
        JPanel channelList = new JPanel(new BorderLayout());
        channelList.setBackground(BG_DARKER);
        channelList.setPreferredSize(new Dimension(240, 0));
        
        channelsPanel = new JPanel();
        channelsPanel.setLayout(new BoxLayout(channelsPanel, BoxLayout.Y_AXIS));
        channelsPanel.setBackground(BG_DARKER);
        channelsPanel.setBorder(new EmptyBorder(15, 10, 10, 10));
        
        // Voice Users Container underneath the channel (Will be re-parented on rebuild)
        voiceUsersPanel = new JPanel();
        voiceUsersPanel.setLayout(new BoxLayout(voiceUsersPanel, BoxLayout.Y_AXIS));
        voiceUsersPanel.setBackground(BG_DARKER);
        voiceUsersPanel.setBorder(new EmptyBorder(0, 20, 0, 0));
        voiceUsersPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        channelList.add(channelsPanel, BorderLayout.CENTER);

        // Bottom of Channel List: User Controls
        JPanel userControls = new JPanel();
        userControls.setLayout(new BoxLayout(userControls, BoxLayout.Y_AXIS));
        userControls.setBackground(new Color(41, 43, 47));
        userControls.setBorder(new EmptyBorder(8, 8, 8, 8));
        
        btnVoice = new JButton("Desconectado");
        btnVoice.setBackground(BG_DARKER);
        btnVoice.setForeground(TEXT_NORMAL);
        btnVoice.setFocusPainted(false);
        btnVoice.setBorderPainted(false);
        btnVoice.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnVoice.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        btnVoice.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVoice.addActionListener(e -> net.leaveVoiceChannel());
        userControls.add(btnVoice);
        userControls.add(Box.createVerticalStrut(6));
        
        // User Info Bar (avatar + name + settings gear)
        JPanel userInfoBar = new JPanel(new BorderLayout(6, 0));
        userInfoBar.setBackground(new Color(41, 43, 47));
        userInfoBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        userInfoBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        JLabel userAvatarLbl = new JLabel() {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                String nm = userField.getText();
                String init = (nm == null || nm.isEmpty()) ? "?" : nm.substring(0, 1).toUpperCase();
                new AvatarIcon(nm == null ? "" : nm, init, BLURPLE).paintIcon(this, g, 2, 2);
            }
        };
        userAvatarLbl.setPreferredSize(new Dimension(36, 36));
        userInfoBar.add(userAvatarLbl, BorderLayout.WEST);
        
        JLabel userNameLbl = new JLabel();
        userNameLbl.setName("bottomUserName");
        userNameLbl.setForeground(Color.WHITE);
        userNameLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        userInfoBar.add(userNameLbl, BorderLayout.CENTER);
        
        JButton btnSettings = new JButton("\u2699");
        btnSettings.setForeground(TEXT_NORMAL);
        btnSettings.setBackground(new Color(41, 43, 47));
        btnSettings.setFocusPainted(false);
        btnSettings.setBorderPainted(false);
        btnSettings.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        btnSettings.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSettings.setPreferredSize(new Dimension(36, 36));
        btnSettings.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnSettings.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent e) { btnSettings.setForeground(TEXT_NORMAL); }
        });
        btnSettings.addActionListener(e -> showSettingsDialog());
        userInfoBar.add(btnSettings, BorderLayout.EAST);
        
        userControls.add(userInfoBar);
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

        // Right Sidebar: Online Users (Removed text here but identical structure)
        JPanel userListPanel = new JPanel(new BorderLayout());
        userListPanel.setBackground(BG_DARKER);
        userListPanel.setPreferredSize(new Dimension(240, 0));
        userListPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        JLabel usersTitle = createCategoryLabel("USUARIOS (N/A)");
        usersTitle.setName("usersTitle");
        userListPanel.add(usersTitle, BorderLayout.NORTH);
        
        listOnlineUsers = new JList<>(onlineUsersModel);
        listOnlineUsers.setBackground(BG_DARKER);
        listOnlineUsers.setForeground(TEXT_NORMAL);
        listOnlineUsers.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        listOnlineUsers.setSelectionBackground(new Color(52, 55, 60));
        listOnlineUsers.setSelectionForeground(Color.WHITE);
        listOnlineUsers.setBorder(null);
        
        listOnlineUsers.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String user = value.toString();
                String initial = user.isEmpty() ? "?" : user.substring(0, 1).toUpperCase();
                Color c = user.equals(userField.getText()) ? BLURPLE : new Color(67, 181, 129);
                if (currentServerOwner.equals(user)) {
                    lbl.setText(user + " 👑");
                }
                lbl.setIcon(new AvatarIcon(user, initial, c));
                lbl.setBorder(new EmptyBorder(4, 4, 4, 4));
                lbl.setIconTextGap(10);
                return lbl;
            }
        });
        
        JScrollPane userScroll = new JScrollPane(listOnlineUsers);
        userScroll.setBorder(null);
        userListPanel.add(userScroll, BorderLayout.CENTER);

        // Split layout essentially
        JPanel rightSide = new JPanel(new BorderLayout());
        rightSide.add(channelList, BorderLayout.WEST);
        rightSide.add(mainChat, BorderLayout.CENTER);
        rightSide.add(userListPanel, BorderLayout.EAST);
        
        panel.add(serverListPanel, BorderLayout.WEST);
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
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
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

    private JButton createServerIcon(String init, String serverName) {
        JButton btn = new JButton(init);
        btn.setName("srv_" + serverName);
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
                if (btn != activeServerBtn) btn.setBackground(BLURPLE);
            }
            public void mouseExited(MouseEvent evt) {
                if (btn != activeServerBtn) btn.setBackground(BG_DARK);
            }
        });
        return btn;
    }
    
    private void setActiveServerBtn(JButton btn) {
        if (activeServerBtn != null) {
            activeServerBtn.setBackground(BG_DARK);
        }
        activeServerBtn = btn;
        if (btn != null) {
            btn.setBackground(BLURPLE);
        }
    }
    
    private void updateChatHeader() {
        chatArea.setText(""); // clear chat on channel switch
        JLabel header = (JLabel) findComponentByName(mainPanel, "chatHeaderTitle");
        if (header != null) {
            header.setText(" # " + currentTextChannel);
        }
    }
    
    private void switchServer(String newServer) {
        this.currentServer = newServer;
        net.getServerInfo(newServer);
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

    private JMenuItem createMenuItem(String text, Color color) {
        JMenuItem item = new JMenuItem(text);
        item.setForeground(color);
        item.setBackground(new Color(24, 25, 28));
        item.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        item.setBorder(new EmptyBorder(6, 12, 6, 12));
        item.setOpaque(true);
        item.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { item.setBackground(BLURPLE); }
            public void mouseExited(MouseEvent e) { item.setBackground(new Color(24, 25, 28)); }
        });
        return item;
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
            try {
                boolean isMe = user.equals(userField.getText());
                Color nameColor = isMe ? BLURPLE : new Color(67, 181, 129);
                String initial = user.isEmpty() ? "?" : user.substring(0, 1).toUpperCase();
                String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

                // Build a message panel: [Avatar | Name  Time]
                //                        [       | message   ]
                JPanel msgPanel = new JPanel(new BorderLayout(10, 0));
                msgPanel.setBackground(BG_DARK);
                msgPanel.setBorder(new EmptyBorder(4, 10, 4, 10));

                // Avatar label
                JLabel avatarLbl = new JLabel(new AvatarIcon(user, initial, nameColor));
                avatarLbl.setVerticalAlignment(SwingConstants.TOP);
                avatarLbl.setPreferredSize(new Dimension(40, 40));
                msgPanel.add(avatarLbl, BorderLayout.WEST);

                // Right side: name+time on top, message below
                JPanel textSide = new JPanel();
                textSide.setLayout(new BoxLayout(textSide, BoxLayout.Y_AXIS));
                textSide.setBackground(BG_DARK);
                textSide.setBorder(new EmptyBorder(0, 0, 0, 0));

                // Name + time row
                JPanel nameRow = new JPanel();
                nameRow.setLayout(new BoxLayout(nameRow, BoxLayout.X_AXIS));
                nameRow.setBackground(BG_DARK);
                nameRow.setAlignmentX(Component.LEFT_ALIGNMENT);
                JLabel nameLbl = new JLabel(user);
                nameLbl.setForeground(nameColor);
                nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
                JLabel timeLbl = new JLabel("  " + time);
                timeLbl.setForeground(new Color(114, 118, 125));
                timeLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                nameRow.add(nameLbl);
                nameRow.add(timeLbl);
                nameRow.add(Box.createHorizontalGlue());
                textSide.add(nameRow);

                // Message row
                JLabel msgLbl = new JLabel(message);
                msgLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
                msgLbl.setForeground(TEXT_NORMAL);
                msgLbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                textSide.add(msgLbl);

                msgPanel.add(textSide, BorderLayout.CENTER);

                // Limit max height so it doesn't stretch
                msgPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, msgPanel.getPreferredSize().height + 10));

                int len = chatArea.getDocument().getLength();
                chatArea.setCaretPosition(len);
                chatArea.insertComponent(msgPanel);
                appendText("\n", BG_DARK, false); // newline after component
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
            String user = voiceUsersModel.get(i);
            JLabel lbl = new JLabel(user);
            
            String initial = user.isEmpty() ? "?" : user.substring(0, 1).toUpperCase();
            Color c = user.equals(userField.getText()) ? BLURPLE : new Color(67, 181, 129);
            lbl.setIcon(new AvatarIcon(user, initial, c));
            lbl.setIconTextGap(8);
            
            lbl.setForeground(TEXT_NORMAL);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lbl.setBorder(new EmptyBorder(2, 20, 2, 0));
            voiceUsersPanel.add(lbl);
            voiceUsersPanel.add(Box.createVerticalStrut(4));
        }
        voiceUsersPanel.revalidate();
        voiceUsersPanel.repaint();
    }

    @Override
    public void onAvatarReceived(String user, String base64) {
        SwingUtilities.invokeLater(() -> {
            try {
                byte[] bytes = java.util.Base64.getDecoder().decode(base64);
                Image img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
                if (img != null) {
                    customAvatars.put(user, img.getScaledInstance(36, 36, Image.SCALE_SMOOTH));
                    listOnlineUsers.repaint();
                    voiceUsersPanel.repaint();
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    @Override
    public void onServerList(java.util.List<String> servers) {
        SwingUtilities.invokeLater(() -> {
            serverListPanel.removeAll();
            JButton firstServerBtn = null;
            for (String srv : servers) {
                String init = srv.isEmpty() ? "?" : srv.substring(0, 1).toUpperCase();
                JButton btn = createServerIcon(init, srv);
                btn.addActionListener(e -> { setActiveServerBtn(btn); switchServer(srv); });
                
                // Right-click context menu
                btn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) { showPopup(e); }
                    @Override
                    public void mouseReleased(MouseEvent e) { showPopup(e); }
                    private void showPopup(MouseEvent e) {
                        if (e.isPopupTrigger()) {
                            JPopupMenu popup = new JPopupMenu();
                            popup.setBackground(new Color(24, 25, 28));
                            popup.setBorder(BorderFactory.createLineBorder(new Color(40, 42, 46), 1));

                            JMenuItem infoItem = createMenuItem("Información del Servidor", Color.WHITE);
                            infoItem.addActionListener(a -> {
                                net.getServerInfo(srv);
                                JOptionPane.showMessageDialog(ChatWindow.this, "Servidor: " + srv, "Info", JOptionPane.INFORMATION_MESSAGE);
                            });
                            popup.add(infoItem);

                            JMenuItem inviteItem = createMenuItem("Generar Invitación", new Color(67, 181, 129));
                            inviteItem.addActionListener(a -> {
                                net.generateInvite(srv);
                            });
                            popup.add(inviteItem);

                            popup.addSeparator();

                            JMenuItem leaveItem = createMenuItem("Abandonar Servidor", new Color(237, 66, 69));
                            leaveItem.addActionListener(a -> {
                                int confirm = JOptionPane.showConfirmDialog(ChatWindow.this,
                                    "¿Seguro que quieres abandonar '" + srv + "'?",
                                    "Abandonar Servidor", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                                if (confirm == JOptionPane.YES_OPTION) {
                                    net.leaveServer(srv);
                                }
                            });
                            popup.add(leaveItem);

                            popup.show(btn, e.getX(), e.getY());
                        }
                    }
                });
                
                if (firstServerBtn == null || srv.equals(currentServer)) firstServerBtn = btn;
                serverListPanel.add(btn);
                serverListPanel.add(Box.createVerticalStrut(10));
            }
            
            // Create Server Button
            JButton addSrv = createServerIcon("+", "Create");
            addSrv.setForeground(new Color(67, 181, 129));
            addSrv.setBackground(BG_DARKEST);
            addSrv.addActionListener(e -> {
                Object[] options = {"Crear Servidor", "Usar Invitación"};
                int n = JOptionPane.showOptionDialog(this,
                    "¿Qué deseas hacer?",
                    "Añadir Servidor",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[0]);
                if (n == 0) {
                    String name = JOptionPane.showInputDialog(this, "Nombre del nuevo servidor:");
                    if (name != null && !name.trim().isEmpty()) net.createServer(name.trim());
                } else if (n == 1) {
                    String code = JOptionPane.showInputDialog(this, "Introduce el código de invitación:");
                    if (code != null && !code.trim().isEmpty()) net.useInvite(code.trim());
                }
            });
            serverListPanel.add(addSrv);
            serverListPanel.revalidate();
            serverListPanel.repaint();
            
            if (firstServerBtn != null && activeServerBtn == null) {
                firstServerBtn.doClick();
            }
        });
    }

    @Override
    public void onServerInfo(String name, String owner, java.util.List<String> textChannels, java.util.List<String> voiceChannels) {
        SwingUtilities.invokeLater(() -> {
            this.currentServer = name;
            this.currentServerOwner = owner;
            
            channelsPanel.removeAll();
            
            JLabel srvTitle = new JLabel(name);
            srvTitle.setName("serverTitleLabel");
            srvTitle.setForeground(Color.WHITE);
            srvTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
            srvTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            srvTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            channelsPanel.add(srvTitle);
            channelsPanel.add(Box.createVerticalStrut(20));
            
            boolean isOwner = owner.equals(userField.getText());
            
            // Text Channels
            JPanel textHeader = new JPanel(new BorderLayout());
            textHeader.setBackground(BG_DARKER);
            textHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
            textHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            textHeader.add(createCategoryLabel("CANALES DE TEXTO"), BorderLayout.WEST);
            if (isOwner) {
                JButton addTxt = new JButton("+");
                addTxt.setForeground(Color.WHITE);
                addTxt.setFocusPainted(false);
                addTxt.setContentAreaFilled(false);
                addTxt.setBorderPainted(false);
                addTxt.setCursor(new Cursor(Cursor.HAND_CURSOR));
                addTxt.addActionListener(e -> {
                    String c = JOptionPane.showInputDialog(this, "Nombre del canal de texto:");
                    if (c != null && !c.trim().isEmpty()) net.addChannel(currentServer, "TEXT", c.trim().toLowerCase().replace(" ", "-"));
                });
                textHeader.add(addTxt, BorderLayout.EAST);
            }
            channelsPanel.add(textHeader);
            
            boolean foundText = false;
            for (String chan : textChannels) {
                JButton txtBtn = createChannelBtn("# " + chan, true);
                txtBtn.addActionListener(e -> {
                    setActiveChannelBtn(txtBtn);
                    currentTextChannel = chan;
                    chatArea.setText("");
                    net.joinTextChannel(currentServer, chan);
                    updateChatHeader();
                });
                channelsPanel.add(txtBtn);
                if (chan.equals(currentTextChannel) || !foundText) {
                    currentTextChannel = chan;
                    setActiveChannelBtn(txtBtn);
                    foundText = true;
                }
            }
            
            channelsPanel.add(Box.createVerticalStrut(20));
            
            // Voice Channels
            JPanel voiceHeader = new JPanel(new BorderLayout());
            voiceHeader.setBackground(BG_DARKER);
            voiceHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
            voiceHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            voiceHeader.add(createCategoryLabel("CANALES DE VOZ"), BorderLayout.WEST);
            if (isOwner) {
                JButton addVc = new JButton("+");
                addVc.setForeground(Color.WHITE);
                addVc.setFocusPainted(false);
                addVc.setContentAreaFilled(false);
                addVc.setBorderPainted(false);
                addVc.setCursor(new Cursor(Cursor.HAND_CURSOR));
                addVc.addActionListener(e -> {
                    String c = JOptionPane.showInputDialog(this, "Nombre del canal de voz:");
                    if (c != null && !c.trim().isEmpty()) net.addChannel(currentServer, "VOICE", c.trim().toLowerCase().replace(" ", "-"));
                });
                voiceHeader.add(addVc, BorderLayout.EAST);
            }
            channelsPanel.add(voiceHeader);
            
            voiceUsersPanel.removeAll();
            for (String chan : voiceChannels) {
                JButton vcBtn = createChannelBtn("[Voz] " + chan, false);
                vcBtn.addActionListener(e -> net.joinVoiceChannel(currentServer, chan));
                channelsPanel.add(vcBtn);
                channelsPanel.add(voiceUsersPanel);
            }
            
            channelsPanel.add(Box.createVerticalGlue());
            channelsPanel.revalidate();
            channelsPanel.repaint();
            
            chatArea.setText("");
            if (!textChannels.isEmpty()) {
                net.joinTextChannel(currentServer, currentTextChannel);
                updateChatHeader();
            }
            
            Component usersTitleObj = findComponentByName(mainPanel, "usersTitle");
            if (usersTitleObj instanceof JLabel) {
                ((JLabel) usersTitleObj).setText("USUARIOS EN SV (" + onlineUsersModel.getSize() + ")");
            }
            listOnlineUsers.repaint();
        });
    }

    class AvatarIcon implements Icon {
        private String user;
        private String letter;
        private Color color;
        public AvatarIcon(String user, String letter, Color color) {
            this.user = user;
            this.letter = letter;
            this.color = color;
        }
        public int getIconWidth() { return 36; }
        public int getIconHeight() { return 36; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            if (customAvatars.containsKey(user)) {
                g2.setClip(new java.awt.geom.Ellipse2D.Float(x, y, 36, 36));
                g2.drawImage(customAvatars.get(user), x, y, null);
            } else {
                g2.setColor(color);
                g2.fillOval(x, y, 36, 36);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                FontMetrics fm = g2.getFontMetrics();
                int sw = fm.stringWidth(letter);
                int sh = fm.getAscent();
                g2.drawString(letter, x + (36 - sw) / 2, y + (36 + sh) / 2 - 2);
            }
            g2.dispose();
        }
    }
}

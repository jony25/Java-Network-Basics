/*
 * Decompiled with CFR 0.152.
 */
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

public class ChatWindow extends JFrame
implements NetworkListener {
    private final NetworkController net;
    private JTextPane chatArea;
    private JTextField inputField;
    private JTextField userField;
    private JPasswordField passField;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JButton btnVoice;
    private final Color BG_DARK = new Color(54, 57, 63);
    private final Color BG_DARKER = new Color(47, 49, 54);
    private final Color BG_DARKEST = new Color(32, 34, 37);
    private final Color TEXT_NORMAL = new Color(220, 221, 222);
    private final Color BLURPLE = new Color(88, 101, 242);
    private final Color GREEN = new Color(67, 181, 129);
    private final DefaultListModel<String> onlineUsersModel = new DefaultListModel();
    private final Map<String, DefaultListModel<String>> voiceUsersMap = new ConcurrentHashMap<>();
    private JList<String> listOnlineUsers;
    private final Map<String, JPanel> voiceUsersPanels = new ConcurrentHashMap<>();
    private final Map<String, JButton> voiceChannelBtns = new ConcurrentHashMap<>();
    private final Map<String, Integer> currentVoiceLimits = new ConcurrentHashMap<>();
    private Point initialClick;
    private String currentServer = "Servidor UPV";
    private String currentTextChannel = "general";
    private JButton activeTextChannelBtn;
    private JButton activeServerBtn;
    private JPanel serverListPanel;
    private JPanel channelsPanel;
    private String currentServerOwner = "";
    private List<String> currentServerMembers = new java.util.ArrayList<>();
    private final Map<String, Image> customAvatars = new ConcurrentHashMap<String, Image>();
    private String myStatus = "ONLINE";
    private boolean isManualStatus = false;
    private final Map<String, String> userStatuses = new ConcurrentHashMap<>();
    private final Map<String, String> userBios = new ConcurrentHashMap<>();

    public ChatWindow(NetworkController networkController) {
        this.net = networkController;
        this.net.setListener(this);
        this.initUI();
    }

    private void initUI() {
        this.setTitle("Discord Clone");
        this.setSize(900, 600);
        this.setDefaultCloseOperation(3);
        this.setUndecorated(true);
        this.setMaximizedBounds(java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        this.setBackground(this.BG_DARK);
        this.cardLayout = new CardLayout();
        this.mainPanel = new JPanel(this.cardLayout);
        this.mainPanel.add((Component)this.createLoginPanel(), "LOGIN");
        this.mainPanel.add((Component)this.createChatPanel(), "CHAT");
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.add((Component)this.buildTitleBar(), "North");
        jPanel.add((Component)this.mainPanel, "Center");
        this.add(jPanel);
        this.setLocationRelativeTo(null);
        
        long[] lastActivity = {System.currentTimeMillis()};
        java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            lastActivity[0] = System.currentTimeMillis();
            // Only auto-revert from IDLE if it was set automatically (not manually)
            if ("IDLE".equals(myStatus) && !isManualStatus) {
                myStatus = "ONLINE";
                net.setStatus("ONLINE");
            }
        }, java.awt.AWTEvent.MOUSE_EVENT_MASK | java.awt.AWTEvent.KEY_EVENT_MASK);
        
        new javax.swing.Timer(60000, e -> {
            // Only auto-set IDLE if status is ONLINE and not manually overridden
            if ("ONLINE".equals(myStatus) && !isManualStatus && System.currentTimeMillis() - lastActivity[0] > 15 * 60 * 1000) {
                myStatus = "IDLE";
                net.setStatus("IDLE");
            }
        }).start();
    }

    private JPanel buildTitleBar() {
        final JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.setBackground(new Color(32, 34, 37));
        jPanel.setPreferredSize(new Dimension(800, 24));
        jPanel.addMouseListener(new MouseAdapter(){

            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                ChatWindow.this.initialClick = mouseEvent.getPoint();
                ChatWindow.this.getComponentAt(ChatWindow.this.initialClick);
            }
        });
        jPanel.addMouseMotionListener(new MouseAdapter(){

            @Override
            public void mouseDragged(MouseEvent mouseEvent) {
                int n = ChatWindow.this.getLocation().x;
                int n2 = ChatWindow.this.getLocation().y;
                int n3 = mouseEvent.getX() - ChatWindow.this.initialClick.x;
                int n4 = mouseEvent.getY() - ChatWindow.this.initialClick.y;
                int n5 = n + n3;
                int n6 = n2 + n4;
                ChatWindow.this.setLocation(n5, n6);
            }
        });
        JLabel jLabel = new JLabel("  Discord Clone");
        jLabel.setForeground(new Color(114, 118, 125));
        jLabel.setFont(new Font("Segoe UI", 1, 12));
        jPanel.add((Component)jLabel, "West");
        JPanel jPanel2 = new JPanel(new FlowLayout(2, 0, 0));
        jPanel2.setOpaque(false);
        JButton jButton = this.createTitleBtn("_");
        jButton.addActionListener(actionEvent -> this.setState(1));
        JButton jButton2 = this.createTitleBtn("\u2610");
        jButton2.addActionListener(actionEvent -> {
            if (this.getExtendedState() == 6) {
                this.setExtendedState(0);
            } else {
                this.setExtendedState(6);
            }
        });
        final JButton jButton3 = this.createTitleBtn("X");
        jButton3.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                jButton3.setBackground(new Color(237, 66, 69));
                jButton3.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                jButton3.setBackground(jPanel.getBackground());
                jButton3.setForeground(new Color(185, 187, 190));
            }
        });
        jButton3.addActionListener(actionEvent -> System.exit(0));
        jPanel2.add(jButton);
        jPanel2.add(jButton2);
        jPanel2.add(jButton3);
        jPanel.add((Component)jPanel2, "East");
        return jPanel;
    }

    private JButton createTitleBtn(String string) {
        JButton jButton = new JButton(string);
        jButton.setPreferredSize(new Dimension(46, 24));
        jButton.setBorderPainted(false);
        jButton.setFocusPainted(false);
        jButton.setContentAreaFilled(false);
        jButton.setOpaque(true);
        jButton.setBackground(new Color(32, 34, 37));
        jButton.setForeground(new Color(185, 187, 190));
        jButton.setFont(new Font("Segoe UI", 0, 12));
        return jButton;
    }

    private JPanel createLoginPanel() {
        Object object;
        JPanel jPanel = new JPanel(new GridBagLayout());
        jPanel.setBackground(this.BG_DARK);
        JPanel jPanel2 = new JPanel();
        jPanel2.setLayout(new BoxLayout(jPanel2, 1));
        jPanel2.setBackground(this.BG_DARK);
        jPanel2.setBorder(new EmptyBorder(32, 32, 32, 32));
        String string = "";
        String string2 = "";
        boolean bl = false;
        File file = new File("auth.config");
        if (file.exists()) {
            try {
                object = new Scanner(file);
                if (((Scanner)object).hasNextLine()) {
                    string = ((Scanner)object).nextLine();
                }
                if (((Scanner)object).hasNextLine()) {
                    string2 = ((Scanner)object).nextLine();
                }
                bl = true;
                ((Scanner)object).close();
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        object = new JLabel("\u00a1Te damos la bienvenida de nuevo!");
        ((JComponent)object).setForeground(Color.WHITE);
        ((JComponent)object).setFont(new Font("Segoe UI", 1, 22));
        ((JComponent)object).setAlignmentX(0.5f);
        JLabel jLabel = new JLabel("\u00a1Nos alegramos mucho de volver a verte!");
        jLabel.setForeground(new Color(185, 187, 190));
        jLabel.setFont(new Font("Segoe UI", 0, 14));
        jLabel.setAlignmentX(0.5f);
        this.userField = this.createStyledTextField("CORREO O N\u00daMERO DE TEL\u00c9FONO");
        if (!string.isEmpty()) {
            this.userField.setText(string);
        }
        this.passField = this.createStyledPasswordField("CONTRASE\u00d1A");
        if (!string2.isEmpty()) {
            this.passField.setText(string2);
        }
        JCheckBox jCheckBox = new JCheckBox("Recordarme");
        jCheckBox.setBackground(this.BG_DARK);
        jCheckBox.setForeground(new Color(185, 187, 190));
        jCheckBox.setFont(new Font("Segoe UI", 0, 12));
        jCheckBox.setFocusPainted(false);
        jCheckBox.setSelected(bl);
        JPanel jPanel3 = new JPanel();
        jPanel3.setLayout(new BoxLayout(jPanel3, 1));
        jPanel3.setBackground(this.BG_DARK);
        JButton jButton = this.createStyledButton("Iniciar sesi\u00f3n", this.BLURPLE);
        JButton jButton2 = this.createStyledButton("Registrarse", this.BG_DARKER);
        ActionListener actionListener = actionEvent -> {
            if (jCheckBox.isSelected()) {
                try {
                    FileWriter fileWriter = new FileWriter("auth.config");
                    fileWriter.write(this.userField.getText() + "\n" + new String(this.passField.getPassword()));
                    fileWriter.close();
                }
                catch (Exception exception) {}
            } else {
                new File("auth.config").delete();
            }
            if (actionEvent.getSource() == jButton) {
                this.net.login(this.userField.getText(), new String(this.passField.getPassword()));
            } else {
                this.net.register(this.userField.getText(), new String(this.passField.getPassword()));
            }
        };
        jButton.addActionListener(actionListener);
        jButton2.addActionListener(actionListener);
        JPanel jPanel4 = new JPanel(new FlowLayout(1, 0, 0));
        jPanel4.setBackground(this.BG_DARK);
        jPanel4.add(jButton);
        JPanel jPanel5 = new JPanel(new FlowLayout(1, 0, 0));
        jPanel5.setBackground(this.BG_DARK);
        jPanel5.add(jButton2);
        JPanel jPanel6 = new JPanel(new FlowLayout(1, 0, 0));
        jPanel6.setBackground(this.BG_DARK);
        jPanel6.add(jCheckBox);
        jPanel3.add(jPanel6);
        jPanel3.add(Box.createVerticalStrut(15));
        jPanel3.add(jPanel4);
        jPanel3.add(Box.createVerticalStrut(10));
        jPanel3.add(jPanel5);
        jPanel2.add((Component)object);
        jPanel2.add(Box.createVerticalStrut(10));
        jPanel2.add(jLabel);
        jPanel2.add(Box.createVerticalStrut(20));
        jPanel2.add(this.userField);
        jPanel2.add(Box.createVerticalStrut(15));
        jPanel2.add(this.passField);
        jPanel2.add(Box.createVerticalStrut(20));
        jPanel2.add(jPanel3);
        jPanel.add(jPanel2);
        
        if (bl && !string.isEmpty() && !string2.isEmpty()) {
            SwingUtilities.invokeLater(() -> jButton.doClick());
        }
        
        return jPanel;
    }

    private JPanel createChatPanel() {
        JComponent jComponent;
        JPanel jPanel = new JPanel(new BorderLayout());
        this.serverListPanel = new JPanel();
        this.serverListPanel.setLayout(new BoxLayout(this.serverListPanel, 1));
        this.serverListPanel.setBackground(this.BG_DARKEST);
        this.serverListPanel.setPreferredSize(new Dimension(72, 0));
        this.serverListPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel jPanel2 = new JPanel(new BorderLayout());
        jPanel2.setBackground(this.BG_DARKER);
        jPanel2.setPreferredSize(new Dimension(240, 0));
        this.channelsPanel = new JPanel();
        this.channelsPanel.setLayout(new BoxLayout(this.channelsPanel, 1));
        this.channelsPanel.setBackground(this.BG_DARKER);
        this.channelsPanel.setBorder(new EmptyBorder(15, 10, 10, 10));
        jPanel2.add((Component)this.channelsPanel, "Center");
        JPanel jPanel3 = new JPanel();
        jPanel3.setLayout(new BoxLayout(jPanel3, 1));
        jPanel3.setBackground(new Color(41, 43, 47));
        jPanel3.setBorder(new EmptyBorder(8, 8, 8, 8));
        this.btnVoice = new JButton("Desconectado");
        this.btnVoice.setBackground(this.BG_DARKER);
        this.btnVoice.setForeground(this.TEXT_NORMAL);
        this.btnVoice.setFocusPainted(false);
        this.btnVoice.setBorderPainted(false);
        this.btnVoice.setAlignmentX(0.0f);
        this.btnVoice.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        this.btnVoice.setCursor(new Cursor(12));
        this.btnVoice.addActionListener(actionEvent -> this.net.leaveVoiceChannel());
        jPanel3.add(this.btnVoice);
        jPanel3.add(Box.createVerticalStrut(6));
        JPanel jPanel4 = new JPanel(new BorderLayout(6, 0));
        jPanel4.setBackground(new Color(41, 43, 47));
        jPanel4.setAlignmentX(0.0f);
        jPanel4.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JLabel jLabel = new JLabel(){

            @Override
            public void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                String string = ChatWindow.this.userField.getText();
                String string2 = string == null || string.isEmpty() ? "?" : string.substring(0, 1).toUpperCase();
                Color statusColor = myStatus.equals("ONLINE") ? GREEN : (myStatus.equals("IDLE") ? new Color(250, 166, 26) : new Color(237, 66, 69));
                new AvatarIcon(string == null ? "" : string, string2, ChatWindow.this.BLURPLE, statusColor).paintIcon(this, graphics, 2, 2);
            }
        };
        jLabel.setPreferredSize(new Dimension(36, 36));
        jLabel.setCursor(new Cursor(12));
        jLabel.addMouseListener(new MouseAdapter(){
            @Override
            public void mousePressed(MouseEvent e) {
                JPopupMenu statusMenu = new JPopupMenu();
                statusMenu.setBackground(new Color(24, 25, 28));
                JMenuItem itemOnline = createMenuItem("Conectado", GREEN);
                itemOnline.addActionListener(ev -> { myStatus = "ONLINE"; isManualStatus = false; net.setStatus("ONLINE"); jPanel4.repaint(); });
                JMenuItem itemIdle = createMenuItem("Ausente", new Color(250, 166, 26));
                itemIdle.addActionListener(ev -> { myStatus = "IDLE"; isManualStatus = true; net.setStatus("IDLE"); jPanel4.repaint(); });
                JMenuItem itemDnd = createMenuItem("No molestar", new Color(237, 66, 69));
                itemDnd.addActionListener(ev -> { myStatus = "DND"; isManualStatus = true; net.setStatus("DND"); jPanel4.repaint(); });
                statusMenu.add(itemOnline); statusMenu.add(itemIdle); statusMenu.add(itemDnd);
                statusMenu.show(jLabel, e.getX(), e.getY());
            }
        });
        jPanel4.add((Component)jLabel, "West");
        JLabel jLabel2 = new JLabel();
        jLabel2.setName("bottomUserName");
        jLabel2.setForeground(Color.WHITE);
        jLabel2.setFont(new Font("Segoe UI", 1, 13));
        jPanel4.add((Component)jLabel2, "Center");
        final JButton jButton = new JButton("\u2699");
        jButton.setForeground(this.TEXT_NORMAL);
        jButton.setBackground(new Color(41, 43, 47));
        jButton.setFocusPainted(false);
        jButton.setBorderPainted(false);
        jButton.setFont(new Font("Segoe UI", 0, 18));
        jButton.setCursor(new Cursor(12));
        jButton.setPreferredSize(new Dimension(36, 36));
        jButton.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                jButton.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                jButton.setForeground(ChatWindow.this.TEXT_NORMAL);
            }
        });
        jButton.addActionListener(actionEvent -> this.showSettingsDialog());
        jPanel4.add((Component)jButton, "East");
        jPanel3.add(jPanel4);
        jPanel2.add((Component)jPanel3, "South");
        JPanel jPanel5 = new JPanel(new BorderLayout());
        jPanel5.setBackground(this.BG_DARK);
        JPanel jPanel6 = new JPanel(new BorderLayout());
        jPanel6.setBackground(this.BG_DARK);
        jPanel6.setBorder(new MatteBorder(0, 0, 1, 0, this.BG_DARKEST));
        JLabel jLabel3 = new JLabel(" # general");
        jLabel3.setName("chatHeaderTitle");
        jLabel3.setForeground(Color.WHITE);
        jLabel3.setFont(new Font("Segoe UI", 1, 16));
        jLabel3.setBorder(new EmptyBorder(15, 15, 15, 15));
        jPanel6.add((Component)jLabel3, "West");
        jPanel5.add((Component)jPanel6, "North");
        this.chatArea = new JTextPane();
        this.chatArea.setEditable(false);
        this.chatArea.setBackground(new Color(54, 57, 63));
        this.chatArea.setForeground(this.TEXT_NORMAL);
        this.chatArea.setFont(new Font("Segoe UI Emoji", 0, 15));
        this.chatArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane jScrollPane = new JScrollPane(this.chatArea);
        jScrollPane.setBorder(null);
        jScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        jPanel5.add((Component)jScrollPane, "Center");
        JPanel jPanel7 = new JPanel(new BorderLayout());
        jPanel7.setBackground(this.BG_DARK);
        jPanel7.setBorder(new EmptyBorder(20, 20, 20, 20));
        JPanel jPanel8 = new JPanel(new BorderLayout());
        jPanel8.setBackground(this.BG_DARKER);
        this.inputField = new JTextField();
        this.inputField.setBackground(new Color(64, 68, 75));
        this.inputField.setForeground(Color.WHITE);
        this.inputField.setCaretColor(Color.WHITE);
        this.inputField.setFont(new Font("Segoe UI Emoji", 0, 14));
        this.inputField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(64, 68, 75), 10, true), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        this.inputField.addActionListener(actionEvent -> this.sendChat());
        
        JPanel jPanel10 = new JPanel(new BorderLayout());
        jPanel10.setBackground(new Color(64, 68, 75));
        jPanel10.setBorder(BorderFactory.createLineBorder(new Color(64, 68, 75), 2, true));
        jPanel10.add((Component)this.inputField, "Center");
        jPanel8.add((Component)jPanel10, "Center");
        jPanel7.add((Component)jPanel8, "Center");
        jPanel5.add((Component)jPanel7, "South");
        JPanel jPanel11 = new JPanel(new BorderLayout());
        jPanel11.setBackground(this.BG_DARKER);
        jPanel11.setPreferredSize(new Dimension(240, 0));
        jPanel11.setBorder(new EmptyBorder(15, 15, 15, 15));
        JLabel jLabel4 = this.createCategoryLabel("USUARIOS (N/A)");
        jLabel4.setName("usersTitle");
        jPanel11.add((Component)jLabel4, "North");
        this.listOnlineUsers = new JList<String>(this.onlineUsersModel);
        this.listOnlineUsers.setBackground(this.BG_DARKER);
        this.listOnlineUsers.setForeground(this.TEXT_NORMAL);
        this.listOnlineUsers.setFont(new Font("Segoe UI", 0, 14));
        this.listOnlineUsers.setSelectionBackground(new Color(52, 55, 60));
        this.listOnlineUsers.setSelectionForeground(Color.WHITE);
        this.listOnlineUsers.setBorder(null);
        this.listOnlineUsers.setCellRenderer(new DefaultListCellRenderer(){

            @Override
            public Component getListCellRendererComponent(JList<?> jList, Object object, int n, boolean bl, boolean bl2) {
                JLabel jLabel = (JLabel)super.getListCellRendererComponent(jList, object, n, bl, bl2);
                String string = object.toString();
                
                if (string.equals("[Conectados]") || string.equals("[Desconectados]")) {
                    jLabel.setForeground(new Color(142, 146, 151));
                    jLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    jLabel.setIcon(null);
                    jLabel.setBorder(new EmptyBorder(10, 5, 2, 5));
                    return jLabel;
                }
                
                String string2 = string.isEmpty() ? "?" : string.substring(0, 1).toUpperCase();
                Color color = string.equals(ChatWindow.this.userField.getText()) ? ChatWindow.this.BLURPLE : (ChatWindow.this.onlineUsersModel.contains(string) ? new Color(67, 181, 129) : new Color(116, 127, 141));
                if (ChatWindow.this.currentServerOwner.equals(string)) {
                    jLabel.setText(string + " (Admin)");
                } else {
                    jLabel.setText(string);
                }
                if (!ChatWindow.this.onlineUsersModel.contains(string)) {
                    jLabel.setForeground(new Color(116, 127, 141));
                } else {
                    jLabel.setForeground(ChatWindow.this.TEXT_NORMAL);
                }
                
                // Use real status from userStatuses map for the status indicator
                String userStatus = ChatWindow.this.userStatuses.getOrDefault(string, "OFFLINE");
                Color statusColor;
                switch (userStatus) {
                    case "ONLINE": statusColor = ChatWindow.this.GREEN; break;
                    case "IDLE": statusColor = new Color(250, 166, 26); break;
                    case "DND": statusColor = new Color(237, 66, 69); break;
                    default: statusColor = Color.GRAY; break;
                }
                jLabel.setIcon(new AvatarIcon(string, string2, color, statusColor));
                jLabel.setBorder(new EmptyBorder(4, 4, 4, 4));
                jLabel.setIconTextGap(10);
                return jLabel;
            }
        });
        this.listOnlineUsers.addMouseListener(new MouseAdapter(){

            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                this.showPopup(mouseEvent);
            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
                this.showPopup(mouseEvent);
            }

            private void showPopup(MouseEvent mouseEvent) {
                int n;
                if (mouseEvent.isPopupTrigger() && (n = ChatWindow.this.listOnlineUsers.locationToIndex(mouseEvent.getPoint())) != -1 && ChatWindow.this.listOnlineUsers.getCellBounds(n, n).contains(mouseEvent.getPoint())) {
                    ChatWindow.this.listOnlineUsers.setSelectedIndex(n);
                    String string = ChatWindow.this.listOnlineUsers.getSelectedValue();
                    if (string == null || string.equals("[Conectados]") || string.equals("[Desconectados]")) {
                        return;
                    }
                    JPopupMenu jPopupMenu = new JPopupMenu();
                    jPopupMenu.setBackground(new Color(24, 25, 28));
                    jPopupMenu.setBorder(BorderFactory.createLineBorder(new Color(40, 42, 46), 1));
                    JMenuItem jMenuItem = ChatWindow.this.createMenuItem("Ver Perfil de " + string, Color.WHITE);
                    jMenuItem.addActionListener(actionEvent -> showUserProfileDialog(string));
                    jPopupMenu.add(jMenuItem);
                    if (ChatWindow.this.currentServerOwner.equals(ChatWindow.this.userField.getText()) && !string.equals(ChatWindow.this.userField.getText())) {
                        jPopupMenu.addSeparator();
                        JMenuItem jMenuItem2 = ChatWindow.this.createMenuItem("Expulsar " + string, new Color(237, 66, 69));
                        jMenuItem2.addActionListener(actionEvent -> JOptionPane.showMessageDialog(ChatWindow.this, string + " expulsado (Simulaci\u00f3n).", "Expulsar", 1));
                        jPopupMenu.add(jMenuItem2);
                    }
                    jPopupMenu.show(ChatWindow.this.listOnlineUsers, mouseEvent.getX(), mouseEvent.getY());
                }
            }
        });
        JScrollPane jScrollPane2 = new JScrollPane(this.listOnlineUsers);
        jScrollPane2.setBorder(null);
        jPanel11.add((Component)jScrollPane2, "Center");
        jComponent = new JPanel(new BorderLayout());
        jComponent.add((Component)jPanel2, "West");
        jComponent.add((Component)jPanel5, "Center");
        jComponent.add((Component)jPanel11, "East");
        jPanel.add((Component)this.serverListPanel, "West");
        jPanel.add((Component)jComponent, "Center");
        return jPanel;
    }

    private JLabel createCategoryLabel(String string) {
        JLabel jLabel = new JLabel(string);
        jLabel.setForeground(new Color(142, 146, 151));
        jLabel.setFont(new Font("Segoe UI", 1, 12));
        jLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
        return jLabel;
    }

    private JButton createChannelBtn(String string, boolean bl) {
        final JButton jButton = new JButton(string);
        jButton.setForeground(new Color(142, 146, 151));
        jButton.setBackground(this.BG_DARKER);
        jButton.setFocusPainted(false);
        jButton.setBorderPainted(false);
        jButton.setContentAreaFilled(false);
        jButton.setOpaque(true);
        jButton.setFont(new Font("Segoe UI", 0, 15));
        jButton.setHorizontalAlignment(2);
        jButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        jButton.setAlignmentX(0.0f);
        jButton.setCursor(new Cursor(12));
        jButton.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                if (jButton != ChatWindow.this.activeTextChannelBtn) {
                    jButton.setBackground(new Color(52, 55, 60));
                    jButton.setForeground(Color.WHITE);
                }
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                if (jButton != ChatWindow.this.activeTextChannelBtn) {
                    jButton.setBackground(ChatWindow.this.BG_DARKER);
                    jButton.setForeground(new Color(142, 146, 151));
                }
            }
        });
        return jButton;
    }

    private void setActiveChannelBtn(JButton jButton) {
        if (this.activeTextChannelBtn != null) {
            this.activeTextChannelBtn.setBackground(this.BG_DARKER);
            this.activeTextChannelBtn.setForeground(new Color(142, 146, 151));
        }
        this.activeTextChannelBtn = jButton;
        if (jButton != null) {
            jButton.setBackground(new Color(66, 70, 77));
            jButton.setForeground(Color.WHITE);
        }
    }

    private JButton createServerIcon(String string, String string2) {
        final JButton jButton = new JButton(string);
        jButton.setName("srv_" + string2);
        jButton.setBackground(this.BG_DARK);
        jButton.setForeground(Color.WHITE);
        jButton.setFont(new Font("Segoe UI", 1, 18));
        jButton.setFocusPainted(false);
        jButton.setBorderPainted(false);
        jButton.setPreferredSize(new Dimension(48, 48));
        jButton.setMaximumSize(new Dimension(48, 48));
        jButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        jButton.setCursor(new Cursor(12));
        jButton.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                if (jButton != ChatWindow.this.activeServerBtn) {
                    jButton.setBackground(ChatWindow.this.BLURPLE);
                }
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                if (jButton != ChatWindow.this.activeServerBtn) {
                    jButton.setBackground(ChatWindow.this.BG_DARK);
                }
            }
        });
        return jButton;
    }

    private void setActiveServerBtn(JButton jButton) {
        if (this.activeServerBtn != null) {
            this.activeServerBtn.setBackground(this.BG_DARK);
        }
        this.activeServerBtn = jButton;
        if (jButton != null) {
            jButton.setBackground(this.BLURPLE);
        }
    }

    private void updateChatHeader() {
        this.chatArea.setText("");
        JLabel jLabel = (JLabel)this.findComponentByName(this.mainPanel, "chatHeaderTitle");
        if (jLabel != null) {
            jLabel.setText(" # " + this.currentTextChannel);
        }
    }

    private void switchServer(String string) {
        this.currentServer = string;
        this.net.getServerInfo(string);
    }

    private Component findComponentByName(Container container, String string) {
        for (Component component : container.getComponents()) {
            Component component2;
            if (string.equals(component.getName())) {
                return component;
            }
            if (!(component instanceof Container) || (component2 = this.findComponentByName((Container)component, string)) == null) continue;
            return component2;
        }
        return null;
    }

    private JTextField createStyledTextField(String string) {
        JTextField jTextField = new JTextField(20);
        this.styleInput(jTextField);
        return jTextField;
    }

    private JPasswordField createStyledPasswordField(String string) {
        JPasswordField jPasswordField = new JPasswordField(20);
        this.styleInput(jPasswordField);
        return jPasswordField;
    }

    private void styleInput(JTextField jTextField) {
        jTextField.setBackground(this.BG_DARKEST);
        jTextField.setForeground(Color.WHITE);
        jTextField.setCaretColor(Color.WHITE);
        jTextField.setFont(new Font("Segoe UI", 0, 14));
        jTextField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(this.BG_DARKEST, 8), BorderFactory.createEmptyBorder(4, 4, 4, 4)));
    }

    private JButton createStyledButton(String string, final Color color) {
        final JButton jButton = new JButton(string);
        jButton.setBackground(color);
        jButton.setForeground(Color.WHITE);
        jButton.setFocusPainted(false);
        jButton.setBorderPainted(false);
        jButton.setFont(new Font("Segoe UI", 1, 14));
        jButton.setPreferredSize(new Dimension(160, 45));
        jButton.setCursor(new Cursor(12));
        jButton.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                jButton.setBackground(color.brighter());
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                jButton.setBackground(color);
            }
        });
        return jButton;
    }

    private void sendChat() {
        String string = this.inputField.getText().trim();
        if (!string.isEmpty()) {
            this.net.sendMessage(this.currentServer, this.currentTextChannel, string);
            this.inputField.setText("");
        }
    }

    private JMenuItem createMenuItem(String string, Color color) {
        final JMenuItem jMenuItem = new JMenuItem(string);
        jMenuItem.setForeground(color);
        jMenuItem.setBackground(new Color(24, 25, 28));
        jMenuItem.setFont(new Font("Segoe UI", 0, 13));
        jMenuItem.setBorder(new EmptyBorder(6, 12, 6, 12));
        jMenuItem.setOpaque(true);
        jMenuItem.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                jMenuItem.setBackground(ChatWindow.this.BLURPLE);
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                jMenuItem.setBackground(new Color(24, 25, 28));
            }
        });
        return jMenuItem;
    }

    private String renderEmojis(String text) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='margin:0;padding:0;'>");
        int i = 0;
        boolean hasEmoji = false;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            int charCount = Character.charCount(cp);
            if (isEmoji(cp)) {
                hasEmoji = true;
                StringBuilder hex = new StringBuilder();
                hex.append(String.format("%x", cp));
                // Check for variation selector or ZWJ sequences
                int next = i + charCount;
                while (next < text.length()) {
                    int nextCp = text.codePointAt(next);
                    if (nextCp == 0xFE0F || nextCp == 0x200D || isEmoji(nextCp)) {
                        if (nextCp != 0xFE0F) { // skip variation selector in URL
                            hex.append("-").append(String.format("%x", nextCp));
                        }
                        next += Character.charCount(nextCp);
                    } else {
                        break;
                    }
                }
                String url = "https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/" + hex + ".png";
                sb.append("<img src='").append(url).append("' width='20' height='20'>");
                i = next;
            } else {
                // Escape HTML special chars
                char ch = (char)cp;
                if (ch == '<') sb.append("&lt;");
                else if (ch == '>') sb.append("&gt;");
                else if (ch == '&') sb.append("&amp;");
                else sb.append(ch);
                i += charCount;
            }
        }
        sb.append("</body></html>");
        return hasEmoji ? sb.toString() : text;
    }
    
    private boolean isEmoji(int cp) {
        if (cp >= 0x1F600 && cp <= 0x1F64F) return true; // Emoticons
        if (cp >= 0x1F300 && cp <= 0x1F5FF) return true; // Misc Symbols and Pictographs
        if (cp >= 0x1F680 && cp <= 0x1F6FF) return true; // Transport and Map
        if (cp >= 0x1F1E0 && cp <= 0x1F1FF) return true; // Flags
        if (cp >= 0x2600 && cp <= 0x26FF) return true;   // Misc symbols
        if (cp >= 0x2700 && cp <= 0x27BF) return true;   // Dingbats
        if (cp >= 0xFE00 && cp <= 0xFE0F) return true;   // Variation Selectors
        if (cp >= 0x1F900 && cp <= 0x1F9FF) return true; // Supplemental Symbols
        if (cp >= 0x1FA00 && cp <= 0x1FA6F) return true; // Chess Symbols
        if (cp >= 0x1FA70 && cp <= 0x1FAFF) return true; // Symbols Extended-A
        if (cp >= 0x231A && cp <= 0x231B) return true;
        if (cp == 0x200D) return true; // ZWJ
        if (cp >= 0x2300 && cp <= 0x23FF) return true;
        if (cp >= 0x2B05 && cp <= 0x2B07) return true;
        if (cp == 0x2B1B || cp == 0x2B1C || cp == 0x2B50 || cp == 0x2B55) return true;
        if (cp >= 0x2934 && cp <= 0x2935) return true;
        if (cp >= 0x25AA && cp <= 0x25AB) return true;
        if (cp >= 0x25FB && cp <= 0x25FE) return true;
        if (cp == 0x2139 || cp == 0x2194 || cp == 0x2195 || cp == 0x2196) return true;
        if (cp >= 0x2197 && cp <= 0x2199) return true;
        if (cp == 0x21A9 || cp == 0x21AA) return true;
        if (cp == 0x23CF || cp == 0x23E9 || cp == 0x23EA || cp == 0x23EB || cp == 0x23EC) return true;
        if (cp == 0x23ED || cp == 0x23EE || cp == 0x23EF || cp == 0x23F0 || cp == 0x23F1) return true;
        if (cp == 0x23F2 || cp == 0x23F3 || cp == 0x23F8 || cp == 0x23F9 || cp == 0x23FA) return true;
        if (cp >= 0xE0020 && cp <= 0xE007F) return true; // Tags
        return false;
    }

    private void appendText(String string, Color color, boolean bl) {
        try {
            StyleContext styleContext = StyleContext.getDefaultStyleContext();
            AttributeSet attributeSet = styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color);
            attributeSet = styleContext.addAttribute(attributeSet, StyleConstants.FontFamily, "Segoe UI Emoji");
            attributeSet = styleContext.addAttribute(attributeSet, StyleConstants.Alignment, 3);
            attributeSet = styleContext.addAttribute(attributeSet, StyleConstants.Bold, bl);
            int n = this.chatArea.getDocument().getLength();
            this.chatArea.getDocument().insertString(n, string, attributeSet);
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(String string, String string2) {
        SwingUtilities.invokeLater(() -> {
            try {
                boolean bl = string.equals(this.userField.getText());
                Color color = bl ? this.BLURPLE : new Color(67, 181, 129);
                String string3 = string.isEmpty() ? "?" : string.substring(0, 1).toUpperCase();
                String string4 = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                JPanel jPanel = new JPanel(new BorderLayout(10, 0));
                jPanel.setBackground(this.BG_DARK);
                jPanel.setBorder(new EmptyBorder(6, 10, 6, 10));
                JLabel jLabel = new JLabel(new AvatarIcon(string, string3, color, null));
                jLabel.setVerticalAlignment(1);
                jLabel.setPreferredSize(new Dimension(40, 40));
                jLabel.setBorder(new EmptyBorder(2, 0, 0, 0));
                jPanel.add((Component)jLabel, "West");
                JPanel jPanel2 = new JPanel();
                jPanel2.setLayout(new BoxLayout(jPanel2, 1));
                jPanel2.setBackground(this.BG_DARK);
                jPanel2.setBorder(new EmptyBorder(0, 0, 0, 0));
                JPanel jPanel3 = new JPanel();
                jPanel3.setLayout(new BoxLayout(jPanel3, 0));
                jPanel3.setBackground(this.BG_DARK);
                jPanel3.setAlignmentX(0.0f);
                JLabel jLabel2 = new JLabel(string);
                jLabel2.setForeground(color);
                jLabel2.setFont(new Font("Segoe UI", 1, 14));
                JLabel jLabel3 = new JLabel("  " + string4);
                jLabel3.setForeground(new Color(114, 118, 125));
                jLabel3.setFont(new Font("Segoe UI", 0, 12));
                jPanel3.add(jLabel2);
                jPanel3.add(jLabel3);
                jPanel3.add(Box.createHorizontalGlue());
                jPanel2.add(jPanel3);
                JLabel jLabel4 = new JLabel(renderEmojis(string2));
                jLabel4.setAlignmentX(0.0f);
                jLabel4.setForeground(this.TEXT_NORMAL);
                jLabel4.setFont(new Font("Segoe UI", 0, 14));
                jPanel2.add(jLabel4);
                jPanel.add((Component)jPanel2, "Center");
                jPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, jPanel.getPreferredSize().height + 10));
                int n = this.chatArea.getDocument().getLength();
                this.chatArea.setCaretPosition(n);
                this.chatArea.insertComponent(jPanel);
                this.appendText("\n", this.BG_DARK, false);
            }
            catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    @Override
    public void onAuthResult(boolean bl, String string) {
        SwingUtilities.invokeLater(() -> {
            if (bl) {
                this.cardLayout.show(this.mainPanel, "CHAT");
                this.net.joinTextChannel(this.currentServer, this.currentTextChannel);
            } else {
                JOptionPane.showMessageDialog(this, string, "Error", 0);
            }
        });
    }

    @Override
    public void onSystemMessage(String string) {
        SwingUtilities.invokeLater(() -> {
            if (string.equals("[VOZ ACTIVA]")) {
                this.btnVoice.setText("\ud83d\udcde Voz Conectada");
                this.btnVoice.setBackground(this.GREEN);
            } else if (string.equals("[VOZ OFF]")) {
                this.btnVoice.setText("\ud83d\udcde Desconectado");
                this.btnVoice.setBackground(this.BG_DARKER);
            } else if (string.startsWith("INVITE_CODE:")) {
                String string2 = string.substring(12);
                JTextField jTextField = new JTextField(string2);
                jTextField.setEditable(false);
                Object[] objectArray = new Object[]{"C\u00f3digo de invitaci\u00f3n generado (comp\u00e1rtelo):", jTextField};
                JOptionPane.showMessageDialog(this, objectArray, "Invitaci\u00f3n", 1);
            } else {
                this.appendText("\n" + string + "\n", new Color(114, 137, 218), false);
            }
        });
    }

    @Override
    public void onUserPresence(String string, String status) {
        SwingUtilities.invokeLater(() -> {
            userStatuses.put(string, status);
            boolean isOnline = !status.equals("OFFLINE");
            if (isOnline) {
                if (!this.onlineUsersModel.contains(string)) {
                    this.onlineUsersModel.addElement(string);
                }
            } else {
                this.onlineUsersModel.removeElement(string);
                userStatuses.remove(string);
            }
            updateOnlineUsersList();
        });
    }
    
    private void updateOnlineUsersList() {
        SwingUtilities.invokeLater(() -> {
            DefaultListModel<String> serverUsersModel = new DefaultListModel<>();
            List<String> online = new java.util.ArrayList<>();
            List<String> offline = new java.util.ArrayList<>();
            
            for (String member : currentServerMembers) {
                if (onlineUsersModel.contains(member)) {
                    online.add(member);
                } else {
                    offline.add(member);
                }
            }
            java.util.Collections.sort(online, String.CASE_INSENSITIVE_ORDER);
            java.util.Collections.sort(offline, String.CASE_INSENSITIVE_ORDER);
            
            if (!online.isEmpty()) serverUsersModel.addElement("[Conectados]");
            for (String u : online) serverUsersModel.addElement(u);
            if (!offline.isEmpty()) serverUsersModel.addElement("[Desconectados]");
            for (String u : offline) serverUsersModel.addElement(u);
            
            listOnlineUsers.setModel(serverUsersModel);
            
            Component usersTitleObj = findComponentByName(mainPanel, "usersTitle");
            if (usersTitleObj instanceof JLabel) {
                ((JLabel)usersTitleObj).setText("USUARIOS (" + currentServerMembers.size() + ")");
            }
            listOnlineUsers.repaint();
        });
    }

    @Override
    public void onVoicePresence(String channel, String username, boolean joining) {
        SwingUtilities.invokeLater(() -> {
            voiceUsersMap.putIfAbsent(channel, new DefaultListModel<>());
            DefaultListModel<String> model = voiceUsersMap.get(channel);
            if (joining) {
                if (!model.contains(username)) {
                    model.addElement(username);
                    if (!username.equals(this.userField.getText())) this.playSound("join");
                }
            } else {
                model.removeElement(username);
                if (!username.equals(this.userField.getText())) this.playSound("leave");
            }
            this.refreshVoiceUsersUI(channel);
        });
    }

    private void refreshVoiceUsersUI(String channel) {
        JPanel panel = voiceUsersPanels.get(channel);
        if (panel == null) return;
        panel.removeAll();
        DefaultListModel<String> model = voiceUsersMap.get(channel);
        int currentUse = model != null ? model.size() : 0;
        
        if (model != null) {
            for (int i = 0; i < model.size(); ++i) {
                String u = model.get(i);
                JLabel jLabel = new JLabel(u);
                String u2 = u.isEmpty() ? "?" : u.substring(0, 1).toUpperCase();
                Color color = u.equals(this.userField.getText()) ? this.BLURPLE : new Color(67, 181, 129);
                jLabel.setIcon(new AvatarIcon(u, u2, color, GREEN)); // TO DO: Real status color
                jLabel.setIconTextGap(8);
                jLabel.setForeground(this.TEXT_NORMAL);
                jLabel.setFont(new Font("Segoe UI", 0, 12));
                jLabel.setBorder(new EmptyBorder(2, 20, 2, 0));
                panel.add(jLabel);
                panel.add(Box.createVerticalStrut(4));
            }
        }
        
        JButton btn = voiceChannelBtns.get(channel);
        if (btn != null) {
            int limit = currentVoiceLimits.getOrDefault(channel, 0);
            String display = limit > 0 ? "[Voz] " + channel + " (" + currentUse + "/" + limit + ")" : "[Voz] " + channel;
            btn.setText(display);
        }
        
        panel.revalidate();
        panel.repaint();
    }

    @Override
    public void onAvatarReceived(String string, String string2) {
        SwingUtilities.invokeLater(() -> {
            try {
                byte[] byArray = Base64.getDecoder().decode(string2);
                BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(byArray));
                if (bufferedImage != null) {
                    this.customAvatars.put(string, bufferedImage.getScaledInstance(36, 36, 4));
                    this.listOnlineUsers.repaint();
                    for (JPanel p : this.voiceUsersPanels.values()) {
                        p.repaint();
                    }
                }
            }
            catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    @Override
    public void onBioReceived(String user, String bio) {
        SwingUtilities.invokeLater(() -> {
            userBios.put(user, bio);
        });
    }

    private void showUserProfileDialog(String username) {
        JDialog dialog = new JDialog(this, "Perfil de " + username, true);
        dialog.setSize(340, 350);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, 1));
        panel.setBackground(BG_DARK);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BG_DARKEST, 2),
            new EmptyBorder(20, 20, 20, 20)
        ));
        
        // Close button
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_DARK);
        topBar.setAlignmentX(0.5f);
        JButton closeBtn = createTitleBtn("X");
        closeBtn.addActionListener(e -> dialog.dispose());
        topBar.add(closeBtn, "East");
        panel.add(topBar);
        
        // Avatar
        String initial = username.isEmpty() ? "?" : username.substring(0, 1).toUpperCase();
        Color avatarColor = username.equals(this.userField.getText()) ? BLURPLE : new Color(67, 181, 129);
        String status = userStatuses.getOrDefault(username, "OFFLINE");
        Color statusColor;
        switch (status) {
            case "ONLINE": statusColor = GREEN; break;
            case "IDLE": statusColor = new Color(250, 166, 26); break;
            case "DND": statusColor = new Color(237, 66, 69); break;
            default: statusColor = Color.GRAY; break;
        }
        JLabel avatarLabel = new JLabel(new AvatarIcon(username, initial, avatarColor, statusColor));
        avatarLabel.setAlignmentX(0.5f);
        panel.add(avatarLabel);
        panel.add(Box.createVerticalStrut(12));
        
        // Username
        JLabel nameLabel = new JLabel(username);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Segoe UI", 1, 18));
        nameLabel.setAlignmentX(0.5f);
        panel.add(nameLabel);
        
        // Status text
        String statusText;
        switch (status) {
            case "ONLINE": statusText = "\u25cf Conectado"; break;
            case "IDLE": statusText = "\u25cf Ausente"; break;
            case "DND": statusText = "\u25cf No molestar"; break;
            default: statusText = "\u25cf Desconectado"; break;
        }
        JLabel statusLabel = new JLabel(statusText);
        statusLabel.setForeground(statusColor);
        statusLabel.setFont(new Font("Segoe UI", 0, 13));
        statusLabel.setAlignmentX(0.5f);
        panel.add(statusLabel);
        panel.add(Box.createVerticalStrut(15));
        
        // Separator
        JPanel sep = new JPanel();
        sep.setBackground(BG_DARKEST);
        sep.setMaximumSize(new Dimension(280, 1));
        sep.setAlignmentX(0.5f);
        panel.add(sep);
        panel.add(Box.createVerticalStrut(15));
        
        // Bio
        JLabel bioTitle = new JLabel("SOBRE M\u00cd");
        bioTitle.setForeground(new Color(142, 146, 151));
        bioTitle.setFont(new Font("Segoe UI", 1, 12));
        bioTitle.setAlignmentX(0.5f);
        panel.add(bioTitle);
        panel.add(Box.createVerticalStrut(5));
        
        String bio = userBios.getOrDefault(username, "Sin descripci\u00f3n.");
        JLabel bioLabel = new JLabel("<html><div style='text-align:center;width:260px'>" + bio + "</div></html>");
        bioLabel.setForeground(TEXT_NORMAL);
        bioLabel.setFont(new Font("Segoe UI", 0, 13));
        bioLabel.setAlignmentX(0.5f);
        panel.add(bioLabel);
        
        // Admin badge
        if (currentServerOwner.equals(username)) {
            panel.add(Box.createVerticalStrut(10));
            JLabel adminLabel = new JLabel("\u2605 Administrador del Servidor");
            adminLabel.setForeground(new Color(250, 166, 26));
            adminLabel.setFont(new Font("Segoe UI", 1, 12));
            adminLabel.setAlignmentX(0.5f);
            panel.add(adminLabel);
        }
        
        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void showServerInfoDialog(String serverName, String owner, List<String> members) {
        JDialog dialog = new JDialog(this, "Información del Servidor", true);
        dialog.setSize(380, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, 1));
        panel.setBackground(BG_DARK);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BG_DARKEST, 2),
            new EmptyBorder(20, 20, 20, 20)
        ));
        
        // Top bar with close button
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_DARK);
        topBar.setAlignmentX(0.5f);
        JLabel titleLbl = new JLabel("Información del Servidor");
        titleLbl.setForeground(Color.WHITE);
        titleLbl.setFont(new Font("Segoe UI", 1, 14));
        topBar.add(titleLbl, "West");
        JButton closeBtn = createTitleBtn("X");
        closeBtn.addActionListener(e -> dialog.dispose());
        topBar.add(closeBtn, "East");
        panel.add(topBar);
        panel.add(Box.createVerticalStrut(15));
        
        // Server icon (using first letter)
        String initial = serverName.isEmpty() ? "?" : serverName.substring(0, 1).toUpperCase();
        JLabel iconLabel = new JLabel(initial);
        iconLabel.setForeground(Color.WHITE);
        iconLabel.setFont(new Font("Segoe UI", 1, 28));
        iconLabel.setHorizontalAlignment(0);
        iconLabel.setPreferredSize(new Dimension(64, 64));
        iconLabel.setMaximumSize(new Dimension(64, 64));
        iconLabel.setOpaque(true);
        iconLabel.setBackground(BLURPLE);
        iconLabel.setAlignmentX(0.5f);
        panel.add(iconLabel);
        panel.add(Box.createVerticalStrut(12));
        
        // Server name
        JLabel nameLabel = new JLabel(serverName);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Segoe UI", 1, 20));
        nameLabel.setAlignmentX(0.5f);
        panel.add(nameLabel);
        panel.add(Box.createVerticalStrut(8));
        
        // Separator
        JPanel sep = new JPanel();
        sep.setBackground(BG_DARKEST);
        sep.setMaximumSize(new Dimension(340, 1));
        sep.setAlignmentX(0.5f);
        panel.add(sep);
        panel.add(Box.createVerticalStrut(12));
        
        // Info items
        JLabel ownerLabel = new JLabel("\u2605 Propietario: " + owner);
        ownerLabel.setForeground(new Color(250, 166, 26));
        ownerLabel.setFont(new Font("Segoe UI", 0, 14));
        ownerLabel.setAlignmentX(0.5f);
        panel.add(ownerLabel);
        panel.add(Box.createVerticalStrut(8));
        
        JLabel membersLabel = new JLabel("\u25cf Miembros: " + members.size());
        membersLabel.setForeground(TEXT_NORMAL);
        membersLabel.setFont(new Font("Segoe UI", 0, 14));
        membersLabel.setAlignmentX(0.5f);
        panel.add(membersLabel);
        panel.add(Box.createVerticalStrut(8));
        
        long onlineCount = members.stream().filter(m -> onlineUsersModel.contains(m)).count();
        JLabel onlineLabel = new JLabel("\u25cf En línea: " + onlineCount);
        onlineLabel.setForeground(GREEN);
        onlineLabel.setFont(new Font("Segoe UI", 0, 14));
        onlineLabel.setAlignmentX(0.5f);
        panel.add(onlineLabel);
        panel.add(Box.createVerticalStrut(20));
        
        // Admin options
        boolean isAdmin = owner.equals(this.userField.getText());
        if (isAdmin) {
            JPanel sep2 = new JPanel();
            sep2.setBackground(BG_DARKEST);
            sep2.setMaximumSize(new Dimension(340, 1));
            sep2.setAlignmentX(0.5f);
            panel.add(sep2);
            panel.add(Box.createVerticalStrut(12));
            
            JLabel adminTitle = new JLabel("OPCIONES DE ADMINISTRADOR");
            adminTitle.setForeground(new Color(142, 146, 151));
            adminTitle.setFont(new Font("Segoe UI", 1, 11));
            adminTitle.setAlignmentX(0.5f);
            panel.add(adminTitle);
            panel.add(Box.createVerticalStrut(10));
            
            JButton renameBtn = this.createStyledButton("Renombrar Servidor", BLURPLE);
            renameBtn.setMaximumSize(new Dimension(220, 36));
            renameBtn.setAlignmentX(0.5f);
            renameBtn.addActionListener(e -> {
                String newName = showCustomInputDialog("Renombrar", "Nuevo nombre del servidor:");
                if (newName != null && !newName.trim().isEmpty()) {
                    this.net.renameServer(serverName, newName.trim());
                    dialog.dispose();
                }
            });
            panel.add(renameBtn);
            panel.add(Box.createVerticalStrut(8));
            
            JButton iconBtn = this.createStyledButton("Cambiar Icono", BG_DARKER);
            iconBtn.setMaximumSize(new Dimension(220, 36));
            iconBtn.setAlignmentX(0.5f);
            iconBtn.addActionListener(e -> {
                JOptionPane.showMessageDialog(dialog, "Funcionalidad de icono de servidor en desarrollo.", "Info", 1);
            });
            panel.add(iconBtn);
        }
        
        dialog.add(panel);
        dialog.setVisible(true);
    }

    @Override
    public void onServerList(List<String> list) {
        SwingUtilities.invokeLater(() -> {
            this.serverListPanel.removeAll();
            JButton jButton = null;
            for (final String string : list) {
                String string2 = string.isEmpty() ? "?" : string.substring(0, 1).toUpperCase();
                final JButton jButton2 = this.createServerIcon(string2, string);
                jButton2.addActionListener(actionEvent -> {
                    this.setActiveServerBtn(jButton2);
                    this.switchServer(string);
                });
                jButton2.addMouseListener(new MouseAdapter(){

                    @Override
                    public void mousePressed(MouseEvent mouseEvent) {
                        this.showPopup(mouseEvent);
                    }

                    @Override
                    public void mouseReleased(MouseEvent mouseEvent) {
                        this.showPopup(mouseEvent);
                    }

                    private void showPopup(MouseEvent mouseEvent) {
                        if (mouseEvent.isPopupTrigger()) {
                            boolean bl;
                            JPopupMenu jPopupMenu = new JPopupMenu();
                            jPopupMenu.setBackground(new Color(24, 25, 28));
                            jPopupMenu.setBorder(BorderFactory.createLineBorder(new Color(40, 42, 46), 1));
                            JMenuItem jMenuItem = ChatWindow.this.createMenuItem("Informaci\u00f3n del Servidor", Color.WHITE);
                            jMenuItem.addActionListener(actionEvent -> {
                                ChatWindow.this.net.getServerInfo(string);
                                JOptionPane.showMessageDialog(ChatWindow.this, "Servidor: " + string, "Info", 1);
                            });
                            jPopupMenu.add(jMenuItem);
                            JMenuItem jMenuItem2 = ChatWindow.this.createMenuItem("Generar Invitaci\u00f3n", new Color(67, 181, 129));
                            jMenuItem2.addActionListener(actionEvent -> ChatWindow.this.net.generateInvite(string));
                            jPopupMenu.add(jMenuItem2);
                            if (string.equals(ChatWindow.this.currentServer) && ChatWindow.this.currentServerOwner.equals(ChatWindow.this.userField.getText())) {
                                JMenuItem jMenuItem3 = ChatWindow.this.createMenuItem("Renombrar Servidor", ChatWindow.this.BLURPLE);
                                jMenuItem3.addActionListener(actionEvent -> {
                                    String string2 = ChatWindow.this.showCustomInputDialog("Renombrar Servidor", "Nuevo nombre del servidor:");
                                    if (string2 != null && !string2.trim().isEmpty()) {
                                        ChatWindow.this.net.renameServer(string, string2.trim());
                                    }
                                });
                                jPopupMenu.add(jMenuItem3);
                            }
                            jPopupMenu.addSeparator();
                            boolean bl2 = bl = string.equals(ChatWindow.this.currentServer) && ChatWindow.this.currentServerOwner.equals(ChatWindow.this.userField.getText());
                            if (bl) {
                                JMenuItem jMenuItem4 = ChatWindow.this.createMenuItem("Eliminar Servidor", new Color(237, 66, 69));
                                jMenuItem4.addActionListener(actionEvent -> {
                                    int n = JOptionPane.showConfirmDialog(ChatWindow.this, "\u00bfSeguro que quieres borrar '" + string + "'? Esta acci\u00f3n es irreversible.", "Eliminar Servidor", 0, 2);
                                    if (n == 0) {
                                        ChatWindow.this.net.deleteServer(string);
                                    }
                                });
                                jPopupMenu.add(jMenuItem4);
                            } else {
                                JMenuItem jMenuItem5 = ChatWindow.this.createMenuItem("Abandonar Servidor", new Color(237, 66, 69));
                                jMenuItem5.addActionListener(actionEvent -> {
                                    int n = JOptionPane.showConfirmDialog(ChatWindow.this, "\u00bfSeguro que quieres abandonar '" + string + "'?", "Abandonar Servidor", 0, 2);
                                    if (n == 0) {
                                        ChatWindow.this.net.leaveServer(string);
                                    }
                                });
                                jPopupMenu.add(jMenuItem5);
                            }
                            jPopupMenu.show(jButton2, mouseEvent.getX(), mouseEvent.getY());
                        }
                    }
                });
                if (jButton == null || string.equals(this.currentServer)) {
                    jButton = jButton2;
                }
                this.serverListPanel.add(jButton2);
                this.serverListPanel.add(Box.createVerticalStrut(10));
            }
            JButton jButton3 = this.createServerIcon("+", "Create");
            jButton3.setForeground(new Color(67, 181, 129));
            jButton3.setBackground(this.BG_DARKEST);
            jButton3.addActionListener(actionEvent -> {
                String string;
                int n = this.showCustomOptionDialog("A\u00f1adir Servidor", "\u00bfQu\u00e9 deseas hacer?", new String[]{"Crear Servidor", "Usar Invitaci\u00f3n"});
                if (n == 0) {
                    String string2 = this.showCustomInputDialog("Crear Servidor", "Nombre del nuevo servidor:");
                    if (string2 != null && !string2.trim().isEmpty()) {
                        this.net.createServer(string2.trim());
                    }
                } else if (n == 1 && (string = this.showCustomInputDialog("Unirse", "Introduce el c\u00f3digo de invitaci\u00f3n:")) != null && !string.trim().isEmpty()) {
                    this.net.useInvite(string.trim());
                }
            });
            this.serverListPanel.add(jButton3);
            this.serverListPanel.revalidate();
            this.serverListPanel.repaint();
            if (jButton != null && this.activeServerBtn == null) {
                jButton.doClick();
            }
        });
    }

    @Override
    public void onServerInfo(String string, String string2, List<String> list, List<String> list2, Map<String, Integer> voiceLimits, List<String> members) {
        SwingUtilities.invokeLater(() -> {
            this.currentServer = string;
            this.currentServerOwner = string2;
            this.channelsPanel.removeAll();
            JLabel jLabel = new JLabel(string + " \u25BC");
            jLabel.setName("serverTitleLabel");
            jLabel.setForeground(Color.WHITE);
            jLabel.setFont(new Font("Segoe UI", 1, 16));
            jLabel.setAlignmentX(0.0f);
            jLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            jLabel.setCursor(new Cursor(12));
            jLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    showServerInfoDialog(string, string2, members);
                }
            });
            this.channelsPanel.add(jLabel);
            this.channelsPanel.add(Box.createVerticalStrut(20));
            boolean bl = string2.equals(this.userField.getText());
            JPanel jPanel = new JPanel(new BorderLayout());
            jPanel.setBackground(this.BG_DARKER);
            jPanel.setAlignmentX(0.0f);
            jPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            jPanel.add((Component)this.createCategoryLabel("CANALES DE TEXTO"), "West");
            if (bl) {
                JButton jButton = new JButton("+");
                jButton.setForeground(Color.WHITE);
                jButton.setFocusPainted(false);
                jButton.setContentAreaFilled(false);
                jButton.setBorderPainted(false);
                jButton.setCursor(new Cursor(12));
                jButton.addActionListener(actionEvent -> {
                    String channelName = this.showCustomInputDialog("Nuevo Canal", "Nombre del canal de texto:");
                    if (channelName != null && !channelName.trim().isEmpty()) {
                        this.net.addChannel(this.currentServer, "TEXT", channelName.trim());
                    }
                });
                jPanel.add((Component)jButton, "East");
            }
            this.channelsPanel.add(jPanel);
            boolean bl2 = false;
            for (String stringChannel : list) {
                JButton channelBtn = this.createChannelBtn("# " + stringChannel, true);
                channelBtn.addActionListener(arg_0 -> this.handleTextChannelClick(channelBtn, stringChannel, arg_0));
                if (bl) {
                    channelBtn.addMouseListener(new MouseAdapter(){
                        @Override
                        public void mousePressed(MouseEvent e) { if (e.isPopupTrigger()) showMenu(e); }
                        @Override
                        public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) showMenu(e); }
                        private void showMenu(MouseEvent e) {
                            JPopupMenu menu = new JPopupMenu();
                            menu.setBackground(new Color(24, 25, 28));
                            JMenuItem item = createMenuItem("Renombrar Canal", BLURPLE);
                            item.addActionListener(ev -> {
                                String newName = showCustomInputDialog("Renombrar", "Nuevo nombre:");
                                if (newName != null && !newName.trim().isEmpty()) {
                                    net.editChannel(currentServer, "TEXT", stringChannel, newName.trim(), 0);
                                }
                            });
                            menu.add(item);
                            menu.show(channelBtn, e.getX(), e.getY());
                        }
                    });
                }
                this.channelsPanel.add((Component)channelBtn);
                if (!stringChannel.equals(this.currentTextChannel) && bl2) continue;
                this.currentTextChannel = stringChannel;
                this.setActiveChannelBtn(channelBtn);
                bl2 = true;
            }
            this.channelsPanel.add(Box.createVerticalStrut(20));
            JPanel jPanel2 = new JPanel(new BorderLayout());
            jPanel2.setBackground(this.BG_DARKER);
            jPanel2.setAlignmentX(0.0f);
            jPanel2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            jPanel2.add((Component)this.createCategoryLabel("CANALES DE VOZ"), "West");
            if (bl) {
                JButton btnVoiceNew = new JButton("+");
                btnVoiceNew.setForeground(Color.WHITE);
                btnVoiceNew.setFocusPainted(false);
                btnVoiceNew.setContentAreaFilled(false);
                btnVoiceNew.setBorderPainted(false);
                btnVoiceNew.setCursor(new Cursor(12));
                btnVoiceNew.addActionListener(actionEvent -> {
                    String channelName = this.showCustomInputDialog("Nuevo Canal", "Nombre del canal de voz:");
                    if (channelName != null && !channelName.trim().isEmpty()) {
                        this.net.addChannel(this.currentServer, "VOICE", channelName.trim());
                    }
                });
                jPanel2.add((Component)btnVoiceNew, "East");
            }
            this.channelsPanel.add(jPanel2);
            voiceUsersPanels.clear();
            voiceChannelBtns.clear();
            currentVoiceLimits.clear();
            
            java.util.Iterator<String> it = list2.iterator();
            while (it.hasNext()) {
                String vcName = it.next();
                int limit = voiceLimits.getOrDefault(vcName, 0);
                currentVoiceLimits.put(vcName, limit);
                
                DefaultListModel<String> curModel = voiceUsersMap.get(vcName);
                int currentUse = curModel != null ? curModel.size() : 0;
                
                String display = limit > 0 ? "[Voz] " + vcName + " (" + currentUse + "/" + limit + ")" : "[Voz] " + vcName;
                JButton jButton = this.createChannelBtn(display, false);
                voiceChannelBtns.put(vcName, jButton);
                
                jButton.addActionListener(arg_0 -> this.handleVoiceChannelClick((String)vcName, arg_0));
                if (bl) {
                    jButton.addMouseListener(new MouseAdapter(){
                        @Override
                        public void mousePressed(MouseEvent e) { if (e.isPopupTrigger()) showMenu(e); }
                        @Override
                        public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) showMenu(e); }
                        private void showMenu(MouseEvent e) {
                            JPopupMenu menu = new JPopupMenu();
                            menu.setBackground(new Color(24, 25, 28));
                            JMenuItem item = createMenuItem("Editar Canal", BLURPLE);
                            item.addActionListener(ev -> {
                                String newName = showCustomInputDialog("Renombrar", "Nuevo nombre (vacío = no cambiar):");
                                String limitStr = showCustomInputDialog("Límite", "Límite de usuarios (0 = sin límite):");
                                if (newName != null && limitStr != null) {
                                    String finalName = newName.trim().isEmpty() ? vcName : newName.trim();
                                    int finalLimit = 0;
                                    try { finalLimit = Integer.parseInt(limitStr.trim()); } catch (Exception ex) {}
                                    net.editChannel(currentServer, "VOICE", vcName, finalName, finalLimit);
                                }
                            });
                            menu.add(item);
                            menu.show(jButton, e.getX(), e.getY());
                        }
                    });
                }
                this.channelsPanel.add(jButton);
                
                JPanel vp = new JPanel();
                vp.setLayout(new BoxLayout(vp, 1));
                vp.setBackground(this.BG_DARKER);
                vp.setBorder(new EmptyBorder(0, 20, 0, 0));
                vp.setAlignmentX(0.0f);
                voiceUsersPanels.put(vcName, vp);
                this.channelsPanel.add(vp);
                refreshVoiceUsersUI(vcName);
            }
            this.channelsPanel.add(Box.createVerticalGlue());
            this.channelsPanel.revalidate();
            this.channelsPanel.repaint();
            this.chatArea.setText("");
            if (!list.isEmpty()) {
                this.net.joinTextChannel(this.currentServer, this.currentTextChannel);
                this.updateChatHeader();
            }
            this.currentServerMembers = members;
            this.updateOnlineUsersList();
        });
    }

    private void showSettingsDialog() {
        JDialog jDialog = new JDialog(this, "Ajustes de Usuario", true);
        jDialog.setSize(400, 420);
        jDialog.setLocationRelativeTo(this);
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, 1));
        jPanel.setBackground(this.BG_DARK);
        jPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        JLabel jLabel = new JLabel("Mi Perfil");
        jLabel.setForeground(Color.WHITE);
        jLabel.setFont(new Font("Segoe UI", 1, 20));
        jLabel.setAlignmentX(0.5f);
        jPanel.add(jLabel);
        jPanel.add(Box.createVerticalStrut(20));
        String string = this.userField.getText();
        String string2 = string == null || string.isEmpty() ? "?" : string.substring(0, 1).toUpperCase();
        JLabel jLabel2 = new JLabel(new AvatarIcon(string == null ? "" : string, string2, this.BLURPLE, GREEN));
        jLabel2.setAlignmentX(0.5f);
        jPanel.add(jLabel2);
        jPanel.add(Box.createVerticalStrut(10));
        JLabel jLabel3 = new JLabel(string);
        jLabel3.setForeground(this.TEXT_NORMAL);
        jLabel3.setFont(new Font("Segoe UI", 0, 16));
        jLabel3.setAlignmentX(0.5f);
        jPanel.add(jLabel3);
        jPanel.add(Box.createVerticalStrut(15));
        
        // Bio section
        JLabel bioLabel = new JLabel("Descripci\u00f3n");
        bioLabel.setForeground(new Color(142, 146, 151));
        bioLabel.setFont(new Font("Segoe UI", 1, 12));
        bioLabel.setAlignmentX(0.5f);
        jPanel.add(bioLabel);
        jPanel.add(Box.createVerticalStrut(5));
        JTextField bioField = this.createStyledTextField(userBios.getOrDefault(string, ""));
        bioField.setMaximumSize(new Dimension(300, 35));
        bioField.setAlignmentX(0.5f);
        jPanel.add(bioField);
        jPanel.add(Box.createVerticalStrut(10));
        JButton btnSaveBio = this.createStyledButton("Guardar Descripci\u00f3n", new Color(67, 181, 129));
        btnSaveBio.setMaximumSize(new Dimension(200, 36));
        btnSaveBio.setAlignmentX(0.5f);
        btnSaveBio.addActionListener(e -> {
            String bio = bioField.getText().trim();
            this.net.setBio(bio);
            userBios.put(string, bio);
        });
        jPanel.add(btnSaveBio);
        jPanel.add(Box.createVerticalStrut(15));
        
        JButton jButton = this.createStyledButton("Cambiar Avatar", this.BLURPLE);
        jButton.setMaximumSize(new Dimension(200, 40));
        jButton.setAlignmentX(0.5f);
        jButton.addActionListener(actionEvent -> {
            JFileChooser jFileChooser = new JFileChooser();
            if (jFileChooser.showOpenDialog(jDialog) == 0) {
                try {
                    BufferedImage bufferedImage = ImageIO.read(jFileChooser.getSelectedFile());
                    if (bufferedImage != null) {
                        Image image = bufferedImage.getScaledInstance(64, 64, 4);
                        BufferedImage bufferedImage2 = new BufferedImage(64, 64, 1);
                        Graphics2D graphics2D = bufferedImage2.createGraphics();
                        graphics2D.drawImage(image, 0, 0, null);
                        graphics2D.dispose();
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        ImageIO.write((RenderedImage)bufferedImage2, "png", byteArrayOutputStream);
                        String base64Str = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
                        this.net.uploadAvatar(base64Str);
                        JOptionPane.showMessageDialog(jDialog, "Avatar actualizado.", "\u00c9xito", 1);
                        jDialog.dispose();
                    }
                }
                catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        });
        jPanel.add(jButton);
        jDialog.add(jPanel);
        jDialog.setVisible(true);
    }

    private String showCustomInputDialog(String string, String string2) {
        final JDialog jDialog = new JDialog(this, string, true);
        jDialog.setUndecorated(true);
        jDialog.setSize(360, 200);
        jDialog.setLocationRelativeTo(this);
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.setBackground(this.BG_DARK);
        jPanel.setBorder(BorderFactory.createLineBorder(this.BG_DARKEST, 2));
        JPanel jPanel2 = new JPanel(new BorderLayout());
        jPanel2.setBackground(this.BG_DARKER);
        jPanel2.setPreferredSize(new Dimension(360, 28));
        JLabel jLabel = new JLabel("  " + string);
        jLabel.setForeground(Color.WHITE);
        jLabel.setFont(new Font("Segoe UI", 1, 12));
        jPanel2.add((Component)jLabel, "West");
        JButton jButton = this.createTitleBtn("X");
        jButton.addActionListener(actionEvent -> jDialog.dispose());
        jPanel2.add((Component)jButton, "East");
        final Point[] pointArray = new Point[1];
        jPanel2.addMouseListener(new MouseAdapter(){

            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                pointArray[0] = mouseEvent.getPoint();
            }
        });
        jPanel2.addMouseMotionListener(new MouseAdapter(){

            @Override
            public void mouseDragged(MouseEvent mouseEvent) {
                Point point = jDialog.getLocation();
                jDialog.setLocation(point.x + mouseEvent.getX() - pointArray[0].x, point.y + mouseEvent.getY() - pointArray[0].y);
            }
        });
        jPanel.add((Component)jPanel2, "North");
        JPanel jPanel3 = new JPanel();
        jPanel3.setLayout(new BoxLayout(jPanel3, 1));
        jPanel3.setBackground(this.BG_DARK);
        jPanel3.setBorder(new EmptyBorder(20, 20, 20, 20));
        JLabel jLabel2 = new JLabel(string2);
        jLabel2.setForeground(this.TEXT_NORMAL);
        jLabel2.setFont(new Font("Segoe UI", 1, 14));
        jLabel2.setAlignmentX(0.5f);
        jPanel3.add(jLabel2);
        jPanel3.add(Box.createVerticalStrut(10));
        JTextField jTextField = this.createStyledTextField("");
        jTextField.setMaximumSize(new Dimension(320, 40));
        jTextField.setPreferredSize(new Dimension(320, 40));
        jTextField.setAlignmentX(0.5f);
        jPanel3.add(jTextField);
        jPanel3.add(Box.createVerticalStrut(20));
        JButton jButton2 = this.createStyledButton("Aceptar", this.BLURPLE);
        jButton2.setPreferredSize(new Dimension(100, 36));
        jButton2.setMaximumSize(new Dimension(100, 36));
        JPanel jPanel4 = new JPanel(new FlowLayout(1, 0, 0));
        jPanel4.setBackground(this.BG_DARK);
        jPanel4.add(jButton2);
        jPanel3.add(jPanel4);
        jPanel.add((Component)jPanel3, "Center");
        jDialog.add(jPanel);
        String[] stringArray = new String[]{null};
        jButton2.addActionListener(actionEvent -> {
            stringArray[0] = jTextField.getText();
            jDialog.dispose();
        });
        jTextField.addActionListener(actionEvent -> jButton2.doClick());
        jDialog.setVisible(true);
        return stringArray[0];
    }

    private int showCustomOptionDialog(String string, String string2, String[] stringArray) {
        final JDialog jDialog = new JDialog(this, string, true);
        jDialog.setUndecorated(true);
        jDialog.setSize(360, 200);
        jDialog.setLocationRelativeTo(this);
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.setBackground(this.BG_DARK);
        jPanel.setBorder(BorderFactory.createLineBorder(this.BG_DARKEST, 2));
        JPanel jPanel2 = new JPanel(new BorderLayout());
        jPanel2.setBackground(this.BG_DARKER);
        jPanel2.setPreferredSize(new Dimension(360, 28));
        JLabel jLabel = new JLabel("  " + string);
        jLabel.setForeground(Color.WHITE);
        jLabel.setFont(new Font("Segoe UI", 1, 12));
        jPanel2.add((Component)jLabel, "West");
        JButton jButton = this.createTitleBtn("X");
        jButton.addActionListener(actionEvent -> jDialog.dispose());
        jPanel2.add((Component)jButton, "East");
        final Point[] pointArray = new Point[1];
        jPanel2.addMouseListener(new MouseAdapter(){

            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                pointArray[0] = mouseEvent.getPoint();
            }
        });
        jPanel2.addMouseMotionListener(new MouseAdapter(){

            @Override
            public void mouseDragged(MouseEvent mouseEvent) {
                Point point = jDialog.getLocation();
                jDialog.setLocation(point.x + mouseEvent.getX() - pointArray[0].x, point.y + mouseEvent.getY() - pointArray[0].y);
            }
        });
        jPanel.add((Component)jPanel2, "North");
        JPanel jPanel3 = new JPanel();
        jPanel3.setLayout(new BoxLayout(jPanel3, 1));
        jPanel3.setBackground(this.BG_DARK);
        jPanel3.setBorder(new EmptyBorder(30, 20, 30, 20));
        JLabel jLabel2 = new JLabel(string2);
        jLabel2.setForeground(this.TEXT_NORMAL);
        jLabel2.setFont(new Font("Segoe UI", 1, 15));
        jLabel2.setAlignmentX(0.5f);
        jPanel3.add(jLabel2);
        jPanel3.add(Box.createVerticalStrut(30));
        JPanel jPanel4 = new JPanel(new FlowLayout(1, 20, 0));
        jPanel4.setBackground(this.BG_DARK);
        int[] nArray = new int[]{-1};
        for (int i = 0; i < stringArray.length; ++i) {
            int n = i;
            JButton jButton2 = this.createStyledButton(stringArray[i], i == 0 ? this.BLURPLE : this.BG_DARKER);
            jButton2.setPreferredSize(new Dimension(130, 36));
            jButton2.addActionListener(actionEvent -> {
                nArray[0] = n;
                jDialog.dispose();
            });
            jPanel4.add(jButton2);
        }
        jPanel3.add(jPanel4);
        jPanel.add((Component)jPanel3, "Center");
        jDialog.add(jPanel);
        jDialog.setVisible(true);
        return nArray[0];
    }

    private void handleVoiceChannelClick(String string, ActionEvent actionEvent) {
        this.net.joinVoiceChannel(this.currentServer, string);
    }

    private void handleTextChannelClick(JButton jButton, String string, ActionEvent actionEvent) {
        this.setActiveChannelBtn(jButton);
        this.currentTextChannel = string;
        this.chatArea.setText("");
        this.net.joinTextChannel(this.currentServer, string);
        this.updateChatHeader();
    }

    class AvatarIcon
    implements Icon {
        private String user;
        private String letter;
        private Color color;
        private Color statusColor;
        
        public AvatarIcon(String string, String string2, Color color, Color statusColor) {
            this.user = string;
            this.letter = string2;
            this.color = color;
            this.statusColor = statusColor;
        }

        @Override
        public int getIconWidth() {
            return 36;
        }

        @Override
        public int getIconHeight() {
            return 36;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int n, int n2) {
            Graphics2D graphics2D = (Graphics2D)graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (ChatWindow.this.customAvatars.containsKey(this.user)) {
                graphics2D.setClip(new Ellipse2D.Float(n, n2, 36.0f, 36.0f));
                graphics2D.drawImage(ChatWindow.this.customAvatars.get(this.user), n, n2, null);
                graphics2D.setClip(null);
            } else {
                graphics2D.setColor(this.color);
                graphics2D.fillOval(n, n2, 36, 36);
                graphics2D.setColor(Color.WHITE);
                graphics2D.setFont(new Font("Segoe UI", 1, 18));
                FontMetrics fontMetrics = graphics2D.getFontMetrics();
                int n3 = n + (36 - fontMetrics.stringWidth(this.letter)) / 2;
                int n4 = n2 + (36 - fontMetrics.getHeight()) / 2 + fontMetrics.getAscent();
                graphics2D.drawString(this.letter, n3, n4);
            }
            if (this.statusColor != null) {
                graphics2D.setColor(ChatWindow.this.BG_DARKER);
                graphics2D.fillOval(n + 24, n2 + 24, 12, 12);
                graphics2D.setColor(this.statusColor);
                graphics2D.fillOval(n + 26, n2 + 26, 8, 8);
            }
            graphics2D.dispose();
        }
    }
    
    private void playSound(String type) {
        if ("DND".equals(this.myStatus) && "message".equals(type)) return;
        Thread.ofVirtual().start(() -> {
            try {
                javax.sound.sampled.AudioFormat af = new javax.sound.sampled.AudioFormat(44100f, 16, 1, true, false);
                javax.sound.sampled.SourceDataLine sdl = javax.sound.sampled.AudioSystem.getSourceDataLine(af);
                sdl.open(af);
                sdl.start();
                byte[] buf = new byte[1];
                int durationMs = "message".equals(type) ? 150 : 200;
                double hz = "message".equals(type) ? 880.0 : ("join".equals(type) ? 1046.5 : 523.25);
                double vol = 0.2;
                for (int i = 0; i < durationMs * 44.1; i++) {
                    double angle = i / (44100f / hz) * 2.0 * Math.PI;
                    buf[0] = (byte)(Math.sin(angle) * 127.0 * vol);
                    sdl.write(buf, 0, 1);
                }
                sdl.drain();
                sdl.stop();
                sdl.close();
            } catch (Exception e) {}
        });
    }
}



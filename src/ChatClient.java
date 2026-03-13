import javax.swing.SwingUtilities;

class ChatClient {
    void main() {
        SwingUtilities.invokeLater(() -> {
            NetworkController net = new NetworkController("localhost", 12345);
            ChatWindow window = new ChatWindow(net);
            window.setVisible(true);
            
            try {
                net.connect();
            } catch (Exception e) {
                window.onSystemMessage("Error: " + e.getMessage());
            }
        });
    }
}
import javax.swing.SwingUtilities;

class ChatClient {
    void main() {
        SwingUtilities.invokeLater(() -> {
            NetworkController net = new NetworkController("localhost", 12345);
            ChatWindow window = new ChatWindow(net);
            window.setVisible(true);
            
            try {
                net.connect(() -> {
                    window.onSystemMessage("Error: No se pudo conectar al servidor.");
                });
            } catch (Exception e) {
                window.onSystemMessage("Error: " + e.getMessage());
            }
        });
    }
}
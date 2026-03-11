import java.io.*;
import java.net.*;
import java.util.*;

class ChatServer {
    private static final int PORT = 12345;
    private static final Set<PrintWriter> clientWriters = Collections.synchronizedSet(new HashSet<>());

    void main() {
        System.out.println("Servidor iniciado en puerto " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (!serverSocket.isClosed()) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket socket;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out = new PrintWriter(socket.getOutputStream(), true);
                clientWriters.add(out);

                String message;
                while ((message = in.readLine()) != null) {
                    broadcast(message);
                }
            } catch (IOException e) {
                System.out.println("Cliente desconectado.");
            } finally {
                if (out != null) clientWriters.remove(out);
                try { socket.close(); } catch (IOException ignored) { }
            }
        }

        private void broadcast(String msg) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(msg);
                }
            }
        }
    }
}
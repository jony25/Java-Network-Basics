import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class ChatServer {
    private static final int TCP_PORT = 12345;
    private static final int UDP_SERVER_PORT = 12346;
    private static final String USER_FILE = "users.txt";
    private static final Map<String, String> userDatabase = new ConcurrentHashMap<>();
    private static final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
    private static DatagramSocket udpSocket;

    void main() {
        loadUsers();
        new Thread(this::runUdpRelay).start();

        try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
            System.out.println("Servidor iniciado. TCP: " + TCP_PORT + ", UDP Relay: " + UDP_SERVER_PORT);
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            System.err.println("Error TCP: " + e.getMessage());
        }
    }

    private void runUdpRelay() {
        try {
            udpSocket = new DatagramSocket(UDP_SERVER_PORT);
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);

                synchronized (clients) {
                    for (ClientHandler client : clients) {
                        // No reenviar al emisor original
                        if (client.udpPort != packet.getPort() || !client.ip.equals(packet.getAddress())) {
                            DatagramPacket forward = new DatagramPacket(
                                    packet.getData(), packet.getLength(), client.ip, client.udpPort);
                            udpSocket.send(forward);
                        }
                    }
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private synchronized void loadUsers() {
        try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(":");
                userDatabase.put(p[0], p[1]);
            }
        } catch (IOException ignored) {}
    }

    private static class ClientHandler extends Thread {
        private final Socket socket;
        private PrintWriter out;
        protected InetAddress ip;
        protected int udpPort = -1;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.ip = socket.getInetAddress();
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out = new PrintWriter(socket.getOutputStream(), true);

                while (true) {
                    String req = in.readLine();
                    if (req == null) return;
                    String[] p = req.split(":");

                    if (p[0].equals("LOGIN") && p[2].equals(userDatabase.get(p[1]))) {
                        this.udpPort = 20000 + new Random().nextInt(10000); // Puerto dinámico
                        out.println("AUTH_OK:" + udpPort + ":" + UDP_SERVER_PORT);
                        clients.add(this);
                        handleChat(in, p[1]);
                        break;
                    } else if (p[0].equals("REG")) {
                        // Lógica de registro simplificada
                        userDatabase.put(p[1], p[2]);
                        try (PrintWriter pw = new PrintWriter(new FileWriter(USER_FILE, true))) {
                            pw.println(p[1] + ":" + p[2]);
                        }
                        out.println("REG_OK");
                    } else { out.println("ERR"); }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally { clients.remove(this); }
        }

        private void handleChat(BufferedReader in, String user) throws IOException {
            String msg;
            while ((msg = in.readLine()) != null) {
                synchronized (clients) {
                    for (ClientHandler c : clients) c.out.println(user + ": " + msg);
                }
            }
        }
    }
}
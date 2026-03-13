import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class ChatServer {
    private static final int TCP_PORT = 12345;
    private static final int UDP_SERVER_PORT = 12346;
    private static final String USER_FILE = "users.txt";
    private static final Map<String, String> userDatabase = new ConcurrentHashMap<>();
    private static final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private static final Map<String, ClientHandler> onlineUsers = new ConcurrentHashMap<>();
    private static DatagramSocket udpSocket;

    void main() {
        loadUsers();
        new Thread(this::runUdpRelay).start();

        try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
            System.out.println("Servidor iniciado. TCP: " + TCP_PORT + ", UDP Relay: " + UDP_SERVER_PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                Thread.ofVirtual().start(new ClientHandler(socket));
            }
        } catch (IOException e) {
            System.err.println("Error TCP: " + e.getMessage());
        }
    }

    private void runUdpRelay() {
        try {
            udpSocket = new DatagramSocket(UDP_SERVER_PORT);
            byte[] buffer = new byte[1024 + 8]; // 8 bytes sequence + PCM payload
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);

                // Identificar al enviador
                ClientHandler sender = null;
                for (ClientHandler client : clients) {
                    if (client.udpPort == packet.getPort() && client.ip.equals(packet.getAddress())) {
                        sender = client;
                        break;
                    }
                }

                if (sender != null && sender.currentServer != null && sender.currentVoiceChannel != null) {
                    // Reenviar solo a los clientes escuchando exactamente el mismo servidor y canal de voz
                    for (ClientHandler client : clients) {
                        if (client != sender 
                            && client.currentServer != null && client.currentServer.equals(sender.currentServer)
                            && client.currentVoiceChannel != null && client.currentVoiceChannel.equals(sender.currentVoiceChannel)) {
                            
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

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        protected InetAddress ip;
        protected int udpPort = -1;
        
        // Scope variables
        protected String currentServer = null;
        protected String currentTextChannel = null;
        protected String currentVoiceChannel = null;
        protected String username = null;

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
                    String[] p = req.split(":", 3);

                    if (p[0].equals("LOGIN") && p.length >= 3 && p[2].equals(userDatabase.get(p[1]))) {
                        this.udpPort = 20000 + new Random().nextInt(10000);
                        this.username = p[1];
                        out.println("AUTH_OK:" + udpPort + ":" + UDP_SERVER_PORT);
                        clients.add(this);
                        onlineUsers.put(this.username, this);
                        
                        // Notify others of login
                        broadcastPresence("ONLINE", this.username);
                        // Send full state to joining user
                        sendInitialState(this);
                        
                        handleChat(in, p[1]);
                        break;
                    } else if (p[0].equals("REG") && p.length >= 3) {
                        userDatabase.put(p[1], p[2]);
                        try (PrintWriter pw = new PrintWriter(new FileWriter(USER_FILE, true))) {
                            pw.println(p[1] + ":" + p[2]);
                        }
                        out.println("REG_OK");
                    } else { out.println("ERR"); }
                }
            } catch (IOException e) {
                System.err.println("Client IO: " + e.getMessage());
            } finally {
                clients.remove(this);
                if (username != null) {
                    onlineUsers.remove(username);
                    broadcastPresence("OFFLINE", username);
                    if (currentVoiceChannel != null) broadcastVoicePresence("LEAVE", currentVoiceChannel, username);
                }
            }
        }

        private void handleChat(BufferedReader in, String user) throws IOException {
            String msgString;
            while ((msgString = in.readLine()) != null) {
                if (msgString.startsWith("JOIN_TEXT:")) {
                    String[] parts = msgString.split(":");
                    if (parts.length >= 3) {
                        this.currentServer = parts[1];
                        this.currentTextChannel = parts[2];
                        replayHistory(this.currentServer, this.currentTextChannel);
                    }
                } else if (msgString.startsWith("JOIN_VOICE:")) {
                    String[] parts = msgString.split(":");
                    if (parts.length >= 3) {
                        if (this.currentVoiceChannel != null) {
                            broadcastVoicePresence("LEAVE", this.currentVoiceChannel, user);
                        }
                        this.currentServer = parts[1];
                        this.currentVoiceChannel = parts[2];
                        broadcastVoicePresence("JOIN", this.currentVoiceChannel, user);
                    }
                } else if (msgString.startsWith("LEAVE_VOICE")) {
                    if (this.currentVoiceChannel != null) {
                        broadcastVoicePresence("LEAVE", this.currentVoiceChannel, user);
                    }
                    this.currentVoiceChannel = null;
                } else if (msgString.startsWith("MSG:")) {
                    String[] parts = msgString.split(":", 4);
                    if (parts.length >= 4) {
                        String srv = parts[1];
                        String chan = parts[2];
                        String txt = parts[3];
                        
                        String fullMessage = user + ": " + txt;
                        appendHistory(srv, chan, fullMessage);
                        
                        for (ClientHandler c : clients) {
                            if (srv.equals(c.currentServer) && chan.equals(c.currentTextChannel)) {
                                c.out.println("MSG:" + srv + ":" + chan + ":" + fullMessage);
                            }
                        }
                    }
                }
            }
        }
        
        private void broadcastPresence(String status, String username) {
            for (ClientHandler c : clients) {
                c.out.println("PRESENCE:" + status + ":" + username);
            }
        }
        
        private void broadcastVoicePresence(String status, String channel, String username) {
            for (ClientHandler c : clients) {
                if (c.currentServer != null && c.currentServer.equals(this.currentServer)) {
                    c.out.println("VOICE_PRESENCE:" + status + ":" + channel + ":" + username);
                }
            }
        }
        
        private void sendInitialState(ClientHandler target) {
            for (String user : onlineUsers.keySet()) {
                target.out.println("PRESENCE:ONLINE:" + user);
                ClientHandler other = onlineUsers.get(user);
                if (other.currentVoiceChannel != null) {
                    target.out.println("VOICE_PRESENCE:JOIN:" + other.currentVoiceChannel + ":" + user);
                }
            }
        }
        
        private void appendHistory(String server, String channel, String formattedMessage) {
            File dir = new File("history");
            if (!dir.exists()) dir.mkdir();
            File f = new File(dir, "history_" + server + "_" + channel + ".txt");
            try (PrintWriter fw = new PrintWriter(new FileWriter(f, true))) {
                fw.println(formattedMessage);
            } catch (IOException ignored) {}
        }
        
        private void replayHistory(String server, String channel) {
            File f = new File("history/history_" + server + "_" + channel + ".txt");
            if (!f.exists()) return;
            
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                List<String> lines = new ArrayList<>();
                String l;
                while ((l = br.readLine()) != null) lines.add(l);
                
                int start = Math.max(0, lines.size() - 50); // Últimos 50 msgs
                for (int i = start; i < lines.size(); i++) {
                    this.out.println("MSG:" + server + ":" + channel + ":" + lines.get(i));
                }
            } catch (IOException ignored) {}
        }
    }
}
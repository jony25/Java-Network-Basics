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
    private static final Map<String, ServerProfile> serverDb = new ConcurrentHashMap<>();
    private static final Map<String, String> avatarDb = new ConcurrentHashMap<>();
    private static final Map<String, String> bioDb = new ConcurrentHashMap<>();
    private static final Map<String, String> inviteDb = new ConcurrentHashMap<>(); // code -> serverName
    private static DatagramSocket udpSocket;

    static class ServerProfile {
        String name;
        String owner;
        Set<String> members = ConcurrentHashMap.newKeySet();
        List<String> textChannels = new ArrayList<>();
        List<String> voiceChannels = new ArrayList<>();
        Map<String, Integer> voiceLimits = new ConcurrentHashMap<>();
        
        public ServerProfile(String name, String owner) {
            this.name = name;
            this.owner = owner;
        }
    }

    void main() {
        loadUsers();
        loadServers();
        loadAvatars();
        loadBios();
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
    
    private synchronized void loadServers() {
        File dir = new File("servers");
        if (!dir.exists()) dir.mkdir();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files != null) {
            for (File f : files) {
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    String name = br.readLine();
                    String owner = br.readLine();
                    ServerProfile sp = new ServerProfile(name, owner);
                    
                    String membersLine = br.readLine();
                    if (membersLine != null && !membersLine.isEmpty()) sp.members.addAll(Arrays.asList(membersLine.split(",")));
                    
                    String textLine = br.readLine();
                    if (textLine != null && !textLine.isEmpty()) sp.textChannels.addAll(Arrays.asList(textLine.split(",")));
                    
                    String voiceLine = br.readLine();
                    if (voiceLine != null && !voiceLine.isEmpty()) sp.voiceChannels.addAll(Arrays.asList(voiceLine.split(",")));
                    
                    String limitsLine = br.readLine();
                    if (limitsLine != null && !limitsLine.isEmpty()) {
                        for (String limitEntry : limitsLine.split(",")) {
                            String[] parts = limitEntry.split("=");
                            if (parts.length == 2) {
                                try {
                                    sp.voiceLimits.put(parts[0], Integer.parseInt(parts[1]));
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }
                    
                    serverDb.put(name, sp);
                } catch (Exception e) {}
            }
        }
        // No more hardcoded defaults — users create their own servers
    }
    
    private static synchronized void saveServer(ServerProfile sp) {
        File dir = new File("servers");
        if (!dir.exists()) dir.mkdir();
        File f = new File(dir, sp.name + ".txt");
        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            pw.println(sp.name);
            pw.println(sp.owner);
            pw.println(String.join(",", sp.members));
            pw.println(String.join(",", sp.textChannels));
            pw.println(String.join(",", sp.voiceChannels));
            
            List<String> limits = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : sp.voiceLimits.entrySet()) {
                limits.add(entry.getKey() + "=" + entry.getValue());
            }
            pw.println(String.join(",", limits));
        } catch (Exception e) {}
    }

    private synchronized void loadAvatars() {
        File dir = new File("avatars");
        if (!dir.exists()) dir.mkdir();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files != null) {
            for (File f : files) {
                try {
                    String b64 = new String(java.nio.file.Files.readAllBytes(f.toPath()));
                    avatarDb.put(f.getName().replace(".txt", ""), b64);
                } catch (Exception e) {}
            }
        }
    }

    private synchronized void loadBios() {
        File dir = new File("bios");
        if (!dir.exists()) dir.mkdir();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files != null) {
            for (File f : files) {
                try {
                    String bio = new String(java.nio.file.Files.readAllBytes(f.toPath()));
                    bioDb.put(f.getName().replace(".txt", ""), bio);
                } catch (Exception e) {}
            }
        }
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
        protected String status = "ONLINE";

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
                        broadcastPresence(this.status, this.username);
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
                        String srvName = parts[1];
                        String vcName = parts[2];
                        ServerProfile sp = serverDb.get(srvName);
                        if (sp != null) {
                            int limit = sp.voiceLimits.getOrDefault(vcName, 0);
                            if (limit > 0) {
                                int currentCount = 0;
                                for (ClientHandler c : clients) {
                                    if (srvName.equals(c.currentServer) && vcName.equals(c.currentVoiceChannel)) currentCount++;
                                }
                                if (currentCount >= limit) {
                                    this.out.println("SYS:Error: El canal de voz '" + vcName + "' está lleno (" + limit + "/" + limit + ").");
                                    continue;
                                }
                            }
                        }
                        
                        if (this.currentVoiceChannel != null) {
                            broadcastVoicePresence("LEAVE", this.currentVoiceChannel, user);
                        }
                        this.currentServer = srvName;
                        this.currentVoiceChannel = vcName;
                        broadcastVoicePresence("JOIN", this.currentVoiceChannel, user);
                    }
                } else if (msgString.startsWith("LEAVE_VOICE")) {
                    if (this.currentVoiceChannel != null) {
                        broadcastVoicePresence("LEAVE", this.currentVoiceChannel, user);
                    }
                } else if (msgString.startsWith("CREATE_SERVER:")) {
                    String[] parts = msgString.split(":", 2);
                    if (parts.length >= 2) {
                        String sName = parts[1].trim();
                        if (!serverDb.containsKey(sName)) {
                            ServerProfile sp = new ServerProfile(sName, user);
                            sp.members.add(user);
                            sp.textChannels.add("general");
                            sp.voiceChannels.add("voz-general");
                            serverDb.put(sName, sp);
                            saveServer(sp);
                            sendServerList(this, user);
                        }
                    }
                } else if (msgString.startsWith("JOIN_SERVER:")) {
                    String[] parts = msgString.split(":", 2);
                    if (parts.length >= 2) {
                        String sName = parts[1].trim();
                        ServerProfile sp = serverDb.get(sName);
                        if (sp != null && !sp.members.contains(user)) {
                            sp.members.add(user);
                            saveServer(sp);
                            sendServerList(this, user); // Update list for user
                        }
                    }
                } else if (msgString.startsWith("LEAVE_SERVER:")) {
                    String[] parts = msgString.split(":", 2);
                    if (parts.length >= 2) {
                        String sName = parts[1].trim();
                        ServerProfile sp = serverDb.get(sName);
                        if (sp != null && sp.members.contains(user) && !sp.owner.equals(user)) {
                            sp.members.remove(user);
                            saveServer(sp);
                            sendServerList(this, user);
                            
                            // Propagate list update so user instantly vanishes from right sidebar
                            for (ClientHandler c : clients) {
                                if (sp.members.contains(c.username)) {
                                    sendServerInfo(c, sp);
                                }
                            }
                        }
                    }
                } else if (msgString.startsWith("DELETE_SERVER:")) {
                    String[] parts = msgString.split(":", 2);
                    if (parts.length >= 2) {
                        String sName = parts[1].trim();
                        ServerProfile sp = serverDb.get(sName);
                        if (sp != null && sp.owner.equals(user)) {
                            // Notify all members first before deleting
                            for (ClientHandler c : clients) {
                                if (sp.members.contains(c.username)) {
                                    sp.members.remove(c.username);
                                    sendServerList(c, c.username);
                                }
                            }
                            
                            serverDb.remove(sName);
                            File oldFile = new File("servers", sName + ".txt");
                            if (oldFile.exists()) oldFile.delete();
                            
                            File histDir = new File("history");
                            if (histDir.exists()) {
                                File[] histFiles = histDir.listFiles((d, n) -> n.startsWith("sv_" + sName + "_ch_"));
                                if (histFiles != null) {
                                    for (File h : histFiles) h.delete();
                                }
                            }
                        }
                    }
                } else if (msgString.startsWith("RENAME_SERVER:")) {
                    String[] parts = msgString.split(":", 3);
                    if (parts.length >= 3) {
                        String oldName = parts[1].trim();
                        String newName = parts[2].trim();
                        ServerProfile sp = serverDb.get(oldName);
                        if (sp != null && sp.owner.equals(user) && !serverDb.containsKey(newName)) {
                            // Rename logic
                            serverDb.remove(oldName);
                            sp.name = newName;
                            serverDb.put(newName, sp);
                            
                            // Rename file on disk
                            File oldFile = new File("servers", oldName + ".txt");
                            if (oldFile.exists()) oldFile.delete();
                            saveServer(sp);
                            
                            // Rename history files
                            File histDir = new File("history");
                            if (histDir.exists()) {
                                File[] histFiles = histDir.listFiles((d, n) -> n.startsWith("sv_" + oldName + "_ch_"));
                                if (histFiles != null) {
                                    for (File h : histFiles) {
                                        String newHistName = h.getName().replaceFirst("sv_" + oldName + "_ch_", "sv_" + newName + "_ch_");
                                        h.renameTo(new File(histDir, newHistName));
                                    }
                                }
                            }
                            
                            // Notify members
                            for (ClientHandler c : clients) {
                                if (sp.members.contains(c.username)) {
                                    sendServerList(c, c.username);
                                }
                            }
                        }
                    }
                } else if (msgString.startsWith("ADD_CHANNEL:")) {
                    String[] parts = msgString.split(":"); // ADD_CHANNEL:server:type:name
                    if (parts.length >= 4) {
                        String sName = parts[1];
                        String type = parts[2];
                        String cName = parts[3];
                        ServerProfile sp = serverDb.get(sName);
                        if (sp != null && sp.owner.equals(user)) { // ONLY OWNER CAN ADD
                            if (type.equals("TEXT")) sp.textChannels.add(cName);
                            else if (type.equals("VOICE")) sp.voiceChannels.add(cName);
                            saveServer(sp);
                            // Notify everybody in this server that structure changed
                            for (ClientHandler c : clients) {
                                if (sp.members.contains(c.username)) {
                                    sendServerInfo(c, sp);
                                }
                            }
                        }
                    }
                } else if (msgString.startsWith("EDIT_CHANNEL:")) {
                    String[] parts = msgString.split(":", 6); // EDIT_CHANNEL:server:type:oldName:newName:limit
                    if (parts.length >= 5) {
                        String sName = parts[1];
                        String type = parts[2];
                        String oldName = parts[3];
                        String newName = parts[4];
                        String limitStr = parts.length >= 6 ? parts[5] : "0";
                        ServerProfile sp = serverDb.get(sName);
                        if (sp != null && sp.owner.equals(user)) {
                            if (type.equals("TEXT")) {
                                int idx = sp.textChannels.indexOf(oldName);
                                if (idx != -1) {
                                    sp.textChannels.set(idx, newName);
                                    File histDir = new File("history");
                                    if (histDir.exists()) {
                                        File oldHist = new File(histDir, "history_" + sName + "_" + oldName + ".txt");
                                        if (oldHist.exists()) oldHist.renameTo(new File(histDir, "history_" + sName + "_" + newName + ".txt"));
                                    }
                                }
                            } else if (type.equals("VOICE")) {
                                int idx = sp.voiceChannels.indexOf(oldName);
                                if (idx != -1) {
                                    sp.voiceChannels.set(idx, newName);
                                    try {
                                        int lim = Integer.parseInt(limitStr);
                                        if (lim > 0) sp.voiceLimits.put(newName, lim);
                                        else sp.voiceLimits.remove(newName);
                                        if (!oldName.equals(newName)) sp.voiceLimits.remove(oldName); // cleanup old limit if name changed
                                    } catch (NumberFormatException e) {}
                                }
                            }
                            saveServer(sp);
                            for (ClientHandler c : clients) {
                                if (sp.members.contains(c.username)) sendServerInfo(c, sp);
                                if (type.equals("TEXT") && sName.equals(c.currentServer) && oldName.equals(c.currentTextChannel)) c.currentTextChannel = newName;
                                if (type.equals("VOICE") && sName.equals(c.currentServer) && oldName.equals(c.currentVoiceChannel)) c.currentVoiceChannel = newName;
                            }
                        }
                    }
                } else if (msgString.startsWith("SET_STATUS:")) {
                    String[] parts = msgString.split(":", 2);
                    if (parts.length >= 2) {
                        this.status = parts[1];
                        broadcastPresence(this.status, this.username);
                    }
                } else if (msgString.startsWith("GET_SERVER_INFO:")) {
                    String[] parts = msgString.split(":", 2);
                    if (parts.length >= 2) {
                        ServerProfile sp = serverDb.get(parts[1]);
                        if (sp != null) sendServerInfo(this, sp);
                    }
                } else if (msgString.startsWith("SET_BIO:")) {
                    String[] parts = msgString.split(":", 2);
                    if (parts.length >= 2) {
                        String bio = parts[1];
                        bioDb.put(user, bio);
                        File dir = new File("bios");
                        if (!dir.exists()) dir.mkdir();
                        File f = new File(dir, user + ".txt");
                        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                            pw.print(bio);
                        } catch (Exception e) {}
                        
                        for (ClientHandler c : clients) {
                            c.out.println("BIO:" + user + ":" + bio);
                        }
                    }
                } else if (msgString.startsWith("AVATAR_UPLOAD:")) {
                    String[] parts = msgString.split(":", 2);
                    if (parts.length >= 2) {
                        String b64 = parts[1];
                        avatarDb.put(user, b64);
                        File dir = new File("avatars");
                        if (!dir.exists()) dir.mkdir();
                        File f = new File(dir, user + ".txt");
                        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                            pw.print(b64);
                        } catch (Exception e) {}
                        
                        for (ClientHandler c : clients) {
                            c.out.println("AVATAR:" + user + ":" + b64);
                        }
                    }
                } else if (msgString.startsWith("GENERATE_INVITE:")) {
                    String[] parts = msgString.split(":", 2);
                    if (parts.length >= 2) {
                        String sName = parts[1].trim();
                        ServerProfile sp = serverDb.get(sName);
                        if (sp != null && sp.members.contains(user)) {
                            String code = java.util.UUID.randomUUID().toString().substring(0, 8);
                            inviteDb.put(code, sName);
                            this.out.println("INVITE_CODE:" + code);
                        }
                    }
                } else if (msgString.startsWith("USE_INVITE:")) {
                    String[] parts = msgString.split(":", 2);
                    if (parts.length >= 2) {
                        String code = parts[1].trim();
                        String sName = inviteDb.get(code);
                        if (sName != null) {
                            ServerProfile sp = serverDb.get(sName);
                            if (sp != null && !sp.members.contains(user)) {
                                sp.members.add(user);
                                saveServer(sp);
                            }
                            sendServerList(this, user);
                        } else {
                            this.out.println("SYS:Código de invitación inválido.");
                        }
                    }
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
        
        private void sendServerList(ClientHandler target, String username) {
            List<String> userServers = new ArrayList<>();
            for (ServerProfile sp : serverDb.values()) {
                if (sp.members.contains(username)) {
                    userServers.add(sp.name);
                }
            }
            target.out.println("SERVER_LIST:" + String.join(",", userServers));
        }
        
        private void sendServerInfo(ClientHandler target, ServerProfile sp) {
            String txts = String.join(",", sp.textChannels);
            String vcs = String.join(",", sp.voiceChannels);
            List<String> limits = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : sp.voiceLimits.entrySet()) {
                limits.add(entry.getKey() + "=" + entry.getValue());
            }
            String limitsStr = String.join(",", limits);
            target.out.println("SERVER_INFO:" + sp.name + ":" + sp.owner + ":" + txts + ":" + vcs + ":" + limitsStr + ":" + String.join(",", sp.members));
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
            sendServerList(target, target.username);
            
            for (Map.Entry<String, String> entry : avatarDb.entrySet()) {
                target.out.println("AVATAR:" + entry.getKey() + ":" + entry.getValue());
            }
            
            for (Map.Entry<String, String> entry : bioDb.entrySet()) {
                target.out.println("BIO:" + entry.getKey() + ":" + entry.getValue());
            }
            
            for (String user : onlineUsers.keySet()) {
                ClientHandler other = onlineUsers.get(user);
                target.out.println("PRESENCE:" + other.status + ":" + user);
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
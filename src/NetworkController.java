import java.io.*;
import java.net.*;

public class NetworkController {
    private final String serverIp;
    private final int tcpPort;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private NetworkListener listener;

    private int myUdpPort;
    private int serverUdpPort;
    private Thread vSender, vReceiver;
    private boolean voiceOn = false;

    public NetworkController(String serverIp, int tcpPort) {
        this.serverIp = serverIp;
        this.tcpPort = tcpPort;
    }

    public void setListener(NetworkListener listener) {
        this.listener = listener;
    }

    public void connect(Runnable onFail) {
        try {
            socket = new Socket(serverIp, tcpPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Thread.ofVirtual().start(this::listenForMessages);
        } catch (IOException e) {
            if (onFail != null) onFail.run();
        }
    }

    public void login(String user, String pass) {
        out.println("LOGIN:" + user + ":" + pass);
    }

    public void register(String user, String pass) {
        out.println("REG:" + user + ":" + pass);
    }

    public void joinTextChannel(String server, String channel) {
        out.println("JOIN_TEXT:" + server + ":" + channel);
    }
    
    public void createServer(String name) {
        if (out != null) out.println("CREATE_SERVER:" + name);
    }
    
    public void requestJoinServer(String name) {
        if (out != null) out.println("JOIN_SERVER:" + name);
    }
    
    public void addChannel(String server, String type, String name) {
        if (out != null) out.println("ADD_CHANNEL:" + server + ":" + type + ":" + name);
    }
    
    public void editChannel(String server, String type, String oldName, String newName, int limit) {
        if (out != null) out.println("EDIT_CHANNEL:" + server + ":" + type + ":" + oldName + ":" + newName + ":" + limit);
    }
    
    public void setStatus(String status) {
        if (out != null) out.println("SET_STATUS:" + status);
    }
    
    public void setBio(String bio) {
        if (out != null) out.println("SET_BIO:" + bio);
    }
    
    public void getBio(String username) {
        if (out != null) out.println("GET_BIO:" + username);
    }
    
    public void getServerInfo(String name) {
        if (out != null) out.println("GET_SERVER_INFO:" + name);
    }

    public void joinVoiceChannel(String server, String channel) {
        out.println("JOIN_VOICE:" + server + ":" + channel);
        if (!voiceOn) {
            vSender = Thread.ofVirtual().start(new VoiceSender(serverIp, serverUdpPort));
            vReceiver = Thread.ofVirtual().start(new VoiceReceiver(myUdpPort));
            voiceOn = true;
            if (listener != null) listener.onSystemMessage("[VOZ ACTIVA]");
        }
    }

    public void leaveVoiceChannel() {
        out.println("LEAVE_VOICE");
        if (voiceOn) {
            if(vSender != null) vSender.interrupt();
            if (vReceiver != null) vReceiver.interrupt();
            voiceOn = false;
            if (listener != null) listener.onSystemMessage("[VOZ OFF]");
        }
    }

    public void sendMessage(String server, String channel, String msg) {
        out.println("MSG:" + server + ":" + channel + ":" + msg);
    }

    private void listenForMessages() {
        try {
            String res;
            while ((res = in.readLine()) != null) {
                if (res.startsWith("AUTH_OK")) {
                    String[] parts = res.split(":");
                    myUdpPort = Integer.parseInt(parts[1]);
                    serverUdpPort = Integer.parseInt(parts[2]);
                    if (listener != null) listener.onAuthResult(true, "Conectado. Puerto UDP: " + myUdpPort);
                } else if (res.startsWith("REG_OK")) {
                    if (listener != null) listener.onAuthResult(true, "Registrado correctamente.");
                } else if (res.equals("ERR")) {
                    if (listener != null) listener.onAuthResult(false, "Error de autenticación/registro.");
                } else if (res.startsWith("PRESENCE:")) {
                    String[] parts = res.split(":", 3);
                    if (parts.length >= 3 && listener != null) {
                        listener.onUserPresence(parts[2], parts[1]);
                    }
                } else if (res.startsWith("VOICE_PRESENCE:")) {
                    String[] parts = res.split(":");
                    if (parts.length >= 4 && listener != null) {
                        listener.onVoicePresence(parts[2], parts[3], parts[1].equals("JOIN"));
                    }
                } else if (res.startsWith("SERVER_LIST:")) {
                    if (listener != null) {
                        String[] parts = res.split(":", 2);
                        if (parts.length >= 2 && !parts[1].isEmpty()) {
                            listener.onServerList(java.util.Arrays.asList(parts[1].split(",")));
                        } else {
                            listener.onServerList(new java.util.ArrayList<>());
                        }
                    }
                } else if (res.startsWith("SERVER_INFO:")) {
                    if (listener != null) {
                        String[] parts = res.split(":", 7);
                        if (parts.length >= 7) {
                            String name = parts[1];
                            String owner = parts[2];
                            java.util.List<String> textChannels = parts[3].isEmpty() ? new java.util.ArrayList<>() : java.util.Arrays.asList(parts[3].split(","));
                            java.util.List<String> voiceChannels = parts[4].isEmpty() ? new java.util.ArrayList<>() : java.util.Arrays.asList(parts[4].split(","));
                            java.util.Map<String, Integer> voiceLimits = new java.util.HashMap<>();
                            if (!parts[5].isEmpty()) {
                                for (String lim : parts[5].split(",")) {
                                    String[] kv = lim.split("=");
                                    if (kv.length == 2) {
                                        try {
                                            voiceLimits.put(kv[0], Integer.parseInt(kv[1]));
                                        } catch (NumberFormatException ignored) {}
                                    }
                                }
                            }
                            java.util.List<String> members = parts[6].isEmpty() ? new java.util.ArrayList<>() : java.util.Arrays.asList(parts[6].split(","));
                            listener.onServerInfo(name, owner, textChannels, voiceChannels, voiceLimits, members);
                        }
                    }
                } else if (res.startsWith("AVATAR:")) {
                    if (listener != null) {
                        String[] parts = res.split(":", 3);
                        if (parts.length >= 3) {
                            listener.onAvatarReceived(parts[1], parts[2]);
                        }
                    }
                } else if (res.startsWith("BIO:")) {
                    if (listener != null) {
                        String[] parts = res.split(":", 3);
                        if (parts.length >= 3) {
                            listener.onBioReceived(parts[1], parts[2]);
                        }
                    }
                } else if (res.startsWith("INVITE_CODE:")) {
                    if (listener != null) {
                        String code = res.split(":", 2)[1];
                        listener.onSystemMessage("INVITE_CODE:" + code);
                    }
                } else if (res.startsWith("MSG:")) {
                    String[] parts = res.split(":", 5);
                    if (parts.length >= 5) {
                        String sender = parts[3];
                        String text = parts[4];
                        if (listener != null) listener.onMessageReceived(sender, text.trim());
                    }
                } else {
                    if (listener != null) listener.onSystemMessage(res);
                }
            }
        } catch (IOException e) {
            if (listener != null) listener.onSystemMessage("Conexión perdida: " + e.getMessage());
        }
    }

    public void uploadAvatar(String base64) {
        if (out != null) out.println("AVATAR_UPLOAD:" + base64);
    }

    public void leaveServer(String name) {
        if (out != null) out.println("LEAVE_SERVER:" + name);
    }

    public void generateInvite(String serverName) {
        if (out != null) out.println("GENERATE_INVITE:" + serverName);
    }

    public void useInvite(String code) {
        if (out != null) out.println("USE_INVITE:" + code);
    }

    public void renameServer(String oldName, String newName) {
        if (out != null) out.println("RENAME_SERVER:" + oldName + ":" + newName);
    }
    
    public void deleteServer(String name) {
        if (out != null) out.println("DELETE_SERVER:" + name);
    }
}

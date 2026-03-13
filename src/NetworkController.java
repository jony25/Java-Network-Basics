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
                        listener.onUserPresence(parts[2], parts[1].equals("ONLINE"));
                    }
                } else if (res.startsWith("VOICE_PRESENCE:")) {
                    String[] parts = res.split(":", 4);
                    if (parts.length >= 4 && listener != null) {
                        listener.onVoicePresence(parts[2], parts[3], parts[1].equals("JOIN"));
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
}

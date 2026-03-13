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

    public void connect() throws IOException {
        socket = new Socket(serverIp, tcpPort);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        Thread.ofVirtual().start(this::listenForMessages);
    }

    public void login(String user, String pass) {
        out.println("LOGIN:" + user + ":" + pass);
    }

    public void register(String user, String pass) {
        out.println("REG:" + user + ":" + pass);
    }

    public void sendMessage(String msg) {
        if (msg.equalsIgnoreCase("/voice")) {
            toggleVoice();
        } else {
            out.println(msg);
        }
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
                } else if (res.contains(": ")) {
                    String[] parts = res.split(": ", 2);
                    if (listener != null) listener.onMessageReceived(parts[0], parts[1]);
                } else {
                    if (listener != null) listener.onSystemMessage(res);
                }
            }
        } catch (IOException e) {
            if (listener != null) listener.onSystemMessage("Conexión perdida: " + e.getMessage());
        }
    }

    private void toggleVoice() {
        if (!voiceOn) {
            vSender = Thread.ofVirtual().start(new VoiceSender(serverIp, serverUdpPort));
            vReceiver = Thread.ofVirtual().start(new VoiceReceiver(myUdpPort));
            voiceOn = true;
            if (listener != null) listener.onSystemMessage("[VOZ ACTIVA]");
        } else {
            vSender.interrupt();
            if (vReceiver != null) vReceiver.interrupt();
            voiceOn = false;
            if (listener != null) listener.onSystemMessage("[VOZ OFF]");
        }
    }
}

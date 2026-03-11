import java.io.*;
import java.net.*;
import java.util.Scanner;

class ChatClient {
    private String serverIp = "localhost";
    private int myUdpPort;
    private int serverUdpPort;
    private Thread vSender, vReceiver;
    private boolean voiceOn = false;

    void main() {
        Scanner sc = new Scanner(System.in);
        try (Socket socket = new Socket(serverIp, 12345)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            while (true) {
                System.out.println("1. Login / 2. Registro");
                String op = sc.nextLine();
                System.out.print("User: "); String u = sc.nextLine();
                System.out.print("Pass: "); String p = sc.nextLine();

                out.println((op.equals("2") ? "REG:" : "LOGIN:") + u + ":" + p);
                String res = in.readLine();

                if (res.startsWith("AUTH_OK")) {
                    String[] parts = res.split(":");
                    myUdpPort = Integer.parseInt(parts[1]);
                    serverUdpPort = Integer.parseInt(parts[2]);
                    System.out.println("Conectado. Puerto asignado: " + myUdpPort);
                    break;
                }
                System.out.println("Respuesta: " + res);
            }

            new Thread(new IncomingReader(in)).start();

            System.out.println("Escribe '/voice' para hablar.");
            while (sc.hasNextLine()) {
                String txt = sc.nextLine();
                if (txt.equalsIgnoreCase("/voice")) toggleVoice();
                else out.println(txt);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void toggleVoice() {
        if (!voiceOn) {
            vSender = new Thread(new VoiceSender(serverIp, serverUdpPort));
            vReceiver = new Thread(new VoiceReceiver(myUdpPort));
            vSender.start();
            vReceiver.start();
            voiceOn = true;
            System.out.println("[VOZ ACTIVA]");
        } else {
            vSender.interrupt(); vReceiver.interrupt();
            voiceOn = false;
            System.out.println("[VOZ OFF]");
        }
    }

    private static class IncomingReader implements Runnable {
        private final BufferedReader in;
        public IncomingReader(BufferedReader in) { this.in = in; }
        public void run() {
            try {
                String m;
                while ((m = in.readLine()) != null) System.out.println("\n" + m);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
import java.io.*;
import java.net.*;
import java.util.Scanner;

class ChatClient {
    private static final String SERVER_IP = "localhost";
    private static final int TCP_PORT = 12345;
    private static final int VOICE_PORT = 12346;

    private Thread voiceSenderThread;
    private Thread voiceReceiverThread;
    private boolean voiceActive = false;

    void main() {
        try (Socket socket = new Socket(SERVER_IP, TCP_PORT)) {
            new Thread(new IncomingReader(socket)).start();
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);

            System.out.println("Conectado. Escribe '/voice' para activar/desactivar audio.");

            while (scanner.hasNextLine()) {
                String input = scanner.nextLine();

                if (input.equalsIgnoreCase("/voice")) {
                    toggleVoice();
                } else {
                    out.println(input);
                }
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getLocalizedMessage());
        }
    }

    private void toggleVoice() {
        if (!voiceActive) {
            voiceSenderThread = new Thread(new VoiceSender(SERVER_IP, VOICE_PORT));
            voiceReceiverThread = new Thread(new VoiceReceiver(VOICE_PORT));
            voiceSenderThread.start();
            voiceReceiverThread.start();
            voiceActive = true;
            System.out.println("[Sistema] Voz activada.");
        } else {
            voiceSenderThread.interrupt();
            voiceReceiverThread.interrupt();
            voiceActive = false;
            System.out.println("[Sistema] Voz desactivada.");
        }
    }

    private static class IncomingReader implements Runnable {
        private final BufferedReader in;

        public IncomingReader(Socket socket) throws IOException {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("\n[Chat]: " + message);
                }
            } catch (IOException e) {
                System.out.println("Desconectado.");
            }
        }
    }
}
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 12345)) {
            new Thread(new IncomingReader(socket)).start();
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);

            while (scanner.hasNextLine()) {
                out.println(scanner.nextLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class IncomingReader implements Runnable {
        private BufferedReader in;

        public IncomingReader(Socket socket) throws IOException {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("\n[Mensaje]: " + message);
                }
            } catch (IOException e) {
                System.out.println("Conexión cerrada.");
            }
        }
    }
}
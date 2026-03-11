import javax.sound.sampled.*;
import java.net.*;

class VoiceSender implements Runnable {
    private final String destIp;
    private final int port;
    private final AudioFormat format = new AudioFormat(8000, 16, 1, true, false);

    public VoiceSender(String destIp, int port) {
        this.destIp = destIp;
        this.port = port;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket();
             TargetDataLine line = AudioSystem.getTargetDataLine(format)) {

            line.open(format);
            line.start();
            byte[] buffer = new byte[1024];
            InetAddress address = InetAddress.getByName(destIp);

            while (!Thread.currentThread().isInterrupted()) {
                int count = line.read(buffer, 0, buffer.length);
                if (count > 0) {
                    DatagramPacket packet = new DatagramPacket(buffer, count, address, port);
                    socket.send(packet);
                }
            }
        } catch (Exception e) {
            System.err.println("Error en el emisor de voz: " + e.getLocalizedMessage());
        }
    }
}
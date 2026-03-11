import javax.sound.sampled.*;
import java.net.*;

class VoiceReceiver implements Runnable {
    private final int port;
    private final AudioFormat format = new AudioFormat(8000, 16, 1, true, false);

    public VoiceReceiver(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port);
             SourceDataLine line = AudioSystem.getSourceDataLine(format)) {

            line.open(format);
            line.start();
            byte[] buffer = new byte[1024];

            while (!Thread.currentThread().isInterrupted()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                line.write(packet.getData(), 0, packet.getLength());
            }
        } catch (Exception e) {
            System.err.println("Error en el receptor de voz: " + e.getLocalizedMessage());
        }
    }
}
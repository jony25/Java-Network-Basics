import javax.sound.sampled.*;
import java.net.*;
import java.nio.ByteBuffer;

class VoiceSender implements Runnable {
    private final String destIp;
    private final int port;
    private final AudioFormat pcmFormat = new AudioFormat(44100, 16, 1, true, false);

    public VoiceSender(String destIp, int port) {
        this.destIp = destIp;
        this.port = port;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket();
             TargetDataLine line = AudioSystem.getTargetDataLine(pcmFormat)) {

            line.open(pcmFormat);
            line.start();
            
            byte[] audioBuffer = new byte[1024]; // PCM chunk
            ByteBuffer packetBuffer = ByteBuffer.allocate(8 + audioBuffer.length);
            InetAddress address = InetAddress.getByName(destIp);
            long seq = 0;

            while (!Thread.currentThread().isInterrupted()) {
                int count = line.read(audioBuffer, 0, audioBuffer.length);
                if (count > 0) {
                    packetBuffer.clear();
                    packetBuffer.putLong(seq++);
                    packetBuffer.put(audioBuffer, 0, count);
                    
                    DatagramPacket packet = new DatagramPacket(
                        packetBuffer.array(), 8 + count, address, port);
                    socket.send(packet);
                }
            }
        } catch (Exception e) {
            System.err.println("Error Sender: " + e.getMessage());
        }
    }
}
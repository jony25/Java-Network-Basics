import javax.sound.sampled.*;
import java.net.*;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;

class VoiceSender implements Runnable {
    private final String destIp;
    private final int port;
    private final AudioFormat pcmFormat = new AudioFormat(44100, 16, 1, true, false);
    private final AudioFormat ulawFormat = new AudioFormat(AudioFormat.Encoding.ULAW, 8000, 8, 1, 1, 8000, false);

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
            byte[] ulawBuffer = new byte[512];   // ULAW chunk
            ByteBuffer packetBuffer = ByteBuffer.allocate(8 + ulawBuffer.length);
            InetAddress address = InetAddress.getByName(destIp);
            long seq = 0;

            while (!Thread.currentThread().isInterrupted()) {
                int count = line.read(audioBuffer, 0, audioBuffer.length);
                if (count > 0) {
                    // Convert PCM to ULAW
                    ByteArrayInputStream bais = new ByteArrayInputStream(audioBuffer, 0, count);
                    AudioInputStream pcmStream = new AudioInputStream(bais, pcmFormat, count / 2); // 2 bytes per frame
                    AudioInputStream ulawStream = AudioSystem.getAudioInputStream(ulawFormat, pcmStream);
                    
                    int ulawCount = ulawStream.read(ulawBuffer);
                    if (ulawCount > 0) {
                        packetBuffer.clear();
                        packetBuffer.putLong(seq++);
                        packetBuffer.put(ulawBuffer, 0, ulawCount);
                        
                        DatagramPacket packet = new DatagramPacket(
                            packetBuffer.array(), 8 + ulawCount, address, port);
                        socket.send(packet);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error Sender: " + e.getMessage());
        }
    }
}
import javax.sound.sampled.*;
import java.net.*;
import java.nio.ByteBuffer;

class VoiceReceiver implements Runnable {
    private final int port;
    private final AudioFormat format = new AudioFormat(8000, 16, 1, true, false);
    private final JitterBuffer jitterBuffer = new JitterBuffer(1024);

    public VoiceReceiver(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            Thread.ofVirtual().start(this::playoutLoop);
            
            byte[] buffer = new byte[1024 + 8];
            while (!Thread.currentThread().isInterrupted()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                ByteBuffer bb = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
                long seq = bb.getLong();
                byte[] audio = new byte[packet.getLength() - 8];
                bb.get(audio);
                
                jitterBuffer.addPacket(new VoicePacket(seq, audio));
            }
        } catch (Exception e) {
            System.err.println("Error Receiver: " + e.getMessage());
        }
    }

    private void playoutLoop() {
        try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
            line.open(format);
            line.start();
            
            Thread.sleep(60); // Retardo inicial (pre-buffering)
            
            while (!Thread.currentThread().isInterrupted()) {
                byte[] audio = jitterBuffer.readPacket();
                line.write(audio, 0, audio.length);
            }
        } catch (Exception e) {
            System.err.println("Error Playout: " + e.getMessage());
        }
    }
}
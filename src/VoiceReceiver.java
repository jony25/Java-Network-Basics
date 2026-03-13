import javax.sound.sampled.*;
import java.net.*;
import java.nio.ByteBuffer;

class VoiceReceiver implements Runnable {
    private final int port;
    private final AudioFormat pcmFormat = new AudioFormat(44100, 16, 1, true, false);
    private final JitterBuffer jitterBuffer = new JitterBuffer(2048); // Size in PCM bytes

    public VoiceReceiver(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            Thread.ofVirtual().start(this::playoutLoop);
            
            byte[] buffer = new byte[2048 + 8]; // PCM chunk + 8 seq
            while (!Thread.currentThread().isInterrupted()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                ByteBuffer bb = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
                long seq = bb.getLong();
                
                int pcmLen = packet.getLength() - 8;
                byte[] pcmAudio = new byte[pcmLen];
                bb.get(pcmAudio);
                
                if (pcmLen > 0) {
                    jitterBuffer.addPacket(new VoicePacket(seq, pcmAudio));
                }
            }
        } catch (Exception e) {
            System.err.println("Error Receiver: " + e.getMessage());
        }
    }

    private void playoutLoop() {
        try (SourceDataLine line = AudioSystem.getSourceDataLine(pcmFormat)) {
            line.open(pcmFormat);
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
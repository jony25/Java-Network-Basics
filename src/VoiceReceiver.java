import javax.sound.sampled.*;
import java.net.*;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;

class VoiceReceiver implements Runnable {
    private final int port;
    private final AudioFormat pcmFormat = new AudioFormat(44100, 16, 1, true, false);
    private final AudioFormat ulawFormat = new AudioFormat(AudioFormat.Encoding.ULAW, 8000, 8, 1, 1, 8000, false);
    private final JitterBuffer jitterBuffer = new JitterBuffer(1024); // Size in PCM bytes

    public VoiceReceiver(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            Thread.ofVirtual().start(this::playoutLoop);
            
            byte[] buffer = new byte[512 + 8]; // 512 ULAW + 8 seq
            while (!Thread.currentThread().isInterrupted()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                ByteBuffer bb = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
                long seq = bb.getLong();
                
                int ulawLen = packet.getLength() - 8;
                byte[] ulawAudio = new byte[ulawLen];
                bb.get(ulawAudio);
                
                // Convert ULAW to PCM
                ByteArrayInputStream bais = new ByteArrayInputStream(ulawAudio);
                AudioInputStream ulawStream = new AudioInputStream(bais, ulawFormat, ulawLen);
                AudioInputStream pcmStream = AudioSystem.getAudioInputStream(pcmFormat, ulawStream);
                
                byte[] pcmAudio = new byte[ulawLen * 2];
                int pcmCount = pcmStream.read(pcmAudio);
                
                if (pcmCount > 0) {
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
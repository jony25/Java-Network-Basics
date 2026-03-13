import java.util.concurrent.PriorityBlockingQueue;

public class JitterBuffer {
    private final PriorityBlockingQueue<VoicePacket> buffer = new PriorityBlockingQueue<>();
    private long expectedSeq = -1;
    private final int silenceSize;

    public JitterBuffer(int silenceSize) {
        this.silenceSize = silenceSize;
    }

    public void addPacket(VoicePacket packet) {
        buffer.offer(packet);
        if (expectedSeq == -1) {
            expectedSeq = packet.seq; 
        }
    }

    public byte[] readPacket() {
        if (expectedSeq == -1) return new byte[silenceSize];

        VoicePacket next = buffer.peek();
        if (next != null && next.seq == expectedSeq) {
            expectedSeq++;
            return buffer.poll().data;
        } else if (next != null && next.seq < expectedSeq) {
            buffer.poll(); // Descartar paquetes atrasados (Late packets)
            return readPacket();
        }
        
        // Packet Loss Concealment (PLC): Enviamos silencios para no desfasar el reloj
        expectedSeq++;
        return new byte[silenceSize];
    }
}

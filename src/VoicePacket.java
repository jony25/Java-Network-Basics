public class VoicePacket implements Comparable<VoicePacket> {
    public final long seq;
    public final byte[] data;

    public VoicePacket(long seq, byte[] data) {
        this.seq = seq;
        this.data = data;
    }

    @Override
    public int compareTo(VoicePacket o) {
        return Long.compare(this.seq, o.seq);
    }
}

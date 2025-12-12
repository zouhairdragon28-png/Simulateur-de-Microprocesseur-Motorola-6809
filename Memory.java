 package projet;

public class Memory {
    // La mémoire du 6809 fait 64 Ko (65536 octets)
    private byte[] data = new byte[65536];

    public int readByte(int address) {
        return data[address & 0xFFFF] & 0xFF;
    }

    public int readWord(int address) {
        int high = readByte(address);
        int low = readByte(address + 1);
        return (high << 8) | low;
    }

    public void writeByte(int address, int value) {
        data[address & 0xFFFF] = (byte) value;
    }

    public void reset() {
        for (int i = 0; i < data.length; i++) {
            data[i] = 0;
        }
    }
}
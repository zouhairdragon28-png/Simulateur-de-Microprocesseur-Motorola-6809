  package projetM6809;

public class Memory {
    // La mémoire contient 65536 octets (64 Ko)
    private byte[] data = new byte[65536];

    public Memory() {
        reset();
    }

    public void reset() {
        for (int i = 0; i < data.length; i++) {
            // Règle :
            // De 0000 à 7FFF (RAM) --> Initialisé à 00
            // De 8000 à FFFF (ROM) --> Initialisé à FF (Mémoire vide standard)
            if (i < 0x8000) {
                data[i] = 0;
            } else {
                data[i] = (byte) 0xFF; // C'est pour ça que A devient FF quand on lit en ROM
            }
        }
    }

    public int read(int address) {
        // Lecture d'un octet et conversion en entier positif (0-255)
        return data[address & 0xFFFF] & 0xFF;
    }

    public void write(int address, int value) {
        data[address & 0xFFFF] = (byte) value;
    }
}

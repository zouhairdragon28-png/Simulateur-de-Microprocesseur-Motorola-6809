 package projet;

public class Cpu6809 {
    // Registres
    public int A, B;   // 8 bits
    public int X, Y;   // 16 bits
    public int PC;     // Program Counter
    public int CC;     // Flags: ... N Z V C (C=bit0, V=bit1, Z=bit2, N=bit3)
    
    private Memory memory;

    public Cpu6809(Memory memory) {
        this.memory = memory;
        reset();
    }

    public void reset() {
        A = 0; B = 0; X = 0; Y = 0; CC = 0;
        PC = 0x1000; // Adresse de départ par défaut
    }

    // --- CŒUR DU PROCESSEUR ---
    public void step() {
        int opcode = memory.readByte(PC);
        PC++;

        switch (opcode) {
            case 0x00: break; // NOP
            
            // --- CHARGEMENT (LOAD) ---
            case 0x86: // LDA Immédiat (#val)
                A = fetchByte(); updateFlagsNZ(A); break;
            case 0xB6: // LDA Étendu ($OOFF) -> Lire en mémoire
                A = memory.readByte(fetchWord()); updateFlagsNZ(A); break;
            case 0xC6: // LDB Immédiat
                B = fetchByte(); updateFlagsNZ(B); break;

            // --- STOCKAGE (STORE) ---
            case 0xB7: // STA Étendu (Sauvegarder A vers mémoire)
                memory.writeByte(fetchWord(), A);
                updateFlagsNZ(A); 
                break;
                
            // --- ARITHMÉTIQUE ---
            case 0x8B: // ADDA Immédiat (A = A + val)
                int valAdd = fetchByte();
                int resAdd = A + valAdd;
                updateFlagsCC(A, valAdd, resAdd); // Gestion complète des flags (C, V, N, Z)
                A = resAdd & 0xFF;
                break;
                
            case 0x80: // SUBA Immédiat (A = A - val)
                int valSub = fetchByte();
                int resSub = A - valSub;
                updateFlagsCC(A, -valSub, resSub);
                A = resSub & 0xFF;
                break;

            // --- COMPARAISON & SAUTS (POUR LES BOUCLES) ---
            case 0x81: // CMPA Immédiat (Compare A avec val)
                int valCmp = fetchByte();
                int resCmp = A - valCmp;
                updateFlagsNZ(resCmp); // Juste mettre à jour les drapeaux, ne pas changer A
                // Gestion spéciale du Carry pour CMP (A < M)
                if ((A & 0xFF) < (valCmp & 0xFF)) CC |= 0x01; else CC &= ~0x01;
                break;

            case 0x27: // BEQ (Branch if Equal / Z=1) - Saut relatif si égalité
                byte offset = (byte) fetchByte(); // Offset signé (-128 à +127)
                if ((CC & 0x04) != 0) { // Si flag Z est allumé
                    PC = PC + offset;
                }
                break;
                
            case 0x20: // BRA (Branch Always) - Saut inconditionnel (GOTO)
                byte offsetBra = (byte) fetchByte();
                PC = PC + offsetBra;
                break;

            default:
                System.out.println(String.format("Instruction inconnue : %02X à %04X", opcode, PC-1));
                break;
        }
    }

    // --- OUTILS ---
    
    // Lire l'octet suivant (et avancer PC)
    private int fetchByte() {
        int val = memory.readByte(PC);
        PC++;
        return val;
    }

    // Lire le mot suivant (16 bits) (et avancer PC de 2)
    private int fetchWord() {
        int high = memory.readByte(PC); PC++;
        int low = memory.readByte(PC); PC++;
        return (high << 8) | low;
    }

    // Mettre à jour N (Négatif) et Z (Zéro)
    private void updateFlagsNZ(int value) {
        // Z (Zero) - Bit 2
        if ((value & 0xFF) == 0) CC |= 0x04; else CC &= ~0x04;
        // N (Negative) - Bit 3 (8ème bit de la donnée)
        if ((value & 0x80) != 0) CC |= 0x08; else CC &= ~0x08;
    }
    
    // Mise à jour complète (Carry, Overflow, Zero, Negative) pour l'arithmétique simple
    private void updateFlagsCC(int v1, int v2, int r) {
        updateFlagsNZ(r);
        // C (Carry) - Bit 0 - Si on dépasse 255 (0xFF)
        if ((r & 0x100) != 0) CC |= 0x01; else CC &= ~0x01;
        // V (Overflow) - Bit 1 - Compliqué (changement de signe incorrect)
        // Simplification pour ce projet :
        boolean v1Pos = (v1 & 0x80) == 0;
        boolean v2Pos = (v2 & 0x80) == 0;
        boolean rPos = (r & 0x80) == 0;
        if (v1Pos == v2Pos && v1Pos != rPos) CC |= 0x02; else CC &= ~0x02;
    }
}
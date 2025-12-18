   package projetM6809;

public class Cpu6809 {
    private Memory memory;
    
    private int PC; 
    private int S, U, X, Y; 
    private int A, B, DP, CC; 
    
    private String currentInstructionStr = "RESET"; 

    public Cpu6809(Memory memory) {
        this.memory = memory;
        reset();
    }

    public void reset() {
        PC = 0xF000;
        S = 0x0000; U = 0x0000; X = 0x0000; Y = 0x0000;
        A = 0x00; B = 0x00; DP = 0x00;
        CC = 0; 
        currentInstructionStr = "RESET";
    }

    public void step() {
        int opcode = memory.read(PC);
        PC++;

        switch(opcode) {
            // --- 8 BITS OPERATIONS ---
            case 0x86: // LDA Imm
                A = memory.read(PC++); updateFlags(A); 
                currentInstructionStr = String.format("LDA #$%02X", A); break;
            case 0xC6: // LDB Imm
                B = memory.read(PC++); updateFlags(B); 
                currentInstructionStr = String.format("LDB #$%02X", B); break;
                
            // --- INDEXED MODE (,X) ---
            case 0xA6: // LDA ,X
                memory.read(PC++); // Skip postbyte (0x84)
                A = memory.read(X); updateFlags(A);
                currentInstructionStr = "LDA ,X"; break;
            case 0xE6: // LDB ,X
                memory.read(PC++); 
                B = memory.read(X); updateFlags(B);
                currentInstructionStr = "LDB ,X"; break;
            case 0xA7: // STA ,X
                memory.read(PC++);
                memory.write(X, A); updateFlags(A);
                currentInstructionStr = "STA ,X"; break;
            case 0xE7: // STB ,X
                memory.read(PC++);
                memory.write(X, B); updateFlags(B);
                currentInstructionStr = "STB ,X"; break;

            // --- DIRECT MODE (<$xx) ---
            case 0x96: // LDA Direct
                int addrA = (DP << 8) | memory.read(PC++);
                A = memory.read(addrA); updateFlags(A);
                currentInstructionStr = String.format("LDA <$%02X", addrA & 0xFF); break;
            case 0xD6: // LDB Direct
                int addrB = (DP << 8) | memory.read(PC++);
                B = memory.read(addrB); updateFlags(B);
                currentInstructionStr = String.format("LDB <$%02X", addrB & 0xFF); break;
            case 0x97: // STA Direct
                int sAddrA = (DP << 8) | memory.read(PC++);
                memory.write(sAddrA, A); updateFlags(A);
                currentInstructionStr = String.format("STA <$%02X", sAddrA & 0xFF); break;
            case 0xD7: // STB Direct
                int sAddrB = (DP << 8) | memory.read(PC++);
                memory.write(sAddrB, B); updateFlags(B);
                currentInstructionStr = String.format("STB <$%02X", sAddrB & 0xFF); break;

            // --- ARITHMETIC ---
            case 0x8B: // ADDA Imm
                int valA = memory.read(PC++); A = (A + valA) & 0xFF; updateFlags(A);
                currentInstructionStr = String.format("ADDA #$%02X", valA); break;
            case 0xCB: // ADDB Imm
                int valB = memory.read(PC++); B = (B + valB) & 0xFF; updateFlags(B);
                currentInstructionStr = String.format("ADDB #$%02X", valB); break;

            // --- 16 BITS ---
            case 0x8E: X = read16(); updateFlags16(X); currentInstructionStr = String.format("LDX #$%04X", X); break;
            case 0xCE: S = read16(); updateFlags16(S); currentInstructionStr = String.format("LDS #$%04X", S); break;
            case 0x8C: // CMPX
                int valX = read16(); int resX = X - valX;
                if (resX == 0) CC |= 0x04; else CC &= ~0x04;
                currentInstructionStr = String.format("CMPX #$%04X", valX); break;

            // --- SPECIALS ---
            case 0x1E: // EXG
                int pb = memory.read(PC++);
                if (pb == 0x8B) { int t=A; A=DP; DP=t; currentInstructionStr = "EXG A,DP"; }
                break;
            case 0x1F: // TFR
                int pbT = memory.read(PC++);
                if (pbT == 0x8B) { DP = A; currentInstructionStr = "TFR A,DP"; }
                break;
            case 0x34: memory.read(PC++); S-=2; currentInstructionStr = "PSHS"; break;
            case 0x35: memory.read(PC++); S+=2; currentInstructionStr = "PULS"; break;
            case 0x7E: PC = read16(); currentInstructionStr = "JMP"; break;
            case 0x20: PC = PC + (byte)memory.read(PC++); currentInstructionStr = "BRA"; break;
            
            case 0x4F: A=0; updateFlags(0); currentInstructionStr = "CLRA"; break;
            case 0x5F: B=0; updateFlags(0); currentInstructionStr = "CLRB"; break;
            case 0x3F: currentInstructionStr = "SWI (END)"; break;
            case 0x12: currentInstructionStr = "NOP"; break;

            default: currentInstructionStr = String.format("??? (%02X)", opcode); break; 
        }
    }

    private int read16() { int h = memory.read(PC++); int l = memory.read(PC++); return (h << 8) | l; }
    private void updateFlags(int res) { CC &= ~(0x08 | 0x04); if (res == 0) CC |= 0x04; if ((res & 0x80) != 0) CC |= 0x08; }
    private void updateFlags16(int res) { CC &= ~(0x08 | 0x04); if (res == 0) CC |= 0x04; }
    public String getCurrentInstructionStr() { return currentInstructionStr; }
    
    public int getPC() { return PC; } public void setPC(int pc) { this.PC = pc; }
    public int getS() { return S; } public int getU() { return U; }
    public int getX() { return X; } public int getY() { return Y; }
    public int getA() { return A; } public int getB() { return B; }
    public int getDP() { return DP; } public int getCC() { return CC; }
}

package projetM6809;

public class Cpu6809 {
    private Memory memory;
    
    private int PC; 
    private int S, U, X, Y; 
    private int A, B, DP, CC; 
    
    private boolean programFinished = false; 
    
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
        programFinished = false; 
        currentInstructionStr = "RESET";
    }

    public void step() {
        if (programFinished) return; 

        int opcode = memory.read(PC);
        PC++;

        switch(opcode) {
            // --- 8 BITS (IMMEDIATE) --  
            case 0x86: A = memory.read(PC++); updateFlags(A); 
                       currentInstructionStr = String.format("LDA #$%02X", A); break;
            case 0xC6: B = memory.read(PC++); updateFlags(B); 
                       currentInstructionStr = String.format("LDB #$%02X", B); break;

            // --- INCREMENT / DECREMENT ---
            case 0x4A: A = (A - 1) & 0xFF; updateFlags(A); currentInstructionStr = "DECA"; break;
            case 0x5A: B = (B - 1) & 0xFF; updateFlags(B); currentInstructionStr = "DECB"; break;
            case 0x4C: A = (A + 1) & 0xFF; updateFlags(A); currentInstructionStr = "INCA"; break;
            case 0x5C: B = (B + 1) & 0xFF; updateFlags(B); currentInstructionStr = "INCB"; break;

            // --- INDEXED MODE ---
            case 0xA6: memory.read(PC++); A = memory.read(X); updateFlags(A); currentInstructionStr = "LDA ,X"; break;
            case 0xE6: memory.read(PC++); B = memory.read(X); updateFlags(B); currentInstructionStr = "LDB ,X"; break;
            case 0xA7: memory.read(PC++); memory.write(X, A); updateFlags(A); currentInstructionStr = "STA ,X"; break;
            case 0xE7: memory.read(PC++); memory.write(X, B); updateFlags(B); currentInstructionStr = "STB ,X"; break;

            // --- DIRECT MODE ---
            case 0x96: int aA=(DP<<8)|memory.read(PC++); A=memory.read(aA); updateFlags(A); 
                       currentInstructionStr=String.format("LDA <$%02X", aA&0xFF); break;
            case 0xD6: int aB=(DP<<8)|memory.read(PC++); B=memory.read(aB); updateFlags(B); 
                       currentInstructionStr=String.format("LDB <$%02X", aB&0xFF); break;
            case 0x97: int sA=(DP<<8)|memory.read(PC++); memory.write(sA,A); updateFlags(A); 
                       currentInstructionStr=String.format("STA <$%02X", sA&0xFF); break;
            case 0xD7: int sB=(DP<<8)|memory.read(PC++); memory.write(sB,B); updateFlags(B); 
                       currentInstructionStr=String.format("STB <$%02X", sB&0xFF); break;

            // --- BRANCH ---
            case 0x20: byte off = (byte)memory.read(PC++); PC += off; 
                       currentInstructionStr = String.format("BRA $%02X", off); break;
            case 0x26: byte offne = (byte)memory.read(PC++); if((CC & 0x04) == 0) PC += offne; 
                       currentInstructionStr = String.format("BNE $%02X", offne); break;
            case 0x27: byte offeq = (byte)memory.read(PC++); if((CC & 0x04) != 0) PC += offeq; 
                       currentInstructionStr = String.format("BEQ $%02X", offeq); break;

            // --- ARITHMETIC ---
            case 0x8B: int vA=memory.read(PC++); A=(A+vA)&0xFF; updateFlags(A); 
                       currentInstructionStr=String.format("ADDA #$%02X", vA); break;
            case 0xCB: int vB=memory.read(PC++); B=(B+vB)&0xFF; updateFlags(B); 
                       currentInstructionStr=String.format("ADDB #$%02X", vB); break;

            // --- 16 BITS ---
            case 0x8E: X = read16(); updateFlags16(X); 
                       currentInstructionStr = String.format("LDX #$%04X", X); break;
            case 0xCE: S = read16(); updateFlags16(S); 
                       currentInstructionStr = String.format("LDS #$%04X", S); break;
            case 0x8C: int vX = read16(); int rX = X - vX; if(rX==0) CC|=4; else CC&=~4; 
                       currentInstructionStr=String.format("CMPX #$%04X", vX); break;

            // --- SPECIALS ---
            case 0x1E: int p1=memory.read(PC++); if(p1==0x8B){int t=A;A=DP;DP=t;currentInstructionStr="EXG A,DP";} break;
            case 0x1F: int p2=memory.read(PC++); if(p2==0x8B){DP=A;currentInstructionStr="TFR A,DP";} break;
            case 0x34: memory.read(PC++); S-=2; currentInstructionStr="PSHS"; break;
            case 0x35: memory.read(PC++); S+=2; currentInstructionStr="PULS"; break;
            case 0x7E: int tg = read16(); PC = tg; currentInstructionStr=String.format("JMP $%04X", tg); break;
            
            // --- STOP ---
            case 0x3F: 
                programFinished = true; 
                currentInstructionStr = "END"; 
                break;
                
            case 0x4F: A=0; updateFlags(0); currentInstructionStr="CLRA"; break;
            case 0x5F: B=0; updateFlags(0); currentInstructionStr="CLRB"; break;
            case 0x12: currentInstructionStr="NOP"; break;

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


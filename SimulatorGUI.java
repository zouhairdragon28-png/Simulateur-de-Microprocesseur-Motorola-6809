 package projetM6809;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class SimulatorGUI extends JFrame {
    private Cpu6809 cpu;
    private Memory memory;
    private JDesktopPane desktopPane;
    
    private JInternalFrame archFrame, editorFrame, ramFrame, romFrame, progFrame;
    private ArchitecturePanel archPanel;
    private JTextArea editorArea;
    private JTextArea progArea; 
    private JTable ramTable, romTable;
    
    public SimulatorGUI(Cpu6809 cpu, Memory memory) {
        this.cpu = cpu;
        this.memory = memory;
        setTitle("MOTO6809 - Final (Strict Mode)");
        setSize(1300, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        desktopPane = new JDesktopPane();
        desktopPane.setBackground(new Color(50, 50, 60)); 
        add(desktopPane, BorderLayout.CENTER);
        createWindows();
        createMenuBar();
        createToolBar();
    }

    private void createWindows() {
        archFrame = createFrame("Architecture interne du 6809", 10, 10, 280, 500);
        archPanel = new ArchitecturePanel(cpu); archFrame.add(archPanel); desktopPane.add(archFrame);

        editorFrame = createFrame("Editeur", 300, 10, 300, 400);
        JToolBar editBar = new JToolBar();
        JButton btnUpdate = new JButton("Mise à jour");
        btnUpdate.addActionListener(e -> compileCode()); 
        editBar.add(btnUpdate);
        editorArea = new JTextArea(""); editorArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JPanel p = new JPanel(new BorderLayout()); p.add(editBar, BorderLayout.NORTH); p.add(new JScrollPane(editorArea), BorderLayout.CENTER);
        editorFrame.add(p); desktopPane.add(editorFrame);

        ramFrame = createFrame("RAM", 610, 10, 200, 350);
        String[] cols = {"Addr", "Val"}; Object[][] data = new Object[32][2]; 
        for(int i=0; i<32; i++) { data[i][0] = String.format("%04X", i); data[i][1] = "00"; }
        ramTable = new JTable(new DefaultTableModel(data, cols)); ramFrame.add(new JScrollPane(ramTable)); desktopPane.add(ramFrame);

        romFrame = createFrame("ROM", 820, 10, 200, 350);
        Object[][] dataRom = new Object[16][2]; for(int i=0; i<16; i++) { dataRom[i][0] = String.format("%04X", 0xFD55 + i); dataRom[i][1] = "FF"; }
        romTable = new JTable(new DefaultTableModel(dataRom, cols)); romFrame.add(new JScrollPane(romTable)); desktopPane.add(romFrame);
        
        progFrame = createFrame("Programme", 1030, 10, 220, 350);
        progArea = new JTextArea(); progArea.setEditable(false); progArea.setBackground(new Color(240, 240, 240));
        progArea.setFont(new Font("Monospaced", Font.PLAIN, 12)); progFrame.add(new JScrollPane(progArea)); desktopPane.add(progFrame);
    }

    private JInternalFrame createFrame(String title, int x, int y, int w, int h) {
        JInternalFrame f = new JInternalFrame(title, true, true, true, true);
        f.setSize(w, h); f.setLocation(x, y); f.setDefaultCloseOperation(JInternalFrame.HIDE_ON_CLOSE); f.setVisible(true); return f;
    }

    private void compileCode() {
        String code = editorArea.getText();
        if (code.trim().isEmpty()) { JOptionPane.showMessageDialog(this, "Vide !"); return; }
        String[] lines = code.split("\n");
        int address = 0xF000; cpu.setPC(address); memory.reset(); 

        try {
            int lineNum = 0;
            for (String line : lines) {
                lineNum++;
                line = line.trim().toUpperCase();
                if (line.isEmpty()) continue; 
                
                String[] parts = line.trim().split("\\s+");
                String mnemo = parts[0];
                String operandStr = (parts.length > 1) ? parts[1] : "";

                // --- VALIDATION STRICTE ---
                // يمنع الرموز المقلوبة أو المتداخلة
                if (operandStr.contains("$#") || operandStr.contains("#<") || operandStr.contains("#,")) {
                    JOptionPane.showMessageDialog(this, "Erreur syntaxe (Ligne "+lineNum+") : Symboles invalides '" + operandStr + "'");
                    return; // Stop tout
                }

                // Détection type opérande
                boolean isImm = operandStr.startsWith("#");
                boolean isDir = operandStr.startsWith("<");
                boolean isIdx = operandStr.equals("X") || operandStr.equals(",X");

                if (mnemo.equals("LDA")) {
                    if (isIdx) { memory.write(address++, 0xA6); memory.write(address++, 0x84); }
                    else if (isImm) memory.write(address++, 0x86);
                    else if (isDir) memory.write(address++, 0x96);
                    else { JOptionPane.showMessageDialog(this, "Erreur LDA (Ligne "+lineNum+") : Manque # ou <"); return; }
                }
                else if (mnemo.equals("LDB")) {
                    if (isIdx) { memory.write(address++, 0xE6); memory.write(address++, 0x84); }
                    else if (isImm) memory.write(address++, 0xC6);
                    else if (isDir) memory.write(address++, 0xD6);
                    else { JOptionPane.showMessageDialog(this, "Erreur LDB (Ligne "+lineNum+") : Manque # ou <"); return; }
                }
                else if (mnemo.equals("ADDA")) {
                    if (isImm) memory.write(address++, 0x8B); 
                    else { JOptionPane.showMessageDialog(this, "Erreur ADDA (Ligne "+lineNum+") : Doit être Immédiat (#)"); return; }
                }
                else if (mnemo.equals("ADDB")) {
                    if (isImm) memory.write(address++, 0xCB);
                    else { JOptionPane.showMessageDialog(this, "Erreur ADDB (Ligne "+lineNum+") : Doit être Immédiat (#)"); return; }
                }
                else if (mnemo.equals("STA")) {
                    if (isIdx) { memory.write(address++, 0xA7); memory.write(address++, 0x84); }
                    else if (isDir) memory.write(address++, 0x97);
                    else memory.write(address++, 0x97); // Par défaut Direct pour STA
                }
                else if (mnemo.equals("STB")) {
                    if (isIdx) { memory.write(address++, 0xE7); memory.write(address++, 0x84); }
                    else memory.write(address++, 0xD7);
                }
                else if (mnemo.equals("LDX")) { memory.write(address++, 0x8E); }
                else if (mnemo.equals("LDS")) { memory.write(address++, 0xCE); }
                else if (mnemo.equals("CMPX")) { memory.write(address++, 0x8C); }
                else if (mnemo.equals("DECA")) { memory.write(address++, 0x4A); continue; }
                else if (mnemo.equals("DECB")) { memory.write(address++, 0x5A); continue; }
                else if (mnemo.equals("INCA")) { memory.write(address++, 0x4C); continue; }
                else if (mnemo.equals("INCB")) { memory.write(address++, 0x5C); continue; }
                else if (mnemo.equals("BNE")) { memory.write(address++, 0x26); }
                else if (mnemo.equals("BEQ")) { memory.write(address++, 0x27); }
                else if (mnemo.equals("BRA")) { memory.write(address++, 0x20); }
                else if (mnemo.equals("JMP")) { memory.write(address++, 0x7E); }
                
                else if (mnemo.equals("EXG")) {
                    memory.write(address++, 0x1E);
                    if (line.contains("A") && line.contains("DP")) memory.write(address++, 0x8B); else memory.write(address++, 0x00);
                    continue;
                }
                else if (mnemo.equals("TFR")) {
                    memory.write(address++, 0x1F);
                    if (line.contains("A") && line.contains("DP")) memory.write(address++, 0x8B); else memory.write(address++, 0x00);
                    continue;
                }
                else if (mnemo.equals("PSHS") || mnemo.equals("PULS")) {
                     memory.write(address++, (mnemo.equals("PSHS")?0x34:0x35)); memory.write(address++, 0xFF); continue;
                }
                else if (mnemo.equals("SWI") || mnemo.equals("END")) { memory.write(address++, 0x3F); continue; }
                else if (mnemo.equals("CLRA")) { memory.write(address++, 0x4F); continue; }
                else if (mnemo.equals("CLRB")) { memory.write(address++, 0x5F); continue; }
                else { JOptionPane.showMessageDialog(this, "Inconnu (Ligne "+lineNum+"): " + mnemo); return; }

                // Ecriture de l'opérande
                if (!operandStr.isEmpty() && !isIdx) {
                    // Nettoyage sécurisé
                    String cleanOp = operandStr.replace("#", "").replace("<", "").replace("$", "");
                    try {
                        int val = Integer.parseInt(cleanOp, 16);
                        if (mnemo.equals("LDX") || mnemo.equals("LDS") || mnemo.equals("CMPX") || mnemo.equals("JMP")) {
                            memory.write(address++, (val >> 8) & 0xFF); memory.write(address++, val & 0xFF);
                        } else {
                            memory.write(address++, val);
                        }
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(this, "Erreur valeur (Ligne "+lineNum+") : " + operandStr); return;
                    }
                }
            }
            JOptionPane.showMessageDialog(this, "Assemblage OK !");
            updateAll();
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Erreur: " + ex.getMessage()); }
    }

    private void updateAll() {
        archPanel.updateValues();
        for(int i=0; i<32; i++) ramTable.setValueAt(String.format("%02X", memory.read(i)), i, 1);
        updateProgramWindow();
        repaint();
    }
    
    private void updateProgramWindow() {
        StringBuilder sb = new StringBuilder(); int p = 0xF000; 
        
        for(int i=0; i<50; i++) {
            int opcode = memory.read(p); 
            String line = String.format("%04X : %02X", p, opcode);
            String fullInst = ""; int bytes = 1;
            int op1 = memory.read(p+1); int op2 = memory.read(p+2);
            
            // Affichage complet avec valeur
            if (opcode==0x86) { fullInst=String.format("LDA #$%02X", op1); bytes=2; }
            else if (opcode==0x4A) { fullInst="DECA"; bytes=1; }
            else if (opcode==0x5A) { fullInst="DECB"; bytes=1; }
            else if (opcode==0x4C) { fullInst="INCA"; bytes=1; }
            else if (opcode==0x5C) { fullInst="INCB"; bytes=1; }
            else if (opcode==0x26) { fullInst=String.format("BNE $%02X", op1); bytes=2; }
            else if (opcode==0x27) { fullInst=String.format("BEQ $%02X", op1); bytes=2; }
            else if (opcode==0x20) { fullInst=String.format("BRA $%02X", op1); bytes=2; }
            else if (opcode==0xA6) { fullInst="LDA ,X"; bytes=2; }
            else if (opcode==0x96) { fullInst=String.format("LDA <$%02X", op1); bytes=2; }
            else if (opcode==0xC6) { fullInst=String.format("LDB #$%02X", op1); bytes=2; }
            else if (opcode==0xD6) { fullInst=String.format("LDB <$%02X", op1); bytes=2; }
            else if (opcode==0x97) { fullInst=String.format("STA <$%02X", op1); bytes=2; }
            else if (opcode==0xD7) { fullInst=String.format("STB <$%02X", op1); bytes=2; }
            else if (opcode==0x8B) { fullInst=String.format("ADDA #$%02X", op1); bytes=2; }
            else if (opcode==0xCB) { fullInst=String.format("ADDB #$%02X", op1); bytes=2; }
            else if (opcode==0x8E) { fullInst=String.format("LDX #$%02X%02X", op1, op2); bytes=3; }
            else if (opcode==0xCE) { fullInst=String.format("LDS #$%02X%02X", op1, op2); bytes=3; }
            else if (opcode==0x8C) { fullInst=String.format("CMPX #$%02X%02X", op1, op2); bytes=3; }
            else if (opcode==0x1E) { fullInst="EXG A,DP"; bytes=2; }
            else if (opcode==0x1F) { fullInst="TFR A,DP"; bytes=2; }
            else if (opcode==0x34) { fullInst="PSHS"; bytes=2; }
            else if (opcode==0x35) { fullInst="PULS"; bytes=2; }
            else if (opcode==0x3F) { fullInst="END"; bytes=1; } 
            else if (opcode==0x7E) { fullInst=String.format("JMP $%02X%02X", op1, op2); bytes=3; }
            else if (opcode==0x4F) { fullInst="CLRA"; bytes=1; }
            else if (opcode==0x5F) { fullInst="CLRB"; bytes=1; }
            else if (opcode==0xFF) { fullInst="..."; bytes=1; } 
            else { fullInst="???"; bytes=1; }

            line += " : " + fullInst;
            sb.append(line).append("\n");
            
            if (opcode == 0x3F || opcode == 0xFF) break; 
            p += bytes;
        }
        progArea.setText(sb.toString());
    }
    
    private void createMenuBar() {
        JMenuBar mb = new JMenuBar(); JMenu winMenu = new JMenu("Fenêtres");
        addSyncCheckBox(winMenu, "Architecture", archFrame); addSyncCheckBox(winMenu, "Editeur", editorFrame);
        addSyncCheckBox(winMenu, "RAM", ramFrame); addSyncCheckBox(winMenu, "ROM", romFrame); addSyncCheckBox(winMenu, "Programme", progFrame);
        mb.add(new JMenu("Fichier")); mb.add(winMenu); setJMenuBar(mb);
    }
    private void addSyncCheckBox(JMenu m, String t, JInternalFrame f) {
        JCheckBoxMenuItem i = new JCheckBoxMenuItem(t, true);
        i.addActionListener(e -> f.setVisible(i.isSelected()));
        f.addInternalFrameListener(new InternalFrameAdapter() {
            public void internalFrameClosing(InternalFrameEvent e) { i.setSelected(false); }
            public void internalFrameOpened(InternalFrameEvent e) { i.setSelected(true); }
        }); m.add(i);
    }
    private void createToolBar() {
        JToolBar tb = new JToolBar(); tb.setFloatable(false);
        JButton r = new JButton("RESET"); r.addActionListener(e->{cpu.reset(); updateAll();});
        JButton s = new JButton("Step"); s.addActionListener(e->{cpu.step(); updateAll();});
        tb.add(r); tb.add(s); add(tb, BorderLayout.NORTH);
    }

    class ArchitecturePanel extends JPanel {
        private Cpu6809 cpu; private JTextField txtPC, txtInst, txtS, txtU, txtA, txtB, txtDP, txtCC, txtX, txtY;
        public ArchitecturePanel(Cpu6809 cpu) {
            this.cpu = cpu; setLayout(null); setBackground(new Color(230, 230, 230));
            addLbl("PC", 90, 20); txtPC = addField(120, 20, 80);
            txtInst = new JTextField(""); txtInst.setBounds(20, 55, 230, 25); txtInst.setEditable(false);
            txtInst.setHorizontalAlignment(0); txtInst.setForeground(new Color(0, 100, 0)); txtInst.setFont(new Font("Monospaced", 1, 14)); add(txtInst);
            addLbl("S", 20, 90); txtS = addField(50, 90, 60); addLbl("U", 140, 90); txtU = addField(170, 90, 60);
            addLbl("A", 20, 150); txtA = addField(50, 150, 40); addLbl("B", 20, 200); txtB = addField(50, 200, 40);
            addLbl("DP", 15, 250); txtDP = addField(50, 250, 40); txtCC = addField(110, 250, 120);
            JLabel f = new JLabel("E F H I N Z V C"); f.setFont(new Font("SansSerif", 0, 10)); f.setBounds(120, 275, 120, 20); add(f);
            addLbl("X", 20, 310); txtX = addField(50, 310, 60); addLbl("Y", 140, 310); txtY = addField(170, 310, 60);
            updateValues();
        }
        private JTextField addField(int x, int y, int w) {
            JTextField t = new JTextField(); t.setBounds(x,y,w,25); t.setEditable(false);
            t.setHorizontalAlignment(0); t.setForeground(Color.BLUE); t.setFont(new Font("SansSerif", 1, 13)); add(t); return t;
        }
        private void addLbl(String s, int x, int y) { JLabel l=new JLabel(s); l.setBounds(x,y,30,25); l.setFont(new Font("SansSerif", 1, 14)); add(l); }
        public void updateValues() {
            txtPC.setText(String.format("%04X", cpu.getPC())); txtInst.setText(cpu.getCurrentInstructionStr());
            txtS.setText(String.format("%04X", cpu.getS())); txtU.setText(String.format("%04X", cpu.getU()));
            txtA.setText(String.format("%02X", cpu.getA())); txtB.setText(String.format("%02X", cpu.getB()));
            txtDP.setText(String.format("%02X", cpu.getDP())); txtX.setText(String.format("%04X", cpu.getX()));
            txtY.setText(String.format("%04X", cpu.getY()));
            String bin = Integer.toBinaryString(cpu.getCC()); while(bin.length()<8)bin="0"+bin; txtCC.setText(bin);
        }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); g.setColor(new Color(200, 200, 200));
            int[] x={100, 220, 220, 180, 100}; int[] y={140, 140, 210, 230, 210};
            g.fillPolygon(x,y,5); g.setColor(Color.GRAY); g.drawPolygon(x,y,5);
            g.setColor(Color.DARK_GRAY); g.setFont(new Font("Arial", 1, 16)); g.drawString("UAL", 145, 190);
            g.drawLine(90, 162, 100, 162); g.drawLine(90, 212, 100, 212); 
        }
    }
}  

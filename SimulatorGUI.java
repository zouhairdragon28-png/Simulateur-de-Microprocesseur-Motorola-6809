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
        
        setTitle("MOTO6809 - Simulateur Final (Indexed Mode Fixed)");
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
        archPanel = new ArchitecturePanel(cpu);
        archFrame.add(archPanel);
        desktopPane.add(archFrame);

        editorFrame = createFrame("Editeur", 300, 10, 300, 400);
        JToolBar editBar = new JToolBar();
        JButton btnUpdate = new JButton("Mise à jour");
        btnUpdate.addActionListener(e -> compileCode()); 
        editBar.add(btnUpdate);
        editorArea = new JTextArea(""); 
        editorArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        
        JPanel p = new JPanel(new BorderLayout());
        p.add(editBar, BorderLayout.NORTH);
        p.add(new JScrollPane(editorArea), BorderLayout.CENTER);
        editorFrame.add(p);
        desktopPane.add(editorFrame);

        ramFrame = createFrame("RAM", 610, 10, 200, 350);
        String[] cols = {"Addr", "Val"};
        Object[][] data = new Object[32][2]; 
        for(int i=0; i<32; i++) { data[i][0] = String.format("%04X", i); data[i][1] = "00"; }
        ramTable = new JTable(new DefaultTableModel(data, cols));
        ramFrame.add(new JScrollPane(ramTable));
        desktopPane.add(ramFrame);

        romFrame = createFrame("ROM", 820, 10, 200, 350);
        Object[][] dataRom = new Object[16][2];
        for(int i=0; i<16; i++) { dataRom[i][0] = String.format("%04X", 0xFD55 + i); dataRom[i][1] = "FF"; }
        romTable = new JTable(new DefaultTableModel(dataRom, cols));
        romFrame.add(new JScrollPane(romTable));
        desktopPane.add(romFrame);
        
        progFrame = createFrame("Programme", 1030, 10, 220, 350);
        progArea = new JTextArea();
        progArea.setEditable(false); 
        progArea.setBackground(new Color(240, 240, 240));
        progArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        progFrame.add(new JScrollPane(progArea));
        desktopPane.add(progFrame);
    }

    private JInternalFrame createFrame(String title, int x, int y, int w, int h) {
        JInternalFrame f = new JInternalFrame(title, true, true, true, true);
        f.setSize(w, h);
        f.setLocation(x, y);
        f.setDefaultCloseOperation(JInternalFrame.HIDE_ON_CLOSE);
        f.setVisible(true);
        return f;
    }

    private void compileCode() {
        String code = editorArea.getText();
        if (code.trim().isEmpty()) { JOptionPane.showMessageDialog(this, "L'éditeur est vide !"); return; }

        String[] lines = code.split("\n");
        int address = 0xF000; 
        cpu.setPC(address);
        memory.reset(); 

        try {
            int lineNum = 0;
            for (String line : lines) {
                lineNum++;
                line = line.trim().toUpperCase();
                if (line.isEmpty()) continue; 
                
                // Nettoyage: on garde '#' et '<'
                // Note: ',X' devient ' X' après nettoyage
                String workingLine = line.replace("$", "").replace(",", " ");
                String[] parts = workingLine.trim().split("\\s+");
                
                if (parts.length == 0) continue;
                String mnemo = parts[0];
                String operandStr = (parts.length > 1) ? parts[1] : "";

                int opcode = 0;
                boolean hasOperand = false;
                boolean is16bit = false;
                boolean isIndexed = operandStr.equals("X"); // Si l'opérande est juste "X", c'est ",X"

                if (mnemo.equals("LDA")) {
                    if (isIndexed) { opcode=0xA6; memory.write(address++, opcode); memory.write(address++, 0x84); continue; }
                    else if (operandStr.startsWith("#")) opcode = 0x86; 
                    else if (operandStr.startsWith("<")) opcode = 0x96; 
                    else opcode = 0x86; 
                    hasOperand = true;
                }
                else if (mnemo.equals("LDB")) {
                    if (isIndexed) { opcode=0xE6; memory.write(address++, opcode); memory.write(address++, 0x84); continue; }
                    else if (operandStr.startsWith("#")) opcode = 0xC6; 
                    else if (operandStr.startsWith("<")) opcode = 0xD6; 
                    else opcode = 0xC6; 
                    hasOperand = true;
                }
                else if (mnemo.equals("STA")) {
                    if (isIndexed) { opcode=0xA7; memory.write(address++, opcode); memory.write(address++, 0x84); continue; }
                    else { opcode = 0x97; hasOperand = true; }
                }
                else if (mnemo.equals("STB")) {
                    if (isIndexed) { opcode=0xE7; memory.write(address++, opcode); memory.write(address++, 0x84); continue; }
                    else { opcode = 0xD7; hasOperand = true; }
                }
                else if (mnemo.equals("ADDA")) { opcode = 0x8B; hasOperand = true; }
                else if (mnemo.equals("ADDB")) { opcode = 0xCB; hasOperand = true; }
                else if (mnemo.equals("LDX")) { opcode = 0x8E; hasOperand = true; is16bit = true; }
                else if (mnemo.equals("LDS")) { opcode = 0xCE; hasOperand = true; is16bit = true; }
                else if (mnemo.equals("CMPX")) { opcode = 0x8C; hasOperand = true; is16bit = true; }
                
                else if (mnemo.equals("EXG")) {
                    memory.write(address++, 0x1E);
                    if (line.contains("A") && line.contains("DP")) memory.write(address++, 0x8B);
                    else memory.write(address++, 0x00);
                    continue;
                }
                else if (mnemo.equals("TFR")) {
                    memory.write(address++, 0x1F);
                    if (line.contains("A") && line.contains("DP")) memory.write(address++, 0x8B);
                    else memory.write(address++, 0x00);
                    continue;
                }
                else if (mnemo.equals("PSHS") || mnemo.equals("PULS")) {
                     memory.write(address++, (mnemo.equals("PSHS")?0x34:0x35));
                     memory.write(address++, 0xFF); continue;
                }
                else if (mnemo.equals("SWI")) { memory.write(address++, 0x3F); continue; }
                else if (mnemo.equals("END")) { continue; }
                else if (mnemo.equals("CLRA")) { memory.write(address++, 0x4F); continue; }
                else if (mnemo.equals("CLRB")) { memory.write(address++, 0x5F); continue; }
                else {
                    JOptionPane.showMessageDialog(this, "Inconnu (Ligne "+lineNum+"): " + mnemo);
                    return;
                }

                memory.write(address++, opcode);

                if (hasOperand) {
                    String cleanOp = operandStr.replace("#", "").replace("<", "");
                    try {
                        int val = Integer.parseInt(cleanOp, 16);
                        if (is16bit) {
                            memory.write(address++, (val >> 8) & 0xFF);
                            memory.write(address++, val & 0xFF);
                        } else {
                            memory.write(address++, val);
                        }
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(this, "Erreur valeur: " + operandStr);
                        return;
                    }
                }
            }
            JOptionPane.showMessageDialog(this, "Assemblage réussi !");
            updateAll();
        } catch (Exception ex) { 
            JOptionPane.showMessageDialog(this, "Erreur critique : " + ex.getMessage()); 
        }
    }

    private void updateAll() {
        archPanel.updateValues();
        for(int i=0; i<32; i++) ramTable.setValueAt(String.format("%02X", memory.read(i)), i, 1);
        updateProgramWindow();
        repaint();
    }
    
    private void updateProgramWindow() {
        StringBuilder sb = new StringBuilder();
        int p = 0xF000; 
        for(int i=0; i<15; i++) {
            int opcode = memory.read(p);
            if (opcode == 0) break; 
            
            String line = String.format("%04X : %02X", p, opcode);
            if (opcode == 0x86) { line += " : LDA Imm"; p+=2; }
            else if (opcode == 0xA6) { line += " : LDA ,X"; p+=2; } // Indexed
            else if (opcode == 0x96) { line += " : LDA Dir"; p+=2; }
            else if (opcode == 0xC6) { line += " : LDB Imm"; p+=2; }
            else if (opcode == 0xD6) { line += " : LDB Dir"; p+=2; }
            else if (opcode == 0x97) { line += " : STA Dir"; p+=2; }
            else if (opcode == 0xD7) { line += " : STB Dir"; p+=2; }
            else if (opcode == 0x8B) { line += " : ADDA"; p+=2; }
            else if (opcode == 0x1E) { line += " : EXG"; p+=2; }
            else if (opcode == 0x1F) { line += " : TFR"; p+=2; }
            else if (opcode == 0x34) { line += " : PSHS"; p+=2; }
            else if (opcode == 0x35) { line += " : PULS"; p+=2; }
            else if (opcode == 0xCE) { line += " : LDS"; p+=3; }
            else if (opcode == 0x8E) { line += " : LDX"; p+=3; }
            else if (opcode == 0x8C) { line += " : CMPX"; p+=3; }
            else if (opcode == 0x3F) { line += " : SWI"; p+=1; }
            else { line += " : ???"; p+=1; }
            
            sb.append(line).append("\n");
        }
        progArea.setText(sb.toString());
    }
    
    private void createMenuBar() {
        JMenuBar mb = new JMenuBar();
        JMenu winMenu = new JMenu("Fenêtres");
        addSyncCheckBox(winMenu, "Architecture", archFrame);
        addSyncCheckBox(winMenu, "Editeur", editorFrame);
        addSyncCheckBox(winMenu, "RAM", ramFrame);
        addSyncCheckBox(winMenu, "ROM", romFrame); 
        addSyncCheckBox(winMenu, "Programme", progFrame);
        mb.add(new JMenu("Fichier"));
        mb.add(winMenu);
        setJMenuBar(mb);
    }
    
    private void addSyncCheckBox(JMenu menu, String title, JInternalFrame frame) {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(title, true);
        item.addActionListener(e -> frame.setVisible(item.isSelected()));
        frame.addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameClosing(InternalFrameEvent e) { item.setSelected(false); }
            @Override
            public void internalFrameOpened(InternalFrameEvent e) { item.setSelected(true); }
        });
        menu.add(item);
    }

    private void createToolBar() {
        JToolBar tb = new JToolBar(); tb.setFloatable(false);
        JButton r = new JButton("RESET"); r.addActionListener(e->{cpu.reset(); updateAll();});
        JButton s = new JButton("Step"); s.addActionListener(e->{cpu.step(); updateAll();});
        tb.add(r); tb.add(s); add(tb, BorderLayout.NORTH);
    }

    class ArchitecturePanel extends JPanel {
        private Cpu6809 cpu;
        private JTextField txtPC, txtInst, txtS, txtU, txtA, txtB, txtDP, txtCC, txtX, txtY;

        public ArchitecturePanel(Cpu6809 cpu) {
            this.cpu = cpu; setLayout(null); setBackground(new Color(230, 230, 230));
            addLbl("PC", 90, 20); txtPC = addField(120, 20, 80);
            txtInst = new JTextField("");
            txtInst.setBounds(20, 55, 230, 25);
            txtInst.setEditable(false);
            txtInst.setHorizontalAlignment(JTextField.CENTER);
            txtInst.setForeground(new Color(0, 100, 0)); 
            txtInst.setFont(new Font("Monospaced", Font.BOLD, 14));
            add(txtInst);
            addLbl("S", 20, 90); txtS = addField(50, 90, 60);
            addLbl("U", 140, 90); txtU = addField(170, 90, 60);
            addLbl("A", 20, 150); txtA = addField(50, 150, 40);
            addLbl("B", 20, 200); txtB = addField(50, 200, 40);
            addLbl("DP", 15, 250); txtDP = addField(50, 250, 40); 
            txtCC = addField(110, 250, 120);
            JLabel f = new JLabel("E F H I N Z V C"); 
            f.setFont(new Font("SansSerif", Font.PLAIN, 10)); 
            f.setBounds(120, 275, 120, 20); add(f);
            addLbl("X", 20, 310); txtX = addField(50, 310, 60);
            addLbl("Y", 140, 310); txtY = addField(170, 310, 60);
            updateValues();
        }
        private JTextField addField(int x, int y, int w) {
            JTextField t = new JTextField(); t.setBounds(x,y,w,25); t.setEditable(false);
            t.setHorizontalAlignment(0); t.setForeground(Color.BLUE); 
            t.setFont(new Font("SansSerif", Font.BOLD, 13)); add(t); return t;
        }
        private void addLbl(String s, int x, int y) { 
            JLabel l=new JLabel(s); l.setBounds(x,y,30,25); 
            l.setFont(new Font("SansSerif", Font.BOLD, 14)); add(l); 
        }
        public void updateValues() {
            txtPC.setText(String.format("%04X", cpu.getPC()));
            txtInst.setText(cpu.getCurrentInstructionStr());
            txtS.setText(String.format("%04X", cpu.getS()));
            txtU.setText(String.format("%04X", cpu.getU()));
            txtA.setText(String.format("%02X", cpu.getA()));
            txtB.setText(String.format("%02X", cpu.getB()));
            txtDP.setText(String.format("%02X", cpu.getDP()));
            txtX.setText(String.format("%04X", cpu.getX()));
            txtY.setText(String.format("%04X", cpu.getY()));
            String bin = Integer.toBinaryString(cpu.getCC()); 
            while(bin.length()<8) bin="0"+bin; txtCC.setText(bin);
        }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); g.setColor(new Color(200, 200, 200));
            int[] x={100, 220, 220, 180, 100}; int[] y={140, 140, 210, 230, 210};
            g.fillPolygon(x,y,5); g.setColor(Color.GRAY); g.drawPolygon(x,y,5);
            g.setColor(Color.DARK_GRAY); g.setFont(new Font("Arial", Font.BOLD, 16));
            g.drawString("UAL", 145, 190);
            g.drawLine(90, 162, 100, 162); g.drawLine(90, 212, 100, 212); 
        }
    }
}

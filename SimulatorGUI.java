 package projet;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;

public class SimulatorGUI extends JFrame {
    private Cpu6809 cpu;
    private Memory memory;

    // Composants graphiques
    private JLabel labelA, labelB, labelX, labelY, labelPC, labelCC;
    private DefaultTableModel memoryModel;
    private JTextArea logArea;

    // --- CONSTRUCTEUR ---
    public SimulatorGUI(Cpu6809 cpu, Memory memory) {
        this.cpu = cpu;
        this.memory = memory;

        setTitle("Simulateur Motorola 6809");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 1. Panneau des Registres
        JPanel registersPanel = new JPanel(new GridLayout(1, 6));
        registersPanel.setBorder(BorderFactory.createTitledBorder("Registres"));
        labelA = new JLabel("A: 00");
        labelB = new JLabel("B: 00");
        labelX = new JLabel("X: 0000");
        labelY = new JLabel("Y: 0000");
        labelPC = new JLabel("PC: 0000");
        labelCC = new JLabel("CC: 00");

        registersPanel.add(labelA); registersPanel.add(labelB);
        registersPanel.add(labelX); registersPanel.add(labelY);
        registersPanel.add(labelPC); registersPanel.add(labelCC);
        add(registersPanel, BorderLayout.NORTH);

        // 2. Panneau Mémoire
        String[] cols = {"Offset", "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "0A", "0B", "0C", "0D", "0E", "0F"};
        memoryModel = new DefaultTableModel(cols, 0);
        JTable table = new JTable(memoryModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- GESTION DE LA MODIFICATION MÉMOIRE (CODE AJOUTÉ) ---
        memoryModel.addTableModelListener(e -> {
            int row = e.getFirstRow();
            int col = e.getColumn();
            
            // Si la modification est valide (pas sur la colonne "Offset" et ligne existante)
            if (row >= 0 && col >= 1) {
                try {
                    // Récupérer la valeur tapée
                    String hexValue = (String) memoryModel.getValueAt(row, col);
                    // Convertir Hex -> Entier
                    int value = Integer.parseInt(hexValue.trim(), 16);
                    
                    // Calculer l'adresse réelle
                    String offsetStr = (String) memoryModel.getValueAt(row, 0);
                    int baseAddress = Integer.parseInt(offsetStr, 16);
                    int realAddress = baseAddress + (col - 1); 
                    
                    // Écrire dans la mémoire
                    memory.writeByte(realAddress, value);
                    log("Mémoire modifiée manuellement à " + String.format("%04X", realAddress) + " : " + String.format("%02X", value));
                } catch (Exception ex) {
                    // Ignore les erreurs si l'utilisateur tape n'importe quoi
                }
            }
        });

        // 3. Panneau Bas (Logs + Boutons)
        JPanel bottomPanel = new JPanel(new BorderLayout());
        logArea = new JTextArea(5, 50);
        logArea.setEditable(false);
        bottomPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton btnStep = new JButton("Step (Pas à Pas)"); 
        JButton btnLoad = new JButton("Charger Test");
        JButton btnReset = new JButton("Reset");

        // Action : Step
        btnStep.addActionListener(e -> {
            try {
                cpu.step();
                updateDisplay();
            } catch (Exception ex) {
                log("Erreur CPU : " + ex.getMessage());
            }
        });

        // Action : Charger Test (Boucle)
        btnLoad.addActionListener(e -> {
            loadTest();
            updateDisplay();
        });

        // Action : Reset
        btnReset.addActionListener(e -> {
            cpu.reset();
            memory.reset();
            log("Reset complet effectué.");
            refreshMemoryView(0x1000, 20);
            updateDisplay();
        });

        btnPanel.add(btnLoad);
        btnPanel.add(btnStep);
        btnPanel.add(btnReset);
        bottomPanel.add(btnPanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        // Affichage initial
        refreshMemoryView(0x1000, 20);
        updateDisplay();
    } 
    // --- FIN DU CONSTRUCTEUR ---

    private void loadTest() {
        int base = 0x1000;
        cpu.PC = base;
        
        // Programme Boucle : Tant que A != 3, A++
        byte[] prog = {
            (byte)0x86, 0x00,          // LDA #0
            (byte)0x8B, 0x01,          // ADDA #1
            (byte)0x81, 0x03,          // CMPA #3
            (byte)0x27, 0x02,          // BEQ +2 (Fin)
            (byte)0x20, (byte)0xF8,    // BRA -8 (Retour ADDA)
            (byte)0xB7, 0x20, 0x00     // STA $2000
        };
        
        for(int i=0; i<prog.length; i++) {
            memory.writeByte(base + i, prog[i]);
        }
        
        log("Programme de test (Boucle) chargé à 1000.");
        refreshMemoryView(base, 20);
    }

    private void updateDisplay() {
        labelA.setText(String.format("A: %02X", cpu.A));
        labelB.setText(String.format("B: %02X", cpu.B));
        labelX.setText(String.format("X: %04X", cpu.X));
        labelY.setText(String.format("Y: %04X", cpu.Y));
        labelPC.setText(String.format("PC: %04X", cpu.PC));
        labelCC.setText(String.format("CC: %02X", cpu.CC));
    }

    private void refreshMemoryView(int start, int rows) {
        memoryModel.setRowCount(0);
        for (int i = 0; i < rows; i++) {
            int addr = start + (i * 16);
            Object[] row = new Object[17];
            row[0] = String.format("%04X", addr);
            for (int j = 0; j < 16; j++) {
                row[j+1] = String.format("%02X", memory.readByte(addr + j));
            }
            memoryModel.addRow(row);
        }
    }

    private void log(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}
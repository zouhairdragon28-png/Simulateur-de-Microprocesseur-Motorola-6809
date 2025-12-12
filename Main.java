package projet;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Memory ram = new Memory();
            Cpu6809 cpu = new Cpu6809(ram);
            SimulatorGUI gui = new SimulatorGUI(cpu, ram);
            gui.setVisible(true);
        });
    }
}
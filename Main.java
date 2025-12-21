  package projetM6809;
 import javax.swing.SwingUtilities;
 import javax.swing.UIManager;

 public class Main {
     public static void main(String[] args) {
         try {
             UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         } catch (Exception e) {}

         SwingUtilities.invokeLater(() -> {
             Memory mem = new Memory();
             Cpu6809 cpu = new Cpu6809(mem);
             SimulatorGUI gui = new SimulatorGUI(cpu, mem);
             gui.setVisible(true);
         });
     }
 }


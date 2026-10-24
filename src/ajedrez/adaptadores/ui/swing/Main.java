package ajedrez.adaptadores.ui.swing;

import javax.swing.*;

/**
 * Punto de entrada de la aplicación.
 * Arranca el menú principal (PantallaMenu) desde donde el jugador
 * elige su perfil, su color y comienza la partida.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Usar el look-and-feel del sistema para mejor integración
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            PantallaMenu menu = new PantallaMenu();
            menu.setVisible(true);
        });
    }
}

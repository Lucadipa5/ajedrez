package ajedrez.adaptadores.ui.swing;

import ajedrez.adaptadores.persistencia.RepositorioPerfilesArchivo;
import ajedrez.nucleo.perfil.ServicioCuentas;

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

            // Raíz de composición: acá se conectan los adaptadores con los puertos del núcleo
            ServicioCuentas cuentas = new ServicioCuentas(new RepositorioPerfilesArchivo("perfiles.dat"));
            PantallaMenu menu = new PantallaMenu(cuentas);
            menu.setVisible(true);
        });
    }
}

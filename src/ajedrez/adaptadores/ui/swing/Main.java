package ajedrez.adaptadores.ui.swing;

import ajedrez.nucleo.servicio.PartidaAjedrez;

import javax.swing.*;

/**
 * Punto de entrada de la aplicación de escritorio.
 * Levanta el adaptador de interfaz gráfica Swing inicializando el servicio del Núcleo.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            PartidaAjedrez partida = new PartidaAjedrez();
            VentanaAjedrez ventana = new VentanaAjedrez(partida);
            ventana.setVisible(true);
        });
    }
}

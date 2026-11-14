package ajedrez.adaptadores.ui.swing;

import ajedrez.adaptadores.ia.MotorPython;
import ajedrez.adaptadores.ia.RegistroDecisionesConsola;
import ajedrez.adaptadores.persistencia.RepositorioPerfilesArchivo;
import ajedrez.adaptadores.registro.RegistroPartidaConsola;
import ajedrez.nucleo.ia.MotorAjedrez;
import ajedrez.nucleo.perfil.ServicioCuentas;
import ajedrez.nucleo.servicio.PartidaAjedrez;

import javax.swing.*;
import java.util.function.Supplier;

/**
 * Punto de entrada y RAÍZ DE COMPOSICIÓN de la aplicación.
 *
 * Es el único lugar que conoce a la vez el núcleo y todos los adaptadores concretos:
 * acá se conecta cada puerto del núcleo con el adaptador elegido (archivo para los perfiles,
 * Python para la IA, consola para los registros, Swing para la interfaz). Cambiar un adaptador
 * es cambiar una línea de este archivo; el núcleo no se entera.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Usar el look-and-feel del sistema para mejor integración
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            // Puerto de salida: persistencia de perfiles → adaptador de archivo
            ServicioCuentas cuentas = new ServicioCuentas(new RepositorioPerfilesArchivo("perfiles.dat"));

            // Puerto de salida: observadores de la partida → registro por consola
            RegistroPartidaConsola registroPartida = new RegistroPartidaConsola();
            Supplier<PartidaAjedrez> fabricaPartidas = () -> {
                PartidaAjedrez partida = PartidaAjedrez.estandar();
                partida.agregarObservador(registroPartida);
                return partida;
            };

            // Puerto de salida: motor de IA → adaptador Python (python-chess), y su registro de decisiones
            Supplier<MotorAjedrez> fabricaMotorIA = MotorPython::new;

            // Puerto de entrada: la interfaz Swing
            PantallaMenu menu = new PantallaMenu(new ServiciosDelJuego(
                    cuentas, fabricaPartidas, fabricaMotorIA, new RegistroDecisionesConsola()));
            menu.setVisible(true);
        });
    }
}

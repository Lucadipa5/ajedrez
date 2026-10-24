package ajedrez.nucleo.estrategias;

import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.tablero.Tablero;

import java.util.List;

/**
 * Interfaz Strategy para el cálculo de trayectorias y posibles casillas destino de una pieza.
 * Permite que las capacidades de movimiento sean intercambiables y combinables.
 */
public interface EstrategiaMovimiento {
    /**
     * Calcula la lista de posiciones físicas a las que la pieza ubicada en 'origen' puede acceder
     * según su patrón geométrico y la disposición de las piezas en el tablero.
     */
    List<Posicion> calcularPosiblesDestinos(Posicion origen, Tablero tablero);
}

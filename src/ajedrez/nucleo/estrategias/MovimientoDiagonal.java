package ajedrez.nucleo.estrategias;

/**
 * Estrategia de movimiento en líneas diagonales.
 * Utilizada por el Alfil y como componente de la Reina.
 */
public class MovimientoDiagonal extends MovimientoLinealBase {
    private static final int[][] DIRECCIONES = {
            {-1, -1}, // Arriba-Izquierda
            {-1, 1},  // Arriba-Derecha
            {1, -1},  // Abajo-Izquierda
            {1, 1}    // Abajo-Derecha
    };

    @Override
    protected int[][] getDirecciones() {
        return DIRECCIONES;
    }
}

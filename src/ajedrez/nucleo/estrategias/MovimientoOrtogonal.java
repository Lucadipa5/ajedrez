package ajedrez.nucleo.estrategias;

/**
 * Estrategia de movimiento en líneas rectas perpendiculares (filas y columnas).
 * Utilizada por la Torre y como componente de la Reina.
 */
public class MovimientoOrtogonal extends MovimientoLinealBase {
    private static final int[][] DIRECCIONES = {
            {-1, 0}, // Arriba
            {1, 0},  // Abajo
            {0, -1}, // Izquierda
            {0, 1}   // Derecha
    };

    @Override
    protected int[][] getDirecciones() {
        return DIRECCIONES;
    }
}

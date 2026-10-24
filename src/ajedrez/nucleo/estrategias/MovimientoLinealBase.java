package ajedrez.nucleo.estrategias;

import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.tablero.Tablero;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase base auxiliar para piezas de largo alcance (deslizantes), como torres y alfiles.
 * Explora casillas en rayos unidimensionales hasta encontrar los límites del tablero o una colisión.
 */
public abstract class MovimientoLinealBase implements EstrategiaMovimiento {
    protected abstract int[][] getDirecciones();

    @Override
    public List<Posicion> calcularPosiblesDestinos(Posicion origen, Tablero tablero) {
        List<Posicion> destinos = new ArrayList<>();
        Pieza piezaOrigen = tablero.obtenerPieza(origen);
        if (piezaOrigen == null) return destinos;

        int[][] direcciones = getDirecciones();
        for (int[] dir : direcciones) {
            int dFila = dir[0];
            int dCol = dir[1];
            Posicion actual = origen.desplazar(dFila, dCol);

            while (actual.esValida()) {
                if (!tablero.estaOcupada(actual)) {
                    destinos.add(actual);
                } else {
                    Pieza piezaEnCasilla = tablero.obtenerPieza(actual);
                    // Si es una pieza contraria, la puede capturar y el rayo se detiene
                    if (piezaEnCasilla.getColor() != piezaOrigen.getColor()) {
                        destinos.add(actual);
                    }
                    // Si es aliada o enemiga, no se puede atravesar
                    break;
                }
                actual = actual.desplazar(dFila, dCol);
            }
        }
        return destinos;
    }
}

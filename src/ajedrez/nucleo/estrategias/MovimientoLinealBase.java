package ajedrez.nucleo.estrategias;

import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.tablero.Tablero;

import java.util.ArrayList;
import java.util.List;

/**
 * Base para piezas de largo alcance (deslizantes): recorre rayos en las direcciones que
 * indique la subclase hasta el borde del tablero o hasta chocar con una pieza.
 *
 * Es el único uso de herencia entre estrategias, y a conciencia: la relación "es-un" es
 * genuina (ortogonal y diagonal SON movimientos lineales), la jerarquía tiene dos niveles
 * y solo varía el dato de las direcciones. Para combinar capacidades se usa composición
 * ({@link MovimientoCompuesto}), no esta base.
 */
public abstract class MovimientoLinealBase implements EstrategiaMovimiento {
    protected abstract int[][] getDirecciones();

    @Override
    public List<Posicion> calcularPosiblesDestinos(Posicion origen, Tablero tablero) {
        List<Posicion> destinos = new ArrayList<>();
        Pieza piezaOrigen = tablero.obtenerPieza(origen);
        if (piezaOrigen == null) return destinos;

        for (int[] dir : getDirecciones()) {
            Posicion actual = origen.desplazar(dir[0], dir[1]);
            while (tablero.esCasillaValida(actual)) {
                Pieza piezaEnCasilla = tablero.obtenerPieza(actual);
                if (piezaEnCasilla == null) {
                    destinos.add(actual);
                } else {
                    // Una pieza rival se puede capturar; ninguna pieza se puede atravesar
                    if (!piezaEnCasilla.esDelMismoBando(piezaOrigen)) {
                        destinos.add(actual);
                    }
                    break;
                }
                actual = actual.desplazar(dir[0], dir[1]);
            }
        }
        return destinos;
    }
}

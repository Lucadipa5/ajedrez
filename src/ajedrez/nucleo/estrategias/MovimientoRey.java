package ajedrez.nucleo.estrategias;

import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.tablero.Tablero;

import java.util.ArrayList;
import java.util.List;

/**
 * Estrategia de movimiento del Rey: una casilla en cualquier dirección adyacente.
 * El enroque es otra estrategia ({@link MovimientoEnroque}) que la fábrica compone con esta.
 */
public class MovimientoRey implements EstrategiaMovimiento {
    private static final int[][] PASOS = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1},           {0, 1},
            {1, -1},  {1, 0},  {1, 1}
    };

    @Override
    public List<Posicion> calcularPosiblesDestinos(Posicion origen, Tablero tablero) {
        List<Posicion> destinos = new ArrayList<>();
        Pieza piezaOrigen = tablero.obtenerPieza(origen);
        if (piezaOrigen == null) return destinos;

        for (int[] paso : PASOS) {
            Posicion destino = origen.desplazar(paso[0], paso[1]);
            if (!tablero.esCasillaValida(destino)) continue;
            Pieza piezaEnDestino = tablero.obtenerPieza(destino);
            if (piezaEnDestino == null || !piezaEnDestino.esDelMismoBando(piezaOrigen)) {
                destinos.add(destino);
            }
        }
        return destinos;
    }
}

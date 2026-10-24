package ajedrez.nucleo.estrategias;

import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.tablero.Tablero;

import java.util.ArrayList;
import java.util.List;

/**
 * Estrategia de movimiento en 'L' para el Caballo.
 * Es la única pieza que salta sobre otras piezas.
 */
public class MovimientoCaballo implements EstrategiaMovimiento {
    private static final int[][] SALTOS = {
            {-2, -1}, {-2, 1},
            {-1, -2}, {-1, 2},
            {1, -2},  {1, 2},
            {2, -1},  {2, 1}
    };

    @Override
    public List<Posicion> calcularPosiblesDestinos(Posicion origen, Tablero tablero) {
        List<Posicion> destinos = new ArrayList<>();
        Pieza piezaOrigen = tablero.obtenerPieza(origen);
        if (piezaOrigen == null) return destinos;

        for (int[] salto : SALTOS) {
            Posicion destino = origen.desplazar(salto[0], salto[1]);
            if (destino.esValida()) {
                if (!tablero.estaOcupada(destino)) {
                    destinos.add(destino);
                } else {
                    Pieza piezaEnDestino = tablero.obtenerPieza(destino);
                    if (piezaEnDestino.getColor() != piezaOrigen.getColor()) {
                        destinos.add(destino);
                    }
                }
            }
        }
        return destinos;
    }
}

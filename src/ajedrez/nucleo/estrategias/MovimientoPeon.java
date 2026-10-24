package ajedrez.nucleo.estrategias;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.tablero.Tablero;

import java.util.ArrayList;
import java.util.List;

/**
 * Estrategia de movimiento para el Peón.
 * Contempla avance frontal simple, avance doble desde posición inicial y capturas diagonales.
 */
public class MovimientoPeon implements EstrategiaMovimiento {

    @Override
    public List<Posicion> calcularPosiblesDestinos(Posicion origen, Tablero tablero) {
        List<Posicion> destinos = new ArrayList<>();
        Pieza peon = tablero.obtenerPieza(origen);
        if (peon == null) return destinos;

        int dFila = (peon.getColor() == Color.BLANCO) ? -1 : 1;
        int filaInicial = (peon.getColor() == Color.BLANCO) ? 6 : 1;

        // 1. Avance simple hacia adelante (no captura hacia adelante)
        Posicion pasoAdelante = origen.desplazar(dFila, 0);
        if (pasoAdelante.esValida() && !tablero.estaOcupada(pasoAdelante)) {
            destinos.add(pasoAdelante);

            // 2. Avance doble si está en su fila inicial y el camino está libre
            if (origen.getFila() == filaInicial) {
                Posicion pasoDoble = origen.desplazar(2 * dFila, 0);
                if (pasoDoble.esValida() && !tablero.estaOcupada(pasoDoble)) {
                    destinos.add(pasoDoble);
                }
            }
        }

        // 3. Capturas diagonales
        int[] dColumnasCaptura = {-1, 1};
        for (int dCol : dColumnasCaptura) {
            Posicion diagonal = origen.desplazar(dFila, dCol);
            if (diagonal.esValida() && tablero.estaOcupada(diagonal)) {
                Pieza enemiga = tablero.obtenerPieza(diagonal);
                if (enemiga.getColor() != peon.getColor()) {
                    destinos.add(diagonal);
                }
            }
        }

        return destinos;
    }
}

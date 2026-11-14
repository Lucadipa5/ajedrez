package ajedrez.nucleo.estrategias;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.tablero.Tablero;

import java.util.ArrayList;
import java.util.List;

/**
 * Estrategia de movimiento del Peón: avance simple, avance doble desde su fila inicial,
 * captura en diagonal y captura al paso (a la casilla que el tablero marca como tal).
 *
 * Las blancas avanzan hacia la fila 0 y las negras hacia la última fila del tablero,
 * cualquiera sea su tamaño.
 */
public class MovimientoPeon implements EstrategiaMovimiento {

    /** -1 para las blancas (suben hacia la fila 0), +1 para las negras. */
    public static int direccionAvance(Color color) {
        return color == Color.BLANCO ? -1 : 1;
    }

    /** Fila desde la que el peón puede avanzar dos casillas. */
    public static int filaInicial(Tablero tablero, Color color) {
        return color == Color.BLANCO ? tablero.getFilas() - 2 : 1;
    }

    /** Si un peón de ese color que llega a esa fila corona. */
    public static boolean esFilaDeCoronacion(Tablero tablero, Color color, int fila) {
        return color == Color.BLANCO ? fila == 0 : fila == tablero.getFilas() - 1;
    }

    @Override
    public List<Posicion> calcularPosiblesDestinos(Posicion origen, Tablero tablero) {
        List<Posicion> destinos = new ArrayList<>();
        Pieza peon = tablero.obtenerPieza(origen);
        if (peon == null) return destinos;

        int dFila = direccionAvance(peon.getColor());

        // 1. Avance simple hacia adelante (no captura hacia adelante)
        Posicion pasoAdelante = origen.desplazar(dFila, 0);
        if (tablero.esCasillaValida(pasoAdelante) && !tablero.estaOcupada(pasoAdelante)) {
            destinos.add(pasoAdelante);

            // 2. Avance doble si está en su fila inicial y el camino está libre
            if (origen.getFila() == filaInicial(tablero, peon.getColor())) {
                Posicion pasoDoble = origen.desplazar(2 * dFila, 0);
                if (tablero.esCasillaValida(pasoDoble) && !tablero.estaOcupada(pasoDoble)) {
                    destinos.add(pasoDoble);
                }
            }
        }

        // 3. Capturas diagonales: pieza rival, o la casilla al paso (donde quedó "la sombra" del peón rival)
        for (int dCol : new int[]{-1, 1}) {
            Posicion diagonal = origen.desplazar(dFila, dCol);
            if (!tablero.esCasillaValida(diagonal)) continue;
            Pieza enemiga = tablero.obtenerPieza(diagonal);
            if (enemiga != null) {
                if (!enemiga.esDelMismoBando(peon)) destinos.add(diagonal);
            } else if (diagonal.equals(tablero.getCasillaAlPaso())) {
                destinos.add(diagonal);
            }
        }

        return destinos;
    }
}

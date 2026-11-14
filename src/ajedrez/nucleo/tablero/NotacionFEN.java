package ajedrez.nucleo.tablero;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.dominio.TipoPieza;

/**
 * Convierte un tablero a notación FEN (Forsyth-Edwards), el formato estándar para describir
 * una posición de ajedrez. Lo usan los motores de IA para recibir la posición.
 *
 * Incluye los seis campos: piezas, turno, derechos de enroque, casilla al paso,
 * reloj de 50 movimientos y número de jugada. FEN solo está definida para el tablero de 8x8.
 */
public final class NotacionFEN {

    private NotacionFEN() {}

    public static String desdeTablero(Tablero tablero, Color turno) {
        return desdeTablero(tablero, turno, 1);
    }

    public static String desdeTablero(Tablero tablero, Color turno, int numeroDeJugada) {
        if (!tablero.esEstandar()) {
            throw new IllegalArgumentException("La notación FEN solo está definida para tableros de 8x8");
        }
        StringBuilder fen = new StringBuilder();
        for (int fila = 0; fila < 8; fila++) {
            int vacias = 0;
            for (int col = 0; col < 8; col++) {
                Pieza pieza = tablero.obtenerPieza(new Posicion(fila, col));
                if (pieza == null) {
                    vacias++;
                    continue;
                }
                if (vacias > 0) {
                    fen.append(vacias);
                    vacias = 0;
                }
                char letra = pieza.getTipo().getSimboloAlgebraico();
                fen.append(pieza.getColor() == Color.BLANCO ? letra : Character.toLowerCase(letra));
            }
            if (vacias > 0) fen.append(vacias);
            if (fila < 7) fen.append('/');
        }
        fen.append(turno == Color.BLANCO ? " w " : " b ");
        fen.append(derechosDeEnroque(tablero));
        fen.append(' ').append(tablero.getCasillaAlPaso() != null ? tablero.getCasillaAlPaso().aNotacionAlgebraica() : "-");
        fen.append(' ').append(tablero.getMediasJugadasSinProgreso());
        fen.append(' ').append(Math.max(1, numeroDeJugada));
        return fen.toString();
    }

    /** "KQkq" según qué reyes y torres siguen sin moverse en sus casillas iniciales; "-" si nadie puede enrocar. */
    private static String derechosDeEnroque(Tablero tablero) {
        StringBuilder sb = new StringBuilder();
        if (puedeEnrocar(tablero, Color.BLANCO, 7, 7)) sb.append('K');
        if (puedeEnrocar(tablero, Color.BLANCO, 7, 0)) sb.append('Q');
        if (puedeEnrocar(tablero, Color.NEGRO, 0, 7)) sb.append('k');
        if (puedeEnrocar(tablero, Color.NEGRO, 0, 0)) sb.append('q');
        return sb.length() == 0 ? "-" : sb.toString();
    }

    private static boolean puedeEnrocar(Tablero tablero, Color color, int fila, int columnaTorre) {
        Pieza rey = tablero.obtenerPieza(new Posicion(fila, 4));
        Pieza torre = tablero.obtenerPieza(new Posicion(fila, columnaTorre));
        return rey != null && rey.getTipo() == TipoPieza.REY && rey.getColor() == color && !rey.seHaMovido()
                && torre != null && torre.getTipo() == TipoPieza.TORRE && torre.getColor() == color && !torre.seHaMovido();
    }
}

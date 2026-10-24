package ajedrez.nucleo.tablero;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;

/**
 * Convierte un tablero a notación FEN (Forsyth-Edwards), el formato estándar
 * para describir una posición de ajedrez. Lo usan los motores de IA para recibir la posición.
 *
 * Como este núcleo no implementa enroque ni captura al paso, esos campos van siempre en "-",
 * así ningún motor propone jugadas que las reglas del núcleo no aceptan.
 */
public final class NotacionFEN {

    private NotacionFEN() {}

    public static String desdeTablero(Tablero tablero, Color turno) {
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
        fen.append(turno == Color.BLANCO ? " w" : " b");
        fen.append(" - - 0 1");
        return fen.toString();
    }
}

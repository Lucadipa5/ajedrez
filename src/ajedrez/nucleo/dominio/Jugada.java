package ajedrez.nucleo.dominio;

import java.util.Objects;

/**
 * Jugada: origen, destino y (opcionalmente) la pieza a la que corona un peón.
 * Se puede convertir desde/hacia notación UCI (por ejemplo "e2e4" o "e7e8q"),
 * que es el formato estándar que usan los motores de ajedrez.
 */
public final class Jugada {
    private final Posicion origen;
    private final Posicion destino;
    private final TipoPieza promocion; // null si no hay coronación

    public Jugada(Posicion origen, Posicion destino, TipoPieza promocion) {
        this.origen = Objects.requireNonNull(origen);
        this.destino = Objects.requireNonNull(destino);
        this.promocion = promocion;
    }

    public Posicion getOrigen() { return origen; }
    public Posicion getDestino() { return destino; }
    public TipoPieza getPromocion() { return promocion; }

    /**
     * Interpreta una jugada en notación UCI. Devuelve null si el texto no es válido.
     */
    public static Jugada desdeUCI(String uci) {
        if (uci == null) return null;
        uci = uci.trim().toLowerCase();
        if (uci.length() != 4 && uci.length() != 5) return null;
        Posicion origen = casilla(uci.charAt(0), uci.charAt(1));
        Posicion destino = casilla(uci.charAt(2), uci.charAt(3));
        if (origen == null || destino == null) return null;
        TipoPieza promocion = null;
        if (uci.length() == 5) {
            promocion = switch (uci.charAt(4)) {
                case 'q' -> TipoPieza.REINA;
                case 'r' -> TipoPieza.TORRE;
                case 'b' -> TipoPieza.ALFIL;
                case 'n' -> TipoPieza.CABALLO;
                default -> null;
            };
            if (promocion == null) return null;
        }
        return new Jugada(origen, destino, promocion);
    }

    // Fila 0 del tablero = fila 8 del ajedrez (las blancas empiezan abajo)
    private static Posicion casilla(char columna, char fila) {
        if (columna < 'a' || columna > 'h' || fila < '1' || fila > '8') return null;
        return new Posicion(8 - (fila - '0'), columna - 'a');
    }

    @Override
    public String toString() {
        return origen.aNotacionAlgebraica() + destino.aNotacionAlgebraica()
                + (promocion != null ? String.valueOf(Character.toLowerCase(promocion.getSimboloAlgebraico())) : "");
    }
}

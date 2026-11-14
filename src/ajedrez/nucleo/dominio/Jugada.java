package ajedrez.nucleo.dominio;

import java.util.Objects;

/**
 * Jugada: origen, destino y (opcionalmente) la pieza a la que corona un peón.
 * Se puede convertir desde/hacia notación UCI (por ejemplo "e2e4" o "e7e8q"),
 * que es el formato estándar que usan los motores de ajedrez.
 *
 * Las jugadas especiales (enroque, captura al paso) no necesitan representación propia:
 * se reconocen por el origen y el destino, igual que en UCI.
 */
public final class Jugada {
    private final Posicion origen;
    private final Posicion destino;
    private final TipoPieza promocion; // null si no hay coronación

    public Jugada(Posicion origen, Posicion destino) {
        this(origen, destino, null);
    }

    public Jugada(Posicion origen, Posicion destino, TipoPieza promocion) {
        this.origen = Objects.requireNonNull(origen);
        this.destino = Objects.requireNonNull(destino);
        this.promocion = promocion;
    }

    public Posicion getOrigen() { return origen; }
    public Posicion getDestino() { return destino; }
    public TipoPieza getPromocion() { return promocion; }

    public Jugada conPromocion(TipoPieza tipo) {
        return new Jugada(origen, destino, tipo);
    }

    public int deltaFila() { return destino.getFila() - origen.getFila(); }
    public int deltaColumna() { return destino.getColumna() - origen.getColumna(); }

    /** Misma casilla de origen y destino (la promoción no cuenta). */
    public boolean mismoRecorrido(Jugada otra) {
        return otra != null && origen.equals(otra.origen) && destino.equals(otra.destino);
    }

    /**
     * Interpreta una jugada en notación UCI (tablero estándar). Devuelve null si el texto no es válido.
     */
    public static Jugada desdeUCI(String uci) {
        if (uci == null) return null;
        uci = uci.trim().toLowerCase();
        if (uci.length() != 4 && uci.length() != 5) return null;
        Posicion origen = Posicion.desdeAlgebraica(uci.substring(0, 2));
        Posicion destino = Posicion.desdeAlgebraica(uci.substring(2, 4));
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

    /** Notación UCI (tablero estándar): "e2e4", "e7e8q". */
    public String aUCI() {
        return origen.aNotacionAlgebraica() + destino.aNotacionAlgebraica()
                + (promocion != null ? String.valueOf(Character.toLowerCase(promocion.getSimboloAlgebraico())) : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Jugada otra)) return false;
        return origen.equals(otra.origen) && destino.equals(otra.destino) && promocion == otra.promocion;
    }

    @Override
    public int hashCode() {
        return Objects.hash(origen, destino, promocion);
    }

    @Override
    public String toString() {
        return aUCI();
    }
}

package ajedrez.adaptadores.ui.swing;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.TipoPieza;
import ajedrez.nucleo.estados.EstadoJaqueMate;
import ajedrez.nucleo.estados.EstadoPartida;
import ajedrez.nucleo.estados.EstadoTablas;

import java.util.EnumMap;
import java.util.Map;

/**
 * Cómo se le cuenta al usuario lo que pasa en el núcleo: textos de los estados y símbolos
 * de las piezas. Es responsabilidad del adaptador de interfaz, no de las reglas del juego.
 */
final class TextosPartida {

    private static final Map<TipoPieza, String[]> SIMBOLOS = new EnumMap<>(TipoPieza.class);

    static {
        SIMBOLOS.put(TipoPieza.PEON, new String[]{"♙", "♟"});
        SIMBOLOS.put(TipoPieza.TORRE, new String[]{"♖", "♜"});
        SIMBOLOS.put(TipoPieza.CABALLO, new String[]{"♘", "♞"});
        SIMBOLOS.put(TipoPieza.ALFIL, new String[]{"♗", "♝"});
        SIMBOLOS.put(TipoPieza.REINA, new String[]{"♕", "♛"});
        SIMBOLOS.put(TipoPieza.REY, new String[]{"♔", "♚"});
    }

    private TextosPartida() {}

    static String nombre(Color color) {
        return color == Color.BLANCO ? "Blancas" : "Negras";
    }

    static String describir(EstadoPartida estado) {
        if (estado instanceof EstadoJaqueMate mate) {
            return "¡JAQUE MATE! Ganaron las " + nombre(mate.getGanador());
        }
        if (estado instanceof EstadoTablas tablas) {
            return "¡TABLAS! " + motivo(tablas);
        }
        return "Turno de las " + nombre(estado.getColorTurnoActual());
    }

    static String motivo(EstadoTablas tablas) {
        return switch (tablas.getMotivo()) {
            case AHOGADO -> "Ahogado: las " + nombre(tablas.getColorEnTurno()) + " no tienen jugadas legales";
            case MATERIAL_INSUFICIENTE -> "Material insuficiente para dar mate";
            case CINCUENTA_MOVIMIENTOS -> "Regla de los 50 movimientos";
            case REPETICION_TRIPLE -> "Triple repetición de la posición";
        };
    }

    /** Símbolo Unicode de la pieza, para cuando falta su imagen. */
    static String simbolo(Pieza pieza) {
        String[] par = SIMBOLOS.get(pieza.getTipo());
        if (par == null) return String.valueOf(pieza.getTipo().getSimboloAlgebraico());
        return pieza.getColor() == Color.BLANCO ? par[0] : par[1];
    }
}

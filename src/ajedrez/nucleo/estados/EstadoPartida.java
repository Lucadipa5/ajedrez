package ajedrez.nucleo.estados;

import ajedrez.nucleo.dominio.Color;

/**
 * Patrón State: las fases de la partida (turno de cada bando, jaque mate, tablas).
 * Cada estado sabe a quién le toca jugar y cómo se transiciona al mover o al deshacer,
 * en vez de un switch sobre una variable de estado.
 *
 * El estado no sabe describirse en texto: eso lo decide cada interfaz de usuario.
 */
public interface EstadoPartida {
    /** Color al que le toca mover, o null si la partida terminó. */
    Color getColorTurnoActual();

    EstadoPartida siguienteTurno();

    /** Estado al que se vuelve al deshacer la última jugada. */
    EstadoPartida turnoAnterior();

    boolean esFinalizada();

    static EstadoPartida turnoDe(Color color) {
        return color == Color.BLANCO ? new EstadoTurnoBlanco() : new EstadoTurnoNegro();
    }
}

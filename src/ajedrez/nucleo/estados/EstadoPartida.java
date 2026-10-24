package ajedrez.nucleo.estados;

import ajedrez.nucleo.dominio.Color;

/**
 * Interfaz State (Patrón de Comportamiento).
 * Modela las fases y el turno activo de la partida de ajedrez.
 *
 * Basado directamente en el ejemplo canónico enseñado en clase:
 * En lugar de flags o if anidados, cada estado encapsula a quién le corresponde jugar
 * y cómo transiciona la partida tras un movimiento válido.
 */
public interface EstadoPartida {
    Color getColorTurnoActual();
    EstadoPartida siguienteTurno();
    EstadoPartida turnoAnterior();
    String getDescripcion();
    boolean esFinalizada();
}

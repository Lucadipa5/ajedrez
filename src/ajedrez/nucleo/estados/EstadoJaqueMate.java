package ajedrez.nucleo.estados;

import ajedrez.nucleo.dominio.Color;

/**
 * Estado final de la partida cuando se produce Jaque Mate.
 */
public class EstadoJaqueMate implements EstadoPartida {
    private final Color ganador;

    public EstadoJaqueMate(Color ganador) {
        this.ganador = ganador;
    }

    @Override
    public Color getColorTurnoActual() {
        return null;
    }

    @Override
    public EstadoPartida siguienteTurno() {
        return this; // Estado terminal
    }

    @Override
    public EstadoPartida turnoAnterior() {
        // Al deshacer, vuelve al turno del bando perdedor que había recibido mate
        return ganador == Color.BLANCO ? new EstadoTurnoNegro() : new EstadoTurnoBlanco();
    }

    @Override
    public String getDescripcion() {
        return "¡JAQUE MATE! Ganaron las " + (ganador == Color.BLANCO ? "Blancas" : "Negras");
    }

    @Override
    public boolean esFinalizada() {
        return true;
    }

    public Color getGanador() {
        return ganador;
    }
}

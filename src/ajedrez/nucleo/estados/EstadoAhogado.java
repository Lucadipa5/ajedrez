package ajedrez.nucleo.estados;

import ajedrez.nucleo.dominio.Color;

/**
 * Estado final de la partida cuando se produce Ahogado (Stalemate).
 * Ocurre cuando el jugador en turno NO está en jaque pero tampoco tiene
 * ningún movimiento legal disponible. El resultado es tablas (empate).
 */
public class EstadoAhogado implements EstadoPartida {
    private final Color colorAhogado;

    public EstadoAhogado(Color colorAhogado) {
        this.colorAhogado = colorAhogado;
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
        // Al deshacer, vuelve al turno del color que quedó ahogado
        return colorAhogado == Color.BLANCO ? new EstadoTurnoBlanco() : new EstadoTurnoNegro();
    }

    @Override
    public String getDescripcion() {
        return "¡AHOGADO! Tablas — " + colorAhogado + " no tiene movimientos legales";
    }

    @Override
    public boolean esFinalizada() {
        return true;
    }

    public Color getColorAhogado() {
        return colorAhogado;
    }
}

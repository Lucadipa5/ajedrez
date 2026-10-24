package ajedrez.nucleo.estados;

import ajedrez.nucleo.dominio.Color;

/**
 * Estado correspondiente al turno de las piezas Negras.
 */
public class EstadoTurnoNegro implements EstadoPartida {

    @Override
    public Color getColorTurnoActual() {
        return Color.NEGRO;
    }

    @Override
    public EstadoPartida siguienteTurno() {
        return new EstadoTurnoBlanco();
    }

    @Override
    public EstadoPartida turnoAnterior() {
        return new EstadoTurnoBlanco();
    }

    @Override
    public String getDescripcion() {
        return "Turno de las Negras";
    }

    @Override
    public boolean esFinalizada() {
        return false;
    }
}

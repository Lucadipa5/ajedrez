package ajedrez.nucleo.estados;

import ajedrez.nucleo.dominio.Color;

/**
 * Estado correspondiente al turno de las piezas Blancas.
 */
public class EstadoTurnoBlanco implements EstadoPartida {

    @Override
    public Color getColorTurnoActual() {
        return Color.BLANCO;
    }

    @Override
    public EstadoPartida siguienteTurno() {
        return new EstadoTurnoNegro();
    }

    @Override
    public EstadoPartida turnoAnterior() {
        return new EstadoTurnoNegro();
    }

    @Override
    public String getDescripcion() {
        return "Turno de las Blancas";
    }

    @Override
    public boolean esFinalizada() {
        return false;
    }
}

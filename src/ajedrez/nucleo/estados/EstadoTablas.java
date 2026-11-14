package ajedrez.nucleo.estados;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.reglas.MotivoTablas;

/**
 * Estado final de la partida en tablas (empate), cualquiera sea el motivo:
 * ahogado, material insuficiente, regla de los 50 movimientos o repetición triple.
 */
public class EstadoTablas implements EstadoPartida {
    private final MotivoTablas motivo;
    private final Color colorEnTurno; // a quién le tocaba mover cuando se declararon las tablas

    public EstadoTablas(MotivoTablas motivo, Color colorEnTurno) {
        this.motivo = motivo;
        this.colorEnTurno = colorEnTurno;
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
        // Al deshacer vuelve a mover quien hizo la jugada que produjo las tablas
        return EstadoPartida.turnoDe(colorEnTurno.opuesto());
    }

    @Override
    public boolean esFinalizada() {
        return true;
    }

    public MotivoTablas getMotivo() {
        return motivo;
    }

    /** Color al que le tocaba mover (en el ahogado, el bando ahogado). */
    public Color getColorEnTurno() {
        return colorEnTurno;
    }
}

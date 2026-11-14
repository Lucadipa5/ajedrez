package ajedrez.nucleo.reglas;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.tablero.Tablero;

/**
 * Tablas si la misma posición (mismas piezas, mismo turno, mismos derechos de enroque
 * y misma casilla al paso) se repite tres veces en la partida.
 */
public class ReglaRepeticionTriple implements ReglaDeTablas {
    private final int repeticionesNecesarias;

    public ReglaRepeticionTriple() {
        this(3);
    }

    public ReglaRepeticionTriple(int repeticionesNecesarias) {
        this.repeticionesNecesarias = repeticionesNecesarias;
    }

    @Override
    public MotivoTablas evaluar(Tablero tablero, Color colorEnTurno, HistorialPosiciones historial) {
        String clave = tablero.claveDePosicion(colorEnTurno);
        return historial.repeticionesDe(clave) >= repeticionesNecesarias ? MotivoTablas.REPETICION_TRIPLE : null;
    }
}

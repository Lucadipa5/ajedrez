package ajedrez.nucleo.reglas;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.tablero.Tablero;

/**
 * Tablas si pasaron 50 movimientos de cada bando (100 medias jugadas) sin capturas ni
 * movimientos de peón. El límite se recibe por constructor para poder tener variantes
 * (la regla de los 75 movimientos) y tests cortos.
 */
public class ReglaCincuentaMovimientos implements ReglaDeTablas {
    private final int limiteDeMediasJugadas;

    public ReglaCincuentaMovimientos() {
        this(100);
    }

    public ReglaCincuentaMovimientos(int limiteDeMediasJugadas) {
        this.limiteDeMediasJugadas = limiteDeMediasJugadas;
    }

    @Override
    public MotivoTablas evaluar(Tablero tablero, Color colorEnTurno, HistorialPosiciones historial) {
        return tablero.getMediasJugadasSinProgreso() >= limiteDeMediasJugadas ? MotivoTablas.CINCUENTA_MOVIMIENTOS : null;
    }
}

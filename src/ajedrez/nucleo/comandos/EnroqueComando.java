package ajedrez.nucleo.comandos;

import ajedrez.nucleo.dominio.Jugada;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.tablero.Tablero;

/**
 * Enroque: el rey avanza dos casillas hacia la torre y la torre salta a la casilla que el rey cruzó.
 * Deshacer devuelve ambas piezas y su estado "sin mover", así el enroque vuelve a estar disponible.
 */
public class EnroqueComando implements ComandoDeJugada {
    private final Tablero tablero;
    private final Jugada jugada;
    private final Posicion origenTorre;
    private final Posicion destinoTorre;

    private Pieza rey;
    private Pieza torre;
    private boolean reyMovidoAntes;
    private boolean torreMovidaAntes;

    public EnroqueComando(Tablero tablero, Jugada jugada) {
        this.tablero = tablero;
        this.jugada = jugada;
        int paso = Integer.signum(jugada.deltaColumna());
        int fila = jugada.getOrigen().getFila();
        this.origenTorre = new Posicion(fila, paso > 0 ? tablero.getColumnas() - 1 : 0);
        this.destinoTorre = jugada.getOrigen().desplazar(0, paso);
    }

    @Override
    public void ejecutar() {
        rey = tablero.obtenerPieza(jugada.getOrigen());
        torre = tablero.obtenerPieza(origenTorre);
        reyMovidoAntes = rey.seHaMovido();
        torreMovidaAntes = torre.seHaMovido();
        tablero.moverPieza(jugada.getOrigen(), jugada.getDestino());
        tablero.moverPieza(origenTorre, destinoTorre);
    }

    @Override
    public void deshacer() {
        tablero.moverPieza(jugada.getDestino(), jugada.getOrigen());
        tablero.moverPieza(destinoTorre, origenTorre);
        rey.setSeHaMovido(reyMovidoAntes);
        torre.setSeHaMovido(torreMovidaAntes);
    }

    @Override
    public Jugada getJugada() {
        return jugada;
    }

    @Override
    public Pieza getPiezaMovida() {
        return rey;
    }

    @Override
    public Pieza getPiezaCapturada() {
        return null;
    }

    @Override
    public boolean esEnroque() {
        return true;
    }
}

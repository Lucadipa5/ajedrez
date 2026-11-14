package ajedrez.nucleo.comandos;

import ajedrez.nucleo.dominio.Jugada;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.tablero.Tablero;

/**
 * Captura al paso: el peón se mueve en diagonal a una casilla vacía y captura al peón rival
 * que acaba de pasar a su lado (que está en la misma fila del origen, en la columna del destino).
 */
public class CapturaAlPasoComando implements ComandoDeJugada {
    private final Tablero tablero;
    private final Jugada jugada;
    private final Posicion casillaDelCapturado;

    private Pieza peon;
    private Pieza capturado;
    private boolean peonMovidoAntes;

    public CapturaAlPasoComando(Tablero tablero, Jugada jugada) {
        this.tablero = tablero;
        this.jugada = jugada;
        this.casillaDelCapturado = new Posicion(jugada.getOrigen().getFila(), jugada.getDestino().getColumna());
    }

    @Override
    public void ejecutar() {
        peon = tablero.obtenerPieza(jugada.getOrigen());
        peonMovidoAntes = peon.seHaMovido();
        capturado = tablero.removerPieza(casillaDelCapturado);
        tablero.moverPieza(jugada.getOrigen(), jugada.getDestino());
    }

    @Override
    public void deshacer() {
        tablero.moverPieza(jugada.getDestino(), jugada.getOrigen());
        peon.setSeHaMovido(peonMovidoAntes);
        if (capturado != null) {
            tablero.colocarPieza(capturado, casillaDelCapturado);
        }
    }

    @Override
    public Jugada getJugada() {
        return jugada;
    }

    @Override
    public Pieza getPiezaMovida() {
        return peon;
    }

    @Override
    public Pieza getPiezaCapturada() {
        return capturado;
    }

    @Override
    public boolean esCapturaAlPaso() {
        return true;
    }
}

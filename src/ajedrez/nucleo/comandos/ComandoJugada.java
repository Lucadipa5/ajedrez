package ajedrez.nucleo.comandos;

import ajedrez.nucleo.dominio.Jugada;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.dominio.TipoPieza;
import ajedrez.nucleo.tablero.Tablero;

/**
 * Decorador (composición) que envuelve al comando mecánico de una jugada y se ocupa del
 * estado de la posición que no es una pieza: la casilla de captura al paso y el reloj
 * de la regla de los 50 movimientos. Así ninguno de los comandos concretos tiene que
 * acordarse de esa contabilidad, y deshacer la restaura exactamente.
 */
public final class ComandoJugada implements ComandoDeJugada {
    private final Tablero tablero;
    private final ComandoDeJugada interno;

    private Posicion casillaAlPasoAnterior;
    private int mediasJugadasAnteriores;

    public ComandoJugada(Tablero tablero, ComandoDeJugada interno) {
        this.tablero = tablero;
        this.interno = interno;
    }

    @Override
    public void ejecutar() {
        casillaAlPasoAnterior = tablero.getCasillaAlPaso();
        mediasJugadasAnteriores = tablero.getMediasJugadasSinProgreso();

        interno.ejecutar();

        Jugada jugada = interno.getJugada();
        Pieza movida = interno.getPiezaMovida();
        boolean esPeon = movida != null && movida.getTipo() == TipoPieza.PEON;

        // Un avance doble de peón deja la casilla que saltó disponible para captura al paso, solo por una jugada
        Posicion nuevaAlPaso = null;
        if (esPeon && Math.abs(jugada.deltaFila()) == 2) {
            nuevaAlPaso = jugada.getOrigen().desplazar(jugada.deltaFila() / 2, 0);
        }
        tablero.setCasillaAlPaso(nuevaAlPaso);

        // Mover un peón o capturar reinicia el reloj de los 50 movimientos
        boolean huboProgreso = esPeon || interno.getPiezaCapturada() != null;
        tablero.setMediasJugadasSinProgreso(huboProgreso ? 0 : mediasJugadasAnteriores + 1);
    }

    @Override
    public void deshacer() {
        interno.deshacer();
        tablero.setCasillaAlPaso(casillaAlPasoAnterior);
        tablero.setMediasJugadasSinProgreso(mediasJugadasAnteriores);
    }

    @Override public Jugada getJugada() { return interno.getJugada(); }
    @Override public Pieza getPiezaMovida() { return interno.getPiezaMovida(); }
    @Override public Pieza getPiezaCapturada() { return interno.getPiezaCapturada(); }
    @Override public boolean esEnroque() { return interno.esEnroque(); }
    @Override public boolean esCapturaAlPaso() { return interno.esCapturaAlPaso(); }
    @Override public TipoPieza getPromocion() { return interno.getPromocion(); }
}

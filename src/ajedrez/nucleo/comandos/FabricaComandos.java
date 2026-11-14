package ajedrez.nucleo.comandos;

import ajedrez.nucleo.dominio.Jugada;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.TipoPieza;
import ajedrez.nucleo.fabrica.FabricaPiezas;
import ajedrez.nucleo.tablero.Tablero;

/**
 * Patrón Factory: decide qué comando corresponde a una jugada (movimiento común, enroque
 * o captura al paso) mirando el tablero. Construir el comando implica una decisión:
 * ese es el caso en que una fábrica aporta algo, en vez de un simple {@code new}.
 */
public class FabricaComandos {
    private final FabricaPiezas fabricaPiezas;

    public FabricaComandos(FabricaPiezas fabricaPiezas) {
        this.fabricaPiezas = fabricaPiezas;
    }

    public ComandoDeJugada crear(Tablero tablero, Jugada jugada) {
        ComandoDeJugada interno;
        if (esEnroque(tablero, jugada)) {
            interno = new EnroqueComando(tablero, jugada);
        } else if (esCapturaAlPaso(tablero, jugada)) {
            interno = new CapturaAlPasoComando(tablero, jugada);
        } else {
            interno = new MoverPiezaComando(tablero, jugada.getOrigen(), jugada.getDestino(),
                    jugada.getPromocion(), fabricaPiezas);
        }
        return new ComandoJugada(tablero, interno);
    }

    /** El rey se desplaza dos columnas en su fila. */
    public static boolean esEnroque(Tablero tablero, Jugada jugada) {
        Pieza pieza = tablero.obtenerPieza(jugada.getOrigen());
        return pieza != null && pieza.getTipo() == TipoPieza.REY
                && jugada.deltaFila() == 0 && Math.abs(jugada.deltaColumna()) == 2;
    }

    /** Un peón se mueve en diagonal a una casilla vacía. */
    public static boolean esCapturaAlPaso(Tablero tablero, Jugada jugada) {
        Pieza pieza = tablero.obtenerPieza(jugada.getOrigen());
        return pieza != null && pieza.getTipo() == TipoPieza.PEON
                && jugada.deltaColumna() != 0 && !tablero.estaOcupada(jugada.getDestino());
    }
}

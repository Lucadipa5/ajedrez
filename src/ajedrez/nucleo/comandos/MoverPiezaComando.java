package ajedrez.nucleo.comandos;

import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.dominio.TipoPieza;
import ajedrez.nucleo.fabrica.FabricaPiezas;
import ajedrez.nucleo.tablero.Tablero;

/**
 * Comando concreto que encapsula el movimiento de una pieza entre dos casillas,
 * la captura potencial de una pieza contraria y la coronación de peón.
 *
 * Implementa el patrón Command permitiendo la reversibilidad exacta del estado del tablero,
 * garantizando que al deshacer una coronación, la pieza vuelva a ser el Peón original.
 */
public class MoverPiezaComando implements Comando {
    private final Tablero tablero;
    private final Posicion origen;
    private final Posicion destino;
    private final TipoPieza tipoPromocion;
    private final FabricaPiezas fabrica;

    private Pieza piezaMovidaOriginal;
    private Pieza piezaCapturada;
    private boolean teniaMovimientoPrevio;
    private boolean fuePromocionada;

    public MoverPiezaComando(Tablero tablero, Posicion origen, Posicion destino) {
        this(tablero, origen, destino, null, null);
    }

    public MoverPiezaComando(Tablero tablero, Posicion origen, Posicion destino, TipoPieza tipoPromocion, FabricaPiezas fabrica) {
        this.tablero = tablero;
        this.origen = origen;
        this.destino = destino;
        this.tipoPromocion = tipoPromocion;
        this.fabrica = fabrica;
    }

    @Override
    public void ejecutar() {
        this.piezaMovidaOriginal = tablero.obtenerPieza(origen);
        if (piezaMovidaOriginal != null) {
            this.teniaMovimientoPrevio = piezaMovidaOriginal.seHaMovido();
        }

        // 1. Mover físicamente y capturar si había pieza enemiga en destino
        this.piezaCapturada = tablero.moverPieza(origen, destino);

        // 2. Si es una coronación, reemplazar la pieza en destino por la pieza promocionada
        if (tipoPromocion != null && fabrica != null && piezaMovidaOriginal != null && piezaMovidaOriginal.getTipo() == TipoPieza.PEON) {
            Pieza piezaPromocionada = fabrica.crearPieza(tipoPromocion, piezaMovidaOriginal.getColor());
            piezaPromocionada.setSeHaMovido(true);
            tablero.colocarPieza(piezaPromocionada, destino);
            this.fuePromocionada = true;
        }
    }

    @Override
    public void deshacer() {
        if (fuePromocionada) {
            // Si fue coronada, quitamos la reina/torre/alfil de destino
            tablero.removerPieza(destino);
            // Y devolvemos el PEÓN ORIGINAL a la casilla origen
            tablero.colocarPieza(piezaMovidaOriginal, origen);
        } else {
            // Movimiento estándar: regresar la pieza de destino a origen
            tablero.moverPieza(destino, origen);
        }

        // Restaurar estado de si ya se había movido
        if (piezaMovidaOriginal != null) {
            piezaMovidaOriginal.setSeHaMovido(teniaMovimientoPrevio);
        }

        // Restaurar la pieza enemiga capturada en la casilla destino (si la hubo)
        if (piezaCapturada != null) {
            tablero.colocarPieza(piezaCapturada, destino);
        }
    }

    public Posicion getOrigen() {
        return origen;
    }

    public Posicion getDestino() {
        return destino;
    }

    public Pieza getPiezaMovida() {
        return piezaMovidaOriginal;
    }

    public Pieza getPiezaCapturada() {
        return piezaCapturada;
    }

    public boolean fuePromocionada() {
        return fuePromocionada;
    }
}

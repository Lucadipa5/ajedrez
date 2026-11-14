package ajedrez.nucleo.servicio;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.TipoPieza;

/**
 * Puerto de salida (driven port) para la coronación de peón: el núcleo necesita que
 * "alguien" elija la pieza, sin saber si es una ventana, una consola o un test.
 */
@FunctionalInterface
public interface SelectorPromocion {
    /** Elección por defecto cuando ningún adaptador decide (tests, IA): siempre Reina. */
    SelectorPromocion SIEMPRE_REINA = color -> TipoPieza.REINA;

    /**
     * Solicita al jugador en turno seleccionar a qué pieza promocionar su peón.
     * @param color Color del jugador que corona.
     * @return El tipo de pieza elegido (PEON y REY no son válidos para coronar).
     */
    TipoPieza solicitarPromocion(Color color);
}

package ajedrez.nucleo.servicio;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.TipoPieza;

/**
 * Puerto de Entrada (Driving Port) para la regla de Coronación de Peón.
 * Permite que el Núcleo delegue a la interfaz de usuario la decisión de qué pieza
 * desea el jugador (Reina, Torre, Alfil o Caballo), sin acoplarse a Swing ni a ventanas.
 */
public interface SelectorPromocion {
    /**
     * Solicita al jugador en turno seleccionar a qué pieza promocionar su peón.
     * @param color Color del jugador que corona.
     * @return El tipo de pieza elegido (PEON y REY no son válidos para coronar).
     */
    TipoPieza solicitarPromocion(Color color);
}

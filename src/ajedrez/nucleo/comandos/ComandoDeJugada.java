package ajedrez.nucleo.comandos;

import ajedrez.nucleo.dominio.Jugada;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.TipoPieza;

/**
 * Comando que aplica una jugada sobre el tablero y, después de ejecutarse, sabe contar
 * qué pasó (qué pieza movió, cuál capturó). Cada tipo de jugada es una clase:
 * mover/capturar/coronar, enrocar, capturar al paso. Agregar una jugada especial nueva
 * es agregar una implementación, no tocar las existentes.
 *
 * Los métodos con default existen para que cada comando declare solo lo que le aplica
 * (segregación de interfaces): el enroque no tiene promoción, el movimiento común no es enroque.
 */
public interface ComandoDeJugada extends Comando {
    Jugada getJugada();

    /** Pieza que se movió (válido después de ejecutar). */
    Pieza getPiezaMovida();

    /** Pieza capturada, o null si no hubo captura (válido después de ejecutar). */
    Pieza getPiezaCapturada();

    default boolean esEnroque() { return false; }

    default boolean esCapturaAlPaso() { return false; }

    /** Tipo al que coronó el peón, o null. */
    default TipoPieza getPromocion() { return null; }
}

package ajedrez.nucleo.fabrica;

import ajedrez.nucleo.tablero.Tablero;

/**
 * Cómo se colocan las piezas al empezar una partida (patrón Strategy).
 *
 * Separarlo de la partida permite tener la disposición clásica, variantes (Chess960,
 * tableros de otro tamaño) o posiciones de prueba para los tests, sin tocar las reglas.
 */
@FunctionalInterface
public interface DisposicionInicial {
    /** Coloca las piezas sobre un tablero ya vacío. */
    void colocarPiezas(Tablero tablero, FabricaPiezas fabrica);
}

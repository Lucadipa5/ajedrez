package ajedrez.nucleo.fabrica;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.dominio.TipoPieza;
import ajedrez.nucleo.tablero.Tablero;

/**
 * Disposición oficial de las 32 piezas: blancas abajo (filas altas del arreglo), negras arriba.
 * Requiere un tablero de 8 columnas; las filas pueden ser más (los peones quedan igual
 * en la segunda fila de cada bando).
 */
public class DisposicionEstandar implements DisposicionInicial {

    private static final TipoPieza[] PRIMERA_FILA = {
            TipoPieza.TORRE, TipoPieza.CABALLO, TipoPieza.ALFIL, TipoPieza.REINA,
            TipoPieza.REY, TipoPieza.ALFIL, TipoPieza.CABALLO, TipoPieza.TORRE
    };

    @Override
    public void colocarPiezas(Tablero tablero, FabricaPiezas fabrica) {
        if (tablero.getColumnas() != PRIMERA_FILA.length || tablero.getFilas() < 4) {
            throw new IllegalArgumentException("La disposición estándar necesita 8 columnas y al menos 4 filas");
        }
        int ultimaFila = tablero.getFilas() - 1;
        for (int c = 0; c < PRIMERA_FILA.length; c++) {
            tablero.colocarPieza(fabrica.crearPieza(PRIMERA_FILA[c], Color.NEGRO), new Posicion(0, c));
            tablero.colocarPieza(fabrica.crearPieza(TipoPieza.PEON, Color.NEGRO), new Posicion(1, c));
            tablero.colocarPieza(fabrica.crearPieza(TipoPieza.PEON, Color.BLANCO), new Posicion(ultimaFila - 1, c));
            tablero.colocarPieza(fabrica.crearPieza(PRIMERA_FILA[c], Color.BLANCO), new Posicion(ultimaFila, c));
        }
    }
}

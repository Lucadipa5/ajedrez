package ajedrez.nucleo.fabrica;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.dominio.TipoPieza;
import ajedrez.nucleo.estrategias.*;
import ajedrez.nucleo.tablero.Tablero;

/**
 * Patrón Factory (Creacional): Centraliza la instanciación de piezas y el ensamblado
 * de sus estrategias de movimiento correspondientes.
 *
 * Muestra el principio Open/Closed y Composición:
 * La Reina no requiere una clase 'Reina' acoplada con código duplicado; simplemente se
 * compone de las estrategias ortogonal y diagonal existentes.
 */
public class FabricaPiezas {

    public Pieza crearPeon(Color color) {
        return new Pieza(TipoPieza.PEON, color, new MovimientoPeon());
    }

    public Pieza crearTorre(Color color) {
        return new Pieza(TipoPieza.TORRE, color, new MovimientoOrtogonal());
    }

    public Pieza crearCaballo(Color color) {
        return new Pieza(TipoPieza.CABALLO, color, new MovimientoCaballo());
    }

    public Pieza crearAlfil(Color color) {
        return new Pieza(TipoPieza.ALFIL, color, new MovimientoDiagonal());
    }

    public Pieza crearReina(Color color) {
        // Demostración explícita de Composición sobre Herencia
        return new Pieza(
                TipoPieza.REINA,
                color,
                new MovimientoCompuesto(new MovimientoOrtogonal(), new MovimientoDiagonal())
        );
    }

    public Pieza crearRey(Color color) {
        return new Pieza(TipoPieza.REY, color, new MovimientoRey());
    }

    public Pieza crearPieza(TipoPieza tipo, Color color) {
        switch (tipo) {
            case TORRE: return crearTorre(color);
            case CABALLO: return crearCaballo(color);
            case ALFIL: return crearAlfil(color);
            case REINA: return crearReina(color);
            case REY: return crearRey(color);
            case PEON:
            default:
                return crearPeon(color);
        }
    }

    /**
     * Llena el tablero con la disposición oficial de 32 piezas de una partida de ajedrez.
     */
    public void poblarTableroInicial(Tablero tablero) {
        tablero.limpiar();

        // Piezas Mayores Negras (Fila 0)
        tablero.colocarPieza(crearTorre(Color.NEGRO), new Posicion(0, 0));
        tablero.colocarPieza(crearCaballo(Color.NEGRO), new Posicion(0, 1));
        tablero.colocarPieza(crearAlfil(Color.NEGRO), new Posicion(0, 2));
        tablero.colocarPieza(crearReina(Color.NEGRO), new Posicion(0, 3));
        tablero.colocarPieza(crearRey(Color.NEGRO), new Posicion(0, 4));
        tablero.colocarPieza(crearAlfil(Color.NEGRO), new Posicion(0, 5));
        tablero.colocarPieza(crearCaballo(Color.NEGRO), new Posicion(0, 6));
        tablero.colocarPieza(crearTorre(Color.NEGRO), new Posicion(0, 7));

        // Peones Negros (Fila 1)
        for (int c = 0; c < 8; c++) {
            tablero.colocarPieza(crearPeon(Color.NEGRO), new Posicion(1, c));
        }

        // Peones Blancos (Fila 6)
        for (int c = 0; c < 8; c++) {
            tablero.colocarPieza(crearPeon(Color.BLANCO), new Posicion(6, c));
        }

        // Piezas Mayores Blancas (Fila 7)
        tablero.colocarPieza(crearTorre(Color.BLANCO), new Posicion(7, 0));
        tablero.colocarPieza(crearCaballo(Color.BLANCO), new Posicion(7, 1));
        tablero.colocarPieza(crearAlfil(Color.BLANCO), new Posicion(7, 2));
        tablero.colocarPieza(crearReina(Color.BLANCO), new Posicion(7, 3));
        tablero.colocarPieza(crearRey(Color.BLANCO), new Posicion(7, 4));
        tablero.colocarPieza(crearAlfil(Color.BLANCO), new Posicion(7, 5));
        tablero.colocarPieza(crearCaballo(Color.BLANCO), new Posicion(7, 6));
        tablero.colocarPieza(crearTorre(Color.BLANCO), new Posicion(7, 7));
    }
}

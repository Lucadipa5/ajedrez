package ajedrez.tests;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.dominio.TipoPieza;
import ajedrez.nucleo.estrategias.MovimientoPeon;
import ajedrez.nucleo.estrategias.MovimientoRey;
import ajedrez.nucleo.fabrica.DisposicionEstandar;
import ajedrez.nucleo.fabrica.FabricaPiezas;
import ajedrez.nucleo.tablero.Tablero;

import java.util.List;

import static ajedrez.tests.Afirmar.p;
import static ajedrez.tests.Afirmar.que;

/**
 * Tablero, piezas y estrategias de movimiento (Strategy + composición sobre herencia).
 */
final class TestsTableroYPiezas {

    static void correr() {
        Afirmar.seccion("Tablero y piezas");
        tableroInstanciableYClonable();
        tableroDeOtroTamano();
        piezasRespetanElTamanoDelTablero();
        peonUsaLasFilasDelTablero();
        reinaCompuestaPorTorreYAlfil();
        fabricaAbiertaAExtension();
        disposicionEstandar();
    }

    private static void tableroInstanciableYClonable() {
        // Arrange
        Tablero t1 = new Tablero();
        FabricaPiezas fabrica = new FabricaPiezas();
        Posicion pos = new Posicion(0, 0);
        t1.colocarPieza(fabrica.crearTorre(Color.BLANCO), pos);

        // Act
        Tablero clon = t1.clonar();
        clon.moverPieza(pos, new Posicion(0, 5));

        // Assert
        que(t1.estaOcupada(pos), "Tablero coloca pieza en casilla");
        que(t1.obtenerPieza(pos).getTipo() == TipoPieza.TORRE, "Pieza colocada es de tipo Torre");
        que(!clon.estaOcupada(pos) && clon.estaOcupada(new Posicion(0, 5)), "Clon del tablero se mueve por su cuenta");
        que(t1.estaOcupada(pos), "Modificar el clon NO afecta el tablero original (independencia)");
    }

    private static void tableroDeOtroTamano() {
        // Arrange
        Tablero estandar = new Tablero();
        Tablero grande = new Tablero(10, 10);

        // Assert
        que(estandar.getFilas() == 8 && estandar.getColumnas() == 8, "El tablero por defecto es de 8x8");
        que(!estandar.esCasillaValida(new Posicion(8, 0)), "En 8x8 la fila 8 no existe");
        que(grande.esCasillaValida(new Posicion(9, 9)), "En 10x10 la casilla (9,9) existe");
        que(!grande.esCasillaValida(new Posicion(10, 0)), "En 10x10 la fila 10 no existe");
        que(grande.clonar().getFilas() == 10, "El clon conserva el tamaño del tablero");
    }

    private static void piezasRespetanElTamanoDelTablero() {
        // Arrange: una torre en la esquina inferior izquierda de un tablero de 10x10
        Tablero grande = new Tablero(10, 10);
        FabricaPiezas fabrica = new FabricaPiezas();
        Posicion esquina = new Posicion(9, 0);
        grande.colocarPieza(fabrica.crearTorre(Color.BLANCO), esquina);

        // Act
        List<Posicion> destinos = grande.obtenerPieza(esquina).calcularMovimientos(esquina, grande);

        // Assert
        que(destinos.contains(new Posicion(0, 0)), "La torre recorre las 10 filas del tablero grande");
        que(destinos.contains(new Posicion(9, 9)), "La torre recorre las 10 columnas del tablero grande");
        que(destinos.size() == 18, "La torre en la esquina de un 10x10 tiene 18 destinos (9 + 9)");
    }

    private static void peonUsaLasFilasDelTablero() {
        // Arrange: tablero de 10 filas; los peones blancos arrancan en la fila 8
        Tablero grande = new Tablero(10, 8);
        FabricaPiezas fabrica = new FabricaPiezas();
        Posicion inicio = new Posicion(8, 4);
        grande.colocarPieza(fabrica.crearPeon(Color.BLANCO), inicio);

        // Act
        List<Posicion> destinos = grande.obtenerPieza(inicio).calcularMovimientos(inicio, grande);

        // Assert
        que(MovimientoPeon.filaInicial(grande, Color.BLANCO) == 8, "La fila inicial del peón blanco depende del alto del tablero");
        que(destinos.contains(new Posicion(6, 4)), "El peón avanza doble desde su fila inicial en un tablero de 10 filas");
        que(MovimientoPeon.esFilaDeCoronacion(grande, Color.NEGRO, 9), "El peón negro corona en la última fila del tablero");
        que(!MovimientoPeon.esFilaDeCoronacion(grande, Color.NEGRO, 7), "La fila 7 no es de coronación en un tablero de 10 filas");
    }

    private static void reinaCompuestaPorTorreYAlfil() {
        // Arrange
        Tablero tablero = new Tablero();
        Pieza reina = new FabricaPiezas().crearReina(Color.BLANCO);
        Posicion centro = new Posicion(4, 4);
        tablero.colocarPieza(reina, centro);

        // Act
        List<Posicion> destinos = reina.calcularMovimientos(centro, tablero);

        // Assert
        que(destinos.contains(new Posicion(4, 0)), "Reina compuesta se mueve ortogonalmente a la izquierda");
        que(destinos.contains(new Posicion(0, 4)), "Reina compuesta se mueve ortogonalmente hacia arriba");
        que(destinos.contains(new Posicion(0, 0)), "Reina compuesta se mueve diagonalmente superior izquierda");
        que(destinos.contains(new Posicion(7, 7)), "Reina compuesta se mueve diagonalmente inferior derecha");
    }

    private static void fabricaAbiertaAExtension() {
        // Arrange: una variante donde el caballo se mueve como el rey, sin tocar ninguna clase existente
        FabricaPiezas fabrica = new FabricaPiezas();
        fabrica.registrar(TipoPieza.CABALLO, MovimientoRey::new);
        Tablero tablero = new Tablero();
        Posicion centro = new Posicion(4, 4);
        tablero.colocarPieza(fabrica.crearCaballo(Color.BLANCO), centro);

        // Act
        List<Posicion> destinos = tablero.obtenerPieza(centro).calcularMovimientos(centro, tablero);

        // Assert
        que(destinos.contains(new Posicion(3, 4)), "La estrategia registrada reemplaza a la original (Open/Closed)");
        que(!destinos.contains(new Posicion(2, 3)), "El caballo de la variante ya no salta en L");
        que(new FabricaPiezas().crearCaballo(Color.BLANCO).calcularMovimientos(centro, tablero).isEmpty() == false,
                "Otra fábrica sigue creando el caballo clásico (sin estado global)");
    }

    private static void disposicionEstandar() {
        // Arrange
        Tablero tablero = new Tablero();

        // Act
        new DisposicionEstandar().colocarPiezas(tablero, new FabricaPiezas());

        // Assert
        que(tablero.getCasillasOcupadas().size() == 32, "La disposición estándar coloca 32 piezas");
        que(tablero.obtenerPieza(p("e1")).getTipo() == TipoPieza.REY, "El rey blanco empieza en e1");
        que(tablero.obtenerPieza(p("d8")).getTipo() == TipoPieza.REINA, "La reina negra empieza en d8");
        que(tablero.obtenerPieza(p("a2")).getColor() == Color.BLANCO, "Los peones blancos empiezan en la segunda fila");
    }
}

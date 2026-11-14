package ajedrez.tests;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.EventoJugada;
import ajedrez.nucleo.dominio.Jugada;
import ajedrez.nucleo.dominio.NotacionAlgebraica;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.dominio.TipoPieza;
import ajedrez.nucleo.servicio.PartidaAjedrez;
import ajedrez.nucleo.tablero.NotacionFEN;
import ajedrez.nucleo.tablero.Tablero;

import static ajedrez.tests.Afirmar.p;
import static ajedrez.tests.Afirmar.que;

/**
 * Notaciones (FEN, UCI, algebraica): el idioma común con los motores de IA y los registros.
 */
final class TestsNotacion {

    static void correr() {
        Afirmar.seccion("Notación");
        fenPosicionInicial();
        fenRefleJaCasillaAlPasoYDerechosDeEnroque();
        fenSoloParaTableroEstandar();
        jugadaDesdeUCI();
        notacionAlgebraica();
    }

    private static void fenPosicionInicial() {
        // Arrange
        PartidaAjedrez partida = PartidaAjedrez.estandar();

        // Act
        String fen = NotacionFEN.desdeTablero(partida.getTablero(), Color.BLANCO);

        // Assert
        que(fen.equals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"), "FEN de la posición inicial es correcta");
    }

    private static void fenRefleJaCasillaAlPasoYDerechosDeEnroque() {
        // Arrange
        PartidaAjedrez partida = PartidaAjedrez.estandar();
        partida.intentarMover(p("e2"), p("e4"));

        // Act
        String trasE4 = NotacionFEN.desdeTablero(partida.getTablero(), Color.NEGRO, partida.getNumeroDeJugada());
        partida.intentarMover(p("g8"), p("f6"));
        partida.intentarMover(p("e1"), p("e2"));
        String trasMoverElRey = NotacionFEN.desdeTablero(partida.getTablero(), Color.NEGRO, partida.getNumeroDeJugada());

        // Assert
        que(trasE4.endsWith(" b KQkq e3 0 1"), "Tras e4 el FEN marca la casilla al paso e3 y el turno negro");
        que(trasMoverElRey.contains(" kq "), "Cuando el rey blanco se mueve, el FEN pierde los derechos de enroque blancos");
        // Nf6 y Ke2 son dos medias jugadas sin peón ni captura; con 3 medias jugadas va la jugada 2
        que(trasMoverElRey.endsWith(" 2 2"), "El FEN cuenta las medias jugadas sin progreso y el número de jugada");
    }

    private static void fenSoloParaTableroEstandar() {
        // Arrange
        Tablero grande = new Tablero(10, 10);

        // Act
        boolean rechazado = false;
        try {
            NotacionFEN.desdeTablero(grande, Color.BLANCO);
        } catch (IllegalArgumentException e) {
            rechazado = true;
        }

        // Assert
        que(rechazado, "La notación FEN rechaza tableros que no sean de 8x8");
    }

    private static void jugadaDesdeUCI() {
        // Act
        Jugada j = Jugada.desdeUCI("e2e4");
        Jugada corona = Jugada.desdeUCI("a7a8q");

        // Assert
        que(j != null && j.getOrigen().equals(new Posicion(6, 4)) && j.getDestino().equals(new Posicion(4, 4)), "UCI e2e4 se traduce a (6,4) -> (4,4)");
        que(corona != null && corona.getPromocion() == TipoPieza.REINA, "UCI a7a8q corona Reina");
        que(Jugada.desdeUCI("z9z9") == null, "UCI inválida devuelve null");
        que("e7e8q".equals(corona.aUCI()) == false && "a7a8q".equals(corona.aUCI()), "La jugada vuelve a UCI con su promoción");
    }

    private static void notacionAlgebraica() {
        // Arrange: una partida corta con captura, enroque, jaque y mate
        PartidaAjedrez partida = PartidaAjedrez.estandar();
        EventoJugada e4 = partida.intentarMover(p("e2"), p("e4"));
        partida.intentarMover(p("d7"), p("d5"));
        EventoJugada exd5 = partida.intentarMover(p("e4"), p("d5"));
        partida.intentarMover(p("g8"), p("f6"));
        EventoJugada cf3 = partida.intentarMover(p("g1"), p("f3"));

        PartidaAjedrez enroque = PartidaAjedrez.estandar();
        for (String[] j : new String[][]{{"e2", "e4"}, {"e7", "e5"}, {"g1", "f3"}, {"b8", "c6"}, {"f1", "c4"}, {"f8", "c5"}}) {
            enroque.intentarMover(p(j[0]), p(j[1]));
        }
        EventoJugada ooo = enroque.intentarMover(p("e1"), p("g1"));

        PartidaAjedrez mate = PartidaAjedrez.estandar();
        for (String[] j : new String[][]{{"f2", "f3"}, {"e7", "e5"}, {"g2", "g4"}}) mate.intentarMover(p(j[0]), p(j[1]));
        EventoJugada dh4 = mate.intentarMover(p("d8"), p("h4"));

        // Assert
        que("e4".equals(NotacionAlgebraica.describir(e4)), "Avance de peón: e4");
        que("exd5".equals(NotacionAlgebraica.describir(exd5)), "Captura de peón: exd5");
        que("Nf3".equals(NotacionAlgebraica.describir(cf3)), "Movimiento de pieza: Nf3");
        que("O-O".equals(NotacionAlgebraica.describir(ooo)), "Enroque corto: O-O");
        que("Qh4#".equals(NotacionAlgebraica.describir(dh4)), "Mate: Qh4#");
    }
}

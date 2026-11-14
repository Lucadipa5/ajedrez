package ajedrez.tests;

import ajedrez.nucleo.comandos.Comando;
import ajedrez.nucleo.comandos.MoverPiezaComando;
import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.EventoJugada;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.dominio.TipoPieza;
import ajedrez.nucleo.estados.EstadoPartida;
import ajedrez.nucleo.estados.EstadoTurnoBlanco;
import ajedrez.nucleo.estados.EstadoTurnoNegro;
import ajedrez.nucleo.fabrica.FabricaPiezas;
import ajedrez.nucleo.reglas.Reglamento;
import ajedrez.nucleo.servicio.ObservadorPartida;
import ajedrez.nucleo.servicio.PartidaAjedrez;
import ajedrez.nucleo.tablero.Tablero;

import static ajedrez.tests.Afirmar.p;
import static ajedrez.tests.Afirmar.que;

/**
 * La partida como servicio: State, Command (deshacer/rehacer), promoción, Observer e inyección.
 */
final class TestsPartida {

    static void correr() {
        Afirmar.seccion("Partida");
        stateTransicionDeTurnos();
        commandMoverYDeshacerRestauraPieza();
        promocionAEleccion();
        promocionUndoRestauraPeonOriginal();
        promocionConCapturaYUndoRestauraAmbas();
        flujoCompletoConUndo();
        eventoInformaCapturaYJaque();
        observadorRecibeLosEventos();
        partidaEnTableroDeOtroTamano();
    }

    private static PartidaAjedrez partidaVacia(ajedrez.nucleo.servicio.SelectorPromocion selector) {
        // Partida sin piezas: cada test coloca las suyas
        return new PartidaAjedrez(new Tablero(), (t, f) -> {}, Reglamento.estandar(), selector);
    }

    private static void stateTransicionDeTurnos() {
        // Arrange
        EstadoPartida estado = new EstadoTurnoBlanco();

        // Act
        EstadoPartida siguiente = estado.siguienteTurno();
        EstadoPartida revertido = siguiente.turnoAnterior();

        // Assert
        que(estado.getColorTurnoActual() == Color.BLANCO, "Estado inicial es Turno de Blancas");
        que(siguiente instanceof EstadoTurnoNegro && siguiente.getColorTurnoActual() == Color.NEGRO, "siguienteTurno pasa a Turno de Negras");
        que(revertido instanceof EstadoTurnoBlanco, "turnoAnterior revierte al turno de Blancas");
    }

    private static void commandMoverYDeshacerRestauraPieza() {
        // Arrange
        Tablero tablero = new Tablero();
        FabricaPiezas fabrica = new FabricaPiezas();
        Posicion origen = new Posicion(1, 0);
        Posicion destino = new Posicion(2, 0);
        Pieza peonBlanco = fabrica.crearPeon(Color.BLANCO);
        Pieza caballoNegro = fabrica.crearCaballo(Color.NEGRO);
        tablero.colocarPieza(peonBlanco, origen);
        tablero.colocarPieza(caballoNegro, destino);
        Tablero estadoAntes = tablero.clonar();
        Comando comando = new MoverPiezaComando(tablero, origen, destino);

        // Act 1: ejecutar
        comando.ejecutar();
        que(!tablero.estaOcupada(origen), "Command ejecutar: casilla origen queda vacía");
        que(tablero.obtenerPieza(destino) == peonBlanco, "Command ejecutar: peón ocupa casilla destino");

        // Act 2: deshacer
        comando.deshacer();

        // Assert
        que(tablero.obtenerPieza(origen) == peonBlanco, "Command undo: peón vuelve a su casilla origen");
        que(tablero.obtenerPieza(destino) == caballoNegro, "Command undo: caballo capturado fue restaurado");
        que(tablero.equals(estadoAntes), "Command undo: el tablero quedó idéntico al estado previo");
    }

    private static void promocionAEleccion() {
        // Arrange: el selector (un stub) siempre elige Caballo
        PartidaAjedrez partida = partidaVacia(color -> TipoPieza.CABALLO);
        Tablero tablero = partida.getTablero();
        FabricaPiezas fabrica = new FabricaPiezas();
        tablero.colocarPieza(fabrica.crearRey(Color.BLANCO), p("e1"));
        tablero.colocarPieza(fabrica.crearRey(Color.NEGRO), p("e8"));
        tablero.colocarPieza(fabrica.crearPeon(Color.BLANCO), p("b7"));

        // Act
        EventoJugada evento = partida.intentarMover(p("b7"), p("b8"));

        // Assert
        que(evento != null, "Peón avanza a la última fila con éxito");
        que(evento.promocion() == TipoPieza.CABALLO, "El evento informa la pieza de la coronación");
        que(tablero.obtenerPieza(p("b8")).getTipo() == TipoPieza.CABALLO, "Peón se coronó en la pieza elegida (Caballo)");
    }

    private static void promocionUndoRestauraPeonOriginal() {
        // Arrange
        PartidaAjedrez partida = partidaVacia(color -> TipoPieza.REINA);
        Tablero tablero = partida.getTablero();
        FabricaPiezas fabrica = new FabricaPiezas();
        tablero.colocarPieza(fabrica.crearRey(Color.BLANCO), p("e1"));
        tablero.colocarPieza(fabrica.crearRey(Color.NEGRO), p("e8"));
        tablero.colocarPieza(fabrica.crearPeon(Color.BLANCO), p("c7"));
        partida.intentarMover(p("c7"), p("c8"));
        que(tablero.obtenerPieza(p("c8")).getTipo() == TipoPieza.REINA, "Peón se convirtió en Reina");

        // Act
        boolean deshizo = partida.deshacerMovimiento();

        // Assert
        que(deshizo, "Undo de coronación fue exitoso");
        que(!tablero.estaOcupada(p("c8")), "Casilla de coronación vuelve a estar vacía tras Undo");
        Pieza restaurada = tablero.obtenerPieza(p("c7"));
        que(restaurada != null && restaurada.getTipo() == TipoPieza.PEON, "Casilla origen recuperó el PEÓN original (no se quedó como Reina)");
        que(partida.getEstadoActual().getColorTurnoActual() == Color.BLANCO, "Turno regresó al jugador blanco tras Undo");
    }

    private static void promocionConCapturaYUndoRestauraAmbas() {
        // Arrange
        PartidaAjedrez partida = partidaVacia(color -> TipoPieza.TORRE);
        Tablero tablero = partida.getTablero();
        FabricaPiezas fabrica = new FabricaPiezas();
        tablero.colocarPieza(fabrica.crearRey(Color.BLANCO), p("e1"));
        tablero.colocarPieza(fabrica.crearRey(Color.NEGRO), p("h8"));
        tablero.colocarPieza(fabrica.crearPeon(Color.BLANCO), p("b7"));
        tablero.colocarPieza(fabrica.crearTorre(Color.NEGRO), p("c8"));

        // Act: captura en diagonal y corona a Torre
        EventoJugada evento = partida.intentarMover(p("b7"), p("c8"));
        que(evento != null && evento.esCaptura(), "Peón captura en diagonal y corona en la última fila");
        que(tablero.obtenerPieza(p("c8")).getTipo() == TipoPieza.TORRE && tablero.obtenerPieza(p("c8")).getColor() == Color.BLANCO,
                "La pieza coronada en destino es una Torre blanca");

        // Act 2: deshacer
        partida.deshacerMovimiento();

        // Assert
        que(tablero.obtenerPieza(p("b7")).getTipo() == TipoPieza.PEON, "Undo restaura el PEÓN en su origen");
        que(tablero.obtenerPieza(p("c8")).getTipo() == TipoPieza.TORRE && tablero.obtenerPieza(p("c8")).getColor() == Color.NEGRO,
                "Undo restaura la TORRE negra capturada en su casilla");
    }

    private static void flujoCompletoConUndo() {
        // Arrange
        PartidaAjedrez partida = PartidaAjedrez.estandar();
        que(partida.getEstadoActual().getColorTurnoActual() == Color.BLANCO, "Inicio: Turno de Blancas");

        // Act
        EventoJugada evento = partida.intentarMover(p("e2"), p("e4"));
        que(evento != null, "Blancas mueven peón de e2 a e4");
        que(partida.getEstadoActual().getColorTurnoActual() == Color.NEGRO, "Turno cambia a Negras");
        boolean deshizo = partida.deshacerMovimiento();

        // Assert
        que(deshizo, "Deshacer movimiento fue exitoso");
        que(partida.getEstadoActual().getColorTurnoActual() == Color.BLANCO, "Deshacer retorna el turno a Blancas");
        que(partida.getTablero().estaOcupada(p("e2")) && !partida.getTablero().estaOcupada(p("e4")), "El peón regresó a e2");
        que(partida.rehacerMovimiento() && partida.getTablero().estaOcupada(p("e4")), "Rehacer vuelve a poner el peón en e4");
    }

    private static void eventoInformaCapturaYJaque() {
        // Arrange
        PartidaAjedrez partida = PartidaAjedrez.estandar();
        partida.intentarMover(p("e2"), p("e4"));
        partida.intentarMover(p("d7"), p("d5"));

        // Act
        EventoJugada captura = partida.intentarMover(p("e4"), p("d5"));
        partida.intentarMover(p("f7"), p("f6"));
        EventoJugada jaque = partida.intentarMover(p("d1"), p("h5"));

        // Assert
        que(captura.esCaptura() && captura.piezaCapturada().getTipo() == TipoPieza.PEON, "El evento informa la pieza capturada");
        que(captura.colorQueMovio() == Color.BLANCO && captura.numeroDeMediaJugada() == 3, "El evento informa quién movió y el número de media jugada");
        que(jaque.jaqueAlRival() && !jaque.finDePartida(), "El evento informa el jaque al rival");
    }

    /** Doble de prueba tipo mock: registra cómo lo llamaron para verificarlo después. */
    private static class ObservadorMock implements ObservadorPartida {
        int movidas, deshechas, rehechas, reinicios;
        EventoJugada ultimo;
        @Override public void alMover(EventoJugada e) { movidas++; ultimo = e; }
        @Override public void alDeshacer(EventoJugada e) { deshechas++; }
        @Override public void alRehacer(EventoJugada e) { rehechas++; }
        @Override public void alReiniciar(PartidaAjedrez p) { reinicios++; }
    }

    private static void observadorRecibeLosEventos() {
        // Arrange
        PartidaAjedrez partida = PartidaAjedrez.estandar();
        ObservadorMock mock = new ObservadorMock();
        partida.agregarObservador(mock);

        // Act
        partida.intentarMover(p("e2"), p("e4"));
        partida.intentarMover(p("e2"), p("e5")); // ilegal: no debe notificar
        partida.deshacerMovimiento();
        partida.rehacerMovimiento();
        partida.iniciarNuevaPartida();
        partida.quitarObservador(mock);
        partida.intentarMover(p("d2"), p("d4"));

        // Assert
        que(mock.movidas == 1 && mock.ultimo.jugada().getDestino().equals(p("e4")), "El observador recibe cada jugada legal (y ninguna ilegal)");
        que(mock.deshechas == 1 && mock.rehechas == 1, "El observador recibe deshacer y rehacer");
        que(mock.reinicios == 1, "El observador recibe el reinicio de la partida");
        que(mock.movidas == 1, "Después de desuscribirse no recibe más eventos");
    }

    private static void partidaEnTableroDeOtroTamano() {
        // Arrange: un ajedrez de 10x10 con reyes y una torre, todo inyectado por constructor
        PartidaAjedrez partida = new PartidaAjedrez(new Tablero(10, 10), (t, f) -> {
            t.colocarPieza(f.crearRey(Color.BLANCO), new Posicion(9, 5));
            t.colocarPieza(f.crearRey(Color.NEGRO), new Posicion(0, 5));
            t.colocarPieza(f.crearTorre(Color.BLANCO), new Posicion(9, 0));
        }, Reglamento.estandar(), null);

        // Act
        EventoJugada evento = partida.intentarMover(new Posicion(9, 0), new Posicion(1, 0));

        // Assert
        que(evento != null, "La torre recorre 8 casillas en un tablero de 10 filas");
        que(partida.getEstadoActual().getColorTurnoActual() == Color.NEGRO, "La partida de 10x10 alterna los turnos igual");
        // La torre en la fila 1 controla toda esa fila: al rey negro solo le quedan las dos casillas de la fila 0
        que(!partida.estaEnJaqueTurnoActual() && partida.obtenerMovimientosLegalesPara(new Posicion(0, 5)).size() == 2,
                "Las reglas de jaque valen en el tablero grande: la torre le quita al rey las casillas de la fila 1");
    }
}

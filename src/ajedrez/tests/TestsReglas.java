package ajedrez.tests;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.EventoJugada;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.dominio.TipoPieza;
import ajedrez.nucleo.estados.EstadoJaqueMate;
import ajedrez.nucleo.estados.EstadoTablas;
import ajedrez.nucleo.estados.EstadoTurnoBlanco;
import ajedrez.nucleo.estados.EstadoTurnoNegro;
import ajedrez.nucleo.fabrica.DisposicionInicial;
import ajedrez.nucleo.fabrica.FabricaPiezas;
import ajedrez.nucleo.reglas.DetectorJaque;
import ajedrez.nucleo.reglas.HistorialPosiciones;
import ajedrez.nucleo.reglas.MotivoTablas;
import ajedrez.nucleo.reglas.ReglaCincuentaMovimientos;
import ajedrez.nucleo.reglas.Reglamento;
import ajedrez.nucleo.reglas.ValidadorMovimiento;
import ajedrez.nucleo.servicio.PartidaAjedrez;
import ajedrez.nucleo.tablero.Tablero;

import java.util.List;

import static ajedrez.tests.Afirmar.p;
import static ajedrez.tests.Afirmar.que;

/**
 * Reglas del juego: jaque, legalidad, enroque, captura al paso, mate y tablas.
 */
final class TestsReglas {

    static void correr() {
        Afirmar.seccion("Reglas");
        deteccionDeJaque();
        validadorImpideMovimientosInvalidos();
        validadorImpideAutoJaque();
        enroqueCortoValido();
        enroqueLargoValido();
        enroqueInvalidoSiElReySeMovio();
        enroqueInvalidoCruzandoCasillaAtacada();
        enroqueInvalidoEnJaque();
        deshacerEnroqueRestauraTodo();
        capturaAlPaso();
        capturaAlPasoSoloInmediata();
        deshacerCapturaAlPasoRestauraElPeon();
        jaqueMateYDeshacer();
        ahogado();
        reglaDeCincuentaMovimientos();
        repeticionTriple();
        materialInsuficiente();
        reglamentoSinReglasDeTablas();
    }

    // -----------------------------------------------------------------------
    // Ayudas
    // -----------------------------------------------------------------------
    private static PartidaAjedrez partidaCon(DisposicionInicial disposicion) {
        return new PartidaAjedrez(new Tablero(), disposicion, Reglamento.estandar(), null);
    }

    private static DisposicionInicial reyesEn(String reyBlanco, String reyNegro) {
        return (t, f) -> {
            t.colocarPieza(f.crearRey(Color.BLANCO), p(reyBlanco));
            t.colocarPieza(f.crearRey(Color.NEGRO), p(reyNegro));
        };
    }

    private static DisposicionInicial paraEnroque() {
        return (t, f) -> {
            reyesEn("e1", "e8").colocarPiezas(t, f);
            t.colocarPieza(f.crearTorre(Color.BLANCO), p("h1"));
            t.colocarPieza(f.crearTorre(Color.BLANCO), p("a1"));
        };
    }

    // -----------------------------------------------------------------------
    // Jaque y legalidad
    // -----------------------------------------------------------------------
    private static void deteccionDeJaque() {
        // Arrange
        Tablero tablero = new Tablero();
        FabricaPiezas fabrica = new FabricaPiezas();
        DetectorJaque detector = new DetectorJaque();
        tablero.colocarPieza(fabrica.crearRey(Color.BLANCO), p("e1"));
        tablero.colocarPieza(fabrica.crearTorre(Color.NEGRO), p("e8"));

        // Act / Assert
        que(detector.estaEnJaque(tablero, Color.BLANCO), "Detector identifica jaque directo de torre en la misma columna");
        tablero.colocarPieza(fabrica.crearAlfil(Color.BLANCO), p("e5"));
        que(!detector.estaEnJaque(tablero, Color.BLANCO), "Jaque neutralizado al interponer pieza aliada");
    }

    private static void validadorImpideMovimientosInvalidos() {
        // Arrange
        Tablero tablero = new Tablero();
        ValidadorMovimiento validador = Reglamento.estandar().getValidador();
        tablero.colocarPieza(new FabricaPiezas().crearTorre(Color.BLANCO), p("a1"));

        // Act / Assert
        que(!validador.esMovimientoLegal(tablero, p("a1"), p("b2"), Color.BLANCO), "Validador rechaza movimiento diagonal para la Torre");
        que(validador.esMovimientoLegal(tablero, p("a1"), p("a4"), Color.BLANCO), "Validador acepta movimiento ortogonal despejado para la Torre");
        que(!validador.esMovimientoLegal(tablero, p("a1"), p("a4"), Color.NEGRO), "Validador rechaza mover una pieza del rival");
    }

    private static void validadorImpideAutoJaque() {
        // Arrange: alfil blanco clavado entre su rey y una torre negra
        Tablero tablero = new Tablero();
        FabricaPiezas fabrica = new FabricaPiezas();
        ValidadorMovimiento validador = Reglamento.estandar().getValidador();
        tablero.colocarPieza(fabrica.crearRey(Color.BLANCO), p("e1"));
        tablero.colocarPieza(fabrica.crearAlfil(Color.BLANCO), p("e2"));
        tablero.colocarPieza(fabrica.crearTorre(Color.NEGRO), p("e8"));

        // Act / Assert
        que(!validador.esMovimientoLegal(tablero, p("e2"), p("f3"), Color.BLANCO),
                "Validador prohíbe mover pieza clavada que expondría al Rey a jaque (auto-jaque)");
    }

    // -----------------------------------------------------------------------
    // Enroque
    // -----------------------------------------------------------------------
    private static void enroqueCortoValido() {
        // Arrange
        PartidaAjedrez partida = partidaCon(paraEnroque());

        // Act
        EventoJugada evento = partida.intentarMover(p("e1"), p("g1"));

        // Assert
        que(evento != null && evento.enroque(), "El enroque corto es legal y se informa como enroque");
        que(partida.getTablero().obtenerPieza(p("g1")).getTipo() == TipoPieza.REY, "El rey queda en g1");
        que(partida.getTablero().obtenerPieza(p("f1")).getTipo() == TipoPieza.TORRE, "La torre salta a f1");
        que(!partida.getTablero().estaOcupada(p("h1")), "h1 queda vacía");
    }

    private static void enroqueLargoValido() {
        // Arrange
        PartidaAjedrez partida = partidaCon(paraEnroque());

        // Act
        EventoJugada evento = partida.intentarMover(p("e1"), p("c1"));

        // Assert
        que(evento != null && evento.enroque(), "El enroque largo es legal");
        que(partida.getTablero().obtenerPieza(p("d1")).getTipo() == TipoPieza.TORRE, "La torre de a1 salta a d1");
    }

    private static void enroqueInvalidoSiElReySeMovio() {
        // Arrange: el rey va y vuelve
        PartidaAjedrez partida = partidaCon(paraEnroque());
        partida.intentarMover(p("e1"), p("e2"));
        partida.intentarMover(p("e8"), p("e7"));
        partida.intentarMover(p("e2"), p("e1"));
        partida.intentarMover(p("e7"), p("e8"));

        // Act / Assert
        que(partida.intentarMover(p("e1"), p("g1")) == null, "No se puede enrocar si el rey ya se movió, aunque haya vuelto");
    }

    private static void enroqueInvalidoCruzandoCasillaAtacada() {
        // Arrange: una torre negra controla la columna f, que el rey tiene que cruzar
        PartidaAjedrez partida = partidaCon((t, f) -> {
            paraEnroque().colocarPiezas(t, f);
            t.colocarPieza(f.crearTorre(Color.NEGRO), p("f5"));
        });

        // Act / Assert
        que(partida.intentarMover(p("e1"), p("g1")) == null, "No se puede enrocar cruzando una casilla atacada");
        que(partida.intentarMover(p("e1"), p("c1")) != null, "El enroque largo, que no cruza la columna f, sigue siendo legal");
    }

    private static void enroqueInvalidoEnJaque() {
        // Arrange: torre negra dando jaque por la columna e
        PartidaAjedrez partida = partidaCon((t, f) -> {
            paraEnroque().colocarPiezas(t, f);
            t.colocarPieza(f.crearTorre(Color.NEGRO), p("e5"));
        });

        // Act / Assert
        que(partida.estaEnJaqueTurnoActual(), "El rey blanco está en jaque");
        que(partida.intentarMover(p("e1"), p("g1")) == null, "No se puede enrocar estando en jaque");
    }

    private static void deshacerEnroqueRestauraTodo() {
        // Arrange
        PartidaAjedrez partida = partidaCon(paraEnroque());
        partida.intentarMover(p("e1"), p("g1"));

        // Act
        boolean deshizo = partida.deshacerMovimiento();

        // Assert
        Tablero t = partida.getTablero();
        que(deshizo && t.obtenerPieza(p("e1")).getTipo() == TipoPieza.REY, "Deshacer el enroque devuelve el rey a e1");
        que(t.obtenerPieza(p("h1")).getTipo() == TipoPieza.TORRE && !t.estaOcupada(p("f1")), "Deshacer el enroque devuelve la torre a h1");
        que(!t.obtenerPieza(p("e1")).seHaMovido() && !t.obtenerPieza(p("h1")).seHaMovido(), "Rey y torre vuelven a figurar como no movidos");
        que(partida.intentarMover(p("e1"), p("g1")) != null, "Después de deshacer, el enroque vuelve a estar disponible");
    }

    // -----------------------------------------------------------------------
    // Captura al paso
    // -----------------------------------------------------------------------
    private static DisposicionInicial paraAlPaso() {
        return (t, f) -> {
            reyesEn("h1", "h8").colocarPiezas(t, f);
            t.colocarPieza(f.crearPeon(Color.BLANCO), p("e5"));
            t.colocarPieza(f.crearPeon(Color.NEGRO), p("d7"));
        };
    }

    private static void capturaAlPaso() {
        // Arrange: el peón negro avanza dos y queda al lado del blanco
        PartidaAjedrez partida = partidaCon(paraAlPaso());
        partida.intentarMover(p("h1"), p("g1"));
        partida.intentarMover(p("d7"), p("d5"));

        // Act
        EventoJugada evento = partida.intentarMover(p("e5"), p("d6"));

        // Assert
        que(p("d6").equals(partida.getTablero().getCasillaAlPaso()) == false, "La casilla al paso se consume con la jugada");
        que(evento != null && evento.capturaAlPaso(), "La captura al paso es legal y se informa como tal");
        que(evento.esCaptura() && evento.piezaCapturada().getTipo() == TipoPieza.PEON, "La captura al paso captura al peón rival");
        que(!partida.getTablero().estaOcupada(p("d5")), "El peón capturado desaparece de d5");
        que(partida.getTablero().obtenerPieza(p("d6")).getColor() == Color.BLANCO, "El peón blanco queda en d6");
    }

    private static void capturaAlPasoSoloInmediata() {
        // Arrange: el avance doble ocurrió, pero se jugó otra cosa en el medio
        PartidaAjedrez partida = partidaCon(paraAlPaso());
        partida.intentarMover(p("h1"), p("g1"));
        partida.intentarMover(p("d7"), p("d5"));
        partida.intentarMover(p("g1"), p("h1"));
        partida.intentarMover(p("h8"), p("g8"));

        // Act / Assert
        que(partida.intentarMover(p("e5"), p("d6")) == null, "La captura al paso solo vale en la jugada inmediata");
    }

    private static void deshacerCapturaAlPasoRestauraElPeon() {
        // Arrange
        PartidaAjedrez partida = partidaCon(paraAlPaso());
        partida.intentarMover(p("h1"), p("g1"));
        partida.intentarMover(p("d7"), p("d5"));
        partida.intentarMover(p("e5"), p("d6"));

        // Act
        partida.deshacerMovimiento();

        // Assert
        Tablero t = partida.getTablero();
        que(t.obtenerPieza(p("d5")) != null && t.obtenerPieza(p("d5")).getColor() == Color.NEGRO, "Deshacer devuelve el peón negro a d5");
        que(t.obtenerPieza(p("e5")) != null && !t.estaOcupada(p("d6")), "Deshacer devuelve el peón blanco a e5");
        que(p("d6").equals(t.getCasillaAlPaso()), "Deshacer restaura la casilla al paso");
    }

    // -----------------------------------------------------------------------
    // Fin de partida
    // -----------------------------------------------------------------------
    private static void jaqueMateYDeshacer() {
        // Arrange: mate del loco
        PartidaAjedrez partida = PartidaAjedrez.estandar();
        partida.intentarMover(p("f2"), p("f3"));
        partida.intentarMover(p("e7"), p("e5"));
        partida.intentarMover(p("g2"), p("g4"));

        // Act
        EventoJugada mate = partida.intentarMover(p("d8"), p("h4"));

        // Assert
        que(mate != null && mate.esMate(), "El evento informa el jaque mate");
        que(partida.getEstadoActual() instanceof EstadoJaqueMate m && m.getGanador() == Color.NEGRO, "El estado es Jaque Mate con las Negras ganadoras");
        que(partida.intentarMover(p("e1"), p("f2")) == null, "Con la partida terminada no se puede mover");

        // Act 2: deshacer el mate
        partida.deshacerMovimiento();
        que(partida.getEstadoActual() instanceof EstadoTurnoNegro, "Deshacer el mate devuelve el turno a quien lo dio (Negras)");
        que(!partida.getEstadoActual().esFinalizada(), "Deshacer el mate reabre la partida");

        // Act 3: rehacer
        partida.rehacerMovimiento();
        que(partida.getEstadoActual() instanceof EstadoJaqueMate, "Rehacer vuelve a evaluar el mate");
    }

    private static void ahogado() {
        // Arrange: rey negro en h8; la dama blanca va a f7 y lo deja sin jugadas pero sin jaque
        PartidaAjedrez partida = partidaCon((t, f) -> {
            t.colocarPieza(f.crearRey(Color.NEGRO), p("h8"));
            t.colocarPieza(f.crearRey(Color.BLANCO), p("g6"));
            t.colocarPieza(f.crearReina(Color.BLANCO), p("f1"));
        });

        // Act
        partida.intentarMover(p("f1"), p("f7"));

        // Assert
        que(partida.getEstadoActual() instanceof EstadoTablas tablas
                && tablas.getMotivo() == MotivoTablas.AHOGADO && tablas.getColorEnTurno() == Color.NEGRO,
                "Sin jugadas legales y sin jaque es ahogado (tablas)");
        partida.deshacerMovimiento();
        que(partida.getEstadoActual() instanceof EstadoTurnoBlanco, "Deshacer el ahogado devuelve el turno a quien movió (Blancas)");
    }

    private static void reglaDeCincuentaMovimientos() {
        // Arrange: la regla aislada
        Tablero tablero = new Tablero();
        tablero.setMediasJugadasSinProgreso(99);
        que(new ReglaCincuentaMovimientos().evaluar(tablero, Color.BLANCO, new HistorialPosiciones()) == null,
                "Con 99 medias jugadas sin progreso todavía no hay tablas");
        tablero.setMediasJugadasSinProgreso(100);
        que(new ReglaCincuentaMovimientos().evaluar(tablero, Color.BLANCO, new HistorialPosiciones()) == MotivoTablas.CINCUENTA_MOVIMIENTOS,
                "Con 100 medias jugadas sin progreso la regla declara tablas");

        // Arrange: integrada en la partida
        PartidaAjedrez partida = partidaCon((t, f) -> {
            reyesEn("e1", "e8").colocarPiezas(t, f);
            t.colocarPieza(f.crearTorre(Color.BLANCO), p("a1"));
            t.colocarPieza(f.crearPeon(Color.NEGRO), p("h7"));
        });
        partida.getTablero().setMediasJugadasSinProgreso(99);

        // Act
        partida.intentarMover(p("a1"), p("a2"));

        // Assert
        que(partida.getEstadoActual() instanceof EstadoTablas tablas && tablas.getMotivo() == MotivoTablas.CINCUENTA_MOVIMIENTOS,
                "La jugada número 100 sin progreso termina la partida en tablas");
        partida.deshacerMovimiento();
        que(partida.getTablero().getMediasJugadasSinProgreso() == 99 && !partida.getEstadoActual().esFinalizada(),
                "Deshacer restaura el reloj de los 50 movimientos");
    }

    private static void repeticionTriple() {
        // Arrange: los caballos salen y vuelven dos veces
        PartidaAjedrez partida = PartidaAjedrez.estandar();
        String[][] ciclo = {{"g1", "f3"}, {"g8", "f6"}, {"f3", "g1"}, {"f6", "g8"}};

        // Act
        for (String[] j : ciclo) partida.intentarMover(p(j[0]), p(j[1]));
        boolean sigueTrasUnCiclo = !partida.getEstadoActual().esFinalizada();
        for (String[] j : ciclo) partida.intentarMover(p(j[0]), p(j[1]));

        // Assert
        que(sigueTrasUnCiclo, "La segunda aparición de la posición no termina la partida");
        que(partida.getEstadoActual() instanceof EstadoTablas tablas && tablas.getMotivo() == MotivoTablas.REPETICION_TRIPLE,
                "La tercera aparición de la misma posición es tablas por repetición");
    }

    private static void materialInsuficiente() {
        // Arrange: rey y caballo blancos contra rey y peón negros
        PartidaAjedrez partida = partidaCon((t, f) -> {
            reyesEn("h1", "h8").colocarPiezas(t, f);
            t.colocarPieza(f.crearCaballo(Color.BLANCO), p("f3"));
            t.colocarPieza(f.crearPeon(Color.NEGRO), p("d4"));
        });

        // Act: el caballo come el último peón
        EventoJugada evento = partida.intentarMover(p("f3"), p("d4"));

        // Assert
        que(evento != null && evento.esCaptura(), "El caballo captura el peón");
        que(partida.getEstadoActual() instanceof EstadoTablas tablas && tablas.getMotivo() == MotivoTablas.MATERIAL_INSUFICIENTE,
                "Rey y caballo contra rey es tablas por material insuficiente");
    }

    private static void reglamentoSinReglasDeTablas() {
        // Arrange: un reglamento inyectado sin ninguna regla de tablas
        FabricaPiezas fabrica = new FabricaPiezas();
        Reglamento base = Reglamento.estandar(fabrica);
        Reglamento sinTablas = new Reglamento(fabrica, base.getFabricaComandos(), base.getValidador(), List.of());
        PartidaAjedrez partida = new PartidaAjedrez(new Tablero(), (t, f) -> {
            reyesEn("e1", "e8").colocarPiezas(t, f);
            t.colocarPieza(f.crearTorre(Color.BLANCO), p("a1"));
        }, sinTablas, null);
        partida.getTablero().setMediasJugadasSinProgreso(100);

        // Act
        partida.intentarMover(p("a1"), p("a2"));

        // Assert
        que(!partida.getEstadoActual().esFinalizada(), "Con otro reglamento la misma posición no es tablas: las reglas son intercambiables");
    }
}

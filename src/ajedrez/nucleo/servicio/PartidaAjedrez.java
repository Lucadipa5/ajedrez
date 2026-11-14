package ajedrez.nucleo.servicio;

import ajedrez.nucleo.comandos.ComandoDeJugada;
import ajedrez.nucleo.comandos.HistorialComandos;
import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.EventoJugada;
import ajedrez.nucleo.dominio.Jugada;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.dominio.TipoPieza;
import ajedrez.nucleo.estados.EstadoJaqueMate;
import ajedrez.nucleo.estados.EstadoPartida;
import ajedrez.nucleo.estados.EstadoTablas;
import ajedrez.nucleo.estados.EstadoTurnoBlanco;
import ajedrez.nucleo.estrategias.MovimientoPeon;
import ajedrez.nucleo.fabrica.DisposicionEstandar;
import ajedrez.nucleo.fabrica.DisposicionInicial;
import ajedrez.nucleo.reglas.HistorialPosiciones;
import ajedrez.nucleo.reglas.MotivoTablas;
import ajedrez.nucleo.reglas.ReglaDeTablas;
import ajedrez.nucleo.reglas.Reglamento;
import ajedrez.nucleo.tablero.Tablero;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servicio central del núcleo (puerto de entrada): una partida de ajedrez.
 * Coordina el tablero, el reglamento, las fases (State), el historial (Command) y avisa
 * a sus observadores (Observer). No conoce ninguna interfaz ni infraestructura.
 *
 * Todas sus dependencias entran por constructor: con otro tablero, otra disposición u otro
 * reglamento se juega otra variante sin tocar esta clase. {@link #estandar()} arma el ajedrez clásico.
 */
public class PartidaAjedrez {
    private final Tablero tablero;
    private final DisposicionInicial disposicion;
    private final Reglamento reglamento;
    private SelectorPromocion selectorPromocion;

    private final HistorialComandos historialComandos = new HistorialComandos();
    private final HistorialPosiciones historialPosiciones = new HistorialPosiciones();
    private final Deque<EventoJugada> eventos = new ArrayDeque<>(); // en espejo con la pila de deshacer
    private final List<ObservadorPartida> observadores = new CopyOnWriteArrayList<>();
    private EstadoPartida estadoActual = new EstadoTurnoBlanco();

    public PartidaAjedrez(Tablero tablero, DisposicionInicial disposicion, Reglamento reglamento,
                          SelectorPromocion selectorPromocion) {
        this.tablero = Objects.requireNonNull(tablero);
        this.disposicion = Objects.requireNonNull(disposicion);
        this.reglamento = Objects.requireNonNull(reglamento);
        this.selectorPromocion = selectorPromocion != null ? selectorPromocion : SelectorPromocion.SIEMPRE_REINA;
        iniciarNuevaPartida();
    }

    /** Ajedrez clásico: tablero de 8x8, disposición oficial, reglamento estándar. */
    public static PartidaAjedrez estandar() {
        return estandar(SelectorPromocion.SIEMPRE_REINA);
    }

    public static PartidaAjedrez estandar(SelectorPromocion selectorPromocion) {
        return new PartidaAjedrez(new Tablero(), new DisposicionEstandar(), Reglamento.estandar(), selectorPromocion);
    }

    public void setSelectorPromocion(SelectorPromocion selectorPromocion) {
        if (selectorPromocion != null) {
            this.selectorPromocion = selectorPromocion;
        }
    }

    // -----------------------------------------------------------------------
    // Observadores
    // -----------------------------------------------------------------------
    public void agregarObservador(ObservadorPartida observador) {
        if (observador != null && !observadores.contains(observador)) observadores.add(observador);
    }

    public void quitarObservador(ObservadorPartida observador) {
        observadores.remove(observador);
    }

    // -----------------------------------------------------------------------
    // Ciclo de vida
    // -----------------------------------------------------------------------
    public void iniciarNuevaPartida() {
        historialComandos.limpiar();
        historialPosiciones.limpiar();
        eventos.clear();
        tablero.limpiar();
        disposicion.colocarPiezas(tablero, reglamento.getFabricaPiezas());
        estadoActual = new EstadoTurnoBlanco();
        historialPosiciones.registrar(tablero.claveDePosicion(Color.BLANCO));
        for (ObservadorPartida o : observadores) o.alReiniciar(this);
    }

    public EventoJugada intentarMover(Posicion origen, Posicion destino) {
        return intentarMover(new Jugada(origen, destino));
    }

    public EventoJugada intentarMover(Posicion origen, Posicion destino, TipoPieza promocion) {
        return intentarMover(new Jugada(origen, destino, promocion));
    }

    /**
     * Intenta ejecutar una jugada. Si es legal la aplica, evalúa el nuevo estado (jaque, mate,
     * tablas o siguiente turno) y devuelve qué pasó; si no es legal devuelve null y no cambia nada.
     */
    public EventoJugada intentarMover(Jugada jugada) {
        if (jugada == null || estadoActual.esFinalizada()) return null;

        Color turno = estadoActual.getColorTurnoActual();
        if (!reglamento.getValidador().esJugadaLegal(tablero, jugada, turno)) {
            return null;
        }
        jugada = resolverPromocion(jugada, turno);

        ComandoDeJugada comando = reglamento.getFabricaComandos().crear(tablero, jugada);
        historialComandos.ejecutarComando(comando);

        EventoJugada evento = cerrarJugada(comando, turno);
        for (ObservadorPartida o : observadores) o.alMover(evento);
        return evento;
    }

    /** Si la jugada corona, decide la pieza: la indicada, o la que elija el selector, o Reina. */
    private Jugada resolverPromocion(Jugada jugada, Color turno) {
        Pieza pieza = tablero.obtenerPieza(jugada.getOrigen());
        boolean corona = pieza.getTipo() == TipoPieza.PEON
                && MovimientoPeon.esFilaDeCoronacion(tablero, turno, jugada.getDestino().getFila());
        if (!corona) {
            return jugada.getPromocion() == null ? jugada : new Jugada(jugada.getOrigen(), jugada.getDestino());
        }
        TipoPieza eleccion = jugada.getPromocion();
        if (!esPromocionValida(eleccion)) eleccion = selectorPromocion.solicitarPromocion(turno);
        if (!esPromocionValida(eleccion)) eleccion = TipoPieza.REINA;
        return jugada.conPromocion(eleccion);
    }

    private static boolean esPromocionValida(TipoPieza tipo) {
        return tipo != null && tipo != TipoPieza.PEON && tipo != TipoPieza.REY;
    }

    /** Tras aplicar (o rehacer) un comando: registra la posición, pasa al nuevo estado y arma el evento. */
    private EventoJugada cerrarJugada(ComandoDeJugada comando, Color quienMovio) {
        Color rival = quienMovio.opuesto();
        historialPosiciones.registrar(tablero.claveDePosicion(rival));
        boolean jaque = reglamento.getDetectorJaque().estaEnJaque(tablero, rival);
        estadoActual = evaluarEstado(rival, jaque);

        EventoJugada evento = new EventoJugada(comando.getJugada(), comando.getPiezaMovida(), comando.getPiezaCapturada(),
                comando.esEnroque(), comando.esCapturaAlPaso(), comando.getPromocion(), jaque,
                estadoActual.esFinalizada(), historialComandos.cantidadComandosEjecutados());
        eventos.push(evento);
        return evento;
    }

    /** Estado de la partida cuando le toca mover a {@code enTurno}. */
    private EstadoPartida evaluarEstado(Color enTurno, boolean enJaque) {
        if (!reglamento.getValidador().tieneMovimientosLegales(tablero, enTurno)) {
            // Sin jugadas legales: en jaque es mate; si no, ahogado
            return enJaque ? new EstadoJaqueMate(enTurno.opuesto()) : new EstadoTablas(MotivoTablas.AHOGADO, enTurno);
        }
        for (ReglaDeTablas regla : reglamento.getReglasDeTablas()) {
            MotivoTablas motivo = regla.evaluar(tablero, enTurno, historialPosiciones);
            if (motivo != null) return new EstadoTablas(motivo, enTurno);
        }
        return estadoActual.siguienteTurno();
    }

    /**
     * Deshace la última jugada (patrón Command) y vuelve al estado anterior, incluso desde un
     * jaque mate o unas tablas.
     */
    public boolean deshacerMovimiento() {
        ComandoDeJugada comando = (ComandoDeJugada) historialComandos.deshacer();
        if (comando == null) return false;
        historialPosiciones.quitarUltima();
        estadoActual = estadoActual.turnoAnterior();
        EventoJugada evento = eventos.isEmpty() ? null : eventos.pop();
        for (ObservadorPartida o : observadores) o.alDeshacer(evento);
        return true;
    }

    /**
     * Rehace la jugada deshecha y vuelve a evaluar el estado resultante.
     */
    public boolean rehacerMovimiento() {
        if (!historialComandos.puedeRehacer() || estadoActual.esFinalizada()) return false;
        Color quienMueve = estadoActual.getColorTurnoActual();
        ComandoDeJugada comando = (ComandoDeJugada) historialComandos.rehacer();
        EventoJugada evento = cerrarJugada(comando, quienMueve);
        for (ObservadorPartida o : observadores) o.alRehacer(evento);
        return true;
    }

    // -----------------------------------------------------------------------
    // Consultas
    // -----------------------------------------------------------------------
    public List<Posicion> obtenerMovimientosLegalesPara(Posicion origen) {
        if (estadoActual.esFinalizada()) return List.of();
        return reglamento.getValidador().obtenerMovimientosLegales(tablero, origen, estadoActual.getColorTurnoActual());
    }

    public List<Jugada> obtenerJugadasLegales(Color color) {
        if (estadoActual.esFinalizada()) return List.of();
        return reglamento.getValidador().obtenerJugadasLegales(tablero, color);
    }

    public boolean estaEnJaqueTurnoActual() {
        if (estadoActual.esFinalizada()) return false;
        return reglamento.getDetectorJaque().estaEnJaque(tablero, estadoActual.getColorTurnoActual());
    }

    /** Número de jugada completa (1 al empezar, 2 después de que muevan ambos bandos, ...). */
    public int getNumeroDeJugada() {
        return historialComandos.cantidadComandosEjecutados() / 2 + 1;
    }

    public EventoJugada getUltimoEvento() {
        return eventos.peek();
    }

    public Tablero getTablero() { return tablero; }
    public EstadoPartida getEstadoActual() { return estadoActual; }
    public HistorialComandos getHistorialComandos() { return historialComandos; }
    public HistorialPosiciones getHistorialPosiciones() { return historialPosiciones; }
    public Reglamento getReglamento() { return reglamento; }
}

package ajedrez.nucleo.servicio;

import ajedrez.nucleo.comandos.HistorialComandos;
import ajedrez.nucleo.comandos.MoverPiezaComando;
import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.dominio.TipoPieza;
import ajedrez.nucleo.estados.EstadoAhogado;
import ajedrez.nucleo.estados.EstadoJaqueMate;
import ajedrez.nucleo.estados.EstadoPartida;
import ajedrez.nucleo.estados.EstadoTurnoBlanco;
import ajedrez.nucleo.fabrica.FabricaPiezas;
import ajedrez.nucleo.reglas.DetectorJaque;
import ajedrez.nucleo.reglas.ValidadorMovimiento;
import ajedrez.nucleo.tablero.Tablero;

import java.util.List;

/**
 * Servicio central del Núcleo (Hexagonal Architecture - Application Service / Facade).
 * Coordina las reglas de juego, el tablero, los turnos (State) y el historial (Command).
 *
 * El núcleo NO conoce frameworks, librerías gráficas ni interfaces de usuario.
 * Expone métodos limpios que los adaptadores de entrada (Swing, Consola, Web) consumen.
 */
public class PartidaAjedrez {
    private final Tablero tablero;
    private final FabricaPiezas fabricaPiezas;
    private final ValidadorMovimiento validadorMovimiento;
    private final DetectorJaque detectorJaque;
    private final HistorialComandos historialComandos;
    private SelectorPromocion selectorPromocion;
    private EstadoPartida estadoActual;

    public PartidaAjedrez() {
        this(color -> TipoPieza.REINA); // Por defecto en ausencia de adaptador de UI
    }

    public PartidaAjedrez(SelectorPromocion selectorPromocion) {
        this.tablero = new Tablero();
        this.fabricaPiezas = new FabricaPiezas();
        this.detectorJaque = new DetectorJaque();
        this.validadorMovimiento = new ValidadorMovimiento(detectorJaque);
        this.historialComandos = new HistorialComandos();
        this.selectorPromocion = selectorPromocion != null ? selectorPromocion : (color -> TipoPieza.REINA);
        this.estadoActual = new EstadoTurnoBlanco();
        iniciarNuevaPartida();
    }

    public void setSelectorPromocion(SelectorPromocion selectorPromocion) {
        if (selectorPromocion != null) {
            this.selectorPromocion = selectorPromocion;
        }
    }

    public void iniciarNuevaPartida() {
        historialComandos.limpiar();
        fabricaPiezas.poblarTableroInicial(tablero);
        this.estadoActual = new EstadoTurnoBlanco();
    }

    /**
     * Intenta ejecutar una jugada desde 'origen' hasta 'destino'.
     * Si la jugada es válida, aplica el comando, evalúa coronación de peón con el selector,
     * detecta jaque y transiciona el turno.
     */
    public boolean intentarMover(Posicion origen, Posicion destino) {
        return intentarMover(origen, destino, null);
    }

    public boolean intentarMover(Posicion origen, Posicion destino, TipoPieza promocionPredefinida) {
        if (estadoActual.esFinalizada()) {
            return false;
        }

        Color turnoActual = estadoActual.getColorTurnoActual();
        if (!validadorMovimiento.esMovimientoLegal(tablero, origen, destino, turnoActual)) {
            return false;
        }

        Pieza piezaAMover = tablero.obtenerPieza(origen);
        boolean esPromocion = (piezaAMover != null && piezaAMover.getTipo() == TipoPieza.PEON &&
                ((piezaAMover.getColor() == Color.BLANCO && destino.getFila() == 0) ||
                 (piezaAMover.getColor() == Color.NEGRO && destino.getFila() == 7)));

        TipoPieza eleccionPromocion = null;
        if (esPromocion) {
            if (promocionPredefinida != null && promocionPredefinida != TipoPieza.PEON && promocionPredefinida != TipoPieza.REY) {
                eleccionPromocion = promocionPredefinida;
            } else {
                eleccionPromocion = selectorPromocion.solicitarPromocion(piezaAMover.getColor());
                if (eleccionPromocion == null || eleccionPromocion == TipoPieza.PEON || eleccionPromocion == TipoPieza.REY) {
                    eleccionPromocion = TipoPieza.REINA;
                }
            }
        }

        // 1. Ejecutar el movimiento a través del patrón Command (incluye la promoción dentro del comando)
        MoverPiezaComando comando = new MoverPiezaComando(tablero, origen, destino, eleccionPromocion, fabricaPiezas);
        historialComandos.ejecutarComando(comando);

        // 2. Evaluar el estado del rival (Jaque / Jaque Mate / Ahogado) y transicionar de Estado
        Color rival = turnoActual.opuesto();
        boolean rivalEnJaque = detectorJaque.estaEnJaque(tablero, rival);
        boolean rivalTieneJugadas = validadorMovimiento.tieneMovimientosLegales(tablero, rival);

        if (!rivalTieneJugadas) {
            if (rivalEnJaque) {
                // Jaque Mate: el rival está en jaque y no puede escapar
                estadoActual = new EstadoJaqueMate(turnoActual);
            } else {
                // Ahogado (Stalemate): el rival NO está en jaque pero no tiene movimientos legales
                estadoActual = new EstadoAhogado(rival);
            }
        } else {
            estadoActual = estadoActual.siguienteTurno();
        }

        return true;
    }

    /**
     * Deshace la última jugada utilizando el patrón Command y revierte el turno.
     * Si la jugada fue una coronación, revierte la pieza al Peón original sin errores.
     */
    public boolean deshacerMovimiento() {
        if (!historialComandos.puedeDeshacer()) {
            return false;
        }
        boolean exito = historialComandos.deshacer();
        if (exito) {
            estadoActual = estadoActual.turnoAnterior();
        }
        return exito;
    }

    /**
     * Rehace el movimiento previamente deshecho.
     */
    public boolean rehacerMovimiento() {
        if (!historialComandos.puedeRehacer()) {
            return false;
        }
        boolean exito = historialComandos.rehacer();
        if (exito) {
            estadoActual = estadoActual.siguienteTurno();
        }
        return exito;
    }

    public List<Posicion> obtenerMovimientosLegalesPara(Posicion origen) {
        if (estadoActual.esFinalizada()) return List.of();
        return validadorMovimiento.obtenerMovimientosLegales(tablero, origen, estadoActual.getColorTurnoActual());
    }

    public boolean estaEnJaqueTurnoActual() {
        if (estadoActual.esFinalizada()) return false;
        return detectorJaque.estaEnJaque(tablero, estadoActual.getColorTurnoActual());
    }

    public Tablero getTablero() {
        return tablero;
    }

    public EstadoPartida getEstadoActual() {
        return estadoActual;
    }

    public HistorialComandos getHistorialComandos() {
        return historialComandos;
    }

    public ValidadorMovimiento getValidadorMovimiento() {
        return validadorMovimiento;
    }

    public DetectorJaque getDetectorJaque() {
        return detectorJaque;
    }

    public FabricaPiezas getFabricaPiezas() {
        return fabricaPiezas;
    }
}

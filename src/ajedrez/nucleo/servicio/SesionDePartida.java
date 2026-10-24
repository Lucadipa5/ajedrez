package ajedrez.nucleo.servicio;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Jugada;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.dominio.TipoPieza;
import ajedrez.nucleo.estados.EstadoJaqueMate;
import ajedrez.nucleo.ia.Dificultad;
import ajedrez.nucleo.ia.MotorAjedrez;
import ajedrez.nucleo.ia.MotorNoDisponibleException;
import ajedrez.nucleo.perfil.PerfilJugador;
import ajedrez.nucleo.perfil.ServicioCuentas;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Servicio de aplicación que coordina una partida con el progreso (XP) del jugador
 * y, opcionalmente, un rival controlado por IA.
 *
 * Reglas de XP:
 * - Solo dan XP las jugadas del color que eligió el jugador.
 * - Comer una pieza rival, dar jaque y ganar la partida suman puntos.
 * - Al deshacer se vuelve al perfil de antes de la jugada, así la XP no se puede ganar dos veces.
 * - El progreso se guarda solo cuando la partida termina.
 */
public class SesionDePartida {

    /** Motivo por el que se otorgó XP. */
    public enum MotivoXP { CAPTURA, JAQUE, VICTORIA }

    /** XP otorgada por una jugada. */
    public record EventoXP(MotivoXP motivo, int puntos, boolean subioNivel, int nivel) {}

    /** Resultado de una jugada válida. */
    public record ResultadoJugada(Jugada jugada, List<EventoXP> eventosXP, boolean partidaTerminada, boolean ganoJugador) {}

    private final PartidaAjedrez partida;
    private final Color colorJugador;
    private final ServicioCuentas cuentas;     // null si se juega sin cuenta
    private PerfilJugador perfil;              // null si se juega sin cuenta
    private final MotorAjedrez motorIA;        // null en modo 2 jugadores
    private final Dificultad dificultad;

    // Copias del perfil alineadas con el historial de jugadas
    private final Deque<PerfilJugador> perfilesAntesDeJugada = new ArrayDeque<>();
    private final Deque<PerfilJugador> perfilesDeshechos = new ArrayDeque<>();
    private final Random azar = new Random();

    public SesionDePartida(PartidaAjedrez partida, Color colorJugador, PerfilJugador perfil,
                           ServicioCuentas cuentas, MotorAjedrez motorIA, Dificultad dificultad) {
        this.partida = partida;
        this.colorJugador = colorJugador;
        this.perfil = perfil;
        this.cuentas = cuentas;
        this.motorIA = motorIA;
        this.dificultad = dificultad;
    }

    // -----------------------------------------------------------------------
    // Jugadas
    // -----------------------------------------------------------------------

    /**
     * Intenta hacer la jugada. Devuelve null si no es legal.
     */
    public ResultadoJugada mover(Jugada jugada) {
        Color mueve = partida.getEstadoActual().getColorTurnoActual();
        Pieza piezaEnDestino = partida.getTablero().obtenerPieza(jugada.getDestino());
        PerfilJugador perfilAntes = perfil != null ? perfil.copiar() : null;

        if (!partida.intentarMover(jugada.getOrigen(), jugada.getDestino(), jugada.getPromocion())) {
            return null;
        }
        if (perfilAntes != null) {
            perfilesAntesDeJugada.push(perfilAntes);
            perfilesDeshechos.clear(); // una jugada nueva invalida lo que se podía rehacer
        }

        List<EventoXP> eventos = new ArrayList<>();
        if (perfil != null && mueve == colorJugador) {
            if (piezaEnDestino != null && piezaEnDestino.getColor() == colorJugador.opuesto()) {
                boolean subio = perfil.registrarPiezaComida();
                eventos.add(new EventoXP(MotivoXP.CAPTURA, PerfilJugador.PUNTOS_COMER_PIEZA, subio, perfil.getNivel()));
            }
            if (partida.estaEnJaqueTurnoActual()) {
                boolean subio = perfil.registrarJaque();
                eventos.add(new EventoXP(MotivoXP.JAQUE, PerfilJugador.PUNTOS_JAQUE, subio, perfil.getNivel()));
            }
        }

        boolean terminada = partida.getEstadoActual().esFinalizada();
        boolean gano = terminada && partida.getEstadoActual() instanceof EstadoJaqueMate mate
                && mate.getGanador() == colorJugador;
        if (terminada && perfil != null) {
            perfil.registrarPartidaJugada();
            if (gano) {
                int nivelAntes = perfil.getNivel();
                perfil.registrarVictoria();
                eventos.add(new EventoXP(MotivoXP.VICTORIA, PerfilJugador.PUNTOS_VICTORIA,
                        perfil.getNivel() > nivelAntes, perfil.getNivel()));
            }
            cuentas.guardar(perfil);
        }
        return new ResultadoJugada(jugada, eventos, terminada, gano);
    }

    /** Con la partida terminada no se puede deshacer (evita repetir la XP de la victoria). */
    public boolean puedeDeshacer() {
        return !partida.getEstadoActual().esFinalizada() && partida.getHistorialComandos().puedeDeshacer();
    }

    public boolean puedeRehacer() {
        return !partida.getEstadoActual().esFinalizada() && partida.getHistorialComandos().puedeRehacer();
    }

    /**
     * Deshace una jugada. Contra la IA deshace también la respuesta de la IA,
     * para que vuelva a ser el turno del jugador.
     */
    public boolean deshacer() {
        if (!deshacerUnaJugada()) return false;
        if (esTurnoDeLaIA()) deshacerUnaJugada();
        return true;
    }

    public boolean rehacer() {
        if (!rehacerUnaJugada()) return false;
        if (esTurnoDeLaIA()) rehacerUnaJugada();
        return true;
    }

    private boolean deshacerUnaJugada() {
        if (!puedeDeshacer() || !partida.deshacerMovimiento()) return false;
        if (perfil != null && !perfilesAntesDeJugada.isEmpty()) {
            perfilesDeshechos.push(perfil);
            perfil = perfilesAntesDeJugada.pop();
        }
        return true;
    }

    /** Rehacer recupera la XP que ya se había ganado con esa jugada (no la suma de nuevo). */
    private boolean rehacerUnaJugada() {
        if (!puedeRehacer() || !partida.rehacerMovimiento()) return false;
        if (perfil != null && !perfilesDeshechos.isEmpty()) {
            perfilesAntesDeJugada.push(perfil);
            perfil = perfilesDeshechos.pop();
        }
        return true;
    }

    /** Empieza otra partida; si la actual no terminó, se descarta la XP ganada en ella. */
    public void reiniciar() {
        if (!partida.getEstadoActual().esFinalizada()) {
            descartarProgresoNoGuardado();
        }
        perfilesAntesDeJugada.clear();
        perfilesDeshechos.clear();
        partida.iniciarNuevaPartida();
    }

    /** Vuelve al último progreso guardado del perfil. */
    public void descartarProgresoNoGuardado() {
        if (perfil != null) {
            perfil = cuentas.recargar(perfil);
        }
    }

    // -----------------------------------------------------------------------
    // IA
    // -----------------------------------------------------------------------

    public boolean esContraIA() {
        return motorIA != null;
    }

    public boolean esTurnoDeLaIA() {
        return motorIA != null && !partida.getEstadoActual().esFinalizada()
                && partida.getEstadoActual().getColorTurnoActual() == colorJugador.opuesto();
    }

    /**
     * Le pide la jugada al motor de IA. Es lento: conviene llamarlo fuera del hilo de la interfaz.
     * Si el motor propone algo que las reglas no aceptan, se elige una jugada legal al azar.
     */
    public Jugada calcularJugadaIA() throws MotorNoDisponibleException {
        Color turno = partida.getEstadoActual().getColorTurnoActual();
        Jugada propuesta = motorIA.elegirJugada(partida.getTablero().clonar(), turno, dificultad);
        List<Jugada> legales = jugadasLegales(turno);
        if (legales.isEmpty()) {
            throw new MotorNoDisponibleException("No hay jugadas legales");
        }
        if (propuesta != null) {
            for (Jugada legal : legales) {
                if (legal.getOrigen().equals(propuesta.getOrigen()) && legal.getDestino().equals(propuesta.getDestino())) {
                    return conPromocionPorDefecto(propuesta);
                }
            }
        }
        return conPromocionPorDefecto(legales.get(azar.nextInt(legales.size())));
    }

    private List<Jugada> jugadasLegales(Color turno) {
        List<Jugada> jugadas = new ArrayList<>();
        for (Map.Entry<Posicion, Pieza> e : partida.getTablero().getCasillasOcupadas().entrySet()) {
            if (e.getValue().getColor() != turno) continue;
            for (Posicion destino : partida.obtenerMovimientosLegalesPara(e.getKey())) {
                jugadas.add(new Jugada(e.getKey(), destino, null));
            }
        }
        return jugadas;
    }

    /** La IA nunca debe abrir el diálogo de coronación: si no eligió pieza, corona Reina. */
    private Jugada conPromocionPorDefecto(Jugada jugada) {
        Pieza pieza = partida.getTablero().obtenerPieza(jugada.getOrigen());
        int filaFinal = jugada.getDestino().getFila();
        boolean corona = pieza != null && pieza.getTipo() == TipoPieza.PEON && (filaFinal == 0 || filaFinal == 7);
        if (corona && jugada.getPromocion() == null) {
            return new Jugada(jugada.getOrigen(), jugada.getDestino(), TipoPieza.REINA);
        }
        return jugada;
    }

    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------
    public PartidaAjedrez getPartida() { return partida; }
    public PerfilJugador getPerfil() { return perfil; }
    public Color getColorJugador() { return colorJugador; }
    public Dificultad getDificultad() { return dificultad; }
}

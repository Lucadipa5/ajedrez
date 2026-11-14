package ajedrez.nucleo.servicio;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.EventoJugada;
import ajedrez.nucleo.dominio.Jugada;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.TipoPieza;
import ajedrez.nucleo.estados.EstadoJaqueMate;
import ajedrez.nucleo.estrategias.MovimientoPeon;
import ajedrez.nucleo.ia.ConfiguracionIA;
import ajedrez.nucleo.ia.DecisionIA;
import ajedrez.nucleo.ia.Dificultad;
import ajedrez.nucleo.ia.MotorNoDisponibleException;
import ajedrez.nucleo.perfil.PerfilJugador;
import ajedrez.nucleo.perfil.ServicioCuentas;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
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
 *
 * Reglas de IA (la IA asesora, no arbitra):
 * - Toda sugerencia del motor se valida contra las jugadas legales del núcleo.
 * - Si el motor falla, no responde o propone algo ilegal, se juega una jugada legal de respaldo
 *   y la partida continúa. Cada decisión queda registrada.
 */
public class SesionDePartida {

    /** Motivo por el que se otorgó XP. */
    public enum MotivoXP { CAPTURA, JAQUE, VICTORIA }

    /** XP otorgada por una jugada. */
    public record EventoXP(MotivoXP motivo, int puntos, boolean subioNivel, int nivel) {}

    /** Resultado de una jugada válida. */
    public record ResultadoJugada(EventoJugada evento, List<EventoXP> eventosXP, boolean partidaTerminada, boolean ganoJugador) {
        public Jugada jugada() {
            return evento.jugada();
        }
    }

    private final PartidaAjedrez partida;
    private final Color colorJugador;
    private final ServicioCuentas cuentas;     // null si se juega sin cuenta
    private PerfilJugador perfil;              // null si se juega sin cuenta
    private final ConfiguracionIA ia;          // null en modo 2 jugadores

    // Copias del perfil alineadas con el historial de jugadas
    private final Deque<PerfilJugador> perfilesAntesDeJugada = new ArrayDeque<>();
    private final Deque<PerfilJugador> perfilesDeshechos = new ArrayDeque<>();
    private final Random azar = new Random();

    public SesionDePartida(PartidaAjedrez partida, Color colorJugador, PerfilJugador perfil,
                           ServicioCuentas cuentas, ConfiguracionIA ia) {
        this.partida = partida;
        this.colorJugador = colorJugador;
        this.perfil = perfil;
        this.cuentas = cuentas;
        this.ia = ia;
    }

    // -----------------------------------------------------------------------
    // Jugadas
    // -----------------------------------------------------------------------

    /**
     * Intenta hacer la jugada. Devuelve null si no es legal.
     */
    public ResultadoJugada mover(Jugada jugada) {
        PerfilJugador perfilAntes = perfil != null ? perfil.copiar() : null;

        EventoJugada evento = partida.intentarMover(jugada);
        if (evento == null) {
            return null;
        }
        if (perfilAntes != null) {
            perfilesAntesDeJugada.push(perfilAntes);
            perfilesDeshechos.clear(); // una jugada nueva invalida lo que se podía rehacer
        }

        List<EventoXP> eventos = new ArrayList<>();
        if (perfil != null && evento.colorQueMovio() == colorJugador) {
            if (evento.esCaptura()) {
                boolean subio = perfil.registrarPiezaComida();
                eventos.add(new EventoXP(MotivoXP.CAPTURA, PerfilJugador.PUNTOS_COMER_PIEZA, subio, perfil.getNivel()));
            }
            if (evento.jaqueAlRival()) {
                boolean subio = perfil.registrarJaque();
                eventos.add(new EventoXP(MotivoXP.JAQUE, PerfilJugador.PUNTOS_JAQUE, subio, perfil.getNivel()));
            }
        }

        boolean terminada = evento.finDePartida();
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
        return new ResultadoJugada(evento, eventos, terminada, gano);
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

    /** Libera los recursos de la sesión (el motor de IA, si lo hay). */
    public void finalizar() {
        if (ia != null) ia.motor().cerrar();
    }

    // -----------------------------------------------------------------------
    // IA
    // -----------------------------------------------------------------------

    public boolean esContraIA() {
        return ia != null;
    }

    public boolean esTurnoDeLaIA() {
        return ia != null && !partida.getEstadoActual().esFinalizada()
                && partida.getEstadoActual().getColorTurnoActual() == colorJugador.opuesto();
    }

    /**
     * Le pide la jugada al motor de IA. Es lento: conviene llamarlo fuera del hilo de la interfaz.
     * La sugerencia se valida contra las jugadas legales; si el motor falla o propone algo que las
     * reglas no aceptan, se elige una jugada legal al azar y la partida sigue.
     * @throws MotorNoDisponibleException solo si no hay ninguna jugada legal (no debería ocurrir)
     */
    public DecisionIA calcularJugadaIA() throws MotorNoDisponibleException {
        Color turno = partida.getEstadoActual().getColorTurnoActual();
        List<Jugada> legales = partida.obtenerJugadasLegales(turno);
        if (legales.isEmpty()) {
            throw new MotorNoDisponibleException("No hay jugadas legales");
        }
        String posicion = partida.getTablero().claveDePosicion(turno);

        Jugada sugerida = null;
        Jugada jugada = null;
        String resultado;
        try {
            sugerida = ia.motor().elegirJugada(partida.getTablero().clonar(), turno, ia.dificultad());
            if (sugerida == null) {
                resultado = "respaldo:sin-sugerencia";
            } else {
                Jugada propuesta = sugerida;
                boolean legal = legales.stream().anyMatch(l -> l.mismoRecorrido(propuesta));
                if (legal) {
                    jugada = conPromocionPorDefecto(sugerida);
                    resultado = DecisionIA.ACEPTADA;
                } else {
                    resultado = "respaldo:jugada-ilegal";
                }
            }
        } catch (MotorNoDisponibleException e) {
            resultado = "respaldo:error (" + e.getMessage() + ")";
        }
        if (jugada == null) {
            jugada = conPromocionPorDefecto(legales.get(azar.nextInt(legales.size())));
        }

        DecisionIA decision = new DecisionIA(posicion, ia.motor().getNombre(), sugerida, jugada, resultado);
        ia.registro().registrar(decision);
        return decision;
    }

    /** La IA nunca debe abrir el diálogo de coronación: si no eligió pieza, corona Reina. */
    private Jugada conPromocionPorDefecto(Jugada jugada) {
        Pieza pieza = partida.getTablero().obtenerPieza(jugada.getOrigen());
        boolean corona = pieza != null && pieza.getTipo() == TipoPieza.PEON
                && MovimientoPeon.esFilaDeCoronacion(partida.getTablero(), pieza.getColor(), jugada.getDestino().getFila());
        if (corona && jugada.getPromocion() == null) {
            return jugada.conPromocion(TipoPieza.REINA);
        }
        return jugada;
    }

    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------
    public PartidaAjedrez getPartida() { return partida; }
    public PerfilJugador getPerfil() { return perfil; }
    public Color getColorJugador() { return colorJugador; }
    public Dificultad getDificultad() { return ia != null ? ia.dificultad() : null; }
}

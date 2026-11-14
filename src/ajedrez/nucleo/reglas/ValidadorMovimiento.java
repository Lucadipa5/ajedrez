package ajedrez.nucleo.reglas;

import ajedrez.nucleo.comandos.Comando;
import ajedrez.nucleo.comandos.FabricaComandos;
import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Jugada;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.tablero.Tablero;

import java.util.ArrayList;
import java.util.List;

/**
 * Validador de reglas de movimiento del ajedrez.
 *
 * Es una función pura: mismo tablero + misma jugada = mismo resultado, siempre. Combina
 * la geometría de la pieza (su Strategy) con la regla que ninguna pieza conoce por sí sola:
 * una jugada no puede dejar al propio rey en jaque. Para eso simula la jugada en un clon
 * del tablero usando el mismo comando que la aplicaría de verdad (así el enroque y la captura
 * al paso se simulan completos, con la torre o el peón capturado donde corresponde).
 *
 * Sus dependencias entran por constructor: en un test se le puede dar otro detector o fábrica.
 */
public class ValidadorMovimiento {
    private final DetectorJaque detectorJaque;
    private final FabricaComandos fabricaComandos;

    public ValidadorMovimiento(DetectorJaque detectorJaque, FabricaComandos fabricaComandos) {
        this.detectorJaque = detectorJaque;
        this.fabricaComandos = fabricaComandos;
    }

    public DetectorJaque getDetectorJaque() {
        return detectorJaque;
    }

    /**
     * Valida si un movimiento desde origen hasta destino es legal para el jugador en turno.
     */
    public boolean esMovimientoLegal(Tablero tablero, Posicion origen, Posicion destino, Color colorTurno) {
        if (origen == null || destino == null) return false;
        return esJugadaLegal(tablero, new Jugada(origen, destino), colorTurno);
    }

    public boolean esJugadaLegal(Tablero tablero, Jugada jugada, Color colorTurno) {
        Posicion origen = jugada.getOrigen();
        Posicion destino = jugada.getDestino();
        if (origen.equals(destino)) return false;
        if (!tablero.esCasillaValida(origen) || !tablero.esCasillaValida(destino)) return false;

        Pieza pieza = tablero.obtenerPieza(origen);
        if (pieza == null || pieza.getColor() != colorTurno) return false;

        // 1. El destino tiene que estar dentro del patrón geométrico de la pieza
        if (!pieza.calcularMovimientos(origen, tablero).contains(destino)) {
            return false;
        }

        // 2. Enroque: no se puede estar en jaque ni cruzar una casilla atacada
        if (FabricaComandos.esEnroque(tablero, jugada)) {
            if (detectorJaque.estaEnJaque(tablero, colorTurno)) return false;
            Tablero unPaso = tablero.clonar();
            unPaso.moverPieza(origen, origen.desplazar(0, Integer.signum(jugada.deltaColumna())));
            if (detectorJaque.estaEnJaque(unPaso, colorTurno)) return false;
        }

        // 3. Simulación completa: la jugada no puede dejar al propio rey en jaque
        Tablero simulado = tablero.clonar();
        Comando comando = fabricaComandos.crear(simulado, jugada);
        comando.ejecutar();
        return !detectorJaque.estaEnJaque(simulado, colorTurno);
    }

    /**
     * Retorna todas las casillas destino a las que la pieza en 'origen' puede moverse legalmente.
     */
    public List<Posicion> obtenerMovimientosLegales(Tablero tablero, Posicion origen, Color colorTurno) {
        List<Posicion> legales = new ArrayList<>();
        Pieza pieza = tablero.obtenerPieza(origen);
        if (pieza == null || pieza.getColor() != colorTurno) {
            return legales;
        }
        for (Posicion destino : pieza.calcularMovimientos(origen, tablero)) {
            if (esJugadaLegal(tablero, new Jugada(origen, destino), colorTurno)) {
                legales.add(destino);
            }
        }
        return legales;
    }

    /** Todas las jugadas legales del color (sin la promoción resuelta). */
    public List<Jugada> obtenerJugadasLegales(Tablero tablero, Color color) {
        List<Jugada> jugadas = new ArrayList<>();
        for (Posicion origen : tablero.posicionesDe(color)) {
            for (Posicion destino : obtenerMovimientosLegales(tablero, origen, color)) {
                jugadas.add(new Jugada(origen, destino));
            }
        }
        return jugadas;
    }

    /**
     * Verifica si el bando especificado tiene al menos un movimiento legal posible en todo el tablero.
     */
    public boolean tieneMovimientosLegales(Tablero tablero, Color color) {
        for (Posicion origen : tablero.posicionesDe(color)) {
            if (!obtenerMovimientosLegales(tablero, origen, color).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}

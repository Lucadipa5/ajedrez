package ajedrez.nucleo.reglas;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.tablero.Tablero;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validador de reglas de movimiento del ajedrez.
 *
 * Mantiene la regla de Testabilidad del Núcleo enseñada por la cátedra:
 * Función pura que valida si una jugada es físicamente posible y reglamentariamente legal
 * (impidiendo que un jugador realice jugadas que dejen a su propio rey en jaque).
 */
public class ValidadorMovimiento {
    private final DetectorJaque detectorJaque;

    public ValidadorMovimiento(DetectorJaque detectorJaque) {
        this.detectorJaque = detectorJaque;
    }

    public ValidadorMovimiento() {
        this(new DetectorJaque());
    }

    /**
     * Valida si un movimiento desde origen hasta destino es legal para el jugador en turno.
     */
    public boolean esMovimientoLegal(Tablero tablero, Posicion origen, Posicion destino, Color colorTurno) {
        if (origen == null || destino == null || origen.equals(destino)) return false;
        if (!origen.esValida() || !destino.esValida()) return false;

        Pieza pieza = tablero.obtenerPieza(origen);
        if (pieza == null) return false;
        if (pieza.getColor() != colorTurno) return false;

        // 1. Verificar si el destino entra dentro del patrón geométrico de la pieza
        List<Posicion> destinosFisicos = pieza.calcularMovimientos(origen, tablero);
        if (!destinosFisicos.contains(destino)) {
            return false;
        }

        // 2. Simulación: verificar que el movimiento no deje al propio rey en jaque
        Tablero simulado = tablero.clonar();
        simulado.moverPieza(origen, destino);

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

        List<Posicion> posibles = pieza.calcularMovimientos(origen, tablero);
        for (Posicion destino : posibles) {
            if (esMovimientoLegal(tablero, origen, destino, colorTurno)) {
                legales.add(destino);
            }
        }
        return legales;
    }

    /**
     * Verifica si el bando especificado tiene al menos un movimiento legal posible en todo el tablero.
     */
    public boolean tieneMovimientosLegales(Tablero tablero, Color color) {
        for (Map.Entry<Posicion, Pieza> entrada : tablero.getCasillasOcupadas().entrySet()) {
            if (entrada.getValue().getColor() == color) {
                List<Posicion> legales = obtenerMovimientosLegales(tablero, entrada.getKey(), color);
                if (!legales.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
}

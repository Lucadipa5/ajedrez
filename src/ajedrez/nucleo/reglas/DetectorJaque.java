package ajedrez.nucleo.reglas;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.tablero.Tablero;

import java.util.List;
import java.util.Map;

/**
 * Servicio de dominio puro para la detección de Jaque.
 * No depende de ninguna infraestructura ni motor externo, permitiendo ser testeado
 * de forma 100% determinística y aislada.
 */
public class DetectorJaque {

    /**
     * Determina si el rey del color especificado está siendo amenazado por alguna pieza enemiga.
     */
    public boolean estaEnJaque(Tablero tablero, Color colorRey) {
        Posicion posRey = tablero.buscarRey(colorRey);
        if (posRey == null) {
            return false; // Si no hay rey en el tablero (ej: tests sintéticos), no hay jaque
        }

        Color colorEnemigo = colorRey.opuesto();
        for (Map.Entry<Posicion, Pieza> entrada : tablero.getCasillasOcupadas().entrySet()) {
            Pieza pieza = entrada.getValue();
            if (pieza.getColor() == colorEnemigo) {
                Posicion posEnemiga = entrada.getKey();
                List<Posicion> ataques = pieza.calcularMovimientos(posEnemiga, tablero);
                if (ataques.contains(posRey)) {
                    return true;
                }
            }
        }
        return false;
    }
}

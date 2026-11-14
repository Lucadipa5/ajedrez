package ajedrez.nucleo.reglas;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.dominio.TipoPieza;
import ajedrez.nucleo.tablero.Tablero;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tablas cuando ningún bando puede dar mate: rey contra rey, rey y una pieza menor
 * (alfil o caballo) contra rey, o rey y alfil contra rey y alfil con los alfiles en
 * casillas del mismo color.
 */
public class ReglaMaterialInsuficiente implements ReglaDeTablas {

    @Override
    public MotivoTablas evaluar(Tablero tablero, Color colorEnTurno, HistorialPosiciones historial) {
        List<Posicion> menores = new ArrayList<>();
        for (Map.Entry<Posicion, Pieza> e : tablero.getCasillasOcupadas().entrySet()) {
            TipoPieza tipo = e.getValue().getTipo();
            if (tipo == TipoPieza.REY) continue;
            if (tipo == TipoPieza.ALFIL || tipo == TipoPieza.CABALLO) {
                menores.add(e.getKey());
            } else {
                return null; // un peón, torre o reina alcanza para dar mate
            }
        }
        if (menores.size() <= 1) return MotivoTablas.MATERIAL_INSUFICIENTE;

        // Dos alfiles de bandos distintos sobre casillas del mismo color tampoco pueden darse mate
        if (menores.size() == 2) {
            Pieza a = tablero.obtenerPieza(menores.get(0));
            Pieza b = tablero.obtenerPieza(menores.get(1));
            boolean ambosAlfiles = a.getTipo() == TipoPieza.ALFIL && b.getTipo() == TipoPieza.ALFIL;
            boolean mismaCasillaColor = colorDeCasilla(menores.get(0)) == colorDeCasilla(menores.get(1));
            if (ambosAlfiles && !a.esDelMismoBando(b) && mismaCasillaColor) {
                return MotivoTablas.MATERIAL_INSUFICIENTE;
            }
        }
        return null;
    }

    private static int colorDeCasilla(Posicion p) {
        return (p.getFila() + p.getColumna()) % 2;
    }
}

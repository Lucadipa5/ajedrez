package ajedrez.nucleo.estrategias;

import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.dominio.TipoPieza;
import ajedrez.nucleo.tablero.Tablero;

import java.util.ArrayList;
import java.util.List;

/**
 * Enroque como estrategia propia: el rey se desplaza dos casillas hacia una torre de su bando
 * que está en el borde de la fila, si ninguno de los dos se movió y las casillas entre ellos están vacías.
 *
 * Esta estrategia resuelve solo la geometría. Que el rey no esté en jaque ni cruce una casilla
 * atacada lo controla el {@code ValidadorMovimiento}, que es quien conoce el jaque.
 * Como es una estrategia aparte, quitar el enroque de una variante es no componerla en la fábrica.
 */
public class MovimientoEnroque implements EstrategiaMovimiento {

    @Override
    public List<Posicion> calcularPosiblesDestinos(Posicion origen, Tablero tablero) {
        List<Posicion> destinos = new ArrayList<>();
        Pieza rey = tablero.obtenerPieza(origen);
        if (rey == null || rey.seHaMovido()) return destinos;

        int fila = origen.getFila();
        for (int columnaTorre : new int[]{0, tablero.getColumnas() - 1}) {
            // Hace falta lugar para que el rey avance dos y la torre quede del otro lado
            if (Math.abs(columnaTorre - origen.getColumna()) < 3) continue;

            Pieza torre = tablero.obtenerPieza(new Posicion(fila, columnaTorre));
            if (torre == null || torre.getTipo() != TipoPieza.TORRE || !torre.esDelMismoBando(rey) || torre.seHaMovido()) {
                continue;
            }
            int paso = columnaTorre > origen.getColumna() ? 1 : -1;
            boolean caminoLibre = true;
            for (int c = origen.getColumna() + paso; c != columnaTorre; c += paso) {
                if (tablero.estaOcupada(new Posicion(fila, c))) {
                    caminoLibre = false;
                    break;
                }
            }
            if (caminoLibre) {
                destinos.add(origen.desplazar(0, 2 * paso));
            }
        }
        return destinos;
    }
}

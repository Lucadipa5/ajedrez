package ajedrez.nucleo.tablero;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.dominio.TipoPieza;

import java.util.*;

/**
 * Representa el tablero físico de ajedrez (8x8) y el estado de sus casillas ocupadas.
 *
 * NOTA DE DISEÑO (ANTI-SINGLETON):
 * A diferencia de los diseños acoplados que convierten el tablero en un Singleton global (Nivel 2),
 * esta clase se instancia limpiamente como un objeto del dominio ordinario. Esto permite:
 * 1. Independencia total y testabilidad unitaria: cada test instancia su propio tablero sin compartir estado mutable.
 * 2. Posibilidad de clonar el tablero para simulaciones puras (detección de jaque, backtracking de jugadas).
 */
public class Tablero {
    private final Map<Posicion, Pieza> casillasOcupadas;

    public Tablero() {
        this.casillasOcupadas = new HashMap<>();
    }

    public boolean esCasillaValida(Posicion pos) {
        return pos != null && pos.esValida();
    }

    public boolean estaOcupada(Posicion pos) {
        return esCasillaValida(pos) && casillasOcupadas.containsKey(pos);
    }

    public Pieza obtenerPieza(Posicion pos) {
        if (!esCasillaValida(pos)) return null;
        return casillasOcupadas.get(pos);
    }

    public void colocarPieza(Pieza pieza, Posicion pos) {
        if (!esCasillaValida(pos) || pieza == null) return;
        casillasOcupadas.put(pos, pieza);
    }

    public Pieza removerPieza(Posicion pos) {
        if (!esCasillaValida(pos)) return null;
        return casillasOcupadas.remove(pos);
    }

    /**
     * Mueve una pieza de origen a destino. Si la casilla destino estaba ocupada,
     * sobreescribe la posición y retorna la pieza capturada (o null si la casilla estaba vacía).
     */
    public Pieza moverPieza(Posicion origen, Posicion destino) {
        if (!estaOcupada(origen) || !esCasillaValida(destino)) {
            return null;
        }
        Pieza piezaAMover = casillasOcupadas.remove(origen);
        Pieza piezaCapturada = casillasOcupadas.put(destino, piezaAMover);
        piezaAMover.setSeHaMovido(true);
        return piezaCapturada;
    }

    public Posicion buscarRey(Color color) {
        for (Map.Entry<Posicion, Pieza> entrada : casillasOcupadas.entrySet()) {
            Pieza p = entrada.getValue();
            if (p.getTipo() == TipoPieza.REY && p.getColor() == color) {
                return entrada.getKey();
            }
        }
        return null;
    }

    public Map<Posicion, Pieza> getCasillasOcupadas() {
        return Collections.unmodifiableMap(casillasOcupadas);
    }

    public void limpiar() {
        casillasOcupadas.clear();
    }

    /**
     * Retorna una copia PROFUNDA del tablero: cada Pieza es una nueva instancia independiente.
     * Esto es esencial para las simulaciones de jaque/jaque-mate: si el clon modifica
     * el flag seHaMovido de una pieza durante la simulación, NO debe afectar al tablero real.
     * Un clon superficial (shallow) compartía las referencias a Pieza y causaba bugs donde
     * los movimientos legales del rival se calculaban incorrectamente, impidiendo la
     * detección del jaque mate.
     */
    public Tablero clonar() {
        Tablero clon = new Tablero();
        for (Map.Entry<Posicion, Pieza> entrada : this.casillasOcupadas.entrySet()) {
            Pieza original = entrada.getValue();
            // Crear nueva instancia de Pieza con el mismo tipo, color, estrategia y seHaMovido
            Pieza copia = new Pieza(original.getTipo(), original.getColor(),
                    original.getEstrategiaMovimiento());
            copia.setSeHaMovido(original.seHaMovido());
            clon.casillasOcupadas.put(entrada.getKey(), copia);
        }
        return clon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tablero tablero = (Tablero) o;
        if (casillasOcupadas.size() != tablero.casillasOcupadas.size()) return false;
        for (Map.Entry<Posicion, Pieza> e : casillasOcupadas.entrySet()) {
            Pieza otra = tablero.casillasOcupadas.get(e.getKey());
            if (otra == null || otra.getTipo() != e.getValue().getTipo() || otra.getColor() != e.getValue().getColor()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(casillasOcupadas.size());
    }
}

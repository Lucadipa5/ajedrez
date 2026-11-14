package ajedrez.nucleo.dominio;

import ajedrez.nucleo.estrategias.EstrategiaMovimiento;
import ajedrez.nucleo.tablero.Tablero;

import java.util.List;
import java.util.Objects;

/**
 * Entidad del núcleo que representa una pieza de ajedrez.
 *
 * Composición sobre herencia: no hay una jerarquía Pieza → Torre → ... con comportamiento heredado.
 * La pieza TIENE una estrategia de movimiento (patrón Strategy) que recibe por constructor;
 * la Reina, por ejemplo, se arma combinando las estrategias de la Torre y el Alfil.
 */
public class Pieza {
    private final TipoPieza tipo;
    private final Color color;
    private final EstrategiaMovimiento estrategiaMovimiento;
    private boolean seHaMovido;

    public Pieza(TipoPieza tipo, Color color, EstrategiaMovimiento estrategiaMovimiento) {
        this.tipo = Objects.requireNonNull(tipo, "El tipo de pieza no puede ser nulo");
        this.color = Objects.requireNonNull(color, "El color no puede ser nulo");
        this.estrategiaMovimiento = Objects.requireNonNull(estrategiaMovimiento, "La estrategia no puede ser nula");
        this.seHaMovido = false;
    }

    public TipoPieza getTipo() {
        return tipo;
    }

    public Color getColor() {
        return color;
    }

    /** Si ya se movió alguna vez en la partida (lo usan el enroque y el avance doble del peón). */
    public boolean seHaMovido() {
        return seHaMovido;
    }

    public void setSeHaMovido(boolean seHaMovido) {
        this.seHaMovido = seHaMovido;
    }

    /**
     * Delega el cálculo de posibles destinos a su estrategia de movimiento.
     */
    public List<Posicion> calcularMovimientos(Posicion origen, Tablero tablero) {
        return estrategiaMovimiento.calcularPosiblesDestinos(origen, tablero);
    }

    public EstrategiaMovimiento getEstrategiaMovimiento() {
        return estrategiaMovimiento;
    }

    public boolean esDelMismoBando(Pieza otra) {
        return otra != null && otra.color == this.color;
    }

    @Override
    public String toString() {
        return color + " " + tipo;
    }
}

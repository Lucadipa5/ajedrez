package ajedrez.nucleo.dominio;

import ajedrez.nucleo.estrategias.EstrategiaMovimiento;
import ajedrez.nucleo.tablero.Tablero;

import java.util.List;
import java.util.Objects;

/**
 * Entidad del núcleo que representa una pieza de ajedrez.
 * Aplica el principio de Composición sobre Herencia: en lugar de forzar una jerarquía
 * rígida con herencia de comportamientos, la pieza POSEE una estrategia de movimiento
 * inyectada en su constructor (has-a / Strategy Pattern).
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

    public boolean seHaMovido() {
        return seHaMovido;
    }

    public void setSeHaMovido(boolean seHaMovido) {
        this.seHaMovido = seHaMovido;
    }

    /**
     * Delega el cálculo de posibles destinos a su estrategia de movimiento inyectada.
     */
    public List<Posicion> calcularMovimientos(Posicion origen, Tablero tablero) {
        return estrategiaMovimiento.calcularPosiblesDestinos(origen, tablero);
    }

    public EstrategiaMovimiento getEstrategiaMovimiento() {
        return estrategiaMovimiento;
    }

    public String getSimboloUnicode() {
        return tipo.getSimboloUnicode(color);
    }

    @Override
    public String toString() {
        return color + " " + tipo;
    }
}

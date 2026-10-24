package ajedrez.nucleo.estrategias;

import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.tablero.Tablero;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Patrón Composite aplicado a Estrategias de Movimiento.
 * EJEMPLO DIRECTO DE COMPOSICIÓN SOBRE HERENCIA:
 * Permite que una pieza combine múltiples capacidades de movimiento independientes
 * sin recurrir a herencia múltiple ni a duplicación de código.
 *
 * Ejemplo concreto del dominio:
 * La REINA se compone de: new MovimientoCompuesto(new MovimientoOrtogonal(), new MovimientoDiagonal()).
 */
public class MovimientoCompuesto implements EstrategiaMovimiento {
    private final List<EstrategiaMovimiento> estrategias;

    public MovimientoCompuesto(EstrategiaMovimiento... estrategias) {
        this.estrategias = new ArrayList<>(Arrays.asList(estrategias));
    }

    public MovimientoCompuesto(List<EstrategiaMovimiento> estrategias) {
        this.estrategias = new ArrayList<>(estrategias);
    }

    public void agregarEstrategia(EstrategiaMovimiento estrategia) {
        this.estrategias.add(estrategia);
    }

    @Override
    public List<Posicion> calcularPosiblesDestinos(Posicion origen, Tablero tablero) {
        List<Posicion> destinosTotales = new ArrayList<>();
        for (EstrategiaMovimiento estrategia : estrategias) {
            List<Posicion> destinosParciales = estrategia.calcularPosiblesDestinos(origen, tablero);
            for (Posicion pos : destinosParciales) {
                if (!destinosTotales.contains(pos)) {
                    destinosTotales.add(pos);
                }
            }
        }
        return destinosTotales;
    }
}

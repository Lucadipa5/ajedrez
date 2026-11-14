package ajedrez.nucleo.fabrica;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.TipoPieza;
import ajedrez.nucleo.estrategias.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Patrón Factory: centraliza la creación de piezas y el ensamblado de su estrategia de movimiento.
 *
 * Open/Closed: en vez de un switch por tipo (que habría que reabrir con cada pieza nueva),
 * mantiene un registro tipo → estrategia. Agregar o cambiar una pieza es registrar una
 * estrategia nueva; ninguna clase existente se modifica.
 *
 * Composición sobre herencia: la Reina no es una subclase con código copiado, es la
 * composición de las estrategias ortogonal y diagonal. El Rey compone su paso simple con el enroque.
 */
public class FabricaPiezas {

    private final Map<TipoPieza, Supplier<EstrategiaMovimiento>> estrategias = new EnumMap<>(TipoPieza.class);

    /** Fábrica con las 6 piezas del ajedrez estándar. */
    public FabricaPiezas() {
        registrar(TipoPieza.PEON, MovimientoPeon::new);
        registrar(TipoPieza.TORRE, MovimientoOrtogonal::new);
        registrar(TipoPieza.CABALLO, MovimientoCaballo::new);
        registrar(TipoPieza.ALFIL, MovimientoDiagonal::new);
        registrar(TipoPieza.REINA, () -> new MovimientoCompuesto(new MovimientoOrtogonal(), new MovimientoDiagonal()));
        registrar(TipoPieza.REY, () -> new MovimientoCompuesto(new MovimientoRey(), new MovimientoEnroque()));
    }

    /**
     * Registra (o reemplaza) cómo se mueve un tipo de pieza. Punto de extensión para variantes.
     */
    public void registrar(TipoPieza tipo, Supplier<EstrategiaMovimiento> estrategia) {
        estrategias.put(tipo, estrategia);
    }

    public Pieza crearPieza(TipoPieza tipo, Color color) {
        Supplier<EstrategiaMovimiento> estrategia = estrategias.get(tipo);
        if (estrategia == null) {
            throw new IllegalArgumentException("No hay estrategia de movimiento registrada para " + tipo);
        }
        return new Pieza(tipo, color, estrategia.get());
    }

    // Atajos de lectura para los tipos estándar
    public Pieza crearPeon(Color color)    { return crearPieza(TipoPieza.PEON, color); }
    public Pieza crearTorre(Color color)   { return crearPieza(TipoPieza.TORRE, color); }
    public Pieza crearCaballo(Color color) { return crearPieza(TipoPieza.CABALLO, color); }
    public Pieza crearAlfil(Color color)   { return crearPieza(TipoPieza.ALFIL, color); }
    public Pieza crearReina(Color color)   { return crearPieza(TipoPieza.REINA, color); }
    public Pieza crearRey(Color color)     { return crearPieza(TipoPieza.REY, color); }
}

package ajedrez.nucleo.reglas;

import ajedrez.nucleo.comandos.FabricaComandos;
import ajedrez.nucleo.fabrica.FabricaPiezas;

import java.util.List;
import java.util.Objects;

/**
 * El reglamento con el que se juega: qué piezas existen y cómo se mueven (fábrica), cómo se
 * aplica una jugada (fábrica de comandos), qué es legal (validador) y cuándo hay tablas (reglas).
 *
 * Agruparlo evita que la partida reciba cinco dependencias sueltas, y permite armar
 * reglamentos distintos (sin regla de 50 movimientos, con otra pieza, otro tablero) para
 * variantes y tests, inyectándolos por constructor.
 */
public class Reglamento {
    private final FabricaPiezas fabricaPiezas;
    private final FabricaComandos fabricaComandos;
    private final ValidadorMovimiento validador;
    private final List<ReglaDeTablas> reglasDeTablas;

    public Reglamento(FabricaPiezas fabricaPiezas, FabricaComandos fabricaComandos,
                      ValidadorMovimiento validador, List<ReglaDeTablas> reglasDeTablas) {
        this.fabricaPiezas = Objects.requireNonNull(fabricaPiezas);
        this.fabricaComandos = Objects.requireNonNull(fabricaComandos);
        this.validador = Objects.requireNonNull(validador);
        this.reglasDeTablas = List.copyOf(reglasDeTablas);
    }

    /** Reglamento del ajedrez estándar con las piezas de siempre. */
    public static Reglamento estandar() {
        return estandar(new FabricaPiezas());
    }

    /** Reglamento estándar sobre una fábrica de piezas dada (por ejemplo, con una pieza nueva registrada). */
    public static Reglamento estandar(FabricaPiezas fabricaPiezas) {
        FabricaComandos fabricaComandos = new FabricaComandos(fabricaPiezas);
        ValidadorMovimiento validador = new ValidadorMovimiento(new DetectorJaque(), fabricaComandos);
        return new Reglamento(fabricaPiezas, fabricaComandos, validador, List.of(
                new ReglaMaterialInsuficiente(),
                new ReglaCincuentaMovimientos(),
                new ReglaRepeticionTriple()));
    }

    public FabricaPiezas getFabricaPiezas() { return fabricaPiezas; }
    public FabricaComandos getFabricaComandos() { return fabricaComandos; }
    public ValidadorMovimiento getValidador() { return validador; }
    public DetectorJaque getDetectorJaque() { return validador.getDetectorJaque(); }
    public List<ReglaDeTablas> getReglasDeTablas() { return reglasDeTablas; }
}

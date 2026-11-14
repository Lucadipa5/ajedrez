package ajedrez.adaptadores.ia;

import ajedrez.nucleo.ia.DecisionIA;
import ajedrez.nucleo.ia.RegistroDecisionesIA;

import java.io.PrintStream;

/**
 * Adaptador de salida: anota por consola cada decisión de la IA (qué sugirió, qué se jugó y por qué).
 */
public class RegistroDecisionesConsola implements RegistroDecisionesIA {
    private final PrintStream salida;

    public RegistroDecisionesConsola() {
        this(System.out);
    }

    public RegistroDecisionesConsola(PrintStream salida) {
        this.salida = salida;
    }

    @Override
    public void registrar(DecisionIA d) {
        salida.println("[ia] " + d.motor() + " sugirió=" + (d.sugerida() != null ? d.sugerida() : "-")
                + " jugada=" + d.jugada() + " resultado=" + d.resultado());
    }
}

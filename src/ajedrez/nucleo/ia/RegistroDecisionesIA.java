package ajedrez.nucleo.ia;

/**
 * Puerto de salida: dónde se anotan las decisiones de la IA (consola, archivo, memoria).
 * El núcleo registra; el adaptador decide el destino.
 */
@FunctionalInterface
public interface RegistroDecisionesIA {
    /** Registro que descarta todo, para cuando no interesa auditar. */
    RegistroDecisionesIA NULO = decision -> {};

    void registrar(DecisionIA decision);
}

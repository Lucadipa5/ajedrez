package ajedrez.nucleo.ia;

import ajedrez.nucleo.dominio.Jugada;

/**
 * Registro de una decisión de la IA: qué sugirió el motor, qué se jugó finalmente y por qué
 * (aceptada, o respaldo por jugada ilegal, sin sugerencia o error). Sirve para auditar.
 */
public record DecisionIA(String posicion, String motor, Jugada sugerida, Jugada jugada, String resultado) {

    public static final String ACEPTADA = "aceptada";

    /** La jugada final no fue la del motor: se usó una jugada legal de respaldo. */
    public boolean usoRespaldo() {
        return !ACEPTADA.equals(resultado);
    }
}

package ajedrez.nucleo.dominio;

/**
 * Enumera los 6 tipos de piezas estándar del ajedrez y sus símbolos Unicode.
 */
public enum TipoPieza {
    PEON("Peón", 'P', "♙", "♟"),
    TORRE("Torre", 'R', "♖", "♜"),
    CABALLO("Caballo", 'N', "♘", "♞"),
    ALFIL("Alfil", 'B', "♗", "♝"),
    REINA("Reina", 'Q', "♕", "♛"),
    REY("Rey", 'K', "♔", "♚");

    private final String nombre;
    private final char simboloAlgebraico;
    private final String simboloBlanco;
    private final String simboloNegro;

    TipoPieza(String nombre, char simboloAlgebraico, String simboloBlanco, String simboloNegro) {
        this.nombre = nombre;
        this.simboloAlgebraico = simboloAlgebraico;
        this.simboloBlanco = simboloBlanco;
        this.simboloNegro = simboloNegro;
    }

    public String getNombre() {
        return nombre;
    }

    public char getSimboloAlgebraico() {
        return simboloAlgebraico;
    }

    public String getSimboloUnicode(Color color) {
        return color == Color.BLANCO ? simboloBlanco : simboloNegro;
    }
}

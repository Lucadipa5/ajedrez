package ajedrez.nucleo.dominio;

/**
 * Los 6 tipos de piezas del ajedrez estándar.
 *
 * Solo guarda lo que es del dominio: el nombre y la letra de la notación algebraica.
 * Cómo se dibuja cada pieza (imagen, símbolo Unicode, color) es asunto de cada adaptador de interfaz.
 */
public enum TipoPieza {
    PEON("Peón", 'P'),
    TORRE("Torre", 'R'),
    CABALLO("Caballo", 'N'),
    ALFIL("Alfil", 'B'),
    REINA("Reina", 'Q'),
    REY("Rey", 'K');

    private final String nombre;
    private final char simboloAlgebraico;

    TipoPieza(String nombre, char simboloAlgebraico) {
        this.nombre = nombre;
        this.simboloAlgebraico = simboloAlgebraico;
    }

    public String getNombre() {
        return nombre;
    }

    /** Letra en notación algebraica inglesa (la que usan FEN y UCI): P, R, N, B, Q, K. */
    public char getSimboloAlgebraico() {
        return simboloAlgebraico;
    }
}

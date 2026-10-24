package ajedrez.nucleo.dominio;

import java.util.Objects;

/**
 * Representa una coordenada inmutable dentro del tablero de ajedrez (fila y columna de 0 a 7).
 * Actúa como Value Object: su identidad está determinada exclusivamente por sus valores (fila, columna).
 */
public final class Posicion {
    private final int fila;
    private final int columna;

    public Posicion(int fila, int columna) {
        this.fila = fila;
        this.columna = columna;
    }

    public int getFila() {
        return fila;
    }

    public int getColumna() {
        return columna;
    }

    public boolean esValida() {
        return fila >= 0 && fila < 8 && columna >= 0 && columna < 8;
    }

    public Posicion desplazar(int deltaFila, int deltaColumna) {
        return new Posicion(this.fila + deltaFila, this.columna + deltaColumna);
    }

    /**
     * Retorna la notación algebraica estándar del ajedrez (ej: a1, e4, h8).
     */
    public String aNotacionAlgebraica() {
        if (!esValida()) return "invalida";
        char colChar = (char) ('a' + columna);
        int filaNum = 8 - fila;
        return "" + colChar + filaNum;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Posicion otra = (Posicion) obj;
        return fila == otra.fila && columna == otra.columna;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fila, columna);
    }

    @Override
    public String toString() {
        return "(" + fila + ", " + columna + ")";
    }
}

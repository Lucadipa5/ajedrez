package ajedrez.nucleo.dominio;

import java.util.Objects;

/**
 * Coordenada inmutable dentro del tablero (fila y columna, desde 0).
 * Es un Value Object: su identidad la dan exclusivamente sus valores.
 *
 * No sabe el tamaño del tablero: si una posición "entra" o no lo decide el {@code Tablero},
 * así el mismo núcleo sirve para tableros de cualquier dimensión.
 */
public final class Posicion {
    /** Cantidad de filas del tablero estándar; se usa solo para la notación algebraica clásica (a1..h8). */
    private static final int FILAS_ESTANDAR = 8;

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

    public Posicion desplazar(int deltaFila, int deltaColumna) {
        return new Posicion(this.fila + deltaFila, this.columna + deltaColumna);
    }

    /**
     * Notación algebraica estándar (a1..h8) para un tablero de 8 filas: la fila 0 del arreglo es la 8.
     */
    public String aNotacionAlgebraica() {
        return aNotacionAlgebraica(FILAS_ESTANDAR);
    }

    /** Notación algebraica para un tablero con la cantidad de filas indicada. */
    public String aNotacionAlgebraica(int filasDelTablero) {
        return "" + (char) ('a' + columna) + (filasDelTablero - fila);
    }

    /**
     * Interpreta una casilla en notación algebraica estándar ("e4"). Devuelve null si no es válida.
     */
    public static Posicion desdeAlgebraica(String casilla) {
        if (casilla == null || casilla.length() != 2) return null;
        char columna = Character.toLowerCase(casilla.charAt(0));
        char fila = casilla.charAt(1);
        if (columna < 'a' || columna > 'h' || fila < '1' || fila > '8') return null;
        return new Posicion(FILAS_ESTANDAR - (fila - '0'), columna - 'a');
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

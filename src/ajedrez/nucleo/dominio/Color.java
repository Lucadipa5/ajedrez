package ajedrez.nucleo.dominio;

/**
 * Color de una pieza y del jugador en turno.
 */
public enum Color {
    BLANCO("Blanco"),
    NEGRO("Negro");

    private final String descripcion;

    Color(String descripcion) {
        this.descripcion = descripcion;
    }

    public Color opuesto() {
        return this == BLANCO ? NEGRO : BLANCO;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}

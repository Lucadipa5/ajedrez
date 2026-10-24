package ajedrez.nucleo.ia;

/**
 * Niveles de dificultad de la IA.
 */
public enum Dificultad {
    FACIL("Fácil"),
    MEDIA("Media"),
    DIFICIL("Difícil");

    private final String nombre;

    Dificultad(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}

package ajedrez.tests;

import ajedrez.nucleo.dominio.Posicion;

/**
 * Mini framework de aserciones: sin dependencias externas, para que los tests del núcleo
 * corran con un `java` pelado. Cada aserción cuenta como un test.
 */
final class Afirmar {
    private static int pasados = 0;
    private static int totales = 0;

    private Afirmar() {}

    static void que(boolean condicion, String nombreTest) {
        totales++;
        if (condicion) {
            pasados++;
            System.out.println("  [PASS] " + nombreTest);
        } else {
            System.err.println("  [FAIL] " + nombreTest);
            throw new AssertionError("Fallo en: " + nombreTest);
        }
    }

    static void seccion(String titulo) {
        System.out.println();
        System.out.println("--- " + titulo + " ---");
    }

    /** Casilla en notación algebraica estándar: p("e4"). */
    static Posicion p(String casilla) {
        Posicion pos = Posicion.desdeAlgebraica(casilla);
        if (pos == null) throw new IllegalArgumentException("Casilla inválida en el test: " + casilla);
        return pos;
    }

    static int pasados() { return pasados; }
    static int totales() { return totales; }
}

package ajedrez.tests;

/**
 * Suite de pruebas unitarias del núcleo, sin infraestructura externa: no hay ventanas,
 * archivos ni procesos. Cada suite agrupa tests de una parte del núcleo y los escribe
 * en formato Arrange-Act-Assert.
 */
public class TestRunner {

    public static void main(String[] args) {
        System.out.println("=======================================================");
        System.out.println("  EJECUTANDO TESTS UNITARIOS DEL NÚCLEO DE AJEDREZ");
        System.out.println("=======================================================");

        try {
            TestsTableroYPiezas.correr();
            TestsReglas.correr();
            TestsPartida.correr();
            TestsNotacion.correr();
            TestsCuentasYSesion.correr();
        } catch (AssertionError e) {
            System.out.println();
            System.out.println("=======================================================");
            System.out.println(" RESULTADO: " + Afirmar.pasados() + "/" + Afirmar.totales() + " tests pasaron. " + e.getMessage());
            System.out.println("=======================================================");
            System.exit(1);
        }

        System.out.println();
        System.out.println("=======================================================");
        System.out.println(" RESULTADO: " + Afirmar.pasados() + "/" + Afirmar.totales() + " tests pasaron con éxito.");
        System.out.println("=======================================================");
    }
}

package ajedrez.nucleo.ia;

/**
 * El motor de IA no está disponible o falló al calcular una jugada.
 */
public class MotorNoDisponibleException extends Exception {
    public MotorNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public MotorNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

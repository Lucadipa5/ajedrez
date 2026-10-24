package ajedrez.nucleo.ia;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Jugada;
import ajedrez.nucleo.tablero.Tablero;

/**
 * Puerto de Salida (Driven Port) para un motor de inteligencia artificial de ajedrez.
 * El núcleo solo pide "elegí una jugada para esta posición"; quién la calcula
 * (un script de Python, un motor externo, etc.) lo decide el adaptador.
 */
public interface MotorAjedrez {

    /** Indica si el motor puede usarse (por ejemplo, si están instaladas sus dependencias). */
    boolean estaDisponible();

    /** Mensaje para el usuario explicando por qué el motor no está disponible. */
    String motivoNoDisponible();

    /**
     * Calcula una jugada para el color en turno.
     * @throws MotorNoDisponibleException si el motor falla o no responde
     */
    Jugada elegirJugada(Tablero tablero, Color turno, Dificultad dificultad) throws MotorNoDisponibleException;

    /** Libera los recursos del motor (procesos, conexiones). */
    void cerrar();
}

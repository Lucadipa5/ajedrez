package ajedrez.nucleo.reglas;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.tablero.Tablero;

/**
 * Una condición de tablas (patrón Strategy). La partida consulta la lista de reglas que recibió
 * después de cada jugada; agregar una condición nueva (regla de los 75 movimientos, por ejemplo)
 * es escribir otra implementación y sumarla a la lista, sin tocar la partida ni las demás reglas.
 *
 * El ahogado no es una de estas reglas: es la ausencia de jugadas legales y la partida lo detecta sola.
 */
@FunctionalInterface
public interface ReglaDeTablas {
    /**
     * @param colorEnTurno a quién le toca mover en la posición que se evalúa
     * @return el motivo de tablas si la regla aplica, o null si no
     */
    MotivoTablas evaluar(Tablero tablero, Color colorEnTurno, HistorialPosiciones historial);
}

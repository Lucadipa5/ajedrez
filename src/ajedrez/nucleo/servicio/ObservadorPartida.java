package ajedrez.nucleo.servicio;

import ajedrez.nucleo.dominio.EventoJugada;

/**
 * Patrón Observer (puerto de salida): la partida avisa qué pasó y quien quiera escucha,
 * sin que el núcleo sepa quiénes son. Hoy escuchan la lista de jugadas de la ventana y un
 * registro por consola; mañana podría ser un archivo PGN, un sonido o una red.
 *
 * Todos los métodos tienen una implementación vacía para que cada observador declare
 * solo los eventos que le interesan (segregación de interfaces).
 *
 * Regla para evitar fugas de memoria: el que se suscribe, se desuscribe ({@link PartidaAjedrez#quitarObservador}).
 */
public interface ObservadorPartida {
    default void alMover(EventoJugada evento) {}

    default void alDeshacer(EventoJugada evento) {}

    default void alRehacer(EventoJugada evento) {}

    default void alReiniciar(PartidaAjedrez partida) {}
}

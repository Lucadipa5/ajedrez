package ajedrez.nucleo.dominio;

/**
 * Lo que pasó al aplicar una jugada. Es lo que el núcleo publica a sus observadores
 * (patrón Observer) y lo que devuelve la partida al adaptador que pidió mover.
 *
 * Es un dato puro: no referencia ninguna clase de interfaz ni de infraestructura.
 *
 * @param numeroDeMediaJugada 1 para la primera jugada de las blancas, 2 para la respuesta de las negras, etc.
 */
public record EventoJugada(
        Jugada jugada,
        Pieza piezaMovida,
        Pieza piezaCapturada,
        boolean enroque,
        boolean capturaAlPaso,
        TipoPieza promocion,
        boolean jaqueAlRival,
        boolean finDePartida,
        int numeroDeMediaJugada) {

    public boolean esCaptura() {
        return piezaCapturada != null;
    }

    public boolean esMate() {
        return finDePartida && jaqueAlRival;
    }

    public Color colorQueMovio() {
        return piezaMovida.getColor();
    }
}

package ajedrez.nucleo.dominio;

/**
 * Describe una jugada en notación algebraica abreviada (SAN simplificada, sin desambiguación):
 * "e4", "Nf3", "exd5", "O-O", "e8=Q", "Qh5+", "Qxf7#".
 * Es conocimiento del dominio (como FEN o UCI), no de la interfaz: cualquier adaptador
 * (ventana, consola, archivo de registro) muestra las jugadas con el mismo texto.
 */
public final class NotacionAlgebraica {

    private NotacionAlgebraica() {}

    public static String describir(EventoJugada evento) {
        StringBuilder sb = new StringBuilder();
        Jugada jugada = evento.jugada();
        if (evento.enroque()) {
            sb.append(jugada.deltaColumna() > 0 ? "O-O" : "O-O-O");
        } else {
            TipoPieza tipo = evento.piezaMovida().getTipo();
            if (tipo == TipoPieza.PEON) {
                if (evento.esCaptura()) sb.append((char) ('a' + jugada.getOrigen().getColumna()));
            } else {
                sb.append(tipo.getSimboloAlgebraico());
            }
            if (evento.esCaptura()) sb.append('x');
            sb.append(jugada.getDestino().aNotacionAlgebraica());
            if (evento.capturaAlPaso()) sb.append(" e.p.");
            if (evento.promocion() != null) sb.append('=').append(evento.promocion().getSimboloAlgebraico());
        }
        if (evento.esMate()) {
            sb.append('#');
        } else if (evento.jaqueAlRival()) {
            sb.append('+');
        }
        return sb.toString();
    }
}

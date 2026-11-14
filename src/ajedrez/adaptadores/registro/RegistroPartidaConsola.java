package ajedrez.adaptadores.registro;

import ajedrez.nucleo.dominio.EventoJugada;
import ajedrez.nucleo.dominio.NotacionAlgebraica;
import ajedrez.nucleo.servicio.ObservadorPartida;
import ajedrez.nucleo.servicio.PartidaAjedrez;

import java.io.PrintStream;

/**
 * Adaptador de salida: escribe la partida por consola a medida que se juega.
 * Es un observador más del núcleo, igual que la lista de jugadas de la ventana:
 * el mismo evento, dos adaptadores, y la partida no conoce a ninguno.
 */
public class RegistroPartidaConsola implements ObservadorPartida {
    private final PrintStream salida;

    public RegistroPartidaConsola() {
        this(System.out);
    }

    public RegistroPartidaConsola(PrintStream salida) {
        this.salida = salida;
    }

    @Override
    public void alMover(EventoJugada evento) {
        salida.println("[partida] " + numerar(evento) + NotacionAlgebraica.describir(evento)
                + (evento.finDePartida() ? "  (fin de la partida)" : ""));
    }

    @Override
    public void alDeshacer(EventoJugada evento) {
        if (evento != null) salida.println("[partida] se deshizo " + numerar(evento) + NotacionAlgebraica.describir(evento));
    }

    @Override
    public void alRehacer(EventoJugada evento) {
        salida.println("[partida] se rehizo " + numerar(evento) + NotacionAlgebraica.describir(evento));
    }

    @Override
    public void alReiniciar(PartidaAjedrez partida) {
        salida.println("[partida] nueva partida (" + partida.getTablero().getFilas() + "x" + partida.getTablero().getColumnas() + ")");
    }

    private static String numerar(EventoJugada evento) {
        int numero = (evento.numeroDeMediaJugada() + 1) / 2;
        return numero + (evento.numeroDeMediaJugada() % 2 == 1 ? ". " : "... ");
    }
}

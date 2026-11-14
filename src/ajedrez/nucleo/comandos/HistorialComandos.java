package ajedrez.nucleo.comandos;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Invoker del patrón Command.
 * Mantiene dos pilas para gestionar el historial de operaciones reversibles (Deshacer y Rehacer).
 */
public class HistorialComandos {
    private final Deque<Comando> pilaDeshacer = new ArrayDeque<>();
    private final Deque<Comando> pilaRehacer = new ArrayDeque<>();

    public void ejecutarComando(Comando comando) {
        comando.ejecutar();
        pilaDeshacer.push(comando);
        pilaRehacer.clear(); // una nueva acción invalida el árbol de rehacer
    }

    public boolean puedeDeshacer() {
        return !pilaDeshacer.isEmpty();
    }

    public boolean puedeRehacer() {
        return !pilaRehacer.isEmpty();
    }

    /** Deshace el último comando y lo devuelve (null si no había nada para deshacer). */
    public Comando deshacer() {
        if (!puedeDeshacer()) return null;
        Comando cmd = pilaDeshacer.pop();
        cmd.deshacer();
        pilaRehacer.push(cmd);
        return cmd;
    }

    /** Rehace el último comando deshecho y lo devuelve (null si no había nada para rehacer). */
    public Comando rehacer() {
        if (!puedeRehacer()) return null;
        Comando cmd = pilaRehacer.pop();
        cmd.ejecutar();
        pilaDeshacer.push(cmd);
        return cmd;
    }

    /** Último comando ejecutado (el que se desharía), o null. */
    public Comando ultimo() {
        return pilaDeshacer.peek();
    }

    public void limpiar() {
        pilaDeshacer.clear();
        pilaRehacer.clear();
    }

    public int cantidadComandosEjecutados() {
        return pilaDeshacer.size();
    }
}

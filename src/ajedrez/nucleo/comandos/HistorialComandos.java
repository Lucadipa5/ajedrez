package ajedrez.nucleo.comandos;

import java.util.Stack;

/**
 * Invoker del patrón Command.
 * Mantiene dos pilas para gestionar el historial de operaciones reversibles (Deshacer y Rehacer).
 */
public class HistorialComandos {
    private final Stack<Comando> pilaDeshacer = new Stack<>();
    private final Stack<Comando> pilaRehacer = new Stack<>();

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

    public boolean deshacer() {
        if (!puedeDeshacer()) return false;
        Comando cmd = pilaDeshacer.pop();
        cmd.deshacer();
        pilaRehacer.push(cmd);
        return true;
    }

    public boolean rehacer() {
        if (!puedeRehacer()) return false;
        Comando cmd = pilaRehacer.pop();
        cmd.ejecutar();
        pilaDeshacer.push(cmd);
        return true;
    }

    public void limpiar() {
        pilaDeshacer.clear();
        pilaRehacer.clear();
    }

    public int cantidadComandosEjecutados() {
        return pilaDeshacer.size();
    }
}

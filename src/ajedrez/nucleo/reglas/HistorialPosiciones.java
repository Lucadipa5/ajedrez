package ajedrez.nucleo.reglas;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Cuántas veces apareció cada posición en la partida (para la regla de repetición).
 * Las claves las produce {@code Tablero.claveDePosicion}; buscar en el mapa es O(1).
 */
public class HistorialPosiciones {
    private final Map<String, Integer> repeticiones = new HashMap<>();
    private final Deque<String> secuencia = new ArrayDeque<>();

    public void registrar(String clave) {
        secuencia.push(clave);
        repeticiones.merge(clave, 1, Integer::sum);
    }

    /** Quita la última posición registrada (al deshacer una jugada). */
    public void quitarUltima() {
        if (secuencia.isEmpty()) return;
        String clave = secuencia.pop();
        repeticiones.computeIfPresent(clave, (k, n) -> n > 1 ? n - 1 : null);
    }

    public int repeticionesDe(String clave) {
        return repeticiones.getOrDefault(clave, 0);
    }

    public int cantidad() {
        return secuencia.size();
    }

    public void limpiar() {
        repeticiones.clear();
        secuencia.clear();
    }
}

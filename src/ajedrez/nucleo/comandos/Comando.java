package ajedrez.nucleo.comandos;

/**
 * Interfaz Command (Patrón de Comportamiento).
 * Encapsula una acción sobre el juego para posibilitar su ejecución y reversión (Undo/Redo).
 */
public interface Comando {
    void ejecutar();
    void deshacer();
}

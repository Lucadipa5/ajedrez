package ajedrez.nucleo.comandos;

/**
 * Interfaz Command (Patrón de Comportamiento).
 * Encapsula una acción sobre el juego para posibilitar su ejecución y reversión (Undo/Redo).
 * Basado directamente en la estructura pedagógica vista en clase.
 */
public interface Comando {
    void ejecutar();
    void deshacer();
}

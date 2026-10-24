package ajedrez.nucleo.perfil;

import java.util.List;

/**
 * Puerto de Salida (Driven Port) para la persistencia de perfiles.
 * El núcleo no sabe si los perfiles se guardan en un archivo, una base de datos o en memoria:
 * eso lo decide el adaptador que implemente esta interfaz.
 */
public interface RepositorioPerfiles {
    /** Devuelve todos los perfiles guardados (lista vacía si no hay ninguno). */
    List<PerfilJugador> cargarTodos();

    /** Reemplaza todos los perfiles guardados por los de la lista. */
    void guardarTodos(List<PerfilJugador> perfiles);
}

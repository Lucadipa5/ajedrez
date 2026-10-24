package ajedrez.adaptadores.ui.swing.perfil;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de perfiles: guarda y carga perfiles en disco usando serialización Java.
 * El archivo se guarda en la misma carpeta del proyecto como "perfiles.dat".
 */
public class GestorPerfiles {
    private static final String ARCHIVO_PERFILES = "perfiles.dat";

    /**
     * Guarda la lista completa de perfiles en disco.
     */
    @SuppressWarnings("unchecked")
    public static void guardarPerfiles(List<PerfilJugador> perfiles) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(ARCHIVO_PERFILES))) {
            oos.writeObject(perfiles);
        } catch (IOException e) {
            System.err.println("[GestorPerfiles] Error al guardar perfiles: " + e.getMessage());
        }
    }

    /**
     * Carga la lista de perfiles desde disco. Si el archivo no existe, devuelve lista vacía.
     */
    @SuppressWarnings("unchecked")
    public static List<PerfilJugador> cargarPerfiles() {
        File archivo = new File(ARCHIVO_PERFILES);
        if (!archivo.exists()) {
            return new ArrayList<>();
        }
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(archivo))) {
            return (List<PerfilJugador>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[GestorPerfiles] Error al cargar perfiles: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Agrega un nuevo perfil y lo persiste en disco.
     */
    public static PerfilJugador crearPerfil(String nombre) {
        List<PerfilJugador> perfiles = cargarPerfiles();
        PerfilJugador nuevo = new PerfilJugador(nombre);
        perfiles.add(nuevo);
        guardarPerfiles(perfiles);
        return nuevo;
    }

    /**
     * Actualiza (reemplaza) un perfil existente por nombre y persiste.
     */
    public static void actualizarPerfil(PerfilJugador perfil) {
        List<PerfilJugador> perfiles = cargarPerfiles();
        for (int i = 0; i < perfiles.size(); i++) {
            if (perfiles.get(i).getNombre().equals(perfil.getNombre())) {
                perfiles.set(i, perfil);
                guardarPerfiles(perfiles);
                return;
            }
        }
        // Si no existe, lo agrega
        perfiles.add(perfil);
        guardarPerfiles(perfiles);
    }
}

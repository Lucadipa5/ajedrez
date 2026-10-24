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
     * Busca un perfil por nombre de usuario (sin distinguir mayúsculas). Devuelve null si no existe.
     */
    private static PerfilJugador buscarPorNombre(List<PerfilJugador> perfiles, String nombre) {
        for (PerfilJugador p : perfiles) {
            if (p.getNombre().equalsIgnoreCase(nombre)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Vuelve a leer desde disco el perfil guardado, descartando cambios en memoria.
     */
    public static PerfilJugador recargarPerfil(PerfilJugador perfil) {
        PerfilJugador guardado = buscarPorNombre(cargarPerfiles(), perfil.getNombre());
        return guardado != null ? guardado : perfil;
    }

    public static boolean existeUsuario(String nombre) {
        return buscarPorNombre(cargarPerfiles(), nombre) != null;
    }

    /**
     * Registra una cuenta nueva con usuario y contraseña y la persiste en disco.
     * @return el perfil creado, o null si el nombre de usuario ya está en uso
     */
    public static PerfilJugador registrar(String nombre, char[] contrasena) {
        List<PerfilJugador> perfiles = cargarPerfiles();
        if (buscarPorNombre(perfiles, nombre) != null) {
            return null;
        }
        PerfilJugador nuevo = new PerfilJugador(nombre);
        nuevo.establecerContrasena(contrasena);
        perfiles.add(nuevo);
        guardarPerfiles(perfiles);
        return nuevo;
    }

    /**
     * Inicia sesión verificando la contraseña.
     * @return el perfil si las credenciales son correctas, o null si no lo son
     */
    public static PerfilJugador iniciarSesion(String nombre, char[] contrasena) {
        PerfilJugador perfil = buscarPorNombre(cargarPerfiles(), nombre);
        if (perfil == null || !perfil.verificarContrasena(contrasena)) {
            return null;
        }
        return perfil;
    }

    /**
     * Indica si el usuario existe pero es una cuenta antigua creada antes del sistema
     * de contraseñas (todavía no tiene contraseña asignada).
     */
    public static boolean esCuentaSinContrasena(String nombre) {
        PerfilJugador perfil = buscarPorNombre(cargarPerfiles(), nombre);
        return perfil != null && !perfil.tieneContrasena();
    }

    /**
     * Asigna contraseña a una cuenta antigua que no tenía. No hace nada si ya tenía una.
     * @return el perfil actualizado, o null si no existe o ya tenía contraseña
     */
    public static PerfilJugador asignarContrasenaACuentaAntigua(String nombre, char[] contrasena) {
        List<PerfilJugador> perfiles = cargarPerfiles();
        PerfilJugador perfil = buscarPorNombre(perfiles, nombre);
        if (perfil == null || perfil.tieneContrasena()) {
            return null;
        }
        perfil.establecerContrasena(contrasena);
        guardarPerfiles(perfiles);
        return perfil;
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

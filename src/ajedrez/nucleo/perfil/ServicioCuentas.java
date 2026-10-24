package ajedrez.nucleo.perfil;

import java.util.List;
import java.util.Objects;

/**
 * Servicio del núcleo para el sistema de cuentas: registro, inicio de sesión y guardado del progreso.
 * Depende solo del puerto {@link RepositorioPerfiles}; la forma concreta de guardar
 * los datos la aporta un adaptador de persistencia.
 */
public class ServicioCuentas {
    private final RepositorioPerfiles repositorio;

    public ServicioCuentas(RepositorioPerfiles repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio);
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
     * Registra una cuenta nueva con usuario y contraseña y la persiste.
     * @return el perfil creado, o null si el nombre de usuario ya está en uso
     */
    public PerfilJugador registrar(String nombre, char[] contrasena) {
        List<PerfilJugador> perfiles = repositorio.cargarTodos();
        if (buscarPorNombre(perfiles, nombre) != null) {
            return null;
        }
        PerfilJugador nuevo = new PerfilJugador(nombre);
        nuevo.establecerContrasena(contrasena);
        perfiles.add(nuevo);
        repositorio.guardarTodos(perfiles);
        return nuevo;
    }

    /**
     * Inicia sesión verificando la contraseña.
     * @return el perfil si las credenciales son correctas, o null si no lo son
     */
    public PerfilJugador iniciarSesion(String nombre, char[] contrasena) {
        PerfilJugador perfil = buscarPorNombre(repositorio.cargarTodos(), nombre);
        if (perfil == null || !perfil.verificarContrasena(contrasena)) {
            return null;
        }
        return perfil;
    }

    /**
     * Indica si el usuario existe pero es una cuenta antigua creada antes del sistema
     * de contraseñas (todavía no tiene contraseña asignada).
     */
    public boolean esCuentaSinContrasena(String nombre) {
        PerfilJugador perfil = buscarPorNombre(repositorio.cargarTodos(), nombre);
        return perfil != null && !perfil.tieneContrasena();
    }

    /**
     * Asigna contraseña a una cuenta antigua que no tenía. No hace nada si ya tenía una.
     * @return el perfil actualizado, o null si no existe o ya tenía contraseña
     */
    public PerfilJugador asignarContrasenaACuentaAntigua(String nombre, char[] contrasena) {
        List<PerfilJugador> perfiles = repositorio.cargarTodos();
        PerfilJugador perfil = buscarPorNombre(perfiles, nombre);
        if (perfil == null || perfil.tieneContrasena()) {
            return null;
        }
        perfil.establecerContrasena(contrasena);
        repositorio.guardarTodos(perfiles);
        return perfil;
    }

    /**
     * Vuelve a leer el perfil guardado, descartando cambios en memoria.
     */
    public PerfilJugador recargar(PerfilJugador perfil) {
        PerfilJugador guardado = buscarPorNombre(repositorio.cargarTodos(), perfil.getNombre());
        return guardado != null ? guardado : perfil;
    }

    /**
     * Guarda el progreso del perfil, reemplazando la versión anterior.
     */
    public void guardar(PerfilJugador perfil) {
        List<PerfilJugador> perfiles = repositorio.cargarTodos();
        for (int i = 0; i < perfiles.size(); i++) {
            if (perfiles.get(i).getNombre().equalsIgnoreCase(perfil.getNombre())) {
                perfiles.set(i, perfil);
                repositorio.guardarTodos(perfiles);
                return;
            }
        }
        perfiles.add(perfil);
        repositorio.guardarTodos(perfiles);
    }
}

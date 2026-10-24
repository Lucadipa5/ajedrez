package ajedrez.adaptadores.persistencia;

import ajedrez.nucleo.perfil.PerfilJugador;
import ajedrez.nucleo.perfil.RepositorioPerfiles;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador de persistencia: guarda los perfiles en un archivo usando serialización Java.
 * Implementa el puerto {@link RepositorioPerfiles} del núcleo.
 */
public class RepositorioPerfilesArchivo implements RepositorioPerfiles {

    // Antes PerfilJugador vivía en la capa de UI; los archivos viejos guardan ese nombre de clase
    private static final String NOMBRE_CLASE_ANTERIOR = "ajedrez.adaptadores.ui.swing.perfil.PerfilJugador";

    private final File archivo;

    public RepositorioPerfilesArchivo(String rutaArchivo) {
        this.archivo = new File(rutaArchivo);
    }

    @Override
    public void guardarTodos(List<PerfilJugador> perfiles) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(archivo))) {
            oos.writeObject(new ArrayList<>(perfiles));
        } catch (IOException e) {
            System.err.println("[RepositorioPerfilesArchivo] Error al guardar perfiles: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PerfilJugador> cargarTodos() {
        if (!archivo.exists()) {
            return new ArrayList<>();
        }
        try {
            byte[] datos = actualizarNombreDeClase(Files.readAllBytes(archivo.toPath()));
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(datos))) {
                return (List<PerfilJugador>) ois.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[RepositorioPerfilesArchivo] Error al cargar perfiles: " + e);
            return new ArrayList<>();
        }
    }

    /**
     * Los archivos viejos guardan el nombre de clase anterior. En la serialización Java el nombre
     * se escribe como [largo de 2 bytes][texto], así que se reemplaza ese bloque por el nombre actual.
     */
    private static byte[] actualizarNombreDeClase(byte[] datos) {
        byte[] viejo = bloqueNombre(NOMBRE_CLASE_ANTERIOR);
        byte[] nuevo = bloqueNombre(PerfilJugador.class.getName());
        ByteArrayOutputStream salida = new ByteArrayOutputStream(datos.length);
        int i = 0;
        while (i < datos.length) {
            if (coincide(datos, i, viejo)) {
                salida.write(nuevo, 0, nuevo.length);
                i += viejo.length;
            } else {
                salida.write(datos[i++]);
            }
        }
        return salida.toByteArray();
    }

    private static byte[] bloqueNombre(String nombre) {
        byte[] texto = nombre.getBytes(StandardCharsets.UTF_8);
        byte[] bloque = new byte[texto.length + 2];
        bloque[0] = (byte) (texto.length >> 8);
        bloque[1] = (byte) texto.length;
        System.arraycopy(texto, 0, bloque, 2, texto.length);
        return bloque;
    }

    private static boolean coincide(byte[] datos, int desde, byte[] patron) {
        if (desde + patron.length > datos.length) return false;
        for (int j = 0; j < patron.length; j++) {
            if (datos[desde + j] != patron[j]) return false;
        }
        return true;
    }
}

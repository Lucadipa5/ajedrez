package ajedrez.adaptadores.ui.swing.perfil;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.NoSuchAlgorithmException;

/**
 * Modelo de datos del perfil de un jugador.
 * Almacena nombre, puntos totales, nivel calculado de forma exponencial
 * y estadísticas de partidas.
 *
 * El nivel sube de forma exponencial: los puntos necesarios para subir de nivel
 * aumentan con cada nivel (base * factor^nivel), haciendo los primeros niveles
 * fáciles y los avanzados cada vez más difíciles.
 */
public class PerfilJugador implements Serializable {
    private static final long serialVersionUID = 1L;

    // --- Puntos otorgados por cada acción ---
    public static final int PUNTOS_VICTORIA = 100;
    public static final int PUNTOS_COMER_PIEZA = 15;
    public static final int PUNTOS_JAQUE = 5;

    // --- Curva de nivel exponencial ---
    // Puntos para subir de nivel N a N+1: BASE_PUNTOS_NIVEL * (FACTOR_NIVEL ^ N)
    private static final int BASE_PUNTOS_NIVEL = 200;
    private static final double FACTOR_NIVEL = 1.6;
    private static final int NIVEL_MAXIMO = 50;

    // --- Hash de contraseña (PBKDF2) ---
    private static final int ITERACIONES_HASH = 65536;
    private static final int LARGO_HASH_BITS = 256;
    private static final int LARGO_SAL_BYTES = 16;

    private String nombre;
    // La contraseña nunca se guarda en texto plano: solo la sal y el hash derivado
    private byte[] salContrasena;
    private byte[] hashContrasena;
    private long puntosAcumulados;
    private int nivel;
    private int puntosEnNivelActual;   // puntos acumulados dentro del nivel actual
    private int puntosParaSiguienteNivel; // cuántos necesita para avanzar al próximo

    // Estadísticas
    private int partidasJugadas;
    private int partidasGanadas;
    private int piezasComidas;
    private int jaquesDados;

    public PerfilJugador(String nombre) {
        this.nombre = nombre;
        this.puntosAcumulados = 0;
        this.nivel = 1;
        this.puntosEnNivelActual = 0;
        this.puntosParaSiguienteNivel = puntosParaSubirDesdeNivel(1);
        this.partidasJugadas = 0;
        this.partidasGanadas = 0;
        this.piezasComidas = 0;
        this.jaquesDados = 0;
    }

    /**
     * Copia profunda del perfil (vía serialización). Se usa para guardar el estado
     * antes de cada jugada y poder restaurarlo al deshacer.
     */
    public PerfilJugador copiar() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(bytes)) {
                oos.writeObject(this);
            }
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                return (PerfilJugador) ois.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("No se pudo copiar el perfil", e);
        }
    }

    // --- Contraseña ---
    public boolean tieneContrasena() {
        return hashContrasena != null && salContrasena != null;
    }

    public void establecerContrasena(char[] contrasena) {
        byte[] sal = new byte[LARGO_SAL_BYTES];
        new SecureRandom().nextBytes(sal);
        this.salContrasena = sal;
        this.hashContrasena = derivarHash(contrasena, sal);
    }

    public boolean verificarContrasena(char[] contrasena) {
        if (!tieneContrasena()) return false;
        byte[] intento = derivarHash(contrasena, salContrasena);
        // Comparación en tiempo constante
        return MessageDigest.isEqual(intento, hashContrasena);
    }

    private static byte[] derivarHash(char[] contrasena, byte[] sal) {
        PBEKeySpec spec = new PBEKeySpec(contrasena, sal, ITERACIONES_HASH, LARGO_HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña", e);
        } finally {
            spec.clearPassword();
        }
    }

    // --- Cálculo exponencial del umbral de cada nivel ---
    public static int puntosParaSubirDesdeNivel(int nivelActual) {
        return (int) (BASE_PUNTOS_NIVEL * Math.pow(FACTOR_NIVEL, nivelActual - 1));
    }

    /**
     * Agrega puntos y actualiza el nivel de forma exponencial.
     * @return true si el jugador subió de nivel
     */
    public boolean agregarPuntos(int puntos) {
        this.puntosAcumulados += puntos;
        boolean subioNivel = false;

        while (puntosEnNivelActual + puntos >= puntosParaSiguienteNivel && nivel < NIVEL_MAXIMO) {
            puntos -= (puntosParaSiguienteNivel - puntosEnNivelActual);
            nivel++;
            puntosEnNivelActual = 0;
            puntosParaSiguienteNivel = puntosParaSubirDesdeNivel(nivel);
            subioNivel = true;
        }

        if (nivel < NIVEL_MAXIMO) {
            puntosEnNivelActual += puntos;
        } else {
            puntosEnNivelActual = puntosParaSiguienteNivel; // nivel max: barra llena
        }

        return subioNivel;
    }

    /**
     * Progreso de la barra de nivel, valor entre 0.0 y 1.0
     */
    public double getProgresoBarra() {
        if (nivel >= NIVEL_MAXIMO) return 1.0;
        return (double) puntosEnNivelActual / puntosParaSiguienteNivel;
    }

    public void registrarVictoria() {
        partidasGanadas++;
        agregarPuntos(PUNTOS_VICTORIA);
    }

    public void registrarPartidaJugada() {
        partidasJugadas++;
    }

    public boolean registrarPiezaComida() {
        piezasComidas++;
        return agregarPuntos(PUNTOS_COMER_PIEZA);
    }

    public boolean registrarJaque() {
        jaquesDados++;
        return agregarPuntos(PUNTOS_JAQUE);
    }

    // --- Getters ---
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public long getPuntosAcumulados() { return puntosAcumulados; }
    public int getNivel() { return nivel; }
    public int getPuntosEnNivelActual() { return puntosEnNivelActual; }
    public int getPuntosParaSiguienteNivel() { return puntosParaSiguienteNivel; }
    public int getPartidasJugadas() { return partidasJugadas; }
    public int getPartidasGanadas() { return partidasGanadas; }
    public int getPiezasComidas() { return piezasComidas; }
    public int getJaquesDados() { return jaquesDados; }
    public int getNivelMaximo() { return NIVEL_MAXIMO; }

    @Override
    public String toString() {
        return nombre + " [Nv." + nivel + " | " + puntosAcumulados + " pts]";
    }
}

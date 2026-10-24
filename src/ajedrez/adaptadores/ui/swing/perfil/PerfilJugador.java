package ajedrez.adaptadores.ui.swing.perfil;

import java.io.Serializable;

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

    private String nombre;
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

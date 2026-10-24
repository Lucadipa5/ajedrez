package ajedrez.adaptadores.ia;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Jugada;
import ajedrez.nucleo.ia.Dificultad;
import ajedrez.nucleo.ia.MotorAjedrez;
import ajedrez.nucleo.ia.MotorNoDisponibleException;
import ajedrez.nucleo.tablero.NotacionFEN;
import ajedrez.nucleo.tablero.Tablero;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/**
 * Adaptador de IA: implementa el puerto {@link MotorAjedrez} lanzando el script
 * {@code ia/motor_ia.py} (basado en la librería python-chess) como un proceso aparte.
 *
 * Se comunica por entrada/salida estándar: le manda la posición en FEN y recibe
 * la jugada en notación UCI. El proceso se inicia una vez y se reutiliza durante toda la partida.
 */
public class MotorPython implements MotorAjedrez {

    private static final String RUTA_SCRIPT = "ia/motor_ia.py";
    private static final String[] COMANDOS_PYTHON = {"python", "py", "python3"};
    private static final int ESPERA_INICIO_SEG = 15;
    private static final int ESPERA_JUGADA_SEG = 60;

    private final ExecutorService lector = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "motor-python");
        t.setDaemon(true);
        return t;
    });

    private Process proceso;
    private BufferedWriter entrada;
    private BufferedReader salida;
    private String motivoNoDisponible;

    public MotorPython() {
        iniciar();
    }

    private void iniciar() {
        File script = new File(RUTA_SCRIPT);
        if (!script.exists()) {
            motivoNoDisponible = "No se encontró el script de IA (" + script.getAbsolutePath() + ").";
            return;
        }
        for (String comando : COMANDOS_PYTHON) {
            try {
                ProcessBuilder pb = new ProcessBuilder(comando, "-u", script.getPath());
                pb.environment().put("PYTHONIOENCODING", "utf-8");
                pb.redirectError(ProcessBuilder.Redirect.DISCARD);
                Process p = pb.start();
                BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
                String saludo = leerLinea(out, ESPERA_INICIO_SEG);
                if ("LISTO".equals(saludo)) {
                    proceso = p;
                    salida = out;
                    entrada = new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), StandardCharsets.UTF_8));
                    motivoNoDisponible = null;
                    return;
                }
                p.destroy();
                if (saludo != null && saludo.startsWith("ERROR ")) {
                    // Python existe pero falta algo (por ejemplo la librería): no tiene sentido probar otro comando
                    motivoNoDisponible = saludo.substring(6);
                    return;
                }
            } catch (IOException e) {
                // Ese comando de Python no existe en el sistema: se prueba el siguiente
            }
        }
        motivoNoDisponible = "No se encontró Python instalado. Instalá Python 3 y la librería con: pip install chess";
    }

    private String leerLinea(BufferedReader out, int segundos) {
        Future<String> futura = lector.submit(out::readLine);
        try {
            return futura.get(segundos, TimeUnit.SECONDS);
        } catch (Exception e) {
            futura.cancel(true);
            return null;
        }
    }

    @Override
    public boolean estaDisponible() {
        return proceso != null && proceso.isAlive();
    }

    @Override
    public String motivoNoDisponible() {
        if (motivoNoDisponible != null) return motivoNoDisponible;
        return estaDisponible() ? null : "El motor de IA se cerró inesperadamente.";
    }

    @Override
    public synchronized Jugada elegirJugada(Tablero tablero, Color turno, Dificultad dificultad)
            throws MotorNoDisponibleException {
        if (!estaDisponible()) {
            throw new MotorNoDisponibleException(motivoNoDisponible());
        }
        try {
            entrada.write(dificultad.name() + " " + NotacionFEN.desdeTablero(tablero, turno));
            entrada.newLine();
            entrada.flush();
        } catch (IOException e) {
            throw new MotorNoDisponibleException("No se pudo comunicar con el motor de IA", e);
        }
        String respuesta = leerLinea(salida, ESPERA_JUGADA_SEG);
        if (respuesta == null) {
            // El proceso quedó colgado o desincronizado: se lo cierra para no leer respuestas viejas
            proceso.destroy();
            motivoNoDisponible = "El motor de IA no respondió a tiempo.";
            throw new MotorNoDisponibleException(motivoNoDisponible);
        }
        if (respuesta.startsWith("ERROR")) {
            throw new MotorNoDisponibleException("El motor de IA devolvió un error: " + respuesta);
        }
        return Jugada.desdeUCI(respuesta);
    }

    @Override
    public void cerrar() {
        if (proceso != null) {
            try {
                entrada.write("SALIR");
                entrada.newLine();
                entrada.flush();
            } catch (IOException ignored) {}
            proceso.destroy();
            proceso = null;
        }
        lector.shutdownNow();
    }
}

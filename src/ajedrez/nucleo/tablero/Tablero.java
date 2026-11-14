package ajedrez.nucleo.tablero;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.dominio.TipoPieza;

import java.util.*;

/**
 * El tablero: sus dimensiones, qué pieza ocupa cada casilla y los dos datos de la posición
 * que no son una pieza pero sí forman parte de ella (como en la notación FEN):
 * la casilla de captura al paso y el reloj de la regla de los 50 movimientos.
 *
 * Por defecto es de 8x8, pero el tamaño se recibe por constructor: todo el núcleo pregunta
 * al tablero si una casilla existe en vez de asumir el 8.
 *
 * NOTA DE DISEÑO (ANTI-SINGLETON): se instancia como un objeto común. Cada test crea el suyo
 * y las reglas lo clonan para simular jugadas sin tocar el tablero real.
 */
public class Tablero {
    public static final int TAMANO_ESTANDAR = 8;

    private final int filas;
    private final int columnas;
    private final Map<Posicion, Pieza> casillasOcupadas;

    // Casilla que "saltó" el último peón que avanzó dos: ahí puede capturarlo al paso un peón rival
    private Posicion casillaAlPaso;
    // Medias jugadas (de cualquier bando) desde la última captura o movimiento de peón
    private int mediasJugadasSinProgreso;

    public Tablero() {
        this(TAMANO_ESTANDAR, TAMANO_ESTANDAR);
    }

    public Tablero(int filas, int columnas) {
        if (filas < 2 || columnas < 2) {
            throw new IllegalArgumentException("El tablero necesita al menos 2 filas y 2 columnas");
        }
        this.filas = filas;
        this.columnas = columnas;
        this.casillasOcupadas = new HashMap<>();
    }

    public int getFilas() {
        return filas;
    }

    public int getColumnas() {
        return columnas;
    }

    public boolean esEstandar() {
        return filas == TAMANO_ESTANDAR && columnas == TAMANO_ESTANDAR;
    }

    public boolean esCasillaValida(Posicion pos) {
        return pos != null
                && pos.getFila() >= 0 && pos.getFila() < filas
                && pos.getColumna() >= 0 && pos.getColumna() < columnas;
    }

    public boolean estaOcupada(Posicion pos) {
        return esCasillaValida(pos) && casillasOcupadas.containsKey(pos);
    }

    public Pieza obtenerPieza(Posicion pos) {
        if (!esCasillaValida(pos)) return null;
        return casillasOcupadas.get(pos);
    }

    public void colocarPieza(Pieza pieza, Posicion pos) {
        if (!esCasillaValida(pos) || pieza == null) return;
        casillasOcupadas.put(pos, pieza);
    }

    public Pieza removerPieza(Posicion pos) {
        if (!esCasillaValida(pos)) return null;
        return casillasOcupadas.remove(pos);
    }

    /**
     * Mueve una pieza de origen a destino. Si la casilla destino estaba ocupada,
     * sobreescribe la posición y retorna la pieza capturada (o null si la casilla estaba vacía).
     */
    public Pieza moverPieza(Posicion origen, Posicion destino) {
        if (!estaOcupada(origen) || !esCasillaValida(destino)) {
            return null;
        }
        Pieza piezaAMover = casillasOcupadas.remove(origen);
        Pieza piezaCapturada = casillasOcupadas.put(destino, piezaAMover);
        piezaAMover.setSeHaMovido(true);
        return piezaCapturada;
    }

    public Posicion buscarRey(Color color) {
        for (Map.Entry<Posicion, Pieza> entrada : casillasOcupadas.entrySet()) {
            Pieza p = entrada.getValue();
            if (p.getTipo() == TipoPieza.REY && p.getColor() == color) {
                return entrada.getKey();
            }
        }
        return null;
    }

    public Map<Posicion, Pieza> getCasillasOcupadas() {
        return Collections.unmodifiableMap(casillasOcupadas);
    }

    /** Posiciones de todas las piezas del color indicado. */
    public List<Posicion> posicionesDe(Color color) {
        List<Posicion> posiciones = new ArrayList<>();
        for (Map.Entry<Posicion, Pieza> e : casillasOcupadas.entrySet()) {
            if (e.getValue().getColor() == color) posiciones.add(e.getKey());
        }
        return posiciones;
    }

    /** Vacía el tablero y reinicia el estado de la posición. */
    public void limpiar() {
        casillasOcupadas.clear();
        casillaAlPaso = null;
        mediasJugadasSinProgreso = 0;
    }

    // -----------------------------------------------------------------------
    // Estado de la posición que no es una pieza
    // -----------------------------------------------------------------------
    public Posicion getCasillaAlPaso() {
        return casillaAlPaso;
    }

    public void setCasillaAlPaso(Posicion casillaAlPaso) {
        this.casillaAlPaso = casillaAlPaso;
    }

    public int getMediasJugadasSinProgreso() {
        return mediasJugadasSinProgreso;
    }

    public void setMediasJugadasSinProgreso(int mediasJugadas) {
        this.mediasJugadasSinProgreso = Math.max(0, mediasJugadas);
    }

    /**
     * Clave que identifica la posición a efectos de la regla de repetición: las piezas, el turno,
     * si reyes y torres se movieron (derechos de enroque) y la casilla al paso.
     * Dos posiciones con la misma clave son "la misma posición" según el reglamento.
     */
    public String claveDePosicion(Color turno) {
        StringBuilder sb = new StringBuilder();
        sb.append(filas).append('x').append(columnas).append(' ');
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                Pieza p = casillasOcupadas.get(new Posicion(f, c));
                if (p == null) {
                    sb.append('.');
                    continue;
                }
                char letra = p.getTipo().getSimboloAlgebraico();
                sb.append(p.getColor() == Color.BLANCO ? letra : Character.toLowerCase(letra));
                // Solo reyes y torres cambian la posición al moverse (pierden el derecho a enrocar)
                if ((p.getTipo() == TipoPieza.REY || p.getTipo() == TipoPieza.TORRE) && p.seHaMovido()) sb.append('*');
            }
            sb.append('/');
        }
        sb.append(' ').append(turno == Color.BLANCO ? 'w' : 'b');
        sb.append(' ').append(casillaAlPaso != null ? casillaAlPaso.toString() : "-");
        return sb.toString();
    }

    /**
     * Copia PROFUNDA del tablero: cada Pieza es una nueva instancia independiente, con su mismo
     * estado. Es lo que permite simular jugadas (¿deja a mi rey en jaque?) sin tocar el tablero real.
     */
    public Tablero clonar() {
        Tablero clon = new Tablero(filas, columnas);
        for (Map.Entry<Posicion, Pieza> entrada : this.casillasOcupadas.entrySet()) {
            Pieza original = entrada.getValue();
            Pieza copia = new Pieza(original.getTipo(), original.getColor(), original.getEstrategiaMovimiento());
            copia.setSeHaMovido(original.seHaMovido());
            clon.casillasOcupadas.put(entrada.getKey(), copia);
        }
        clon.casillaAlPaso = this.casillaAlPaso;
        clon.mediasJugadasSinProgreso = this.mediasJugadasSinProgreso;
        return clon;
    }

    /** Dos tableros son iguales si tienen las mismas dimensiones y las mismas piezas (tipo y color) en las mismas casillas. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tablero tablero = (Tablero) o;
        if (filas != tablero.filas || columnas != tablero.columnas) return false;
        if (casillasOcupadas.size() != tablero.casillasOcupadas.size()) return false;
        for (Map.Entry<Posicion, Pieza> e : casillasOcupadas.entrySet()) {
            Pieza otra = tablero.casillasOcupadas.get(e.getKey());
            if (otra == null || otra.getTipo() != e.getValue().getTipo() || otra.getColor() != e.getValue().getColor()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(filas, columnas, casillasOcupadas.size());
    }
}

package ajedrez.adaptadores.ui.swing;

import ajedrez.nucleo.dominio.Posicion;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

/**
 * Dibuja el tablero sobre la imagen de fondo (Summoner's Rift).
 *
 * El área de juego de la imagen es un trapecio (está en perspectiva: el lado de arriba es más
 * angosto que el de abajo). Las 64 casillas se calculan con una homografía que lleva el cuadrado
 * 8x8 a ese trapecio, así cada baldosa queda deformada con la misma perspectiva que el marco.
 *
 * Baldosas: claras → blanca; oscuras de la mitad de abajo → azul; oscuras de la mitad de arriba → roja.
 * La mitad se toma según la pantalla (no según el color), así el lado del jugador siempre es azul.
 */
class VistaTablero extends JComponent {

    // Esquinas del área de juego dentro de tablero_fondo.jpg (1408x768)
    private static final Point2D.Double ESQ_SUP_IZQ = new Point2D.Double(440, 122);
    private static final Point2D.Double ESQ_SUP_DER = new Point2D.Double(969, 122);
    private static final Point2D.Double ESQ_INF_DER = new Point2D.Double(996, 669);
    private static final Point2D.Double ESQ_INF_IZQ = new Point2D.Double(412, 669);
    // Rectángulo del marco de piedra: es lo que tiene que entrar siempre en pantalla
    private static final Rectangle REGION_MARCO = new Rectangle(345, 60, 720, 680);
    // Separación entre baldosas (fracción de la casilla) para que se vea la junta de piedra
    private static final double JUNTA = 0.015;

    private static final BufferedImage FONDO_COMPUESTO;
    private static final Homografia PERSPECTIVA =
            new Homografia(ESQ_SUP_IZQ, ESQ_SUP_DER, ESQ_INF_DER, ESQ_INF_IZQ);

    static {
        BufferedImage fondo = cargar("tablero_fondo.jpg");
        FONDO_COMPUESTO = fondo != null
                ? componerTablero(fondo, cargar("baldosa_blanca.png"), cargar("baldosa_azul.png"), cargar("baldosa_roja.png"))
                : null;
    }

    private static final java.awt.Color BG = new java.awt.Color(35, 32, 28);
    private static final java.awt.Color COLOR_SELECCION = new java.awt.Color(250, 230, 60, 120);
    private static final java.awt.Color COLOR_BORDE_SELECCION = new java.awt.Color(250, 220, 50, 230);
    private static final java.awt.Color COLOR_ULTIMA_JUGADA = new java.awt.Color(240, 220, 90, 95);
    private static final java.awt.Color COLOR_MARCA = new java.awt.Color(25, 20, 12, 95);
    private static final java.awt.Color COLOR_MARCA_CAPTURA = new java.awt.Color(25, 20, 12, 110);
    private static final java.awt.Color COLOR_MARCA_CAPTURA_HOVER = new java.awt.Color(220, 50, 40, 200);
    private static final java.awt.Color COLOR_HOVER_DESTINO = new java.awt.Color(120, 200, 80, 120);
    private static final java.awt.Color COLOR_HOVER_CAPTURA = new java.awt.Color(230, 70, 50, 70);

    // -----------------------------------------------------------------------
    // Estado que la ventana actualiza (indexado por posición lógica [fila][columna])
    // -----------------------------------------------------------------------
    final BufferedImage[][] imagenPieza = new BufferedImage[8][8];
    final String[][] simboloPieza = new String[8][8];            // si falta la imagen
    final java.awt.Color[][] colorSimbolo = new java.awt.Color[8][8];
    final boolean[][] destino = new boolean[8][8];
    final boolean[][] captura = new boolean[8][8];
    final boolean[][] ultimaJugada = new boolean[8][8];
    Posicion seleccionada;
    Posicion reyEnJaque;
    boolean esMate;
    float pulso = 1f;
    float escalaMarca = 1f;
    int desplazamientoX = 0;

    private final boolean invertido;
    private Posicion hover;
    private Consumer<Posicion> alClickear = p -> {};

    // Escala y desplazamiento actuales de la imagen dentro del componente
    private double escala = 1;
    private double origenX, origenY;
    private BufferedImage fondoEscalado;
    private Dimension tamanoCache;

    VistaTablero(boolean invertido) {
        this.invertido = invertido;
        setOpaque(true);
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { actualizarHover(casillaEn(e.getPoint())); }
            @Override public void mouseExited(MouseEvent e) { actualizarHover(null); }
            @Override public void mousePressed(MouseEvent e) {
                Posicion p = casillaEn(e.getPoint());
                if (p != null && SwingUtilities.isLeftMouseButton(e)) alClickear.accept(p);
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    void setAlClickear(Consumer<Posicion> accion) {
        this.alClickear = accion;
    }

    private void actualizarHover(Posicion p) {
        if (p == null ? hover == null : p.equals(hover)) return;
        hover = p;
        setCursor(p != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        repaint();
    }

    // -----------------------------------------------------------------------
    // Geometría: posición lógica ↔ casilla en pantalla
    // -----------------------------------------------------------------------
    private int filaVisual(Posicion p) { return invertido ? 7 - p.getFila() : p.getFila(); }
    private int colVisual(Posicion p)  { return invertido ? 7 - p.getColumna() : p.getColumna(); }

    private void recalcularEscala() {
        Dimension d = getSize();
        if (d.equals(tamanoCache) && fondoEscalado != null) return;
        tamanoCache = d;
        escala = Math.min(d.width / (double) REGION_MARCO.width, d.height / (double) REGION_MARCO.height);
        origenX = d.width / 2.0 - (REGION_MARCO.getCenterX()) * escala;
        origenY = d.height / 2.0 - (REGION_MARCO.getCenterY()) * escala;
        if (FONDO_COMPUESTO != null) {
            int w = Math.max(1, (int) Math.round(FONDO_COMPUESTO.getWidth() * escala));
            int h = Math.max(1, (int) Math.round(FONDO_COMPUESTO.getHeight() * escala));
            fondoEscalado = escalarConCalidad(FONDO_COMPUESTO, w, h);
        }
    }

    /** Punto de pantalla para la coordenada (u, v) del tablero, con u, v entre 0 y 8. */
    private Point2D.Double aPantalla(double colU, double filaV) {
        Point2D.Double p = PERSPECTIVA.aplicar(colU / 8.0, filaV / 8.0);
        return new Point2D.Double(origenX + desplazamientoX + p.x * escala, origenY + p.y * escala);
    }

    private Shape formaCasilla(Posicion pos) {
        int f = filaVisual(pos), c = colVisual(pos);
        Point2D.Double a = aPantalla(c, f), b = aPantalla(c + 1, f), d = aPantalla(c + 1, f + 1), e = aPantalla(c, f + 1);
        Path2D.Double forma = new Path2D.Double();
        forma.moveTo(a.x, a.y);
        forma.lineTo(b.x, b.y);
        forma.lineTo(d.x, d.y);
        forma.lineTo(e.x, e.y);
        forma.closePath();
        return forma;
    }

    /** Centro de la casilla en coordenadas de este componente. */
    Point centroDeCasilla(Posicion pos) {
        recalcularEscala();
        Point2D.Double c = aPantalla(colVisual(pos) + 0.5, filaVisual(pos) + 0.5);
        return new Point((int) Math.round(c.x), (int) Math.round(c.y));
    }

    Point centroDelTablero() {
        recalcularEscala();
        Point2D.Double c = aPantalla(4, 4);
        return new Point((int) Math.round(c.x), (int) Math.round(c.y));
    }

    private Posicion casillaEn(Point punto) {
        recalcularEscala();
        double x = (punto.x - origenX - desplazamientoX) / escala;
        double y = (punto.y - origenY) / escala;
        Point2D.Double uv = PERSPECTIVA.invertir(x, y);
        if (uv.x < 0 || uv.x >= 1 || uv.y < 0 || uv.y >= 1) return null;
        int colV = (int) (uv.x * 8), filaV = (int) (uv.y * 8);
        return new Posicion(invertido ? 7 - filaV : filaV, invertido ? 7 - colV : colV);
    }

    // -----------------------------------------------------------------------
    // Dibujo
    // -----------------------------------------------------------------------
    @Override
    protected void paintComponent(Graphics g) {
        recalcularEscala();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(BG);
        g2.fillRect(0, 0, getWidth(), getHeight());
        if (fondoEscalado != null) {
            g2.drawImage(fondoEscalado, (int) Math.round(origenX) + desplazamientoX, (int) Math.round(origenY), null);
        }
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // Primero los resaltados de cada casilla, después las piezas (de atrás hacia adelante)
        for (int f = 0; f < 8; f++)
            for (int c = 0; c < 8; c++)
                dibujarResaltados(g2, new Posicion(f, c));
        dibujarCoordenadas(g2);
        for (int filaV = 0; filaV < 8; filaV++) {
            for (int colV = 0; colV < 8; colV++) {
                int f = invertido ? 7 - filaV : filaV, c = invertido ? 7 - colV : colV;
                dibujarPieza(g2, new Posicion(f, c));
            }
        }
        g2.dispose();
    }

    private void dibujarResaltados(Graphics2D g2, Posicion pos) {
        int f = pos.getFila(), c = pos.getColumna();
        Shape forma = formaCasilla(pos);
        Rectangle2D caja = forma.getBounds2D();
        double cx = caja.getCenterX(), cy = caja.getCenterY();
        double ancho = caja.getWidth(), alto = caja.getHeight();
        boolean esHover = pos.equals(hover);

        if (ultimaJugada[f][c]) {
            g2.setColor(COLOR_ULTIMA_JUGADA);
            g2.fill(forma);
        }
        if (pos.equals(seleccionada)) {
            g2.setColor(COLOR_SELECCION);
            g2.fill(forma);
            g2.setColor(COLOR_BORDE_SELECCION);
            g2.setStroke(new BasicStroke(2.5f));
            g2.draw(forma);
        }

        // Rey en jaque: brillo rojo radial que late (fijo e intenso si es mate)
        if (pos.equals(reyEnJaque)) {
            float intensidad = esMate ? 1f : 0.55f + 0.45f * pulso;
            int a = Math.round(235 * intensidad);
            Shape clipAnterior = g2.getClip();
            g2.clip(forma);
            g2.setPaint(new RadialGradientPaint(new Point2D.Double(cx, cy), (float) (Math.max(ancho, alto) * 0.72),
                    new float[]{0f, 0.45f, 1f},
                    new java.awt.Color[]{
                            new java.awt.Color(255, 40, 30, a),
                            new java.awt.Color(230, 30, 20, Math.round(a * 0.65f)),
                            new java.awt.Color(200, 0, 0, 0)}));
            g2.fill(forma);
            g2.setClip(clipAnterior);
            g2.setColor(new java.awt.Color(220, 25, 15, Math.round(220 * intensidad)));
            g2.setStroke(new BasicStroke(3f));
            g2.draw(forma);
        }

        if (destino[f][c] && esHover) {
            g2.setColor(captura[f][c] ? COLOR_HOVER_CAPTURA : COLOR_HOVER_DESTINO);
            g2.fill(forma);
        }
        // Marcas de movimiento posible (elipses: acompañan la perspectiva de la casilla)
        if (destino[f][c] && !(esHover && !captura[f][c])) {
            if (!captura[f][c]) {
                double rx = ancho * 0.16 * escalaMarca, ry = alto * 0.16 * escalaMarca;
                g2.setColor(COLOR_MARCA);
                g2.fill(new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2));
            } else {
                double grosor = Math.min(ancho, alto) * 0.085;
                double factor = 0.7 + 0.3 * escalaMarca;
                double rx = (ancho / 2 - grosor / 2 - 1) * factor, ry = (alto / 2 - grosor / 2 - 1) * factor;
                g2.setColor(esHover ? COLOR_MARCA_CAPTURA_HOVER : COLOR_MARCA_CAPTURA);
                g2.setStroke(new BasicStroke((float) grosor));
                g2.draw(new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2));
            }
        }
    }

    /** Letras en la fila de abajo y números en la columna izquierda, dentro de las casillas. */
    private void dibujarCoordenadas(Graphics2D g2) {
        float tam = (float) Math.max(10, 15 * escala);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 1).deriveFont(tam));
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < 8; i++) {
            // Columna izquierda (visual): números
            int fila = invertido ? 7 - i : i;
            Point2D.Double p = aPantalla(0.08, i + 0.06);
            dibujarTextoCoordenada(g2, String.valueOf(8 - fila), p.x, p.y + fm.getAscent(), (i % 2 == 0));
            // Fila de abajo (visual): letras
            int col = invertido ? 7 - i : i;
            Point2D.Double q = aPantalla(i + 0.92, 7.94);
            String letra = String.valueOf((char) ('a' + col));
            dibujarTextoCoordenada(g2, letra, q.x - fm.stringWidth(letra), q.y - fm.getDescent(), ((7 + i) % 2 == 0));
        }
    }

    private void dibujarTextoCoordenada(Graphics2D g2, String s, double x, double y, boolean sobreClara) {
        g2.setColor(sobreClara ? new java.awt.Color(80, 58, 35) : new java.awt.Color(255, 250, 240, 230));
        g2.drawString(s, (float) x, (float) y);
    }

    private void dibujarPieza(Graphics2D g2, Posicion pos) {
        int f = pos.getFila(), c = pos.getColumna();
        Rectangle2D caja = formaCasilla(pos).getBounds2D();
        double lado = Math.min(caja.getWidth(), caja.getHeight());
        BufferedImage img = imagenPieza[f][c];
        if (img != null) {
            double margen = lado * 0.06;
            double max = lado - margen * 2;
            double s = Math.min(max / img.getWidth(), max / img.getHeight());
            int w = (int) Math.round(img.getWidth() * s), h = (int) Math.round(img.getHeight() * s);
            int x = (int) Math.round(caja.getCenterX() - w / 2.0);
            int y = (int) Math.round(caja.getCenterY() - h / 2.0);
            // Sombra suave debajo de la pieza para que "apoye" sobre la baldosa
            g2.setColor(new java.awt.Color(0, 0, 0, 70));
            g2.fill(new Ellipse2D.Double(caja.getCenterX() - w * 0.3, y + h - h * 0.09, w * 0.6, h * 0.12));
            g2.drawImage(img, x, y, w, h, null);
        } else if (simboloPieza[f][c] != null && !simboloPieza[f][c].isEmpty()) {
            String s = simboloPieza[f][c];
            g2.setFont(new Font(PantallaMenu.FUENTE_AJEDREZ, Font.BOLD, Math.max(12, (int) (lado * 0.62))));
            FontMetrics fm = g2.getFontMetrics();
            float x = (float) (caja.getCenterX() - fm.stringWidth(s) / 2.0);
            float y = (float) (caja.getCenterY() - fm.getHeight() / 2.0 + fm.getAscent());
            g2.setColor(colorSimbolo[f][c]);
            g2.drawString(s, x, y);
        }
    }

    // -----------------------------------------------------------------------
    // Composición del tablero (una sola vez, al cargar la clase)
    // -----------------------------------------------------------------------
    private static BufferedImage componerTablero(BufferedImage fondo, BufferedImage blanca,
                                                 BufferedImage azul, BufferedImage roja) {
        BufferedImage salida = new BufferedImage(fondo.getWidth(), fondo.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = salida.createGraphics();
        g.drawImage(fondo, 0, 0, null);
        g.dispose();
        if (blanca == null || azul == null || roja == null) return salida;

        int minX = (int) Math.floor(Math.min(ESQ_SUP_IZQ.x, ESQ_INF_IZQ.x));
        int maxX = (int) Math.ceil(Math.max(ESQ_SUP_DER.x, ESQ_INF_DER.x));
        int minY = (int) Math.floor(ESQ_SUP_IZQ.y);
        int maxY = (int) Math.ceil(ESQ_INF_IZQ.y);

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                Point2D.Double uv = PERSPECTIVA.invertir(x + 0.5, y + 0.5);
                if (uv.x < 0 || uv.x >= 1 || uv.y < 0 || uv.y >= 1) continue;
                double cu = uv.x * 8, cv = uv.y * 8;
                int col = (int) cu, fila = (int) cv;
                double lu = (cu - col - JUNTA) / (1 - 2 * JUNTA);
                double lv = (cv - fila - JUNTA) / (1 - 2 * JUNTA);
                if (lu < 0 || lu >= 1 || lv < 0 || lv >= 1) continue;

                // Casilla clara si (fila + columna) es par (a8 y h1 son claras)
                BufferedImage baldosa = (fila + col) % 2 == 0 ? blanca : (fila >= 4 ? azul : roja);
                int pixel = muestrear(baldosa, lu * baldosa.getWidth(), lv * baldosa.getHeight());
                salida.setRGB(x, y, mezclarSobre(pixel, salida.getRGB(x, y)));
            }
        }
        return salida;
    }

    /** Muestreo bilineal (con alfa) de la baldosa. */
    private static int muestrear(BufferedImage img, double x, double y) {
        int w = img.getWidth(), h = img.getHeight();
        x = Math.max(0, Math.min(w - 1.001, x - 0.5));
        y = Math.max(0, Math.min(h - 1.001, y - 0.5));
        int x0 = (int) x, y0 = (int) y;
        double fx = x - x0, fy = y - y0;
        int p00 = img.getRGB(x0, y0), p10 = img.getRGB(x0 + 1, y0);
        int p01 = img.getRGB(x0, y0 + 1), p11 = img.getRGB(x0 + 1, y0 + 1);
        int resultado = 0;
        for (int desplazamiento = 0; desplazamiento <= 24; desplazamiento += 8) {
            double v = (1 - fx) * (1 - fy) * ((p00 >>> desplazamiento) & 255)
                    + fx * (1 - fy) * ((p10 >>> desplazamiento) & 255)
                    + (1 - fx) * fy * ((p01 >>> desplazamiento) & 255)
                    + fx * fy * ((p11 >>> desplazamiento) & 255);
            resultado |= ((int) Math.round(v) & 255) << desplazamiento;
        }
        return resultado;
    }

    private static int mezclarSobre(int arriba, int abajo) {
        double a = ((arriba >>> 24) & 255) / 255.0;
        int r = (int) Math.round(((arriba >> 16) & 255) * a + ((abajo >> 16) & 255) * (1 - a));
        int g = (int) Math.round(((arriba >> 8) & 255) * a + ((abajo >> 8) & 255) * (1 - a));
        int b = (int) Math.round((arriba & 255) * a + (abajo & 255) * (1 - a));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static BufferedImage escalarConCalidad(BufferedImage img, int w, int h) {
        BufferedImage salida = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = salida.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return salida;
    }

    private static BufferedImage cargar(String nombre) {
        try (InputStream is = VistaTablero.class.getResourceAsStream("recursos/" + nombre)) {
            return is != null ? ImageIO.read(is) : null;
        } catch (IOException e) {
            System.err.println("No se pudo cargar imagen: " + nombre);
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Homografía: transforma el cuadrado unidad en un cuadrilátero (y al revés)
    // -----------------------------------------------------------------------
    private static final class Homografia {
        private final double a, b, c, d, e, f, g, h;      // directa
        private final double[] inv = new double[9];       // inversa

        /** p0..p3: imágenes de (0,0), (1,0), (1,1) y (0,1). */
        Homografia(Point2D.Double p0, Point2D.Double p1, Point2D.Double p2, Point2D.Double p3) {
            double dx1 = p1.x - p2.x, dx2 = p3.x - p2.x, dx3 = p0.x - p1.x + p2.x - p3.x;
            double dy1 = p1.y - p2.y, dy2 = p3.y - p2.y, dy3 = p0.y - p1.y + p2.y - p3.y;
            double den = dx1 * dy2 - dx2 * dy1;
            g = (dx3 * dy2 - dx2 * dy3) / den;
            h = (dx1 * dy3 - dx3 * dy1) / den;
            a = p1.x - p0.x + g * p1.x;
            b = p3.x - p0.x + h * p3.x;
            c = p0.x;
            d = p1.y - p0.y + g * p1.y;
            e = p3.y - p0.y + h * p3.y;
            f = p0.y;
            // Inversa = adjunta de [[a b c][d e f][g h 1]] (la escala no importa en coordenadas homogéneas)
            inv[0] = e - f * h;  inv[1] = c * h - b;  inv[2] = b * f - c * e;
            inv[3] = f * g - d;  inv[4] = a - c * g;  inv[5] = c * d - a * f;
            inv[6] = d * h - e * g; inv[7] = b * g - a * h; inv[8] = a * e - b * d;
        }

        Point2D.Double aplicar(double u, double v) {
            double w = g * u + h * v + 1;
            return new Point2D.Double((a * u + b * v + c) / w, (d * u + e * v + f) / w);
        }

        Point2D.Double invertir(double x, double y) {
            double w = inv[6] * x + inv[7] * y + inv[8];
            return new Point2D.Double((inv[0] * x + inv[1] * y + inv[2]) / w, (inv[3] * x + inv[4] * y + inv[5]) / w);
        }
    }
}

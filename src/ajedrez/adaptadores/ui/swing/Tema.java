package ajedrez.adaptadores.ui.swing;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Estética compartida por el menú y la ventana de juego, inspirada en la interfaz de
 * League of Legends (azul noche, dorado y piedra) para que combine con el tablero de Summoner's Rift.
 */
final class Tema {

    private Tema() {}

    // -----------------------------------------------------------------------
    // Paleta
    // -----------------------------------------------------------------------
    static final Color FONDO_OSCURO   = new Color(9, 13, 20);
    static final Color FONDO_PANEL    = new Color(12, 20, 32, 228);
    static final Color FONDO_BOTON    = new Color(22, 30, 40);
    static final Color FONDO_BOTON_2  = new Color(10, 16, 26);
    static final Color DORADO         = new Color(200, 170, 110);
    static final Color DORADO_OSCURO  = new Color(120, 90, 40);
    static final Color DORADO_BRILLO  = new Color(240, 210, 140);
    static final Color TEXTO          = new Color(240, 230, 210);
    static final Color TEXTO_SUAVE    = new Color(160, 155, 140);
    static final Color HEXTECH        = new Color(10, 200, 185);
    static final Color ROJO_JAQUE     = new Color(230, 50, 40);

    // -----------------------------------------------------------------------
    // Tipografías: títulos con serif (como las letras talladas del marco), texto con Segoe UI
    // -----------------------------------------------------------------------
    private static final String FUENTE_TITULOS = elegirFuente("Palatino Linotype", "Book Antiqua", "Georgia", "Serif");

    static Font titulo(float tamano) {
        return new Font(FUENTE_TITULOS, Font.BOLD, 1).deriveFont(tamano);
    }

    /** Título en mayúsculas con letras separadas. */
    static Font tituloEspaciado(float tamano, float espaciado) {
        return titulo(tamano).deriveFont(Map.of(TextAttribute.TRACKING, espaciado));
    }

    static Font texto(int estilo, float tamano) {
        return new Font("Segoe UI", estilo, 1).deriveFont(tamano);
    }

    private static String elegirFuente(String... candidatas) {
        java.util.Set<String> disponibles = java.util.Set.of(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String f : candidatas) if (disponibles.contains(f)) return f;
        return candidatas[candidatas.length - 1];
    }

    static void suavizar(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    // -----------------------------------------------------------------------
    // Adornos
    // -----------------------------------------------------------------------

    /**
     * Marco estilo "hextech": fondo azul noche translúcido, borde dorado doble
     * con esquinas recortadas y un pequeño rombo en cada esquina.
     */
    static void pintarMarco(Graphics2D g2, int x, int y, int w, int h) {
        int corte = 10;
        Shape forma = octogono(x + 0.5, y + 0.5, w - 1, h - 1, corte);
        g2.setColor(FONDO_PANEL);
        g2.fill(forma);
        g2.setColor(DORADO_OSCURO);
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(forma);
        g2.setColor(new Color(DORADO.getRed(), DORADO.getGreen(), DORADO.getBlue(), 90));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(octogono(x + 4.5, y + 4.5, w - 9, h - 9, corte - 3));
        g2.setColor(DORADO);
        for (int[] e : new int[][]{{x, y}, {x + w, y}, {x, y + h}, {x + w, y + h}}) {
            rombo(g2, e[0] + (e[0] == x ? corte / 2 : -corte / 2), e[1] + (e[1] == y ? corte / 2 : -corte / 2), 3);
        }
    }

    static Shape octogono(double x, double y, double w, double h, double c) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x + c, y);
        p.lineTo(x + w - c, y);
        p.lineTo(x + w, y + c);
        p.lineTo(x + w, y + h - c);
        p.lineTo(x + w - c, y + h);
        p.lineTo(x + c, y + h);
        p.lineTo(x, y + h - c);
        p.lineTo(x, y + c);
        p.closePath();
        return p;
    }

    static void rombo(Graphics2D g2, double cx, double cy, double r) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(cx, cy - r);
        p.lineTo(cx + r, cy);
        p.lineTo(cx, cy + r);
        p.lineTo(cx - r, cy);
        p.closePath();
        g2.fill(p);
    }

    /** Línea dorada que se desvanece en los extremos, con un rombo en el centro (o un texto). */
    static void pintarSeparador(Graphics2D g2, int x, int y, int w, String texto) {
        Font f = tituloEspaciado(13f, 0.18f);
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics();
        int anchoTexto = texto == null ? 14 : fm.stringWidth(texto) + 24;
        int cx = x + w / 2;
        Color transparente = new Color(DORADO.getRed(), DORADO.getGreen(), DORADO.getBlue(), 0);
        g2.setStroke(new BasicStroke(1f));
        g2.setPaint(new GradientPaint(x, y, transparente, cx - anchoTexto / 2f, y, DORADO));
        g2.drawLine(x, y, cx - anchoTexto / 2, y);
        g2.setPaint(new GradientPaint(cx + anchoTexto / 2f, y, DORADO, x + w, y, transparente));
        g2.drawLine(cx + anchoTexto / 2, y, x + w, y);
        g2.setColor(DORADO);
        if (texto == null) {
            rombo(g2, cx, y, 4);
        } else {
            rombo(g2, cx - anchoTexto / 2.0, y, 2.5);
            rombo(g2, cx + anchoTexto / 2.0, y, 2.5);
            g2.drawString(texto, cx - fm.stringWidth(texto) / 2, y + fm.getAscent() / 2 - 1);
        }
    }

    // -----------------------------------------------------------------------
    // Imágenes
    // -----------------------------------------------------------------------
    static BufferedImage cargarImagen(String nombre) {
        try (InputStream is = Tema.class.getResourceAsStream("recursos/" + nombre)) {
            return is != null ? ImageIO.read(is) : null;
        } catch (IOException e) {
            System.err.println("No se pudo cargar imagen: " + nombre);
            return null;
        }
    }

    /**
     * Escala con buena calidad. Al achicar mucho se reduce a la mitad en varios pasos
     * (evita el "serrucho" de reducir de golpe una imagen grande).
     */
    static BufferedImage escalarConCalidad(BufferedImage img, int w, int h, boolean conTransparencia) {
        int tipo = conTransparencia ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage actual = img;
        int cw = img.getWidth(), ch = img.getHeight();
        while (cw / 2 >= w && ch / 2 >= h) {
            cw /= 2;
            ch /= 2;
            actual = dibujarEscalada(actual, cw, ch, tipo);
        }
        return dibujarEscalada(actual, Math.max(1, w), Math.max(1, h), tipo);
    }

    private static BufferedImage dibujarEscalada(BufferedImage img, int w, int h, int tipo) {
        BufferedImage salida = new BufferedImage(w, h, tipo);
        Graphics2D g = salida.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return salida;
    }

    // -----------------------------------------------------------------------
    // Fondo: el mapa de Summoner's Rift desenfocado y oscurecido
    // -----------------------------------------------------------------------
    static class PanelFondo extends JPanel {
        private static final BufferedImage MAPA = cargarImagen("tablero_fondo.jpg");
        // Punto de la imagen (en fracción de ancho/alto) que queda en el centro del fondo
        private static final double ENFOQUE_X = 0.17, ENFOQUE_Y = 0.55;
        private BufferedImage cache;

        PanelFondo(LayoutManager layout) {
            super(layout);
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth(), h = getHeight();
            if (cache == null || cache.getWidth() != w || cache.getHeight() != h) {
                cache = construir(w, h);
            }
            g.drawImage(cache, 0, 0, null);
        }

        private BufferedImage construir(int w, int h) {
            BufferedImage img = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(FONDO_OSCURO);
            g.fillRect(0, 0, w, h);
            if (MAPA != null) {
                // "cover": llenar todo manteniendo proporción, encuadrado en el bosque de la izquierda
                // (el centro de la imagen es el tablero, que como fondo se vería gris)
                double s = Math.max(w / (double) MAPA.getWidth(), h / (double) MAPA.getHeight());
                int dw = (int) (MAPA.getWidth() * s), dh = (int) (MAPA.getHeight() * s);
                int x = (int) Math.max(w - dw, Math.min(0, w / 2.0 - dw * ENFOQUE_X));
                int y = (int) Math.max(h - dh, Math.min(0, h / 2.0 - dh * ENFOQUE_Y));
                // Desenfoque barato: achicar y volver a agrandar con interpolación suave
                BufferedImage chica = escalarConCalidad(MAPA, Math.max(1, dw / 6), Math.max(1, dh / 6), false);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(chica, x, y, dw, dh, null);
            }
            // Oscurecer: más arriba y abajo, un poco menos en el centro
            g.setPaint(new LinearGradientPaint(0, 0, 0, h, new float[]{0f, 0.45f, 1f},
                    new Color[]{new Color(5, 9, 16, 215), new Color(8, 14, 24, 135), new Color(5, 9, 16, 225)}));
            g.fillRect(0, 0, w, h);
            // Viñeta en los costados
            g.setPaint(new RadialGradientPaint(w / 2f, h / 2f, Math.max(w, h) * 0.75f,
                    new float[]{0.55f, 1f}, new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 170)}));
            g.fillRect(0, 0, w, h);
            g.dispose();
            return img;
        }
    }
}

package ajedrez.adaptadores.ui.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa transparente (glass pane) que dibuja textos flotantes animados,
 * por ejemplo "+15 XP" cuando el jugador gana experiencia.
 * Solo se hace visible mientras hay animaciones activas, para no interferir con el mouse.
 */
class CapaAnimacionXP extends JComponent {

    private static final int DURACION_MS = 1400;
    private static final int SUBIDA_PX   = 60;
    private static final java.awt.Color COLOR_DORADO  = new java.awt.Color(240, 200, 70);
    private static final java.awt.Color COLOR_DETALLE = new java.awt.Color(245, 240, 230);
    private static final java.awt.Color COLOR_BORDE   = new java.awt.Color(25, 20, 15, 230);

    private static class TextoFlotante {
        final String texto;
        final String detalle;
        final Point centro;
        final long inicio;
        final float tamano;
        final java.awt.Color color;

        TextoFlotante(String texto, String detalle, Point centro, long inicio, float tamano, java.awt.Color color) {
            this.texto = texto;
            this.detalle = detalle;
            this.centro = centro;
            this.inicio = inicio;
            this.tamano = tamano;
            this.color = color;
        }

        /** Zona que ocupa el texto durante toda su animación (subida + "pop" incluidos). */
        Rectangle zona() {
            int largo = Math.max(texto.length(), detalle != null ? detalle.length() : 0);
            int ancho = Math.round(largo * tamano * 0.8f) + 40;
            int arriba = Math.round(SUBIDA_PX + tamano * 1.6f);
            int abajo = Math.round(tamano * 2.4f) + 10;
            return new Rectangle(centro.x - ancho / 2, centro.y - arriba, ancho, arriba + abajo);
        }
    }

    private final List<TextoFlotante> textos = new ArrayList<>();
    private final Timer timer = new Timer(16, e -> avanzar());

    CapaAnimacionXP() {
        setOpaque(false);
    }

    /**
     * Agrega un texto flotante centrado en {@code centro} (coordenadas de esta capa).
     * @param detalle texto chico debajo del principal (puede ser null)
     * @param retrasoMs espera antes de empezar, para encadenar varias animaciones
     */
    void mostrar(String texto, String detalle, Point centro, int retrasoMs, float tamano) {
        mostrar(texto, detalle, centro, retrasoMs, tamano, COLOR_DORADO);
    }

    void mostrar(String texto, String detalle, Point centro, int retrasoMs, float tamano, java.awt.Color color) {
        textos.add(new TextoFlotante(texto, detalle, centro, System.currentTimeMillis() + retrasoMs, tamano, color));
        setVisible(true);
        if (!timer.isRunning()) timer.start();
    }

    /**
     * Avanza un cuadro. Solo se repinta la zona de cada texto: repintar toda la capa obligaría
     * a redibujar la ventana entera 60 veces por segundo (muy lento en pantalla completa).
     */
    private void avanzar() {
        long ahora = System.currentTimeMillis();
        for (TextoFlotante t : textos) {
            repaint(t.zona());
        }
        textos.removeIf(t -> ahora - t.inicio > DURACION_MS);
        if (textos.isEmpty()) {
            timer.stop();
            setVisible(false);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        long ahora = System.currentTimeMillis();
        for (TextoFlotante t : textos) {
            long transcurrido = ahora - t.inicio;
            if (transcurrido < 0) continue;
            float p = Math.min(1f, transcurrido / (float) DURACION_MS);

            // Sube rápido al principio y frena (ease-out cúbico)
            float subida = 1f - (float) Math.pow(1f - p, 3);
            // Opaco hasta el 55%, después se desvanece
            float alpha = p < 0.55f ? 1f : 1f - (p - 0.55f) / 0.45f;
            // "Pop" inicial: crece un poco de más y vuelve a su tamaño
            float escala;
            if (p < 0.10f)      escala = 0.5f + (p / 0.10f) * 0.7f;
            else if (p < 0.22f) escala = 1.2f - ((p - 0.10f) / 0.12f) * 0.2f;
            else                escala = 1f;

            int y = t.centro.y - Math.round(SUBIDA_PX * subida);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));

            float tamPrincipal = Math.max(6f, t.tamano * escala);
            dibujarTextoConBorde(g2, t.texto, new Font("Segoe UI", Font.BOLD, 1).deriveFont(tamPrincipal),
                    t.centro.x, y, t.color);
            if (t.detalle != null) {
                float tamDetalle = Math.max(6f, t.tamano * 0.5f * escala);
                dibujarTextoConBorde(g2, t.detalle, new Font("Segoe UI", Font.BOLD, 1).deriveFont(tamDetalle),
                        t.centro.x, y + Math.round(tamPrincipal * 1.05f), COLOR_DETALLE);
            }
        }
        g2.dispose();
    }

    private void dibujarTextoConBorde(Graphics2D g2, String s, Font f, int cx, int cy, java.awt.Color relleno) {
        GlyphVector gv = f.createGlyphVector(g2.getFontRenderContext(), s);
        Rectangle2D b = gv.getVisualBounds();
        Shape forma = gv.getOutline(
                (float) (cx - b.getWidth() / 2 - b.getX()),
                (float) (cy - b.getHeight() / 2 - b.getY()));
        g2.setStroke(new BasicStroke(Math.max(2f, f.getSize2D() / 6f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(COLOR_BORDE);
        g2.draw(forma);
        g2.setColor(relleno);
        g2.fill(forma);
    }
}

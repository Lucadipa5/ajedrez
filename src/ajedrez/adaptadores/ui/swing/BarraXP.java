package ajedrez.adaptadores.ui.swing;

import javax.swing.*;
import java.awt.*;

/**
 * Barra de experiencia con la estética del juego (riel oscuro, borde dorado y relleno
 * dorado → hextech). Reemplaza al JProgressBar del sistema, que no combina con el resto.
 */
class BarraXP extends JComponent {
    private int valor = 0;       // 0 a 100
    private String texto = "";

    BarraXP(int alto) {
        setPreferredSize(new Dimension(180, alto));
        setMinimumSize(new Dimension(60, alto));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, alto));
    }

    int getValor() {
        return valor;
    }

    void setValor(int valor) {
        this.valor = Math.max(0, Math.min(100, valor));
        repaint();
    }

    void setTexto(String texto) {
        this.texto = texto;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.suavizar(g2);
        int w = getWidth(), h = getHeight();

        g2.setColor(new Color(5, 9, 14));
        g2.fillRect(0, 0, w, h);

        int lleno = Math.round((w - 4) * valor / 100f);
        if (lleno > 0) {
            g2.setPaint(new GradientPaint(2, 0, new Color(150, 110, 45), w, 0, Tema.DORADO_BRILLO));
            g2.fillRect(2, 2, lleno, h - 4);
            // Reflejo superior
            g2.setColor(new Color(255, 255, 255, 45));
            g2.fillRect(2, 2, lleno, (h - 4) / 2);
            // Punta brillante hextech
            g2.setColor(new Color(10, 200, 185, 200));
            g2.fillRect(2 + lleno - 2, 2, 2, h - 4);
        }

        g2.setColor(Tema.DORADO_OSCURO);
        g2.drawRect(0, 0, w - 1, h - 1);

        if (!texto.isEmpty()) {
            g2.setFont(Tema.texto(Font.BOLD, Math.max(9f, h * 0.62f)));
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(texto)) / 2, ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(new Color(0, 0, 0, 170));
            g2.drawString(texto, tx + 1, ty + 1);
            g2.setColor(Tema.TEXTO);
            g2.drawString(texto, tx, ty);
        }
        g2.dispose();
    }
}

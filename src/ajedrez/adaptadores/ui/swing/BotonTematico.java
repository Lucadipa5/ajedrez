package ajedrez.adaptadores.ui.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Botón con la estética del juego: fondo azul noche, borde dorado y texto en mayúsculas.
 * Puede quedar "seleccionado" (relleno dorado), útil para elegir modo o dificultad.
 */
class BotonTematico extends JButton {
    private boolean hover = false;
    private boolean seleccionado = false;
    private final float tamanoTexto;

    BotonTematico(String texto) {
        this(texto, 13f);
    }

    BotonTematico(String texto, float tamanoTexto) {
        super(texto);
        this.tamanoTexto = tamanoTexto;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(Tema.tituloEspaciado(tamanoTexto, 0.08f));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
        });
    }

    void setSeleccionado(boolean seleccionado) {
        this.seleccionado = seleccionado;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(fm.stringWidth(getText().toUpperCase()) + 36, Math.round(tamanoTexto * 2.6f));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.suavizar(g2);
        int w = getWidth(), h = getHeight();
        boolean activo = isEnabled();
        boolean presionado = getModel().isPressed();
        Shape forma = Tema.octogono(1, 1, w - 2.5, h - 2.5, 5);

        // Fondo
        if (seleccionado) {
            g2.setPaint(new GradientPaint(0, 0, new Color(170, 135, 70), 0, h, new Color(110, 80, 35)));
        } else {
            Color arriba = presionado ? Tema.FONDO_BOTON_2 : (hover && activo ? new Color(30, 44, 58) : Tema.FONDO_BOTON);
            g2.setPaint(new GradientPaint(0, 0, arriba, 0, h, Tema.FONDO_BOTON_2));
        }
        g2.fill(forma);

        // Brillo interior al pasar el mouse
        if (hover && activo && !seleccionado) {
            g2.setPaint(new GradientPaint(0, h, new Color(10, 200, 185, 55), 0, h / 2f, new Color(10, 200, 185, 0)));
            g2.fill(forma);
        }

        // Borde
        Color borde = !activo ? new Color(70, 65, 55)
                : seleccionado || hover ? Tema.DORADO_BRILLO : Tema.DORADO_OSCURO;
        g2.setColor(borde);
        g2.setStroke(new BasicStroke(seleccionado || hover ? 1.6f : 1.2f));
        g2.draw(forma);

        // Texto
        String texto = getText().toUpperCase();
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        Color colorTexto = !activo ? new Color(100, 95, 85)
                : seleccionado ? new Color(20, 16, 10)
                : hover ? Tema.DORADO_BRILLO : Tema.TEXTO;
        g2.setColor(colorTexto);
        g2.drawString(texto, (w - fm.stringWidth(texto)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}

package ajedrez.adaptadores.ui.swing;

import ajedrez.nucleo.dominio.EventoJugada;
import ajedrez.nucleo.dominio.NotacionAlgebraica;
import ajedrez.nucleo.servicio.ObservadorPartida;
import ajedrez.nucleo.servicio.PartidaAjedrez;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Lista de jugadas de la partida en notación algebraica. Es un observador del núcleo:
 * se suscribe a la partida y se actualiza sola con cada jugada, deshacer o reinicio,
 * sin que la ventana tenga que avisarle.
 */
class PanelJugadas extends JPanel implements ObservadorPartida {

    private static final int ALTO_FILA = 22;

    private final List<String> mediasJugadas = new ArrayList<>();
    private final Lista lista = new Lista();
    private final JScrollPane scroll;

    PanelJugadas() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setPreferredSize(new Dimension(200, 0));

        JLabel titulo = new JLabel("JUGADAS", SwingConstants.CENTER);
        titulo.setFont(Tema.tituloEspaciado(12f, 0.25f));
        titulo.setForeground(Tema.DORADO);
        titulo.setBorder(new EmptyBorder(6, 0, 8, 0));

        scroll = new JScrollPane(lista, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUI(new BarraTematica());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(ALTO_FILA);

        add(titulo, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    // -----------------------------------------------------------------------
    // ObservadorPartida
    // -----------------------------------------------------------------------
    @Override
    public void alMover(EventoJugada evento) {
        enHiloDeInterfaz(() -> {
            mediasJugadas.add(NotacionAlgebraica.describir(evento));
            refrescar(true);
        });
    }

    @Override
    public void alRehacer(EventoJugada evento) {
        alMover(evento);
    }

    @Override
    public void alDeshacer(EventoJugada evento) {
        enHiloDeInterfaz(() -> {
            if (!mediasJugadas.isEmpty()) mediasJugadas.remove(mediasJugadas.size() - 1);
            refrescar(true);
        });
    }

    @Override
    public void alReiniciar(PartidaAjedrez partida) {
        enHiloDeInterfaz(() -> {
            mediasJugadas.clear();
            refrescar(false);
        });
    }

    private static void enHiloDeInterfaz(Runnable accion) {
        if (SwingUtilities.isEventDispatchThread()) accion.run();
        else SwingUtilities.invokeLater(accion);
    }

    private void refrescar(boolean irAlFinal) {
        lista.revalidate();
        lista.repaint();
        if (irAlFinal) {
            SwingUtilities.invokeLater(() -> {
                JScrollBar barra = scroll.getVerticalScrollBar();
                barra.setValue(barra.getMaximum());
            });
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.suavizar(g2);
        Tema.pintarMarco(g2, 2, 2, getWidth() - 4, getHeight() - 4);
        g2.dispose();
    }

    /** Las filas "1.  e4   e5", pintadas a mano con el tema. */
    private class Lista extends JComponent {
        Lista() {
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            int filas = Math.max(1, (mediasJugadas.size() + 1) / 2);
            return new Dimension(160, filas * ALTO_FILA + 4);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Tema.suavizar(g2);
            g2.setFont(Tema.texto(Font.PLAIN, 13f));
            FontMetrics fm = g2.getFontMetrics();
            int filas = (mediasJugadas.size() + 1) / 2;
            int ultima = mediasJugadas.size() - 1;
            for (int i = 0; i < filas; i++) {
                int y = i * ALTO_FILA;
                if (i % 2 == 1) {
                    g2.setColor(new Color(255, 255, 255, 10));
                    g2.fillRect(0, y, getWidth(), ALTO_FILA);
                }
                int baseline = y + (ALTO_FILA - fm.getHeight()) / 2 + fm.getAscent();
                g2.setColor(Tema.TEXTO_SUAVE);
                g2.drawString((i + 1) + ".", 6, baseline);
                pintarJugada(g2, 2 * i, 34, baseline, ultima);
                if (2 * i + 1 < mediasJugadas.size()) pintarJugada(g2, 2 * i + 1, 100, baseline, ultima);
            }
            g2.dispose();
        }

        private void pintarJugada(Graphics2D g2, int indice, int x, int baseline, int ultima) {
            g2.setColor(indice == ultima ? Tema.DORADO_BRILLO : Tema.TEXTO);
            g2.setFont(Tema.texto(indice == ultima ? Font.BOLD : Font.PLAIN, 13f));
            g2.drawString(mediasJugadas.get(indice), x, baseline);
        }
    }

    /** Barra de desplazamiento mínima: solo el pulgar dorado, sin flechas. */
    private static class BarraTematica extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = Tema.DORADO_OSCURO;
            trackColor = new Color(0, 0, 0, 0);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return botonVacio();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return botonVacio();
        }

        private static JButton botonVacio() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            // sin riel
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create();
            Tema.suavizar(g2);
            g2.setColor(thumbColor);
            g2.fillRoundRect(r.x + 1, r.y, r.width - 2, r.height, 4, 4);
            g2.dispose();
        }
    }
}

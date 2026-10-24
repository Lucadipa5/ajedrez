package ajedrez.adaptadores.ui.swing;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicPasswordFieldUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * Ventanas emergentes con la estética del juego (reemplazan a JOptionPane, que usa el estilo de Windows).
 * Todas comparten el mismo marco: azul noche, borde dorado, título en mayúsculas y botones del tema.
 * Enter acepta (botón por defecto) y Esc cancela; se pueden arrastrar desde cualquier parte del marco.
 */
final class Dialogos {

    private Dialogos() {}

    /** El tipo cambia el color del acento del título. */
    enum Tipo {
        INFO(Tema.DORADO), ERROR(new Color(230, 70, 55)), ADVERTENCIA(new Color(235, 165, 60)), PREGUNTA(Tema.HEXTECH);

        final Color acento;

        Tipo(Color acento) {
            this.acento = acento;
        }
    }

    // -----------------------------------------------------------------------
    // API
    // -----------------------------------------------------------------------
    static void mensaje(Component padre, String titulo, String texto, Tipo tipo) {
        mostrar(padre, titulo, tipo, cuerpoTexto(texto), new String[]{"Aceptar"}, 0, null);
    }

    /** @return true si se eligió {@code si} */
    static boolean confirmar(Component padre, String titulo, String texto, Tipo tipo, String si, String no) {
        return mostrar(padre, titulo, tipo, cuerpoTexto(texto), new String[]{no, si}, 1, null) == 1;
    }

    /**
     * Varias opciones como botones (la última es la destacada).
     * @return índice elegido, o -1 si se cerró con Esc
     */
    static int opciones(Component padre, String titulo, String texto, Tipo tipo, String... botones) {
        return mostrar(padre, titulo, tipo, cuerpoTexto(texto), botones, botones.length - 1, null);
    }

    /**
     * Formulario con etiquetas arriba de cada campo.
     * @return true si se aceptó
     */
    static boolean formulario(Component padre, String titulo, String[] etiquetas, JComponent[] campos, String aceptar) {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        for (int i = 0; i < etiquetas.length; i++) {
            JLabel lbl = new JLabel(etiquetas[i].toUpperCase());
            lbl.setFont(Tema.tituloEspaciado(11f, 0.18f));
            lbl.setForeground(Tema.DORADO);
            lbl.setBorder(new EmptyBorder(4, 0, 0, 0)); // espacio para tildes de mayúsculas
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            campos[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            campos[i].setMaximumSize(new Dimension(Integer.MAX_VALUE, campos[i].getPreferredSize().height));
            if (i > 0) form.add(Box.createVerticalStrut(12));
            form.add(lbl);
            form.add(Box.createVerticalStrut(5));
            form.add(campos[i]);
        }
        form.setPreferredSize(new Dimension(320, form.getPreferredSize().height));
        return mostrar(padre, titulo, Tipo.INFO, form, new String[]{"Cancelar", aceptar}, 1,
                campos.length > 0 ? campos[0] : null) == 1;
    }

    /**
     * Elegir entre opciones que se muestran como baldosas con imagen (por ejemplo, la coronación).
     * @return índice elegido, o -1 si se cerró con Esc
     */
    static int elegirConImagenes(Component padre, String titulo, String texto, String[] nombres, BufferedImage[] imagenes) {
        JPanel cuerpo = new JPanel(new BorderLayout(0, 14));
        cuerpo.setOpaque(false);
        cuerpo.add(cuerpoTexto(texto), BorderLayout.NORTH);
        JPanel fila = new JPanel(new GridLayout(1, nombres.length, 10, 0));
        fila.setOpaque(false);
        int[] elegido = {-1};
        for (int i = 0; i < nombres.length; i++) {
            int indice = i;
            BotonImagen b = new BotonImagen(nombres[i], imagenes[i]);
            b.addActionListener(e -> {
                elegido[0] = indice;
                SwingUtilities.getWindowAncestor(b).dispose();
            });
            fila.add(b);
        }
        cuerpo.add(fila, BorderLayout.CENTER);
        mostrar(padre, titulo, Tipo.PREGUNTA, cuerpo, new String[0], -1, null);
        return elegido[0];
    }

    static JTextField campoTexto() {
        JTextField campo = new JTextField(18);
        campo.setUI(new BasicTextFieldUI());
        estilizarCampo(campo);
        return campo;
    }

    static JPasswordField campoContrasena() {
        JPasswordField campo = new JPasswordField(18);
        campo.setUI(new BasicPasswordFieldUI());
        estilizarCampo(campo);
        return campo;
    }

    // -----------------------------------------------------------------------
    // Construcción
    // -----------------------------------------------------------------------
    private static void estilizarCampo(JTextField campo) {
        campo.setFont(Tema.texto(Font.PLAIN, 14f));
        campo.setBackground(new Color(5, 9, 14));
        campo.setForeground(Tema.TEXTO);
        campo.setCaretColor(Tema.DORADO_BRILLO);
        campo.setSelectionColor(new Color(120, 90, 40));
        campo.setSelectedTextColor(Tema.TEXTO);
        Border normal = new CompoundBorder(BorderFactory.createLineBorder(Tema.DORADO_OSCURO), new EmptyBorder(7, 9, 7, 9));
        Border enfocado = new CompoundBorder(BorderFactory.createLineBorder(Tema.DORADO_BRILLO), new EmptyBorder(7, 9, 7, 9));
        campo.setBorder(normal);
        campo.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { campo.setBorder(enfocado); }
            @Override public void focusLost(FocusEvent e)   { campo.setBorder(normal); }
        });
    }

    private static JComponent cuerpoTexto(String texto) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        for (String linea : texto.split("\n")) {
            JLabel lbl = new JLabel(linea.isEmpty() ? " " : linea);
            lbl.setFont(Tema.texto(Font.PLAIN, 14f));
            lbl.setForeground(Tema.TEXTO);
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(lbl);
            panel.add(Box.createVerticalStrut(3));
        }
        return panel;
    }

    /**
     * Arma y muestra la ventana (modal). Devuelve el índice del botón elegido, o -1 si se cerró con Esc.
     */
    private static int mostrar(Component padre, String titulo, Tipo tipo, JComponent cuerpo,
                               String[] botones, int porDefecto, JComponent foco) {
        Window dueño = padre instanceof Window w ? w : (padre != null ? SwingUtilities.getWindowAncestor(padre) : null);
        JDialog dialogo = new JDialog(dueño, titulo, Dialog.ModalityType.APPLICATION_MODAL);
        dialogo.setUndecorated(true);
        boolean transparente = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT);
        if (transparente) dialogo.setBackground(new Color(0, 0, 0, 0));

        int[] resultado = {-1};
        JPanel marco = new JPanel(new BorderLayout(0, 16)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Tema.suavizar(g2);
                int w = getWidth(), h = getHeight();
                if (!transparente) {
                    g2.setColor(Tema.FONDO_OSCURO);
                    g2.fillRect(0, 0, w, h);
                }
                // Fondo opaco con el mismo contorno que los marcos del juego
                g2.setPaint(new GradientPaint(0, 0, new Color(16, 26, 40), 0, h, Tema.FONDO_OSCURO));
                g2.fill(Tema.octogono(0.5, 0.5, w - 1, h - 1, 10));
                Tema.pintarMarco(g2, 0, 0, w, h);
                // Acento del tipo de mensaje bajo el título
                g2.setPaint(new GradientPaint(24, 0, tipo.acento, w * 0.7f, 0, new Color(0, 0, 0, 0)));
                g2.fillRect(24, 54, w - 48, 2);
                g2.setColor(tipo.acento);
                Tema.rombo(g2, 24, 55, 4);
                g2.dispose();
            }
        };
        marco.setOpaque(false);
        marco.setBorder(new EmptyBorder(20, 26, 22, 26));

        JLabel lblTitulo = new JLabel(titulo.toUpperCase());
        lblTitulo.setFont(Tema.tituloEspaciado(17f, 0.12f));
        lblTitulo.setForeground(Tema.TEXTO);
        // Margen arriba: las tildes de las mayúsculas (Ó, Ñ) sobresalen de la altura normal de la letra
        lblTitulo.setBorder(new EmptyBorder(5, 0, 5, 0));
        marco.add(lblTitulo, BorderLayout.NORTH);
        marco.add(cuerpo, BorderLayout.CENTER);

        if (botones.length > 0) {
            JPanel filaBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            filaBotones.setOpaque(false);
            for (int i = 0; i < botones.length; i++) {
                int indice = i;
                BotonTematico b = new BotonTematico(botones[i], 12f);
                b.setSeleccionado(i == porDefecto);
                b.setPreferredSize(new Dimension(Math.max(110, b.getPreferredSize().width), 34));
                b.addActionListener(e -> {
                    resultado[0] = indice;
                    dialogo.dispose();
                });
                filaBotones.add(b);
                if (i == porDefecto) dialogo.getRootPane().setDefaultButton(b);
            }
            marco.add(filaBotones, BorderLayout.SOUTH);
        }

        // Esc cierra sin elegir
        dialogo.getRootPane().registerKeyboardAction(e -> dialogo.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Arrastrar la ventana desde el marco
        MouseAdapter arrastre = new MouseAdapter() {
            private Point inicio;
            @Override public void mousePressed(MouseEvent e) { inicio = e.getPoint(); }
            @Override public void mouseDragged(MouseEvent e) {
                Point p = dialogo.getLocation();
                dialogo.setLocation(p.x + e.getX() - inicio.x, p.y + e.getY() - inicio.y);
            }
        };
        marco.addMouseListener(arrastre);
        marco.addMouseMotionListener(arrastre);

        dialogo.setContentPane(marco);
        dialogo.pack();
        dialogo.setSize(Math.max(380, dialogo.getWidth()), dialogo.getHeight());
        dialogo.setLocationRelativeTo(dueño);
        if (foco != null) {
            dialogo.addWindowListener(new WindowAdapter() {
                @Override public void windowOpened(WindowEvent e) { foco.requestFocusInWindow(); }
            });
        }
        dialogo.setVisible(true); // bloquea hasta que se cierre
        return resultado[0];
    }

    /** Baldosa clickeable con una imagen y su nombre (para elegir pieza). */
    private static class BotonImagen extends JButton {
        private static final BufferedImage BALDOSA = Tema.cargarImagen("baldosa_blanca.png");
        private final BufferedImage imagen;
        private final String nombre;
        private boolean hover;

        BotonImagen(String nombre, BufferedImage imagen) {
            this.nombre = nombre;
            this.imagen = imagen;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(96, 124));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Tema.suavizar(g2);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            int w = getWidth();
            int lado = Math.min(w - 6, getHeight() - 30);
            int x = (w - lado) / 2, y = hover ? 1 : 3;
            if (BALDOSA != null) g2.drawImage(BALDOSA, x, y, lado, lado, null);
            if (imagen != null) {
                int alto = (int) (lado * 0.8);
                int ancho = imagen.getWidth() * alto / imagen.getHeight();
                g2.drawImage(imagen, (w - ancho) / 2, y + (lado - alto) / 2, ancho, alto, null);
            }
            if (hover) {
                g2.setColor(Tema.DORADO_BRILLO);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRect(x, y, lado, lado);
            }
            g2.setFont(Tema.tituloEspaciado(12f, 0.12f));
            FontMetrics fm = g2.getFontMetrics();
            String t = nombre.toUpperCase();
            g2.setColor(hover ? Tema.DORADO_BRILLO : Tema.TEXTO);
            g2.drawString(t, (w - fm.stringWidth(t)) / 2, getHeight() - 6);
            g2.dispose();
        }
    }
}

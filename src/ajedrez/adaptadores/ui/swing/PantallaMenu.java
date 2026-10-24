package ajedrez.adaptadores.ui.swing;

import ajedrez.adaptadores.ui.swing.perfil.GestorPerfiles;
import ajedrez.adaptadores.ui.swing.perfil.PerfilJugador;
import ajedrez.nucleo.dominio.Color;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;

/**
 * Pantalla de menú principal.
 * Permite crear/seleccionar perfil de jugador y elegir el color con el que se juega
 * antes de iniciar la partida.
 */
public class PantallaMenu extends JFrame {

    // Paleta de colores oscura estilo ajedrez
    private static final java.awt.Color BG_DARK      = new java.awt.Color(30, 28, 25);
    private static final java.awt.Color BG_CARD      = new java.awt.Color(45, 43, 40);
    private static final java.awt.Color COLOR_DORADO  = new java.awt.Color(212, 175, 55);
    private static final java.awt.Color COLOR_TEXTO   = new java.awt.Color(235, 230, 220);
    private static final java.awt.Color COLOR_SUBTXT  = new java.awt.Color(160, 155, 145);
    private static final java.awt.Color BTN_BLANCO    = new java.awt.Color(240, 230, 200);
    private static final java.awt.Color BTN_NEGRO     = new java.awt.Color(55, 50, 45);
    private static final java.awt.Color BTN_HOVER_BL  = new java.awt.Color(255, 245, 215);
    private static final java.awt.Color BTN_HOVER_NG  = new java.awt.Color(70, 65, 60);

    private PerfilJugador perfilSeleccionado = null;
    private JLabel lblPerfilActual;
    private JLabel lblNivelActual;
    private JProgressBar barraExp;

    public PantallaMenu() {
        setTitle("Ajedrez — Menú Principal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 680);
        setLocationRelativeTo(null);
        setResizable(false);

        // Ícono de la ventana (rey de ajedrez dibujado programáticamente)
        setIconImage(crearIconoAjedrez());

        setContentPane(construirPanelPrincipal());
    }

    // -----------------------------------------------------------------------
    // Construcción del panel principal
    // -----------------------------------------------------------------------
    private JPanel construirPanelPrincipal() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBackground(BG_DARK);

        panel.add(construirPanelTitulo(), BorderLayout.NORTH);
        panel.add(construirPanelCentro(), BorderLayout.CENTER);
        panel.add(construirPanelCreditos(), BorderLayout.SOUTH);

        return panel;
    }

    // -----------------------------------------------------------------------
    // Panel superior: título y ícono decorativo
    // -----------------------------------------------------------------------
    private JPanel construirPanelTitulo() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(40, 20, 20, 20));

        // Pieza decorativa grande
        JLabel icono = new JLabel("♔", SwingConstants.CENTER);
        icono.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 90));
        icono.setForeground(COLOR_DORADO);
        icono.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titulo = new JLabel("AJEDREZ", SwingConstants.CENTER);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 38));
        titulo.setForeground(COLOR_TEXTO);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitulo = new JLabel("Ingeniería de Software", SwingConstants.CENTER);
        subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitulo.setForeground(COLOR_DORADO);
        subtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(icono);
        panel.add(Box.createVerticalStrut(8));
        panel.add(titulo);
        panel.add(Box.createVerticalStrut(4));
        panel.add(subtitulo);

        return panel;
    }

    // -----------------------------------------------------------------------
    // Panel central: perfil + botones de juego
    // -----------------------------------------------------------------------
    private JPanel construirPanelCentro() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(10, 40, 10, 40));

        panel.add(construirTarjetaPerfil());
        panel.add(Box.createVerticalStrut(25));
        panel.add(construirSeparador("NUEVA PARTIDA"));
        panel.add(Box.createVerticalStrut(15));
        panel.add(construirBotonesColor());

        return panel;
    }

    // -----------------------------------------------------------------------
    // Tarjeta de perfil
    // -----------------------------------------------------------------------
    private JPanel construirTarjetaPerfil() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_DORADO, 1),
                new EmptyBorder(15, 20, 15, 20)
        ));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.setMaximumSize(new Dimension(440, 200));

        // Fila: ícono perfil + info + botón cambiar
        JPanel filaInfo = new JPanel(new BorderLayout(10, 0));
        filaInfo.setBackground(BG_CARD);

        JLabel iconoPerfil = new JLabel("👤");
        iconoPerfil.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        iconoPerfil.setForeground(COLOR_DORADO);

        JPanel infoTexto = new JPanel();
        infoTexto.setLayout(new BoxLayout(infoTexto, BoxLayout.Y_AXIS));
        infoTexto.setBackground(BG_CARD);

        lblPerfilActual = new JLabel("Sin perfil seleccionado");
        lblPerfilActual.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblPerfilActual.setForeground(COLOR_TEXTO);

        lblNivelActual = new JLabel("Seleccioná o creá un perfil para continuar");
        lblNivelActual.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblNivelActual.setForeground(COLOR_SUBTXT);

        infoTexto.add(lblPerfilActual);
        infoTexto.add(Box.createVerticalStrut(3));
        infoTexto.add(lblNivelActual);

        JPanel botonesGestion = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        botonesGestion.setBackground(BG_CARD);

        JButton btnNuevoPerfil = crearBotonSecundario("＋ Nuevo");
        btnNuevoPerfil.addActionListener(e -> onCrearPerfil());

        JButton btnCargarPerfil = crearBotonSecundario("📂 Cargar");
        btnCargarPerfil.addActionListener(e -> onCargarPerfil());

        botonesGestion.add(btnNuevoPerfil);
        botonesGestion.add(btnCargarPerfil);

        filaInfo.add(iconoPerfil, BorderLayout.WEST);
        filaInfo.add(infoTexto, BorderLayout.CENTER);
        filaInfo.add(botonesGestion, BorderLayout.EAST);

        // Barra de experiencia
        JPanel filaBarra = new JPanel(new BorderLayout(0, 4));
        filaBarra.setBackground(BG_CARD);
        filaBarra.setBorder(new EmptyBorder(12, 0, 0, 0));

        JLabel lblExpTitulo = new JLabel("Experiencia");
        lblExpTitulo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblExpTitulo.setForeground(COLOR_SUBTXT);

        barraExp = new JProgressBar(0, 100);
        barraExp.setValue(0);
        barraExp.setStringPainted(true);
        barraExp.setString("0 / 0 XP");
        barraExp.setFont(new Font("Segoe UI", Font.BOLD, 11));
        barraExp.setForeground(COLOR_DORADO);
        barraExp.setBackground(new java.awt.Color(20, 18, 15));
        barraExp.setBorder(BorderFactory.createLineBorder(new java.awt.Color(80, 75, 70)));
        barraExp.setPreferredSize(new Dimension(0, 22));

        filaBarra.add(lblExpTitulo, BorderLayout.NORTH);
        filaBarra.add(barraExp, BorderLayout.CENTER);

        card.add(filaInfo);
        card.add(filaBarra);

        return card;
    }

    // -----------------------------------------------------------------------
    // Separador con título
    // -----------------------------------------------------------------------
    private JPanel construirSeparador(String texto) {
        JPanel sep = new JPanel(new BorderLayout(10, 0));
        sep.setBackground(BG_DARK);
        sep.setMaximumSize(new Dimension(440, 20));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSeparator izq = new JSeparator();
        izq.setForeground(COLOR_DORADO);

        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(COLOR_DORADO);

        JSeparator der = new JSeparator();
        der.setForeground(COLOR_DORADO);

        sep.add(izq, BorderLayout.WEST);
        sep.add(lbl, BorderLayout.CENTER);
        sep.add(der, BorderLayout.EAST);
        return sep;
    }

    // -----------------------------------------------------------------------
    // Botones de selección de color (Blancas / Negras)
    // -----------------------------------------------------------------------
    private JPanel construirBotonesColor() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 0));
        panel.setBackground(BG_DARK);
        panel.setMaximumSize(new Dimension(440, 130));
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnBlancas = crearBotonColor("♔", "Jugar con\nBlancas",
                BTN_BLANCO, new java.awt.Color(50, 45, 40), BTN_HOVER_BL);
        btnBlancas.addActionListener(e -> iniciarPartida(Color.BLANCO));

        JButton btnNegras = crearBotonColor("♚", "Jugar con\nNegras",
                BTN_NEGRO, COLOR_DORADO, BTN_HOVER_NG);
        btnNegras.addActionListener(e -> iniciarPartida(Color.NEGRO));

        panel.add(btnBlancas);
        panel.add(btnNegras);
        return panel;
    }

    // -----------------------------------------------------------------------
    // Créditos
    // -----------------------------------------------------------------------
    private JPanel construirPanelCreditos() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(10, 0, 15, 0));
        JLabel lbl = new JLabel("Ezequiel Vales — Ingeniería de Software");
        lbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lbl.setForeground(COLOR_SUBTXT);
        panel.add(lbl);
        return panel;
    }

    // -----------------------------------------------------------------------
    // Lógica de eventos
    // -----------------------------------------------------------------------
    private void onCrearPerfil() {
        String nombre = JOptionPane.showInputDialog(this,
                "Ingresá tu nombre de usuario:",
                "Crear Perfil", JOptionPane.PLAIN_MESSAGE);
        if (nombre != null && !nombre.trim().isEmpty()) {
            perfilSeleccionado = GestorPerfiles.crearPerfil(nombre.trim());
            actualizarTarjetaPerfil();
        }
    }

    private void onCargarPerfil() {
        List<PerfilJugador> perfiles = GestorPerfiles.cargarPerfiles();
        if (perfiles.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No hay perfiles guardados.\nCreá uno primero.",
                    "Sin perfiles", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] nombres = perfiles.stream()
                .map(p -> p.getNombre() + " [Nv." + p.getNivel() + " | " + p.getPuntosAcumulados() + " pts]")
                .toArray(String[]::new);

        String seleccion = (String) JOptionPane.showInputDialog(this,
                "Seleccioná tu perfil:",
                "Cargar Perfil",
                JOptionPane.PLAIN_MESSAGE,
                null,
                nombres,
                nombres[0]);

        if (seleccion != null) {
            int idx = java.util.Arrays.asList(nombres).indexOf(seleccion);
            perfilSeleccionado = perfiles.get(idx);
            actualizarTarjetaPerfil();
        }
    }

    private void actualizarTarjetaPerfil() {
        if (perfilSeleccionado == null) return;
        lblPerfilActual.setText(perfilSeleccionado.getNombre());
        lblNivelActual.setText("Nivel " + perfilSeleccionado.getNivel()
                + "  •  " + perfilSeleccionado.getPuntosAcumulados() + " pts totales"
                + "  •  " + perfilSeleccionado.getPartidasGanadas() + " victorias");

        int progreso = (int) (perfilSeleccionado.getProgresoBarra() * 100);
        barraExp.setValue(progreso);
        barraExp.setString(perfilSeleccionado.getPuntosEnNivelActual()
                + " / " + perfilSeleccionado.getPuntosParaSiguienteNivel() + " XP");
    }

    private void iniciarPartida(Color colorJugador) {
        if (perfilSeleccionado == null) {
            int resp = JOptionPane.showConfirmDialog(this,
                    "No hay ningún perfil seleccionado.\n¿Querés jugar sin perfil (sin guardar progreso)?",
                    "Sin perfil", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (resp != JOptionPane.YES_OPTION) return;
        }

        VentanaAjedrez ventana = new VentanaAjedrez(perfilSeleccionado, colorJugador, this);
        ventana.setVisible(true);
        this.setVisible(false);
    }

    // -----------------------------------------------------------------------
    // Actualizar la tarjeta de perfil desde la ventana de juego al terminar
    // -----------------------------------------------------------------------
    public void onPartidaTerminada() {
        actualizarTarjetaPerfil();
        this.setVisible(true);
    }

    // -----------------------------------------------------------------------
    // Utilidades de creación de componentes estilizados
    // -----------------------------------------------------------------------
    private JButton crearBotonColor(String simbolo, String texto,
                                     java.awt.Color bgColor, java.awt.Color fgColor,
                                     java.awt.Color hoverColor) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                // Símbolo
                g2.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 36));
                g2.setColor(fgColor);
                FontMetrics fm = g2.getFontMetrics();
                int sx = (getWidth() - fm.stringWidth(simbolo)) / 2;
                g2.drawString(simbolo, sx, 48);

                // Texto multiline
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                g2.setColor(fgColor);
                String[] lineas = texto.split("\n");
                FontMetrics fm2 = g2.getFontMetrics();
                int y = 70;
                for (String linea : lineas) {
                    int lx = (getWidth() - fm2.stringWidth(linea)) / 2;
                    g2.drawString(linea, lx, y);
                    y += fm2.getHeight();
                }
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(200, 110));
        btn.setBackground(bgColor);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(hoverColor); btn.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(bgColor);    btn.repaint(); }
        });
        return btn;
    }

    private JButton crearBotonSecundario(String texto) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(COLOR_DORADO);
        btn.setBackground(new java.awt.Color(60, 58, 55));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(5, 10, 5, 10));
        return btn;
    }

    // -----------------------------------------------------------------------
    // Ícono programático: rey de ajedrez dorado sobre fondo oscuro
    // -----------------------------------------------------------------------
    public static Image crearIconoAjedrez() {
        // Primero intentamos cargar icono.png si existe en el directorio de trabajo
        File archivoIcono = new File("icono.png");
        if (archivoIcono.exists()) {
            try {
                return new ImageIcon(archivoIcono.getAbsolutePath()).getImage();
            } catch (Exception ignored) {}
        }

        // Si no hay archivo, generamos un ícono programático
        int size = 64;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fondo circular oscuro
        g2.setColor(new java.awt.Color(40, 38, 35));
        g2.fillOval(2, 2, size - 4, size - 4);

        // Borde dorado
        g2.setColor(COLOR_DORADO);
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawOval(2, 2, size - 4, size - 4);

        // Símbolo del rey
        g2.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 42));
        g2.setColor(COLOR_DORADO);
        FontMetrics fm = g2.getFontMetrics();
        String sym = "♔";
        int x = (size - fm.stringWidth(sym)) / 2;
        int y = (size - fm.getHeight()) / 2 + fm.getAscent() - 2;
        g2.drawString(sym, x, y);

        g2.dispose();
        return img;
    }
}

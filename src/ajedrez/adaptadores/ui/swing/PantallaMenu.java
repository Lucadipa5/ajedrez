package ajedrez.adaptadores.ui.swing;

import ajedrez.adaptadores.ia.MotorPython;
import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.ia.Dificultad;
import ajedrez.nucleo.ia.MotorAjedrez;
import ajedrez.nucleo.perfil.PerfilJugador;
import ajedrez.nucleo.perfil.ServicioCuentas;
import ajedrez.nucleo.servicio.PartidaAjedrez;
import ajedrez.nucleo.servicio.SesionDePartida;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/**
 * Pantalla de menú principal, con la misma estética que la partida (Summoner's Rift):
 * fondo del mapa desenfocado, marcos azul noche con borde dorado y botones del tema.
 * Permite iniciar sesión o registrarse, elegir el modo (2 jugadores / contra la IA)
 * y el color con el que se juega.
 */
public class PantallaMenu extends JFrame {

    /**
     * Busca una fuente que pueda renderizar símbolos Unicode de ajedrez (♔♕♖♗♘♙).
     * Prueba varias fuentes conocidas y selecciona la primera disponible.
     */
    static String obtenerFuenteAjedrez() {
        String[] candidatas = {
            "Segoe UI Symbol", "Arial Unicode MS", "DejaVu Sans",
            "Noto Sans Symbols2", "Noto Sans Symbols", "Symbola",
            "Segoe UI Emoji", "Apple Symbols", "Lucida Sans Unicode"
        };
        char testChar = '♔'; // ♔ rey blanco
        for (String nombre : candidatas) {
            Font f = new Font(nombre, Font.PLAIN, 12);
            if (f.canDisplay(testChar)) {
                return nombre;
            }
        }
        // Fallback: recorrer todas las fuentes del sistema
        for (String nombre : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            Font f = new Font(nombre, Font.PLAIN, 12);
            if (f.canDisplay(testChar)) {
                return nombre;
            }
        }
        return "Segoe UI Symbol"; // último recurso
    }

    static final String FUENTE_AJEDREZ = obtenerFuenteAjedrez();

    private static final int ANCHO_CONTENIDO = 460;
    private static final int LARGO_MAX_USUARIO = 20;
    private static final int LARGO_MIN_CONTRASENA = 4;

    private final ServicioCuentas cuentas;
    private PerfilJugador perfilSeleccionado = null;

    // Tarjeta de perfil
    private JLabel lblPerfilActual;
    private JLabel lblNivelActual;
    private AvatarJugador avatar;
    private BarraXP barraExp;
    private BotonTematico btnIniciarSesion;
    private BotonTematico btnRegistrarse;
    private BotonTematico btnCambiarFoto;
    private BotonTematico btnCerrarSesion;

    // Modo y dificultad
    private boolean contraIA = false;
    private Dificultad dificultadElegida = Dificultad.MEDIA;
    private BotonTematico btnModoDosJugadores;
    private BotonTematico btnModoIA;
    private JLabel lblDificultad;
    private final Map<Dificultad, BotonTematico> botonesDificultad = new EnumMap<>(Dificultad.class);

    public PantallaMenu(ServicioCuentas cuentas) {
        this.cuentas = cuentas;
        setTitle("Ajedrez — Menú Principal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setIconImage(crearIconoAjedrez());

        JPanel fondo = new Tema.PanelFondo(new BorderLayout());
        fondo.add(construirEncabezado(), BorderLayout.NORTH);
        fondo.add(construirPanelCentro(), BorderLayout.CENTER);
        fondo.add(construirPanelCreditos(), BorderLayout.SOUTH);
        setContentPane(fondo);

        setSize(560, 800);
        setLocationRelativeTo(null);
        actualizarTarjetaPerfil();
    }

    // -----------------------------------------------------------------------
    // Encabezado: los dos reyes a los costados del título
    // -----------------------------------------------------------------------
    private JComponent construirEncabezado() {
        BufferedImage reyBlanco = Tema.cargarImagen("rey_blanco.png");
        BufferedImage reyNegro = Tema.cargarImagen("rey_negro.png");
        JComponent encabezado = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Tema.suavizar(g2);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                int w = getWidth();
                int cx = w / 2;

                // Título con relleno dorado degradado y contorno oscuro
                Font fuente = Tema.tituloEspaciado(58f, 0.06f);
                Shape titulo = fuente.createGlyphVector(g2.getFontRenderContext(), "AJEDREZ").getOutline();
                Rectangle bt = titulo.getBounds();
                int tx = cx - bt.width / 2 - bt.x, ty = 112;
                g2.translate(tx, ty);
                g2.setColor(new java.awt.Color(0, 0, 0, 160));
                g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(titulo);
                g2.setPaint(new GradientPaint(0, bt.y, Tema.DORADO_BRILLO, 0, bt.y + bt.height, new java.awt.Color(150, 110, 50)));
                g2.fill(titulo);
                g2.translate(-tx, -ty);

                // Reyes a los costados, con un halo
                int alto = 92;
                dibujarRey(g2, reyBlanco, cx - bt.width / 2 - 62, 40, alto, new java.awt.Color(10, 200, 185, 70));
                dibujarRey(g2, reyNegro, cx + bt.width / 2 + 62, 40, alto, new java.awt.Color(220, 60, 50, 70));

                Tema.pintarSeparador(g2, cx - 170, 140, 340, null);
                g2.setFont(Tema.tituloEspaciado(13f, 0.3f));
                g2.setColor(Tema.DORADO);
                String sub = "INGENIERÍA DE SOFTWARE";
                g2.drawString(sub, cx - g2.getFontMetrics().stringWidth(sub) / 2, 166);
                g2.dispose();
            }

            private void dibujarRey(Graphics2D g2, BufferedImage img, int cx, int y, int alto, java.awt.Color halo) {
                if (img == null) return;
                int ancho = img.getWidth() * alto / img.getHeight();
                g2.setPaint(new RadialGradientPaint(cx, y + alto / 2f, alto * 0.6f,
                        new float[]{0f, 1f}, new java.awt.Color[]{halo, new java.awt.Color(0, 0, 0, 0)}));
                g2.fill(new Ellipse2D.Double(cx - alto * 0.6, y + alto / 2.0 - alto * 0.6, alto * 1.2, alto * 1.2));
                g2.drawImage(img, cx - ancho / 2, y, ancho, alto, null);
            }
        };
        encabezado.setPreferredSize(new Dimension(560, 190));
        return encabezado;
    }

    // -----------------------------------------------------------------------
    // Centro: perfil + nueva partida
    // -----------------------------------------------------------------------
    private JPanel construirPanelCentro() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(6, 0, 0, 0));

        panel.add(construirTarjetaPerfil());
        panel.add(Box.createVerticalStrut(26));
        panel.add(construirSeparador("NUEVA PARTIDA"));
        panel.add(Box.createVerticalStrut(16));
        panel.add(construirSelectorModo());
        panel.add(Box.createVerticalStrut(18));
        panel.add(construirBotonesColor());
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JComponent construirSeparador(String texto) {
        JComponent sep = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Tema.suavizar(g2);
                Tema.pintarSeparador(g2, 0, getHeight() / 2, getWidth(), texto);
                g2.dispose();
            }
        };
        fijarTamano(sep, ANCHO_CONTENIDO, 20);
        return sep;
    }

    private static void fijarTamano(JComponent c, int w, int h) {
        Dimension d = new Dimension(w, h);
        c.setPreferredSize(d);
        c.setMaximumSize(d);
        c.setMinimumSize(d);
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
    }

    // -----------------------------------------------------------------------
    // Tarjeta de perfil
    // -----------------------------------------------------------------------
    private JPanel construirTarjetaPerfil() {
        JPanel card = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Tema.suavizar(g2);
                Tema.pintarMarco(g2, 0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        fijarTamano(card, ANCHO_CONTENIDO, 150);

        // Avatar: con la sesión iniciada, un click permite cambiar la foto de perfil
        avatar = new AvatarJugador(62);
        avatar.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (perfilSeleccionado != null) onCambiarFoto();
            }
        });

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        lblPerfilActual = new JLabel();
        lblPerfilActual.setFont(Tema.titulo(19f));
        lblPerfilActual.setForeground(Tema.TEXTO);
        lblNivelActual = new JLabel();
        lblNivelActual.setFont(Tema.texto(Font.PLAIN, 12f));
        lblNivelActual.setForeground(Tema.TEXTO_SUAVE);
        info.add(Box.createVerticalStrut(4));
        info.add(lblPerfilActual);
        info.add(Box.createVerticalStrut(3));
        info.add(lblNivelActual);

        JPanel botones = new JPanel();
        botones.setOpaque(false);
        botones.setLayout(new BoxLayout(botones, BoxLayout.Y_AXIS));
        btnIniciarSesion = new BotonTematico("Iniciar sesión", 11f);
        btnIniciarSesion.addActionListener(e -> onIniciarSesion());
        btnRegistrarse = new BotonTematico("Registrarse", 11f);
        btnRegistrarse.addActionListener(e -> onRegistrarse());
        btnCambiarFoto = new BotonTematico("Cambiar foto", 11f);
        btnCambiarFoto.addActionListener(e -> onCambiarFoto());
        btnCerrarSesion = new BotonTematico("Cerrar sesión", 11f);
        btnCerrarSesion.addActionListener(e -> onCerrarSesion());
        for (JButton b : new JButton[]{btnIniciarSesion, btnRegistrarse, btnCambiarFoto, btnCerrarSesion}) {
            fijarTamano(b, 138, 30);
        }
        botones.add(btnIniciarSesion);
        botones.add(btnCambiarFoto);
        botones.add(Box.createVerticalStrut(6));
        botones.add(btnRegistrarse);
        botones.add(btnCerrarSesion);

        JPanel filaBarra = new JPanel(new BorderLayout(0, 5));
        filaBarra.setOpaque(false);
        filaBarra.setBorder(new EmptyBorder(12, 0, 0, 0));
        JLabel lblExp = new JLabel("EXPERIENCIA");
        lblExp.setFont(Tema.tituloEspaciado(10f, 0.25f));
        lblExp.setForeground(Tema.DORADO);
        barraExp = new BarraXP(18);
        filaBarra.add(lblExp, BorderLayout.NORTH);
        filaBarra.add(barraExp, BorderLayout.CENTER);

        JPanel arriba = new JPanel(new BorderLayout(14, 0));
        arriba.setOpaque(false);
        arriba.add(avatar, BorderLayout.WEST);
        arriba.add(info, BorderLayout.CENTER);
        arriba.add(botones, BorderLayout.EAST);

        card.add(arriba, BorderLayout.CENTER);
        card.add(filaBarra, BorderLayout.SOUTH);
        return card;
    }

    // -----------------------------------------------------------------------
    // Selector de modo: 2 jugadores / contra la IA (+ dificultad)
    // -----------------------------------------------------------------------
    private JPanel construirSelectorModo() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel filaModos = new JPanel(new GridLayout(1, 2, 12, 0));
        filaModos.setOpaque(false);
        fijarTamano(filaModos, ANCHO_CONTENIDO, 40);
        btnModoDosJugadores = new BotonTematico("2 jugadores", 13f);
        btnModoDosJugadores.addActionListener(e -> elegirModo(false));
        btnModoIA = new BotonTematico("Contra la IA", 13f);
        btnModoIA.addActionListener(e -> elegirModo(true));
        filaModos.add(btnModoDosJugadores);
        filaModos.add(btnModoIA);

        JPanel filaDificultad = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        filaDificultad.setOpaque(false);
        fijarTamano(filaDificultad, ANCHO_CONTENIDO, 30);
        lblDificultad = new JLabel("DIFICULTAD");
        lblDificultad.setFont(Tema.tituloEspaciado(11f, 0.2f));
        lblDificultad.setBorder(new EmptyBorder(0, 0, 0, 6));
        filaDificultad.add(lblDificultad);
        for (Dificultad d : Dificultad.values()) {
            BotonTematico b = new BotonTematico(d.getNombre(), 11f);
            b.setPreferredSize(new Dimension(92, 28));
            b.addActionListener(e -> elegirDificultad(d));
            botonesDificultad.put(d, b);
            filaDificultad.add(b);
        }

        panel.add(filaModos);
        panel.add(Box.createVerticalStrut(10));
        panel.add(filaDificultad);

        elegirModo(false);
        elegirDificultad(Dificultad.MEDIA);
        return panel;
    }

    private void elegirModo(boolean ia) {
        contraIA = ia;
        btnModoDosJugadores.setSeleccionado(!ia);
        btnModoIA.setSeleccionado(ia);
        // La fila de dificultad queda siempre en su lugar (no "salta" el diseño), pero solo se usa contra la IA
        lblDificultad.setForeground(ia ? Tema.DORADO : new java.awt.Color(90, 85, 75));
        botonesDificultad.values().forEach(b -> b.setEnabled(ia));
    }

    private void elegirDificultad(Dificultad d) {
        dificultadElegida = d;
        botonesDificultad.forEach((dif, b) -> b.setSeleccionado(dif == d));
    }

    // -----------------------------------------------------------------------
    // Botones de color: baldosas de piedra con el rey de cada color
    // -----------------------------------------------------------------------
    private JPanel construirBotonesColor() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 0));
        panel.setOpaque(false);
        fijarTamano(panel, ANCHO_CONTENIDO, 170);

        BufferedImage baldosa = Tema.cargarImagen("baldosa_blanca.png");
        JButton btnBlancas = new BotonBaldosa("Blancas", baldosa, Tema.cargarImagen("rey_blanco.png"));
        btnBlancas.addActionListener(e -> iniciarPartida(Color.BLANCO));
        JButton btnNegras = new BotonBaldosa("Negras", oscurecer(baldosa), Tema.cargarImagen("rey_negro.png"));
        btnNegras.addActionListener(e -> iniciarPartida(Color.NEGRO));

        panel.add(btnBlancas);
        panel.add(btnNegras);
        return panel;
    }

    /** Versión oscura de la baldosa (piedra negra) para el botón de Negras. */
    private static BufferedImage oscurecer(BufferedImage img) {
        if (img == null) return null;
        BufferedImage argb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        argb.getGraphics().drawImage(img, 0, 0, null);
        return new RescaleOp(new float[]{0.30f, 0.32f, 0.38f, 1f}, new float[]{0, 0, 0, 0}, null).filter(argb, null);
    }

    private static class BotonBaldosa extends JButton {
        private final BufferedImage baldosa;
        private final BufferedImage rey;
        private final String color;
        private boolean hover;
        private BufferedImage baldosaEscalada, reyEscalado;

        BotonBaldosa(String color, BufferedImage baldosa, BufferedImage rey) {
            this.color = color;
            this.baldosa = baldosa;
            this.rey = rey;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Tema.suavizar(g2);
            int w = getWidth(), h = getHeight();
            int elevar = hover ? 3 : 0;
            int alturaCartel = 46;
            int lado = Math.min(w - 8, h - alturaCartel + 10);
            int bx = (w - lado) / 2, by = 4 - elevar;

            // Halo al pasar el mouse
            if (hover) {
                g2.setPaint(new RadialGradientPaint(w / 2f, by + lado / 2f, lado * 0.75f, new float[]{0f, 1f},
                        new java.awt.Color[]{new java.awt.Color(240, 210, 140, 90), new java.awt.Color(0, 0, 0, 0)}));
                g2.fillRect(0, 0, w, h);
            }
            if (baldosa != null) {
                if (baldosaEscalada == null || baldosaEscalada.getWidth() != lado) {
                    baldosaEscalada = Tema.escalarConCalidad(baldosa, lado, lado, true);
                }
                g2.drawImage(baldosaEscalada, bx, by, null);
            }
            if (rey != null) {
                int alto = (int) (lado * 0.78);
                int ancho = rey.getWidth() * alto / rey.getHeight();
                if (reyEscalado == null || reyEscalado.getHeight() != alto) {
                    reyEscalado = Tema.escalarConCalidad(rey, ancho, alto, true);
                }
                g2.setColor(new java.awt.Color(0, 0, 0, 80));
                g2.fill(new Ellipse2D.Double(w / 2.0 - ancho * 0.3, by + lado * 0.86, ancho * 0.6, lado * 0.08));
                g2.drawImage(reyEscalado, (w - ancho) / 2, by + (lado - alto) / 2 - 4, null);
            }

            // Cartel con el texto, estilo placa del marco
            int cy = h - alturaCartel;
            Shape placa = Tema.octogono(6, cy, w - 12, alturaCartel - 4, 8);
            g2.setColor(new java.awt.Color(10, 16, 26, 235));
            g2.fill(placa);
            g2.setColor(hover ? Tema.DORADO_BRILLO : Tema.DORADO_OSCURO);
            g2.setStroke(new BasicStroke(hover ? 1.8f : 1.3f));
            g2.draw(placa);
            g2.setFont(Tema.tituloEspaciado(10f, 0.25f));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(Tema.TEXTO_SUAVE);
            g2.drawString("JUGAR CON", (w - fm.stringWidth("JUGAR CON")) / 2, cy + 16);
            g2.setFont(Tema.tituloEspaciado(17f, 0.12f));
            fm = g2.getFontMetrics();
            String t = color.toUpperCase();
            g2.setColor(hover ? Tema.DORADO_BRILLO : Tema.TEXTO);
            g2.drawString(t, (w - fm.stringWidth(t)) / 2, cy + 36);
            g2.dispose();
        }
    }

    // -----------------------------------------------------------------------
    // Créditos
    // -----------------------------------------------------------------------
    private JComponent construirPanelCreditos() {
        JLabel lbl = new JLabel("Proyecto Ajedrez - Ingeniería de Software 2026", SwingConstants.CENTER);
        lbl.setFont(Tema.titulo(12f).deriveFont(Font.ITALIC));
        lbl.setForeground(Tema.TEXTO_SUAVE);
        lbl.setBorder(new EmptyBorder(8, 0, 18, 0));
        return lbl;
    }

    // -----------------------------------------------------------------------
    // Lógica de eventos
    // -----------------------------------------------------------------------
    private void onIniciarSesion() {
        JTextField campoUsuario = Dialogos.campoTexto();
        JPasswordField campoContrasena = Dialogos.campoContrasena();

        while (true) {
            if (!Dialogos.formulario(this, "Iniciar sesión", new String[]{"Usuario", "Contraseña"},
                    new JComponent[]{campoUsuario, campoContrasena}, "Entrar")) return;

            String usuario = campoUsuario.getText().trim();
            char[] contrasena = campoContrasena.getPassword();
            try {
                if (usuario.isEmpty() || contrasena.length == 0) {
                    mostrarError("Completá usuario y contraseña.");
                    continue;
                }

                // Cuentas creadas antes del sistema de contraseñas: se les pide crear una
                if (cuentas.esCuentaSinContrasena(usuario)) {
                    PerfilJugador perfil = asignarContrasenaCuentaAntigua(usuario);
                    if (perfil != null) {
                        perfilSeleccionado = perfil;
                        actualizarTarjetaPerfil();
                    }
                    return;
                }

                PerfilJugador perfil = cuentas.iniciarSesion(usuario, contrasena);
                if (perfil == null) {
                    mostrarError("Usuario o contraseña incorrectos.");
                    campoContrasena.setText("");
                    continue;
                }
                perfilSeleccionado = perfil;
                actualizarTarjetaPerfil();
                return;
            } finally {
                Arrays.fill(contrasena, '\0');
            }
        }
    }

    private PerfilJugador asignarContrasenaCuentaAntigua(String usuario) {
        Dialogos.mensaje(this, "Proteger cuenta",
                "La cuenta \"" + usuario + "\" se creó antes de que existieran las contraseñas.\n"
                        + "Elegí una contraseña para protegerla.", Dialogos.Tipo.INFO);

        JPasswordField campoNueva = Dialogos.campoContrasena();
        JPasswordField campoConfirmar = Dialogos.campoContrasena();

        while (true) {
            if (!Dialogos.formulario(this, "Proteger cuenta", new String[]{"Nueva contraseña", "Repetir contraseña"},
                    new JComponent[]{campoNueva, campoConfirmar}, "Guardar")) return null;

            char[] nueva = campoNueva.getPassword();
            char[] confirmar = campoConfirmar.getPassword();
            try {
                String error = validarContrasena(nueva, confirmar);
                if (error != null) {
                    mostrarError(error);
                    continue;
                }
                return cuentas.asignarContrasenaACuentaAntigua(usuario, nueva);
            } finally {
                Arrays.fill(nueva, '\0');
                Arrays.fill(confirmar, '\0');
            }
        }
    }

    private void onRegistrarse() {
        JTextField campoUsuario = Dialogos.campoTexto();
        JPasswordField campoContrasena = Dialogos.campoContrasena();
        JPasswordField campoConfirmar = Dialogos.campoContrasena();

        while (true) {
            if (!Dialogos.formulario(this, "Registrarse", new String[]{"Usuario", "Contraseña", "Repetir contraseña"},
                    new JComponent[]{campoUsuario, campoContrasena, campoConfirmar}, "Crear cuenta")) return;

            String usuario = campoUsuario.getText().trim();
            char[] contrasena = campoContrasena.getPassword();
            char[] confirmar = campoConfirmar.getPassword();
            try {
                if (usuario.isEmpty()) {
                    mostrarError("Ingresá un nombre de usuario.");
                    continue;
                }
                if (usuario.length() > LARGO_MAX_USUARIO) {
                    mostrarError("El usuario puede tener como máximo " + LARGO_MAX_USUARIO + " caracteres.");
                    continue;
                }
                String error = validarContrasena(contrasena, confirmar);
                if (error != null) {
                    mostrarError(error);
                    continue;
                }

                PerfilJugador nuevo = cuentas.registrar(usuario, contrasena);
                if (nuevo == null) {
                    mostrarError("El usuario \"" + usuario + "\" ya existe. Elegí otro nombre.");
                    continue;
                }
                perfilSeleccionado = nuevo;
                actualizarTarjetaPerfil();
                Dialogos.mensaje(this, "Registro exitoso",
                        "Cuenta creada. ¡Bienvenido/a, " + usuario + "!", Dialogos.Tipo.INFO);
                return;
            } finally {
                Arrays.fill(contrasena, '\0');
                Arrays.fill(confirmar, '\0');
            }
        }
    }

    /** Elegir, cambiar o quitar la foto de perfil. */
    private void onCambiarFoto() {
        if (perfilSeleccionado.getFoto() != null) {
            int op = Dialogos.opciones(this, "Foto de perfil", "¿Qué querés hacer con tu foto de perfil?",
                    Dialogos.Tipo.PREGUNTA, "Cancelar", "Quitar foto", "Elegir otra");
            if (op == 1) {
                cuentas.cambiarFoto(perfilSeleccionado, null);
                actualizarTarjetaPerfil();
            }
            if (op != 2) return;
        }

        // Selector de archivos nativo del sistema (el explorador de Windows)
        FileDialog selector = new FileDialog(this, "Elegir foto de perfil", FileDialog.LOAD);
        selector.setFile("*.png;*.jpg;*.jpeg;*.gif;*.bmp");
        selector.setFilenameFilter((dir, nombre) -> nombre.toLowerCase().matches(".*\\.(png|jpe?g|gif|bmp)"));
        selector.setVisible(true);
        if (selector.getFile() == null) return;

        try {
            byte[] foto = AvatarJugador.prepararFoto(new File(selector.getDirectory(), selector.getFile()));
            cuentas.cambiarFoto(perfilSeleccionado, foto);
            actualizarTarjetaPerfil();
        } catch (IOException | IllegalArgumentException e) {
            mostrarError("No se pudo usar esa imagen.\n" + e.getMessage());
        }
    }

    private void onCerrarSesion() {
        perfilSeleccionado = null;
        actualizarTarjetaPerfil();
    }

    /** Devuelve un mensaje de error si la contraseña no es válida, o null si está bien. */
    private String validarContrasena(char[] contrasena, char[] confirmacion) {
        if (contrasena.length < LARGO_MIN_CONTRASENA) {
            return "La contraseña debe tener al menos " + LARGO_MIN_CONTRASENA + " caracteres.";
        }
        if (!Arrays.equals(contrasena, confirmacion)) {
            return "Las contraseñas no coinciden.";
        }
        return null;
    }

    private void mostrarError(String mensaje) {
        Dialogos.mensaje(this, "Error", mensaje, Dialogos.Tipo.ERROR);
    }

    private void actualizarTarjetaPerfil() {
        boolean logueado = perfilSeleccionado != null;
        btnIniciarSesion.setVisible(!logueado);
        btnRegistrarse.setVisible(!logueado);
        btnCambiarFoto.setVisible(logueado);
        btnCerrarSesion.setVisible(logueado);
        avatar.setCursor(logueado ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        avatar.setToolTipText(logueado ? "Cambiar foto de perfil" : null);

        if (!logueado) {
            lblPerfilActual.setText("Invitado");
            lblNivelActual.setText("Iniciá sesión para guardar tu progreso");
            avatar.setDatos(null, null);
            barraExp.setValor(0);
            barraExp.setTexto("0 / 0 XP");
            return;
        }
        lblPerfilActual.setText(perfilSeleccionado.getNombre());
        lblNivelActual.setText(perfilSeleccionado.getPuntosAcumulados() + " pts  •  "
                + perfilSeleccionado.getPartidasGanadas() + " victorias");
        avatar.setDatos(perfilSeleccionado.getFoto(), perfilSeleccionado.getNivel());
        barraExp.setValor((int) (perfilSeleccionado.getProgresoBarra() * 100));
        barraExp.setTexto(perfilSeleccionado.getPuntosEnNivelActual()
                + " / " + perfilSeleccionado.getPuntosParaSiguienteNivel() + " XP");
    }

    private void iniciarPartida(Color colorJugador) {
        if (perfilSeleccionado == null) {
            if (!Dialogos.confirmar(this, "Sin sesión",
                    "No iniciaste sesión.\n¿Querés jugar como invitado (sin guardar progreso)?",
                    Dialogos.Tipo.ADVERTENCIA, "Jugar", "Cancelar")) return;
        }

        if (!contraIA) {
            abrirPartida(colorJugador, null, null);
            return;
        }

        // El motor de IA tarda un momento en arrancar (lanza Python): se inicia en segundo plano
        Dificultad dificultad = dificultadElegida;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        getContentPane().setEnabled(false);
        new SwingWorker<MotorAjedrez, Void>() {
            @Override
            protected MotorAjedrez doInBackground() {
                return new MotorPython();
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                getContentPane().setEnabled(true);
                try {
                    MotorAjedrez motor = get();
                    if (!motor.estaDisponible()) {
                        String motivo = motor.motivoNoDisponible();
                        motor.cerrar();
                        Dialogos.mensaje(PantallaMenu.this, "IA no disponible",
                                "No se pudo iniciar la IA.\n" + motivo, Dialogos.Tipo.ERROR);
                        return;
                    }
                    abrirPartida(colorJugador, motor, dificultad);
                } catch (Exception e) {
                    Dialogos.mensaje(PantallaMenu.this, "IA no disponible",
                            "No se pudo iniciar la IA.\n" + e.getMessage(), Dialogos.Tipo.ERROR);
                }
            }
        }.execute();
    }

    private void abrirPartida(Color colorJugador, MotorAjedrez motor, Dificultad dificultad) {
        SesionDePartida sesion = new SesionDePartida(new PartidaAjedrez(), colorJugador,
                perfilSeleccionado, cuentas, motor, dificultad);
        VentanaAjedrez ventana = new VentanaAjedrez(sesion, motor, this);
        ventana.setVisible(true);
        this.setVisible(false);
    }

    // -----------------------------------------------------------------------
    // Actualizar la tarjeta de perfil desde la ventana de juego al terminar
    // -----------------------------------------------------------------------
    public void onPartidaTerminada(PerfilJugador perfilActualizado) {
        perfilSeleccionado = perfilActualizado;
        actualizarTarjetaPerfil();
        this.setVisible(true);
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
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        Tema.suavizar(g2);

        // Fondo circular azul noche
        g2.setColor(Tema.FONDO_OSCURO);
        g2.fillOval(2, 2, size - 4, size - 4);

        // Borde dorado
        g2.setColor(Tema.DORADO);
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawOval(2, 2, size - 4, size - 4);

        // Símbolo del rey
        g2.setFont(new Font(FUENTE_AJEDREZ, Font.PLAIN, 42));
        FontMetrics fm = g2.getFontMetrics();
        String sym = "♔";
        int x = (size - fm.stringWidth(sym)) / 2;
        int y = (size - fm.getHeight()) / 2 + fm.getAscent() - 2;
        g2.drawString(sym, x, y);

        g2.dispose();
        return img;
    }
}

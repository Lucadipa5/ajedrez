package ajedrez.adaptadores.ui.swing;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Jugada;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.dominio.TipoPieza;
import ajedrez.nucleo.estados.EstadoJaqueMate;
import ajedrez.nucleo.ia.MotorAjedrez;
import ajedrez.nucleo.perfil.PerfilJugador;
import ajedrez.nucleo.servicio.PartidaAjedrez;
import ajedrez.nucleo.servicio.SelectorPromocion;
import ajedrez.nucleo.servicio.SesionDePartida;
import ajedrez.nucleo.servicio.SesionDePartida.EventoXP;
import ajedrez.nucleo.servicio.SesionDePartida.ResultadoJugada;
import ajedrez.nucleo.tablero.Tablero;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ventana del juego de Ajedrez (adaptador de entrada Swing).
 *
 * Solo se ocupa de mostrar el tablero y traducir clicks en jugadas: las reglas del juego,
 * la XP y el turno de la IA los resuelve el núcleo a través de {@link SesionDePartida}.
 *
 * - Tablero con coordenadas que se mantiene cuadrado al cambiar el tamaño
 * - Pantalla completa (botón, F11; Esc para salir)
 * - Marcas animadas de movimientos posibles y de la última jugada
 * - Jaque resaltado: el rey brilla en rojo, el tablero se sacude y aparece un cartel
 * - Animaciones de XP
 * - Modo contra la IA: la IA piensa en segundo plano sin congelar la ventana
 */
public class VentanaAjedrez extends JFrame implements SelectorPromocion {

    // -----------------------------------------------------------------------
    // Paleta visual
    // -----------------------------------------------------------------------
    private static final java.awt.Color COLOR_CASILLA_CLARA = new java.awt.Color(240, 217, 181);
    private static final java.awt.Color COLOR_CASILLA_OSCURA = new java.awt.Color(181, 136, 99);
    private static final java.awt.Color COLOR_SELECCIONADO = new java.awt.Color(246, 236, 90);
    private static final java.awt.Color COLOR_ULTIMA_JUGADA = new java.awt.Color(205, 210, 106);
    private static final java.awt.Color BG_PANEL = new java.awt.Color(35, 32, 28);
    private static final java.awt.Color COLOR_COORD = new java.awt.Color(200, 180, 140);
    private static final java.awt.Color COLOR_DORADO = new java.awt.Color(212, 175, 55);
    private static final java.awt.Color COLOR_JAQUE = new java.awt.Color(230, 50, 40);
    private static final java.awt.Color COLOR_TEXTO = new java.awt.Color(235, 230, 220);

    // -----------------------------------------------------------------------
    // Estado
    // -----------------------------------------------------------------------
    private final SesionDePartida sesion;
    private final PartidaAjedrez partida;
    private final MotorAjedrez motorIA;          // null en modo 2 jugadores
    private final PantallaMenu menuOrigen;
    private final boolean tableroInvertido;      // las negras abajo si el jugador eligió negras

    private final CasillaBoton[][] botonesCasillas = new CasillaBoton[8][8]; // indexado por posición lógica
    private final PanelTableroCuadrado panelTableroConCoordenadas;
    private JPanel panelTablero;
    private Posicion casillaSeleccionada = null;
    private List<Posicion> destinosLegalesActuales = List.of();
    private Jugada ultimaJugada = null;

    // Turno de la IA: cada partida nueva cambia la generación para ignorar respuestas viejas
    private boolean iaPensando = false;
    private int generacionPartida = 0;

    // Pantalla completa
    private boolean pantallaCompleta = false;
    private Rectangle limitesVentana;
    private JButton btnPantallaCompleta;

    // Animaciones
    private final CapaAnimacionXP capaXP = new CapaAnimacionXP();
    private Timer timerBarra;
    private Timer timerMarcas;
    private long inicioAnimacionMarcas;
    private static final int DURACION_MARCAS_MS = 180;
    private final Timer timerPulsoJaque;
    private final long inicioPulso = System.currentTimeMillis();

    // Cache de imagenes personalizadas para piezas
    private static final Map<TipoPieza, BufferedImage> imagenesNegras = new HashMap<>();
    private static final Map<TipoPieza, BufferedImage> imagenesBlancas = new HashMap<>();

    static {
        cargarImagenPieza(imagenesNegras, TipoPieza.REY, "rey_negro.png");
        cargarImagenPieza(imagenesNegras, TipoPieza.REINA, "reina_negro.png");
        cargarImagenPieza(imagenesNegras, TipoPieza.TORRE, "torre_negro.png");
        cargarImagenPieza(imagenesNegras, TipoPieza.ALFIL, "alfil_negro.png");
        cargarImagenPieza(imagenesNegras, TipoPieza.CABALLO, "caballo_negro.png");
        cargarImagenPieza(imagenesNegras, TipoPieza.PEON, "peon_negro.png");

        cargarImagenPieza(imagenesBlancas, TipoPieza.REY, "rey_blanco.png");
        cargarImagenPieza(imagenesBlancas, TipoPieza.REINA, "reina_blanco.png");
        cargarImagenPieza(imagenesBlancas, TipoPieza.TORRE, "torre_blanco.png");
        cargarImagenPieza(imagenesBlancas, TipoPieza.ALFIL, "alfil_blanco.png");
        cargarImagenPieza(imagenesBlancas, TipoPieza.CABALLO, "caballo_blanco.png");
        cargarImagenPieza(imagenesBlancas, TipoPieza.PEON, "peon_blanco.png");
    }

    private static void cargarImagenPieza(Map<TipoPieza, BufferedImage> cache, TipoPieza tipo, String nombreArchivo) {
        try {
            InputStream is = VentanaAjedrez.class.getResourceAsStream("recursos/" + nombreArchivo);
            if (is != null) {
                cache.put(tipo, ImageIO.read(is));
                is.close();
            }
        } catch (IOException e) {
            System.err.println("No se pudo cargar imagen: " + nombreArchivo);
        }
    }

    // -----------------------------------------------------------------------
    // Componentes UI
    // -----------------------------------------------------------------------
    private final JLabel labelEstado = new JLabel("", SwingConstants.CENTER);
    private final CartelJaque cartelJaque = new CartelJaque();
    private final JButton btnDeshacer = new JButton();
    private final JButton btnRehacer = new JButton();

    // Panel de perfil (barra de nivel)
    private JLabel lblNivel;
    private JProgressBar barraExp;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    public VentanaAjedrez(SesionDePartida sesion, MotorAjedrez motorIA, PantallaMenu menuOrigen) {
        this.sesion = sesion;
        this.partida = sesion.getPartida();
        this.motorIA = motorIA;
        this.menuOrigen = menuOrigen;
        this.tableroInvertido = sesion.getColorJugador() == Color.NEGRO;
        this.partida.setSelectorPromocion(this);

        String modo = sesion.esContraIA() ? "contra la IA (" + sesion.getDificultad() + ")" : "2 jugadores";
        setTitle("Ajedrez — " + modo + " — Jugando con "
                + (sesion.getColorJugador() == Color.BLANCO ? "Blancas" : "Negras"));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                onSalir();
            }
        });

        setSize(780, 880);
        setMinimumSize(new Dimension(560, 640));
        setLocationRelativeTo(null);
        setIconImage(PantallaMenu.crearIconoAjedrez());
        setGlassPane(capaXP);

        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_PANEL);

        panelTableroConCoordenadas = new PanelTableroCuadrado();
        add(construirPanelSuperior(), BorderLayout.NORTH);
        add(panelTableroConCoordenadas, BorderLayout.CENTER);
        add(construirPanelInferior(), BorderLayout.SOUTH);
        configurarAtajosDeTeclado();

        // El brillo del rey en jaque late mientras dure el jaque
        timerPulsoJaque = new Timer(40, e -> {
            float fase = (float) ((System.currentTimeMillis() - inicioPulso) / 1000.0 * Math.PI * 2 * 0.9);
            float pulso = 0.5f + 0.5f * (float) Math.sin(fase);
            for (CasillaBoton[] fila : botonesCasillas)
                for (CasillaBoton b : fila) b.setPulso(pulso);
            cartelJaque.setPulso(pulso);
        });

        actualizarVista();
        SwingUtilities.invokeLater(this::jugarTurnoIASiCorresponde);
    }

    // -----------------------------------------------------------------------
    // Panel superior: estado del turno, jaque y perfil
    // -----------------------------------------------------------------------
    private JPanel construirPanelSuperior() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PANEL);
        panel.setBorder(new EmptyBorder(8, 12, 4, 12));

        JPanel panelJuego = new JPanel();
        panelJuego.setLayout(new BoxLayout(panelJuego, BoxLayout.Y_AXIS));
        panelJuego.setBackground(BG_PANEL);

        labelEstado.setFont(new Font("Segoe UI", Font.BOLD, 17));
        labelEstado.setForeground(COLOR_TEXTO);
        labelEstado.setAlignmentX(Component.CENTER_ALIGNMENT);
        cartelJaque.setAlignmentX(Component.CENTER_ALIGNMENT);

        panelJuego.add(Box.createVerticalStrut(4));
        panelJuego.add(labelEstado);
        panelJuego.add(Box.createVerticalStrut(6));
        panelJuego.add(cartelJaque);

        panel.add(panelJuego, BorderLayout.CENTER);
        panel.add(construirPanelPerfil(), BorderLayout.EAST);
        return panel;
    }

    private JPanel construirPanelPerfil() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new java.awt.Color(45, 42, 38));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_DORADO, 1),
                new EmptyBorder(6, 12, 6, 12)
        ));

        PerfilJugador perfil = sesion.getPerfil();
        if (perfil != null) {
            JLabel lblNombrePerfil = new JLabel(perfil.getNombre());
            lblNombrePerfil.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblNombrePerfil.setForeground(COLOR_DORADO);
            lblNombrePerfil.setAlignmentX(Component.RIGHT_ALIGNMENT);

            lblNivel = new JLabel();
            lblNivel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblNivel.setForeground(new java.awt.Color(180, 175, 165));
            lblNivel.setAlignmentX(Component.RIGHT_ALIGNMENT);

            barraExp = new JProgressBar(0, 100);
            barraExp.setStringPainted(true);
            barraExp.setFont(new Font("Segoe UI", Font.BOLD, 10));
            barraExp.setForeground(COLOR_DORADO);
            barraExp.setBackground(new java.awt.Color(20, 18, 15));
            barraExp.setPreferredSize(new Dimension(180, 16));
            barraExp.setMaximumSize(new Dimension(220, 16));
            barraExp.setBorder(BorderFactory.createLineBorder(new java.awt.Color(80, 75, 70)));

            panel.add(lblNombrePerfil);
            panel.add(Box.createVerticalStrut(2));
            panel.add(lblNivel);
            panel.add(Box.createVerticalStrut(4));
            panel.add(barraExp);
            actualizarBarraPerfil();
        } else {
            JLabel sinPerfil = new JLabel("Invitado");
            sinPerfil.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            sinPerfil.setForeground(new java.awt.Color(130, 125, 115));
            panel.add(sinPerfil);
        }
        return panel;
    }

    // -----------------------------------------------------------------------
    // Tablero con coordenadas que se mantiene cuadrado y centrado
    // -----------------------------------------------------------------------
    private class PanelTableroCuadrado extends JPanel {
        private final JPanel letrasArriba = construirFilaLetras();
        private final JPanel letrasAbajo = construirFilaLetras();
        private final JPanel numerosIzq = construirColumnaNumeros();
        private final JPanel numerosDer = construirColumnaNumeros();
        private int desplazamientoX = 0; // para la sacudida del jaque

        PanelTableroCuadrado() {
            setLayout(null);
            setBackground(BG_PANEL);
            panelTablero = construirPanelTablero();
            add(letrasArriba);
            add(letrasAbajo);
            add(numerosIzq);
            add(numerosDer);
            add(panelTablero);
        }

        @Override
        public void doLayout() {
            int margen = Math.max(18, Math.min(getWidth(), getHeight()) / 30);
            int disponible = Math.min(getWidth() - 2 * margen - 16, getHeight() - 2 * margen - 8);
            int borde = 3;
            int lado = Math.max(8, (disponible - 2 * borde) / 8 * 8) + 2 * borde;
            int x = (getWidth() - lado) / 2 + desplazamientoX;
            int y = (getHeight() - lado) / 2;

            panelTablero.setBounds(x, y, lado, lado);
            letrasArriba.setBounds(x + borde, y - margen, lado - 2 * borde, margen);
            letrasAbajo.setBounds(x + borde, y + lado, lado - 2 * borde, margen);
            numerosIzq.setBounds(x - margen, y + borde, margen, lado - 2 * borde);
            numerosDer.setBounds(x + lado, y + borde, margen, lado - 2 * borde);

            Font fuente = new Font("Segoe UI", Font.BOLD, Math.max(12, lado / 52));
            for (JPanel p : new JPanel[]{letrasArriba, letrasAbajo, numerosIzq, numerosDer})
                for (Component c : p.getComponents()) c.setFont(fuente);
        }

        /** Sacude el tablero de lado a lado (se usa al dar jaque). */
        void sacudir() {
            long inicio = System.currentTimeMillis();
            Timer t = new Timer(16, null);
            t.addActionListener(e -> {
                float p = (System.currentTimeMillis() - inicio) / 380f;
                if (p >= 1f) {
                    desplazamientoX = 0;
                    t.stop();
                } else {
                    desplazamientoX = (int) Math.round(Math.sin(p * Math.PI * 7) * 9 * (1 - p));
                }
                doLayout();
                repaint();
            });
            t.start();
        }
    }

    private JPanel construirFilaLetras() {
        JPanel fila = new JPanel(new GridLayout(1, 8));
        fila.setBackground(BG_PANEL);
        for (int i = 0; i < 8; i++) {
            int col = tableroInvertido ? 7 - i : i;
            JLabel lbl = new JLabel(String.valueOf((char) ('a' + col)), SwingConstants.CENTER);
            lbl.setForeground(COLOR_COORD);
            fila.add(lbl);
        }
        return fila;
    }

    private JPanel construirColumnaNumeros() {
        JPanel col = new JPanel(new GridLayout(8, 1));
        col.setBackground(BG_PANEL);
        for (int i = 0; i < 8; i++) {
            int fila = tableroInvertido ? 7 - i : i;  // fila 0 del array → número 8
            JLabel lbl = new JLabel(String.valueOf(8 - fila), SwingConstants.CENTER);
            lbl.setForeground(COLOR_COORD);
            col.add(lbl);
        }
        return col;
    }

    private JPanel construirPanelTablero() {
        JPanel tablero = new JPanel(new GridLayout(8, 8));
        tablero.setBorder(BorderFactory.createLineBorder(new java.awt.Color(25, 22, 18), 3));

        // Las casillas se agregan en orden visual; si el tablero está invertido se recorren al revés
        for (int filaVisual = 0; filaVisual < 8; filaVisual++) {
            for (int colVisual = 0; colVisual < 8; colVisual++) {
                int fila = tableroInvertido ? 7 - filaVisual : filaVisual;
                int col = tableroInvertido ? 7 - colVisual : colVisual;
                CasillaBoton boton = new CasillaBoton();
                Posicion pos = new Posicion(fila, col);
                boton.addActionListener(e -> onCasillaClick(pos));
                botonesCasillas[fila][col] = boton;
                tablero.add(boton);
            }
        }
        return tablero;
    }

    // -----------------------------------------------------------------------
    // Panel inferior: controles
    // -----------------------------------------------------------------------
    private JPanel construirPanelInferior() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        panel.setBackground(BG_PANEL);
        panel.setBorder(new EmptyBorder(0, 0, 6, 0));

        estilizarBotonControl(btnDeshacer, "Deshacer");
        btnDeshacer.addActionListener(e -> {
            if (iaPensando) return;
            if (sesion.deshacer()) {
                limpiarSeleccion();
                ultimaJugada = null;
                actualizarBarraPerfil();
                actualizarVista();
            }
        });

        estilizarBotonControl(btnRehacer, "Rehacer");
        btnRehacer.addActionListener(e -> {
            if (iaPensando) return;
            if (sesion.rehacer()) {
                limpiarSeleccion();
                ultimaJugada = null;
                actualizarBarraPerfil();
                actualizarVista();
            }
        });

        JButton btnNuevaPartida = new JButton();
        estilizarBotonControl(btnNuevaPartida, "Nueva Partida");
        btnNuevaPartida.addActionListener(e -> reiniciarPartida());

        btnPantallaCompleta = new JButton();
        estilizarBotonControl(btnPantallaCompleta, "Pantalla completa");
        btnPantallaCompleta.addActionListener(e -> alternarPantallaCompleta());

        JButton btnMenu = new JButton();
        estilizarBotonControl(btnMenu, "Menú");
        btnMenu.addActionListener(e -> onSalir());

        panel.add(btnDeshacer);
        panel.add(btnRehacer);
        panel.add(btnNuevaPartida);
        panel.add(btnPantallaCompleta);
        panel.add(btnMenu);
        return panel;
    }

    private void estilizarBotonControl(JButton btn, String texto) {
        btn.setText(texto);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(new java.awt.Color(230, 225, 215));
        btn.setBackground(new java.awt.Color(60, 58, 54));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(7, 14, 7, 14));
    }

    // -----------------------------------------------------------------------
    // Pantalla completa
    // -----------------------------------------------------------------------
    private void configurarAtajosDeTeclado() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "pantallaCompleta");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "salirPantallaCompleta");
        am.put("pantallaCompleta", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { alternarPantallaCompleta(); }
        });
        am.put("salirPantallaCompleta", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (pantallaCompleta) alternarPantallaCompleta();
            }
        });
    }

    private void alternarPantallaCompleta() {
        boolean activar = !pantallaCompleta;
        // Para quitar/poner la barra de título hay que ocultar la ventana un instante
        dispose();
        if (activar) {
            limitesVentana = getBounds();
            setUndecorated(true);
            setBounds(getGraphicsConfiguration().getBounds());
        } else {
            setUndecorated(false);
            setBounds(limitesVentana);
        }
        pantallaCompleta = activar;
        btnPantallaCompleta.setText(activar ? "Salir de pantalla completa" : "Pantalla completa");
        setVisible(true);
    }

    // -----------------------------------------------------------------------
    // SelectorPromocion
    // -----------------------------------------------------------------------
    @Override
    public TipoPieza solicitarPromocion(Color color) {
        String[] opciones = {"Reina", "Torre", "Alfil", "Caballo"};
        int sel = JOptionPane.showOptionDialog(this,
                "¡Tu peón llegó a la última fila!\nElegí la pieza para coronar:",
                "Coronación de Peón (" + color + ")",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, opciones, opciones[0]);
        return switch (sel) {
            case 1 -> TipoPieza.TORRE;
            case 2 -> TipoPieza.ALFIL;
            case 3 -> TipoPieza.CABALLO;
            default -> TipoPieza.REINA;
        };
    }

    // -----------------------------------------------------------------------
    // Clicks en casillas
    // -----------------------------------------------------------------------
    private void onCasillaClick(Posicion clickeada) {
        if (partida.getEstadoActual().esFinalizada() || iaPensando || sesion.esTurnoDeLaIA()) return;

        if (casillaSeleccionada != null) {
            if (casillaSeleccionada.equals(clickeada)) {
                limpiarSeleccion();
                actualizarVista();
                return;
            }
            ResultadoJugada resultado = sesion.mover(new Jugada(casillaSeleccionada, clickeada, null));
            if (resultado != null) {
                limpiarSeleccion();
                aplicarResultado(resultado);
                jugarTurnoIASiCorresponde();
                return;
            }
        }

        // Seleccionar pieza
        Pieza pieza = partida.getTablero().obtenerPieza(clickeada);
        if (pieza != null && pieza.getColor() == partida.getEstadoActual().getColorTurnoActual()) {
            casillaSeleccionada = clickeada;
            destinosLegalesActuales = partida.obtenerMovimientosLegalesPara(clickeada);
            iniciarAnimacionMarcas();
        } else {
            limpiarSeleccion();
        }
        actualizarVista();
    }

    private void limpiarSeleccion() {
        casillaSeleccionada = null;
        destinosLegalesActuales = List.of();
    }

    /** Muestra en pantalla lo que pasó con una jugada (de una persona o de la IA). */
    private void aplicarResultado(ResultadoJugada resultado) {
        ultimaJugada = resultado.jugada();

        int retraso = 0;
        for (EventoXP evento : resultado.eventosXP()) {
            Point centro = evento.motivo() == SesionDePartida.MotivoXP.VICTORIA
                    ? centroDelTablero() : centroDeCasilla(resultado.jugada().getDestino());
            animarGananciaXP(evento, centro, retraso);
            retraso += 350;
        }

        actualizarVista();

        if (partida.estaEnJaqueTurnoActual()) {
            anunciarJaque(false);
        } else if (partida.getEstadoActual() instanceof EstadoJaqueMate) {
            anunciarJaque(true);
        }

        if (resultado.partidaTerminada()) {
            onPartidaFinalizada(resultado.ganoJugador());
        }
    }

    // -----------------------------------------------------------------------
    // Turno de la IA
    // -----------------------------------------------------------------------
    private void jugarTurnoIASiCorresponde() {
        if (!sesion.esTurnoDeLaIA() || iaPensando) return;
        iaPensando = true;
        int generacion = generacionPartida;
        actualizarVista();

        new SwingWorker<Jugada, Void>() {
            @Override
            protected Jugada doInBackground() throws Exception {
                long inicio = System.currentTimeMillis();
                Jugada jugada = sesion.calcularJugadaIA();
                // Pausa mínima para que la respuesta no sea instantánea y se pueda seguir la partida
                long espera = 450 - (System.currentTimeMillis() - inicio);
                if (espera > 0) Thread.sleep(espera);
                return jugada;
            }

            @Override
            protected void done() {
                if (generacion != generacionPartida || !isDisplayable()) return; // partida reiniciada o ventana cerrada
                iaPensando = false;
                try {
                    ResultadoJugada resultado = sesion.mover(get());
                    if (resultado != null) {
                        aplicarResultado(resultado);
                    } else {
                        actualizarVista();
                    }
                } catch (Exception e) {
                    actualizarVista();
                    Throwable causa = e.getCause() != null ? e.getCause() : e;
                    JOptionPane.showMessageDialog(VentanaAjedrez.this,
                            "La IA no pudo jugar:\n" + causa.getMessage(),
                            "Error de la IA", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // -----------------------------------------------------------------------
    // Render del tablero
    // -----------------------------------------------------------------------
    private void actualizarVista() {
        Tablero tablero = partida.getTablero();
        Posicion reyEnJaque = posicionReyAmenazado();
        boolean esMate = partida.getEstadoActual() instanceof EstadoJaqueMate;

        for (int fila = 0; fila < 8; fila++) {
            for (int col = 0; col < 8; col++) {
                Posicion pos = new Posicion(fila, col);
                CasillaBoton boton = botonesCasillas[fila][col];
                Pieza pieza = tablero.obtenerPieza(pos);

                if (pieza != null) {
                    BufferedImage imgPieza = (pieza.getColor() == Color.NEGRO)
                            ? imagenesNegras.get(pieza.getTipo())
                            : imagenesBlancas.get(pieza.getTipo());
                    if (imgPieza != null) {
                        boton.setPiezaImagen(imgPieza);
                    } else {
                        java.awt.Color colorTexto = (pieza.getColor() == Color.BLANCO)
                                ? new java.awt.Color(255, 255, 255)
                                : new java.awt.Color(20, 20, 20);
                        boton.setPiezaVisual(pieza.getSimboloUnicode(), colorTexto);
                    }
                } else {
                    boton.setPiezaVisual("", java.awt.Color.BLACK);
                }

                boolean esCasillaClara = (fila + col) % 2 == 0;
                java.awt.Color colorBase = esCasillaClara ? COLOR_CASILLA_CLARA : COLOR_CASILLA_OSCURA;
                boolean esSeleccionada = casillaSeleccionada != null && casillaSeleccionada.equals(pos);
                boolean esUltimaJugada = ultimaJugada != null
                        && (ultimaJugada.getOrigen().equals(pos) || ultimaJugada.getDestino().equals(pos));
                boolean esDestino = destinosLegalesActuales.contains(pos);

                if (esSeleccionada) {
                    boton.setBackground(mezclar(colorBase, COLOR_SELECCIONADO, 0.55f));
                } else if (esUltimaJugada) {
                    boton.setBackground(mezclar(colorBase, COLOR_ULTIMA_JUGADA, 0.5f));
                } else {
                    boton.setBackground(colorBase);
                }
                boton.setHighlight(esDestino, esDestino && pieza != null, esSeleccionada);
                boton.setJaque(pos.equals(reyEnJaque), esMate);
            }
        }

        if (reyEnJaque != null && !esMate) {
            if (!timerPulsoJaque.isRunning()) timerPulsoJaque.start();
        } else {
            timerPulsoJaque.stop();
        }

        // Textos de estado
        labelEstado.setText(textoEstado());
        if (esMate) {
            cartelJaque.mostrar("¡JAQUE MATE!", false);
        } else if (reyEnJaque != null) {
            Color enJaque = partida.getEstadoActual().getColorTurnoActual();
            cartelJaque.mostrar("¡JAQUE AL REY " + (enJaque == Color.BLANCO ? "BLANCO" : "NEGRO") + "!", true);
        } else {
            cartelJaque.ocultar();
        }

        btnDeshacer.setEnabled(!iaPensando && sesion.puedeDeshacer());
        btnRehacer.setEnabled(!iaPensando && sesion.puedeRehacer());
    }

    private String textoEstado() {
        if (partida.getEstadoActual().esFinalizada() || !sesion.esContraIA()) {
            return partida.getEstadoActual().getDescripcion();
        }
        if (sesion.esTurnoDeLaIA()) {
            return "La IA está pensando…";
        }
        return "Tu turno (" + (sesion.getColorJugador() == Color.BLANCO ? "Blancas" : "Negras") + ")";
    }

    /** Rey del bando en turno si está en jaque, o el rey que recibió mate. */
    private Posicion posicionReyAmenazado() {
        if (partida.getEstadoActual() instanceof EstadoJaqueMate mate) {
            return partida.getTablero().buscarRey(mate.getGanador().opuesto());
        }
        if (partida.estaEnJaqueTurnoActual()) {
            return partida.getTablero().buscarRey(partida.getEstadoActual().getColorTurnoActual());
        }
        return null;
    }

    private void anunciarJaque(boolean esMate) {
        Posicion rey = posicionReyAmenazado();
        if (rey == null) return;
        panelTableroConCoordenadas.sacudir();
        capaXP.mostrar(esMate ? "¡JAQUE MATE!" : "¡JAQUE!", null,
                centroDeCasilla(rey), 0, esMate ? 34f : 28f, new java.awt.Color(255, 90, 75));
    }

    private static java.awt.Color mezclar(java.awt.Color a, java.awt.Color b, float t) {
        return new java.awt.Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /** Las marcas de movimientos posibles aparecen creciendo con un pequeño rebote. */
    private void iniciarAnimacionMarcas() {
        inicioAnimacionMarcas = System.currentTimeMillis();
        if (timerMarcas == null) {
            timerMarcas = new Timer(15, e -> {
                float p = Math.min(1f, (System.currentTimeMillis() - inicioAnimacionMarcas) / (float) DURACION_MARCAS_MS);
                // ease-out con rebote leve (pasa de 1 y vuelve)
                float escala = (float) (1 + 2.2 * Math.pow(p - 1, 3) + 1.2 * Math.pow(p - 1, 2));
                for (CasillaBoton[] fila : botonesCasillas)
                    for (CasillaBoton b : fila) b.setEscalaMarca(escala);
                if (p >= 1f) ((Timer) e.getSource()).stop();
            });
        }
        for (CasillaBoton[] fila : botonesCasillas)
            for (CasillaBoton b : fila) b.setEscalaMarca(0f);
        timerMarcas.restart();
    }

    // -----------------------------------------------------------------------
    // Perfil y animaciones de XP
    // -----------------------------------------------------------------------
    private void actualizarBarraPerfil() {
        if (sesion.getPerfil() == null || barraExp == null) return;
        if (timerBarra != null) timerBarra.stop();
        barraExp.setValue((int) (sesion.getPerfil().getProgresoBarra() * 100));
        actualizarTextosPerfil();
    }

    private void actualizarTextosPerfil() {
        PerfilJugador perfil = sesion.getPerfil();
        barraExp.setString(perfil.getPuntosEnNivelActual() + "/" + perfil.getPuntosParaSiguienteNivel() + " XP");
        lblNivel.setText("Nivel " + perfil.getNivel());
    }

    private void animarGananciaXP(EventoXP evento, Point centro, int retrasoMs) {
        String motivo = switch (evento.motivo()) {
            case CAPTURA -> "Captura";
            case JAQUE -> "Jaque";
            case VICTORIA -> "Victoria";
        };
        capaXP.mostrar("+" + evento.puntos() + " XP", motivo, centro, retrasoMs, 24f);
        if (evento.subioNivel()) {
            capaXP.mostrar("¡NIVEL " + evento.nivel() + "!", "Subiste de nivel",
                    centroDelTablero(), retrasoMs + 450, 44f);
        }
        if (barraExp != null) {
            actualizarTextosPerfil();
            animarBarra((int) (sesion.getPerfil().getProgresoBarra() * 100), evento.subioNivel());
        }
    }

    /** Llena la barra de XP de forma progresiva; si subió de nivel, la llena, la vacía y sigue. */
    private void animarBarra(int objetivo, boolean pasarPorLleno) {
        if (timerBarra != null) timerBarra.stop();
        final int[] destino = {pasarPorLleno ? 100 : objetivo};
        final boolean[] faltaSegundoTramo = {pasarPorLleno};
        timerBarra = new Timer(15, e -> {
            int actual = barraExp.getValue();
            if (actual == destino[0]) {
                if (faltaSegundoTramo[0]) {
                    faltaSegundoTramo[0] = false;
                    barraExp.setValue(0);
                    destino[0] = objetivo;
                } else {
                    ((Timer) e.getSource()).stop();
                }
                return;
            }
            int diferencia = destino[0] - actual;
            int paso = Math.max(1, Math.abs(diferencia) / 8);
            barraExp.setValue(actual + Integer.signum(diferencia) * paso);
        });
        timerBarra.start();
    }

    private Point centroDeCasilla(Posicion pos) {
        CasillaBoton b = botonesCasillas[pos.getFila()][pos.getColumna()];
        return SwingUtilities.convertPoint(b, b.getWidth() / 2, b.getHeight() / 2, capaXP);
    }

    private Point centroDelTablero() {
        return SwingUtilities.convertPoint(panelTablero,
                panelTablero.getWidth() / 2, panelTablero.getHeight() / 2, capaXP);
    }

    // -----------------------------------------------------------------------
    // Fin de partida / reinicio / salir
    // -----------------------------------------------------------------------
    private void onPartidaFinalizada(boolean gane) {
        String desc = partida.getEstadoActual().getDescripcion();
        String mensaje = gane
                ? "¡Ganaste la partida!\n+" + PerfilJugador.PUNTOS_VICTORIA + " XP"
                : "La partida terminó.\n" + desc;

        // Se espera a que se vean las animaciones antes de mostrar el cartel
        Timer espera = new Timer(1300, e ->
                JOptionPane.showMessageDialog(this, mensaje, "Fin de partida",
                        gane ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.PLAIN_MESSAGE));
        espera.setRepeats(false);
        espera.start();
    }

    private void reiniciarPartida() {
        generacionPartida++;
        iaPensando = false;
        sesion.reiniciar();
        limpiarSeleccion();
        ultimaJugada = null;
        actualizarBarraPerfil();
        actualizarVista();
        jugarTurnoIASiCorresponde();
    }

    private void onSalir() {
        int resp = JOptionPane.showConfirmDialog(this,
                "No se guardará el progreso de esta partida.\n¿Seguro que querés salir al menú principal?",
                "Salir de la partida", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (resp != JOptionPane.YES_OPTION) return;

        // No se guarda: se vuelve al último progreso guardado
        generacionPartida++;
        sesion.descartarProgresoNoGuardado();
        timerPulsoJaque.stop();
        if (motorIA != null) motorIA.cerrar();
        this.dispose();
        if (menuOrigen != null) {
            menuOrigen.onPartidaTerminada(sesion.getPerfil());
        }
    }

    // -----------------------------------------------------------------------
    // Cartel de jaque: píldora roja que late
    // -----------------------------------------------------------------------
    private static class CartelJaque extends JComponent {
        private String texto = "";
        private boolean late = false;
        private float pulso = 1f;

        CartelJaque() {
            setFont(new Font("Segoe UI", Font.BOLD, 15));
            setPreferredSize(new Dimension(320, 30));
            setMaximumSize(new Dimension(420, 30));
        }

        void mostrar(String texto, boolean late) {
            this.texto = texto;
            this.late = late;
            if (!late) pulso = 1f;
            repaint();
        }

        void ocultar() {
            texto = "";
            repaint();
        }

        void setPulso(float pulso) {
            if (!late) return;
            this.pulso = pulso;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (texto.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            FontMetrics fm = g2.getFontMetrics(getFont());
            int ancho = Math.min(getWidth() - 4, fm.stringWidth(texto) + 40);
            int alto = getHeight() - 4;
            int x = (getWidth() - ancho) / 2;

            // Halo que late
            int alfaHalo = Math.round(40 + 70 * pulso);
            g2.setColor(new java.awt.Color(COLOR_JAQUE.getRed(), COLOR_JAQUE.getGreen(), COLOR_JAQUE.getBlue(), alfaHalo));
            g2.fillRoundRect(x - 2, 0, ancho + 4, alto + 4, alto + 4, alto + 4);
            // Píldora
            g2.setColor(mezclar(new java.awt.Color(150, 25, 20), COLOR_JAQUE, pulso));
            g2.fillRoundRect(x, 2, ancho, alto, alto, alto);
            // Texto
            g2.setFont(getFont());
            g2.setColor(java.awt.Color.WHITE);
            g2.drawString(texto, (getWidth() - fm.stringWidth(texto)) / 2, 2 + (alto - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // -----------------------------------------------------------------------
    // Casilla del tablero
    // -----------------------------------------------------------------------
    private static class CasillaBoton extends JButton {
        private String textoPieza = "";
        private java.awt.Color colorPieza = java.awt.Color.BLACK;
        private BufferedImage imagenPieza = null;
        private boolean esDestino = false;
        private boolean esCaptura = false;
        private boolean esSeleccionada = false;
        private boolean hover = false;
        private float escalaMarca = 1f;
        private boolean enJaque = false;
        private boolean esMate = false;
        private float pulso = 1f;

        public CasillaBoton() {
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
            });
        }

        public void setEscalaMarca(float escala) {
            if (escalaMarca == escala) return;
            this.escalaMarca = escala;
            if (esDestino) repaint();
        }

        public void setPiezaVisual(String texto, java.awt.Color color) {
            this.textoPieza = texto != null ? texto : "";
            this.colorPieza = color;
            this.imagenPieza = null;
            repaint();
        }

        public void setPiezaImagen(BufferedImage imagen) {
            this.imagenPieza = imagen;
            this.textoPieza = "";
            repaint();
        }

        public void setHighlight(boolean esDestino, boolean esCaptura, boolean esSeleccionada) {
            this.esDestino = esDestino;
            this.esCaptura = esCaptura;
            this.esSeleccionada = esSeleccionada;
            repaint();
        }

        public void setJaque(boolean enJaque, boolean esMate) {
            this.enJaque = enJaque;
            this.esMate = esMate;
            repaint();
        }

        public void setPulso(float pulso) {
            this.pulso = pulso;
            if (enJaque) repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            int w = getWidth();
            int h = getHeight();
            int lado = Math.min(w, h);
            float cx = w / 2f;
            float cy = h / 2f;

            // --- Rey en jaque: brillo rojo radial (late; fijo e intenso si es mate) ---
            if (enJaque) {
                float intensidad = esMate ? 1f : 0.55f + 0.45f * pulso;
                int a = Math.round(235 * intensidad);
                RadialGradientPaint brillo = new RadialGradientPaint(
                        new java.awt.geom.Point2D.Float(cx, cy), lado * 0.72f,
                        new float[]{0f, 0.45f, 1f},
                        new java.awt.Color[]{
                                new java.awt.Color(255, 40, 30, a),
                                new java.awt.Color(230, 30, 20, Math.round(a * 0.65f)),
                                new java.awt.Color(200, 0, 0, 0)});
                g2.setPaint(brillo);
                g2.fillRect(0, 0, w, h);
                g2.setColor(new java.awt.Color(200, 20, 15, Math.round(200 * intensidad)));
                g2.setStroke(new BasicStroke(3f));
                g2.drawRect(1, 1, w - 3, h - 3);
            }

            // --- Hover sobre un destino legal: se ilumina la casilla ---
            if (esDestino && hover) {
                g2.setColor(esCaptura ? COLOR_HOVER_CAPTURA : COLOR_HOVER_DESTINO);
                g2.fillRect(0, 0, w, h);
            }

            // --- Marca de destino legal (aparece con animación de escala) ---
            if (esDestino && !(hover && !esCaptura)) {
                if (!esCaptura) {
                    float r = lado * 0.16f * escalaMarca;
                    g2.setColor(COLOR_MARCA);
                    g2.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
                } else {
                    float grosor = lado * 0.085f;
                    float r = (lado / 2f - grosor / 2f - 1) * (0.7f + 0.3f * escalaMarca);
                    g2.setColor(hover ? COLOR_MARCA_CAPTURA_HOVER : COLOR_MARCA_CAPTURA);
                    g2.setStroke(new BasicStroke(grosor));
                    g2.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
                }
            }

            // --- Casilla seleccionada: borde interior fino ---
            if (esSeleccionada) {
                g2.setColor(COLOR_BORDE_SELECCION);
                g2.setStroke(new BasicStroke(3f));
                g2.drawRect(1, 1, w - 3, h - 3);
            }

            // --- Pieza ---
            if (imagenPieza != null) {
                int margen = Math.max(4, lado / 20);
                int maxW = w - margen * 2;
                int maxH = h - margen * 2;
                double scale = Math.min((double) maxW / imagenPieza.getWidth(), (double) maxH / imagenPieza.getHeight());
                int drawW = (int) Math.round(imagenPieza.getWidth() * scale);
                int drawH = (int) Math.round(imagenPieza.getHeight() * scale);
                g2.drawImage(imagenPieza, (w - drawW) / 2, (h - drawH) / 2, drawW, drawH, null);
            } else if (!textoPieza.isEmpty()) {
                g2.setFont(new Font(PantallaMenu.FUENTE_AJEDREZ, Font.BOLD, Math.max(12, (int) (lado * 0.62f))));
                FontMetrics fm = g2.getFontMetrics();
                int x = (w - fm.stringWidth(textoPieza)) / 2;
                int y = (h - fm.getHeight()) / 2 + fm.getAscent();
                java.awt.Color sombra = colorPieza.equals(java.awt.Color.WHITE)
                        ? new java.awt.Color(30, 30, 30, 180)
                        : new java.awt.Color(220, 220, 220, 130);
                g2.setColor(sombra);
                g2.drawString(textoPieza, x - 1, y);
                g2.drawString(textoPieza, x + 1, y);
                g2.drawString(textoPieza, x, y - 1);
                g2.drawString(textoPieza, x, y + 1);
                g2.setColor(colorPieza);
                g2.drawString(textoPieza, x, y);
            }

            g2.dispose();
        }

        // Colores para las marcas de movimientos posibles
        private static final java.awt.Color COLOR_MARCA                = new java.awt.Color(30, 25, 15, 60);
        private static final java.awt.Color COLOR_MARCA_CAPTURA        = new java.awt.Color(30, 25, 15, 70);
        private static final java.awt.Color COLOR_MARCA_CAPTURA_HOVER  = new java.awt.Color(200, 50, 40, 170);
        private static final java.awt.Color COLOR_HOVER_DESTINO        = new java.awt.Color(120, 170, 70, 130);
        private static final java.awt.Color COLOR_HOVER_CAPTURA        = new java.awt.Color(220, 80, 60, 60);
        private static final java.awt.Color COLOR_BORDE_SELECCION      = new java.awt.Color(200, 170, 40, 200);
    }
}

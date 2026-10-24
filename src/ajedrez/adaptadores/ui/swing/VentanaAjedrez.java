package ajedrez.adaptadores.ui.swing;

import ajedrez.adaptadores.ui.swing.perfil.GestorPerfiles;
import ajedrez.adaptadores.ui.swing.perfil.PerfilJugador;
import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.dominio.TipoPieza;
import ajedrez.nucleo.estados.EstadoJaqueMate;
import ajedrez.nucleo.servicio.PartidaAjedrez;
import ajedrez.nucleo.servicio.SelectorPromocion;
import ajedrez.nucleo.tablero.Tablero;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ventana principal del juego de Ajedrez.
 *
 * Mejoras incluidas:
 * - Tablero con coordenadas (letras a-h / números 1-8) alrededor del tablero
 * - Selección de pieza con borde verde vibrante en casillas destino
 * - Ícono de ventana personalizado (rey dorado o archivo icono.png)
 * - Sistema de perfil de jugador con barra de nivel exponencial
 * - El jugador puede elegir con qué color jugar
 * - Vuelve al menú al terminar la partida
 */
public class VentanaAjedrez extends JFrame implements SelectorPromocion {

    // -----------------------------------------------------------------------
    // Paleta visual
    // -----------------------------------------------------------------------
    private static final java.awt.Color COLOR_CASILLA_CLARA    = new java.awt.Color(240, 217, 181);
    private static final java.awt.Color COLOR_CASILLA_OSCURA   = new java.awt.Color(181, 136, 99);
    private static final java.awt.Color COLOR_SELECCIONADO     = new java.awt.Color(246, 236, 90);   // amarillo suave
    private static final java.awt.Color BG_PANEL              = new java.awt.Color(35, 32, 28);
    private static final java.awt.Color COLOR_COORD            = new java.awt.Color(200, 180, 140);
    private static final java.awt.Color COLOR_DORADO           = new java.awt.Color(212, 175, 55);

    // -----------------------------------------------------------------------
    // Estado interno
    // -----------------------------------------------------------------------
    private final PartidaAjedrez partida;
    private PerfilJugador perfilJugador;              // puede ser null si no hay perfil
    private final Color colorJugador;                 // color elegido por el humano
    private final PantallaMenu menuOrigen;            // para volver al menú al salir

    private final CasillaBoton[][] botonesCasillas = new CasillaBoton[8][8];
    private JPanel panelTablero;
    private Posicion casillaSeleccionada = null;
    private List<Posicion> destinosLegalesActuales = List.of();

    // Copias del perfil alineadas con el historial de jugadas: al deshacer se vuelve
    // al perfil de antes de la jugada, así la XP no se puede ganar dos veces.
    private final Deque<PerfilJugador> perfilesAntesDeJugada = new ArrayDeque<>();
    private final Deque<PerfilJugador> perfilesDeshechos = new ArrayDeque<>();

    // Animaciones
    private final CapaAnimacionXP capaXP = new CapaAnimacionXP();
    private Timer timerBarra;
    private Timer timerMarcas;
    private long inicioAnimacionMarcas;
    private static final int DURACION_MARCAS_MS = 180;

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
    private final JLabel labelEstado;
    private final JLabel labelJaque;
    private final JButton btnDeshacer;
    private final JButton btnRehacer;

    // Panel de perfil (barra de nivel)
    private JLabel lblNombrePerfil;
    private JLabel lblNivel;
    private JProgressBar barraExp;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    public VentanaAjedrez(PerfilJugador perfil, Color colorJugador, PantallaMenu menuOrigen) {
        this.perfilJugador = perfil;
        this.colorJugador  = colorJugador;
        this.menuOrigen    = menuOrigen;
        this.partida       = new PartidaAjedrez();
        this.partida.setSelectorPromocion(this);

        // Configurar ventana
        setTitle("Ajedrez — Jugando con " + (colorJugador == Color.BLANCO ? "Blancas" : "Negras"));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                onSalir();
            }
        });

        setSize(780, 870);
        setLocationRelativeTo(null);
        setResizable(false);
        setIconImage(PantallaMenu.crearIconoAjedrez());
        setGlassPane(capaXP);

        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_PANEL);

        // Construir secciones
        labelEstado = new JLabel("", SwingConstants.CENTER);
        labelJaque  = new JLabel("", SwingConstants.CENTER);
        btnDeshacer = new JButton();
        btnRehacer  = new JButton();

        add(construirPanelSuperior(), BorderLayout.NORTH);
        add(construirPanelTableroConCoordenadas(), BorderLayout.CENTER);
        add(construirPanelInferior(), BorderLayout.SOUTH);

        actualizarVista();
    }

    // -----------------------------------------------------------------------
    // Panel superior: estado del turno, jaque y perfil
    // -----------------------------------------------------------------------
    private JPanel construirPanelSuperior() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PANEL);
        panel.setBorder(new EmptyBorder(8, 12, 4, 12));

        // --- Info de juego (izquierda-centro) ---
        JPanel panelJuego = new JPanel(new GridLayout(2, 1));
        panelJuego.setBackground(BG_PANEL);

        labelEstado.setFont(new Font("Segoe UI", Font.BOLD, 17));
        labelEstado.setForeground(new java.awt.Color(235, 230, 220));

        labelJaque.setFont(new Font("Segoe UI", Font.BOLD, 14));
        labelJaque.setForeground(new java.awt.Color(255, 80, 80));

        panelJuego.add(labelEstado);
        panelJuego.add(labelJaque);

        // --- Panel de perfil (derecha) ---
        JPanel panelPerfil = construirPanelPerfil();

        panel.add(panelJuego, BorderLayout.CENTER);
        panel.add(panelPerfil, BorderLayout.EAST);

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

        if (perfilJugador != null) {
            lblNombrePerfil = new JLabel(perfilJugador.getNombre());
            lblNombrePerfil.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblNombrePerfil.setForeground(COLOR_DORADO);
            lblNombrePerfil.setAlignmentX(Component.RIGHT_ALIGNMENT);

            lblNivel = new JLabel("Nivel " + perfilJugador.getNivel());
            lblNivel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblNivel.setForeground(new java.awt.Color(180, 175, 165));
            lblNivel.setAlignmentX(Component.RIGHT_ALIGNMENT);

            barraExp = new JProgressBar(0, 100);
            barraExp.setValue((int)(perfilJugador.getProgresoBarra() * 100));
            barraExp.setString(perfilJugador.getPuntosEnNivelActual()
                    + "/" + perfilJugador.getPuntosParaSiguienteNivel() + " XP");
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
        } else {
            JLabel sinPerfil = new JLabel("Invitado");
            sinPerfil.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            sinPerfil.setForeground(new java.awt.Color(130, 125, 115));
            panel.add(sinPerfil);
        }

        return panel;
    }

    // -----------------------------------------------------------------------
    // Tablero con coordenadas (letras A-H y números 1-8)
    // -----------------------------------------------------------------------
    private JPanel construirPanelTableroConCoordenadas() {
        // Panel externo que envuelve el tablero con las coordenadas
        JPanel wrapper = new JPanel(new BorderLayout(4, 4));
        wrapper.setBackground(BG_PANEL);
        wrapper.setBorder(new EmptyBorder(6, 12, 6, 12));

        // --- Letras superiores (a-h) ---
        wrapper.add(construirFilaLetras(true), BorderLayout.NORTH);

        // --- Fila central: números + tablero ---
        JPanel centro = new JPanel(new BorderLayout(4, 0));
        centro.setBackground(BG_PANEL);

        centro.add(construirColumnaNumeros(true), BorderLayout.WEST);
        centro.add(construirPanelTablero(), BorderLayout.CENTER);
        centro.add(construirColumnaNumeros(false), BorderLayout.EAST);

        wrapper.add(centro, BorderLayout.CENTER);

        // --- Letras inferiores (a-h) ---
        wrapper.add(construirFilaLetras(false), BorderLayout.SOUTH);

        return wrapper;
    }

    private JPanel construirFilaLetras(boolean arriba) {
        // 8 columnas de letras, alineadas con las casillas
        JPanel fila = new JPanel(new GridLayout(1, 8));
        fila.setBackground(BG_PANEL);

        String[] letras = {"a", "b", "c", "d", "e", "f", "g", "h"};
        for (String letra : letras) {
            JLabel lbl = new JLabel(letra, SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lbl.setForeground(COLOR_COORD);
            fila.add(lbl);
        }
        return fila;
    }

    private JPanel construirColumnaNumeros(boolean izquierda) {
        JPanel col = new JPanel(new GridLayout(8, 1));
        col.setBackground(BG_PANEL);
        col.setPreferredSize(new Dimension(22, 0));

        // Fila 0 del array = fila 8 del tablero visual (las blancas empiezan abajo)
        for (int i = 0; i < 8; i++) {
            int numero = 8 - i; // fila 0 del array → número 8
            JLabel lbl = new JLabel(String.valueOf(numero), SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lbl.setForeground(COLOR_COORD);
            col.add(lbl);
        }
        return col;
    }

    private JPanel construirPanelTablero() {
        panelTablero = new JPanel(new GridLayout(8, 8));
        panelTablero.setBorder(BorderFactory.createLineBorder(new java.awt.Color(25, 22, 18), 3));

        for (int fila = 0; fila < 8; fila++) {
            for (int col = 0; col < 8; col++) {
                CasillaBoton boton = new CasillaBoton();
                final int f = fila;
                final int c = col;
                boton.addActionListener(e -> onCasillaClick(new Posicion(f, c)));
                botonesCasillas[fila][col] = boton;
                panelTablero.add(boton);
            }
        }
        return panelTablero;
    }

    // -----------------------------------------------------------------------
    // Panel inferior: controles
    // -----------------------------------------------------------------------
    private JPanel construirPanelInferior() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panel.setBackground(BG_PANEL);
        panel.setBorder(new EmptyBorder(0, 0, 6, 0));

        estilizarBotonControl(btnDeshacer, "Deshacer");
        btnDeshacer.addActionListener(e -> {
            if (partida.getEstadoActual().esFinalizada()) return;
            if (partida.deshacerMovimiento()) {
                if (perfilJugador != null && !perfilesAntesDeJugada.isEmpty()) {
                    perfilesDeshechos.push(perfilJugador);
                    perfilJugador = perfilesAntesDeJugada.pop();
                    actualizarBarraPerfil();
                }
                casillaSeleccionada = null;
                destinosLegalesActuales = List.of();
                actualizarVista();
            }
        });

        estilizarBotonControl(btnRehacer, "Rehacer");
        btnRehacer.addActionListener(e -> {
            if (partida.getEstadoActual().esFinalizada()) return;
            if (partida.rehacerMovimiento()) {
                // Se recupera la XP que ya se había ganado con esa jugada (no se suma de nuevo)
                if (perfilJugador != null && !perfilesDeshechos.isEmpty()) {
                    perfilesAntesDeJugada.push(perfilJugador);
                    perfilJugador = perfilesDeshechos.pop();
                    actualizarBarraPerfil();
                }
                casillaSeleccionada = null;
                destinosLegalesActuales = List.of();
                actualizarVista();
            }
        });

        JButton btnNuevaPartida = new JButton();
        estilizarBotonControl(btnNuevaPartida, "Nueva Partida");
        btnNuevaPartida.addActionListener(e -> reiniciarPartida());

        JButton btnMenu = new JButton();
        estilizarBotonControl(btnMenu, "Menú");
        btnMenu.addActionListener(e -> onSalir());

        panel.add(btnDeshacer);
        panel.add(btnRehacer);
        panel.add(btnNuevaPartida);
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
    // Lógica de clicks en casillas
    // -----------------------------------------------------------------------
    private void onCasillaClick(Posicion clickeada) {
        if (partida.getEstadoActual().esFinalizada()) return;

        if (casillaSeleccionada != null) {
            if (casillaSeleccionada.equals(clickeada)) {
                // Deseleccionar
                casillaSeleccionada = null;
                destinosLegalesActuales = List.of();
                actualizarVista();
                return;
            }

            // Guardar pieza en destino y quién mueve antes de mover (para detectar captura)
            Pieza piezaEnDestino = partida.getTablero().obtenerPieza(clickeada);
            boolean mueveJugador = partida.getEstadoActual().getColorTurnoActual() == colorJugador;
            PerfilJugador perfilAntes = perfilJugador != null ? perfilJugador.copiar() : null;

            boolean movio = partida.intentarMover(casillaSeleccionada, clickeada);
            if (movio) {
                if (perfilAntes != null) {
                    perfilesAntesDeJugada.push(perfilAntes);
                    perfilesDeshechos.clear(); // una jugada nueva invalida lo que se podía rehacer
                }
                // La XP solo se otorga por movimientos del color que eligió el jugador
                if (mueveJugador) {
                    int retraso = 0;
                    // ¿Comió una pieza del rival?
                    if (piezaEnDestino != null && piezaEnDestino.getColor() == colorJugador.opuesto()) {
                        registrarPiezaComida(clickeada);
                        retraso = 350;
                    }
                    // ¿Dejó al rey rival en jaque?
                    if (partida.estaEnJaqueTurnoActual()) {
                        registrarJaque(clickeada, retraso);
                    }
                }
                // ¿Terminó la partida?
                if (partida.getEstadoActual().esFinalizada()) {
                    onPartidaFinalizada();
                }
                casillaSeleccionada = null;
                destinosLegalesActuales = List.of();
                actualizarVista();
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
            casillaSeleccionada = null;
            destinosLegalesActuales = List.of();
        }
        actualizarVista();
    }

    // -----------------------------------------------------------------------
    // Actualizar render del tablero
    // -----------------------------------------------------------------------
    private void actualizarVista() {
        Tablero tablero = partida.getTablero();

        for (int fila = 0; fila < 8; fila++) {
            for (int col = 0; col < 8; col++) {
                Posicion pos = new Posicion(fila, col);
                CasillaBoton boton = botonesCasillas[fila][col];
                Pieza pieza = tablero.obtenerPieza(pos);

                // Render de la pieza
                if (pieza != null) {
                    BufferedImage imgPieza = (pieza.getColor() == Color.NEGRO)
                            ? imagenesNegras.get(pieza.getTipo())
                            : imagenesBlancas.get(pieza.getTipo());

                    if (imgPieza != null) {
                        boton.setPiezaImagen(imgPieza);
                    } else {
                        // Pieza blanca o sin imagen → usar símbolo Unicode
                        java.awt.Color colorTexto = (pieza.getColor() == Color.BLANCO)
                                ? new java.awt.Color(255, 255, 255)
                                : new java.awt.Color(20, 20, 20);
                        boton.setPiezaVisual(pieza.getSimboloUnicode(), colorTexto);
                    }
                } else {
                    boton.setPiezaVisual("", java.awt.Color.BLACK);
                }

                // Color base de la casilla
                boolean esCasillaClara = (fila + col) % 2 == 0;
                java.awt.Color colorBase = esCasillaClara ? COLOR_CASILLA_CLARA : COLOR_CASILLA_OSCURA;

                // Estado de resaltado
                boolean esSeleccionada = casillaSeleccionada != null && casillaSeleccionada.equals(pos);
                boolean esDestino      = destinosLegalesActuales.contains(pos);

                boton.setBackground(esSeleccionada ? mezclar(colorBase, COLOR_SELECCIONADO, 0.55f) : colorBase);
                boton.setHighlight(
                        esDestino,
                        esDestino && pieza != null,   // captura posible
                        esSeleccionada
                );
            }
        }

        // Labels de estado
        labelEstado.setText(partida.getEstadoActual().getDescripcion());
        if (partida.estaEnJaqueTurnoActual()) {
            labelJaque.setText("¡EL REY ESTÁ EN JAQUE!");
        } else {
            labelJaque.setText("");
        }

        // Con la partida terminada no se puede deshacer (evita repetir la XP de la victoria)
        boolean finalizada = partida.getEstadoActual().esFinalizada();
        btnDeshacer.setEnabled(!finalizada && partida.getHistorialComandos().puedeDeshacer());
        btnRehacer.setEnabled(!finalizada && partida.getHistorialComandos().puedeRehacer());
    }

    private static java.awt.Color mezclar(java.awt.Color a, java.awt.Color b, float t) {
        return new java.awt.Color(
                Math.round(a.getRed()   + (b.getRed()   - a.getRed())   * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t));
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
    // Perfil: registrar eventos
    // -----------------------------------------------------------------------
    private void registrarPiezaComida(Posicion pos) {
        if (perfilJugador == null) return;
        boolean subioNivel = perfilJugador.registrarPiezaComida();
        animarGananciaXP(PerfilJugador.PUNTOS_COMER_PIEZA, "Captura", centroDeCasilla(pos), 0, subioNivel);
    }

    private void registrarJaque(Posicion pos, int retrasoMs) {
        if (perfilJugador == null) return;
        boolean subioNivel = perfilJugador.registrarJaque();
        animarGananciaXP(PerfilJugador.PUNTOS_JAQUE, "Jaque", centroDeCasilla(pos), retrasoMs, subioNivel);
    }

    private void registrarVictoria() {
        if (perfilJugador == null) return;
        int nivelAntes = perfilJugador.getNivel();
        perfilJugador.registrarVictoria();
        animarGananciaXP(PerfilJugador.PUNTOS_VICTORIA, "Victoria", centroDelTablero(), 0,
                perfilJugador.getNivel() > nivelAntes);
        GestorPerfiles.actualizarPerfil(perfilJugador);
    }

    private void actualizarBarraPerfil() {
        if (perfilJugador == null || barraExp == null) return;
        if (timerBarra != null) timerBarra.stop();
        barraExp.setValue((int) (perfilJugador.getProgresoBarra() * 100));
        actualizarTextosPerfil();
    }

    private void actualizarTextosPerfil() {
        barraExp.setString(perfilJugador.getPuntosEnNivelActual()
                + "/" + perfilJugador.getPuntosParaSiguienteNivel() + " XP");
        if (lblNivel != null) lblNivel.setText("Nivel " + perfilJugador.getNivel());
    }

    // -----------------------------------------------------------------------
    // Animaciones de XP
    // -----------------------------------------------------------------------
    private void animarGananciaXP(int puntos, String motivo, Point centro, int retrasoMs, boolean subioNivel) {
        capaXP.mostrar("+" + puntos + " XP", motivo, centro, retrasoMs, 24f);
        if (subioNivel) {
            capaXP.mostrar("¡NIVEL " + perfilJugador.getNivel() + "!", "Subiste de nivel",
                    centroDelTablero(), retrasoMs + 450, 44f);
        }
        actualizarTextosPerfil();
        animarBarra((int) (perfilJugador.getProgresoBarra() * 100), subioNivel);
    }

    /** Llena la barra de XP de forma progresiva; si subió de nivel, la llena, la vacía y sigue. */
    private void animarBarra(int objetivo, boolean pasarPorLleno) {
        if (barraExp == null) return;
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
    // Partido finalizado / salir
    // -----------------------------------------------------------------------
    private void onPartidaFinalizada() {
        // Determinar ganador
        String desc = partida.getEstadoActual().getDescripcion();
        boolean gane = partida.getEstadoActual() instanceof EstadoJaqueMate mate
                && mate.getGanador() == colorJugador;

        if (perfilJugador != null) {
            perfilJugador.registrarPartidaJugada();
            if (gane) registrarVictoria();
            GestorPerfiles.actualizarPerfil(perfilJugador);
        }

        String mensaje = gane
                ? "¡Ganaste la partida!\n+" + PerfilJugador.PUNTOS_VICTORIA + " XP"
                : "La partida terminó.\n" + desc;

        // Se espera a que se vea la animación de XP antes de mostrar el cartel
        Timer espera = new Timer(gane && perfilJugador != null ? 1300 : 0, e ->
                JOptionPane.showMessageDialog(this, mensaje, "Fin de partida",
                        gane ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.PLAIN_MESSAGE));
        espera.setRepeats(false);
        espera.start();
    }

    private void reiniciarPartida() {
        perfilesAntesDeJugada.clear();
        perfilesDeshechos.clear();
        // Si la partida quedó sin terminar, se descarta la XP ganada en ella
        if (!partida.getEstadoActual().esFinalizada()) {
            descartarProgresoNoGuardado();
            actualizarBarraPerfil();
        }
        partida.iniciarNuevaPartida();
        casillaSeleccionada = null;
        destinosLegalesActuales = List.of();
        actualizarVista();
    }

    private void onSalir() {
        int resp = JOptionPane.showConfirmDialog(this,
                "No se guardará el progreso de esta partida.\n¿Seguro que querés salir al menú principal?",
                "Salir de la partida", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (resp != JOptionPane.YES_OPTION) return;

        // No se guarda: se vuelve al último progreso guardado en disco
        descartarProgresoNoGuardado();
        this.dispose();
        if (menuOrigen != null) {
            menuOrigen.onPartidaTerminada(perfilJugador);
        }
    }

    private void descartarProgresoNoGuardado() {
        if (perfilJugador != null) {
            perfilJugador = GestorPerfiles.recargarPerfil(perfilJugador);
        }
    }

    // -----------------------------------------------------------------------
    // Componente visual de casilla con highlight de borde
    // -----------------------------------------------------------------------
    private static class CasillaBoton extends JButton {
        private String textoPieza = "";
        private java.awt.Color colorPieza = java.awt.Color.BLACK;
        private BufferedImage imagenPieza = null;
        private boolean esDestino    = false;
        private boolean esCaptura    = false;
        private boolean esSeleccionada = false;
        private boolean hover = false;
        private float escalaMarca = 1f;

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
            this.imagenPieza = null; // Limpiar imagen
            repaint();
        }

        public void setPiezaImagen(BufferedImage imagen) {
            this.imagenPieza = imagen;
            this.textoPieza = ""; // Limpiar texto
            repaint();
        }

        public void setHighlight(boolean esDestino, boolean esCaptura, boolean esSeleccionada) {
            this.esDestino     = esDestino;
            this.esCaptura     = esCaptura;
            this.esSeleccionada = esSeleccionada;
            repaint();
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

            // --- Hover sobre un destino legal: se ilumina la casilla ---
            if (esDestino && hover) {
                g2.setColor(esCaptura ? COLOR_HOVER_CAPTURA : COLOR_HOVER_DESTINO);
                g2.fillRect(0, 0, w, h);
            }

            // --- Marca de destino legal (aparece con animación de escala) ---
            if (esDestino && !(hover && !esCaptura)) {
                if (!esCaptura) {
                    // Casilla vacía → punto suave en el centro
                    float r = lado * 0.16f * escalaMarca;
                    g2.setColor(COLOR_MARCA);
                    g2.fill(new java.awt.geom.Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
                } else {
                    // Pieza rival → anillo que rodea la pieza
                    float grosor = lado * 0.085f;
                    float r = (lado / 2f - grosor / 2f - 1) * (0.7f + 0.3f * escalaMarca);
                    g2.setColor(hover ? COLOR_MARCA_CAPTURA_HOVER : COLOR_MARCA_CAPTURA);
                    g2.setStroke(new BasicStroke(grosor));
                    g2.draw(new java.awt.geom.Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
                }
            }

            // --- Casilla seleccionada: borde interior fino ---
            if (esSeleccionada) {
                g2.setColor(COLOR_BORDE_SELECCION);
                g2.setStroke(new BasicStroke(3f));
                g2.drawRect(1, 1, w - 3, h - 3);
            }

            // --- Dibujo de la pieza ---
            if (imagenPieza != null) {
                // Pieza con imagen PNG personalizada (mantiene proporcion de aspecto)
                int margen = 4;
                int maxW = w - margen * 2;
                int maxH = h - margen * 2;
                int imgW = imagenPieza.getWidth();
                int imgH = imagenPieza.getHeight();
                double scale = Math.min((double) maxW / imgW, (double) maxH / imgH);
                int drawW = (int) Math.round(imgW * scale);
                int drawH = (int) Math.round(imgH * scale);
                int ix = (w - drawW) / 2;
                int iy = (h - drawH) / 2;
                g2.drawImage(imagenPieza, ix, iy, drawW, drawH, null);
            } else if (!textoPieza.isEmpty()) {
                g2.setFont(new Font(PantallaMenu.FUENTE_AJEDREZ, Font.BOLD, 44));
                FontMetrics fm = g2.getFontMetrics();
                int x = (w - fm.stringWidth(textoPieza)) / 2;
                int y = (h - fm.getHeight()) / 2 + fm.getAscent();

                // Sombra/contorno para contraste
                java.awt.Color sombra = colorPieza.equals(new java.awt.Color(255, 255, 255))
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

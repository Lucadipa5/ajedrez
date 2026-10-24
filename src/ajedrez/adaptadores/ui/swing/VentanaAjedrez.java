package ajedrez.adaptadores.ui.swing;

import ajedrez.adaptadores.ui.swing.perfil.GestorPerfiles;
import ajedrez.adaptadores.ui.swing.perfil.PerfilJugador;
import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.dominio.TipoPieza;
import ajedrez.nucleo.servicio.PartidaAjedrez;
import ajedrez.nucleo.servicio.SelectorPromocion;
import ajedrez.nucleo.tablero.Tablero;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

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
    private static final java.awt.Color COLOR_SELECCIONADO     = new java.awt.Color(252, 231, 50);   // amarillo vivo
    private static final java.awt.Color COLOR_DESTINO_BORDE    = new java.awt.Color(80, 200, 90);    // verde vivo (borde)
    private static final java.awt.Color COLOR_DESTINO_FILL     = new java.awt.Color(80, 200, 90, 70);// verde translúcido
    private static final java.awt.Color COLOR_CAPTURA_BORDE    = new java.awt.Color(220, 60, 60);    // rojo (hay pieza enemiga)
    private static final java.awt.Color COLOR_CAPTURA_FILL     = new java.awt.Color(220, 60, 60, 60);
    private static final java.awt.Color BG_PANEL               = new java.awt.Color(35, 32, 28);
    private static final java.awt.Color COLOR_COORD            = new java.awt.Color(200, 180, 140);
    private static final java.awt.Color COLOR_DORADO           = new java.awt.Color(212, 175, 55);

    // -----------------------------------------------------------------------
    // Estado interno
    // -----------------------------------------------------------------------
    private final PartidaAjedrez partida;
    private final PerfilJugador perfilJugador;        // puede ser null si no hay perfil
    private final Color colorJugador;                 // color elegido por el humano
    private final PantallaMenu menuOrigen;            // para volver al menú al salir

    private final CasillaBoton[][] botonesCasillas = new CasillaBoton[8][8];
    private Posicion casillaSeleccionada = null;
    private List<Posicion> destinosLegalesActuales = List.of();

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
        setTitle("Ajedrez — Jugando con " + (colorJugador == Color.BLANCO ? "Blancas ♔" : "Negras ♚"));
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
            JLabel sinPerfil = new JLabel("Sin perfil");
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
        JPanel panelTablero = new JPanel(new GridLayout(8, 8));
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

        estilizarBotonControl(btnDeshacer, "⏮ Deshacer");
        btnDeshacer.addActionListener(e -> {
            if (partida.deshacerMovimiento()) {
                casillaSeleccionada = null;
                destinosLegalesActuales = List.of();
                actualizarVista();
            }
        });

        estilizarBotonControl(btnRehacer, "⏭ Rehacer");
        btnRehacer.addActionListener(e -> {
            if (partida.rehacerMovimiento()) {
                casillaSeleccionada = null;
                destinosLegalesActuales = List.of();
                actualizarVista();
            }
        });

        JButton btnNuevaPartida = new JButton("🔄 Nueva Partida");
        estilizarBotonControl(btnNuevaPartida, "🔄 Nueva Partida");
        btnNuevaPartida.addActionListener(e -> reiniciarPartida());

        JButton btnMenu = new JButton("🏠 Menú");
        estilizarBotonControl(btnMenu, "🏠 Menú");
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
        String[] opciones = {"Reina (♕)", "Torre (♖)", "Alfil (♗)", "Caballo (♘)"};
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

            // Guardar pieza en destino antes de mover (para detectar captura)
            Pieza piezaEnDestino = partida.getTablero().obtenerPieza(clickeada);

            boolean movio = partida.intentarMover(casillaSeleccionada, clickeada);
            if (movio) {
                // ¿Hubo captura?
                if (piezaEnDestino != null && piezaEnDestino.getColor() != colorJugador.opuesto()) {
                    // era pieza enemiga → comiste una pieza
                    registrarPiezaComida();
                }
                // ¿Está en jaque?
                if (partida.estaEnJaqueTurnoActual()) {
                    registrarJaque();
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
                    java.awt.Color colorTexto = (pieza.getColor() == Color.BLANCO)
                            ? new java.awt.Color(255, 255, 255)
                            : new java.awt.Color(20, 20, 20);
                    boton.setPiezaVisual(pieza.getSimboloUnicode(), colorTexto);
                } else {
                    boton.setPiezaVisual("", java.awt.Color.BLACK);
                }

                // Color base de la casilla
                boolean esCasillaClara = (fila + col) % 2 == 0;
                java.awt.Color colorBase = esCasillaClara ? COLOR_CASILLA_CLARA : COLOR_CASILLA_OSCURA;

                // Estado de resaltado
                boolean esSeleccionada = casillaSeleccionada != null && casillaSeleccionada.equals(pos);
                boolean esDestino      = destinosLegalesActuales.contains(pos);
                boolean hayPiezaEnemiga = pieza != null
                        && casillaSeleccionada != null
                        && pieza.getColor() != tablero.obtenerPieza(casillaSeleccionada).getColor();

                boton.setBackground(esSeleccionada ? COLOR_SELECCIONADO : colorBase);
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
            labelJaque.setText("⚠ ¡EL REY ESTÁ EN JAQUE! ⚠");
        } else {
            labelJaque.setText("");
        }

        btnDeshacer.setEnabled(partida.getHistorialComandos().puedeDeshacer());
        btnRehacer.setEnabled(partida.getHistorialComandos().puedeRehacer());
    }

    // -----------------------------------------------------------------------
    // Perfil: registrar eventos
    // -----------------------------------------------------------------------
    private void registrarPiezaComida() {
        if (perfilJugador == null) return;
        boolean subioNivel = perfilJugador.registrarPiezaComida();
        actualizarBarraPerfil();
        if (subioNivel) mostrarSubidaNivel();
    }

    private void registrarJaque() {
        if (perfilJugador == null) return;
        boolean subioNivel = perfilJugador.registrarJaque();
        actualizarBarraPerfil();
        if (subioNivel) mostrarSubidaNivel();
    }

    private void registrarVictoria() {
        if (perfilJugador == null) return;
        perfilJugador.registrarVictoria();
        actualizarBarraPerfil();
        GestorPerfiles.actualizarPerfil(perfilJugador);
    }

    private void actualizarBarraPerfil() {
        if (perfilJugador == null || barraExp == null) return;
        int prog = (int)(perfilJugador.getProgresoBarra() * 100);
        barraExp.setValue(prog);
        barraExp.setString(perfilJugador.getPuntosEnNivelActual()
                + "/" + perfilJugador.getPuntosParaSiguienteNivel() + " XP");
        if (lblNivel != null) lblNivel.setText("Nivel " + perfilJugador.getNivel());
    }

    private void mostrarSubidaNivel() {
        if (perfilJugador == null) return;
        JOptionPane.showMessageDialog(this,
                "🎉 ¡Subiste al nivel " + perfilJugador.getNivel() + "!\n"
                        + perfilJugador.getPuntosAcumulados() + " puntos acumulados.",
                "¡Subiste de nivel!", JOptionPane.INFORMATION_MESSAGE);
    }

    // -----------------------------------------------------------------------
    // Partido finalizado / salir
    // -----------------------------------------------------------------------
    private void onPartidaFinalizada() {
        // Determinar ganador
        String desc = partida.getEstadoActual().getDescripcion();
        boolean gane = desc.contains(colorJugador == Color.BLANCO ? "Blanco" : "Negro");

        if (perfilJugador != null) {
            perfilJugador.registrarPartidaJugada();
            if (gane) registrarVictoria();
            GestorPerfiles.actualizarPerfil(perfilJugador);
        }

        String mensaje = gane
                ? "🏆 ¡Ganaste la partida!\n+" + PerfilJugador.PUNTOS_VICTORIA + " XP"
                : "La partida terminó.\n" + desc;

        JOptionPane.showMessageDialog(this, mensaje, "Fin de partida",
                gane ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.PLAIN_MESSAGE);
    }

    private void reiniciarPartida() {
        partida.iniciarNuevaPartida();
        casillaSeleccionada = null;
        destinosLegalesActuales = List.of();
        actualizarVista();
    }

    private void onSalir() {
        if (perfilJugador != null) {
            GestorPerfiles.actualizarPerfil(perfilJugador);
        }
        this.dispose();
        if (menuOrigen != null) {
            menuOrigen.onPartidaTerminada();
        }
    }

    // -----------------------------------------------------------------------
    // Componente visual de casilla con highlight de borde
    // -----------------------------------------------------------------------
    private static class CasillaBoton extends JButton {
        private String textoPieza = "";
        private java.awt.Color colorPieza = java.awt.Color.BLACK;
        private boolean esDestino    = false;
        private boolean esCaptura    = false;
        private boolean esSeleccionada = false;

        public CasillaBoton() {
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        public void setPiezaVisual(String texto, java.awt.Color color) {
            this.textoPieza = texto != null ? texto : "";
            this.colorPieza = color;
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

            int w = getWidth();
            int h = getHeight();

            // --- Overlay de destino legal ---
            if (esDestino && !esCaptura) {
                // Casilla vacía → círculo verde en el centro
                g2.setColor(COLOR_DESTINO_FILL);
                int r = Math.min(w, h) / 4;
                g2.fillOval(w / 2 - r, h / 2 - r, r * 2, r * 2);
                // Borde verde suave
                g2.setColor(COLOR_DESTINO_BORDE);
                g2.setStroke(new BasicStroke(3f));
                g2.drawOval(w / 2 - r, h / 2 - r, r * 2, r * 2);
            } else if (esDestino && esCaptura) {
                // Casilla con pieza enemiga → borde rojo alrededor de toda la casilla
                g2.setColor(COLOR_CAPTURA_FILL);
                g2.fillRect(0, 0, w, h);
                g2.setColor(COLOR_CAPTURA_BORDE);
                g2.setStroke(new BasicStroke(4f));
                g2.drawRect(2, 2, w - 4, h - 4);
            }

            // --- Borde amarillo si la casilla está seleccionada ---
            if (esSeleccionada) {
                g2.setColor(COLOR_SELECCIONADO);
                g2.setStroke(new BasicStroke(4f));
                g2.drawRect(2, 2, w - 4, h - 4);
            }

            // --- Dibujo de la pieza ---
            if (!textoPieza.isEmpty()) {
                g2.setFont(new Font("Segoe UI Symbol", Font.BOLD, 44));
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

        // Colores para los highlights de destino
        private static final java.awt.Color COLOR_DESTINO_FILL  = new java.awt.Color(80, 200, 90, 70);
        private static final java.awt.Color COLOR_DESTINO_BORDE = new java.awt.Color(80, 200, 90);
        private static final java.awt.Color COLOR_CAPTURA_FILL  = new java.awt.Color(220, 60, 60, 60);
        private static final java.awt.Color COLOR_CAPTURA_BORDE = new java.awt.Color(220, 60, 60);
        private static final java.awt.Color COLOR_SELECCIONADO  = new java.awt.Color(252, 231, 50);
    }
}

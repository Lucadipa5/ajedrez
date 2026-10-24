package ajedrez.adaptadores.ui.swing;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.dominio.TipoPieza;
import ajedrez.nucleo.servicio.PartidaAjedrez;
import ajedrez.nucleo.servicio.SelectorPromocion;
import ajedrez.nucleo.tablero.Tablero;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Adaptador de Entrada (Driving Adapter) - Interfaz Gráfica en Swing.
 *
 * Implementa SelectorPromocion para permitir al usuario elegir la pieza al coronar,
 * y renderiza el tablero con alta fidelidad y contraste antialiased.
 */
public class VentanaAjedrez extends JFrame implements SelectorPromocion {
    private final PartidaAjedrez partida;
    private final CasillaBoton[][] botonesCasillas = new CasillaBoton[8][8];
    private final JLabel labelEstado;
    private final JLabel labelJaque;
    private final JButton btnDeshacer;
    private final JButton btnRehacer;

    private Posicion casillaSeleccionada = null;
    private List<Posicion> destinosLegalesActuales = List.of();

    private static final java.awt.Color COLOR_CLARO = new java.awt.Color(240, 217, 181);
    private static final java.awt.Color COLOR_OSCURO = new java.awt.Color(181, 136, 99);
    private static final java.awt.Color COLOR_SELECCIONADO = new java.awt.Color(246, 246, 105);
    private static final java.awt.Color COLOR_DESTINO = new java.awt.Color(186, 202, 68);

    public VentanaAjedrez(PartidaAjedrez partida) {
        this.partida = partida;
        // Inyectamos esta ventana como el proveedor de elección de coronación
        this.partida.setSelectorPromocion(this);

        setTitle("Ajedrez - Arquitectura Hexagonal y Principios SOLID");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 780);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- Panel Superior: Estado del Turno y Jaque ---
        JPanel panelSuperior = new JPanel(new GridLayout(2, 1));
        panelSuperior.setBackground(new java.awt.Color(48, 46, 43));
        panelSuperior.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        labelEstado = new JLabel("Turno: Blancas", SwingConstants.CENTER);
        labelEstado.setFont(new Font("Segoe UI", Font.BOLD, 18));
        labelEstado.setForeground(java.awt.Color.WHITE);

        labelJaque = new JLabel("", SwingConstants.CENTER);
        labelJaque.setFont(new Font("Segoe UI", Font.BOLD, 15));
        labelJaque.setForeground(new java.awt.Color(255, 75, 75));

        panelSuperior.add(labelEstado);
        panelSuperior.add(labelJaque);
        add(panelSuperior, BorderLayout.NORTH);

        // --- Panel Central: Grilla 8x8 de Casillas ---
        JPanel panelTablero = new JPanel(new GridLayout(8, 8));
        panelTablero.setBorder(BorderFactory.createLineBorder(new java.awt.Color(38, 36, 33), 4));

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
        add(panelTablero, BorderLayout.CENTER);

        // --- Panel Inferior: Controles de Command (Undo / Redo / Reiniciar) ---
        JPanel panelInferior = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panelInferior.setBackground(new java.awt.Color(48, 46, 43));

        btnDeshacer = new JButton("⏮ Deshacer (Undo)");
        btnDeshacer.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnDeshacer.addActionListener(e -> {
            if (partida.deshacerMovimiento()) {
                casillaSeleccionada = null;
                destinosLegalesActuales = List.of();
                actualizarVista();
            }
        });

        btnRehacer = new JButton("⏭ Rehacer (Redo)");
        btnRehacer.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRehacer.addActionListener(e -> {
            if (partida.rehacerMovimiento()) {
                casillaSeleccionada = null;
                destinosLegalesActuales = List.of();
                actualizarVista();
            }
        });

        JButton btnNuevaPartida = new JButton("🔄 Nueva Partida");
        btnNuevaPartida.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnNuevaPartida.addActionListener(e -> {
            partida.iniciarNuevaPartida();
            casillaSeleccionada = null;
            destinosLegalesActuales = List.of();
            actualizarVista();
        });

        panelInferior.add(btnDeshacer);
        panelInferior.add(btnRehacer);
        panelInferior.add(btnNuevaPartida);
        add(panelInferior, BorderLayout.SOUTH);

        actualizarVista();
    }

    /**
     * Implementación del puerto SelectorPromocion.
     * Muestra un diálogo interactivo en Swing para que el jugador elija a qué pieza coronar.
     */
    @Override
    public TipoPieza solicitarPromocion(Color color) {
        String[] opciones = {"Reina (♕)", "Torre (♖)", "Alfil (♗)", "Caballo (♘)"};
        int seleccion = JOptionPane.showOptionDialog(
                this,
                "¡Tu peón ha llegado a la última fila!\nElegí la pieza para coronar:",
                "Coronación de Peón (" + color + ")",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]
        );

        switch (seleccion) {
            case 1: return TipoPieza.TORRE;
            case 2: return TipoPieza.ALFIL;
            case 3: return TipoPieza.CABALLO;
            case 0:
            default:
                return TipoPieza.REINA;
        }
    }

    private void onCasillaClick(Posicion clickeada) {
        if (partida.getEstadoActual().esFinalizada()) {
            return;
        }

        // Si ya teníamos una casilla seleccionada previamente
        if (casillaSeleccionada != null) {
            // Si hace click en la misma casilla, deseleccionamos
            if (casillaSeleccionada.equals(clickeada)) {
                casillaSeleccionada = null;
                destinosLegalesActuales = List.of();
                actualizarVista();
                return;
            }

            // Intentamos ejecutar el movimiento a través del Núcleo
            boolean movio = partida.intentarMover(casillaSeleccionada, clickeada);
            if (movio) {
                casillaSeleccionada = null;
                destinosLegalesActuales = List.of();
                actualizarVista();
                return;
            }
        }

        // Si no había selección previa o el movimiento fue inválido, evaluamos seleccionar la nueva
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

    private void actualizarVista() {
        Tablero tablero = partida.getTablero();

        for (int fila = 0; fila < 8; fila++) {
            for (int col = 0; col < 8; col++) {
                Posicion pos = new Posicion(fila, col);
                CasillaBoton boton = botonesCasillas[fila][col];
                Pieza pieza = tablero.obtenerPieza(pos);

                // Configurar render visual de la pieza con alto contraste
                if (pieza != null) {
                    java.awt.Color colorTexto = (pieza.getColor() == Color.BLANCO)
                            ? new java.awt.Color(255, 255, 255)
                            : new java.awt.Color(25, 25, 25);
                    boton.setPiezaVisual(pieza.getSimboloUnicode(), colorTexto);
                } else {
                    boton.setPiezaVisual("", java.awt.Color.BLACK);
                }

                // Coloreado del fondo de la casilla
                boolean esCasillaClara = (fila + col) % 2 == 0;
                java.awt.Color colorBase = esCasillaClara ? COLOR_CLARO : COLOR_OSCURO;

                if (casillaSeleccionada != null && casillaSeleccionada.equals(pos)) {
                    boton.setBackground(COLOR_SELECCIONADO);
                } else if (destinosLegalesActuales.contains(pos)) {
                    boton.setBackground(COLOR_DESTINO);
                } else {
                    boton.setBackground(colorBase);
                }
            }
        }

        // Actualizar barra de estado y advertencias de jaque
        labelEstado.setText(partida.getEstadoActual().getDescripcion());
        if (partida.estaEnJaqueTurnoActual()) {
            labelJaque.setText("⚠ ¡EL REY ESTÁ EN JAQUE! ⚠");
        } else {
            labelJaque.setText("");
        }

        btnDeshacer.setEnabled(partida.getHistorialComandos().puedeDeshacer());
        btnRehacer.setEnabled(partida.getHistorialComandos().puedeRehacer());
    }

    /**
     * Componente visual personalizado para las casillas del tablero.
     * Dibuja los caracteres Unicode con antialiasing y un borde de sombra de alto contraste
     * para garantizar perfecta legibilidad tanto en casillas claras como oscuras.
     */
    private static class CasillaBoton extends JButton {
        private String textoPieza = "";
        private java.awt.Color colorPieza = java.awt.Color.BLACK;

        public CasillaBoton() {
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(true);
        }

        public void setPiezaVisual(String texto, java.awt.Color color) {
            this.textoPieza = texto != null ? texto : "";
            this.colorPieza = color;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!textoPieza.isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(new Font("Segoe UI Symbol", Font.BOLD, 46));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(textoPieza)) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

                // Contorno sutil oscuro para que las piezas blancas destaquen en casillas claras
                if (colorPieza.equals(new java.awt.Color(255, 255, 255))) {
                    g2.setColor(new java.awt.Color(50, 50, 50, 200));
                    g2.drawString(textoPieza, x - 1, y);
                    g2.drawString(textoPieza, x + 1, y);
                    g2.drawString(textoPieza, x, y - 1);
                    g2.drawString(textoPieza, x, y + 1);
                } else {
                    // Contorno sutil claro para que las piezas negras destaquen en casillas oscuras
                    g2.setColor(new java.awt.Color(220, 220, 220, 140));
                    g2.drawString(textoPieza, x - 1, y);
                    g2.drawString(textoPieza, x + 1, y);
                    g2.drawString(textoPieza, x, y - 1);
                    g2.drawString(textoPieza, x, y + 1);
                }

                // Cuerpo principal del glifo
                g2.setColor(colorPieza);
                g2.drawString(textoPieza, x, y);
                g2.dispose();
            }
        }
    }
}

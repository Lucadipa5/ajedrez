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

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ventana del juego de Ajedrez (adaptador de entrada Swing).
 *
 * Solo se ocupa de mostrar el tablero y traducir clicks en jugadas: las reglas del juego,
 * la XP y el turno de la IA los resuelve el núcleo a través de {@link SesionDePartida}.
 *
 * - Tablero temático (Summoner's Rift) en perspectiva, dibujado por {@link VistaTablero}
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
    private static final java.awt.Color COLOR_JAQUE = Tema.ROJO_JAQUE;

    // -----------------------------------------------------------------------
    // Estado
    // -----------------------------------------------------------------------
    private final SesionDePartida sesion;
    private final PartidaAjedrez partida;
    private final MotorAjedrez motorIA;          // null en modo 2 jugadores
    private final PantallaMenu menuOrigen;
    private final boolean tableroInvertido;      // las negras abajo si el jugador eligió negras

    private final VistaTablero vistaTablero;
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
        BufferedImage img = Tema.cargarImagen(nombreArchivo);
        if (img != null) cache.put(tipo, img);
    }

    // -----------------------------------------------------------------------
    // Componentes UI
    // -----------------------------------------------------------------------
    private final JLabel labelEstado = new JLabel("", SwingConstants.CENTER);
    private final CartelJaque cartelJaque = new CartelJaque();
    private final JButton btnDeshacer = new BotonTematico("Deshacer");
    private final JButton btnRehacer = new BotonTematico("Rehacer");

    // Panel de perfil (barra de nivel)
    private JLabel lblNivel;
    private BarraXP barraExp;
    private AvatarJugador avatarPerfil;

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
        getContentPane().setBackground(Tema.FONDO_OSCURO);

        vistaTablero = new VistaTablero(tableroInvertido);
        vistaTablero.setAlClickear(this::onCasillaClick);
        add(construirPanelSuperior(), BorderLayout.NORTH);
        add(vistaTablero, BorderLayout.CENTER);
        add(construirPanelInferior(), BorderLayout.SOUTH);
        configurarAtajosDeTeclado();

        // El brillo del rey en jaque late mientras dure el jaque
        timerPulsoJaque = new Timer(40, e -> {
            float fase = (float) ((System.currentTimeMillis() - inicioPulso) / 1000.0 * Math.PI * 2 * 0.9);
            float pulso = 0.5f + 0.5f * (float) Math.sin(fase);
            vistaTablero.pulso = pulso;
            vistaTablero.repintarCasilla(vistaTablero.reyEnJaque); // solo la casilla del rey
            cartelJaque.setPulso(pulso);
        });

        actualizarVista();
        SwingUtilities.invokeLater(this::jugarTurnoIASiCorresponde);
    }

    // -----------------------------------------------------------------------
    // Panel superior: estado del turno, jaque y perfil
    // -----------------------------------------------------------------------
    private JPanel construirPanelSuperior() {
        JPanel panel = new BarraTematica(new BorderLayout(), true);
        panel.setBorder(new EmptyBorder(8, 14, 8, 14));

        JPanel panelJuego = new JPanel();
        panelJuego.setLayout(new BoxLayout(panelJuego, BoxLayout.Y_AXIS));
        panelJuego.setOpaque(false);

        labelEstado.setFont(Tema.tituloEspaciado(20f, 0.04f));
        labelEstado.setForeground(Tema.TEXTO);
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
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Tema.suavizar(g2);
                Tema.pintarMarco(g2, 0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new BorderLayout(10, 0));
        panel.setBorder(new EmptyBorder(8, 12, 9, 16));

        JPanel columna = new JPanel();
        columna.setOpaque(false);
        columna.setLayout(new BoxLayout(columna, BoxLayout.Y_AXIS));
        panel.add(columna, BorderLayout.CENTER);

        PerfilJugador perfil = sesion.getPerfil();
        if (perfil != null) {
            avatarPerfil = new AvatarJugador(48);
            panel.add(avatarPerfil, BorderLayout.WEST);

            JLabel lblNombrePerfil = new JLabel(perfil.getNombre());
            lblNombrePerfil.setFont(Tema.titulo(15f));
            lblNombrePerfil.setForeground(Tema.DORADO);
            lblNombrePerfil.setAlignmentX(Component.RIGHT_ALIGNMENT);

            lblNivel = new JLabel();
            lblNivel.setFont(Tema.texto(Font.PLAIN, 11f));
            lblNivel.setForeground(Tema.TEXTO_SUAVE);
            lblNivel.setAlignmentX(Component.RIGHT_ALIGNMENT);

            barraExp = new BarraXP(16);
            barraExp.setPreferredSize(new Dimension(190, 16));
            barraExp.setMaximumSize(new Dimension(220, 16));
            barraExp.setAlignmentX(Component.RIGHT_ALIGNMENT);

            columna.add(lblNombrePerfil);
            columna.add(Box.createVerticalStrut(2));
            columna.add(lblNivel);
            columna.add(Box.createVerticalStrut(4));
            columna.add(barraExp);
            actualizarBarraPerfil();
        } else {
            JLabel sinPerfil = new JLabel("INVITADO");
            sinPerfil.setFont(Tema.tituloEspaciado(13f, 0.12f));
            sinPerfil.setForeground(Tema.TEXTO_SUAVE);
            columna.add(sinPerfil);
        }
        return panel;
    }

    /** Sacude el tablero de lado a lado (se usa al dar jaque). */
    private void sacudirTablero() {
        long inicio = System.currentTimeMillis();
        Timer t = new Timer(16, null);
        t.addActionListener(e -> {
            float p = (System.currentTimeMillis() - inicio) / 380f;
            if (p >= 1f) {
                vistaTablero.desplazamientoX = 0;
                t.stop();
            } else {
                vistaTablero.desplazamientoX = (int) Math.round(Math.sin(p * Math.PI * 7) * 9 * (1 - p));
            }
            vistaTablero.repaint();
        });
        t.start();
    }

    // -----------------------------------------------------------------------
    // Panel inferior: controles
    // -----------------------------------------------------------------------
    private JPanel construirPanelInferior() {
        JPanel panel = new BarraTematica(new FlowLayout(FlowLayout.CENTER, 12, 10), false);
        panel.setBorder(new EmptyBorder(2, 0, 4, 0));

        btnDeshacer.addActionListener(e -> {
            if (iaPensando) return;
            if (sesion.deshacer()) {
                limpiarSeleccion();
                ultimaJugada = null;
                actualizarBarraPerfil();
                actualizarVista();
            }
        });

        btnRehacer.addActionListener(e -> {
            if (iaPensando) return;
            if (sesion.rehacer()) {
                limpiarSeleccion();
                ultimaJugada = null;
                actualizarBarraPerfil();
                actualizarVista();
            }
        });

        JButton btnNuevaPartida = new BotonTematico("Nueva Partida");
        btnNuevaPartida.addActionListener(e -> reiniciarPartida());

        btnPantallaCompleta = new BotonTematico("Pantalla completa");
        btnPantallaCompleta.addActionListener(e -> alternarPantallaCompleta());

        JButton btnMenu = new BotonTematico("Menú");
        btnMenu.addActionListener(e -> onSalir());

        panel.add(btnDeshacer);
        panel.add(btnRehacer);
        panel.add(btnNuevaPartida);
        panel.add(btnPantallaCompleta);
        panel.add(btnMenu);
        return panel;
    }

    /** Barra superior/inferior: azul noche con una línea dorada hacia el tablero. */
    private static class BarraTematica extends JPanel {
        private final boolean lineaAbajo;

        BarraTematica(LayoutManager layout, boolean lineaAbajo) {
            super(layout);
            this.lineaAbajo = lineaAbajo;
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth(), h = getHeight();
            java.awt.Color claro = new java.awt.Color(16, 26, 40), oscuro = Tema.FONDO_OSCURO;
            g2.setPaint(new GradientPaint(0, 0, lineaAbajo ? oscuro : claro, 0, h, lineaAbajo ? claro : oscuro));
            g2.fillRect(0, 0, w, h);
            Tema.suavizar(g2);
            Tema.pintarSeparador(g2, 0, lineaAbajo ? h - 1 : 0, w, null);
            g2.dispose();
        }
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
        TipoPieza[] tipos = {TipoPieza.REINA, TipoPieza.TORRE, TipoPieza.ALFIL, TipoPieza.CABALLO};
        Map<TipoPieza, BufferedImage> imagenes = color == Color.BLANCO ? imagenesBlancas : imagenesNegras;
        String[] nombres = new String[tipos.length];
        BufferedImage[] imgs = new BufferedImage[tipos.length];
        for (int i = 0; i < tipos.length; i++) {
            nombres[i] = tipos[i].getNombre();
            imgs[i] = imagenes.get(tipos[i]);
        }
        // Esc o cerrar sin elegir corona Reina (opción por defecto)
        int sel = Dialogos.elegirConImagenes(this, "Coronación",
                "¡Tu peón llegó a la última fila! Elegí la pieza para coronar:", nombres, imgs);
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
                    Dialogos.mensaje(VentanaAjedrez.this, "Error de la IA",
                            "La IA no pudo jugar:\n" + causa.getMessage(), Dialogos.Tipo.ERROR);
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

        VistaTablero v = vistaTablero;
        for (int fila = 0; fila < 8; fila++) {
            for (int col = 0; col < 8; col++) {
                Posicion pos = new Posicion(fila, col);
                Pieza pieza = tablero.obtenerPieza(pos);

                v.imagenPieza[fila][col] = null;
                v.simboloPieza[fila][col] = null;
                if (pieza != null) {
                    BufferedImage imgPieza = (pieza.getColor() == Color.NEGRO)
                            ? imagenesNegras.get(pieza.getTipo())
                            : imagenesBlancas.get(pieza.getTipo());
                    if (imgPieza != null) {
                        v.imagenPieza[fila][col] = imgPieza;
                    } else {
                        v.simboloPieza[fila][col] = pieza.getSimboloUnicode();
                        v.colorSimbolo[fila][col] = pieza.getColor() == Color.BLANCO
                                ? java.awt.Color.WHITE : new java.awt.Color(20, 20, 20);
                    }
                }

                v.ultimaJugada[fila][col] = ultimaJugada != null
                        && (ultimaJugada.getOrigen().equals(pos) || ultimaJugada.getDestino().equals(pos));
                v.destino[fila][col] = destinosLegalesActuales.contains(pos);
                v.captura[fila][col] = v.destino[fila][col] && pieza != null;
            }
        }
        v.seleccionada = casillaSeleccionada;
        v.reyEnJaque = reyEnJaque;
        v.esMate = esMate;
        v.invalidar();

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
        sacudirTablero();
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
                vistaTablero.escalaMarca = (float) (1 + 2.2 * Math.pow(p - 1, 3) + 1.2 * Math.pow(p - 1, 2));
                vistaTablero.repintarDestinos();
                if (p >= 1f) ((Timer) e.getSource()).stop();
            });
        }
        vistaTablero.escalaMarca = 0f;
        timerMarcas.restart();
    }

    // -----------------------------------------------------------------------
    // Perfil y animaciones de XP
    // -----------------------------------------------------------------------
    private void actualizarBarraPerfil() {
        if (sesion.getPerfil() == null || barraExp == null) return;
        if (timerBarra != null) timerBarra.stop();
        barraExp.setValor((int) (sesion.getPerfil().getProgresoBarra() * 100));
        actualizarTextosPerfil();
    }

    private void actualizarTextosPerfil() {
        PerfilJugador perfil = sesion.getPerfil();
        barraExp.setTexto(perfil.getPuntosEnNivelActual() + "/" + perfil.getPuntosParaSiguienteNivel() + " XP");
        lblNivel.setText("Nivel " + perfil.getNivel());
        if (avatarPerfil != null) avatarPerfil.setDatos(perfil.getFoto(), perfil.getNivel());
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
            int actual = barraExp.getValor();
            if (actual == destino[0]) {
                if (faltaSegundoTramo[0]) {
                    faltaSegundoTramo[0] = false;
                    barraExp.setValor(0);
                    destino[0] = objetivo;
                } else {
                    ((Timer) e.getSource()).stop();
                }
                return;
            }
            int diferencia = destino[0] - actual;
            int paso = Math.max(1, Math.abs(diferencia) / 8);
            barraExp.setValor(actual + Integer.signum(diferencia) * paso);
        });
        timerBarra.start();
    }

    private Point centroDeCasilla(Posicion pos) {
        return SwingUtilities.convertPoint(vistaTablero, vistaTablero.centroDeCasilla(pos), capaXP);
    }

    private Point centroDelTablero() {
        return SwingUtilities.convertPoint(vistaTablero, vistaTablero.centroDelTablero(), capaXP);
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
                Dialogos.mensaje(this, gane ? "Victoria" : "Fin de partida", mensaje, Dialogos.Tipo.INFO));
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
        if (!Dialogos.confirmar(this, "Salir de la partida",
                "No se guardará el progreso de esta partida.\n¿Seguro que querés salir al menú principal?",
                Dialogos.Tipo.ADVERTENCIA, "Salir", "Seguir jugando")) return;

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
}

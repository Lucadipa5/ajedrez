package ajedrez.tests;

import ajedrez.nucleo.comandos.Comando;
import ajedrez.nucleo.comandos.MoverPiezaComando;
import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Pieza;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.dominio.TipoPieza;
import ajedrez.nucleo.estados.EstadoPartida;
import ajedrez.nucleo.estados.EstadoTurnoBlanco;
import ajedrez.nucleo.estados.EstadoTurnoNegro;
import ajedrez.nucleo.fabrica.FabricaPiezas;
import ajedrez.nucleo.reglas.DetectorJaque;
import ajedrez.nucleo.reglas.ValidadorMovimiento;
import ajedrez.nucleo.servicio.PartidaAjedrez;
import ajedrez.nucleo.dominio.Jugada;
import ajedrez.nucleo.ia.Dificultad;
import ajedrez.nucleo.ia.MotorAjedrez;
import ajedrez.nucleo.perfil.PerfilJugador;
import ajedrez.nucleo.perfil.RepositorioPerfiles;
import ajedrez.nucleo.perfil.ServicioCuentas;
import ajedrez.nucleo.servicio.SesionDePartida;
import ajedrez.nucleo.tablero.NotacionFEN;
import ajedrez.nucleo.tablero.Tablero;

import java.util.ArrayList;
import java.util.List;

/**
 * Suite de Pruebas Unitarias del Núcleo sin infraestructura externa.
 * Valida de forma aislada e independiente cada componente de arquitectura y diseño,
 * incluyendo los casos críticos de coronación y deshacer transaccional (Undo).
 */
public class TestRunner {
    private static int testsPasados = 0;
    private static int testsTotales = 0;

    public static void main(String[] args) {
        System.out.println("=======================================================");
        System.out.println("  EJECUTANDO TESTS UNITARIOS DEL NÚCLEO DE AJEDREZ");
        System.out.println("=======================================================");

        testTableroInstanciableYClonable();
        testCommandMoverYDeshacerRestauraPieza();
        testStateTransicionDeTurnos();
        testComposicionSobreHerenciaReina();
        testDeteccionDeJaque();
        testValidadorImpideMovimientosInvalidos();
        testValidadorImpideAutoJaque();
        testPromocionAEleccion();
        testPromocionUndoRestauraPeonOriginal();
        testPromocionConCapturaYUndoRestauraAmbas();
        testPartidaFlujoCompletoConUndo();
        testNotacionFENPosicionInicial();
        testJugadaDesdeUCI();
        testServicioCuentas();
        testFotoDePerfil();
        testXPSoloParaElColorDelJugador();
        testDeshacerContraIADeshaceAmbasJugadas();

        System.out.println("=======================================================");
        System.out.println(" RESULTADO: " + testsPasados + "/" + testsTotales + " tests pasaron con éxito.");
        System.out.println("=======================================================");
    }

    private static void afirmar(boolean condicion, String nombreTest) {
        testsTotales++;
        if (condicion) {
            testsPasados++;
            System.out.println("  [PASS] " + nombreTest);
        } else {
            System.err.println("  [FAIL] " + nombreTest);
            throw new AssertionError("Fallo en: " + nombreTest);
        }
    }

    private static void testTableroInstanciableYClonable() {
        Tablero t1 = new Tablero();
        FabricaPiezas fabrica = new FabricaPiezas();
        Pieza torre = fabrica.crearTorre(Color.BLANCO);
        Posicion pos = new Posicion(0, 0);

        t1.colocarPieza(torre, pos);
        afirmar(t1.estaOcupada(pos), "Tablero coloca pieza en casilla");
        afirmar(t1.obtenerPieza(pos).getTipo() == TipoPieza.TORRE, "Pieza colocada es de tipo Torre");

        Tablero clon = t1.clonar();
        afirmar(clon.estaOcupada(pos), "Clon del tablero conserva las casillas ocupadas");
        clon.moverPieza(pos, new Posicion(0, 5));
        afirmar(t1.estaOcupada(pos), "Modificar el clon NO afecta el tablero original (independencia)");
    }

    private static void testCommandMoverYDeshacerRestauraPieza() {
        Tablero tablero = new Tablero();
        FabricaPiezas fabrica = new FabricaPiezas();
        Posicion origen = new Posicion(1, 0);
        Posicion destino = new Posicion(2, 0);

        Pieza peonBlanco = fabrica.crearPeon(Color.BLANCO);
        Pieza caballoNegro = fabrica.crearCaballo(Color.NEGRO);

        tablero.colocarPieza(peonBlanco, origen);
        tablero.colocarPieza(caballoNegro, destino);

        Tablero estadoAntes = tablero.clonar();

        // Arrange
        Comando comando = new MoverPiezaComando(tablero, origen, destino);

        // Act 1: Ejecutar
        comando.ejecutar();
        afirmar(!tablero.estaOcupada(origen), "Command ejecutar: casilla origen queda vacía");
        afirmar(tablero.obtenerPieza(destino) == peonBlanco, "Command ejecutar: peón ocupa casilla destino");

        // Act 2: Deshacer (Undo)
        comando.deshacer();
        afirmar(tablero.obtenerPieza(origen) == peonBlanco, "Command undo: peón vuelve a su casilla origen");
        afirmar(tablero.obtenerPieza(destino) == caballoNegro, "Command undo: caballo capturado fue restaurado");
        afirmar(tablero.equals(estadoAntes), "Command undo: el tablero quedó idéntico al estado previo");
    }

    private static void testStateTransicionDeTurnos() {
        EstadoPartida estado = new EstadoTurnoBlanco();
        afirmar(estado.getColorTurnoActual() == Color.BLANCO, "Estado inicial es Turno de Blancas");

        EstadoPartida siguiente = estado.siguienteTurno();
        afirmar(siguiente instanceof EstadoTurnoNegro, "Transición siguienteTurno pasa a Turno de Negras");
        afirmar(siguiente.getColorTurnoActual() == Color.NEGRO, "Color actual en siguiente turno es Negro");

        EstadoPartida revertido = siguiente.turnoAnterior();
        afirmar(revertido instanceof EstadoTurnoBlanco, "Transición turnoAnterior revierte al turno de Blancas");
    }

    private static void testComposicionSobreHerenciaReina() {
        Tablero tablero = new Tablero();
        FabricaPiezas fabrica = new FabricaPiezas();
        Pieza reina = fabrica.crearReina(Color.BLANCO);
        Posicion centro = new Posicion(4, 4);
        tablero.colocarPieza(reina, centro);

        List<Posicion> destinos = reina.calcularMovimientos(centro, tablero);

        afirmar(destinos.contains(new Posicion(4, 0)), "Reina compuesta se mueve ortogonalmente a la izquierda");
        afirmar(destinos.contains(new Posicion(0, 4)), "Reina compuesta se mueve ortogonalmente hacia arriba");
        afirmar(destinos.contains(new Posicion(0, 0)), "Reina compuesta se mueve diagonalmente superior izquierda");
        afirmar(destinos.contains(new Posicion(7, 7)), "Reina compuesta se mueve diagonalmente inferior derecha");
    }

    private static void testDeteccionDeJaque() {
        Tablero tablero = new Tablero();
        FabricaPiezas fabrica = new FabricaPiezas();
        DetectorJaque detector = new DetectorJaque();

        Posicion posRey = new Posicion(7, 4);
        Posicion posTorreEnemiga = new Posicion(0, 4);

        tablero.colocarPieza(fabrica.crearRey(Color.BLANCO), posRey);
        tablero.colocarPieza(fabrica.crearTorre(Color.NEGRO), posTorreEnemiga);

        afirmar(detector.estaEnJaque(tablero, Color.BLANCO), "Detector identifica Jaque directo de torre en misma columna");

        tablero.colocarPieza(fabrica.crearAlfil(Color.BLANCO), new Posicion(3, 4));
        afirmar(!detector.estaEnJaque(tablero, Color.BLANCO), "Jaque neutralizado al interponer pieza aliada");
    }

    private static void testValidadorImpideMovimientosInvalidos() {
        Tablero tablero = new Tablero();
        FabricaPiezas fabrica = new FabricaPiezas();
        ValidadorMovimiento validador = new ValidadorMovimiento();

        Posicion posTorre = new Posicion(7, 0);
        tablero.colocarPieza(fabrica.crearTorre(Color.BLANCO), posTorre);

        boolean legal = validador.esMovimientoLegal(tablero, posTorre, new Posicion(6, 1), Color.BLANCO);
        afirmar(!legal, "Validador rechaza movimiento diagonal para la Torre");

        boolean legalOrtogonal = validador.esMovimientoLegal(tablero, posTorre, new Posicion(4, 0), Color.BLANCO);
        afirmar(legalOrtogonal, "Validador acepta movimiento ortogonal despejado para la Torre");
    }

    private static void testValidadorImpideAutoJaque() {
        Tablero tablero = new Tablero();
        FabricaPiezas fabrica = new FabricaPiezas();
        ValidadorMovimiento validador = new ValidadorMovimiento();

        Posicion posRey = new Posicion(7, 4);
        Posicion posEscudo = new Posicion(6, 4);
        Posicion posTorreEnemiga = new Posicion(0, 4);

        tablero.colocarPieza(fabrica.crearRey(Color.BLANCO), posRey);
        tablero.colocarPieza(fabrica.crearAlfil(Color.BLANCO), posEscudo);
        tablero.colocarPieza(fabrica.crearTorre(Color.NEGRO), posTorreEnemiga);

        boolean puedeDesclavar = validador.esMovimientoLegal(tablero, posEscudo, new Posicion(5, 5), Color.BLANCO);
        afirmar(!puedeDesclavar, "Validador prohíbe mover pieza clavada que expondría al Rey a jaque (auto-jaque)");
    }

    private static void testPromocionAEleccion() {
        // Configuramos para que elija coronar en Caballo
        PartidaAjedrez partida = new PartidaAjedrez(color -> TipoPieza.CABALLO);
        Tablero tablero = partida.getTablero();
        tablero.limpiar();

        FabricaPiezas fabrica = new FabricaPiezas();
        tablero.colocarPieza(fabrica.crearRey(Color.BLANCO), new Posicion(7, 4));
        tablero.colocarPieza(fabrica.crearRey(Color.NEGRO), new Posicion(0, 4));

        Posicion peonCasiCoronado = new Posicion(1, 1);
        tablero.colocarPieza(fabrica.crearPeon(Color.BLANCO), peonCasiCoronado);

        boolean movio = partida.intentarMover(peonCasiCoronado, new Posicion(0, 1));
        afirmar(movio, "Peón avanza a la última fila con éxito");

        Pieza coronada = tablero.obtenerPieza(new Posicion(0, 1));
        afirmar(coronada.getTipo() == TipoPieza.CABALLO, "Peón se coronó en la pieza elegida (Caballo)");
    }

    private static void testPromocionUndoRestauraPeonOriginal() {
        PartidaAjedrez partida = new PartidaAjedrez(color -> TipoPieza.REINA);
        Tablero tablero = partida.getTablero();
        tablero.limpiar();

        FabricaPiezas fabrica = new FabricaPiezas();
        tablero.colocarPieza(fabrica.crearRey(Color.BLANCO), new Posicion(7, 4));
        tablero.colocarPieza(fabrica.crearRey(Color.NEGRO), new Posicion(0, 4));

        Posicion origen = new Posicion(1, 2);
        Posicion destino = new Posicion(0, 2);
        Pieza peonOriginal = fabrica.crearPeon(Color.BLANCO);
        tablero.colocarPieza(peonOriginal, origen);

        // Coronar a Reina
        partida.intentarMover(origen, destino);
        afirmar(tablero.obtenerPieza(destino).getTipo() == TipoPieza.REINA, "Peón se convirtió en Reina");

        // Deshacer coronación (Undo)
        boolean deshizo = partida.deshacerMovimiento();
        afirmar(deshizo, "Undo de coronación fue exitoso");

        // Casilla destino debe quedar vacía
        afirmar(!tablero.estaOcupada(destino), "Casilla de coronación (destino) vuelve a estar vacía tras Undo");

        // Casilla origen debe volver a tener el PEÓN original (NO una Reina)
        Pieza restaurada = tablero.obtenerPieza(origen);
        afirmar(restaurada != null && restaurada.getTipo() == TipoPieza.PEON, "Casilla origen recuperó el PEÓN original (no se quedó como Reina)");
        afirmar(partida.getEstadoActual().getColorTurnoActual() == Color.BLANCO, "Turno regresó al jugador blanco tras Undo");
    }

    private static void testPromocionConCapturaYUndoRestauraAmbas() {
        PartidaAjedrez partida = new PartidaAjedrez(color -> TipoPieza.TORRE);
        Tablero tablero = partida.getTablero();
        tablero.limpiar();

        FabricaPiezas fabrica = new FabricaPiezas();
        tablero.colocarPieza(fabrica.crearRey(Color.BLANCO), new Posicion(7, 4));
        tablero.colocarPieza(fabrica.crearRey(Color.NEGRO), new Posicion(0, 7));

        Posicion origenPeon = new Posicion(1, 1);
        Posicion destinoTorre = new Posicion(0, 2);
        Pieza peonBlanco = fabrica.crearPeon(Color.BLANCO);
        Pieza torreNegra = fabrica.crearTorre(Color.NEGRO);

        tablero.colocarPieza(peonBlanco, origenPeon);
        tablero.colocarPieza(torreNegra, destinoTorre);

        // Captura en diagonal y coronación a Torre
        boolean capturoYCorono = partida.intentarMover(origenPeon, destinoTorre);
        afirmar(capturoYCorono, "Peón captura en diagonal y corona en la fila 0");
        afirmar(tablero.obtenerPieza(destinoTorre).getTipo() == TipoPieza.TORRE, "Pieza coronada en destino es Torre");
        afirmar(tablero.obtenerPieza(destinoTorre).getColor() == Color.BLANCO, "Torre en destino pertenece a las Blancas");

        // Deshacer
        partida.deshacerMovimiento();
        afirmar(tablero.obtenerPieza(origenPeon).getTipo() == TipoPieza.PEON, "Undo restaura el PEÓN en su origen");
        afirmar(tablero.obtenerPieza(destinoTorre).getTipo() == TipoPieza.TORRE, "Undo restaura la TORRE capturada en su destino");
        afirmar(tablero.obtenerPieza(destinoTorre).getColor() == Color.NEGRO, "Torre restaurada pertenece al rival (Negras)");
    }

    private static void testPartidaFlujoCompletoConUndo() {
        PartidaAjedrez partida = new PartidaAjedrez();
        Posicion peonE2 = new Posicion(6, 4);
        Posicion peonE4 = new Posicion(4, 4);

        afirmar(partida.getEstadoActual().getColorTurnoActual() == Color.BLANCO, "Inicio: Turno de Blancas");
        boolean exito1 = partida.intentarMover(peonE2, peonE4);
        afirmar(exito1, "Blancas mueven peón de e2 a e4");
        afirmar(partida.getEstadoActual().getColorTurnoActual() == Color.NEGRO, "Turno cambia a Negras");

        boolean deshizo = partida.deshacerMovimiento();
        afirmar(deshizo, "Deshacer movimiento fue exitoso");
        afirmar(partida.getEstadoActual().getColorTurnoActual() == Color.BLANCO, "Deshacer retorna el turno a Blancas");
        afirmar(partida.getTablero().estaOcupada(peonE2), "Peón regresó a e2");
        afirmar(!partida.getTablero().estaOcupada(peonE4), "Casilla e4 vuelve a estar vacía");
    }

    // -----------------------------------------------------------------------
    // Notación FEN / UCI (usadas por el adaptador de IA)
    // -----------------------------------------------------------------------
    private static void testNotacionFENPosicionInicial() {
        PartidaAjedrez partida = new PartidaAjedrez();
        String fen = NotacionFEN.desdeTablero(partida.getTablero(), Color.BLANCO);
        afirmar(fen.equals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1"),
                "FEN de la posición inicial es correcta");
    }

    private static void testJugadaDesdeUCI() {
        Jugada j = Jugada.desdeUCI("e2e4");
        afirmar(j != null && j.getOrigen().equals(new Posicion(6, 4)) && j.getDestino().equals(new Posicion(4, 4)),
                "UCI e2e4 se traduce a (6,4) -> (4,4)");
        Jugada corona = Jugada.desdeUCI("a7a8q");
        afirmar(corona != null && corona.getPromocion() == TipoPieza.REINA, "UCI a7a8q corona Reina");
        afirmar(Jugada.desdeUCI("z9z9") == null, "UCI inválida devuelve null");
    }

    // -----------------------------------------------------------------------
    // Cuentas y XP (con un repositorio en memoria, sin archivos)
    // -----------------------------------------------------------------------
    private static class RepositorioEnMemoria implements RepositorioPerfiles {
        private List<PerfilJugador> perfiles = new ArrayList<>();
        @Override public List<PerfilJugador> cargarTodos() {
            List<PerfilJugador> copia = new ArrayList<>();
            for (PerfilJugador p : perfiles) copia.add(p.copiar());
            return copia;
        }
        @Override public void guardarTodos(List<PerfilJugador> lista) {
            perfiles = new ArrayList<>();
            for (PerfilJugador p : lista) perfiles.add(p.copiar());
        }
    }

    private static void testServicioCuentas() {
        ServicioCuentas cuentas = new ServicioCuentas(new RepositorioEnMemoria());
        afirmar(cuentas.registrar("ana", "clave".toCharArray()) != null, "Registro de cuenta nueva");
        afirmar(cuentas.registrar("ANA", "otra".toCharArray()) == null, "No se puede registrar un usuario repetido");
        afirmar(cuentas.iniciarSesion("ana", "clave".toCharArray()) != null, "Inicio de sesión con contraseña correcta");
        afirmar(cuentas.iniciarSesion("ana", "mala".toCharArray()) == null, "Inicio de sesión rechaza contraseña incorrecta");
    }

    private static void testFotoDePerfil() {
        ServicioCuentas cuentas = new ServicioCuentas(new RepositorioEnMemoria());
        PerfilJugador perfil = cuentas.registrar("lucia", "1234".toCharArray());
        byte[] foto = {1, 2, 3, 4};
        cuentas.cambiarFoto(perfil, foto);
        PerfilJugador recargado = cuentas.iniciarSesion("lucia", "1234".toCharArray());
        afirmar(java.util.Arrays.equals(recargado.getFoto(), foto), "La foto de perfil se guarda con la cuenta");
        cuentas.cambiarFoto(recargado, null);
        afirmar(cuentas.iniciarSesion("lucia", "1234".toCharArray()).getFoto() == null, "La foto de perfil se puede quitar");
        boolean rechazada = false;
        try {
            perfil.setFoto(new byte[PerfilJugador.TAMANO_MAXIMO_FOTO + 1]);
        } catch (IllegalArgumentException e) {
            rechazada = true;
        }
        afirmar(rechazada, "Una foto demasiado grande se rechaza");
    }

    private static void testXPSoloParaElColorDelJugador() {
        ServicioCuentas cuentas = new ServicioCuentas(new RepositorioEnMemoria());
        PerfilJugador perfil = cuentas.registrar("juan", "1234".toCharArray());
        // El jugador eligió Negras: las capturas de las Blancas no le dan XP
        SesionDePartida sesion = new SesionDePartida(new PartidaAjedrez(), Color.NEGRO, perfil, cuentas, null, null);
        sesion.mover(Jugada.desdeUCI("e2e4"));
        sesion.mover(Jugada.desdeUCI("d7d5"));
        SesionDePartida.ResultadoJugada blancasComen = sesion.mover(Jugada.desdeUCI("e4d5"));
        afirmar(blancasComen.eventosXP().isEmpty(), "Captura del rival no da XP");
        SesionDePartida.ResultadoJugada negrasComen = sesion.mover(Jugada.desdeUCI("d8d5"));
        afirmar(negrasComen.eventosXP().size() == 1 && sesion.getPerfil().getPuntosAcumulados() == PerfilJugador.PUNTOS_COMER_PIEZA,
                "Captura del jugador da XP");

        sesion.deshacer();
        afirmar(sesion.getPerfil().getPuntosAcumulados() == 0, "Deshacer quita la XP de la jugada");
        sesion.rehacer();
        afirmar(sesion.getPerfil().getPuntosAcumulados() == PerfilJugador.PUNTOS_COMER_PIEZA, "Rehacer la recupera sin duplicarla");
        sesion.deshacer();
        sesion.mover(Jugada.desdeUCI("d8d5"));
        afirmar(sesion.getPerfil().getPuntosAcumulados() == PerfilJugador.PUNTOS_COMER_PIEZA,
                "Deshacer y volver a capturar no suma XP dos veces");
    }

    private static void testDeshacerContraIADeshaceAmbasJugadas() {
        MotorAjedrez motorFalso = new MotorAjedrez() {
            @Override public boolean estaDisponible() { return true; }
            @Override public String motivoNoDisponible() { return null; }
            @Override public Jugada elegirJugada(Tablero t, Color turno, Dificultad d) { return Jugada.desdeUCI("e7e5"); }
            @Override public void cerrar() {}
        };
        SesionDePartida sesion = new SesionDePartida(new PartidaAjedrez(), Color.BLANCO, null, null, motorFalso, Dificultad.MEDIA);
        sesion.mover(Jugada.desdeUCI("e2e4"));
        afirmar(sesion.esTurnoDeLaIA(), "Después de mover el jugador, es turno de la IA");
        try {
            sesion.mover(sesion.calcularJugadaIA());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        afirmar(!sesion.esTurnoDeLaIA(), "La IA jugó y vuelve a ser turno del jugador");
        sesion.deshacer();
        afirmar(sesion.getPartida().getTablero().estaOcupada(new Posicion(6, 4))
                        && sesion.getPartida().getEstadoActual().getColorTurnoActual() == Color.BLANCO,
                "Deshacer contra la IA vuelve a antes de la jugada del jugador");
    }
}

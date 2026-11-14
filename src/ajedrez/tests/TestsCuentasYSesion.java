package ajedrez.tests;

import ajedrez.nucleo.dominio.Color;
import ajedrez.nucleo.dominio.Jugada;
import ajedrez.nucleo.dominio.Posicion;
import ajedrez.nucleo.ia.ConfiguracionIA;
import ajedrez.nucleo.ia.DecisionIA;
import ajedrez.nucleo.ia.Dificultad;
import ajedrez.nucleo.ia.MotorAjedrez;
import ajedrez.nucleo.ia.MotorNoDisponibleException;
import ajedrez.nucleo.perfil.PerfilJugador;
import ajedrez.nucleo.perfil.RepositorioPerfiles;
import ajedrez.nucleo.perfil.ServicioCuentas;
import ajedrez.nucleo.servicio.PartidaAjedrez;
import ajedrez.nucleo.servicio.SesionDePartida;
import ajedrez.nucleo.tablero.Tablero;

import java.util.ArrayList;
import java.util.List;

import static ajedrez.tests.Afirmar.p;
import static ajedrez.tests.Afirmar.que;

/**
 * Cuentas, XP y partida contra la IA. Los puertos de salida se reemplazan por dobles de prueba:
 * un fake (repositorio en memoria), stubs (motores que devuelven algo fijo) y un mock (registro que anota).
 */
final class TestsCuentasYSesion {

    static void correr() {
        Afirmar.seccion("Cuentas y sesión");
        servicioCuentas();
        fotoDePerfil();
        xpSoloParaElColorDelJugador();
        iaJuegaYSeDeshacenAmbasJugadas();
        iaIlegalUsaRespaldoYQuedaRegistrada();
        iaCaidaNoCortaLaPartida();
    }

    /** FAKE: funciona de verdad, pero en memoria en vez de en un archivo. */
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

    /** STUB: un motor que siempre propone la misma jugada. */
    private static MotorAjedrez motorFijo(String uci) {
        return new MotorAjedrez() {
            @Override public boolean estaDisponible() { return true; }
            @Override public String motivoNoDisponible() { return null; }
            @Override public Jugada elegirJugada(Tablero t, Color turno, Dificultad d) { return Jugada.desdeUCI(uci); }
            @Override public void cerrar() {}
        };
    }

    /** MOCK: guarda las decisiones que le registraron para verificarlas en el Assert. */
    private static class RegistroMock implements ajedrez.nucleo.ia.RegistroDecisionesIA {
        final List<DecisionIA> decisiones = new ArrayList<>();
        @Override public void registrar(DecisionIA d) { decisiones.add(d); }
    }

    private static void servicioCuentas() {
        // Arrange
        ServicioCuentas cuentas = new ServicioCuentas(new RepositorioEnMemoria());

        // Act / Assert
        que(cuentas.registrar("ana", "clave".toCharArray()) != null, "Registro de cuenta nueva");
        que(cuentas.registrar("ANA", "otra".toCharArray()) == null, "No se puede registrar un usuario repetido");
        que(cuentas.iniciarSesion("ana", "clave".toCharArray()) != null, "Inicio de sesión con contraseña correcta");
        que(cuentas.iniciarSesion("ana", "mala".toCharArray()) == null, "Inicio de sesión rechaza contraseña incorrecta");
    }

    private static void fotoDePerfil() {
        // Arrange
        ServicioCuentas cuentas = new ServicioCuentas(new RepositorioEnMemoria());
        PerfilJugador perfil = cuentas.registrar("lucia", "1234".toCharArray());
        byte[] foto = {1, 2, 3, 4};

        // Act
        cuentas.cambiarFoto(perfil, foto);
        PerfilJugador recargado = cuentas.iniciarSesion("lucia", "1234".toCharArray());

        // Assert
        que(java.util.Arrays.equals(recargado.getFoto(), foto), "La foto de perfil se guarda con la cuenta");
        cuentas.cambiarFoto(recargado, null);
        que(cuentas.iniciarSesion("lucia", "1234".toCharArray()).getFoto() == null, "La foto de perfil se puede quitar");
        boolean rechazada = false;
        try {
            perfil.setFoto(new byte[PerfilJugador.TAMANO_MAXIMO_FOTO + 1]);
        } catch (IllegalArgumentException e) {
            rechazada = true;
        }
        que(rechazada, "Una foto demasiado grande se rechaza");
    }

    private static void xpSoloParaElColorDelJugador() {
        // Arrange: el jugador eligió Negras; las capturas de las Blancas no le dan XP
        ServicioCuentas cuentas = new ServicioCuentas(new RepositorioEnMemoria());
        PerfilJugador perfil = cuentas.registrar("juan", "1234".toCharArray());
        SesionDePartida sesion = new SesionDePartida(PartidaAjedrez.estandar(), Color.NEGRO, perfil, cuentas, null);
        sesion.mover(Jugada.desdeUCI("e2e4"));
        sesion.mover(Jugada.desdeUCI("d7d5"));

        // Act
        SesionDePartida.ResultadoJugada blancasComen = sesion.mover(Jugada.desdeUCI("e4d5"));
        SesionDePartida.ResultadoJugada negrasComen = sesion.mover(Jugada.desdeUCI("d8d5"));

        // Assert
        que(blancasComen.eventosXP().isEmpty(), "Captura del rival no da XP");
        que(negrasComen.eventosXP().size() == 1 && sesion.getPerfil().getPuntosAcumulados() == PerfilJugador.PUNTOS_COMER_PIEZA,
                "Captura del jugador da XP");
        sesion.deshacer();
        que(sesion.getPerfil().getPuntosAcumulados() == 0, "Deshacer quita la XP de la jugada");
        sesion.rehacer();
        que(sesion.getPerfil().getPuntosAcumulados() == PerfilJugador.PUNTOS_COMER_PIEZA, "Rehacer la recupera sin duplicarla");
        sesion.deshacer();
        sesion.mover(Jugada.desdeUCI("d8d5"));
        que(sesion.getPerfil().getPuntosAcumulados() == PerfilJugador.PUNTOS_COMER_PIEZA, "Deshacer y volver a capturar no suma XP dos veces");
    }

    private static void iaJuegaYSeDeshacenAmbasJugadas() throws AssertionError {
        // Arrange
        SesionDePartida sesion = new SesionDePartida(PartidaAjedrez.estandar(), Color.BLANCO, null, null,
                new ConfiguracionIA(motorFijo("e7e5"), Dificultad.MEDIA));
        sesion.mover(Jugada.desdeUCI("e2e4"));
        que(sesion.esTurnoDeLaIA(), "Después de mover el jugador, es turno de la IA");

        // Act
        DecisionIA decision;
        try {
            decision = sesion.calcularJugadaIA();
        } catch (MotorNoDisponibleException e) {
            throw new AssertionError(e);
        }
        sesion.mover(decision.jugada());

        // Assert
        que(!decision.usoRespaldo() && decision.jugada().getDestino().equals(p("e5")), "La sugerencia legal del motor se acepta tal cual");
        que(!sesion.esTurnoDeLaIA(), "La IA jugó y vuelve a ser turno del jugador");
        sesion.deshacer();
        que(sesion.getPartida().getTablero().estaOcupada(new Posicion(6, 4))
                        && sesion.getPartida().getEstadoActual().getColorTurnoActual() == Color.BLANCO,
                "Deshacer contra la IA vuelve a antes de la jugada del jugador");
    }

    private static void iaIlegalUsaRespaldoYQuedaRegistrada() {
        // Arrange: el motor propone una jugada ilegal (e2e5) para las negras
        RegistroMock registro = new RegistroMock();
        SesionDePartida sesion = new SesionDePartida(PartidaAjedrez.estandar(), Color.BLANCO, null, null,
                new ConfiguracionIA(motorFijo("e2e5"), Dificultad.FACIL, registro));
        sesion.mover(Jugada.desdeUCI("e2e4"));

        // Act
        DecisionIA decision;
        try {
            decision = sesion.calcularJugadaIA();
        } catch (MotorNoDisponibleException e) {
            throw new AssertionError(e);
        }

        // Assert
        que(decision.usoRespaldo() && decision.resultado().contains("ilegal"), "Una sugerencia ilegal jamás llega al tablero: se usa respaldo");
        que(sesion.mover(decision.jugada()) != null, "La jugada de respaldo es legal");
        que(registro.decisiones.size() == 1 && registro.decisiones.get(0) == decision, "Cada decisión de la IA queda registrada (auditoría)");
    }

    private static void iaCaidaNoCortaLaPartida() {
        // Arrange: un motor que falla siempre
        MotorAjedrez caido = new MotorAjedrez() {
            @Override public boolean estaDisponible() { return false; }
            @Override public String motivoNoDisponible() { return "simulado"; }
            @Override public Jugada elegirJugada(Tablero t, Color c, Dificultad d) throws MotorNoDisponibleException {
                throw new MotorNoDisponibleException("API caída (simulada)");
            }
            @Override public void cerrar() {}
        };
        SesionDePartida sesion = new SesionDePartida(PartidaAjedrez.estandar(), Color.BLANCO, null, null,
                new ConfiguracionIA(caido, Dificultad.DIFICIL));
        sesion.mover(Jugada.desdeUCI("d2d4"));

        // Act
        DecisionIA decision;
        try {
            decision = sesion.calcularJugadaIA();
        } catch (MotorNoDisponibleException e) {
            throw new AssertionError(e);
        }

        // Assert
        que(decision.usoRespaldo() && decision.resultado().startsWith("respaldo:error"), "Si el motor falla se juega una jugada de respaldo");
        que(sesion.mover(decision.jugada()) != null, "La partida continúa aunque la IA esté caída");
    }
}

package ajedrez.adaptadores.ui.swing;

import ajedrez.nucleo.ia.MotorAjedrez;
import ajedrez.nucleo.ia.RegistroDecisionesIA;
import ajedrez.nucleo.perfil.ServicioCuentas;
import ajedrez.nucleo.servicio.PartidaAjedrez;

import java.util.function.Supplier;

/**
 * Lo que la interfaz recibe armado desde la raíz de composición ({@code Main}):
 * el servicio de cuentas, cómo crear una partida y cómo crear el motor de IA.
 * La ventana no hace {@code new} de ninguna implementación concreta del núcleo ni de otros adaptadores.
 */
public record ServiciosDelJuego(
        ServicioCuentas cuentas,
        Supplier<PartidaAjedrez> fabricaPartidas,
        Supplier<MotorAjedrez> fabricaMotorIA,
        RegistroDecisionesIA registroIA) {
}

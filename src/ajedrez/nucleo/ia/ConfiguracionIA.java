package ajedrez.nucleo.ia;

import java.util.Objects;

/**
 * Todo lo que necesita una partida contra la IA: el motor, la dificultad y dónde registrar
 * sus decisiones. Agruparlo evita pasar tres parámetros sueltos a la sesión.
 */
public record ConfiguracionIA(MotorAjedrez motor, Dificultad dificultad, RegistroDecisionesIA registro) {

    public ConfiguracionIA {
        Objects.requireNonNull(motor, "El motor de IA no puede ser nulo");
        if (dificultad == null) dificultad = Dificultad.MEDIA;
        if (registro == null) registro = RegistroDecisionesIA.NULO;
    }

    public ConfiguracionIA(MotorAjedrez motor, Dificultad dificultad) {
        this(motor, dificultad, RegistroDecisionesIA.NULO);
    }
}

package com.juanp.custommobs.mob;

import java.util.Locale;

/**
 * Actitud de un mob de servidor ({@link MobCategory#SERVER}) frente a los jugadores.
 *
 * <p>No aplica a los mobs de jugador, que se rigen por
 * {@link com.juanp.custommobs.combat.PlayerTargetMode}.
 */
public enum Attitude {

    /** Ataca a los jugadores a la vista. */
    HOSTILE,
    /** No ataca salvo que lo agredan: devuelve el golpe y luego olvida. */
    NEUTRAL,
    /** Nunca ataca jugadores; persigue a quien los agreda o amenace. */
    DEFENDER;

    public static final Attitude DEFAULT = NEUTRAL;

    /** {@code null} solo si el texto no esta vacio y no es un valor valido. */
    public static Attitude parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        try {
            return valueOf(raw.toUpperCase(Locale.ROOT).trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

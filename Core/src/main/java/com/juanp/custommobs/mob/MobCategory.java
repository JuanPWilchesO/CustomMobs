package com.juanp.custommobs.mob;

import java.util.Locale;

/**
 * Categoria de un mob custom.
 *
 * <ul>
 *   <li>{@link #PLAYER}: invocado por un jugador. Tiene dueno, leash, receta de huevo
 *       y reglas PvP (Teams, defensa del dueno).</li>
 *   <li>{@link #SERVER}: mob de servidor. Sin dueno ni receta obligatoria. Se rige por
 *       facciones y por su actitud frente a los jugadores (PvE).</li>
 * </ul>
 */
public enum MobCategory {

    PLAYER,
    SERVER;

    public static final MobCategory DEFAULT = PLAYER;

    /** {@code null} solo si el texto no esta vacio y no es un valor valido. */
    public static MobCategory parse(String raw) {
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

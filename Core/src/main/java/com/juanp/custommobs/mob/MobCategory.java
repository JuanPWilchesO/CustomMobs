package com.juanp.custommobs.mob;

import java.util.Locale;

/**
 * Categoria de un mob custom.
 *
 * <ul>
 *   <li>{@link #PLAYER}: invocado por un jugador. Tiene dueno, leash, receta de huevo
 *       y reglas PvP (Teams, defensa del dueno).</li>
 *   <li>{@link #MOUNT}: montura. Como un mob de jugador —huevo, dueno, cupo— pero
 *       ademas preparada para montarse (domesticada, con silla y con dueno).</li>
 *   <li>{@link #SERVER}: mob de servidor. Sin dueno ni receta obligatoria. Se rige por
 *       facciones y por su actitud frente a los jugadores (PvE).</li>
 * </ul>
 */
public enum MobCategory {

    PLAYER,
    MOUNT,
    SERVER;

    public static final MobCategory DEFAULT = PLAYER;

    /** {@code true} si tiene dueno: se invoca con huevo y cuenta cupo. */
    public boolean owned() {
        return this == PLAYER || this == MOUNT;
    }

    /** {@code true} si se puede montar. */
    public boolean mountable() {
        return this == MOUNT;
    }

    /** {@code null} solo si el texto no esta vacio y no es un valor valido. */
    public static MobCategory parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        String text = raw.toUpperCase(Locale.ROOT).trim();
        if (text.equals("MONTURA")) {
            return MOUNT;
        }
        try {
            return valueOf(text);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

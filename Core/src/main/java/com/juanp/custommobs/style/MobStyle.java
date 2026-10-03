package com.juanp.custommobs.style;

import org.bukkit.ChatColor;

import java.util.Locale;

/**
 * Estilo elegido por un jugador (o por un team) para sus player mobs.
 *
 * <p>Los dos campos son opcionales y tienen significados distintos:
 *
 * <ul>
 *   <li>{@code nameColor}: {@code null} deja el nombre tal cual lo escribio el admin en el
 *       yml. Con valor, TINE el nombre entero — sobreescribe el color del yml, que es lo
 *       que se pidio.</li>
 *   <li>{@code glowColor}: {@code null} hereda el {@code glow} del yml del mob; {@code "nada"}
 *       lo apaga; un color lo enciende con ese color (el brillo de color lo determina un
 *       equipo de scoreboard, no la entidad).</li>
 * </ul>
 */
public record MobStyle(String nameColor, String glowColor) {

    public static final MobStyle EMPTY = new MobStyle(null, null);

    /** Texto que apaga el brillo en el yml y en el comando. */
    public static final String OFF = "nada";

    /** Color del nombre, resuelto, o {@code null} si no se eligio ninguno. */
    public ChatColor name() {
        return color(this.nameColor);
    }

    /** {@code true} si se pidio apagar el brillo explicitamente. */
    public boolean glowOff() {
        return this.glowColor != null && OFF.equalsIgnoreCase(this.glowColor);
    }

    /** Color del brillo, o {@code null} si se hereda del yml o se apaga. */
    public ChatColor glow() {
        return this.glowOff() ? null : color(this.glowColor);
    }

    /** {@code true} si hay algo que aplicar de verdad. */
    public boolean isEmpty() {
        return this.nameColor == null && this.glowColor == null;
    }

    /** Resuelve un nombre de color. Acepta "nada"/"none" para apagar. */
    public static ChatColor color(String raw) {
        if (raw == null || raw.isBlank() || OFF.equalsIgnoreCase(raw) || "none".equalsIgnoreCase(raw)) {
            return null;
        }
        try {
            ChatColor color = ChatColor.valueOf(raw.toUpperCase(Locale.ROOT).trim());
            return color.isColor() ? color : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

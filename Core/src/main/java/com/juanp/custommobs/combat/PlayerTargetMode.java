package com.juanp.custommobs.combat;

import java.util.Locale;

/**
 * Politica de ataque contra jugadores. El admin elige el valor por defecto en
 * {@code config.yml} y cada mob puede sobrescribirlo en su yml con
 * {@code targets.players}.
 */
public enum PlayerTargetMode {

    /** Nunca ataca jugadores. Mob de apoyo puramente PvE. */
    NEVER,

    /**
     * Solo ataca jugadores que Teams marque como enemigos.
     * Ojo: Teams 1.0.2 nunca llena esa lista, asi que hoy equivale a NEVER.
     */
    ENEMIES,

    /** Ataca a cualquier jugador que no sea de su team ni aliado. */
    NON_ALLIES,

    /**
     * Neutral por defecto; se vuelve hostil contra quien agreda a su bando
     * (el dueno, sus companeros, sus aliados o los mobs de ellos).
     */
    DEFENSIVE;

    /** @return el modo, o {@code null} si el texto no es reconocido (se hereda el global). */
    public static PlayerTargetMode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.toLowerCase(Locale.ROOT).trim().replace('-', '_');
        return switch (value) {
            case "never", "nunca" -> NEVER;
            case "enemies", "enemigos" -> ENEMIES;
            case "non_allies", "nonallies", "no_aliados", "todos" -> NON_ALLIES;
            case "defensive", "defensivo", "retaliate", "reactivo" -> DEFENSIVE;
            default -> null;
        };
    }
}

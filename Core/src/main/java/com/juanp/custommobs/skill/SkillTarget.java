package com.juanp.custommobs.skill;

import java.util.Locale;

/** A quien apunta una skill. */
public enum SkillTarget {

    /** El propio mob. */
    SELF,
    /** El dueno del mob (solo mobs de jugador que lo tengan). */
    OWNER,
    /** La entidad que el mob esta atacando en este momento. */
    TARGET,
    /** Enemigos vivos dentro del radio, segun la politica de objetivos del mob. */
    ENEMIES,
    /** Aliados vivos dentro del radio: dueno, companeros de team o de faccion aliada. */
    ALLIES;

    public static SkillTarget parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.toUpperCase(Locale.ROOT).trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

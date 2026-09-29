package com.juanp.custommobs.skill;

import java.util.Locale;

/** Que hace una skill cuando se ejecuta. */
public enum SkillEffect {

    /** Aplica dano a cada objetivo. */
    DAMAGE,
    /** Restaura vida a cada objetivo. */
    HEAL,
    /** El mob habla: el texto va a los jugadores dentro del radio. */
    MESSAGE,
    /** Aplica un efecto de pocion a cada objetivo. */
    POTION;

    public static SkillEffect parse(String raw) {
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

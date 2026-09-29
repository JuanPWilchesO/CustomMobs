package com.juanp.custommobs.item;

import org.bukkit.attribute.Attribute;

import java.util.Locale;

/** Traduce claves amistosas del yml a constantes de {@link Attribute}. */
public final class Attributes {

    private Attributes() {
    }

    public static Attribute resolve(String key) {
        if (key == null) {
            return null;
        }
        return switch (key.toLowerCase(Locale.ROOT).trim()) {
            case "health", "max-health" -> Attribute.MAX_HEALTH;
            case "damage", "attack-damage" -> Attribute.ATTACK_DAMAGE;
            case "speed", "movement-speed" -> Attribute.MOVEMENT_SPEED;
            case "follow-range" -> Attribute.FOLLOW_RANGE;
            case "armor" -> Attribute.ARMOR;
            case "armor-toughness" -> Attribute.ARMOR_TOUGHNESS;
            case "knockback-resistance" -> Attribute.KNOCKBACK_RESISTANCE;
            case "attack-speed" -> Attribute.ATTACK_SPEED;
            case "attack-knockback" -> Attribute.ATTACK_KNOCKBACK;
            case "max-absorption" -> Attribute.MAX_ABSORPTION;
            case "scale" -> Attribute.SCALE;
            case "step-height" -> Attribute.STEP_HEIGHT;
            case "jump-strength" -> Attribute.JUMP_STRENGTH;
            case "gravity" -> Attribute.GRAVITY;
            case "water-movement-efficiency" -> Attribute.WATER_MOVEMENT_EFFICIENCY;
            case "movement-efficiency" -> Attribute.MOVEMENT_EFFICIENCY;
            default -> null;
        };
    }
}

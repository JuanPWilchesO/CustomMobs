package com.juanp.custommobs.item;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

/**
 * Especificacion de un item de equipamiento tal como viene del yml.
 *
 * @param material    material base
 * @param displayName nombre visible (ya con color) o vacio
 * @param lore        lore (ya con color)
 * @param unbreakable si es irrompible
 * @param glint       forzar el brillo de encantamiento
 * @param enchants    encantamiento -> nivel; la clave admite {@code id}, {@code minecraft:id} o {@code excellentenchants:id}
 * @param dropChance  probabilidad de drop al morir (0 = nunca)
 */
public record EquipItem(
        Material material,
        String displayName,
        List<String> lore,
        boolean unbreakable,
        boolean glint,
        Map<String, Integer> enchants,
        float dropChance
) {
}

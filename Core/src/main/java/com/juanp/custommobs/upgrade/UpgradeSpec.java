package com.juanp.custommobs.upgrade;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

/**
 * Un item de mejora definido en {@code upgrades/<archivo>.yml}.
 *
 * <p>Al usarlo sobre un mob propio, le sube las caracteristicas de combate indicadas y el
 * item se consume. Las claves de {@code stats} son las mismas que las de {@code attributes}
 * en un mob (health, damage, armor...), y el valor es <b>cuanto sube</b>, no el valor final.
 *
 * @param id           identificador unico (campo {@code id} o nombre del archivo)
 * @param material     material base del item
 * @param displayName  nombre, con los codigos de color ya traducidos
 * @param lore         descripcion, con los codigos ya traducidos
 * @param stats        caracteristica de combate -&gt; cuanto sube
 * @param recipeAmount cuantos salen por crafteo; {@code 0} = no se craftea
 * @param recipeExtras ingredientes extra, ademas del material base
 */
public record UpgradeSpec(
        String id,
        Material material,
        String displayName,
        List<String> lore,
        Map<String, Double> stats,
        int recipeAmount,
        List<Material> recipeExtras
) {

    /** {@code true} si el item se puede conseguir crafteando. */
    public boolean craftable() {
        return this.recipeAmount > 0 && !this.stats.isEmpty();
    }

    /** Firma de ingredientes, para detectar recetas indistinguibles. */
    public String signature() {
        java.util.List<String> parts = new java.util.ArrayList<>();
        parts.add(this.material.name());
        for (Material extra : this.recipeExtras) {
            parts.add(extra.name());
        }
        java.util.Collections.sort(parts);
        return String.join("+", parts);
    }
}

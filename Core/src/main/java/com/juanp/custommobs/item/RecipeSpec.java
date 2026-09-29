package com.juanp.custommobs.item;

import org.bukkit.Material;

import java.util.List;

/**
 * Receta shapeless del huevo custom. Siempre lleva el huevo de invocacion
 * de la definicion como primer ingrediente; el resto es configurable.
 *
 * @param extras ingredientes adicionales al huevo (orden irrelevante)
 * @param amount cantidad de huevos que produce la receta; {@code <= 0} desactiva el crafteo
 */
public record RecipeSpec(List<Material> extras, int amount) {

    /** Receta por defecto para definiciones sin seccion {@code recipe}: huevo + diamante. */
    public static final RecipeSpec DEFAULT = new RecipeSpec(List.of(Material.DIAMOND), 1);

    public boolean enabled() {
        return this.amount > 0 && !this.extras.isEmpty();
    }

    /** Firma unica de ingredientes, para detectar recetas que chocan entre si. */
    public String signature(Material egg) {
        List<String> names = new java.util.ArrayList<>();
        names.add(egg.name());
        for (Material extra : this.extras) {
            names.add(extra.name());
        }
        names.sort(String::compareTo);
        return String.join(" + ", names);
    }
}

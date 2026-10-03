package com.juanp.custommobs.item;

import com.juanp.custommobs.drop.ItemCatalog;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.function.Consumer;

/** Construye los ItemStack de equipamiento a partir de la especificacion del yml. */
public final class ItemFactory {

    private ItemFactory() {
    }

    /**
     * @param catalog  catalogo de objetos con nombre; puede ser {@code null}
     * @param onProblem recibe el motivo del problema, ya redactado, para que quien llama
     *                  solo tenga que añadir a que mob se refiere
     * @return el objeto, o {@code null} si no se pudo construir
     */
    public static ItemStack build(EquipItem spec, ItemCatalog catalog, Consumer<String> onProblem) {
        // Con 'item:' el objeto sale del catalogo tal cual: conserva su NBT, que es
        // justamente lo que el yml no puede escribir.
        if (spec.fromCatalog()) {
            ItemStack stored = catalog == null ? null : catalog.get(spec.itemName()).orElse(null);
            if (stored == null) {
                if (onProblem != null) {
                    onProblem.accept("Item de catalogo desconocido '" + spec.itemName() + "'");
                }
                return null;
            }
            return stored.clone();
        }

        if (spec.material() == null) {
            return null;
        }
        ItemStack item = new ItemStack(spec.material());

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (!spec.displayName().isBlank()) {
                meta.setDisplayName(spec.displayName());
            }
            if (!spec.lore().isEmpty()) {
                meta.setLore(spec.lore());
            }
            if (spec.unbreakable()) {
                meta.setUnbreakable(true);
            }
            if (spec.glint()) {
                meta.setEnchantmentGlintOverride(Boolean.TRUE);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }

        for (var entry : spec.enchants().entrySet()) {
            Enchantment enchantment = Enchants.resolve(entry.getKey());
            if (enchantment == null) {
                if (onProblem != null) {
                    onProblem.accept("Encantamiento desconocido '" + entry.getKey() + "'");
                }
                continue;
            }
            // Unsafe a proposito: los encantamientos custom pueden no ser "aplicables"
            // segun la tabla vanilla y no deben lanzar excepcion.
            item.addUnsafeEnchantment(enchantment, entry.getValue());
        }

        return item;
    }
}

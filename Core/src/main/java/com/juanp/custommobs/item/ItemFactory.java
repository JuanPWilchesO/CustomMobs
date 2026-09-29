package com.juanp.custommobs.item;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.function.Consumer;

/** Construye los ItemStack de equipamiento a partir de la especificacion del yml. */
public final class ItemFactory {

    private ItemFactory() {
    }

    public static ItemStack build(EquipItem spec, Consumer<String> onUnknownEnchant) {
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
                if (onUnknownEnchant != null) {
                    onUnknownEnchant.accept(entry.getKey());
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

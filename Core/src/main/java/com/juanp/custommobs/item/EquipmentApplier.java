package com.juanp.custommobs.item;

import com.juanp.custommobs.drop.ItemCatalog;
import com.juanp.custommobs.mob.MobDefinition;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.function.Consumer;

/** Aplica el equipamiento configurado a un mob. */
public final class EquipmentApplier {

    private EquipmentApplier() {
    }

    public static void apply(Mob mob, MobDefinition definition, ItemCatalog catalog,
                             Consumer<String> onProblem) {
        EntityEquipment equipment = mob.getEquipment();
        if (equipment == null) {
            return;
        }

        for (Map.Entry<EquipmentSlot, EquipItem> entry : definition.equipment().entrySet()) {
            EquipmentSlot slot = entry.getKey();
            ItemStack item = ItemFactory.build(entry.getValue(), catalog, onProblem);
            if (item == null) {
                // Sin objeto no se toca el slot: mejor vacio que un item a medias.
                continue;
            }

            switch (slot) {
                case HEAD -> equipment.setHelmet(item);
                case CHEST -> equipment.setChestplate(item);
                case LEGS -> equipment.setLeggings(item);
                case FEET -> equipment.setBoots(item);
                case HAND -> equipment.setItemInMainHand(item);
                case OFF_HAND -> equipment.setItemInOffHand(item);
                default -> {
                }
            }

            setDropChance(equipment, slot, entry.getValue().dropChance());
        }
    }

    private static void setDropChance(EntityEquipment equipment, EquipmentSlot slot, float chance) {
        float value = Math.max(0.0F, Math.min(1.0F, chance));
        switch (slot) {
            case HEAD -> equipment.setHelmetDropChance(value);
            case CHEST -> equipment.setChestplateDropChance(value);
            case LEGS -> equipment.setLeggingsDropChance(value);
            case FEET -> equipment.setBootsDropChance(value);
            case HAND -> equipment.setItemInMainHandDropChance(value);
            case OFF_HAND -> equipment.setItemInOffHandDropChance(value);
            default -> {
            }
        }
    }
}

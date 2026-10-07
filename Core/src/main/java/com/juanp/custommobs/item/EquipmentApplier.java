package com.juanp.custommobs.item;

import com.juanp.custommobs.drop.ItemCatalog;
import com.juanp.custommobs.mob.MobDefinition;
import org.bukkit.Material;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Aplica el equipamiento configurado a un mob.
 *
 * <p>El mob aparece <b>exactamente</b> como dice su definicion. Hay entidades que nacen
 * equipadas —el hacha del vindicador, el arco del esqueleto, la ballesta del saqueador— y
 * ese equipo no lo puso el servidor: un slot que la definicion no mencione se deja vacio.
 */
public final class EquipmentApplier {

    /** Los seis slots de equipo de un mob. */
    private static final List<EquipmentSlot> SLOTS = List.of(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND);

    private EquipmentApplier() {
    }

    public static void apply(Mob mob, MobDefinition definition, ItemCatalog catalog,
                             Consumer<String> onProblem) {
        EntityEquipment equipment = mob.getEquipment();
        if (equipment == null) {
            return;
        }

        for (Map.Entry<EquipmentSlot, EquipItem> entry : definition.equipment().entrySet()) {
            ItemStack item = ItemFactory.build(entry.getValue(), catalog, onProblem);
            if (item == null) {
                // Sin objeto no se toca el slot: mejor vacio que un item a medias.
                continue;
            }
            setItem(equipment, entry.getKey(), item);
            setDropChance(equipment, entry.getKey(), entry.getValue().dropChance());
        }

        // Lo que la definicion no ponga se vacia: asi no queda equipo de fabrica.
        for (EquipmentSlot slot : SLOTS) {
            if (!definition.equipment().containsKey(slot)) {
                setItem(equipment, slot, new ItemStack(Material.AIR));
                setDropChance(equipment, slot, 0.0F);
            }
        }
    }

    private static void setItem(EntityEquipment equipment, EquipmentSlot slot, ItemStack item) {
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

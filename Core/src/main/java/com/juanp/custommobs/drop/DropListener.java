package com.juanp.custommobs.drop;

import com.juanp.custommobs.mob.CustomMob;
import com.juanp.custommobs.mob.MobService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Aplica la tabla de drops de un mob custom al morir.
 *
 * <p>Suma dos fuentes: lo que el admin escribio en el yml del mob y lo que se capturo con
 * el comando. Si el mob tiene {@code clear-vanilla-drops: true}, primero se vacia lo que
 * el juego soltaria por su cuenta.
 */
public final class DropListener implements Listener {

    private final MobService service;
    private final DropService drops;

    public DropListener(MobService service, DropService drops) {
        this.service = service;
        this.drops = drops;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Optional<CustomMob> customMob = this.service.find(entity.getUniqueId());
        if (customMob.isEmpty()) {
            return;
        }
        var definition = customMob.get().definition();

        if (definition.clearVanillaDrops()) {
            event.getDrops().clear();
        }
        for (DropSpec spec : definition.drops()) {
            this.give(event, spec);
        }
    }

    private void give(EntityDeathEvent event, DropSpec spec) {
        if (!this.drops.rolls(spec)) {
            return;
        }
        ItemStack item = this.drops.build(spec, this.drops.rollAmount(spec));
        if (item != null) {
            event.getDrops().add(item);
        }
    }
}

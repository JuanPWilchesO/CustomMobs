package com.juanp.custommobs.mob;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;

/**
 * Evita que los mobs de rol se quemen con el sol.
 *
 * <p>Un NPC con apariencia de zombi arderia al amanecer y estropearia la escena.
 * Solo se cancela el encendido solar: el fuego de lava o de un atacante se respeta.
 */
public final class DaylightListener implements Listener {

    private final MobService service;

    public DaylightListener(MobService service) {
        this.service = service;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCombust(EntityCombustEvent event) {
        if (event instanceof EntityCombustByBlockEvent || event instanceof EntityCombustByEntityEvent) {
            return;
        }
        this.service.find(event.getEntity().getUniqueId()).ifPresent(customMob -> {
            if (!customMob.definition().burnsInDaylight()) {
                event.setCancelled(true);
            }
        });
    }
}

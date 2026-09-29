package com.juanp.custommobs.spawner;

import com.juanp.custommobs.mob.MobService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/** Al morir un mob de spawner, programa su reaparicion en el bloque de origen. */
public final class SpawnerListener implements Listener {

    private final MobService service;

    public SpawnerListener(MobService service) {
        this.service = service;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        this.service.handleSpawnerDeath(event.getEntity());
    }
}

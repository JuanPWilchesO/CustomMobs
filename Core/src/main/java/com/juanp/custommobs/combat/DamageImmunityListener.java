package com.juanp.custommobs.combat;

import com.juanp.custommobs.mob.CustomMob;
import com.juanp.custommobs.mob.MobService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Tipos de dano que un mob ignora, segun su lista {@code immune}.
 *
 * <p>Vale para cualquier categoria: montura, mob de servidor, o mob de jugador anclado a el
 * o a un bloque. El caso tipico es {@code fall} —una montura que salta no deberia hacerse
 * dano al caer— y {@code potion}, que es la causa {@code magic} del dano de las pociones.
 */
public final class DamageImmunityListener implements Listener {

    private final MobService service;

    public DamageImmunityListener(MobService service) {
        this.service = service;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        CustomMob mob = this.service.find(victim.getUniqueId()).orElse(null);
        if (mob == null || !mob.definition().ignores(event.getCause())) {
            return;
        }
        event.setCancelled(true);
    }
}

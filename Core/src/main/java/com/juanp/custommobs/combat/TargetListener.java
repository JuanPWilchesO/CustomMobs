package com.juanp.custommobs.combat;

import com.juanp.custommobs.mob.CustomMob;
import com.juanp.custommobs.mob.MobService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

import java.util.Optional;

/** Cancela objetivos invalidos, incluso si vanilla intenta fijarlos. */
public final class TargetListener implements Listener {

    private final MobService service;
    private final TargetPolicy policy;

    public TargetListener(MobService service) {
        this.service = service;
        this.policy = service.policy();
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        Optional<CustomMob> soldier = this.service.find(mob.getUniqueId());
        if (soldier.isEmpty()) {
            return;
        }
        LivingEntity target = event.getTarget();
        if (target == null) {
            return;
        }
        if (!this.policy.isValidTarget(soldier.get(), target)) {
            event.setCancelled(true);
        }
    }
}

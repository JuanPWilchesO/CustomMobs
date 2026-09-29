package com.juanp.custommobs.combat;

import com.juanp.custommobs.config.PluginConfig;
import com.juanp.custommobs.mob.CustomMob;
import com.juanp.custommobs.mob.MobService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Respaldo de IA para servidores sin la API MobGoals.
 *
 * <p>Mantiene el objetivo de cada soldado valido y busca el mas cercano si no tiene.
 */
public final class TargetingTask extends BukkitRunnable {

    private final MobService service;
    private final TargetPolicy policy;

    public TargetingTask(MobService service, PluginConfig config) {
        this.service = service;
        this.policy = service.policy();
    }

    @Override
    public void run() {
        this.service.prune();
        this.service.aggro().prune();
        boolean goals = this.service.usesGoals();

        for (CustomMob customMob : this.service.active()) {
            if (!(customMob.entity() instanceof Mob mob) || !mob.isValid()) {
                continue;
            }
            // El radio de separacion se aplica siempre, con goals o sin ellos.
            this.service.enforceLeash(customMob, mob);
            if (goals) {
                continue;
            }
            LivingEntity current = mob.getTarget();
            if (current != null && this.policy.isValidTarget(customMob, current)) {
                continue;
            }
            mob.setTarget(this.policy.findTarget(mob));
        }
    }
}

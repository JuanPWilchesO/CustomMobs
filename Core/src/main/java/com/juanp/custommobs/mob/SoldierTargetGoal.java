package com.juanp.custommobs.mob;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import java.util.EnumSet;

/**
 * Goal de objetivo para el soldado, registrado con la API MobGoals de Paper.
 *
 * <p>Sustituye los goals TARGET de vanilla, de modo que el mob solo persigue lo que
 * la {@link com.juanp.custommobs.combat.TargetPolicy} considera valido.
 */
public final class SoldierTargetGoal implements Goal<Mob> {

    private final MobService service;
    private final Mob mob;
    private final GoalKey<Mob> key;

    public SoldierTargetGoal(MobService service, Mob mob) {
        this.service = service;
        this.mob = mob;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(service.plugin(), "soldier_target"));
    }

    @Override
    public boolean shouldActivate() {
        return this.resolve() != null;
    }

    @Override
    public boolean shouldStayActive() {
        LivingEntity target = this.mob.getTarget();
        return target != null && this.service.policy().isValidTarget(this.mob, target);
    }

    @Override
    public void start() {
        LivingEntity target = this.resolve();
        if (target != null) {
            this.mob.setTarget(target);
        }
    }

    @Override
    public void stop() {
        this.mob.setTarget(null);
    }

    @Override
    public void tick() {
        if (this.mob.getTarget() == null) {
            LivingEntity target = this.resolve();
            if (target != null) {
                this.mob.setTarget(target);
            }
        }
    }

    @Override
    public GoalKey<Mob> getKey() {
        return this.key;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.TARGET);
    }

    private LivingEntity resolve() {
        return this.service.policy().findTarget(this.mob);
    }
}

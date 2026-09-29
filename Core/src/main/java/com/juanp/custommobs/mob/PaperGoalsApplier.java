package com.juanp.custommobs.mob;

import com.destroystokyo.paper.entity.ai.GoalType;
import com.destroystokyo.paper.entity.ai.MobGoals;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;

/**
 * Aplica los goals de Paper. Aislado en su propia clase para que el resto del plugin
 * pueda cargarse aunque el servidor no exponga la API MobGoals (Spigot).
 */
final class PaperGoalsApplier {

    private PaperGoalsApplier() {
    }

    static void apply(MobService service, Mob mob) {
        MobGoals goals = Bukkit.getMobGoals();
        goals.removeAllGoals(mob, GoalType.TARGET);
        goals.addGoal(mob, 1, new SoldierTargetGoal(service, mob));
    }
}

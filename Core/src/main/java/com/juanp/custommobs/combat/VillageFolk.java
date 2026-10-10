package com.juanp.custommobs.combat;

import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Snowman;

/**
 * Aldeanos (y comerciantes) y golems: la gente del pueblo.
 *
 * <p>Reune en un solo sitio la lista que deciden {@code village-friendly}. La usan la
 * politica de objetivos —para no apuntarlos— y el bando protegido —para salir en su
 * defensa.
 */
public final class VillageFolk {

    private VillageFolk() {
    }

    public static boolean isFolk(Entity entity) {
        return entity instanceof AbstractVillager || entity instanceof IronGolem
                || entity instanceof Snowman;
    }
}

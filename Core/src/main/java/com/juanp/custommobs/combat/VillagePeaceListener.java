package com.juanp.custommobs.combat;

import com.juanp.custommobs.mob.CustomMob;
import com.juanp.custommobs.mob.MobService;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Snowman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

/**
 * Un mob amistoso con el pueblo no es molestado por los golems.
 *
 * <p>El golem ataca a cualquier {@code Monster} que tenga cerca; da igual el tipo, el disfraz
 * o lo que diga su definicion. Como los mobs custom son casi siempre monstruos, sin esto un
 * soldado tuyo acaba a golemazos con el primero que pase por la aldea.
 *
 * <p>Con {@code village-friendly: true} en el yml, cuando un golem de hierro o de nieve
 * apunta a ese mob, se cancela la eleccion de objetivo.
 *
 * <p>Ojo con la otra mitad: que los aldeanos <b>no huyan</b> no se puede decidir aqui. Los
 * aldeanos tienen sus propias metas de huida y solo reaccionan a ciertos tipos (zombis,
 * illagers, ravager...). De un esqueleto no huyen. Por eso, para un mob que conviva con
 * aldeanos, conviene usar la familia esqueleto: con un zombi o un illager huerian igual.
 */
public final class VillagePeaceListener implements Listener {

    private final MobService service;

    public VillagePeaceListener(MobService service) {
        this.service = service;
    }

    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        LivingEntity target = event.getTarget();
        if (target == null) {
            return;
        }
        CustomMob mob = this.service.find(target.getUniqueId()).orElse(null);
        if (mob == null || !mob.definition().villageFriendly()) {
            return;
        }
        Entity source = event.getEntity();
        if (source instanceof IronGolem || source instanceof Snowman) {
            event.setCancelled(true);
            event.setTarget(null);
        }
    }
}

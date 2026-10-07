package com.juanp.custommobs.mob;

import com.juanp.custommobs.CustomMobsPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Paso helado de una montura: el agua que pisa se convierte en hielo escarchado.
 *
 * <p>No es una pocion —eso no existe— sino el mismo efecto del encantamiento de botas,
 * hecho aqui a mano: se mira el bloque bajo la montura y, si es agua, se hiela; el hielo se
 * deja derretir al cabo de unos segundos.
 */
public final class MountFrostTask extends BukkitRunnable {

    /** Radio alrededor de la montura, como el encantamiento. */
    private static final int RADIUS = 1;

    /** Cuanto tarda en derretirse el hielo escarchado. */
    private static final long MELT_TICKS = 200L;

    private final CustomMobsPlugin plugin;
    private final MobService service;

    public MountFrostTask(CustomMobsPlugin plugin, MobService service) {
        this.plugin = plugin;
        this.service = service;
    }

    /** Para dejar una sola linea de diagnostico por arranque. */
    private boolean reported;

    @Override
    public void run() {
        int horses = 0;
        int withFrost = 0;
        for (CustomMob customMob : this.service.active()) {
            if (!(customMob.entity() instanceof AbstractHorse horse)) {
                continue;
            }
            horses++;
            if (customMob.definition().mount() == null
                    || !customMob.definition().mount().frostWalker()) {
                continue;
            }
            withFrost++;
            this.freeze(horse);
        }
        // Diagnostico: solo con 'debug' encendido.
        if (!this.reported && this.plugin.config().debug()) {
            this.reported = true;
            this.plugin.getLogger().info("[paso helado] caballos=" + horses
                    + ", con paso helado=" + withFrost);
        }
    }

    /** Hiela el agua que la montura tiene bajo las patas y la deja derretirse. */
    private void freeze(AbstractHorse horse) {
        World world = horse.getWorld();
        Location below = horse.getLocation().subtract(0.0D, 1.0D, 0.0D);
        int centerX = below.getBlockX();
        int centerY = below.getBlockY();
        int centerZ = below.getBlockZ();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                Block block = world.getBlockAt(centerX + dx, centerY, centerZ + dz);
                if (block.getType() != Material.WATER) {
                    continue;
                }
                block.setType(Material.FROSTED_ICE);
                this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                    // Solo se derrite si sigue siendo hielo nuestro: si el servidor ya lo
                    // cambio, no se pisa esa decision.
                    if (block.getType() == Material.FROSTED_ICE) {
                        block.setType(Material.WATER);
                    }
                }, MELT_TICKS);
            }
        }
    }
}

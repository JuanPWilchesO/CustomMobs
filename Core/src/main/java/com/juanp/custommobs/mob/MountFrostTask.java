package com.juanp.custommobs.mob;

import com.juanp.custommobs.CustomMobsPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

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

    /** Cuantos bloques por delante se hiela, para que el suelo este listo al llegar. */
    private static final int AHEAD = 2;

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

    /**
     * Hiela la superficie del agua que la montura va a pisar.
     *
     * <p>Se congela la <b>superficie</b> —el agua que tiene aire encima—, no el bloque de
     * debajo: si el caballo va nadando, un bloque mas abajo sigue siendo agua profunda y la
     * superficie nunca se hace suelo. Es lo que hace el encantamiento. Ademas se mira un
     * poco por delante, en la direccion de la marcha, para que el hielo este listo antes de
     * que llegue: a galope, congelar solo bajo las patas se queda corto.
     */
    private void freeze(AbstractHorse horse) {
        World world = horse.getWorld();
        Location at = horse.getLocation();
        this.freezeAt(world, at.getBlockX(), at.getBlockY(), at.getBlockZ());

        Vector heading = at.getDirection().setY(0.0D);
        if (heading.lengthSquared() > 1.0E-4D) {
            heading.normalize();
            int aheadX = at.getBlockX() + (int) Math.round(heading.getX() * AHEAD);
            int aheadZ = at.getBlockZ() + (int) Math.round(heading.getZ() * AHEAD);
            if (aheadX != at.getBlockX() || aheadZ != at.getBlockZ()) {
                this.freezeAt(world, aheadX, at.getBlockY(), aheadZ);
            }
        }
    }

    /** Hiela la superficie del agua alrededor de un punto, a la altura de las patas. */
    private void freezeAt(World world, int centerX, int centerY, int centerZ) {
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    this.freezeSurface(world, centerX + dx, centerY + dy, centerZ + dz);
                }
            }
        }
    }

    /**
     * Convierte en hielo escarchado un bloque de agua, siempre que sea <b>superficie</b>
     * —con algo que no sea agua encima—; es lo que deja suelo firme donde pisar.
     */
    private void freezeSurface(World world, int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        if (block.getType() != Material.WATER) {
            return;
        }
        if (world.getBlockAt(x, y + 1, z).getType() == Material.WATER) {
            return; // no es la superficie: hay agua encima
        }
        block.setType(Material.FROSTED_ICE);
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            // Solo se derrite si sigue siendo hielo nuestro: si el servidor ya lo cambio,
            // no se pisa esa decision.
            if (block.getType() == Material.FROSTED_ICE) {
                block.setType(Material.WATER);
            }
        }, MELT_TICKS);
    }
}

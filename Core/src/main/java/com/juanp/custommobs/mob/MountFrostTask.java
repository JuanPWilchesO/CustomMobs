package com.juanp.custommobs.mob;

import com.juanp.custommobs.CustomMobsPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * Paso helado de una montura: el agua que pisa se convierte en hielo escarchado.
 *
 * <p>Lo que importa es que el hielo este puesto <b>antes</b> de que el caballo llegue. Por
 * eso se mira cada tick, se hiela <b>por delante</b> un tramo que crece con la velocidad (si
 * va rapido, o el servidor da un tiron, un alcance fijo no llega) y se hiela tambien la
 * columna donde esta la montura, que es lo que la levanta cuando ya esta en el agua.
 *
 * <p>En cada columna se busca <b>la superficie</b> —el agua que tiene aire encima— dentro de
 * una ventana corta alrededor de las patas, y se hiela <b>un solo bloque</b>:
 * <ul>
 *   <li>un solo bloque por columna, y con aire encima, evita que el hielo se propague hacia
 *       abajo y deje una columna que atrape al caballo;</li>
 *   <li>buscar la superficie <b>por columna</b> —y no a la altura de las patas— es lo que
 *       deja salir al caballo cuando se ha hundido y la superficie le queda por encima.</li>
 * </ul>
 *
 * <p>Solo se mira hasta <b>un bloque por encima de las patas</b>: mas arriba esta la cabeza,
 * y congelarla ahi encerraria a la montura.
 */
public final class MountFrostTask extends BukkitRunnable {

    /** Cuanto tarda en derretirse el hielo escarchado. */
    private static final long MELT_TICKS = 200L;

    /** Tope duro de bloques que se miran por delante, por mucha prisa que lleve. */
    private static final int MAX_AHEAD = 8;

    /** Tope duro del radio, para que un yml no hiele medio oceano de golpe. */
    private static final int MAX_RADIUS = 4;

    /** Hasta donde se busca la superficie: un bloque por encima de las patas... */
    private static final int WINDOW_UP = 1;

    /** ...y dos por debajo. */
    private static final int WINDOW_DOWN = 2;

    /** Ticks de margen al calcular el alcance por velocidad. */
    private static final double SPEED_MARGIN_TICKS = 2.0D;

    private final CustomMobsPlugin plugin;
    private final MobService service;

    /** DIAGNOSTICO: bloques helados desde el ultimo informe. */
    private int frozenSinceReport;
    private long lastReport = System.currentTimeMillis();

    public MountFrostTask(CustomMobsPlugin plugin, MobService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public void run() {
        int horses = 0;
        int withFrost = 0;
        for (CustomMob customMob : this.service.active()) {
            if (!(customMob.entity() instanceof AbstractHorse horse)) {
                continue;
            }
            horses++;
            MountSpec spec = customMob.definition().mount();
            if (spec == null || !spec.frostWalker()) {
                continue;
            }
            withFrost++;
            this.freeze(horse, spec);
        }
        this.report(horses, withFrost);
    }

    /** DIAGNOSTICO (solo con debug): cuantos bloques se helaron en el ultimo segundo. */
    private void report(int horses, int withFrost) {
        if (!this.plugin.config().debug()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastReport < 1000L) {
            return;
        }
        this.plugin.getLogger().info("[paso helado] caballos=" + horses
                + " conPaso=" + withFrost + " helados=" + this.frozenSinceReport + "/s");
        this.frozenSinceReport = 0;
        this.lastReport = now;
    }

    /** Hiela el agua que la montura va a pisar: bajo las patas y por delante. */
    private void freeze(AbstractHorse horse, MountSpec spec) {
        World world = horse.getWorld();
        Location at = horse.getLocation();
        int radius = Math.max(0, Math.min(MAX_RADIUS, spec.frostRadius()));

        Vector heading = horse.getVelocity();
        heading.setY(0.0D);
        double speed = heading.length();
        if (speed < 1.0E-4D) {
            heading = at.getDirection();
            heading.setY(0.0D);
        }
        // El alcance crece con la velocidad: dos ticks de margen, por si el servidor da un
        // tiron y el caballo avanza de golpe.
        int bySpeed = (int) Math.ceil(speed * SPEED_MARGIN_TICKS) + 1;
        int ahead = Math.min(MAX_AHEAD, Math.max(spec.frostAhead(), bySpeed));

        // El cuerpo del caballo: donde cae esto no se pone hielo. Congelar dentro no lo
        // levanta —Minecraft no expulsa entidades de un bloque— sino que lo deja atrapado.
        BoundingBox body = horse.getBoundingBox();

        this.freezeArea(world, at.getBlockX(), at.getBlockY(), at.getBlockZ(), radius, body);
        if (heading.lengthSquared() > 1.0E-4D) {
            heading.normalize();
            for (int step = 1; step <= ahead; step++) {
                this.freezeArea(world,
                        at.getBlockX() + (int) Math.round(heading.getX() * step),
                        at.getBlockY(),
                        at.getBlockZ() + (int) Math.round(heading.getZ() * step),
                        radius, body);
            }
        }
    }

    /** Hiela la superficie del agua en cada columna de un cuadrado alrededor del punto. */
    private void freezeArea(World world, int centerX, int feetY, int centerZ, int radius,
                           BoundingBox body) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                this.freezeColumn(world, centerX + dx, feetY, centerZ + dz, body);
            }
        }
    }

    /**
     * Busca en una columna la superficie del agua —el agua con aire encima— dentro de la
     * ventana y hiela <b>ese</b> bloque, uno solo.
     */
    private void freezeColumn(World world, int x, int feetY, int z, BoundingBox body) {
        for (int y = feetY + WINDOW_UP; y >= feetY - WINDOW_DOWN; y--) {
            Block block = world.getBlockAt(x, y, z);
            if (block.getType() != Material.WATER) {
                continue;
            }
            if (!world.getBlockAt(x, y + 1, z).getType().isAir()) {
                continue; // no es la superficie: hay algo encima
            }
            BoundingBox cell = new BoundingBox(x, y, z, x + 1.0D, y + 1.0D, z + 1.0D);
            if (body.overlaps(cell)) {
                continue; // ahi esta el caballo: esa columna se deja en paz
            }
            this.ice(block);
            return; // una por columna: asi el hielo no se propaga hacia abajo
        }
    }

    /** Convierte el bloque en hielo escarchado y programa su derretimiento. */
    private void ice(Block block) {
        block.setType(Material.FROSTED_ICE);
        this.frozenSinceReport++;
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            // Solo se derrite si sigue siendo hielo nuestro: si el servidor ya lo cambio,
            // no se pisa esa decision.
            if (block.getType() == Material.FROSTED_ICE) {
                block.setType(Material.WATER);
            }
        }, MELT_TICKS);
    }
}

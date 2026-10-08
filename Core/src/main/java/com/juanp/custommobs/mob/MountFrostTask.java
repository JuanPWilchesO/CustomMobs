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
 * <p>No es una pocion —eso no existe— sino el mismo efecto del encantamiento de botas,
 * hecho aqui a mano: se mira el agua de la superficie y, si la hay, se hiela; el hielo se
 * deja derretir al cabo de unos segundos.
 *
 * <p>Tres cosas aprendidas a golpes, y las tres estan aqui:
 * <ul>
 *   <li>solo se hiela la <b>superficie</b> (agua con aire encima). Si no, el hielo se
 *       propaga hacia abajo cada ciclo y acaba dejando una columna que atrapa al caballo;</li>
 *   <li>se hiela <b>por delante</b>, en la direccion de la marcha: a galope —o con un tiron
 *       de lag— el caballo llega al agua antes de que el hielo aparezca;</li>
 *   <li>nunca se pone hielo <b>donde esta el caballo</b>. Al hundirse un poco, la superficie
 *       queda a su altura o por encima, y congelarla ahi lo encerraba y lo asfixiaba.</li>
 * </ul>
 *
 * <p>El radio y el alcance los fija cada montura en su yml.
 */
public final class MountFrostTask extends BukkitRunnable {

    /** Cuanto tarda en derretirse el hielo escarchado. */
    private static final long MELT_TICKS = 200L;

    /** Tope duro de bloques que se miran por delante, por mucha prisa que lleve. */
    private static final int MAX_AHEAD = 8;

    /** Tope duro del radio, para que un yml no hiele medio oceano de golpe. */
    private static final int MAX_RADIUS = 4;

    /** Cuanto se mira por encima de las patas: cubre la superficie cuando va algo hundido. */
    private static final int WINDOW_UP = 1;

    /** Cuanto se mira por debajo: el suelo que va pisando. */
    private static final int WINDOW_DOWN = 3;

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
            MountSpec spec = customMob.definition().mount();
            if (spec == null || !spec.frostWalker()) {
                continue;
            }
            withFrost++;
            this.freeze(horse, spec);
        }
        // Diagnostico: solo con 'debug' encendido.
        if (!this.reported && this.plugin.config().debug()) {
            this.reported = true;
            this.plugin.getLogger().info("[paso helado] caballos=" + horses
                    + ", con paso helado=" + withFrost);
        }
    }

    /**
     * Hiela el agua que la montura va a pisar: bajo las patas y una fila por delante, en la
     * direccion de la marcha.
     */
    private void freeze(AbstractHorse horse, MountSpec spec) {
        World world = horse.getWorld();
        Location at = horse.getLocation();
        int radius = Math.max(0, Math.min(MAX_RADIUS, spec.frostRadius()));
        int ahead = Math.max(0, Math.min(MAX_AHEAD, spec.frostAhead()));
        // El cuerpo del caballo: donde cae esto no se pone hielo, para no encerrarlo.
        BoundingBox body = horse.getBoundingBox();

        this.freezeAt(world, at.getBlockX(), at.getBlockY(), at.getBlockZ(), radius, body);

        Vector heading = horse.getVelocity();
        heading.setY(0.0D);
        if (heading.lengthSquared() < 1.0E-4D) {
            // Quieta: se usa hacia donde mira, que es a donde saldra.
            heading = at.getDirection();
            heading.setY(0.0D);
        }
        if (heading.lengthSquared() < 1.0E-4D) {
            return;
        }
        heading.normalize();
        for (int step = 1; step <= ahead; step++) {
            int x = at.getBlockX() + (int) Math.round(heading.getX() * step);
            int z = at.getBlockZ() + (int) Math.round(heading.getZ() * step);
            this.freezeAt(world, x, at.getBlockY(), z, radius, body);
        }
    }

    /** Hiela la superficie del agua alrededor de un punto, a la altura de las patas. */
    private void freezeAt(World world, int centerX, int centerY, int centerZ, int radius,
                          BoundingBox body) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = WINDOW_UP; dy >= -WINDOW_DOWN; dy--) {
                    this.freezeSurface(world, centerX + dx, centerY + dy, centerZ + dz, body);
                }
            }
        }
    }

    /**
     * Convierte en hielo escarchado un bloque de agua, siempre que sea la <b>superficie</b>
     * —con aire encima, como exige el encantamiento— y que la montura no este ocupando ese
     * hueco.
     *
     * <p>La condicion del aire evita que el hielo se propague hacia abajo; la del cuerpo
     * evita meterlo dentro del caballo. Las dos sonaron mal en produccion.
     */
    private void freezeSurface(World world, int x, int y, int z, BoundingBox body) {
        Block block = world.getBlockAt(x, y, z);
        if (block.getType() != Material.WATER) {
            return;
        }
        if (!world.getBlockAt(x, y + 1, z).getType().isAir()) {
            return; // no es la superficie: hay algo encima
        }
        BoundingBox cell = new BoundingBox(x, y, z, x + 1.0D, y + 1.0D, z + 1.0D);
        if (body.overlaps(cell)) {
            return; // ahi esta la montura: congelarlo la encerraria
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

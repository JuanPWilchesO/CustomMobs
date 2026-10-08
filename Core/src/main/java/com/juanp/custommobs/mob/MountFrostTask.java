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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Paso helado de una montura: el agua que pisa se convierte en hielo escarchado.
 *
 * <p>El trabajo no es helar, es <b>llegar a tiempo</b>. Y la leccion mas cara: si el propio
 * plugin cuesta caro, el servidor va lento, y entonces el hielo llega tarde <b>por culpa del
 * plugin</b>. Se mide en milisegundos de tick lo que se gana en bloques de delantal.
 *
 * <p>Por eso esta escrito para costar lo minimo:
 * <ul>
 *   <li>una sola lectura de bloque por columna, tirando del mapa de altura del mundo, en vez
 *       de recorrer niveles a ciegas;</li>
 *   <li>el derretimiento va en una cola, no en una tarea programada por bloque (que eran
 *       miles de tareas pendientes);</li>
 *   <li>las areas que se miran son pequenas: no hay delantales de decenas de bloques.</li>
 * </ul>
 *
 * <p>Reglas del hielo, cada una por un fallo visto en produccion:
 * <ul>
 *   <li>nunca se hiela a la altura de las patas ni por encima: si la montura va algo hundida,
 *       esa seria la altura de su cuerpo o su cabeza, y se asfixia;</li>
 *   <li>nunca se hiela un bloque que solape a la montura: congelar dentro no la levanta
 *       (Minecraft no expulsa entidades de un bloque), la deja clavada;</li>
 *   <li>una sola capa por columna, y con aire encima: asi el hielo no se propaga hacia abajo.</li>
 * </ul>
 */
public final class MountFrostTask extends BukkitRunnable {

    /** Cuanto tarda en derretirse el hielo escarchado. */
    private static final long MELT_TICKS = 400L;

    /** Radio del cerco que se mira alrededor de la montura. */
    private static final int RADIUS = 2;

    /** Delantal por delante, en bloques. */
    private static final int AHEAD = 10;

    /** Bloques que se anade al delantal por cada tick que el server tenga la montura parada. */
    private static final int STALL_BONUS = 1;

    /** Tope del delantal, para que un tiron de lag no dispare el coste. */
    private static final int MAX_AHEAD = 20;

    /** Hasta donde se mira de las patas hacia abajo. */
    private static final int WINDOW_DOWN = 3;

    private final CustomMobsPlugin plugin;
    private final MobService service;

    /** Ultima posicion de cada montura, para sacar el avance real entre ticks. */
    private final Map<UUID, Location> lastPosition = new ConcurrentHashMap<>();

    /** Ultimo rumbo real, para seguir helando cuando el server estanca la posicion. */
    private final Map<UUID, Vector> lastHeading = new ConcurrentHashMap<>();

    /** Ticks seguidos con la posicion del server parada, por montura. */
    private final Map<UUID, Integer> stalledTicks = new ConcurrentHashMap<>();

    /** Hielos puestos por el plugin, en orden, para ir derritiendolos. */
    private final Deque<Frozen> frozen = new ArrayDeque<>();

    /** Reloj propio, en ticks de esta tarea. */
    private long clock;

    /** DIAGNOSTICO: bloques helados desde el ultimo informe. */
    private int frozenSinceReport;
    private double maxStepSinceReport;
    private long lastReport = System.currentTimeMillis();

    /** Un bloque de hielo puesto por el plugin, con el tick en que se puso. */
    private record Frozen(World world, int x, int y, int z, long tick) {
    }

    public MountFrostTask(CustomMobsPlugin plugin, MobService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public void run() {
        this.clock++;
        this.meltExpired();
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
        this.lastPosition.keySet().removeIf(key -> this.service.find(key).isEmpty());
        this.lastHeading.keySet().removeIf(key -> this.service.find(key).isEmpty());
        this.stalledTicks.keySet().removeIf(key -> this.service.find(key).isEmpty());
        this.report(horses, withFrost);
    }

    /** Derrite el hielo que ya cumplio su tiempo. En cola: lo mas viejo primero. */
    private void meltExpired() {
        while (!this.frozen.isEmpty()
                && this.clock - this.frozen.peekFirst().tick() >= MELT_TICKS) {
            Frozen item = this.frozen.pollFirst();
            Block block = item.world().getBlockAt(item.x(), item.y(), item.z());
            if (block.getType() == Material.FROSTED_ICE) {
                block.setType(Material.WATER);
            }
        }
    }

    /** DIAGNOSTICO (solo con debug): hielo por segundo y avance maximo por tick. */
    private void report(int horses, int withFrost) {
        if (!this.plugin.config().debug()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastReport < 1000L) {
            return;
        }
        this.plugin.getLogger().info("[paso helado] caballos=" + horses
                + " conPaso=" + withFrost + " helados=" + this.frozenSinceReport
                + "/s avanceMax=" + String.format("%.2f", this.maxStepSinceReport) + "/tick"
                + " vivas=" + this.frozen.size());
        this.frozenSinceReport = 0;
        this.maxStepSinceReport = 0.0D;
        this.lastReport = now;
    }

    /** Hiela el agua que la montura va a pisar: bajo las patas y un delantal por delante. */
    private void freeze(AbstractHorse horse, MountSpec spec) {
        World world = horse.getWorld();
        Location at = horse.getLocation();
        // Si ya esta dentro del agua no se hiela nada: helar la superficie a su altura le
        // levantaria un pozo rodeado de hielo del que, nadando, no puede salir.
        if (at.getBlock().getType() == Material.WATER) {
            return;
        }

        BoundingBox body = horse.getBoundingBox();
        this.freezeArea(world, at.getBlockX(), at.getBlockY(), at.getBlockZ(), RADIUS, body);

        // Avance real desde el tick anterior.
        Location previous = this.lastPosition.put(horse.getUniqueId(), at.clone());
        Vector step = null;
        if (previous != null && previous.getWorld() == world) {
            Vector delta = at.toVector().subtract(previous.toVector());
            delta.setY(0.0D);
            if (delta.lengthSquared() > 1.0E-4D) {
                step = delta;
            }
        }
        UUID id = horse.getUniqueId();
        int stalled;
        if (step != null) {
            this.maxStepSinceReport = Math.max(this.maxStepSinceReport, step.length());
            step.normalize();
            this.lastHeading.put(id, step.clone());
            stalled = 0;
        } else {
            Vector heading = this.lastHeading.get(id);
            if (heading == null) {
                heading = at.getDirection();
                heading.setY(0.0D);
            }
            if (heading.lengthSquared() < 1.0E-4D) {
                return;
            }
            step = heading.clone();
            stalled = Math.min(MAX_AHEAD, this.stalledTicks.merge(id, 1, Integer::sum));
        }
        this.stalledTicks.put(id, stalled);

        // El delantal crece con el retraso del servidor: mientras tenga la montura parada, se
        // sigue helando hacia donde iba, cada tick un poco mas lejos.
        int ahead = Math.min(MAX_AHEAD, AHEAD + stalled * STALL_BONUS);
        for (int index = 1; index <= ahead; index++) {
            int x = at.getBlockX() + (int) Math.round(step.getX() * index);
            int z = at.getBlockZ() + (int) Math.round(step.getZ() * index);
            this.freezeArea(world, x, at.getBlockY(), z, 1, body);
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
     * Hiela la superficie del agua de una columna, si procede.
     *
     * <p>Se usa el mapa de altura del mundo para saber donde acaba el agua: una lectura por
     * columna en vez de recorrer niveles a ciegas. Recorrer el nivel cuesta; y si el plugin
     * cuesta, el servidor va lento y el hielo llega tarde.
     */
    private void freezeColumn(World world, int x, int feetY, int z, BoundingBox body) {
        int y = world.getHighestBlockYAt(x, z);
        if (y > feetY - 1 || y < feetY - WINDOW_DOWN) {
            return; // fuera de la ventana: ni por encima de las patas, ni demasiado abajo
        }
        Block block = world.getBlockAt(x, y, z);
        if (block.getType() != Material.WATER) {
            return;
        }
        BoundingBox cell = new BoundingBox(x, y, z, x + 1.0D, y + 1.0D, z + 1.0D);
        if (body.overlaps(cell)) {
            return; // ahi esta la montura
        }
        block.setType(Material.FROSTED_ICE);
        this.frozen.addLast(new Frozen(world, x, y, z, this.clock));
        this.frozenSinceReport++;
    }
}

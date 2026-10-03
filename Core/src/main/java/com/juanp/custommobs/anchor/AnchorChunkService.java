package com.juanp.custommobs.anchor;

import com.juanp.custommobs.CustomMobsPlugin;
import com.juanp.custommobs.config.PluginConfig;
import com.juanp.custommobs.mob.MobDefinition;
import com.juanp.custommobs.mob.MobService;
import com.juanp.custommobs.mob.PlayerMobRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Mantiene cargados los chunks alrededor del bloque de un mob de jugador anclado a un
 * punto.
 *
 * <p>Un mob con {@code anchor: point} vive fijo en su bloque. Sin esto, cuando ningun
 * jugador tiene su chunk cerca, el chunk se descarga: el mob desaparece de la vista y
 * queda inerte hasta que alguien pase. Con esto su zona se mantiene cargada, asi que
 * nunca se pierde ni deja de contar.
 *
 * <p>Solo se suelta cuando el mob se recoge con su huevo. Si luego se coloca en otro
 * sitio, el siguiente ciclo pide los chunks del lugar nuevo y suelta los del viejo.
 *
 * <p><b>Coste</b>: cada mob ocupa un cuadrado de (2*radio+1)^2 chunks. Con el radio por
 * defecto (4) son 81 chunks por mob.
 */
public final class AnchorChunkService extends BukkitRunnable {

    private final CustomMobsPlugin plugin;
    private final MobService service;
    private final PluginConfig config;

    /** linkId -&gt; chunks que ese mob tiene cargadas ahora mismo. */
    private final Map<UUID, Held> held = new HashMap<>();

    public AnchorChunkService(CustomMobsPlugin plugin, MobService service, PluginConfig config) {
        this.plugin = plugin;
        this.service = service;
        this.config = config;
    }

    /** Chunks cargados por un vinculo, con el mundo al que pertenecen. */
    private static final class Held {
        private final String world;
        private final Set<Long> chunks = new HashSet<>();

        private Held(String world) {
            this.world = world;
        }
    }

    @Override
    public void run() {
        Set<UUID> wanted = new HashSet<>();
        for (PlayerMobRegistry.MobLink link : this.service.playerMobs().all()) {
            if (!link.deployed()) {
                continue;
            }
            MobDefinition definition = this.plugin.registry().get(link.definitionId()).orElse(null);
            if (definition == null || !definition.leash().anchoredToPoint()) {
                continue;
            }
            // El mob puede traer su propio radio; si no, manda el global.
            int radius = definition.chunkRadius() != null
                    ? definition.chunkRadius()
                    : this.config.pointChunkRadius();
            if (radius <= 0) {
                continue;
            }
            Location point = link.location();
            if (point == null || point.getWorld() == null) {
                continue;
            }
            wanted.add(link.linkId());
            this.hold(link.linkId(), point, radius);
        }
        // Un vinculo recogido —o que dejo de ser de este tipo— suelta lo suyo.
        Iterator<Map.Entry<UUID, Held>> iterator = this.held.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Held> entry = iterator.next();
            if (!wanted.contains(entry.getKey())) {
                this.release(entry.getValue());
                iterator.remove();
            }
        }
    }

    private void hold(UUID linkId, Location point, int radius) {
        World world = point.getWorld();
        String worldName = world.getName();
        int centerX = point.getBlockX() >> 4;
        int centerZ = point.getBlockZ() >> 4;

        Held current = this.held.get(linkId);
        if (current == null || !current.world.equals(worldName)) {
            if (current != null) {
                this.release(current);
            }
            current = new Held(worldName);
            this.held.put(linkId, current);
        }

        Set<Long> wanted = new HashSet<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                wanted.add(pack(centerX + dx, centerZ + dz));
            }
        }

        // Suelta lo que quedo fuera (el mob se movio de sitio o bajo el radio).
        for (Iterator<Long> it = current.chunks.iterator(); it.hasNext(); ) {
            Long key = it.next();
            if (!wanted.contains(key)) {
                world.removePluginChunkTicket(unpackX(key), unpackZ(key), this.plugin);
                it.remove();
            }
        }
        // Pide lo que falta. Se repite el ticket cada ciclo a proposito: si otro servicio
        // lo solto, aqui se vuelve a poner.
        for (Long key : wanted) {
            world.addPluginChunkTicket(unpackX(key), unpackZ(key), this.plugin);
            current.chunks.add(key);
        }
    }

    private void release(Held held) {
        if (held.chunks.isEmpty()) {
            return;
        }
        World world = Bukkit.getWorld(held.world);
        if (world != null) {
            for (Long key : held.chunks) {
                world.removePluginChunkTicket(unpackX(key), unpackZ(key), this.plugin);
            }
        }
        held.chunks.clear();
    }

    /** Suelta todos los chunks que este servicio tuviera pedidos. */
    public void shutdown() {
        for (Held held : this.held.values()) {
            this.release(held);
        }
        this.held.clear();
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static int unpackX(long key) {
        return (int) (key >> 32);
    }

    private static int unpackZ(long key) {
        return (int) key;
    }
}

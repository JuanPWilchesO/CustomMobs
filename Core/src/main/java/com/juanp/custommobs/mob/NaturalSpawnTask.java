package com.juanp.custommobs.mob;

import com.juanp.custommobs.CustomMobsPlugin;
import com.juanp.custommobs.config.PluginConfig;
import com.juanp.custommobs.region.RegionGate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Hace aparecer mobs de servidor al azar por el mundo.
 *
 * <p>Es lo contrario de un spawner: aqui no hay punto fijo ni reaparicion, sino intentos
 * alrededor de los jugadores, como hace el propio Minecraft. Los mobs que salen son de una
 * sola vida y sin persistencia: el servidor se los lleva cuando no hay nadie cerca.
 *
 * <p><b>Solo suma.</b> No toca la aparicion vanilla ni los spawners del plugin: si algo no
 * cuadra en las condiciones, simplemente no aparece nada.
 */
public final class NaturalSpawnTask extends BukkitRunnable {

    /** Intentos por sitio antes de rendirse con ese jugador. */
    private static final int PLACE_TRIES = 12;

    private final CustomMobsPlugin plugin;
    private final MobService service;
    private final MobRegistry registry;
    private final RegionGate regions;
    private final Random random = new Random();

    public NaturalSpawnTask(CustomMobsPlugin plugin, MobService service, MobRegistry registry,
                            RegionGate regions) {
        this.plugin = plugin;
        this.service = service;
        this.registry = registry;
        this.regions = regions;
    }

    @Override
    public void run() {
        PluginConfig config = this.service.config();
        if (!config.spawnEnabled() || this.service.naturalCount() >= config.spawnGlobalCap()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int attempt = 0; attempt < config.spawnAttempts(); attempt++) {
                if (this.service.naturalCount() >= config.spawnGlobalCap()) {
                    return;
                }
                this.attempt(player);
            }
        }
    }

    /** Un intento: elegir mob, tirar la probabilidad, buscar sitio y soltar el grupo. */
    private void attempt(Player player) {
        List<MobDefinition> candidates = new ArrayList<>();
        for (MobDefinition definition : this.registry.all()) {
            if (this.eligible(definition, player)) {
                candidates.add(definition);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        MobDefinition definition = candidates.get(this.random.nextInt(candidates.size()));
        if (this.random.nextDouble() > definition.spawn().chance()) {
            return;
        }
        Location spot = this.findSpot(player, definition.spawn());
        if (spot == null) {
            return;
        }
        NaturalSpawn spawn = definition.spawn();
        int group = spawn.groupMin() + this.random.nextInt(
                Math.max(1, spawn.groupMax() - spawn.groupMin() + 1));
        for (int i = 0; i < group; i++) {
            this.service.spawnNatural(definition, spot);
        }
    }

    /** Condiciones que se deciden sin mirar el terreno. */
    private boolean eligible(MobDefinition definition, Player player) {
        NaturalSpawn spawn = definition.spawn();
        if (spawn == null || !spawn.natural() || !definition.server()) {
            return false;
        }
        if (!this.service.worldEnabled(player.getWorld())) {
            return false;
        }
        if (!spawn.worlds().isEmpty()
                && !spawn.worlds().contains(player.getWorld().getName().toLowerCase(Locale.ROOT))) {
            return false;
        }
        return this.service.naturalCount(definition.id()) < spawn.cap();
    }

    /** Busca un sitio valido alrededor del jugador. */
    private Location findSpot(Player player, NaturalSpawn spawn) {
        World world = player.getWorld();
        for (int tries = 0; tries < PLACE_TRIES; tries++) {
            double distance = spawn.minDistance()
                    + this.random.nextDouble() * Math.max(0.0D, spawn.maxDistance() - spawn.minDistance());
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            int x = player.getLocation().getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            int z = player.getLocation().getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
            // Regiones (WorldGuard), si el mob las limita. Se mira antes de nada: es lo que
            // mas descarta y sale mas barato que mirar el terreno.
            Location candidate = new Location(world, x + 0.5D, 0.0D, z + 0.5D);
            if (!this.regions.allows(candidate, spawn.allowedRegion(), spawn.denyRegions())) {
                continue;
            }
            // Superficie: es donde tiene sentido y donde no aparece dentro de una cueva.
            int y = world.getHighestBlockYAt(x, z) + 1;
            if (y < spawn.minY() || y > spawn.maxY()) {
                continue;
            }
            Block ground = world.getBlockAt(x, y - 1, z);
            Block air = world.getBlockAt(x, y, z);
            if (!ground.getType().isSolid() || !air.isPassable()) {
                continue;
            }
            if (!this.timeMatches(world, spawn.time())) {
                continue;
            }
            int light = air.getLightLevel();
            if (light < spawn.lightMin() || light > spawn.lightMax()) {
                continue;
            }
            if (!spawn.biomes().isEmpty()
                    && !spawn.biomes().contains(this.biomeOf(ground).toLowerCase(Locale.ROOT))) {
                continue;
            }
            return new Location(world, x + 0.5D, y, z + 0.5D);
        }
        return null;
    }

    /** {@code any} siempre vale; {@code day} y {@code night} miran la hora del mundo. */
    private boolean timeMatches(World world, String time) {
        if (time == null || time.isBlank() || time.equals("any")) {
            return true;
        }
        boolean night = world.getTime() >= 13000L && world.getTime() <= 23000L;
        return time.equals("night") == night;
    }

    /** Bioma del bloque, sin el prefijo {@code minecraft:}. */
    private String biomeOf(Block block) {
        String raw = block.getBiome().getKey().toString();
        int colon = raw.indexOf(':');
        return colon < 0 ? raw : raw.substring(colon + 1);
    }
}

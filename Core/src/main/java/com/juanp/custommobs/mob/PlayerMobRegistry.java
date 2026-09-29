package com.juanp.custommobs.mob;

import com.juanp.custommobs.CustomMobsPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Cupo de mobs de jugador: cuantos tiene vivo cada jugador.
 *
 * <p>Se lleva en disco a proposito. Un mob de jugador anclado a un bloque fijo
 * ({@code anchor: point} en su yml) puede quedarse en un chunk descargado, y ahi no hay
 * API que permita enumerarlo: si contaramos solo los cargados, el jugador podria
 * desplegar una tropa en bloques repartidos y saltarse el tope.
 *
 * <p>El cupo sube al invocar y baja al morir o al retirar el mob. Es tolerante a fallos:
 * si un mob desaparece sin avisar (un plugin externo, un corte), la cuenta se corrige
 * con {@code /custommobs cuota liberar <jugador>}.
 */
public final class PlayerMobRegistry {

    private final CustomMobsPlugin plugin;
    private final File file;

    /** Jugador -> mobs suyos. */
    private final Map<UUID, Set<UUID>> byOwner = new HashMap<>();
    /** Mob -> jugador, para poder restar por id de entidad. */
    private final Map<UUID, UUID> ownerOf = new HashMap<>();

    private boolean dirty;

    public PlayerMobRegistry(CustomMobsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "player-mobs.yml");
    }

    public void load() {
        this.byOwner.clear();
        this.ownerOf.clear();
        if (!this.file.exists()) {
            this.plugin.getLogger().info("Cupo de mobs: sin registro previo, se empieza de cero.");
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(this.file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String rawOwner : players.getKeys(false)) {
            UUID ownerId = parse(rawOwner);
            if (ownerId == null) {
                continue;
            }
            Set<UUID> mobs = new HashSet<>();
            for (String rawMob : players.getStringList(rawOwner)) {
                UUID mobId = parse(rawMob);
                if (mobId != null && mobs.add(mobId)) {
                    this.ownerOf.put(mobId, ownerId);
                }
            }
            if (!mobs.isEmpty()) {
                this.byOwner.put(ownerId, mobs);
            }
        }
        this.plugin.getLogger().info("Cupo de mobs cargado: " + this.ownerOf.size()
                + " mobs repartidos entre " + this.byOwner.size() + " jugadores.");
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Set<UUID>> entry : this.byOwner.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            List<String> ids = new ArrayList<>(entry.getValue().size());
            for (UUID mobId : entry.getValue()) {
                ids.add(mobId.toString());
            }
            yaml.set("players." + entry.getKey(), ids);
        }
        try {
            yaml.save(this.file);
            this.dirty = false;
        } catch (IOException ex) {
            this.plugin.getLogger().warning("No se pudo guardar el cupo de mobs: " + ex.getMessage());
        }
    }

    /** Suma un mob al cupo de su dueno. Repetirlo no lo cuenta dos veces. */
    public void add(UUID ownerId, UUID mobId) {
        if (ownerId == null || mobId == null) {
            return;
        }
        UUID previous = this.ownerOf.put(mobId, ownerId);
        if (previous != null && !previous.equals(ownerId)) {
            Set<UUID> old = this.byOwner.get(previous);
            if (old != null) {
                old.remove(mobId);
            }
        }
        if (previous == null || !previous.equals(ownerId)) {
            this.byOwner.computeIfAbsent(ownerId, id -> new HashSet<>()).add(mobId);
            this.dirty = true;
        }
    }

    /** Resta un mob por su id de entidad. */
    public void remove(UUID mobId) {
        UUID ownerId = this.ownerOf.remove(mobId);
        if (ownerId == null) {
            return;
        }
        Set<UUID> mobs = this.byOwner.get(ownerId);
        if (mobs != null) {
            mobs.remove(mobId);
            if (mobs.isEmpty()) {
                this.byOwner.remove(ownerId);
            }
        }
        this.dirty = true;
    }

    public int count(UUID ownerId) {
        Set<UUID> mobs = this.byOwner.get(ownerId);
        return mobs == null ? 0 : mobs.size();
    }

    public int total() {
        return this.ownerOf.size();
    }

    public Set<UUID> mobsOf(UUID ownerId) {
        Set<UUID> mobs = this.byOwner.get(ownerId);
        return mobs == null ? Set.of() : Set.copyOf(mobs);
    }

    /** Borra el cupo de un jugador. Sirve para corregir cuentas desincronizadas. */
    public int reset(UUID ownerId) {
        Set<UUID> mobs = this.byOwner.remove(ownerId);
        if (mobs == null) {
            return 0;
        }
        for (UUID mobId : mobs) {
            this.ownerOf.remove(mobId);
        }
        this.dirty = true;
        return mobs.size();
    }

    public boolean isDirty() {
        return this.dirty;
    }

    private static UUID parse(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

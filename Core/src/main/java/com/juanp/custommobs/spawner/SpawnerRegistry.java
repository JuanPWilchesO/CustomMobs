package com.juanp.custommobs.spawner;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Registro persistente de spawners.
 *
 * <p>El punto de reaparicion no puede vivir en la entidad: cuando el mob muere, se
 * lleva su PersistentDataContainer consigo. Por eso los spawners se guardan aparte,
 * en {@code data/spawners.yml}.
 */
public final class SpawnerRegistry {

    private final Plugin plugin;
    private final File file;
    private final Map<UUID, SpawnerEntry> entries = new LinkedHashMap<>();

    public SpawnerRegistry(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/spawners.yml");
    }

    /** Lee los spawners del disco. Los archivos corruptos se ignoran. */
    public void load() {
        this.entries.clear();
        if (!this.file.exists()) {
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(this.file);
        ConfigurationSection root = cfg.getConfigurationSection("spawners");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                UUID id = UUID.fromString(key);
                String definition = section.getString("definition");
                String world = section.getString("world");
                if (definition == null || world == null) {
                    continue;
                }
                this.entries.put(id, new SpawnerEntry(id, definition, world,
                        section.getDouble("x"), section.getDouble("y"), section.getDouble("z"),
                        section.getInt("respawn-seconds", 0), section.getLong("respawn-at", 0L)));
            } catch (IllegalArgumentException ignored) {
                this.plugin.getLogger().warning("Spawner con id invalido en data/spawners.yml: " + key);
            }
        }
    }

    /** Vuelca los spawners al disco. */
    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (SpawnerEntry entry : this.entries.values()) {
            String base = "spawners." + entry.id() + ".";
            cfg.set(base + "definition", entry.definitionId());
            cfg.set(base + "world", entry.world());
            cfg.set(base + "x", entry.x());
            cfg.set(base + "y", entry.y());
            cfg.set(base + "z", entry.z());
            cfg.set(base + "respawn-seconds", entry.respawnSeconds());
            if (entry.awaitingRespawn()) {
                cfg.set(base + "respawn-at", entry.respawnAt());
            }
        }
        try {
            File parent = this.file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                this.plugin.getLogger().warning("No se pudo crear la carpeta data/");
            }
            cfg.save(this.file);
        } catch (IOException ex) {
            this.plugin.getLogger().warning("No se pudo guardar data/spawners.yml: " + ex.getMessage());
        }
    }

    public void put(SpawnerEntry entry) {
        this.entries.put(entry.id(), entry);
    }

    public void remove(UUID id) {
        this.entries.remove(id);
    }

    public Optional<SpawnerEntry> get(UUID id) {
        return Optional.ofNullable(this.entries.get(id));
    }

    public Collection<SpawnerEntry> all() {
        return List.copyOf(this.entries.values());
    }

    public int size() {
        return this.entries.size();
    }
}

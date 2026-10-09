package com.juanp.custommobs.faction;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Faccion de cada jugador, guardada en {@code data/player-factions.yml}.
 *
 * <p>Es un fichero propio del plugin, como {@code data/spawners.yml} o
 * {@code player-mobs.yml}: la faccion es estado de juego, no un permiso, y no deberia
 * depender de tener LuckPerms instalado.
 *
 * <p>Con faccion asignada, los mobs de faccion tratan al jugador por las relaciones de
 * {@code factions.yml} —aliado o enemigo— en vez de por su actitud; sin faccion, manda la
 * actitud de siempre, asi que nada de lo que ya estaba cambia.
 *
 * <p>La asignacion es cosa de un administrador, por comando o consola.
 */
public final class PlayerFactions {

    private final Plugin plugin;
    private final File file;
    private final Map<UUID, String> entries = new LinkedHashMap<>();

    public PlayerFactions(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/player-factions.yml");
    }

    /** Lee las facciones del disco. Un archivo corrupto o una linea mala se ignoran. */
    public void load() {
        this.entries.clear();
        if (!this.file.exists()) {
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(this.file);
        ConfigurationSection root = cfg.getConfigurationSection("players");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            String faction = root.getString(key);
            if (faction == null || faction.isBlank()) {
                continue;
            }
            try {
                this.entries.put(UUID.fromString(key), faction.trim().toLowerCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // clave que no es un UUID: se descarta
            }
        }
    }

    public Optional<String> of(UUID playerId) {
        return Optional.ofNullable(this.entries.get(playerId));
    }

    public boolean has(UUID playerId) {
        return this.entries.containsKey(playerId);
    }

    public void set(UUID playerId, String faction) {
        this.entries.put(playerId, faction.trim().toLowerCase(Locale.ROOT));
        this.save();
    }

    public boolean clear(UUID playerId) {
        boolean removed = this.entries.remove(playerId) != null;
        if (removed) {
            this.save();
        }
        return removed;
    }

    public Map<UUID, String> all() {
        return Map.copyOf(this.entries);
    }

    private void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        ConfigurationSection root = cfg.createSection("players");
        for (Map.Entry<UUID, String> entry : this.entries.entrySet()) {
            root.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            File parent = this.file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            cfg.save(this.file);
        } catch (IOException ex) {
            this.plugin.getLogger().warning("No se pudo guardar data/player-factions.yml: "
                    + ex.getMessage());
        }
    }
}

package com.juanp.custommobs.upgrade;

import com.juanp.custommobs.CustomMobsPlugin;
import com.juanp.custommobs.item.Attributes;
import com.juanp.custommobs.mob.Texts;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Catalogo de items de mejora, uno por archivo en {@code upgrades/}.
 *
 * <p>Solo se aceptan caracteristicas de <b>combate</b>: si un archivo pide otra cosa, esa
 * clave se descarta con un aviso y el item carga igual.
 */
public final class UpgradeRegistry {

    /** Caracteristicas que un item de mejora puede tocar. */
    public static final List<String> COMBAT_STATS = List.of(
            "health", "damage", "speed", "armor", "armor-toughness",
            "knockback-resistance", "attack-speed", "attack-knockback");

    private final CustomMobsPlugin plugin;
    private final File folder;
    private final Map<String, UpgradeSpec> byId = new LinkedHashMap<>();

    public UpgradeRegistry(CustomMobsPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "upgrades");
    }

    /** Relee la carpeta entera. */
    public void reload() {
        this.byId.clear();
        if (!this.folder.isDirectory()) {
            if (!this.folder.mkdirs()) {
                this.plugin.getLogger().warning("No se pudo crear la carpeta upgrades/");
                return;
            }
            // Primera vez: se deja el ejemplo que viaja dentro del jar, para que se vea
            // como se escribe uno. Como los mobs, no se sobrescribe nunca.
            try {
                this.plugin.saveResource("upgrades/ejemplo_piedra_vida.yml", false);
            } catch (Throwable throwable) {
                this.plugin.getLogger().warning("No se pudo copiar el ejemplo de mejora: "
                        + throwable.getMessage());
            }
        }
        File[] files = this.folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String fileId = file.getName().substring(0, file.getName().length() - 4);
            UpgradeSpec spec = read(file, fileId);
            if (spec == null) {
                continue;
            }
            if (this.byId.putIfAbsent(spec.id(), spec) != null) {
                this.plugin.getLogger().warning("Item de mejora repetido: '" + spec.id() + "'");
            }
        }
        this.plugin.getLogger().info("Items de mejora cargados: " + this.byId.size());
    }

    private UpgradeSpec read(File file, String fileId) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String id = normalize(cfg.getString("id", fileId));
        Material material = Material.matchMaterial(cfg.getString("material", ""));
        if (id == null || material == null) {
            this.plugin.getLogger().warning("Item de mejora sin 'id' o 'material' valido: " + file.getName());
            return null;
        }
        if (material.isAir()) {
            this.plugin.getLogger().warning("El item de mejora '" + id + "' usa un material vacio; se omite.");
            return null;
        }

        Map<String, Double> stats = new LinkedHashMap<>();
        ConfigurationSection section = cfg.getConfigurationSection("stats");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String stat = key.toLowerCase(Locale.ROOT).trim();
                if (!COMBAT_STATS.contains(stat)) {
                    this.plugin.getLogger().warning("El item de mejora '" + id + "' pide '"
                            + key + "', que no es una caracteristica de combate; se descarta.");
                    continue;
                }
                double amount = section.getDouble(key);
                if (amount == 0.0D) {
                    continue;
                }
                stats.put(stat, amount);
            }
        }
        if (stats.isEmpty()) {
            this.plugin.getLogger().warning("El item de mejora '" + id + "' no mejora nada; se omite.");
            return null;
        }

        List<String> lore = new ArrayList<>();
        for (String line : cfg.getStringList("lore")) {
            lore.add(Texts.color(line));
        }
        List<Material> extras = new ArrayList<>();
        for (String raw : cfg.getStringList("recipe.extras")) {
            Material extra = Material.matchMaterial(raw);
            if (extra == null) {
                this.plugin.getLogger().warning("Ingrediente desconocido '" + raw + "' en '" + id + "'.");
                continue;
            }
            extras.add(extra);
        }
        int amount = Math.max(0, cfg.getInt("recipe.amount", 1));
        return new UpgradeSpec(id, material, Texts.color(cfg.getString("display-name", id)),
                List.copyOf(lore), Map.copyOf(stats), amount, List.copyOf(extras));
    }

    public Optional<UpgradeSpec> get(String id) {
        return Optional.ofNullable(id == null ? null : this.byId.get(id.toLowerCase(Locale.ROOT)));
    }

    public List<UpgradeSpec> all() {
        return List.copyOf(this.byId.values());
    }

    public int size() {
        return this.byId.size();
    }

    /** Resuelve la caracteristica a un atributo de Bukkit; {@code null} si no aplica. */
    public static org.bukkit.attribute.Attribute attributeOf(String stat) {
        return Attributes.resolve(stat);
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.toLowerCase(Locale.ROOT).trim().replace(' ', '_');
    }
}

package com.juanp.custommobs.drop;

import com.juanp.custommobs.CustomMobsPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Catalogo de objetos con nombre.
 *
 * <p>Resuelve el problema de fondo: un objeto tocado por otro plugin (un encantamiento de
 * ExcellentEnchants, cualquier NBT) no se puede escribir a mano en un yml. El comando lo
 * captura una vez y le da un <b>nombre</b>; despues cualquier mob lo llama por ese nombre.
 *
 * <p>Cada objeto vive en su propio archivo, {@code items/<nombre>.yml}, y el nombre queda
 * escrito dentro. Asi el catalogo se puede leer, copiar y versionar como cualquier config.
 */
public final class ItemCatalog {

    private final CustomMobsPlugin plugin;
    private final File folder;

    public ItemCatalog(CustomMobsPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "items");
    }

    /**
     * Guarda el objeto bajo ese nombre, con todo su NBT.
     *
     * @return {@code false} si el nombre no es valido o no se pudo escribir
     */
    public boolean save(String rawName, ItemStack item) {
        String name = clean(rawName);
        if (name == null || item == null || item.getType() == Material.AIR) {
            return false;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("nombre", name);
        yaml.set("item", Base64.getEncoder().encodeToString(item.serializeAsBytes()));
        yaml.set("etiqueta", labelOf(item));
        try {
            if (!this.folder.exists() && !this.folder.mkdirs()) {
                this.plugin.getLogger().warning("No se pudo crear la carpeta items/");
                return false;
            }
            yaml.save(this.fileOf(name));
            return true;
        } catch (IOException ex) {
            this.plugin.getLogger().warning("No se pudo guardar el item '" + name + "': " + ex.getMessage());
            return false;
        }
    }

    /** Objeto guardado con ese nombre, con su NBT intacto. */
    public Optional<ItemStack> get(String rawName) {
        String name = clean(rawName);
        if (name == null) {
            return Optional.empty();
        }
        File file = this.fileOf(name);
        if (!file.exists()) {
            return Optional.empty();
        }
        String data = YamlConfiguration.loadConfiguration(file).getString("item");
        if (data == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(ItemStack.deserializeBytes(Base64.getDecoder().decode(data)));
        } catch (Throwable ex) {
            this.plugin.getLogger().warning("El item '" + name + "' esta corrupto y no se pudo leer.");
            return Optional.empty();
        }
    }

    /** Nombres guardados, ordenados. */
    public List<String> names() {
        File[] files = this.folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>(files.length);
        for (File file : files) {
            names.add(file.getName().substring(0, file.getName().length() - 4));
        }
        Collections.sort(names);
        return List.copyOf(names);
    }

    /** Borra un objeto del catalogo. */
    public boolean remove(String rawName) {
        String name = clean(rawName);
        if (name == null) {
            return false;
        }
        File file = this.fileOf(name);
        return file.exists() && file.delete();
    }

    /** Etiqueta legible del objeto, para que el archivo se entienda al abrirlo. */
    private static String labelOf(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name();
    }

    private File fileOf(String name) {
        return new File(this.folder, name + ".yml");
    }

    /**
     * Normaliza el nombre: minusculas, sin espacios y solo con caracteres seguros.
     * El nombre acaba siendo un nombre de archivo, asi que no puede llevar barras ni puntos.
     */
    private static String clean(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String name = raw.toLowerCase(Locale.ROOT).trim().replace(' ', '_');
        if (!name.matches("[a-z0-9_-]{1,48}")) {
            return null;
        }
        return name;
    }
}

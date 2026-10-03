package com.juanp.custommobs.mob;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/** Claves de PersistentDataContainer usadas para marcar entidades e items. */
public final class MobKeys {

    private final NamespacedKey definition;
    private final NamespacedKey owner;
    private final NamespacedKey team;
    private final NamespacedKey link;
    private final NamespacedKey spawner;
    private final NamespacedKey spawnWorld;
    private final NamespacedKey spawnX;
    private final NamespacedKey spawnY;
    private final NamespacedKey spawnZ;

    public MobKeys(Plugin plugin) {
        this.definition = new NamespacedKey(plugin, "definition");
        this.owner = new NamespacedKey(plugin, "owner");
        this.team = new NamespacedKey(plugin, "team");
        this.link = new NamespacedKey(plugin, "link");
        this.spawner = new NamespacedKey(plugin, "spawner");
        this.spawnWorld = new NamespacedKey(plugin, "spawn_world");
        this.spawnX = new NamespacedKey(plugin, "spawn_x");
        this.spawnY = new NamespacedKey(plugin, "spawn_y");
        this.spawnZ = new NamespacedKey(plugin, "spawn_z");
    }

    public NamespacedKey definition() {
        return this.definition;
    }

    public NamespacedKey owner() {
        return this.owner;
    }

    public NamespacedKey team() {
        return this.team;
    }

    /** Id del vinculo huevo<->mob. Lo llevan la entidad y el huevo que la representa. */
    public NamespacedKey link() {
        return this.link;
    }

    public NamespacedKey spawner() {
        return this.spawner;
    }

    public NamespacedKey spawnWorld() {
        return this.spawnWorld;
    }

    public NamespacedKey spawnX() {
        return this.spawnX;
    }

    public NamespacedKey spawnY() {
        return this.spawnY;
    }

    public NamespacedKey spawnZ() {
        return this.spawnZ;
    }
}

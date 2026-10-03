package com.juanp.custommobs.config;

import com.juanp.custommobs.combat.PlayerTargetMode;
import com.juanp.custommobs.mob.Texts;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Configuracion tipada del plugin.
 *
 * @param debug               log extra
 * @param targetRadius        radio de busqueda de objetivos
 * @param taskIntervalTicks   intervalo del task de objetivos
 * @param consumeEgg          si el huevo se consume al invocar
 * @param playerTargetMode    politica por defecto contra jugadores
 * @param aggroDurationMillis cuanto dura la hostilidad ganada por agresion
 * @param aggroWatchRadius    radio en el que se vigilan agresiones al bando
 * @param aggroOnFriendlyMobs si reacciona a agresiones contra mobs del mismo bando
 * @param skillIntervalTicks  cada cuantos ticks se evaluan las skills
 * @param playerPermission    permiso para usar los huevos custom
 * @param adminPermission     permiso para los comandos de administracion
 * @param defaultPlayerMobs   cupo de quien no tenga grupo mapeado; {@code 0} = sin limite
 * @param groupLimits         cupo por grupo de LuckPerms, con las claves en minusculas
 * @param motdEnabled         si el MOTD se manda al entrar
 * @param motd                lineas del MOTD, ya con los codigos de color traducidos
 */
public record PluginConfig(
        boolean debug,
        double targetRadius,
        long taskIntervalTicks,
        boolean consumeEgg,
        PlayerTargetMode playerTargetMode,
        long aggroDurationMillis,
        double aggroWatchRadius,
        boolean aggroOnFriendlyMobs,
        long skillIntervalTicks,
        String playerPermission,
        String adminPermission,
        int defaultPlayerMobs,
        Map<String, Integer> groupLimits,
        boolean motdEnabled,
        List<String> motd
) {

    public static PluginConfig load(FileConfiguration cfg) {
        PlayerTargetMode mode = PlayerTargetMode.parse(cfg.getString("targeting.player-mode", "defensive"));
        if (mode == null) {
            mode = PlayerTargetMode.DEFENSIVE;
        }
        return new PluginConfig(
                cfg.getBoolean("debug", false),
                Math.max(1.0D, cfg.getDouble("targeting.radius", 16.0D)),
                Math.max(1L, cfg.getLong("targeting.interval-ticks", 20L)),
                cfg.getBoolean("craft.consume-egg", true),
                mode,
                Math.max(1L, cfg.getLong("targeting.aggro-duration-seconds", 120L)) * 1000L,
                Math.max(1.0D, cfg.getDouble("targeting.aggro-watch-radius", 24.0D)),
                cfg.getBoolean("targeting.aggro-on-friendly-mobs", true),
                Math.max(1L, cfg.getLong("skills.interval-ticks", 20L)),
                cfg.getString("permissions.player", "custommobs.player"),
                cfg.getString("permissions.admin", "custommobs.admin"),
                Math.max(0, cfg.getInt("limits.default-player-mobs", 0)),
                readGroupLimits(cfg),
                cfg.getBoolean("branding.motd-enabled", true),
                colorList(cfg.getStringList("branding.motd"))
        );
    }

    private static List<String> colorList(List<String> raw) {
        List<String> lines = new ArrayList<>(raw.size());
        for (String line : raw) {
            lines.add(Texts.color(line));
        }
        return List.copyOf(lines);
    }

    /** Cupo por grupo. Las claves se guardan en minusculas para comparar sin sorpresas. */
    private static Map<String, Integer> readGroupLimits(FileConfiguration cfg) {
        ConfigurationSection section = cfg.getConfigurationSection("limits.groups");
        if (section == null) {
            return Map.of();
        }
        Map<String, Integer> limits = new LinkedHashMap<>();
        for (String group : section.getKeys(false)) {
            limits.put(group.toLowerCase(Locale.ROOT), Math.max(0, section.getInt(group)));
        }
        return Map.copyOf(limits);
    }
}

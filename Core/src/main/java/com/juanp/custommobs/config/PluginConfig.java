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
 * @param enabledWorlds       mundos donde funciona el plugin, en minusculas; vacio = todos
 * @param recallEnabled       si los mobs abandonados se retiran solos
 * @param recallRadius        radio de abandono; {@code 0} = derivado de la simulation-distance
 * @param recallMobSeconds    segundos antes de destruir el mob abandonado
 * @param recallChunkSeconds  segundos antes de liberar el chunk que se mantuvo cargado
 */
public record PluginConfig(
        boolean debug,
        double targetRadius,
        long taskIntervalTicks,
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
        List<String> motd,
        List<String> enabledWorlds,
        boolean recallEnabled,
        double recallRadius,
        long recallMobSeconds,
        long recallChunkSeconds
) {

    public static PluginConfig load(FileConfiguration cfg) {
        PlayerTargetMode mode = PlayerTargetMode.parse(cfg.getString("targeting.player-mode", "defensive"));
        if (mode == null) {
            mode = PlayerTargetMode.DEFENSIVE;
        }
        // La chunk tiene que sobrevivir al mob: si se pidiera soltar antes de destruirlo,
        // nadie podria tocarlo. Por eso el suelo de chunk-seconds es mob-seconds.
        long mobSeconds = Math.max(1L, cfg.getLong("recall.mob-seconds", 60L));
        long chunkSeconds = Math.max(mobSeconds, cfg.getLong("recall.chunk-seconds", 120L));
        return new PluginConfig(
                cfg.getBoolean("debug", false),
                Math.max(1.0D, cfg.getDouble("targeting.radius", 16.0D)),
                Math.max(1L, cfg.getLong("targeting.interval-ticks", 20L)),
                mode,
                Math.max(1L, cfg.getLong("targeting.aggro-duration-seconds", 120L)) * 1000L,
                Math.max(1.0D, cfg.getDouble("targeting.aggro-watch-radius", 24.0D)),
                cfg.getBoolean("targeting.aggro-on-friendly-mobs", true),
                Math.max(1L, cfg.getLong("skills.interval-ticks", 20L)),
                cfg.getString("permissions.player", "custommobs.player"),
                cfg.getString("permissions.admin", "custommobs.admin"),
                // El mismo valor que trae la plantilla: un servidor con un config.yml
                // anterior no debe quedarse sin tope por no tener la clave.
                Math.max(0, cfg.getInt("limits.default-player-mobs", 3)),
                readGroupLimits(cfg),
                cfg.getBoolean("branding.motd-enabled", true),
                motdOf(cfg),
                lowerList(cfg.getStringList("worlds.enabled")),
                cfg.getBoolean("recall.enabled", true),
                Math.max(0.0D, cfg.getDouble("recall.radius", 0.0D)),
                mobSeconds,
                chunkSeconds
        );
    }

    private static List<String> lowerList(List<String> raw) {
        List<String> values = new ArrayList<>(raw.size());
        for (String value : raw) {
            if (value != null && !value.isBlank()) {
                values.add(value.toLowerCase(java.util.Locale.ROOT).trim());
            }
        }
        return List.copyOf(values);
    }

    /**
     * MOTD efectivo: el del yml si lo define, y si no el que trae el plugin.
     *
     * <p>Hace falta un valor por defecto en el codigo porque {@code config.yml} NO se
     * reescribe en un servidor que ya lo tenia: sin esto, un servidor existente se quedaria
     * sin MOTD solo porque su archivo es anterior a esta seccion.
     */
    private static List<String> motdOf(FileConfiguration cfg) {
        if (cfg.isList("branding.motd")) {
            return colorList(cfg.getStringList("branding.motd"));
        }
        return DEFAULT_MOTD;
    }

    /** El mismo MOTD que trae la plantilla del config, para servidores con un yml anterior. */
    private static final List<String> DEFAULT_MOTD = colorList(List.of(
            "&0&m----------------------------------",
            " &cCustomMobs &0| &cAP2P Project",
            " &cBienvenido, &4{jugador}&c.",
            "&0&m----------------------------------"));

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

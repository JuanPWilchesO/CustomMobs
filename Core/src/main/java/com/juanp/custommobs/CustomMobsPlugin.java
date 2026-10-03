package com.juanp.custommobs;

import com.juanp.custommobs.api.CustomMobsApi;
import com.juanp.custommobs.combat.AggroListener;
import com.juanp.custommobs.combat.TargetListener;
import com.juanp.custommobs.audio.AmbientTask;
import com.juanp.custommobs.audio.SoundListener;
import com.juanp.custommobs.audio.SoundService;
import com.juanp.custommobs.combat.TargetingTask;
import com.juanp.custommobs.command.CustomMobsCommand;
import com.juanp.custommobs.config.PluginConfig;
import com.juanp.custommobs.craft.CraftService;
import com.juanp.custommobs.craft.EggListener;
import com.juanp.custommobs.disguise.DisguiseLink;
import com.juanp.custommobs.disguise.DisguiseLinkResolver;
import com.juanp.custommobs.drop.DropListener;
import com.juanp.custommobs.drop.DropService;
import com.juanp.custommobs.faction.FactionBook;
import com.juanp.custommobs.group.GroupLink;
import com.juanp.custommobs.group.GroupLinkResolver;
import com.juanp.custommobs.impl.CustomMobsApiImpl;
import com.juanp.custommobs.message.MotdListener;
import com.juanp.custommobs.mob.Texts;
import com.juanp.custommobs.mob.DaylightListener;
import com.juanp.custommobs.mob.MobRegistry;
import com.juanp.custommobs.mob.MobService;
import com.juanp.custommobs.skill.SkillService;
import com.juanp.custommobs.spawner.SpawnerListener;
import com.juanp.custommobs.spawner.SpawnerRegistry;
import com.juanp.custommobs.style.StyleRegistry;
import com.juanp.custommobs.style.StyleService;
import com.juanp.custommobs.team.TeamLink;
import com.juanp.custommobs.team.TeamLinkResolver;
import org.bukkit.command.PluginCommand;

import java.io.File;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * CustomMobs: mobs definidos por YAML, invocables por crafteo, vinculados al plugin de Teams.
 */
public final class CustomMobsPlugin extends JavaPlugin {

    /**
     * Firma del autor en el log. Va hardcodeada a proposito: es la firma del proyecto,
     * no un ajuste de configuracion. Los colores son los de la bandera palestina:
     * negro, blanco, rojo y verde.
     */
    private static final String SIGNATURE = Texts.color("&0Plugin &fby &cAP2P &2Project");

    private PluginConfig config;
    private MobRegistry registry;
    private MobService mobService;
    private TeamLink teamLink;
    private GroupLink groupLink;
    private FactionBook factions;
    private DisguiseLink disguiseLink;
    private DropService dropService;
    private StyleService styleService;
    private SpawnerRegistry spawners;
    private CraftService craftService;
    private SoundService soundService;
    private AmbientTask ambientTask;
    private SkillService skillService;
    private TargetingTask targetingTask;
    private CustomMobsApiImpl api;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.config = PluginConfig.load(this.getConfig());

        // La firma va lo primero que escribe el plugin, para que se vea al arrancar.
        this.getLogger().info(SIGNATURE);

        this.registry = new MobRegistry(this);
        this.registry.reload();

        if (!new File(this.getDataFolder(), "factions.yml").exists()) {
            this.saveResource("factions.yml", false);
        }
        this.factions = new FactionBook();
        this.factions.reload(this);

        this.teamLink = TeamLinkResolver.resolve(this);
        this.groupLink = GroupLinkResolver.resolve(this);
        this.disguiseLink = DisguiseLinkResolver.resolve(this);
        this.spawners = new SpawnerRegistry(this);
        this.spawners.load();
        this.mobService = new MobService(this, this.config, this.registry, this.teamLink, this.factions,
                this.disguiseLink, this.groupLink, this.spawners);
        this.mobService.probeGoals();

        this.soundService = new SoundService(this.mobService);
        this.dropService = new DropService(this);

        StyleRegistry styleRegistry = new StyleRegistry(this);
        styleRegistry.load();
        this.styleService = new StyleService(this.mobService, styleRegistry);
        this.mobService.attachStyles(this.styleService);

        this.craftService = new CraftService(this, this.registry, this.mobService.keys());
        this.craftService.registerAll();

        this.api = new CustomMobsApiImpl(this.mobService, this.registry);
        this.getServer().getServicesManager().register(CustomMobsApi.class, this.api, this, ServicePriority.Normal);

        PluginManager pluginManager = this.getServer().getPluginManager();
        pluginManager.registerEvents(this.mobService, this);
        pluginManager.registerEvents(new EggListener(this, this.craftService, this.mobService), this);
        pluginManager.registerEvents(new TargetListener(this.mobService), this);
        pluginManager.registerEvents(new AggroListener(this.mobService), this);
        pluginManager.registerEvents(new SoundListener(this.mobService, this.soundService), this);
        pluginManager.registerEvents(new DaylightListener(this.mobService), this);
        pluginManager.registerEvents(new SpawnerListener(this.mobService), this);
        pluginManager.registerEvents(new DropListener(this.mobService, this.dropService), this);
        pluginManager.registerEvents(new MotdListener(this), this);

        PluginCommand command = this.getCommand("custommobs");
        if (command != null) {
            CustomMobsCommand executor = new CustomMobsCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        this.targetingTask = new TargetingTask(this.mobService, this.config);
        long interval = this.config.taskIntervalTicks();
        this.targetingTask.runTaskTimer(this, interval, interval);

        this.ambientTask = new AmbientTask(this.soundService);
        this.ambientTask.runTaskTimer(this, 20L, 20L);

        this.skillService = new SkillService(this.mobService);
        long skillInterval = this.config.skillIntervalTicks();
        this.skillService.runTaskTimer(this, skillInterval, skillInterval);

        this.mobService.restoreAll();
        this.mobService.resumeSpawners();

        this.getLogger().info("CustomMobs habilitado. Definiciones: " + this.registry.size()
                + " | facciones: " + this.factions.size()
                + " | disfraces: " + this.disguiseLink.name()
                + " | teams: " + this.teamLink.name()
                + " | IA MobGoals: " + this.mobService.usesGoals()
                + " | jugadores: " + this.config.playerTargetMode()
                + " | aggro: " + (this.config.aggroDurationMillis() / 1000L) + "s"
                + " | skills cada " + skillInterval + "t"
                + " | grupos: " + this.groupLink.name()
                + " | cupo por defecto: " + (this.config.defaultPlayerMobs() > 0
                        ? String.valueOf(this.config.defaultPlayerMobs()) : "sin limite"));
    }

    @Override
    public void onDisable() {
        if (this.mobService != null) {
            this.mobService.playerMobs().save();
        }
        if (this.styleService != null) {
            this.styleService.registry().save();
        }
        if (this.targetingTask != null) {
            this.targetingTask.cancel();
        }
        if (this.ambientTask != null) {
            this.ambientTask.cancel();
        }
        if (this.craftService != null) {
            this.craftService.unregisterAll();
        }
        if (this.spawners != null) {
            this.spawners.save();
        }
        if (this.mobService != null) {
            this.mobService.clear();
        }
        this.getServer().getServicesManager().unregisterAll(this);
    }

    /** Recarga config, catalogo y recetas. */
    public void reloadAll() {
        this.reloadConfig();
        this.config = PluginConfig.load(this.getConfig());
        this.registry.reload();
        this.factions.reload(this);
        this.craftService.registerAll();
        if (this.mobService != null) {
            this.mobService.playerMobs().load();
        }
    }

    /**
     * El jar del plugin. Sirve para leer los recursos que trae dentro (los ejemplos
     * de {@code mobs/}), porque {@code getFile()} es protected en JavaPlugin.
     */
    public File jarFile() {
        return this.getFile();
    }

    public PluginConfig config() {
        return this.config;
    }

    public MobRegistry registry() {
        return this.registry;
    }

    public MobService mobs() {
        return this.mobService;
    }

    public DropService drops() {
        return this.dropService;
    }

    public StyleService styles() {
        return this.styleService;
    }

    public CraftService craft() {
        return this.craftService;
    }

    public TeamLink teamLink() {
        return this.teamLink;
    }

    public FactionBook factions() {
        return this.factions;
    }

    public DisguiseLink disguiseLink() {
        return this.disguiseLink;
    }

    public SpawnerRegistry spawners() {
        return this.spawners;
    }

    public CustomMobsApi api() {
        return this.api;
    }
}

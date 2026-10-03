package com.juanp.custommobs.mob;

import com.juanp.custommobs.CustomMobsPlugin;
import com.juanp.custommobs.combat.AggroRegistry;
import com.juanp.custommobs.combat.TargetPolicy;
import com.juanp.custommobs.config.PluginConfig;
import com.juanp.custommobs.disguise.DisguiseLink;
import com.juanp.custommobs.faction.FactionBook;
import com.juanp.custommobs.group.GroupLink;
import com.juanp.custommobs.item.Attributes;
import com.juanp.custommobs.item.EquipmentApplier;
import com.juanp.custommobs.spawner.SpawnerEntry;
import com.juanp.custommobs.style.StyleService;
import com.juanp.custommobs.spawner.SpawnerRegistry;
import com.juanp.custommobs.team.TeamLink;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nucleo del plugin: invoca, marca, restaura y rastrea los mobs custom.
 *
 * <p>El vinculo (dueno, team, definicion, punto de aparicion) vive en el PersistentDataContainer
 * de la entidad, por eso sobrevive reinicios sin necesidad de un archivo aparte. La unica
 * excepcion son los spawners: su punto debe sobrevivir a la muerte del mob, asi que se guardan
 * en {@code data/spawners.yml}.
 */
public final class MobService implements Listener {

    private final CustomMobsPlugin plugin;
    private final PluginConfig config;
    private final MobRegistry registry;
    private final TeamLink teamLink;
    private final PlayerMobRegistry playerMobs;
    private final MobKeys keys;
    private final TargetPolicy policy;
    private final AggroRegistry aggro;
    private final FactionBook factions;
    private final DisguiseLink disguiseLink;
    private final GroupLink groupLink;
    private final SpawnerRegistry spawners;
    private final Map<UUID, CustomMob> active = new ConcurrentHashMap<>();

    private StyleService styles;

    private boolean paperGoals = true;

    public MobService(CustomMobsPlugin plugin, PluginConfig config, MobRegistry registry, TeamLink teamLink,
                      FactionBook factions, DisguiseLink disguiseLink, GroupLink groupLink,
                      SpawnerRegistry spawners) {
        this.plugin = plugin;
        this.config = config;
        this.registry = registry;
        this.teamLink = teamLink;
        this.factions = factions;
        this.disguiseLink = disguiseLink;
        this.groupLink = groupLink;
        this.spawners = spawners;
        this.playerMobs = new PlayerMobRegistry(plugin);
        this.playerMobs.load();
        this.keys = new MobKeys(plugin);
        this.aggro = new AggroRegistry();
        this.policy = new TargetPolicy(this, config.targetRadius());
    }

    public CustomMobsPlugin plugin() {
        return this.plugin;
    }

    /** Cupo persistente de mobs de jugador. */
    public PlayerMobRegistry playerMobs() {
        return this.playerMobs;
    }

    public MobKeys keys() {
        return this.keys;
    }

    public TargetPolicy policy() {
        return this.policy;
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

    public PluginConfig config() {
        return this.config;
    }

    /** {@code true} si la IA se gestiona con la API MobGoals de Paper. */
    public boolean usesGoals() {
        return this.paperGoals;
    }

    public Collection<CustomMob> active() {
        return List.copyOf(this.active.values());
    }

    /**
     * Mobs de jugador vivos que pertenecen a este jugador.
     *
     * <p>Sale del registro persistente, no de las entidades cargadas: un mob anclado a un
     * bloque fijo puede estar en un chunk descargado y aun asi ocupar cupo.
     */
    public int countPlayerMobs(UUID ownerId) {
        return this.playerMobs.count(ownerId);
    }

    /**
     * Engancha el servicio de estilos. Se hace despues de construir porque el estilo
     * necesita al servicio de mobs, y el servicio de mobs necesita al estilo para
     * aplicar el nombre: seria un ciclo en el constructor.
     */
    public void attachStyles(StyleService styles) {
        this.styles = styles;
    }

    /** Lee los grupos del jugador (LuckPerms, o el respaldo sin el). */
    public GroupLink groupLink() {
        return this.groupLink;
    }

    /**
     * Cupo del jugador, segun su grupo de LuckPerms.
     *
     * <p>Si pertenece a varios grupos gana el cupo mas alto: es lo menos sorprendente
     * cuando un rango hereda de otro. Si ninguno tiene cupo mapeado en el config, o no
     * se pudieron leer sus grupos, se usa el cupo por defecto.
     */
    public int limitOf(UUID ownerId) {
        int best = -1;
        if (ownerId != null) {
            for (String group : this.groupLink.groupsOf(ownerId)) {
                Integer limit = this.config.groupLimits().get(group);
                if (limit != null && limit > best) {
                    best = limit;
                }
            }
        }
        return best >= 0 ? best : this.config.defaultPlayerMobs();
    }

    /** {@code true} si el jugador ya alcanzo su maximo de mobs simultaneos. */
    public boolean atPlayerLimit(UUID ownerId) {
        int max = this.limitOf(ownerId);
        return max > 0 && this.countPlayerMobs(ownerId) >= max;
    }

    /** Resultado de vaciar el cupo de un jugador. */
    public record PurgeResult(int removed, int unreachable) {
    }

    /**
     * Rehace el cupo de un jugador sin tocar sus mobs.
     *
     * <p>Deja en la cuenta solo los que se pueden ver ahora mismo. Una entrada de un mob
     * que ya no existe (murio sin avisar, lo borro otro plugin) desaparece y no vuelve;
     * una de un mob real que esta en un chunk descargado volvera a sumarse cuando su
     * chunk cargue. Sirve para corregir cuentas infladas sin matar nada.
     *
     * @return cuantas entradas fantasma se descartaron
     */
    public int resyncPlayerMobs(UUID ownerId) {
        if (ownerId == null) {
            return 0;
        }
        int before = this.playerMobs.count(ownerId);
        this.playerMobs.reset(ownerId);
        int seen = 0;
        for (CustomMob customMob : this.active.values()) {
            if (ownerId.equals(customMob.ownerId())) {
                this.playerMobs.add(ownerId, customMob.entity().getUniqueId());
                seen++;
            }
        }
        this.playerMobs.save();
        return Math.max(0, before - seen);
    }

    /**
     * Retira los mobs de un jugador y deja su cupo a cero.
     *
     * <p>Solo alcanza los que estan cargados. Un mob anclado a un bloque en un chunk
     * descargado no se puede tocar hasta que ese chunk cargue; cuando lo haga, volvera a
     * contar solo, porque el registro se repuebla al cargar la entidad.
     *
     * <p>Es destructivo: los mobs que alcanza desaparecen del mundo.
     *
     * @return cuantos se retiraron y cuantos quedaron fuera de alcance
     */
    public PurgeResult purgePlayerMobs(UUID ownerId) {
        if (ownerId == null) {
            return new PurgeResult(0, 0);
        }
        List<LivingEntity> doomed = new ArrayList<>();
        for (CustomMob customMob : this.active.values()) {
            if (ownerId.equals(customMob.ownerId())) {
                doomed.add(customMob.entity());
            }
        }
        for (LivingEntity entity : doomed) {
            this.active.remove(entity.getUniqueId());
            entity.remove();
        }
        int forgotten = this.playerMobs.reset(ownerId);
        this.playerMobs.save();
        return new PurgeResult(doomed.size(), Math.max(0, forgotten - doomed.size()));
    }

    /** Cupo restante del jugador; {@code -1} si no hay limite configurado. */
    public int remainingPlayerMobs(UUID ownerId) {
        int max = this.limitOf(ownerId);
        return max <= 0 ? -1 : Math.max(0, max - this.countPlayerMobs(ownerId));
    }

    public Optional<CustomMob> find(UUID entityId) {
        return Optional.ofNullable(this.active.get(entityId));
    }

    /** Comprueba una sola vez si el servidor expone la API MobGoals. */
    public void probeGoals() {
        try {
            Bukkit.getMobGoals();
            this.paperGoals = true;
        } catch (Throwable throwable) {
            this.paperGoals = false;
            this.plugin.getLogger().info("API MobGoals no disponible. Se usara el modo por eventos.");
        }
    }

    /** Invoca un mob. Los mobs de servidor ignoran el dueno aunque se pase uno. */
    public LivingEntity spawn(MobDefinition definition, Location location, Player owner) {
        if (owner != null && !definition.server() && this.atPlayerLimit(owner.getUniqueId())) {
            return null;
        }
        return this.spawn(definition, location, owner, null);
    }

    /** Invoca un mob sin dueno: mobs de servidor y pruebas por consola. */
    public LivingEntity spawn(MobDefinition definition, Location location) {
        return this.spawn(definition, location, null, null);
    }

    private LivingEntity spawn(MobDefinition definition, Location location, Player owner, UUID knownSpawnerId) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }
        Class<? extends Entity> type = definition.entityType().getEntityClass();
        if (type == null) {
            return null;
        }
        UUID ownerId = owner != null && !definition.server() ? owner.getUniqueId() : null;

        // El disfraz se encola antes de crear la entidad: si no, se ve el mob base un instante.
        this.disguiseLink.applyNext(definition);
        // La configuracion va dentro del consumidor de aparicion: asi el nombre ya esta
        // puesto cuando otros plugins (MobHealth, por ejemplo) ven la entidad por primera vez.
        Entity spawned = world.spawn(location, type, entity -> {
            if (entity instanceof Mob mob) {
                this.configure(mob, definition, ownerId, knownSpawnerId, location);
            }
        });
        if (!(spawned instanceof Mob mob)) {
            spawned.remove();
            return null;
        }
        this.register(mob);
        if (ownerId != null) {
            this.playerMobs.add(ownerId, mob.getUniqueId());
        }
        return mob;
    }

    /** Configura una entidad ya existente y la registra. */
    public void prepare(Mob mob, MobDefinition definition, UUID ownerId) {
        this.configure(mob, definition, ownerId, null, mob.getLocation());
        this.register(mob);
    }

    /**
     * Marca la entidad como mob custom. Se invoca desde el consumidor de aparicion de
     * Paper, antes de que la entidad entre al mundo.
     */
    private void configure(Mob mob, MobDefinition definition, UUID ownerId, UUID knownSpawnerId,
                           Location spawn) {
        mob.setPersistent(true);
        mob.setRemoveWhenFarAway(false);
        mob.setCanPickupItems(false);
        mob.setCustomName(definition.displayName());
        mob.setCustomNameVisible(true);
        if (definition.glow()) {
            mob.setGlowing(true);
        }

        this.applyAttributes(mob, definition);
        EquipmentApplier.apply(mob, definition, this.plugin.drops().catalog(),
                problem -> this.plugin.getLogger().warning(problem + " en " + definition.id()));

        UUID teamId = ownerId != null ? this.teamLink.teamOf(ownerId).orElse(null) : null;
        var container = mob.getPersistentDataContainer();
        container.set(this.keys.definition(), PersistentDataType.STRING, definition.id());
        if (ownerId != null) {
            container.set(this.keys.owner(), PersistentDataType.STRING, ownerId.toString());
        }
        if (teamId != null) {
            container.set(this.keys.team(), PersistentDataType.STRING, teamId.toString());
        }

        // Punto de aparicion: ancla del leash y origen de la reaparicion.
        World spawnWorld = spawn.getWorld();
        if (spawnWorld != null) {
            container.set(this.keys.spawnWorld(), PersistentDataType.STRING, spawnWorld.getName());
            container.set(this.keys.spawnX(), PersistentDataType.DOUBLE, spawn.getX());
            container.set(this.keys.spawnY(), PersistentDataType.DOUBLE, spawn.getY());
            container.set(this.keys.spawnZ(), PersistentDataType.DOUBLE, spawn.getZ());
        }
        UUID spawnerId = knownSpawnerId;
        if (spawnerId == null && definition.server() && definition.respawnSeconds() > 0 && spawnWorld != null) {
            spawnerId = UUID.randomUUID();
            this.spawners.put(new SpawnerEntry(spawnerId, definition.id(), spawnWorld.getName(),
                    spawn.getX(), spawn.getY(), spawn.getZ(), definition.respawnSeconds(), 0L));
            this.spawners.save();
        }
        if (spawnerId != null) {
            container.set(this.keys.spawner(), PersistentDataType.STRING, spawnerId.toString());
        }
    }

    private void applyAttributes(Mob mob, MobDefinition definition) {
        for (Map.Entry<String, Double> entry : definition.attributes().entrySet()) {
            Attribute attribute = Attributes.resolve(entry.getKey());
            if (attribute == null) {
                this.plugin.getLogger().warning("Atributo desconocido '" + entry.getKey() + "' en " + definition.id());
                continue;
            }
            AttributeInstance instance = mob.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            instance.setBaseValue(entry.getValue());
        }

        AttributeInstance health = mob.getAttribute(Attribute.MAX_HEALTH);
        double max = health != null ? health.getBaseValue() : 20.0D;
        mob.setHealth(Math.max(1.0D, max));
    }

    /** Registra la entidad si lleva la marca de CustomMobs y aun no estaba registrada. */
    public void register(LivingEntity entity) {
        UUID entityId = entity.getUniqueId();
        if (this.active.containsKey(entityId)) {
            return;
        }
        this.read(entity).ifPresent(customMob -> {
            this.active.put(entityId, customMob);
            // Recupera el cupo si el archivo se perdio: es idempotente, repetirlo no suma dos veces.
            if (customMob.ownerId() != null) {
                this.playerMobs.add(customMob.ownerId(), entityId);
            }
            // La definicion manda: reafirmamos el nombre al cargar. Un mob guardado con el
            // nombre que le impuso otro plugin lo conservaria para siempre si no. Encima va
            // el estilo que haya elegido el dueno (color del nombre y brillo).
            if (this.styles != null) {
                this.styles.applyName(entity, customMob.definition(), customMob.ownerId(), customMob.teamId());
                this.styles.applyGlow(entity, customMob.definition(), customMob.ownerId(), customMob.teamId());
            } else {
                entity.setCustomName(customMob.definition().displayName());
                entity.setCustomNameVisible(true);
            }
            this.disguiseLink.apply(entity, customMob.definition());
            // Un mob de rol con 'ai: false' se queda quieto en su puesto.
            if (!customMob.definition().usesAi() && entity instanceof Mob mob) {
                mob.setAI(false);
            }
            this.applyBehaviour(entity);
        });
    }

    /** Reconstruye el vinculo leyendo el PersistentDataContainer. */
    public Optional<CustomMob> read(LivingEntity entity) {
        var container = entity.getPersistentDataContainer();
        String definitionId = container.get(this.keys.definition(), PersistentDataType.STRING);
        if (definitionId == null) {
            return Optional.empty();
        }
        Optional<MobDefinition> definition = this.registry.get(definitionId);
        if (definition.isEmpty()) {
            return Optional.empty();
        }
        UUID ownerId = parseUuid(container.get(this.keys.owner(), PersistentDataType.STRING));
        UUID teamId = parseUuid(container.get(this.keys.team(), PersistentDataType.STRING));
        return Optional.of(new CustomMob(entity, definition.get(), ownerId, teamId));
    }

    private void applyBehaviour(LivingEntity entity) {
        if (!(entity instanceof Mob mob) || !this.paperGoals) {
            return;
        }
        try {
            PaperGoalsApplier.apply(this, mob);
        } catch (Throwable throwable) {
            this.paperGoals = false;
            this.plugin.getLogger().info("API MobGoals no disponible ("
                    + throwable.getClass().getSimpleName() + "). Se usara el modo por eventos.");
        }
    }

    /** Restaura los mobs ya cargados al arrancar el plugin. */
    public void restoreAll() {
        int restored = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof LivingEntity living)) {
                    continue;
                }
                int before = this.active.size();
                this.register(living);
                if (this.active.size() > before) {
                    restored++;
                }
            }
        }
        this.plugin.getLogger().info("Mobs custom restaurados: " + restored);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof LivingEntity living) {
                this.register(living);
            }
        }
    }

    /** Quita del registro los mobs muertos o descargados. */
    public void prune() {
        this.active.values().removeIf(customMob -> {
            LivingEntity entity = customMob.entity();
            return !entity.isValid() || entity.isDead();
        });
        // El cupo se toca al invocar y al morir: se guarda al vuelo, no en cada cambio.
        if (this.playerMobs.isDirty()) {
            this.playerMobs.save();
        }
        if (this.styles != null && this.styles.registry().isDirty()) {
            this.styles.registry().save();
        }
    }

    // ------------------------------------------------------------------ ancla y spawner

    /** Punto de aparicion guardado en la entidad, si lo tiene. */
    public Optional<Location> spawnPointOf(CustomMob customMob) {
        var container = customMob.entity().getPersistentDataContainer();
        String worldName = container.get(this.keys.spawnWorld(), PersistentDataType.STRING);
        Double x = container.get(this.keys.spawnX(), PersistentDataType.DOUBLE);
        Double y = container.get(this.keys.spawnY(), PersistentDataType.DOUBLE);
        Double z = container.get(this.keys.spawnZ(), PersistentDataType.DOUBLE);
        if (worldName == null || x == null || y == null || z == null) {
            return Optional.empty();
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(new Location(world, x, y, z));
    }

    /** Identificador del spawner de una entidad, o {@code null} si no lo es. */
    public UUID spawnerIdOf(Entity entity) {
        String raw = entity.getPersistentDataContainer().get(this.keys.spawner(), PersistentDataType.STRING);
        return parseUuid(raw);
    }

    /** Punto de referencia del mob: su dueno, o el bloque donde aparecio. */
    public Location anchorOf(CustomMob customMob) {
        if (!customMob.definition().leash().anchoredToPoint()) {
            Player owner = this.onlineOwner(customMob);
            return owner != null ? owner.getLocation() : null;
        }
        return this.spawnPointOf(customMob).orElse(null);
    }

    /** {@code true} si el mob esta mas alla del radio maximo respecto a su ancla. */
    public boolean isBeyondLeash(CustomMob customMob, Mob mob) {
        LeashSettings leash = customMob.definition().leash();
        if (!leash.enabled()) {
            return false;
        }
        Location anchor = this.anchorOf(customMob);
        if (anchor == null) {
            return false;
        }
        if (!anchor.getWorld().getName().equals(mob.getWorld().getName())) {
            return true;
        }
        return mob.getLocation().distance(anchor) > leash.maxDistance();
    }

    /**
     * Devuelve al mob hacia su ancla cuando se aleja mas del radio configurado.
     * Si supera la distancia de teletransporte, se reubica directamente.
     */
    public void enforceLeash(CustomMob customMob, Mob mob) {
        LeashSettings leash = customMob.definition().leash();
        if (!leash.enabled()) {
            return;
        }
        Location anchor = this.anchorOf(customMob);
        if (anchor == null) {
            return;
        }

        if (!anchor.getWorld().getName().equals(mob.getWorld().getName())) {
            if (leash.teleportDistance() > 0.0D) {
                mob.teleport(anchor);
            }
            return;
        }

        double distance = mob.getLocation().distance(anchor);
        if (distance <= leash.maxDistance()) {
            return;
        }

        // Fuera de radio: suelta el objetivo y vuelve al ancla.
        mob.setTarget(null);

        if (leash.teleportDistance() > 0.0D && distance > leash.teleportDistance()) {
            mob.teleport(anchor);
            return;
        }

        try {
            mob.getPathfinder().moveTo(anchor, leash.returnSpeed());
        } catch (Throwable ignored) {
            // Servidor sin la API Pathfinder de Paper.
        }
    }

    /** Un mob de jugador que muere deja libre su cupo y limpia su brillo. */
    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (this.styles != null) {
            this.styles.clear(event.getEntity());
        }
        this.playerMobs.remove(event.getEntity().getUniqueId());
    }

    /** Elimina un mob y, si era un spawner, deja de reanimarlo. */
    public void despawn(CustomMob customMob) {
        if (this.styles != null) {
            this.styles.clear(customMob.entity());
        }
        this.playerMobs.remove(customMob.entity().getUniqueId());
        UUID spawnerId = this.spawnerIdOf(customMob.entity());
        if (spawnerId != null) {
            this.spawners.remove(spawnerId);
            this.spawners.save();
        }
        this.active.remove(customMob.entity().getUniqueId());
        customMob.entity().remove();
    }

    /** Si el mob pertenecia a un spawner, programa su reaparicion. */
    public void handleSpawnerDeath(LivingEntity entity) {
        UUID spawnerId = this.spawnerIdOf(entity);
        if (spawnerId == null) {
            return;
        }
        Optional<SpawnerEntry> entry = this.spawners.get(spawnerId);
        if (entry.isEmpty()) {
            return;
        }
        if (!entry.get().respawns()) {
            this.spawners.remove(spawnerId);
            this.spawners.save();
            return;
        }
        SpawnerEntry pending = entry.get().awaiting(
                System.currentTimeMillis() + entry.get().respawnSeconds() * 1000L);
        this.spawners.put(pending);
        this.spawners.save();
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin,
                () -> this.respawnNow(pending), entry.get().respawnSeconds() * 20L);
    }

    /**
     * Reanuda los spawners que quedaron esperando reaparicion. Los que no tienen marca
     * se dejan en paz: su mob sigue en el mundo (aunque el chunk este descargado).
     */
    public void resumeSpawners() {
        long now = System.currentTimeMillis();
        int resumed = 0;
        int scheduled = 0;
        for (SpawnerEntry entry : this.spawners.all()) {
            if (!entry.awaitingRespawn()) {
                continue;
            }
            long remaining = entry.respawnAt() - now;
            if (remaining <= 0L) {
                if (this.respawnNow(entry)) {
                    resumed++;
                }
            } else {
                this.plugin.getServer().getScheduler().runTaskLater(this.plugin,
                        () -> this.respawnNow(entry), Math.max(1L, remaining / 50L));
                scheduled++;
            }
        }
        if (resumed > 0 || scheduled > 0) {
            this.plugin.getLogger().info("Spawners pendientes: " + resumed + " reaparecidos, "
                    + scheduled + " programados.");
        }
    }

    private boolean respawnNow(SpawnerEntry entry) {
        MobDefinition definition = this.registry.get(entry.definitionId()).orElse(null);
        World world = Bukkit.getWorld(entry.world());
        if (definition == null || world == null) {
            this.plugin.getLogger().warning("Spawner sin definicion o mundo validos: " + entry.id());
            return false;
        }
        LivingEntity spawned = this.spawn(definition, new Location(world, entry.x(), entry.y(), entry.z()),
                null, entry.id());
        if (spawned != null) {
            this.spawners.put(entry.alive());
            this.spawners.save();
            this.plugin.getLogger().info("Spawner reactivado: " + definition.id() + " en " + entry.world()
                    + " (" + (int) entry.x() + ", " + (int) entry.y() + ", " + (int) entry.z() + ")");
        }
        return spawned != null;
    }

    // ------------------------------------------------------------------ bando protegido

    public AggroRegistry aggro() {
        return this.aggro;
    }

    /**
     * {@code true} si la entidad agredida forma parte del bando protegido por el mob:
     * el propio mob, su dueno, un companero o aliado suyo, o un mob del mismo bando.
     */
    public boolean isProtectedSide(CustomMob customMob, LivingEntity candidate) {
        // Defensa propia: si lo agreden a el, reacciona siempre, sin depender
        // de 'aggro-on-friendly-mobs'.
        if (customMob.entity().getUniqueId().equals(candidate.getUniqueId())) {
            return true;
        }
        if (customMob.isOwner(candidate.getUniqueId())) {
            return true;
        }
        if (customMob.definition().server()) {
            return this.isProtectedFromServerMob(customMob, candidate);
        }
        if (candidate instanceof Player player) {
            return this.teamLink.isAlly(customMob.ownerId(), player.getUniqueId());
        }
        Optional<CustomMob> other = this.find(candidate.getUniqueId());
        if (other.isEmpty() || !this.config.aggroOnFriendlyMobs()) {
            return false;
        }
        return this.isSameSide(customMob, other.get());
    }

    /**
     * Bando de un mob de servidor: los jugadores si es defensor, y los mobs de su
     * misma faccion o de una aliada.
     */
    private boolean isProtectedFromServerMob(CustomMob customMob, LivingEntity candidate) {
        if (candidate instanceof Player) {
            return customMob.definition().attitude() == Attitude.DEFENDER;
        }
        Optional<CustomMob> other = this.find(candidate.getUniqueId());
        if (other.isEmpty()) {
            return false;
        }
        String mine = customMob.definition().faction();
        if (mine == null) {
            return false;
        }
        String theirs = other.get().definition().faction();
        if (mine.equals(theirs)) {
            return true;
        }
        return theirs != null && this.factions.isAlly(mine, theirs);
    }

    private boolean isSameSide(CustomMob first, CustomMob second) {
        if (first.ownerId() == null || second.ownerId() == null) {
            return first.teamId() != null && first.teamId().equals(second.teamId());
        }
        if (first.isOwner(second.ownerId())) {
            return true;
        }
        return this.teamLink.isAlly(first.ownerId(), second.ownerId());
    }

    private Player onlineOwner(CustomMob customMob) {
        if (customMob.ownerId() == null) {
            return null;
        }
        Player owner = this.plugin.getServer().getPlayer(customMob.ownerId());
        if (owner == null || !owner.isOnline() || owner.isDead()) {
            return null;
        }
        return owner;
    }

    public void clear() {
        this.active.clear();
    }

    private static UUID parseUuid(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

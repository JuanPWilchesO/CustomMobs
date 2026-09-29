package com.juanp.custommobs.command;

import com.juanp.custommobs.CustomMobsPlugin;
import com.juanp.custommobs.mob.CustomMob;
import com.juanp.custommobs.mob.MobDefinition;
import org.bukkit.Location;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** /custommobs reload | list | give | enchants */
public final class CustomMobsCommand implements CommandExecutor, TabCompleter {

    private final CustomMobsPlugin plugin;

    public CustomMobsCommand(CustomMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        // Lo unico que un jugador raso puede hacer por su cuenta es ver SU propio cupo.
        boolean ownQuota = args.length == 1 && "cuota".equalsIgnoreCase(args[0]);
        String needed = ownQuota ? this.plugin.config().playerPermission()
                : this.plugin.config().adminPermission();
        if (!sender.hasPermission(needed)) {
            sender.sendMessage("[CustomMobs] No tienes permiso (" + needed + ").");
            return true;
        }
        if (args.length == 0) {
            this.help(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                this.plugin.reloadAll();
                sender.sendMessage("[CustomMobs] Recargado. Definiciones: " + this.plugin.registry().size());
            }
            case "list" -> {
                sender.sendMessage("[CustomMobs] Definiciones (" + this.plugin.registry().size() + "):");
                for (MobDefinition definition : this.plugin.registry().all()) {
                    sender.sendMessage(" - " + definition.id() + " (" + definition.entityType().name()
                            + ", " + definition.category().name().toLowerCase(Locale.ROOT) + ")");
                }
            }
            case "give" -> this.give(sender, args);
            case "spawn" -> this.spawn(sender, args);
            case "remove" -> this.remove(sender, args);
            case "active" -> this.active(sender);
            case "cuota" -> this.quota(sender, args);
            case "kill" -> this.kill(sender, args);
            case "enchants" -> this.enchants(sender, args);
            default -> this.help(sender);
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("[CustomMobs] Uso: /" + "custommobs give <id> [jugador]");
            return;
        }
        MobDefinition definition = this.plugin.registry().get(args[1]).orElse(null);
        if (definition == null) {
            sender.sendMessage("[CustomMobs] No existe la definicion '" + args[1] + "'.");
            return;
        }

        Player target;
        if (args.length >= 3) {
            target = this.plugin.getServer().getPlayerExact(args[2]);
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            target = null;
        }
        if (target == null) {
            sender.sendMessage("[CustomMobs] Jugador no encontrado.");
            return;
        }

        ItemStack egg = this.plugin.craft().createEgg(definition);
        if (egg == null) {
            sender.sendMessage("[CustomMobs] '" + definition.id() + "' no define huevo; usa /custommobs spawn.");
            return;
        }
        target.getInventory().addItem(egg);
        sender.sendMessage("[CustomMobs] Huevo '" + definition.id() + "' entregado a " + target.getName() + ".");
    }

    private void spawn(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("[CustomMobs] Uso: /custommobs spawn <id> [mundo x y z]");
            return;
        }
        MobDefinition definition = this.plugin.registry().get(args[1]).orElse(null);
        if (definition == null) {
            sender.sendMessage("[CustomMobs] No existe la definicion '" + args[1] + "'.");
            return;
        }

        Location location;
        if (args.length >= 6) {
            World world = this.plugin.getServer().getWorld(args[2]);
            if (world == null) {
                sender.sendMessage("[CustomMobs] Mundo no encontrado: " + args[2]);
                return;
            }
            try {
                location = new Location(world, Double.parseDouble(args[3]),
                        Double.parseDouble(args[4]), Double.parseDouble(args[5]));
            } catch (NumberFormatException ex) {
                sender.sendMessage("[CustomMobs] Coordenadas invalidas.");
                return;
            }
        } else if (sender instanceof Player player) {
            location = player.getLocation();
        } else {
            sender.sendMessage("[CustomMobs] Desde consola hace falta: /custommobs spawn <id> <mundo> <x> <y> <z>");
            return;
        }

        Player owner = sender instanceof Player player && !definition.server() ? player : null;
        LivingEntity spawned = this.plugin.mobs().spawn(definition, location, owner);
        if (spawned == null) {
            sender.sendMessage("[CustomMobs] No se pudo invocar '" + definition.id() + "'.");
            return;
        }
        sender.sendMessage("[CustomMobs] Invocado '" + definition.id() + "' (" + definition.category().name().toLowerCase(Locale.ROOT)
                + ") en " + location.getWorld().getName() + " " + location.getBlockX() + " "
                + location.getBlockY() + " " + location.getBlockZ() + ".");
    }

    /** Lista los mobs custom vivos, con su posicion y si pertenecen a un spawner. */
    /**
     * Consulta el cupo de mobs de jugador y permite corregirlo.
     *
     * <p>El cupo vive en disco porque incluye los mobs anclados a un bloque que ahora
     * mismo estan en un chunk descargado. Si uno desaparece sin avisar (un plugin
     * externo, un corte), queda contado de mas:
     * <ul>
     *   <li>{@code recontar} rehace la cuenta y no toca ningun mob.</li>
     *   <li>{@code retirar} ademas mata los mobs que alcanza.</li>
     * </ul>
     */
    private void quota(CommandSender sender, String[] args) {
        // 'liberar' se acepta como sinonimo por costumbre, pero el nombre bueno es
        // 'recontar': el verbo no libera nada, solo rehace la cuenta.
        boolean libera = args.length >= 3
                && ("recontar".equalsIgnoreCase(args[1]) || "liberar".equalsIgnoreCase(args[1]));
        boolean retira = args.length >= 3 && "retirar".equalsIgnoreCase(args[1]);
        if (libera || retira) {
            Player target = this.plugin.getServer().getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage("[CustomMobs] El jugador " + args[2] + " debe estar conectado para tocarle el cupo.");
                return;
            }
            if (libera) {
                int ghosts = this.plugin.mobs().resyncPlayerMobs(target.getUniqueId());
                int used = this.plugin.mobs().countPlayerMobs(target.getUniqueId());
                int max = this.plugin.mobs().limitOf(target.getUniqueId());
                sender.sendMessage("[CustomMobs] Cupo de " + target.getName() + " recontado: " + ghosts
                        + " entradas fantasma descartadas. No se toco ningun mob.");
                sender.sendMessage("[CustomMobs] Quedan " + used
                        + (max > 0 ? " de " + max + " mobs" : " mobs") + ".");
                if (max > 0 && used >= max) {
                    sender.sendMessage("[CustomMobs] Sigue en el tope: sus mobs existen, asi que no puede invocar mas.");
                    sender.sendMessage("[CustomMobs] Para liberar cupo de verdad hay que retirarlos:"
                            + " /custommobs cuota retirar " + target.getName());
                } else if (used > 0) {
                    sender.sendMessage("[CustomMobs] Los anclados en chunks descargados volveran a contar cuando carguen.");
                }
                return;
            }
            var result = this.plugin.mobs().purgePlayerMobs(target.getUniqueId());
            sender.sendMessage("[CustomMobs] Cupo de " + target.getName() + " a cero. Mobs retirados: "
                    + result.removed() + ".");
            if (result.unreachable() > 0) {
                sender.sendMessage("[CustomMobs] " + result.unreachable()
                        + " siguen anclados en chunks descargados: no se pueden retirar hasta que carguen,"
                        + " y volveran a contar solos cuando eso pase.");
            }
            return;
        }

        UUID ownerId;
        String label;
        if (args.length >= 2) {
            Player target = this.plugin.getServer().getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("[CustomMobs] Jugador no encontrado: " + args[1]);
                return;
            }
            ownerId = target.getUniqueId();
            label = target.getName();
        } else if (sender instanceof Player player) {
            ownerId = player.getUniqueId();
            label = player.getName();
        } else {
            sender.sendMessage("[CustomMobs] Uso: /custommobs cuota [jugador]");
            sender.sendMessage("[CustomMobs]      /custommobs cuota recontar <jugador>  (solo rehace la cuenta, no toca mobs)");
            sender.sendMessage("[CustomMobs]      /custommobs cuota retirar <jugador>  (mata sus mobs y deja el cupo en cero)");
            return;
        }

        int max = this.plugin.mobs().limitOf(ownerId);
        int used = this.plugin.mobs().countPlayerMobs(ownerId);
        sender.sendMessage("[CustomMobs] " + label + ": " + used
                + (max > 0 ? " de " + max + " mobs" : " mobs (sin limite)"));
        int loaded = 0;
        for (CustomMob customMob : this.plugin.mobs().active()) {
            if (!ownerId.equals(customMob.ownerId())) {
                continue;
            }
            loaded++;
            var location = customMob.entity().getLocation();
            sender.sendMessage(" - " + customMob.definition().id() + " en "
                    + location.getWorld().getName() + " " + location.getBlockX() + " "
                    + location.getBlockY() + " " + location.getBlockZ());
        }
        if (used > loaded) {
            sender.sendMessage("[CustomMobs] " + (used - loaded)
                    + " mas en chunks descargados (anclados a su bloque).");
        }
    }

    /** Nombre del dueno, aunque este desconectado. */
    private String ownerName(UUID ownerId) {
        String name = this.plugin.getServer().getOfflinePlayer(ownerId).getName();
        return name != null ? name : ownerId.toString().substring(0, 8);
    }

    private void active(CommandSender sender) {
        var mobs = this.plugin.mobs().active();
        sender.sendMessage("[CustomMobs] Mobs activos: " + mobs.size());
        for (CustomMob customMob : mobs) {
            var entity = customMob.entity();
            var location = entity.getLocation();
            String spawner = this.plugin.mobs().spawnerIdOf(entity) != null ? " [spawner]" : "";
            String disguised = this.plugin.mobs().disguiseLink().isDisguised(entity) ? " [disfrazado]" : "";
            String owner = customMob.ownerId() == null ? "" : " [dueno: "
                    + this.ownerName(customMob.ownerId()) + "]";
            sender.sendMessage(" - " + customMob.definition().id() + spawner + disguised + owner + " en "
                    + location.getWorld().getName() + " " + location.getBlockX() + " "
                    + location.getBlockY() + " " + location.getBlockZ()
                    + " | nombre='" + entity.getCustomName() + "'");
        }
    }

    /**
     * Mata los mobs custom de una definicion en todo el servidor. Si pertenecen a un
     * spawner, volveran a aparecer; para eliminarlos de raiz usa 'remove'.
     */
    private void kill(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("[CustomMobs] Uso: /custommobs kill <id>");
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        int killed = 0;
        for (CustomMob customMob : this.plugin.mobs().active()) {
            if (!customMob.definition().id().equals(id)) {
                continue;
            }
            customMob.entity().setHealth(0.0D);
            killed++;
        }
        sender.sendMessage("[CustomMobs] Eliminados " + killed + " mobs de '" + id + "'.");
    }

    /** Borra los mobs custom cercanos al emisor. Util para desmontar escenas. */
    private void remove(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[CustomMobs] 'remove' solo funciona desde el juego.");
            return;
        }
        double radius = 5.0D;
        if (args.length >= 2) {
            try {
                radius = Math.max(1.0D, Double.parseDouble(args[1]));
            } catch (NumberFormatException ex) {
                sender.sendMessage("[CustomMobs] Radio invalido.");
                return;
            }
        }

        int removed = 0;
        for (CustomMob customMob : this.plugin.mobs().active()) {
            var entity = customMob.entity();
            if (!entity.isValid() || !entity.getWorld().equals(player.getWorld())) {
                continue;
            }
            if (entity.getLocation().distance(player.getLocation()) > radius) {
                continue;
            }
            this.plugin.mobs().despawn(customMob);
            removed++;
        }
        sender.sendMessage("[CustomMobs] Eliminados " + removed + " mobs custom en un radio de "
                + (int) radius + " bloques.");
    }

    /** Lista las claves de encantamiento disponibles (incluye las de ExcellentEnchants). */
    private void enchants(CommandSender sender, String[] args) {
        String filter = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
        int shown = 0;
        for (Enchantment enchantment : Registry.ENCHANTMENT) {
            String key = enchantment.getKey().toString();
            if (!filter.isEmpty() && !key.contains(filter)) {
                continue;
            }
            sender.sendMessage(" - " + key);
            if (++shown >= 60) {
                sender.sendMessage("... (afina con /custommobs enchants <texto>)");
                break;
            }
        }
        sender.sendMessage("[CustomMobs] Mostrados: " + shown + ". Usa la clave completa en 'enchants:' del yml.");
    }

    private void help(CommandSender sender) {
        sender.sendMessage("[CustomMobs] /custommobs reload | list | give <id> [jugador] | spawn <id> [mundo x y z]");
        sender.sendMessage("[CustomMobs]           | remove [radio] | active | kill <id> | enchants [filtro]");
        sender.sendMessage("[CustomMobs]           | cuota [jugador] | cuota recontar <jugador> | cuota retirar <jugador>");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String option : List.of("reload", "list", "give", "spawn", "remove", "active", "kill", "enchants", "cuota")) {
                if (option.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(option);
                }
            }
            return out;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("spawn")
                || args[0].equalsIgnoreCase("kill"))) {
            for (MobDefinition definition : this.plugin.registry().all()) {
                if (definition.id().startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(definition.id());
                }
            }
        }
        return out;
    }
}

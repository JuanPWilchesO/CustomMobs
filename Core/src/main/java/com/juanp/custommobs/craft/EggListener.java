package com.juanp.custommobs.craft;

import com.juanp.custommobs.CustomMobsPlugin;
import com.juanp.custommobs.mob.MobDefinition;
import com.juanp.custommobs.mob.MobService;
import com.juanp.custommobs.mob.PlayerMobRegistry;
import com.juanp.custommobs.mob.Texts;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

/**
 * El huevo custom como ficha de un mob.
 *
 * <p>Un huevo recien crafteado no esta ligado a nada: al colocarlo nace un mob y el huevo
 * queda ligado a el. Desde entonces el huevo <b>no se gasta</b> — representa a ese mob,
 * sirve para recogerlo y lo vuelve a desplegar.
 *
 * <p>Si el mob muere, el vinculo se borra y el huevo queda inerte: apunta a la nada, asi que
 * colocarlo no hace nada. No hay que ir a buscarlo por el mundo para desactivarlo.
 */
public final class EggListener implements Listener {

    private final CustomMobsPlugin plugin;
    private final CraftService craft;
    private final MobService service;

    public EggListener(CustomMobsPlugin plugin, CraftService craft, MobService service) {
        this.plugin = plugin;
        this.craft = craft;
        this.service = service;
    }

    /** Colocar el huevo: despliega el mob, o lo vuelve a traer si estaba guardado. */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) {
            return;
        }
        ItemStack item = event.getItem();
        Optional<MobDefinition> definition = this.craft.definitionOf(item);
        if (definition.isEmpty()) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        // Los mundos excluidos valen para TODO: tampoco se invoca desde el huevo. El
        // chequeo de 'spawn' llegaba tarde y con un mensaje enganoso; aqui se dice claro.
        if (!this.service.worldEnabled(player.getWorld())) {
            player.sendMessage(Texts.color("&cEl plugin no funciona en este mundo: no puedes invocar mobs aqui."));
            return;
        }
        if (!player.hasPermission(this.service.config().playerPermission())) {
            player.sendMessage(Texts.color("&cNo tienes permiso para usar los huevos de CustomMobs."));
            return;
        }

        Optional<UUID> bound = this.craft.linkOf(item);
        if (bound.isPresent()) {
            PlayerMobRegistry.MobLink link = this.service.playerMobs().byLink(bound.get()).orElse(null);
            if (link == null) {
                player.sendMessage(Texts.color("&cEste huevo ya no sirve: el mob que representaba ya no existe."));
                return;
            }
            if (link.deployed()) {
                player.sendMessage(Texts.color("&eEse mob ya esta desplegado. Golpealo con este huevo para recogerlo."));
                return;
            }
        }

        if (this.service.atPlayerLimit(player.getUniqueId())) {
            player.sendMessage(Texts.color("&cYa tienes " + this.service.limitOf(player.getUniqueId())
                    + " mobs desplegados, tu maximo. Recoge alguno con su huevo."));
            return;
        }

        Optional<PlayerMobRegistry.MobLink> used = this.service.deploy(definition.get(),
                this.spawnLocation(event, player), player, bound.orElse(null));
        if (used.isEmpty()) {
            player.sendMessage(Texts.color("&cNo se pudo invocar el mob."));
            return;
        }
        if (bound.isEmpty()) {
            // Huevo nuevo: se liga al mob que acaba de nacer. No se gasta.
            this.craft.bind(item, used.get().linkId());
            player.sendMessage(Texts.color("&aHuevo vinculado a tu mob. Golpealo con el para recogerlo."));
        } else {
            player.sendMessage(Texts.color("&aMob desplegado de nuevo."));
        }
    }

    /** Golpear el mob con su propio huevo: se recoge y vuelve dentro del huevo. */
    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (this.craft.definitionOf(item).isEmpty()) {
            return;
        }
        Optional<UUID> bound = this.craft.linkOf(item);
        if (bound.isEmpty()) {
            return;
        }
        PlayerMobRegistry.MobLink link = this.service.playerMobs().byLink(bound.get()).orElse(null);
        if (link == null || !link.deployed()) {
            return;
        }
        if (!link.entityId().equals(event.getRightClicked().getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        if (this.service.store(link.linkId())) {
            event.getPlayer().sendMessage(Texts.color("&aMob recogido. Vuelve a colocar el huevo cuando quieras."));
        }
    }

    private Location spawnLocation(PlayerInteractEvent event, Player player) {
        Block clicked = event.getClickedBlock();
        if (clicked == null || event.getBlockFace() == null) {
            return player.getLocation();
        }
        return clicked.getRelative(event.getBlockFace()).getLocation().add(0.5D, 0.0D, 0.5D);
    }
}

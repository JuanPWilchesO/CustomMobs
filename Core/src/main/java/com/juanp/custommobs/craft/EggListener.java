package com.juanp.custommobs.craft;

import com.juanp.custommobs.CustomMobsPlugin;
import com.juanp.custommobs.mob.MobDefinition;
import com.juanp.custommobs.mob.MobService;
import com.juanp.custommobs.mob.Texts;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/** Convierte el uso del huevo custom en la invocacion del soldado. */
public final class EggListener implements Listener {

    private final CustomMobsPlugin plugin;
    private final CraftService craft;
    private final MobService service;

    public EggListener(CustomMobsPlugin plugin, CraftService craft, MobService service) {
        this.plugin = plugin;
        this.craft = craft;
        this.service = service;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) {
            return;
        }

        Optional<MobDefinition> definition = this.craft.definitionOf(event.getItem());
        if (definition.isEmpty()) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!player.hasPermission(this.service.config().playerPermission())) {
            player.sendMessage(Texts.color("&cNo tienes permiso para usar los huevos de CustomMobs."));
            return;
        }
        if (this.service.atPlayerLimit(player.getUniqueId())) {
            player.sendMessage(Texts.color("&cYa tienes " + this.service.limitOf(player.getUniqueId())
                    + " mobs activos, tu maximo. Espera a que caiga alguno."));
            return;
        }
        LivingEntity spawned = this.service.spawn(definition.get(), this.spawnLocation(event, player), player);
        if (spawned == null) {
            player.sendMessage(Texts.color("&cNo se pudo invocar el mob."));
            return;
        }
        if (this.service.config().consumeEgg()) {
            this.consume(player);
        }
    }

    private Location spawnLocation(PlayerInteractEvent event, Player player) {
        Block clicked = event.getClickedBlock();
        if (clicked == null || event.getBlockFace() == null) {
            return player.getLocation();
        }
        return clicked.getRelative(event.getBlockFace()).getLocation().add(0.5D, 0.0D, 0.5D);
    }

    private void consume(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            return;
        }
        hand.setAmount(hand.getAmount() - 1);
    }
}

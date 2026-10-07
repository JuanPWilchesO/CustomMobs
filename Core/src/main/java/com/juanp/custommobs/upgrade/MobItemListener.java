package com.juanp.custommobs.upgrade;

import com.juanp.custommobs.CustomMobsPlugin;
import com.juanp.custommobs.book.MobBookService;
import com.juanp.custommobs.mob.CustomMob;
import com.juanp.custommobs.mob.MobService;
import com.juanp.custommobs.mob.Texts;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * El libro de inspeccion y los items de mejora.
 *
 * <p>El de mejora se aplica <b>haciendo clic derecho sobre un mob propio</b> con el item en
 * la mano, y se consume. Si el mob no es tuyo, no se gasta.
 */
public final class MobItemListener implements Listener {

    private final CustomMobsPlugin plugin;
    private final MobService service;
    private final UpgradeService upgrades;
    private final MobBookService book;

    public MobItemListener(CustomMobsPlugin plugin, MobService service,
                           UpgradeService upgrades, MobBookService book) {
        this.plugin = plugin;
        this.service = service;
        this.upgrades = upgrades;
        this.book = book;
    }

    /** El libro se abre desde cualquier clic derecho. */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!this.book.isBook(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        this.book.open(event.getPlayer());
    }

    /** Con el libro en la mano apuntando a un mob, tambien se abre. */
    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (this.book.isBook(item)) {
            event.setCancelled(true);
            this.book.open(player);
            return;
        }

        Optional<UpgradeSpec> spec = this.upgrades.specOf(item);
        if (spec.isEmpty()) {
            return;
        }
        CustomMob customMob = this.service.find(event.getRightClicked().getUniqueId()).orElse(null);
        if (customMob == null) {
            return;
        }
        if (!player.getUniqueId().equals(customMob.ownerId())) {
            player.sendMessage(Texts.color("&cEse mob no es tuyo."));
            return;
        }
        event.setCancelled(true);
        this.upgrades.applyTo(customMob, spec.get());
        item.setAmount(item.getAmount() - 1);
        player.sendMessage(Texts.color("&aMejora aplicada a " + customMob.definition().displayName()
                + "&a: &f" + UpgradeService.describe(spec.get())));
    }
}

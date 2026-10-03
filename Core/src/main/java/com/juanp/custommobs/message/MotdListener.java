package com.juanp.custommobs.message;

import com.juanp.custommobs.CustomMobsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Manda el MOTD configurado al jugador que entra.
 *
 * <p>Se engancha con prioridad MONITOR para no interferir con nadie que cancele o
 * modifique el evento de entrada: el MOTD es informativo, no condicional.
 */
public final class MotdListener implements Listener {

    private final CustomMobsPlugin plugin;

    public MotdListener(CustomMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        var config = this.plugin.config();
        if (!config.motdEnabled() || config.motd().isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        for (String line : config.motd()) {
            player.sendMessage(this.fill(line, player));
        }
    }

    /** Sustituye los marcadores disponibles en cada linea. */
    private String fill(String line, Player player) {
        return line
                .replace("{jugador}", player.getName())
                .replace("{online}", String.valueOf(this.plugin.getServer().getOnlinePlayers().size()))
                .replace("{max}", String.valueOf(this.plugin.getServer().getMaxPlayers()));
    }
}

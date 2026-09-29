package com.juanp.custommobs.disguise;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/** Elige el enlace de disfraces segun si LibsDisguises esta instalado y activo. */
public final class DisguiseLinkResolver {

    private DisguiseLinkResolver() {
    }

    public static DisguiseLink resolve(Plugin plugin) {
        Plugin libsDisguises = Bukkit.getPluginManager().getPlugin("LibsDisguises");
        if (libsDisguises != null && libsDisguises.isEnabled()) {
            try {
                return new LibsDisguisesLink(plugin);
            } catch (Throwable throwable) {
                plugin.getLogger().warning("LibsDisguises presente pero no se pudo enlazar ("
                        + throwable.getClass().getSimpleName() + "). Se seguira sin disfraces.");
            }
        }
        return new NoDisguiseLink();
    }
}

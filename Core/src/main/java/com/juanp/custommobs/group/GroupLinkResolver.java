package com.juanp.custommobs.group;

import net.luckperms.api.LuckPerms;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/** Elige la implementacion de grupos segun si LuckPerms esta instalado. */
public final class GroupLinkResolver {

    private GroupLinkResolver() {
    }

    public static GroupLink resolve(JavaPlugin plugin) {
        try {
            RegisteredServiceProvider<LuckPerms> provider =
                    plugin.getServer().getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null && provider.getProvider() != null) {
                return new LuckPermsGroupLink(provider.getProvider());
            }
        } catch (Throwable ex) {
            // La API es opcional: si falla, el plugin debe seguir funcionando.
            plugin.getLogger().warning("LuckPerms esta presente pero su API no respondio ("
                    + ex.getMessage() + "). Se usara el cupo por defecto.");
        }
        return new NoGroupLink();
    }
}

package com.juanp.custommobs.team;

import com.hites.godxteam.api.TeamsAPI;
import org.bukkit.plugin.Plugin;

/**
 * Elige la implementacion de {@link TeamLink} segun lo que haya en el servidor.
 *
 * <p>Las clases de la API de Teams solo se resuelven si el plugin esta presente;
 * por eso la referencia va dentro del metodo y detras de un try/catch.
 */
public final class TeamLinkResolver {

    private TeamLinkResolver() {
    }

    public static TeamLink resolve(Plugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Teams") == null) {
            return new SoloTeamLink();
        }
        try {
            TeamsAPI api = plugin.getServer().getServicesManager().load(TeamsAPI.class);
            if (api == null) {
                plugin.getLogger().warning("Teams esta presente pero no registro su API. Modo solo.");
                return new SoloTeamLink();
            }
            return new TeamsTeamLink(api);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("No se pudo enganchar la API de Teams ("
                    + throwable.getClass().getSimpleName() + "). Modo solo.");
            return new SoloTeamLink();
        }
    }
}

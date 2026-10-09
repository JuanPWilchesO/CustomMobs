package com.juanp.custommobs.region;

import com.juanp.custommobs.CustomMobsPlugin;
import org.bukkit.Location;

import java.util.List;

/**
 * Decide si un punto cae dentro de las regiones que el mob permite para aparecer.
 *
 * <p>La integracion con WorldGuard es <b>opcional</b>: sin el plugin, todo punto vale. Los
 * enganches con WorldGuard viven en {@link WorldGuardRegionGate}, que solo se carga si el
 * plugin esta instalado; asi el plugin no revienta cuando no lo esta.
 */
public interface RegionGate {

    /**
     * {@code true} si el punto sirve para este mob.
     *
     * @param allowedRegion  region donde SI puede aparecer; vacia = en cualquier parte
     * @param deniedRegions  regiones donde NO puede aparecer; vacia = ninguna vetada
     */
    boolean allows(Location location, String allowedRegion, List<String> deniedRegions);

    /** Un cerco sin reglas: todo vale. Es el que se usa cuando WorldGuard no esta. */
    RegionGate OPEN = (location, allowedRegion, deniedRegions) -> true;

    /** El cerco que toca: el de WorldGuard si esta instalado, y si no uno abierto. */
    static RegionGate create(CustomMobsPlugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") == null) {
            return OPEN;
        }
        try {
            return new WorldGuardRegionGate();
        } catch (Throwable failure) {
            plugin.getLogger().warning("WorldGuard esta instalado pero no se pudo enganchar ("
                    + failure.getClass().getSimpleName() + "); el spawneo aleatorio ignorara"
                    + " las regiones.");
            return OPEN;
        }
    }
}

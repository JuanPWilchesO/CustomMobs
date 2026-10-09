package com.juanp.custommobs.region;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * El cerco de regiones, apoyado en WorldGuard.
 *
 * <p>Solo se carga si WorldGuard esta instalado: por eso vive aparte de {@link RegionGate},
 * que es lo unico que el resto del plugin toca.
 *
 * <p>Reglas:
 * <ol>
 *   <li>Si el punto cae en varias regiones <b>nombradas</b> por el mob y se contradicen —una
 *       permitida y otra vetada— <b>manda la de mayor prioridad de WorldGuard</b>. Asi manda
 *       la regla mas especifica, que es como se piensan las regiones.</li>
 *   <li>Si no cae en ninguna region nombrada: se permite solo si el mob no se limita a una
 *       region. Con {@code allowed-region} puesta, fuera de ella no aparece.</li>
 * </ol>
 */
final class WorldGuardRegionGate implements RegionGate {

    @Override
    public boolean allows(Location location, String allowedRegion, List<String> deniedRegions) {
        boolean limited = allowedRegion != null && !allowedRegion.isBlank();
        if (!limited && deniedRegions.isEmpty()) {
            return true;
        }
        String allowed = limited ? allowedRegion.trim().toLowerCase(Locale.ROOT) : "";
        Set<String> denied = deniedRegions.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(name -> name.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        RegionManager regions = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(location.getWorld()));
        if (regions == null) {
            return !limited;
        }
        ApplicableRegionSet applicable =
                regions.getApplicableRegions(BukkitAdapter.asBlockVector(location));

        ProtectedRegion winner = null;
        for (ProtectedRegion region : applicable) {
            String id = region.getId().toLowerCase(Locale.ROOT);
            if (!id.equals(allowed) && !denied.contains(id)) {
                continue;
            }
            if (winner == null || region.getPriority() > winner.getPriority()) {
                winner = region;
            }
        }
        if (winner != null) {
            return winner.getId().toLowerCase(Locale.ROOT).equals(allowed);
        }
        return !limited;
    }
}

package com.juanp.custommobs.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro de hostilidad ganada por agresion.
 *
 * <p>Es lo que sostiene el modo {@link PlayerTargetMode#DEFENSIVE}: el mob es neutral
 * hasta que alguien ataca a su bando, y entonces guarda a ese jugador como objetivo
 * durante un tiempo limitado.
 */
public final class AggroRegistry {

    private final Map<UUID, Map<UUID, Long>> entries = new ConcurrentHashMap<>();

    /** Marca al jugador como hostil para ese mob durante {@code durationMillis}. */
    public void mark(UUID mobId, UUID playerId, long durationMillis) {
        this.entries.computeIfAbsent(mobId, key -> new ConcurrentHashMap<>())
                .put(playerId, System.currentTimeMillis() + durationMillis);
    }

    public boolean isHostile(UUID mobId, UUID playerId) {
        Map<UUID, Long> mobEntries = this.entries.get(mobId);
        if (mobEntries == null) {
            return false;
        }
        Long expiry = mobEntries.get(playerId);
        if (expiry == null) {
            return false;
        }
        if (expiry < System.currentTimeMillis()) {
            mobEntries.remove(playerId);
            return false;
        }
        return true;
    }

    public void forget(UUID mobId) {
        this.entries.remove(mobId);
    }

    /** Limpia marcas vencidas y mobs sin marcas. */
    public void prune() {
        long now = System.currentTimeMillis();
        this.entries.values().forEach(mobEntries -> mobEntries.values().removeIf(expiry -> expiry < now));
        this.entries.values().removeIf(Map::isEmpty);
    }
}

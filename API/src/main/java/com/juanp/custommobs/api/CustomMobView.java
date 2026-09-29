package com.juanp.custommobs.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Vista inmutable de un mob custom.
 *
 * @param definitionId id de la definicion en {@code mobs/}
 * @param ownerId      UUID del jugador que lo invoco
 * @param teamId       UUID del team al que pertenecia el dueno al invocarlo
 * @param displayName  nombre visible, ya con codigos de color
 */
public record CustomMobView(String definitionId, UUID ownerId, Optional<UUID> teamId, String displayName) {
}

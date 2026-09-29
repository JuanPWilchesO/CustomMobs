package com.juanp.custommobs.mob;

import org.bukkit.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Un mob custom vivo: la entidad mas su vinculo de propiedad.
 *
 * @param entity     entidad gestionada
 * @param definition definicion de origen
 * @param ownerId    UUID del jugador que lo invoco (puede ser nulo si el dato se perdio)
 * @param teamId     UUID del team del dueno al invocarlo (puede ser nulo)
 */
public record CustomMob(LivingEntity entity, MobDefinition definition, UUID ownerId, UUID teamId) {

    public Optional<UUID> teamIdOptional() {
        return Optional.ofNullable(this.teamId);
    }

    public boolean isOwner(UUID candidate) {
        return this.ownerId != null && this.ownerId.equals(candidate);
    }
}

package com.juanp.custommobs.team;

import java.util.Optional;
import java.util.UUID;

/**
 * Abstrae el plugin de Teams. Hay dos implementaciones: una que consulta la API real
 * de Teams y otra de respaldo cuando Teams no esta instalado.
 */
public interface TeamLink {

    /** Nombre corto de la implementacion, para logs. */
    String name();

    /** {@code true} si el plugin de Teams esta presente y su API respondio. */
    boolean isAvailable();

    /** UUID del team de un jugador, si tiene. */
    Optional<UUID> teamOf(UUID player);

    /** {@code true} si ambos jugadores estan en el mismo team o en teams aliados. */
    boolean isAlly(UUID playerA, UUID playerB);

    /** {@code true} si el jugador pertenece a un team enemigo del team indicado. */
    boolean isEnemyTeam(UUID teamId, UUID player);

    /**
     * Jefe del team del jugador, si tiene team. Es quien decide los ajustes del team
     * (por ejemplo, el color de sus player mobs).
     */
    Optional<UUID> ownerOf(UUID player);
}

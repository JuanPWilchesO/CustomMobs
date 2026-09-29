package com.juanp.custommobs.spawner;

import java.util.UUID;

/**
 * Un punto de aparicion que revive al mob tras morir.
 *
 * <p>El estado es explicito: {@code respawnAt} en 0 significa que el mob existe (vive
 * en el mundo, aunque su chunk este descargado); un valor mayor es la marca de tiempo
 * en la que toca reaparecer. Sin esa marca, un reinicio del servidor no podria
 * distinguir "el mob murio" de "el chunk esta descargado" y duplicaria mobs.
 *
 * @param id              identificador del spawner (distinto del UUID de la entidad)
 * @param definitionId    definicion que se invoca
 * @param world           mundo del bloque
 * @param x               coordenada X
 * @param y               coordenada Y
 * @param z               coordenada Z
 * @param respawnSeconds  segundos hasta reaparecer; {@code <= 0} = una sola vida
 * @param respawnAt       instante (epoch ms) de la reaparicion pendiente; 0 = mob vivo
 */
public record SpawnerEntry(UUID id, String definitionId, String world,
                           double x, double y, double z, int respawnSeconds, long respawnAt) {

    public boolean respawns() {
        return this.respawnSeconds > 0;
    }

    /** {@code true} si el mob murio y esta esperando su turno. */
    public boolean awaitingRespawn() {
        return this.respawnAt > 0L;
    }

    /** Copia marcada como pendiente de reaparecer en {@code at}. */
    public SpawnerEntry awaiting(long at) {
        return new SpawnerEntry(this.id, this.definitionId, this.world,
                this.x, this.y, this.z, this.respawnSeconds, at);
    }

    /** Copia marcada como viva (mob presente en el mundo). */
    public SpawnerEntry alive() {
        return new SpawnerEntry(this.id, this.definitionId, this.world,
                this.x, this.y, this.z, this.respawnSeconds, 0L);
    }
}

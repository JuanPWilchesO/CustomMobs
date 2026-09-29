package com.juanp.custommobs.mob;

/**
 * Un sonido configurable de un mob.
 *
 * @param sound  clave del sonido, tal como en {@code entity.zombie.ambient}
 * @param volume volumen (0.0 en adelante)
 * @param pitch  tono (0.5 a 2.0 recomendado)
 */
public record SoundSpec(String sound, float volume, float pitch) {
}

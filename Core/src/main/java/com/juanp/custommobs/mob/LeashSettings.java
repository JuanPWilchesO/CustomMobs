package com.juanp.custommobs.mob;

/**
 * Limite de separacion respecto al ancla del mob, configurable por definicion.
 *
 * @param maxDistance      radio maximo en bloques; {@code 0} = sin limite
 * @param teleportDistance distancia a partir de la cual se teletransporta al ancla; {@code 0} = nunca
 * @param returnSpeed      velocidad del retorno (multiplicador del pathfinder)
 * @param anchor           referencia: el dueno ({@link AnchorMode#OWNER}) o el bloque de aparicion
 *                         ({@link AnchorMode#POINT}). Los mobs de servidor siempre usan POINT.
 */
public record LeashSettings(double maxDistance, double teleportDistance, double returnSpeed,
                            AnchorMode anchor) {

    public static final LeashSettings NONE =
            new LeashSettings(0.0D, 0.0D, 1.0D, AnchorMode.DEFAULT);

    /** {@code true} si hay radio maximo configurado. */
    public boolean enabled() {
        return this.maxDistance > 0.0D;
    }

    /** {@code true} si el mob se queda cerca del bloque donde aparecio. */
    public boolean anchoredToPoint() {
        return this.anchor == AnchorMode.POINT;
    }
}

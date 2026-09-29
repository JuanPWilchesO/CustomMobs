package com.juanp.custommobs.mob;

/**
 * Apariencia de un mob via LibsDisguises (opcional).
 *
 * @param enabled  si se aplica el disfraz
 * @param type     {@code player} para apariencia de jugador, o un {@code EntityType} (WOLF, SKELETON...)
 * @param skin     nombre de jugador del que copiar la skin (solo {@code type: player})
 * @param skinUrl  textura directa (alternativa a {@code skin})
 * @param showName si el disfraz conserva el nombre visible del mob
 */
public record DisguiseSpec(boolean enabled, String type, String skin, String skinUrl, boolean showName) {

    public static final DisguiseSpec NONE = new DisguiseSpec(false, null, null, null, true);

    public boolean player() {
        return "player".equalsIgnoreCase(this.type);
    }
}

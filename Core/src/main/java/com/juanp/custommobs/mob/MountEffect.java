package com.juanp.custommobs.mob;

/**
 * Un efecto constante de una montura.
 *
 * @param potion    efecto, tal como {@code FIRE_RESISTANCE} o {@code CONDUIT_POWER}
 * @param to        a quien se le aplica: {@code mount} (por defecto), {@code rider} o
 *                  {@code nearby}
 * @param amplifier nivel; {@code 0} = nivel I
 * @param radius    radio en bloques, solo para {@code nearby}
 * @param visible   si el efecto se muestra con particulas e icono; por defecto, no
 */
public record MountEffect(String potion, String to, int amplifier, double radius, boolean visible) {

    /** {@code true} si el efecto va al jinete. */
    public boolean toRider() {
        return "rider".equalsIgnoreCase(this.to);
    }

    /** {@code true} si el efecto va a quien este cerca. */
    public boolean toNearby() {
        return "nearby".equalsIgnoreCase(this.to);
    }
}

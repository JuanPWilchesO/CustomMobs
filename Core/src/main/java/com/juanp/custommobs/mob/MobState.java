package com.juanp.custommobs.mob;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Estado de un mob en el momento de guardarlo en su huevo.
 *
 * <p>Sin esto, recoger y volver a colocar creaba una entidad <b>nueva</b>: la vida volvia
 * al maximo (guardar curaba de gratis) y se perdian los rasgos que el juego sortea al
 * aparecer, como la bandera de capitan de un vindicador.
 *
 * <p>No es el estado completo de la entidad —eso no lo permite Bukkit— sino lo que de
 * verdad se nota al volver a colocarla: vida, absorcion, fuego, aire, los indicadores
 * visuales y los efectos de pocion. El equipo y los atributos siguen saliendo de la
 * definicion del mob.
 */
public record MobState(
        double health,
        double absorption,
        int fireTicks,
        int remainingAir,
        boolean glowing,
        boolean invisible,
        boolean silent,
        List<String> potionEffects
) {

    /** Los efectos se guardan como {@code clave;duracion;amplificador}. */
    private static final String EFFECT_SEPARATOR = ";";

    /** Lee el estado de una entidad viva. */
    public static MobState capture(LivingEntity entity) {
        List<String> effects = new ArrayList<>();
        for (PotionEffect effect : entity.getActivePotionEffects()) {
            effects.add(effect.getType().getKey() + EFFECT_SEPARATOR
                    + effect.getDuration() + EFFECT_SEPARATOR + effect.getAmplifier());
        }
        return new MobState(entity.getHealth(), entity.getAbsorptionAmount(),
                entity.getFireTicks(), entity.getRemainingAir(),
                entity.isGlowing(), entity.isInvisible(), entity.isSilent(),
                List.copyOf(effects));
    }

    /**
     * Devuelve el estado a la entidad recien creada.
     *
     * <p>La vida se recorta al maximo actual: si la definicion bajo la vida maxima desde
     * que se guardo, el valor viejo no puede dejarla por encima.
     */
    public void applyTo(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(Attribute.MAX_HEALTH);
        double max = attribute != null ? attribute.getValue() : 20.0D;
        entity.setHealth(Math.max(1.0D, Math.min(max, this.health)));
        entity.setAbsorptionAmount(Math.max(0.0D, this.absorption));
        entity.setFireTicks(Math.max(0, this.fireTicks));
        entity.setRemainingAir(Math.max(0, this.remainingAir));
        entity.setGlowing(this.glowing);
        entity.setInvisible(this.invisible);
        entity.setSilent(this.silent);
        for (String raw : this.potionEffects) {
            PotionEffect effect = parseEffect(raw);
            if (effect != null) {
                entity.addPotionEffect(effect);
            }
        }
    }

    /** {@code null} si el texto no describe un efecto valido. */
    private static PotionEffect parseEffect(String raw) {
        String[] parts = raw.split(EFFECT_SEPARATOR);
        if (parts.length < 3) {
            return null;
        }
        PotionEffectType type = Registry.EFFECT.get(
                NamespacedKey.fromString(parts[0].toLowerCase(Locale.ROOT).trim()));
        if (type == null) {
            return null;
        }
        try {
            int duration = Math.max(1, Integer.parseInt(parts[1].trim()));
            int amplifier = Math.max(0, Integer.parseInt(parts[2].trim()));
            return new PotionEffect(type, duration, amplifier, false, true, true);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

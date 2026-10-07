package com.juanp.custommobs.mob;

import com.juanp.custommobs.item.Attributes;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Estado de un mob en el momento de guardarlo en su huevo.
 *
 * <p>Sin esto, recoger y volver a colocar creaba una entidad <b>nueva</b>: la vida volvia
 * al maximo (guardar curaba de gratis) y se perdian las mejoras hechas al mob.
 *
 * <p>De los atributos solo se guardan las <b>desviaciones</b> respecto a la definicion: si
 * alguien subio la vida maxima con un item, ese valor viaja en el huevo; lo que la
 * definicion fije y nadie haya tocado se sigue leyendo del yml, asi que un cambio de
 * administrador sigue aplicandose.
 *
 * <p>No es el estado completo de la entidad —eso no lo permite Bukkit sin NMS— sino lo que
 * de verdad se nota al volver a colocarla.
 */
public record MobState(
        double health,
        double absorption,
        int fireTicks,
        int remainingAir,
        boolean glowing,
        boolean invisible,
        boolean silent,
        Map<String, Double> attributes,
        List<String> potionEffects
) {

    /** Los efectos se guardan como {@code clave;duracion;amplificador}. */
    private static final String EFFECT_SEPARATOR = ";";

    /** Margen para considerar dos valores de atributo iguales. */
    private static final double EPSILON = 0.0001D;

    /**
     * Lee el estado de una entidad viva.
     *
     * @param definition definicion del mob, para saber que valor es "el de serie";
     *                   {@code null} si no se conoce
     */
    public static MobState capture(LivingEntity entity, MobDefinition definition) {
        List<String> effects = new ArrayList<>();
        for (PotionEffect effect : entity.getActivePotionEffects()) {
            effects.add(effect.getType().getKey() + EFFECT_SEPARATOR
                    + effect.getDuration() + EFFECT_SEPARATOR + effect.getAmplifier());
        }
        return new MobState(entity.getHealth(), entity.getAbsorptionAmount(),
                entity.getFireTicks(), entity.getRemainingAir(),
                entity.isGlowing(), entity.isInvisible(), entity.isSilent(),
                captureAttributes(entity, definition), List.copyOf(effects));
    }

    /** Solo los atributos que difieren de lo que fija la definicion (o del valor de serie). */
    private static Map<String, Double> captureAttributes(LivingEntity entity, MobDefinition definition) {
        Map<Attribute, Double> expected = new LinkedHashMap<>();
        if (definition != null) {
            for (Map.Entry<String, Double> entry : definition.attributes().entrySet()) {
                Attribute attribute = Attributes.resolve(entry.getKey());
                if (attribute != null) {
                    expected.put(attribute, entry.getValue());
                }
            }
        }
        Map<String, Double> found = new LinkedHashMap<>();
        for (Attribute attribute : Attributes.known()) {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            Double baseline = expected.get(attribute);
            double reference = baseline != null ? baseline : attribute.getDefaultValue();
            double base = instance.getBaseValue();
            if (Math.abs(base - reference) > EPSILON) {
                found.put(attribute.getKey().toString(), base);
            }
        }
        return Map.copyOf(found);
    }

    /**
     * Devuelve el estado a la entidad recien creada.
     *
     * <p>Los atributos van antes que la vida: si alguien subio la vida maxima, la actual
     * tiene que recortarse contra el maximo nuevo, no contra el viejo.
     */
    public void applyTo(LivingEntity entity) {
        for (Map.Entry<String, Double> entry : this.attributes.entrySet()) {
            Attribute attribute = Registry.ATTRIBUTE.get(NamespacedKey.fromString(entry.getKey()));
            if (attribute == null) {
                continue;
            }
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance != null) {
                instance.setBaseValue(entry.getValue());
            }
        }
        AttributeInstance health = entity.getAttribute(Attribute.MAX_HEALTH);
        double max = health != null ? health.getValue() : 20.0D;
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

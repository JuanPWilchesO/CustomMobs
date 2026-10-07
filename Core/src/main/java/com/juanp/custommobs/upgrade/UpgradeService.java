package com.juanp.custommobs.upgrade;

import com.juanp.custommobs.CustomMobsPlugin;
import com.juanp.custommobs.craft.CraftService;
import com.juanp.custommobs.mob.CustomMob;
import com.juanp.custommobs.mob.MobKeys;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Construye, identifica y aplica los items de mejora.
 *
 * <p>Aplicar un item sube las caracteristicas de combate indicadas <b>sobre las que el mob
 * ya tenia</b>: no las fija, las incrementa. Como el huevo guarda las desviaciones respecto
 * a la definicion, la mejora viaja con el mob cuando se le recoge y se le vuelve a colocar.
 */
public final class UpgradeService {

    private final CustomMobsPlugin plugin;
    private final UpgradeRegistry registry;
    private final MobKeys keys;

    public UpgradeService(CustomMobsPlugin plugin, UpgradeRegistry registry, MobKeys keys) {
        this.plugin = plugin;
        this.registry = registry;
        this.keys = keys;
    }

    /** Construye el item tal como lo vera el jugador, marcado con su id. */
    public ItemStack create(UpgradeSpec spec) {
        ItemStack item = new ItemStack(spec.material());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(spec.displayName());
            if (!spec.lore().isEmpty()) {
                meta.setLore(spec.lore());
            }
            meta.getPersistentDataContainer().set(this.keys.upgrade(),
                    PersistentDataType.STRING, spec.id());
            item.setItemMeta(meta);
        }
        return item;
    }

    /** El item de mejora que representa ese objeto, si es uno. */
    public Optional<UpgradeSpec> specOf(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return Optional.empty();
        }
        String id = item.getItemMeta().getPersistentDataContainer()
                .get(this.keys.upgrade(), PersistentDataType.STRING);
        return id == null ? Optional.empty() : this.registry.get(id);
    }

    /**
     * Sube al mob las caracteristicas del item.
     *
     * <p>La vida tiene un detalle: al subir el maximo, se cura al mob en la misma cantidad,
     * porque si no la mejora no se notaria hasta el siguiente golpe.
     */
    public void applyTo(CustomMob customMob, UpgradeSpec spec) {
        LivingEntity entity = customMob.entity();
        for (Map.Entry<String, Double> entry : spec.stats().entrySet()) {
            Attribute attribute = UpgradeRegistry.attributeOf(entry.getKey());
            if (attribute == null) {
                continue;
            }
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            instance.setBaseValue(instance.getBaseValue() + entry.getValue());
            if (attribute == Attribute.MAX_HEALTH) {
                AttributeInstance health = entity.getAttribute(Attribute.MAX_HEALTH);
                double max = health != null ? health.getValue() : 20.0D;
                entity.setHealth(Math.min(max, entity.getHealth() + entry.getValue()));
            }
        }
    }

    /** Texto corto con lo que mejora, para los mensajes. */
    public static String describe(UpgradeSpec spec) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Double> entry : spec.stats().entrySet()) {
            parts.add(entry.getKey() + " +" + trim(entry.getValue()));
        }
        return String.join(", ", parts);
    }

    private static String trim(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    /** Registra las recetas de los items que se crafteen. */
    public void registerRecipes(CraftService craft) {
        for (UpgradeSpec spec : this.registry.all()) {
            if (!spec.craftable()) {
                continue;
            }
            List<org.bukkit.Material> ingredients = new ArrayList<>();
            ingredients.add(spec.material());
            ingredients.addAll(spec.recipeExtras());
            NamespacedKey key = new NamespacedKey(this.plugin, "upgrade_" + spec.id());
            ItemStack result = this.create(spec);
            result.setAmount(Math.max(1, Math.min(9, spec.recipeAmount())));
            if (!craft.registerShapeless(key, result, ingredients)) {
                this.plugin.getLogger().warning("El crafteo de '" + spec.id()
                        + "' choca con otra receta (" + CraftService.signatureOf(ingredients)
                        + "). Cambia 'recipe.extras' para diferenciarlo.");
            }
        }
    }

    /** Id normalizado, tal como se guarda en el archivo. */
    public static String normalize(String raw) {
        return raw == null ? null : raw.toLowerCase(Locale.ROOT).trim().replace(' ', '_');
    }
}

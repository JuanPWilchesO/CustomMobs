package com.juanp.custommobs.item;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;

import java.util.Locale;

/**
 * Resuelve encantamientos por id de configuracion.
 *
 * <p>Acepta tres formas:
 * <ul>
 *   <li>{@code sharpness} -> {@code minecraft:sharpness}</li>
 *   <li>{@code minecraft:sharpness} -> directo</li>
 *   <li>{@code excellentenchants:night_vision} -> encantamiento custom de ExcellentEnchants</li>
 * </ul>
 *
 * <p>Si el id no lleva namespace, se busca primero en el namespace de vanilla y
 * despues en el de ExcellentEnchants.
 */
public final class Enchants {

    public static final String CUSTOM_NAMESPACE = "excellentenchants";

    private Enchants() {
    }

    public static Enchantment resolve(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String id = raw.toLowerCase(Locale.ROOT).trim();

        if (id.contains(":")) {
            NamespacedKey key = NamespacedKey.fromString(id);
            return key == null ? null : lookup(key);
        }

        Enchantment vanilla = lookup(NamespacedKey.minecraft(id));
        if (vanilla != null) {
            return vanilla;
        }
        NamespacedKey custom = NamespacedKey.fromString(CUSTOM_NAMESPACE + ":" + id);
        return custom == null ? null : lookup(custom);
    }

    private static Enchantment lookup(NamespacedKey key) {
        try {
            Enchantment enchantment = Registry.ENCHANTMENT.get(key);
            if (enchantment != null) {
                return enchantment;
            }
        } catch (Throwable ignored) {
            // Registro no disponible o clave invalida.
        }
        try {
            return Enchantment.getByKey(key);
        } catch (Throwable ignored) {
            return null;
        }
    }
}

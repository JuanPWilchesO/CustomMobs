package com.juanp.custommobs.mob;

import com.juanp.custommobs.CustomMobsPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Reparte los efectos constantes de las monturas.
 *
 * <p>Los efectos se vuelven a aplicar cada ciclo con una duracion algo mayor que el ciclo,
 * asi que no parpadean: mientras el mob exista, el efecto esta puesto. Se aplican sin
 * particulas y sin icono, porque no son una pocion que alguien haya bebido.
 *
 * <p>A quien se le da lo decide el yml: al mob ({@code mount}), a quien lo monta
 * ({@code rider}) o a los jugadores que tenga cerca ({@code nearby}).
 */
public final class MountEffectTask extends BukkitRunnable {

    /** Un poco mas que el ciclo de un segundo, para que el efecto no se caiga entre medias. */
    private static final int DURATION_TICKS = 30;

    private final CustomMobsPlugin plugin;
    private final MobService service;

    public MountEffectTask(CustomMobsPlugin plugin, MobService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public void run() {
        for (CustomMob customMob : this.service.active()) {
            List<MountEffect> effects = customMob.definition().mountEffects();
            if (effects.isEmpty() || !(customMob.entity() instanceof AbstractHorse horse)) {
                continue;
            }
            for (MountEffect effect : effects) {
                PotionEffectType type = this.typeOf(effect.potion());
                if (type == null) {
                    continue;
                }
                // Normalmente sin particulas ni icono: no es una pocion bebida. Pero
                // se puede pedir visible, que para probar es mucho mas util.
                PotionEffect potion = new PotionEffect(type, DURATION_TICKS,
                        Math.max(0, effect.amplifier()), false, effect.visible(), effect.visible());
                if (effect.toRider()) {
                    for (Entity passenger : horse.getPassengers()) {
                        if (passenger instanceof Player riding && this.isFriendly(customMob, riding)) {
                            riding.addPotionEffect(potion);
                        }
                    }
                } else if (effect.toNearby()) {
                    double radius = effect.radius() > 0.0D ? effect.radius() : 8.0D;
                    for (Entity near : horse.getNearbyEntities(radius, radius, radius)) {
                        if (near instanceof Player player && this.isFriendly(customMob, player)) {
                            player.addPotionEffect(potion);
                        }
                    }
                } else {
                    horse.addPotionEffect(potion);
                }
            }
        }
    }

    /**
     * {@code true} si ese jugador puede recibir los beneficios de la montura.
     *
     * <p>No vale cualquiera que pase cerca: solo el dueno, su team y sus aliados. Regalar
     * resistencia al fuego a un desconocido no es lo que se pidio.
     */
    private boolean isFriendly(CustomMob customMob, Player player) {
        UUID ownerId = customMob.ownerId();
        if (ownerId == null) {
            return false;
        }
        if (ownerId.equals(player.getUniqueId())) {
            return true;
        }
        return this.service.teamLink().isAlly(ownerId, player.getUniqueId());
    }

    /** El efecto de Bukkit, o {@code null} si el yml trae un id que no existe. */
    private PotionEffectType typeOf(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Registry.EFFECT.get(NamespacedKey.fromString(raw.toLowerCase(Locale.ROOT).trim()));
    }
}

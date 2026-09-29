package com.juanp.custommobs.audio;

import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Emite el sonido ambiente de cada mob segun el intervalo de su yml. */
public final class AmbientTask extends BukkitRunnable {

    private final SoundService sounds;
    private final Map<UUID, Long> last = new ConcurrentHashMap<>();

    public AmbientTask(SoundService sounds) {
        this.sounds = sounds;
    }

    @Override
    public void run() {
        this.sounds.playAmbient(System.currentTimeMillis(), this.last);
    }
}

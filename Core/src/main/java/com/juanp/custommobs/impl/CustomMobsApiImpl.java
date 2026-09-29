package com.juanp.custommobs.impl;

import com.juanp.custommobs.api.CustomMobView;
import com.juanp.custommobs.api.CustomMobsApi;
import com.juanp.custommobs.mob.CustomMob;
import com.juanp.custommobs.mob.MobDefinition;
import com.juanp.custommobs.mob.MobRegistry;
import com.juanp.custommobs.mob.MobService;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Implementacion de la API publica, registrada en el ServicesManager. */
public final class CustomMobsApiImpl implements CustomMobsApi {

    private final MobService service;
    private final MobRegistry registry;

    public CustomMobsApiImpl(MobService service, MobRegistry registry) {
        this.service = service;
        this.registry = registry;
    }

    @Override
    public Collection<String> definitionIds() {
        List<String> ids = new ArrayList<>();
        for (MobDefinition definition : this.registry.all()) {
            ids.add(definition.id());
        }
        return ids;
    }

    @Override
    public boolean isCustomMob(Entity entity) {
        return entity instanceof LivingEntity living && this.service.find(living.getUniqueId()).isPresent();
    }

    @Override
    public Optional<CustomMobView> viewOf(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return Optional.empty();
        }
        return this.service.find(living.getUniqueId()).map(this::toView);
    }

    @Override
    public Optional<LivingEntity> spawn(String definitionId, Location location, Player owner) {
        MobDefinition definition = this.registry.get(definitionId).orElse(null);
        if (definition == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.service.spawn(definition, location, owner));
    }

    private CustomMobView toView(CustomMob customMob) {
        return new CustomMobView(
                customMob.definition().id(),
                customMob.ownerId(),
                customMob.teamIdOptional(),
                customMob.definition().displayName()
        );
    }
}

package com.juanp.custommobs.api;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;

/**
 * Contrato publico de CustomMobs para otros plugins.
 *
 * <p>Se registra en el {@code ServicesManager} de Bukkit al habilitar el plugin,
 * de modo que otros plugins pueden obtenerlo con
 * {@code Bukkit.getServicesManager().load(CustomMobsApi.class)} sin depender
 * de las clases internas.
 */
public interface CustomMobsApi {

    /** Identificadores de todas las definiciones cargadas desde {@code mobs/*.yml}. */
    Collection<String> definitionIds();

    /** {@code true} si la entidad es un mob gestionado por CustomMobs. */
    boolean isCustomMob(Entity entity);

    /** Vista de solo lectura de un mob custom, o vacio si la entidad no lo es. */
    Optional<CustomMobView> viewOf(Entity entity);

    /**
     * Invoca una definicion en la ubicacion dada, vinculada al jugador indicado.
     *
     * @return la entidad creada, o vacio si el id no existe o el mundo no esta cargado
     */
    Optional<LivingEntity> spawn(String definitionId, Location location, Player owner);
}

package com.juanp.custommobs.group;

import java.util.Set;
import java.util.UUID;

/**
 * Abstrae la lectura de grupos de LuckPerms.
 *
 * <p>Hace falta la API porque Bukkit no tiene concepto de grupo: los permisos si son
 * nativos, pero para saber a que grupo pertenece un jugador hay que preguntarle a
 * LuckPerms. Si no esta instalado, una implementacion de respaldo trata a todo el mundo
 * como el grupo {@code default}.
 */
public interface GroupLink {

    /** Nombre corto de la implementacion, para logs. */
    String name();

    /** {@code true} si LuckPerms esta presente y su API respondio. */
    boolean isAvailable();

    /**
     * Grupos del jugador: el primario y los heredados, en minusculas.
     *
     * <p>Devuelve vacio si el jugador no esta cargado en memoria (por ejemplo, si esta
     * desconectado). Eso hace que caiga al cupo por defecto, que es el comportamiento
     * prudente: no se le concede un cupo alto por no poder comprobarlo.
     */
    Set<String> groupsOf(UUID playerId);
}

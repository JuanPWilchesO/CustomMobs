package com.juanp.custommobs.group;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Lee los grupos reales del jugador desde la API de LuckPerms. */
public final class LuckPermsGroupLink implements GroupLink {

    private final LuckPerms api;

    public LuckPermsGroupLink(LuckPerms api) {
        this.api = api;
    }

    @Override
    public String name() {
        return "LuckPerms";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Set<String> groupsOf(UUID playerId) {
        // getUser solo devuelve usuarios ya cargados: no bloquea la hebra principal
        // esperando a que LuckPerms los lea de disco.
        User user = this.api.getUserManager().getUser(playerId);
        if (user == null) {
            return Set.of();
        }
        Set<String> groups = new LinkedHashSet<>();
        groups.add(user.getPrimaryGroup().toLowerCase(Locale.ROOT));
        for (Group group : user.getInheritedGroups(user.getQueryOptions())) {
            groups.add(group.getName().toLowerCase(Locale.ROOT));
        }
        return Set.copyOf(groups);
    }
}

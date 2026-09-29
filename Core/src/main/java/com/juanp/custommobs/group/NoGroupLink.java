package com.juanp.custommobs.group;

import java.util.Set;
import java.util.UUID;

/** Respaldo sin LuckPerms: todo el mundo pertenece al grupo {@code default}. */
public final class NoGroupLink implements GroupLink {

    private static final Set<String> DEFAULT = Set.of("default");

    @Override
    public String name() {
        return "sin LuckPerms";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public Set<String> groupsOf(UUID playerId) {
        return DEFAULT;
    }
}

package com.juanp.custommobs.mob;

/** Traduccion de codigos de color estilo {@code &a} a la seccion {@code §}. */
public final class Texts {

    private Texts() {
    }

    public static String color(String input) {
        return input == null ? "" : input.replace('&', '\u00a7');
    }
}

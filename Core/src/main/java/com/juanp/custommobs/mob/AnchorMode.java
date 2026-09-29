package com.juanp.custommobs.mob;

import java.util.Locale;

/** Punto de referencia al que un mob vuelve cuando se aleja demasiado. */
public enum AnchorMode {

    /** Sigue al dueno que lo invoco. */
    OWNER,
    /** Se queda cerca del bloque donde aparecio. */
    POINT;

    public static final AnchorMode DEFAULT = OWNER;

    /** {@code null} solo si el texto no esta vacio y no es un valor valido. */
    public static AnchorMode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        try {
            return valueOf(raw.toUpperCase(Locale.ROOT).trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

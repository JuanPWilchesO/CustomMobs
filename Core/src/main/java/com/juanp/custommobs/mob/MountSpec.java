package com.juanp.custommobs.mob;

import org.bukkit.Material;

import java.util.List;

/**
 * Como se prepara una montura al aparecer.
 *
 * <p>Solo aplica a {@link MobCategory#MOUNT}. El mob tiene que ser un caballo o similar
 * ({@code AbstractHorse}): caballo, burro, mula, caballo esqueleto, llama, camello.
 *
 * @param color   color del caballo ({@code Horse.Color}), o {@code null} para el de serie
 * @param style   marcas del caballo ({@code Horse.Style}), o {@code null}
 * @param saddled si aparece con silla puesta
 * @param tamed   si aparece domesticada; sin esto el jugador no puede montarla
 * @param armor   material de la armadura de caballo, o {@code null}
 * @param effects efectos constantes que reparte la montura mientras vive
 * @param frostWalker si el agua que pisa se convierte en hielo escarchado
 * @param frostRadius radio, en bloques, del hielo que va dejando
 * @param frostAhead cuantos bloques por delante se hiela, ademas de bajo las patas
 */
public record MountSpec(
        String color,
        String style,
        boolean saddled,
        boolean tamed,
        Material armor,
        List<MountEffect> effects,
        boolean frostWalker,
        int frostRadius,
        int frostAhead
) {

    /** Lo que se aplica si el mob declara la categoria sin seccion {@code mount:}. */
    public static final MountSpec DEFAULT =
            new MountSpec(null, null, true, true, null, List.of(), false, 2, 3);
}

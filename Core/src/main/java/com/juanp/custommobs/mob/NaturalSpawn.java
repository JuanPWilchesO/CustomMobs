package com.juanp.custommobs.mob;

import java.util.List;

/**
 * Aparicion aleatoria de un mob por el mundo.
 *
 * <p><b>No es un spawner.</b> Un spawner es un punto fijo que reaparece; esto es un mob que
 * brota al azar, vive una vez y muere. Por eso un mob natural nunca crea entrada en
 * {@code data/spawners.yml}.
 *
 * @param natural      si aparece solo por el mundo
 * @param worlds       mundos permitidos (vacio = los del plugin)
 * @param biomes       biomas permitidos, en minusculas (vacio = todos)
 * @param groupMin     cuantos salen juntos, minimo
 * @param groupMax     cuantos salen juntos, maximo
 * @param chance       probabilidad por intento, de 0.0 a 1.0
 * @param time         {@code any}, {@code day} o {@code night}
 * @param lightMin     luz minima del bloque donde aparece
 * @param lightMax     luz maxima
 * @param minY         altura minima
 * @param maxY         altura maxima
 * @param minDistance  distancia minima a un jugador
 * @param maxDistance  distancia maxima a un jugador
 * @param cap          tope de ese mob en todo el mundo
 */
public record NaturalSpawn(
        boolean natural,
        List<String> worlds,
        List<String> biomes,
        int groupMin,
        int groupMax,
        double chance,
        String time,
        int lightMin,
        int lightMax,
        int minY,
        int maxY,
        double minDistance,
        double maxDistance,
        int cap,
        String allowedRegion,
        List<String> denyRegions
) {

    /** Lo que se usa cuando el mob no declara la seccion. */
    public static final NaturalSpawn NONE = new NaturalSpawn(false, List.of(), List.of(),
            1, 1, 0.0D, "any", 0, 15, 0, 320, 24.0D, 96.0D, 20, "", List.of());
}

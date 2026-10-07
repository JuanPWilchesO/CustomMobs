# Banco de pruebas

Mobs que **no son contenido de juego**: existen solo para comprobar que cada funcion
nueva hace lo que dice. Cada uno deja una senal que no se puede confundir con el
comportamiento normal del mob.

## Como se instalan

```bash
cp pruebas/mobs/*.yml <servidor>/plugins/CustomMobs/mobs/
```

Luego, en el juego:

```
/custommobs reload
/custommobs list
```

Ninguno se craftea (`recipe.amount: 0`): se invocan con
`/custommobs give <id>`, para no chocar con las recetas de los ejemplos.

---

## Que hay aqui

| Archivo | Que prueba |
|---|---|
| `prueba_montura.yml` | La montura **basica** — a proposito **sin efectos ni paso helado**: huevo, domesticada, silla, montarse, cupo propio |
| `prueba_montura_helada.yml` | Variante, armadura, **paso helado**, **efectos** y una **skill pasiva** |
| `prueba_fuego_amigo.yml` | **Sin fuego amigo**, **el dueno ataca y el mob va**, y **`despedir`** |

## Prueba 1 — Montura basica (`prueba_montura`)

> **Esta NO tiene efectos ni paso helado.** Es la de `saddled`/`tamed` y nada mas: existe
> para probar el ciclo del huevo y el cupo. Los efectos y el hielo son de `prueba_montura_helada`.

1. `/custommobs give prueba_montura` y colocas el huevo.
2. **Debe salir domesticada y con silla.** Subete con **clic derecho**, sin caerte.
3. `/custommobs cuota` → **debe contar en la linea de Monturas**, no en las otras dos.
4. Con ella colocada, intenta poner la segunda montura
   (`prueba_montura_helada`): **debe rechazarla** por cupo (por defecto, 1 montura).
5. **Recógela con su huevo** y vuelve a colocarla: la montura sigue siendo tuya.

## Prueba 2 — La montura completa (`prueba_montura_helada`)

1. `/custommobs give prueba_montura_helada`, colócala y subete.
2. **Debe ser negra con manchas blancas y armadura de diamante** (variante y armadura
   del yml).
3. **Subete y camina sobre el agua**: el agua bajo las patas **se convierte en hielo
   escarchado** y se derrite a los ~10 segundos.
4. **Efectos**: tu montura debe tener resistencia al fuego; **tu**, poder de conduit
   (estando montado); y cualquiera de tu team o tus aliados que pase a menos de 8
   bloques, velocidad.
5. **Skill pasiva**: cada 5 segundos, estando a menos de 8 bloques, debe aparecer
   `[montura] OK: te cure 2 con una skill pasiva.`
6. **No debe disparar skills al montarla**: subete y bajate varias veces; el clic
   derecho es para montarse, no para dialogar.

## Prueba 3 — Fuego amigo y mando (`prueba_fuego_amigo`)

1. `/custommobs give prueba_fuego_amigo`, colócalo y **dale un espadazo**.
   **No debe perder vida**: el fuego amigo esta bloqueado.
2. Con el mob cerca, **ataca tu a un orco** (`03_orco_faccion`).
   Tu mob debe **ir a por el orco**, sin que nadie te haya pegado antes.
3. Con dos cuentas y team: que tu companero le pegue a tu mob.
   **Tampoco debe hacerle dano**, porque es aliado.
4. `/custommobs despedir prueba_fuego_amigo` → el mob desaparece y su huevo queda
   inservible. Repite con `despedir todos` si tienes varios.

> Ojo: el paso 3 necesita que las dos cuentas esten en el **mismo team**. Sin team,
> cada jugador es un desconocido y no hay nada que bloquear.

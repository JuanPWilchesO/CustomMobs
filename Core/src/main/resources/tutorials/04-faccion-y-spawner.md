# Tutorial 4 — Mob de faccion con spawner

Los mobs de `category: server` no tienen dueno: pertenecen a una faccion y se gobiernan por
`attitude`. Este tutorial crea un orco que pelea contra las facciones enemigas y reaparece.

## 1. Declara la faccion

En `factions.yml`:

```yaml
orcos:
  allies: [goblins]
  enemies: [humanos]
humanos:
  enemies: [orcos, goblins]
```

Las relaciones son **simetricas**: si `orcos` declara `humanos` enemigo, vale al reves.

## 2. El mob

`mobs/orco.yml`:

```yaml
id: orco
category: server
display-name: '&2Orco'
type: ZOMBIE
faction: orcos
attitude: hostile

burn-in-daylight: false

attributes:
  health: 30.0
  damage: 6.0

leash:
  max-distance: 10.0
  teleport-distance: 24.0

respawn-seconds: 30
```

- **Sin `egg` ni `recipe`**: no se craftea. Se invoca con `/custommobs spawn`.
- **Sin `anchor`**: los mobs de servidor quedan siempre anclados a su punto.
- `attitude: hostile` — ataca jugadores a la vista. Otras opciones: `neutral`, `defender`.
- `respawn-seconds: 30` — al morir, reaparece en el mismo bloque a los 30 s.

## 3. Invocalo

```
/custommobs spawn orco
```

Desde el juego lo coloca donde estas; desde consola, `/custommobs spawn orco <mundo> <x> <y> <z>`.

## 4. Comprueba el spawner

- Matelo (`/custommobs kill orco` o a golpes).
- Mira `/custommobs active`: desaparece.
- Espera 30 s: vuelve a estar, en el mismo sitio.
- El estado vive en `data/spawners.yml`, asi que **sobrevive a un reinicio**.

## 5. Peleas entre facciones

Coloca un mob de `humanos` cerca: se atacaran solos. Los de la **misma faccion o aliadas**
son intocables. Ver `../docs/facciones-equipos-y-combate.md`.

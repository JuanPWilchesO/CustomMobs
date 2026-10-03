# El archivo de un mob

Todo mob vive en `mobs/<archivo>.yml`. Solo `id` y `type` son obligatorios.

## Identidad

| Campo | Valores | Por defecto | Notas |
|---|---|---|---|
| `id` | texto | nombre del archivo | identificador unico |
| `category` | `player` \| `server` | `player` | ver abajo |
| `display-name` | texto con `&` | el `id` | los codigos `&` se traducen a color |
| `type` | tipo de entidad | — | `ZOMBIE`, `SKELETON`, `VILLAGER`, `RAVAGER`… cualquier entidad que sea un `Mob` |
| `glow` | `true` \| `false` | `false` | contorno visible |
| `lore` | lista de textos | vacio | se muestra en el huevo |
| `egg` | material | — | huevo base de la receta |

**`player`** — se invoca con un huevo crafteado, tiene dueno y lo protege.
**`server`** — no tiene dueno, pertenece a una faccion, y puede ser un spawner.

## Atributos

```yaml
attributes:
  health: 40.0
  damage: 5.0
  speed: 0.25
  follow-range: 24.0
```

Claves admitidas: `health`, `max-health`, `damage`, `attack-damage`, `speed`,
`movement-speed`, `armor`, `armor-toughness`, `attack-speed`, `attack-knockback`,
`knockback-resistance`, `follow-range`, `gravity`, `jump-strength`, `step-height`,
`scale`, `max-absorption`, `movement-efficiency`, `water-movement-efficiency`.

## Equipamiento

```yaml
equipment:
  main-hand:
    material: IRON_AXE
    name: '&7Espada del guardia'
    lore:
      - '&8Forjada en el puesto.'
    unbreakable: true
    glint: true
    drop-chance: 0.0
    enchants:
      sharpness: 3
      excellentenchants:regrowth: 2
  boots:
    material: NETHERITE_BOOTS
    enchants:
      protection: 4
```

Slots: `helmet`, `chestplate`, `leggings`, `boots`, `main-hand`, `off-hand`.
Se aceptan tambien los nombres de Bukkit (`head`, `chest`, `CHEST`…).

`drop-chance` va de `0.0` (nunca suelta) a `1.0` (siempre).

**Un slot tambien puede salir del catalogo** de objetos con nombre — es la via para
equipar algo con NBT, como un encantamiento de otro plugin:

```yaml
equipment:
  main-hand:
    item: espada_del_sargento    # nombre guardado con /custommobs item save
    drop-chance: 0.0
```

Con `item:` el objeto manda **tal cual** y conserva su NBT; solo se le respeta
`drop-chance`, que es propiedad del mob y no del objeto.

## Encantamientos

Los nombres se resuelven en este orden:

| Escribes | Se aplica |
|---|---|
| `sharpness` | `minecraft:sharpness` (vanilla) |
| `minecraft:sharpness` | ese namespace tal cual |
| `excellentenchants:regrowth` | encantamiento custom de ExcellentEnchants |

Sin namespace se busca primero en vanilla y luego en ExcellentEnchants. Los
encantamientos custom no se comprueban al cargar: si escribes mal un id, el mob carga
igual y el encantamiento simplemente no aparece. Los ids validos se listan con
`/custommobs enchants excellentenchants`.

## Objetivos contra jugadores

```yaml
targets:
  players: defensive
```

| Modo | Contra jugadores |
|---|---|
| `never` | nunca. Mob de apoyo puramente PvE |
| `enemies` | solo si Teams los marca enemigos |
| `non-allies` | todo jugador que no sea de su team ni aliado |
| `defensive` | neutral hasta que agredan a su bando |

Sin este campo, se hereda `targeting.player-mode` del config (`defensive` por defecto).
Los monstruos hostiles de vanilla y los mobs de facciones enemigas se atacan igual.

**La regla que no se rompe**: el dueno, los companeros de team y los aliados **nunca**
son objetivo, ni siquiera si ellos agreden primero.

## Faccion y actitud (solo `category: server`)

```yaml
faction: orcos
attitude: hostile
```

| `attitude` | Comportamiento frente a jugadores |
|---|---|
| `hostile` | ataca jugadores a la vista |
| `neutral` | no ataca salvo que lo agredan |
| `defender` | nunca ataca jugadores; persigue a los monstruos que los amenazan |

En un mob de servidor, la relacion con jugadores la manda `attitude`; `targets.players`
es para los mobs de jugador. Ver `facciones-equipos-y-combate.md`.

## Ancla y leash

```yaml
leash:
  anchor: owner
  max-distance: 10.0
  teleport-distance: 24.0
  return-speed: 1.0
```

- `anchor`: `owner` (pegado a su dueno) o `point` (fijo al bloque donde aparecio).
- `max-distance`: radio que puede alejarse. `0` = sin limite.
- `teleport-distance`: si se aleja mas, se teletransporta de vuelta.

Los mobs de `category: server` quedan **siempre** anclados a su punto: no se les aplica `anchor`.

### Ancla a un bloque: zona cargada

Un mob de jugador con `anchor: point` vive fijo en su bloque. Para que no desaparezca ni
deje de contar cuando no hay jugadores cerca, su zona se mantiene **cargada**:

```yaml
# Radio en chunks que se mantiene cargado alrededor del bloque.
# Si no se pone, se usa el global (point.chunk-radius del config). 0 = desactivado.
chunk-radius: 4
```

Ojo con el coste: es un cuadrado de `(2*radio+1)^2` chunks. Con radio 4 son **81 chunks**
por mob. Solo se suelta cuando el mob se recoge con su huevo; si luego se coloca en otro
sitio, se cargan los chunks del lugar nuevo.

## Apariencia, sonidos y resto

```yaml
disguise:
  enabled: true
  type: player
  skin: NombreDeJugador
  skin-url: ''
  show-name: true

sounds:
  ambient:
    sound: entity.zombie.ambient
    volume: 1.0
    pitch: 0.7
    interval-seconds: 10
  hurt:   { sound: entity.zombie.hurt,  volume: 1.0, pitch: 0.7 }
  death:  { sound: entity.zombie.death, volume: 1.0, pitch: 0.7 }
  attack: { sound: entity.zombie.attack, volume: 1.0, pitch: 0.7 }

burn-in-daylight: false
ai: true
respawn-seconds: 30
```

- `disguise.type`: `player` usa `skin` (nombre de jugador) o `skin-url`.
- `burn-in-daylight`: por defecto `false` en mobs de servidor y `true` en los de jugador.
- `ai: false`: el mob se queda quieto en su puesto. Util para figurantes.
- `respawn-seconds`: solo mobs de servidor. `0` = una sola vida. Con un valor mayor,
  el punto funciona como **spawner** de ese mob.

## Crafteo

```yaml
recipe:
  extras:
    - DIAMOND
    - NETHER_STAR
  amount: 1
```

- Los `extras` se suman al huevo base (`egg`).
- `amount: 0` **desactiva el crafteo**: el mob solo se da con `/custommobs give`.
- Sin bloque `recipe`: por defecto huevo + diamante.
- Dos mobs con los mismos ingredientes colisionan: el segundo se omite con un aviso,
  porque el cliente no podria distinguirlos. (`/custommobs reload` lo reporta.)

## Skills y drops

- **Skills**: ver `skills.md`.
- **Drops**: ver `drops-y-objetos.md`.

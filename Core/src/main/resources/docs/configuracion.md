# Configuracion general (`config.yml`)

```yaml
debug: false

branding:
  motd-enabled: true
  motd:
    - '&0&m----------------------------------'
    - ' &cCustomMobs &0| &cAP2P Project'
    - ' &cBienvenido, &4{jugador}&c.'
    - '&0&m----------------------------------'

targeting:
  radius: 16.0
  interval-ticks: 20
  player-mode: defensive
  aggro-duration-seconds: 120
  aggro-watch-radius: 24.0
  aggro-on-friendly-mobs: true

skills:
  interval-ticks: 20

permissions:
  player: custommobs.player
  admin: custommobs.admin

limits:
  default-player-mobs: 3
  default-point-mobs: 1
  groups:
    default: 3
    vip: 5
    staff: 10
  point-groups:
    default: 1
    vip: 2
    staff: 3

point:
  chunk-radius: 4

worlds:
  enabled: []

recall:
  enabled: true
  radius: 0.0
  mob-seconds: 60
  chunk-seconds: 120
```

## Seccion por seccion

| Seccion | Que controla |
|---|---|
| `debug` | log extra |
| `branding.motd-enabled` | si el mensaje de bienvenida se manda |
| `branding.motd` | las lineas del MOTD (admite `&` para color) |
| `targeting.radius` | radio de busqueda de objetivos |
| `targeting.interval-ticks` | cada cuanto se revisan los objetivos |
| `targeting.player-mode` | politica por defecto contra jugadores |
| `targeting.aggro-duration-seconds` | cuanto dura la venganza |
| `targeting.aggro-watch-radius` | radio en el que se percibe la agresion al bando |
| `targeting.aggro-on-friendly-mobs` | reacciona si agreden a un mob del mismo bando |
| `skills.interval-ticks` | cada cuanto se evaluan las skills |
| `permissions.player` | permiso para usar los huevos |
| `permissions.admin` | permiso para los comandos de admin |
| `limits.default-player-mobs` | cupo de mobs anclados al dueno, sin grupo mapeado |
| `limits.default-point-mobs` | cupo de mobs anclados a un bloque, sin grupo mapeado |
| `limits.groups` | cupo por grupo de LuckPerms (anclados al dueno) |
| `limits.point-groups` | cupo por grupo para los anclados a un bloque |
| `point.chunk-radius` | radio en chunks que se carga alrededor de un bloque ancla |
| `worlds.enabled` | mundos donde funciona el plugin; vacio = todos |
| `recall.enabled` | si los mobs abandonados se retiran solos |
| `recall.radius` | radio de abandono; `0` = derivado de la simulation-distance |
| `recall.mob-seconds` | segundos antes de destruir el mob abandonado |
| `recall.chunk-seconds` | segundos antes de liberar el chunk que se mantuvo cargado |

## Dos cuentas de mobs

El cupo de mobs de jugador esta **partido en dos**, y no se pisan:

- **Anclados al dueno** (`anchor: owner`): los que te siguen. Los manda
  `limits.default-player-mobs` y `limits.groups`.
- **Anclados a un bloque** (`anchor: point`): los fijos. Los manda
  `limits.default-point-mobs` y `limits.point-groups`.

Asi puedes llevar el cupo de seguidores lleno y, ademas, tener tus mobs fijos. Un
`0` en cualquiera de los dos significa **sin limite** para esa cuenta.

## Zona cargada de un mob fijo (`point.chunk-radius`)

Un mob de jugador anclado a un bloque mantiene cargada una zona a su alrededor para no
desaparecer ni dejar de contar aunque no haya jugadores cerca. El radio se mide en chunks
y el mob puede sobrescribirlo con su propio `chunk-radius`.

**Coste**: es un cuadrado de `(2*radio+1)^2` chunks por mob. Con el valor por defecto (4)
son **81 chunks**. Bajalo o ponlo en `0` si notas presion de memoria.

## El MOTD

Admite los marcadores `{jugador}`, `{online}` y `{max}`. Cambiarlo **no pide reinicio**:
se aplica con `/custommobs reload`.

La **firma** (`Plugin by AP2P Project`) no se configura: va hardcodeada en el codigo, con
los colores de la bandera palestina.

Ojo con donde se ve ese color: la firma se pinta en la **consola del servidor** (la terminal,
o el panel que uses), porque los colores viajan como codigos ANSI. En el **archivo**
`logs/latest.log` sale en texto plano: Paper le quita el formato a todo lo que escribe un
plugin. Es como funcionan los logs, no una limitacion evitable.

## Ventana de abandono (`recall`)

Cuando dejas atras un mob anclado a ti —estas en otro mundo no habilitado, o demasiado
lejos— el mob recibe un **plazo** antes de retirarlo:

1. Se pide un **ticket de chunk** en su posicion y te llega un **aviso por chat**. El
ticket es imprescindible: sin el, la chunk se descargaria y no habria forma de destruir
al mob cuando venza el plazo.
2. A los `mob-seconds` el mob se **destruye** y su vinculo se borra, asi que su huevo
queda inerte.
3. A los `chunk-seconds` se **suelta el ticket** y la chunk vuelve a lo normal.

- Si el dueno **vuelve antes** del plazo, la cuenta se cancela y el mob se conserva.
- **Estar desconectado no es abandono**: al volver, tus mobs siguen donde estaban.
- Los mobs **anclados a un punto** no siguen a nadie, asi que no se abandonan.
- `chunk-seconds` nunca puede ser menor que `mob-seconds`: soltar la chunk antes de
destruir el mob lo dejaria fuera de juego, sin forma de tocarlo.
- `radius: 0.0` deriva el radio de la `simulation-distance` del mundo. Pon un valor fijo
si quieres otro comportamiento.

## Filtrar mundos (`worlds.enabled`)

```yaml
worlds:
  enabled:
    - world
    - mundo_pvp
```

- Lista **vacia** = el plugin funciona en todos los mundos.
- Fuera de la lista, el mob no se gestiona: no cuenta cupo, no recibe IA, y
  **el huevo no invoca nada**: un mundo excluido tambien bloquea la invocacion.
- Los nombres son los de Bukkit, asi que vale cualquiera, incluidos los de Multiverse-Core
  (mira el nombre exacto con `/mv list`).

# CustomMobs

Mobs definidos por YAML, invocados con huevos crafteados, con facciones, disfraces,
equipamiento, encantamientos custom, skills y un cupo de mobs por jugador que sale del
grupo de LuckPerms.

Pensado para **Paper**. Se apoya en tres plugins opcionales: **Teams** (bandos de
jugadores), **LibsDisguises** (apariencias) y **LuckPerms** (grupos y cupos). Sin
cualquiera de ellos el plugin sigue funcionando: cada uno degrada por su cuenta.

---

## Requisitos

- **Paper** 1.21 o superior (probado en Paper 26.3).
- **Java 21+**. Ojo: Paper 26.3 exige **Java 25** en el servidor.
- Opcionales, cada uno independiente:
  - **LuckPerms** — para el cupo por grupo. Sin el, todos caen al cupo por defecto.
  - **Teams** — para bandos de jugadores. Sin el, cada jugador solo se protege a si mismo.
  - **LibsDisguises** — para dar apariencia a los mobs.

---

## Instalacion

1. Copia el jar a `plugins/`.
2. Arranca el servidor una vez. Se crean:
   - `plugins/CustomMobs/config.yml` — configuracion general.
   - `plugins/CustomMobs/factions.yml` — relaciones entre facciones.
   - `plugins/CustomMobs/mobs/` — un archivo por mob. Se copian **cinco ejemplos
     numerados** que funcionan como tutorial: abrelos en orden.
3. Edita lo que quieras.
4. `/custommobs reload` — recarga mobs, config y facciones **sin reiniciar**.

> `plugin.yml` es la excepcion: los permisos que declara solo se leen al arrancar. Si
> cambias sus valores por defecto hace falta reiniciar el servidor.

---

## Primeros pasos: tu primer mob

Crea `plugins/CustomMobs/mobs/guardia.yml`:

```yaml
id: guardia
category: player
display-name: '&bGuardia'
type: ZOMBIE
egg: ZOMBIE_SPAWN_EGG

attributes:
  health: 40.0
  damage: 5.0

skills:
  - id: aviso
    effect: message
    target: self
    range: 20.0
    cooldown-seconds: 30
    message: '{mob} &fVigilando la zona.'
```

`/custommobs reload` y luego, dentro del juego:

- `/custommobs give guardia` — te da el huevo.
- O craftealo: **huevo de zombi + diamante** (la receta por defecto).
- Usa el huevo → aparece el Guardia, y **tu eres su dueno**.

---

## El archivo de un mob

Todo mob vive en `mobs/<archivo>.yml`. Solo `id` y `type` son obligatorios.

### Identidad

| Campo | Valores | Por defecto | Notas |
|---|---|---|---|
| `id` | texto | nombre del archivo | identificador unico |
| `category` | `player` \| `server` | `player` | ver abajo |
| `display-name` | texto con `&` | el `id` | los codigos `&` se traducen a color |
| `type` | tipo de entidad | — | `ZOMBIE`, `SKELETON`, `VILLAGER`, `RAVAGER`… cualquier entidad que sea un `Mob` |
| `glow` | `true` \| `false` | `false` | contorno visible |
| `lore` | lista de textos | vacio | se muestra en el huevo |

**`player`** — se invoca con un huevo crafteado, tiene dueno y lo protege.
**`server`** — no tiene dueno, pertenece a una faccion, y puede ser un spawner.

### Atributos

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

### Equipamiento

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

### Encantamientos

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

### Objetivos

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

### Ancla y leash

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

### Apariencia, sonidos y resto

```yaml
disguise:
  enabled: true
  type: player
  skin: NombreDeJugador
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

- `burn-in-daylight`: por defecto `false` en mobs de servidor y `true` en los de jugador.
- `ai: false`: el mob se queda quieto en su puesto. Util para figurantes.
- `respawn-seconds`: solo mobs de servidor. `0` = una sola vida. Con un valor mayor,
  el punto funciona como **spawner** de ese mob.

### Crafteo

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

### Skills

```yaml
skills:
  - id: golpe_pesado
    effect: damage
    target: target
    range: 4.0
    amount: 9.0
    cooldown-seconds: 8
    chance: 0.6

  - id: grito
    effect: message
    target: self
    range: 20.0
    cooldown-seconds: 60
    message: '{mob} &fAlto ahi.'

  - id: vendaje
    effect: heal
    target: owner
    range: 12.0
    amount: 6.0
    cooldown-seconds: 25
    message: '&aTe venda las heridas.'

  - id: veneno
    effect: potion
    target: enemies
    range: 8.0
    potion: poison
    potion-duration-seconds: 6
    potion-amplifier: 0
```

| Campo | Valores | Por defecto |
|---|---|---|
| `id` | texto unico dentro del mob | — |
| `effect` | `damage` \| `heal` \| `message` \| `potion` | — |
| `target` | `self` \| `owner` \| `target` \| `enemies` \| `allies` | — |
| `range` | bloques (tope 32) | `8.0` |
| `cooldown-seconds` | entero | `5` |
| `chance` | `0.0` a `1.0` | `1.0` |
| `amount` | dano o curacion | `0.0` |
| `message` | texto con `&` | — |
| `potion` | `poison`, `speed`, `weakness`… | — |

Detalles que importan:

- Los objetivos usan **la misma politica de combate que la IA**: si el mob considera
  enemigo a alguien, sus skills tambien. No hay dos definiciones de "enemigo".
- `enemies` incluye a quien agredio a su bando (venganza), no solo a los enemigos
  declarados.
- En `effect: message` el `target` **se ignora**: el mob habla a los jugadores dentro
  de `range`. `{mob}` se sustituye por su nombre.
- `message` tambien funciona en `damage`, `heal` y `potion`: si el objetivo es un
  jugador, le llega el aviso. Es la forma de que una skill sea **observable**.
- Si falla la tirada de `chance`, se reintenta en el siguiente ciclo **sin gastar** el
  cooldown.
- Una skill mal escrita se descarta sola. Nunca tumba la carga del mob.
- El intervalo de evaluacion es `skills.interval-ticks` del config (20 = 1 segundo).

### Tabla de drops

```yaml
drops:
  # Objetos vanilla: legibles y a mano.
  - material: DIAMOND
    amount: 2
    chance: 0.5

  # Objetos con NBT: se referencian por NOMBRE (ver el catalogo mas abajo).
  - item: espada_del_sargento
    chance: 0.25
    min: 1
    max: 1

# true = el mob no suelta NADA de lo vanilla: solo su tabla.
clear-vanilla-drops: false
```

`amount` es un atajo de `min`/`max`. Un nombre que no exista en el catalogo no rompe
nada: se avisa **una vez** en el log, nombrando el mob, y ese drop se omite.

---

## Catalogo de objetos (NBT)

Hay objetos que **no se pueden escribir a mano** en un yml: un encantamiento de
ExcellentEnchants, cualquier NBT puesto por otro plugin. Para esos esta el catalogo.

```
/custommobs item save <nombre>     guarda el objeto de tu mano
/custommobs item list              lista los nombres guardados
/custommobs item remove <nombre>   borra uno
```

Cada objeto vive en **`items/<nombre>.yml`**, con su nombre escrito dentro:

```yaml
nombre: espada_del_sargento
item: '<base64 con todo el NBT>'
etiqueta: '&cEspada del Sargento'
```

El nombre se normaliza a minusculas y sin espacios, porque acaba siendo un nombre de
archivo. Despues lo llamas por ese nombre desde la tabla de drops o desde el
equipamiento. **Un mismo objeto sirve para varios mobs.**

---

## Color y brillo de los player mobs

Cada jugador puede distinguir sus mobs de los de los demas:

```
/custommobs color <color|nada>     color del nombre
/custommobs glow <color|nada>      brillo, y de que color
```

- El color **tine el nombre entero** y sobreescribe el que escribio el admin en el yml.
- El brillo **se hereda del yml** del mob. Si el yml no lo tiene, se queda sin glow.
  `nada` lo apaga explicitamente.
- **Con Teams, el ajuste es del team**: lo decide solo su jefe y aplica a los mobs de
  todos los miembros, para que un equipo se vea uniforme. Sin Teams, cada jugador manda
  sobre los suyos.

El color del brillo lo determina un **equipo de scoreboard** (asi funciona en Minecraft),
no la entidad: el plugin crea equipos `cm_<color>` y limpia la entrada al morir o al
cambiar de color.

Se guarda en `styles.yml` y se aplica en caliente: cambias el color y tus mobs vivos se
actualizan al momento.

---

## Facciones y actitud

Solo para `category: server`. Las relaciones se declaran en `factions.yml`:

```yaml
orcos:
  allies: [goblins]
  enemies: [humanos, enanos]
```

Las relaciones son **simetricas**: si `orcos` declara `humanos` enemigo, la enemistad
vale al reves. Lo que no se declara es **neutral**, y dos facciones neutrales solo se
atacan si una agrede a la otra.

La actitud frente a **jugadores** se define por mob:

| `attitude` | Comportamiento |
|---|---|
| `hostile` | ataca jugadores a la vista |
| `neutral` | no ataca salvo que lo agredan |
| `defender` | nunca ataca jugadores; persigue a los monstruos que los amenazan |

---

## Comandos

Todos bajo `/custommobs` (alias `cmobs`, `cm`).

| Comando | Que hace |
|---|---|
| `reload` | recarga mobs, config y facciones sin reiniciar |
| `list` | lista las definiciones cargadas |
| `give <id> [jugador]` | entrega el huevo de ese mob |
| `spawn <id> [mundo x y z]` | invoca el mob. Desde el juego, tu quedas como dueno |
| `kill <id>` | mata todos los mobs de esa definicion |
| `remove [radio]` | retira los mobs custom cercanos |
| `active` | lista los mobs vivos, con dueno y ubicacion |
| `enchants [filtro]` | lista encantamientos disponibles (util para validar ids) |
| `cuota [jugador]` | consulta el cupo de mobs |
| `cuota recontar <jugador>` | rehace la cuenta **sin tocar mobs** |
| `cuota retirar <jugador>` | retira sus mobs y deja el cupo en cero (**destructivo**) |
| `item save <nombre>` | guarda el objeto de tu mano en el catalogo, con su NBT |
| `item list` | lista los objetos guardados |
| `item remove <nombre>` | borra uno del catalogo |
| `color <color\|nada>` | color del nombre de tus player mobs |
| `glow <color\|nada>` | brillo de tus player mobs |

**`recontar` vs `retirar`** — la diferencia es importante:

- `recontar`: no mata nada. Deja en la cuenta solo los mobs que se ven ahora. Una
  entrada de un mob que ya no existe desaparece y no vuelve; la de un mob real que
  esta en un chunk descargado **volvera a contar** cuando su chunk cargue. Sirve para
  corregir cuentas infladas.
- `retirar`: **mata** los mobs que alcanza y deja el cupo en cero. Los que estan
  anclados en chunks descargados no se pueden tocar hasta que carguen.

---

## Permisos y cupos

| Permiso | Para que | Por defecto |
|---|---|---|
| `custommobs.player` | invocar mobs con los huevos | `true` |
| `custommobs.admin` | comandos de administracion | `op` |

El unico comando que un jugador raso puede usar por su cuenta es `cuota` sin
argumentos, para ver **su propio** cupo.

> **Ojo con `default: false`**: a diferencia de `op`, **ignora** el flag de operador.
> Si lo cambias, ni tu (siendo op) podreis invocar hasta que se os conceda el permiso.

Los permisos van por el sistema **nativo de Bukkit**, asi que sirve cualquier plugin de
permisos, no solo LuckPerms.

### Cupo de mobs por grupo

En `config.yml`:

```yaml
limits:
  default-player-mobs: 3
  groups:
    default: 3
    vip: 5
    staff: 10
```

- El grupo se lee de **LuckPerms en el momento de invocar**, no al conectarse: un cambio
  de rango aplica al instante.
- Si el jugador pertenece a **varios grupos, gana el cupo mas alto**.
- Si su grupo no esta mapeado, o no se pudieron leer sus grupos, se usa
  `default-player-mobs`. `0` = sin limite.
- Sin LuckPerms instalado, todos caen al grupo `default`.
- Cambiar `limits` aplica con `/custommobs reload`, **sin reiniciar**.

El cupo se lleva en `player-mobs.yml` y **cuenta los mobs anclados a un bloque aunque
su chunk este descargado** — un conteo que solo mirase las entidades cargadas se
saltaria el tope desplegando mobs por bloques repartidos.

---

## Configuracion general (`config.yml`)

```yaml
debug: false

targeting:
  radius: 16.0                 # radio de busqueda de objetivos
  interval-ticks: 20           # cada cuanto se revisan
  player-mode: defensive       # never | enemies | non-allies | defensive
  aggro-duration-seconds: 120  # cuanto dura la venganza
  aggro-watch-radius: 24.0     # radio en el que se percibe la agresion al bando
  aggro-on-friendly-mobs: true # reacciona si agreden a un mob del mismo bando

craft:
  consume-egg: true            # si el huevo se gasta al invocar

skills:
  interval-ticks: 20           # cada cuanto se evaluan las skills

permissions:
  player: custommobs.player
  admin: custommobs.admin

limits:
  default-player-mobs: 3
  groups: { default: 3, vip: 5, staff: 10 }

branding:
  motd-enabled: true                     # false = sin mensaje de bienvenida
  motd:                                  # se manda al entrar; admite '&' para color
    - '&0&m----------------------------------'
    - ' &cCustomMobs &0| &cAP2P Project'
    - ' &cBienvenido, &4{jugador}&c.'
    - '&0&m----------------------------------'
```

El MOTD admite los marcadores `{jugador}`, `{online}` y `{max}`. Cambiarlo **no pide
reinicio**: se aplica con `/custommobs reload`.

La **firma** del log (`Plugin by: AP2P Project`) no se configura: va hardcodeada en el
codigo, con los colores de la bandera palestina.

### Venganza (aggro)

Cuando un jugador agrede al **bando protegido** de un mob —su dueno, un companero, un
aliado o un mob del mismo lado— los mobs de ese bando en `aggro-watch-radius` lo marcan
como hostil durante `aggro-duration-seconds`, **sin importar el modo de objetivos**.
La IA y las skills reaccionan igual, porque consultan el mismo registro.

---

## API para otros plugins

Se registra un servicio `CustomMobsApi` en el `ServicesManager`:

```java
CustomMobsApi api = getServer().getServicesManager().load(CustomMobsApi.class);

api.definitionIds();                              // ids de mobs cargados
api.isCustomMob(entity);                          // es un mob gestionado?
api.viewOf(entity);                               // vista de solo lectura
api.spawn("guardia", location, ownerPlayer);      // invoca uno
```

---

## Archivos que genera

| Archivo | Que guarda |
|---|---|
| `config.yml` | configuracion general |
| `factions.yml` | relaciones entre facciones |
| `mobs/*.yml` | un archivo por mob |
| `player-mobs.yml` | cupo de mobs de jugador (incluye los de chunks descargados) |
| `styles.yml` | color de nombre y brillo elegidos por jugador y por team |
| `items/<nombre>.yml` | catalogo de objetos con NBT, uno por nombre |
| `data/spawners.yml` | puntos de aparicion que sobreviven a la muerte del mob |

El vinculo de cada mob (dueno, team, definicion, punto de aparicion) vive en el
**PersistentDataContainer** de la entidad, asi que sobrevive reinicios sin archivo
aparte. Los spawners son la excepcion: su punto debe sobrevivir a la muerte del mob.

---

## Resolucion de problemas

**Un mob no aparece al usar el huevo.**
Revisa en este orden: (1) que tengas `custommobs.player`; (2) que no hayas llegado a tu
cupo — `/custommobs cuota`; (3) que `consume-egg` y la receta sean lo que crees.

**"Jugador no encontrado" al usar `give`.**
`getPlayerExact` solo encuentra jugadores **conectados**.

**Los ids duplicados de mob se ignoran.**
Dos mobs con el mismo `id` (o el mismo archivo renombrado) colisionan. El log lo avisa.

**El crafteo no aparece o avisa de colision.**
Dos mobs con los mismos ingredientes: el cliente no puede distinguirlos. Cambia
`recipe.extras` en uno de ellos.

**Un encantamiento no se aplica.**
Los encantamientos custom no se validan al cargar. Comprueba el id real con
`/custommobs enchants excellentenchants`.

**Quedo bloqueado sin tener mobs.**
Tu cupo incluye mobs anclados en chunks descargados. Si un mob desaparecio sin pasar por
muerte ni retirada (un plugin externo, un corte), la cuenta queda alta:
`/custommobs cuota recontar <jugador>`.

**Cambie los permisos por defecto y no aplican.**
`plugin.yml` solo se lee al arrancar. Requiere reinicio del servidor.

# Skills

Una skill es algo que el mob hace solo: golpear mas fuerte, curar a su dueno, avisar por
chat o envenenar a los enemigos. Se declaran en una lista dentro del mob.

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

## Campos

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
| `potion-duration-seconds` | entero | `5` |
| `potion-amplifier` | entero (`0` = nivel I) | `0` |

## Los cuatro efectos

| `effect` | Hace |
|---|---|
| `damage` | resta `amount` de vida al objetivo |
| `heal` | suma `amount` de vida al objetivo |
| `message` | manda un texto a los jugadores en rango |
| `potion` | aplica `potion` durante `potion-duration-seconds`, a nivel `potion-amplifier` |

## Los cinco objetivos

| `target` | Quien es |
|---|---|
| `self` | el propio mob |
| `owner` | su dueno |
| `target` | a quien esta atacando ahora |
| `enemies` | los enemigos declarados **y** quien lo agredio (venganza) |
| `allies` | companeros de team y aliados |

**Los objetivos usan la misma politica de combate que la IA**: si el mob considera
enemigo a alguien, sus skills tambien. No hay dos definiciones de "enemigo".

## Detalles que importan

- En `effect: message` el `target` **se ignora**: el mob habla a los jugadores dentro
  de `range`. `{mob}` se sustituye por su nombre.
- `message` tambien funciona en `damage`, `heal` y `potion`: si el objetivo es un
  jugador, le llega el aviso. Es la forma de que una skill sea **observable**.
- Si falla la tirada de `chance`, se reintenta en el siguiente ciclo **sin gastar** el
  cooldown.
- `range` tiene un tope duro de **32 bloques**; lo que pidas por encima se recorta.
- Una skill mal escrita **se descarta sola**. Nunca tumba la carga del mob.
- El intervalo de evaluacion es `skills.interval-ticks` del config (20 = 1 segundo).

# Changelog

## 0.2.0 — monturas, aparicion natural y mejoras

- **Monturas** (`category: mount`): caballos y similares con variante, armadura, **efectos
  constantes** (al mob, a quien la monta o a los jugadores del bando dentro de un radio) y
  **paso helado**. Tienen **su propia cuenta de cupo**, aparte de los mobs que siguen y de
  los fijos. **No se teletransportan**: si las dejas atras se quedan, y la ventana de
  abandono se las lleva con aviso.
- **Aparicion aleatoria** (`spawn.natural`): los mobs de servidor pueden brotar solos por el
  mundo, de una sola vida y sin dejar spawner, con probabilidad, grupo, hora, luz, altura y
  tope propios. Es aparte de los spawners: solo suma.
- **Libro de inspeccion** renombrado a **Inventario de fuerzas**, con los valores en colores
  oscuros para que se lean sobre el papel claro.
- **Un mob custom no se modifica a mano**: se bloquean la etiqueta, la silla o armadura a
  mano, la correa, alimentarlo o criarlo y el equipado por dispensador.
- **Fuego amigo bloqueado**: tu propio mob, tus companeros de team y tus aliados no reciben
  dano de tu bando.
- **El huevo conserva las estadisticas** del mob al recogerlo y volverlo a colocar: la vida
  herida y las mejoras viajan con el (antes, guardarlo era una cura gratis).
- **`kill` alcanza los mobs en chunks descargadas**: carga su chunk un momento por el vinculo
  y los mata, asi que ya se limpia lo que quedo perdido.
- **`range` es alcance maximo tambien para `owner` y `target`**: una skill de montura ya no
  alcanza al dueno desde el otro lado del mundo.

## 0.1.0 — primera version publica

- **Mobs definidos por YAML**, un archivo por mob en `mobs/`. Dos categorias: `player`
  (con dueno, lo defiende) y `server` (sin dueno, con faccion y actitud).
- **Huevo-ficha**: el huevo no se gasta. Se liga al mob que invoca, lo recoge al
  golpearlo con el y lo vuelve a desplegar. Si el mob muere, el huevo queda inerte solo,
  sin tener que ir a buscarlo por el mundo.
- **Skills** con cuatro efectos (`damage`, `heal`, `message`, `potion`) y tres modos:
  `pasiva` (siempre, mientras el mob este cargado), `activa` (solo en combate) e
  `interaccion` (clic derecho, para dialogos).
- **Dos cuentas de cupo** independientes: mobs que siguen al dueno y mobs fijos a un
  bloque. Cada una con su cupo por grupo de LuckPerms, y no se pisan.
- **Zona cargada** alrededor del bloque de un mob fijo: no desaparece ni deja de contar
  aunque no haya jugadores cerca. Radio configurable por mob con `chunk-radius`.
- **Ventana de abandono**: un mob anclado a su dueno que no puede volver a su lado recibe
  un aviso y un plazo (60 s) antes de retirarse; su chunk se libera a los 120 s.
- **Facciones** con relaciones simetricas, integracion con **Teams** (bandos de jugadores)
  y **LibsDisguises** (apariencias), todo opcional y degradando por su cuenta.
- **Equipamiento** con encantamientos vanilla y custom, tabla de **drops** y **catalogo
  de objetos con NBT**.
- **Documentacion y tutoriales** dentro del propio jar, copiados a la carpeta del plugin
  al arrancar.

### Requisitos

- Servidor **Paper 1.21+** con **Java 21+** (probado en Paper 26.3).
- Para **compilar** hace falta **JDK 25**: `paper-api` 26.3 esta en bytecode de Java 25.
  El jar resultante, en cambio, es Java 21.

### Licencia

GPL-3.0. Si redistribuyes una version modificada, tiene que seguir siendo libre.

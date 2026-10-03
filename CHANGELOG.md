# Changelog

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

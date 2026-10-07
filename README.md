# Squissy (mod para Minecraft Forge 1.20.1)

Squissy es una babosa redondita y achuchable que brilla en cinco colores: rojo, verde, amarillo, rosa y azul.

## Subirlo a GitHub (se descomprime y compila solo)
1. Crea un repositorio nuevo en GitHub.
2. *Add file > Create new file*, escribe `.github/workflows/build.yml` y pega el contenido del archivo **build.yml** que viene aparte. Commit.
3. *Add file > Upload files* y arrastra el **.zip tal cual**. Commit.
4. Pestaña **Actions**: se extrae el zip, se guarda el código en el repositorio y se compila.
   Descarga el .jar en el artefacto **mod-jar** y ponlo en la carpeta `mods` de Forge 1.20.1 (47.3.0 o superior).
   Si subes un zip nuevo, el workflow lo vuelve a extraer y compilar solo.

## Cómo es Squissy
- **Colores**: aparece en rojo, verde, amarillo, rosa o azul y **brilla** con un latido suave. Vive en pantanos, selvas, bosque oscuro y cuevas frondosas.
- **Domesticar**: dale **cualquier comida** (1 de cada 3 intentos funciona).
- **Rastro de baba**: al andar deja una capa de baba brillante de su color. Sobre su baba **resbala y va más rápida**.
  La baba se seca sola pasados unos segundos.
- **Trepa paredes**: se pega a las paredes y las sube con su baba.
- **Babas mágicas**: suelta una cada 5-10 minutos (salvaje o domesticada).
- **Tentáculos y cola** de babosa, y se aplasta y estira como gelatina.

## Domesticada (todo con clic derecho)
- **Mano vacía**: cambia el modo: **te sigue** → **se queda quieta** → **deambula** por una zona pequeña (radio de 8 bloques
  alrededor del punto donde estaba, sin irse lejos).
- **Agachado + mano vacía**: abre su **barriguita** (9 huecos).
- **Cualquier objeto en la mano**: lo **absorbe** a la barriguita. También absorbe lo que haya tirado cerca.
  Ves flotando sobre ella el primer objeto que lleva. Si muere, suelta todo.
- **Comida con la vida baja**: se cura.
- **Tinte** (rojo, verde/lima, amarillo, rosa, azul/celeste): le cambia el color.

## Botas babosas
- Receta: 4 **babas mágicas** en forma de botas (`M M` / `M M`).
- Dejan el mismo rastro de baba (de colores) al andar y, sobre la baba, te hacen **resbalar y ganar velocidad**.
- Se reparan con babas mágicas.

## Dónde ajustar cosas
- `entity/SquissyEntity.java`: probabilidad de domesticar, tiempo entre babas mágicas, radio de deambular (`WANDER_RADIUS`), velocidad.
- `block/SlimeTrailBlock.java`: cuánto dura la baba (500-1000 ticks).
- `item/SlimeBootsItem.java`: empujón y efecto de velocidad de las botas.
- `data/squissy/tags/worldgen/biome/has_squissy.json` y `forge/biome_modifier/add_squissy.json`: dónde y cuántos aparecen.
- `client/SquissyShape.java` es la forma redonda (generada con un script a partir de una esfera de cubitos).

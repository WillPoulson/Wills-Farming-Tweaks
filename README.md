![Banner image](https://cdn.modrinth.com/data/cached_images/b51a0833636b342b48df6e17ee6d7bdf4b1ca6f1.png)

**Will's Farming Tweaks** is a Fabric mod for **Minecraft 26.1** that tweaks hoes, bonemeal, and farmland. Harvest mature crops in an area around the block you click, spread bonemeal growth to nearby crops, and optionally block farmland trampling.

## Requirements

- **Minecraft** 26.1  
- **Fabric Loader** 0.18.5 or newer  
- **Fabric API**  
- **Java 25** (runtime and for building)

## Features

### Hoe-based crop harvest

Right-click a **mature** crop with a hoe to harvest every mature crop in a square around it (same Y level). Each harvested crop costs **1 durability**. Defaults by tool (radius = Chebyshev distance, so radius 1 is a 3×3 footprint):

| Hoe        | Default radius | Approx. footprint |
| ---------- | -------------- | ------------------- |
| Wooden     | 0              | 1×1 (clicked block only) |
| Stone      | 1              | 3×3                 |
| Copper     | 2              | 5×5                 |
| Iron       | 2              | 5×5                 |
| Golden     | 2              | 5×5                 |
| Diamond    | 3              | 7×7                 |
| Netherite  | 4              | 9×9                 |

Harvest uses a short cooldown (configurable) to reduce spam-clicking.

### Area bonemeal

Bonemeal on a crop still favours the block you used it on, then can grow other immature crops in a configurable **radius** around it, with falloff by distance. Chance and radius are editable in the config.

### Farmland trampling

By default, jumping or landing on farmland **does not** turn it to dirt (`allowTrample` is **false**). Set **`allowTrample`** to **`true`** in the config if you want vanilla trampling back.

## Configuration

On first run the mod creates:

`config/wills-farming-tweaks.json`

You can adjust:

- **`allowTrample`** — `false` = protect farmland from entity trampling; `true` = vanilla behaviour.  
- **`harvest`** — cooldown, default radius, and per-item radii (`radiusByItemId`, e.g. `minecraft:iron_hoe`).  
- **`bonemeal`** — `radius` and `baseChance` for nearby growth.

## Building

```bash
./gradlew build
```

The remapped mod JAR is in `build/libs/`.

## Download

Releases for Fabric are published on [Modrinth](https://modrinth.com/mod/wills-farming-tweaks) (check the file for the matching Minecraft version).

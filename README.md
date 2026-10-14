# Ore Hives (Fabric 1.21.1)

Licensed MIT (see LICENSE). Not published yet.

Bees whose flower is an "ore flower" slowly fill a nearby **Ore Beehive** with ore combs.
Shear the hive to collect combs, then spin them in a **Honey Centrifuge**.

## Ore flowers
- `#orehives:ore_flowers/<ore>`: blocks that directly count as that ore. OrePlants crops are listed as optional entries, so OrePlants is a soft dependency.
- Fallback (works without OrePlants): any `#orehives:ore_flowers` block (default `#minecraft:flowers`) on or next to a `#c:ores/<ore>` block.
- Ores: iron, copper, gold, diamond, emerald, redstone, lapis.

## Centrifuge
- Right-click with combs to load them; right-click empty-handed to take the results.
- Hoppers: insert from the top or sides, extract from the bottom. A comparator reads the fill level.
- Outputs are loot tables: `data/orehives/loot_table/centrifuge/<ore>_comb.json`.

| Comb | Output (+30% honeycomb, 5% ore honey) |
|---|---|
| iron / gold | 2-4 nuggets |
| copper | 50% chance of 1 raw copper |
| redstone / lapis | 2-4 |
| diamond | 5% chance of 1 diamond |
| emerald | 8% chance of 1 emerald |

## Ore honey and Haste potions
- Brewing: Awkward + Ore Honey -> Haste (3:00); add Redstone for 8:00, Glowstone for Haste II (1:30).

## Config
`config/orehives.json`: `checkIntervalTicks`, `combChancePerBee`, `maxBeesCounted`, `beeRadius`, `maxStoredCombs`, `preciousChanceMultiplier` (diamond/emerald), `allowFlowerOnOreFallback`, `centrifugeTicks`.

## Development
- `python3 tools/gen_assets.py`: regenerates all JSON files and the pixel textures (drawn by code, no AI images).
- `./gradlew build`: builds the release jar (the gametests are kept out of it).
- `./gradlew runGametest`: runs the headless gametests (source set `src/gametest`).
- `./gradlew runClient`: starts a dev client.

# TerraVera

A [TerraFirmaCraft](https://github.com/TerraFirmaCraft/TerraFirmaCraft) addon for Minecraft 1.21.1 (NeoForge) that
reworks the very beginning of the game around one change: **cordage before tools**.

## Cordage before tools

In TerraFirmaCraft you knap a stone head, put a stick under it in a crafting grid, and you have an axe. TerraVera
adds the step that is actually missing: **something to hold the head on**.

```
grass / plants  ──break──►  plant fiber
plant fiber     ──ret in a sealed barrel of water──►  retted fiber
retted fiber    ──twist x3──►  cordage
cordage + stick + knapped head  ──►  a stone tool
```

Fibre comes in three grades, and where you can get it is gated on what you already have:

| Fibre | Source | Strength | Notes |
| --- | --- | --- | --- |
| Grass | Common grasses, ferns | 0.35 | Everywhere. Weak. |
| Herb | Broadleaf herbs, goldenrod, yucca | 0.55 | A step up. |
| Bast | Cattail, reed, pampas, wild jute | 0.90 | **Needs a blade to strip.** |

That last row is the deliberate bootstrap: your first knife is lashed with bad grass cordage, and only once you have
it can you get at the fibre that makes good cordage. The strength of the cordage carries through to the durability of
whatever you lash with it, so an early axe is genuinely worse than one you make an hour later.

Retting is a real process — soaking the stems rots away the pectin binding the bast to the woody core — and it takes
an in-game day in a sealed barrel. It is the main time gate on early progression.

## Knapping is TerraFirmaCraft's

TerraVera does **not** touch knapping. You knap heads the way you always have: right click a pair of loose rocks,
use TFC's knapping screen, follow TFC's patterns, get TFC's stone tool heads. All TerraVera changes is what happens
next — the head has to be lashed onto its haft with cordage instead of just being set on a stick.

| Head | Cordage | Becomes |
| --- | --- | --- |
| Axe head | 2 | Axe |
| Hammer head | 2 | Hammer |
| Hoe head | 2 | Hoe |
| Javelin head | 1 | Javelin |
| Knife head | 1 | Knife |
| Shovel head | 1 | Shovel |
| Obsidian shard | 1 | Obsidian knife, obsidian javelin |

Heavier heads take more cordage, because a head that takes a swing shock every stroke needs a real binding, not a
single wrap. TFC's stick-only hafting recipes are disabled by this mod's datapack.

## Building

TerraFirmaCraft is not on a public Maven repository, so you have to point the build at a copy:

- **Local jar** — drop a TerraFirmaCraft NeoForge 1.21.1 jar into `libs/`. This takes priority over everything else.
- **CurseForge** — set `tfcCurseFileId` in `gradle.properties` to the file id from the download URL.

Then:

```bash
./gradlew build          # build the mod jar
./gradlew runClient      # launch a dev client
```

## Configuration

`config/terravera-server.toml`, all hot-reloadable:

- `requireCordageForHafting` — set `false` to make the cordage in a lashing recipe optional.
- `scaleDurabilityByCordage`, `minimumDurabilityMultiplier`, `maximumDurabilityMultiplier`.
- `plantsDropFibre`, `fibreDropChance`, `fibreKnifeBonus`, `rettingTicks`.

## Extending it

Everything that defines the mod's behaviour is data:

- `data/<ns>/terravera/fibre_source/<name>.json` — what yields fibre, how much, and how good. Loaded through
  TerraFirmaCraft's own data manager system, so it reloads with `/reload` and syncs to clients automatically.
- `data/<ns>/recipe/…` — `terravera:lashing` and `terravera:twisting` recipes.

Adding a new fibre plant, or making a modded tool head require cordage, is a single JSON file.

## Licence

[EUPL v1.2](LICENSE.txt), the same licence as TerraFirmaCraft, which this builds on.

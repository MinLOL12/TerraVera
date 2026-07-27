# TerraVera

A [TerraFirmaCraft](https://github.com/TerraFirmaCraft/TerraFirmaCraft) addon for Minecraft 1.21.1 (NeoForge) that
reworks the very beginning of the game around two changes.

### 1. Cordage before tools

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

### 2. Knapping by function, not by silhouette

TerraFirmaCraft's knapping asks you to reproduce an exact picture. Miss a square and you get nothing, with no
explanation. TerraVera throws the pictures away and **measures the stone instead**:

- **A sturdy base.** Enough contiguous, un-notched stone at the butt to take a blow or be lashed to a haft without
  splitting. Critically, the base is measured *relative to the widest part of the piece* — a broad head on a spindly
  neck fails, because that neck is where it would snap.
- **A strong tip.** A working end that tapers to a point or an edge, but not so fine it shears off first use.
- **A whole piece.** One connected body. Two flakes sitting next to each other are not a tool head.

What you get depends on the geometry you produced, not on which recipe you were aiming at:

| Head | Working end | Becomes |
| --- | --- | --- |
| **Wedge** | Short 2–3 wide edge with mass behind it | Axe |
| **Point** | Converging point, symmetric | Javelin |
| **Blade** | Long fine edge on a narrow body | Knife |
| **Broad** | Wide shallow edge | Shovel, Hoe |
| **Maul** | No working end at all | Hammer |

There is no single correct axe head. All four of these are one:

```
 ##       ##      ##       ###
####     ####    ###      ####
#####   #####   #####    #####
#####   #####   #####    #####
```

The analysis runs in all four rotations, so it does not matter which way up you worked the stone. And when a piece
*isn't* usable yet, the screen says why — "the base is notched and will split", "the working end is too blunt" —
which teaches the underlying model instead of asking you to memorise pictures.

Every head also carries a **quality** score based on how comfortably it cleared the bar, which combines with cordage
strength to scale the finished tool's durability.

The five shipped heads are also registered as `tfc:knapping` recipes for every TFC rock category. This makes them
visible in TFC's Rock Knapping recipe integrations (including JEI). The pattern shown there is one representative
shape for the profile, not the only shape the shaping screen accepts.

## Building

TerraFirmaCraft is not on a public Maven repository, so you have to point the build at a copy:

- **Local jar** — drop a TerraFirmaCraft NeoForge 1.21.1 jar into `libs/`. This takes priority over everything else.
- **CurseForge** — set `tfcCurseFileId` in `gradle.properties` to the file id from the download URL.

Then:

```bash
./gradlew build          # build the mod jar
./gradlew test           # run the knapping geometry tests
./gradlew runClient      # launch a dev client
```

## Configuration

`config/terravera-server.toml`, all hot-reloadable:

- `requireCordageForHafting` — set `false` to restore TFC's stick-only hafting.
- `scaleDurabilityByCraftsmanship`, `minimumDurabilityMultiplier`, `maximumDurabilityMultiplier`.
- `plantsDropFibre`, `fibreDropChance`, `fibreKnifeBonus`, `rettingTicks`.
- `showKnappingFeedback` — set `false` to hide the "why isn't this working" hints.

## Extending it

Everything that defines the mod's behaviour is data, loaded through TerraFirmaCraft's own data manager system (so it
reloads with `/reload` and syncs to clients automatically):

- `data/<ns>/terravera/head_profile/<kind>.json` — what counts as a given kind of head.
- `data/<ns>/terravera/knappable_stone/<name>.json` — what you can sit down and knap.
- `data/<ns>/terravera/fibre_source/<name>.json` — what yields fibre, how much, and how good.
- `data/<ns>/recipe/…` — `terravera:lashing` and `terravera:twisting` recipes.

Adding a new stone, a new fibre plant, or retuning what counts as an axe bit is a single JSON file.

## Licence

[EUPL v1.2](LICENSE.txt), the same licence as TerraFirmaCraft, which this builds on.

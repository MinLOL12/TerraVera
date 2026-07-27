# TerraVera

A [TerraFirmaCraft](https://github.com/TerraFirmaCraft/TerraFirmaCraft) addon for Minecraft 1.21.1 (NeoForge) that
reworks the very beginning of the game around two changes.

### 1. Cordage before tools

In TerraFirmaCraft you knap a stone head, put a stick under it in a crafting grid, and you have an axe. TerraVera
adds the step that is actually missing: **something to hold the head on**.

```
grass / plants  ──pull by hand / cut──►  plant fiber
plant fiber     ──twist x3 by hand──►  primitive cordage
plant fiber     ──ret in a sealed barrel of water──►  retted fiber
retted fiber    ──twist x3──►  cordage
cordage + stick + knapped head  ──►  a stone tool
```

Fibre comes in three grades, and where you can get it is gated on what you already have:

| Fibre | Source | Strength | Notes |
| --- | --- | --- | --- |
| Grass | Common grasses, ferns | 0.35 | Everywhere. Weak. Pull it out bare-handed (slowly), or cut it with a blade. |
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

- `requireCordageForHafting` — set `false` to allow stick-only hafting for TerraVera heads.
- `scaleDurabilityByCraftsmanship`, `minimumDurabilityMultiplier`, `maximumDurabilityMultiplier`.
- `plantsDropFibre`, `fibreDropChance`, `fibreKnifeBonus`, `handGatheringSpeed`, `rettingTicks`.
- `showKnappingFeedback` — set `false` to hide the "why isn't this working" hints.
- `scaleFoodEatTimeBySize` — set `false` to go back to vanilla's flat 1.6 second eat time for every food.
- `foodEatTimeMultiplier` — global multiplier on top of the size-derived eat time, for tuning pace without retuning every band.
- `showFlavorTooltip` — set `false` to hide the "Flavor: ..." line on food tooltips.
- `[disease]` — `enableDisease`, `diseaseChanceMultiplier`, `diseaseDurationMultiplier`, `enableContagion`,
  `contagionRange`, `enableFoodborneIllness`, `enableWoundInfection`.
- `[water]` — `enableWaterContamination`, `waterContaminationMultiplier`, `warnBeforeDrinkingUnsafeWater`,
  `showWaterTooltip`.
- `[hygiene]` — `enableHygiene`, `hygieneDecayMultiplier`.

### Food: flavor and eating time

Two small, TFC-flavoured changes to eating:

- **Flavor is visible on the item, not just felt after eating.** Every food's tooltip now shows a `Flavor: ...` line
  (e.g. `Flavor: Delicious`), derived from the same [`TasteSystem`](src/main/java/com/terravera/common/food/TasteSystem.java)
  that already adjusted saturation gain. It reflects the current player's monotony penalty, so a food you've been
  living on will show as blander than the first bite did, without needing to actually eat it again to find out.
- **Every food in TerraFirmaCraft has an assigned flavour.** The complete TFC roster — fruit, vegetables, grains at
  every stage from raw ear to baked bread, raw and cooked meats, eggs, cheese, soups, salads, sandwiches and jarred
  preserves — has a hand-set taste value in [`TasteSystem.DEFAULT_TASTES`](src/main/java/com/terravera/common/food/TasteSystem.java),
  anchored to the vanilla foods, so nothing shows a generic `Plain` by accident. If something *does* read as Plain,
  it genuinely is bland (looking at you, raw cabbage).
- **How long a bite takes depends on how big the bite is.** Vanilla (and TFC, which doesn't touch this) eats every
  item in a flat 1.6 seconds, whether it's a single berry or a whole roast. TerraVera reads the item's TFC
  [size and weight](https://terrafirmacraft.github.io/Field-Guide/en_us/getting_started/size_and_weight.html) and
  scales the eating animation and completion time from it:

  | TFC size | Eat time |
  | --- | --- |
  | Tiny / Very Small | ~4 seconds |
  | Small | ~10–15 seconds |
  | Normal | ~15–20 seconds |
  | Large / Very Large / Huge | ~20–50 seconds |

  Weight nudges the duration within its size's band (a `Heavy` item of the same size takes a bit longer than a
  `Very Light` one), except for `Tiny`/`Very Small` items, where any weight is still a single mouthful. See
  [`FoodEatTime`](src/main/java/com/terravera/common/food/FoodEatTime.java) for the exact bands.

### Disease, water, and sanitation

TerraFirmaCraft splits the world's water into "fresh" and "salt", and every fresh block is equally safe to drink. That
is the thing this system replaces. Water now has a **contamination** value you can read off the landscape before you
drink it, illnesses are **real diseases with real incubation periods**, and the ways out of them unlock gradually.

**Water sources are not interchangeable.** Contamination is computed from flow, the size and depth of the body of
water, the mud and muck around it, and the climate:

| Source | Risk | Why |
| --- | --- | --- |
| Cold, fast, high-altitude runoff | Very low | Flow and cold both suppress pathogen load |
| Rivers and flowing water | Low | Moving and diluted, but drains everything upstream |
| Lakes and deep pools | Moderate | Still, but large enough to stay reasonable |
| Shallow ponds and puddles | High | Warm, still, small, and walked in |
| Water over mud, swamps, lowlands | Very high | The worst natural water in the game |
| Anything downhill of your own waste | Near-certain | This is how you give yourself cholera |

Warm climates raise contamination and freezing ones suppress it. A jug remembers where it was filled, so a jug of
boiled spring water and a jug of swamp water are no longer the same item.

**Nothing makes you ill instantly.** Every illness has an incubation period, so you drink from the swamp on day three
and fall ill on day five — the lesson has to be learned by reasoning about what you did, which is the entire point.

| Illness | Caught from | Incubates | Signature |
| --- | --- | --- | --- |
| Common cold | Close contact | ~1.5 days | Fatigue, cough. Confers short immunity |
| Influenza | Close contact | ~1.25 days | Fever, aching, exhaustion |
| Norovirus | Contaminated food, filthy hands | ~0.75 days | Violent, short, dehydrating |
| Giardiasis | Untreated surface water | ~9 days | Malabsorption. Lives in clear mountain streams |
| Cryptosporidiosis | Untreated surface water | ~6 days | Malabsorption; resists most treatment but boiling |
| Dysentery | Poor sanitation, dirty water | ~2 days | The everyday killer of pre-modern camps |
| Typhoid | Fouled wells, poor sanitation | ~11 days | Weeks of sustained fever. **Filtration does not stop it** |
| Cholera | Badly contaminated water | ~1 day | Kills by dehydration, fast |
| Tapeworm | Undercooked meat | ~20 days | Constant hunger, nutrition that never adds up |
| Trichinosis | Undercooked pork and bear | ~8 days | Larvae in muscle — fever plus severe muscle pain |
| Infected wound | A cut taken while filthy | ~2.5 days | The most preventable illness in the mod |
| Tetanus | A dirty wound in soil | ~7 days | Rigid spasms. Rare, critical, needs real medicine |

Symptoms are the mundane, economic ones: fatigue, fever, chills, nausea, muscle pain, increased hunger, dehydration,
and **malabsorption** — which takes back part of every meal, and so quietly shrinks your health bar through TFC's
nutrition system while you eat exactly as much as you always did.

**Prevention and treatment unlock in tiers.**

- **Hygiene** is available from the first minute and is a sliding multiplier on every food- and wound-borne infection,
  not a checkbox. You get filthy butchering, working in mud, and simply living; you wash with water, better with wood
  ash or lye, best with soap (tallow or olive oil + lye, boiled — real saponification).
- **Boiling** needs only a firepit and a clay pot. Hold a filled container to a hot pot and the water is safe. Total,
  permanent, and costs fuel every single time.
- **Filtration** (sand, charcoal, cordage) is portable and fire-free. It removes the *parasites* and most of the load,
  but deliberately **not the bacteria** — so it never obsoletes boiling, and both stay worth carrying.
- **Herbal remedies** are gathered as you travel: bitter herbs from the same plants that give herb fibre, wormwood from
  dry shrubs, willow bark stripped from willow with a knife (salicin — genuine antipyretic chemistry). They shorten and
  blunt; they never cure.
- **Prepared remedies** need a pot or salt: clean dressings, and rehydration salts — oral rehydration therapy, which is
  the real-world answer to cholera and is the difference between surviving it and not.
- **Medicine** is the endgame: a concentrated extract distilled in strong alcohol, which cures essentially anything.

Nutrition feeds back into all of it — a well-fed player is meaningfully harder to infect than a starving one, so the
disease system compounds with the food system rather than sitting beside it.

Everything above is data-driven and hot-reloadable. All of it can be turned off or retuned in
`config/terravera-server.toml` under `[disease]`, `[water]`, and `[hygiene]`.

## Extending it

Everything that defines the mod's behaviour is data, loaded through TerraFirmaCraft's own data manager system (so it
reloads with `/reload` and syncs to clients automatically):

- `data/<ns>/terravera/head_profile/<kind>.json` — what counts as a given kind of head.
- `data/<ns>/terravera/knappable_stone/<name>.json` — what you can sit down and knap.
- `data/<ns>/terravera/fibre_source/<name>.json` — what yields fibre, how much, and how good.
- `data/<ns>/terravera/illness/<name>.json` — a disease: its vectors, incubation, duration, symptoms, and remedies.
- `data/<ns>/terravera/remedy/<name>.json` — what treats what, by how much, and at which progression tier.
- `data/<ns>/recipe/…` — `terravera:lashing` and `terravera:twisting` recipes.

Adding a disease is a single JSON file — pick its transmission vectors, an incubation period, a symptom list, and the
remedy tags that treat it. The health system also reads a handful of tags (`terravera:fouls_water`,
`terravera:soils_player`, `terravera:risky_raw_meat`, `terravera:remedies/*`, `terravera:herbs/*`), so a pack can point
it at other mods' blocks and items without touching code.

Adding a new stone, a new fibre plant, or retuning what counts as an axe bit is a single JSON file.

## Licence

[EUPL v1.2](LICENSE.txt), the same licence as TerraFirmaCraft, which this builds on.

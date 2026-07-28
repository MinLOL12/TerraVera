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

### Bark and off-grid water

Living trees can be right-clicked with a knife, shears, or a tagged cutting tool to remove species-specific bark.
Fresh bark carries moisture, tannin, flexibility, flammability, and thickness data. Dry it on a drying rack (much
faster near a fire) or slowly over a campfire before using it as tinder, TFC firepit fuel, tanning stock, bast fibre,
folded birch containers, or layered weatherproof roofing. Willow remains usable fresh as a basic salicylate remedy.
Two strips per tree in a seven-day recovery window are sustainable; a third damages the canopy and repeated
ring-barking can kill the trunk. Harvest pressure is saved with the world rather than resetting on reload.

Four passive water collectors bridge early survival and settled water storage:

- **Rain Catcher** — fastest and largest, but only works under open sky during rain.
- **Dew Collector** — small nighttime yield when the cloth has clear sky.
- **Rock Basin** — cheap rain storage with more contamination than covered collectors.
- **Solar Still** — very low daytime output, but the cleanest collected water.

Each is a real fluid handler, so TFC jugs and other compatible containers can draw from it. Source contamination is
written onto the filled container for TerraVera's disease system. Their frames, cloth layers, stone courses, covers,
and animated water surfaces use GeckoLib geometry rather than flat block textures.

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

## Building with load paths

TerraVera now treats player-built construction as **structure**, without turning every hut into a finite-element
engineering exam. The system watches the structural blocks a player places and gives an unsafe member a brief grace
period to be braced before it sheds as dropped material. Natural caves and worldgen are deliberately left alone.

- **Rubble Foundation** is a compact laid-stone footing. Start posts and heavy walls on it (or on tagged ground/rock),
  not in mid-air.
- **Wooden Support Beams** can be placed vertically as posts or horizontally as lintels/purlins. They are light,
  flexible and forgiving, but their safe span and carrying capacity are modest.
- **Wrought-Iron Support Beams** are forge-welded fabricated sections. Their recipe uses wrought-iron double sheets
  and rods at TFC welding heat; they carry much heavier stone and brick roof work, and span roughly twice as far as
  timber.
- Masonry is strong under compression but heavy. Timber is much lighter but weaker. Roof slabs/stairs must rest on a
  beam, and horizontal beams need anchored posts under both ends. A line of stone blocks can make a wall, but a broad,
  heavy roof needs posts, lintels, and a real foundation.

The bracing rules are deliberately generous about *what counts as support*, because the members you place to hold a
building up must never be the hardest thing to keep standing:

- A **foundation** is the bottom of the load path. It only has to be sitting on something — grass, sand, gravel, rock,
  or another footing. It is not itself a loaded member needing a footing beneath it.
- A **post** is anchored when the bottom of its column lands on a footing, on tagged ground, on masonry or timber, on
  another beam, or on any block with a solid top face. A post standing on your plank floor is a post, not a hazard.
- A **lintel** needs support within its span at both ends. A post under the end counts, and so does resting on a wall
  head, being bedded into masonry, or simply lying on the ground as a sill.

The material categories are datapack tags under `data/terravera/tags/block/structural/`, so packs can classify their
own TFC stones, bricks, thatch, and lumber. `enableStructuralIntegrity` turns the mechanic off for worlds that prefer
free placement.

## Learned knowledge and fitted handles

Characters begin inexperienced rather than uniformly competent. TerraVera records separate practical experience for
**mining, smithing, building, cooking, medicine, and butchery**. Experience has a diminishing curve and conservative,
field-specific effects: mining improves exposed-ore recognition and work efficiency; smithing improves temperature
control, repair quality and metal economy; building slightly improves safe beam layout; cooking improves the quality
of food made; medicine improves diagnosis/dosing of a correctly matched remedy; butchery decides how much of an
animal you actually recover. None of these replace the underlying
material, heat, support, or treatment requirement.

Craft a **Field Notes** book with a book and charcoal, then use it to review each field and its accumulated experience.

The handle path also continues after the first stick haft:

1. Make and lash the wooden handle as normal.
2. Cut and lace leather with cordage into a **Leather Tool Grip**, then bind it over the existing handle with a fresh
   cord. It gives modest control and useful-yield improvements.
3. Tap a tagged tropical latex tree with a knife for **Raw Latex**. Compound it with charcoal, sulfur-bearing powder,
   and a leather backing into a **Rubber Tool Grip**, then bind that over an already-lashed handle with heavy cordage.
   Rubber costs more but better damps shock and improves task control.

A grip fits **any hafted tool with a handle** — your own lashed stone work, a TFC stone tool, or a metal one. A wrap
is a physical thing tied around a haft; it does not care how the haft was made. Leather can also be upgraded to rubber
in place. The grip assembly recipe copies the exact tool stack, so its existing durability, knapping, lashing and
repair history are preserved rather than reset.

## Soil, glass, and seeds

Soil preparation is TerraVera's; seeds and crops are TerraFirmaCraft's.

TerraVera used to ship its own seed line with genetic quality, generations, and a generic crop block to grow them
in. That block rendered with vanilla wheat models regardless of what had been sown and overrode TFC's own planting,
which is where the visual glitching came from. It is gone. What remains is the part TFC does not do:

- **Prepared farmland** you clear, loosen, amend, and weed, which pays back as extra produce at harvest — more of
  whatever crop you were actually growing, not a parallel seed currency.
- **Greenhouses** in four tiers, which simulate an interior microclimate rather than applying a flat yield bonus.

The seed-extraction recipes are still there; they now hand back **TFC seeds**, so threshing wheat gives you
`tfc:seeds/wheat` and it plants, grows, and harvests exactly as TFC intends.

Greenhouse trays are now real. Right-click the controller with seeds to sow a tray, and the tray tracks what was
sown and how far along it is, growing against the greenhouse's actual simulated climate — a cold, unlit hoop house
genuinely stalls. When a tray finishes, the **Collect** button in the control panel hands you the crop, and a
well-run glasshouse yields up to three of it per tray. Trays empty when collected; the greenhouse is a workspace,
not an automatic farm.


## Butchering

Killing an animal does not produce steaks. It produces a **carcass**, and a carcass is a job of work.

The carcass is an item, not a block, so a deer shot on a mountainside can be carried home rather than butchered
where it fell. Once home, hang the animal on a **Carcass Rack** (`carcass_rack`), which displays a 3D model of the animal hanging from its hook. Hold a **Butcher's Knife** (or any blade) and right-click the hanging carcass to perform each stage:

| Stage | Products |
| --- | --- |
| Bleed | Blood — but only if the animal is still fresh |
| Skin | Hide, or fleece from a wool-bearing animal |
| Draw | Heart, liver, kidneys, stomach |
| Break down | Shoulder, ribs, loin, leg, and trimmings |
| Strip the frame | Bones, marrow bones, fat, suet, sinew, tendon |

The order is fixed because it is fixed in reality. You cannot skin an animal you have already quartered, and if you
break the primals before drawing the guts you have opened the stomach into the meat. You can stop after the primal
cuts if all you want is dinner — at the cost of the fat, bone, and sinew everything else is made from.

### Carcass Rack, Realistic Pixel Wear, and the Butcher's Knife

- **Carcass Rack**: Right-click an empty rack with a carcass item to hang the animal. Every time you use the Butcher's Knife on it, the 3D animal model visually sheds its anatomical layers — hide and fur pixels are stripped away when skinned, the belly opens when drawn, thick primal muscle cuts wear off when broken down, and remaining fat/sinew wears away until only the bare skeletal frame remains. Each cut drops the stage's loot directly below the rack. Sneak right-click with an empty hand to remove a carcass at any stage.
- **Butcher's Knife Tiers**: Specialized butchering knives come in five TerraFirmaCraft metal tiers: **Wrought Iron**, **Steel**, **Black Steel**, **Blue Steel**, and **Red Steel**. Using a Butcher's Knife on a hanging carcass provides a **+15% cut quality bonus**, **30% faster butchering speed**, and killing an animal with one yields a cleaner carcass with higher initial workmanship (`0.75` instead of `0.50`).

### Everything comes off an animal for a reason

| Product | Feeds |
| --- | --- |
| **Fat**, suet | Rendered tallow → soap, candles, dubbin, cooking |
| **Sinew**, tendon | Sinew cord — the strongest lashing in the mod — bowstrings, and sewing thread |
| **Bone** | Bone meal, needles, awls, and marrow-bone broth |
| **Blood** | Blood meal fertiliser, or black pudding if you have a stomach to boil it in |
| **Organs** | Cooked offal, and boiled liver as a real nutritional medicine |
| **Hide** | Handed straight to TFC's own soaking and tanning chain — TerraVera does not duplicate it |

### Freshness

A carcass passes through **fresh → cool → aging → spoiling → rotten**, and heat drives it. Spoilage roughly doubles
for every 10 °C, so a TFC winter lets you hang a deer for days and a hot afternoon gives you until evening.

The bands are not uniform. Blood and offal are the first things lost; muscle survives well past them, and hanging a
carcass genuinely improves it before it turns. Hide, bone, and sinew outlast everything, which is why even a rotten
animal is still worth the knife work. Prompt butchering is rewarded rather than merely required.

### Skill, and the knife in your hand

Two things decide how much of an animal you recover, and neither substitutes for the other.

**Keenness** is what the blade is made of: a knapped flake tears where steel parts the seams, and TFC's metal tiers
map straight onto it, so butchering improves as your metalworking does. **Edge** is the condition of that particular
tool — a steel knife worn to its last few points is worse than a fresh copper one. A dull knife does not only waste
meat, it takes noticeably longer, because you end up sawing.

**Practice** is the other half. Beginners waste; experienced butchers recover more of everything. But a sharp knife
in unpracticed hands and a stone flake in expert hands both land in the middle: neither buying your way past the
skill nor grinding past the equipment fully works.

Mistakes compound. A carcass that was badly skinned is a worse carcass to draw and to break down — the hide is
nicked and the muscle torn — and careful later work only partly recovers it. That is the mechanical reason to slow
down at the start.

`enableButchery`, `enableCarcassFreshness`, and `carcassSpoilageMultiplier` control all of it.


## Configuration

`config/terravera-server.toml`, all hot-reloadable:

- `requireCordageForHafting` — set `false` to allow stick-only hafting for TerraVera heads.
- `scaleDurabilityByCraftsmanship`, `minimumDurabilityMultiplier`, `maximumDurabilityMultiplier`.
- `plantsDropFibre`, `fibreDropChance`, `fibreKnifeBonus`, `handGatheringSpeed`, `rettingTicks`.
- `showKnappingFeedback` — set `false` to hide the "why isn't this working" hints.
- `scaleFoodEatTimeBySize` — set `false` to go back to vanilla's flat 1.6 second eat time for every food.
- `foodEatTimeMultiplier` — global multiplier on top of the size-derived eat time, for tuning pace without retuning every band.
- `enableButchery` — set `false` to let animals drop their normal loot instead of a carcass.
- `enableCarcassFreshness`, `carcassSpoilageMultiplier` — how long you have to process an animal before it turns.
- `showFlavorTooltip` — set `false` to hide the "Flavor: ..." line on food tooltips.
- `[building].enableStructuralIntegrity` — set `false` to disable load-path checks and structural failures.
- `[disease]` — `enableDisease`, `diseaseChanceMultiplier`, `diseaseDurationMultiplier`, `enableContagion`,
  `contagionRange`, `enableFoodborneIllness`, `enableWoundInfection`.
- `[water]` — `enableWaterContamination`, `waterContaminationMultiplier`, `warnBeforeDrinkingUnsafeWater`,
  `showWaterTooltip`.
- `[hygiene]` — `enableHygiene`, `hygieneDecayMultiplier`.
- `[temperature]` — `enableBodyTemperature`, `temperatureRateMultiplier`, `enableTemperatureDamage`,
  `showTemperatureSymptoms`, `enableWetClothing`.

### 3. Blacksmithing maintenance instead of durability refills

Metal tools are no longer treated as items that can be magically refilled. Once a metal tool is worn down, heat it in a
charcoal forge until TFC reports it is hot enough to work, put the tool in your off hand, hold a metal hammer in your
main hand, and strike a placed **Workplate** or any TFC anvil. Sneak-right-click the surface to choose the next
operation:

- **Drawing** lengthens and thins a worn edge.
- **Upsetting** shortens and thickens an end that needs mass behind it.
- **Flattening** spreads high spots and broad edges.
- **Straightening** takes bends out with corrective blows.
- **Bending** deliberately sets or corrects a curve.
- **Controlled hammer strikes** true the shape while closing damage.
- **Forge welding** is the special case: it requires welding heat and consumes flux plus a separate welding-hot sheet or
  rod of matching metal, because separate hot metal is being joined. Ordinary repairs do not consume flux.

Every maintenance operation restores only a little durability, changes the tool's recorded shape, and loses a tiny
amount of metal to scale, filing, and sharpening. That remaining metal mass caps the tool's future maximum repair, so a
well-loved tool can be kept alive many times but slowly wears away over its lifetime. The **Workplate** itself is made
as a TFC welding recipe from a wrought-iron double sheet and a wrought-iron rod stiffener; the welding process supplies
the realistic requirements of heat, an anvil, a metal hammer, and flux.

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
  ash or lye, best with soap. Soap is now a real chain: leach and boil wood ash into concentrated lye, saponify tallow
  or olive oil into soap curd, then salt/cure it into hard bars.
- **Boiling** needs only a firepit and a clay pot. Hold a filled container to a hot pot and the water is safe. Total,
  permanent, and costs fuel every single time.
- **Filtration** (sand, charcoal, cordage) is portable and fire-free. It removes the *parasites* and most of the load,
  but deliberately **not the bacteria** — so it never obsoletes boiling, and both stay worth carrying.
- **Herbal remedies** are gathered as you travel: bitter herbs from the same plants that give herb fibre, wormwood from
  dry shrubs, willow bark stripped from willow with a knife (salicin — genuine antipyretic chemistry). They shorten and
  blunt; they never cure.
- **Prepared remedies** need a pot or salt: clean dressings, salicylate extract from willow, wormwood tincture,
  antiseptic tincture, activated charcoal, and rehydration salts — oral rehydration therapy, which is the real-world
  answer to cholera and is the difference between surviving it and not.
- **Medicine** is the endgame: an apothecary kit built from those prepared extracts in strong alcohol, rather than raw
  herbs dumped in a pot. It cures essentially anything because reaching it now means you have the whole treatment chain.

Nutrition feeds back into all of it — a well-fed player is meaningfully harder to infect than a starving one, so the
disease system compounds with the food system rather than sitting beside it.

Everything above is data-driven and hot-reloadable. All of it can be turned off or retuned in
`config/terravera-server.toml` under `[disease]`, `[water]`, and `[hygiene]`.

## Warmth, weather, and what you are wearing

TerraVera gives the player a **core body temperature** rather than a cold bar. It is a real number the game tracks,
and one the player is never shown. What they get instead — the same contract as the disease and taste systems — is
their own body reporting on itself:

> *You feel slightly chilled.*
> *Your hands feel stiff.*
> *You are struggling to stay warm.*
> *You feel overheated.*

**Nothing about it is instant.** Core temperature has inertia. Walking into a blizzard naked does not hurt you; it
takes over three minutes of it to reach the first band that impairs anything, and the better part of ten to become
hypothermic — and the first two bands either side of comfortable do nothing mechanical at all. They exist purely so
you are told before anything is taken away from you. Only the two extreme bands do slow, capped direct damage, and by
then you have ignored several minutes of escalating warnings.

The model is one equation, and every behaviour below falls out of it rather than being special-cased:

```
heat produced = basal metabolism x activity  (+ shivering, + fever)
heat lost     = (core - felt ambient) / (skin resistance x clothing)  + sweat
dCore/dt      = (produced - lost) x thermal inertia
```

### What the environment does

Biome climate, latitude, altitude, season, and time of day all come straight from TerraFirmaCraft's own climate model,
so the thermometer you are already reading is the one the system uses. On top of that TerraVera reads what a
thermometer in a box would miss: **wind** (worse when wet, worse on a ridge, worse in a storm), **rain**, **water
immersion** (water strips heat far faster than air at the same temperature), and **sun versus shade** — full midday
sun is worth about six degrees, a tree is worth all of them back, and shade becomes a genuine resource in the desert.

### What clothing does

Insulation is a **divisor, not a bonus**, so the first layer matters enormously and the fifth barely at all. It is
also **symmetric**: the same term that keeps heat in on a winter night traps it during summer work. No rule punishes
you for wearing a parka in the desert; it simply also works in the desert.

| Material | Warmth | Wind | Breathes | When wet | Why you would wear it |
| --- | --- | --- | --- | --- | --- |
| Bare skin | none | none | fully | — | Comfortable to about 22 °C at rest. Below that you are losing. |
| Plant fibre | negligible | poor | yes | useless | Day one, from the fibre you are already gathering. |
| Straw | negligible | poor | yes | useless | A wide brim is the only real sun protection you can weave. |
| Burlap | light | fair | yes | poor | The first actual fabric. |
| Linen | light | fair | **excellent** | fine | The hot-climate answer: light, cool, fast drying. |
| Wool | **strong** | fair | fair | **still works** | The cold milestone. Comfortable to about −6 °C at rest. |
| Felt | strong | good | poor | fair | Warmer and windproof, but airless. |
| Leather | moderate | **near-total** | poor | fair | Stops wind and weather rather than being warm. |
| Oilskin | moderate | **total** | none | **barely wets** | The rain answer: it cannot fail because it cannot soak. |
| Fur | **extreme** | good | poor | poor | Warmest thing there is. A liability anywhere warm. |
| Silk | moderate | fair | very good | fair | Light and versatile, good in both directions. |
| Quilted | extreme | good | fair | fair | The best cold gear that can be sewn. |

**Wet clothing is the single most important lesson in the cold.** A soaked garment keeps about a quarter of its dry
insulation — except wool, which keeps most of it, and oilskin, which never gets there. Wetness is stored **per
garment, on the item**, so a soaked coat is still soaked tomorrow, still soaked when you take it off, and can be hung
on a **Drying Rack** near a fire while you wear a spare. Carrying a change of clothes is a genuine strategy.

### What buildings do

The hut you actually built matters, and for the reasons a real hut would. A bounded flood fill answers four questions
a builder already understands:

- **Is it enclosed?** A roof and walls stop the wind and hold air still. Much the largest single effect.
- **Is it sealed?** Every gap leaks. An open door is a hole, and the model notices.
- **What is it made of?** Stone has enormous thermal mass, so it lags the outdoors — cool through a hot afternoon,
  holding warmth into a cold evening. Timber and thatch insulate better per block but track the weather.
- **What is underfoot?** A dirt floor conducts your heat into the ground; a raised plank floor over an air gap does
  not, which is exactly why cold-climate buildings have one.

Fires, hearths, forges, and stoves are **localised**: inverse-square falloff, worth far more inside a sealed room than
in the open. A campfire under the stars still helps — refusing to count it because there are no walls would be
perverse — just much less.

### Heat, work, and water

Mining, running, and swinging an axe raise the metabolic term, which is why you overheat chopping wood in a coat and
then freeze the moment you stop — exertion decays slowly on purpose, so that dangerous interval exists in game too.
Overheating makes you sweat, sweating **costs water**, and sweat only cools you if it can evaporate. That last point
is why an airless parka in the heat is dangerous for a second reason beyond its warmth: it shuts off the body's only
active cooling. Shade, breathable cloth, and staying hydrated are how heat is beaten — it is never a hard ceiling.

### Sleeping

Lying down in the open in the cold is the most dangerous thing an unprepared player can do, and your body keeps
cooling while you are unconscious. You are never *refused* the bed — being blocked with no explanation is the worst
possible version of this — but you are told, before you commit, that this is a bad place to spend the night. Bedding
and a fire are the fix, and the message says so.

## Over forty garments, and how you make them

TFC's sewing table can only tell light cloth from dark cloth, so sewing alone could never know whether the bolt you
fed it was wool or linen. TerraVera splits the job the way a tailor would:

1. **Sew the pattern.** At a sewing table, stitch out a **hood**, **body**, **leg**, or **foot** panel. The pattern is
   reusable across every material line.
2. **Face it with a material.** Combine the panel with the cloth, hide, or pelt you actually want the garment made of,
   plus a cord to bind it. *That* is what decides how the finished garment behaves.

Both halves are real work, and there are eleven material lines × four body slots, plus a few single-slot specialities
(the straw sun hat, the oilskin cloak) that only make sense as one piece — forty-two garments in all. They equip in
the ordinary armour slots and render on the player. Physical armour value is near-zero throughout on purpose: this is
clothing, not an armour tier.

The textile chain behind them is its own small progression: matted **plant fibre cloth** and plaited **straw mats** on
day one; **burlap** and **linen** once you have jute and flax; **wool cloth** from sheep, then **felt** by working it
further; **dubbin** rendered from fat to turn leather into **oilskin**; **fur pelts** from scraping a hide without
tanning the fur off; and finally **batting** and **quilted cloth**, the best cold-weather fabric that can be sewn.

Everything is tunable in `config/terravera-server.toml` under `[temperature]`, including turning the whole system,
wet clothing, symptom messages, or the extreme-band damage off independently.

## Extending it

Everything that defines the mod's behaviour is data, loaded through TerraFirmaCraft's own data manager system (so it
reloads with `/reload` and syncs to clients automatically):

- `data/<ns>/terravera/head_profile/<kind>.json` — what counts as a given kind of head.
- `data/<ns>/terravera/knappable_stone/<name>.json` — what you can sit down and knap.
- `data/<ns>/terravera/fibre_source/<name>.json` — what yields fibre, how much, and how good.
- `data/<ns>/terravera/illness/<name>.json` — a disease: its vectors, incubation, duration, symptoms, and remedies.
- `data/<ns>/terravera/remedy/<name>.json` — what treats what, by how much, and at which progression tier.
- `data/<ns>/recipe/…` — `terravera:lashing` and `terravera:twisting` recipes.
- `data/<ns>/tags/item/clothing_repair/<material>.json` — what mends each clothing line.
- `data/<ns>/tags/item/furred_hides.json` — hides that still have usable fur on them.

Adding a disease is a single JSON file — pick its transmission vectors, an incubation period, a symptom list, and the
remedy tags that treat it. The health system also reads a handful of tags (`terravera:fouls_water`,
`terravera:soils_player`, `terravera:risky_raw_meat`, `terravera:remedies/*`, `terravera:herbs/*`), so a pack can point
it at other mods' blocks and items without touching code.

Adding a new stone, a new fibre plant, or retuning what counts as an axe bit is a single JSON file.

## Licence

[EUPL v1.2](LICENSE.txt), the same licence as TerraFirmaCraft, which this builds on.

## Refrigeration and climate control

Climate control is an **industrial endgame** rather than a survival shortcut. Early cooling is architectural: dig underground rooms, build thick stone shells for thermal mass, add high/low ventilation, and use water-and-air evaporative cooling in dry regions. Those methods are passive; they do not need a grid and they do not make a hot, leaky house comfortable.

The final step is the **Vapor-Compression Air Conditioner**. Its recipe calls for industrial iron/copper, a mechanical piston/compressor, and redstone control hardware. Craft a **Programmed Climate Control Circuit**, then open the unit GUI and install it. An unprogrammed unit is intentionally inert. It also requires a sealed insulated room and continuing service with an **Air Filter** and **Refrigerant Canister**.

Power is now physical. Wire must make a continuous, face-connected run from a source to the AC's terminal lugs - wires never jump gaps, and a machine is only tapped through the terminal faces it exposes. Three conductors are available:

- **Single Wire** - a bare copper conductor for horizontal runs, rated at 100 W.
- **Insulated Copper Cable** - the workhorse cable, 200 W.
- **Wire Intersection** - a six-way cast junction rated for 400 W, used for vertical drops, climbs, and crossing runs.

Wires glow faintly while they are carrying current, so a dead run is obvious at a glance. Each grid sums its sources against its loads: if demand exceeds supply everything browns out, and if a run is asked to carry more than its weakest feeder conductor is rated for, that conductor heats up, sparks, and eventually burns out - split heavy loads onto heavier wire.

The machines are animated with GeckoLib and tell you their own state. The **Wind Turbine** rotor spins while the block has open sky above it (attach wire to the terminal lugs on the mast); the **Hand Crank Generator** handle spins a full revolution on every right-click and keeps a slow driving rotation for the five seconds it is producing 140 W; and the **Air Conditioner** roof fan sits still when unpowered, idles slowly when fed, and spins up with a faint cabinet vibration while the compressor is actively moving heat. The **Electrical Generator** supplies a steady 360 W through the terminal posts on its four sides.

The controller GUI exposes a 16–30 °C target and five compressor speeds. Electrical demand grows sharply with both compressor speed and the gap between outdoor air and the selected target; badly sealed or uninsulated buildings multiply that demand. The system models vapor-compression cooling as heat moved from the room through its condenser, not a direct change to player temperature. Consequently desert settlements gain far more from the machine than cool regions, where insulation, stone mass, and ventilation remain the sensible investment.

Filters are part of indoor air quality: a serviced system keeps dust/airborne load out of circulated air; neglected filters and refrigerant/compressor wear steadily reduce output until the unit is serviced. This makes large electrical generation, filtration materials, mechanical repair, and building design ongoing requirements rather than a one-time craft.

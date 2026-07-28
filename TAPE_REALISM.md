# Most Realistic Tape – Chemistry & History

This mod now implements pressure-sensitive adhesive (PSA) tape with historically and chemically correct steps, not magic shapeless recipes.

## Why tape is hard

Modern tape looks trivial, but it is a **viscoelastic composite**:

- **Backing** (paper or cloth) gives tensile strength, prevents stretch.
- **Adhesive** is *never* just glue. It is a blend:
  - **Elastomer** (rubber, polyisoprene) – provides cohesive strength, holds itself together, elastic recovery.
  - **Tackifier** (rosin, abietic acid) – lowers glass-transition temperature Tg, adds tack, makes it stick on light pressure.
  - **Plasticizer** (olive oil, tallow, beeswax) – keeps adhesive soft at room temp, prevents crystallization.

The Dahlquist criterion for tack: storage modulus G' < 0.3 MPa at 1 Hz, 25°C. That is why tape stays sticky forever but doesn't flow.

## Historical chain implemented

### 1. Pine resin – tapping conifers
Real: Pinus, Picea, Abies exude oleoresin when wounded. Composition: 70% resin acids (abietic, pimaric), 20% turpentine (alpha-pinene), 10% water.
In-game: Right-click pine/spruce/fir logs with a knife (tagged `terravera:resin_trees`). 40% chance.

### 2. Rosin – distilling resin
Real: Heat resin to 150-200°C. Monoterpenes boil (turpentine bp 154-170°C), water evaporates, solid remains: **colophony/rosin**, 90% abietic acid, brittle amber glass.
Use: Tackifier since 2000 BCE, violin bow rosin, solder flux.
In-game: Pot recipe `pine_resin x2 + water 100mB @200°C -> rosin x2` (comment explains turpentine off-gassing).

### 3. Natural rubber – coagulating latex
Real: Hevea brasiliensis latex = 30-40% cis-1,4-polyisoprene particles stabilized by proteins. Acid (acetic acid 5% from vinegar, pH 4) neutralizes charge, particles coalesce. Wash, press, smoke.
In-game: Shapeless `raw_latex x2 + salt -> natural_rubber`. Comment says real process uses vinegar (acetic acid); salt is gameplay fallback. Barrel sealed with vinegar would be even more real (you can add fluid `tfc:vinegar` if you have it).

### 4. Rubber adhesive – compounding PSA
Real 1930s Scotch tape formula (3M): 60% natural rubber, 35% rosin, 5% plasticizer, dissolved 40% solids in turpentine/naphtha, coated 20-30 µm wet onto backing, dried at 80-100°C. Peel ~3 N/25mm.
Chemistry: Rubber = high MW ~1e6 Da, entanglement gives cohesion. Rosin = low MW tackifier, miscible, lowers Tg from -70°C (pure rubber) to -20°C, increases tan delta. Oil = further lowers modulus.
In-game: Pot `natural_rubber + rosin + tallow (or olive oil) @150°C` with `olive_oil` fluid 100mB -> `rubber_adhesive x3`. Alt recipe with olive oil only.

### 5. Backing – paper strips
Real masking tape = creped paper (cellulose), creping gives stretch conformability. Duct tape = cotton duck.
In-game: Craft `paper_sheet + knife -> paper_strips x4`. Alt `plant_fiber_cloth + knife -> x3`. Represents cutting backing.

### 6. Assembly – coating
Real: Adhesive dissolved, brushed, dried, wound.
In-game: Shapeless `rubber_adhesive + paper_strips x2 -> adhesive_tape x6`. One tape roll hangs ~6 papers.

### 7. Primitive glues also hang paper
- **Hide glue**: Collagen (Gly-Pro-Hyp triple helix) from sinew/tendon/bone, boiled 6-12h at 80-100°C -> gelatin. Strong 30 MPa shear, reversible.
  In-game: Pot water 500mB + sinew + tendon + marrow_bone @95°C -> hide_glue x3.
- **Pine pitch glue**: Neolithic, 10kya. Rosin 60% + charcoal filler 20% (raises viscosity, reduces brittleness) + tallow 20% (plasticizer). Melt 120°C.
  In-game: Pot tallow 100mB + rosin + charcoal x2 @130°C -> pine_pitch_glue x3.

## Hanging paper

- Right-click wall with paper (blank or written) in main hand + tape/glue in offhand or inventory.
- Consumes 1 tape/glue + 1 paper, places `posted_paper` block entity that stores `PaperContent` (text + strokes).
- Renderer draws paper quad + two tape strips at top corners + freehand ink strokes + typed text.
- Breaking posted paper drops paper sheet with content preserved.

## Writing / drawing

- Paper item (`paper_sheet`, `plant_fiber_cloth` as TFC paper, `written_paper`, `written_parchment`, `written_bark`) is right-click to open screen.
- Requires quill + ink (iron gall or charcoal) in inventory – same historical inks already in mod.
- Screen: canvas 0-1 normalized strokes, color palette (iron gall black, walnut brown, vermillion, indigo, green, charcoal), thickness, text box for real typing, clear, done. Sends `SavePaperPayload` to server.

All codecs use `PaperContent` data component: `text: string, strokes: [{color: int, thickness: float, points: [{x,y}]}]`.

This is as realistic as Minecraft gets without adding a full chemical plant.

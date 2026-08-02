#!/usr/bin/env python3
"""
Procedurally generates TerraVera's water-sterilization resources.

Single source of truth for the five GeckoLib sterilization machines (SODIS rack, bio-sand filter,
distillation still, UV sterilizer, clarifier): this script writes

    * assets/terravera/geo/<machine>.geo.json      - Bedrock geometry with explicit per-face UVs
    * assets/terravera/textures/block/<machine>.png - 64x64 RGBA atlases painted from the same UVs
    * assets/terravera/blockstates/<machine>.json   - facing x water_level x active variants
    * assets/terravera/models/block/<machine>.json  - builtin/entity models
    * assets/terravera/models/item/<machine>.json   - held-item display transforms
    * assets/terravera/textures/item/*.png          - 16x16 item icons for the treatment consumables
    * lang/en_us.json merge                         - all new localization entries

Every face rectangle in a texture atlas is computed from the geo model's own cube geometry, so a
texture can never drift out of alignment with its model.

Run from the repository root:
    python3 tools/gen_sterilizer_resources.py
"""
import json
import random
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/terravera"
BLOCK_TEX = ASSETS / "textures/block"
ITEM_TEX = ASSETS / "textures/item"
GEO = ASSETS / "geo"
BLOCKSTATES = ASSETS / "blockstates"
MODELS_BLOCK = ASSETS / "models/block"
MODELS_ITEM = ASSETS / "models/item"
LANG = ASSETS / "lang/en_us.json"

TEX_SIZE = 64


# --------------------------------------------------------------------------------------------------------------------
# Model definitions. Each cube: (name, origin, size, uv_origin, material). Bones: (name, pivot, rotation, cubes).
# UV layout follows Blockbench's box-UV convention, emitted explicitly per face so the parser never guesses.
# --------------------------------------------------------------------------------------------------------------------

def wood_frame_bones():
    return [
        ("frame", [0, 0, 0], [0, 0, 0], [
            ("base",    [-8, 0, -6],  [16, 2, 12],  [0, 0],   "wood"),
            ("post_nw", [-8, 2, -5],  [2, 14, 2],   [0, 16],  "wood"),
            ("post_ne", [6, 2, -5],   [2, 14, 2],   [8, 16],  "wood"),
            ("post_sw", [-8, 2, 3],   [2, 14, 2],   [16, 16], "wood"),
            ("post_se", [6, 2, 3],    [2, 14, 2],   [24, 16], "wood"),
            ("crossbar", [-8, 16, -1], [16, 2, 2],  [0, 20],  "wood"),
            ("shelf",   [-6, 6, -3], [12, 2, 6],   [0, 24],  "wood"),
        ]),
        ("bottle1", [-2, 8, -1], [-22, 0, 6], [
            ("glass1", [-6, 6, -6], [4, 4, 8],  [0, 32],  "glass"),
            ("water1", [-5, 7, -5], [2, 2, 6],  [32, 0],  "water"),
            ("neck1",  [-5, 8, -8], [2, 2, 2],  [32, 8],  "glass"),
            ("cap1",   [-5, 9, -9], [2, 2, 1],  [32, 12], "wood"),
        ]),
        ("bottle2", [2, 8, 1], [24, 0, -8], [
            ("glass2", [2, 6, -2], [4, 4, 8],  [0, 44],  "glass"),
            ("water2", [3, 7, -1], [2, 2, 6],  [47, 0],  "water"),
            ("neck2",  [3, 8, 4],  [2, 2, 2],  [40, 12], "glass"),
            ("cap2",   [3, 9, 6],  [2, 2, 1],  [44, 12], "wood"),
        ]),
        ("sun", [0, 19, 0], [0, 0, 0], [
            ("sun_glint", [-1, 18, -1], [2, 2, 1], [16, 0], "glow"),
        ]),
    ]


def bio_sand_bones():
    return [
        ("body", [0, 0, 0], [0, 0, 0], [
            ("wall_n", [-5, 0, -5], [10, 14, 1], [0, 0],  "stone"),
            ("wall_s", [-5, 0, 4],  [10, 14, 1], [10, 0], "stone"),
            ("wall_w", [-5, 0, -5], [1, 14, 10], [20, 0], "stone"),
            ("wall_e", [4, 0, -5],  [1, 14, 10], [30, 0], "stone"),
            ("lid",    [-6, 14, -6], [12, 2, 12], [0, 16], "wood"),
            ("spout",  [2, 1, 4],    [4, 3, 3],   [0, 30], "wood"),
        ]),
        ("water", [0, 12, 0], [0, 0, 0], [
            ("water", [-4, 11, -4], [8, 1, 8], [0, 34], "water"),
        ]),
        ("biolayer", [0, 10, 0], [0, 0, 0], [
            ("biolayer", [-4, 9, -4], [8, 1, 8], [16, 34], "algae"),
        ]),
        ("drip1", [-2, 13, 0], [0, 0, 0], [
            ("drip1", [-2, 10, 0], [1, 3, 1], [0, 44], "water"),
        ]),
        ("drip2", [2, 13, 0], [0, 0, 0], [
            ("drip2", [2, 10, 0], [1, 3, 1], [4, 44], "water"),
        ]),
    ]


def still_bones():
    return [
        ("pot", [0, 6, 0], [0, 0, 0], [
            ("pot_body", [-6, 0, -6], [12, 6, 12], [0, 0],  "copper"),
            ("pot_rim",  [-6, 6, -6], [12, 1, 12], [0, 20], "copper_dark"),
        ]),
        ("dome", [0, 7, 0], [0, 0, 0], [
            ("dome_low",  [-5, 7, -5],  [10, 2, 10], [0, 26], "copper"),
            ("dome_high", [-4, 9, -4],  [8, 3, 8],   [0, 38], "copper"),
            ("pipe",      [-1, 11, -1], [2, 3, 2],   [0, 48], "copper_dark"),
        ]),
        ("arm", [0, 12, 0], [0, 0, 0], [
            ("tube", [-1, 12, 3], [2, 2, 7], [16, 48], "copper"),
        ]),
        ("receiver", [6, 8, 0], [0, 0, 0], [
            ("jug",       [3, 4, 10], [4, 8, 4], [24, 48], "glass"),
            ("jug_water", [4, 5, 11], [2, 5, 2], [36, 48], "water"),
        ]),
        ("water", [0, 5, 0], [0, 0, 0], [
            ("water", [-5, 1, -5], [10, 2, 10], [0, 52], "water"),
        ]),
        ("steam1", [-3, 13, 0], [0, 0, 0], [
            ("steam1", [-4, 12, -1], [2, 2, 2], [0, 52], "steam"),
        ]),
        ("steam2", [3, 13, 0], [0, 0, 0], [
            ("steam2", [3, 12, -1], [2, 2, 2], [4, 52], "steam"),
        ]),
        ("steam3", [0, 14, 0], [0, 0, 0], [
            ("steam3", [-1, 13, -1], [2, 2, 2], [8, 52], "steam"),
        ]),
    ]


def uv_bones():
    return [
        ("case", [0, 0, 0], [0, 0, 0], [
            ("base",   [-7, 0, -7],  [14, 2, 14],  [0, 0],  "enamel"),
            ("back",   [-7, 2, -7],  [14, 10, 2],  [0, 16], "enamel"),
            ("side_w", [-7, 2, 2],   [2, 10, 5],   [0, 28], "enamel"),
            ("side_e", [5, 2, 2],    [2, 10, 5],   [8, 28], "enamel"),
            ("top",    [-7, 12, -7], [14, 2, 14],  [0, 36], "enamel"),
            ("face",   [-7, 2, -5],  [14, 10, 2],  [0, 52], "glass"),
        ]),
        ("lamp", [0, 7, 0], [0, 0, 0], [
            ("lamp", [-4, 5, 3], [8, 2, 3], [0, 56], "glow"),
        ]),
        ("water", [0, 6, 0], [0, 0, 0], [
            ("water", [-6, 3, 2], [12, 1, 5], [16, 2], "water"),
        ]),
        ("bubble1", [-2, 6, 0], [0, 0, 0], [
            ("bubble1", [-3, 3, 1], [1, 1, 1], [16, 0], "glass"),
        ]),
        ("bubble2", [2, 6, 0], [0, 0, 0], [
            ("bubble2", [2, 3, 1], [1, 1, 1], [16, 4], "glass"),
        ]),
        ("bubble3", [0, 8, 0], [0, 0, 0], [
            ("bubble3", [-1, 5, 1], [1, 1, 1], [16, 6], "glass"),
        ]),
        ("outlet", [7, 4, 0], [0, 0, 0], [
            ("outlet", [6, 2, -6], [2, 4, 4], [16, 8], "metal"),
        ]),
        ("inlet", [-7, 4, 0], [0, 0, 0], [
            ("inlet", [-8, 2, -6], [2, 4, 4], [24, 8], "metal"),
        ]),
    ]


def clarifier_bones():
    return [
        ("tank", [0, 0, 0], [0, 0, 0], [
            ("tank_base", [-7, 0, -7], [14, 2, 14], [0, 0],  "wood"),
            ("wall_n",    [-7, 2, -7], [14, 8, 1],  [0, 16], "wood"),
            ("wall_s",    [-7, 2, 6],  [14, 8, 1],  [0, 25], "wood"),
            ("wall_w",    [-7, 2, -7], [1, 8, 14],  [0, 34], "wood"),
            ("wall_e",    [6, 2, -7],  [1, 8, 14],  [0, 42], "wood"),
            ("rim",       [-7, 10, -7], [14, 1, 14], [0, 48], "wood"),
            ("spout",     [2, 2, 6],   [4, 2, 2],   [16, 0], "metal"),
        ]),
        ("water", [0, 7, 0], [0, 0, 0], [
            ("water", [-6, 3, -6], [12, 2, 12], [16, 4], "water"),
        ]),
        ("paddle", [0, 7, 0], [0, 0, 0], [
            ("shaft",   [-1, 2, -1], [2, 10, 2], [16, 20], "wood"),
            ("blade_n", [-1, 4, -5], [2, 2, 2],  [16, 28], "wood"),
            ("blade_s", [-1, 4, 3],  [2, 2, 2],  [20, 28], "wood"),
            ("blade_w", [-5, 4, -1], [2, 2, 2],  [24, 28], "wood"),
            ("blade_e", [3, 4, -1],  [2, 2, 2],  [28, 28], "wood"),
        ]),
    ]


MACHINES = {
    "sodis_rack": wood_frame_bones(),
    "bio_sand_filter": bio_sand_bones(),
    "distillation_still": still_bones(),
    "uv_sterilizer": uv_bones(),
    "clarifier": clarifier_bones(),
}

VISIBLE_BOUNDS = {
    "sodis_rack": [3, 3.2, [0, 1.6, 0]],
    "bio_sand_filter": [2.5, 2.6, [0, 1.3, 0]],
    "distillation_still": [2.5, 2.8, [0, 1.4, 0]],
    "uv_sterilizer": [2.5, 2.6, [0, 1.3, 0]],
    "clarifier": [2.5, 2.6, [0, 1.3, 0]],
}


def box_faces(cube):
    """Compute the six explicit face rectangles for a cube under Blockbench box-UV rules."""
    u, v = cube[3]
    sx, sy, sz = cube[2]
    return {
        "west":  {"uv": [u, v + sz], "texture_size": [sz, sy]},
        "north": {"uv": [u + sz, v + sz], "texture_size": [sx, sy]},
        "south": {"uv": [u + sz + sx, v + sz], "texture_size": [sx, sy]},
        "east":  {"uv": [u + sz + sx + sx, v + sz], "texture_size": [sz, sy]},
        "up":    {"uv": [u + sz, v], "texture_size": [sx, sz]},
        "down":  {"uv": [u + sz + sx, v], "texture_size": [sx, sz]},
    }


def build_geo(model_id, bones):
    geo_bones = []
    for name, pivot, rotation, cubes in bones:
        geo_cubes = []
        for c in cubes:
            cname, origin, size, _uv, _material = c
            geo_cubes.append({
                "name": cname,
                "origin": origin,
                "size": size,
                "uv": _uv,
                "faces": box_faces(c),
            })
        geo_bones.append({
            "name": name,
            "pivot": pivot,
            "rotation": rotation,
            "cubes": geo_cubes,
        })
    width, height, offset = VISIBLE_BOUNDS[model_id]
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": f"geometry.terravera.{model_id}",
                "texture_width": TEX_SIZE,
                "texture_height": TEX_SIZE,
                "visible_bounds_width": width,
                "visible_bounds_height": height,
                "visible_bounds_offset": offset,
            },
            "bones": geo_bones,
        }],
    }, geo_bones


def validate_bounds(model_id, bones):
    for name, _p, _r, cubes in bones:
        for c in cubes:
            cname, origin, size, _uv, _mat = c
            faces = box_faces(c)
            for face, spec in faces.items():
                u, v = spec["uv"]
                w, h = spec["texture_size"]
                if u < 0 or v < 0 or u + w > TEX_SIZE or v + h > TEX_SIZE:
                    raise ValueError(f"{model_id}/{name}/{cname} face {face} overflows atlas: "
                                     f"({u},{v}) {w}x{h}")


# --------------------------------------------------------------------------------------------------------------------
# PNG writer
# --------------------------------------------------------------------------------------------------------------------

def write_png(path, pixels):
    height = len(pixels)
    width = len(pixels[0])
    raw = b"".join(b"\x00" + b"".join(bytes(px) for px in row) for row in pixels)

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(raw, 9))
           + chunk(b"IEND", b""))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)
    print(f"wrote {path.relative_to(ROOT)} ({width}x{height})")


# --------------------------------------------------------------------------------------------------------------------
# Material painters. Each paints the (x0,y0)-(x1,y1) region of an RGBA image with a recognizable material.
# --------------------------------------------------------------------------------------------------------------------

def _shade(base, rng, amount=14):
    return tuple(max(0, min(255, int(c + rng.uniform(-amount, amount)))) for c in base[:3]) + (base[3] if len(base) > 3 else 255,)


def paint_wood(img, rect, face, rng, base=(166, 124, 82)):
    x0, y0, x1, y1 = rect
    horizontal = face in ("north", "south", "east", "west")
    for y in range(y0, y1):
        for x in range(x0, x1):
            px = _shade(base, rng, 10)
            if horizontal:
                if (y - y0) % 4 == 0:
                    px = (px[0] - 34, px[1] - 26, px[2] - 18, px[3])
                elif (y - y0) % 4 == 3:
                    px = (min(255, px[0] + 12), min(255, px[1] + 10), min(255, px[2] + 8), px[3])
            else:
                if (x - x0) % 4 == 0:
                    px = (px[0] - 30, px[1] - 24, px[2] - 16, px[3])
            # faint grain knots
            if rng.random() < 0.05:
                px = (px[0] - 22, px[1] - 18, px[2] - 12, px[3])
            img[y][x] = px


def paint_copper(img, rect, face, rng, dark=False):
    x0, y0, x1, y1 = rect
    base = (168, 96, 58) if not dark else (140, 74, 46)
    for y in range(y0, y1):
        for x in range(x0, x1):
            t = ((y - y0) / max(1, (y1 - y0))) if face not in ("up", "down") else ((x - x0) / max(1, (x1 - x0)))
            r = int(196 - 40 * t + rng.uniform(-8, 8))
            g = int(120 - 34 * t + rng.uniform(-8, 8))
            b = int(72 - 24 * t + rng.uniform(-8, 8))
            px = (max(0, min(255, r)), max(0, min(255, g)), max(0, min(255, b)), 255)
            if rng.random() < 0.06:  # patina flecks
                px = (70, 120, 105, 255)
            img[y][x] = px


def paint_glass(img, rect, face, rng, alpha=190):
    x0, y0, x1, y1 = rect
    for y in range(y0, y1):
        for x in range(x0, x1):
            t = ((x - x0) / max(1, (x1 - x0)) + (y - y0) / max(1, (y1 - y0))) / 2
            px = (int(180 + 40 * t + rng.uniform(-6, 6)),
                  int(212 + 30 * t + rng.uniform(-6, 6)),
                  int(228 + 22 * t + rng.uniform(-6, 6)), alpha)
            if rng.random() < 0.10:
                px = (245, 250, 255, alpha + 20)
            img[y][x] = px


def paint_water(img, rect, face, rng, alpha=205):
    x0, y0, x1, y1 = rect
    for y in range(y0, y1):
        for x in range(x0, x1):
            t = ((y - y0) / max(1, (y1 - y0))) if face not in ("up", "down") else ((x - x0) / max(1, (x1 - x0)))
            r = int(52 - 14 * t + rng.uniform(-4, 4))
            g = int(96 - 20 * t + rng.uniform(-4, 4))
            b = int(196 - 26 * t + rng.uniform(-4, 4))
            px = (max(0, min(255, r)), max(0, min(255, g)), max(0, min(255, b)), alpha)
            if face == "up" and rng.random() < 0.12:
                px = (190, 220, 250, alpha + 25)  # ripple glints on the surface
            img[y][x] = px


def paint_steam(img, rect, face, rng, alpha=150):
    x0, y0, x1, y1 = rect
    for y in range(y0, y1):
        for x in range(x0, x1):
            a = int(alpha * (0.6 + 0.4 * rng.random()))
            img[y][x] = (235, 240, 248, min(255, a))


def paint_enamel(img, rect, face, rng):
    x0, y0, x1, y1 = rect
    for y in range(y0, y1):
        for x in range(x0, x1):
            px = _shade((232, 230, 220), rng, 8)
            if y == y0 or y == y1 - 1 or x == x0 or x == x1 - 1:
                px = (196, 194, 184, 255)
            if face in ("up",) and (x - x0) % 6 < 2:
                px = (58, 108, 158, 255)  # a stripe of industrial blue on the top
            img[y][x] = px


def paint_glow(img, rect, face, rng):
    x0, y0, x1, y1 = rect
    for y in range(y0, y1):
        for x in range(x0, x1):
            t = ((x - x0) / max(1, (x1 - x0)))
            core = int(150 + 100 * t)
            px = (min(255, core), min(255, core + 20), 255, 240)
            if rng.random() < 0.15:
                px = (255, 255, 255, 255)
            img[y][x] = px


def paint_stone(img, rect, face, rng):
    x0, y0, x1, y1 = rect
    for y in range(y0, y1):
        for x in range(x0, x1):
            px = _shade((146, 142, 134), rng, 16)
            if rng.random() < 0.08:
                px = (168, 164, 156, 255)
            if rng.random() < 0.06:
                px = (118, 114, 108, 255)
            img[y][x] = px


def paint_algae(img, rect, face, rng):
    x0, y0, x1, y1 = rect
    for y in range(y0, y1):
        for x in range(x0, x1):
            px = _shade((74, 96, 46), rng, 10)
            if rng.random() < 0.18:
                px = (52, 74, 34, 255)
            img[y][x] = px


def paint_metal(img, rect, face, rng):
    x0, y0, x1, y1 = rect
    for y in range(y0, y1):
        for x in range(x0, x1):
            px = _shade((150, 156, 162), rng, 8)
            if (x - x0 + y - y0) % 5 == 0:
                px = (120, 126, 132, 255)
            img[y][x] = px


PAINTERS = {
    "wood": paint_wood,
    "copper": paint_copper,
    "copper_dark": lambda img, r, f, rng: paint_copper(img, r, f, rng, dark=True),
    "glass": paint_glass,
    "water": paint_water,
    "steam": paint_steam,
    "enamel": paint_enamel,
    "glow": paint_glow,
    "stone": paint_stone,
    "algae": paint_algae,
    "metal": paint_metal,
}


def paint_block_texture(model_id, bones):
    """Paint the 64x64 atlas from the source cube tuples so texture UVs exactly match the model."""
    rng = random.Random(0x5EED ^ sum(ord(c) for c in model_id))
    img = [[(0, 0, 0, 0)] * TEX_SIZE for _ in range(TEX_SIZE)]
    for _name, _p, _r, cubes in bones:
        for c in cubes:
            _cname, _origin, _size, _uv, material = c
            painter = PAINTERS[material]
            for face, spec in box_faces(c).items():
                u, v = spec["uv"]
                w, h = spec["texture_size"]
                painter(img, (u, v, u + w, v + h), face, rng)
    write_png(BLOCK_TEX / f"{model_id}.png", img)


# --------------------------------------------------------------------------------------------------------------------
# Blockstates / models
# --------------------------------------------------------------------------------------------------------------------

def build_blockstate(model_id):
    variants = {}
    for facing in ("north", "south", "east", "west"):
        for level in range(5):
            for active in ("false", "true"):
                variants[f"facing={facing},water_level={level},active={active}"] = {
                    "model": f"terravera:block/{model_id}"
                }
    return {"variants": variants}


BLOCK_ITEM_MODEL = {
    "parent": "minecraft:builtin/entity",
    "display": {
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.4, 0.4, 0.4]},
        "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.4, 0.4, 0.4]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.45, 0.45, 0.45]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [0.45, 0.45, 0.45]},
        "gui": {"rotation": [30, 225, 0], "scale": [0.625, 0.625, 0.625]},
        "ground": {"translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
        "fixed": {"scale": [0.5, 0.5, 0.5]},
    },
}


def generate_models_and_states():
    for model_id, bones in MACHINES.items():
        geo, _geo_bones = build_geo(model_id, bones)
        validate_bounds(model_id, bones)
        (GEO / f"{model_id}.geo.json").write_text(json.dumps(geo, indent=2))
        (BLOCKSTATES / f"{model_id}.json").write_text(json.dumps(build_blockstate(model_id), indent=2))
        block_model = {"parent": "minecraft:builtin/entity",
                       "textures": {"particle": f"terravera:block/{model_id}"}}
        (MODELS_BLOCK / f"{model_id}.json").write_text(json.dumps(block_model, indent=2))
        (MODELS_ITEM / f"{model_id}.json").write_text(json.dumps(BLOCK_ITEM_MODEL, indent=2))
        paint_block_texture(model_id, bones)
        print(f"generated {model_id}")


# --------------------------------------------------------------------------------------------------------------------
# Item icons (16x16 pixel art)
# --------------------------------------------------------------------------------------------------------------------

def new_item_canvas():
    return [[(0, 0, 0, 0)] * 16 for _ in range(16)]


def rect(img, x0, y0, x1, y1, color):
    for y in range(max(0, y0), min(16, y1)):
        for x in range(max(0, x0), min(16, x1)):
            img[y][x] = color


def add_noise(img, rng, amount=10):
    for y in range(16):
        for x in range(16):
            px = img[y][x]
            if px[3] > 0:
                img[y][x] = tuple(max(0, min(255, int(c + rng.uniform(-amount, amount)))) for c in px[:3]) + (px[3],)


def bottle_icon(base, liquid, cap=(120, 90, 60)):
    rng = random.Random(7)
    img = new_item_canvas()
    rect(img, 6, 2, 10, 4, cap)          # cap
    rect(img, 7, 4, 9, 6, (180, 200, 210, 230))  # neck
    rect(img, 5, 6, 11, 14, (180, 200, 210, 230))  # body
    rect(img, 6, 8, 10, 13, liquid)       # liquid
    rect(img, 5, 14, 11, 15, (120, 130, 140, 255))  # base shade
    add_noise(img, rng, 6)
    return img


def draw_item_icons():
    rng = random.Random(11)
    icons = {}

    # boiling stones - a cluster of fire-warmed river stones
    img = new_item_canvas()
    for (cx, cy, r, c) in [(4, 10, 3, (120, 116, 108)), (8, 11, 3, (96, 92, 88)),
                           (12, 9, 2, (136, 130, 120)), (7, 7, 2, (148, 142, 132))]:
        for y in range(16):
            for x in range(16):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                    img[y][x] = _shade(c, rng, 12) + (255,)
    for (x, y) in [(3, 6), (5, 7), (9, 5), (11, 8)]:  # heat shimmer
        rect(img, x, y, x + 1, y + 1, (255, 190, 120, 180))
    icons["boiling_stones"] = img

    # cloth filter - folded burlap square with a cordage tie
    img = new_item_canvas()
    rect(img, 2, 3, 14, 14, (172, 138, 96, 255))
    rect(img, 2, 3, 14, 4, (150, 118, 80, 255))
    rect(img, 6, 0, 10, 3, (120, 96, 66, 255))   # knot
    add_noise(img, rng, 14)
    icons["cloth_filter"] = img

    # unfired ceramic filter candle - pale clay cylinder
    img = new_item_canvas()
    rect(img, 5, 2, 11, 15, (196, 168, 128, 255))
    rect(img, 5, 2, 11, 3, (178, 150, 112, 255))
    rect(img, 6, 3, 10, 14, (188, 160, 122, 255))
    add_noise(img, rng, 8)
    icons["unfired_ceramic_filter_candle"] = img

    # fired ceramic filter candle - terracotta with crosshatch
    img = new_item_canvas()
    rect(img, 5, 2, 11, 15, (178, 96, 62, 255))
    rect(img, 5, 2, 11, 3, (158, 82, 52, 255))
    rect(img, 6, 3, 10, 14, (168, 90, 58, 255))
    for i in range(3, 14, 2):
        rect(img, 6, i, 10, i + 1, (150, 76, 48, 255))
    add_noise(img, rng, 6)
    icons["ceramic_filter_candle"] = img

    # chlorine tablets - white puck stack
    img = new_item_canvas()
    rect(img, 4, 3, 12, 13, (235, 240, 242, 255))
    rect(img, 4, 3, 12, 5, (215, 222, 226, 255))
    rect(img, 4, 8, 12, 10, (215, 222, 226, 255))
    rect(img, 7, 4, 9, 5, (150, 170, 190, 255))
    rect(img, 7, 9, 9, 10, (150, 170, 190, 255))
    icons["chlorine_tablets"] = img

    # iodine drops - dark brown dropper bottle
    img = bottle_icon((96, 52, 26, 255), (110, 60, 30, 255), (60, 40, 30, 255))
    icons["iodine_drops"] = img

    # permanganate crystals - vivid purple crystals
    img = new_item_canvas()
    rect(img, 3, 8, 13, 14, (86, 70, 120, 255))
    rect(img, 5, 5, 8, 8, (148, 70, 170, 255))
    rect(img, 9, 3, 11, 6, (172, 92, 190, 255))
    rect(img, 10, 7, 13, 9, (120, 66, 150, 255))
    for (x, y) in [(6, 6), (10, 4)]:
        rect(img, x, y, x + 1, y + 1, (240, 210, 250, 255))
    icons["permanganate_crystals"] = img

    # citrus juice - small green-tinted bottle
    img = bottle_icon((150, 210, 90, 230), (130, 190, 70, 240), (80, 110, 50, 255))
    icons["citrus_juice"] = img

    # colloidal silver - cloudy grey-blue bottle
    img = bottle_icon((168, 176, 190, 230), (150, 160, 176, 240), (90, 98, 110, 255))
    icons["colloidal_silver"] = img

    # moringa seed powder - tan pouch with green seed
    img = new_item_canvas()
    rect(img, 3, 5, 13, 13, (168, 142, 96, 255))
    rect(img, 5, 2, 11, 6, (150, 124, 82, 255))
    rect(img, 6, 7, 10, 11, (104, 128, 64, 255))
    rect(img, 7, 8, 9, 10, (84, 108, 52, 255))
    icons["moringa_seed_powder"] = img

    # alum crystals - white translucent prisms
    img = new_item_canvas()
    rect(img, 3, 10, 13, 14, (205, 210, 218, 255))
    rect(img, 5, 6, 8, 9, (228, 232, 240, 255))
    rect(img, 9, 4, 12, 7, (240, 244, 250, 255))
    rect(img, 10, 8, 13, 11, (222, 228, 236, 255))
    for (x, y) in [(6, 7), (10, 5)]:
        rect(img, x, y, x + 1, y + 1, (255, 255, 255, 255))
    icons["alum_crystals"] = img

    # ro membrane - white spiral-wound cartridge
    img = new_item_canvas()
    rect(img, 5, 2, 11, 14, (232, 236, 240, 255))
    rect(img, 5, 2, 11, 3, (206, 212, 218, 255))
    rect(img, 5, 13, 11, 14, (206, 212, 218, 255))
    for i, (y0, y1) in enumerate([(4, 5), (6, 7), (8, 9), (10, 11)]):
        x0 = 6 + (i % 2)
        rect(img, x0, y0, x0 + 1, y1, (160, 172, 184, 255))
    icons["ro_membrane"] = img

    for name, img in icons.items():
        write_png(ITEM_TEX / f"{name}.png", img)
        item_model = {"parent": "minecraft:item/generated",
                      "textures": {"layer0": f"terravera:item/{name}"}}
        (MODELS_ITEM / f"{name}.json").write_text(json.dumps(item_model, indent=2))


# --------------------------------------------------------------------------------------------------------------------
# Lang
# --------------------------------------------------------------------------------------------------------------------

def merge_lang():
    entries = {
        "block.terravera.sodis_rack": "Solar Disinfection Rack",
        "block.terravera.bio_sand_filter": "Bio-Sand Filter",
        "block.terravera.distillation_still": "Copper Distillation Still",
        "block.terravera.uv_sterilizer": "UV Sterilizer",
        "block.terravera.clarifier": "Clarifier Tank",
        "item.terravera.sodis_rack": "Solar Disinfection Rack",
        "item.terravera.bio_sand_filter": "Bio-Sand Filter",
        "item.terravera.distillation_still": "Copper Distillation Still",
        "item.terravera.uv_sterilizer": "UV Sterilizer",
        "item.terravera.clarifier": "Clarifier Tank",
        "item.terravera.boiling_stones": "Fire-Heated Boiling Stones",
        "item.terravera.cloth_filter": "Cloth Filter",
        "item.terravera.ceramic_filter_candle": "Ceramic Filter Candle",
        "item.terravera.unfired_ceramic_filter_candle": "Unfired Ceramic Filter Candle",
        "item.terravera.chlorine_tablets": "Chlorine Tablets",
        "item.terravera.iodine_drops": "Iodine Tincture",
        "item.terravera.permanganate_crystals": "Potassium Permanganate Crystals",
        "item.terravera.citrus_juice": "Sour Citrus Juice",
        "item.terravera.colloidal_silver": "Colloidal Silver",
        "item.terravera.moringa_seed_powder": "Moringa Seed Powder",
        "item.terravera.alum_crystals": "Alum Crystals",
        "item.terravera.ro_membrane": "Reverse-Osmosis Membrane",
    }
    for tid, name in [
        ("untreated", "Untreated"), ("settled", "Settled"), ("cloth_filtered", "Cloth-Filtered"),
        ("acidified", "Acidified"), ("flocculated", "Flocculated"), ("filtered", "Charcoal-Filtered"),
        ("bio_filtered", "Bio-Sand Filtered"), ("ceramic_filtered", "Ceramic-Filtered"),
        ("silvered", "Silver-Treated"), ("pasteurized", "Pasteurized"), ("solar_disinfected", "Solar-Disinfected"),
        ("iodized", "Iodine-Treated"), ("permanganate", "Permanganate-Treated"),
        ("chlorinated", "Chlorinated"), ("uv_sterilized", "UV-Sterilized"), ("ro_purified", "RO-Purified"),
        ("distilled", "Distilled"), ("boiled", "Boiled"),
    ]:
        entries[f"terravera.water_treatment.{tid}"] = name

    methods = {
        "boiling": ("Boiling in a Pot", "Hold a filled container against a TFC firepit pot that is at or above 100C and the water inside is marked boiled. The clay pot is one of the first things a TFC player builds, so the complete answer to waterborne disease is available on day one - for the price of fuel, every single time.", "Clay pot, firepit, fuel."),
        "rolling_boil": ("Rolling Boil (5 Minutes)", "The sustained version of boiling. A brief boil is not a boil: keep the pot rolling for a full five minutes and even the hardiest cysts - Giardia and Cryptosporidium - are dead. TerraVera treats any pot boiled to 100C as having had the full rolling boil.", "Clay pot, firepit, and enough fuel for a sustained burn."),
        "pasteurization": ("Pasteurization (63-99C)", "Hold a pot hot but below the boil and the water is pasteurized: most bacteria and viruses die, but the tough protozoan cysts survive. It is what you get when you cannot quite reach a boil - better than nothing, never as good as boiling.", "TFC pot held between 63C and 99C."),
        "stone_boiling": ("Stone Boiling", "Drop fire-heated stones into a container of water. The stones carry their heat into the water and raise it past pasteurization without a pot over the fire. The classic no-pot trick; use the Boiling Stones on a filled container.", "Boiling Stones (rocks heated in a TFC kiln or forge), any water container."),
        "sodis": ("SODIS - Solar Water Disinfection", "Glass bottles on a rack, tilted into the sun. Six hours of clear daylight lets the UV in sunlight do the disinfection - free, fireless, and one of the great emergency methods. The SODIS Rack needs clear sky and daylight to process.", "SODIS Rack, glass bottles, a clear day."),
        "solar_still": ("Solar Still Distillation", "The passive Solar Still collector condenses vapour, so the water it produces has effectively been distilled by the sun. It is slow - one bucket per long day - but the water that comes out is as clean as water gets.", "Solar Still collector, open sky, daylight."),
        "distillation": ("Distillation", "Boil water in the copper still and condense the steam back down. Nothing survives the round trip; the distillate is the cleanest water in the game. Costs copper, fuel, and time - the still is the mid-game answer.", "Copper Distillation Still, heat, fuel."),
        "cloth_filter": ("Cloth Filtration", "Fold any woven cloth - burlap, linen, a shirt - and pour through it. It catches grit, mud, and the biggest parasites. It is the cheapest method in the catalogue and exactly as good as that suggests.", "Burlap or linen cloth, cordage, a container."),
        "charcoal_filter": ("Sand-and-Charcoal Filter", "The TerraVera water filter: sand and charcoal in a frame. It removes the protozoa and most of the load, but leaves the bacteria - cholera and typhoid go straight through it. The field answer; boiling is still the camp answer.", "Water Filter item: sand, charcoal, cordage, cloth."),
        "bio_sand": ("Bio-Sand Filtration", "A barrel of graded sand and gravel with a living layer of organisms on top. The biology eats the pathogens while gravity pulls the water through. The Bio-Sand Filter is the cheap always-on workhorse of a settled camp.", "Bio-Sand Filter block: sand, gravel, burlap, cordage."),
        "ceramic_filter": ("Ceramic Candle Filtration", "A fired clay candle with pores small enough to strain out bacteria. Fragile, but reusable for a very long time if rinsed. The candle starts as an unfired clay piece and must go through a TFC kiln.", "Ceramic Filter Candle (fired from clay in a TFC kiln)."),
        "settling": ("Settling / Sedimentation", "Let the water stand and the mud falls out, taking a surprising fraction of the load with it. The Clarifier Tank does it properly, with a slow paddle and an overflow spout. It never fully sterilizes - but it makes everything else work better.", "Clarifier Tank, time."),
        "flocculation": ("Flocculation (Alum)", "Stir in alum and the suspended particles clump into flakes heavy enough to sink, dragging pathogens down with them. The method real water-treatment plants use; the Alum Crystals do it for one container at a time.", "Alum Crystals, a container, and time to settle."),
        "moringa": ("Moringa Seed Powder", "Crushed moringa seeds are a traditional flocculant used across Africa and Asia for thousands of years. The powder makes the fine particles clump and settle, exactly like alum but from a seed.", "Moringa Seed Powder, a container, and time to settle."),
        "chlorination": ("Chlorination", "Chlorine is the world's default answer to bad water: cheap, fast, and lethal to bacteria and viruses. Its one famous weakness is Cryptosporidium, which shrugs it off - boil if you can.", "Chlorine Tablets (salt, lye, and charcoal, boiled in a TFC pot)."),
        "iodination": ("Iodine Disinfection", "Iodine tincture, a few drops per litre. The backpacker's classic: light, fast, reliable against bacteria and viruses. Leave it to work, and expect the taste.", "Iodine Tincture (wood ash and salt, boiled in a TFC pot)."),
        "permanganate": ("Potassium Permanganate", "A strong oxidizer sold as deep-purple crystals. A few crystals turn the water pink and kill most pathogens; the water will taste of iron afterwards. A standard field-kit method.", "Permanganate Crystals (charcoal and salt, boiled in a TFC pot)."),
        "acidification": ("Acidification (Citrus / Vinegar)", "The emergency field trick: sour the water hard and wait. Acid slows most bacteria down - it buys you time on the trail, not safety. The last resort that is still a real method.", "Sour Citrus Juice (citrus or TFC vinegar)."),
        "silver": ("Colloidal Silver", "Silver ions are mildly antimicrobial and - uniquely among chemical methods - keep working for days, which makes silver-treated water stay good in storage. Slow, mild, persistent.", "Colloidal Silver (silver ingot and salt, boiled in a TFC pot)."),
        "uv": ("Ultraviolet Sterilization", "A UV lamp in a glass chamber. The radiation shreds the DNA of almost everything that passes through - bacteria, viruses, protozoa. A few exceptionally tough spores survive, which is why RO and distillation still outrank it.", "UV Sterilizer block (iron, glass, an RO membrane)."),
        "reverse_osmosis": ("Reverse Osmosis", "Force the water through a membrane whose pores are smaller than anything alive. The Reverse-Osmosis Membrane item does it for one container at a time - the most thorough portable filter that exists.", "Reverse-Osmosis Membrane (wrought-iron sheets and burlap). A full machine version is planned."),
        "freezing": ("Freezing and Melting", "Water that froze and then melted carries far less than the water it came from - the ice crystals exclude the pathogens. It is not sterilization: some bacteria survive the freeze. Cold, high, running water is simply the safest natural source there is.", "A hard winter. The TerraVera disease system already models cold water as cleaner."),
    }
    for mid, (name, desc, materials) in methods.items():
        entries[f"terravera.sterilization.{mid}.name"] = name
        entries[f"terravera.sterilization.{mid}.desc"] = desc
        entries[f"terravera.sterilization.{mid}.materials"] = materials

    for cat in ["heat", "solar", "distillation", "filtration", "settling", "chemical", "physical"]:
        entries[f"terravera.sterilization.category.{cat}"] = cat.title()

    entries.update({
        "terravera.sterilizer.capacity": "Holds %s mB of water",
        "terravera.sterilizer.process": "Processes a full batch in %s seconds",
        "terravera.sterilizer.status": "Water: %s/%s mB - batch %s%% - %s",
        "terravera.sterilizer.status_done": "ready",
        "terravera.sterilizer.status_waiting": "working",
        "terravera.sterilizer.sodis_rack_hint": "Glass bottles tilted into the sun. Processes only in clear daylight",
        "terravera.sterilizer.bio_sand_filter_hint": "Gravity-fed sand and gravel; the living layer does the work",
        "terravera.sterilizer.distillation_still_hint": "Boils and condenses; the distillate is completely clean",
        "terravera.sterilizer.uv_sterilizer_hint": "UV lamp chamber; nearly everything dies in it",
        "terravera.sterilizer.clarifier_hint": "Settling tank; mud falls out and takes the load with it",
        "terravera.water.pasteurized": "Water pasteurized - most pathogens killed, cysts survive",
        "terravera.water.boiled": "Water boiled - safe",
        "terravera.water.treatment_needs_container": "Hold a container of water in the other hand",
        "terravera.water.already_better": "This water is already treated at least as well: %s",
        "terravera.water.treated_with": "Water treated: %s",
        "terravera.tooltip.treatment_item": "Treats water: %s",
        "terravera.tooltip.treatment_how": "Right-click while holding a container of water",
        "terravera.tooltip.treatment_uses": "%s uses",
    })

    lang = json.loads(LANG.read_text(encoding="utf-8"))
    lang.update(entries)
    LANG.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"merged {len(entries)} lang entries into en_us.json")


def main():
    generate_models_and_states()
    draw_item_icons()
    merge_lang()


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Procedurally generates TerraVera's power-system textures.

Each machine texture is a 32x32 atlas split into four 16x16 quadrants that the geo models sample by colour role:

    top-left     main body material   (steel / painted case / wood)
    top-right    secondary material   (blades / grille / steel fittings)
    bottom-left  panels and detail    (dark steel / side panels / dark wood)
    bottom-right copper/brass terminals

Wire textures are 16x16; each has a normal and a 'lit' (energised) variant.
"""
import random
import struct
import zlib
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/terravera/textures/block"


def write_png(path: Path, pixels):
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
    print(f"wrote {path.relative_to(OUT.parents[3])} ({width}x{height})")


def jitter(rng, base, spread):
    return tuple(max(0, min(255, int(c + rng.uniform(-spread, spread)))) for c in base)


def fill_quadrant(pixels, rng, x0, y0, base, spread):
    for y in range(y0, y0 + 16):
        for x in range(x0, x0 + 16):
            pixels[y][x] = jitter(rng, base, spread) + (255,)


def machine_atlas(name, tl, tr, bl, br, spread=14, seed=1):
    rng = random.Random(seed)
    pixels = [[(0, 0, 0, 255)] * 32 for _ in range(32)]
    fill_quadrant(pixels, rng, 0, 0, tl, spread)
    fill_quadrant(pixels, rng, 16, 0, tr, spread)
    fill_quadrant(pixels, rng, 0, 16, bl, spread)
    fill_quadrant(pixels, rng, 16, 16, br, spread)
    write_png(OUT / name, pixels)


def wire_texture(name, base, spread, seed, lit=False, glow=(255, 170, 60)):
    rng = random.Random(seed)
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            px = jitter(rng, base, spread)
            if lit:
                # Energised conductors pick up a warm emissive bloom, strongest on the copper core.
                strength = 0.35 + 0.25 * ((x + y) % 3 == 0)
                px = tuple(int(px[i] * (1 - strength) + glow[i] * strength) for i in range(3))
            row.append(px + (255,))
        pixels.append(row)
    write_png(OUT / name, pixels)


def main():
    # Wind turbine: light steel tower, off-white blades, dark nacelle, copper terminals.
    machine_atlas("wind_turbine.png", (154, 164, 173), (232, 232, 226), (88, 97, 107), (184, 115, 51), seed=11)
    # Air conditioner: painted case, dark grille steel, light side panels, copper terminal block.
    machine_atlas("air_conditioner_unit.png", (229, 231, 227), (58, 63, 68), (200, 204, 201), (184, 115, 51), seed=22)
    # Hand crank: oiled wood base, steel axle and arm, dark turned-wood grip, brass terminals.
    machine_atlas("hand_crank_unit.png", (138, 90, 43), (154, 164, 173), (95, 61, 30), (201, 134, 60), seed=33)

    # Conductors.
    wire_texture("single_wire.png", (196, 124, 62), 18, seed=44)
    wire_texture("single_wire_lit.png", (196, 124, 62), 18, seed=44, lit=True)
    wire_texture("copper_wire_lit.png", (178, 110, 74), 16, seed=55, lit=True, glow=(255, 190, 90))
    wire_texture("wire_intersection.png", (128, 134, 140), 16, seed=66)
    wire_texture("wire_intersection_lit.png", (128, 134, 140), 16, seed=66, lit=True, glow=(255, 200, 110))


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
import os
import struct
import zlib

def write_png(filename, width, height, pixels):
    os.makedirs(os.path.dirname(filename), exist_ok=True)
    with open(filename, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        # IHDR
        ihdr = struct.pack(">2I5B", width, height, 8, 2, 0, 0, 0)
        f.write(struct.pack(">I", len(ihdr)) + b"IHDR" + ihdr + struct.pack(">I", zlib.crc32(b"IHDR" + ihdr)))
        # IDAT
        raw_data = bytearray()
        for y in range(height):
            raw_data.append(0)  # filter type 0
            for x in range(width):
                r, g, b = pixels[y * width + x]
                raw_data.extend([r, g, b])
        idat = zlib.compress(raw_data)
        f.write(struct.pack(">I", len(idat)) + b"IDAT" + idat + struct.pack(">I", zlib.crc32(b"IDAT" + idat)))
        # IEND
        f.write(struct.pack(">I", 0) + b"IEND" + struct.pack(">I", zlib.crc32(b"IEND")))

def make_carcass_rack_texture(filename, hide_color):
    width, height = 64, 64
    pixels = [(0, 0, 0)] * (width * height)
    
    # Define colors
    wood_color = (139, 90, 43)    # Rack frame wood
    iron_color = (120, 120, 120)  # Hook iron
    meat_color = (180, 40, 40)    # Red muscle meat
    fat_color = (230, 220, 150)   # Yellowish white fat
    bone_color = (240, 235, 220)  # Skeleton bone white
    organ_color = (120, 20, 40)   # Dark liver/organ red
    blood_color = (200, 10, 10)   # Blood mark red
    
    for y in range(height):
        for x in range(width):
            # UV mapping regions matching geo.json
            if y < 32 and x < 32: # Wood frame (uv 0..32, 0..32)
                pixels[y * width + x] = wood_color if ((x + y) % 2 == 0) else (125, 80, 38)
            elif y < 16 and 32 <= x < 48: # Hook iron
                pixels[y * width + x] = iron_color
            elif y >= 32 and x < 36: # Hide torso / legs
                r, g, b = hide_color
                noise = 10 if ((x * 7 + y * 13) % 2 == 0) else -10
                pixels[y * width + x] = (max(0, min(255, r + noise)), max(0, min(255, g + noise)), max(0, min(255, b + noise)))
            elif y < 32 and x >= 48: # Blood marks
                pixels[y * width + x] = blood_color
            elif 12 <= y < 32 and x < 48: # Meat cuts (ribs, loin, leg, shoulder)
                pixels[y * width + x] = meat_color if ((x + y) % 3 != 0) else (160, 30, 30)
            elif y >= 40 and x >= 40: # Organs & fat
                pixels[y * width + x] = organ_color if (x < 52) else fat_color
            elif 12 <= y < 40 and 36 <= x < 60: # Skeleton bones
                pixels[y * width + x] = bone_color
            else:
                pixels[y * width + x] = hide_color

    write_png(filename, width, height, pixels)

def make_butcher_knife_texture(filename, blade_color, handle_color=(120, 70, 30)):
    width, height = 16, 16
    pixels = [(0, 0, 0)] * (width * height)
    
    # Simple icon: handle bottom-left, broad butcher cleaver/knife blade top-right
    for y in range(height):
        for x in range(width):
            if (x == y and x < 5) or (x == y + 1 and x < 5) or (x + 1 == y and y < 5):
                pixels[y * width + x] = handle_color
            elif 4 <= x <= 13 and 3 <= y <= 12 and (x + y >= 9) and (y - x <= 2):
                pixels[y * width + x] = blade_color if ((x + y) % 2 == 0) else (
                    min(255, blade_color[0] + 20), min(255, blade_color[1] + 20), min(255, blade_color[2] + 20))
            else:
                pixels[y * width + x] = (255, 255, 255) # background (or let's use transparent if RGBA, but RGB with standard background is ok, let's use RGBA!)

def write_png_rgba(filename, width, height, pixels_rgba):
    os.makedirs(os.path.dirname(filename), exist_ok=True)
    with open(filename, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        ihdr = struct.pack(">2I5B", width, height, 8, 6, 0, 0, 0) # color type 6 = RGBA
        f.write(struct.pack(">I", len(ihdr)) + b"IHDR" + ihdr + struct.pack(">I", zlib.crc32(b"IHDR" + ihdr)))
        raw_data = bytearray()
        for y in range(height):
            raw_data.append(0)
            for x in range(width):
                r, g, b, a = pixels_rgba[y * width + x]
                raw_data.extend([r, g, b, a])
        idat = zlib.compress(raw_data)
        f.write(struct.pack(">I", len(idat)) + b"IDAT" + idat + struct.pack(">I", zlib.crc32(b"IDAT" + idat)))
        f.write(struct.pack(">I", 0) + b"IEND" + struct.pack(">I", zlib.crc32(b"IEND")))

def make_knife_rgba(filename, blade_color, handle_color=(120, 70, 30, 255)):
    width, height = 16, 16
    pixels = [(0, 0, 0, 0)] * (width * height)
    
    for y in range(height):
        for x in range(width):
            if (3 <= x <= 6 and 10 <= y <= 13 and (x + y >= 14)):
                pixels[y * width + x] = handle_color
            elif 5 <= x <= 13 and 2 <= y <= 10 and (x + y >= 9) and (x - y >= -2):
                r, g, b = blade_color
                if (x + y) % 2 == 0:
                    r = min(255, r + 25)
                    g = min(255, g + 25)
                    b = min(255, b + 25)
                pixels[y * width + x] = (r, g, b, 255)
    write_png_rgba(filename, width, height, pixels)

if __name__ == "__main__":
    base_block = "src/main/resources/assets/terravera/textures/block/carcass_rack"
    species_colors = {
        "default": (139, 90, 43),
        "cattle": (120, 70, 35),
        "deer": (150, 100, 50),
        "pig": (210, 150, 140),
        "sheep": (235, 230, 215),
        "goat": (190, 190, 185),
        "small_game": (140, 95, 60),
        "fowl": (170, 140, 100),
        "large_game": (95, 60, 35),
        "predator": (130, 110, 85)
    }
    for name, col in species_colors.items():
        make_carcass_rack_texture(f"{base_block}/{name}.png", col)
    print("Generated 10 carcass rack textures.")

    base_item = "src/main/resources/assets/terravera/textures/item"
    knives = {
        "wrought_iron_butchers_knife": (180, 180, 185),
        "steel_butchers_knife": (150, 160, 170),
        "black_steel_butchers_knife": (45, 50, 55),
        "blue_steel_butchers_knife": (60, 90, 180),
        "red_steel_butchers_knife": (180, 50, 50)
    }
    for name, col in knives.items():
        make_knife_rgba(f"{base_item}/{name}.png", col)
    print("Generated 5 butcher knife textures.")

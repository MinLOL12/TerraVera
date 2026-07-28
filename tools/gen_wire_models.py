#!/usr/bin/env python3
"""Generates the multipart blockstates and element models for TerraVera's wire blocks.

Every wire type shares the same geometry. For each type we emit:
  - models/block/<type>.json                     static cross, used by the item model
  - models/block/wire/<type>_core{,_lit}.json    the junction core
  - models/block/wire/<type>_arm_<dir>{,_lit}    one arm per connection direction
  - blockstates/<type>.json                      multipart wiring the parts to the connection/powered properties
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/terravera"

TYPES = {
    "single_wire": "terravera:block/single_wire",
    "copper_wire": "terravera:block/copper_wire",
    "wire_intersection": "terravera:block/wire_intersection",
}

# from/to boxes must match WireBlock's shapes.
ARMS = {
    "north": [7, 0, 0, 9, 3, 8],
    "south": [7, 0, 8, 9, 3, 16],
    "west": [0, 0, 7, 8, 3, 9],
    "east": [8, 0, 7, 16, 3, 9],
    "up": [7, 2, 7, 9, 16, 9],
}
CORE = [6, 0, 6, 10, 3, 10]


def faces_for(box, uv_top, uv_side):
    x0, y0, z0, x1, y1, z1 = box
    return {
        "north": {"uv": uv_side, "texture": "#wire"},
        "south": {"uv": uv_side, "texture": "#wire"},
        "east": {"uv": uv_side, "texture": "#wire"},
        "west": {"uv": uv_side, "texture": "#wire"},
        "up": {"uv": uv_top, "texture": "#wire"},
        "down": {"uv": uv_top, "texture": "#wire"},
    }


def element(box):
    return {"from": box[:3], "to": box[3:], "faces": faces_for(box, [6, 6, 10, 10], [0, 13, 16, 16])}


def model(texture_ref, *boxes):
    return {
        "ambientocclusion": False,
        "textures": {"particle": texture_ref, "wire": texture_ref},
        "elements": [element(box) for box in boxes],
    }


def write(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")
    print(f"wrote {path.relative_to(ROOT)}")


def main():
    for type_name, texture in TYPES.items():
        lit_texture = texture + "_lit"

        # Item/static cross model: core + all four horizontal arms.
        write(ROOT / f"models/block/{type_name}.json",
              model(texture, CORE, ARMS["north"], ARMS["south"], ARMS["west"], ARMS["east"]))

        # Multipart pieces.
        write(ROOT / f"models/block/wire/{type_name}_core.json", model(texture, CORE))
        write(ROOT / f"models/block/wire/{type_name}_core_lit.json", model(lit_texture, CORE))
        for direction, box in ARMS.items():
            write(ROOT / f"models/block/wire/{type_name}_arm_{direction}.json", model(texture, box))
            write(ROOT / f"models/block/wire/{type_name}_arm_{direction}_lit.json", model(lit_texture, box))

        # Blockstate: core follows `powered`; each arm follows its connection property and `powered`.
        cases = [
            {"when": {"powered": "false"}, "apply": {"model": f"terravera:block/wire/{type_name}_core"}},
            {"when": {"powered": "true"}, "apply": {"model": f"terravera:block/wire/{type_name}_core_lit"}},
        ]
        for direction in ARMS:
            cases.append({"when": {"AND": [{direction: "true"}, {"powered": "false"}]},
                          "apply": {"model": f"terravera:block/wire/{type_name}_arm_{direction}"}})
            cases.append({"when": {"AND": [{direction: "true"}, {"powered": "true"}]},
                          "apply": {"model": f"terravera:block/wire/{type_name}_arm_{direction}_lit"}})
        write(ROOT / f"blockstates/{type_name}.json", {"multipart": cases})


if __name__ == "__main__":
    main()

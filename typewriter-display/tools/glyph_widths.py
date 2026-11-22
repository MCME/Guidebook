"""Writes glyph-widths.txt: how wide each character is in Minecraft's default font, for the Typewriter box's layout.

Run it again when Minecraft changes its font:

    python3 -I typewriter-display/tools/glyph_widths.py <version> <assets/minecraft dir> <unifont.zip> \
        > typewriter-display/src/main/resources/com/mcmiddleearth/guidebook/typewriter/glyph-widths.txt

<assets/minecraft dir> is from the client jar (the bitmap font JSON and PNGs). unifont.zip is in the launcher's asset
store: look up minecraft/font/unifont.zip in assets/indexes/<newest>.json for its hash under assets/objects/.

Each width is a character's advance with the 1px gap after it, as the client works it out: for a bitmap glyph, its
rightmost filled column + 1 scaled to the font's height, + 1; for a Unifont glyph, half its filled width + 1. Bold
adds 1px, which the layout adds itself. Unifont covers tens of thousands of characters, so only those in UNIFONT_CHARS
are included; the layout counts any other character as the widest a Unifont glyph can be.
"""

import json
import struct
import sys
import zipfile
import zlib
from pathlib import Path


def read_png(path):
    data = path.read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n"
    pos, idat, palette, trns = 8, b"", None, None
    while pos < len(data):
        length, kind = struct.unpack(">I4s", data[pos:pos + 8])
        chunk = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if kind == b"IHDR":
            width, height, depth, color, _, _, interlace = struct.unpack(">IIBBBBB", chunk)
        elif kind == b"PLTE":
            palette = chunk
        elif kind == b"tRNS":
            trns = chunk
        elif kind == b"IDAT":
            idat += chunk
    assert interlace == 0
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[color]
    bits_per_pixel = channels * depth
    stride = (width * bits_per_pixel + 7) // 8
    bpp = max(1, bits_per_pixel // 8)
    raw = zlib.decompress(idat)
    rows, prev, i = [], bytearray(stride), 0
    for _ in range(height):
        filt = raw[i]
        line = bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            b = prev[x]
            c = prev[x - bpp] if x >= bpp else 0
            if filt == 1:
                line[x] = (line[x] + a) & 255
            elif filt == 2:
                line[x] = (line[x] + b) & 255
            elif filt == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif filt == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append(line)
        prev = line

    def sample(row, x):
        if depth == 8:
            return row[x * channels:(x + 1) * channels]
        per_byte = 8 // depth
        byte = row[x // per_byte]
        shift = 8 - depth * (x % per_byte + 1)
        return [(byte >> shift) & ((1 << depth) - 1)]

    def alpha(x, y):
        px = sample(rows[y], x)
        if color == 6:
            return px[3]
        if color == 4:
            return px[1]
        if color == 3:
            return trns[px[0]] if trns and px[0] < len(trns) else 255
        return 255

    return width, height, alpha


# Characters Guidebook itself writes that only Unifont draws: <bullet>'s marker
UNIFONT_CHARS = "\u25b8"


def unifont_widths(zip_path):
    with zipfile.ZipFile(zip_path) as archive:
        name = next(n for n in archive.namelist() if n.startswith("unifont_all_no_pua") and n.endswith(".hex"))
        wanted = {ord(c) for c in UNIFONT_CHARS}
        widths = {}
        for line in archive.read(name).decode().splitlines():
            code, bits = line.split(":")
            if int(code, 16) not in wanted:
                continue
            cell = len(bits) // 16 * 4
            rows = [int(bits[i * len(bits) // 16:(i + 1) * len(bits) // 16], 16) for i in range(16)]
            filled = [x for x in range(cell) if any(row >> (cell - 1 - x) & 1 for row in rows)]
            widths[chr(int(code, 16))] = (max(filled) - min(filled) + 1) // 2 + 1
        missing = set(UNIFONT_CHARS) - set(widths)
        if missing:
            sys.exit(f"Not in {name}: {sorted(missing)}")
        return widths


def bitmap_widths(assets):
    widths = {}
    for provider in json.loads((assets / "font/include/space.json").read_text())["providers"]:
        widths.update(provider["advances"])
    for provider in json.loads((assets / "font/include/default.json").read_text())["providers"]:
        if provider["type"] != "bitmap":
            continue
        width, height, alpha = read_png(assets / "textures" / provider["file"].split(":")[1])
        rows = provider["chars"]
        cell_w, cell_h = width // len(rows[0]), height // len(rows)
        scale = provider.get("height", 8) / cell_h
        for r, row in enumerate(rows):
            for c, char in enumerate(row):
                # The first provider with a character draws it, and U+0000 marks an empty cell
                if char == "\u0000" or char in widths:
                    continue
                filled = 0
                for x in range(cell_w - 1, -1, -1):
                    if any(alpha(c * cell_w + x, r * cell_h + y) for y in range(cell_h)):
                        filled = x + 1
                        break
                widths[char] = int(0.5 + filled * scale) + 1
    return widths


def main(version, assets, unifont):
    widths = bitmap_widths(Path(assets))
    widths.update({c: w for c, w in unifont_widths(unifont).items() if c not in widths})
    print(f"# Character widths in Minecraft {version}'s default font, written by tools/glyph_widths.py. Don't edit.")
    print("# Each line is a width in pixels, then the code points of the characters that wide.")
    for width in sorted(set(widths.values())):
        codes = sorted(ord(c) for c, w in widths.items() if w == width)
        print(width, " ".join(f"{code:04x}" for code in codes))


if __name__ == "__main__":
    main(*sys.argv[1:])

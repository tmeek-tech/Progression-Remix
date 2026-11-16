"""
Art for the dye works and the sulfur works (registry/ModDyes.java): sulfur and its block, foul red mud, and the coal
tar dyes' crystals and powders. gen_dyes.py calls run() first, passing gen_assets. Only textures, block and item models
and blockstates are written here. The crystals, beads and granules are drawn as gen_air_art draws sodium nitrite, urea
and the iron catalyst, each heap in its own colours.
"""
import math
import random

from PIL import Image

import gen_air_art as A

G = None  # gen_assets, set by run()


def run(gen):
    global G
    G = gen
    A.G = gen
    items()
    sulfur_block()


def crystals(seed, pal, edge, pieces=None):
    """A little heap of short prisms seen from above and in front: the top lit, the left in light shade and the right
    in deeper, outlined. `pal` is top, left and right."""

    def crystal(img, cx, cy, w, rng):
        h = w * rng.uniform(0.7, 1.1)
        for y in range(16):
            for x in range(16):
                dx, dy = x + 0.5 - cx, y + 0.5 - (cy - h / 2)
                if abs(dx) > w:
                    continue
                slope = (w - abs(dx)) / 2
                if -slope <= dy <= slope:
                    col = pal[0]
                elif slope < dy <= h + slope:
                    col = pal[1] if dx < 0 else pal[2]
                else:
                    continue
                img.putpixel((x, y), col + (255,))
        A.outline(img, edge)

    return A.heap(seed, pieces or [(5.5, 6, 2.5), (10.5, 5.5, 2.5), (3.5, 10, 2.5), (8, 9.5, 3), (12.5, 10, 2.5),
                                   (6, 12.5, 2.5), (10.5, 12.5, 2.5)], crystal)


def needles(seed, pal, edge):
    """A little heap of long thin crystals lying every which way, as acetanilide's and naphthalene's flakes: each a
    lit line with a shaded side, outlined."""
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for _ in range(9):
        piece = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        cx, cy = rng.uniform(4, 12), rng.uniform(6, 12.5)
        a = rng.uniform(-0.9, 0.9) + (0 if rng.random() < 0.5 else math.pi / 2)
        length = rng.uniform(3, 5)
        for t in range(-int(length * 2), int(length * 2) + 1):
            x, y = cx + math.cos(a) * t / 2, cy + math.sin(a) * t / 2
            for dx, dy, col in ((0, 0, pal[0]), (0, 1, pal[1]), (1, 1, pal[2])):
                px, py = int(x) + dx, int(y) + dy
                if 0 <= px < 16 and 0 <= py < 16:
                    piece.putpixel((px, py), col + (255,))
        A.outline(piece, edge)
        img.alpha_composite(piece)
    return img


def granules(seed, dark, base, light, glint, edge):
    """A little heap of rough lumps, each lit on its upper left with a glint and outlined."""

    def granule(img, cx, cy, size, rng):
        lumps = [size * rng.uniform(0.8, 1.15) for _ in range(8)]
        inside = []
        for y in range(16):
            for x in range(16):
                dx, dy = x + 0.5 - cx, y + 0.5 - cy
                ang = int((math.atan2(dy, dx) + math.pi) / (2 * math.pi) * 8) % 8
                if math.hypot(dx, dy * 1.15) > lumps[ang]:
                    continue
                t = 0.5 - dx / size * 0.35 - dy / size * 0.4 + rng.uniform(-0.1, 0.1)
                col = A.mix(base, light, (t - 0.65) * 3) if t > 0.65 else A.mix(dark, base, t / 0.65)
                img.putpixel((x, y), col + (255,))
                inside.append((x, y, dx + dy))
        if inside:
            x, y, _ = min(inside, key=lambda p: p[2])
            img.putpixel((x + 1, y + 1) if img.getpixel((x + 1, y + 1))[3] else (x, y), glint + (255,))
        A.outline(img, edge)

    return A.heap(seed, [(5, 5.5, 2.4), (10.5, 5, 2.2), (3.5, 9.5, 2.3), (8, 9, 2.6), (12.5, 9.5, 2.2),
                         (6, 12.5, 2.4), (10.5, 12.5, 2.3)], granule)


def powder(seed, light, base, shade, edge):
    """A little mound of fine powder, lit on its upper left, speckled, outlined."""
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8, y + 0.5 - 11
            # a low mound: wide and flat, its top rounded
            if dy > 3.5 or (dx / 6.5) ** 2 + (dy / (5 if dy < 0 else 3.5)) ** 2 > 1:
                continue
            t = -dx / 13 - dy / 9 + rng.uniform(-0.12, 0.12)
            col = light if t > 0.25 else base if t > -0.2 else shade
            img.putpixel((x, y), col + (255,))
    A.outline(img, edge)
    return img


def tints(top, left, right):
    return [top, left, right]


def items():
    white = tints((0xFF, 0xFF, 0xFA), (0xE6, 0xE4, 0xDA), (0xC8, 0xC4, 0xB6))
    textures = {
        # sulfur: bright lemon-yellow crystals
        'sulfur': crystals(5101, tints((0xFF, 0xF4, 0x7A), (0xEC, 0xD4, 0x2C), (0xC4, 0xA4, 0x14)),
                           (0x6E, 0x5A, 0x08)),
        # red mud gone black with sulfide: a wet black ball with a dull sheen
        'foul_red_mud': G.mud_ball_texture(5102, (0x0C, 0x0A, 0x0A), (0x24, 0x20, 0x1E), (0x42, 0x3A, 0x36),
                                           (0x98, 0x90, 0x86)),
        'aniline_salt': crystals(5103, white, (0x70, 0x6C, 0x60)),
        'acetanilide': needles(5104, white, (0x74, 0x70, 0x66)),
        'nitroacetanilide': needles(5105, tints((0xFF, 0xFA, 0xD8), (0xEE, 0xE4, 0xAE), (0xCC, 0xBE, 0x84)),
                                    (0x7A, 0x6C, 0x3A)),
        'nitroaniline': crystals(5106, tints((0xFF, 0xEE, 0x70), (0xF0, 0xCC, 0x2E), (0xC8, 0x9C, 0x14)),
                                 (0x70, 0x50, 0x08)),
        'nitronaphthalene': needles(5107, tints((0xFF, 0xF0, 0x9C), (0xEE, 0xD8, 0x64), (0xC8, 0xAC, 0x3A)),
                                    (0x76, 0x60, 0x1A)),
        # naphthylamine reddens as it stands in the air
        'naphthylamine': crystals(5108, tints((0xFF, 0xEE, 0xEA), (0xEC, 0xC8, 0xC2), (0xCA, 0xA0, 0x9A)),
                                  (0x74, 0x50, 0x4A)),
        'naphthol': needles(5109, tints((0xFF, 0xFC, 0xF0), (0xEE, 0xE6, 0xD2), (0xCE, 0xC2, 0xA8)),
                            (0x76, 0x6C, 0x58)),
        'phenylglycine': powder(5110, (0xFF, 0xFF, 0xF8), (0xEC, 0xEA, 0xE0), (0xCC, 0xC8, 0xBC), (0x72, 0x6E, 0x64)),
        # sodamide: greyish lumps, as it was sold
        'sodamide': granules(5111, (0x8A, 0x8C, 0x90), (0xB4, 0xB6, 0xBA), (0xDC, 0xDE, 0xE0), (0xFF, 0xFF, 0xFF),
                             (0x44, 0x46, 0x4A)),
        # the leuco base: colourless crystals with a faint green in their shade
        'leuco_malachite_green': crystals(5112, tints((0xFA, 0xFF, 0xFA), (0xDC, 0xEC, 0xE0), (0xB8, 0xCE, 0xC0)),
                                          (0x5C, 0x70, 0x64)),
        'saltpetre': crystals(5113, white, (0x6C, 0x6A, 0x64)),
    }
    for name, img in textures.items():
        G.save_png(img, G.asset('textures', 'item', name + '.png'))
        A.item_model(name, {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'mcremix:item/{name}'}})


def sulfur_block():
    """
    A block of sulfur: yellow crystal faces packed together, each its own shade, darker along the seams where they
    meet and with a bright glint now and then, tiling.
    """
    rng = random.Random(5120)
    dark, base, light, glint = (0xB8, 0x98, 0x10), (0xE4, 0xCC, 0x30), (0xFA, 0xEA, 0x70), (0xFF, 0xFA, 0xC8)
    seeds = [(rng.uniform(0, 16), rng.uniform(0, 16), rng.uniform(-0.6, 0.6)) for _ in range(11)]
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            near = sorted((math.hypot((x - sx + 8) % 16 - 8, (y - sy + 8) % 16 - 8), shade)
                          for sx, sy, shade in seeds)
            (d1, shade), (d2, _) = near[0], near[1]
            t = 0.5 + shade * 0.5 + rng.uniform(-0.05, 0.05)
            col = A.mix(dark, base, t * 2) if t < 0.5 else A.mix(base, light, t * 2 - 1)
            if d2 - d1 < 0.9:
                col = A.mix(col, dark, 0.55)
            elif rng.random() < 0.025:
                col = glint
            img.putpixel((x, y), col + (255,))
    A.png(img, 'sulfur_block')
    A.model('sulfur_block', {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'mcremix:block/sulfur_block'}})
    A.states('sulfur_block', {'variants': {'': {'model': 'mcremix:block/sulfur_block'}}})
    A.item_model('sulfur_block', {'parent': 'mcremix:block/sulfur_block'})

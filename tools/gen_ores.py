"""
Realistic ores (0.42.0, Taran's ask of 2026-11-27, design/realistic-ores.md): real minerals for the metals the mod
uses, each with an ore block for every rock it occurs in, drawn on that rock's own texture, so a deposit matches the
stone round it wherever it runs (vanilla's, Create's, and Exploration Remix's layers, which are built of them).
gen_assets.py calls run() after gen_early, passing itself; this file adds to the lang and tag files written before it
and owns the files of its own blocks and items.

Which minerals occur in which rocks is read from Mineral.java and Host.java, so the Java stays the one list. Each
mineral is drawn in its real habit: bog iron as rusty nodules, hematite in steel-grey and red bands (earthy red specks
in sandstone and terracotta), magnetite as black octahedra, malachite in green banded crusts, chalcopyrite as brassy
grains with a blue-purple tarnish, native copper branching, sphalerite as resinous brown crystals, gold in a vein of
milky quartz, ilmenite as black plates, pyrite as brassy cubes, sulfur as yellow crusts.

Processing (Taran's card: each its own way): the oxides fire Searing into a bloom as raw iron does; malachite and
native copper are copper as raw copper is; the sulfides are roasted Hot first, giving off sulfur dioxide (chalcopyrite
into copper matte, sphalerite into zinc calcine, pyrite into hematite); gold quartz is gold as raw gold is; ilmenite
crushes into black sand. Tags give Create's crushing and the crucible's melting to the minerals that are raw ore as
vanilla's are, and leave the sulfides out of them so they must be roasted.

The world generation is OreBodies.java's: this writes its feature, the biome tags that say where each kind of body
goes, and takes vanilla's iron, copper and gold ores and Create's zinc ore out of the overworld (Taran's card: replace
them). Washing red sand no longer turns up bauxite (the card on old sources): bauxite is mined now.
"""
import math
import os
import random
import re

from PIL import Image

import gen_colours as C
import gen_decor as D

G = None  # gen_assets, set by run()

JAVA = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'src', 'main', 'java', 'com',
                    'mcremix', 'ores')


def run(gen):
    global G
    G = gen
    textures()
    models()
    loot()
    tags()
    recipes()
    worldgen()
    lang()


# ------------------------------------------------------------------------------------------------ the Java's lists

def read_hosts():
    """Host.java's rocks in order: id -> (block id, look, soft)."""
    src = open(os.path.join(JAVA, 'Host.java')).read()
    hosts = {}
    for m in re.finditer(r'^\t([A-Z_]+)\("(\w+)", "([\w:]+)", \(\) -> Blocks\.\w+,\s*Look\.(\w+)(, true)?', src,
                         re.M):
        hosts[m.group(2)] = (m.group(3), m.group(4), bool(m.group(5)))
    return hosts


def read_minerals():
    """Mineral.java's minerals in order: id -> (tier, drop, min, max, [host ids])."""
    src = open(os.path.join(JAVA, 'Mineral.java')).read()
    body = src[src.index('public enum Mineral {'):src.index(';', src.index('public enum Mineral {'))]
    minerals = {}
    for m in re.finditer(r'([A-Z_]+)\("(\w+)", Tier\.(\w+), "([\w:]+)", (\d+), (\d+),([^)]*)\)', body):
        hosts = [h.strip().lower() for h in m.group(7).split(',') if h.strip()]
        minerals[m.group(2)] = (m.group(3), m.group(4), int(m.group(5)), int(m.group(6)), hosts)
    return minerals


HOSTS = read_hosts()
MINERALS = read_minerals()
assert len(HOSTS) == 31 and len(MINERALS) == 11, (len(HOSTS), len(MINERALS))


def block_id(mineral, host):
    return f'{mineral}_ore' if host == 'stone' else f'{host}_{mineral}_ore'


def ore_blocks(mineral=None):
    return [block_id(m, h) for m, (_, _, _, _, hosts) in MINERALS.items() if mineral in (None, m) for h in hosts]


# the minerals that are items of their own (sulfur's is the sulfur works' item)
ITEMS = ['bog_iron', 'hematite', 'magnetite', 'malachite', 'chalcopyrite', 'copper_matte', 'native_copper',
         'sphalerite', 'zinc_calcine', 'gold_quartz', 'ilmenite', 'pyrite']
OWN_ROCKS = ['bauxite_ore', 'rock_salt', 'ilmenite_sand']

# each rock's own textures: one for every face, side and end, or sandstone's top, side and bottom
VANILLA_TEX = {'dripstone': 'dripstone_block'}
CREATE_TEX = {'limestone': 'palettes/stone_types/limestone', 'asurine': 'palettes/stone_types/natural/asurine_0',
              'crimsite': 'palettes/stone_types/natural/crimsite_0', 'ochrum': 'palettes/stone_types/natural/ochrum_0',
              'veridium': 'palettes/stone_types/natural/veridium_0', 'scorchia': 'palettes/stone_types/scorchia',
              'scoria': 'palettes/stone_types/scoria'}
# rocks laid down as sediment, where hematite is the red of the rock rather than steel-grey bands through it
SEDIMENT = {'sandstone', 'red_sandstone', 'terracotta', 'white_terracotta', 'orange_terracotta', 'yellow_terracotta',
            'red_terracotta', 'brown_terracotta', 'light_gray_terracotta', 'limestone', 'ochrum', 'calcite',
            'dripstone'}


def host_faces(host):
    """face -> texture image for a rock: 'all', or 'side' and 'end', or 'top', 'side' and 'bottom'."""
    look = HOSTS[host][1]
    if host in CREATE_TEX:
        return {'all': C.jar_png(C.CREATE, f'assets/create/textures/block/{CREATE_TEX[host]}.png')}
    name = VANILLA_TEX.get(host, host)
    tex = lambda n: C.jar_png(C.MC, f'assets/minecraft/textures/block/{n}.png')
    if look in ('COLUMN', 'PILLAR'):
        return {'side': tex(name if look == 'COLUMN' else name + '_side'), 'end': tex(name + '_top')}
    if look == 'SANDSTONE':
        return {'top': tex(name + '_top'), 'side': tex(name), 'bottom': tex(name + '_bottom')}
    return {'all': tex(name)}


# ------------------------------------------------------------------------------------------------ drawing

# each mineral's colours, darkest first
PAL = {
    'bog_iron': [(54, 29, 13), (88, 49, 21), (122, 71, 31), (154, 97, 45), (184, 126, 66)],
    'hematite_grey': [(34, 34, 40), (58, 58, 66), (88, 88, 100), (138, 138, 152)],
    'hematite_red': [(78, 22, 16), (112, 34, 24), (146, 50, 34), (176, 72, 50)],
    'magnetite': [(16, 16, 19), (30, 30, 36), (54, 56, 64), (128, 134, 150)],
    'malachite': [(12, 62, 40), (24, 100, 63), (42, 140, 89), (84, 190, 132), (150, 226, 182)],
    'azurite': [(28, 50, 136), (54, 92, 196)],
    'chalcopyrite': [(92, 76, 24), (146, 124, 44), (190, 168, 72), (226, 210, 126)],
    'tarnish': [(94, 68, 158), (52, 104, 176), (150, 72, 136), (70, 140, 150)],
    'native_copper': [(98, 44, 18), (152, 74, 34), (198, 108, 58), (238, 156, 102)],
    'patina': [(66, 136, 106), (96, 168, 136)],
    'sphalerite': [(44, 22, 8), (82, 44, 16), (128, 76, 28), (180, 120, 50), (236, 186, 98)],
    'quartz': [(168, 162, 152), (200, 194, 184), (226, 222, 214), (246, 244, 238)],
    'gold': [(150, 102, 14), (214, 160, 32), (250, 212, 70), (255, 244, 160)],
    'ilmenite': [(14, 13, 16), (30, 29, 34), (104, 104, 118), (204, 206, 220)],
    'pyrite': [(108, 92, 40), (162, 146, 74), (202, 188, 112), (236, 228, 172)],
    'sulfur': [(140, 134, 18), (200, 194, 36), (234, 230, 72), (250, 250, 156)],
    'copper_matte': [(18, 16, 16), (38, 32, 30), (62, 52, 46), (118, 94, 78)],
    'zinc_calcine': [(104, 102, 96), (146, 144, 138), (182, 180, 174), (212, 210, 204)],
}


class Face:
    """A 16 by 16 texture being drawn on, and which of its pixels are mineral already."""

    def __init__(self, img):
        self.img = img.copy()
        self.px = self.img.load()
        self.taken = set()

    def put(self, x, y, rgb):
        if 0 <= x < 16 and 0 <= y < 16:
            self.px[x, y] = tuple(rgb) + (255,)
            self.taken.add((x, y))

    def shade_rock(self, x, y, f):
        """Darkens the rock at (x, y), where no mineral is: the shadow a grain casts into it."""
        if 0 <= x < 16 and 0 <= y < 16 and (x, y) not in self.taken:
            r, g, b, a = self.px[x, y]
            self.px[x, y] = (int(r * f), int(g * f), int(b * f), a)

    def free(self, cells, margin=0):
        for (x, y) in cells:
            if not (margin <= x < 16 - margin and margin <= y < 16 - margin):
                return False
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    if (x + dx, y + dy) in self.taken:
                        return False
        return True

    def grain(self, cells, pal, base=1, glint=None, shadow=0.72):
        """A grain lit from the top left: lighter where nothing of it is up or left of a pixel, darker where nothing
        is down or right, its shadow cast into the rock below and right of it."""
        cells = set(cells)
        top = len(pal) - 1
        for (x, y) in cells:
            i = base
            if (x - 1, y) not in cells or (x, y - 1) not in cells:
                i += 1
            if (x + 1, y) not in cells or (x, y + 1) not in cells:
                i -= 1
            self.put(x, y, pal[max(0, min(top, i))])
        if glint:
            self.put(glint[0], glint[1], pal[top])
        for (x, y) in cells:
            for d in ((1, 0), (0, 1), (1, 1)):
                if (x + d[0], y + d[1]) not in cells:
                    self.shade_rock(x + d[0], y + d[1], shadow)


def disc(cx, cy, r):
    return {(x, y) for x in range(16) for y in range(16) if (x - cx) ** 2 + (y - cy) ** 2 <= r * r}


def diamond(cx, cy, r):
    return {(cx + dx, cy + dy) for dx in range(-r, r + 1) for dy in range(-r, r + 1) if abs(dx) + abs(dy) <= r}


def place(face, rng, make, tries=40, margin=1):
    """Cells from make(x, y) at a free spot, or None when there is no room left."""
    for _ in range(tries):
        cells = make(rng.randint(margin, 15 - margin), rng.randint(margin, 15 - margin))
        if cells and face.free(cells, margin):
            return cells
    return None


def draw_bog_iron(face, rng, host):
    pal = PAL['bog_iron']
    for _ in range(rng.randint(4, 6)):
        r = rng.choice((1.2, 1.6, 1.6, 2.1))
        cells = place(face, rng, lambda x, y: disc(x + 0.5, y + 0.5, r))
        if not cells:
            continue
        cx = sum(c[0] for c in cells) / len(cells)
        cy = sum(c[1] for c in cells) / len(cells)
        # a nodule's rind is darkest, rusting inward, its middle the brightest
        for (x, y) in cells:
            d = math.hypot(x - cx, y - cy) / max(r, 1)
            face.put(x, y, pal[3 if d < 0.45 else 2 if d < 0.8 else 1])
        face.put(round(cx - 0.6), round(cy - 0.6), pal[4])
        for (x, y) in cells:
            face.shade_rock(x + 1, y + 1, 0.7)


def draw_hematite(face, rng, host):
    if host in SEDIMENT:
        # red earthy hematite: the colour of red beds, in soft specks with no shine
        pal = PAL['hematite_red']
        for _ in range(rng.randint(8, 11)):
            cells = place(face, rng, lambda x, y: {c for c in disc(x + 0.5, y + 0.5, rng.choice((0.8, 1.2, 1.5)))
                                                   if rng.random() < 0.85} or {(x, y)})
            if cells:
                for (x, y) in cells:
                    face.put(x, y, pal[rng.randint(1, 3)])
                for (x, y) in cells:
                    face.shade_rock(x + 1, y + 1, 0.82)
        return
    # banded iron: steel-grey hematite in lenses with red jasper along them
    grey, red = PAL['hematite_grey'], PAL['hematite_red']
    for n in range(2):
        y0 = rng.randint(3, 6) + n * 6
        length = rng.randint(9, 12)
        x0 = rng.randint(1, 15 - length)
        phase = rng.uniform(0, 6.3)
        cells = set()
        for x in range(x0, x0 + length):
            y = y0 + round(0.8 * math.sin(x * 0.7 + phase))
            thick = 2 if x0 < x < x0 + length - 1 else 1
            for t in range(thick):
                cells.add((x, y + t))
        face.grain(cells, grey, base=1, glint=(x0 + length // 2, min(c[1] for c in cells if c[0] == x0 + length // 2)))
        for (x, y) in cells:
            if (x, y + 1) not in cells and rng.random() < 0.8:
                face.put(x, y + 1, red[rng.randint(1, 2)])


def draw_magnetite(face, rng, host):
    pal = PAL['magnetite']
    for _ in range(rng.randint(5, 6)):
        r = rng.choice((1, 1, 2))
        cells = place(face, rng, lambda x, y: diamond(x, y, r))
        if not cells:
            continue
        cx = sum(c[0] for c in cells) // len(cells)
        cy = sum(c[1] for c in cells) // len(cells)
        for (x, y) in cells:
            i = 1
            if x - cx + y - cy < 0:
                i = 2  # the faces turned to the light
            elif x - cx + y - cy > 0:
                i = 0
            face.put(x, y, pal[i])
        face.put(cx, cy - r, pal[3])
        for (x, y) in cells:
            face.shade_rock(x + 1, y + 1, 0.75)


def draw_malachite(face, rng, host):
    pal, blue = PAL['malachite'], PAL['azurite']
    for _ in range(rng.randint(4, 5)):
        r = rng.choice((1.5, 1.9, 2.3, 2.6))
        cells = place(face, rng, lambda x, y: disc(x + 0.5, y + 0.5, r), margin=0)
        if not cells:
            continue
        cx = sum(c[0] for c in cells) / len(cells)
        cy = sum(c[1] for c in cells) / len(cells)
        # botryoidal: a rounded crust banded light and dark green round its middle, lit from the top left
        for (x, y) in cells:
            d = math.hypot(x - cx, y - cy)
            ring = (3, 1, 2, 1)[int(d * 1.5) % 4]
            if x - cx + y - cy < -1:
                ring += 1
            elif x - cx + y - cy > 1.5:
                ring -= 1
            face.put(x, y, pal[max(0, min(4, ring))])
        if rng.random() < 0.6:
            x, y = rng.choice(sorted(cells))
            face.put(x, y, blue[rng.randint(0, 1)])
        for (x, y) in cells:
            face.shade_rock(x + 1, y + 1, 0.68)
            face.shade_rock(x + 1, y, 0.85)


def walk(rng, x, y, steps, dx, dy):
    cells = [(x, y)]
    for _ in range(steps):
        if rng.random() < 0.35:
            if rng.random() < 0.5:
                x += dx
            else:
                y += dy
        else:
            x += dx
            y += dy
        cells.append((x, y))
    return cells


def draw_chalcopyrite(face, rng, host):
    pal, tarnish = PAL['chalcopyrite'], PAL['tarnish']
    dx, dy = rng.choice(((1, 1), (1, -1)))
    cells = place(face, rng, lambda x, y: set(walk(rng, x, y, rng.randint(3, 4), dx, dy)), margin=1)
    if cells:
        for (x, y) in cells:
            face.put(x, y, pal[rng.choice((1, 2, 2))])
        for (x, y) in cells:
            face.shade_rock(x + 1, y + 1, 0.72)
    shapes = (((0, 0), (1, 0), (0, 1), (1, 1)), ((0, 0), (1, 0), (1, 1)), ((0, 0), (0, 1), (1, 1), (2, 1)),
              ((0, 0), (1, 0), (2, 0), (1, 1), (2, 1)), ((1, 0), (0, 1), (1, 1), (2, 1), (1, 2)))
    for _ in range(rng.randint(5, 6)):
        shape = rng.choice(shapes)
        cells = place(face, rng, lambda x, y: {(x + a, y + b) for a, b in shape})
        if cells:
            face.grain(cells, pal, base=2)
            # the peacock tarnish chalcopyrite takes in air, blue and purple on its edges
            for (x, y) in cells:
                if ((x + 1, y) not in cells or (x, y + 1) not in cells) and rng.random() < 0.5:
                    face.put(x, y, tarnish[rng.randint(0, 3)])


def draw_native_copper(face, rng, host):
    pal, patina = PAL['native_copper'], PAL['patina']
    for _ in range(2):
        for _ in range(30):
            x, y = rng.randint(2, 13), rng.randint(2, 13)
            dx, dy = rng.choice(((1, 1), (1, -1), (-1, 1), (-1, -1)))
            stem = walk(rng, x, y, rng.randint(5, 7), dx, dy)
            cells = set(stem)
            for (bx, by) in stem[1:-1]:
                if rng.random() < 0.4:
                    cells |= set(walk(rng, bx, by, rng.randint(1, 3), dx, -dy) if rng.random() < 0.5
                                 else walk(rng, bx, by, rng.randint(1, 3), -dx, dy))
            if face.free(cells, 1):
                break
        else:
            continue
        for (x, y) in cells:
            lit = (x - 1, y) not in cells or (x, y - 1) not in cells
            face.put(x, y, patina[rng.randint(0, 1)] if rng.random() < 0.1 else pal[3 if lit else rng.choice((1, 2))])
        for (x, y) in cells:
            face.shade_rock(x + 1, y + 1, 0.7)


def draw_sphalerite(face, rng, host):
    pal = PAL['sphalerite']
    shapes = (((1, 0), (2, 0), (0, 1), (1, 1), (2, 1), (3, 1), (1, 2), (2, 2)),
              ((0, 0), (1, 0), (2, 0), (0, 1), (1, 1), (2, 1), (1, 2)),
              ((1, 0), (0, 1), (1, 1), (2, 1), (0, 2), (1, 2), (2, 2), (1, 3)),
              ((0, 0), (1, 0), (0, 1), (1, 1), (2, 1), (2, 2)))
    for _ in range(rng.randint(4, 5)):
        shape = rng.choice(shapes)
        cells = place(face, rng, lambda x, y: {(x + a, y + b) for a, b in shape})
        if cells:
            # resinous: dark brown through, amber where the light comes through its edges, one glossy point
            face.grain(cells, pal, base=1, shadow=0.7)
            top = min(cells, key=lambda c: (c[1], c[0]))
            face.put(top[0], top[1], pal[3])
            face.put(top[0] + 1, top[1] + 1, pal[4]) if (top[0] + 1, top[1] + 1) in cells else None


def draw_gold_quartz(face, rng, host):
    quartz, gold = PAL['quartz'], PAL['gold']
    # two lenses of milky quartz, slanting across the face, gold flecked through them
    for n in range(2):
        for _ in range(40):
            length = rng.randint(8, 11)
            x0, y0 = rng.randint(0, 15 - length), rng.randint(2, 13)
            slope = rng.choice((-0.6, -0.4, 0.4, 0.6))
            cells = {}
            for i in range(length):
                x = x0 + i
                mid = y0 + slope * (i - length / 2)
                thick = 1 if i in (0, length - 1) else 2 if i in (1, length - 2) else 3
                for t in range(thick):
                    y = round(mid - (thick - 1) / 2) + t
                    cells[(x, y)] = t / max(1, thick - 1)
            if all(1 <= y <= 14 for (_, y) in cells) and face.free(cells):
                break
        else:
            continue
        for (x, y), t in cells.items():
            face.put(x, y, quartz[3 if t < 0.3 else 2 if t < 0.8 else 1])
        for (x, y) in cells:
            if (x, y + 1) not in cells:
                face.shade_rock(x, y + 1, 0.7)
            if (x + 1, y) not in cells:
                face.shade_rock(x + 1, y, 0.85)
        spots = sorted(cells)
        for _ in range(3):
            x, y = rng.choice(spots)
            face.put(x, y, gold[2])
            if (x + 1, y) in cells:
                face.put(x + 1, y, gold[1])
            if (x, y - 1) in cells and rng.random() < 0.5:
                face.put(x, y - 1, gold[3])


def draw_ilmenite(face, rng, host):
    pal = PAL['ilmenite']
    for _ in range(rng.randint(7, 9)):
        w, h = rng.choice(((3, 1), (4, 1), (3, 2), (4, 2), (5, 2)))
        cells = place(face, rng, lambda x, y: {(x + a, y + b) for a in range(w) for b in range(h)})
        if cells:
            # black plates with a sub-metallic shine along the edge facing the light
            for (x, y) in cells:
                face.put(x, y, pal[0] if (x, y - 1) in cells else pal[2])
            x, y = min(cells)
            face.put(x + rng.randint(0, w - 1), y, pal[3])
            for (x, y) in cells:
                face.shade_rock(x, y + 1, 0.65)
                face.shade_rock(x + 1, y + 1, 0.8)


def draw_pyrite(face, rng, host):
    pal = PAL['pyrite']
    for _ in range(rng.randint(3, 4)):
        s = rng.choice((2, 3, 3))
        cells = place(face, rng, lambda x, y: {(x + a, y + b) for a in range(s) for b in range(s)})
        if not cells:
            continue
        x0, y0 = min(cells)
        for (x, y) in cells:
            a, b = x - x0, y - y0
            i = 2
            if a == 0 or b == 0:
                i = 3
            if a == s - 1 or b == s - 1:
                i = 1
            if (a, b) in ((0, s - 1), (s - 1, 0)):
                i = 2
            if (a, b) == (s - 1, s - 1):
                i = 0
            face.put(x, y, pal[i])
        for (x, y) in cells:
            face.shade_rock(x + 1, y + 1, 0.7)


def draw_sulfur(face, rng, host):
    pal = PAL['sulfur']
    for _ in range(rng.randint(3, 4)):
        def crust(x, y):
            # a crust along the rock, crystals pointing up out of it
            width = rng.randint(3, 5)
            cells = {(x + i, y) for i in range(width)} | {(x + i, y + 1) for i in range(1, width - 1)}
            for i in range(width):
                if rng.random() < 0.55:
                    cells.add((x + i, y - 1))
                    if rng.random() < 0.35:
                        cells.add((x + i, y - 2))
            return cells
        cells = place(face, rng, crust, margin=1)
        if cells:
            for (x, y) in cells:
                tip = (x, y - 1) not in cells
                face.put(x, y, pal[3] if tip else pal[2] if (x - 1, y) not in cells else pal[1])
            for (x, y) in cells:
                if (x, y + 1) not in cells:
                    face.put(x, y, pal[0]) if (x, y - 1) in cells else None
                face.shade_rock(x + 1, y + 1, 0.7)


DRAW = {'bog_iron': draw_bog_iron, 'hematite': draw_hematite, 'magnetite': draw_magnetite,
        'malachite': draw_malachite, 'chalcopyrite': draw_chalcopyrite, 'native_copper': draw_native_copper,
        'sphalerite': draw_sphalerite, 'gold_quartz': draw_gold_quartz, 'ilmenite': draw_ilmenite,
        'pyrite': draw_pyrite, 'sulfur': draw_sulfur}


def ore_face(mineral, host, face_name, img):
    # the same pattern in every rock, as vanilla's stone and deepslate ores share theirs; each face of a block its own
    seed = sum(ord(ch) * (i + 1) for i, ch in enumerate(mineral)) * 31 + {'all': 0, 'side': 1, 'end': 2, 'top': 2,
                                                                           'bottom': 3}[face_name]
    face = Face(img)
    DRAW[mineral](face, random.Random(seed), host)
    return face.img


def recolour(img, pal, rng=None, jitter=0):
    """A texture's light and dark kept, its colours swapped for a palette's, darkest first."""
    out = img.copy()
    px = out.load()
    lum = sorted({sum(px[x, y][:3]) for x in range(16) for y in range(16)})
    lo, hi = lum[0], max(lum[-1], lum[0] + 1)
    for y in range(16):
        for x in range(16):
            t = (sum(px[x, y][:3]) - lo) / (hi - lo)
            if rng and jitter:
                t = max(0.0, min(1.0, t + rng.uniform(-jitter, jitter)))
            f = t * (len(pal) - 1)
            i = min(len(pal) - 2, int(f))
            px[x, y] = tuple(round(pal[i][k] + (pal[i + 1][k] - pal[i][k]) * (f - i)) for k in range(3)) + (255,)
    return out


def bauxite_texture():
    """Laterite: terracotta's grain recoloured red-brown, full of round pea-sized grains (pisoliths)."""
    rng = random.Random(4201)
    face = Face(recolour(C.jar_png(C.MC, 'assets/minecraft/textures/block/terracotta.png'),
                         [(86, 38, 21), (112, 52, 29), (134, 64, 35), (156, 78, 43)]))
    for _ in range(10):
        cells = place(face, rng, lambda x, y: disc(x + 0.5, y + 0.5, rng.choice((0.9, 1.3, 1.3))), margin=0)
        if cells:
            face.grain(cells, [(74, 32, 16), (150, 82, 44), (194, 120, 70), (222, 152, 98)], base=2, shadow=0.7)
    return face.img


def rock_salt_texture():
    """Halite: calcite's grain recoloured pink-white, cut by the straight edges of its cubic cleavage."""
    rng = random.Random(4202)
    img = recolour(C.jar_png(C.MC, 'assets/minecraft/textures/block/calcite.png'),
                   [(186, 160, 154), (212, 190, 184), (232, 216, 210), (246, 238, 234)])
    face = Face(img)
    for _ in range(4):
        x0, y0, s = rng.randint(0, 11), rng.randint(0, 11), rng.randint(3, 5)
        for t in range(s):
            face.put(x0 + t, y0, (250, 246, 242))          # a lit top edge
            face.put(x0 + s, y0 + t, (176, 146, 140))      # a shaded side
        face.put(x0 + s, y0 + s, (168, 136, 130))
    return face.img


def ilmenite_sand_texture():
    """Beach sand with black ilmenite in streaks along it, as the waves leave it."""
    rng = random.Random(4203)
    img = C.jar_png(C.MC, 'assets/minecraft/textures/block/sand.png')
    px = img.load()
    dark = [(40, 37, 36), (58, 54, 51), (82, 76, 70)]
    for y in range(16):
        for x in range(16):
            streak = math.sin(y * 1.1 + x * 0.3 + 1.7 * math.sin(x * 0.4)) > 0.35
            if rng.random() < (0.38 if streak else 0.06):
                px[x, y] = rng.choice(dark) + (255,)
    return img


# ------------------------------------------------------------------------------------------------ items

def inside(poly, x, y):
    """Whether (x, y) is inside a convex polygon whose corners run clockwise on screen."""
    n = len(poly)
    for i in range(n):
        (x0, y0), (x1, y1) = poly[i], poly[(i + 1) % n]
        if (x1 - x0) * (y - y0) - (y1 - y0) * (x - x0) < 0:
            return False
    return True


def lump_mask(rng, cx=7.5, cy=8.0, r=4.6, lobes=3):
    mask = set()
    for _ in range(lobes):
        ox, oy, rr = rng.uniform(-1.6, 1.6), rng.uniform(-1.3, 1.3), r * rng.uniform(0.7, 0.95)
        mask |= disc(cx + ox, cy + oy, rr)
    return {(x, y) for (x, y) in mask if 1 <= x <= 14 and 2 <= y <= 14}


def shade_lump(img, mask, pal, rng, cx=7.5, cy=8.0, jitter=0.2):
    px = img.load()
    for (x, y) in mask:
        t = 0.55 - (x - cx) / 11 - (y - cy) / 9 + rng.uniform(-jitter, jitter)
        i = max(0, min(len(pal) - 1, int(t * len(pal))))
        if any(n not in mask for n in ((x + 1, y), (x, y + 1))):
            i = max(0, i - 1)
        px[x, y] = pal[i] + (255,)
    # a dark outline round it, as vanilla's items have
    for (x, y) in mask:
        for n in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if n not in mask and 0 <= n[0] < 16 and 0 <= n[1] < 16:
                px[n] = tuple(int(c * 0.45) for c in pal[0]) + (255,)


def item_texture(name):
    rng = random.Random(sum(map(ord, name)) * 17)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    if name == 'magnetite':
        # one black octahedron, its four faces lit from the top left
        pal = PAL['magnetite']
        cells = diamond(8, 8, 6)
        for (x, y) in cells:
            dx, dy = x - 8, y - 8
            i = 3 if dx <= 0 and dy <= 0 else 2 if dy <= 0 else 1 if dx <= 0 else 0
            if abs(dx) + abs(dy) == 6:
                i = max(0, i - 1)
            px[x, y] = pal[i] + (255,)
        for t in range(-5, 1):
            px[8, 8 + t] = pal[3] + (255,)
        px[6, 5] = (190, 196, 210, 255)
        return img
    if name == 'pyrite':
        # a brassy cube seen from above a corner: its top lightest, its left side mid, its right side dark, the sides
        # finely striated as pyrite's faces are
        pal = PAL['pyrite']
        faces = (((8, 2), (14, 5), (8, 8), (2, 5)), ((2, 5), (8, 8), (8, 14), (2, 11)),
                 ((8, 8), (14, 5), (14, 11), (8, 14)))
        for y in range(16):
            for x in range(16):
                for n, poly in enumerate(faces):
                    if inside(poly, x + 0.5, y + 0.5):
                        px[x, y] = pal[(3, 2, 1)[n]] + (255,)
                        break
        # its front edge catches the light, its outline dark
        for y in range(8, 14):
            px[8, y] = pal[3] + (255,)
        for (x, y) in ((4, 4), (5, 4), (6, 3)):
            px[x, y] = (255, 250, 214, 255)
        mask = {(x, y) for x in range(16) for y in range(16) if px[x, y][3]}
        for (x, y) in mask:
            for d in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (x + d[0], y + d[1])
                if n not in mask and 0 <= n[0] < 16 and 0 <= n[1] < 16:
                    px[n] = (52, 42, 14, 255)
        return img
    if name == 'sphalerite':
        # a resinous crystal: dark brown through, amber where the light comes through its faces, a glossy point
        pal = PAL['sphalerite']
        faces = (((7, 2), (12, 6), (8, 8), (3, 6)), ((3, 6), (8, 8), (8, 14), (3, 10)),
                 ((8, 8), (12, 6), (13, 11), (8, 14)))
        for y in range(16):
            for x in range(16):
                for n, poly in enumerate(faces):
                    if inside(poly, x + 0.5, y + 0.5):
                        px[x, y] = pal[(3, 2, 1)[n]] + (255,)
                        break
        for (x, y) in ((6, 4), (7, 4), (5, 5)):
            px[x, y] = pal[4] + (255,)
        mask = {(x, y) for x in range(16) for y in range(16) if px[x, y][3]}
        for (x, y) in mask:
            for d in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (x + d[0], y + d[1])
                if n not in mask and 0 <= n[0] < 16 and 0 <= n[1] < 16:
                    px[n] = (30, 14, 4, 255)
        return img
    if name == 'native_copper':
        # a branching nugget of copper
        pal = PAL['native_copper']
        cells = set()
        for (x, y) in walk(rng, 3, 12, 9, 1, -1):
            cells |= {(x, y), (x + 1, y)}
        for (bx, by) in ((6, 9), (8, 6), (5, 10)):
            for (x, y) in walk(rng, bx, by, 3, rng.choice((1, -1)), -1):
                cells.add((x, y))
        mask = {(x, y) for (x, y) in cells if 0 <= x < 16 and 0 <= y < 16}
        shade_lump(img, mask, [(120, 56, 24), (180, 96, 48), (222, 134, 78), (250, 182, 132)], rng, jitter=0.1)
        for (x, y) in list(mask)[:2]:
            px[x, y] = PAL['patina'][0] + (255,)
        return img
    pal = {'bog_iron': PAL['bog_iron'], 'hematite': PAL['hematite_grey'], 'malachite': PAL['malachite'],
           'chalcopyrite': PAL['chalcopyrite'], 'copper_matte': PAL['copper_matte'],
           'zinc_calcine': PAL['zinc_calcine'], 'gold_quartz': PAL['quartz'], 'ilmenite': PAL['ilmenite']}[name]
    mask = lump_mask(rng)
    shade_lump(img, mask, pal, rng)
    spots = sorted(mask)
    if name == 'hematite':
        # a red streak across the steel-grey
        for x in range(3, 13):
            y = 9 + round(math.sin(x * 0.6))
            if (x, y) in mask:
                px[x, y] = PAL['hematite_red'][2] + (255,)
    elif name == 'malachite':
        for (x, y) in spots:
            if round(math.hypot(x - 6, y - 10) * 1.3) % 3 == 0:
                px[x, y] = PAL['malachite'][0] + (255,)
    elif name == 'chalcopyrite':
        for (x, y) in rng.sample(spots, 6):
            px[x, y] = rng.choice(PAL['tarnish']) + (255,)
    elif name == 'copper_matte':
        for (x, y) in rng.sample(spots, 5):
            px[x, y] = (196, 112, 62, 255)
    elif name == 'zinc_calcine':
        for (x, y) in rng.sample(spots, 9):
            px[x, y] = PAL['zinc_calcine'][0] + (255,)
    elif name == 'gold_quartz':
        for (x, y) in rng.sample(spots, 6):
            px[x, y] = PAL['gold'][rng.randint(1, 3)] + (255,)
    elif name == 'ilmenite':
        for y in (6, 9, 12):
            for x in range(3, 13):
                if (x, y) in mask and rng.random() < 0.7:
                    px[x, y] = PAL['ilmenite'][3] + (255,)
    elif name == 'bog_iron':
        for (x, y) in rng.sample(spots, 6):
            px[x, y] = PAL['bog_iron'][0] + (255,)
    return img


# ------------------------------------------------------------------------------------------------ files

def textures():
    for mineral, (_, _, _, _, hosts) in MINERALS.items():
        for host in hosts:
            for face_name, img in host_faces(host).items():
                suffix = '' if face_name == 'all' else '_' + face_name
                G.save_png(ore_face(mineral, host, face_name, img),
                           G.asset('textures', 'block', 'ore', block_id(mineral, host) + suffix + '.png'))
    G.save_png(bauxite_texture(), G.asset('textures', 'block', 'bauxite_ore.png'))
    G.save_png(rock_salt_texture(), G.asset('textures', 'block', 'rock_salt.png'))
    G.save_png(ilmenite_sand_texture(), G.asset('textures', 'block', 'ilmenite_sand.png'))
    for name in ITEMS:
        G.save_png(item_texture(name), G.asset('textures', 'item', name + '.png'))


def models():
    for mineral, (_, _, _, _, hosts) in MINERALS.items():
        for host in hosts:
            name = block_id(mineral, host)
            tex = f'mcremix:block/ore/{name}'
            look = HOSTS[host][1]
            if look in ('COLUMN', 'PILLAR'):
                model = {'parent': 'minecraft:block/cube_column', 'textures': {'side': tex + '_side', 'end': tex + '_end'}}
            elif look == 'SANDSTONE':
                model = {'parent': 'minecraft:block/cube_bottom_top',
                         'textures': {'top': tex + '_top', 'side': tex + '_side', 'bottom': tex + '_bottom'}}
            else:
                model = {'parent': 'minecraft:block/cube_all', 'textures': {'all': tex}}
            D.model(name, model)
            m = f'mcremix:block/{name}'
            if look == 'PILLAR':
                D.states(name, {'variants': {'axis=x': {'model': m, 'x': 90, 'y': 90}, 'axis=y': {'model': m},
                                             'axis=z': {'model': m, 'x': 90}}})
            else:
                D.states(name, {'variants': {'': {'model': m}}})
            D.item_model(name, {'parent': m})
    for name in OWN_ROCKS:
        D.model(name, {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'mcremix:block/{name}'}})
        D.states(name, {'variants': {'': {'model': f'mcremix:block/{name}'}}})
        D.item_model(name, {'parent': f'mcremix:block/{name}'})
    for name in ITEMS:
        D.item_model(name, {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'mcremix:item/{name}'}})


def ore_loot(name, drop, low, high):
    """An ore's loot as vanilla's: itself with Silk Touch, else its mineral, more with Fortune."""
    functions = []
    if (low, high) != (1, 1):
        functions.append({'function': 'minecraft:set_count', 'add': False,
                          'count': {'type': 'minecraft:uniform', 'min': low, 'max': high}})
    functions += [{'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune',
                   'formula': 'minecraft:ore_drops'}, {'function': 'minecraft:explosion_decay'}]
    return {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{
        'type': 'minecraft:alternatives', 'children': [
            {'type': 'minecraft:item', 'name': f'mcremix:{name}', 'conditions': [{
                'condition': 'minecraft:match_tool',
                'predicate': {'enchantments': [{'enchantment': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}]},
            {'type': 'minecraft:item', 'name': drop, 'functions': functions}]}]}],
        'random_sequence': f'mcremix:blocks/{name}'}


def loot():
    for mineral, (_, drop, low, high, hosts) in MINERALS.items():
        for host in hosts:
            name = block_id(mineral, host)
            G.write_json(G.data('mcremix', 'loot_tables', 'blocks', name + '.json'), ore_loot(name, drop, low, high))
    G.write_json(G.data('mcremix', 'loot_tables', 'blocks', 'bauxite_ore.json'),
                 ore_loot('bauxite_ore', 'mcremix:bauxite', 1, 2))
    G.write_json(G.data('mcremix', 'loot_tables', 'blocks', 'rock_salt.json'), ore_loot('rock_salt', 'mcremix:salt', 2, 4))
    G.write_json(G.data('mcremix', 'loot_tables', 'blocks', 'ilmenite_sand.json'), G.loot_self('ilmenite_sand'))


# the metal each mineral is an ore of, for forge:ores/<metal>
METAL = {'bog_iron': 'iron', 'hematite': 'iron', 'magnetite': 'iron', 'malachite': 'copper',
         'chalcopyrite': 'copper', 'native_copper': 'copper', 'sphalerite': 'zinc', 'gold_quartz': 'gold',
         'ilmenite': 'titanium', 'pyrite': 'sulfur', 'sulfur': 'sulfur'}
# the minerals that are raw ore as vanilla's raw iron, copper and gold are: Create crushes them, the crucible melts them
RAW = {'bog_iron': 'iron', 'hematite': 'iron', 'magnetite': 'iron', 'malachite': 'copper', 'native_copper': 'copper',
       'gold_quartz': 'gold'}


def tags():
    m = lambda names: [f'mcremix:{n}' for n in names]
    hard, soft = [], []
    for mineral, (tier, _, _, _, hosts) in MINERALS.items():
        names = ore_blocks(mineral)
        for host in hosts:
            (soft if HOSTS[host][2] else hard).append(block_id(mineral, host))
        if tier == 'STONE':
            D.add_tag('minecraft', 'blocks', 'needs_stone_tool', m(names))
        elif tier == 'IRON':
            D.add_tag('minecraft', 'blocks', 'needs_iron_tool', m(names))
        for kind in ('blocks', 'items'):
            D.add_tag('mcremix', kind, f'ores/{mineral}', m(names))
            D.add_tag('forge', kind, f'ores/{METAL[mineral]}', m(names))
            D.add_tag('forge', kind, 'ores', m(names))
            D.add_tag('forge', kind, 'ores_in_ground/stone', m([block_id(mineral, 'stone')] if 'stone' in hosts else []))
            D.add_tag('forge', kind, 'ores_in_ground/deepslate',
                      m([block_id(mineral, 'deepslate')] if 'deepslate' in hosts else []))
            D.add_tag('forge', kind, 'ore_rates/singular', m(names))
        # vanilla's own ore block tags, which other mods key on and piglins guard gold by (blocks only: the item tags
        # would hand the ores to vanilla's and Create's recipes for raw ore)
        if METAL[mineral] in ('iron', 'copper', 'gold'):
            D.add_tag('minecraft', 'blocks', f'{METAL[mineral]}_ores', m(names))
    D.add_tag('minecraft', 'blocks', 'mineable/pickaxe', m(hard + ['bauxite_ore', 'rock_salt']))
    D.add_tag('minecraft', 'blocks', 'mineable/shovel', m(soft + ['ilmenite_sand']))
    D.add_tag('minecraft', 'blocks', 'needs_stone_tool', m(['bauxite_ore']))
    for kind in ('blocks', 'items'):
        D.add_tag('forge', kind, 'ores/aluminum', m(['bauxite_ore']))
        D.add_tag('forge', kind, 'ores/aluminium', m(['bauxite_ore']))
        D.add_tag('forge', kind, 'ores/salt', m(['rock_salt']))
        D.add_tag('forge', kind, 'ores', m(['bauxite_ore', 'rock_salt']))
        D.add_tag('forge', kind, 'sand', m(['ilmenite_sand']))
    for mineral, metal in RAW.items():
        D.add_tag('forge', 'items', f'raw_materials/{metal}', m([mineral]))
        D.add_tag('forge', 'items', 'raw_materials', m([mineral]))
    # Create's zinc ore smelts straight into zinc by forge:ores/zinc, which sphalerite's ores are now: that is off,
    # as sphalerite is roasted first (its old zinc ore still crushes)
    G.disabled('create', 'smelting/zinc_ingot_from_ore')
    G.disabled('create', 'blasting/zinc_ingot_from_ore')


def firing(path, ingredient, result, heat, time, byproduct=None):
    G.firing(path, {'item': ingredient}, result, heat, time, byproduct=byproduct)


SO2 = 'mcremix:sulfur_dioxide'


def recipes():
    # iron: the oxides fire into a bloom as raw iron does
    for mineral in ('bog_iron', 'hematite', 'magnetite'):
        firing(f'iron_bloom_from_{mineral}', f'mcremix:{mineral}', 'mcremix:iron_bloom', 'searing', 300)
    firing('hematite_from_pyrite', 'mcremix:pyrite', 'mcremix:hematite', 'hot', 200, (SO2, 100))
    # Power Grid's electromagnet pulls crushed magnetite clean of its rock: more iron than a bloom
    G.recipe('magnetization/magnetite', {
        'type': 'powergrid:magnetization', 'ingredients': [G.item('mcremix:magnetite')],
        'results': [G.item('create:crushed_raw_iron'), {'item': 'create:crushed_raw_iron', 'chance': 0.75}]})
    # copper: malachite gives up its carbon dioxide and native copper only melts; chalcopyrite is roasted to matte,
    # and the matte blown Searing until the last of its sulfur is gone
    firing('copper_ingot_from_malachite', 'mcremix:malachite', 'minecraft:copper_ingot', 'hot', 200,
           ('mcremix:carbon_dioxide', 50))
    firing('copper_ingot_from_native_copper', 'mcremix:native_copper', 'minecraft:copper_ingot', 'hot', 100)
    firing('copper_matte', 'mcremix:chalcopyrite', 'mcremix:copper_matte', 'hot', 200, (SO2, 100))
    firing('copper_ingot_from_copper_matte', 'mcremix:copper_matte', 'minecraft:copper_ingot', 'searing', 200,
           (SO2, 50))
    # zinc: sphalerite roasted to calcine, and the calcine fired Searing, the fire's carbon taking its oxygen
    firing('zinc_calcine', 'mcremix:sphalerite', 'mcremix:zinc_calcine', 'hot', 200, (SO2, 100))
    firing('zinc_ingot_from_zinc_calcine', 'mcremix:zinc_calcine', 'create:zinc_ingot', 'searing', 200)
    # gold: fired out of its quartz, or crushed (raw gold's recipe, by its tag)
    firing('gold_ingot_from_gold_quartz', 'mcremix:gold_quartz', 'minecraft:gold_ingot', 'hot', 200)
    # titanium: ilmenite crushed is black sand, which the electromagnet already sorts
    G.recipe('crushing/black_sand_from_ilmenite', {
        'type': 'create:crushing', 'ingredients': [G.item('mcremix:ilmenite')], 'processingTime': 200,
        'results': [G.item('mcremix:black_sand'), {'item': 'mcremix:black_sand', 'chance': 0.5}]})
    G.recipe('splashing/ilmenite_sand', {
        'type': 'create:splashing', 'ingredients': [G.item('mcremix:ilmenite_sand')],
        'results': [G.item('mcremix:black_sand'), {'item': 'minecraft:sand', 'chance': 0.5}]})
    # an ore block (as Silk Touch takes it) crushes into its mineral, more than mining gives, as Create's ores do
    for mineral, (_, drop, low, high, _) in MINERALS.items():
        G.recipe(f'crushing/{mineral}_ore', {
            'type': 'create:crushing', 'ingredients': [{'tag': f'mcremix:ores/{mineral}'}], 'processingTime': 250,
            'results': [G.item(drop, high), {'item': drop, 'chance': 0.75},
                        {'item': 'create:experience_nugget', 'chance': 0.75}]})
    G.recipe('crushing/bauxite_ore', {
        'type': 'create:crushing', 'ingredients': [G.item('mcremix:bauxite_ore')], 'processingTime': 250,
        'results': [G.item('mcremix:bauxite', 2), {'item': 'mcremix:bauxite', 'chance': 0.5}]})
    G.recipe('crushing/rock_salt', {
        'type': 'create:crushing', 'ingredients': [G.item('mcremix:rock_salt')], 'processingTime': 200,
        'results': [G.item('mcremix:salt', 4), {'item': 'mcremix:salt', 'count': 2, 'chance': 0.5}]})
    # the card on old sources: washed red sand keeps its gold and platinum (real placers), but bauxite is mined now
    G.write_json(G.data('create', 'recipes', 'splashing', 'red_sand.json'), {
        'type': 'create:splashing', 'ingredients': [G.item('minecraft:red_sand')],
        'results': [{'item': 'minecraft:gold_nugget', 'count': 3, 'chance': 0.125},
                    {'item': 'mcremix:platinum_nugget', 'chance': 0.01},
                    {'item': 'minecraft:dead_bush', 'chance': 0.05}]})


# the biome tags that say where each kind of ore body goes (Formation.java): over its centre, or every column of it
BODY_BIOMES = {
    'red_beds': ['#minecraft:is_badlands', 'minecraft:desert', {'id': '#forge:is_desert', 'required': False}],
    'bog_iron': ['minecraft:swamp', 'minecraft:mangrove_swamp', '#minecraft:is_river',
                 {'id': '#forge:is_swamp', 'required': False}],
    'porphyry': ['#minecraft:is_mountain', '#minecraft:is_hill', 'minecraft:meadow', 'minecraft:cherry_grove',
                 'minecraft:windswept_savanna', {'id': '#forge:is_mountain', 'required': False},
                 {'id': '#forge:is_peak', 'required': False}, {'id': '#forge:is_slope', 'required': False}],
    'copper_oxide': ['#minecraft:is_badlands', 'minecraft:desert', '#minecraft:is_savanna',
                     {'id': '#forge:is_desert', 'required': False}],
    'gold': ['#minecraft:is_badlands', '#minecraft:is_mountain', '#minecraft:is_hill',
             {'id': '#forge:is_mountain', 'required': False}],
    'gold_high': ['#minecraft:is_badlands'],
    'ilmenite_sand': ['#minecraft:is_beach'],
    'bauxite': ['#minecraft:is_jungle', '#minecraft:is_savanna', {'id': '#forge:is_jungle', 'required': False}],
    'rock_salt': ['#minecraft:is_badlands', 'minecraft:desert', {'id': '#forge:is_desert', 'required': False}],
}

# vanilla's iron, copper and gold ores in the overworld, and Create's zinc: the real minerals replace them (Taran's card)
REPLACED = ['minecraft:ore_iron_upper', 'minecraft:ore_iron_middle', 'minecraft:ore_iron_small',
            'minecraft:ore_copper', 'minecraft:ore_copper_large', 'minecraft:ore_gold', 'minecraft:ore_gold_lower',
            'minecraft:ore_gold_extra', 'create:zinc_ore']


def worldgen():
    for name, values in BODY_BIOMES.items():
        G.write_json(G.data('mcremix', 'tags', 'worldgen', 'biome', 'ore_bodies', name + '.json'),
                     {'replace': False, 'values': values})
    G.write_json(G.data('mcremix', 'worldgen', 'configured_feature', 'ore_bodies.json'),
                 {'type': 'mcremix:ore_bodies', 'config': {}})
    # no placement: once a chunk, at its corner, and OreBodies draws the chunk's part of every body reaching it
    G.write_json(G.data('mcremix', 'worldgen', 'placed_feature', 'ore_bodies.json'),
                 {'feature': 'mcremix:ore_bodies', 'placement': []})
    G.write_json(G.data('mcremix', 'forge', 'biome_modifier', 'ore_bodies.json'), {
        'type': 'forge:add_features', 'biomes': '#minecraft:is_overworld', 'features': 'mcremix:ore_bodies',
        'step': 'underground_ores'})
    G.write_json(G.data('mcremix', 'forge', 'biome_modifier', 'remove_replaced_ores.json'), {
        'type': 'forge:remove_features', 'biomes': '#minecraft:is_overworld', 'features': REPLACED,
        'steps': ['underground_ores']})


# ------------------------------------------------------------------------------------------------ text

MINERAL_NAMES = {'bog_iron': 'Bog Iron', 'hematite': 'Hematite', 'magnetite': 'Magnetite', 'malachite': 'Malachite',
                 'chalcopyrite': 'Chalcopyrite', 'native_copper': 'Native Copper', 'sphalerite': 'Sphalerite',
                 'gold_quartz': 'Gold Quartz', 'ilmenite': 'Ilmenite', 'pyrite': 'Pyrite', 'sulfur': 'Sulfur'}

SUMMARIES = {
    'bog_iron': ('Rusty nodules a block or two under _swamps_, _mangroves_ and _river banks_: the first iron. '
                 '_Fire_ it _Searing_ into a bloom.'),
    'hematite': ("Iron's main ore: wide _banded beds_ under most land, red specks in _desert_ and _badlands_ rock. "
                 '_Fire_ it _Searing_ into a bloom.'),
    'magnetite': ('Black and _magnetic_, in lenses deep in the _deepslate_. Fires into a bloom, or Power Grid\'s '
                  '_electromagnet_ sorts it for more iron.'),
    'malachite': ('Green copper ore near the ground: on top of copper deposits, and under _deserts_, _badlands_ and '
                  '_savannas_. _Fire_ it _Hot_ into copper.'),
    'chalcopyrite': ('Brassy copper ore in big bodies under _hills_ and _mountains_. _Roast_ it _Hot_ into _copper '
                     'matte_ first, and mind the _sulfur dioxide_.'),
    'copper_matte': 'Roasted chalcopyrite. _Fire_ it _Searing_ to blow out the last sulfur, and copper is left.',
    'native_copper': 'Copper as the ground made it, in sheets through _basalt_. It only needs _melting_.',
    'sphalerite': ('Zinc ore, in pods through _limestone_ and stone. _Roast_ it _Hot_ into _zinc calcine_, giving '
                   'off _sulfur dioxide_.'),
    'zinc_calcine': "Roasted sphalerite. _Fired Searing_, the fire's carbon takes its oxygen and leaves _zinc_.",
    'gold_quartz': ('Milky quartz shot through with gold, in long thin veins, richest under _badlands_ and '
                    '_mountains_. _Crush_ it, or _fire_ it _Hot_.'),
    'ilmenite': 'Black titanium ore, in seams deep in _deepslate_ and _basalt_. Crushed, it is _black sand_.',
    'pyrite': ('_Fool\'s gold_, scattered through the rock. _Roasted Hot_ it turns to _hematite_, giving off '
               '_sulfur dioxide_.'),
}


def lang():
    def host_name(host):
        return ' '.join(w.capitalize() for w in host.split('_'))

    text = {}
    for mineral, (_, _, _, _, hosts) in MINERALS.items():
        for host in hosts:
            prefix = '' if host == 'stone' else host_name(host) + ' '
            text[f'block.mcremix.{block_id(mineral, host)}'] = f'{prefix}{MINERAL_NAMES[mineral]} Ore'
    for name in ITEMS:
        text[f'item.mcremix.{name}'] = MINERAL_NAMES.get(name, ' '.join(w.capitalize() for w in name.split('_')))
        text[f'item.mcremix.{name}.tooltip.summary'] = SUMMARIES[name]
    text.update({
        'block.mcremix.bauxite_ore': 'Bauxite Ore',
        'block.mcremix.bauxite_ore.tooltip.summary': (
            "_Aluminium's_ ore: red-brown laterite in a blanket just under the ground of _jungles_ and _savannas_."),
        'block.mcremix.rock_salt': 'Rock Salt',
        'block.mcremix.rock_salt.tooltip.summary': (
            'Halite in thick beds, left by seas that dried up long ago, deep under _deserts_ and _badlands_. It '
            'breaks into _salt_.'),
        'block.mcremix.ilmenite_sand': 'Ilmenite Sand',
        'block.mcremix.ilmenite_sand.tooltip.summary': (
            'Beach sand streaked black where the waves sorted out the heavy grains. _Wash_ it for _black sand_.'),
        'item.mcremix.bauxite.tooltip.summary': (
            "Aluminium's ore, mined from _bauxite_ just under the ground of _jungles_ and _savannas_."),
    })
    D.merge_json(G.asset('lang', 'en_us.json'), lambda obj: obj.update(text))

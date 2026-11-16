"""
Art for the air liquefier and what is made beside it: the aluminium column section, bare and frosting over as it runs
cold, the Linde expansion valve, and the platinum, catalyst, urea and sodium nitrite items. gen_assets.py calls run()
after gen_column, passing itself. Only textures, block and item models and blockstates are written here; the blocks'
Java, recipes, loot, tags and lang are written elsewhere. The column section is drawn with gen_column's own plate,
flange and nozzle painters in aluminium rather than steel, so the two read as one family.
"""
import math
import random

from PIL import Image

import gen_column as C
import gen_decor as D

G = None  # gen_assets, set by run()


def mix(a, b, t):
    return tuple(max(0, min(255, round(a[i] + (b[i] - a[i]) * t))) for i in range(3))


def ramp(base, dark, light, deep):
    """
    Seven shades from one of gen_assets' palettes, laid out as gen_decor's STEEL is: 1 its dark, 3 its base, 6 its
    light, and 0 a deep shadow under its dark for bores and holes. The shades either side of its base lie close to it,
    so a plate's mottle stays faint on metal as pale as aluminium.
    """
    return [mix(dark, deep, 0.45), dark, mix(dark, base, 0.7), base, mix(base, light, 0.4), mix(base, light, 0.75),
            light]


# bare aluminium, from gen_assets' PALETTES['aluminium']: paler than iron and a little blue
ALUMINIUM = ramp((0xCC, 0xD3, 0xDA), (0x92, 0x9B, 0xA6), (0xEE, 0xF2, 0xF6), deep=(0x20, 0x26, 0x30))
# polished aluminium (Taran, 0.36.0: clean and shiny, as the aluminium tanks and pipes are): bright, a little blue,
# with more between its shadow and its highlight than bare aluminium's mottle needs, so seams and edges read crisply
POLISHED = [(0x3C, 0x44, 0x50), (0x7C, 0x86, 0x93), (0xA2, 0xAB, 0xB6), (0xC2, 0xC9, 0xD1), (0xD5, 0xDB, 0xE1),
            (0xE5, 0xE9, 0xEE), (0xF6, 0xF8, 0xFA)]
# brushed: the shade of each column of a polished plate, faint lines running down it
STREAKS = (4, 4, 5, 4, 4, 4, 4, 5, 5, 4, 4, 4, 4, 4, 5, 4)
# rime over aluminium, by the shade of the metal under it: white, with a cold blue in its shadows
RIME = [(0xA2, 0xB6, 0xCC), (0xC0, 0xCF, 0xDF), (0xD4, 0xE0, 0xEB), (0xE6, 0xEE, 0xF5), (0xF1, 0xF6, 0xFA),
        (0xF8, 0xFB, 0xFD), (0xFF, 0xFF, 0xFF)]
# icicle ice, root to tip: frost white where it grows from the flange, clear pale blue down its length
ICE = [(0xFF, 0xFF, 0xFF), (0xEA, 0xF4, 0xFC), (0xD4, 0xEA, 0xF8), (0xC0, 0xDE, 0xF2), (0xB0, 0xD4, 0xEE),
       (0xCC, 0xE6, 0xF8)]
# the column section's three looks by its frost property, and the prefix each look's textures and models take
FROSTS = (('', 0), ('frosted_', 1), ('frozen_', 2))
TURN = {'north': 0, 'east': 90, 'south': 180, 'west': 270}


def run(gen):
    global G
    G = gen
    column_textures()
    column_section()
    valve_textures()
    expansion_valve()
    items()


def png(img, name):
    G.save_png(img, G.asset('textures', 'block', name + '.png'))


def model(name, obj):
    G.write_json(G.asset('models', 'block', name + '.json'), obj)


def item_model(name, obj):
    G.write_json(G.asset('models', 'item', name + '.json'), obj)


def states(name, obj):
    G.write_json(G.asset('blockstates', name + '.json'), obj)


# ------------------------------------------------------------------------------------------------ frost

def blotches(seed):
    """Soft, uneven patches over a texture, about 0 to 1: two sizes of smoothed random grid, wrapped so they tile."""
    rng = random.Random(seed)
    out = [[0.0] * 16 for _ in range(16)]
    smooth = lambda t: t * t * (3 - 2 * t)
    for cell, weight in ((4, 0.65), (2, 0.35)):
        n = 16 // cell
        grid = [[rng.random() for _ in range(n)] for _ in range(n)]
        g = lambda i, j: grid[j % n][i % n]
        for y in range(16):
            for x in range(16):
                gx, gy = (x + 0.5) / cell - 0.5, (y + 0.5) / cell - 0.5
                x0, y0 = math.floor(gx), math.floor(gy)
                fx, fy = smooth(gx - x0), smooth(gy - y0)
                top = g(x0, y0) + (g(x0 + 1, y0) - g(x0, y0)) * fx
                bottom = g(x0, y0 + 1) + (g(x0 + 1, y0 + 1) - g(x0, y0 + 1)) * fx
                out[y][x] += weight * (top + (bottom - top) * fy)
    return out


# how cold a pixel must be, by frost level, for rime to cover it, and for a thin bloom of it at a patch's edge
COVER = {1: (0.76, 0.66), 2: (0.40, 0.30)}


def frost(img, seed, level, cold=lambda x, y: 0.0, pal=POLISHED):
    """
    Rime over a texture drawn in `pal`. Level 1: white patches, granular, thickest where `cold` says frost gathers,
    each a crust with a faint shadow under its lower right edge. Level 2: thick frost over nearly all of it, whiter,
    the plate's seams and rivets showing through only as faint bumps, what metal still shows sunk in shadow. A bore,
    drawn in the palette's deepest shade, stays dark.
    """
    if not level:
        return img
    rng = random.Random(seed)
    field = blotches(seed)
    index = {c: i for i, c in enumerate(pal)}
    hard, soft = COVER[level]
    px = img.load()
    under = {}
    for y in range(16):
        for x in range(16):
            c = px[x, y]
            if c[3] == 0:
                continue
            i = index.get(c[:3])
            if i is None:
                i = min(range(len(pal)), key=lambda k: sum(abs(pal[k][j] - c[j]) for j in range(3)))
            under[x, y] = (i, field[y][x] * 0.8 + rng.random() * 0.2 + cold(x, y))
    for (x, y), (i, v) in under.items():
        c = px[x, y]
        if i == 0:
            continue
        frosted = [under[n][1] >= soft for n in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)) if n in under]
        if v < soft and sum(frosted) >= 3:
            v = soft  # a lone pixel of metal in the rime takes a thin bloom of it, not a speck of dirt
        if v < soft:
            # bare metal just under or right of a crust lies in its shadow
            if any(under.get(n, (0, 0))[1] >= hard and under[n][0] for n in ((x, y - 1), (x - 1, y))):
                px[x, y] = D.shade(pal, max(1, i - 1))
            continue
        # rime is whiter than the plate under it; thick frost evens the plate out, leaving only a trace of it
        i = (i + 6) // 2 if level == 2 else i + 1
        r = rng.random()
        # rime is granular: a few crystals catch the light, a few lie in shadow
        j = max(1, min(6, i + (2 if r < 0.15 else -1 if r < 0.27 else 0)))
        col = RIME[j]
        if v < hard:
            col = mix(c[:3], col, 0.5)
        px[x, y] = col + (255,)
    return img


def low_and_flanged(x, y):
    """Where frost gathers on a section's side: low down, and along the flange at its top and bottom."""
    return 0.4 * (y / 15) ** 2 + (0.25 if y < 2 or y > 13 else 0.12 if y in (2, 13) else 0.0)


def round_the_rim(x, y):
    """On a section's top or bottom: round the flange ring at its edge, and settled over the rest."""
    d = min(x, y, 15 - x, 15 - y)
    return 0.3 if d < 2 else 0.18 if d < 3 else 0.08


def under_the_bolts(x, y):
    """On flange stock: heaviest along each band's shaded underside, and a little everywhere."""
    return 0.12 + (0.15 if y % 4 == 3 else 0.0)


def round_the_nozzle(x, y):
    """On a nozzle's face: on the outer ring of its flange, which is all a pipe leaves showing."""
    d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
    return 0.2 if d > 5.5 else 0.08


# ------------------------------------------------------------------------------------------------ column textures

def icicle_texture():
    """
    Icicle ice: each column a shade of clear pale blue, a highlight down every fourth, frost white at the top where it
    grows from the flange. An icicle's faces each take one column, from the top down its length.
    """
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            col = ICE[min(y, len(ICE) - 1)]
            if x % 4 == 0 and y:
                col = mix(col, (0xFF, 0xFF, 0xFF), 0.5)
            elif x % 4 == 3 and y:
                col = mix(col, (0x8C, 0xB8, 0xDC), 0.3)
            px[x, y] = col + (255,)
    return img


def polished_plate():
    """A polished aluminium plate: smooth, brushed in faint lines down it, no mottle and no rivets."""
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = D.shade(POLISHED, STREAKS[x])
    return img


def section_side(left=False, right=False, up=False, down=False):
    """
    An aluminium column section's side, polished, as the connected texture draws it on a formed column: a brushed
    plate running on across the column, a crisp seam under the slim flange round its top and over the one round its
    bottom, a lit band under the column's top flange for its cap and a darker skirt at its foot, and a lit edge and a
    shaded one where its plates turn the column's corners. A lone section is all of these.
    """
    img = polished_plate()
    px = img.load()
    row = lambda y, i: [px.__setitem__((x, y), D.shade(POLISHED, i)) for x in range(16)]
    row(0, 2)
    row(15, 2)
    if up:
        row(1, 2)
    else:
        row(1, 6)
        row(2, 5)
        row(3, 2)
    if down:
        row(14, 5)
    else:
        row(12, 2)
        row(13, 3)
        row(14, 3)
    for y in range(16):
        if not left:
            px[0, y] = D.shade(POLISHED, 6)
        if not right:
            px[15, y] = D.shade(POLISHED, 2)
    return img


def section_top(left=False, right=False, up=False, down=False):
    """
    A section's top or bottom, polished, as the connected texture draws it across a formed column: one brushed plate,
    edged where the column ends, lit along its top and left and shaded along its bottom and right, and a crisp groove
    round it two pixels in from those edges, lit just inside along its top and left.
    """
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = D.shade(POLISHED, 6 if (x + 2 * y) % 11 == 0 else 5)
    x0, x1 = (0 if left else 2), (15 if right else 13)
    y0, y1 = (0 if up else 2), (15 if down else 13)
    for x in range(x0, x1 + 1):
        if not up:
            px[x, 2] = D.shade(POLISHED, 2)
            if 3 <= x <= 12 or left and x < 3 or right and x > 12:
                px[x, 3] = D.shade(POLISHED, 6)
        if not down:
            px[x, 13] = D.shade(POLISHED, 2)
    for y in range(y0, y1 + 1):
        if not left:
            px[2, y] = D.shade(POLISHED, 2)
            if 3 <= y <= 12 or up and y < 3 or down and y > 12:
                px[3, y] = D.shade(POLISHED, 6)
        if not right:
            px[13, y] = D.shade(POLISHED, 2)
    for i in range(16):
        if not up:
            px[i, 0] = D.shade(POLISHED, 6)
        if not left:
            px[0, i] = D.shade(POLISHED, 6)
        if not down:
            px[i, 15] = D.shade(POLISHED, 2)
        if not right:
            px[15, i] = D.shade(POLISHED, 2)
    return img


def polished_flange():
    """
    Flange stock, polished: bands four pixels deep, each lit along its top edge and shaded along its bottom, so any
    strip of it a flange's narrow faces show reads as a crisp ring. No bolts.
    """
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = D.shade(POLISHED, (6, 5, 4, 2)[y % 4])
    return img


def polished_nozzle():
    """
    A flanged nozzle seen end on, polished: a smooth flange round a dark bore, a bolt at each corner of it, the bore's
    rim shaded on its far side. A Create pipe joining it covers 3 to 13 with its rim, so the bolts sit outside that.
    """
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 3.2:
                i = 0
            elif d < 4.4:
                i = 2 if x + y > 15 else 3
            elif x in (1, 14) or y in (1, 14):
                i = 6 if x == 1 or y == 1 else 2
            else:
                i = 5 if x + y < 15 else 4
            px[x, y] = D.shade(POLISHED, i)
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
        px[x, y] = D.shade(POLISHED, 6)
        px[x + 1, y + 1] = D.shade(POLISHED, 2)
    return img


def rim_frost(left, right, up, down):
    """
    Where frost gathers on a section's top or bottom as the connected texture draws it: round the column's rim, where
    the column ends, and settled over the rest.
    """
    def cold(x, y):
        d = min([16] + ([x] if not left else []) + ([15 - x] if not right else []) + ([y] if not up else [])
                + ([15 - y] if not down else []))
        return 0.3 if d < 2 else 0.18 if d < 3 else 0.08
    return cold


def column_textures():
    """
    The section's plate and top, polished, and joined up across a formed column by the connected textures (Taran,
    0.36.0), and its flange and nozzle, bare and at each level of frost. Each tile of a sheet frosts as a block does.
    """
    for prefix, level in FROSTS:
        side = lambda l, r, u, d: frost(section_side(l, r, u, d), 4711 + level, level, low_and_flanged)
        top = lambda l, r, u, d: frost(section_top(l, r, u, d), 4721 + level, level, rim_frost(l, r, u, d))
        png(side(False, False, False, False), f'{prefix}aluminium_column_section')
        png(C.ct_sheet(side), f'{prefix}aluminium_column_section_connected')
        png(top(False, False, False, False), f'{prefix}aluminium_column_section_top')
        png(C.ct_sheet(top), f'{prefix}aluminium_column_section_top_connected')
        png(frost(polished_flange(), 4731 + level, level, under_the_bolts), f'{prefix}aluminium_column_flange')
        png(frost(polished_nozzle(), 4741 + level, level, round_the_nozzle), f'{prefix}aluminium_column_nozzle')
    png(icicle_texture(), 'aluminium_column_icicle')


# ------------------------------------------------------------------------------------------------ column models

# icicles hanging off each side's bottom flange: the left edge of each, as seen from outside that side, and its whole
# length, uneven, its last pixel a thinner tip
ICICLES = {'north': ((2.5, 3), (6, 5), (9, 2), (12.5, 4)),
           'east': ((1.5, 2), (4.5, 4), (8.5, 3), (13, 5)),
           'south': ((3, 4), (7, 2.5), (10.5, 5), (13.5, 2)),
           'west': ((2, 5), (5.5, 2), (9.5, 3.5), (12, 2.5))}
SIDES = ['north', 'east', 'south', 'west']


def turned(frm, to, faces, side):
    """A box drawn on the north side, turned about the block's upright axis to `side`, its faces turned with it."""
    (x0, y0, z0), (x1, y1, z1) = frm, to
    for _ in range(TURN[side] // 90):
        # a quarter turn clockwise seen from above: north to east, (x, z) to (16 - z, x)
        x0, z0, x1, z1 = 16 - z1, x0, 16 - z0, x1
    k = TURN[side] // 90
    faces = {(SIDES[(SIDES.index(d) + k) % 4] if d in SIDES else d): f for d, f in faces.items()}
    return D.el((x0, y0, z0), (x1, y1, z1), faces)


def icicles():
    """
    The frozen section's icicles: about a pixel square, hanging from the bottom flange's lip a pixel out from the side,
    rooted on the lip's face and reaching below the block, each ending in a half pixel tip. They are culled with their
    side, as the lips are, so they show only on the column's outside.
    """
    els = []
    for side, hanging in ICICLES.items():
        for n, (x, length) in enumerate(hanging):
            u = (n * 5) % 16
            body = {d: D.face('#ice', [u, 0, u + 1, length - 1], cull=side) for d in ('north', 'east', 'west')}
            body['down'] = D.face('#ice', [u, length - 2, u + 1, length - 1], cull=side)
            els.append(turned((x, 2 - length, -1), (x + 1, 1, 0), body, side))
            tip = {d: D.face('#ice', [u, length - 1, u + 0.5, length], cull=side)
                   for d in ('north', 'east', 'west', 'south')}
            tip['down'] = D.face('#ice', [u, length - 1, u + 0.5, length - 0.5], cull=side)
            els.append(turned((x + 0.25, 1 - length, -0.75), (x + 0.75, 2 - length, -0.25), tip, side))
    return els


def column_section():
    """
    As steel's section: a full cube with a flange lip half a pixel proud round its top and bottom on each outer side,
    a pixel deep where steel's is a pixel and a half, and a flanged nozzle drawn on the north side, turned by the
    blockstate to each side a pipe joins. Each frost level has its own section and nozzle; the frozen section has
    icicles too.
    """
    for prefix, _ in FROSTS:
        tex = {'side': f'mcremix:block/{prefix}aluminium_column_section',
               'top': f'mcremix:block/{prefix}aluminium_column_section_top',
               'flange': f'mcremix:block/{prefix}aluminium_column_flange',
               'particle': f'mcremix:block/{prefix}aluminium_column_section'}
        cube = D.el((0, 0, 0), (16, 16, 16), {
            d: D.face('#top' if d in ('up', 'down') else '#side', [0, 0, 16, 16], cull=d)
            for d in ('north', 'east', 'south', 'west', 'up', 'down')})
        elements = [cube]
        # aluminium's flange is slimmer than steel's: a pixel round its top and bottom (Taran, 0.36.0)
        for side in SIDES:
            elements.append(C.lip(side, 0, 1))
            elements.append(C.lip(side, 15, 16))
        if prefix == 'frozen_':
            tex['ice'] = 'mcremix:block/aluminium_column_icicle'
            elements += icicles()
        model(f'{prefix}aluminium_column_section', {'parent': 'minecraft:block/block', 'textures': tex,
                                                    'elements': elements})
        model(f'{prefix}aluminium_column_nozzle', {
            'textures': {'nozzle': f'mcremix:block/{prefix}aluminium_column_nozzle',
                         'flange': f'mcremix:block/{prefix}aluminium_column_flange',
                         'particle': f'mcremix:block/{prefix}aluminium_column_section'},
            'elements': [D.el((1, 1, -2), (15, 15, 0), {
                'north': D.face('#nozzle', [1, 1, 15, 15]),
                'up': D.face('#flange', [1, 4, 15, 6]), 'down': D.face('#flange', [1, 4, 15, 6]),
                'east': D.face('#flange', [4, 1, 6, 15]), 'west': D.face('#flange', [4, 1, 6, 15])})]})
    parts = []
    for prefix, level in FROSTS:
        when = {'frost': str(level)}
        parts.append({'when': when, 'apply': {'model': f'mcremix:block/{prefix}aluminium_column_section'}})
        for side, y in TURN.items():
            apply = {'model': f'mcremix:block/{prefix}aluminium_column_nozzle'}
            if y:
                apply['y'] = y
            parts.append({'when': {**when, side: 'true'}, 'apply': apply})
    states('aluminium_column_section', {'multipart': parts})
    item_model('aluminium_column_section', {'parent': 'mcremix:block/aluminium_column_section'})


# ------------------------------------------------------------------------------------------------ valve textures

# the drum's shading across its twelve pixels: gen_column's ROUND kept bright, as polished aluminium is
DRUM = (3, 4, 5, 6, 6, 5, 5, 4, 4, 4, 3, 2)


def drum_side():
    """
    The separator drum's side, polished: a square drum shaded round across its twelve pixels as Create shades its
    tanks (DRUM), with two crisp bands round it, each a groove shaded under a lit edge. No rivets.
    """
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for x in range(16):
        i = DRUM[min(11, max(0, x - 2))]
        for y in range(16):
            px[x, y] = D.shade(POLISHED, i + (y < 4) + (y < 2))
        for y0 in (5, 10):
            px[x, y0 - 1] = D.shade(POLISHED, min(6, i + 2))
            px[x, y0] = D.shade(POLISHED, max(1, i - 2))
            px[x, y0 + 1] = D.shade(POLISHED, max(1, i - 1))
    return img


def head():
    """
    The drum's head, seen end on, polished: a smooth dish lit towards its upper left inside a crisp groove, and its
    rim lit along its top and left edges and shaded along its bottom and right.
    """
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if x in (0, 15) or y in (0, 15):
                i = 6 if x == 0 or y == 0 else 2
            elif 5.4 <= r < 6.4:
                i = 2 if x + y > 15 else 6
            elif r < 5.4:
                i = 6 if x + y < 9 else 5 if x + y < 17 else 4
            else:
                i = 4
            px[x, y] = D.shade(POLISHED, i)
    return img


def pipe_side():
    """A stub's side, polished: a pipe shaded round, lit just left of its middle, darkest at its right edge."""
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    across = (2, 3, 4, 4, 5, 5, 4, 6, 5, 3, 3, 3, 2, 2, 2, 3)
    for y in range(16):
        for x in range(16):
            px[x, y] = D.shade(POLISHED, across[x])
    return img


def port():
    """
    A small flange seen end on, polished, for the stubs' ends and the valve's inlet: in its middle six pixels, a dark
    bore in a shadowed ring, a bolt at each corner, lit along its top and left edges.
    """
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if not (5 <= x <= 10 and 5 <= y <= 10):
                i = 5
            elif d < 1.2:
                i = 0
            elif d < 2.2:
                i = 2 - (x + y > 15)
            elif x == 5 or y == 5:
                i = 6
            elif x == 10 or y == 10:
                i = 2
            else:
                i = 4
            px[x, y] = D.shade(POLISHED, i)
    for x, y in ((6, 6), (9, 6), (6, 9), (9, 9)):
        px[x, y] = D.shade(POLISHED, 6)
    return img


def cast():
    """The valve's body, bonnet and stem, cast and polished: lit along the top, shading down its face, no speckle."""
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = D.shade(POLISHED, 6 if y < 2 or x == 7 else 5 if y < 9 else 4)
    return img


def wheel():
    """The handwheel, polished: bright, a highlight along every fourth pixel so its thin bars glint."""
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = D.shade(POLISHED, 6 if (x + y) % 4 == 0 or y < 1 else 4 if (x + y) % 4 == 2 else 5)
    return img


def colder_low(x, y):
    """On the drum: liquid air gathers in its bottom, so it frosts thickest low down."""
    return 0.6 * (y / 15) ** 1.5


def valve_textures():
    """
    All polished aluminium (Taran, 0.36.0): the drum, its heads and stubs, bare and frosted; the valve's body, inlet
    and handwheel, on the warm side of it, never frost.
    """
    for prefix, level in (('', 0), ('frosted_', 1)):
        png(frost(drum_side(), 4761, level, colder_low), f'{prefix}expansion_valve_drum')
        png(frost(head(), 4762, level, lambda x, y: 0.15), f'{prefix}expansion_valve_head')
        png(frost(pipe_side(), 4763, level, lambda x, y: 0.3), f'{prefix}expansion_valve_stub')
        png(frost(port(), 4764, level, lambda x, y: 0.25), f'{prefix}expansion_valve_port')
    png(cast(), 'expansion_valve_body')
    png(port(), 'expansion_valve_inlet')
    png(wheel(), 'expansion_valve_handwheel')


# ------------------------------------------------------------------------------------------------ the valve

def box(tex, frm, to, skip=()):
    return D.el(frm, to, D.box_faces(tex, frm, to, skip=skip))


def handwheel(cx, cz, y0, y1, a=2.75, t=0.75):
    """
    A handwheel lying flat, as gen_oil draws the flywheel's rim: eight bars round it, four square to the axes and four
    turned 45 degrees about their middles, and two thin spokes crossing at its hub; `a` is from its middle to the
    outside of a bar.
    """
    half = a * math.tan(math.pi / 8)
    bars = [((cx - half, y0, cz - a), (cx + half, y1, cz - a + t)),
            ((cx - half, y0, cz + a - t), (cx + half, y1, cz + a)),
            ((cx - a, y0, cz - half), (cx - a + t, y1, cz + half)),
            ((cx + a - t, y0, cz - half), (cx + a, y1, cz + half))]
    els = [box('#wheel', frm, to) for frm, to in bars]
    d = (a - t / 2) / math.sqrt(2)
    for sx, sz in ((1, 1), (-1, -1), (1, -1), (-1, 1)):
        x, z = cx + sx * d, cz + sz * d
        # a bar long in x turned +45 about y lies along (1, -1): the tangent where the corner is (+,+) or (-,-)
        if sx == sz:
            frm, to = (x - half, y0, z - t / 2), (x + half, y1, z + t / 2)
        else:
            frm, to = (x - t / 2, y0, z - half), (x + t / 2, y1, z + half)
        e = box('#wheel', frm, to)
        e['rotation'] = {'origin': [round(x, 3), (y0 + y1) / 2, round(z, 3)], 'axis': 'y', 'angle': 45}
        els.append(e)
    m, s = (y0 + y1) / 2, 0.25
    els.append(box('#wheel', (cx - a + t / 2, m - s, cz - s), (cx + a - t / 2, m + s, cz + s)))
    els.append(box('#wheel', (cx - s, m - s, cz - a + t / 2), (cx + s, m + s, cz + a - t / 2)))
    for e in els:
        e['from'] = [round(v, 3) for v in e['from']]
        e['to'] = [round(v, 3) for v in e['to']]
    return els


def valve_elements():
    """
    The expansion valve facing north: a squat aluminium separator drum twelve pixels across, a square box shaded round
    (Taran, 0.36.0: no stepped edges), two bands round it and a flange for its top, with a short flanged stub up
    out of its top, where the cold gas left over leaves, and one down out of its bottom, where liquid air drips out. On
    its north side, the valve: a body at the drum's middle, its inlet flange at the block's face where a pipe meets it,
    and a bonnet up the drum's front to a handwheel lying flat over the top flange's front edge, above where a pipe's
    rim would hide it. All of it polished aluminium.
    """
    els = []
    drum = box('#drum', (2, 2, 2), (14, 13, 14), skip=('up',))
    drum['faces']['down'] = D.face('#head', D.inset([2, 2, 14, 14]))
    for d in SIDES:
        drum['faces'][d] = D.face('#drum', [2, 3, 14, 14])
    els.append(drum)
    faces = {d: D.face('#flange', [1.5, 4.5, 14.5, 5.5]) for d in SIDES}
    faces['up'] = D.face('#head', D.inset([1.5, 1.5, 14.5, 14.5]))
    faces['down'] = D.face('#flange', D.inset([1.5, 1.5, 14.5, 14.5]))
    els.append(D.el((1.5, 13, 1.5), (14.5, 14, 14.5), faces))
    # the stubs: a neck four pixels across, and a flange six across at its end
    for neck, flange, end in ((((6, 14, 6), (10, 15, 10)), ((5, 15, 5), (11, 16, 11)), 'up'),
                              (((6, 1, 6), (10, 2, 10)), ((5, 0, 5), (11, 1, 11)), 'down')):
        els.append(box('#stub', *neck, skip=('up', 'down')))
        faces = {d: D.face('#stub_flange', [5, 4.5, 11, 5.5]) for d in SIDES}
        faces[end] = D.face('#stub_end', [5, 5, 11, 11], cull=end)
        faces['down' if end == 'up' else 'up'] = D.face('#stub_end', [5, 5, 11, 11])
        els.append(D.el(*flange, faces))
    # the valve: body, inlet flange, bonnet, stem and hub, then the wheel
    body = box('#body', (5.5, 5, 0.5), (10.5, 10, 2.5), skip=('south', 'north'))
    inlet = D.el((5, 4.5, 0), (11, 10.5, 0.5), {
        'north': D.face('#inlet', [5, 5, 11, 11], cull='north'),
        **{d: D.face('#inlet', [5, 5, 11, 5.5]) for d in ('east', 'west', 'up', 'down')}})
    els += [body, inlet,
            box('#body', (6.5, 10, 0.75), (9.5, 11.5, 2.25), skip=('south', 'down')),
            box('#body', (7.5, 11.5, 1.25), (8.5, 14.25, 2.25), skip=('south', 'down')),
            box('#body', (7.25, 14.25, 1), (8.75, 15.5, 2.5), skip=('down',))]
    els += handwheel(8, 1.75, 14.5, 15.25)
    return els


VALVE_TEXTURES = {'drum': 'mcremix:block/expansion_valve_drum', 'head': 'mcremix:block/expansion_valve_head',
                  'flange': 'mcremix:block/aluminium_column_flange', 'stub': 'mcremix:block/expansion_valve_stub',
                  'stub_flange': 'mcremix:block/aluminium_column_flange',
                  'stub_end': 'mcremix:block/expansion_valve_port', 'inlet': 'mcremix:block/expansion_valve_inlet',
                  'body': 'mcremix:block/expansion_valve_body', 'wheel': 'mcremix:block/expansion_valve_handwheel',
                  'particle': 'mcremix:block/expansion_valve_drum'}
# working, its drum and stubs frost over, the stubs (which carry the coldest of it) the thickest; its inlet, valve and
# handwheel stay bare
FROSTED_TEXTURES = {'drum': 'mcremix:block/frosted_expansion_valve_drum',
                    'head': 'mcremix:block/frosted_expansion_valve_head',
                    'flange': 'mcremix:block/frosted_aluminium_column_flange',
                    'stub': 'mcremix:block/frosted_expansion_valve_stub',
                    'stub_flange': 'mcremix:block/frozen_aluminium_column_flange',
                    'stub_end': 'mcremix:block/frosted_expansion_valve_port',
                    'particle': 'mcremix:block/frosted_expansion_valve_drum'}


def expansion_valve():
    model('expansion_valve', {'parent': 'minecraft:block/block', 'textures': VALVE_TEXTURES,
                              'elements': valve_elements()})
    model('frosted_expansion_valve', {'parent': 'mcremix:block/expansion_valve', 'textures': FROSTED_TEXTURES})
    variants = {}
    for facing, y in TURN.items():
        for frosted in ('false', 'true'):
            v = {'model': 'mcremix:block/' + ('frosted_' if frosted == 'true' else '') + 'expansion_valve'}
            if y:
                v['y'] = y
            variants[f'facing={facing},frosted={frosted}'] = v
    states('expansion_valve', {'variants': variants})
    # block/block, which the model is drawn on, gives the item the display every block item has, as the condenser's
    item_model('expansion_valve', {'parent': 'mcremix:block/expansion_valve'})


# ------------------------------------------------------------------------------------------------ items

# platinum, nine shades darkest first for Create's nugget: a cool silvery white, a little warm in its greys, brighter
# in its highlights than iron
PLATINUM_SHADES = [(0x46, 0x44, 0x42), (0x5E, 0x5B, 0x59), (0x7A, 0x77, 0x75), (0x97, 0x95, 0x93), (0xB4, 0xB2, 0xB1),
                   (0xCD, 0xCC, 0xCB), (0xE2, 0xE2, 0xE2), (0xF3, 0xF3, 0xF4), (0xFF, 0xFF, 0xFF)]


def outline(img, colour):
    """A dark rim round everything drawn, as items are drawn."""
    shape = {(x, y) for y in range(16) for x in range(16) if img.getpixel((x, y))[3]}
    for y in range(16):
        for x in range(16):
            if (x, y) not in shape and any((x + dx, y + dy) in shape for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                img.putpixel((x, y), colour + (255,))


def platinum_gauze():
    """
    Platinum wire woven into a fine square of gauze, finer than the copper mesh: a wire every other pixel, each
    crossing lit where the wire on top passes over, the far edges in shadow.
    """
    base, dark, light, under = (0xC6, 0xC8, 0xCA), (0x7E, 0x80, 0x84), (0xFA, 0xFA, 0xFB), (0xA4, 0xA6, 0xA9)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(2, 14):
        for x in range(2, 14):
            on_x, on_y = x % 2 == 0, y % 2 == 0
            if not (on_x or on_y):
                continue
            if on_x and on_y:
                col = light if (x + y) % 4 == 0 else base
            else:
                # between crossings a wire dips under the next one across it
                col = under if (x // 2 + y // 2) % 2 else base
            if x == 13 or y == 13:
                col = dark
            elif x == 2 or y == 2:
                col = mix(col, light, 0.5)
            img.putpixel((x, y), col + (255,))
    return img


def heap(seed, pieces, draw):
    """A little heap, drawn back to front so the pieces in front lie over those behind, each outlined by `draw`."""
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for cx, cy, size in pieces:
        piece = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        draw(piece, cx + rng.uniform(-0.25, 0.25), cy + rng.uniform(-0.25, 0.25), size, rng)
        img.alpha_composite(piece)
    return img


def iron_catalyst():
    """
    Fused magnetite: a little heap of dark grey-black granules, rough and uneven, each lit on its upper left with a
    steely glint and outlined black, flecked here and there with the white of the alumina in it.
    """
    edge, dark, base, light, glint = (0x0C, 0x0C, 0x0E), (0x1E, 0x1E, 0x23), (0x36, 0x37, 0x3D), (0x5E, 0x60, 0x6A), \
        (0x9C, 0xA0, 0xAC)
    specks = ((0xF2, 0xF0, 0xEA), (0xC8, 0xC6, 0xC0))

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
                col = mix(base, light, (t - 0.65) * 3) if t > 0.65 else mix(dark, base, t / 0.65)
                img.putpixel((x, y), col + (255,))
                inside.append((x, y, dx + dy))
        x, y, _ = min(inside, key=lambda p: p[2])
        img.putpixel((x + 1, y + 1) if img.getpixel((x + 1, y + 1))[3] else (x, y), glint + (255,))
        # the alumina: on about half the granules, a white fleck away from the glint
        clear = [p for p in inside if abs(p[0] - x) + abs(p[1] - y) > 2 and -1.5 < p[2] < 1.5]
        if clear and rng.random() < 0.55:
            sx, sy, _ = rng.choice(clear)
            img.putpixel((sx, sy), rng.choice(specks) + (255,))
        outline(img, edge)

    return heap(4771, [(5, 5.5, 2.4), (10.5, 5, 2.2), (3.5, 9.5, 2.3), (8, 9, 2.6), (12.5, 9.5, 2.2), (6, 12.5, 2.4),
                       (10.5, 12.5, 2.3)], granule)


def urea():
    """
    A little heap of urea prills: tiny round beads, a faint off-white, packed in rows, each lit on its upper left and
    shadowed on its lower right, the gaps between them in shade and the heap outlined.
    """
    light, base, shade, gap, edge = (0xFF, 0xFF, 0xFC), (0xF0, 0xEE, 0xE6), (0xD2, 0xCE, 0xC2), (0xB4, 0xAF, 0xA2), \
        (0x76, 0x72, 0x68)
    rows = [(5, (7, 10)), (7, (5, 8, 11)), (9, (4, 7, 10, 13)), (11, (2, 5, 8, 11, 14)), (13, (4, 7, 10, 13))]
    rng = random.Random(4772)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    beads = {}
    for cy, xs in rows:
        for cx in xs:
            if cy == 13 and rng.random() < 0.3:
                continue
            for dx, dy in ((0, 0), (-1, 0), (1, 0), (0, -1), (0, 1)):
                beads[cx + dx, cy + dy] = (cx, cy, dx - dy)
    for (x, y), (cx, cy, t) in beads.items():
        # a bead's top and left catch the light, its right and bottom are in shade; now and then one glints
        col = light if t < 0 else base if t == 0 else shade
        if t == 0 and rng.random() < 0.2:
            col = light
        img.putpixel((x, y), col + (255,))
    for y in range(16):
        for x in range(16):
            if (x, y) in beads:
                continue
            near = {beads[n][:2] for n in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)) if n in beads}
            if len(near) > 1:
                img.putpixel((x, y), gap + (255,))
    outline(img, edge)
    return img


def sodium_nitrite():
    """
    A little heap of sodium nitrite crystals, white to pale yellow: each a short prism seen from above and in front,
    its top face lit, its left face in light shade and its right in deeper, outlined.
    """
    white = [(0xFF, 0xFF, 0xFA), (0xEE, 0xEC, 0xDC), (0xD2, 0xCE, 0xB4)]
    yellow = [(0xFC, 0xF6, 0xD0), (0xEE, 0xE0, 0xA0), (0xD0, 0xBC, 0x70)]
    edge = (0x7C, 0x70, 0x48)

    def crystal(img, cx, cy, w, rng):
        tint = rng.uniform(0.0, 0.8)
        pal = [mix(a, b, tint) for a, b in zip(white, yellow)]
        h = w * rng.uniform(0.7, 1.1)
        for y in range(16):
            for x in range(16):
                dx, dy = x + 0.5 - cx, y + 0.5 - (cy - h / 2)
                if abs(dx) > w:
                    continue
                slope = (w - abs(dx)) / 2
                if -slope <= dy <= slope:
                    col = pal[0]  # the top, a flattened lozenge
                elif slope < dy <= h + slope:
                    col = pal[1] if dx < 0 else pal[2]
                else:
                    continue
                img.putpixel((x, y), col + (255,))
        outline(img, edge)

    return heap(4773, [(5.5, 6, 2.5), (10.5, 5.5, 2.5), (3.5, 10, 2.5), (8, 9.5, 3), (12.5, 10, 2.5),
                       (6, 12.5, 2.5), (10.5, 12.5, 2.5)], crystal)


def items():
    textures = {'platinum_nugget': G.create_item_texture('nugget', PLATINUM_SHADES),
                'platinum_gauze': platinum_gauze(),
                'iron_catalyst': iron_catalyst(),
                'urea': urea(),
                'sodium_nitrite': sodium_nitrite()}
    for name, img in textures.items():
        G.save_png(img, G.asset('textures', 'item', name + '.png'))
        item_model(name, {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'mcremix:item/{name}'}})

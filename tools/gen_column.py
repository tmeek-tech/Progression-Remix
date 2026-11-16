"""
Resources for the distillation build's second stage (registry/ModColumn.java): the steel fractionating column's
sections, the steel condenser and its vapour line, the column's warm cuts' tags, and their Ponder structures. gen_assets.py
calls run() after gen_decor, passing itself, and this file uses gen_decor's steel palettes and model helpers, adding to
the lang and tag files written before it rather than owning any shared file.
"""
import random

from PIL import Image

import gen_decor as D

G = None  # gen_assets, set by run()

# fresh steel and its one stage of rust, as gen_decor draws them; the prefix each look's textures and models take
LOOKS = (('', D.STEEL, 0.0), ('exposed_', D.EXPOSED, 0.05))
STAGES = D.STAGES
PATINAS = ['none', 'exposed', 'weathered', 'oxidized', 'waxed', 'waxed_exposed', 'waxed_weathered', 'waxed_oxidized']
BLOCKS = ['steel_column_section', 'steel_condenser']


def run(gen):
    global G
    G = gen
    textures()
    column_section()
    condenser()
    vapour_line()
    recipes()
    loot_and_tags()
    ponder_schematics()
    lang()


def look_of(patina):
    """The texture prefix a patina is drawn with: steel shows only its first stage of rust."""
    return '' if patina in ('none', 'waxed') else 'exposed_'


# ------------------------------------------------------------------------------------------------ textures

def plate(seed, pal):
    """Plain steel plate with a faint mottle."""
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = D.shade(pal, 3 + (rng.random() < 0.25) - (rng.random() < 0.2))
    return img


def rivet(px, pal, x, y):
    """A rivet head: lit on its upper left, shadowed under."""
    px[x, y] = D.shade(pal, 6)
    if x + 1 < 16:
        px[x + 1, y] = D.shade(pal, 4)
    if y + 1 < 16:
        px[x, y + 1] = D.shade(pal, 1)


def ct_sheet(tile):
    """
    A connected texture sheet as Create's RECTANGLE type reads it (client/ModSpriteShifts): four by four tiles, each
    drawn by tile(left, right, up, down), whether the face joins one of its own column on that side. Across, the tiles
    are a lone block, the left end of a row (joined on its right), the middle and the right end; down, the top of a
    column (joined below), the middle, the bottom and a lone block. The block's own texture is the lone block's tile.
    """
    sheet = Image.new('RGBA', (64, 64))
    for col in range(4):
        for row in range(4):
            sheet.paste(tile(col in (2, 3), col in (1, 2), row in (1, 2), row in (0, 1)), (col * 16, row * 16))
    return sheet


def ring(start_open, end_open):
    """
    Where rivets go along an edge two pixels in: every four pixels, so a row of them runs on into the next block of the
    column, turning the corner two pixels in from an end the column stops at.
    """
    return [2, 6, 10, 14 if end_open else 13]


def section_side(seed, pal, rust, left=False, right=False, up=False, down=False):
    """
    A column section's side, as the connected texture draws it on a formed column: plate, a seam under the flange
    round its top and over the one round its bottom, a row of rivets along each, running on across the column. The
    column's top section is capped, a riveted band lit along its top under its top flange, and its bottom section stands
    on a darker skirt; where the column ends at either side, the seam where its plates turn the corner. A lone section
    is all of these.
    """
    img = plate(seed, pal)
    px = img.load()
    if up:
        for x in range(16):
            px[x, 2] = D.shade(pal, 2)
        for x in range(1, 16, 4):
            rivet(px, pal, x, 3)
    else:
        for x in range(16):
            px[x, 2] = D.shade(pal, 5)
            px[x, 3] = D.shade(pal, 4)
            px[x, 4] = D.shade(pal, 1)
        for x in range(1, 16, 4):
            px[x, 3] = D.shade(pal, 6)
    if down:
        for x in range(16):
            px[x, 13] = D.shade(pal, 4)
        for x in range(1, 16, 4):
            rivet(px, pal, x, 11)
    else:
        for x in range(16):
            px[x, 11] = D.shade(pal, 1)
            px[x, 12] = D.shade(pal, 2)
            px[x, 13] = D.shade(pal, 2)
        for x in range(1, 16, 4):
            px[x, 12] = D.shade(pal, 4)
    if not left:
        for y in range(16):
            px[0, y] = D.shade(pal, 1)
        for y in (6, 9):
            rivet(px, pal, 2, y)
    if not right:
        for y in range(16):
            px[15, y] = D.shade(pal, 4)
        for y in (6, 9):
            rivet(px, pal, 12, y)
    if rust:
        D.rusty(img, random.Random(seed + 1), rust)
    return img


def section_top(seed, pal, rust, left=False, right=False, up=False, down=False):
    """
    A section's top or bottom, as the connected texture draws it across a formed column: one plate, edged where the
    column ends, lit along its top and left and shaded along its bottom and right, with a ring of rivets two pixels in
    from those edges.
    """
    img = plate(seed, pal)
    px = img.load()
    for i in ring(left, right):
        if not up:
            rivet(px, pal, i, 2)
        if not down:
            rivet(px, pal, i, 12)
    for i in ring(up, down):
        if not left:
            rivet(px, pal, 2, i)
        if not right:
            rivet(px, pal, 12, i)
    for i in range(16):
        if not up:
            px[i, 0] = D.shade(pal, 2)
        if not left:
            px[0, i] = D.shade(pal, 2)
        if not down:
            px[i, 15] = D.shade(pal, 4)
        if not right:
            px[15, i] = D.shade(pal, 4)
    if rust:
        D.rusty(img, random.Random(seed + 1), rust)
    return img


def flange(seed, pal, rust):
    """
    Flange stock: a bolt row every four pixels down it, lit along its top edge and shaded along its bottom, so any strip
    of it a flange's narrow faces show reads as a bolted ring.
    """
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        band = y % 4
        for x in range(16):
            px[x, y] = D.shade(pal, (5, 4, 4, 2)[band] + (rng.random() < 0.15) - (rng.random() < 0.1))
    for y in range(1, 16, 4):
        for x in range(1, 16, 4):
            px[x, y] = D.shade(pal, 6)
            px[x + 1, y] = D.shade(pal, 3)
    if rust:
        D.rusty(img, rng, rust)
    return img


def nozzle_face(seed, pal, rust):
    """
    A flanged nozzle seen end on: a bolted flange round a dark bore. The model shows pixels 1 to 15 of it; a Create
    pipe joining it covers 3 to 13 with its rim, so the bolts sit outside that, on the ring left showing.
    """
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 3.2:
                px[x, y] = D.shade(pal, 0)
            elif d < 4.4:
                px[x, y] = D.shade(pal, 2)
            else:
                px[x, y] = D.shade(pal, 4 + (rng.random() < 0.2) - (rng.random() < 0.2))
    for x, y in ((1, 1), (7, 1), (13, 1), (1, 7), (13, 7), (1, 13), (7, 13), (13, 13)):
        rivet(px, pal, x, y)
    if rust:
        D.rusty(img, rng, rust)
    return img


# a square shell or drum shaded round, as Create shades its pipes and tanks: the shade of each of the twelve pixels
# across a face, lit a quarter of the way in from one edge and darkening to the other
ROUND = (1, 3, 5, 6, 6, 5, 4, 4, 3, 3, 2, 1)


def shell(seed, pal, rust):
    """
    The condenser's shell, its axis running across the texture: a square shell shaded round across its twelve pixels
    (rows 2 to 13), with a girth seam a quarter of the way in from each end where one ring of plate meets the next.
    """
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        i = ROUND[min(11, max(0, y - 2))]
        for x in range(16):
            px[x, y] = D.shade(pal, i + (rng.random() < 0.05) - (rng.random() < 0.05))
        for x in (4, 11):
            px[x, y] = D.shade(pal, i - 2)
            px[x + 1, y] = D.shade(pal, i + 1)
    if rust:
        D.rusty(img, random.Random(seed + 1), rust)
    return img


def head(seed, pal, rust):
    """A condenser's end: a dished head bolted to its flange, the outlet in the middle."""
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if r < 2.2:
                px[x, y] = D.shade(pal, 0)
            elif r < 3.4:
                px[x, y] = D.shade(pal, 5)
            elif d > 6:
                px[x, y] = D.shade(pal, 2 if d > 6.6 else 4)
            else:
                # the dish, lit towards its upper left
                px[x, y] = D.shade(pal, 3 + (x + y < 13) + (rng.random() < 0.15) - (rng.random() < 0.15))
    for x, y in ((2, 2), (7, 1), (12, 2), (1, 7), (13, 7), (2, 12), (7, 13), (12, 12)):
        rivet(px, pal, x, y)
    if rust:
        D.rusty(img, rng, rust)
    return img


def pipe_side(seed, pal, rust):
    """The vapour line's side: a big steel pipe lit from one side, its middle eight pixels across."""
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    across = (2, 3, 5, 6, 6, 5, 4, 4, 3, 3, 2, 2, 1, 1, 2, 3)
    for y in range(16):
        for x in range(16):
            px[x, y] = D.shade(pal, across[x] + (rng.random() < 0.1) - (rng.random() < 0.1))
    if rust:
        D.rusty(img, rng, rust * 1.5)
    return img


def pipe_end(seed, pal, rust):
    """The vapour line's end: a ring of pipe wall round its dark bore."""
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            r = max(abs(x - 7.5), abs(y - 7.5))
            px[x, y] = D.shade(pal, 0 if r < 5 else 5 if r < 6.5 else 3)
    if rust:
        D.rusty(img, rng, rust)
    return img


def textures():
    for prefix, pal, rust in LOOKS:
        side = lambda l, r, u, d: section_side(4501, pal, rust, l, r, u, d)
        top = lambda l, r, u, d: section_top(4502, pal, rust, l, r, u, d)
        D.png(side(False, False, False, False), f'{prefix}steel_column_section')
        D.png(ct_sheet(side), f'{prefix}steel_column_section_connected')
        D.png(top(False, False, False, False), f'{prefix}steel_column_section_top')
        D.png(ct_sheet(top), f'{prefix}steel_column_section_top_connected')
        D.png(flange(4503, pal, rust), f'{prefix}steel_column_flange')
        D.png(nozzle_face(4504, pal, rust), f'{prefix}steel_column_nozzle')
        D.png(shell(4505, pal, rust), f'{prefix}steel_condenser_shell')
        D.png(head(4506, pal, rust), f'{prefix}steel_condenser_head')
        D.png(pipe_side(4507, pal, rust), f'{prefix}steel_vapour_line')
        D.png(pipe_end(4508, pal, rust), f'{prefix}steel_vapour_line_end')


# ------------------------------------------------------------------------------------------------ models

def lip(side, y0, y1):
    """
    Half a pixel of flange standing out of a section's {@code side}, from y0 to y1. It is culled with that side, so a
    lip shows only where the column has no section beside it: on its outside.
    """
    t = 0.5
    box = {'north': ((0, y0, -t), (16, y1, 0)), 'south': ((0, y0, 16), (16, y1, 16 + t)),
           'west': ((-t, y0, 0), (0, y1, 16)), 'east': ((16, y0, 0), (16 + t, y1, 16))}[side]
    strip = [0.5, 4, 15.5, 4 + (y1 - y0)]
    faces = {}
    for d in ('north', 'south', 'east', 'west', 'up', 'down'):
        if d == {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east'}[side]:
            continue
        if d in ('up', 'down'):
            uv = [0.5, 4, 15.5, 4.5]
        elif d == side:
            uv = strip
        else:
            uv = [4, 4, 4.5, 4 + (y1 - y0)]
        faces[d] = D.face('#flange', uv, cull=side)
    return D.el(*box, faces)


def column_section():
    for prefix, _, _ in LOOKS:
        tex = {'side': f'mcremix:block/{prefix}steel_column_section',
               'top': f'mcremix:block/{prefix}steel_column_section_top',
               'flange': f'mcremix:block/{prefix}steel_column_flange',
               'particle': f'mcremix:block/{prefix}steel_column_section'}
        cube = D.el((0, 0, 0), (16, 16, 16), {
            d: D.face('#top' if d in ('up', 'down') else '#side', [0, 0, 16, 16], cull=d)
            for d in ('north', 'east', 'south', 'west', 'up', 'down')})
        elements = [cube]
        for side in ('north', 'east', 'south', 'west'):
            elements.append(lip(side, 0, 1.5))
            elements.append(lip(side, 14.5, 16))
        D.model(f'{prefix}steel_column_section', {'parent': 'minecraft:block/block', 'textures': tex,
                                                 'elements': elements})
        # a nozzle on the north side, turned to the others by the blockstate: wider and deeper than the rim a Create
        # pipe draws where it joins (3 to 13 across, 2 deep), so its flange shows round it
        D.model(f'{prefix}steel_column_nozzle', {
            'textures': {'nozzle': f'mcremix:block/{prefix}steel_column_nozzle',
                         'flange': f'mcremix:block/{prefix}steel_column_flange',
                         'particle': f'mcremix:block/{prefix}steel_column_section'},
            'elements': [D.el((1, 1, -2), (15, 15, 0), {
                'north': D.face('#nozzle', [1, 1, 15, 15]),
                'up': D.face('#flange', [1, 4, 15, 6]), 'down': D.face('#flange', [1, 4, 15, 6]),
                'east': D.face('#flange', [4, 1, 6, 15]), 'west': D.face('#flange', [4, 1, 6, 15])})]})
    parts = []
    turn = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    for stage in STAGES:
        prefix = stage[0]
        when = D.patina_when(stage)
        parts.append({'when': when, 'apply': {'model': f'mcremix:block/{prefix}steel_column_section'}})
        for side, y in turn.items():
            apply = {'model': f'mcremix:block/{prefix}steel_column_nozzle'}
            if y:
                apply['y'] = y
            parts.append({'when': {**when, side: 'true'}, 'apply': apply})
    D.states('steel_column_section', {'multipart': parts})
    D.item_model('steel_column_section', {'parent': 'mcremix:block/steel_column_section'})


def condenser():
    """
    Lying along z, turned by its blockstate to lie along x: a flanged head at each end, the shell between them one
    square box shaded round (Taran, 0.36.0: no stepped edges), and a short water nozzle out of its top, bottom and two
    sides, from the shell to the block's face.
    """
    for prefix, _, _ in LOOKS:
        tex = {'shell': f'mcremix:block/{prefix}steel_condenser_shell',
               'head': f'mcremix:block/{prefix}steel_condenser_head',
               'flange': f'mcremix:block/{prefix}steel_column_flange',
               'neck': f'mcremix:block/{prefix}steel_vapour_line_end',
               'particle': f'mcremix:block/{prefix}steel_condenser_shell'}
        elements = []
        for z0, z1, end in ((0, 2, 'north'), (14, 16, 'south')):
            faces = {end: D.face('#head', [1, 1, 15, 15], cull=end)}
            for d in ('east', 'west'):
                faces[d] = D.face('#flange', [z0, 1, z1, 15], rotation=90)
            for d in ('up', 'down'):
                faces[d] = D.face('#flange', [1, z0, 15, z1])
            elements.append(D.el((1, 1, z0), (15, 15, z1), faces))
        faces = {d: D.face('#shell', [2, 2, 14, 14]) for d in ('east', 'west')}
        faces.update({d: D.face('#shell', [2, 2, 14, 14], rotation=90) for d in ('up', 'down')})
        elements.append(D.el((2, 2, 2), (14, 14, 14), faces))
        for frm, to, out in (((6, 14, 6), (10, 16, 10), 'up'), ((6, 0, 6), (10, 2, 10), 'down'),
                             ((14, 6, 6), (16, 10, 10), 'east'), ((0, 6, 6), (2, 10, 10), 'west')):
            faces = {out: D.face('#neck', [0, 0, 16, 16])}
            for d in ('north', 'south', 'east', 'west', 'up', 'down'):
                if d != out and d != {'up': 'down', 'down': 'up', 'east': 'west', 'west': 'east'}[out]:
                    faces[d] = D.face('#flange', [6, 4, 10, 6])
            elements.append(D.el(frm, to, faces))
        D.model(f'{prefix}steel_condenser', {'parent': 'minecraft:block/block', 'textures': tex,
                                            'elements': elements})
    variants = {}
    for patina in PATINAS:
        for axis, y in (('z', 0), ('x', 90)):
            v = {'model': f'mcremix:block/{look_of(patina)}steel_condenser'}
            if y:
                v['y'] = y
            variants[f'axis={axis},patina={patina}'] = v
    D.states('steel_condenser', {'variants': variants})
    D.item_model('steel_condenser', {'parent': 'mcremix:block/steel_condenser'})


def vapour_line():
    """
    The partials the condenser draws its vapour line with (client/SteelCondenserRenderer): a length of eight pixel pipe
    rising through a block with a flange at its foot, and an elbow coming up from below and turning out of the block's
    north side, flanged at both ends.
    """
    for prefix, _, _ in LOOKS:
        tex = {'pipe': f'mcremix:block/{prefix}steel_vapour_line',
               'end': f'mcremix:block/{prefix}steel_vapour_line_end',
               'flange': f'mcremix:block/{prefix}steel_column_flange',
               'particle': f'mcremix:block/{prefix}steel_vapour_line'}
        sides = lambda y0, y1: {d: D.face('#pipe', [4, 16 - y1, 12, 16 - y0]) for d in ('north', 'south', 'east',
                                                                                        'west')}
        foot = D.el((3, 0, 3), (13, 1, 13), {
            **{d: D.face('#flange', [3, 4, 13, 5]) for d in ('north', 'south', 'east', 'west')},
            'up': D.face('#flange', [3, 3, 13, 13])})
        straight = D.el((4, 0, 4), (12, 16, 12), {**sides(0, 16), 'up': D.face('#end', [4, 4, 12, 12]),
                                                   'down': D.face('#end', [4, 4, 12, 12])})
        D.model(f'steel_condenser/{prefix}vapour_line', {'textures': tex, 'elements': [straight, foot]})
        rising = D.el((4, 0, 4), (12, 12, 12), {**sides(0, 12), 'up': D.face('#pipe', [4, 4, 12, 12]),
                                                 'down': D.face('#end', [4, 4, 12, 12])})
        out = D.el((4, 4, 0), (12, 12, 4), {
            'north': D.face('#end', [4, 4, 12, 12]),
            'east': D.face('#pipe', [12, 4, 16, 12], rotation=90), 'west': D.face('#pipe', [0, 4, 4, 12], rotation=90),
            'up': D.face('#pipe', [4, 0, 12, 4]), 'down': D.face('#pipe', [4, 12, 12, 16])})
        end_flange = D.el((3, 3, 0), (13, 13, 1), {
            **{d: D.face('#flange', [3, 4, 13, 5]) for d in ('east', 'west', 'up', 'down')},
            'south': D.face('#flange', [3, 3, 13, 13])})
        D.model(f'steel_condenser/{prefix}vapour_elbow', {'textures': tex,
                                                          'elements': [rising, out, foot, end_flange]})


# ------------------------------------------------------------------------------------------------ recipes

def recipes():
    sheet = {'tag': 'forge:plates/steel'}
    # four steel sheets round a steel fluid tank make two sections
    G.recipe('steel_column_section', {
        'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': [' S ', 'STS', ' S '],
        'key': {'S': sheet, 'T': G.item('mcremix:steel_fluid_tank')},
        'result': G.item('mcremix:steel_column_section', 2)})
    # a steel shell round copper tubes, which carry the heat away
    G.recipe('steel_condenser', {
        'type': 'minecraft:crafting_shapeless', 'category': 'building',
        'ingredients': [G.item('mcremix:steel_fluid_tank'), G.item('mcremix:steel_fluid_pipe'),
                        G.item('mcremix:steel_fluid_pipe')] + [{'tag': 'forge:plates/copper'}] * 4,
        'result': G.item('mcremix:steel_condenser')})


def loot_and_tags():
    for name in BLOCKS:
        G.write_json(G.data('mcremix', 'loot_tables', 'blocks', name + '.json'), G.loot_self(name))
    m = lambda names: [f'mcremix:{n}' for n in names]
    D.add_tag('minecraft', 'blocks', 'mineable/pickaxe', m(BLOCKS))
    # steel, as its pipes and tanks: nothing it holds is too hot for it
    D.add_tag('mcremix', 'blocks', 'fluid_ceiling/searing', m(BLOCKS))
    # a warm cut does what its plain form does: hot creosote soaks timber, hot vinegar cleans patina
    D.add_tag('mcremix', 'fluids', 'creosote', m(['creosote', 'hot_creosote']))
    D.add_tag('mcremix', 'fluids', 'vinegar', m(['vinegar', 'hot_vinegar']))


# ------------------------------------------------------------------------------------------------ structures

def block_pos(pos):
    x, y, z = pos
    return ('compound', {'X': ('int', x), 'Y': ('int', y), 'Z': ('int', z)})


def section(origin, width, height, pos, nozzles=(), condenser=None):
    """
    A column section at pos, already joined as the column would join it: the controller (its bottom north-west block)
    with the column's size, the rest pointing to it. Ponder's world forms no columns of its own.
    """
    props = {'north': 'false', 'east': 'false', 'south': 'false', 'west': 'false', 'patina': 'none'}
    for side in nozzles:
        props[side] = 'true'
    if pos == origin:
        nbt = {'id': ('string', 'mcremix:steel_column_section'), 'Size': ('int', width), 'Height': ('int', height)}
        if condenser:
            nbt['Column'] = ('compound', {'Condenser': block_pos(condenser)})
    else:
        nbt = {'id': ('string', 'mcremix:steel_column_section'), 'Controller': block_pos(origin)}
    return ('mcremix:steel_column_section', props, nbt)


def column(b, origin, width, height, nozzles=None, condenser=None):
    """A column already joined, its condenser at {@code condenser} if it has one, so the vapour line is drawn."""
    nozzles = nozzles or {}
    ox, oy, oz = origin
    for y in range(height):
        for x in range(width):
            for z in range(width):
                pos = (ox + x, oy + y, oz + z)
                b[pos] = section(origin, width, height, pos, nozzles.get(pos, ()), condenser)


def pipe(*sides):
    return ('create:fluid_pipe', dict({d: 'true' if d in sides else 'false'
                                       for d in ('north', 'south', 'east', 'west', 'up', 'down')}, waterlogged='false'))


def floor(b, size, heater_at, heater):
    G.plate(b, size)
    b[heater_at] = heater


def ponder_schematics():
    """
    steel_column/intro: a 7x7 plate with a kindled Blaze Burner sunk into its middle, a column of four sections on it
    for coal tar, its condenser to the east lying north-south, the feed pipe on the bottom section's north side, a pipe
    on the west side of each of the three lower sections, the top cut's pipe off the condenser's south end and water
    into its east side. steel_column/reflux: a campfire sunk in the plate, a column three high for wood spirit, feed in
    the north side of its bottom, methanol drawn west from its middle, the top cut out of the condenser's south end, and
    from its north end a pump up and back into the top section's north side. steel_condenser/inline: a 5x5 plate, a
    condenser lying east-west in a line of pipe, with water piped in and out of its sides.
    """
    b = {}
    floor(b, 7, (3, 0, 3), ('create:blaze_burner', {'blaze': 'kindled', 'facing': 'north'}))
    column(b, (3, 1, 3), 1, 4, {(3, 1, 3): ('north', 'west'), (3, 2, 3): ('west',), (3, 3, 3): ('west',)},
           condenser=(4, 1, 3))
    b[(4, 1, 3)] = ('mcremix:steel_condenser', {'axis': 'z', 'patina': 'none'})
    b[(3, 1, 2)] = pipe('north', 'south')
    for y in (1, 2, 3):
        b[(2, y, 3)] = pipe('east', 'west')
    b[(4, 1, 4)] = pipe('north', 'south')
    b[(5, 1, 3)] = pipe('east', 'west')
    G.save_structure('steel_column/intro', (7, 7, 7), b)

    b = {}
    floor(b, 7, (3, 0, 3), ('minecraft:campfire', {'facing': 'north', 'lit': 'true', 'signal_fire': 'false',
                                                   'waterlogged': 'false'}))
    column(b, (3, 1, 3), 1, 3, {(3, 1, 3): ('north',), (3, 2, 3): ('west',), (3, 3, 3): ('north',)},
           condenser=(4, 1, 3))
    b[(4, 1, 3)] = ('mcremix:steel_condenser', {'axis': 'z', 'patina': 'none'})
    b[(3, 1, 2)] = pipe('north', 'south')
    b[(2, 2, 3)] = pipe('east', 'west')
    b[(4, 1, 4)] = pipe('north', 'south')
    b[(5, 1, 3)] = pipe('east', 'west')
    b[(4, 1, 2)] = pipe('south', 'up')
    b[(4, 2, 2)] = ('create:mechanical_pump', {'facing': 'up', 'waterlogged': 'false'})
    b[(4, 3, 2)] = pipe('down', 'west')
    b[(3, 3, 2)] = pipe('east', 'south')
    G.save_structure('steel_column/reflux', (7, 6, 7), b)

    b = {}
    G.plate(b)
    b[(2, 1, 2)] = ('mcremix:steel_condenser', {'axis': 'x', 'patina': 'none'})
    for x in (0, 1, 3, 4):
        b[(x, 1, 2)] = pipe('east', 'west')
    b[(2, 1, 1)] = pipe('north', 'south')
    b[(2, 1, 3)] = pipe('north', 'south')
    G.save_structure('steel_condenser/inline', (5, 3, 5), b)


# ------------------------------------------------------------------------------------------------ lang

def lang():
    rusts = ' Like all steel it _rusts_ a little where it gets wet; _honeycomb_ waxes it.'
    text = {
        'block.mcremix.steel_column_section': 'Steel Column Section',
        'block.mcremix.steel_column_section.tooltip.summary': (
            'A riveted, flanged section of a _fractionating column_. Stack them _up to sixteen high_ and _one to three '
            'across_ over _heat_, with a _steel condenser_ beside the bottom one. Pipe _feed_ into the bottom section: '
            'every cut comes out at once, each from _its own section_, the lightest over the top to the condenser and '
            'the residue from the bottom. Cuts leave _as hot_ as their section.' + rusts),
        'block.mcremix.steel_condenser': 'Steel Condenser',
        'block.mcremix.steel_condenser.tooltip.summary': (
            'Copper tubes in a steel shell, cooled by _water_, _cold water_ or _coolant_ piped into its top, bottom or '
            'sides, which come out warm. Beside a _steel column\'s_ bottom section it condenses the column\'s _top '
            'cut_, which comes out of its _ends_. Anywhere else it cools a _hot cut_ piped into one end into its plain '
            'form out of the other.' + rusts),
        'entity.mcremix.solvent_vapour': 'Solvent Vapour',
        # the column's goggles
        'mcremix.goggles.steel_column.size': '%s sections high, %s by %s',
        'mcremix.goggles.steel_column.idle': 'Idle: pipe feed into the bottom section',
        'mcremix.goggles.steel_column.wont_distil': '%s won\'t split',
        'mcremix.goggles.steel_column.no_condenser': 'Needs a steel condenser beside its bottom section',
        'mcremix.goggles.steel_column.too_cold': 'Too cold: needs %s heat under it',
        'mcremix.goggles.steel_column.held_up_condenser': 'Held up: its condenser is full',
        'mcremix.goggles.steel_column.held_up': 'Held up: section %s is full, draw its cut off',
        'mcremix.goggles.steel_column.running': 'Splitting %s',
        'mcremix.goggles.steel_column.heat': '%s of %s blocks under it %s or hotter, pace %s',
        'mcremix.goggles.steel_column.reflux': 'Reflux ratio %s',
        'mcremix.goggles.steel_column.needs_or_reflux': '%s sections short: build it taller, or send back reflux',
        'mcremix.goggles.steel_column.needs': '%s sections short: build it taller',
        'mcremix.goggles.steel_column.venting': '%s mB a second escaping over the top',
        'mcremix.goggles.steel_column.sections': 'From the top:',
        'mcremix.goggles.steel_column.top': '%s, over to the condenser',
        'mcremix.goggles.steel_column.separating': 'separating',
        'mcremix.goggles.steel_column.holding': ': %s %s / %s mB',
        'mcremix.goggles.steel_column.feed': 'Feed: %s %s / %s mB',
        'mcremix.goggles.steel_column.reflux_held': 'Reflux: %s %s / %s mB',
        'mcremix.goggles.steel_column.no_room': 'No section for %s: it escapes over the top',
        'mcremix.goggles.steel_column.empty': 'nothing',
        # the condenser's
        'mcremix.goggles.steel_condenser.column': 'Condensing for the column beside it',
        'mcremix.goggles.steel_condenser.inline': 'Inline: cools a hot cut piped into an end',
        'mcremix.goggles.steel_condenser.dry': 'Nothing to cool with',
        'mcremix.goggles.steel_condenser.warm_full': 'Its warm water has nowhere to go',
        'mcremix.goggles.steel_condenser.full': 'Full: draw off what it made',
        'mcremix.goggles.steel_condenser.working': 'Working',
        'mcremix.goggles.steel_condenser.ready': 'Ready',
        'mcremix.goggles.steel_condenser.rate': 'Up to %s mB a tick',
        'mcremix.goggles.steel_condenser.venting': '%s mB a second escaping',
        'mcremix.goggles.steel_condenser.holding': '%s %s / %s mB',
        # JEI
        'mcremix.jei.distilling.column': 'In a steel column: %s sections, every cut at once',
        'mcremix.jei.distilling.column_reflux': 'Or %s with a reflux ratio of 1, at half the pace',
        'mcremix.jei.distilling.close_column': 'In a steel column: %s more sections between them, or reflux',
        'mcremix.jei.steel_condenser_cooling': 'Cooling Hot Cuts',
        'mcremix.jei.steel_condenser_cooling.rate': '%s mB/t',
    }
    D.merge_json(G.asset('lang', 'en_us.json'), lambda obj: obj.update(text))

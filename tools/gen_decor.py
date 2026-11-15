"""
Resources for the distillation build's sixth stage (registry/ModDecor.java): steel catwalks, catwalk stairs, railings
and caged ladders, creosote timber and argon tubes. gen_assets.py calls run() last, passing itself, so this file uses
its helpers and adds to the lang and tag files it wrote rather than owning any shared file. Kept apart so the other
build threads writing to gen_assets.py at the same time do not collide with this one.
"""
import json
import math
import os
import random

from PIL import Image

G = None  # gen_assets, set by run()

# steel as the casings and frames draw it, darkest first; exposed steel warmer, with rust
STEEL = [(31, 31, 35), (50, 50, 55), (62, 62, 66), (78, 78, 81), (93, 92, 95), (107, 105, 107), (120, 118, 120)]
EXPOSED = [(36, 32, 31), (56, 51, 49), (68, 61, 59), (82, 74, 70), (97, 89, 84), (113, 104, 99), (124, 114, 108)]
RUST = [(92, 52, 34), (118, 66, 40), (133, 87, 63), (150, 96, 60)]
# steel rusts to its first stage only (Weathering.lastStage), so these are its only two looks
STAGES = (('', 'none', 'waxed'), ('exposed_', 'exposed|weathered|oxidized', 'waxed_exposed|waxed_weathered|waxed_oxidized'))
STEEL_BLOCKS = ['steel_catwalk', 'steel_catwalk_stairs', 'steel_railing', 'steel_caged_ladder']
CREOSOTE_BLOCKS = ['creosote_log', 'creosote_planks', 'creosote_stairs', 'creosote_slab', 'creosote_fence',
                   'creosote_fence_gate', 'creosote_post']
# The fluids that make creosote timber and argon tubes, which later stages of the distillation build add: creosote from
# coal tar in the glass lab (stage 1) and argon from the air column (stage 4). Until then the blocks are creative only
# (Taran's card, 2026-11-15: no stand-in recipes), so these are None and their Spout recipes, and the empty glass tube's
# own, are left out. The stage that adds each fluid sets its id here, and the recipes below appear.
CREOSOTE_FLUID = None
ARGON_FLUID = None


def run(gen):
    global G
    G = gen
    textures()
    catwalk()
    catwalk_stairs()
    railing()
    caged_ladder()
    creosote()
    argon_tube()
    recipes()
    loot_and_tags()
    ponder_schematics()
    lang()


def png(img, name):
    G.save_png(img, G.asset('textures', 'block', name + '.png'))


def model(name, obj):
    G.write_json(G.asset('models', 'block', name + '.json'), obj)


def item_model(name, obj):
    G.write_json(G.asset('models', 'item', name + '.json'), obj)


def states(name, obj):
    G.write_json(G.asset('blockstates', name + '.json'), obj)


def merge_json(path, update):
    """Adds to a file gen_assets.py wrote, or writes it if it did not."""
    obj = json.load(open(path)) if os.path.exists(path) else {}
    update(obj)
    G.write_json(path, obj)


def add_tag(ns, kind, name, values):
    path = G.data(ns, 'tags', kind, *name.split('/')) + '.json'

    def add(obj):
        obj.setdefault('replace', False)
        obj['values'] = obj.get('values', []) + [v for v in values if v not in obj.get('values', [])]
    merge_json(path, add)


def face(tex, uv=None, cull=None, tint=None, rotation=None, light=False):
    f = {'texture': tex}
    if uv:
        f['uv'] = uv
    if cull:
        f['cullface'] = cull
    if rotation:
        f['rotation'] = rotation
    return f


def el(frm, to, faces, rotation=None, light=False, shade=True):
    e = {'from': list(frm), 'to': list(to), 'faces': faces}
    if rotation:
        e['rotation'] = rotation
    if light:
        e['forge_data'] = {'block_light': 15, 'sky_light': 15}
    if not shade:
        e['shade'] = False
    return e


def box_faces(tex, frm, to, skip=(), uvs=None):
    """
    Every face of a box, each mapped onto the texture where it lies, as Minecraft maps a box with no uv given. A box
    taller than it is wide is a post or strap: its sides take the texture turned a quarter, so the bar texture's light
    and shade run up it rather than across.
    """
    x0, y0, z0 = frm
    x1, y1, z1 = to
    auto = {'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0], 'south': [x0, 16 - y1, x1, 16 - y0],
            'west': [z0, 16 - y1, z1, 16 - y0], 'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0],
            'up': [x0, z0, x1, z1], 'down': [x0, 16 - z1, x1, 16 - z0]}
    upright = (y1 - y0) > 2 * max(x1 - x0, z1 - z0)
    out = {}
    for d, uv in auto.items():
        if d in skip:
            continue
        if upright and d not in ('up', 'down'):
            across = (x1 - x0) if d in ('north', 'south') else (z1 - z0)
            out[d] = face(tex, inset([0, 0, y1 - y0, across]), rotation=90)
            continue
        out[d] = face(tex, (uvs or {}).get(d, inset(uv)))
    return out


def inset(uv):
    """
    A uv kept half a pixel inside the texture: a thin or slanted face whose uv touches the sprite's edge picks up specks
    of whatever sprite lies next to it in the atlas.
    """
    return [min(15.5, max(0.5, v)) for v in uv]


# ------------------------------------------------------------------------------------------------ textures

def shade(pal, i):
    return pal[max(0, min(len(pal) - 1, i))] + (255,)


def rusty(img, rng, amount, mask=None):
    """Spots and runs of rust over a steel texture: exposed steel, flecked rather than covered."""
    w, h = img.size
    px = img.load()
    for _ in range(int(w * h * amount)):
        x, y = rng.randrange(w), rng.randrange(h)
        for k in range(rng.choice((1, 1, 2, 3))):
            yy = y + k  # rust runs down
            if yy >= h or px[x, yy][3] == 0:
                break
            if mask and not mask(x, yy):
                break
            px[x, yy] = RUST[rng.randrange(len(RUST))] + (255,)


def steel_plate(seed, pal, rivets=()):
    """Plain dark steel with a faint mottle, and rivets where asked."""
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = shade(pal, 3 + (rng.random() < 0.25) - (rng.random() < 0.2))
    for x, y in rivets:
        px[x, y] = shade(pal, 5)
        px[x + 1, y] = shade(pal, 2) if x + 1 < 16 else px[x + 1 - 16, y]
        px[x, y + 1] = shade(pal, 1)
    return img


def grating(seed, pal, rust=0.0):
    """
    Bar grating seen from above: bearing bars running north-south every other pixel, a cross rod every four, and slots
    through between them, a fine mesh as real grating is. Light catches the bar tops.
    """
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if y % 4 == 3:
                px[x, y] = shade(pal, 3 + (rng.random() < 0.3))
            elif x % 2 == 0:
                px[x, y] = shade(pal, 5 - (y % 4 == 0) + (rng.random() < 0.2))
    if rust:
        rusty(img, rng, rust)
    return img


def grating_side(seed, pal, rust=0.0):
    """The bar grating's edge: the ends of the bearing bars, with the cross rods' line through them."""
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if x % 2 == 0:
                px[x, y] = shade(pal, 4 - (y % 2) + (rng.random() < 0.2))
            elif y % 4 == 2:
                px[x, y] = shade(pal, 2)
    if rust:
        rusty(img, rng, rust)
    return img


def frame_band(seed, pal, rust=0.0):
    """The flat bar banding a catwalk's edge, and its stairs' stringers: plain steel with a bolt now and then."""
    img = steel_plate(seed, pal, rivets=[(3, 7), (11, 7)])
    px = img.load()
    for x in range(16):
        px[x, 0] = shade(pal, 5)
        px[x, 15] = shade(pal, 1)
    if rust:
        rusty(img, random.Random(seed + 1), rust)
    return img


def round_bar(seed, pal, rust=0.0):
    """Steel bar and tube, lit from above: rails, posts, ladder stiles, rungs and cage hoops all use it."""
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            band = (y % 4)
            px[x, y] = shade(pal, (5, 4, 3, 2)[band] + (rng.random() < 0.15) - (rng.random() < 0.1))
    if rust:
        rusty(img, rng, rust)
    return img


def textures():
    for prefix, _, _ in STAGES:
        pal = EXPOSED if prefix else STEEL
        r = 0.05 if prefix else 0
        png(grating(4401, pal, r), f'{prefix}steel_catwalk_grating')
        png(grating_side(4402, pal, r), f'{prefix}steel_catwalk_grating_side')
        png(frame_band(4403, pal, r), f'{prefix}steel_catwalk_frame')
        png(round_bar(4404, pal, r * 1.5), f'{prefix}steel_rail')
    creosote_textures()
    argon_textures()


# ------------------------------------------------------------------------------------------------ catwalk

def stage_textures(prefix, *names):
    tex = {n: f'mcremix:block/{prefix}steel_{n}' for n in names}
    tex['particle'] = f'mcremix:block/{prefix}steel_catwalk_frame'
    return tex


def patina_when(stage):
    _, fresh, waxed = stage
    return {'patina': f'{fresh}|{waxed}'}


def catwalk():
    """The grating 2 pixels deep in the top of the block, banded round its open edges by a 4 pixel flat bar."""
    for prefix, _, _ in STAGES:
        tex = stage_textures(prefix, 'catwalk_grating', 'catwalk_grating_side', 'catwalk_frame')
        model(f'{prefix}steel_catwalk_grating', {'parent': 'minecraft:block/block', 'textures': tex, 'elements': [
            el((0, 14, 0), (16, 16, 16), {
                'up': face('#catwalk_grating', [0, 0, 16, 16]), 'down': face('#catwalk_grating', [0, 0, 16, 16]),
                **{d: face('#catwalk_grating_side', [0, 1, 16, 3], cull=d) for d in ('north', 'south', 'east', 'west')}
            })]})
        # the north edge's band; the blockstate turns it for the others
        model(f'{prefix}steel_catwalk_edge', {'parent': 'minecraft:block/block', 'textures': tex, 'elements': [
            el((0, 12, 0), (16, 16, 1), {
                'north': face('#catwalk_frame', [0, 4, 16, 8], cull='north'), 'south': face('#catwalk_frame', [0, 4, 16, 8]),
                'up': face('#catwalk_frame', [0, 1, 16, 2]), 'down': face('#catwalk_frame', [0, 14, 16, 15]),
                'east': face('#catwalk_frame', [7, 4, 8, 8]), 'west': face('#catwalk_frame', [8, 4, 9, 8])})]})
    parts = []
    for stage in STAGES:
        prefix = stage[0]
        parts.append({'when': patina_when(stage), 'apply': {'model': f'mcremix:block/{prefix}steel_catwalk_grating'}})
        for side, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            apply = {'model': f'mcremix:block/{prefix}steel_catwalk_edge'}
            if y:
                apply['y'] = y
            parts.append({'when': {**patina_when(stage), side: 'false'}, 'apply': apply})
    states('steel_catwalk', {'multipart': parts})
    model('steel_catwalk_inventory', {'parent': 'minecraft:block/block',
                                      'textures': stage_textures('', 'catwalk_grating', 'catwalk_grating_side',
                                                                 'catwalk_frame'),
                                      'elements': [
        el((0, 6, 0), (16, 8, 16), {'up': face('#catwalk_grating'), 'down': face('#catwalk_grating'),
                                    **{d: face('#catwalk_grating_side', [0, 1, 16, 3])
                                       for d in ('north', 'south', 'east', 'west')}}),
        *[el(a, b, box_faces('#catwalk_frame', a, b)) for a, b in (
            ((0, 4, 0), (16, 8, 1)), ((0, 4, 15), (16, 8, 16)), ((0, 4, 1), (1, 8, 15)), ((15, 4, 1), (16, 8, 15)))]]})
    item_model('steel_catwalk', {'parent': 'mcremix:block/steel_catwalk_inventory'})


# ------------------------------------------------------------------------------------------------ catwalk stairs

def sloped(x0, x1, y_mid, z_mid, thick, length):
    """A bar of square section lying at 45 degrees up toward the north, centred on (y_mid, z_mid)."""
    return (x0, y_mid - thick / 2, z_mid - length / 2), (x1, y_mid + thick / 2, z_mid + length / 2), \
        {'angle': 45, 'axis': 'x', 'origin': [8, y_mid, z_mid]}


def stairs_rail_elements(x0, x1):
    """One side's handrail, drawn on the west side as you climb north: two posts, the sloped rail and a knee rail."""
    t = '#rail'
    out = []
    for a, b in (((x0, 8, 13.5), (x1, 18, 15)), ((x0, 16, 1), (x1, 30.5, 2.5))):
        out.append(el(a, b, box_faces(t, a, b)))
    for y_mid in (24, 18):
        a, b, rot = sloped(x0, x1, y_mid, 8, 1.5, 22.6)
        out.append(el(a, b, box_faces(t, a, b), rotation=rot))
    return out


def catwalk_stairs():
    """Two grating treads on sloped flat-bar stringers; a handrail on each open side, as rail_left and rail_right."""
    for prefix, _, _ in STAGES:
        tex = stage_textures(prefix, 'catwalk_grating', 'catwalk_grating_side', 'catwalk_frame', 'rail')
        tread = lambda y, z0, z1: el((1, y - 2, z0), (15, y, z1), {
            'up': face('#catwalk_grating', [1, 4, 15, 12]), 'down': face('#catwalk_grating', [1, 4, 15, 12]),
            'north': face('#catwalk_grating_side', [1, 1, 15, 3]), 'south': face('#catwalk_frame', [1, 1, 15, 3]),
            'east': face('#catwalk_grating_side', [z0, 1, z1, 3]), 'west': face('#catwalk_grating_side', [z0, 1, z1, 3])})
        stringers = []
        for x0, x1 in ((0, 1), (15, 16)):
            a, b, rot = sloped(x0, x1, 7, 8, 4, 22.6)
            stringers.append(el(a, b, box_faces('#catwalk_frame', a, b), rotation=rot))
        model(f'{prefix}steel_catwalk_stairs', {'parent': 'minecraft:block/block', 'textures': tex,
                                                'elements': [tread(8, 8, 16), tread(16, 0, 8)] + stringers})
        model(f'{prefix}steel_catwalk_stairs_rail_left', {'parent': 'minecraft:block/block', 'textures': tex,
                                                          'elements': stairs_rail_elements(0, 1.5)})
        model(f'{prefix}steel_catwalk_stairs_rail_right', {'parent': 'minecraft:block/block', 'textures': tex,
                                                           'elements': stairs_rail_elements(14.5, 16)})
    parts = []
    for stage in STAGES:
        prefix = stage[0]
        for facing, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            for name, when in (('', {}), ('_rail_left', {'left': 'true'}), ('_rail_right', {'right': 'true'})):
                apply = {'model': f'mcremix:block/{prefix}steel_catwalk_stairs{name}'}
                if y:
                    apply['y'] = y
                parts.append({'when': {**patina_when(stage), 'facing': facing, **when}, 'apply': apply})
    states('steel_catwalk_stairs', {'multipart': parts})
    tex = stage_textures('', 'catwalk_grating', 'catwalk_grating_side', 'catwalk_frame', 'rail')
    elements = json.load(open(G.asset('models', 'block', 'steel_catwalk_stairs.json')))['elements']
    item_model('steel_catwalk_stairs', {'parent': 'minecraft:block/block', 'textures': tex,
                                        'display': stairs_display(), 'elements': elements})


def stairs_display():
    return {'gui': {'rotation': [30, 135, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
            'fixed': {'rotation': [0, 90, 0], 'scale': [0.5, 0.5, 0.5]},
            'ground': {'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
            'thirdperson_righthand': {'rotation': [75, 135, 0], 'translation': [0, 2.5, 0],
                                      'scale': [0.375, 0.375, 0.375]},
            'firstperson_righthand': {'rotation': [0, 135, 0], 'scale': [0.4, 0.4, 0.4]},
            'firstperson_lefthand': {'rotation': [0, 225, 0], 'scale': [0.4, 0.4, 0.4]}}


# ------------------------------------------------------------------------------------------------ railing

def railing_elements(t='#rail'):
    """One side's railing, on the north side: posts at both ends, handrail, knee rail and toe board."""
    boxes = [((0, 0, 0), (1.5, 16, 2)), ((14.5, 0, 0), (16, 16, 2)),       # posts, meeting a neighbour's as one
             ((1.5, 14.5, 0.25), (14.5, 16, 1.75)),                         # handrail
             ((1.5, 7.5, 0.5), (14.5, 8.75, 1.5)),                          # knee rail
             ((1.5, 0, 0.75), (14.5, 2.5, 1.25))]                           # toe board
    return [el(a, b, box_faces(t, a, b)) for a, b in boxes]


def railing():
    for prefix, _, _ in STAGES:
        tex = stage_textures(prefix, 'rail')
        tex['particle'] = f'mcremix:block/{prefix}steel_rail'
        model(f'{prefix}steel_railing_side', {'parent': 'minecraft:block/block', 'textures': tex,
                                              'elements': railing_elements()})
    parts = []
    for stage in STAGES:
        prefix = stage[0]
        for side, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            apply = {'model': f'mcremix:block/{prefix}steel_railing_side'}
            if y:
                apply['y'] = y
            parts.append({'when': {**patina_when(stage), side: 'true'}, 'apply': apply})
    states('steel_railing', {'multipart': parts})
    item_model('steel_railing', {'parent': 'minecraft:block/block', 'textures': {'rail': 'mcremix:block/steel_rail',
                                                                                 'particle': 'mcremix:block/steel_rail'},
                                 'display': stairs_display(),
                                 'elements': [dict(e, **{'from': [e['from'][0], e['from'][1], e['from'][2] + 7],
                                                         'to': [e['to'][0], e['to'][1], e['to'][2] + 7]})
                                              for e in railing_elements()]})


# ------------------------------------------------------------------------------------------------ caged ladder

def caged_ladder():
    """
    Two stiles and two rungs a block against the wall (south, facing north, as vanilla's ladder is drawn), and around
    the climber a hoop at the foot of each block with five straps running up, as a real cage has.
    """
    ladder = [((1, 0, 14), (2.5, 16, 15.5)), ((13.5, 0, 14), (15, 16, 15.5)),
              ((2.5, 3, 14.25), (13.5, 4, 15.25)), ((2.5, 11, 14.25), (13.5, 12, 15.25)),
              ((1, 6, 15.5), (2.5, 7, 16)), ((13.5, 6, 15.5), (15, 7, 16))]  # brackets to the wall
    cage = [((0, 0, 0), (16, 1.5, 1)), ((0, 0, 1), (1, 1.5, 14)), ((15, 0, 1), (16, 1.5, 14)),  # the hoop
            ((4, 1.5, 0), (5, 16, 0.75)), ((11, 1.5, 0), (12, 16, 0.75)),                         # straps
            ((0, 1.5, 6), (0.75, 16, 7)), ((15.25, 1.5, 6), (16, 16, 7)),
            ((0, 1.5, 12), (0.75, 16, 13)), ((15.25, 1.5, 12), (16, 16, 13))]
    for prefix, _, _ in STAGES:
        tex = stage_textures(prefix, 'rail')
        tex['particle'] = f'mcremix:block/{prefix}steel_rail'
        model(f'{prefix}steel_caged_ladder', {'parent': 'minecraft:block/block', 'textures': tex,
                                              'elements': [el(a, b, box_faces('#rail', a, b)) for a, b in ladder]})
        model(f'{prefix}steel_caged_ladder_cage', {'parent': 'minecraft:block/block', 'textures': tex,
                                                   'elements': [el(a, b, box_faces('#rail', a, b)) for a, b in cage]})
    parts = []
    for stage in STAGES:
        prefix = stage[0]
        for facing, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            for name, when in (('', {}), ('_cage', {'caged': 'true'})):
                apply = {'model': f'mcremix:block/{prefix}steel_caged_ladder{name}'}
                if y:
                    apply['y'] = y
                parts.append({'when': {**patina_when(stage), 'facing': facing, **when}, 'apply': apply})
    states('steel_caged_ladder', {'multipart': parts})
    tex = {'rail': 'mcremix:block/steel_rail', 'particle': 'mcremix:block/steel_rail'}
    item_model('steel_caged_ladder', {'parent': 'minecraft:block/block', 'textures': tex, 'display': stairs_display(),
                                      'elements': [el(a, b, box_faces('#rail', a, b)) for a, b in ladder + cage]})


# ------------------------------------------------------------------------------------------------ creosote

# creosoted wood, darkest first: near black, the brown of old sleepers, and an oily sheen
CREOSOTE = [(20, 15, 11), (30, 22, 16), (40, 30, 21), (51, 38, 26), (63, 47, 32), (78, 60, 41)]
SHEEN = (88, 76, 62)
# heartwood the creosote barely got into, shown on a log's end
HEART = [(74, 50, 30), (92, 64, 38), (108, 78, 46)]


def creosote_grain(seed, vertical=True, boards=None):
    """Long dark grain with a few oily highlights; planks split into boards with dark joints."""
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    streak = [rng.randrange(6) for _ in range(16)]
    for a in range(16):
        drift = 0
        for b in range(16):
            if rng.random() < 0.12:
                drift += rng.choice((-1, 1))
            v = 2 + (streak[a] > 3) - (streak[a] < 1) + max(-1, min(1, drift)) // 1
            x, y = (a, b) if vertical else (b, a)
            px[x, y] = CREOSOTE[max(0, min(5, v))] + (255,)
    for _ in range(5):  # oily sheen, along the grain
        a, b = rng.randrange(16), rng.randrange(14)
        for k in range(rng.randrange(2, 5)):
            x, y = (a, b + k) if vertical else (b + k, a)
            if x < 16 and y < 16:
                px[x, y] = SHEEN + (255,)
    if boards:
        for row in boards:
            for x in range(16):
                px[x, row] = CREOSOTE[0] + (255,)
        for row, offset in zip([0] + [r + 1 for r in boards], (3, 11, 6, 13)):
            if row < 16:
                px[offset, min(15, row + 1)] = CREOSOTE[0] + (255,)  # a nail or knot in each board
    return img


def creosote_end(seed):
    """A log's sawn end: the outer sapwood soaked black, the heartwood inside paler brown, with growth rings."""
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            ring = int(r * 1.3 + rng.random() * 0.6) % 2
            if r > 5.2:
                c = CREOSOTE[1 + ring]
            elif r > 4.2:
                c = CREOSOTE[3]
            else:
                c = HEART[ring + (r < 1.5)]
            px[x, y] = c + (255,)
    for x in range(16):  # the edge
        for y in (0, 15):
            px[x, y] = CREOSOTE[0] + (255,)
            px[y, x] = CREOSOTE[0] + (255,)
    return img


def creosote_textures():
    png(creosote_grain(4501), 'creosote_log')
    png(creosote_end(4502), 'creosote_log_top')
    png(creosote_grain(4503, vertical=False, boards=(3, 7, 11, 15)), 'creosote_planks')


def creosote():
    log = {'side': 'mcremix:block/creosote_log', 'end': 'mcremix:block/creosote_log_top'}
    model('creosote_log', {'parent': 'minecraft:block/cube_column', 'textures': log})
    model('creosote_log_horizontal', {'parent': 'minecraft:block/cube_column_horizontal', 'textures': log})
    states('creosote_log', {'variants': {
        'axis=y': {'model': 'mcremix:block/creosote_log'},
        'axis=z': {'model': 'mcremix:block/creosote_log_horizontal', 'x': 90},
        'axis=x': {'model': 'mcremix:block/creosote_log_horizontal', 'x': 90, 'y': 90}}})
    planks = {'all': 'mcremix:block/creosote_planks'}
    model('creosote_planks', {'parent': 'minecraft:block/cube_all', 'textures': planks})
    states('creosote_planks', {'variants': {'': {'model': 'mcremix:block/creosote_planks'}}})
    side = {'bottom': 'mcremix:block/creosote_planks', 'top': 'mcremix:block/creosote_planks',
            'side': 'mcremix:block/creosote_planks'}
    for name, parent in (('creosote_stairs', 'stairs'), ('creosote_stairs_inner', 'inner_stairs'),
                         ('creosote_stairs_outer', 'outer_stairs'), ('creosote_slab', 'slab'),
                         ('creosote_slab_top', 'slab_top')):
        model(name, {'parent': f'minecraft:block/{parent}', 'textures': side})
    states('creosote_stairs', G.stairs_states('mcremix:block/creosote_stairs'))
    states('creosote_slab', {'variants': {'type=bottom': {'model': 'mcremix:block/creosote_slab'},
                                          'type=top': {'model': 'mcremix:block/creosote_slab_top'},
                                          'type=double': {'model': 'mcremix:block/creosote_planks'}}})
    tex = {'texture': 'mcremix:block/creosote_planks'}
    for name, parent in (('creosote_fence_post', 'fence_post'), ('creosote_fence_side', 'fence_side'),
                         ('creosote_fence_inventory', 'fence_inventory'),
                         ('creosote_fence_gate', 'template_fence_gate'),
                         ('creosote_fence_gate_open', 'template_fence_gate_open'),
                         ('creosote_fence_gate_wall', 'template_fence_gate_wall'),
                         ('creosote_fence_gate_wall_open', 'template_fence_gate_wall_open')):
        model(name, {'parent': f'minecraft:block/{parent}', 'textures': tex})
    parts = [{'apply': {'model': 'mcremix:block/creosote_fence_post'}}]
    for side_, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        apply = {'model': 'mcremix:block/creosote_fence_side', 'uvlock': True}
        if y:
            apply['y'] = y
        parts.append({'when': {side_: 'true'}, 'apply': apply})
    states('creosote_fence', {'multipart': parts})
    gate = {}
    for facing, y in (('south', 0), ('west', 90), ('north', 180), ('east', 270)):
        for in_wall in ('false', 'true'):
            for open_ in ('false', 'true'):
                name = 'creosote_fence_gate' + ('_wall' if in_wall == 'true' else '') + \
                       ('_open' if open_ == 'true' else '')
                v = {'model': f'mcremix:block/{name}', 'uvlock': True}
                if y:
                    v['y'] = y
                gate[f'facing={facing},in_wall={in_wall},open={open_}'] = v
    states('creosote_fence_gate', {'variants': gate})
    # the post: a 6 pixel square pole, the log's side along it and its sawn end at each end
    post = el((5, 0, 5), (11, 16, 11), {
        **{d: face('#side', [5, 0, 11, 16]) for d in ('north', 'south', 'east', 'west')},
        'up': face('#end', [5, 5, 11, 11], cull='up'), 'down': face('#end', [5, 5, 11, 11], cull='down')})
    model('creosote_post', {'parent': 'minecraft:block/block',
                            'textures': {**log, 'particle': 'mcremix:block/creosote_log'}, 'elements': [post]})
    # where posts join, a stub of the same pole from the middle of the block to the face, drawn pointing up; and under a
    # lying post with a lantern hung from it, the top of a chain, as vanilla draws chain
    stub = el((5, 11, 5), (11, 16, 11), {d: face('#side', [5, 0, 11, 5]) for d in ('north', 'south', 'east', 'west')})
    model('creosote_post_stub', {'parent': 'minecraft:block/block', 'textures': log, 'elements': [stub]})
    chain = [{'from': [6.5, 0, 8], 'to': [9.5, 5, 8], 'rotation': {'origin': [8, 8, 8], 'axis': 'y', 'angle': 45},
              'shade': False, 'faces': {'north': {'uv': [3, 11, 0, 16], 'texture': '#chain'},
                                        'south': {'uv': [0, 11, 3, 16], 'texture': '#chain'}}},
             {'from': [8, 0, 6.5], 'to': [8, 5, 9.5], 'rotation': {'origin': [8, 8, 8], 'axis': 'y', 'angle': 45},
              'shade': False, 'faces': {'west': {'uv': [6, 11, 3, 16], 'texture': '#chain'},
                                        'east': {'uv': [3, 11, 6, 16], 'texture': '#chain'}}}]
    model('creosote_post_chain', {'parent': 'minecraft:block/block', 'textures': {'chain': 'minecraft:block/chain'},
                                  'elements': chain})
    parts = [{'when': {'axis': 'y'}, 'apply': {'model': 'mcremix:block/creosote_post'}},
             {'when': {'axis': 'z'}, 'apply': {'model': 'mcremix:block/creosote_post', 'x': 90}},
             {'when': {'axis': 'x'}, 'apply': {'model': 'mcremix:block/creosote_post', 'x': 90, 'y': 90}}]
    for side_, turn in (('up', {}), ('down', {'x': 180}), ('north', {'x': 90}), ('east', {'x': 90, 'y': 90}),
                        ('south', {'x': 90, 'y': 180}), ('west', {'x': 90, 'y': 270})):
        parts.append({'when': {side_: 'true'}, 'apply': {'model': 'mcremix:block/creosote_post_stub', **turn}})
    parts.append({'when': {'chain': 'true'}, 'apply': {'model': 'mcremix:block/creosote_post_chain'}})
    states('creosote_post', {'multipart': parts})
    for name, parent in (('creosote_log', 'creosote_log'), ('creosote_planks', 'creosote_planks'),
                         ('creosote_stairs', 'creosote_stairs'), ('creosote_slab', 'creosote_slab'),
                         ('creosote_fence', 'creosote_fence_inventory'),
                         ('creosote_fence_gate', 'creosote_fence_gate'), ('creosote_post', 'creosote_post')):
        item_model(name, {'parent': f'mcremix:block/{parent}'})


# ------------------------------------------------------------------------------------------------ argon tubes

def argon_textures():
    """Clear glass with a highlight down one side; argon's glow, violet with a paler core; the nickel end caps."""
    glass = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = glass.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = (214, 222, 236, 70)
        px[1, y] = (250, 252, 255, 200)
        px[2, y] = (236, 240, 250, 120)
    png(glass, 'argon_tube_glass')
    glow = Image.new('RGBA', (16, 16))
    px = glow.load()
    rng = random.Random(4601)
    # the gas fills the bore, 3 px across (texels 6 to 9): violet at its sides, lighter down the middle, which still
    # reads violet through the glass
    for y in range(16):
        for x in range(16):
            edge = min(x, 15 - x)
            base = (120, 64, 222) if edge < 6 else (150, 92, 240) if edge == 6 else (190, 142, 255)
            f = 0.95 + rng.random() * 0.07
            px[x, y] = tuple(min(255, int(c * f)) for c in base) + (255,)
    png(glow, 'argon_tube_glow')
    cap = Image.new('RGBA', (16, 16))
    px = cap.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = [(96, 98, 104), (150, 152, 158), (178, 180, 186), (132, 134, 140)][(y // 2) % 4] + (255,)
        px[0, y] = (70, 72, 78, 255)
    png(cap, 'argon_tube_cap')
    # the empty tube's item: a short clear tube with a cap at each end, drawn on the diagonal
    tube = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = tube.load()
    for i in range(3, 13):
        for w in (-1, 0, 1):
            x, y = i + w, 15 - i
            if 0 <= x < 16:
                px[x, y] = (226, 236, 248, 255) if w == -1 else (186, 198, 214, 150) if w == 0 else (120, 130, 146, 220)
    for i in (2, 3, 12, 13):
        for w in (-1, 0, 1):
            x, y = i + w, 15 - i
            if 0 <= x < 16:
                px[x, y] = (150, 152, 158, 255) if w < 1 else (96, 98, 104, 255)
    G.save_png(tube, G.asset('textures', 'item', 'glass_tube.png'))
    item_model('glass_tube', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'mcremix:item/glass_tube'}})


def argon_tube():
    """Drawn standing up: the glass tube, the gas inside it, a cap at each end or a thin collar where tubes join."""
    tex = {'glass': 'mcremix:block/argon_tube_glass', 'glow': 'mcremix:block/argon_tube_glow',
           'cap': 'mcremix:block/argon_tube_cap', 'particle': 'mcremix:block/argon_tube_glass'}
    tube = el((6, 0, 6), (10, 16, 10), {d: face('#glass', [6, 0, 10, 16]) for d in ('north', 'south', 'east', 'west')})
    model('argon_tube_glass', {'parent': 'minecraft:block/block', 'textures': tex, 'elements': [tube]})
    core = lambda lit: el((6.5, 0, 6.5), (9.5, 16, 9.5), {
        **{d: face('#glow' if lit else '#cap', [6.5, 0, 9.5, 16]) for d in ('north', 'south', 'east', 'west')}},
        light=lit, shade=not lit)
    model('argon_tube_lit', {'parent': 'minecraft:block/block', 'textures': tex, 'elements': [core(True)]})
    # unlit, the gas is not seen: only a fine dark electrode wire runs a little way in from each end
    wire = [el((7.5, 0, 7.5), (8.5, 4, 8.5), box_faces('#cap', (7.5, 0, 7.5), (8.5, 4, 8.5))),
            el((7.5, 12, 7.5), (8.5, 16, 8.5), box_faces('#cap', (7.5, 12, 7.5), (8.5, 16, 8.5)))]
    model('argon_tube_unlit', {'parent': 'minecraft:block/block', 'textures': tex, 'elements': wire})
    for end, (y0, y1), (c0, c1) in (('bottom', (0, 3), (0, 1)), ('top', (13, 16), (15, 16))):
        model(f'argon_tube_cap_{end}', {'parent': 'minecraft:block/block', 'textures': tex, 'elements': [
            el((5, y0, 5), (11, y1, 11), box_faces('#cap', (5, y0, 5), (11, y1, 11)))]})
        model(f'argon_tube_collar_{end}', {'parent': 'minecraft:block/block', 'textures': tex, 'elements': [
            el((5.5, c0, 5.5), (10.5, c1, 10.5), box_faces('#cap', (5.5, c0, 5.5), (10.5, c1, 10.5),
                                                          skip=('up' if end == 'top' else 'down',)))]})
    # the model's top is the positive end along y and x (x 90 then y 90), the negative along z (x 90 alone)
    turns = {'y': {}, 'z': {'x': 90}, 'x': {'x': 90, 'y': 90}}
    parts = []
    for axis, rot in turns.items():
        top_end, bottom_end = ('joins_neg', 'joins_pos') if axis == 'z' else ('joins_pos', 'joins_neg')
        add = lambda m, when: parts.append({'when': {'axis': axis, **when}, 'apply': {'model': f'mcremix:block/{m}',
                                                                                         **rot}})
        add('argon_tube_glass', {})
        add('argon_tube_lit', {'lit': 'true'})
        add('argon_tube_unlit', {'lit': 'false'})
        add('argon_tube_cap_top', {top_end: 'false'})
        add('argon_tube_collar_top', {top_end: 'true'})
        add('argon_tube_cap_bottom', {bottom_end: 'false'})
        add('argon_tube_collar_bottom', {bottom_end: 'true'})
    states('argon_tube', {'multipart': parts})
    elements = []
    for name in ('argon_tube_glass', 'argon_tube_lit', 'argon_tube_cap_top', 'argon_tube_cap_bottom'):
        elements += json.load(open(G.asset('models', 'block', name + '.json')))['elements']
    item_model('argon_tube', {'parent': 'minecraft:block/block', 'textures': tex, 'display': stairs_display(),
                              'elements': elements})


# ------------------------------------------------------------------------------------------------ recipes

def shaped(name, pattern, key, count=1):
    G.recipe(name, {'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': pattern, 'key': key,
                    'result': G.item(f'mcremix:{name}', count)})


def recipes():
    sheet, nugget = {'tag': 'forge:plates/steel'}, {'tag': 'forge:nuggets/steel'}
    shaped('steel_catwalk', ['N N', 'SSS'], {'S': sheet, 'N': nugget}, 3)
    shaped('steel_catwalk_stairs', ['C  ', 'CC '], {'C': G.item('mcremix:steel_catwalk')}, 2)
    shaped('steel_railing', ['NNN', 'N N'], {'N': nugget}, 2)
    shaped('steel_caged_ladder', ['N N', 'SNS', 'N N'], {'S': sheet, 'N': nugget}, 3)
    # creosote poured over bare timber by a Spout (Taran's card, 2026-11-15); a log only once its bark is off, as real
    # poles are peeled before they are treated
    untreated = lambda base: {'type': 'forge:difference', 'base': base,
                              'subtracted': {'tag': 'mcremix:creosote_timber'}}
    if CREOSOTE_FLUID:
        for name, ingredient, amount in (('creosote_log', untreated({'tag': 'forge:stripped_logs'}), 250),
                                         ('creosote_planks', untreated({'tag': 'minecraft:planks'}), 100)):
            G.recipe(f'filling/{name}', {'type': 'create:filling',
                                         'ingredients': [ingredient, {'fluid': CREOSOTE_FLUID, 'amount': amount}],
                                         'results': [G.item(f'mcremix:{name}')]})
    planks, log = G.item('mcremix:creosote_planks'), G.item('mcremix:creosote_log')
    shaped('creosote_planks', ['#'], {'#': log}, 4)
    shaped('creosote_stairs', ['#  ', '## ', '###'], {'#': planks}, 4)
    shaped('creosote_slab', ['###'], {'#': planks}, 6)
    shaped('creosote_fence', ['#S#', '#S#'], {'#': planks, 'S': G.item('minecraft:stick')}, 3)
    shaped('creosote_fence_gate', ['S#S', 'S#S'], {'#': planks, 'S': G.item('minecraft:stick')})
    shaped('creosote_post', ['#', '#'], {'#': log}, 4)
    # an empty tube, then argon let into it by a Spout
    if ARGON_FLUID:
        shaped('glass_tube', ['N', 'G', 'N'], {'G': G.item('minecraft:glass_pane'), 'N': {'tag': 'forge:nuggets/iron'}},
               2)
        G.recipe('filling/argon_tube', {'type': 'create:filling',
                                        'ingredients': [G.item('mcremix:glass_tube'),
                                                        {'fluid': ARGON_FLUID, 'amount': 50}],
                                        'results': [G.item('mcremix:argon_tube')]})


def loot_and_tags():
    for name in STEEL_BLOCKS + CREOSOTE_BLOCKS + ['argon_tube']:
        if name not in ('steel_railing', 'creosote_slab'):
            G.write_json(G.data('mcremix', 'loot_tables', 'blocks', name + '.json'), G.loot_self(name))
    # a railing drops one for each side it has; a double slab both halves
    G.write_json(G.data('mcremix', 'loot_tables', 'blocks', 'steel_railing.json'), {
        'type': 'minecraft:block', 'pools': [
            {'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': 'mcremix:steel_railing'}],
             'conditions': [{'condition': 'minecraft:block_state_property', 'block': 'mcremix:steel_railing',
                             'properties': {side: 'true'}}, {'condition': 'minecraft:survives_explosion'}]}
            for side in ('north', 'east', 'south', 'west')]})
    G.write_json(G.data('mcremix', 'loot_tables', 'blocks', 'creosote_slab.json'), {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{
            'type': 'minecraft:item', 'name': 'mcremix:creosote_slab',
            'functions': [{'function': 'minecraft:set_count', 'count': 2, 'add': False,
                           'conditions': [{'condition': 'minecraft:block_state_property',
                                           'block': 'mcremix:creosote_slab', 'properties': {'type': 'double'}}]},
                          {'function': 'minecraft:explosion_decay'}]}]}]})
    m = lambda names: [f'mcremix:{n}' for n in names]
    add_tag('minecraft', 'blocks', 'mineable/pickaxe', m(STEEL_BLOCKS))
    add_tag('minecraft', 'blocks', 'mineable/axe', m(CREOSOTE_BLOCKS))
    add_tag('minecraft', 'blocks', 'climbable', m(['steel_caged_ladder']))
    for kind in ('blocks', 'items'):
        add_tag('minecraft', kind, 'planks', m(['creosote_planks']))
        add_tag('minecraft', kind, 'wooden_stairs', m(['creosote_stairs']))
        add_tag('minecraft', kind, 'wooden_slabs', m(['creosote_slab']))
        add_tag('minecraft', kind, 'wooden_fences', m(['creosote_fence']))
        add_tag('minecraft', kind, 'fence_gates', m(['creosote_fence_gate']))
        add_tag('mcremix', kind, 'creosote_timber', m(CREOSOTE_BLOCKS))
    # the vanilla stripped logs and woods, under Forge's name, for the Spout to creosote (other mods add theirs)
    woods = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry']
    add_tag('forge', 'items', 'stripped_logs', [f'minecraft:stripped_{w}_log' for w in woods]
            + ['minecraft:stripped_crimson_stem', 'minecraft:stripped_warped_stem', 'minecraft:stripped_bamboo_block'])


# ------------------------------------------------------------------------------------------------ ponder

def catwalk_state(walks, stairs, pos):
    """A catwalk at pos, joined on each side to a catwalk or to stairs that climb along that side's axis."""
    x, y, z = pos
    props = {'patina': 'none', 'waterlogged': 'false'}
    for side, (dx, dz) in (('north', (0, -1)), ('south', (0, 1)), ('west', (-1, 0)), ('east', (1, 0))):
        n = (x + dx, y, z + dz)
        joined = n in walks or (n in stairs and (stairs[n] in ('east', 'west')) == (dx != 0))
        props[side] = 'true' if joined else 'false'
    return ('mcremix:steel_catwalk', props)


def ponder_schematics():
    """
    catwalk/intro: a steel column four high in the middle of a 7x7 plate, a ring of catwalks round it a block up with a
    gap on the north for a caged ladder up the column's north face, railings on the ring's open edges, and a flight of
    catwalk stairs climbing west onto it from the east. argon_tube/intro: a black board along the back of a 5x5 plate,
    three tubes lying east-west in front of it, and a creative voltage source at the front right.
    """
    b = {}
    G.plate(b, 7)
    for y in range(1, 5):
        b[(3, y, 3)] = ('mcremix:steel_block', {})
    walks = {(x, 2, z) for x in range(2, 5) for z in range(2, 5)} - {(3, 2, 3), (3, 2, 2)}
    stairs = {(5, 2, 3): 'west', (6, 1, 3): 'west'}
    for pos in walks:
        b[pos] = catwalk_state(walks, stairs, pos)
    for pos, facing in stairs.items():
        b[pos] = ('mcremix:steel_catwalk_stairs', {'facing': facing, 'left': 'true', 'right': 'true',
                                                   'patina': 'none', 'waterlogged': 'false'})
    rails = {}
    for x, _, z in walks:
        for side, (dx, dz) in (('north', (0, -1)), ('south', (0, 1)), ('west', (-1, 0)), ('east', (1, 0))):
            n = (x + dx, 2, z + dz)
            if n not in walks and n not in stairs and n != (3, 2, 3) and n != (3, 2, 2):
                rails.setdefault((x, 3, z), set()).add(side)
    for pos, sides in rails.items():
        b[pos] = ('mcremix:steel_railing', {**{s: 'true' if s in sides else 'false'
                                               for s in ('north', 'east', 'south', 'west')},
                                            'patina': 'none', 'waterlogged': 'false'})
    b[(3, 1, 2)] = ('mcremix:steel_caged_ladder', {'facing': 'north', 'caged': 'false', 'patina': 'none',
                                                    'waterlogged': 'false'})
    b[(3, 2, 2)] = ('mcremix:steel_caged_ladder', {'facing': 'north', 'caged': 'true', 'patina': 'none',
                                                    'waterlogged': 'false'})
    G.save_structure('catwalk/intro', (7, 5, 7), b)

    b = {}
    G.plate(b)
    for x in range(5):
        for y in range(1, 4):
            b[(x, y, 3)] = ('minecraft:black_concrete', {})
    for x in (1, 2, 3):
        b[(x, 2, 2)] = ('mcremix:argon_tube', {'axis': 'x', 'lit': 'false', 'joins_neg': 'false',
                                               'joins_pos': 'false'})
    b[(4, 1, 1)] = ('powergrid:creative_voltage_source', {'facing': 'west'})
    G.save_structure('argon_tube/intro', (5, 4, 5), b)


# ------------------------------------------------------------------------------------------------ lang

def lang():
    rusts = ' Like all steel it _rusts_ a little where it gets wet; _honeycomb_ waxes it.'
    text = {
        'block.mcremix.steel_catwalk': 'Steel Catwalk',
        'block.mcremix.steel_catwalk.tooltip.summary': (
            'Steel _bar grating_ in the top of its block, so it lies _flush_ with the floor beside it. Light and water '
            'pass through. An edge band shows wherever it doesn\'t run on into another catwalk. Stand _railings_ on '
            'its edges.' + rusts),
        'block.mcremix.steel_catwalk_stairs': 'Steel Catwalk Stairs',
        'block.mcremix.steel_catwalk_stairs.tooltip.summary': (
            'Grating treads on steel stringers, climbing the way you face to a catwalk ahead. Each open side has a '
            '_handrail_; set stairs side by side for a _wide flight_ with rails only on the outside.' + rusts),
        'block.mcremix.steel_railing': 'Steel Railing',
        'block.mcremix.steel_railing.tooltip.summary': (
            'A guard rail with a handrail, knee rail and _toe board_. It goes on the edge you aim at, or in front of '
            'you; use another on it to rail _another side_. Nothing can _jump_ it.' + rusts),
        'block.mcremix.steel_caged_ladder': 'Steel Caged Ladder',
        'block.mcremix.steel_caged_ladder.tooltip.summary': (
            'A steel ladder with a _safety cage_ around the climber, so you can\'t fall off it. The cage starts _one '
            'block up_, leaving the foot open to step in.' + rusts),
        'block.mcremix.creosote_log': 'Creosote Log',
        'block.mcremix.creosote_log.tooltip.summary': (
            'A peeled log soaked in _creosote_ by a _Spout_, dark and oily like a telegraph pole. Its end shows the '
            'paler heartwood the creosote never reached. It _weeps_ a little in hot sun.'),
        'block.mcremix.creosote_planks': 'Creosote Planks',
        'block.mcremix.creosote_planks.tooltip.summary': (
            'Planks soaked in _creosote_ by a _Spout_, dark like railway sleepers. They _weep_ a little in hot sun, '
            'and burn as any wood does.'),
        'block.mcremix.creosote_stairs': 'Creosote Stairs',
        'block.mcremix.creosote_slab': 'Creosote Slab',
        'block.mcremix.creosote_fence': 'Creosote Fence',
        'block.mcremix.creosote_fence_gate': 'Creosote Fence Gate',
        'block.mcremix.creosote_post': 'Creosote Post',
        'block.mcremix.creosote_post.tooltip.summary': (
            'A creosoted pole, standing or lying like a log, for _jetties_, _telegraph poles_ and posts. Posts _join_ '
            'where one meets another\'s side, so a crossarm meets its pole. _Lanterns_ hang from it, and _torches_ '
            'stand on a standing one.'),
        'item.mcremix.glass_tube': 'Glass Tube',
        'item.mcremix.glass_tube.tooltip.summary': (
            'A glass tube with an electrode sealed into each end. A _Spout_ fills it with _argon_.'),
        'block.mcremix.argon_tube': 'Argon Tube',
        'block.mcremix.argon_tube.tooltip.summary': (
            'A glass tube of _argon_ that _glows violet_ with about _800 V_ across its two ends, as a _transformer_ '
            'gives, and stays lit down to _600 V_. Tubes set end to end _join_ into one line; wire them _in series_ '
            'with short jumpers between the ends, each needing its 800 V, or feed each on its own.'),
        'mcremix.goggles.argon_tube.volts': 'Across it: %s V',
        'mcremix.goggles.argon_tube.lit': 'Glowing',
        'mcremix.goggles.argon_tube.too_low': 'Dark: needs %s V to strike',
    }
    G_lang = G.asset('lang', 'en_us.json')
    merge_json(G_lang, lambda obj: obj.update(text))

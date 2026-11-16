"""
Resources for the distillation build's third stage, crude oil (registry/ModOil.java, and crude oil's fluids, items and
pockets elsewhere): the diesel engine's models and textures, petroleum coke and paraffin wax, crude oil's split and
what is made from its cuts, the new tags, damage types and world generation, and their Ponder structures. gen_assets.py
calls run() after gen_column, passing itself; crude oil's fluids are in gen_assets' CHEMICALS, and this file adds to the
lang and tag files written before it rather than owning any shared file.
"""
import math
import random

from PIL import Image

import gen_decor as D

G = None  # gen_assets, set by run()

# old stationary engine green, as such engines were painted, from shadow to highlight, and the cast iron under it
PAINT = [(0x18, 0x24, 0x1A), (0x22, 0x34, 0x26), (0x2C, 0x44, 0x30), (0x38, 0x56, 0x3C), (0x46, 0x68, 0x4A),
         (0x58, 0x7C, 0x5A), (0x6C, 0x90, 0x6C)]
IRON = [(0x1C, 0x1C, 0x1E), (0x2A, 0x2A, 0x2D), (0x38, 0x38, 0x3B), (0x48, 0x48, 0x4B), (0x5A, 0x5A, 0x5D),
        (0x6E, 0x6E, 0x70), (0x86, 0x86, 0x88)]
BRASS = [(0x5C, 0x3E, 0x14), (0x7E, 0x58, 0x1E), (0xA0, 0x74, 0x2A), (0xC0, 0x90, 0x3A), (0xDA, 0xAC, 0x50),
         (0xEC, 0xC8, 0x70), (0xF8, 0xE4, 0x9C)]


def run(gen):
    global G
    G = gen
    textures()
    diesel_engine()
    recipes()
    tags()
    damage_types()
    worldgen()
    ponder_schematics()
    lang()


# ------------------------------------------------------------------------------------------------ textures

def mottled(seed, pal, base=3, speckle=0.25):
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = D.shade(pal, base + (rng.random() < speckle) - (rng.random() < speckle * 0.8))
    return img


def body(seed):
    """Painted cast iron: green, lit along the top, worn through to the iron at its edges."""
    rng = random.Random(seed)
    img = mottled(seed, PAINT)
    px = img.load()
    for x in range(16):
        px[x, 0] = D.shade(PAINT, 5)
        px[x, 15] = D.shade(PAINT, 1)
    for y in range(16):
        px[0, y] = D.shade(PAINT, 4)
        px[15, y] = D.shade(PAINT, 1)
    for _ in range(10):
        x, y = rng.choice([(rng.randrange(16), rng.choice((0, 1, 14, 15))),
                           (rng.choice((0, 1, 14, 15)), rng.randrange(16))])
        px[x, y] = D.shade(IRON, rng.choice((3, 4)))
    return img


def cylinder(seed):
    """The cylinder's cooling fins: bare cast iron in horizontal ribs, each lit on top and shadowed under."""
    img = mottled(seed, IRON)
    px = img.load()
    for y in range(16):
        rib = y % 4
        for x in range(16):
            if rib == 0:
                px[x, y] = D.shade(IRON, 5)
            elif rib == 3:
                px[x, y] = D.shade(IRON, 1)
    return img


def flywheel(seed):
    """The flywheel's iron: dark, the rim's worn face bright where it turns."""
    img = mottled(seed, IRON, base=2)
    px = img.load()
    for x in range(16):
        for y in (0, 1):
            px[x, y] = D.shade(IRON, 5 - y)
    return img


def stack(seed):
    """The exhaust stack: sooty iron pipe, blacker towards its top."""
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            i = 3 - (y < 6) - (y < 3) + (rng.random() < 0.2) - (rng.random() < 0.2)
            if x in (0, 1):
                i += 1
            px[x, y] = D.shade(IRON, i)
    return img


def tank(seed):
    """The fuel tank: brass, a seam round it and a filler cap."""
    img = mottled(seed, BRASS)
    px = img.load()
    for x in range(16):
        px[x, 7] = D.shade(BRASS, 1)
        px[x, 8] = D.shade(BRASS, 5)
    return img


def petroleum_coke():
    return G.lump_texture(31, (0x14, 0x14, 0x16), (0x6A, 0x6C, 0x74), accent=(0x9C, 0xA0, 0xAA), accent_chance=0.12)


def paraffin_wax():
    """A slab of paraffin wax: milky white, faintly see-through at its edges, lit on its upper left."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    rng = random.Random(32)
    for y in range(4, 13):
        for x in range(2, 14):
            # a slab seen a little from above: its top face over its front
            top = y < 7 and x >= 2 + (7 - y) - 1
            if y < 7 and not top:
                continue
            if top:
                col, a = (0xFA, 0xF8, 0xEE), 245
            else:
                col, a = (0xE6, 0xE2, 0xD2), 235
            if x == 2 or y == 12 or x == 13:
                col, a = (0xC8, 0xC2, 0xAE), 210
            if rng.random() < 0.08:
                col = tuple(max(0, c - 10) for c in col)
            img.putpixel((x, y), col + (a,))
    return img


def textures():
    D.png(body(4601), 'diesel_engine_body')
    D.png(cylinder(4602), 'diesel_engine_cylinder')
    D.png(flywheel(4603), 'diesel_engine_flywheel')
    D.png(stack(4604), 'diesel_engine_stack')
    D.png(tank(4605), 'diesel_engine_tank')
    G.save_png(petroleum_coke(), G.asset('textures', 'item', 'petroleum_coke.png'))
    G.save_png(paraffin_wax(), G.asset('textures', 'item', 'paraffin_wax.png'))
    for name in ('petroleum_coke', 'paraffin_wax'):
        D.item_model(name, {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'mcremix:item/{name}'}})


# ------------------------------------------------------------------------------------------------ the engine

def box(tex, frm, to, skip=()):
    return D.el(frm, to, D.box_faces(tex, frm, to, skip=skip))


def flywheel_elements(axis='y'):
    """
    The shaft and the spoked flywheel on it, modelled upright with the flywheel at the top (axis 'y', the partial the
    engine turns), or lying along x with the flywheel at its east end (axis 'x', for the item). The rim is eight bars,
    four square to the axes and four turned 45 degrees.
    """
    els = []
    a, t, y0, y1 = 6.5, 1.5, 13.25, 15.25  # the rim's outer apothem, its thickness and its span along the shaft
    half = a * math.tan(math.pi / 8)
    parts = [('#shaft', (6, 0, 6), (10, 16, 10), None),
             ('#flywheel', (5.5, 12.5, 5.5), (10.5, 16, 10.5), None),
             ('#flywheel', (7.25, 13.75, 2.5), (8.75, 14.75, 13.5), None),
             ('#flywheel', (2.5, 13.75, 7.25), (13.5, 14.75, 8.75), None),
             ('#flywheel', (8 - half, y0, 8 - a), (8 + half, y1, 8 - a + t), None),
             ('#flywheel', (8 - half, y0, 8 + a - t), (8 + half, y1, 8 + a), None),
             ('#flywheel', (8 - a, y0, 8 - half), (8 - a + t, y1, 8 + half), None),
             ('#flywheel', (8 + a - t, y0, 8 - half), (8 + a, y1, 8 + half), None)]
    d = (a - t / 2) / math.sqrt(2)
    for sx, sz in ((1, 1), (-1, -1), (1, -1), (-1, 1)):
        cx, cz = 8 + sx * d, 8 + sz * d
        # a bar long in x turned +45 about y lies along (1, -1): the tangent where the corner is (+,+) or (-,-)
        if sx == sz:
            frm, to = (cx - half, y0, cz - t / 2), (cx + half, y1, cz + t / 2)
        else:
            frm, to = (cx - t / 2, y0, cz - half), (cx + t / 2, y1, cz + half)
        parts.append(('#flywheel', frm, to, (cx, (y0 + y1) / 2, cz)))
    for tex, frm, to, turn in parts:
        if axis == 'x':
            # lying along x: y and x trade places, and a turn about y becomes one about x the other way
            frm, to = (frm[1], frm[0], frm[2]), (to[1], to[0], to[2])
            turn = turn and (turn[1], turn[0], turn[2])
        e = box(tex, frm, to)
        if tex == '#shaft':
            ends = ('up', 'down') if axis == 'y' else ('east', 'west')
            for f in ends:
                e['faces'][f] = D.face('#shaft_end', [6, 6, 10, 10])
        if turn:
            e['rotation'] = {'origin': [round(v, 3) for v in turn], 'axis': axis, 'angle': 45 if axis == 'y' else -45}
        e['from'] = [round(v, 3) for v in e['from']]
        e['to'] = [round(v, 3) for v in e['to']]
        els.append(e)
    return els


def engine_elements():
    """
    The engine's body facing north: a base plate, the crankcase the shaft runs through, the finned cylinder pointing
    out of its front with its head, the exhaust stack rising off the head, the brass fuel tank on the crankcase, and a
    bearing housing each side where the shaft comes out. The shaft and flywheel are the partial.
    """
    return [
        box('#body', (3.5, 0, 1), (12.5, 1.5, 15)),
        box('#body', (4, 1.5, 5), (12, 12, 14)),
        box('#body', (3, 5, 5), (4, 11, 11)),
        box('#body', (12, 5, 5), (13, 11, 11)),
        box('#cylinder', (5, 5, 0.5), (11, 11, 5)),
        box('#cylinder', (4.5, 4.5, 1), (11.5, 11.5, 1.75)),
        box('#cylinder', (4.5, 4.5, 2.5), (11.5, 11.5, 3.25)),
        box('#cylinder', (4.5, 4.5, 4), (11.5, 11.5, 4.75)),
        box('#body', (5.5, 5.5, 0), (10.5, 10.5, 0.5)),
        box('#stack', (7, 11, 1.5), (9, 15.5, 3.5)),
        box('#stack', (6.5, 15.5, 1), (9.5, 16, 4)),
        box('#tank', (5, 12, 8), (11, 14.5, 13)),
        box('#body', (7.5, 14.5, 9.5), (8.5, 15, 10.5)),
    ]


ENGINE_TEXTURES = {'body': 'mcremix:block/diesel_engine_body', 'cylinder': 'mcremix:block/diesel_engine_cylinder',
                   'stack': 'mcremix:block/diesel_engine_stack', 'tank': 'mcremix:block/diesel_engine_tank',
                   'particle': 'mcremix:block/diesel_engine_body'}
FLYWHEEL_TEXTURES = {'flywheel': 'mcremix:block/diesel_engine_flywheel', 'shaft': 'create:block/axis',
                     'shaft_end': 'create:block/axis_top', 'particle': 'mcremix:block/diesel_engine_flywheel'}


def diesel_engine():
    D.model('diesel_engine', {'parent': 'minecraft:block/block', 'textures': ENGINE_TEXTURES,
                              'elements': engine_elements()})
    D.model('diesel_engine/flywheel', {'textures': FLYWHEEL_TEXTURES, 'elements': flywheel_elements('y')})
    # the item shows the flywheel on its shaft, lying across the engine
    D.item_model('diesel_engine', {'parent': 'minecraft:block/block', 'textures': {**ENGINE_TEXTURES,
                                                                                   **FLYWHEEL_TEXTURES},
                                   'elements': engine_elements() + flywheel_elements('x')})
    turn = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    variants = {}
    for facing, y in turn.items():
        v = {'model': 'mcremix:block/diesel_engine'}
        if y:
            v['y'] = y
        variants[f'facing={facing}'] = v
    D.states('diesel_engine', {'variants': variants})
    G.write_json(G.data('mcremix', 'loot_tables', 'blocks', 'diesel_engine.json'), G.loot_self('diesel_engine'))


# ------------------------------------------------------------------------------------------------ recipes

VANILLA_WAXABLE = ['copper_block', 'cut_copper', 'cut_copper_stairs', 'cut_copper_slab']


def recipes():
    item = G.item
    # crude oil split, lightest first: sour gas goes off as a gas (the still's vent, the column condenser's gas
    # outlet), bitumen is what is left; Hot, as coal tar, 30 seconds a bucket in the still
    G.recipe('distilling/crude_oil', {
        'type': 'mcremix:distilling', 'heat': 'hot', 'processingTime': 600,
        'ingredients': [{'fluid': 'mcremix:crude_oil', 'amount': 1000}],
        'results': [{'fluid': f'mcremix:{f}', 'amount': a} for f, a in
                    (('sour_gas', 50), ('naphtha', 250), ('kerosene', 150), ('diesel', 200), ('heavy_oil', 200),
                     ('bitumen', 150))],
        'boiling_points': [20, 100, 210, 300, 375, 400], 'close': [False] * 5})
    # naphtha cracked with steam at Searing: ethylene, and fuel gas with it
    G.recipe('steel_furnace/naphtha_cracking', {
        'type': 'mcremix:steel_furnace', 'heat': 'searing', 'processingTime': 100,
        'ingredients': [{'fluid': 'mcremix:naphtha', 'amount': 250},
                        {'fluid': 'mcremix:pressurized_steam', 'amount': 100}],
        'results': [{'fluid': 'mcremix:ethylene', 'amount': 150}, {'fluid': 'mcremix:fuel_gas', 'amount': 100}]})
    # heavy oil left to stand cool drops its wax; what is left is fuel oil
    G.recipe('settling/paraffin_wax', {
        'type': 'mcremix:settling', 'processingTime': 200,
        'ingredients': [{'fluid': 'mcremix:heavy_oil', 'amount': 250}],
        'results': [item('mcremix:paraffin_wax'), {'fluid': 'mcremix:fuel_oil', 'amount': 225}]})
    # bitumen boiled down over Searing heat, as a coker drum does: petroleum coke, and no gas off it
    G.recipe('boiling/petroleum_coke', {
        'type': 'mcremix:boiling', 'heatRequirement': 'heated', 'processingTime': 200,
        'ingredients': [{'fluid': 'mcremix:bitumen', 'amount': 250}], 'results': [item('mcremix:petroleum_coke', 2)]})
    # paraffin wax does what honeycomb does: a candle on string, and vanilla's waxed copper
    G.recipe('crafting/candle_from_paraffin_wax', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['S', 'W'],
        'key': {'S': item('minecraft:string'), 'W': item('mcremix:paraffin_wax')}, 'result': item('minecraft:candle')})
    for stage in ('', 'exposed_', 'weathered_', 'oxidized_'):
        for block in VANILLA_WAXABLE:
            name = stage + block
            if name == 'oxidized_copper_block':
                name = 'oxidized_copper'
            elif name in ('exposed_copper_block', 'weathered_copper_block'):
                name = name.replace('_block', '')
            G.recipe(f'waxing/waxed_{name}_from_paraffin_wax', {
                'type': 'minecraft:crafting_shapeless', 'category': 'building',
                'ingredients': [item(f'minecraft:{name}'), item('mcremix:paraffin_wax')],
                'result': item(f'minecraft:waxed_{name}')})
    # the engine: Create's flywheel, a piston for the cylinder, a steel tank for fuel, in steel plate
    G.recipe('diesel_engine', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['SPS', 'FTS'],
        'key': {'S': {'tag': 'forge:plates/steel'}, 'P': item('create:mechanical_piston'),
                'F': item('create:flywheel'), 'T': item('mcremix:steel_fluid_tank')},
        'result': item('mcremix:diesel_engine')})


def tags():
    m = lambda names: [f'mcremix:{n}' for n in names]
    D.add_tag('mcremix', 'items', 'coke', m(['coal_coke', 'petroleum_coke']))
    D.add_tag('mcremix', 'items', 'waxes', ['minecraft:honeycomb', 'mcremix:paraffin_wax'])
    # the engine runs on a cut drawn hot as it does on the cold one
    D.add_tag('mcremix', 'fluids', 'diesel', m(['diesel', 'hot_diesel']))
    D.add_tag('mcremix', 'fluids', 'kerosene', m(['kerosene', 'hot_kerosene']))
    D.add_tag('minecraft', 'blocks', 'mineable/pickaxe', ['mcremix:diesel_engine'])


def damage_types():
    # the hydrogen sulfide in sour gas (SourGasCloud), and the sulfur dioxide off it burnt (SulfurDioxideCloud); armour
    # does nothing for either (gen_assets puts both in bypasses_armor)
    for name in ('sour_gas', 'sulfur_dioxide'):
        G.write_json(G.data('mcremix', 'damage_type', name + '.json'),
                     {'message_id': f'mcremix.{name}', 'scaling': 'when_caused_by_living_non_player',
                      'exhaustion': 0.0})


def worldgen():
    """
    An oil pocket in about every 6 by 6 chunks (Taran's pick), between y -40 and -16, under every Overworld biome. It
    goes in with the underground decoration, after caves and ancient cities are cut, so it can seal itself off from
    them.
    """
    G.write_json(G.data('mcremix', 'worldgen', 'configured_feature', 'oil_pocket.json'),
                 {'type': 'mcremix:oil_pocket', 'config': {}})
    G.write_json(G.data('mcremix', 'worldgen', 'placed_feature', 'oil_pocket.json'), {
        'feature': 'mcremix:oil_pocket',
        'placement': [{'type': 'minecraft:rarity_filter', 'chance': 36}, {'type': 'minecraft:in_square'},
                      {'type': 'minecraft:height_range',
                       'height': {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': -40},
                                  'max_inclusive': {'absolute': -16}}},
                      {'type': 'minecraft:biome'}]})
    G.write_json(G.data('mcremix', 'forge', 'biome_modifier', 'oil_pocket.json'), {
        'type': 'forge:add_features', 'biomes': '#minecraft:is_overworld', 'features': 'mcremix:oil_pocket',
        'step': 'underground_decoration'})


# ------------------------------------------------------------------------------------------------ structures

def pipe(*sides):
    return ('create:fluid_pipe', dict({d: 'true' if d in sides else 'false'
                                       for d in ('north', 'south', 'east', 'west', 'up', 'down')}, waterlogged='false'))


def ponder_schematics():
    """
    Ponder looks from the north, so each scene's front is its north side. oil/pocket: a 7x7 plate under four layers of
    deepslate, its front two rows left out so the pocket shows: crude oil three across and two deep in the middle of
    the lower two layers, open to the front, a shaft of air from it up to a hose pulley on top facing north (its pipe
    side west, its shaft east), a creative motor east of it, and west of it a pump turned through a cogwheel by a
    second motor, into a tank. diesel_engine/intro: a 5x5 plate, an engine in the middle facing north, a hand crank on
    its west side, a shaft out of its east side into a gearbox, and a tank behind it piped into its back.
    steel_furnace/cracking: a 5x5 plate, a Steel Furnace facing north with a tank piped into each side, and a pipe up
    out of its top and away to the back.
    """
    deep = ('minecraft:deepslate', {'axis': 'y'})
    oil = ('mcremix:crude_oil', {'level': '0'})
    tank = lambda pos: ('create:fluid_tank', {'top': 'true', 'bottom': 'true', 'shape': 'window'},
                        G.lone_tank_nbt(pos))
    b = {}
    G.plate(b, 7)
    for y in range(1, 5):
        for x in range(7):
            for z in range(2, 7):
                b[(x, y, z)] = deep
    for y in (1, 2):
        for x in (2, 3, 4):
            for z in (2, 3, 4):
                b[(x, y, z)] = oil
    for y in (3, 4):
        b[(3, y, 3)] = ('minecraft:air', {})
    b[(3, 5, 3)] = ('create:hose_pulley', {'facing': 'north', 'waterlogged': 'false'})
    b[(4, 5, 3)] = ('create:creative_motor', {'facing': 'west'})
    b[(2, 5, 3)] = pipe('west', 'east')
    b[(1, 5, 3)] = ('create:mechanical_pump', {'facing': 'west', 'waterlogged': 'false'})
    b[(1, 5, 4)] = ('create:cogwheel', {'axis': 'x', 'waterlogged': 'false'})
    b[(2, 5, 4)] = ('create:creative_motor', {'facing': 'west'})
    b[(0, 5, 3)] = tank((0, 5, 3))
    G.save_structure('oil/pocket', (7, 7, 7), b)

    b = {}
    G.plate(b)
    b[(2, 1, 2)] = ('mcremix:diesel_engine', {'facing': 'north'})
    b[(1, 1, 2)] = ('create:hand_crank', {'facing': 'west', 'waterlogged': 'false'})
    b[(3, 1, 2)] = ('create:shaft', {'axis': 'x', 'waterlogged': 'false'})
    b[(4, 1, 2)] = ('create:gearbox', {'axis': 'y'})
    b[(2, 1, 3)] = pipe('north', 'south')
    b[(2, 1, 4)] = tank((2, 1, 4))
    G.save_structure('diesel_engine/intro', (5, 3, 5), b)

    b = {}
    G.plate(b)
    b[(2, 1, 2)] = ('mcremix:steel_furnace', {'chimney': 'true', 'facing': 'north', 'heat': 0, 'lit': 'true'})
    b[(1, 1, 2)] = pipe('west', 'east')
    b[(3, 1, 2)] = pipe('west', 'east')
    for x in (0, 4):
        b[(x, 1, 2)] = tank((x, 1, 2))
    b[(2, 2, 2)] = pipe('down', 'south')
    b[(2, 2, 3)] = pipe('north', 'south')
    G.save_structure('steel_furnace/cracking', (5, 3, 5), b)


# ------------------------------------------------------------------------------------------------ lang

def lang():
    text = {
        # items
        'item.mcremix.petroleum_coke': 'Petroleum Coke',
        'item.mcremix.petroleum_coke.tooltip.summary': (
            'Bitumen boiled down in a _basin_ over _Searing_ heat. Works wherever _Coal Coke_ does: it burns _Searing_, '
            'makes _steel_, and goes into _cementation packs_, _Carbon Electrodes_ and _Rutile Briquettes_.'),
        'item.mcremix.paraffin_wax': 'Paraffin Wax',
        'item.mcremix.paraffin_wax.tooltip.summary': (
            'Drops out of _heavy oil_ left to stand cool. Does what _honeycomb_ does: _waxes_ copper and steel, and '
            'makes a _candle_ on string.'),
        # the engine
        'block.mcremix.diesel_engine': 'Diesel Engine',
        'block.mcremix.diesel_engine.tooltip.summary': (
            'An old stationary engine: one cylinder, a _flywheel_ and a smoke stack. Pipe _diesel_ or _kerosene_ into '
            'its _back_ or _bottom_, or pour it in by bucket. It gives _32 RPM_ and _16,384 su_, a whole steam '
            'engine\'s worth, burning a bucket of diesel every _50 seconds_. Kerosene gives _three quarters_ of that.'),
        'block.mcremix.diesel_engine.tooltip.condition1': 'Starting',
        'block.mcremix.diesel_engine.tooltip.behaviour1': (
            'It must be _cranked_: with fuel in it, turn its shaft at _8 RPM_ or more, by a _hand crank_ or any turning '
            'shaft, for _2 seconds_, and it catches, turning the way it was cranked.'),
        'block.mcremix.diesel_engine.tooltip.condition2': 'Stalling',
        'block.mcremix.diesel_engine.tooltip.behaviour2': (
            'It _stalls_ when it runs _dry_, or when what it drives is _too much_ for it, and must be cranked again.'),
        'mcremix.goggles.diesel_engine.running': 'Running on %s',
        'mcremix.goggles.diesel_engine.running_weaker': 'Running on %s: three quarters of its power',
        'mcremix.goggles.diesel_engine.stalled_dry': 'Stalled: it ran dry',
        'mcremix.goggles.diesel_engine.stalled_overloaded': 'Stalled: what it drives was too much for it',
        'mcremix.goggles.diesel_engine.no_fuel': 'Needs diesel or kerosene',
        'mcremix.goggles.diesel_engine.catching': 'Catching: %s%%',
        'mcremix.goggles.diesel_engine.crank': 'Crank it to start: its shaft at %s RPM or more for %s seconds',
        'mcremix.goggles.diesel_engine.fuel': '%s %s / %s mB, %s seconds',
        # gases
        'entity.mcremix.sour_gas_cloud': 'Sour Gas',
        'entity.mcremix.fuel_gas_cloud': 'Fuel Gas',
        'entity.mcremix.sulfur_dioxide_cloud': 'Sulfur Dioxide',
        'death.attack.mcremix.sour_gas': '%1$s breathed in sour gas',
        'death.attack.mcremix.sour_gas.player': '%1$s breathed in sour gas while fighting %2$s',
        'death.attack.mcremix.sulfur_dioxide': '%1$s choked on sulfur dioxide',
        'death.attack.mcremix.sulfur_dioxide.player': '%1$s choked on sulfur dioxide while fighting %2$s',
        # the Steel Furnace's second feed and new fuels
        'mcremix.goggles.steel_furnace.waiting_for': 'Needs %s mB of %s with it',
        'block.mcremix.steel_furnace.tooltip.behaviour1': (
            'Pipe or pour a fluid it works into its _front_, _sides_ or _top_ and it waits in the _feed tank_; a '
            'second fluid a job takes waits in a tank of its own. What it gives waits there for a _pipe_ to draw it '
            'off. _Ethanol_ over _alumina_ in the chamber gives _ethylene_ at _Hot_; _ethylene dichloride_ cracks into '
            '_vinyl chloride_ and _hydrogen chloride_ at _Searing_; _naphtha_ with _steam_ cracks into _ethylene_ and '
            '_fuel gas_ at _Searing_.'),
        'block.mcremix.steel_furnace.tooltip.behaviour2': (
            'Pump _wood spirit_, _ethanol_, _acetone_, _light oil_, _creosote_, _coal tar_, _fuel oil_, _sour gas_, '
            '_hydrogen_, _ethylene_ or _fuel gas_ in from _underneath_ and _light_ it: the faster the pump, the '
            '_hotter_ it burns, up to what that fuel can reach, and anything faster is _lost_. Only _hydrogen_, '
            '_ethylene_ and _fuel gas_ reach _Blazing_. _Sour gas_ puffs out _sulfur dioxide_, which chokes and kills '
            'plants. Unlit, gas fuel _escapes_ out of the front and liquid fuel _spills_ out. It takes no solid fuel, '
            'no heat from below and no bellows.'),
        # the column's gas outlet
        'mcremix.goggles.steel_column.gas_over': '%s: a gas, over the top to the condenser',
        'mcremix.goggles.steel_condenser.gas_venting': '%s mB of gas a second escaping: its gas outlet is full',
        # JEI
        'mcremix.jei.distilling.gas': 'A gas: it leaves the still by the cow receiver\'s vent, and a steel column '
                                      'through its condenser, to pipe off from its ends',
        'mcremix.jei.steel_furnace_fuel.sour': 'Puffs out a mB of sulfur dioxide for every %s mB burnt',
        'mcremix.jei.diesel_engine': 'Diesel Engine',
        'mcremix.jei.diesel_engine.piped': 'Piped into its back or bottom, or poured in',
        'mcremix.jei.diesel_engine.speed': '%s RPM',
        'mcremix.jei.diesel_engine.capacity': '%s su',
        'mcremix.jei.diesel_engine.burns': '%s mB a tick: %ss a bucket',
        'mcremix.jei.diesel_engine.crank': 'Crank it to start',
    }
    D.merge_json(G.asset('lang', 'en_us.json'), lambda obj: obj.update(text))

"""
Resources for coiling (0.37.0, Taran): the coil winder in andesite, copper, iron, steel, aluminium and titanium
(registry/ModCoiling.java), which winds Power Grid's copper and resistive coils in place of their hand recipes: its
models, the wire's textures, blockstates, recipes, tags, loot tables, Ponder structure and text (the Ponder scene's
text is gen_assets' ponder_lang's, from CoilingScenes.java). gen_assets.py calls
run() after gen_dyes, passing itself; this file adds to the lang and tag files written before it rather than owning any
shared file.

The winder is drawn facing south, its open front, and turned by its blockstate. Its shaft runs east to west through
its two cheeks, and the bobbin on it is drawn upright, as Create draws a shaft, for the renderer to turn to the shaft's
axis with it. The bobbin and its wire are octagons, so they read as round as they turn.
"""
import math
import random

from PIL import Image

import gen_decor as D

G = None  # gen_assets, set by run()

# slowest first, as ModCoiling.METALS
METALS = ['andesite', 'copper', 'iron', 'steel', 'aluminium', 'titanium']
# the depths the wire fills a bobbin through (CoilWinderRenderer.WIRE_STEPS), each a quarter of a pixel deeper
WIRE_STEPS = 7
# the bobbin, in pixels: how far out from its axis its barrel and its two flanges reach, and where along it they run
BARREL, FLANGE = 3, 5
BARREL_SPAN = (4, 12)
FLANGE_SPANS = ((3, 4), (12, 13))
TAN = math.tan(math.radians(22.5))
COPPER_WIRE = (0xc8, 0x79, 0x4a)
FACINGS = (('south', 0), ('west', 90), ('north', 180), ('east', 270))


def run(gen):
    global G
    G = gen
    textures()
    models()
    blockstates()
    recipes()
    tags()
    loot()
    ponder_schematics()
    lang()


def winder(metal):
    """A winder's id: coil_winder for andesite, as Create's own machines go unnamed by their andesite."""
    return 'coil_winder' if metal == 'andesite' else f'{metal}_coil_winder'


def looks(metal):
    """{patina: look} for a winder in `metal`; andesite and aluminium look the same whatever it says, and titanium's
    patina keeps its tint."""
    if metal in ('andesite', 'aluminium'):
        return {patina: metal for patina in G.PATINAS}
    return {patina: G.stage_prefix(metal, stage) + metal for patina, stage in G.PATINAS.items()}


def every_look():
    out = []
    for metal in METALS:
        for look in looks(metal).values():
            if look not in out:
                out.append(look)
    return out


# ------------------------------------------------------------------------------------------------ textures

def wire_texture(seed, colour=None, across=False):
    """Turns of wire side by side, a pixel each, every other one in the light: grey, for the renderer to tint the
    colour of what is wound, or `colour` for the item. Across, the turns run up the texture rather than along it."""
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            turn = x if across else y
            v = max(0, min(255, (238 if turn % 2 == 0 else 184) + rnd.randint(-8, 8)))
            rgb = (v, v, v) if colour is None else tuple(v * c // 255 for c in colour)
            img.putpixel((x, y), rgb + (255,))
    return img


def textures():
    G.save_png(wire_texture(3700), G.asset('textures', 'block', 'coil_winder', 'wire.png'))
    G.save_png(wire_texture(3701, COPPER_WIRE, across=True),
               G.asset('textures', 'block', 'coil_winder', 'copper_wire.png'))


# ------------------------------------------------------------------------------------------------ models

def octagon(radius, span, rim, ends=None, axis='y'):
    """
    A round bar or disc `radius` pixels out from the middle of the block, from span[0] to span[1] along `axis` ('y'
    upright, as Create draws a shaft, or 'x'): a regular octagon of four boxes, two square to the block and two turned
    45 degrees about the axis, each giving two of its eight sides (`rim`). The four lie over one another at its ends,
    so `ends` is a face of one colour there, or None to leave them off where something covers them.
    """
    across = radius * TAN
    i = 'xyz'.index(axis)
    cross = [j for j in range(3) if j != i]
    names = {0: ('west', 'east'), 1: ('down', 'up'), 2: ('north', 'south')}
    out = []
    for wide in cross:
        frm, to = [0, 0, 0], [0, 0, 0]
        frm[i], to[i] = span
        for j in cross:
            reach = radius if j == wide else across
            frm[j], to[j] = round(8 - reach, 4), round(8 + reach, 4)
        for turned in (False, True):
            faces = {d: dict(rim) for d in names[wide]}
            if ends:
                faces.update({d: dict(ends) for d in names[i]})
            box = G.box(list(frm), list(to), faces)
            if turned:
                box['rotation'] = {'origin': [8, 8, 8], 'axis': axis, 'angle': 45}
            out.append(box)
    return out


def bobbin_elements(axis='y'):
    """A turned spruce bobbin: its barrel, and a flange at each end a pixel thick."""
    rim = {'texture': '#bobbin'}
    ends = {'texture': '#bobbin_end', 'uv': [7, 7, 8, 8]}
    elements = octagon(BARREL, BARREL_SPAN, rim, axis=axis)
    for span in FLANGE_SPANS:
        elements += octagon(FLANGE, span, rim, ends, axis=axis)
    return elements


BOBBIN_TEXTURES = {'bobbin': 'minecraft:block/stripped_spruce_log',
                   'bobbin_end': 'minecraft:block/stripped_spruce_log_top'}


def wire_elements(step, axis='y', tint=True):
    """The wire `step` quarter pixels deep on the bobbin's barrel, between its flanges, which cover its ends."""
    rim = {'texture': '#wire'}
    if tint:
        rim['tintindex'] = 0
    return octagon(BARREL + step / 4, BARREL_SPAN, rim, axis=axis)


def guide_elements():
    """The wire guide, a brass block running along the rod at the winder's front, an eye under it for the wire."""
    return [G.box([7, 11.5, 12.5], [9, 13.5, 14.5], G.all_faces('#guide')),
            G.box([7.5, 10.75, 13], [8.5, 11.5, 14], G.all_faces('#guide', skip=('up',)))]


def frame_elements():
    """
    The winder's frame, facing south: a bed of its casing, a cheek either side holding its shaft, a bearing boss on
    the outside of each round the shaft, which shows half a pixel past it, a tie bar across the back and, at the
    front, the rod its wire guide runs along. Everything clears the bobbin's flanges as they turn.
    """
    C, A = '#casing', '#axis'
    face = G.face
    out = [G.box([0, 0, 0], [16, 2, 16], {'north': face(C, 'north'), 'south': face(C, 'south'),
                                          'east': face(C, 'east'), 'west': face(C, 'west'), 'up': face(C),
                                          'down': face(C, 'down')})]
    for x0, x1 in ((1, 2.5), (13.5, 15)):
        out.append(G.box([x0, 2, 2], [x1, 14, 15], G.all_faces(C, skip=('down',))))
    boss = {'texture': A, 'uv': [6, 5, 10, 11]}
    for x0, x1, outside in ((0.5, 1, 'west'), (15, 15.5, 'east')):
        out.append(G.box([x0, 5, 5], [x1, 11, 11], {d: dict(boss) for d in (outside, 'north', 'south', 'up', 'down')}))
    out.append(G.box([2.5, 12, 2], [13.5, 14, 3.5], {d: face(C) for d in ('north', 'south', 'up', 'down')}))
    rod = {'texture': A, 'uv': [7, 2.5, 8, 13.5], 'rotation': 90}
    out.append(G.box([2.5, 12, 13], [13.5, 13, 14], {d: dict(rod) for d in ('north', 'south', 'up', 'down')}))
    return out


def frame_textures(look):
    casing = 'create:block/andesite_casing' if look == 'andesite' else f'mcremix:block/{look}_casing'
    # titanium's tints are on what holds a part, not on its rods
    metal = 'titanium' if look.endswith('titanium') else look
    axis = 'create:block/axis' if look == 'andesite' else f'mcremix:block/{metal}_axis'
    return {'casing': casing, 'axis': axis, 'particle': casing}


def shaft_textures(metal):
    if metal == 'andesite':
        return {'shaft': 'create:block/axis', 'shaft_end': 'create:block/axis_top'}
    return {'shaft': f'mcremix:block/{metal}_axis', 'shaft_end': f'mcremix:block/{metal}_axis_top'}


def shaft_across():
    """The shaft through the item, east to west, as the renderer draws it in the block."""
    side = {'texture': '#shaft', 'uv': [6, 0, 10, 16], 'rotation': 90}
    end = {'texture': '#shaft_end', 'uv': [6, 6, 10, 10]}
    return [G.box([0, 6, 6], [16, 10, 10], dict({d: dict(side) for d in ('north', 'south', 'up', 'down')},
                                                east=dict(end), west=dict(end)))]


def models():
    folder = lambda *path: G.asset('models', 'block', 'coil_winder', *path)
    G.write_json(folder('frame.json'), {'elements': frame_elements()})
    for look in every_look():
        G.write_json(folder(look + '.json'), {'parent': 'mcremix:block/coil_winder/frame',
                                              'textures': frame_textures(look)})
    G.write_json(folder('bobbin.json'), {'textures': dict(BOBBIN_TEXTURES, particle=BOBBIN_TEXTURES['bobbin']),
                                         'elements': bobbin_elements()})
    for step in range(1, WIRE_STEPS + 1):
        G.write_json(folder(f'wire_{step}.json'), {
            'textures': {'wire': 'mcremix:block/coil_winder/wire', 'particle': 'mcremix:block/coil_winder/wire'},
            'elements': wire_elements(step)})
    G.write_json(folder('guide.json'), {
        'textures': {'guide': 'create:block/brass_block', 'particle': 'create:block/brass_block'},
        'elements': guide_elements()})
    # the item: the winder in its fresh metal, its shaft and bobbin across it and a copper coil half wound, front on
    for metal in METALS:
        textures = dict(frame_textures(metal), **shaft_textures(metal), **BOBBIN_TEXTURES,
                        wire='mcremix:block/coil_winder/copper_wire', guide='create:block/brass_block')
        G.write_json(G.asset('models', 'item', winder(metal) + '.json'), {
            'parent': 'minecraft:block/block',
            'textures': textures,
            'elements': frame_elements() + shaft_across() + bobbin_elements('x')
            + wire_elements(4, 'x', tint=False) + guide_elements(),
            'display': {'gui': {'rotation': [30, 45, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]}}})


def blockstates():
    for metal in METALS:
        variants = {}
        for facing, turn in FACINGS:
            for patina, look in looks(metal).items():
                variant = {'model': f'mcremix:block/coil_winder/{look}'}
                if turn:
                    variant['y'] = turn
                variants[f'facing={facing},patina={patina}'] = variant
        D.states(winder(metal), {'variants': variants})


# ------------------------------------------------------------------------------------------------ data

def part(metal, name):
    """A winder's parts in its metal: Create's own for andesite."""
    return f'create:{name}' if metal == 'andesite' else f'mcremix:{metal}_{name}'


def casing(metal):
    return 'create:andesite_casing' if metal == 'andesite' else G.TIER_CASINGS[metal]


def recipes():
    item = G.item
    # a shaft to turn the bobbin on, over its metal's casing, over the cog that drives the wire guide
    for metal in METALS:
        G.recipe(winder(metal), {
            'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['S', 'C', 'G'],
            'key': {'S': item(part(metal, 'shaft')), 'C': item(casing(metal)), 'G': item(part(metal, 'cogwheel'))},
            'result': item('mcremix:' + winder(metal))})
    # Power Grid's two coils, from what their hand recipes took, wound in place of them; a copper coil takes 2.5
    # seconds at andesite's 128 RPM, a resistive one, with less wire on a thinner stick, under 2
    G.recipe('coiling/copper_coil', {
        'type': 'mcremix:coiling', 'processingTime': 400, 'colour': '#c8794a',
        'ingredients': [{'tag': 'forge:wires/copper'}] * 4 + [item('minecraft:stick')] + [item('create:cardboard')] * 2,
        'results': [item('powergrid:copper_coil')]})
    G.recipe('coiling/resistive_coil', {
        'type': 'mcremix:coiling', 'processingTime': 300, 'colour': '#8f9294',
        'ingredients': [{'tag': 'forge:wires/iron'}] * 4 + [item('minecraft:stick')],
        'results': [item('powergrid:resistive_coil')]})
    G.disabled('powergrid', 'crafting/copper_coil')
    G.disabled('powergrid', 'crafting/resistive_coil')


def tags():
    D.add_tag('minecraft', 'blocks', 'mineable/pickaxe', ['mcremix:' + winder(m) for m in METALS])


def loot():
    for metal in METALS:
        G.write_json(G.data('mcremix', 'loot_tables', 'blocks', winder(metal) + '.json'), G.loot_self(winder(metal)))


def ponder_schematics():
    """
    coil_winder/intro: a 5x5 plate; a coil winder facing north at (2,1,2), driven from the west by a creative motor at
    (0,1,2) through a shaft at (1,1,2), and an andesite funnel on its east side at (3,1,2) to take its coils.
    """
    b = {}
    G.plate(b, 5)
    b[(0, 1, 2)] = ('create:creative_motor', {'facing': 'east'})
    b[(1, 1, 2)] = ('create:shaft', {'axis': 'x', 'waterlogged': 'false'})
    b[(2, 1, 2)] = ('mcremix:coil_winder', {'facing': 'north', 'patina': 'none'})
    b[(3, 1, 2)] = ('create:andesite_funnel', {'facing': 'east', 'extracting': 'true', 'powered': 'false',
                                               'waterlogged': 'false'})
    G.save_structure('coil_winder/intro', (5, 3, 5), b)


# ------------------------------------------------------------------------------------------------ text

def lang():
    text = {}
    speeds = {'andesite': 128, 'copper': 256, 'iron': 256, 'steel': 512, 'aluminium': 512, 'titanium': 2048}
    for metal in METALS:
        key = 'block.mcremix.' + winder(metal)
        text[key] = G.title_case(winder(metal))
        text[key + '.tooltip.summary'] = (
            'Winds Power Grid\'s _coils_ from _wire_ on a bobbin turned by its _shaft_, which goes in at _either side_ '
            'of its open front. It winds _faster the faster it turns_, up to _%d RPM_ in %s.'
            % (speeds[metal], 'andesite' if metal == 'andesite' else metal))
        text[key + '.tooltip.condition1'] = 'What it winds'
        text[key + '.tooltip.behaviour1'] = (
            '_Four Copper Wire_, a _Stick_ and _two Cardboard_ into a _Copper Coil_; _four Iron Wire_ and a _Stick_ '
            'into a _Resistive Coil_. They go in by _hand_, or by _funnel_, _chute_ or _hopper_ from any side, a kind '
            'of thing to each of its _four slots_. Take the coils with an _empty hand_ or a _funnel_.')
        text[key + '.tooltip.condition2'] = 'How fast'
        text[key + '.tooltip.behaviour2'] = (
            'A Copper Coil takes _2.5 seconds at 128 RPM_, half that at 256 and so on. Each metal turns as fast as its '
            'shafts: _andesite 128 RPM_, _copper and iron 256_, _steel and aluminium 512_, _titanium 2048_.')
    g = 'mcremix.goggles.coil_winder.'
    text.update({
        g + 'winding': 'Winding %s, %s%%',
        g + 'still': 'Holds enough for %s, but is not turning',
        g + 'full': 'Full of coils: take them out',
        g + 'empty': 'Empty: give it wire to wind',
        g + 'waiting': 'Still wants %s',
        g + 'holds': 'Holds %s %s',
        g + 'made': 'Wound %s %s',
        'mcremix.goggles.wants.count': '%s %s',
        'mcremix.goggles.wants.comma': ', ',
        'mcremix.goggles.wants.and': ' and ',
        'mcremix.jei.coiling': 'Coiling',
        'mcremix.jei.coiling.time': '%ss at %s RPM',
        'mcremix.jei.coiling.speed': (
            'At andesite\'s top speed. It winds as fast as it turns: twice as fast at 256 RPM, a copper or iron '
            'winder\'s top speed, and four times at 512, steel\'s and aluminium\'s'),
    })
    D.merge_json(G.asset('lang', 'en_us.json'), lambda obj: obj.update(text))

"""
Power Grid's parts made as real ones are (0.40.0, Taran, 2026-11-25: "make the recipes more realistic"): the survey is
design/power-grid-recipes.md and Taran's cards are in project memory. Wire is drawn through a die on the Coil Winder,
insulated in PVC; magnets are hardened steel; generators are laminated steel with carbon brushes and a Bakelite hub;
resistors are carbon in Bakelite, varistors zinc oxide; capacitors are aluminium foil rolled up in waxed paper; the
Battery is Volta's pile, wet with brine; the tubes are filled with real gases; and chips and solar panels are made on
silicon wafers, from quartz reduced with coke in the Arc Furnace and purified the real way (Taran's pick): worked into
trichlorosilane with hydrogen chloride, distilled, laid down as polysilicon by hydrogen, cast and sawn.

The new items are registry/ModPowerGrid.java and the new fluids fluids/Chemical.java; the gas seals themselves are
gen_vacuum's, the trace of neon off the air column gen_air's, and the electric motor and integrated circuit
gen_assets' gen_power_grid_recipes. gen_assets.py calls run() last, passing itself; this file adds to the lang and tag
files written before it rather than owning any shared file.
"""
import math
import random

from PIL import Image

import gen_decor as D

G = None  # gen_assets, set by run()

ITEMS = ['zinc_oxide', 'phosphor', 'quartz_briquette', 'crude_silicon', 'polysilicon', 'silicon_ingot',
         'silicon_wafer']
# what each metal's wire is called in Power Grid, and its colour on the bobbin as the winder draws it
WIRES = (('copper', 'powergrid:wire', '#c8794a'), ('iron', 'powergrid:iron_wire', '#8f9294'),
         ('gold', 'powergrid:golden_wire', '#e8b830'))
# silicon's nine shades, darkest first, as Create's ingots are drawn: a dark, faintly blue metallic grey
SILICON_SHADES = [((c >> 16) & 255, (c >> 8) & 255, c & 255) for c in (
    0x14161D, 0x1E212B, 0x2A2E3B, 0x383D4D, 0x494F61, 0x5D6478, 0x767E93, 0x99A2B6, 0xC8CFDE)]


def run(gen):
    global G
    G = gen
    textures()
    battery()
    recipes()
    lang()


# ------------------------------------------------------------------------------------------------ textures

def wafer_texture(seed):
    """A sawn silicon wafer from above: a mirror-grey disc with its flat at the bottom, darker to the lower right, a
    band of sky light across it and a faint violet bloom where the light glances off, its rim a pixel darker."""
    rnd = random.Random(seed)
    shadow, base, light, bloom = (0x30, 0x35, 0x46), (0x76, 0x80, 0x98), (0xE8, 0xEE, 0xFA), (0x8E, 0x7E, 0xC8)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    cx, cy, r = 7.5, 7.0, 6.6
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d > r or y > 12:
                continue
            t = 0.55 - ((x - cx) + (y - cy)) / (3.2 * r) + rnd.uniform(-0.03, 0.03)
            col = G.mix(shadow, base, min(1.0, t * 1.6)) if t < 0.62 else G.mix(base, light, min(1.0, (t - 0.62) * 2))
            # the band of light, running from the lower left to the upper right, and a thinner one beside it
            along = (x - cx) + (y - cy)
            if abs(along + 2.5) < 1.0:
                col = G.mix(col, light, 0.7)
            elif abs(along - 1.5) < 0.6:
                col = G.mix(col, light, 0.35)
            elif along > 4:
                col = G.mix(col, bloom, 0.25)
            if d > r - 1.0 or y == 12:
                col = G.mix(col, (0x10, 0x12, 0x18), 0.5)
            img.putpixel((x, y), col + (255,))
    return img


def textures():
    # zinc oxide: a white powder with a faint lavender shadow, zinc's colour; phosphor: willemite's pale green
    G.save_png(G.powder_texture(4801, (0x9A, 0x9A, 0xB0), (0xE2, 0xE2, 0xEC), (0xFF, 0xFF, 0xFF), clumps=0.2),
               G.asset('textures', 'item', 'zinc_oxide.png'))
    G.save_png(G.powder_texture(4802, (0x5E, 0x8E, 0x4E), (0xB4, 0xE6, 0x96), (0xEE, 0xFF, 0xDC), clumps=0.1),
               G.asset('textures', 'item', 'phosphor.png'))
    # the briquette: black coke flecked with white quartz, as the rutile briquette is with rutile's red
    G.save_png(G.lump_texture(4803, (0x14, 0x12, 0x12), (0x5A, 0x56, 0x54), accent=(0xEE, 0xE8, 0xE0),
                              accent_chance=0.18), G.asset('textures', 'item', 'quartz_briquette.png'))
    # crude silicon: dull blue-grey metal lumps; polysilicon: brighter, its crystals glinting
    G.save_png(G.lump_texture(4804, (0x26, 0x29, 0x32), (0x8C, 0x94, 0xA6), accent=(0x4A, 0x3A, 0x30),
                              accent_chance=0.1), G.asset('textures', 'item', 'crude_silicon.png'))
    G.save_png(G.lump_texture(4805, (0x40, 0x46, 0x54), (0xC4, 0xCC, 0xD8), accent=(0xF4, 0xF8, 0xFF),
                              accent_chance=0.16), G.asset('textures', 'item', 'polysilicon.png'))
    G.save_png(G.create_item_texture('ingot', SILICON_SHADES), G.asset('textures', 'item', 'silicon_ingot.png'))
    G.save_png(wafer_texture(4806), G.asset('textures', 'item', 'silicon_wafer.png'))
    for name in ITEMS:
        D.item_model(name, {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'mcremix:item/{name}'}})


# ------------------------------------------------------------------------------------------------ sodium-sulfur battery

# Taran's ask with the battery card (2026-11-25): a high-capacity battery of sodium and sulfur, built to the answers of
# its four cards (project memory, sodium-sulfur-battery-cards)
BATTERY = 'sodium_sulfur_battery'
BATTERY_ITEMS = ['unfired_beta_alumina_tube', 'beta_alumina_tube', 'incomplete_sodium_sulfur_battery']
# Taran's look card: Power Grid's Battery as it is, its copper ribs sulfur yellow and its frame dark steel. Its side
# and top, and their connected sheets, are kept in tools/powergrid_textures.json (Power Grid is Apache-2.0)
FACES = {'_side': 'battery_side', '_side_connected': 'battery_side_connected', '_top': 'battery_top',
         '_top_connected': 'battery_top_connected'}
# sulfur yellow for Create's eight shades of copper (gen_assets.COPPER_RAMP), darkest first, one for one: the ramp of
# the sulfur item (ModDyes.SULFUR) drawn out to eight
RIBS = [(126, 100, 10), (150, 122, 14), (172, 142, 18), (192, 160, 22), (210, 180, 30), (224, 198, 40),
        (238, 214, 56), (252, 236, 110)]
# how light Power Grid's grey frame is (its mean of red, green and blue) against a shade of dark steel
# (gen_decor.STEEL, 0 to 6): its darkest corner, its plain grey and its lit edge
FRAME_LIGHT = ((56, 0), (97, 3), (186, 6))
# beta-alumina, ivory once fired; pressed and unfired, a chalky grey (dark, base, light, for gen_assets' shade)
FIRED = ((0x9C, 0x92, 0x7A), (0xE4, 0xDC, 0xC4), (0xFF, 0xFB, 0xF0))
UNFIRED = ((0x80, 0x7E, 0x78), (0xBC, 0xBA, 0xB2), (0xDE, 0xDC, 0xD4))
SULFUR = ((0x9A, 0x6E, 0x08), (0xE8, 0xB4, 0x1C), (0xFF, 0xE0, 0x70))


def steel(i):
    """Dark steel (gen_decor.STEEL) at a shade from 0 to 6, between its own where it falls between."""
    i = max(0.0, min(6.0, i))
    lo = min(5, int(i))
    return G.mix(D.STEEL[lo], D.STEEL[lo + 1], i - lo)


def frame(colour):
    """A grey of Power Grid's frame in dark steel, as much lighter or darker than its neighbours as it was."""
    light = sum(colour[:3]) / 3
    (l0, s0), (l1, s1), (l2, s2) = FRAME_LIGHT
    if light <= l1:
        return steel(s0 + (s1 - s0) * (light - l0) / (l1 - l0))
    return steel(s1 + (s2 - s1) * (light - l1) / (l2 - l1))


def recoloured(name):
    """
    One of Power Grid's battery textures in the sodium-sulfur battery's colours: its copper ribs sulfur yellow, shade
    for shade; its grey frame dark steel; and the pale ribbed panels either side of them, greener than grey, as they
    are.
    """
    t = G.POWERGRID_TEXTURES[name]
    palette = []
    for r, g, b, a in t['palette']:
        if a == 0:
            palette.append((0, 0, 0, 0))
        elif r - g > 40:
            # copper: its own shade in Create's ramp, or the nearest
            near = min(range(len(G.COPPER_RAMP)), key=lambda i: sum(abs(c - d) for c, d in zip(G.COPPER_RAMP[i],
                                                                                               (r, g, b))))
            palette.append(RIBS[near] + (a,))
        elif g - b >= 12:
            palette.append((r, g, b, a))
        else:
            palette.append(frame((r, g, b)) + (a,))
    img = Image.new('RGBA', (len(t['rows'][0]), len(t['rows'])))
    for y, row in enumerate(t['rows']):
        for x, c in enumerate(row):
            img.putpixel((x, y), palette[G.INDEX_CHARS.index(c)])
    return img


def outline(img, shape, colour):
    for y in range(16):
        for x in range(16):
            if (x, y) not in shape and any((x + dx, y + dy) in shape for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                img.putpixel((x, y), colour + (255,))


def tube_texture(seed, palette, fired):
    """
    A beta-alumina tube lying corner to corner, closed and rounded at its upper end and open at its lower one, where
    its dark bore shows, lit along its upper left. An unfired one is chalky and flecked.
    """
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    ax, ay, bx, by = 3.6, 12.4, 11.4, 4.6
    dx, dy = bx - ax, by - ay
    length = math.hypot(dx, dy)
    shape = set()
    for y in range(16):
        for x in range(16):
            cx, cy = x + 0.5, y + 0.5
            t = max(0.0, min(1.0, ((cx - ax) * dx + (cy - ay) * dy) / (length * length)))
            d = math.hypot(cx - ax - t * dx, cy - ay - t * dy)
            if d >= 2.2:
                continue
            cross = (dx * (cy - ay) - dy * (cx - ax)) / length
            light = 0.55 - 0.4 * cross / 2.2 + rng.uniform(-0.04, 0.04)
            if not fired and rng.random() < 0.12:
                light += rng.choice((-0.2, 0.15))
            colour = G.shade(palette, light)
            if math.hypot(cx - ax - 0.3, cy - ay + 0.3) < 1.25:
                colour = G.mix(palette[0], (0, 0, 0), 0.55)
            img.putpixel((x, y), colour + (255,))
            shape.add((x, y))
    outline(img, shape, G.mix(palette[0], (0, 0, 0), 0.55))
    return img


def incomplete_battery_texture(seed):
    """A Steel Casing on the assembly line, seen from above the front: its open top shows the white beta-alumina
    tube standing in yellow sulfur, and its front the sulfur-yellow ribs the battery will have."""
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    shape = set()
    for y in range(4, 15):
        for x in range(3, 13):
            if y < 7:
                colour = steel(5 if y == 4 else 1.5)
                if 4 <= x <= 11 and y >= 5:
                    colour = G.shade(SULFUR, 0.35 + 0.3 * (y - 5) + rng.uniform(-0.05, 0.05))
            elif 6 <= x <= 9 and 8 <= y <= 13:
                colour = RIBS[(6, 2)[y % 2] + (1 if x == 6 else -1 if x == 9 else 0)]
            else:
                colour = steel(5 if x == 3 else 1.5 if x == 12 else 3.5 - (y == 14) + rng.uniform(-0.3, 0.3))
            img.putpixel((x, y), colour + (255,))
            shape.add((x, y))
    for x, y, t in ((7, 3, 0.9), (8, 3, 0.7), (7, 4, 0.75), (8, 4, 0.5), (7, 5, 0.55), (8, 5, 0.35)):
        img.putpixel((x, y), G.shade(FIRED, t) + (255,))
        shape.add((x, y))
    outline(img, shape, (0x14, 0x15, 0x17))
    return img


def battery():
    for face, theirs in FACES.items():
        D.png(recoloured(theirs), BATTERY + face)
    item_png = lambda img, name: G.save_png(img, G.asset('textures', 'item', name + '.png'))
    item_png(tube_texture(4813, UNFIRED, False), 'unfired_beta_alumina_tube')
    item_png(tube_texture(4814, FIRED, True), 'beta_alumina_tube')
    item_png(incomplete_battery_texture(4815), 'incomplete_sodium_sulfur_battery')
    D.model(BATTERY, {'parent': 'minecraft:block/cube_column', 'textures': {
        'side': f'mcremix:block/{BATTERY}_side', 'end': f'mcremix:block/{BATTERY}_top',
        'particle': f'mcremix:block/{BATTERY}_side'}})
    D.states(BATTERY, {'variants': {'': {'model': f'mcremix:block/{BATTERY}'}}})
    D.item_model(BATTERY, {'parent': f'mcremix:block/{BATTERY}'})
    for name in BATTERY_ITEMS:
        D.item_model(name, {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'mcremix:item/{name}'}})
    # Power Grid's Battery adds what charge a broken one held to the item this drops
    G.write_json(G.data('mcremix', 'loot_tables', 'blocks', BATTERY + '.json'), G.loot_self(BATTERY))
    D.add_tag('minecraft', 'blocks', 'mineable/pickaxe', ['mcremix:' + BATTERY])
    # the Ponder scene: a battery two blocks high, joined into one, on a magma block
    b = {}
    G.plate(b)
    b[(2, 1, 2)] = ('minecraft:magma_block', {})
    for y in (2, 3):
        nbt = {'id': ('string', 'mcremix:' + BATTERY), 'LastKnownPos': G.block_pos_nbt((2, y, 2))}
        if y == 2:
            nbt.update({'Size': ('int', 1), 'Height': ('int', 2)})
        else:
            nbt['Controller'] = G.block_pos_nbt((2, 2, 2))
        b[(2, y, 2)] = ('mcremix:' + BATTERY, {}, nbt)
    G.save_structure(BATTERY + '/intro', (5, 4, 5), b)


# ------------------------------------------------------------------------------------------------ recipes

def shaped(path, pattern, key, result, count=1, notify=True):
    obj = {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'key': key, 'pattern': pattern,
           'result': G.item(result, count)}
    if notify:
        obj['show_notification'] = True
    G.power_grid_recipe('crafting/' + path, obj)


def shapeless(path, ingredients, result, count=1):
    G.power_grid_recipe('crafting/' + path, {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                             'ingredients': ingredients, 'result': G.item(result, count)})


def mechanical(path, pattern, key, result):
    G.power_grid_recipe('mechanical_crafting/' + path, {'type': 'create:mechanical_crafting', 'acceptMirrored': True,
                                                        'key': key, 'pattern': pattern, 'result': G.item(result)})


def deploy(transitional, part):
    return {'type': 'create:deploying', 'ingredients': [transitional, part], 'results': [transitional]}


def step(kind, transitional, *more):
    return {'type': 'create:' + kind, 'ingredients': [transitional, *more], 'results': [transitional]}


def assembly(path, base, loops, results, sequence, transitional):
    G.power_grid_recipe('sequenced_assembly/' + path, {
        'type': 'create:sequenced_assembly', 'ingredient': base, 'loops': loops,
        'results': [dict(G.item(i), chance=float(c)) for i, c in results], 'sequence': sequence,
        'transitionalItem': transitional})


def recipes():
    item = G.item
    coke, bakelite, wax = {'tag': 'mcremix:coke'}, item('mcremix:bakelite'), {'tag': 'mcremix:waxes'}
    copper_wire, iron_wire, gold_wire = ({'tag': f'forge:wires/{m}'} for m in ('copper', 'iron', 'gold'))
    iron_plate, copper_plate = {'tag': 'forge:plates/iron'}, {'tag': 'forge:plates/copper'}
    steel_plate, aluminium_plate = {'tag': 'forge:plates/steel'}, {'tag': 'forge:plates/aluminium'}
    casing, coils, wafer = item('powergrid:conductive_casing'), {'tag': 'forge:copper_coils'}, item(
        'mcremix:silicon_wafer')
    pvc = item('mcremix:pvc_sheet')

    # ---- wire: drawn through a die on the Coil Winder, an ingot into four, in place of sawing plates (card 5)
    for metal, wire, colour in WIRES:
        G.recipe(f'coiling/{metal}_wire', {
            'type': 'mcremix:coiling', 'processingTime': 160, 'colour': colour,
            'ingredients': [{'tag': f'forge:ingots/{metal}'}], 'results': [item(wire, 4)]})
        G.disabled('powergrid', f'cutting/{metal}_wire_cutting')
    # sheathed in PVC, not kelp (card 6): four wires to a sheet, and a cord's jacket
    shapeless('insulated_copper_wire', [copper_wire] * 4 + [pvc], 'powergrid:insulated_copper_wire', 4)
    shapeless('copper_cord', [item('powergrid:insulated_copper_wire')] * 2 + [pvc], 'powergrid:copper_cord')

    # ---- magnets are hardened steel, which holds its magnetism where stone and soft iron don't
    G.recipe('magnetization/magnet', {'type': 'powergrid:magnetization', 'ingredients': [{'tag': 'forge:ingots/steel'}],
                                      'results': [item('powergrid:magnet')]})
    G.disabled('powergrid', 'magnetization/andesite_alloy')

    # ---- the generator: laminated steel round its coils, and a commutator of copper segments in a Bakelite hub fed
    # by carbon brushes
    mechanical('generator_induction_rotor', ['SMS', 'MHM', 'SMS'],
               {'S': steel_plate, 'M': coils, 'H': item('create:shaft')}, 'powergrid:generator_induction_rotor')
    mechanical('generator_large_induction_rotor', ['  S  ', ' SMS ', 'SMRMS', ' SMS ', '  S  '],
               {'S': steel_plate, 'M': coils, 'R': item('powergrid:generator_induction_rotor')},
               'powergrid:generator_large_induction_rotor')
    mechanical('generator_commutator', [' P ', 'KMK', 'BHB', ' C '],
               {'P': item('powergrid:pins'), 'K': coke, 'M': copper_plate, 'B': bakelite, 'H': item('create:shaft'),
                'C': item('create:andesite_casing')}, 'powergrid:generator_commutator')
    shaped('generator_housing', ['SC', 'ES'], {'S': steel_plate, 'C': copper_plate, 'E': casing},
           'powergrid:generator_housing')

    # ---- resistors are carbon in a Bakelite binder on copper leads; a potentiometer's track is carbon on a Bakelite
    # wafer under a copper wiper; a varistor is a disc of zinc oxide
    shapeless('resistor', [coke, bakelite, copper_wire, copper_wire], 'powergrid:resistor', 4)
    shaped('potentiometer', ['B', 'N', 'K'], {'B': bakelite, 'N': {'tag': 'forge:nuggets/copper'}, 'K': coke},
           'powergrid:potentiometer')
    shapeless('varistor', [item('mcremix:zinc_oxide'), copper_wire, copper_wire], 'powergrid:varistor')
    G.firing('zinc_oxide', {'tag': 'forge:nuggets/zinc'}, 'mcremix:zinc_oxide', 'searing', 100)
    # capacitors: aluminium foil rolled up in waxed paper on the Coil Winder (card 7); honeycomb's beeswax works too
    G.recipe('coiling/capacitor', {
        'type': 'mcremix:coiling', 'processingTime': 320, 'colour': '#d4d9de',
        'ingredients': [aluminium_plate] * 2 + [item('minecraft:paper')] * 2 + [wax],
        'results': [item('powergrid:capacitor', 4)]})
    G.disabled('powergrid', 'crafting/capacitor')

    # ---- the Battery is Volta's pile: copper and zinc, wet with brine (card 4); Blazing Acid isn't made any more
    battery = item('powergrid:incomplete_battery')
    assembly('battery', casing, 3, [('powergrid:battery', 100), ('powergrid:conductive_casing', 5),
                                    ('create:copper_sheet', 2), ('powergrid:zinc_sheet', 2)],
             [deploy(battery, copper_plate), deploy(battery, {'tag': 'forge:plates/zinc'}),
              step('filling', battery, {'fluid': 'mcremix:salt_water', 'amount': 250})], battery)
    G.disabled('powergrid', 'mixing/acid')

    # ---- the sodium-sulfur battery (Taran's ask with card 4): beta-alumina tubes, alumina pressed with soda ash and
    # fired Blazing as real ones are sintered, near 1,600 °C; on the line a Steel Casing takes a tube, molten sodium is
    # spouted into it and molten sulfur round it, and the press seals it, a cell to a loop
    G.recipe('compacting/unfired_beta_alumina_tube', {
        'type': 'create:compacting',
        'ingredients': [item('mcremix:alumina'), item('mcremix:alumina'), item('mcremix:soda_ash')],
        'results': [item('mcremix:unfired_beta_alumina_tube')]})
    G.firing('beta_alumina_tube', item('mcremix:unfired_beta_alumina_tube'), 'mcremix:beta_alumina_tube', 'blazing',
             400)
    cell = item('mcremix:incomplete_sodium_sulfur_battery')
    G.recipe('sequenced_assembly/sodium_sulfur_battery', {
        'type': 'create:sequenced_assembly', 'ingredient': item('mcremix:steel_casing'), 'loops': 2,
        'results': [item('mcremix:sodium_sulfur_battery')], 'transitionalItem': cell,
        'sequence': [deploy(cell, item('mcremix:beta_alumina_tube')),
                     step('filling', cell, {'fluid': 'mcremix:molten_sodium', 'amount': 100}),
                     step('filling', cell, {'fluid': 'mcremix:molten_sulfur', 'amount': 250}),
                     step('pressing', cell)]})
    # sulfur melts at 115 °C, so the crucible melts it back into what settles out of it
    G.recipe('melting/sulfur', {'type': 'mcremix:melting', 'ingredient': item('mcremix:sulfur'),
                                'result': {'fluid': 'mcremix:molten_sulfur', 'amount': 100}})

    # ---- boards: copper pressed onto the Bakelite blank, then printed with a wax resist through the schematic,
    # where Power Grid had dough. (A spout would lose the board's design: Power Grid carries it only through deployers
    # and presses.)
    board = item('powergrid:incomplete_unetched_circuit')
    assembly('unetched_circuit_board', item('powergrid:circuit_schematic'), 2, [('powergrid:unetched_circuit', 100)],
             [deploy(board, item('powergrid:empty_circuit')), deploy(board, copper_plate), step('pressing', board),
              deploy(board, wax)], board)

    # ---- switches, plugs and gauges: insulated in Bakelite or glass, needles turned against a magnet
    shapeless('relay', [coils, iron_plate, bakelite], 'powergrid:relay')
    shapeless('relay_dpdt', [coils, iron_plate, iron_plate, bakelite], 'powergrid:relay_dpdt')
    shaped('device_connector', [' Z ', 'CBC'], {'Z': {'tag': 'forge:plates/brass'}, 'C': copper_plate, 'B': bakelite},
           'powergrid:device_connector')
    shaped('wire_connector', [' C ', 'CGC'], {'C': {'tag': 'forge:nuggets/copper'}, 'G': {'tag': 'forge:glass/colorless'}},
           'powergrid:wire_connector')
    shaped('voltage_gauge', ['M', 'c', 'A'], {'M': item('powergrid:magnet'), 'c': coils, 'A': casing},
           'powergrid:voltage_gauge')
    shaped('multimeter', ['WRW', 'CBV'], {'W': copper_wire, 'R': item('powergrid:resistor'),
                                          'C': item('powergrid:current_gauge'), 'B': bakelite,
                                          'V': item('powergrid:voltage_gauge')}, 'powergrid:multimeter')
    shaped('factory_light', ['I', 'L', 'G'], {'I': iron_plate, 'L': item('powergrid:light_bulb'),
                                              'G': item('minecraft:glass_pane')}, 'powergrid:factory_light')

    # ---- the silicon chain (card 2: purified the real way)
    # quartz and coke bound with pitch by a press, as a rutile briquette is
    G.recipe('compacting/quartz_briquette', {
        'type': 'create:compacting',
        'ingredients': [{'tag': 'forge:gems/quartz'}, coke, {'fluid': 'mcremix:pitch', 'amount': 50}],
        'results': [item('mcremix:quartz_briquette')]})
    # the arc takes the quartz's oxygen off with the coke: crude silicon, with iron still in it
    G.recipe('arc_furnace/crude_silicon', {'type': 'mcremix:arc_furnace', 'energy': 32000,
                                           'ingredients': [item('mcremix:quartz_briquette')],
                                           'results': [item('mcremix:crude_silicon')]})
    # hydrogen chloride run over it, Hot, in a Steel Furnace or a hot tube: Si + 3 HCl -> SiHCl3 + H2, the iron going
    # with it as ferric chloride; a crude silicon wears away for each batch
    G.recipe('steel_furnace/crude_trichlorosilane', {
        'type': 'mcremix:steel_furnace', 'heat': 'hot', 'processingTime': 100, 'catalyst_lasts': 300,
        'ingredients': [item('mcremix:crude_silicon'), {'fluid': 'mcremix:hydrogen_chloride', 'amount': 300}],
        'results': [{'fluid': 'mcremix:crude_trichlorosilane', 'amount': 100}, {'fluid': 'mcremix:hydrogen', 'amount': 100}]})
    # distilled: trichlorosilane boils at 32 degrees, and the iron stays in the flask as ferric chloride
    G.recipe('distilling/crude_trichlorosilane', {
        'type': 'mcremix:distilling', 'heat': 'warm', 'processingTime': 600,
        'ingredients': [{'fluid': 'mcremix:crude_trichlorosilane', 'amount': 1000}],
        'results': [{'fluid': 'mcremix:trichlorosilane', 'amount': 950}, {'fluid': 'mcremix:ferric_chloride', 'amount': 50}],
        'boiling_points': [32, 316], 'close': [False]})
    # the Siemens process: with hydrogen at Blazing it lays down pure silicon and gives back its hydrogen chloride,
    # SiHCl3 + H2 -> Si + 3 HCl, so the hydrogen chloride and hydrogen go round and round
    G.recipe('steel_furnace/polysilicon', {
        'type': 'mcremix:steel_furnace', 'heat': 'blazing', 'processingTime': 200,
        'ingredients': [{'fluid': 'mcremix:trichlorosilane', 'amount': 100}, {'fluid': 'mcremix:hydrogen', 'amount': 100}],
        'results': [item('mcremix:polysilicon'), {'fluid': 'mcremix:hydrogen_chloride', 'amount': 300}]})
    # melted in the Arc Furnace's cold hearth into a bar, which a saw slices into wafers
    G.recipe('arc_furnace/silicon_ingot', {'type': 'mcremix:arc_furnace', 'energy': 16000,
                                           'ingredients': [item('mcremix:polysilicon')],
                                           'results': [item('mcremix:silicon_ingot')]})
    G.recipe('cutting/silicon_wafer', {'type': 'create:cutting', 'ingredients': [item('mcremix:silicon_ingot')],
                                       'processingTime': 100, 'results': [item('mcremix:silicon_wafer', 8)]})

    # ---- what is made on a wafer: a diode and a static induction transistor are a die between their contacts, a BJT
    # a die in a metal can with gold bond wire; a chip a die with gold bond wires and copper leads in a polyethylene
    # package (gen_assets' integrated circuit); a solar panel silicon cells in polyethylene under glass, strung with
    # copper ribbon in an aluminium frame
    shaped('diode', ['WC'], {'W': wafer, 'C': copper_plate}, 'powergrid:diode')
    shaped('vfet', ['C', 'W', 'I'], {'C': copper_plate, 'W': wafer, 'I': iron_plate}, 'powergrid:vfet')
    for name, other in (('bjt_npn', 'bjt_pnp'), ('bjt_pnp', 'bjt_npn')):
        can = item(f'powergrid:incomplete_{name}')
        # an NPN die gets its wafer first, a PNP its gold, as Power Grid has them
        parts = [deploy(can, wafer), deploy(can, gold_wire)]
        assembly(name, iron_plate, 1, [(f'powergrid:{name}', 100), (f'powergrid:{other}', 10),
                                       ('create:iron_sheet', 3), ('mcremix:silicon_wafer', 3)],
                 (parts if name == 'bjt_npn' else parts[::-1]) + [step('pressing', can)], can)
    panel = item('powergrid:incomplete_solar_panel')
    assembly('solar_panel', item('minecraft:glass_pane'), 1,
             [('powergrid:solar_panel', 100), ('mcremix:silicon_wafer', 3), ('mcremix:aluminium_sheet', 3),
              ('mcremix:polyethylene_pellets', 3)],
             [deploy(panel, item('mcremix:polyethylene_pellets')), deploy(panel, wafer), step('cutting', panel),
              deploy(panel, copper_wire), step('pressing', panel), deploy(panel, aluminium_plate)], panel)

    # ---- the cathode ray tube's screen: zinc oxide and quartz fired together into willemite, 2 ZnO + SiO2
    G.recipe('mixing/phosphor', {
        'type': 'create:mixing', 'heatRequirement': 'superheated',
        'ingredients': [item('mcremix:zinc_oxide'), item('mcremix:zinc_oxide'), {'tag': 'forge:gems/quartz'}],
        'results': [item('mcremix:phosphor', 2)]})


# ------------------------------------------------------------------------------------------------ text

def lang():
    text = {}
    tips = {
        'zinc_oxide': (
            '_Zinc_ burnt in a _Searing_ fire: a white powder. Pressed between two _Copper Wire_ it makes a _Varistor_; '
            'fired with _quartz_ it makes _Phosphor_.'),
        'phosphor': (
            '_Zinc oxide_ and _quartz_ fired together in a _superheated_ basin into willemite, which glows _green_ '
            'where electrons strike it: the screen of a _Cathode Ray Tube_.'),
        'quartz_briquette': (
            '_Quartz_ and _coke_ bound with _pitch_ by a press. The _Arc Furnace_ melts it down, the coke taking the '
            'quartz\'s oxygen, into _Crude Silicon_.'),
        'crude_silicon': (
            'Silicon, iron and all: far too dirty for a chip. _Hydrogen chloride_ run over it at _Hot_ heat, in a '
            '_Steel Furnace_ or a _hot tube_, comes off as _crude trichlorosilane_ and _hydrogen_; one wears away for '
            'each batch.'),
        'polysilicon': (
            'Silicon laid down out of _trichlorosilane_ by _hydrogen_ in a _Blazing_ Steel Furnace, as pure as '
            'anything gets. The _Arc Furnace_ melts it into an _ingot_.'),
        'silicon_ingot': 'Polysilicon melted and frozen into a bar. A _saw_ slices it into _eight wafers_.',
        'silicon_wafer': (
            'What Power Grid\'s _diodes_, _transistors_, _integrated circuits_ and _solar panels_ are made on.'),
    }
    for name in ITEMS:
        key = 'item.mcremix.' + name
        text[key] = G.title_case(name)
        text[key + '.tooltip.summary'] = tips[name]
    text.update({
        'block.mcremix.sodium_sulfur_battery': 'Sodium-Sulfur Battery',
        'block.mcremix.sodium_sulfur_battery.tooltip.summary': (
            'Power Grid\'s _Battery_ four times over, and it comes _full_: the sodium is the charge. It works only '
            '_hot_, with _Warm_ heat or hotter under every block of its bottom. Cold, it keeps its charge but gives '
            'and takes none.'),
        'item.mcremix.unfired_beta_alumina_tube': 'Unfired Beta-Alumina Tube',
        'item.mcremix.unfired_beta_alumina_tube.tooltip.summary': (
            '_Alumina_ pressed with _soda ash_. Fired at _Blazing_ heat, it becomes a _Beta-Alumina Tube_.'),
        'item.mcremix.beta_alumina_tube': 'Beta-Alumina Tube',
        'item.mcremix.beta_alumina_tube.tooltip.summary': (
            'A ceramic that lets _sodium_ through, hot, and nothing else: the heart of a _Sodium-Sulfur Battery_.'),
        'item.mcremix.incomplete_sodium_sulfur_battery': 'Incomplete Sodium-Sulfur Battery',
        'mcremix.goggles.sodium_sulfur_battery.charge': 'Charge: %s of %s Wh',
        'mcremix.goggles.sodium_sulfur_battery.hot': 'Hot: working',
        'mcremix.goggles.sodium_sulfur_battery.cold': 'Cold: needs %s heat or hotter under every block of its bottom',
    })
    D.merge_json(G.asset('lang', 'en_us.json'), lambda obj: obj.update(text))

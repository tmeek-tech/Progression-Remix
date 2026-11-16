"""
Resources for the distillation build's fifth and last stage, the dyes and sulfur (registry/ModDyes.java, and its fluids
and gases elsewhere): cleaning sour gas with red mud or by the Claus process, molten sulfur, sulfuric acid over the
platinum gauze, mixed acid, the nitrations, the amines, naphthol, the diazo salts and the coal tar dyes, saltpetre and
gunpowder; and the tags, damage types, loot tables, Ponder structures and text they need. The items' and the block of
sulfur's art is gen_dyes_art.py's, which this runs first. gen_assets.py calls run() after gen_air, passing itself; the
new fluids are in gen_assets' CHEMICALS and their formulas in gen_formulas, and this file adds to the lang and tag files
written before it rather than owning any shared file. The numbers are design/dyes-sulfur-build-numbers.md's.
"""
import gen_decor as D

G = None  # gen_assets, set by run()

BLOCKS = ['sulfur_block']


def run(gen):
    global G
    G = gen
    import gen_dyes_art
    gen_dyes_art.run(gen)
    recipes()
    tags()
    damage_types()
    loot()
    ponder_schematics()
    lang()


def fluid(name, amount):
    return {'fluid': name if ':' in name else f'mcremix:{name}', 'amount': amount}


def item(name, count=1):
    return G.item(name if ':' in name else f'mcremix:{name}', count)


def items(name, count):
    """`count` of an item as separate ingredients, as Create's recipes take them."""
    return [item(name)] * count


# ice or packed ice, which a diazotising basin is kept cold with
ICE = [{'item': 'minecraft:ice'}, {'item': 'minecraft:packed_ice'}]


def mixing(name, ingredients, results, heated=False, time=None):
    obj = {'type': 'create:mixing', 'ingredients': ingredients, 'results': results}
    if heated:
        obj['heatRequirement'] = 'heated'
    if time:
        obj['processingTime'] = time
    G.recipe('mixing/' + name, obj)


def vat_mixing(name, ingredients, results, time=None, fumes=None):
    obj = {'type': 'mcremix:vat_mixing', 'ingredients': ingredients, 'results': results}
    if time:
        obj['processingTime'] = time
    if fumes:
        obj['fumes'] = fumes
    G.recipe('vat_mixing/' + name, obj)


# ------------------------------------------------------------------------------------------------ recipes

def recipes():
    sulfur()
    acids()
    amines()
    diazo()
    dyes()
    gunpowder()


def sulfur():
    # red mud, the gasworks way: a vat of it left to stand cleans sour gas piped in, each mud taking 250 mB and turning
    # foul; aired in an open basin, or in a vat with oxygen, it goes red again and drops the sulfur it took
    G.recipe('settling/red_mud_cleaning', {
        'type': 'mcremix:settling', 'processingTime': 100,
        'ingredients': [item('red_mud'), fluid('sour_gas', 250)],
        'results': [item('foul_red_mud'), fluid('fuel_gas', 150)]})
    G.recipe('airing/foul_red_mud', {
        'type': 'mcremix:airing', 'processingTime': 400,
        'ingredients': [item('foul_red_mud')],
        'results': [item('red_mud'), item('sulfur')]})
    # the Claus process, the refinery way: sour gas washed in soda ash solution, the foul soda stripped of its hydrogen
    # sulfide in a still, and that burnt over alumina into molten sulfur
    mixing('soda_ash_solution', [item('soda_ash'), fluid('minecraft:water', 250)], [fluid('soda_ash_solution', 250)])
    vat_mixing('claus_wash', [fluid('sour_gas', 250), fluid('soda_ash_solution', 150)],
        [fluid('fuel_gas', 150), fluid('foul_soda', 250)])
    G.recipe('distilling/foul_soda', {
        'type': 'mcremix:distilling', 'heat': 'warm', 'processingTime': 600,
        'ingredients': [fluid('foul_soda', 1000)],
        'results': [fluid('hydrogen_sulfide', 400), fluid('soda_ash_solution', 600)],
        'boiling_points': [-60, 100], 'close': [False]})
    G.recipe('steel_furnace/claus', {
        'type': 'mcremix:steel_furnace', 'heat': 'hot', 'processingTime': 100, 'catalyst_lasts': 16000,
        'ingredients': [item('alumina'), fluid('hydrogen_sulfide', 100)],
        'results': [fluid('molten_sulfur', 100), fluid('minecraft:water', 100)]})
    # molten sulfur sets in a basin or vat with no heat under it, 100 mB a sulfur
    G.recipe('settling/molten_sulfur', {
        'type': 'mcremix:settling', 'processingTime': 20,
        'ingredients': [fluid('molten_sulfur', 100)],
        'results': [item('sulfur')]})
    G.recipe('sulfur_block', {
        'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': ['SSS', 'SSS', 'SSS'],
        'key': {'S': item('sulfur')}, 'result': item('sulfur_block')})
    G.recipe('sulfur_from_block', {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [item('sulfur_block')],
        'result': item('sulfur', 9)})


def acids():
    # sulfur burnt in the Furnace: it burns away, its sulfur dioxide collecting in the Furnace's tank
    G.write_json(G.data('mcremix', 'recipes', 'firing', 'sulfur.json'), {
        'type': 'mcremix:firing', 'ingredient': item('sulfur'), 'heat': 'warm', 'time': 100,
        'byproduct': fluid('sulfur_dioxide', 100)})
    # the contact process: sulfur dioxide over the platinum gauze at Hot, the draught giving the oxygen
    G.recipe('steel_furnace/sulfur_trioxide', {
        'type': 'mcremix:steel_furnace', 'heat': 'hot', 'processingTime': 100, 'catalyst_lasts': 64000,
        'ingredients': [item('platinum_gauze'), fluid('sulfur_dioxide', 100)],
        'results': [fluid('sulfur_trioxide', 100)]})
    # into water, half of it goes up as an acid fog; soaked up in sulfuric acid, none does (Taran's pick)
    vat_mixing('sulfuric_acid', [fluid('sulfur_trioxide', 250), fluid('minecraft:water', 250)],
        [fluid('sulfuric_acid', 250)], fumes=fluid('sulfur_trioxide', 125))
    vat_mixing('sulfuric_acid_soaked', [fluid('sulfur_trioxide', 250), fluid('minecraft:water', 250),
                                 fluid('sulfuric_acid', 250)], [fluid('sulfuric_acid', 750)])
    mixing('mixed_acid', [fluid('nitric_acid', 250), fluid('sulfuric_acid', 250)], [fluid('mixed_acid', 500)])


def amines():
    # benzene nitrated: slowly in nitric acid, four times as fast in mixed acid, which gives its sulfuric acid back
    mixing('nitrobenzene', [fluid('light_oil', 250), fluid('nitric_acid', 250)],
           [fluid('nitrobenzene', 250), fluid('minecraft:water', 250)], time=400)
    mixing('nitrobenzene_mixed_acid', [fluid('light_oil', 250), fluid('mixed_acid', 500)],
           [fluid('nitrobenzene', 250), fluid('sulfuric_acid', 250)], time=100)
    # Bechamp's reduction, iron and a little vinegar
    mixing('aniline', items('minecraft:iron_nugget', 2) + [fluid('nitrobenzene', 250), fluid('vinegar', 50)],
           [fluid('aniline', 250)], heated=True)
    mixing('aniline_salt', [fluid('aniline', 250), fluid('hydrochloric_acid', 250)],
           [item('aniline_salt', 4), fluid('minecraft:water', 250)])
    mixing('acetanilide', [fluid('aniline', 250), fluid('vinegar', 250)],
           [item('acetanilide', 4), fluid('minecraft:water', 250)], heated=True)
    mixing('nitroacetanilide', items('acetanilide', 4) + [fluid('mixed_acid', 500)],
           [item('nitroacetanilide', 4), fluid('sulfuric_acid', 250)])
    mixing('nitroaniline', items('nitroacetanilide', 4) + [fluid('hydrochloric_acid', 250)],
           [item('nitroaniline', 4), fluid('vinegar', 250)], heated=True)
    mixing('nitronaphthalene', items('naphthalene', 2) + [fluid('nitric_acid', 250)],
           [item('nitronaphthalene', 2), fluid('minecraft:water', 250)], time=400)
    mixing('nitronaphthalene_mixed_acid', items('naphthalene', 2) + [fluid('mixed_acid', 500)],
           [item('nitronaphthalene', 2), fluid('sulfuric_acid', 250)], time=100)
    mixing('naphthylamine', items('nitronaphthalene', 2) + items('minecraft:iron_nugget', 2) + [fluid('vinegar', 50)],
           [item('naphthylamine', 2)], heated=True)
    # naphthol: naphthalene sulfonated, then melted with caustic soda
    mixing('naphthalenesulfonic_acid', items('naphthalene', 2) + [fluid('sulfuric_acid', 250)],
           [fluid('naphthalenesulfonic_acid', 250)], heated=True)
    mixing('naphthol', [fluid('naphthalenesulfonic_acid', 250), fluid('caustic_soda', 250)],
           [item('naphthol', 2), fluid('minecraft:water', 250)], heated=True)


def diazo():
    # diazotising, kept cold: ice in a basin, cold water in a vat, which comes out as water
    for salt, amine in (('diazo_salt', None), ('nitro_diazo_salt', 'nitroaniline'),
                        ('naphthyl_diazo_salt', 'naphthylamine')):
        base = [item('sodium_nitrite')] + (items(amine, 2) if amine else [])
        fluids = ([] if amine else [fluid('aniline', 250)]) + [fluid('hydrochloric_acid', 250)]
        mixing(salt, base + [ICE] + fluids, [fluid(salt, 250)])
        vat_mixing(salt, base + fluids + [fluid('cold_water', 250)], [fluid(salt, 250), fluid('minecraft:water', 250)])


def dyes():
    # 8 dyes for every 250 mB of aniline (Taran's pick), all mixed cold unless heated
    mixing('mauve_dye', [fluid('aniline', 250), fluid('bleach', 250)], [item('mauve_dye', 8)])
    mixing('magenta_dye', items('aniline_salt', 4) + [item('minecraft:iron_nugget'), fluid('nitrobenzene', 250)],
           [item('minecraft:magenta_dye', 8)], heated=True)
    mixing('black_dye', items('aniline_salt', 4) + [fluid('ferric_chloride', 250)], [item('minecraft:black_dye', 8)])
    mixing('yellow_dye', [fluid('diazo_salt', 250), fluid('aniline', 250)], [item('minecraft:yellow_dye', 16)])
    mixing('orange_dye', [fluid('diazo_salt', 250), fluid('sodium_phenolate', 250)], [item('minecraft:orange_dye', 8)])
    mixing('red_dye', items('naphthol', 2) + [fluid('nitro_diazo_salt', 250), fluid('caustic_soda', 250)],
           [item('minecraft:red_dye', 8)])
    mixing('maroon_dye', items('naphthol', 2) + [fluid('naphthyl_diazo_salt', 250), fluid('caustic_soda', 250)],
           [item('maroon_dye', 8)])
    mixing('malachite_green_dye', items('leuco_malachite_green', 4) + [fluid('ferric_chloride', 250)],
           [item('malachite_green_dye', 8)])
    vat_mixing('bleach', [fluid('chlorine', 250), fluid('caustic_soda', 250)], [fluid('bleach', 500)])
    # indigo, Heumann's way: chloroacetic acid and aniline into phenylglycine, melted with caustic soda, and the melt
    # aired. Sodamide triples the yield (Taran's pick); the melt with it is a three-batch one, so a basin holding both
    # takes it before the one without, which Create picks by how many items it takes
    vat_mixing('chloroacetic_acid', [fluid('vinegar', 250), fluid('chlorine', 250)],
        [fluid('chloroacetic_acid', 250), fluid('hydrogen_chloride', 250)])
    mixing('phenylglycine', [fluid('aniline', 250), fluid('chloroacetic_acid', 250)],
           [item('phenylglycine', 4), fluid('hydrochloric_acid', 250)], heated=True)
    mixing('indigo_melt', items('phenylglycine', 12) + items('sodamide', 3) + [fluid('caustic_soda', 750)],
           [fluid('indigo_melt', 750)], heated=True)
    mixing('indigo_melt_without_sodamide', items('phenylglycine', 12) + [fluid('caustic_soda', 250)],
           [fluid('indigo_melt', 250)], heated=True)
    vat_mixing('sodamide', [fluid('molten_sodium', 250), fluid('ammonia', 250)],
        [item('sodamide', 4), fluid('hydrogen', 125)])
    G.recipe('airing/indigo_melt', {
        'type': 'mcremix:airing', 'processingTime': 200,
        'ingredients': [fluid('indigo_melt', 250)],
        'results': [item('indigo_dye', 8), fluid('caustic_soda', 250)]})
    # malachite green: benzal chloride from benzene and chlorine, benzaldehyde from that, dimethylaniline over alumina,
    # the two joined into the colourless leuco base, and that oxidised with ferric chloride (Taran's pick)
    vat_mixing('benzal_chloride', [fluid('light_oil', 250), fluid('chlorine', 500)],
        [fluid('benzal_chloride', 250), fluid('hydrogen_chloride', 500)])
    mixing('benzaldehyde', [fluid('benzal_chloride', 250), fluid('minecraft:water', 250)],
           [fluid('benzaldehyde', 250), fluid('hydrochloric_acid', 250)], heated=True)
    G.recipe('steel_furnace/dimethylaniline', {
        'type': 'mcremix:steel_furnace', 'heat': 'hot', 'processingTime': 100, 'catalyst_lasts': 16000,
        'ingredients': [item('alumina'), fluid('aniline', 100), fluid('methanol', 200)],
        'results': [fluid('dimethylaniline', 100), fluid('minecraft:water', 200)]})
    vat_mixing('leuco_malachite_green', [fluid('benzaldehyde', 125), fluid('dimethylaniline', 250),
                                  fluid('hydrochloric_acid', 50)],
        [item('leuco_malachite_green', 4), fluid('minecraft:water', 125)])


def gunpowder():
    # saltpetre from nitric acid and soda ash; its CO2 goes off a basin and stays in a vat
    mixing('saltpetre', [item('soda_ash'), fluid('nitric_acid', 250)],
           [item('saltpetre', 2), fluid('minecraft:water', 125), fluid('carbon_dioxide', 125)])
    mixing('gunpowder', [item('sulfur')] + items('saltpetre', 2) + [item('minecraft:charcoal')],
           [item('minecraft:gunpowder', 4)])


# ------------------------------------------------------------------------------------------------ tags

def tags():
    m = lambda names: [f'mcremix:{n}' for n in names]
    D.add_tag('minecraft', 'blocks', 'mineable/pickaxe', m(BLOCKS))
    # what keeps a diazo salt cold, touching its tank, basin or vat
    D.add_tag('mcremix', 'blocks', 'keeps_cold', ['minecraft:ice', 'minecraft:packed_ice', 'minecraft:blue_ice',
                                                  'minecraft:snow_block', 'minecraft:powder_snow'])
    D.add_tag('minecraft', 'damage_type', 'bypasses_armor', m(['hydrogen_sulfide', 'acid_fog']))
    D.add_tag('forge', 'items', 'dusts/sulfur', ['mcremix:sulfur'])
    D.add_tag('forge', 'items', 'dusts', ['mcremix:sulfur'])
    D.add_tag('forge', 'items', 'storage_blocks/sulfur', ['mcremix:sulfur_block'])
    D.add_tag('forge', 'items', 'storage_blocks', ['mcremix:sulfur_block'])
    D.add_tag('forge', 'blocks', 'storage_blocks/sulfur', ['mcremix:sulfur_block'])
    D.add_tag('forge', 'blocks', 'storage_blocks', ['mcremix:sulfur_block'])


def damage_types():
    for name in ('hydrogen_sulfide', 'acid_fog'):
        G.write_json(G.data('mcremix', 'damage_type', name + '.json'),
                     {'message_id': f'mcremix.{name}', 'scaling': 'when_caused_by_living_non_player',
                      'exhaustion': 0.0})


def loot():
    for name in BLOCKS:
        G.write_json(G.data('mcremix', 'loot_tables', 'blocks', name + '.json'), G.loot_self(name))


# ------------------------------------------------------------------------------------------------ structures

def pipe(*sides, block='create:fluid_pipe'):
    return (block, dict({d: 'true' if d in sides else 'false'
                         for d in ('north', 'south', 'east', 'west', 'up', 'down')}, waterlogged='false'))


def tank(pos):
    """A lone window tank of Create's."""
    return ('create:fluid_tank', {'top': 'true', 'bottom': 'true', 'shape': 'window'}, G.lone_tank_nbt(pos))


def vat(b, pos):
    """A one-block vat: a tank with its windows shut, standing on andesite scaffolding."""
    x, y, z = pos
    b[(x, y - 1, z)] = ('create:andesite_scaffolding', {'distance': 0, 'bottom': 'false', 'waterlogged': 'false'})
    b[pos] = ('create:fluid_tank', {'top': 'true', 'bottom': 'true', 'shape': 'plain'},
              G.tank_nbt(pos, pos, 1, 1, window=False))


def raised_tank(b, pos):
    """A lone tank on a block of andesite casing, level with a vat's tank."""
    x, y, z = pos
    b[(x, y - 1, z)] = ('create:andesite_casing', {})
    b[pos] = tank(pos)


def vat_mixer(b, vat_pos):
    """A copper vat mixer on the north side of a one-block vat, facing into it, turned by a cogwheel to its west on a
    bracket, on a creative motor, both on andesite casing, as in vat/mixer."""
    x, y, z = vat_pos
    b[(x, y, z - 1)] = ('mcremix:copper_vat_mixer', {'facing': 'south', 'patina': 'none'})
    b[(x - 1, y, z - 1)] = G.bracketed('create:cogwheel', 'create:metal_bracket', axis='z')
    b[(x - 1, y, z - 2)] = ('create:creative_motor', {'facing': 'south'})
    b[(x - 1, y - 1, z - 1)] = ('create:andesite_casing', {})
    b[(x - 1, y - 1, z - 2)] = ('create:andesite_casing', {})


def steel_furnace(facing='south'):
    return ('mcremix:steel_furnace', {'chimney': 'false', 'facing': facing, 'heat': 0, 'lit': 'false'})


def ponder_schematics():
    """
    sour_gas_cleaning/intro: a 7x7 plate. Along z=1, red mud's way: sour gas in a tank at (0,2,1) piped east into a
    one-block vat at (2,2,1), and fuel gas piped on east from it into a tank at (4,2,1); an open basin at (6,1,1).
    Along z=4, the Claus process: sour gas in a tank at (0,2,4) piped into a wash vat at (2,2,4), stirred by a vat
    mixer on its north side; hydrogen sulfide in a tank at (4,2,4) piped east into a Steel Furnace at (6,2,4) facing
    south, its fuel pipe under it.
    sulfuric_acid/intro: a 7x7 plate; a Furnace at (1,1,3) facing south piped up and east into the west side of a Steel
    Furnace at (3,2,3) facing south, which has its fuel pipe under it from a tank at (3,1,4); the Steel Furnace piped
    east into a one-block vat at (5,2,3), stirred by a vat mixer on its north side.
    dye_works/intro: a 7x7 plate; a basin at (1,1,3) and one at (3,1,3), each under a mixer two up with a cogwheel over
    it, ice either side of the second at (3,1,2) and (3,1,4), and an open basin at (5,1,3).
    """
    b = {}
    G.plate(b, 7)
    for z in (1, 4):
        raised_tank(b, (0, 2, z))
        b[(1, 2, z)] = pipe('east', 'west')
        vat(b, (2, 2, z))
    b[(3, 2, 1)] = pipe('east', 'west')
    raised_tank(b, (4, 2, 1))
    b[(6, 1, 1)] = ('create:basin', {'facing': 'down'})
    raised_tank(b, (4, 2, 4))
    b[(5, 2, 4)] = pipe('east', 'west')
    b[(6, 2, 4)] = steel_furnace()
    b[(6, 1, 4)] = pipe('up', 'south')
    vat_mixer(b, (2, 2, 4))
    G.save_structure('sour_gas_cleaning/intro', (7, 4, 7), b)

    b = {}
    G.plate(b, 7)
    b[(1, 1, 3)] = G.kiln_state('south', chimney=False)
    b[(2, 1, 3)] = pipe('west', 'up')
    b[(2, 2, 3)] = pipe('down', 'east')
    b[(3, 2, 3)] = steel_furnace()
    b[(3, 1, 3)] = pipe('up', 'south')
    b[(3, 1, 4)] = tank((3, 1, 4))
    b[(4, 2, 3)] = pipe('east', 'west')
    vat(b, (5, 2, 3))
    vat_mixer(b, (5, 2, 3))
    G.save_structure('sulfuric_acid/intro', (7, 4, 7), b)

    b = {}
    G.plate(b, 7)
    for x in (1, 3):
        b[(x, 1, 3)] = ('create:basin', {'facing': 'down'})
        b[(x, 3, 3)] = ('create:mechanical_mixer', {})
        b[(x, 4, 3)] = ('create:cogwheel', {'axis': 'y'})
    b[(3, 1, 2)] = ('minecraft:ice', {})
    b[(3, 1, 4)] = ('minecraft:ice', {})
    b[(5, 1, 3)] = ('create:basin', {'facing': 'down'})
    G.save_structure('dye_works/intro', (7, 5, 7), b)


# ------------------------------------------------------------------------------------------------ lang

def lang():
    text = {
        # blocks
        'block.mcremix.sulfur_block': 'Block of Sulfur',
        'block.mcremix.sulfur_block.tooltip.summary': (
            'Nine _sulfur_ pressed together, and what _molten sulfur_ sets into poured out in the world.'),
        # items
        'item.mcremix.sulfur': 'Sulfur',
        'item.mcremix.sulfur.tooltip.summary': (
            'Yellow crystals, from _sour gas_ cleaned either way: _foul red mud_ aired, or _molten sulfur_ from the '
            '_Claus process_ set in a cool basin. _Fired_ in a Furnace it burns away to _sulfur dioxide_; with '
            '_saltpetre_ and _charcoal_ it is _gunpowder_.'),
        'item.mcremix.foul_red_mud': 'Foul Red Mud',
        'item.mcremix.foul_red_mud.tooltip.summary': (
            '_Red mud_ gone black with the _hydrogen sulfide_ it took out of _sour gas_ in a vat. Left in an _open '
            'basin_, with nothing over it, it goes _red again_ in the air and drops a _sulfur_: 20 seconds a mud. In a '
            '_vat_ it airs with _50 mB of oxygen_ piped in.'),
        'item.mcremix.aniline_salt': 'Aniline Salt',
        'item.mcremix.aniline_salt.tooltip.summary': (
            'Aniline with its _hydrochloric acid_, white crystals as dye works bought it: for _magenta_ and _black_.'),
        'item.mcremix.acetanilide': 'Acetanilide',
        'item.mcremix.acetanilide.tooltip.summary': (
            '_Aniline_ heated with _vinegar_. Nitrated in _mixed acid_, it is the start of _nitroaniline_.'),
        'item.mcremix.nitroacetanilide': 'Nitroacetanilide',
        'item.mcremix.nitroacetanilide.tooltip.summary': (
            'Heated with _hydrochloric acid_ it gives _nitroaniline_, and its _vinegar_ back.'),
        'item.mcremix.nitroaniline': 'Nitroaniline',
        'item.mcremix.nitroaniline.tooltip.summary': (
            'Yellow crystals. _Diazotised_, kept cold, it is _nitro diazo salt_, for _para red_.'),
        'item.mcremix.nitronaphthalene': 'Nitronaphthalene',
        'item.mcremix.nitronaphthalene.tooltip.summary': (
            '_Naphthalene_ nitrated. _Iron_ and a little _vinegar_, heated, make it _naphthylamine_.'),
        'item.mcremix.naphthylamine': 'Naphthylamine',
        'item.mcremix.naphthylamine.tooltip.summary': (
            '_Diazotised_, kept cold, it is _naphthyl diazo salt_, for _maroon_.'),
        'item.mcremix.naphthol': 'Naphthol',
        'item.mcremix.naphthol.tooltip.summary': (
            '_Naphthalenesulfonic acid_ melted with _caustic soda_: what _para red_ and _maroon_ are coupled to.'),
        'item.mcremix.phenylglycine': 'Phenylglycine',
        'item.mcremix.phenylglycine.tooltip.summary': (
            '_Aniline_ heated with _chloroacetic acid_. Melted with _caustic soda_ it gives the _indigo melt_; '
            '_sodamide_ in the melt triples the yield.'),
        'item.mcremix.sodamide': 'Sodamide',
        'item.mcremix.sodamide.tooltip.summary': (
            '_Molten sodium_ and _ammonia_ stirred in a vat. In the _indigo melt_ it triples the indigo.'),
        'item.mcremix.leuco_malachite_green': 'Leucomalachite Green',
        'item.mcremix.leuco_malachite_green.tooltip.summary': (
            '_Benzaldehyde_ and _dimethylaniline_ joined: a colourless base that _ferric chloride_ turns _malachite '
            'green_.'),
        'item.mcremix.saltpetre': 'Saltpetre',
        'item.mcremix.saltpetre.tooltip.summary': (
            'Sodium nitrate, from _nitric acid_ and _soda ash_. With _sulfur_ and _charcoal_, _gunpowder_.'),
        # buckets that need saying
        'item.mcremix.diazo_salt_bucket.tooltip.summary': (
            '_Keep it on ice_: in a tank, basin or vat that no _ice_, _packed ice_, _blue ice_, _snow_ or _powder '
            'snow_ touches, outside a snowy place, a _tenth_ of it breaks down every _10 seconds_. Heated, it breaks '
            'down at once, into _phenol_. Pipes carry it without loss. For _aniline yellow_ and _orange_.'),
        'item.mcremix.nitro_diazo_salt_bucket.tooltip.summary': (
            '_Keep it on ice_, as all diazo salts: a _tenth_ of it breaks down every _10 seconds_ where no ice or snow '
            'touches it. With _naphthol_ and _caustic soda_ it couples into _para red_.'),
        'item.mcremix.naphthyl_diazo_salt_bucket.tooltip.summary': (
            '_Keep it on ice_, as all diazo salts: a _tenth_ of it breaks down every _10 seconds_ where no ice or snow '
            'touches it. With _naphthol_ and _caustic soda_ it couples into _maroon_.'),
        'item.mcremix.molten_sulfur_bucket.tooltip.summary': (
            '_Warm_, so no PVC carries it. In a basin or vat with no heat under it, it sets into _sulfur_, 100 mB a '
            'second; poured out, it sets into a _block of sulfur_. Standing in it scalds.'),
        'item.mcremix.indigo_melt_bucket.tooltip.summary': (
            '_Warm_. Aired in an _open basin_, with nothing over it, or in a vat with _oxygen_, it turns to _indigo '
            'dye_ and gives its _caustic soda_ back.'),
        'item.mcremix.sulfuric_acid_bucket.tooltip.summary': (
            'From _sulfur trioxide_ stirred into water in a vat: half fogs off unless _sulfuric acid_ is piped back '
            'in to soak it up. Poured out, it strips _rust_ and _patina_ and soaks away.'),
        'item.mcremix.mixed_acid_bucket.tooltip.summary': (
            '_Nitric_ and _sulfuric acid_ together: it nitrates _four times as fast_, and gives its sulfuric acid '
            'back.'),
        # gases
        'entity.mcremix.hydrogen_sulfide_cloud': 'Hydrogen Sulfide',
        'entity.mcremix.acid_fog': 'Acid Fog',
        'death.attack.mcremix.hydrogen_sulfide': '%1$s breathed in hydrogen sulfide',
        'death.attack.mcremix.hydrogen_sulfide.player': '%1$s breathed in hydrogen sulfide while fighting %2$s',
        'death.attack.mcremix.acid_fog': '%1$s choked in an acid fog',
        'death.attack.mcremix.acid_fog.player': '%1$s choked in an acid fog while fighting %2$s',
        # goggles
        'mcremix.goggles.vat.airing': 'Airing',
        'mcremix.goggles.diazo_salt.warming': 'Warming: a tenth of its diazo salt breaks down every 10 seconds. '
                                              'Put ice or snow against it',
        # the machines that changed
        'block.mcremix.steel_furnace.tooltip.condition3': 'Sulfur works',
        'block.mcremix.steel_furnace.tooltip.behaviour3': (
            'Pipe _hydrogen sulfide_ in over _alumina_ at _Hot_ for _molten sulfur_ and water; _sulfur dioxide_ over '
            '_platinum gauze_ at _Hot_ for _sulfur trioxide_; _aniline_ with _methanol_ over _alumina_ for '
            '_dimethylaniline_. Hydrogen sulfide piped into the _bottom_ burns as fuel, puffing a mB of _sulfur '
            'dioxide_ for every mB.'),
        'block.mcremix.steel_furnace.tooltip.behaviour2': (
            'Pump _wood spirit_, _ethanol_, _acetone_, _light oil_, _creosote_, _coal tar_, _fuel oil_, _sour gas_, '
            '_hydrogen sulfide_, _hydrogen_, _ethylene_ or _fuel gas_ in from _underneath_ and _light_ it: the faster '
            'the pump, the _hotter_ it burns, up to what that fuel can reach, and anything faster is _lost_. Only '
            '_hydrogen_, _ethylene_ and _fuel gas_ reach _Blazing_. _Sour gas_ puffs out _sulfur dioxide_, which chokes '
            'and kills plants, and _hydrogen sulfide_ twice as much. Unlit, gas fuel _escapes_ out of the front and '
            'liquid fuel _spills_ out. It takes no solid fuel, no heat from below and no bellows.'),
        'block.mcremix.kiln.tooltip.condition4': 'Coking coal, charring logs, burning sulfur',
        'block.mcremix.kiln.tooltip.behaviour4': (
            'Coal gives off _coal tar_ as it cokes, logs _wood spirit_ as they char, limestone _carbon dioxide_ as it '
            'burns to lime, and _sulfur_ burns away, at _Warm_, into _sulfur dioxide_. Draw it off with a _pump_ and '
            'pipe, or a _bucket_ (the gases only by pipe); once a bucket\'s worth waits in the Furnace, the rest goes '
            'up the chimney, the tar and spirit _burning off_ and the sulfur dioxide _sinking_ round it, choking. '
            'Carbon dioxide waiting in it goes up the chimney when tar or spirit comes.'),
        # JEI
        'mcremix.jei.airing': 'Airing',
        'mcremix.jei.airing.time': 'Airs in %s',
        'mcremix.jei.airing.basin': 'In an open basin, with nothing over it, no mixer and no heat: a batch at a time',
        'mcremix.jei.airing.vat': 'In a vat, with 50 mB of oxygen piped in for each batch: a batch for every bucket '
                                  'it holds',
        'mcremix.jei.vat_mixing.fumes': '%s mB fogs off',
        'mcremix.jei.vat_mixing.fumes.note': 'Each batch lets that much %s go into the air over the vat',
        'mcremix.jei.steel_furnace_fuel.hydrogen_sulfide': 'Puffs out a mB of sulfur dioxide for every mB burnt',
        'mcremix.jei.info.diazo_salt': (
            'Keep it on ice. In a tank, basin or vat that no ice, packed ice, blue ice, snow block or powder snow '
            'touches, outside a snowy biome, a tenth of it breaks down every 10 seconds, fizzing off as nitrogen: '
            'about half is gone in a minute. In a heated basin it breaks down at once, plain diazo salt into half as '
            'much phenol. Pipes carry it without loss.'),
        'mcremix.jei.info.hydrogen_sulfide': (
            'Stripped out of foul soda in a still, at Warm. Let loose it is deadly: it sinks and pools, and poisons '
            'whatever breathes it, two damage a second through any armour. A thin cloud shows as a faint yellow '
            'haze; a thick one, half a bucket or more, shows nothing at all. It burns, and at a flame flashes into '
            'sulfur dioxide.'),
        'mcremix.jei.info.sulfur_trioxide': (
            'Sulfur dioxide burnt over platinum gauze. Let loose, or fogging off as it is stirred into water, it is a '
            'white acid fog that sinks, stings whatever breathes it and rusts and weathers the metal under it, as '
            'hydrogen chloride mist does.'),
    }
    D.merge_json(G.asset('lang', 'en_us.json'), lambda obj: obj.update(text))

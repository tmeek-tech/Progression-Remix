"""
Resources for the distillation build's fourth stage, the liquid air plant (registry/ModAir.java, and the air plant's
fluids and gases elsewhere): its recipes, the new tags, damage types, loot tables, Ponder structures and text. The
blocks' and items' art is gen_air_art.py's, which this runs first. gen_assets.py calls run() after gen_oil, passing
itself; the new fluids are in gen_assets' CHEMICALS and their formulas in gen_formulas, and this file adds to the lang
and tag files written before it rather than owning any shared file.
"""
import gen_decor as D

G = None  # gen_assets, set by run()

BLOCKS = ['aluminium_column_section', 'expansion_valve']


def run(gen):
    global G
    G = gen
    import gen_air_art
    gen_air_art.run(gen)
    recipes()
    tags()
    damage_types()
    loot()
    ponder_schematics()
    lang()


def fluid(name, amount):
    return {'fluid': name if ':' in name else f'mcremix:{name}', 'amount': amount}


# ------------------------------------------------------------------------------------------------ recipes

def recipes():
    item = G.item
    # the liquefier: air and nitrogen squeezed cold, from 256 RPM, a batch of 100 mB every 10 ticks at 256 RPM
    for gas in ('air', 'nitrogen'):
        G.recipe(f'pressurizing/hot_compressed_{gas}', {
            'type': 'mcremix:pressurizing', 'heat': 'cold', 'min_speed': 256, 'processingTime': 10,
            'ingredients': [fluid(gas, 100)], 'results': [fluid(f'hot_compressed_{gas}', 100)]})
    # Haber's process: nitrogen and hydrogen squeezed hot over iron, a batch a second at 256 RPM
    G.recipe('pressurizing/ammonia', {
        'type': 'mcremix:pressurizing', 'heat': 'hot', 'min_speed': 256, 'processingTime': 20,
        'catalyst_lasts': 16000,
        'ingredients': [item('mcremix:iron_catalyst'), fluid('nitrogen', 100), fluid('hydrogen', 300)],
        'results': [fluid('ammonia', 200)]})
    # urea: ammonia and CO2 squeezed hot, a batch a second at 256 RPM; a bucket of ammonia makes 40
    G.recipe('pressurizing/urea', {
        'type': 'mcremix:pressurizing', 'heat': 'hot', 'min_speed': 256, 'processingTime': 20,
        'ingredients': [fluid('ammonia', 100), fluid('carbon_dioxide', 50)],
        'results': [item('mcremix:urea', 4), fluid('minecraft:water', 50)]})
    # the air column's split, lightest first: nitrogen over the top, argon from the middle, oxygen from the bottom; it
    # boils in the world's warmth, so its heat is never asked for. Argon and oxygen boil 3 degrees apart: four extra
    # sections between them, or liquid nitrogen sent back into the top
    G.recipe('distilling/liquid_air', {
        'type': 'mcremix:distilling', 'heat': 'warm', 'processingTime': 200, 'still': 'air_column',
        'extra_sections': 4, 'reflux': 'mcremix:liquid_nitrogen',
        'ingredients': [fluid('liquid_air', 1000)],
        'results': [fluid('nitrogen', 780), fluid('argon', 10), fluid('oxygen', 210)],
        'boiling_points': [-196, -186, -183], 'close': [False, True, True]})
    # the Ostwald process, done here in one step: ammonia burnt with oxygen over platinum gauze at Searing
    G.recipe('steel_furnace/nitrogen_dioxide', {
        'type': 'mcremix:steel_furnace', 'heat': 'searing', 'processingTime': 100, 'catalyst_lasts': 64000,
        'ingredients': [item('mcremix:platinum_gauze'), fluid('ammonia', 100), fluid('oxygen', 175)],
        'results': [fluid('nitrogen_dioxide', 100), fluid('minecraft:water', 150)]})
    G.recipe('vat_mixing/nitric_acid', {
        'type': 'mcremix:vat_mixing',
        'ingredients': [fluid('nitrogen_dioxide', 250), fluid('minecraft:water', 250)],
        'results': [fluid('nitric_acid', 500)]})
    G.recipe('vat_mixing/sodium_nitrite', {
        'type': 'mcremix:vat_mixing', 'processingTime': 100,
        'ingredients': [fluid('nitrogen_dioxide', 250), fluid('caustic_soda', 250)],
        'results': [item('mcremix:sodium_nitrite', 2), fluid('minecraft:water', 250)]})
    # Create's red sand washing, platinum now beside its gold nuggets and bauxite
    G.write_json(G.data('create', 'recipes', 'splashing', 'red_sand.json'), {
        'type': 'create:splashing', 'ingredients': [item('minecraft:red_sand')],
        'results': [{'item': 'minecraft:gold_nugget', 'count': 3, 'chance': 0.125},
                    {'item': 'mcremix:bauxite', 'chance': 0.25},
                    {'item': 'mcremix:platinum_nugget', 'chance': 0.01},
                    {'item': 'minecraft:dead_bush', 'chance': 0.05}]})
    G.recipe('platinum_gauze', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['NN', 'NN'],
        'key': {'N': {'tag': 'forge:nuggets/platinum'}}, 'result': item('mcremix:platinum_gauze')})
    G.recipe('compacting/iron_catalyst', {
        'type': 'create:compacting',
        'ingredients': [item('minecraft:iron_nugget')] * 4 + [item('mcremix:alumina')],
        'results': [item('mcremix:iron_catalyst', 2)]})
    sheet = {'tag': 'forge:plates/aluminium'}
    G.recipe('expansion_valve', {
        'type': 'minecraft:crafting_shapeless', 'category': 'building',
        'ingredients': [item('create:fluid_valve'), item('mcremix:aluminium_fluid_tank'), sheet, sheet],
        'result': item('mcremix:expansion_valve')})
    G.recipe('aluminium_column_section', {
        'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': [' S ', 'STS', ' S '],
        'key': {'S': sheet, 'T': item('mcremix:aluminium_fluid_tank')},
        'result': item('mcremix:aluminium_column_section', 2)})


# ------------------------------------------------------------------------------------------------ tags

def tags():
    m = lambda names: [f'mcremix:{n}' for n in names]
    D.add_tag('minecraft', 'blocks', 'mineable/pickaxe', m(BLOCKS))
    # aluminium, as its pipes and tanks
    D.add_tag('mcremix', 'blocks', 'fluid_ceiling/warm', m(BLOCKS))
    D.add_tag('mcremix', 'fluids', 'cryogenic', m(['liquid_air', 'liquid_nitrogen']))
    # what cracks in that cold: steel and iron pipes, pumps, electric pumps and tanks, and PVC pipes
    D.add_tag('mcremix', 'blocks', 'cracks_in_cold',
              G.fluid_blocks('steel') + G.fluid_blocks('iron') + G.PVC_BLOCKS)
    D.add_tag('forge', 'items', 'nuggets/platinum', ['mcremix:platinum_nugget'])
    D.add_tag('forge', 'items', 'nuggets', ['mcremix:platinum_nugget'])
    D.add_tag('minecraft', 'damage_type', 'bypasses_armor', m(['ammonia', 'nitrogen_dioxide', 'nitrogen', 'argon']))


def damage_types():
    for name in ('nitrogen', 'argon', 'ammonia', 'nitrogen_dioxide'):
        G.write_json(G.data('mcremix', 'damage_type', name + '.json'),
                     {'message_id': f'mcremix.{name}', 'scaling': 'when_caused_by_living_non_player',
                      'exhaustion': 0.0})


def loot():
    for name in BLOCKS:
        G.write_json(G.data('mcremix', 'loot_tables', 'blocks', name + '.json'), G.loot_self(name))


# ------------------------------------------------------------------------------------------------ structures

def block_pos(pos):
    x, y, z = pos
    return ('compound', {'X': ('int', x), 'Y': ('int', y), 'Z': ('int', z)})


def pipe(*sides, block='create:fluid_pipe'):
    return (block, dict({d: 'true' if d in sides else 'false'
                         for d in ('north', 'south', 'east', 'west', 'up', 'down')}, waterlogged='false'))


def column(b, origin, height, nozzles):
    """A one-wide aluminium column already joined, as Ponder's world forms none of its own."""
    ox, oy, oz = origin
    for y in range(height):
        pos = (ox, oy + y, oz)
        props = {'north': 'false', 'east': 'false', 'south': 'false', 'west': 'false', 'frost': '0',
                 'down': 'true' if y else 'false'}
        for side in nozzles.get(pos, ()):
            props[side] = 'true'
        if y == 0:
            nbt = {'id': ('string', 'mcremix:aluminium_column_section'), 'Size': ('int', 1), 'Height': ('int', height)}
        else:
            nbt = {'id': ('string', 'mcremix:aluminium_column_section'), 'Controller': block_pos(origin)}
        b[pos] = ('mcremix:aluminium_column_section', props, nbt)


def ponder_schematics():
    """
    liquefier/intro: a 7x7 plate; a Pressurizer facing north at (2,1,3) driven by a shaft from the west, its vessel
    above with an encased fan blowing into its south side; hot compressed air piped east into a steel condenser lying
    east-west at (4,2,3), coolant piped into its top; on east into the expansion valve at (6,2,3), which gives liquid
    air down into a tank south of its bottom, and air out of its top, piped round the north back into the vessel.
    air_column/intro: a 7x7 plate and a one-wide aluminium column seven high at (3,1,3): liquid air fed into its bottom's
    north side, oxygen drawn west from its bottom, argon west from its sixth section, nitrogen west from its top, and
    liquid nitrogen reflux into its top's north side. cold_lines/intro: a 5x5 plate, a steel pipe line along z=1 and a
    copper one along z=3, each fed from a tank at its west end.
    """
    b = {}
    G.plate(b, 7)
    b[(1, 1, 3)] = ('create:shaft', {'axis': 'x', 'waterlogged': 'false'})
    b[(2, 1, 3)] = ('mcremix:pressurizer', {'facing': 'north', 'vessel': 'true'})
    b[(2, 2, 3)] = ('mcremix:pressurizer_vessel', {'north': 'true', 'east': 'true', 'south': 'false', 'west': 'false'})
    b[(2, 2, 4)] = ('create:encased_fan', {'facing': 'north'})
    b[(3, 2, 3)] = pipe('east', 'west')
    b[(4, 2, 3)] = ('mcremix:steel_condenser', {'axis': 'x', 'against': 'none', 'patina': 'none'})
    b[(4, 3, 3)] = pipe('up', 'down')
    b[(5, 2, 3)] = pipe('east', 'west')
    b[(6, 2, 3)] = ('mcremix:expansion_valve', {'facing': 'south', 'frosted': 'false'})
    b[(6, 1, 3)] = pipe('up', 'south')
    b[(6, 1, 4)] = ('create:fluid_tank', {'top': 'true', 'bottom': 'true', 'shape': 'window'}, G.lone_tank_nbt((6, 1, 4)))
    b[(6, 3, 3)] = pipe('down', 'north')
    b[(6, 3, 2)] = pipe('south', 'west')
    for x in (3, 4, 5):
        b[(x, 3, 2)] = pipe('east', 'west')
    b[(2, 3, 2)] = pipe('east', 'down')
    b[(2, 2, 2)] = pipe('up', 'south')
    G.save_structure('liquefier/intro', (7, 5, 7), b)

    b = {}
    G.plate(b, 7)
    column(b, (3, 1, 3), 7, {(3, 1, 3): ('north', 'west'), (3, 6, 3): ('west',), (3, 7, 3): ('north', 'west')})
    b[(3, 1, 2)] = pipe('north', 'south')
    b[(3, 1, 1)] = pipe('north', 'south')
    for y in (1, 6, 7):
        b[(2, y, 3)] = pipe('east', 'west')
    b[(3, 7, 2)] = pipe('north', 'south')
    G.save_structure('air_column/intro', (7, 9, 7), b)

    b = {}
    G.plate(b)
    b[(0, 1, 1)] = ('create:fluid_tank', {'top': 'true', 'bottom': 'true', 'shape': 'window'}, G.lone_tank_nbt((0, 1, 1)))
    b[(0, 1, 3)] = ('create:fluid_tank', {'top': 'true', 'bottom': 'true', 'shape': 'window'}, G.lone_tank_nbt((0, 1, 3)))
    for x in (1, 2, 3, 4):
        b[(x, 1, 1)] = pipe('east', 'west', block='mcremix:steel_fluid_pipe')
        b[(x, 1, 3)] = pipe('east', 'west')
    G.save_structure('cold_lines/intro', (5, 3, 5), b)


# ------------------------------------------------------------------------------------------------ lang

def lang():
    melts = ' Like all aluminium it _never rusts_, and _melts_ with anything _Hot_ beside it.'
    text = {
        # blocks
        'block.mcremix.aluminium_column_section': 'Aluminium Column Section',
        'block.mcremix.aluminium_column_section.tooltip.summary': (
            'A riveted, flanged section of an _air column_, which splits _liquid air_. Stack them _up to sixteen high_ '
            'and _one to three across_; pipe or pour _liquid air_ into the bottom section. It needs _no heat_: liquid '
            'air boils in the world\'s warmth, _5 mB a tick_ for every block of its floor. _Nitrogen_ comes from the '
            'top section, _argon_ from the one below it, and _oxygen_ from the bottom, all as gases. It frosts over as '
            'it runs.' + melts),
        'block.mcremix.aluminium_column_section.tooltip.condition1': 'Argon and oxygen',
        'block.mcremix.aluminium_column_section.tooltip.behaviour1': (
            'They boil only _3 °C apart_, so they need _four sections_ between them: _seven_ in all. Short of them, '
            'part of the argon _escapes_ over the top. _Liquid nitrogen_ piped into the top section as _reflux_ stands '
            'in for them, one for each _half_ of the reflux ratio, and boils back into _nitrogen_, at a cost in pace.'),
        'block.mcremix.expansion_valve': 'Expansion Valve',
        'block.mcremix.expansion_valve.tooltip.summary': (
            'Where air is _liquefied_. Pipe _compressed air_ or _compressed nitrogen_ into any _side_: it expands '
            'through the valve and a _fifth_ of it turns _liquid_, coming out of the _bottom_. The rest comes out of '
            'the _top_ as the gas again, cold: pipe it back into the _Pressurizer_ to go round again. With nothing on '
            'its top, that gas is _let go_.' + melts),
        'block.mcremix.expansion_valve.tooltip.condition1': 'The liquefier',
        'block.mcremix.expansion_valve.tooltip.behaviour1': (
            'An _encased fan_ blowing into a _Pressurizer\'s_ vessel feeds it _air_, which it squeezes into _hot '
            'compressed air_ from _256 RPM_. A _steel condenser_ with _coolant_ in its jacket chills that into '
            '_compressed air_, and this valve liquefies it. A _steel Pressurizer_ at full speed makes a bucket of '
            '_liquid air_ every _12.5 seconds_.'),
        # items
        'item.mcremix.platinum_nugget': 'Platinum Nugget',
        'item.mcremix.platinum_nugget.tooltip.summary': (
            'Now and then _washed out of red sand_, beside its gold. Four make a _platinum gauze_.'),
        'item.mcremix.platinum_gauze': 'Platinum Gauze',
        'item.mcremix.platinum_gauze.tooltip.summary': (
            'Lies in a _Steel Furnace\'s_ chamber, as copper mesh does: _ammonia_ and _oxygen_ burnt over it at '
            '_Searing_ give _nitrogen dioxide_ and water. It lasts _64 buckets_ of ammonia.'),
        'item.mcremix.iron_catalyst': 'Iron Catalyst',
        'item.mcremix.iron_catalyst.tooltip.summary': (
            'Iron nuggets _pressed_ with _alumina_. Dropped into a _Pressurizer\'s_ vessel, by hand or funnel, it is '
            'what _nitrogen_ and _hydrogen_ are squeezed over, _Hot_, into _ammonia_. A pellet lasts _16 buckets_ of '
            'ammonia.'),
        'item.mcremix.urea': 'Urea',
        'item.mcremix.urea.tooltip.summary': (
            '_Bulk bone meal_, squeezed _Hot_ out of _ammonia_ and _CO2_. On a _crop_ it grows it _one stage_, every '
            'time; on anything else bone meal works on it does what _one bone meal_ does. _Dispensers_ use it too.'),
        'item.mcremix.sodium_nitrite': 'Sodium Nitrite',
        'item.mcremix.sodium_nitrite.tooltip.summary': (
            'White crystals, stirred out of _nitrogen dioxide_ and _caustic soda_ in a _vat_.'),
        # gases
        'entity.mcremix.oxygen_cloud': 'Oxygen',
        'entity.mcremix.nitrogen_cloud': 'Nitrogen',
        'entity.mcremix.argon_cloud': 'Argon',
        'entity.mcremix.ammonia_cloud': 'Ammonia',
        'entity.mcremix.nitrogen_dioxide_cloud': 'Nitrogen Dioxide',
        'death.attack.mcremix.nitrogen': '%1$s ran out of air in nitrogen',
        'death.attack.mcremix.nitrogen.player': '%1$s ran out of air in nitrogen while fighting %2$s',
        'death.attack.mcremix.argon': '%1$s ran out of air in argon',
        'death.attack.mcremix.argon.player': '%1$s ran out of air in argon while fighting %2$s',
        'death.attack.mcremix.ammonia': '%1$s breathed in ammonia',
        'death.attack.mcremix.ammonia.player': '%1$s breathed in ammonia while fighting %2$s',
        'death.attack.mcremix.nitrogen_dioxide': '%1$s breathed in nitrogen dioxide',
        'death.attack.mcremix.nitrogen_dioxide.player': '%1$s breathed in nitrogen dioxide while fighting %2$s',
        # cold lines
        'mcremix.tooltip.cracks_in_cold': 'Cracks with liquid air or liquid nitrogen in it',
        # goggles
        'mcremix.goggles.air_column.boiling': 'Boiling in the air\'s warmth: %s mB a tick',
        'mcremix.goggles.expansion_valve.idle': 'Idle: pipe compressed air or nitrogen into a side',
        'mcremix.goggles.expansion_valve.working': 'Liquefying',
        'mcremix.goggles.expansion_valve.ready': 'Ready',
        'mcremix.goggles.expansion_valve.full': 'Full: draw the liquid off its bottom',
        'mcremix.goggles.expansion_valve.gas_lost': '%s mB a second let go: nothing on its top draws it off',
        'mcremix.goggles.expansion_valve.gas_full': '%s mB a second let go: what is on its top is full',
        'mcremix.goggles.expansion_valve.holding': '%s %s / %s mB',
        'mcremix.goggles.steel_condenser.needs_coolant': 'Too warm: only coolant chills compressed gas cold enough',
        'mcremix.goggles.pressurizer.no_catalyst': 'Needs %s in its vessel',
        'mcremix.goggles.pressurizer.waiting_for': 'Needs %s mB of %s with it',
        'mcremix.goggles.pressurizer.catalyst': '%s %s in its vessel, %s%% worn',
        'mcremix.goggles.steel_furnace.oxygen': 'Fed oxygen: twice as hot, up to %s',
        'mcremix.goggles.arc_furnace.argon': 'Argon: %s / %s mB, half again from each sponge',
        # the machines that changed
        'block.mcremix.pressurizer.tooltip.behaviour1': (
            '_Ethylene_ into _polyethylene pellets_ at _512 RPM_, with _heat_ under it. _Vinyl chloride_ into _PVC '
            'resin_ at _256 RPM_, heated or not. _Water_ into _pressurised steam_ at _128 RPM_, over a _Hot_ fire. '
            '_Air_ or _nitrogen_ into their _hot compressed_ forms at _256 RPM_. _Nitrogen_ with _hydrogen_ into '
            '_ammonia_ over _iron catalyst_, and _ammonia_ with _CO2_ into _urea_, both at _256 RPM_ over a _Hot_ '
            'fire.'),
        'block.mcremix.pressurizer.tooltip.condition3': 'Pipes, funnels and fans',
        'block.mcremix.pressurizer.tooltip.behaviour3': (
            'Fluid goes in at any of the _four sides_ of its _vessel_, which reaches out to meet each pipe, a job\'s '
            '_second fluid_ waiting in a tank of its own, and what it makes is drawn off the same sides: fluid by '
            '_pumps_, items by _funnels_ or an _empty hand_. A _catalyst_ goes in by hand or funnel. An _encased fan_ '
            'blowing into a side feeds it _air_, a mB a tick for every _16 RPM_. Its _shaft_ goes in at _either side_ '
            'of the lower block, left or right of the dial.'),
        'block.mcremix.water_cooler.tooltip.summary': (
            'A heat sink with a _water jacket_. Bolted onto a _Power Grid_ device or an _aluminium_ or _titanium '
            'support_, it draws the heat into _water_ piped through it, faster than any heat sink, and gives it back as '
            '_warm water_. _Cold water_ cools twice as fast, and _coolant_ three times as fast, coming out _hot_. '
            '_Liquid nitrogen_ cools _six times_ as fast and boils off as _nitrogen_.'),
        'block.mcremix.arc_furnace.tooltip.condition3': 'Shield gas',
        'block.mcremix.arc_furnace.tooltip.behaviour3': (
            'Pipe _argon_ into its _sides_: each sponge melted with _25 mB_ of it there gives _half an ingot more_, '
            'so two sponges give _three ingots_.'),
        # JEI
        'mcremix.jei.liquefying': 'Liquefying',
        'mcremix.jei.liquefying.share': 'Up to %s mB a tick goes through: a fifth turns liquid and runs out of the '
                                        'bottom, the rest comes out of the top',
        'mcremix.jei.liquefying.round': 'Pipe it back to the Pressurizer to go round again',
        'mcremix.jei.distilling.air_column': 'In the aluminium air column only: %s sections, every cut drawn off its own',
        'mcremix.jei.distilling.air_column_reflux': 'Or %s with liquid nitrogen piped into the top at a reflux ratio of %s',
        'mcremix.jei.distilling.close_air_column': 'Boils close to its neighbour: %s more sections between them, or '
                                                   'liquid nitrogen reflux',
        'mcremix.jei.distilling.no_heat': 'No heat: it boils in the world\'s warmth',
        'mcremix.jei.pressurizing.catalyst': 'Dropped into the vessel; one wears away every %s buckets it makes',
        'mcremix.jei.cooling.coolant_only': 'Coolant only: water leaves it too warm to liquefy',
        'mcremix.jei.steel_furnace_fuel.oxygen': 'With a mB of oxygen piped in for every %s mB of it, it burns twice as '
                                                 'hot, and a level hotter at most',
        'mcremix.jei.arc_furnace.argon': 'With %s mB of argon in its sides, each sponge gives half an ingot more',
        'mcremix.jei.info.water_cooler': (
            'Bolt it onto a Power Grid device or an aluminium or titanium support and pipe water, cold water, coolant or '
            'liquid nitrogen through it. It takes heat in step with how far above the air that is: on water 15 W for '
            'each degree, on cold water twice that, on coolant three times, on liquid nitrogen six. Water comes out '
            'warm, coolant hot, and liquid nitrogen boils off as nitrogen.'),
        'mcremix.jei.info.liquid_nitrogen': (
            'Made by squeezing nitrogen in a Pressurizer, cooling it on coolant in a steel condenser and letting it '
            'through an expansion valve. It is the coldest coolant for a water cooler, and the reflux of the aluminium '
            'air column. Steel, iron and PVC crack with it in them; copper, aluminium, glass and the column take it. '
            'Spilt, it boils away in a moment, freezing water and putting out fire beside it.'),
        'mcremix.jei.info.liquid_air': (
            'Made by squeezing air in a Pressurizer, cooling it on coolant in a steel condenser and letting it through '
            'an expansion valve. The aluminium air column splits it into nitrogen, argon and oxygen. Steel, iron and '
            'PVC crack with it in them; copper, aluminium, glass and the column take it.'),
    }
    D.merge_json(G.asset('lang', 'en_us.json'), lambda obj: obj.update(text))

"""
Resources for vacuum sealing (0.37.0, Taran): a Pressurizer turned the other way pulls a vacuum in its vessel and, over
Hot heat, seals Power Grid's bulbs and tubes in it in place of their hand recipes (pressurizer/VacuumSealingRecipe.java).
Its recipes, the hand recipes it replaces, and its text; the Ponder scene's text is gen_assets' ponder_lang's, from
PressurizerScenes.java, and the dial's blue vacuum zone is drawn in gen_assets' pressurizer_dial_texture. gen_assets.py
calls run() after gen_coiling, passing itself; this file adds to the lang file written before it rather than owning any
shared file.
"""
import gen_decor as D

G = None  # gen_assets, set by run()

# the speed each depth of vacuum takes, and how long a seal takes at it, in ticks
BULB, TUBE, CRT = 128, 256, 512
SEAL_TICKS, CRT_TICKS = 100, 200


def run(gen):
    global G
    G = gen
    recipes()
    lang()


def seals():
    """(what it seals, its least speed, ticks at that speed, what goes into the vessel): Power Grid's hand recipes'
    ingredients, and a glass pane for each tube whose hand recipe had no glass to seal it in."""
    item = G.item
    pane, glass = item('minecraft:glass_pane'), item('minecraft:glass')
    plate, wire = {'tag': 'forge:plates/iron'}, {'tag': 'forge:wires/iron'}
    glowstone, quartz, amethyst = item('minecraft:glowstone_dust'), item('minecraft:quartz'), item(
        'minecraft:amethyst_shard')
    return [
        ('light_bulb', BULB, SEAL_TICKS, [pane] * 3 + [wire, plate]),
        ('lv_light_bulb', BULB, SEAL_TICKS, [pane] * 3 + [{'tag': 'minecraft:coals'}, plate]),
        ('neon_bulb', BULB, SEAL_TICKS, [glass, glowstone, plate]),
        ('growth_lamp', BULB, SEAL_TICKS, [pane] * 4 + [quartz, wire, plate]),
        ('triode', TUBE, SEAL_TICKS, [pane, item('create:polished_rose_quartz'), glowstone, plate]),
        ('pentode', TUBE, SEAL_TICKS, [item('powergrid:triode'), amethyst, plate]),
        ('thyratron', TUBE, SEAL_TICKS, [pane, item('powergrid:heavy_wire_connector'), item('minecraft:ender_pearl'),
                                         plate]),
        ('barretter_tube', TUBE, SEAL_TICKS, [pane, quartz, wire, plate]),
        ('regulator_tube', TUBE, SEAL_TICKS, [pane, amethyst, glowstone, plate]),
        ('crt', CRT, CRT_TICKS, [{'tag': 'forge:copper_coils'}] * 4 + [glass] * 4
         + [item('create:electron_tube'), glowstone]),
    ]


def recipes():
    for name, speed, ticks, ingredients in seals():
        G.recipe('vacuum_sealing/' + name, {
            'type': 'mcremix:vacuum_sealing', 'ingredients': ingredients,
            'results': [G.item('powergrid:' + name)], 'min_speed': speed, 'heat': 'hot', 'processingTime': ticks})
        G.disabled('powergrid', ('mechanical_crafting/' if name == 'crt' else 'crafting/') + name)


# ------------------------------------------------------------------------------------------------ text

def lang():
    text = {}
    for block, condition in (('pressurizer', 5), ('titanium_pressurizer', 3)):
        key = f'block.mcremix.{block}.tooltip.'
        text[key + f'condition{condition}'] = 'Turned the other way'
        text[key + f'behaviour{condition}'] = (
            'Its pump _pulls the air out_ of its vessel instead, the needle dropping _below zero_ into the _blue_, and '
            'it _seals_ what lies in the vessel over a _Hot_ fire that melts the glass shut: Power Grid\'s _bulbs_ from '
            '_128 RPM_, its _tubes_ from _256_ and the _cathode ray tube_ from _512_, twice as fast at twice the speed. '
            'Put the parts in by _hand_ or _funnel_, a kind of thing to each of its _four slots_.')
    g = 'mcremix.goggles.pressurizer.'
    text.update({
        g + 'sealing': 'Sealing %s in a vacuum',
        g + 'vacuum': 'Pulling a vacuum, with nothing to seal',
        g + 'seal_waiting': 'Still wants %s to seal',
        g + 'turn_to_squeeze': 'Pulling a vacuum: turn it the other way to squeeze',
        g + 'turn_to_seal': 'Squeezing: turn it the other way to seal',
        g + 'to_seal': 'Holds %s %s to seal',
        'mcremix.jei.vacuum_sealing': 'Vacuum Sealing',
        'mcremix.jei.vacuum_sealing.speed': (
            'Turned the other way from squeezing, at least %s RPM: the faster it turns, the deeper the vacuum, and '
            'twice as fast seals twice as quick'),
        'mcremix.jei.vacuum_sealing.heat': 'The heat under the Pressurizer that melts the glass shut, at least; any '
                                           'hotter works the same',
    })
    D.merge_json(G.asset('lang', 'en_us.json'), lambda obj: obj.update(text))

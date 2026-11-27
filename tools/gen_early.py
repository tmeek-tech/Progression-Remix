"""
Resources for the first hour's two changes (0.41.0, Taran's ask of 2026-11-26): bread from the mill, and flint tools in
place of wooden and stone ones. gen_assets.py calls run() after gen_powergrid, passing itself; this file adds to the
lang, tag and loot modifier files written before it rather than owning any shared file.

Bread: vanilla's bread and cake recipes are off and its cookies take flour, so wheat is milled into flour first (a
millstone or crushing wheels, Create's own recipe), worked into dough with water and baked on a campfire or by fan
smoking (Create's recipes). Besides Create's water bucket, mixer and fan, a water bottle kneads flour into dough and is
given back (food/KeepBottleRecipe), so the first bread needs glass, not iron. Create's cake takes dough.

Flint: flint knapped on stone (flint/Knapping) gives Knapped Flint, which makes a pickaxe, hatchet, shovel, hoe and
knife in vanilla's shapes (flint/FlintTools), but for the hatchet: logs break only with an axe (flint/LogsNeedAnAxe,
the mcremix:needs_an_axe tag written here), so the hatchet comes first and crafts in the inventory's 2x2 grid, from
knapped flint and a stick that leaves drop more often (flint/LeafSticksModifier, written here). Wooden and stone tools
are no longer made: their recipes are off, the loot modifier written here swaps them for flint ones in any loot, and
the code takes them out of JEI, the creative tabs, mobs' hands and trades (flint/OldTools). The Ponder scene's text is
gen_assets' ponder_lang's, from FlintScenes.java.
"""
from PIL import Image

import gen_decor as D

G = None  # gen_assets, set by run()

TOOLS = ['pickaxe', 'hatchet', 'shovel', 'hoe', 'knife']
# what each replaces: wooden and stone tools by their vanilla name
REPLACES = {'pickaxe': 'pickaxe', 'hatchet': 'axe', 'shovel': 'shovel', 'hoe': 'hoe', 'knife': 'sword'}
# vanilla's item tag for each kind of tool
KIND_TAGS = {'pickaxe': 'pickaxes', 'hatchet': 'axes', 'shovel': 'shovels', 'hoe': 'hoes', 'knife': 'swords'}


def run(gen):
    global G
    G = gen
    textures()
    models()
    recipes()
    tags()
    loot()
    advancements()
    weapon_attributes()
    ponder_schematics()
    lang()


# ------------------------------------------------------------------------------------------------ textures

# flint, dark and glassy, lit from the top left: outline, dark, mid-dark, mid, light, the glint along a knapped edge
FLINT = {'O': (17, 17, 21), 'D': (36, 36, 42), 'd': (51, 51, 58), 'M': (69, 69, 78), 'L': (102, 103, 114),
         'W': (152, 155, 166)}
# a stick for the handle: its outline, then dark, mid and light
WOOD = {'o': (40, 29, 13), 'h': (77, 56, 26), 'w': (108, 80, 40), 'l': (141, 107, 58)}
# the cord lashing a head to its handle
CORD = {'C': (110, 92, 60), 'c': (176, 158, 114)}
PALETTE = dict(FLINT, **WOOD, **CORD)

# Each tool is a head drawn over a handle: the handle runs from the bottom left corner up to the row given, as
# vanilla's tools do, and the head (flint and its lashing) is drawn over it. '.' leaves what is under it.
SPRITES = {
    'knapped_flint': (None, [
        '................',
        '................',
        '............OO..',
        '...........OWO..',
        '..........OWLDO.',
        '.........OWMLdO.',
        '........OWLMdDO.',
        '.......OWMLdMDO.',
        '......OLWMdMDO..',
        '.....OWLMLdDDO..',
        '....OLWMdMDDO...',
        '....OWMLdDDO....',
        '...OLMdMDDO.....',
        '...OMdDDOO......',
        '...OOOO.........',
        '................']),
    'flint_pickaxe': (5, [
        '................',
        '................',
        '.....OOOOOO.....',
        '....OWLLLMMOO...',
        '...OWLMMdddDcO..',
        '....OOODDOOCcCO.',
        '...........OMdDO',
        '............OMdO',
        '............OdDO',
        '............OMdO',
        '............OdDO',
        '.............OO.']),
    'flint_hatchet': (3, [
        '................',
        '.......OOO......',
        '......OWLLO.....',
        '.....OWLMMdO....',
        '.....OLMMddCcC..',
        '.....OLMdDDcCo..',
        '......OdDDOCcO..',
        '.......OOO......']),
    'flint_shovel': (9, [
        '................',
        '................',
        '...........OOO..',
        '..........OWLLO.',
        '.........OWLMMdO',
        '.........OLMMdDO',
        '.........OLMdDO.',
        '..........OdDO..',
        '.........CcCO...',
        '.........cC.....']),
    'flint_hoe': (3, [
        '................',
        '......OOOO......',
        '.....OWLLLOO....',
        '......OMdDDcCO..',
        '.......OOOOCcCo.']),
    'flint_knife': (11, [
        '................',
        '................',
        '............OO..',
        '...........OWLO.',
        '..........OWLMO.',
        '.........OWLMdO.',
        '........OWLMdO..',
        '.......OWLMdO...',
        '......OLMdDO....',
        '......OMdDO.....',
        '.....CcCOO......',
        '....cCc.........']),
}


def handle(top):
    """A stick from the bottom left corner up to row `top`, two pixels wide with an outline on its lower edge."""
    pixels = {}
    for y in range(top, 14):
        x = 15 - y
        pixels[(x, y)] = 'h'
        pixels[(x + 1, y)] = 'l' if y % 2 else 'w'
        pixels[(x + 2, y)] = 'o'
    pixels[(2, 14)] = pixels[(3, 14)] = 'o'
    return pixels


def sprite(name):
    top, rows = SPRITES[name]
    pixels = handle(top) if top is not None else {}
    for y, row in enumerate(rows):
        if len(row) != 16:
            raise SystemExit(f'{name}: row {y} is {len(row)} wide')
        for x, ch in enumerate(row):
            if ch != '.':
                pixels[(x, y)] = ch
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for (x, y), ch in pixels.items():
        img.putpixel((x, y), PALETTE[ch] + (255,))
    return img


def textures():
    for name in SPRITES:
        G.save_png(sprite(name), G.asset('textures', 'item', name + '.png'))


def models():
    D.item_model('knapped_flint', {'parent': 'minecraft:item/generated',
                                   'textures': {'layer0': 'mcremix:item/knapped_flint'}})
    for tool in TOOLS:
        D.item_model('flint_' + tool, {'parent': 'minecraft:item/handheld',
                                       'textures': {'layer0': f'mcremix:item/flint_{tool}'}})


# ------------------------------------------------------------------------------------------------ data

def recipes():
    flint, stick = G.item('mcremix:knapped_flint'), {'tag': 'forge:rods/wooden'}
    shapes = {
        'pickaxe': ['KKK', ' S ', ' S '],
        'hatchet': ['KK', 'KS'],  # 2x2, so it can come first: logs need an axe
        'shovel': ['K', 'S', 'S'],
        'hoe': ['KK', ' S', ' S'],
        'knife': ['K', 'S'],
    }
    for tool, pattern in shapes.items():
        G.recipe('crafting/flint_' + tool, {
            'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': pattern,
            'key': {'K': flint, 'S': stick}, 'result': G.item('mcremix:flint_' + tool)})
    for material in ('wooden', 'stone'):
        for kind in REPLACES.values():
            G.disabled('minecraft', f'{material}_{kind}')
    # the recipe book learns each once there is Knapped Flint to make it with, as vanilla's did with cobblestone
    for tool in TOOLS:
        unlock('combat' if tool == 'knife' else 'tools', 'crafting/flint_' + tool, 'has_knapped_flint',
               {'items': ['mcremix:knapped_flint']})

    # bread is baked from dough, and dough is milled flour and water (Create's recipes); vanilla's three wheat is off,
    # and so is its wheat cake, Create's dough cake standing in for it
    G.disabled('minecraft', 'bread')
    G.disabled('minecraft', 'cake')
    G.write_json(G.data('minecraft', 'recipes', 'cookie.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['#X#'],
        'key': {'#': {'tag': 'forge:flour/wheat'}, 'X': G.item('minecraft:cocoa_beans')},
        'result': G.item('minecraft:cookie', 8), 'show_notification': True})
    # a water bottle kneads flour into dough as Create's water bucket does, and is given back
    G.recipe('crafting/dough_from_water_bottle', {
        'type': 'mcremix:crafting_shapeless_keep_bottle', 'category': 'misc',
        'ingredients': [{'tag': 'forge:flour/wheat'},
                        {'type': 'forge:partial_nbt', 'item': 'minecraft:potion', 'nbt': {'Potion': 'minecraft:water'}}],
        'result': G.item('create:dough')})
    unlock('food', 'crafting/dough_from_water_bottle', 'has_flour', {'tag': 'forge:flour/wheat'})


def unlock(group, recipe, criterion, items):
    """A recipe advancement as vanilla's: the recipe book learns `recipe` once the player holds `items`."""
    G.write_json(G.data('mcremix', 'advancements', 'recipes', group, recipe.split('/')[-1] + '.json'), {
        'parent': 'minecraft:recipes/root',
        'criteria': {
            criterion: {'conditions': {'items': [items]}, 'trigger': 'minecraft:inventory_changed'},
            'has_the_recipe': {'conditions': {'recipe': 'mcremix:' + recipe}, 'trigger': 'minecraft:recipe_unlocked'}},
        'requirements': [[criterion, 'has_the_recipe']],
        'rewards': {'recipes': ['mcremix:' + recipe]},
        'sends_telemetry_event': False})


def tags():
    tools = [f'mcremix:flint_{tool}' for tool in TOOLS]
    D.add_tag('mcremix', 'items', 'flint_tools', tools)
    for tool in TOOLS:
        D.add_tag('minecraft', 'items', KIND_TAGS[tool], [f'mcremix:flint_{tool}'])
    # vanilla lists its pickaxes by name for amethyst clusters to drop all four shards
    D.add_tag('minecraft', 'items', 'cluster_max_harvestables', ['mcremix:flint_pickaxe'])
    # what flint is knapped on: natural stone of the overworld and the nether, cobblestone, and the stone anvils
    D.add_tag('mcremix', 'blocks', 'knapping_stones', [
        '#minecraft:base_stone_overworld', '#minecraft:base_stone_nether', '#forge:cobblestone', '#forge:stone',
        'minecraft:smooth_stone', 'mcremix:stone_anvil', 'mcremix:chipped_stone_anvil', 'mcremix:damaged_stone_anvil'])
    # nothing needs flint in particular (FlintTools.FLINT ranks after stone, so it mines whatever stone did)
    D.add_tag('mcremix', 'blocks', 'needs_flint_tool', [])
    # what bare hands, or any tool but an axe, make no headway on (flint/LogsNeedAnAxe)
    D.add_tag('mcremix', 'blocks', 'needs_an_axe', ['#minecraft:logs'])


def loot():
    G.write_json(G.data('mcremix', 'loot_modifiers', 'swap_old_tools.json'),
                 {'type': 'mcremix:swap_old_tools', 'conditions': []})
    # leaves drop a stick a tenth of the time on top of vanilla's fiftieth, unless taken whole by shears or silk touch
    G.write_json(G.data('mcremix', 'loot_modifiers', 'leaf_sticks.json'),
                 {'type': 'mcremix:leaf_sticks', 'conditions': [{'condition': 'minecraft:survives_explosion'}],
                  'chance': 0.1})
    D.merge_json(G.data('forge', 'loot_modifiers', 'global_loot_modifiers.json'),
                 lambda obj: obj['entries'].extend(['mcremix:swap_old_tools', 'mcremix:leaf_sticks']))


def advancements():
    """Vanilla's first tool advancements, pointed at flint: getting an upgrade is the flint pickaxe now, the stone age
    shows one, and a flint hatchet scrapes wax off copper as any axe does."""
    pick = {'item': 'mcremix:flint_pickaxe', 'nbt': '{Damage:0}'}
    G.write_json(G.data('minecraft', 'advancements', 'story', 'mine_stone.json'), {
        'parent': 'minecraft:story/root',
        'criteria': {'get_stone': {'conditions': {'items': [{'tag': 'minecraft:stone_tool_materials'}]},
                                   'trigger': 'minecraft:inventory_changed'}},
        'display': {'announce_to_chat': True, 'description': {'translate': 'advancements.story.mine_stone.description'},
                    'frame': 'task', 'hidden': False, 'icon': pick, 'show_toast': True,
                    'title': {'translate': 'advancements.story.mine_stone.title'}},
        'requirements': [['get_stone']], 'sends_telemetry_event': True})
    G.write_json(G.data('minecraft', 'advancements', 'story', 'upgrade_tools.json'), {
        'parent': 'minecraft:story/mine_stone',
        'criteria': {'flint_pickaxe': {'conditions': {'items': [{'items': ['mcremix:flint_pickaxe']}]},
                                       'trigger': 'minecraft:inventory_changed'}},
        'display': {'announce_to_chat': True,
                    'description': {'translate': 'advancements.story.upgrade_tools.description'},
                    'frame': 'task', 'hidden': False, 'icon': pick, 'show_toast': True,
                    'title': {'translate': 'advancements.story.upgrade_tools.title'}},
        'requirements': [['flint_pickaxe']], 'sends_telemetry_event': True})
    waxed = ['minecraft:waxed_' + b for b in (
        'copper_block', 'exposed_copper', 'weathered_copper', 'oxidized_copper', 'cut_copper', 'exposed_cut_copper',
        'weathered_cut_copper', 'oxidized_cut_copper', 'cut_copper_slab', 'exposed_cut_copper_slab',
        'weathered_cut_copper_slab', 'oxidized_cut_copper_slab', 'cut_copper_stairs', 'exposed_cut_copper_stairs',
        'weathered_cut_copper_stairs', 'oxidized_cut_copper_stairs')]
    axes = ['minecraft:wooden_axe', 'minecraft:golden_axe', 'minecraft:stone_axe', 'minecraft:iron_axe',
            'minecraft:diamond_axe', 'minecraft:netherite_axe', 'mcremix:flint_hatchet']
    G.write_json(G.data('minecraft', 'advancements', 'husbandry', 'wax_off.json'), {
        'parent': 'minecraft:husbandry/wax_on',
        'criteria': {'wax_off': {'conditions': {'location': [
            {'condition': 'minecraft:location_check', 'predicate': {'block': {'blocks': waxed}}},
            {'condition': 'minecraft:match_tool', 'predicate': {'items': axes}}]},
            'trigger': 'minecraft:item_used_on_block'}},
        'display': {'announce_to_chat': True, 'description': {'translate': 'advancements.husbandry.wax_off.description'},
                    'frame': 'task', 'hidden': False, 'icon': {'item': 'mcremix:flint_hatchet', 'nbt': '{Damage:0}'},
                    'show_toast': True, 'title': {'translate': 'advancements.husbandry.wax_off.title'}},
        'requirements': [['wax_off']], 'sends_telemetry_event': True})


def weapon_attributes():
    """Better Combat's moves for each, where it is installed: the knife a dagger's, the rest their kind's."""
    for tool, preset in (('knife', 'dagger'), ('hatchet', 'axe'), ('pickaxe', 'pickaxe')):
        G.write_json(G.data('mcremix', 'weapon_attributes', f'flint_{tool}.json'), {'parent': 'bettercombat:' + preset})


def ponder_schematics():
    """flint/knapping: a 5x5 plate, gravel at (1,1,2) to dig and stone at (3,1,2) to strike."""
    b = {}
    G.plate(b, 5)
    b[(1, 1, 2)] = ('minecraft:gravel', {})
    b[(3, 1, 2)] = ('minecraft:stone', {})
    G.save_structure('flint/knapping', (5, 3, 5), b)


# ------------------------------------------------------------------------------------------------ text

def lang():
    text = {
        'item.mcremix.knapped_flint': 'Knapped Flint',
        'item.mcremix.knapped_flint.tooltip.summary': (
            'Flint struck on _stone_ until a sharp flake comes away: use flint on any stone _four times_. Makes '
            '_flint tools_, the _hatchet_ first, since logs need an axe.'),
        'mcremix.jei.knapping': 'Knapping',
        'mcremix.jei.knapping.strikes': '%s strikes on stone',
        'mcremix.logs.need_an_axe': 'That log needs an axe. A flint hatchet will do',
        # vanilla's first tutorial hint says to punch the tree
        'tutorial.find_tree.description': 'Its logs need a flint hatchet',
    }
    summaries = {
        'pickaxe': ('_Sharp but brittle_: mines whatever stone did, _iron ore_ included, as fast as _iron_, but lasts '
                    '_half as long_ as stone.'),
        'knife': ('In place of the sword: it _stabs_ rather than sweeps, a little softer than a stone sword but '
                  'quicker. _Brittle_: lasts _half as long_ as stone.'),
        'hatchet': ('_Logs_ need an axe, so this comes first, and it crafts right in the _inventory grid_. _Sharp but '
                    'brittle_: as fast as _iron_, but lasts _half as long_ as stone.'),
    }
    for tool in TOOLS:
        key = f'item.mcremix.flint_{tool}'
        text[key] = 'Flint ' + tool.capitalize()
        text[key + '.tooltip.summary'] = summaries.get(
            tool, '_Sharp but brittle_: as fast as _iron_, but lasts _half as long_ as stone.')
    D.merge_json(G.asset('lang', 'en_us.json'), lambda obj: obj.update(text))

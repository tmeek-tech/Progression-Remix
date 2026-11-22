#!/usr/bin/env python3
"""Writes the FTB Quests book the mod ships into src/main/resources/mcremix_questbook/ (0.39.0).

Taran's ask (2026-11-18): a quest book that walks through the progression but leaves the solutions to the
player. Taran's cards: the book ships inside the jar (com.mcremix.compat.QuestBook copies it into
config/ftbquests/quests when FTB Quests has no book there yet), each quest gives the idea and not the recipe,
quests give no rewards, and every quest in a chapter shows from the start, locked until the ones before it
are done.

The book lives in this file as data; run it from the project root after any change:
    python3 tools/gen_quests.py
Ids come from each chapter's and quest's key, so they stay put between versions and players keep their
progress when the book updates. Renaming a key gives that quest a new id (and loses its progress).

Text: {Warm}, {Hot}, {Searing}, {Blazing} and {Cold} become the heat words in their colours, and a paragraph
starting with "@" is a grey pointer to JEI or Ponder.

Voice (Taran, 2026-11-22, 0.39.1): short, plain and a little playful. Say what to make and why it works, leave out
what JEI, a tooltip, the chapter or the quest's own title already says, and keep any aside to a few words, never a
joke for its own sake.
"""

import hashlib
import os
import sys

OUT = os.path.join('src', 'main', 'resources', 'mcremix_questbook')
SPACING = 1.75  # grid units between neighbouring quests

HEAT = {
	'{Cold}': '&3Cold&r',
	'{Warm}': '&eWarm&r',
	'{Hot}': '&6Hot&r',
	'{Searing}': '&cSearing&r',
	'{Blazing}': '&bBlazing&r',
}


def make_id(*parts):
	h = int(hashlib.sha1(':'.join(('mcremix',) + parts).encode()).hexdigest()[:16], 16) & 0x7FFFFFFFFFFFFFFF
	return '%016X' % (h or 1)


class Long(int):
	pass


def text(s):
	for k, v in HEAT.items():
		s = s.replace(k, v)
	if '{' in s:
		raise SystemExit('unknown markup in: ' + s)
	return s


def paragraphs(ps):
	out = []
	for p in ps:
		if out:
			out.append('')
		out.append('&7' + text(p[1:].strip()) + '&r' if p.startswith('@') else text(p))
	return out


# ---- tasks

def item(id_, count=1):
	t = {'type': 'item', 'item': id_}
	if count != 1:
		t['count'] = Long(count)
	return t


def look(state, title, icon):
	"""An observation task: look at a block in this state (block[prop=value,...])."""
	return {'type': 'observation', 'observe_type': 2, 'to_observe': state, 'timer': Long(10), 'title': title, 'icon': icon}


def look_tag(tag, title, icon):
	return {'type': 'observation', 'observe_type': 1, 'to_observe': tag, 'timer': Long(10), 'title': title, 'icon': icon}


def structure(tag, title, icon):
	return {'type': 'structure', 'structure': tag, 'title': title, 'icon': icon}


def check(title='Got it'):
	return {'type': 'checkmark', 'title': title}


# ---- chapters and quests

CHAPTERS = []


class Chapter:
	def __init__(self, key, title, subtitle, icon, side=False):
		self.key, self.title, self.subtitle, self.icon, self.side = key, title, subtitle, icon, side
		self.quests = []
		CHAPTERS.append(self)

	def q(self, key, x, y, title, subtitle, desc, tasks, deps=(), icon=None, shape=None, size=None, optional=False):
		self.quests.append(dict(key=key, x=x, y=y, title=title, subtitle=subtitle, desc=desc, tasks=tasks,
			deps=list(deps), icon=icon, shape=shape, size=size, optional=optional))


MILESTONE = dict(shape='hexagon', size=1.5)

# ---------------------------------------------------------------- 1. The Hearth
c = Chapter('hearth', 'The Hearth', 'Everything starts with fire', 'mcremix:kiln')
c.q('welcome', 0, 0, 'Welcome to the Remix', 'How this book works', [
	"This book runs from your first fire to the dye works. Each quest says what to make and why it works, never "
	"how to build it. That part's yours.",
	"JEI has every recipe and the heat, speed or charge it needs. Ponder shows a block at work: hover over an "
	"item and hold W. Engineer's Goggles show what a placed block is doing.",
	"Item quests tick themselves off. If one doesn't, click the task.",
], [check()], icon='minecraft:writable_book', **MILESTONE)
c.q('campfire', 1, -1, 'Food Goes on the Fire', "Vanilla's furnaces have gone cold", [
	"Vanilla's furnace, smoker and blast furnace have gone cold: no menus, no smelting, not even from a hopper.",
	"Cook on a campfire. Later, an Encased Fan blowing through fire smokes food in bulk.",
], [item('minecraft:campfire')], deps=['welcome'])
c.q('furnace', 1, 0, 'The Furnace', 'No menu, and a cut-open front', [
	"The furnace recipe now makes the Furnace, and it has no menu. Click things to fire into the upper chamber "
	"and fuel into the firebox, then watch them glow on the grate.",
	"Hoppers and funnels work it too: things to fire in the top, fuel in the sides, results out the bottom. It "
	"won't take food.",
	"@Ponder: the Furnace.",
], [item('mcremix:kiln')], deps=['welcome'])
c.q('light', 2, 1, 'Light It', 'Fuel waits for a flame', [
	"Fuel doesn't light itself. Flint and steel or a fire charge does, by hand or from a Deployer.",
	"Flint and steel takes iron, which comes later, so go hunting for your first lighter. Ruined portal chests "
	"can hold flint and steel or a fire charge, and mineshafts, dungeons and shipwrecks still keep iron.",
	"Once lit, it burns all its fuel, work or no work, so load it when you mean it. A shovel puts it out and "
	"keeps the fuel.",
	"Heat from the block underneath needs no lighting.",
], [look('mcremix:kiln[lit=true]', 'Look at a lit Furnace', 'minecraft:flint_and_steel')], deps=['furnace'])
c.q('charcoal', 2, 0, 'Warm: Charcoal', 'One scale for every fire', [
	"Every fire sits on one scale: {Warm}, {Hot}, {Searing}, {Blazing}. Each recipe has a minimum, and every "
	"level above it halves the time.",
	"Wood burns {Warm}, as does a campfire or magma block under the Furnace: enough to char logs and fire clay "
	"into bricks and terracotta. A Furnace on a campfire stays {Warm} forever, for free.",
	"@JEI: the Furnace tab gives each firing's heat, and the Furnace Heat tab shows what burns how hot.",
], [item('minecraft:charcoal')], deps=['furnace'])
c.q('hot', 3, 0, 'Hot: Glass and Copper', 'Charcoal and coal burn Hot', [
	"Charcoal and coal burn {Hot}, as does fire or lava under the Furnace. That covers vanilla's smelting that "
	"isn't food: glass, smooth stone, copper, gold. Raw iron, iron ore and ancient debris want hotter fires of "
	"their own.",
	"Copper is your first real metal, for Create's parts and the first tier of your own.",
], [item('minecraft:glass'), item('minecraft:copper_ingot')], deps=['charcoal', 'light'])
c.q('coke', 4, 0, 'Coal Coke', 'Coal baked hotter', [
	"Fire coal at {Hot} and it cokes into Coal Coke, which burns {Searing} and lasts twice as long.",
	"Coking also leaves coal tar in a tank inside the Furnace. Past a bucket it burns off at the chimney, so "
	"ignore it for now. Later, that black goo is one of the most useful things you make.",
], [item('mcremix:coal_coke')], deps=['hot'])
c.q('searing', 5, 0, 'Searing', 'The heat iron needs', [
	"Get a Furnace to {Searing}: Coal Coke or blaze rods in the firebox, or soul fire, a soul campfire or a fed "
	"Blaze Burner underneath.",
	"{Searing} is where iron, steel and brass begin. Goggles show a Furnace's exact heat, its fuel and what each "
	"slot is waiting for, and a comparator reads its heat.",
], [look('mcremix:kiln[heat=3]', 'Look at a Searing Furnace', 'mcremix:coal_coke')], deps=['coke'], **MILESTONE)

# ---------------------------------------------------------------- 2. Turning
c = Chapter('turning', 'Turning', "Create's kinetics, now with consequences", 'create:cogwheel')
c.q('alloy', 0, 0, 'Andesite Alloy', "Create's own tier, steady and slow", [
	"Create's own shafts, cogs, gearboxes and machines are the andesite tier. They never weather and their "
	"shafts take any load, but their supports top out at 128 RPM.",
	"Slow, sturdy and maintenance-free: the workhorse for heavy machines all game.",
], [item('create:andesite_alloy')], deps=['hearth:hot'])
c.q('support', 1, 0, 'Shafts Need Holding', 'Every part turns in a support', [
	"Shafts and cogs don't float any more. Each needs a support: a machine, gearbox, clutch, gearshift, chain "
	"drive, casing, or a bracket bolted to something solid. Water wheels, windmills and engines drive but hold "
	"nothing.",
	"A support holds the parts in line with it as far as their material reaches through the bare parts between, "
	"like scaffolding from a post: two blocks for wood and brass, four for andesite, copper and iron, six for "
	"steel and up. Out of reach, a part won't go on. Lose its support and it drops.",
	"@Ponder: any shaft, bracket or casing.",
], [item('create:shaft')], deps=['alloy'])
c.q('goggles', 1, -1, "Engineer's Goggles", 'See what a block is doing', [
	"Engineer's Goggles show what a placed block is up to: each part's material, what holds it and its limits, a "
	"Furnace's heat and fuel, what a machine is waiting for.",
	"Their lenses sit in gold sheet, and sheets come only off a press, so goggles wait for iron.",
], [item('create:goggles')], deps=['support', 'bloom_iron:press'])
c.q('limits', 2, 0, 'Stability and Strength', 'Speed breaks supports, load breaks shafts', [
	"Every material has two limits. Stability is how fast its supports can turn. Strength is how much load its "
	"shafts and cogs can carry. A part turns as fast as its supports allow, whatever it's made of.",
	"Spin a support past its Stability and it grinds for a few seconds, then drops. Pull more stress through a "
	"shaft or cog than its Strength and it wobbles, then snaps. Speed never breaks shafts, and load never breaks "
	"supports.",
	"Tooltips show both as a word and a bar. Goggles give the numbers.",
], [check()], deps=['support'], icon='create:stressometer')
c.q('wood', 2, -1, 'Wood Is the Fuse', 'The cheapest parts give way first', [
	"All eleven woods make parts, for next to nothing. They're also the weakest: supports stop at 64 RPM, and "
	"the shafts carry less load than anything else.",
	"That makes a wooden shaft a fuse. Put one between an engine and a risky machine, and when the machine gets "
	"greedy, the wood snaps first.",
	"Crimson and warped parts don't burn.",
], [look_tag('#mcremix:wooden_shafts', 'Look at a wooden shaft', 'mcremix:oak_shaft')], deps=['support'],
	optional=True)
c.q('copper', 3, 0, 'Copper Parts', 'Full speed, light duty', [
	"Copper supports spin to 256 RPM, twice andesite, but copper shafts and cogs only carry light loads. It's "
	"your early road to full speed.",
	"Copper weathers within a day, anywhere, and each stage adds a little friction at its supports. Honeycomb "
	"waxes it and an axe scrapes it, same as copper blocks.",
	"Copper gearboxes, clutches, gearshifts and chain drives are built on Create's copper casing.",
], [item('mcremix:copper_shaft')], deps=['limits'])
c.q('weathering', 4, -1, 'Weathering', 'Fast, and never quite all the way', [
	"Copper and iron weather fast, and each block stops at a stage of its own, weathered or fully weathered. "
	"Within a day a build gets a lived-in patchwork instead of one flat green.",
	"Iron only rusts while wet: rained on, waterlogged or beside water. Dry iron under rustier iron rusts too, "
	"so rust runs down a wall. Indoor machines stay clean.",
	"Honeycomb seals a block as it is. Scrape it back a stage with an axe and it weathers back to the same stage.",
], [item('minecraft:honeycomb')], deps=['copper', 'bloom_iron:iron'], optional=True)
c.q('brass', 4, 0, 'Brass', 'A heated basin, no blazes needed', [
	"A basin on a Furnace is heated by the fuel burning inside. {Searing} heats it like a fed Blaze Burner, "
	"enough for brass; {Warm} or {Hot} only warms it. Heat from under the Furnace passes up as it is, so a soul "
	"campfire beneath only warms the basin.",
	"Superheated recipes still want a Blaze Burner on Blaze Cake, later Power Grid's basin heater run hot "
	"enough, or a Steel Furnace burning {Blazing}.",
], [item('create:brass_ingot')], deps=['copper', 'hearth:searing', 'bloom_iron:press'])
c.q('brass_parts', 5, 0, 'Clockwork Brass', 'Delicate and fancy', [
	"Brass parts are fine clockwork: polished, pretty and delicate. Their supports stop at 96 RPM and their "
	"shafts carry little more than wood's. Brass already has some powerful machines, so its parts don't get to "
	"be strong too.",
	"Brass never tarnishes. It has no clutch, gearshift or chain gearshift, and the Mechanical Crafter comes "
	"only in brass.",
], [item('mcremix:brass_cogwheel')], deps=['brass'])
c.q('machines', 4, 1, 'Machines in Every Metal', 'Swap the casing, swap the tier', [
	"Create's press, mixer, millstone, fan, saw, drill, deployer and arm also come in copper, iron, steel, "
	"aluminium and titanium: the same machine in a new casing, held to its metal's top speed.",
	"Use a casing on a placed machine to change its metal, up or down. It keeps its facing and settings, and the "
	"old casing pops out.",
	"@Ponder: any machine shows the materials scene.",
], [item('create:copper_casing')], deps=['copper'])
c.q('bellows', 1, 1, 'The Bellows', 'One level hotter', [
	"A turning Bellows blows into whatever its nozzle faces. A Furnace there, or one standing on a fire there, "
	"burns a level hotter, up to {Searing}. It won't reach {Blazing} or light a cold Furnace, and fuel it fans "
	"burns twice as fast.",
	"So cheap heat goes further: a fanned Furnace on a campfire is {Hot} for free, and on lava {Searing}. Aimed "
	"at a lit campfire, it cooks food twice as fast.",
], [item('mcremix:bellows')], deps=['alloy', 'hearth:furnace'])
c.q('boiler', 0, 2, 'Steam from Fire', 'A Furnace under a boiler', [
	"A boiler on a Furnace is heated by the fuel burning inside, like a Blaze Burner. {Searing} boils it; {Warm} "
	"or {Hot} only keeps it simmering. Heat from under the Furnace passes up as it is, so a soul campfire "
	"beneath only simmers it.",
	"So steam power arrives with Coal Coke, no Nether trip needed.",
	"Brass, iron and steel have steam engines of their own, each only for tanks of its own metal. Create's "
	"engine stays on Create's copper tanks.",
], [item('create:steam_engine')], deps=['alloy', 'hearth:searing', 'bloom_iron:press'])

# ---------------------------------------------------------------- 3. Bloom Iron
c = Chapter('bloom_iron', 'Bloom Iron', 'Iron is struck, not smelted', 'mcremix:iron_bloom')
c.q('bloom', 0, 0, "Raw Iron Won't Smelt", 'Fire it into a bloom', [
	"Raw iron no longer smelts into ingots. Fire raw iron or iron ore at {Searing} for a glowing Iron Bloom: "
	"spongy iron full of slag that has to be beaten out.",
	"@JEI: the Furnace and Striking Blooms tabs.",
], [item('mcremix:iron_bloom')], deps=['hearth:searing'])
c.q('stone_anvil', 1, 0, 'A Stone Anvil', 'Strike before you have iron', [
	"An iron anvil takes a lot of iron and a press a whole block, so your first blooms meet a Stone Anvil. Four "
	"strikes a bloom.",
	"It cracks as you work and crumbles after about fifteen blooms. No menu, no repairs: it's a rock you hit "
	"things on.",
], [item('mcremix:stone_anvil')], deps=['bloom'])
c.q('iron', 2, 0, 'Iron', 'An ingot, and sometimes slag', [
	"Each bloom gives an ingot, and half the time a lump of slag. An iron anvil lasts about a hundred blooms, "
	"and a Mechanical Press takes the hammer out of your hands for good.",
	"Create's crushing and washing still work too: cheaper, but leaner.",
], [item('minecraft:iron_ingot')], deps=['stone_anvil'], **MILESTONE)
c.q('slag', 3, -1, 'Slag', 'Waste worth keeping', [
	"Don't bin it. Slag fertilises like bone meal, and washing it with an Encased Fan gives back iron nuggets "
	"and gravel. Later, a Power Grid electromagnet pulls more iron out of it.",
], [item('mcremix:slag')], deps=['iron'], optional=True)
c.q('press', 3, 0, 'The Mechanical Press', 'Strikes, presses and beats', [
	"The press costs a block of iron, so it waits for a steady supply. It strikes blooms, presses ingots into "
	"sheets, and later it's the only thing that beats blister steel into steel.",
], [item('create:mechanical_press')], deps=['iron', 'turning:alloy'])
c.q('iron_parts', 3, 1, 'Iron Parts', 'Full speed, any load', [
	"Iron supports spin to 256 RPM like copper's, but iron shafts and cogs take any load. The price: iron "
	"supports are a little heavier to turn, even clean.",
	"Iron rusts only where it gets wet, each stage adding friction. Iron Casing, made from iron sheet, is the "
	"base for iron gearboxes, clutches, gearshifts, chain drives and machines.",
], [item('mcremix:iron_casing')], deps=['press', 'turning:copper'])
c.q('drill', 2, 1, 'Bits and Blades', 'The body sets the speed, the bits what it cuts', [
	"Drills and saws come with iron bits or an iron blade, cutting what an iron pickaxe or axe would. Use other "
	"bits or blades on a placed one to swap them in.",
	"Stone is cheap and light. Gold cuts twice as fast for half the stress, but balks at iron, copper and lapis "
	"ore, and a golden blade won't cut stone. Only diamond bits get through obsidian and ancient debris. A drill "
	"that can't cut a block skates over it, throwing sparks.",
], [item('create:mechanical_drill')], deps=['iron', 'turning:alloy'])

# ---------------------------------------------------------------- 4. Steel
c = Chapter('steel', 'Steel', 'Iron that soaked up carbon', 'mcremix:steel_ingot')
c.q('pack', 0, 0, 'A Cementation Pack', 'Iron sealed in coke', [
	"Make steel the way cementation furnaces did: iron packed in coke and sealed in, to soak up the carbon.",
], [item('mcremix:cementation_pack')], deps=['bloom_iron:iron', 'hearth:coke'])
c.q('blister', 1, 0, 'Blister Steel', 'The slowest firing there is', [
	"Fire a pack at {Searing}, from any heat source, and it comes out as Blister Steel. It's the slowest firing "
	"there is, so start it and go do something else.",
	"Blister steel is brittle, its skin bubbled with blisters, and crafts into nothing.",
], [item('mcremix:blister_steel')], deps=['pack'])
c.q('steel', 2, 0, 'Steel', 'Beaten under a press', [
	"Only a Mechanical Press beats blister steel into steel ingots, and presses those into sheets.",
	"Much later, a crucible over {Blazing} heat alloys iron and coke straight into molten steel, four times as "
	"fast.",
], [item('mcremix:steel_ingot')], deps=['blister', 'bloom_iron:press'], **MILESTONE)
c.q('steel_casing', 3, 0, 'Steel Casing', 'The casing for 512 RPM', [
	"Steel Casing, made from steel sheet, is the base for steel gearboxes, clutches, gearshifts, chain drives "
	"and machines.",
], [item('mcremix:steel_casing')], deps=['steel'])
c.q('steel_parts', 4, 0, '512 RPM', 'Fast, unbreakable and heavy', [
	"Steel supports spin to 512 RPM, and steel shafts and cogs take any load and reach six blocks from a "
	"support. The price: steel supports are the heaviest to turn.",
	"Machines draw stress per RPM, so at 512 one works twice as fast for twice the stress. Spend speed where it "
	"pays.",
	"Steel rusts where it gets wet, but never past the first stage.",
], [item('mcremix:steel_shaft')], deps=['steel_casing'])

# ---------------------------------------------------------------- 5. Molten Metal
c = Chapter('molten_metal', 'Molten Metal', 'Pipes, heat and the crucible', 'mcremix:crucible')
c.q('crucible', 0, 0, 'The Crucible', 'Melting by the heat below', [
	"Clay shaped like a pot and fired {Hot} becomes a crucible. It hangs on a shaft between two posts and melts "
	"what goes in by the heat underneath: zinc and aluminium at {Hot}, copper, gold and brass at {Searing}, and "
	"iron, steel and netherite only at {Blazing}, over a superheated Blaze Burner or, later, Power Grid's basin "
	"heater past 1,200 °C.",
	"Old tools and armour melt for half the metal that went into them. Copper and zinc together come out as "
	"brass.",
	"@JEI: the Melting and Alloying tabs.",
], [item('mcremix:crucible')], deps=['hearth:hot'])
c.q('molds', 1, 0, 'Pouring', 'Tip it into sand', [
	"Turn the crucible's shaft to tip it. It pours whatever rises over the lip onto what's in front: a Sand Mold "
	"on a depot or belt, a tank, another crucible, or the floor, where it spills and sets as rough metal. Aim "
	"carefully.",
	"A press in a basin rams sand round an ingot or block to make a mold. A full mold sets in two seconds and "
	"the casting breaks out.",
	"@JEI: the Casting tab. Ponder: the crucible.",
], [item('mcremix:sand_ingot_mold')], deps=['crucible', 'bloom_iron:press'])
c.q('big', 2, 0, 'The Big Crucible', 'Twenty-seven as one', [
	"Twenty-seven crucibles in a 3x3x3 cube join into one big crucible. Heat all nine columns and it melts and "
	"pours nine times as fast. Tipping it takes real muscle.",
], [look('mcremix:crucible[formed=true]', 'Look at a big crucible', 'mcremix:crucible')], deps=['molds'],
	optional=True)
c.q('pipes', 0, 1, 'Every Pipe Has a Ceiling', 'Fluids are hot or cold too', [
	"Fluids sit on the same heat scale as fires: water is {Cold}, lava {Hot}, molten metal as hot as it melts. "
	"Every pipe, pump and tank has a ceiling: brass, glass, copper and iron take up to {Hot}, steel {Searing}, "
	"aluminium only {Warm} and PVC only {Cold}. Past it, a part glows, then bursts or melts and spills.",
	"Plain pipes only join pipes of their own metal. Pumps, valves and smart pipes join anything, so use one as "
	"the adapter. Tanks of different metals never join.",
	"Tooltips say how hot a fluid each part takes.",
], [item('create:mechanical_pump')], deps=['hearth:hot', 'bloom_iron:press'])

# ---------------------------------------------------------------- 6. Tar, Spirit and Glass
c = Chapter('glass_lab', 'Tar, Spirit and Glass', 'The first chemistry, a bucket at a time', 'mcremix:round_flask')
c.q('tar', 0, -1, 'Coal Tar', 'What coking gives off', [
	"Coking coal gives off coal tar, and charring logs wood spirit, into a tank in the Furnace. Pipe either off "
	"from any side, or bucket it out. The tank holds a bucket of one; overflow, or the other one, burns off at "
	"the chimney.",
	"Remember that black goo? Almost every chemical in this book starts with coal tar.",
], [item('mcremix:coal_tar_bucket')], deps=['hearth:coke', 'bloom_iron:iron'])
c.q('spirit', 0, 1, 'Wood Spirit', 'What charring gives off', [
	"Charring logs gives off a little wood spirit per log: acetone, methanol and vinegar in one, waiting to be "
	"split.",
], [item('mcremix:wood_spirit_bucket')], deps=['hearth:charcoal', 'bloom_iron:iron'])
c.q('glassware', 1, 0, 'Lab Glass', 'Glassware on retort stands', [
	"Lab glass is eight pieces, each on an iron retort stand. Each takes heat from what's under it, like a "
	"basin, up to {Hot}. Give it {Searing} and it cracks.",
	"Hot glass also cracks in rain or beside water, so give your lab a roof. Cracked glass hisses, then shatters "
	"five seconds later, spilling everything.",
], [item('mcremix:round_flask')], deps=['hearth:hot', 'bloom_iron:iron'])
c.q('still', 2, 0, 'The Still', 'A bucket at a time, cut by cut', [
	"A still is a round flask over heat, a still head with a thermometer, a coil condenser on its arm and a cow "
	"receiver under the drip, with glass column pieces between flask and head when a mixture needs them.",
	"A bucket comes over cut by cut, lightest first, each into its own flask, and the leftovers stay behind. The "
	"condenser needs water running through it, and cuts that boil close together need column pieces to come over "
	"at full pace.",
	"@JEI: the Distilling tab. Ponder: the still head.",
], [item('mcremix:still_head'), item('mcremix:coil_condenser'), item('mcremix:cow_receiver')], deps=['glassware'])
c.q('tar_cuts', 3, -1, 'Splitting Coal Tar', 'Light oil, carbolic oil, creosote and pitch', [
	"Coal tar distils at {Hot} into light oil, carbolic oil and creosote, with pitch left in the flask.",
	"Nothing goes to waste. Pitch binds electrodes and briquettes, creosote soaks timber and burns as fuel, "
	"carbolic oil hides phenol and naphthalene, and light oil is fuel now and the start of the dyes much later.",
], [item('mcremix:pitch_bucket')], deps=['still', 'tar'])
c.q('spirit_cuts', 3, 1, 'Splitting Wood Spirit', 'Acetone, methanol and vinegar', [
	"Wood spirit distils at {Warm} into acetone and methanol, leaving vinegar. The two boil only 9 °C apart, so "
	"a bare still is slow to split them. Column pieces fix that.",
	"Vinegar strips copper's patina, the way acid strips rust.",
], [item('mcremix:methanol_bucket')], deps=['still', 'spirit'])
c.q('ethanol', 3, 0, 'Wash and Ethanol', 'Sugar ferments, the still does the rest', [
	"Sugar in water ferments into wash by itself: in a cool basin with nothing working it, a batch at a time, or "
	"in a vat, a batch for every bucket it holds. Distil the wash at {Warm} for ethanol.",
	"Ethanol fuels the Steel Furnace and starts you on ethylene.",
	"@JEI: the Fermenting tab.",
], [item('mcremix:ethanol_bucket')], deps=['still'])
c.q('formaldehyde', 4, 1, 'Formaldehyde', 'Methanol over hot copper', [
	"The hot tube handles catalyst jobs up to {Hot}. Run methanol over copper mesh in it and out comes "
	"formaldehyde.",
	"@JEI: the Steel Furnace tab lists the hot tube's jobs too.",
], [item('mcremix:formaldehyde_bucket')], deps=['spirit_cuts'])
c.q('caustic', 4, -2, 'Caustic Soda', 'From lime and kelp', [
	"Limestone or chalk fired at {Searing} burns to quicklime, giving off carbon dioxide. Wash it into slaked "
	"lime, then mix that with soda ash and water for caustic soda and chalk.",
	"Soda ash comes from kelp: fire it to ash and wash that. Later, splitting salt water makes caustic soda by "
	"the bucket.",
], [item('mcremix:caustic_soda_bucket')], deps=['hearth:searing', 'bloom_iron:press'])
c.q('vat', 5, -2, 'A Vat', 'Closed tanks on scaffolding', [
	"Stand tanks on a full layer of Create's andesite scaffolding and they form a vat, up to three wide and five "
	"high. It's closed, so it keeps the gases a basin would lose, and it holds up to four fluids at once in "
	"layers.",
	"Left alone it settles and ferments. A mixer used on its side stirs it, but only one of the vat's own metal: "
	"copper for Create's copper tanks. Create's own mixer won't fit.",
	"@JEI: the Vat Mixing tab. Ponder: the copper mixer.",
], [item('create:andesite_scaffolding'), item('mcremix:copper_mechanical_mixer')], deps=['turning:machines', 'bloom_iron:press'])
c.q('phenol', 5, -1, 'Phenol', 'Out of carbolic oil', [
	"Carbolic oil settles in a settling flask into naphthalene and strained oil. Shake the strained oil with "
	"caustic soda in a separating funnel for sodium phenolate under a layer of creosote.",
	"Stir sodium phenolate with carbon dioxide in a vat for phenol and soda ash. Carbon dioxide comes off "
	"fermenting and burning lime, and travels by pipe only: there's no bucket of it.",
], [item('mcremix:phenol_bucket')], deps=['tar_cuts', 'caustic', 'vat'])
c.q('bakelite', 6, 0, 'Bakelite', 'The first plastic', [
	"Press phenol and formaldehyde together in a heated basin and they set into Bakelite, the first plastic. "
	"It's an ingredient, never a material for parts.",
	"With steel, it unlocks Create: Power Grid.",
], [item('mcremix:bakelite')], deps=['phenol', 'formaldehyde'], **MILESTONE)

# ---------------------------------------------------------------- 7. Electricity
c = Chapter('electricity', 'Electricity', 'Create: Power Grid, and pulling things apart with current', 'mcremix:electrolyser')
c.q('power_grid', 0, 0, 'Create: Power Grid', 'Real circuits, volts and amps', [
	"Power Grid adds real circuits: generators, wire, transformers and devices that care about volts and amps. "
	"Its own Ponder scenes explain its parts better than this book could.",
	"Here its motor and transformer core take steel, its boards, switches, buttons and sockets take Bakelite, "
	"and the boards are etched in ferric chloride.",
], [item('powergrid:wire')], deps=['glass_lab:bakelite', 'steel:steel'])
c.q('coils', 1, 0, 'The Coil Winder', 'Coils are wound, not crafted', [
	"Power Grid's coils come off a Coil Winder: copper wire into a Copper Coil, iron wire into a Resistive Coil. "
	"The faster it turns, the faster it winds.",
	"It comes in andesite and every metal from copper to titanium, each held to its metal's top speed.",
	"@JEI: the Coiling tab.",
], [item('mcremix:coil_winder'), item('powergrid:copper_coil')], deps=['power_grid'])
c.q('generator', 2, 0, 'A Generator', 'Rotation into current', [
	"Power Grid's generator is copper coils strung on shafts round rotors and a commutator, and it has to be "
	"excited before it gives anything.",
	"Its rotor and commutator come together on Mechanical Crafters, which come only in brass.",
	"@Ponder: the generator parts show how to build and start one.",
], [item('powergrid:generator_induction_rotor'), item('powergrid:generator_commutator')], deps=['coils', 'turning:brass'])
c.q('transformer', 3, 0, 'Stepping Volts', 'Every device wants its own voltage', [
	"A generator gives one voltage and every device wants another. A transformer steps it up or down.",
	"Your next two devices pull opposite ways: the Electrolyser wants a few volts, the Spark Plug about a "
	"thousand.",
], [item('powergrid:transformer_core')], deps=['generator'])
c.q('electrolyser', 4, 0, 'The Electrolyser', 'Volts to start, amps to work', [
	"An Electrolyser goes on the side of a basin or vat and splits what's inside. Fit a Carbon Electrode as its "
	"anode (+) first, then its cathode.",
	"It behaves like a real cell. Below about 4 V almost nothing flows; past that the current shoots up, so a "
	"generator wired straight across will cook it. Step the voltage down or wire several in series. Wired "
	"backwards, nothing flows.",
	"The anode burns away with the power through it, so driving it gently costs less carbon per batch.",
	"@Ponder: the Electrolyser. JEI: the Electrolysis tab.",
], [item('mcremix:electrolyser'), item('mcremix:carbon_electrode'),
	look('mcremix:electrolyser[anode=true,cathode=true]', 'Look at an Electrolyser with both electrodes',
		'mcremix:carbon_electrode')], deps=['transformer', 'glass_lab:tar_cuts'], **MILESTONE)
c.q('salt_water', 5, 1, 'Salt Water', 'Only from the sea', [
	"Water taken in an ocean or beach biome, by bucket, pump or hose pulley, is salt water.",
], [item('mcremix:salt_water_bucket')], deps=['bloom_iron:iron'])
c.q('chlorine', 5, 0, 'Chlorine', 'Splitting the sea', [
	"Split salt water for chlorine, hydrogen and caustic soda. On a basin the gases escape, and chlorine rolls "
	"out as a low yellow-green cloud that hurts anything breathing and kills the plants under it. A vat keeps "
	"both gases, ready to pipe away.",
	"Iron stirred with chlorine and water in a vat makes ferric chloride, which etches Power Grid's circuit "
	"boards.",
], [item('mcremix:ferric_chloride_bucket')], deps=['electrolyser', 'salt_water', 'glass_lab:vat'])
c.q('spark_plug', 3, -1, 'The Spark Plug', 'Light a fire from a circuit', [
	"A Spark Plug screws into a Furnace's or Steel Furnace's side or back. It sparks only from about 900 V, so "
	"step up with a transformer. While it sparks it lights any fuel there to catch, and a switch or relay in its "
	"circuit decides when. No more running round with flint and steel.",
], [item('mcremix:spark_plug')], deps=['transformer'], optional=True)
c.q('electric_heat', 0, -1, 'Electric Heat', 'Heat with no fire', [
	"Power Grid's heating coil and basin heater count by their real temperature: 200 °C is {Warm}, 400 {Hot}, "
	"600 {Searing} and 1,200 {Blazing}. Under a Furnace they heat it with no fuel at all.",
	"Skip the Bellows: moving air only cools an electric heater.",
], [item('powergrid:heating_coil')], deps=['coils'], optional=True)

# ---------------------------------------------------------------- 8. Aluminium
c = Chapter('aluminium', 'Aluminium', 'From red sand to the everyday late metal', 'mcremix:aluminium_ingot')
c.q('bauxite', 0, 0, 'Bauxite', 'Washed out of red sand', [
	"Wash red sand with an Encased Fan and a quarter of the time you'll find bauxite among the gold nuggets.",
	"From here on, every step can be worked out from what you see.",
], [item('mcremix:bauxite')], deps=['electricity:electrolyser'])
c.q('liquor', 1, 0, 'Bauxite Liquor', 'Dissolved in caustic soda', [
	"Bauxite dissolves in caustic soda in a heated basin into bauxite liquor, leaving red mud behind.",
], [item('mcremix:bauxite_liquor_bucket')], deps=['bauxite', 'glass_lab:caustic'])
c.q('hydroxide', 2, 0, 'Settling', 'Leave it to stand', [
	"Left to stand cool, the liquor settles out aluminium hydroxide and hands back some caustic soda. A basin "
	"settles a batch at a time, with no heat under it and nothing working over it. A vat settles a batch for "
	"every bucket it holds.",
	"@JEI: the Settling tab.",
], [item('mcremix:aluminium_hydroxide')], deps=['liquor'])
c.q('alumina', 3, 0, 'Alumina', 'Fired white', [
	"Fire aluminium hydroxide at {Searing} for alumina, the white powder electrolysis turns into metal. Keep a "
	"stash: it's a catalyst later too.",
], [item('mcremix:alumina')], deps=['hydroxide'])
c.q('aluminium', 4, 0, 'Aluminium', 'Split by current', [
	"An Electrolyser splits alumina into molten aluminium, no heat needed: the current keeps the melt hot. Pipe "
	"it off and cast it in sand molds.",
	"It eats far more charge than salt water, so this is where a good circuit pays for itself.",
], [item('mcremix:aluminium_ingot')], deps=['alumina', 'molten_metal:molds'], **MILESTONE)
c.q('light_soft', 5, 0, 'Light and Soft', "Steel's speed, no drag, and a weakness", [
	"Aluminium supports spin to 512 RPM with no drag, and aluminium never weathers. The catch: its shafts and "
	"cogs carry only light loads, and it melts beside anything {Hot}, from lava and fire to a lit Furnace, a "
	"Blaze Burner or molten metal. A melted part runs down and sets as rough aluminium.",
	"Its pipes take only {Warm} fluids and its tanks make no boiler. Keep it well away from the furnace yard.",
	"Every aluminium bracket and casing needs a Polyethylene Bushing, so aluminium parts wait for the Steel "
	"Furnace chapter.",
], [item('mcremix:aluminium_sheet')], deps=['aluminium'])
c.q('heat_sink', 5, 1, 'The Heat Sink', 'Fins for hot devices', [
	"Bolt a heat sink to a Power Grid device, base against it, and it draws the heat out, so the device takes "
	"more power before it overheats. Several add up, and a fan blowing through the fins sheds far more.",
], [item('mcremix:heat_sink')], deps=['aluminium'])
c.q('red_mud', 1, 1, 'Red Mud Bricks', 'Waste made into walls', [
	"All that red mud piling up? Fire it {Warm} into red mud bricks, with stairs, slabs and walls of their own.",
], [item('mcremix:red_mud_bricks')], deps=['liquor'], optional=True)

# ---------------------------------------------------------------- 9. The Steel Furnace
c = Chapter('steel_furnace', 'The Steel Furnace', 'Fluid fuel, the Pressurizer and plastics', 'mcremix:steel_furnace')
c.q('steel_furnace', 0, 0, 'The Steel Furnace', 'A Furnace cased in steel', [
	"Use Steel Casing on a Furnace for a Steel Furnace. It fires what a Furnace fires and heats a boiler or "
	"basin on top the same way, but burns only fluid fuel pumped up from underneath, and still needs lighting.",
	"It's sealed: no heat comes up through it, and a Bellows does nothing for it.",
], [item('mcremix:steel_furnace')], deps=['steel:steel_casing', 'glass_lab:tar'])
c.q('flow', 1, 0, 'Flow Is Heat', "The pump's speed sets the fire", [
	"Lit, it burns every drop that arrives, and how hot depends on the fuel and how fast it comes. Pumps move "
	"fuel in step with their speed, so the pump is your thermostat. Each fuel has a ceiling: wood spirit stops "
	"at {Hot}, ethanol and coal tar at {Searing}.",
	"Pump faster than the ceiling can use and the extra goes up the chimney. Pump into it unlit and the fuel "
	"pours out the front.",
	"@JEI: the Steel Furnace Fuel tab.",
], [look('mcremix:steel_furnace[heat=3]', 'Look at a Searing Steel Furnace', 'mcremix:steel_furnace')],
	deps=['steel_furnace'])
c.q('blazing', 2, 0, 'Blazing', 'The hottest fire', [
	"Only the richest fuels reach {Blazing}: hydrogen and ethylene, and later fuel gas. At {Blazing} a Steel "
	"Furnace superheats a boiler or basin on top, like a Blaze Burner on Blaze Cake.",
], [look('mcremix:steel_furnace[heat=4]', 'Look at a Blazing Steel Furnace', 'minecraft:blaze_powder')],
	deps=['flow', 'electricity:chlorine'])
c.q('pressurizer', 0, 2, 'The Pressurizer', 'Speed is pressure', [
	"The Pressurizer is a steel machine with a piston pump inside, and its speed is its pressure. Each job has a "
	"minimum speed, and twice as fast works twice as quick.",
	"It's a beast to turn, so gear it up. Jobs that need heat take it from the block underneath, and the dial on "
	"the front reads the pressure.",
	"@JEI: the Pressurizing tab. Ponder: the Pressurizer.",
], [item('mcremix:pressurizer')], deps=['steel:steel_parts'])
c.q('polyethylene', 1, 2, 'Polyethylene', 'Ethylene squeezed solid', [
	"Run ethanol over alumina, in a Steel Furnace at {Hot} or a hot tube, and it turns to ethylene gas. A "
	"Pressurizer at full steel speed over {Warm} heat or more squeezes that into polyethylene pellets, and "
	"pellets pressed over a heated basin make a Polyethylene Bushing.",
	"Every aluminium and titanium bracket and casing takes a bushing, and Power Grid's integrated circuit takes "
	"pellets.",
], [item('mcremix:polyethylene_bushing')], deps=['pressurizer', 'flow', 'glass_lab:ethanol', 'aluminium:alumina'])
c.q('al_casing', 2, 2, 'Aluminium Casing', 'Aluminium supports at last', [
	"Aluminium Casing takes a Polyethylene Bushing and is the base for aluminium gearboxes, clutches, "
	"gearshifts, chain drives and machines: steel's speed with no drag, if you mind the heat.",
	"About that heat: aluminium supports warm up as they turn. See the Heat and Cooling chapter.",
], [item('mcremix:aluminium_casing')], deps=['polyethylene', 'aluminium:light_soft'], **MILESTONE)
c.q('pvc', 1, 3, 'PVC Pipes', 'Thin, white and long', [
	"Chlorine and ethylene stirred in a vat over ferric chloride make ethylene dichloride. A Steel Furnace "
	"cracks that at {Searing} into vinyl chloride, a Pressurizer squeezes that into PVC resin, and a press makes "
	"the resin into sheets, which craft into pipes.",
	"PVC pipes lie flat on block faces, and a pump reaches twice as far through them as through other pipe, so "
	"they're the pick for long runs. They take only {Cold} fluids and never weather.",
], [item('mcremix:pvc_pipe')], deps=['polyethylene', 'electricity:chlorine'])
c.q('acid', 2, 3, 'Hydrochloric Acid', 'Rust remover', [
	"Cracking ethylene dichloride gives off hydrogen chloride. An Electrolyser splits it back into chlorine and "
	"hydrogen, or stir it into water for hydrochloric acid.",
	"Poured out, the acid strips rust and patina back to bare metal, though waxed metal is sealed against it. A "
	"Spout of it cleans weathered parts riding a belt.",
	"Let loose, hydrogen chloride hangs as a mist that rusts the metal under it.",
], [item('mcremix:hydrochloric_acid_bucket')], deps=['pvc'])
c.q('steam', 0, 3, 'Pressurised Steam', 'Steel boilers need no fire', [
	"A Pressurizer over {Hot} heat squeezes water into pressurised steam, and that's all a boiler of steel tanks "
	"runs on: no water, no fire. It needs only half the tanks for each level, too.",
	"Steam its engines don't use builds pressure. Give it a whistle to vent, or it bursts.",
], [item('mcremix:steel_steam_engine')], deps=['pressurizer'])
c.q('vacuum', -1, 2, 'Vacuum Sealing', 'The Pressurizer, run backwards', [
	"Run a Pressurizer backwards and it pulls a vacuum, its needle dropping below zero, sealing whatever lies in "
	"its vessel over a {Hot} fire. Power Grid's light bulbs and vacuum tubes are made this way now, not by hand.",
	"Deeper vacuums need more speed: bulbs seal from 128 RPM, tubes from 256, and the cathode ray tube needs 512.",
	"@JEI: the Vacuum Sealing tab.",
], [item('powergrid:light_bulb')], deps=['pressurizer'])

# ---------------------------------------------------------------- 10. Heat and Cooling
c = Chapter('cooling', 'Heat and Cooling', 'Friction heat, and how to shed it', 'mcremix:water_cooler')
c.q('friction', 0, 0, 'Friction Heat', 'Aluminium warms as it turns', [
	"Aluminium supports warm up as they turn. At about 256 RPM one settles near 600 °C, but at 512 it melts "
	"about a minute after a cold start. Only what holds a part heats, so an aluminium shaft in a steel bracket "
	"stays cool.",
	"Goggles show the temperature: gold from 520 °C, red from 620. Red means trouble.",
], [item('mcremix:aluminium_gearbox')], deps=['steel_furnace:al_casing'])
c.q('sink_fan', 1, -1, 'Sinks and Fans', 'Air cooling', [
	"A heat sink bolted to the support carries it to about 384 RPM, and a fan of 128 RPM or more blowing through "
	"the fins takes it all the way to 512. A fan aimed straight at it with no sink helps about as much as one "
	"sink.",
], [item('create:encased_fan')], deps=['friction'])
c.q('water_cooler', 1, 0, 'The Water Cooler', 'Water takes the heat away', [
	"A water cooler is a heat sink's base in a copper water jacket, bolted on the same way. It takes heat in "
	"step with how hot its support runs, warming its water as it goes, so it only uses water as fast as it cools.",
	"Pump the warm water away or it stops. Coolers work on Power Grid devices too.",
], [item('mcremix:water_cooler')], deps=['friction'])
c.q('cold_water', 2, 0, 'Cold Water', 'From wherever it snows', [
	"Water drawn anywhere cold enough to snow is cold water, though the sea still gives salt water even where it "
	"freezes. It stays cold wherever you pipe it, and in a water cooler it works twice as well as plain water.",
], [item('mcremix:cold_water_bucket')], deps=['water_cooler'])
c.q('radiator', 3, 0, 'Radiators', 'Chill it and send it round', [
	"Radiators side by side in one plane join into a panel that chills any water into cold water and hot coolant "
	"back into coolant, faster with a fan blowing through it.",
	"Close the loop: warm water out of a cooler, cold water back in.",
], [item('mcremix:radiator')], deps=['cold_water'])
c.q('coolant', 4, 0, 'Coolant', 'Three times water', [
	"Ethylene dichloride and caustic soda mixed over heat give ethylene glycol, and glycol with water is "
	"coolant. In a water cooler it takes heat three times as fast as water and comes out hot, for a radiator to "
	"chill back.",
	"It never wears out, so build the loop once and let it run.",
], [item('mcremix:coolant_bucket')], deps=['radiator', 'steel_furnace:pvc'])

# ---------------------------------------------------------------- 11. Titanium
c = Chapter('titanium', 'Titanium', 'The top tier, if you can keep it cool', 'mcremix:titanium_ingot')
c.q('black_sand', 0, 0, 'Black Sand', 'Crushed basalt', [
	"Crush or mill basalt into black sand. Unlikely as it looks, that's where titanium starts.",
], [item('mcremix:black_sand')], deps=['cooling:water_cooler'])
c.q('rutile', 1, 0, 'Rutile', 'What the magnet leaves', [
	"Power Grid's electromagnet pulls the iron out of black sand as nuggets. What's left is rutile, titanium's "
	"ore.",
], [item('mcremix:rutile')], deps=['black_sand'])
c.q('briquette', 2, 0, 'Rutile Briquettes', 'Rutile and carbon, pressed', [
	"A press binds rutile and coke into briquettes. When chlorine meets them, the coke's carbon steals the "
	"rutile's oxygen.",
], [item('mcremix:rutile_briquette')], deps=['rutile', 'glass_lab:tar_cuts'])
c.q('salt', 0, 1, 'Salt', 'Boiled out of the sea', [
	"Salt water in a heated basin with no mixer or press over it boils down to salt.",
	"@JEI: the Boiling tab.",
], [item('mcremix:salt')], deps=['electricity:salt_water', 'turning:brass'])
c.q('sodium', 1, 1, 'Molten Sodium', 'Split from molten salt', [
	"A crucible over {Hot} heat melts salt, and an Electrolyser splits the molten salt into molten sodium and "
	"chlorine.",
	"Keep sodium away from water. It goes off with a bang and a burst of fire.",
], [item('mcremix:molten_sodium_bucket')], deps=['salt', 'molten_metal:crucible', 'electricity:electrolyser'])
c.q('sponge', 3, 1, 'Titanium Sponge', 'Chlorine, then sodium', [
	"Chlorine run over a briquette in a {Blazing} Steel Furnace comes off as titanium tetrachloride, a gas that "
	"fumes into white smoke and acid mist if it gets loose.",
	"Stir it in a vat with molten sodium for titanium sponge and salt. The salt goes round again.",
], [item('mcremix:titanium_sponge')], deps=['briquette', 'sodium', 'steel_furnace:blazing'])
c.q('arc', 3, -1, 'The Arc Furnace', 'An arc over a cooled hearth', [
	"The Arc Furnace is a Power Grid device: a copper hearth under a Bakelite beam, a Carbon Electrode hanging "
	"from each terminal. Its arc melts nothing until enough power runs through it.",
	"On its own the hearth overheats fast, so give it a water cooler. Both electrodes burn away as it works.",
	"@Ponder: the Arc Furnace. JEI: the Arc Furnace tab.",
], [item('mcremix:arc_furnace')], deps=['electricity:transformer', 'cooling:water_cooler'])
c.q('titanium', 4, 0, 'Titanium', 'Sponge, melted down', [
	"The arc melts titanium sponge into ingots. Load it by hand, funnel or arm, and let a hopper empty it.",
], [item('mcremix:titanium_ingot')], deps=['sponge', 'arc'], **MILESTONE)
c.q('ti_parts', 5, 0, 'Rock-solid', '2,048 RPM, if you can cool it', [
	"Titanium supports spin to 2,048 RPM, four times steel, with no drag, any load and no weathering. Its casing "
	"takes a bushing, like aluminium's.",
	"The catch: they need twice aluminium's cooling at the same speed, and past 1,000 °C they burn, flaring "
	"white and setting fire across the gap, then crumbling to powder. Heat sinks melt long before that, so fast "
	"titanium needs water coolers, on cold water or coolant for the top speeds.",
	"Heat leaves temper colours, gold, purple and blue, that stay until acid strips them.",
], [item('mcremix:titanium_casing')], deps=['titanium', 'cooling:coolant'])
c.q('ti_pressurizer', 5, 1, 'The Titanium Pressurizer', 'Four times the pace', [
	"In titanium the Pressurizer spins to 2,048 RPM, so its jobs run up to four times as fast. It heats and "
	"burns like any titanium machine, though, so past about 128 RPM it wants a water cooler.",
], [item('mcremix:titanium_pressurizer')], deps=['ti_parts'], optional=True)

# ---------------------------------------------------------------- 12. The Steel Column
c = Chapter('steel_column', 'The Steel Column', 'Distilling nonstop, every cut at once', 'mcremix:steel_column_section')
c.q('sections', 0, 0, 'Column Sections', 'Split nonstop', [
	"Steel Column Sections join like Create's tanks, up to three across and sixteen high, each block layer a "
	"section. Heat under the column boils its feed: each block under it at the mixture's heat adds more, and "
	"hotter adds more again.",
	"Feed goes in at the bottom, the residue stays there, and each cut condenses in its own section above, in "
	"boiling order.",
	"@Ponder: the column section.",
], [item('mcremix:steel_column_section')], deps=['glass_lab:still', 'steel:steel'])
c.q('condenser', 1, 0, 'The Steel Condenser', 'Without one, nothing moves', [
	"A Steel Condenser beside the bottom section takes the vapour from the top one. No condenser, no column. "
	"Water, cold water or coolant through its jacket sets how much it condenses, each better than the last.",
], [item('mcremix:steel_condenser')], deps=['sections'])
c.q('draw', 2, 0, 'Draw Every Cut', 'A cut nobody draws holds everything up', [
	"A pipe on a section's side draws that section's cut. Leave one undrawn and it fills its section and stalls "
	"the whole column.",
	"Each mixture needs a section for every cut, with empty sections between close ones. Cuts a short column has "
	"no room for escape over the top.",
	"@JEI: the Distilling tab gives the sections each mixture needs.",
], [check()], deps=['condenser'], icon='mcremix:steel_fluid_pipe')
c.q('reflux', 3, -1, 'Reflux', 'Trade pace for height', [
	"Pipe some of the top cut back into the top section and it stands in for the empty sections close cuts need, "
	"at the cost of pace. It makes no room for a cut the column is simply too short for.",
], [check()], deps=['draw'], icon='create:mechanical_pump', optional=True)
c.q('hot_cuts', 3, 1, 'Hot Cuts', 'Some cuts leave hot', [
	"Cuts leave as hot as their section. Carbolic oil, creosote and vinegar come out too hot for any PVC pipe, "
	"and pitch is always kept warm. A Steel Condenser anywhere but beside a column cools hot cuts inline.",
	"Whatever escapes over the top hangs low as solvent vapour and flashes into flame at anything {Warm} or "
	"hotter. Mind your furnaces.",
	"@JEI: the Cooling Hot Cuts tab.",
], [item('mcremix:hot_creosote_bucket')], deps=['draw'], optional=True)

# ---------------------------------------------------------------- 13. Crude Oil
c = Chapter('crude_oil', 'Crude Oil', 'Dug for, pumped up and split', 'mcremix:crude_oil_bucket')
c.q('pocket', 0, 0, 'Oil Pockets', 'Found only by digging', [
	"Crude oil hides in sealed pockets deep underground, between y -40 and -16, under every biome. A skin of "
	"stone keeps them off caves and water, so the only way to find one is to dig.",
	"Spilt crude creeps out slowly, clings like cobweb and burns.",
], [item('mcremix:crude_oil_bucket')], deps=['steel_column:condenser'])
c.q('hose', 0, 1, 'An Endless Pocket', 'Pumped up, not mined out', [
	"To a hose pulley, any pocket is endless, however small. The catch: crude comes up at a slow, fixed rate, "
	"however fast the pump turns.",
	"@Ponder: the hose pulley.",
], [item('create:hose_pulley')], deps=['pocket'])
c.q('cuts', 1, 0, 'Six Cuts', 'Crude, split', [
	"Crude distils at {Hot} into sour gas, naphtha, kerosene, diesel and heavy oil, with bitumen left behind. "
	"Sour gas leaves by the still's vent or the condenser's gas outlet.",
	"Kerosene, diesel and heavy oil come out of a column hot, and bitumen is always warm.",
], [item('mcremix:diesel_bucket')], deps=['pocket'])
c.q('engine', 2, 0, 'The Diesel Engine', 'Crank it to start', [
	"The diesel engine is one block built round a steel flywheel. Crank it briskly for two seconds and it "
	"catches, running on diesel or, weaker, kerosene, and turning the way it was cranked.",
	"Run it dry or overload it and it stalls. Back to the crank.",
	"@JEI: the Diesel Engine tab. Ponder: the diesel engine.",
], [item('mcremix:diesel_engine')], deps=['cuts'], **MILESTONE)
c.q('cracking', 2, -1, 'Cracking Naphtha', 'More ethylene', [
	"Naphtha and pressurised steam crack in a Steel Furnace at {Searing} into ethylene and fuel gas, and fuel "
	"gas burns {Blazing}.",
], [check()], deps=['cuts', 'steel_furnace:steam', 'steel_furnace:flow'], icon='mcremix:naphtha_bucket',
	optional=True)
c.q('wax', 2, 1, 'Paraffin Wax', 'Heavy oil, settled', [
	"Heavy oil settles into paraffin wax and fuel oil. Wax waxes anything honeycomb does, so the bees can take a "
	"break, and it makes candles.",
], [item('mcremix:paraffin_wax')], deps=['cuts'], optional=True)
c.q('pet_coke', 1, 1, 'Petroleum Coke', 'Coke from the bottom of the barrel', [
	"Boil bitumen down in a basin over {Searing} heat for petroleum coke, which stands in for Coal Coke "
	"everywhere.",
], [item('mcremix:petroleum_coke')], deps=['cuts'], optional=True)

# ---------------------------------------------------------------- 14. Liquid Air
c = Chapter('liquid_air', 'Liquid Air', 'Squeezed, chilled and split', 'mcremix:liquid_air_bucket')
c.q('valve', 0, 0, 'The Liquefier', 'Squeeze, chill, let go', [
	"An Encased Fan blowing into a Pressurizer's vessel feeds it air, and from 256 RPM the Pressurizer squeezes "
	"it hot. A Steel Condenser with coolant in its jacket chills it, and an aluminium Expansion Valve lets it "
	"go, a fifth of it turning liquid.",
	"The rest comes out cold, ready for another lap.",
	"@JEI: the Liquefying tab.",
], [item('mcremix:expansion_valve')], deps=['steel_column:condenser', 'cooling:coolant',
	'steel_furnace:pressurizer'])
c.q('liquid', 1, 0, 'Liquid Air', 'Cold enough to crack steel', [
	"Liquid air is cryogenic. Steel and iron pipes, pumps and tanks frost white, then crack and burst, and PVC "
	"fares no better. Copper, aluminium and glass take it, and tooltips say what cracks.",
	"Spilt, it boils off in seconds, freezing the water beside it.",
], [item('mcremix:liquid_air_bucket')], deps=['valve'])
c.q('air_column', 2, 0, 'The Air Column', "Boiled by the world's warmth", [
	"Aluminium Column Sections join like steel ones, but need no heat and no condenser: to liquid air, the world "
	"is a furnace. Nitrogen comes off the top, oxygen the bottom and argon a section of its own between. Spare "
	"sections sit just under the top and draw nothing, and goggles show which section draws what.",
	"Argon and oxygen boil only 3 °C apart, so the column needs extra sections between them, or liquid nitrogen "
	"piped back into its top as reflux. It frosts up and grows icicles as it runs, and melts beside anything "
	"{Hot}.",
], [look('mcremix:aluminium_column_section[frost=2]', 'Look at an air column grown icicles',
	'mcremix:aluminium_column_section')], deps=['liquid'], **MILESTONE)
c.q('oxygen', 3, -1, 'Oxygen', 'A hotter fire', [
	"Pipe oxygen into a lit Steel Furnace beside its fuel and the fire burns twice as hot, its ceiling a level "
	"higher. Let loose, oxygen makes fire spread.",
], [check()], deps=['air_column'], icon='minecraft:fire_charge')
c.q('ammonia', 3, 0, 'Ammonia and Urea', 'Nitrogen, fixed', [
	"Press iron nuggets with alumina into iron catalyst pellets. Drop one in a Pressurizer's vessel and nitrogen "
	"and hydrogen squeeze into ammonia over {Hot} heat.",
	"Squeeze ammonia with carbon dioxide for urea, bone meal by the bucket: on a crop it grows one stage.",
], [item('mcremix:urea')], deps=['air_column', 'electricity:chlorine'])
c.q('platinum', 4, 1, 'Platinum', 'Rare in red sand', [
	"Washing red sand turns up a platinum nugget about once in a hundred. Hoard them: nuggets make a gauze, a "
	"catalyst that lasts a long time.",
], [item('mcremix:platinum_gauze')], deps=['aluminium:bauxite'])
c.q('nitric', 4, 0, 'Nitric Acid', 'Ammonia, burnt', [
	"Burn ammonia and oxygen over platinum gauze in a {Searing} Steel Furnace for nitrogen dioxide, a brown, "
	"poisonous haze. Stir it with water in a vat for nitric acid, or with caustic soda for sodium nitrite.",
	"Every coal tar dye needs nitric acid, so make plenty.",
], [item('mcremix:nitric_acid_bucket')], deps=['ammonia', 'platinum', 'oxygen'])
c.q('argon', 2, -1, 'Argon', 'A bonus and a glow', [
	"Pipe argon into an Arc Furnace's sides and each sponge gives half an ingot more. Or have a Spout fill a "
	"glass tube with it for an argon tube, which glows violet with enough volts across it.",
], [item('mcremix:argon_tube')], deps=['air_column'], optional=True)
c.q('nitrogen', 2, 1, 'Liquid Nitrogen', 'The coldest coolant', [
	"In a water cooler, liquid nitrogen cools six times as fast as water and boils off as nitrogen. A Steel "
	"Condenser's jacket and radiators won't take it.",
], [item('mcremix:liquid_nitrogen_bucket')], deps=['air_column'], optional=True)

# ---------------------------------------------------------------- 15. Sulfur and Dyes
c = Chapter('dyes', 'Sulfur and Dyes', 'The last chemistry, and four new colours', 'mcremix:mauve_dye')
c.q('sulfur', 0, 0, 'Sulfur', 'Cleaned out of sour gas', [
	"Sour gas is fuel gas carrying hydrogen sulfide, which poisons and flashes. Clean it and the sulfur comes "
	"out as a bonus.",
	"Small scale: pipe sour gas through a vat of red mud with no mixer, then air the foul mud in an open basin, "
	"where it drops sulfur and turns red again. Big scale: wash the gas with soda ash solution in a stirred vat, "
	"boil the foul soda for hydrogen sulfide, and burn that over alumina in a Steel Furnace into molten sulfur.",
	"@JEI: the Airing tab.",
], [item('mcremix:sulfur')], deps=['crude_oil:cuts', 'aluminium:liquor'])
c.q('sulfuric', 1, 0, 'Sulfuric Acid', 'Sulfur, burnt twice', [
	"Burn sulfur in a Furnace for sulfur dioxide, and platinum gauze in a {Hot} Steel Furnace turns that into "
	"sulfur trioxide. Stir it into water in a vat for sulfuric acid, but half fogs off as acid mist unless "
	"there's acid in the vat already.",
	"Mixed with nitric acid, it nitrates four times as fast.",
], [item('mcremix:sulfuric_acid_bucket')], deps=['sulfur', 'liquid_air:platinum'])
c.q('aniline', 0, 2, 'Aniline', 'The root of the dyes', [
	"Nitrate light oil into nitrobenzene, then cook that with iron and vinegar into aniline. All basin work; JEI "
	"has each step.",
], [item('mcremix:aniline_bucket')], deps=['liquid_air:nitric', 'glass_lab:tar_cuts'], **MILESTONE)
c.q('diazo', 2, 1, 'Keep It on Ice', 'Diazo salts spoil warm', [
	"Aniline, hydrochloric acid, sodium nitrite and ice give a diazo salt, and diazo salts are fussy. Unless ice "
	"or snow touches whatever holds one, or it stands somewhere cold enough to snow, it breaks down. Heat ruins "
	"it outright.",
	"Goggles tell you when it's warming up.",
], [item('mcremix:diazo_salt_bucket')], deps=['aniline', 'steel_furnace:acid'])
c.q('maroon', 3, 0, 'Maroon', 'Needs the acid', [
	"Maroon comes from naphthylamine's diazo salt with naphthol, and naphthol needs sulfuric acid. Yellow, "
	"orange and red come from diazo salts too, so you'll get plenty of practice.",
], [item('mcremix:maroon_dye')], deps=['sulfuric', 'diazo'])
c.q('mauve', 1, 3, 'Mauve', 'The first coal tar dye', [
	"Mauve comes from aniline and bleach. The real thing launched the synthetic dye industry, by accident. It's "
	"one of four colours only the dye works makes.",
], [item('mcremix:mauve_dye')], deps=['aniline'])
c.q('indigo', 0, 3, 'Indigo', 'Melted and aired', [
	"Indigo comes from phenylglycine melted with caustic soda, the melt aired into dye. Sodamide in the melt "
	"triples it.",
], [item('mcremix:indigo_dye')], deps=['aniline'])
c.q('malachite', -1, 3, 'Malachite Green', 'Turned green at the end', [
	"Benzaldehyde and dimethylaniline give a colourless base, and ferric chloride turns it green at the last "
	"step.",
], [item('mcremix:malachite_green_dye')], deps=['aniline'])
c.q('pale', 1, 4, 'Pale Shades', 'Four more colours', [
	"Mix each new dye with white for a pale shade: lilac, periwinkle, mint and rose. All eight go on everything "
	"vanilla and Create dye, from wool and glass to seats and sails.",
], [item('mcremix:lilac_dye')], deps=['mauve'], optional=True)
c.q('gunpowder', 0, -1, 'Gunpowder', 'Made, not hunted', [
	"Soda ash and nitric acid give saltpetre, and saltpetre with sulfur and charcoal gives gunpowder. The "
	"creepers can keep theirs.",
], [item('mcremix:saltpetre')], deps=['sulfur', 'liquid_air:nitric'], optional=True)

# ---------------------------------------------------------------- side roads
c = Chapter('ruins', 'Ruins', 'The villages are empty', 'minecraft:mossy_cobblestone', side=True)
c.q('ruin', 0, 0, 'A Ruined Village', 'Long abandoned', [
	"Every village is a ruin, each its own age: roofs caved in, walls crumbled, rubble where it fell, and nature "
	"moving back in. Nobody lives there, not even zombies or golems.",
	"Salvage what's left: cold campfires, lanterns, beds, workstations and the odd Cold Furnace. Someone got to "
	"the chests first and took anything valuable. The streets are dark at night, and monsters walk them.",
], [structure('#minecraft:village', 'Find a village', 'minecraft:bell')])
c.q('villagers', 1, 0, 'Bringing People Back', "Cure, don't breed", [
	"Every villager comes from curing a zombie villager out in the world, and they don't breed. Each one you "
	"cure is worth keeping safe.",
], [item('minecraft:golden_apple')], deps=['ruin', 'hearth:hot'])
c.q('cold_furnace', 1, 1, 'A Cold Furnace', 'Still good for something', [
	"Nothing makes vanilla's furnace any more; you'll find them in ruins as Cold Furnaces. They smelt nothing, "
	"but smokers and blast furnaces, the butcher's and armorer's workstations, still craft round one, or round "
	"the Furnace.",
], [item('minecraft:furnace')], deps=['ruin'], optional=True)

c = Chapter('decor', 'Decor', 'Things built to look right', 'mcremix:steel_catwalk', side=True)
c.q('movers', 0, 0, 'Wooden Movers', 'Drawbridges before andesite', [
	"Mechanical bearings, pistons, gantries and rope pulleys come in every wood, before you have andesite alloy, "
	"so drawbridges and gates can match what they're built into.",
], [item('mcremix:oak_mechanical_bearing')], optional=True)
c.q('creosote', 1, 0, 'Creosote Timber', 'Soaked dark', [
	"A Spout of creosote soaks stripped logs and planks into creosote timber, with posts, fences and stairs of "
	"its own.",
], [item('mcremix:creosote_log')], deps=['glass_lab:tar_cuts'], optional=True)
c.q('catwalks', 2, 0, 'Catwalks', 'Steel walkways', [
	"Steel catwalks, catwalk stairs, railings and caged ladders, for getting round a plant without falling into "
	"it. They rust where they get wet, but only to the first stage.",
	"@Ponder: the catwalk.",
], [item('mcremix:steel_catwalk')], deps=['steel:steel'], optional=True)
c.q('plating', 3, 0, 'Titanium Plating', 'Tinted by fire', [
	"Fire titanium plating in a Furnace and it takes the colour of the heat: gold at {Hot}, purple at {Searing}, "
	"blue at {Blazing}. A colour only gets deeper, and acid strips it back.",
], [item('mcremix:titanium_plating')], deps=['titanium:titanium'], optional=True)
c.q('tiles', 4, 0, 'Encaustic Tiles', 'Four make a figure', [
	"Each new colour's glazed terracotta is a Victorian encaustic tile. Place four facing round and they join "
	"into a bigger figure, as vanilla's do.",
], [item('mcremix:mauve_glazed_terracotta')], deps=['dyes:mauve'], optional=True)


# ---------------------------------------------------------------- SNBT

def snbt(v, indent=0):
	tab = '\t' * indent
	if isinstance(v, bool):
		return 'true' if v else 'false'
	if isinstance(v, Long):
		return '%dL' % v
	if isinstance(v, int):
		return str(v)
	if isinstance(v, float):
		return repr(v) + 'd'
	if isinstance(v, str):
		return '"' + v.replace('\\', '\\\\').replace('"', '\\"') + '"'
	if isinstance(v, dict):
		lines = ['{']
		for k in sorted(v):
			lines.append(tab + '\t' + k + ': ' + snbt(v[k], indent + 1))
		lines.append(tab + '}')
		return '\n'.join(lines)
	if isinstance(v, list):
		if not v:
			return '[ ]'
		lines = ['[']
		for x in v:
			lines.append(tab + '\t' + snbt(x, indent + 1))
		lines.append(tab + ']')
		return '\n'.join(lines)
	raise TypeError(type(v))


def crossings():
	"""Dependency lines in a chapter that cross each other or run through a quest, to keep each chapter readable."""
	def cross(a, b, c, d):
		def orient(p, q, r):
			v = (q[0] - p[0]) * (r[1] - p[1]) - (q[1] - p[1]) * (r[0] - p[0])
			return (v > 1e-9) - (v < -1e-9)
		if {a, b} & {c, d}:
			return False
		return orient(a, b, c) * orient(a, b, d) < 0 and orient(c, d, a) * orient(c, d, b) < 0
	def near(p, a, b):
		ax, ay, bx, by = a[0], a[1], b[0], b[1]
		t = max(0, min(1, ((p[0] - ax) * (bx - ax) + (p[1] - ay) * (by - ay)) / ((bx - ax) ** 2 + (by - ay) ** 2)))
		return ((ax + t * (bx - ax) - p[0]) ** 2 + (ay + t * (by - ay) - p[1]) ** 2) ** 0.5 < 0.35
	found = []
	for ch in CHAPTERS:
		at = {q['key']: (q['x'], q['y']) for q in ch.quests}
		lines = [(d, q['key']) for q in ch.quests for d in q['deps'] if ':' not in d]
		for i, (a, b) in enumerate(lines):
			for c, d in lines[i + 1:]:
				if cross(at[a], at[b], at[c], at[d]):
					found.append('%s: %s-%s crosses %s-%s' % (ch.key, a, b, c, d))
			for k, p in at.items():
				if k not in (a, b) and near(p, at[a], at[b]):
					found.append('%s: %s-%s runs through %s' % (ch.key, a, b, k))
	return found


def build():
	for line in crossings():
		print('warning:', line)
	quest_ids = {}
	for ch in CHAPTERS:
		for q in ch.quests:
			key = ch.key + ':' + q['key']
			if key in quest_ids:
				raise SystemExit('duplicate quest ' + key)
			quest_ids[key] = make_id('quest', key)
	ids = set(quest_ids.values())
	files = {}
	side_group = make_id('group', 'side')
	files['chapter_groups.snbt'] = {'chapter_groups': [{'id': side_group, 'title': 'Side Roads',
		'icon': 'minecraft:mossy_cobblestone'}]}
	files['data.snbt'] = {
		'version': 13, 'title': 'Minecraft Remix', 'icon': 'mcremix:kiln',
		'default_reward_team': False, 'default_consume_items': False, 'default_autoclaim_rewards': 'disabled',
		'default_quest_shape': 'circle', 'default_quest_disable_jei': False, 'emergency_items_cooldown': 300,
		'drop_loot_crates': False, 'loot_crate_no_drop': {'passive': 4000, 'monster': 600, 'boss': 0},
		'disable_gui': False, 'grid_scale': 0.5, 'pause_game': False, 'lock_message': '',
		'progression_mode': 'linear', 'detection_delay': 20,
	}
	order = {False: 0, True: 0}
	for ch in CHAPTERS:
		quests = []
		seen = {}
		for q in ch.quests:
			at = (q['x'], q['y'])
			if at in seen:
				raise SystemExit('%s: %s and %s share a spot' % (ch.key, seen[at], q['key']))
			seen[at] = q['key']
			key = ch.key + ':' + q['key']
			deps = []
			for d in q['deps']:
				full = d if ':' in d else ch.key + ':' + d
				if full not in quest_ids:
					raise SystemExit('%s depends on unknown %s' % (key, full))
				deps.append(quest_ids[full])
			tasks = []
			for i, t in enumerate(q['tasks']):
				t = dict(t)
				t['id'] = make_id('task', key, str(i))
				ids.add(t['id'])
				tasks.append(t)
			out = {
				'id': quest_ids[key], 'title': text(q['title']), 'subtitle': text(q['subtitle']),
				'description': paragraphs(q['desc']), 'x': q['x'] * SPACING, 'y': q['y'] * SPACING,
				'tasks': tasks, 'dependencies': deps, 'rewards': [],
			}
			if q['icon']:
				out['icon'] = q['icon']
			if q['shape']:
				out['shape'] = q['shape']
			if q['size']:
				out['size'] = float(q['size'])
			if q['optional']:
				out['optional'] = True
			quests.append(out)
		chapter = {
			'id': make_id('chapter', ch.key), 'filename': ch.key, 'title': ch.title, 'subtitle': [ch.subtitle],
			'icon': ch.icon, 'group': side_group if ch.side else '', 'order_index': order[ch.side],
			'default_quest_shape': '', 'default_hide_dependency_lines': False, 'quest_links': [], 'images': [],
			'quests': quests,
		}
		order[ch.side] += 1
		files['chapters/' + ch.key + '.snbt'] = chapter
	if len(ids) != len(quest_ids) + sum(len(q['tasks']) for ch in CHAPTERS for q in ch.quests):
		raise SystemExit('id clash')
	return files


def main():
	files = build()
	text_files = {name: snbt(v) + '\n' for name, v in files.items()}
	digest = hashlib.sha1()
	for name in sorted(text_files):
		digest.update(name.encode() + b'\0' + text_files[name].encode('utf-8') + b'\0')
	os.makedirs(os.path.join(OUT, 'chapters'), exist_ok=True)
	for name in os.listdir(os.path.join(OUT, 'chapters')):
		if 'chapters/' + name not in text_files:
			os.remove(os.path.join(OUT, 'chapters', name))
	for name, body in text_files.items():
		with open(os.path.join(OUT, name), 'w', encoding='utf-8', newline='\n') as f:
			f.write(body)
	with open(os.path.join(OUT, 'index.txt'), 'w', encoding='utf-8', newline='\n') as f:
		f.write('# The quest book Minecraft Remix installs for FTB Quests, made by tools/gen_quests.py.\n')
		f.write('book ' + digest.hexdigest()[:16] + '\n')
		for name in sorted(text_files):
			f.write(name + '\n')
	quests = sum(len(ch.quests) for ch in CHAPTERS)
	print('%d chapters, %d quests, book %s' % (len(CHAPTERS), quests, digest.hexdigest()[:16]))


if __name__ == '__main__':
	main()

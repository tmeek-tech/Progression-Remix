# Minecraft Remix

A Forge 1.20.1 overhaul built on Create. It remixes vanilla progression around one shared heat scale, shafts and
cogs that need supports to hold them, in materials that weather, steel that runs to 512 RPM, machines in every
metal, drills and saws fitted with tool tiers, pipes and tanks in every metal rated for heat, a crucible that melts and pours metal,
electrolysis on Create: Power Grid's circuits, aluminium that runs hot unless it is cooled, plastics and PVC pipes, titanium that turns at 2,048 RPM and burns unless water cools it, and abandoned villages. The design plan is in [docs/DESIGN.md](docs/DESIGN.md).

| | |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.4.6 (works on 47.1.3 and later) |
| Required | Create 6.0.7 or later 6.0.x, Ponder (ships with Create), Create: Power Grid 0.6.2 or later, Architectury API (Power Grid needs it) |
| Optional | Immersive Weathering, JEI |
| Plays alongside | William Wythers' Overhauled Overworld, Better Combat |

## What is in 0.28.1

**Heat and the Furnace.** Vanilla's furnace (now the **Cold Furnace**), smoker and blast furnace have gone cold. The
**Furnace** takes their place: crafted exactly like vanilla's, from eight cobblestone, and in vanilla's furnace
textures, but its front is cut open and it has no menu. Click things into its upper chamber and fuel into its
low firebox, watch them glow on the grate, take them out by hand or with a hopper underneath. Everything that heats
shares one scale: Cold, Warm, Hot, Searing, Blazing. Wood burns Warm, charcoal and coal Hot (so does Immersive
Weathering's charred wood), Coal Coke and blaze rods Searing. A furnace also takes heat from the block under it:
campfires and magma (Warm), fire and lava (Hot), soul fire and fed blaze burners (Searing), superheated blaze burners
(Blazing). Every level above what a recipe needs halves its time. Fuel in the firebox waits until the furnace is lit,
with flint and steel or a fire charge, by hand or from a deployer, or by a Spark Plug; lit, it burns whether or not
there is anything to fire, until the fuel runs out and the fire dies. A shovel puts it out and leaves the fuel in. Heat
from underneath needs no lighting.

**Steam and mixing.** A furnace heats a Create steam boiler or basin standing on it. A Warm or Hot fire only keeps a
boiler simmering, as a campfire would, and is too cool for heated recipes; a Searing one (Coal Coke, or charcoal
fanned by a Bellows) heats either like a fed blaze burner, so steam engines and brass no longer wait for blazes. Heat
from under the furnace passes up unchanged, and a lit furnace burns its fuel whether or not the boiler or basin has
work, firing its chamber at the same time. Superheating takes a blaze burner, or a Steel Furnace burning hydrogen or
ethylene at Blazing.

**Bellows.** A Create-powered Bellows blows into the block its nozzle faces. A furnace there, or a furnace standing on
a fire there, burns one heat level hotter (up to Searing), and fuel it fans burns twice as fast; a campfire it blows
into cooks twice as fast. So a campfire-heated furnace reaches Hot with no fuel, and a lava-heated one reaches
Searing. It works at any speed (2 su per RPM); a crank at the back pumps the lid once per turn.

**Bloom iron.** Raw iron no longer smelts. A Searing furnace fires it into a glowing **Iron Bloom**; four strikes on
an anvil (or one Mechanical Press) beat out an ingot and often **Slag**, which fertilises like bone meal and washes
back into iron nuggets.
Before there is iron for an anvil, a **Stone Anvil** (smooth stone, stone and cobblestone in the anvil's pattern) does
the striking: it has no menu, cracks as you work it, and crumbles after about fifteen blooms. Crushed raw iron fired
at Blazing gives two blooms. Ancient debris needs a Blazing furnace.

**Steel.** Four iron ingots, four Coal Coke and a clay ball to seal them make a **Cementation Pack**. Fired at Searing,
from any heat source, the iron soaks up carbon and comes out as four **Blister Steel**, the slowest firing there is.
Blister steel is brittle, and only a Mechanical Press beats it into a **steel ingot**. Ingots press into **steel
sheets**, and a sheet applied to a stripped log makes **Steel Casing**, the way Create makes its casings.

**Supports.** Shafts and cogs no longer hang in the air. Each is held by a **support**: a machine,
gearbox, clutch, gearshift, chain drive, encased shaft or cog, or a shaft or cog with a **bracket** bolted to a solid
block (a floor, wall, ceiling or the top of a post). A support holds the parts in line with it as far along as
their material reaches: wood and brass two blocks, andesite, copper and iron four, steel, aluminium and titanium six. A part out
of reach of every support will not go on. A bracket goes down on its own against a solid face, and a shaft or
cog placed into it lies along it and is held by it; a bracket also clips onto a part already placed, and with brackets
in your offhand one is fitted to each part as you place it. Take a support away and the parts only it held drop
off; break the block a bracket is bolted to and the bracket falls off. Brackets come in every wood, brass, copper, iron, steel, aluminium and titanium besides Create's own, and
any shaft or cog goes into any material's casing (andesite, brass, copper, iron, steel, aluminium or titanium; iron's is the
**Iron Casing**, an iron sheet on a stripped log), which is never used up. A cog used on a placed casing goes into it
the other way round, as an encased cog across the face you clicked, and the casing pops back out (sneak to put the cog
beside it instead). A rotation speed controller holds the large cog in its top slot. Goggles show what holds a part, how far it reaches, its top speed and its load limit. A
support's tooltip (a gearbox's, clutch's, gearshift's, chain drive's, bracket's, casing's or machine's) rates its
**Stability**, the top speed it turns at (Wobbly to Rock-solid), and a shaft's or cog's its **Strength**, the load it
carries (Flimsy to Unbreakable), with a bar, as Create rates stress; with goggles on it gives the RPM or su.

**Shaft and cog materials.** Only supports have a top speed, their Stability: a part in a bracket or casing turns
as fast as the bracket or casing allows, whatever the part is made of, and a gearbox or machine as fast as its own
material allows. Wood turns to 64 RPM, brass 96, Create's andesite parts 128, copper and iron 256, steel
and aluminium 512, titanium 2,048; past it a support grinds, smoking or sparking, then breaks, and what only it held drops. The
bare parts in between take any speed. Shafts and cogs have a Strength instead, the load they carry: wooden, brass,
copper and aluminium ones carry 2048, 3072, 4096 and 8192 su, and put more stress through a network than one carries
and it wobbles for about a second, then breaks off and drops, bare, bracketed or encased. Andesite, iron, steel and
titanium ones carry any load, and supports never break from load, so a wooden shaft is a fuse between an engine
and a risky machine. **Brass** shafts and cogs are delicate polished clockwork that never tarnishes, only a little stronger than
wood (brass already makes some strong devices): a slender plain arbor and thin spoked wheels, with brackets drawn as
a watch's clock bridges. Brass has gearboxes and chain drives but no clutch, gearshift or chain gearshift. Copper oxidizes; iron is slightly heavier to turn and rusts only when
wet or under rustier iron; steel is the heaviest and rusts like iron, but only to its first stage; aluminium has no drag at all and never weathers,
but melts beside anything Hot, and its brackets and casing (and so its gearboxes, clutches and chain drives) need
a Polyethylene Bushing (see Plastics below); titanium is the fastest of all (see Titanium below). Steel, aluminium and
titanium are the only materials past 256 RPM, so Create's speed cap is raised to 2,048 when a world loads
(`raiseCreateSpeedCap` in the server config). Copper, iron and steel
parts, brackets and casings weather through four stages like copper blocks, each stage adding friction at a
support; honeycomb waxes them and axes scrape them.

**Every wood.** Wooden shafts, brackets, gearboxes, clutches and gearshifts come in all eleven woods, oak to bamboo, so they can
match what they are built into. Only the look changes: every wooden support tops out at 64 RPM and every wooden shaft gives way first under load. Crimson and warped
parts are cut from stems and do not burn. Each part is made of one wood: its logs make its shafts, and its stripped log
or wood cases its gearbox and clutch. The one wooden part of earlier versions loads as oak.

**Wooden movers.** Mechanical bearings, pistons (sticky too) with their poles, gantry carriages and gantry shafts, and
rope pulleys also come in every wood, for drawbridges and gates. They work exactly like Create's: a wooden piston
pushes out its own wood's head on any wood's poles, slime makes it sticky, and a wooden carriage rides any gantry
shaft, Create's included. The ones that turn top out at 64 RPM like other wood, and Create's own count as andesite
(128 RPM). Crafted like Create's from one wood: its stripped log as the casing and its shaft as the shaft, its slab in
a rope pulley, its gearbox in a gantry carriage, and planks round a stick (poles) or redstone (gantry shafts), so no
andesite alloy is needed.

**Gearboxes.** Wooden, copper, iron and steel gearboxes join Create's, which is the andesite one and now shares the andesite
top speed (128 RPM). Each relays rotation like Create's gearbox with its material's top speed and weathering, so fast
lines need copper, iron or steel gearboxes and a wooden one is the cheapest. Crafted like Create's: four shafts or
cogs of the material around a stripped log, copper casing, Iron Casing or steel casing. A gearbox stands upright, but lies on
its side to meet a shaft above or below it (or when placed sneaking), so there is no separate vertical item.

**Clutches and gearshifts.** Create's clutch and gearshift are andesite too, so they also stop at 128 RPM. Wooden,
copper, iron and steel ones cut off or reverse the far side when powered, just like Create's, with their material's top speed
and weathering. Crafted like Create's, from the material's casing, a shaft (clutch) or cog (gearshift) and redstone;
wood has no cogs, so a wooden gearshift is a wooden gearbox and redstone. A wooden clutch is the cheapest switch.

**Chain drives.** Create's encased chain drive and adjustable chain gearshift are andesite as well, so they stop at
128 RPM, and a powered chain gearshift doubling 128 RPM now breaks the andesite drives past it. Copper, iron and steel
chain drives and chain gearshifts relay rotation along a row just like Create's, with their material's top speed and
weathering, and one row can mix materials. Crafted like Create's, from the material's casing and three of its
nuggets; a chain drive and an electron tube make a chain gearshift. Goggles on a powered chain gearshift say how much
faster the rest of its row turns.

**Machines in every metal.** Presses, mixers, millstones, encased fans, saws, drills, deployers and mechanical arms
come in copper, iron, steel, aluminium and titanium as well as Create's own, which count as andesite (the arm as
brass). Mechanical crafters are Create's brass ones only. Each works exactly like Create's and with Create's other machines (chutes, arms, crafter
grids, contraptions, schematics), in its casing's colours. A machine is a support held to its material's top
speed (brass 96 RPM, andesite 128, copper and iron 256, steel and aluminium 512, titanium 2,048) and breaks past it; it takes any
load (only the shafts and cogs driving it break from load), draws its friction, and copper, iron and steel ones weather like parts. Use a casing on a placed
machine to make it that material, a step down as well as up: it keeps its facing and contents and the casing it had
pops out. Crafted with Create's recipe and the tier's casing. Crushing wheels stay Create's andesite only: they are
no support and carry any load, but still turn no faster than 128 RPM. A copper, iron, steel or aluminium mixer
fits a vat of its own metal as its vat form.

**Drill bits and saw blades.** Every drill and saw, Create's and each metal's, comes with iron bits or an iron blade
and takes stone, gold or diamond ones: use them on a placed drill or saw to fit them (a deployer can too), and the old
ones pop out. They never wear, and a broken drill or saw keeps what it had fitted. Bits cut what a pickaxe of their
material could mine at that pickaxe's speed, so obsidian, crying obsidian and ancient debris need diamond bits (a third
faster than iron, twice the stress). Stone bits need no iron and half the stress but cut a third slower and stop at
gold, diamond and redstone ore; gold bits cut twice as fast for half the stress but only what a golden pickaxe can, so
they stop at iron, copper and lapis ore. A drill stuck on a block too hard for its bits throws sparks, and looking at it
says which bits would cut it. Blades work the same way on a saw, and every blade cuts wood: a gold blade strips and
planks logs twice as fast for half the stress, but gold is too soft for stone, which skates across it uncut in a shower
of sparks and moves on; diamond is the fastest stonecutter. Three of the material over an iron ingot make bits, over an
iron sheet a blade. The stone, golden and diamond drills and saws of earlier versions are gone.

**Weathering.** Copper and iron weather within a day: vanilla's copper blocks, stairs and slabs, Create's copper
shingles and tiles, this mod's parts and, with Immersive Weathering, its cut and plate iron and iron doors, trapdoors
and bars. Each block weathers by itself rather than waiting for the blocks around it, and stops at a stage of its own,
the second-to-last or the last (weathered, or fully oxidized or rusted), picked for its spot; so a build weathers into
a mix, not one flat green. The copper on Water Coolers, Radiators and the Arc Furnace weathers too, for looks only. An
axe scrapes a block back and it weathers to the same stage again; honeycomb keeps it as it is. Iron only rusts while
wet, in rain or water, so indoor machines stay free of friction and iron under a roof stays clean; but rust runs
down, so dry iron under rustier iron rusts too, never past the block above it. The pace and the
mix are in the server config, which can also switch all of this off.

**Fluids and heat.** Every fluid has a heat on the furnace's scale, and every pipe, pump and tank a limit.
Aluminium pipes, pumps and tanks hold only Warm fluids; brass, iron and glass ones and Create's copper parts hold up to
Hot, like lava; steel ones hold up to Searing, like molten copper, gold and brass. Nothing carries Blazing iron, steel
or netherite: those are poured. A part holding something too hot glows and smokes for about three seconds, then bursts
(glass shatters) and spills what it held; aluminium melts into rough aluminium, and beside anything Hot it melts too.
Glass pipes carry a quarter as much, never corrode and show the fluid inside. **Brass** pipes are thin and fine, 6 px
across, and a line with one in it carries half as much. **Iron** pipes, pumps and tanks rust in the weather, never from
what runs inside, and rusted through they drip until scraped, cleaned with acid or waxed. With `rustedIronLeaks` on in
the server config (off by default) they also leak: every rusted block loses 1 mB a tick, liquids dripping away and gases
getting out as clouds. **Steel** pipes, pumps and tanks rust as iron does, but only to their first stage, and never
leak. **Aluminium** ones never weather. Pumps run to
96 RPM in brass, 256 in iron and Create's copper, and 512 in steel and aluminium (aluminium's heating with friction, as
its supports do). The **Brass Mechanical Pump** has a handwheel: hold right-click on it to set the most it lets
through, from 1 to 24 mB a tick, which is all it moves at 96 RPM. Every pipe, pump and tank is crafted as Create's copper ones are, in its own metal, but
iron's upright grids are Create's chute and item vault, so iron pipes are made across only and an iron tank has its
sheets either side of the barrel. Create's copper pipes, pumps and tanks weather like copper blocks, for looks only. A pipe joins only pipes of its
own metal (copper, brass, iron, glass, steel, aluminium or PVC), while pumps, valves and smart pipes join any, so one
of them goes between two metals. Iron and brass tanks make Create's ordinary
boilers, aluminium ones none. A boiler of **steel tanks** is compact, a level for every two tanks, and runs only on
pressurised steam piped in from a Pressurizer, not on water and fire (its goggles show its size and steam). Steam its
engines do not use builds pressure: a steam whistle vents it, screaming, and without one the boiler bursts and scalds
whoever is near.

**Steam engines and electric pumps.** Brass, iron and steel have **steam engines** of their own, made as Create's is
with their metal's block in place of the copper block. Each goes only on tanks of its own metal, and Create's only on
copper ones; whatever its metal, an engine runs as Create's does, so the metal changes the boiler, not the engine.
An engine turns a shaft of any material, which needs a bracket or support like any bare shaft, and a boiler's gauge is
drawn in its tanks' metal. Aluminium tanks make no boiler, so there is no aluminium engine. Power Grid's **electric
pump** is the copper one, and iron, steel and aluminium have their own, each a Power Grid electric motor on a pipe of
its metal. They go faster the more power they take, up to their metal's pump speed (256 RPM in iron, 512 in steel and
aluminium), draw what Power Grid's draws, and hold, rust or melt as their metal's pipes do. There is no brass one: the
Brass Mechanical Pump's handwheel does its job, and one from an older world is Power Grid's pump.

**The Crucible.** A fired clay pot hung on a shaft. It melts ingots, nuggets, sheets, blocks and raw or crushed ore
(90 mB an ingot) with the heat under it: zinc and aluminium at Hot; copper, gold and brass at Searing; iron, steel and netherite over
a superheated Blaze Burner. Metal tools and armour, Create's diving gear among them, melt into half the metal they were
made of. Metals that meet in it join as brass, steel (iron and a Coal Coke) or netherite (gold and
netherite scrap). Turning its shaft tips it over to pour whatever rises over its lip into what is in front: a **Sand
Mold** on a depot or belt (pressed from sand round an ingot or a block), which sets and breaks out as an ingot or a
block; a tank; another crucible; or the ground, where molten metal runs like slow lava, starts fires and sets into
rough metal that mines back into ingots. Twenty-seven crucibles placed as a cube join into one big crucible that works
nine times as fast.

**Power Grid and Bakelite.** Create: Power Grid is required now: its motor and transformer core take steel, and its
circuit boards, switches, buttons and sockets take **Bakelite**. Coking coal in the Furnace gives off **coal tar**, and
charring logs gives off **wood spirit**; pipes draw them off the Furnace, and what nothing takes burns away at the
chimney. A press over a heated basin sets 100 mB of each hard into Bakelite.

**Salt water, chlorine and hydrogen.** Water taken from the sea, by bucket, pump or hose pulley, is **salt water**.
Split by an Electrolyser it gives **chlorine**, **hydrogen** and **caustic soda**. Chlorine let loose sinks into a low
green cloud that hurts whoever breathes it and kills the plants under it; mixed with water and an iron nugget it makes
**ferric chloride**, which etches Power Grid's circuit boards. Hydrogen pumped into a Steel Furnace burns hotter the
faster it comes, up to Blazing, and let loose it just hisses away.

**The Electrolyser.** A Power Grid device that sits on the side of a basin or a vat, a black Bakelite head with two
**Carbon Electrodes** (coke and tar, pressed and baked) reaching into it from its + and - terminals, so you can see what
is fitted. Wire its + and - across a source: nothing flows until
about 4 V, then the current, and how fast it splits, climb steeply, so a generator straight across one cooks it. Step
the voltage down with a transformer, or wire several in series. The anode burns away by the power through it, the
cathode a tenth as fast. On a basin, the gases it makes escape: a basin holds no gas, and gas piped into one rises off
into the air. A **vat** is a stack of tanks, one to three wide and up to five high, on a whole layer of andesite
scaffolding: it holds up to four fluids at once in layers, heaviest at the bottom and gases at the top, keeps its gases
for its pipes, settles and ferments many batches at once, and sends what it makes as items out of an outlet under
its cone, piped to every inventory beside or under its scaffolding; a funnel on the scaffolding, facing out, pulls them
out too. Standing, it looks like a brewery's conical fermenter, all in its tanks' own metal: the scaffolding
becomes a cone on braced legs, each side of its bottom layer has a manway door unless something is fitted there, and
its windows shut, so goggles show what it holds.

**Aluminium.** Washing red sand turns up **bauxite**. Heated with caustic soda in a basin it dissolves into **bauxite
liquor** and leaves **red mud**, which fires into red mud bricks. Left to stand cool, in a basin or a vat, the liquor
settles out **aluminium hydroxide** and gives back some caustic soda; fired at Searing, the hydroxide becomes
**alumina**, which an Electrolyser splits into **molten aluminium** for the crucible and sand molds. Aluminium makes
every kind of shaft, cog, gearbox, clutch, gearshift and chain drive, on **Aluminium Casing**, and the **Heat Sink**:
bolted onto a Power Grid device it draws the heat out, so the device takes more power before it overheats, and a fan
through its fins makes it shed far more. Aluminium supports run hot too (see Friction heat below).

**The Steel Furnace.** Steel casing used on a Furnace cases it in steel; it keeps what it held, but its solid fuel
pops out. A Steel Furnace fires what a Furnace fires and heats a boiler or basin on top the same way, but it burns
only fuel pumped up into it from underneath (wood spirit, ethanol, coal tar, hydrogen or ethylene): no solid fuel, no
heat from below, no bellows. Lit, it burns the fuel as it comes, hotter the faster it comes, so the pump's speed sets
its heat (a pump moves 1 mB a tick for every 2 RPM), up to each fuel's own ceiling: wood spirit Hot, ethanol and coal
tar Searing, hydrogen and ethylene Blazing. Fuel that comes faster than burns at the ceiling is lost up the chimney in
black smoke, and a second after the fuel stops the fire goes out. Fuel pumped into it unlit escapes out of the front:
liquids pour out as puddles to clean up, gases vent. It works fluids too, piped or poured in at its front, sides or
top: ethanol run over alumina in its chamber at Hot becomes ethylene (an alumina lasts 16 buckets), and at Searing it
cracks ethylene dichloride into vinyl chloride and hydrogen chloride.

**The Spark Plug.** A copper core in Bakelite in an iron shell, screwed into the side or back of a Furnace or Steel
Furnace (never its front). On a Power Grid circuit it sparks at about 1,000 V (from 900 V, holding down to 800),
drawing about 10 mA, and lights the furnace whenever there is fuel in it to catch. It sparks the whole time it is
powered, so a switch or relay decides when, and it never wears out.

**Plastics.** Sugar left in water in a basin or vat ferments into **ethanol**. The Steel Furnace turns ethanol into
**ethylene**, and the **Pressurizer**, a steel machine on a shaft, sets ethylene into **Polyethylene Pellets** at 512 RPM
over Warm heat or hotter. A press over a heated basin makes two pellets into a **Polyethylene Bushing**, which
aluminium brackets and casings need. Power Grid's integrated circuit takes pellets too. In a vat, chlorine and
ethylene stirred over a little ferric chloride make **ethylene dichloride**; the Steel Furnace cracks it into **vinyl
chloride**, giving off hydrogen chloride; and the Pressurizer squeezes vinyl chloride into **PVC Resin** at 256 RPM, heat
or no heat. A press makes resin into **PVC Sheets**, and three sheets make eight **PVC Pipes**. Let loose, ethylene
ripens the crops it drifts over a stage, and vinyl chloride makes whoever breathes it dizzy.

**The Pressurizer.** Its shaft is pressure: each job needs a speed (128 RPM for steam, 256 for PVC resin, 512 for
polyethylene) and goes faster above it, and it takes 32 su for every RPM. It is two blocks: a body with a pressure
dial on its front, whose needle reads the speed against its top speed and goes into the red past it, and its shaft
going in at either side, under a square pressure vessel narrower than a block. Pipes go on any of the vessel's four
sides, which puts out a nozzle to meet each one, and funnels there or an empty hand take what it makes. Over Hot heat at 128 RPM it turns water into **pressurised steam**,
20 mB a tick and more the faster it turns. Piped into a boiler of steel tanks, the steam drives its engines with no fire
under it: every 10 mB a tick is a level of heat and water both. The steel one stops at 512 RPM; the **Titanium
Pressurizer** (the same recipe in titanium, round the same steel tank) takes up to 2,048, up to four times as fast, but
like any titanium machine it heats as it turns, so past about 128 RPM it wants a Water Cooler, and its vessel takes
on the temper colours of its heat.

**PVC pipes.** Thin white pipes that lie flat on the faces of blocks, like red alloy wire, turning inside and outside
corners and joining side by side; a wrench cuts a joint. Where one meets a pump, tank or pipe it rises to meet it. They
run on Create's own pumps and network, a pump reaches twice as far through them (32 blocks), and they never weather,
but they hold only cold fluids: anything warmer makes them sag and burst.

**Vat mixing.** A Mechanical Mixer used on the side of a vat goes on as a plate of the vat's own tank with a cog on
its face, as though put inside, and like Create's mixer it turns by that cog, from the cogs beside it, not by a shaft. A mixer only fits a vat of its own material: copper, iron, steel and aluminium mixers each fit
vats of their own metal, Create's own works only over basins, and no mixer fits a brass vat. Sneaking puts down an
ordinary mixer instead. Turning, it stirs four batches at a time of wet, unheated mixing, Create's
and this mod's, and nothing settles or ferments in the vat meanwhile. Hydrogen chloride stirred into water makes
**hydrochloric acid**; an Electrolyser splits hydrogen chloride back into chlorine and hydrogen instead.

**Hydrochloric acid.** Poured from a bucket or an open pipe onto rusted iron or oxidized copper blocks, it strips them
back to bare metal and then soaks away; a Spout of it cleans a weathered part, bracket, pipe, pump or tank on a depot or belt, 100
mB each. Hydrogen chloride let loose is a pale mist that rusts the iron and copper it drifts over by a stage.

**Friction heat.** Aluminium supports (an aluminium bracket, casing, gearbox, clutch, gearshift, chain drive or
machine) warm up as they turn, the faster the hotter, and cool off on their own. Only what holds the part heats, so an
aluminium shaft in a steel bracket stays cool, and only titanium heats besides (see Titanium below). Goggles show how hot one is. Left
alone, aluminium runs whole up to about 256 RPM; faster, it glows and smokes from 520 °C, drips from 620 and melts at
660, about a minute from cold at 512 RPM. The aluminium runs down and sets as rough aluminium: a bracket melts off the
part it held and a casing off the part inside, which stay only if something else holds them, and a gearbox or machine
melts whole. A **Heat Sink** bolted on, base against it, carries it to about 384 RPM, and so does an encased fan
blowing straight onto it; a fan of 128 RPM or more blowing through the sink's fins carries it to aluminium's top speed
of 512. The server config's `frictionHeat` turns it off.

**Water cooling.** A **Water Cooler**, a heat sink with a copper water jacket (a heat sink, two fluid pipes and two
copper sheets), bolts on like a heat sink, onto an aluminium or titanium support or a Power Grid device, and cools faster
than anything else. Pipe water through it, in at any face but its base: it takes heat in step with how hot what it
cools is, using water only as it cools, and gives the water back as **warm water**, which has to be pumped away or it
stops. A **Radiator** (six aluminium sheets round three fluid pipes make two) chills any water into **cold water**,
slowly on its own and much faster with an encased fan blowing through it. Radiators side by side facing the same way
join into a panel that shares what it holds; pipes join it on its front or back, and a fan under each column cools that
whole column. Cold water takes heat twice as fast as water, and also comes from any snowy biome or anywhere else
cold enough to snow, by bucket, pump or hose pulley. Poured out, warm or cold water is plain water.

**Coolant.** Ethylene dichloride and caustic soda mixed over heat make **ethylene glycol** and salt water, and glycol
mixed with water makes bright green **Coolant**. In a water cooler it takes heat three times as fast as water and comes
out as **hot coolant**, which a radiator cools back. It never wears out, so it is worth keeping it going round a loop
of water coolers and radiators, where water is free to run through once and pour away.

**Titanium.** The top tier, in every part, as a casing and in every machine: it turns up to **2,048 RPM** under any
load, with no drag, and never weathers, but its supports make friction heat twice as fast as aluminium's, so
they need twice the cooling: left alone they hold about 128 RPM. A heat sink melts past 660 °C, so it helps titanium
only to about 225 RPM even with a fan; a Water Cooler holds it to about 700 RPM on water, 1,300 on cold water and 1,900
on coolant, and two coolers of coolant carry it to 2,048. Past **1,000 °C** it burns: it flares white for three
seconds, setting fire to anything that burns across the gap beside it, and crumbles into white **titanium dioxide**
powder (good for rutile again, or white dye). Heat leaves its colours on it, as on the real metal: past 400 °C what
holds a part turns **gold**, past 550 **purple** and past 700 **blue**, and keeps the deepest it reached; hydrochloric
acid strips it back to plain. **Titanium Plating** (four sheets make four) takes the same colours in a Furnace: gold at
Hot, purple at Searing, blue at Blazing.

**Making titanium.** Basalt crushed or milled is **black sand**, and Power Grid's electromagnet pulls the iron out of it,
leaving **rutile**. Salt water in a heated basin with nothing working it boils down to **salt**; melted in a crucible,
an Electrolyser splits it into **molten sodium** and chlorine (sodium meeting water goes off with a bang and a burst of
fire). Rutile and coal coke pressed with coal tar make **Rutile Briquettes**, and chlorine run over one in a Steel
Furnace at Blazing gives **titanium tetrachloride**, which fumes into white smoke and acid mist if it escapes. Stirred
with molten sodium in a steel vat it leaves **titanium sponge** and salt. The **Arc Furnace**, a Power Grid device,
strikes an arc between two Carbon Electrodes through the sponge in its copper hearth and melts it into **titanium
ingots**: a quarter-ohm load, 20 V for 1.6 kW, melting only from 400 W, and its hearth needs a water cooler.

**Villages.** Villages are abandoned: no villagers, zombie villagers or iron golems, and no progression loot in their
chests. They generate as ruins, with mossy stone, broken windows, burnt-out torches, cold campfires, cobwebs and weedy
fields; with Immersive Weathering, some houses have burned down to charred shells. Villagers cannot breed; the only
way to get one is to cure a zombie villager met out in the world.

**Enchanting and XP** are left vanilla for now, while they are redesigned together.

**Ponder.** Every new interaction has a Ponder scene (hold W over the item): furnace basics, heating from below,
fanning fires with bellows, bloom iron and anvils, blister steel, furnace-heated steam boilers and basins,
supports, speed controllers as supports, brackets, casings in every material, wood as a fuse, kinetic materials, steel parts, wooden parts in
every wood, gearbox materials, clutch and gearshift materials, chain drive materials, machine materials, drill bits, saw blades, the crucible, drawing off tar and wood spirit,
pressing Bakelite, the heat sink, the Electrolyser on a basin and on a vat, lighting a furnace with a Spark Plug, the
Steel Furnace, mixing in a vat, the Pressurizer, steam engines in every boiler metal, friction heat in aluminium, cooling with water and coolant, titanium's
heat, colours and burning, and the Arc Furnace. Every metal's machines and every wood's movers also get Create's
own scenes for them. Any scene can be paused, with the button beside Replay or the forward key (W), and a scene
opened by holding W starts playing.

**JEI.** A Furnace tab lists everything a furnace fires with the heat and time it needs, a Furnace Heat tab shows
which fuels and which blocks underneath give each heat level, a Striking Blooms tab shows the anvil recipe, and
Melting and Alloying tabs show what a crucible melts and makes. Settling and Electrolysis tabs show what settles out
and what an Electrolyser splits, with the charge each takes and its time at a given current. Fermenting, Steel
Furnace, Steel Furnace Fuel, Vat Mixing and Pressurizing tabs show what ferments, what the Steel Furnace works and how
many mB a tick of each fuel give each heat, what stirs together in a vat, and what the Pressurizer squeezes at what
speed and heat. Boiling and Arc Furnace tabs show what a heated basin boils down and what the Arc Furnace melts,
with the energy it takes, and a Casting tab every molten metal poured into a sand mold. Every one of these tabs is laid out like Create's basin pages, with the machine drawn at
work in the middle, over a campfire, lava or a blaze burner when it needs heat, and a bar giving the heat, time, speed,
charge or energy, with a slot on its end cycling through what heats it that hot. Every metal's machines join Create's tabs for their machine. Information pages explain the Water
Cooler, the Radiator and warm water, cold water, coolant and hot coolant. Vanilla's furnace, smoker, blast furnace and fuel tabs are hidden while they are
cold.

**Structural formulas.** Every compound's tooltip draws its structural formula, its atoms colour coded by element
(carbon grey, hydrogen white, oxygen red, chlorine green, sodium violet, and so on) and the formula written out
underneath: this mod's chemicals and their buckets, Create's limestone, and Minecraft's water, ice, snow, sugar,
quartz, clay, calcite, dripstone and bone meal. Polymers show their repeat unit, salts their ions, sugars their rings.
A material of one element (a metal's ingots, nuggets, sheets, blocks and melt, molten sodium, diamond) shows just its
symbol in its colour.
A resource pack can change them or add more under `assets/<namespace>/formulas`.

**Looks.** Brass parts are clean polished clockwork with no red: smooth bevelled plates with one fine inset line,
plain bushings where shafts come out, a slender 3 px arbor with no collars, a hoop across the large cog's spokes, a
fine chain, and brackets as clock bridges. The Brass Mechanical Pump has a shallower impeller under a handwheel on a
stem. This mod's ingots, nuggets and sheets are drawn in Create's own style, and the Electrolyser is a black Bakelite
head with tinned clamps and a thin red or blue band on each terminal. Titanium is brighter and polished, and keeps its shine under its temper colours. Steel is a dark gunmetal now, the weight of Create's metal girder and industrial iron rather than a grey
between iron and aluminium, and titanium's gold, purple and blue tempers are faint washes over its own pale grey
instead of full colours. Red mud bricks are vanilla's own mud bricks under a red tint, so they keep vanilla's brick
pattern and any resource pack that retextures mud bricks carries straight over to them. The Heat Sink is a small
finned plate, twelve pixels across and half a block tall, where it used to fill its face. Heat sinks, Water Coolers,
Radiators and the Arc Furnace are drawn from several textures each, so their fins, bolted ledges, water ports,
louvres, tube ends, hammered copper shell and sooty refractory lining all read close up.

All balance numbers are in the server config (`serverconfig/mcremix-server.toml` in each world); speed and load limits
and heat sources are block tags so packs can retune them.

## Building

Needs JDK 17 (Gradle downloads one through the foojay toolchain resolver if none is installed) and network access to:

- `services.gradle.org` and `plugins.gradle.org` (Gradle and its plugins)
- `maven.neoforged.net`, `maven.minecraftforge.net` (ModDevGradle and Forge)
- `piston-meta.mojang.com`, `piston-data.mojang.com`, `libraries.minecraft.net`, `resources.download.minecraft.net`
  (Minecraft itself)
- `maven.parchmentmc.org` (parameter names)
- `maven.createmod.net` (Create, Ponder, Flywheel), `maven.ithundxr.dev` (Registrate), `maven.blamejared.com` (JEI)
- `repo.maven.apache.org` (Maven Central), `api.foojay.io` (JDK download, only if no JDK 17 is installed)
- `api.modrinth.com` (Create: Power Grid and Architectury API, and the companion mods with `-Pcompanions`)

```
./gradlew build          # mod jar in build/libs/
./gradlew runClient      # dev client with Create, Ponder, Flywheel, Power Grid and JEI
./gradlew runServer
./gradlew runGameTestServer   # the GameTests in src/gametest; -Pcompanions also loads IW, WWOO and
                              # Better Combat (runClient takes -Pcompanions too)
```

If `create_version=6.0.8-+` in `gradle.properties` does not resolve, set it to `6.0.7-281`.

## Generated resources

Textures, models, blockstates, lang, loot tables, tags, recipes and Ponder schematics are written by
`tools/gen_assets.py` (needs Python 3 and Pillow). The script owns `src/main/resources/assets/mcremix` and
`src/main/resources/data` and wipes them each run, so change the script rather than the files. Ponder text is read
straight from the `*Scenes.java` classes, so after editing scene text, rerun it:

```
python3 -m pip install pillow
python3 tools/gen_assets.py
```

Most textures are drawn procedurally. The shaft and cog textures only reuse the shape of Create's UV layout
(`tools/uv_masks.json`) so they fit Create's models, which are referenced as parents, and the gearbox, clutch and
gearshift casings are drawn from scratch to fit Create's models of them. The brass, iron, glass, steel and aluminium
pipes, pumps and tanks, and the weathered copper and rusted iron ones, are Create's own copper textures recoloured: they are kept as palettes in
`tools/create_fluid_textures.json` (Create is MIT licensed).

## Layout

```
src/main/java/com/mcremix/
  heat/         HeatLevel scale, HeatSources (what counts as hot underneath), BellowsTarget
  kiln/         The Furnace (the kiln in code): block, block entity, firing recipe type, fuels, furnace lockout
  bellows/      Bellows block and block entity (what it blows into and why)
  item/         Coal coke, iron bloom (and anvil hammering), slag
  smithing/     Stone anvil
  machines/     Machine tiers: the copper, iron, steel and aluminium machines, casing swaps, fitting bits and blades
  drilling/     Drill bits and the tiered drill
  sawing/       Saw blades and the tiered saw
  movers/       Wooden bearings, pistons, poles, gantries and rope pulleys
  fluids/       Fluid heat and part ratings, overheating, brass, iron, glass, steel and aluminium pipes, pumps and
                tanks, copper patina and iron rust, rusted iron leaking, steel pressure boilers, molten metal, rough metal and spills, the chemicals (coal tar to
                hydrogen, glycol and coolant), salt water from the sea and cold water where it snows, the chlorine
                cloud, and warm water and hot coolant poured out turning to water and coolant
  crucible/     The crucible, its melting and alloying recipes, and sand molds
  electrolysis/ The Electrolyser and Carbon Electrodes, vats, settling and electrolysis recipes, and gases let loose
  heatsink/     The aluminium heat sink and its share of Power Grid's heat model
  cooling/      Water coolers, radiator panels and what each works with
  kinetics/     Shaft, cog, gearbox, clutch, gearshift and chain drive materials, supports, brackets and
                casings, speed/load limits, friction heat and melting aluminium, and how copper and iron weather,
                whichever mod adds them
  village/      Empty and ruined villages, no breeding, village loot filter
  compat/       Immersive Weathering soot, Power Grid heater temperatures (looked up at runtime, no compile dependency),
                JEI tabs (compiled against JEI's API, loaded only when JEI is installed)
  ponder/       Ponder scenes and plugin
  client/       Renderers, Flywheel visuals, partial models, bellows particles and sounds
  mixin/        Kinetic limits, cold furnaces, the heat basins read, fitted bits and blades, Create taking machine
                tiers and wooden movers for its own and drawing them, the drill and
                saw goggle lines, the random tick every block's weathering passes through, and the hooks into Create's
                pipes, tanks, boilers and whistles, and basins settling, tanks holding a vat, open pipe ends letting
                gas go, and Power Grid's fans cooling the support they blow on
src/gametest/   GameTests for the gameplay checklist (never packaged into the jar)
docs/           DESIGN.md (the plan), BUILD_NOTES.md (what to check on the first compile), TEST_REPORT.md
tools/          gen_assets.py, the UV masks it uses and Create's fluid textures it recolours
```

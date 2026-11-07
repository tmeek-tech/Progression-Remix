# Test reports

Newest first: [0.28.1](#0281-ponder-starts-playing-and-no-valve-lever), [0.28.0](#0280-tarans-six-changes), [0.27.0](#0270-stability-for-supports-strength-for-shafts-and-cogs), [0.26.0](#0260-tarans-nine-changes), [0.25.0](#0250-tarans-list-part-three), [0.24.0](#0240-tarans-list-part-two), [0.23.0](#0230-tarans-list-part-one), [0.22.1](#0221-rusted-iron-leaks-as-an-option-and-vat-outlets), [0.22.0](#0220-texture-changes), [0.21.0](#0210-the-fermenter-look), [0.20.0](#0200-pipe-and-tank-metals), [0.19.1](#0191-the-titanium-pressurizer), [0.19.0](#0190-titanium), [0.18.0](#0180-heat-and-cooling), [0.17.0](#0170-machine-tiers-fitted-bits-and-wooden-movers), [0.16.0](#0160-plastics-the-steel-furnace-and-pvc-pipes), [0.15.1](#0151-brackets-on-their-own), [0.15.0](#0150-friction-points-brackets-casings-and-brass), [0.14.0](#0140-power-grid-chemistry-and-aluminium), [0.13.0](#0130-heat-rated-pipes-pressure-boilers-and-the-crucible), [0.12.0](#0120-steel-the-steel-spring-and-every-wood), [0.11.1](#0111-rust-runs-down), [0.11.0](#0110-furnace-heated-basins-chain-drive-tiers-and-the-furnace), [0.8.2](#082-iron-rusts-only-while-wet), [0.8.1](#081-fast-varied-weathering),
[0.8.0](#080-slag-roads-kiln-boilers-ruined-villages-saw-blades-clutches-and-gearshifts),
[0.4.0](#040-bellows-gearbox-tiers-stone-anvil-jei-tabs-and-drill-bits), then [phase 1](#phase-1-gameplay-010).

## 0.28.1: Ponder starts playing, and no valve lever

This covers section 35 of [BUILD_NOTES.md](BUILD_NOTES.md): Taran's follow-up of 2026-11-04 to 0.28.0. A Ponder scene
opened by holding W starts playing (W pauses only when pressed afresh), and the valve lever under a vat's cone is gone.
Tested on 2026-11-04 against the same versions as 0.28.0.

**Result:** 0.28.1 compiled with no errors. Every GameTest passes in a fresh test world: 329 of 329 plain and 335 of 335
with `-Pcompanions` (on its second run; see below). A dev client shows both changes working. Not checked: the jar on a real Forge server.

| Check | Result | How |
| --- | --- | --- |
| Build | Pass | `./gradlew build`: `mcremix-1.20.1-0.28.1.jar`, 4,511,031 bytes, SHA-256 `d3257c45…a56b5e97`; `builds/mcremix-1.20.1-0.28.1.zip` holds the same jar. Its `mods.toml` says 0.28.1; no PNG in it carries a `caBX` chunk; the refmap maps `PonderUIMixin`'s `tick` to its SRG name, and `PonderPause.Button`'s `keyPressed` is reobfuscated to `m_7933_` |
| Production server | Not run | As for 0.20.0 to 0.28.0, running the downloaded Forge installer was not tried in this environment |
| GameTests, plain | Pass | 329 of 329 |
| GameTests, with companions | Pass | 335 of 335 on the second run, online so Gradle could fetch the companion mods from Modrinth. The first run failed one, `gasLeaksOutOfRustedTanks` (the chlorine leaked, but no cloud was within three blocks of the tank at tick 80); nothing it touches changed in 0.28.1, and it passed on the next run and in every run of 0.28.0, so it reads as a flake |
| Ponder opened on W | Pass | In the client, with key repeat on (a key held for two seconds typed a row of letters into chat), W held over the Pressurizer in the creative inventory opened its scene, which played on with W still held and after it was let go, its button showing the pause sign. W pressed again paused it, held for 3.5 seconds the scene stayed paused (the scene identical in screenshots 1.5 seconds apart), and W pressed after letting go played it again |
| No valve lever | Pass | In the client, a copper vat with a chest beside it has only the outlet pipe to the chest under its cone, nothing on the other sides; one with a chest and a barrel has a pipe to each, and one with nothing beside it has its cone closed |
| Dev client | Pass | Nothing from the mod in the log but the usual dev refmap warning. Screenshots in `builds/screenshots/0.28.1` |

### Known gaps

- The jar has not been started on a real Forge server.
- `gasLeaksOutOfRustedTanks` failed once with companions and passed the next time; why it can miss its cloud was not looked into.

## 0.28.0: Taran's six changes

This covers section 34 of [BUILD_NOTES.md](BUILD_NOTES.md): Taran's six changes of 2026-11-02 and the two cards, on top
of 0.27.0. Steel rusts wherever iron does but only to its first stage; vats pipe their outlet to every inventory they
fill and funnels on their scaffolding pull items out; titanium Pressurizers keep their colour until heated; Pressurizers
look right in the hand; metal casting has a JEI tab; and Ponder scenes can be paused. Tested on 2026-11-02 against the
same versions as 0.27.0.

**Result:** 0.28.0 compiled with no errors. Every GameTest passes in a fresh test world: 329 of 329 plain and 335 of 335
with `-Pcompanions`. A dev client shows every change working. Not checked: the jar on a real Forge server.

| Check | Result | How |
| --- | --- | --- |
| Build | Pass | `./gradlew build`: `mcremix-1.20.1-0.28.0.jar`, 4,509,185 bytes, SHA-256 `19b9f997…d970877f`; `builds/mcremix-1.20.1-0.28.0.zip` holds the same jar. Its `mods.toml` says 0.28.0; no PNG in it carries a `caBX` chunk; the refmap maps `ClientLevelMixin` and `PonderUIMixin` to their SRG names |
| Production server | Not run | As for 0.20.0 to 0.27.0, running the downloaded Forge installer was not tried in this environment |
| GameTests, plain | Pass | 329 of 329 |
| GameTests, with companions | Pass | 335 of 335, run online so Gradle could fetch the companion mods from Modrinth |
| Steel to its first stage | Pass | `steelRustsOnlyToItsFirstStage` (a steel shaft, gearbox, large cog, pipe, pump, tank, electric pump and press beside water stop at their first stage after 600 random ticks and stop ticking; old rusted and weathered steel, waxed or not, comes back to it in one random tick), the steel fan in `MachineTierTests` and `PatinaTests`; in the client exposed steel pipes, a pump, tanks, a vat, the electric pump, a boiler's gauge and dial, a gearbox, shaft and cogs show rust spots beside clean ones, and the creative tab and JEI list four steel shafts (plain, exposed and their waxed forms), with nothing in the log |
| Titanium Pressurizer colours | Pass | `aTitaniumVesselOnlyTakesItsPressurizersColour` |
| Pressurizers in the hand | Pass | In the client, the steel and titanium Pressurizers held in first and third person show their own vessel, bands, valve and dial, with no scraps of other textures |
| Vats and inventories | Pass | `aWideVatFillsAnInventoryBesideAnyBlockOfItsScaffolding`, `everyPlaceAVatPutsItemsHasItsOwnBit`, and the settling and dough tests; in the client, seen from below in spectator, a 2-wide vat pipes its outlet to a chest beside its north-east block and down into a barrel under its south-west block, and a 3-wide vat to a chest by its north-east corner and to a funnel; taking the chest away takes its pipe away across the vat at once |
| Funnels on the scaffolding | Pass | `aFunnelOnAVatsScaffoldingPullsOutWhatItMakes`; in the client a funnel on a 3-wide vat of bauxite liquor dropped eleven aluminium hydroxide into a hopper under it, with the outlet piped to it |
| Casting in JEI | Pass | In the client, uses of the Sand Ingot Mold open a Casting tab of four pages: each metal into the mold on a depot under a tipped crucible, the casting and sand out, "Sets in 2s" |
| Ponder pause | Pass | In the client the button sits left of Replay with W on it; paused, two screenshots two seconds apart differ only in the buttons' flashing frames, and W plays it again; the vat scene shows its funnel |
| Dev client | Pass | Nothing from the mod in the log but the usual dev refmap warning. Screenshots in `builds/screenshots/0.28.0` |

### Known gaps

- The jar has not been started on a real Forge server.
- What Taran meant by vats connecting to inventories being bugged was not answered; the fix follows Claude's reading (the outlet's pipes), and items already came out on the server.

## 0.27.0: Stability for supports, Strength for shafts and cogs

This covers section 33 of [BUILD_NOTES.md](BUILD_NOTES.md): Taran's ask of 2026-10-29 and its three cards, on top of
0.26.0. Supports (brackets, casings, gearboxes, clutches, gearshifts, chain drives and machines) have a Stability, their
top speed, which a part in a bracket or casing takes alone; shafts and cogs have a Strength, their load, and are the only
things a load breaks; and parts reach twice as far from a support. Tested on 2026-10-29 against the same versions as
0.26.0.

**Result:** 0.27.0 compiled with no errors. Every GameTest passes in a fresh test world: 324 of 324 plain and 330 of 330
with `-Pcompanions`. A dev client shows the tooltips, goggles, reach and Ponder scenes working. Not checked: the jar on
a real Forge server.

| Check | Result | How |
| --- | --- | --- |
| Build | Pass | `./gradlew build`: `mcremix-1.20.1-0.27.0.jar`, 4,472,429 bytes, SHA-256 `8d5b6ba4…2fe563f6`; `builds/mcremix-1.20.1-0.27.0.zip` holds the same jar. Its `mods.toml` says 0.27.0; no PNG in it carries a `caBX` chunk |
| Production server | Not run | As for 0.20.0 to 0.26.0, running the downloaded Forge installer was not tried in this environment |
| GameTests, plain | Pass | 324 of 324 |
| GameTests, with companions | Pass | 330 of 330, run online so Gradle could fetch the companion mods from Modrinth |
| A held part takes its support's top speed | Pass | `encasedPartsTakeTheirCasingsTopSpeed` (a casing sets the speed whatever the part is made of, and past it the casing gives way and leaves the part) and `frictionPointsGrindAboveTheirSpeedLimit` (Create's andesite shaft in a copper bracket carries 192 RPM, past andesite's 128; an andesite bracket grinds off a copper shaft); in the client goggles on a copper shaft in a steel bracket read "Stability: up to 512 RPM", where until 0.26.0 it would have been copper's 256, and in an oak bracket 64 |
| Only shafts and cogs break from load | Pass | `onlyShaftsAndCogsGiveWayUnderLoad` (a copper bracket or casing on an iron shaft carries a load past copper's; copper inside an iron casing breaks), `woodenGearboxesTakeAnyLoad`, `woodenClutchesAndGearshiftsTakeAnyLoad` (they turn on under a load past wood's; a birch shaft after the gearbox gives way), and the machine, mover and vat mixer tests (each support takes any load) |
| Reach twice as far | Pass | `barePartsGoOnlyWhereSomethingHoldsThem` (copper four out, birch and brass two, steel six, and Create's placement helper stops at the end of andesite's four); in the client a steel line runs six shafts from its gearbox, and a seventh is refused with "Needs a support within 6 blocks along its axis" |
| Tooltips | Pass | `tooltipsRateEveryMaterial` (each level's words in the lang file; supports, casings and the crushing wheel rated for Stability alone, shafts and cogs for Strength alone); in the client a copper shaft reads Strength: Light-duty, the brass shaft Delicate and Create's shaft Unbreakable, the copper gearbox Stability: Stable, the oak bracket Wobbly, the steel casing Very Stable, the titanium gearbox Rock-solid, and Create's press and crushing wheel Steady under their stress impact; with goggles on, the RPM or su instead. The shift summaries of a shaft, gearbox and bracket say which limit is whose |
| Goggles | Pass | In the client: a bare copper shaft shows how far it is from its support, that it reaches 4 blocks, that it turns as fast as its supports allow, and its Strength; the copper gearbox its Stability alone; bracketed shafts the bracket's Stability and the shaft's Strength |
| Ponder | Pass | In the client the supports scene on its 9 by 9 plate (copper four, oak two, steel six, and every shaft dropping once the gearbox goes), the material tiers scene (the oak brackets grind off and the oak shafts drop), the brackets scene (the line stays when the oak bracket grinds off, held by the copper bracket) and the casings scene, each with the new words |
| Dev client | Pass | Nothing from the mod in the log but the usual dev refmap warning. Screenshots in `builds/screenshots/0.27.0` |

### Known gaps

- The jar has not been started on a real Forge server.

## 0.26.0: Taran's nine changes

This covers section 32 of [BUILD_NOTES.md](BUILD_NOTES.md): Taran's nine changes of 2026-10-27 and the tenth, cogs
right-clicked into casings, on top of 0.25.0. Element symbols on one-element materials; the Pressurizer driven from
either side, square, piped at its vessel's sides and recessed round its shaft; cleaner, thicker brass parts and the clock
bridge bracket; the Electrolyser; ingots and sheets in Create's style; the Brass Mechanical Pump's handwheel in place of
the Brass Electric Pump; boiler gauges in their tanks' metal; engines driving any shaft; and steel boilers on piped
steam alone. Tested on 2026-10-27 against the same versions as 0.25.0.

**Result:** 0.26.0 compiled with no errors. Every GameTest passes in a fresh test world: 324 of 324 plain and 330 of 330
with `-Pcompanions`. A dev client shows every change working, after fixes for the two things it turned up. Not checked:
the jar on a real Forge server.

| Check | Result | How |
| --- | --- | --- |
| Build | Pass | `./gradlew build`: `mcremix-1.20.1-0.26.0.jar`, 4,470,592 bytes, SHA-256 `f6778b62…8d605a1b`; `builds/mcremix-1.20.1-0.26.0.zip` holds the same jar. Its `mods.toml` says 0.26.0; no PNG in it carries a `caBX` chunk |
| Production server | Not run | As for 0.20.0 to 0.25.0, running the downloaded Forge installer was not tried in this environment |
| GameTests, plain | Pass | 324 of 324 |
| GameTests, with companions | Pass | 330 of 330, run online so Gradle could fetch the companion mods from Modrinth |
| Element symbols | Pass | `compoundsShowTheirFormulas` (an iron ingot shows Fe alone, a gold nugget Au, a diamond C, an aluminium sheet Al, a titanium ingot Ti, a zinc nugget Zn and a bucket of molten sodium Na); in the client the tooltips of iron, copper, gold, zinc, aluminium and titanium show the symbol alone in its colour |
| The Pressurizer | Pass | `itsShaftGoesInAtEitherSide`, `onlyItsVesselsFourSidesTakePipes`, `oneFrom025TurnsASideToItsShaft` and the rest of `PressurizerTests`; in the client steel and titanium Pressurizers, square, are turned through a side, the shaft's ends lit in the recesses with no flicker, and a pipe at the vessel's side is met by a nozzle; the Ponder scene and the JEI page draw the shaft through its sides |
| Brass parts | Pass | No test reaches textures, so in the client: the brass shaft, cogs, gearbox, chain drive and casing in smooth polished brass with no red, the shaft thicker and without rings, and the clock bridge bracket on a shaft and on a cog |
| The Electrolyser | Pass | In the client, on basins: the black Bakelite head, one thin red and one thin blue band, tinned steel clamps |
| Ingots and sheets | Pass | In the client beside Create's own: steel, aluminium, titanium, blister steel, the PVC sheet and Bakelite drawn as Create draws its ingots, nuggets and sheets |
| The Brass Mechanical Pump | Pass | `brassPumpHandwheelsSetTheMostTheyLetThrough` (a line carries no more than the lowest handwheel in it lets through), `theBrassElectricPumpIsPowerGridsNow`; in the client the handwheel on its stem, and holding right-click on it opens Create's value board, 1 to 24 mB a tick |
| Steam engines and boilers | Pass | `enginesDriveAnyShaftButHoldNone` (a brass shaft in its bracket and a steel one a gearbox holds turn at Create's speed and stay themselves; Create's helper is refused where only an engine would hold the shaft, and an engine's shaft drops once its gearbox goes), `steelBoilersRunOnPipedSteamAlone` and the engine tests from 0.24.0; in the client gauges in copper, brass, iron at its rust stages, steel and weathered copper, engines turning shafts of other metals, and goggles on a steel boiler reading Size and Steam |
| Cogs into casings | Pass | `cogsGoIntoPlacedCasings` (from the top, a side and the front; a large cog; Create's cog into Create's brass casing; in creative nothing is used and no casing comes out; sneaking puts the cog beside the casing; a deployer holding a cog puts it in); in the client cogs right-clicked into casings of several metals |
| Dev client | Pass | It turned up two things, both fixed and looked at again: the shaft's ends in the Pressurizer's sides were drawn black (the block now lets light in), and every metal's gauge kept Create's copper needle post and bands (now recoloured too). Nothing from the mod in the log but the usual dev refmap warning. Screenshots in `builds/screenshots/0.26.0` |

### Known gaps

- The jar has not been started on a real Forge server.
- A Pressurizer from an older save relights its block when it upgrades, so its shaft's ends are not left dark; that
  was not seen in game, since the dev world's Pressurizers were placed again after the fix.

## 0.25.0: Taran's list, part three

This covers section 31 of [BUILD_NOTES.md](BUILD_NOTES.md), the last of three jars for Taran's list: structural
formulas drawn on the tooltips of compounds, their elements colour coded, and the JEI pages for this mod's fourteen
recipe types laid out and drawn as Create's, on top of 0.24.0. Tested on 2026-10-27 against the same versions as
0.24.0.

**Result:** 0.25.0 compiled with no errors. Every GameTest passes in a fresh test world: 318 of 318 plain and 324 of 324
with `-Pcompanions`. A dev client shows every change working. Not checked: the jar on a real Forge server.

| Check | Result | How |
| --- | --- | --- |
| Build | Pass | `./gradlew build`: `mcremix-1.20.1-0.25.0.jar`, 4,420,664 bytes, SHA-256 `84ac1278…c5689661`; `builds/mcremix-1.20.1-0.25.0.zip` holds the same jar. Its `mods.toml` says 0.25.0; no PNG in it carries a `caBX` chunk |
| Production server | Not run | As for 0.20.0 to 0.24.0, running the downloaded Forge installer was not tried in this environment |
| GameTests, plain | Pass | 318 of 318 |
| GameTests, with companions | Pass | 324 of 324, run online so Gradle could fetch the companion mods from Modrinth |
| Structural formulas | Pass | `everyDrawingHoldsWhatItsFormulaSays` (each drawing's atoms, hydrogens, counts and shares add up to its written formula), `writtenFormulasCountTheirAtoms`, `compoundsShowTheirFormulas` (the items, fluids and buckets each formula lists find it); in the client the tooltips of water, sugar, salt, Bakelite, polyethylene pellets, PVC sheet, bone meal, clay and quartz draw their structures, atoms in their colours, and JEI's tooltips for this mod's fluids draw theirs |
| JEI pages | Pass | No GameTest reaches JEI's screens, so each of the fourteen tabs was opened in the dev client: Melting and Alloying (the crucible over lava or a blaze burner, its pot full of the metal), Furnace and Furnace Heat (the lit Furnace, over a campfire for Warm and lava for Hot, captions over its slots), Boiling, Settling and Fermenting (Create's basin, its fluid and floating items showing over the rim), Electrolysis (an Electrolyser on the basin's side), Steel Furnace and its fuel table, Striking Blooms (a bloom jumping on the Stone Anvil), Vat Mixing (a steel vat on its legs, the mixer's cog turning on its front), Pressurizing (the needle at each recipe's speed, a heated one a block higher over its fire) and Arc Furnace. Every bar's words show whole, and its tooltip and the heat-source slot's notes read as meant |
| Dev client log | Pass | Nothing from the mod in the log but the usual dev refmap warning. Screenshots in `builds/screenshots/0.25.0` |

### Known gaps

- The jar has not been started on a real Forge server.

## 0.24.0: Taran's list, part two

This covers section 30 of [BUILD_NOTES.md](BUILD_NOTES.md), the second of three jars for Taran's list: the Pressurizer
in two blocks with a working pressure dial, steam engines in brass, iron and steel that go only on their own metal's
boilers, and electric pumps in brass, iron, steel and aluminium, on top of 0.23.0. Tested on 2026-10-26 against the
same versions as 0.23.0.

**Result:** 0.24.0 compiled with no errors. Every GameTest passes in a fresh test world: 315 of 315 plain and 321 of 321
with `-Pcompanions`. A dev client shows every change working. Not checked: the jar on a real Forge server.

| Check | Result | How |
| --- | --- | --- |
| Build | Pass | `./gradlew build`: `mcremix-1.20.1-0.24.0.jar`, 4,354,220 bytes, SHA-256 `1036557f…70ee48a7`; `builds/mcremix-1.20.1-0.24.0.zip` holds the same jar. Its `mods.toml` says 0.24.0; no PNG in it carries a `caBX` chunk |
| Production server | Not run | As for 0.20.0 to 0.23.0, running the downloaded Forge installer was not tried in this environment |
| GameTests, plain | Pass | 315 of 315 |
| GameTests, with companions | Pass | 321 of 321, run online so Gradle could fetch the companion mods from Modrinth |
| The Pressurizer | Pass | `placingOnePutsItsVesselOnTop`, `itsTwoHalvesGoTogether`, `onlyItsSidesAndUndersideTakePipes`, `itsShaftIsAtItsBackOnly`, `theDialReadsSpeedAsAShareOfTopSpeed`, `oneFromBefore024TurnsToItsDrive`; in the client a steel Pressurizer driven at 256 RPM from its back points its needle just short of straight up, half its top speed, beside an idle titanium one at rest, each under its vessel; the Ponder scene plays with the vessel and the dial |
| Steam engines | Pass | `eachEngineRunsOnItsOwnMetalsBoilerAlone`, `createsEngineOnAnotherMetalsBoilerBecomesThatMetals`, `everyMetalsEngineGivesWhatCreatesGives`, `anEngineGoesOnlyAgainstItsOwnMetalsTank`, `enginesAreMadeAsCreatesIsOnTheirMetalsBlock`; in the client copper, brass, iron and steel engines on their boilers beside an aluminium tank, a brass engine refused on an iron tank with "Brass Steam Engine goes only on Brass Fluid Tank boilers" over the hotbar, goggles on the iron boiler counting its one engine; the new Ponder scene plays and its engines turn |
| Electric pumps | Pass | `eachMetalsElectricPumpTopsOutAtItsMetalsSpeed` (a creative voltage source wired to a device connector on each pump: brass tops out at 96 RPM while Power Grid's own goes past it on the same volts, iron at 256, steel and aluminium at 512), `anElectricPumpPumpsThroughPipesOfAnyMetal`, `electricPumpsAreAMotorOnAPipe`, `electricPumpsHoldWhatTheirMetalHolds`, and the iron one in `ironPartsRustOnlyWhileWet`; in the client each pump in its metal and the iron one at every rust stage; their tooltips read Power Grid's maximum voltage line (147, 240 and 339 V) as Power Grid's own pump reads 240 V |
| Dev client log | Pass | Nothing from the mod in the log but the usual dev refmap warning. Screenshots in `builds/screenshots/0.24.0` |

### Known gaps

- The jar has not been started on a real Forge server.

## 0.23.0: Taran's list, part one

This covers section 29 of [BUILD_NOTES.md](BUILD_NOTES.md), the first of three jars for Taran's list of nineteen:
friction points renamed supports, the speed controller's top slot, Stability and Max Speed tooltips, no brass clutch,
gearshift or chain gearshift and crafters only in brass, Iron Casing, shinier brass and titanium, weathering to at least
the second-to-last stage, copper weathering on coolers, radiators and arc furnaces, pipes that join only their own metal,
Electrolysers on the sides of basins and vat mixers turned by a cog, on top of 0.22.1. Tested on 2026-10-26 against the
same versions as 0.22.1.

**Result:** 0.23.0 compiled with no errors. Every GameTest passes in a fresh test world: 300 of 300 plain and 306 of 306
with `-Pcompanions`. A dev client shows every change working. Not checked: the jar on a real Forge server.

| Check | Result | How |
| --- | --- | --- |
| Build | Pass | `./gradlew build`: `mcremix-1.20.1-0.23.0.jar`, 4,275,051 bytes, SHA-256 `086c273b…3b897a41`; `builds/mcremix-1.20.1-0.23.0.zip` holds the same jar. Its `mods.toml` says 0.23.0; no PNG in it carries a `caBX` chunk. The jar had to be built online once, for NeoForged's AutoRenamingTool, which this environment had not cached |
| Production server | Not run | Running the downloaded Forge installer was refused in this environment's predecessors (0.20.0 to 0.22.1), so it was not tried |
| GameTests, plain | Pass | 300 of 300. One earlier run failed `basinsTakeElectrolysersOnTheirSide`: Create takes a fake player's click on a basin's side as a deployer setting its filter, so the test now places its Electrolyser straight from the item (BUILD_NOTES section 29) |
| GameTests, with companions | Pass | 306 of 306, run online so Gradle could fetch the companion mods from Modrinth |
| Supports and the speed controller | Pass | `speedControllersHoldTheCogOnTop`; in the client, goggles on a shaft two blocks past a controller's cog say it is held by a support two blocks along; the new Ponder scene plays |
| Tooltips | Pass | `tooltipsRateEveryMaterial`; in the client, brass reads Delicate and Slow, titanium Unbreakable and Ludicrous, Create's shaft Unbreakable and Moderate, oak Flimsy and Sluggish, the copper press Light-duty and Fast under Create's stress line, aluminium Sturdy and Very Fast, the crushing wheel Unbreakable and Moderate, each with Create's bars in its level's colour; with goggles on, 3,072 su and 96 RPM and so on |
| Brass, crafters and Iron Casing | Pass | The machine tier, gearbox, clutch and chain drive tests pass with Iron Casing in place of Industrial Iron and no brass clutch; the client draws Iron Casing, the iron gearbox, encased shaft and press |
| Weathering | Pass | The weathering tests, changed for the later final stages, and `coolersRadiatorsAndArcFurnacesWeather`; in the client a water cooler, radiator and arc furnace at each stage show their patina |
| Pipes | Pass | `pipesJoinOnlyTheirOwnMetal`, `pvcJoinsOnlyPvcAndFittings`; in the client a row of copper, iron, brass, a pump, steel, glass and aluminium pipes ends in a rim at every change of metal and joins the pump on both sides |
| Electrolysers | Pass | `basinsTakeElectrolysersOnTheirSide`; in the client two Electrolysers on a basin's sides show the electrodes fitted (both on one, the anode alone on the other); the basin Ponder scene plays with it on the side |
| Vat mixers | Pass | `aVatMixerTurnsByItsCogNotAShaft`, `aVatMixerBreaksPastItsMetalsTopSpeed`; in the client copper, steel and iron vats each take a mixer drawn as a plate of their tank with a cog in its metal, turned by a cog beside it; the vat mixer Ponder scene plays |
| Looks | Pass | In the client: the brass gearbox, chain drive, bracket, shaft and cogs (ruby jewels, rosettes, the large cog's hoop) and titanium plain and in its three tints beside steel and aluminium. Screenshots in `builds/screenshots/0.23.0` |
| Dev client log | Pass | Nothing from the mod in the log but the usual dev refmap warning |

### Known gaps

- The jar has not been started on a real Forge server.
- In the dev client, holding a shaft while looking at a shaft line draws Create's placement ghost as a long translucent
  band; Create's own shaft does the same, so it is Create's ghost under software rendering, not this version.

## 0.22.1: Rusted iron leaks as an option, and vat outlets

This covers section 28 of [BUILD_NOTES.md](BUILD_NOTES.md): rusted-through iron only drips unless the server config's
`rustedIronLeaks` is on (off by default), and a vat's outlet is drawn only toward inventories it empties into, on top of
0.22.0. Tested on 2026-10-26 against the same versions as 0.22.0.

**Result:** 0.22.1 compiled with no errors. Every GameTest passes in a fresh test world: 293 of 293 plain and 299 of 299
with `-Pcompanions`. A dev client draws the outlets. Not checked: the jar on a real Forge server.

| Check | Result | How |
| --- | --- | --- |
| Build | Pass | `./gradlew build`: `mcremix-1.20.1-0.22.1.jar`, 4,361,661 bytes, SHA-256 `c5927afc…bfb098f3`; `builds/mcremix-1.20.1-0.22.1.zip` holds the same jar. Its `mods.toml` says 0.22.1; no GameTest classes |
| Production server | Not run | Running the downloaded Forge installer was refused in this environment, as for 0.20.0 and 0.21.0 |
| GameTests, plain | Pass | 293 of 293. The first plain run stopped at start-up with Forge's "Failed to register some entries", the dev-only flake BUILD_NOTES knows; the next run started normally |
| GameTests, with companions | Pass | 299 of 299 |
| Leaks | Pass | `rustedIronOnlyDripsByDefault` (a rusted iron tank of water keeps all 4,000 mB for 110 ticks with the option off); `rustedIronTanksLeak`, `gasLeaksOutOfRustedTanks` and `rustedIronPipesLeak` pass in their own batch with it on |
| Dev client | Pass | Under Xvfb: a vat with nothing round it has a closed cone and no outlet; outlets run down into a chest and a hopper and sideways to chests and a barrel on vats one, two and three across; a chest placed beside a vat brings its pipe and breaking it takes the pipe away |

### Known gaps

- The jar has not been started on a real Forge server.
- An inventory beside a corner of a vat two or three across gets no pipe drawn, though the vat still empties into it.

## 0.22.0: Texture changes

This covers section 27 of [BUILD_NOTES.md](BUILD_NOTES.md): red mud bricks drawn as vanilla's mud bricks under a red
tint, steel a dark gunmetal, titanium's temper colours at two fifths of their old strength, a small finned heat sink,
and more textures for the heat sink, Water Cooler, Radiator and Arc Furnace, on top of 0.21.0. Tested on 2026-10-26
against the same versions as 0.21.0 (Forge 47.4.6, Create 6.0.8, Power Grid 0.6.2 and Architectury 9.2.14).

**Result:** 0.22.0 compiled with no errors. Every GameTest passes in a fresh test world: 292 of 292 plain and 298 of
298 with `-Pcompanions`. A dev client draws every changed block and item beside what it is meant to match. Not
checked: the jar on a real Forge server (see below).

| Check | Result | How |
| --- | --- | --- |
| Build | Pass | `./gradlew build`: `mcremix-1.20.1-0.22.0.jar`, 4,359,946 bytes, SHA-256 `af76f012…0b87bce`; `builds/mcremix-1.20.1-0.22.0.zip` holds the same jar. Its `mods.toml` says 0.22.0; it has no GameTest classes, no access transformer and no `caBX` chunks in its 1,324 PNGs, and no `red_mud_bricks.png` (the blocks draw vanilla's) |
| Production server | Not run | Running the downloaded Forge installer was refused in this environment, as for 0.20.0 and 0.21.0. 0.22.0 adds no mixin; its only new code is two client colour handlers and the heat sink's shape |
| GameTests, plain | Pass | 292 of 292, on the final code |
| GameTests, with companions | Pass | 298 of 298, on the final code. The same changes on top of 0.20.0 passed 291 of 291 and 297 of 297 before the rebase |
| Rebase | Pass | The project folder checked as exactly 0.21.0 (with `caBX` stripped, 32 files changed and 2 deleted from 0.20.0, as its thread reported, and every other file byte for byte the same). Only the docs conflicted; the regenerated assets are 0.22.0's own changes plus `steel_fluid_tank_vat.png`, 0.21.0's new steel vat sheet, which follows the darker steel |
| Dev client | Pass | In the dev client under Xvfb, from a showcase datapack: steel parts and machines beside `create:industrial_iron_block` at both ends of the row, matching it in weight; titanium casing, gearboxes and plating plain and in each temper beside each other; red mud bricks, stairs, slabs and walls beside vanilla's mud bricks, with the same brick courses in red, in the world and as items; heat sinks on top and side of an aluminium gearbox, a Water Cooler, radiators along both axes and two Arc Furnaces, each close up; steel vats one and two across in 0.21.0's fermenter look beside a copper and an iron one, all in their own metal; the steel ingot, sheet and blister steel in the hotbar. The dev world, made on these changes over 0.20.0, opens on 0.22.0 with nothing missing. Nothing from this mod in the log's warnings or errors but the dev-only refmap notice |

### Known gaps

- The jar has not been started on a real Forge server (above).
- Only looked at with the game's own renderer, not with Embeddium or Oculus. Both honour vanilla's block colour
  handlers, which the red mud bricks use, but that was not tried.
- Not tried with a resource pack that retextures mud bricks, though the red mud bricks are meant to follow one.

## 0.21.0: The fermenter look

This covers section 26 of [BUILD_NOTES.md](BUILD_NOTES.md): a standing vat drawn like a brewery's conical fermenter in
its tanks' own metal, with its scaffolding as a cone on braced legs, a manway door on each side of its lowest layer
where nothing is fitted, and its windows shut, on top of 0.20.0. Tested on 2026-10-26 against the same versions as
0.20.0 (Forge 47.4.6, Create 6.0.8, Power Grid 0.6.2 and Architectury 9.2.14).

**Result:** 0.21.0 compiled with no errors. Every GameTest passes in a fresh test world: 292 of 292 plain and 298 of
298 with `-Pcompanions`. A dev client draws vats of every width, metal, patina and rust stage, leaves a door off behind
a vat mixer, breaks a vat up and forms it again, and plays both vat Ponder scenes. Not checked: the jar on a real Forge
server (see below).

| Check | Result | How |
| --- | --- | --- |
| Build | Pass | `./gradlew build`: `mcremix-1.20.1-0.21.0.jar`, 4,356,462 bytes, SHA-256 `9ea54065…6401fbab`; `builds/mcremix-1.20.1-0.21.0.zip` holds the same jar. Its `mods.toml` says 0.21.0; it has no GameTest classes, no access transformer and no `caBX` chunks, and neither of the two classes 0.21.0 removes |
| Production server | Not run | Running the downloaded Forge installer was refused in this environment, as for 0.20.0. Both new mixin hooks target Create (`onWrenched` and `invalidate`, with `remap = false`), so the dev runs cover them |
| GameTests, plain | Pass | 292 of 292, run again on the final code |
| GameTests, with companions | Pass | 298 of 298, run before the last two client-only changes (the doors' gasket and the stray texture fix), which a server never loads |
| Windows | Pass | `aStandingVatKeepsItsWindowsShut` (a small vat's tank shuts its windows as it forms, a wrench gives `PASS` and leaves them shut, and taken off its scaffolding they open again and a wrench shuts them as on any tank) |
| Dev client | Pass | In the dev client under Xvfb: copper vats one, two and three across, each with a cone, legs, an outlet with its valve and a door on each side, and nothing stray under the wider ones (a bug found here and fixed, BUILD_NOTES section 26); copper vats in each stage of patina, iron vats in each stage of rust, and brass, steel and aluminium vats, each in its own metal; a vat mixer on a side with no door behind it; a block of scaffolding taken out (the vat breaks up, its windows open and the scaffolding is drawn as itself) and put back (it forms again and shuts them). `/ponder mcremix:electrolyser` and `/ponder mcremix:ferric_chloride_bucket` play the vat and vat mixer scenes with the vat formed and the goggles text. Nothing from this mod in the log's warnings or errors |

### Known gaps

- The jar has not been started on a real Forge server (above).
- Only looked at with the game's own renderer, not with Embeddium or Oculus, which build chunks their own way.
- A vat standing across a chunk border was not tried in game.

## 0.20.0: Pipe and tank metals

This covers section 25 of [BUILD_NOTES.md](BUILD_NOTES.md): wooden pipes, pumps and tanks, Create's mixer's vat form,
the Steel Spring and slag roads taken out; brass, iron and aluminium pipes, pumps and tanks added, with iron and
aluminium vat mixers and recipes for every pipe, pump and tank, on top of 0.19.1. Tested on 2026-10-26 against the
same versions as 0.19.1 (Forge 47.4.6, Create 6.0.8, Power Grid 0.6.2 and Architectury 9.2.14).

**Result:** 0.20.0 compiled with no errors. Every GameTest passes in a fresh test world: 291 of 291 plain and 297 of 297
with `-Pcompanions`. A dev client draws every new pipe, pump and tank, each stage of iron's rust and a rusted tank
dripping, and plays the changed vat mixer scene. Not checked: the jar on a real Forge server (see below).

| Check | Result | How |
| --- | --- | --- |
| Build | Pass | `./gradlew build`: `mcremix-1.20.1-0.20.0.jar`, 4,328,707 bytes, SHA-256 `62d46041…6bfa16c21`; `builds/mcremix-1.20.1-0.20.0.zip` holds the same jar. Its `mods.toml` says 0.20.0; it has no GameTest classes and no access transformer. Its textures carry no `caBX` chunks (BUILD_NOTES section 25); with them the jar was 6.2 MB |
| Production server | Not run | Running the downloaded Forge installer was refused in this environment, so no server was set up. No mixin in 0.20.0 needs remapping (all of the new and changed ones target Create with `remap = false`), so the dev runs cover them |
| GameTests, plain | Pass | 291 of 291. The first run failed four: a copper vat mixer meant to break at 512 RPM turned at 256 (a creative motor turns no faster; it now goes through a steel large cog), the iron pipe's upright recipe made Create's chute (and the iron tank's would have made an item vault; both recipes changed), a scraped tank kept leaking for up to a second (fixed: a block stops leaking as soon as it is scraped, cleaned or waxed), and lava beside a melted pipe washed away its one layer of rough aluminium (the test's lava is now under the pipe) |
| GameTests, with companions | Pass | 297 of 297 |
| Iron rust and leaks | Pass | `ironPartsRustOnlyWhileWet` (a pipe, pump and tank beside water rust to their own final stage over 300 random ticks, dry ones stay bright), `rustedIronTanksLeak` (one rusted block loses 80 to 112 mB in 110 ticks, two lose twice that, a waxed one and one an axe scraped back lose nothing), `gasLeaksOutOfRustedTanks` (chlorine leaking from a rusted tank hangs beside it as a cloud), `rustedIronPipesLeak` (a rusted pump and pipes lose over 150 mB in 240 ticks of pumping, a waxed line nothing) |
| Aluminium | Pass | `aluminiumTanksMeltWithLavaButIronHoldsIt`, `lavaMeltsAluminiumPipesButNotIron`, `aluminiumMeltsBesideLava` (a pipe over lava and a tank of water beside it melt into rough aluminium, an iron pipe beside lava stays) |
| Brass | Pass | `brassPumpsPumpAndSnapPastNinetySix`, `brassPipesCarryHalfAsMuch` (in 280 ticks a line with a brass pipe moves between a quarter and two thirds of what an all-copper line does) |
| Boilers | Pass | `ironAndBrassTanksAreBoilersAndAluminiumNever`, `steelBoilersAreCompact` |
| Vat mixers | Pass | `aMixerUsedOnAVatGoesOnAsItsVatForm`, `createsMixerHasNoVatForm`, `noMixerFitsABrassVat`, `eachMixerFitsAVatOfItsOwnMetal`, `vatMixersStirTheirVats` (copper, iron, steel and aluminium at 256 RPM), `vatMixersTopOutAtTheirMixersMaterial`, `aVatMixerBreaksPastItsMetalsTopSpeed` |
| Recipes | Pass | `everyPipePumpAndTankCanBeCrafted` (every pipe across, every pipe but iron's upright, each pump, aluminium's only with a polyethylene bushing, and each tank; iron's upright grids still make Create's chute and item vault), `cementationPackRecipe` and `cementationPackFiresIntoBlisterSteel` (moved from the spring's tests, which held them) |
| Old worlds | Pass | `removedBlocksAreKnown` (slag roads map to dirt paths; wooden pipes, pumps and tanks, the spring and Create's mixer's vat form are dropped without a warning), and `slagFertilisesLikeBoneMeal` now checks slag does nothing on a path |
| Dev client | Pass | In the dev client under Xvfb: brass pipes draw 6 px across, straight, bent, rising and joined to Create's copper pipes, with the brass pump on a motor; iron pipes, pumps and tanks in all four rust stages; aluminium, steel and glass parts beside them; the brass, iron and aluminium items in the hotbar. A rusted iron tank of water drips from its sides. `/ponder mcremix:ferric_chloride_bucket` plays the vat mixer scene with a copper vat and mixer and the new texts. Nothing from this mod in the log's warnings or errors |

### Known gaps

- The jar has not been started on a real Forge server (above).
- The pipe metals have no Ponder scene of their own, as stage two's heat-rated pipes had none: tooltips give each
  part's heat limit, and the vat mixer scene names the metals.
- Not played in a survival world; the GameTests cover how the parts behave.

## 0.19.1: The titanium Pressurizer

This covers section 24 of [BUILD_NOTES.md](BUILD_NOTES.md): the Pressurizer in titanium, up to 2,048 RPM, on top of
0.19.0. Tested on 2026-10-23 against the same versions as 0.19.0 (Forge 47.4.6, Create 6.0.8, Power Grid 0.6.2 and
Architectury 9.2.14).

**Result:** 0.19.1 compiled with no errors. Every GameTest passes in a fresh test world: 291 of 291 plain and 297 of 297
with `-Pcompanions`. A dev client draws the titanium Pressurizer in all four colours, shows its item and tooltip, and
plays its Ponder scenes. The jar starts a real Forge server with the three mods it needs.

| Check | Result | How |
| --- | --- | --- |
| Build | Pass | `./gradlew build`: `mcremix-1.20.1-0.19.1.jar`, 4,570,365 bytes, SHA-256 `669195f0…5339d4b`; `builds/mcremix-1.20.1-0.19.1.zip` holds the same jar. Its `mods.toml` says 0.19.1; it has no GameTest classes and no access transformer |
| Production server | Pass | Forge 47.4.6 with Create 6.0.8, Power Grid 0.6.2, Architectury 9.2.14 and this jar makes a new world, is ready 22.2 seconds after launch (`Done (2.511s)`) and stops cleanly. The log has the same warnings and errors as 0.19.0's, none from this mod |
| GameTests, plain | Pass | 291 of 291: the new test below and every 0.19.0 test. The first plain run stopped before any test, with Create's own Registrate reporting unused register callbacks and naming none; the same code started normally the next time, as when 0.16.0 and 0.8.1 each saw it once (BUILD_NOTES section 24) |
| GameTests, with companions | Pass | 297 of 297 |
| Titanium Pressurizer | Pass | `aTitaniumPressurizerWorksTwiceAsFastAt1024` (two titanium cog doublings drive it at 1,024 RPM, twice what steel stands, and over magma it sets ethylene into three pellets in about six seconds, twice the steel one's best; it warms past 100 °C as it turns and keeps working through a change of temper colour), `pressurizerAndCircuitRecipes` (a titanium shaft, two titanium sheets, a steel fluid tank and titanium casing craft one) |
| Dev client | Pass | In the dev client under Xvfb: the titanium Pressurizer draws plain, gold, purple and blue beside the steel one, darker than it, with the same gauge, sockets and window. Its item renders in the inventory, its tooltip gives its summary, jobs and temper colours with High stress impact, and `/ponder mcremix:titanium_pressurizer` plays friction heat, titanium's heat scene and the Pressurizer's own scene. Nothing from this mod in the log's warnings or errors but the two expected for a block new to a saved world |

### Known gaps

- The Pressurizer's Ponder scene shows the steel one; the titanium one's speed and heat are in its tooltip and in the
  titanium heat scene, not that scene.
- It has not been used in a played survival world; the GameTests cover how it behaves.

## 0.19.0: Titanium

This covers section 23 of [BUILD_NOTES.md](BUILD_NOTES.md): idea bank stage five (titanium parts, casings and machines
to 2,048 RPM, friction heat at twice aluminium's cooling need, burning, temper colours, heat sinks melting on it; black
sand, rutile, salt, sodium, rutile briquettes, titanium tetrachloride, sponge and the Arc Furnace; titanium plating) and
every snowy biome giving cold water, on top of 0.18.0. Tested on 2026-10-23 against the same versions as 0.18.0 (Forge 47.4.6, Create 6.0.8, Power Grid 0.6.2
and Architectury 9.2.14).

**Result:** 0.19.0 compiled with no errors. Every GameTest passes in a fresh test world: 290 of 290 plain and
296 of 296 with `-Pcompanions`. A dev client draws every new block and plays both new Ponder scenes through. The jar starts a real Forge server with the three mods it needs.

| Check | Result | How |
| --- | --- | --- |
| Build | Pass | `./gradlew build`: `mcremix-1.20.1-0.19.0.jar`, 4,550,067 bytes, SHA-256 `ece959dc…553c6db8`; `builds/mcremix-1.20.1-0.19.0.zip` holds the same jar. Its `mods.toml` says 0.19.0; it has no GameTest classes and no access transformer |
| Production server | Pass | Forge 47.4.6 with Create 6.0.8, Power Grid 0.6.2, Architectury 9.2.14 and this jar makes a new world, raises Create's speed cap to 2,048 RPM, is ready 34.8 seconds after launch (`Done (6.322s)`, the machine busy with a GameTest run at the time) and stops cleanly. The log has the same warnings and errors as 0.18.0's, none from this mod |
| GameTests, plain | Pass | 290 of 290: the 17 new tests below and every 0.18.0 test |
| GameTests, with companions | Pass | 296 of 296, after the cold water fix below; the first two companion runs each failed `aBucketDippedWhereItSnowsComesUpCold` alone |
| Speed | Pass | `titaniumCarries1024WhereSteelBreaks` (a titanium gearbox past two cog doublings turns at 1,024 RPM where a steel one grinds apart, and after 7 s it is at about 700 °C and tinted, still turning; Create's cap is 2,048) |
| Heat and cooling | Pass | `titaniumNeedsTwiceTheCooling` (with nothing on it, a heat sink, water, cold water or coolant, titanium settles at the same share of the way to burning as aluminium does to melting at half the speed; whole at 128 RPM bare, 680 on water, 1,250 on cold water and 1,800 on coolant, burning at 160, 800, 1,450 and 2,048 on one cooler of coolant, and whole at 2,048 on two), `heatSinksMeltOnHotTitanium` (a sink on titanium heated past what aluminium stands melts into rough aluminium where it was; one on an aluminium gearbox at the same 256 RPM never gets that hot) |
| Temper colours | Pass | `heatTintsTitaniumForGood` (a gearbox, a bracket on a steel shaft, an encased shaft and a press turn gold past 400 °C, purple past 550 and blue past 700, keep their heat through each swap, stay blue as they cool and turn on throughout), `aTintNeverStopsTheLine` (straight after a swap the gearbox and the shaft beyond still turn at full speed), `acidStripsTitaniumTints` (acid takes a blue gearbox, a purple bracket and gold plating back to plain, and a copper bracket's patina off) |
| Burning | Pass | `overheatedTitaniumBurns` (past 1,000 °C at 256 RPM with nothing cooling them, titanium holders flare for three seconds, one setting fire to planks across a gap, then crumble into titanium dioxide, an ingot's worth to each powder: a gearbox whole, a bracket off its steel shaft and a casing off the one inside, and each shaft turns on, held by the steel gearbox beyond it, as cool as the air) |
| The chain | Pass | `saltWaterBoilsDownInAHeatedBasin` (500 mB to a salt over a fed blaze burner, nothing in a cool basin or under a mixer), `saltMeltsAndRutileIsPressedIntoBriquettes`, `moltenSaltSplitsIntoSodiumAndChlorine` (sodium kept, chlorine let off as a cloud), `chlorineOverRutileBriquettesGivesTitaniumTetrachloride` (200 mB from 400 mB of chlorine and two briquettes in a Steel Furnace at Blazing), `sodiumStirredIntoTitaniumTetrachlorideLeavesSponge` (two sponges and eight salt in a steel vat), `moltenSodiumMeetingWaterGoesOffWithABangAndAFire` (no glass broken, fire round about), `escapedTitaniumTetrachlorideFumes` (it leaves hydrogen chloride mist) |
| Arc Furnace | Pass | `anArcFurnaceMeltsSpongeIntoIngots` (two electrodes clicked into both holders and sponge into the hearth; 16 V gives about 1 kW and melts the sponge into an ingot that a hopper takes, both electrodes wearing alike; 8 V is too weak to melt anything; after 8 s the water-cooled hearth is at 71 °C and an uncooled one at 143 °C), `anArcFurnaceStopsWhenAnElectrodeBurnsOut` |
| Plating | Pass | `titaniumPlatingTakesItsFurnacesHeat` (plain plating comes out gold from a Hot Furnace, purple from a Searing one and blue from a Blazing one; gold and purple plating wait for more heat, and no Furnace takes blue) |
| Cold water | Pass | `aBucketDippedWhereItSnowsComesUpCold` failed in the first two companion runs, never in a plain one: WWOO gives snowy plains the frozen ocean's temperature quirk, so it rains on patches of it, and the test's pond was on one. Every biome tagged `forge:is_snowy` now gives cold water, and the test also checks height (windswept gravelly hills give plain water at y 90 and cold water at y 250, where it snows) |
| Dev client | Pass | In the dev client under Xvfb (section 19 of the build notes): the titanium block, black sand, plating in all four colours, the Arc Furnace, the four gearbox colours and the titanium casing, press and shaft all draw with their own textures. Titanium's heat scene (from the plating's Ponder) and the Arc Furnace's play through, every text in turn. The first time, the fire in the burning beat never showed and its two texts overlapped; both are fixed and the beat was played again. Nothing from this mod in the log's warnings or errors |

### Known gaps

- Sodium and water in the same vat do nothing; only sodium poured out into the world reacts.
- The Arc Furnace and plating have not been used in a played survival world; the GameTests cover how they behave.
- Sounds were not checked (no audio device).

## 0.18.0: Heat and cooling

This covers section 22 of [BUILD_NOTES.md](BUILD_NOTES.md): part 3 of the friction redesign (aluminium friction points
heating as they turn and melting when too hot; heat sinks, fans, water coolers and radiator panels cooling them; cold
water from snowy biomes; coolant made from glycol), on top of 0.17.0. Tested on 2026-10-22 against the same versions
as 0.17.0 (Forge 47.4.6, Create 6.0.8, Power Grid 0.6.2 and Architectury 9.2.14).

**Result:** 0.18.0 compiled with no errors. Every GameTest passes in a fresh test world: 273 of 273 plain and 279 of 279
with `-Pcompanions`. A dev client plays both new Ponder scenes through. The jar starts a real Forge server with the
three mods it needs.

| Check | Result | How |
| --- | --- | --- |
| Build | Pass | `./gradlew build`: `mcremix-1.20.1-0.18.0.jar`, 4,077,809 bytes, SHA-256 `ec0bfd48…80dfdedc`; `builds/mcremix-1.20.1-0.18.0.zip` holds the same jar. Its `mods.toml` says 0.18.0; it has no GameTest classes and no access transformer |
| Production server | Pass | Forge 47.4.6 with Create 6.0.8, Power Grid 0.6.2, Architectury 9.2.14 and this jar makes a new world, is ready 22.2 seconds after launch (`Done (3.273s)`) and stops cleanly. The log has the same warnings and errors as 0.17.0's, none from this mod, and the new world's server config has `frictionHeat = true` with its comment |
| GameTests, plain | Pass | 273 of 273: the 11 new tests below and every 0.17.0 test |
| GameTests, with companions | Pass | 279 of 279 |
| Friction heat | Pass | `onlyAluminiumHoldersHeatUp` (at 256 RPM an aluminium bracket, casing or gearbox warms up whatever it holds, while an aluminium shaft in a steel bracket or casing, and a steel gearbox, stay at the air's temperature), `coolingCarriesAluminiumFaster` (where aluminium settles: whole to about 256 RPM bare, about 384 with a heat sink or a fan blowing straight on it, 512 with a 128 RPM fan through the sink; melting about a minute from cold at 512), `heatSinksAndFansCoolAluminium` (four gearboxes heated 480 degrees above the air: bare, it climbs on; a sink and a fan straight on it cool about alike; a fan through the sink cools faster than either) |
| Melting | Pass | `overheatedAluminiumMelts` (past 660 °C at 512 RPM a gearbox melts whole, an aluminium casing melts off the steel shaft inside and an aluminium bracket off the steel shaft it held, and each shaft turns on, held by the steel gearbox beyond it, as cool as the air; the aluminium sets as rough aluminium) |
| Water coolers | Pass | `waterCoolersTurnWaterWarmAsTheyCool` (on water far faster than a heat sink, giving back as much warm water as the water it took; on cold water faster, on coolant faster again, giving back hot coolant; with its output full or no water it cools nothing; one on a Power Grid resistor cools it, and one on a steel gearbox has nothing to do), `pipesJoinCoolersAndRadiatorsWhereTheyShould` (a cooler on any face but its base, a radiator only at its front and back; a cooler takes water, cold water and coolant, a radiator any water, hot coolant and coolant, and neither takes salt water) |
| Radiator panels | Pass | `radiatorsChillWarmWater` (slowly in still air and faster with a 128 RPM fan; a panel shares its water through one pipe in and another out, and with a fan under each column every block chills fast; a radiator beside it facing another way stays on its own; plain water chills as warm water does, hot coolant comes back as coolant, and cold water passes through) |
| Cold water | Pass | `aBucketDippedWhereItSnowsComesUpCold` (a frozen river and a pond on snowy plains give cold water, a frozen ocean salt water), `aPumpDrawsColdWaterFromAFrozenRiver`, `warmAndColdWaterPouredOutTurnToWater` (hot coolant poured out turns to coolant, and coolant stays coolant) |
| Coolant | Pass | `coolantIsGlycolMadeFromDichlorideAndMixedWithWater` (ethylene dichloride and caustic soda in a basin over a fed blaze burner make glycol and salt water; glycol and water, unheated, make coolant) |
| Ponder | Pass | In the dev client under Xvfb (section 19 of the build notes), friction heat in aluminium plays through from the heat sink's Ponder and cooling with water and coolant from the water cooler's: every text in turn, the gearboxes, fans and pumps turning and the radiators giving off cold mist. No errors or exceptions from this mod in the log |

### Known gaps

- A radiator loop has not been built and run in a played world; the GameTests cover how it behaves.
- Sounds were not checked (no audio device).

## 0.17.0: Machine tiers, fitted bits and wooden movers

This covers section 21 of [BUILD_NOTES.md](BUILD_NOTES.md): part 2 of the friction redesign (presses, mixers,
millstones, fans, saws, drills, deployers, crafters and arms in copper, iron, steel and aluminium, swapped with a
casing; drill bits and saw blades fitted to any drill or saw; copper and steel vat mixers; bearings, pistons, gantries
and rope pulleys in every wood), on top of 0.16.0. Tested on 2026-10-22 against the same versions as 0.16.0 (Forge
47.4.6, Create 6.0.8, Power Grid 0.6.2 and Architectury 9.2.14).

**Result:** 0.17.0 compiled with no errors. Every GameTest passes in a fresh test world: 262 of 262 plain and 268 of 268
with `-Pcompanions`. A dev client shows the wooden movers drawn and moving in their wood with and without Flywheel, and
the new Ponder scenes. The jar starts a real Forge server with the three mods it needs.

| Check | Result | How |
| --- | --- | --- |
| Build | Pass | `./gradlew build`: `mcremix-1.20.1-0.17.0.jar`, 3,896,253 bytes, SHA-256 `094995e8…877176d3`; `builds/mcremix-1.20.1-0.17.0.zip` holds the same jar. Its `mods.toml` says 0.17.0; it has no GameTest classes and no access transformer |
| Production server | Pass | Forge 47.4.6 with Create 6.0.8, Power Grid 0.6.2, Architectury 9.2.14 and this jar makes a new world, is ready 23.1 seconds after launch (`Done (3.516s)`) and stops cleanly, with no warnings from this mod |
| GameTests, plain | Pass | 262 of 262: the 30 new tests below and every 0.16.0 test, the stone, golden and diamond drill and saw tests rewritten for bits and blades |
| GameTests, with companions | Pass | 268 of 268 |
| Create takes the tiers for its own | Pass | `createTakesEveryTierForItsOwn` (Create's `has`, `isIn` and `is` checks for every kind and tier), `tieredFansBlowThroughChutes`, `craftersOfAnyTierWorkTogether` (a copper crafter joins Create's in a grid, takes its items and shares its inventory when connected), `armsReachEveryTier`, `tieredDeployersReachAndPlaceAlongside` |
| Limits and weathering | Pass | `machinesTopOutAtTheirMaterialAndDrawItsFriction` (every machine of every tier, Create's own as andesite or brass, is a friction point capped at its material and carrying its load; iron presses draw 0.125 su per RPM more, steel 0.25, copper and aluminium none, weathered ones more; Create's crushing wheel is no friction point, carries any load and tops out at 128 RPM), `andesiteMachinesBreakPastTheirTopSpeed` (Create's millstone grinds itself apart at 256 RPM while an iron one runs on), `machinesWeatherByTheirMaterial` (copper anywhere, iron and steel only when wet, aluminium never), `machinesAreWaxedScrapedAndDropAsTheyWere` |
| Casing swaps | Pass | `casingsSwapAPlacedMachine` (up and down: what it holds stays, the old casing pops out and it finds its speed again; a machine that does not come in that material is left alone), `swapsKeepBitsAndLeaveFrontsToCreate` (a drill keeps its bits; a deployer's and a crafter's front still take the casing in hand), `deployersSwapCasingsToo` |
| Bits and blades | Pass | `bitsAreFittedByUsingThemOnADrill`, `deployerFitsBitsToADrill`, `brokenDrillKeepsItsBits` (it goes back down with them, and only drills with iron bits stack with new ones), `goldBitsFeedAHopperFromACobbleGenerator`, `bladesAreFittedByUsingThemOnASaw`, `deployerFitsBladesAndBrokenSawsKeepThem`, `sawStressFollowsTheirBlades`, `drillBitsRecipes`, `sawBladeRecipes`, and the 0.4.0 to 0.16.0 drill and saw tests run on fitted bits and blades |
| Vat mixers | Pass | `copperAndSteelMixersFitCopperAndSteelVats` (neither fits a wooden vat; iron and aluminium mixers go down as ordinary mixers), `copperAndSteelVatMixersStirTheirVats` (at 256 RPM, which Create's could not take), `vatMixersTopOutAtTheirMixersMaterial`, `createsVatMixerBreaksPastAndesitesTopSpeed` (and gives the mixer back), `copperAndSteelVatMixersWeatherAsTheirMixersDo` (broken, a weathered one gives back a weathered mixer) |
| Wooden movers | Pass | `createTakesEveryWoodenMoverForItsOwn` (every kind in every wood, axe-mineable, placed with what they hold by a schematicannon), `woodenMoversAreCraftedFromTheirWood` (a mixed-wood recipe makes nothing), `woodenPistonsPushTheirOwnHeadOnAnyPoles` (an oak piston on spruce poles pushes out an oak head), `slimeMakesAWoodenPistonSticky`, `pistonHeadsDropTheirWoodsPole`, `woodenCarriagesRideAnyGantryShafts` (an oak carriage over spruce shafts with Create's in the middle), `woodenPulleysLetDownCreatesRope`, `woodenMoversTopOutAt64Rpm` (an oak bearing breaks at 128 RPM, Create's at 256, and each takes Create's bearing's stress) |
| In game | Pass | A dev client under Xvfb, as in section 19 of the build notes. With Flywheel's instancing backend and with Flywheel off: an oak piston on spruce poles pushing a birch bearing, the bearing's top drawn in birch while it moves; an oak gantry carriage riding spruce and Create's gantry shafts, its shaft and cogs in oak; a jungle rope pulley letting down Create's rope; an oak bearing turning a plank once assembled; Create's own bearing, piston, carriage and pulley beside them for comparison. All placed with commands and still right after saving and loading the world. No mixin errors or exceptions in the log |
| Ponder | Pass | In the dev client, Ponder opens for a wooden piston and a wooden gantry carriage with Create's scenes, the new machine materials scene plays through for a copper press (the andesite press breaking, the iron one made steel with a casing, the copper one weathering and waxed), and Create's press lists it after its own two scenes |

### Known gaps

- A wooden piston's hidden pole, the one inside the piston while it is extended, comes back in the piston's own wood
  after a cycle, so mixed-wood poles can trade one pole for the piston's wood. Create keeps no record of it.
- Picking Create's rope or magnet in creative gives Create's rope pulley, even under a wooden pulley.

## 0.16.0: Plastics, the Steel Furnace and PVC pipes

This covers section 20 of [BUILD_NOTES.md](BUILD_NOTES.md): stage four of the idea bank (furnaces that must be lit,
the Steel Furnace on piped fuel, the Spark Plug, fermenting, vats of layered fluids with mixers on their sides, the
Pressurizer and pressurised steam, polyethylene, PVC and PVC face pipes, hydrogen chloride and hydrochloric acid), on
top of 0.15.1. It was built in a copy of 0.15.0 and rebased onto 0.15.1 before these tests. Tested on 2026-10-22
against the same versions as 0.15.1 (Forge 47.4.6, Create 6.0.8, Power Grid 0.6.2 and Architectury 9.2.14).

**Result:** 0.16.0 compiled with no errors. Every GameTest passes in a fresh test world: 232 of 232 plain and 238 of 238
with `-Pcompanions`. A dev client shows the new blocks, a vat's layers (kept through a reload), the new Ponder
scenes and the new JEI tabs right, after the fixes it led to below. The jar starts a real Forge server with the three mods it needs.

| Check | Result | How |
| --- | --- | --- |
| Build | Pass | `./gradlew build`: `mcremix-1.20.1-0.16.0.jar`, 2,909,685 bytes, SHA-256 `181da4ac…993542d8`; `builds/mcremix-1.20.1-0.16.0.zip` holds the same jar. Its `mods.toml` says 0.16.0; it has no GameTest classes and no access transformer |
| Production server | Pass | Forge 47.4.6 with Create 6.0.8, Power Grid 0.6.2, Architectury 9.2.14 and this jar makes a new world, is ready in 28.5 seconds (0.15.1's jar took 33.9 on the same machine; it is the new world being generated) and stops cleanly, with no warnings from this mod |
| GameTests, plain | Pass | 232 of 232: the 34 new tests below and every 0.15.1 test but `hydrogenIsFurnaceFuel`, which is gone with the Furnace's piped hydrogen |
| GameTests, with companions | Pass | 238 of 238 on the final code, after two flukes outside this stage (Test world, below) |
| Lighting | Pass | `aFurnaceBurnsOnlyOnceLit` (fuel burns only once lit, then with nothing to fire until it runs out and the fire goes out; flint and steel will not catch with nothing in it; a shovel puts it out and leaves the fuel; a fire charge lights it again; a Deployer lights it and puts it out; lava underneath heats a Furnace never lit). The boiler, basin, bellows and companion tests light their Furnaces first and expect a lit one to burn whether or not there is work |
| Steel Furnace fuel | Pass | `aSteelFurnaceBurnsHotterTheFasterItsFuelIsPumped` (hydrogen from a 4 RPM pump is Hot, from 16 RPM Blazing; 32 RPM is no hotter and the rest is lost; ethylene twice as rich), `aSteelFurnaceBurnsOnlyPipedFuel` (no solid fuel, no heat from lava below, no bellows, no lighting with nothing piped in; a Furnace takes no fuel by pipe), `aLitSteelFurnaceBurnsAllItIsFed` (20 mB a tick of hydrogen keeps it Blazing, the rest lost and its goggles say so; out a second after the fuel stops), `anUnlitSteelFurnaceLetsItsFuelEscape` (ethanol pools and pours out a bucket at a time, backs the pipe up when walled in, hydrogen vents, and lighting it flares off what pooled) |
| Steel Furnace work | Pass | `steelCasingMakesAFurnaceASteelFurnace` (keeps its chamber, pops its coal out, puts the fire out, uses one casing), `ethanolOverAluminaGivesEthylene` (on wood spirit, an alumina worn away for every 16 buckets; without one it burns its fuel and makes nothing), `ethyleneDichlorideCracksIntoVinylChloride` (Searing on coal tar, vinyl chloride and hydrogen chloride a tank each) |
| Spark Plug | Pass | `aSparkPlugLightsItsFurnaceAt1000Volts` (sparks at 1,000 V drawing about 10 mA, lighting the planks in a Furnace and the hydrogen piped into a Steel Furnace; nothing at 500 V; a Furnace put out with a shovel catches again at once), `aSparkPlugGoesOnlyOnAFurnacesSide` (side or back, never front, top or anything else; pops off when the furnace goes or turns to face it), `aSparkPlugIsCopperBakeliteAndIron` |
| Basins, vats and fermenting | Pass | `aBasinLetsGasGo` (chlorine piped in rises off as a cloud, gas already in it goes, water stays), `aVatHoldsFourFluidsInLayers` (four and no fifth, heaviest liquid lowest and lightest gas highest; a pipe draws off one it has no use for), `aVatKeepsItsLayersThroughASave` (saved, then loaded again, it has its chlorine and ethylene back as well as its water), `sugarFermentsInABasinLeftAlone` (250 mB of ethanol in 30 seconds; not under a mixer), `aVatFermentsABatchForEveryBucket` (four batches at once; its pipes take the ethanol but not the water it is working on) |
| Mixers on vats | Pass | `aMixerUsedOnAWoodenVatGoesOnAsItsVatForm` (facing into the Oak Tank, named after the mixer, breaking back into it), `aMixerOnlyFitsAVatOfItsMaterial` (nothing goes on a copper vat, sneaking puts an ordinary mixer down, and one forced onto a copper vat stirs nothing and says so), `aVatMixerMakesHydrochloricAcid` (four batches a stir), `chlorineAndEthyleneJoinOverFerricChloride` (ferric chloride used up slowly), `aVatMixerKneadsDough` (Create's wet unheated mixing, the dough coming out of the scaffolding into a chest) |
| Pressurizer | Pass | `ethyleneSetsIntoPelletsAt512OverHeat` (two pellets in 150 ticks at 512 RPM over magma; too slow at 256, too cold with nothing under it; no pipes at its shaft faces or bottom), `vinylChlorideSetsIntoResinAt256Unheated` (an empty hand takes the resin; too slow at 128), `steamRunsASteelBoilerWithNoFireUnderIt` (20 mB a tick over lava at 128 RPM, none over magma; that steam is two levels of heat and water in a four-tank steel boiler, whose engine runs at full strength), `aFurnaceUnderneathHeatsIt` (a lit Furnace on charcoal heats it Hot and it makes pellets; an unlit one with the same charcoal leaves it too cold), `pressurizerAndCircuitRecipes` |
| PVC pipes | Pass | `pvcPipesLieFlatAndJoinRoundCorners` (along a floor, up a wall round an inside corner, over a step's edge through a corner piece; a wrench cuts and mends the joint, breaking the corner piece cuts it; taking the step away drops both pipes), `pumpsReachTwiceAsFarThroughPvc` (a pump fills a tank through twenty PVC pipes, and through fifteen of Create's copper pipes, as before, but not twenty), `hotFluidsMakePvcSagAndBurst` (lava bursts it, water runs through) |
| Acid and gases | Pass | `pouredAcidStripsRustThenSoaksAway` (cut copper, a rusted iron shaft and a weathered copper pipe come back bare, waxed copper does not, then the acid soaks away and its run-off dries up), `aSpoutOfAcidCleansWeatheredParts` (100 mB each; the pipe loses its patina; waxed copper is left), `ethyleneRipensCropsOnceACloud` (one stage only, gone in five seconds, nothing out of reach), `vinylChlorideMakesYouDizzy` (a pig, not a husk), `hydrogenChlorideMistRustsMetalOnce` (an iron shaft and copper under it a stage, once; waxed copper and copper out of reach untouched) |
| Polyethylene | Pass | `aluminiumFrictionPointsTakePolyethylene` (the casing and bracket need a bushing; aluminium shafts and cogs take none), with the bushing pressed over a heated basin in `PlasticsTests` |
| In game | Pass | A dev client under Xvfb, as in section 19 of the build notes: the Steel Furnace, Spark Plug, Pressurizer and Create's mixer in its vat form placed in a creative world; PVC pipes along a floor, up a wall round an inside corner and over a step's outside corner; an Oak Tank vat of eight tanks filled by `/data` with ferric chloride, chlorine and ethylene, showing three layers through its windows and still holding them after Save and Quit and loading the world again; JEI's Fermenting, Steel Furnace, Steel Furnace Fuel, Vat Mixing and Pressurizing tabs, every line in full. The rest of section 20's list is the GameTests' |
| Ponder | Pass | In the dev client, the Furnace scene (lit with flint and steel, burning with nothing to fire, put out with a shovel) and the new Spark Plug, Steel Furnace, vat mixer and Pressurizer scenes play through. The vat mixer scene shows its vat's chlorine and ethylene hanging at the top, which it never did before the vat save fix |

### Bugs found and fixed

- **Every heat change cut off the pump feeding a Steel Furnace.** A furnace's block state changed with the usual
  neighbour update, which Create's pipes take to mean their pipework changed, so they started their flows over and the
  fuel stopped for a moment at each change. `aSteelFurnaceBurnsHotterTheFasterItsFuelIsPumped` caught it; both furnaces
  change their state without that update now, telling comparators directly.
- **A Pressurizer on a Furnace got no heat from it.** It read heat from below as a crucible does, which counts blaze
  burners, fire, lava and heaters but not a Furnace. Found writing these notes; a Furnace or Steel Furnace underneath
  now heats it as hot as its chamber (`aFurnaceUnderneathHeatsIt`). A crucible on a Furnace still gets no heat from it,
  as in 0.13.0.
- **A vat took Electrolysers to be fitted in a new world's first ticks**, before any had asked for work, because the
  time one last asked started at 0. Found by the new vat tests; it starts long ago now.
- **A vat lost everything but its tank's own fluid when it was loaded again** (since 0.14.0). Its data was saved and
  loaded at the tail of Create's tank `write` and `read`, which return early unless they are writing for or reading
  from a client, so it only ever went to players. Found when a vat filled with `/data` showed no layers; it is saved
  and loaded at every return now (`aVatKeepsItsLayersThroughASave`). This also made the vat mixer Ponder scene show
  its vat's layers, which it never had.
- **A wooden or steel tank joined into a bigger one drew scrambled textures** (since 0.13.0), as did weathered copper
  tanks: dark, or see-through, which spoiled every vat. Create's connected textures move a quad's corners onto the
  connected sprite but leave the quad naming the plain one, and `SpriteSwapModel` went by the name. It goes by where the
  corners lie now; checked on an oak vat in a world and in Ponder.
- **Two JEI tabs cut their lines short**: the Steel Furnace Fuel tab's Searing and Blazing rates and the Pressurizing
  tab's steam line, and the Vat Mixing note. The two tabs are 140 wide now and the note reads "Stirred in a vat". The
  design plan's fuel table gave coal tar and ethylene a Warm rate of ½ mB a tick, which a pump never delivers (Create's
  pumps move at least 1 mB a tick) and Taran's answers never had; JEI was right, and the table now starts them at Hot.
- **Create warned at start-up about six of this stage's recipes.** The Pressurizer's and Steel Furnace's recipes give
  a time, which Create's base recipe type says has no effect, and the ethylene dichloride recipe takes three fluids
  where a basin recipe takes two. Only warnings: the times and all three fluids were used all along. Found in the
  production server's log; those recipe types now say they take a time, and a vat recipe up to four fluids.
- **The coal tar a cold Steel Furnace lets out in its Ponder scene was a block of tar**; it is a low puddle on the
  floor now.
- **A Furnace Ponder line said a furnace burns no fuel of its own while heat comes from below**, which stopped being
  true once a lit furnace burns whatever happens. It says that heat needs no fuel and no lighting now.

### Test world

Every run started from a deleted `run/gametest/world`. The first Pressurizer test read the whole goggles readout, whose
Create stress lines need a client, and crashed the test server; the tests read the Pressurizer's own lines now.

The companion suite ran five times while the last fixes went in. Three runs passed all 238, the last of them on the final code. One
stopped before any test: Create's own registration found unused register callbacks at the first registry event, before
anything of this mod was registered, and it did not happen again. One failed the friction thread's
`frictionPointsGrindAboveTheirSpeedLimit`, whose Metal Bracket item, ground off its shaft, was no longer within 3 blocks
of its lane at tick 200; that test passed in the other four companion runs and in every plain run, and nothing of this
stage touches it. The plain suite passed all 232 every time.

### Files changed

New: `client/PressurizerRenderer.java`, `PvcPipeModel.java`, `SparkPlugEffects.java`, `VatLayersRenderer.java`,
`VatMixerRenderer.java`, `compat/jei/FermentingCategory.java`, `PressurizingCategory.java`, `StandingCategory.java`,
`SteelFurnaceCategory.java`, `SteelFurnaceFuelCategory.java`, `VatMixingCategory.java`, `electrolysis/BasinVent.java`,
`FermentingRecipe.java`, `VatMixerBlock.java`, `VatMixerBlockEntity.java`, `VatMixingRecipe.java`,
`fluids/AcidBlock.java`, `EthyleneCloud.java`, `GasCloud.java`, `HydrogenChlorideMist.java`, `PvcCornerBlock.java`,
`PvcGeometry.java`, `PvcPipeBlock.java`, `PvcPipeBlockEntity.java`, `PvcPipes.java`, `VinylChlorideHaze.java`,
`kiln/SparkPlugBlock.java`, `SparkPlugBlockEntity.java`, `SteelFurnaceBlock.java`, `SteelFurnaceBlockEntity.java`,
`SteelFurnaceFuels.java`, `SteelFurnaceRecipe.java`, the mixins `FluidTankRendererMixin` and `PumpBlockEntityMixin`,
`ponder/PressurizerScenes.java`, `SparkPlugScenes.java`, `SteelFurnaceScenes.java`, `VatMixerScenes.java`,
`pressurizer/PressurizerBlock.java`, `PressurizerBlockEntity.java`, `PressurizingRecipe.java`, the tests `AcidTests`,
`PlasticsTests`, `PressurizerTests`, `PvcPipeTests`, `SparkPlugTests`, `SteelFurnaceTests` and `VatTests`, the
`mcremix:furnace_lighters` item tag, and Power Grid's integrated circuit recipe. Changed: `bellows/BellowsBlockEntity.java`,
`client/SpriteSwapModel.java`, `TieredKineticVisual.java`, `compat/jei/KilnHeatCategory.java`, `RemixJeiPlugin.java`, `SettlingCategory.java`,
`electrolysis/BasinSettling.java`, `Gases.java`, `Vat.java`, `fluids/Chemical.java`, `ChlorineCloud.java`,
`FluidPartMaterial.java`, `GasBlock.java`, `Overheating.java`, `PressureBoilerData.java`, `Spills.java`,
`heat/BellowsTarget.java`, `kiln/KilnBlock.java`, `KilnBlockEntity.java`, `kinetics/Weathering.java`, the mixins
`BasinBlockEntityMixin`, `BlockItemMixin`, `FluidPropagatorMixin`, `FluidTankBlockEntityAccessor` and
`FluidTankBlockEntityMixin`,
`mcremix.mixins.json`, `ponder/BasinScenes.java`, `BoilerScenes.java`, `ElectrolysisScenes.java`, `HeatScenes.java`,
`ModPonderPlugin.java`, `TarScenes.java`, `registry/ModBlockEntities.java`, `ModBlocks.java`, `ModEntities.java`,
`ModFluidBlocks.java`, `ModFluids.java`, `ModItems.java`, `ModRecipes.java`, `ModTags.java`, the tests `BellowsTests`,
`BoilerTests`, `CompanionTests`, `ElectrolysisTests`, `FrictionPointTests`, `KilnBasinTests`, `KilnTests`,
`PowerGridRecipeTests` and `SpringTests`, the `mcremix:polyethylene`, `mcremix:fluid_ceiling/cold` and
`mcremix:kinetic_material/steel` tags, `tools/gen_assets.py` and everything it writes (models, textures, recipes,
the acid filling recipes, lang and the four new Ponder schematics), `gradle.properties` (0.16.0), README.md, DESIGN.md
and BUILD_NOTES.md. Ferric chloride's recipe moved from `mixing` to `vat_mixing`.

## 0.15.1: Brackets on their own

This covers section 19a of [BUILD_NOTES.md](BUILD_NOTES.md): Taran's ask that a bracket can go down on its own and a
shaft be placed into it, on top of 0.15.0. Tested on 2026-10-22 against the same versions as 0.15.0 (Forge 47.4.6,
Create 6.0.8, Power Grid 0.6.2 and Architectury 9.2.14).

**Result:** 0.15.1 compiled with no errors. Every GameTest passes in a fresh test world: 199 of 199 plain and 205 of 205
with `-Pcompanions`. A dev client placed brackets on a floor and a wall, put a shaft and a cog into them, turned one
with a wrench and played the brackets Ponder scene. The jar starts a real Forge server with the three mods it needs.

| Check | Result | How |
| --- | --- | --- |
| Build | Pass | `./gradlew build`: `mcremix-1.20.1-0.15.1.jar`, 3,438,299 bytes, SHA-256 `67c1f808…040ae253`; `builds/mcremix-1.20.1-0.15.1.zip` holds the same jar. Its `mods.toml` says 0.15.1; it has no GameTest classes and no access transformer |
| Production server | Pass | Forge 47.4.6 with Create 6.0.8, Power Grid 0.6.2, Architectury 9.2.14 and this jar makes a new world, is ready in 3.5 seconds and stops cleanly, with no warnings from this mod |
| GameTests, plain | Pass | 199 of 199: the three new tests below and every 0.15.0 test unchanged |
| GameTests, with companions | Pass | 205 of 205 |
| Brackets on their own | Pass | `bracketsGoDownOnTheirOwn` (on the floor, its shaft the way the player looks; on a wall, lined up with a shaft pointing at it; refused with the message where nothing solid is beside it; no full block, nothing bolts to its top; a wrench turns it on its plate; Create's metal bracket goes down too and drops when its wall goes) |
| Parts into brackets | Pass | `aPartPlacedIntoABracketTakesIt` (a shaft placed into a bracket lies along it though the player looks across it, takes the bracket, uses no bracket and is a friction point; a cog placed from the floor in front of one goes in and gets a cog's bracket; both count as held; breaking the shaft drops it and its bracket) |
| Create's cog rule | Pass | `heldCogsKeepCreatesRules` (a large cog beside another in its plane is refused though a bracket would hold it, and with a bracket in the offhand, with no message about holding) |
| In game | Pass | A copper bracket on the floor and Create's metal bracket on a wall stand with outlines that fit them and the wall's face still drawn around the plate; a copper shaft and a copper cog placed into floor brackets lie along them; a wrench turned the wall bracket to take a shaft up the wall, and a shaft placed into it stood upright |
| Ponder | Pass | The brackets scene puts two brackets down, places shafts into them, shows the line and fits the iron bracket on the post |

### Bugs found and fixed

- **Held cogs skipped Create's rule against cogs that clash (0.15.0).** The placement rule answered only whether a
  bare part was held, so a large cog could go beside another in its plane wherever something held it, and a bracket in
  the offhand let one in anywhere. Found reading Create's `CogWheelBlock` while building this; an unheld part is
  refused and a held one still asks its block now.

### Files changed

New: `mixin/WrenchableDirectionalBlockMixin.java`. Changed: `McRemix.java`, `kinetics/Brackets.java`,
`FrictionPoints.java`, `KineticFamilies.java` (a comment), `ShaftSupport.java`, the mixins `BlockItemMixin`,
`BlockStateBaseMixin` and `BracketBlockItemMixin`, `mcremix.mixins.json`, `ponder/FrictionPointScenes.java`,
`PonderParts.java`, `FrictionPointTests.java`, `tools/gen_assets.py` and the lang file it writes (bracket and shaft
tooltips, the refusal message and the scene's text), `gradle.properties` (0.15.1), README, DESIGN.md and BUILD_NOTES.md.

## 0.15.0: Friction points, brackets, casings and brass

This covers section 19 of [BUILD_NOTES.md](BUILD_NOTES.md), part one of Taran's friction points (friction points,
brackets, casings, brass and load limits), on top of 0.14.0. Tested on 2026-10-22 against the same versions as 0.14.0
(Forge 47.4.6, Create 6.0.8, Power Grid 0.6.2 and Architectury 9.2.14), from this folder's 0.14.0 source as it stood on
2026-10-21.

**Result:** 0.15.0 compiled with no errors. Every GameTest passes in a fresh test world: 196 of 196 plain and 202 of 202
with `-Pcompanions`. A dev client went through the "In game" list in section 19, Ponder scenes included, and starting
it found that 0.14.0 crashed every client at start-up, which 0.15.0 fixes. The jar also starts a real Forge server with
the three mods it needs. Section 18's list for 0.14.0 is still to check by hand.

| Check | Result | How |
| --- | --- | --- |
| Build | Pass | `./gradlew build`: `mcremix-1.20.1-0.15.0.jar`, 3,430,832 bytes, SHA-256 `8144f167…f12927d`; `builds/mcremix-1.20.1-0.15.0.zip` holds the same jar. Its `mods.toml` says 0.15.0 and still makes `powergrid` 0.6.2 or later mandatory; it has no GameTest classes and no access transformer |
| Production server | Pass | A server from the Forge 47.4.6 installer with Create 6.0.8, Power Grid 0.6.2, Architectury 9.2.14 and this jar makes a new world, is ready in 3.6 seconds and stops cleanly, with no warnings from this mod past writing its new server config |
| GameTests, plain | Pass | 196 of 196: the 11 new `FrictionPointTests` and the reworked tests below |
| GameTests, with companions | Pass | 202 of 202 |
| What holds what | Pass | `barePartsGoOnlyWhereSomethingHoldsThem` (each material's span, counted through the parts between; sources hold nothing; Create's helper that runs a line of shafts on keeps to the same rule), `partsPopOffWhenWhatHoldsThemGoes` (all along a line when its gearbox goes, and a part whose bracket's block goes; a part with a bracket of its own stays and holds the next) |
| Brackets | Pass | `aBracketInTheOffhandGoesOnAsThePartIsPlaced`, `bracketsBoltOnlyToSolidBlocks` (below, above or beside the part, only against a solid block), `bracketsStayOnThroughWaxAxeAndWeather` |
| Casings | Pass | `casingsEncaseAnyPartAndKeepIt` (every casing on parts of every material, Create's too; a sneaking wrench takes the casing off, breaking drops the part inside, and the casing is not used up), `encasedPartsTakeTheSlowerOfPartAndCasing`, `encasedFrictionComesFromTheCasing` |
| Load | Pass | `bracketsAndCasingsGiveWayUnderLoad` (a copper bracket or casing on an iron shaft gives way and leaves the shaft turning; copper inside an iron casing breaks, casing and all), `woodIsTheFuseUnderLoad`, `woodenGearboxIsTheFuseUnderLoad`, `woodenClutchAndGearshiftAreFusesUnderLoad` |
| Top speed | Pass | `frictionPointsGrindAboveTheirSpeedLimit`, `everyWoodenFrictionPointGrindsAbove64Rpm`, `brassEncasedShaftsTakeTheBrassLimit` (Create's brass encased shaft is brass) |
| Brass and aluminium | Pass | `brassSitsJustAboveWood` (every part and a bracket, 96 RPM, 3,072 su, never tarnishes), `aluminiumFrictionPointsWaitForPolyethylene` (its bracket, casing, gearbox, clutch, gearshift and chain drives need polyethylene, which comes in stage 4; its shafts and cogs can be made now) |
| In game | Pass | Section 19's list in a dev client: a part refused with the right message with and without a bracket in the offhand, brackets in every material and weathering stage, casings showing their part, brass clockwork, goggles, five presses wobbling oak shafts loose until one breaks off, and an oak bracket at 96 RPM grinding until it gives way while a copper one runs on |
| Ponder | Pass | The friction points, brackets, casings, wood fuse, materials, steel, Furnace basin and byproducts scenes play through |

### Bugs found and fixed

- **0.14.0 crashed every client at start-up.** `ChemicalFluidType.initializeClient` read its chemical before its
  constructor had set it, since Forge calls it from `FluidType`'s constructor, and only on a client, so servers and
  GameTests never saw it. It reads the chemical only when asked now.
- **A sneaking wrench could not take a casing off.** Taking it off puts the bare part back, and the new placement rule
  took that for a new part with nothing holding it and refused it, so the casing stayed. `casingsEncaseAnyPartAndKeepIt`
  caught it; only a part put where there was room for one counts as placed now.
- **The wrong message for an offhand bracket.** With a bracket in the offhand that had nothing to bolt to (a vertical
  shaft on open floor), the refusal still told the player to hold a bracket in the offhand. Found in game; it now says
  the bracket has nothing to bolt to and needs a solid block beside the part, across its axis.

### Test world

Every run started from a deleted `run/gametest/world`. One run failed `frictionPointsGrindAboveTheirSpeedLimit`: the
bracket that gives way drops inside the shaft that stays, which pushes the item out along its length, past where the
test looked. It looks within 3 blocks now, and every run since passed.

The dev client needed two things that do not touch the jar. `runClient` crashed on joining a world with an
`IllegalAccessError` from Power Grid's `LevelEntitiesAccessor` mixin, which clashes with the official name of
`Level.getEntities()` in a dev environment only; `dev/accesstransformer.cfg` makes both methods public there. And on
the software renderer it ran on, Flywheel's default `indirect` backend left some turning shafts undrawn from some
camera spots, Create's own shafts as much as this mod's from the same spot, so the screenshots were taken with
`/flywheel backend flywheel:instancing`, which draws everything.

### Files changed

New: `client/EncasedPartRenderer.java`, `EncasedPartTextures.java`, `EncasedPartVisual.java`, `Wobble.java`,
`kinetics/Brackets.java`, `EncasedPart.java`, `EncasedPartBlockEntity.java`, `EncasedParts.java`, `EncasingCasing.java`,
`FrictionGoggles.java`, `FrictionPoints.java`, `KineticMaterials.java`, `PartSwaps.java`, `ShaftSupport.java`,
`TieredBracketBlock.java`, `TieredEncased.java`, `TieredEncasedCogwheelBlock.java`, `TieredEncasedShaftBlock.java`, the
mixins `BlockItemMixin`, `BracketBlockItemMixin`, `BracketedBlockEntityBehaviourAccessor`,
`BracketedBlockEntityBehaviourMixin`, `KineticBlockEntityRendererMixin`, `LevelMixin` and `RotatingInstanceMixin`,
`ponder/FrictionPointScenes.java`, `PonderParts.java`, `FrictionPointTests.java`, `dev/accesstransformer.cfg`, and the
`mcremix:kinetic_material/*` and `mcremix:friction_points` block tags. Removed: the shear pin Ponder scene and the
`shear_pins` tag, the `max_speed/*` and `max_load/*` block tags, and the Aluminium Casing's item application recipes
(it is crafted with polyethylene now). Changed: `McRemix.java`, `client/ModPartialModels.java`, `ModSpriteShifts.java`,
`compat/jei/RemixJeiPlugin.java`, `config/RemixConfig.java`, `fluids/ChemicalFluidType.java`, `RatedPumpBlock.java`,
`kinetics/AluminiumMelting.java`, `KineticFamilies.java`, `KineticLimits.java`, `KineticMaterial.java`,
`KineticPart.java`, `KineticVariant.java`, `TieredChainDriveBlockEntity.java`, `TieredChainGearshiftBlockEntity.java`,
`TieredClutchBlockEntity.java`, `TieredGearboxBlockEntity.java`, `TieredGearshiftBlockEntity.java`,
`TieredKineticBlockEntity.java`, `TieredShaftBlock.java`, the mixins `BlockStateBaseMixin` and
`KineticBlockEntityMixin`, `ponder/BasinScenes.java`, `KineticScenes.java`, `ModPonderPlugin.java`,
`registry/ModBlockEntities.java`, `ModBlocks.java`, `ModTags.java`, the tests `GT`, `ChainDriveTests`, `ClutchTests`,
`GearboxTests`, `KineticTests`, `SteelTests` and `WoodTests`, the Ponder schematics of the Furnace basin, byproducts,
steel, tiers and woods scenes, `build.gradle` (the dev access transformer), `tools/gen_assets.py` and
`tools/create_fluid_textures.json` (Create's bracket, copper casing and Industrial Iron textures) and everything they
write, `gradle.properties` (0.15.0), README.md, DESIGN.md and BUILD_NOTES.md.

## 0.14.0: Power Grid, chemistry and aluminium

This covers section 18 of [BUILD_NOTES.md](BUILD_NOTES.md), stage three of building the idea bank, on top of 0.13.0,
with tools and armour melting in the crucible (the crucible thread's patch). Tested on 2026-10-21 against the same
versions as 0.11.0 plus Create: Power Grid 0.6.2 and Architectury 9.2.14, from this folder's source as it stood that day.

**Result:** 0.14.0 compiled with no errors (deprecation warnings only, none new in kind). Every GameTest passes in a
fresh test world: 185 of 185 plain and 191 of 191 with `-Pcompanions`. No world was played in a client, so the "In
game" list in section 18 is still to check by hand, the Ponder scenes included.

| Check | Result | How |
| --- | --- | --- |
| Build | Pass | `./gradlew build`: `mcremix-1.20.1-0.14.0.jar`, 3,038,586 bytes, SHA-256 `477cf11a…683bcac`; `builds/mcremix-1.20.1-0.14.0.zip` holds the same jar. Its `mods.toml` makes `powergrid` 0.6.2 or later mandatory, and it has no GameTest classes |
| GameTests, plain | Pass | 185 of 185: the 35 new ones listed below, and the Power Grid tests in `CompanionTests`, which now run in every plain run since Power Grid is always loaded |
| GameTests, with companions | Pass | 191 of 191 (see Test world for one earlier failure) |
| Coal tar, wood spirit, Bakelite | Pass | `cokingCoalGivesOffTar` (a pipe draws the tar off but cannot fill the Furnace), `tarWithNowhereToGoBurnsOff`, `charringLogsGivesOffWoodSpirit` (a Furnace already holding tar burns the spirit off and keeps its tar), `tarAndWoodSpiritPressIntoBakelite` |
| Power Grid's recipes | Pass | `powerGridPartsTakeSteelAndBakelite` (this mod's copies win: steel sheet in the motor, boards sawn from Bakelite, Bakelite in the switches, button and socket), `circuitBoardsAreEtchedInFerricChloride`, `washingRedSandTurnsUpBauxite` |
| Salt water | Pass | `aBucketDippedInTheSeaComesUpSalt`, `aBucketDippedInlandComesUpWater`, `aPumpDrawsSaltWaterFromTheSea`, `aPumpDrawsWaterInland`, `aHosePulleyDrawsSaltWaterFromTheSea` |
| Chlorine | Pass | `chlorineLetLooseBecomesACloud`, `chlorineChokesAndKillsPlants` (a pig chokes and a husk does not; grass under the cloud goes to dirt and its poppies die, and the rest lives), `ventedChlorineGathersIntoACloud` (one cloud, growing) |
| Bauxite to alumina | Pass | `bauxiteDissolvesInHotCausticSoda`, `redMudFiresIntoBricks` (bricks, block, stairs, slabs, walls, stonecutting, a double slab dropping two), `hydroxideFiresIntoAlumina`, `aluminaSplitsIntoMoltenAluminium` (an ingot's worth each, with no heat under the basin) |
| The Electrolyser | Pass | `saltWaterSplitsInABasin` (electrodes clicked in anode first; at 6 V the caustic soda stays and the chlorine escapes as a cloud; the cathode wears a tenth as fast), `anElectrolyserNeedsAbout4VoltsTheRightWayRound` (next to nothing at 3 V, nothing reversed), `aBurntOutAnodeIsGoneAndTheElectrolyserStops` (and starts again when a new one is fed in), `carbonElectrodesAreBakedFromCokeAndTar` |
| In series | Pass | `electrolysersInSeriesShareOneSource`: two on one vat across 10 V settle at once at the same current, over 10 A, about 5 V each; across 6 V under 1 A flows |
| Vats and settling | Pass | `aVatStandsOnAWholeLayerOfScaffolding` (one or two across; not on part of a layer or the ground; it breaks up when the scaffolding goes), `aVatSettlesABatchForEveryBucket` (four buckets of liquor give four hydroxide into a chest at once; pipes can take the soda back but not the liquor), `aBasinSettlesOnlyWhenLeftAlone` (not over a fed burner or under a mixer), `aVatKeepsItsGasesAndStopsWhenFull` (gases drawn off first; it starts again once there is room) |
| Hydrogen | Pass | `hydrogenIsFurnaceFuel` (burns Searing 100 mB at a time only while something needs firing; nothing else goes in by pipe), `escapedHydrogenHissesAway` (no flame, no fire) |
| Aluminium parts | Pass | `aluminiumCarries512WithNoDrag` (512 RPM past steel cogs, no friction in shaft, cog or large cog, and a soaked shaft does not weather), `aluminiumBreaksUnderHeavyLoad` (three presses at 256 RPM it carries, five break it), `aluminiumPartsMeltBesideLava` (it melts into rough aluminium where it stood; a copper shaft by lava and an aluminium one away from it stay) |
| Heat sinks | Pass | `heatSinksCoolAPowerGridDevice`: four resistors at 500 W; one with a sink runs cooler than a bare one, with a fan through the sink cooler still, and a sink beside one with its base turned away does nothing |
| Tools and armour melt | Pass | `gearMeltsIntoHalfItsMetal` (from the crucible thread's patch) |
| Wooden tanks renamed | Pass | `woodenTanksBurnWithLava` and `steelBoilersAreCompactAndWoodenTanksAreNoBoilers`, the 0.13.0 tests renamed |

### Bugs found and fixed

- **Basins hardly settled.** Settling ran from a hook at the end of the basin's tick, but Create's basin returns early
  on every tick its contents have not changed, so a basin left alone, the very case settling is for, settled only on
  the odd tick something changed. `aBasinSettlesOnlyWhenLeftAlone` caught it; the hook now runs at every return.
- **A burnt-out anode still showed.** The Electrolyser told its block about the change before the anode was gone, so
  the block kept drawing it. `aBurntOutAnodeIsGoneAndTheElectrolyserStops` caught it.
- **Electrolysers in series never settled.** Two in series on one source jumped between about 0.2 A and 37 A every
  few ticks. Power Grid's p-n junction holds its voltage at a critical value whenever a solver step crosses it, and two
  of them in series kept knocking each other back. The Electrolyser now uses the step limit circuit simulators use
  (SPICE's), and the pair settles at 27.2 A, 4.88 V each, on the first tick.

### Test world

Every run started from a deleted `run/gametest/world`. One run with companions failed `redMudFiresIntoBricks`: the
double slab it broke dropped nothing it could find. That run and two others passed everything else, and the earlier
companion run and every plain run passed it. The likely cause is that the test broke the slab on its first tick, when
its chunk's entities may not be loaded yet, so a search for the drop finds nothing. It now breaks the slab ten ticks
in, and the last plain and companion runs pass. `SpringTests` and `GearboxTests` each break a block on a test's first
tick too, and the 0.13.0 report's saw test failure may be the same thing.

### Files changed

New: `electrolysis/` (`ElectrolyserBlock`, `ElectrolyserBlockEntity`, `ElectrolysisRecipe`, `SettlingRecipe`,
`BasinElectrolysis`, `BasinSettling`, `CarbonElectrodeItem`, `Gases`, `Vat`, `VatHolder`), `heatsink/`
(`HeatSinkBlock`, `HeatSinkBlockEntity`), `fluids/Chemical.java`, `ChemicalFluidType.java`, `ChlorineCloud.java`,
`GasBlock.java`, `SeaWater.java`, `kinetics/AluminiumMelting.java`, `compat/jei/BasinSlots.java`,
`ElectrolysisCategory.java`, `SettlingCategory.java`, `ponder/ElectrolysisScenes.java`, `HeatSinkScenes.java`,
`TarScenes.java`, `registry/ModEntities.java`, `util/DownModelShapes.java`, the mixins `BasinBlockEntityAccessor`,
`FluidDrainingBehaviourMixin`, `FluidTankBlockEntityAccessor`, `FluidTankBlockMixin`, `OpenEndedPipeMixin` and
`ThermalBehaviourAccessor`, `data/powergrid/recipes/`, and `CoalTarTests.java`, `PowerGridRecipeTests.java`,
`ChlorineTests.java`, `SeaWaterTests.java`, `AluminaTests.java`, `HeatSinkTests.java`, `ElectrolysisTests.java`.
Renamed: `fluids/VatBlockEntity.java` to `WoodenTankBlockEntity.java`. Changed: `McRemix.java`,
`client/FluidPartModels.java`, `McRemixClient.java`, `ModSpriteShifts.java`, `compat/jei/KilnCategory.java`,
`KilnHeatCategory.java`, `RemixJeiPlugin.java`, `fluids/FluidPartMaterial.java`, `MoltenMetal.java`,
`RatedTankBlock.java`, `RatedTankItem.java`, `kiln/FiringRecipe.java`, `KilnBlock.java`, `KilnBlockEntity.java`,
`KilnProcessing.java`, `kinetics/KineticFamilies.java`, `KineticMaterial.java`, the mixins `BasinBlockEntityMixin`,
`FluidTankBlockEntityMixin`, `KineticBlockEntityMixin` and `SteamEngineBlockEntityMixin`,
`ponder/ModPonderPlugin.java`, `registry/ModBlockEntities.java`, `ModBlocks.java`, `ModFluids.java`, `ModItems.java`,
`ModRecipes.java`, the older tests that count parts or name wooden tanks (`ChainDriveTests`, `ClutchTests`,
`GearboxTests`, `FluidPartTests`, `PressureBoilerTests`), `SteelTests` and `CrucibleTests` (new tests),
`build.gradle` (Power Grid and Architectury required), `mods.toml`, `tools/gen_assets.py` and everything it writes,
`gradle.properties` (0.14.0), README.md, DESIGN.md and BUILD_NOTES.md.

## 0.13.0: Heat-rated pipes, pressure boilers and the crucible

This covers section 17 of [BUILD_NOTES.md](BUILD_NOTES.md), stage two of building the idea bank, on top of 0.12.0.
Tested on 2026-10-21 against the same versions as 0.11.0, from this folder's source as it stood that day.

**Result:** 0.13.0 compiled with no errors (deprecation warnings only, none new in kind). Every GameTest passes in a
fresh test world: 148 of 148 plain and 156 of 156 with `-Pcompanions -Ppowergrid`. No world was played in a client,
so the "In game" list in section 17 is still to check by hand.

| Check | Result | How |
| --- | --- | --- |
| Build | Pass | `./gradlew build`: `mcremix-1.20.1-0.13.0.jar`, 1,820,616 bytes, SHA-256 `61302bca…af8915`; `builds/mcremix-1.20.1-0.13.0.zip` holds the same jar. It has the refmap for the new mixins, the pipe, pump, tank, crucible and mold models and textures, the melting and alloying recipes and the crucible's Ponder schematic, and no GameTest classes |
| GameTests, plain | Pass | 148 of 148, among them the 27 new ones in `FluidPartTests`, `PatinaTests`, `PressureBoilerTests` and `CrucibleTests` |
| GameTests, with companions | Pass | 156 of 156 |
| Heat ratings | Pass | `fluidsAndPartsAreRatedForHeat` (water Cold; lava and molten zinc Hot; molten gold and copper Searing; molten iron and steel Blazing, still or running; wooden pipes and vats Cold, Create's copper parts, its spout and glass pipes Hot, steel Searing, a basin unrated) |
| Too hot, a part bursts | Pass | `vatsBurnWithLava` (an oak and a crimson vat of lava are gone by tick 140 and the lava spills where they stood), `copperTanksHoldLavaButNotMoltenGold`, `steelTanksHoldMoltenGoldButNotIron` (the burst steel tank drops whole in the iron it spilt), `lavaBurnsWoodenPipesButNotCopper` (pumped lava reaches the end of a copper line and burns out the oak pipe in the other), `moltenGoldNeedsSteelPipes` |
| Glass pipes and wooden pumps | Pass | `glassPipesCarryAQuarterAsFast` (one glass pipe in a line lets through less than half of what copper does), `woodenPumpsPumpAndSnapPastSixtyFour` (an oak pump fills a vat at 32 RPM and breaks at 128), `woodenPumpsFindNewPipes` (a pipe broken and built back beside an oak pump is pushed into again) |
| Copper patina | Pass | `onlyCreatesCopperPartsTakeAPatina` (Create's pipe, glass pipe, pump and tank start fresh and tick until oxidized or waxed; its creative tank, encased pipe and this mod's wooden and steel parts take none), `copperPartsWeatherLikeCopper`, `honeycombWaxesAndAnAxeScrapes`, `copperPartsDropAsTheyWere`, `wrenchedPipesKeepTheirPatina`, `weatheredPipesCarryAsMuch` |
| Pressure boilers | Pass | `steelBoilersAreCompactAndVatsAreNoBoilers` (four steel tanks make two levels where four copper make one; an engine runs on steel, not on a vat), `spareSteamBurstsABoilerWithNoWhistle` (one spare level builds pressure; at the burst its four tanks drop, a villager beside it is scalded and the glass beside it holds), `aWhistleVentsSpareSteam`, `aBalancedBoilerBuildsNoPressure` |
| Melting and alloying | Pass | `metalMeltsAtItsOwnHeat` (copper melts to 90 mB over a Searing burner, where raw iron waits unmelted; over a Blazing one raw iron melts to 90 mB), `itGoesInOnlyWhileItsMetalFits` (nine of twenty iron ingots go in and then not a nugget more, up to sixteen coke, no dirt, and nothing comes out), `metalsJoinAsAlloys` (copper and zinc make brass, iron and a coke make steel, gold and scrap make netherite) |
| Pouring and casting | Pass | `tippedItPoursIntoAMoldOnADepot` (a turning shaft tips it, the mold fills and sets, and an ingot breaks out of the sand), `pouredOnTheGroundItSets` (metal short of a bucket sets as rough metal holding all of it) |
| Big crucibles | Pass | `aCubeOfCruciblesJoinsIntoOne` (broken, it falls apart and shares out its metal), `aBigCrucibleNeedsHeatUnderEveryColumn` (it is as hot as its coolest column), `aBrokenCrucibleSetsItsMetal` (it drops what had not melted, and its metal sets where it stood) |

### Test world

Every run started from a deleted `run/gametest/world`. The first plain run failed one older test,
`sawsOnAPistonCutWhatTheyArePushedInto` ("STONE saw should drop both logs"); the next run, again on a fresh world,
passed all 148. Nothing about saws changed in this round, so that test looks sensitive to timing. Worth watching if it
fails again.

### Files changed

New: `fluids/` (`FluidHeat`, `FluidPartMaterial`, `Overheating`, `Spills`, `Patina`, `PatinaEvents`,
`KeepPatinaModifier`, `PressureBoilerData`, `RatedPipeBlock`, `RatedPumpBlock`, `RatedTankBlock`, `RatedTankItem`,
`VatBlockEntity`, `SteelTankBlockEntity`, `MoltenMetal`, `MoltenMetalType`, `MoltenMetalFluid`, `MoltenMetalBlock`,
`RoughMetalBlock`, `RoughMetalBlockEntity`), `crucible/` (`CrucibleBlock`, `CrucibleBlockEntity`, `CrucibleRecipes`,
`MeltingRecipe`, `AlloyingRecipe`, `SandMoldItem`, `CrucibleArmPointType`), `client/CrucibleRenderer.java`,
`CrucibleEffects.java`, `FluidPartModels.java`, `SpriteSwapModel.java`, `GlassPipeRenderer.java`,
`RatedPumpRenderer.java`, `compat/jei/MeltingCategory.java`, `AlloyingCategory.java`, `ponder/CrucibleScenes.java`,
`registry/ModFluids.java`, `ModFluidBlocks.java`, the mixins `BlockMixin`, `CopperFluidBlockMixin`,
`FluidNetworkMixin`, `FluidPipeBlockMixin`, `FluidPropagatorMixin`, `FluidTankBlockAccessor`,
`FluidTankBlockEntityMixin`, `FluidTransportBehaviourMixin`, `GlassFluidPipeBlockMixin`, `HoneycombItemMixin`,
`PipeConnectionMixin`, `SmartFluidTankBehaviourMixin`, `SteamEngineBlockEntityMixin` and `WhistleBlockEntityMixin`,
`tools/create_fluid_textures.json`, and `FluidPartTests.java`, `PatinaTests.java`, `PressureBoilerTests.java`,
`CrucibleTests.java`. Changed: `McRemix.java`, `client/ClientEvents.java`, `McRemixClient.java`,
`ModPartialModels.java`, `compat/jei/JeiText.java`, `RemixJeiPlugin.java`, `kinetics/Weathering.java`,
`ponder/ModPonderPlugin.java`, `registry/ModArmPoints.java`, `ModBlockEntities.java`, `ModBlocks.java`,
`ModItems.java`, `ModLootModifiers.java`, `ModRecipes.java`, `ModTags.java`, `mcremix.mixins.json`,
`tools/gen_assets.py` and everything it writes, `gradle.properties` (0.13.0), README.md, DESIGN.md and
BUILD_NOTES.md.

## 0.12.0: Steel, the Steel Spring and every wood

This covers section 16 of [BUILD_NOTES.md](BUILD_NOTES.md), stage one of building the idea bank, on top of 0.11.1.
Tested on 2026-10-20 against the same versions as 0.11.0, from this folder's source as it stood that day.

**Result:** 0.12.0 compiled with no errors (deprecation warnings only, none new in kind). Every GameTest passes in a
fresh test world: 121 of 121 plain and 129 of 129 with `-Pcompanions -Ppowergrid`. No world was played in a client,
so the "In game" list in section 16 is still to check by hand.

| Check | Result | How |
| --- | --- | --- |
| Build | Pass | `./gradlew build`: `mcremix-1.20.1-0.12.0.jar`, 1,417,476 bytes, SHA-256 `d820a395…b30d31`; `builds/mcremix-1.20.1-0.12.0.zip` holds the same jar. It has the spring's blockstate, models and textures, the pack's recipe and firing, and both Ponder schematics |
| GameTests, plain | Pass | 121 of 121, among them the 18 new ones in `SteelTests`, `WoodTests` and `SpringTests` |
| GameTests, with companions | Pass | 129 of 129 |
| Steel to 512 RPM | Pass | `steelCarries512WhereIronBreaks` (Create's cap reads 512; cogs double 256 to 512, and a steel shaft, even a waxed rusted one, holds it where an iron one breaks), `brassEncasedShaftsKeepTheAndesiteLimit` (Create's brass encased shaft breaks at 192 RPM), `steelIsTheHeaviestToTurn` (0.25 su per RPM clean, 0.5 exposed, 0.75 weathered, 2.25 rusted, against iron's 0.125) |
| Steel rusts like iron | Pass | `steelRustsOnlyWhenWet`, `rustRunsDownBetweenIronAndSteel` |
| Steel recipes | Pass | `steelRecipes` (shafts, gearbox, chain drive, nuggets, waxing; the press makes ingots from blister steel and sheets from ingots; a sheet on a stripped log makes casing; blister steel crafts into nothing) |
| Every wood | Pass | `everyWoodHasItsOwnParts` (44 parts with wood's limits, their log's sound and map colour, burning unless crimson or warped, cut with an axe), `everyWoodBreaksAbove64Rpm`, `woodenPartsAreMadeOfOneWood` (each wood's recipes, no mixing, stripped wood works, a bellows takes a cherry shaft), `oldWoodenPartsLoadAsOak` |
| Spring winds and slips | Pass | `springWindsWhileItsShaftTurns` (about a turn a second at 64 RPM, 8 su per RPM on the network, the shaft runs on through, windows and comparator at 1), `springSlipsWhenFullOrTurnedTheWrongWay` (full: no more turns and no stress, windows 4, comparator 15; turned the other way: keeps its turns) |
| Spring lets go by Hooke's law | Pass | `letGoItTurnsBackAtHookesLawSpeed` (half wound 16 RPM, a quarter 8, a hundredth of a turn 1, slack 0, at a constant of 32; let go full it turns the other way at 32 RPM with 8 su per RPM to give, and has slowed by tick 150), `springRunsDownAndStops` (its last twentieth of a turn at 1 RPM, then it stops and reads slack) |
| Spring under load and beside a motor | Pass | `overloadedSpringHoldsItsTurns` (two presses overstress it and it keeps its turns), `springGivesWayToAnotherSource` (let go while a motor turns it, it keeps its turns and nothing breaks; with the motor gone it drives its shaft at 16 RPM) |
| Spring and pack recipes, firing | Pass | `springAndCementationPackRecipes` (the spring recipe; the pack in any order and not without clay; the pack fires into four blister steel at Searing, slower than a bloom; a broken spring drops itself), `cementationPackFiresIntoBlisterSteel` (a coke-fired Furnace turns a pack into four blister steel) |

### Test world

The first runs of this round reused `run/gametest/world` and failed `mobsDropVanillaXp`, `fannedCampfireCooksTwiceAsFast`,
`sawsOnAPistonCutWhatTheyArePushedInto` and `stuckDrillStartsAgainWhenTheBlockChanges`: the zombie dropped 10, then 15
XP, and 4, then 6 cooked beef lay round the campfires, one more lot for every run. Entities saved in the old world load
after a test has cleared its space, so each run's drops piled into the next run's tests once the 18 new tests had
moved every test to a new place. With the world deleted, all four pass. Nothing in the mod changed for them.

### Files changed

New: `kinetics/WoodSpecies.java`, `kinetics/OldWoodenParts.java`, `kinetics/SpeedCap.java`,
`kinetics/SteelSpringBlock.java`, `kinetics/SteelSpringBlockEntity.java`, `client/SteelSpringRenderer.java`,
`client/ModSpriteShifts.java`, and `SteelTests.java`, `WoodTests.java`, `SpringTests.java`. Changed:
`kinetics/KineticMaterial.java`, `KineticVariant.java`, `KineticFamilies.java`, `TieredShaftBlock.java`,
`TieredGearboxBlock.java`, `TieredClutchBlock.java`, `TieredGearshiftBlock.java`, `TieredKineticBlockEntity.java`,
`Weathering.java` (`isIron` counts steel), `client/ModPartialModels.java`, `client/TieredKineticVisual.java`,
`config/RemixConfig.java` (`raiseCreateSpeedCap`, `springTurns`), `registry/ModBlocks.java`, `ModBlockEntities.java`,
`ModItems.java`, `ponder/KineticScenes.java`, `HeatScenes.java`, `ModPonderPlugin.java`, `tools/gen_assets.py` and
everything it writes, `GearboxTests.java`, `ClutchTests.java`, `ChainDriveTests.java`, `KineticTests.java`,
`gradle.properties` (0.12.0), README.md, DESIGN.md and BUILD_NOTES.md.

## 0.11.1: Rust runs down

This covers section 15a of [BUILD_NOTES.md](BUILD_NOTES.md): iron rusts while the iron on top of it is rustier, even
when dry. Tested on 2026-10-20 against the same versions as 0.11.0, from this folder's source as it stood that day.

**Result:** 0.11.1 compiled with no errors (the same 11 deprecation warnings as 0.11.0) and needed no fixes. The plain
run passed 103 of 103 GameTests and `-Pcompanions -Ppowergrid` passed 111 of 111. No world was played in a client.

| Check | Result | How |
| --- | --- | --- |
| Build | Pass | `./gradlew build`: `mcremix-1.20.1-0.11.1.jar`, 1,039,991 bytes, SHA-256 `8151cf65…f2984c`; `builds/mcremix-1.20.1-0.11.1.zip` holds the same jar |
| GameTests, plain | Pass | 103 of 103. `rustRunsDownOntoDryIron` logged its dry column under rusted iron at stages [3, 2, 2, 2] from the top |
| GameTests, with companions | Pass | 111 of 111, among them `immersiveWeatheringRustRunsDown`. The column came out [2, 1, 1, 1] in this world: each spot's final stage comes from the world seed |

### Files changed

`kinetics/Weathering.java` (`rustierAbove`, `isIron`, and both `randomTick` and `rust` use them),
`config/RemixConfig.java` (config comments), `WeatheringTests.java` (`rustRunsDownOntoDryIron`, and one more check in
`weatheringCanBeSwitchedBack`), `CompanionTests.java` (`immersiveWeatheringRustRunsDown`), `gradle.properties`
(0.11.1), README.md, DESIGN.md and BUILD_NOTES.md.

## 0.11.0: Furnace-heated basins, chain drive tiers and the Furnace

This covers sections 13 to 15 of [BUILD_NOTES.md](BUILD_NOTES.md) (0.9.0, 0.10.0 and 0.11.0), built on 0.8.1 and
0.8.2. Tested on 2026-10-20 with Forge 47.4.6, Create 6.0.8, Flywheel 1.0.6-beta-266, Ponder 1.0.91 and JEI 15.20.0.106,
from this folder's source as it stood that day.

**Result:** 0.11.0 compiled on the first try with no errors (11 deprecation warnings, all older than this round) and
needed no fixes. The plain run passed 102 of 102 GameTests and `-Pcompanions -Ppowergrid` passed 109 of 109, the counts
the status paragraph in BUILD_NOTES gives. No gravel fell on a test this time. The built jar also loads on a Forge
server with every companion mod. No world was played in a client, so the "In game" lists in sections 13 to 15 are still
to check by hand.

### Checks

| Check | Result | How |
| --- | --- | --- |
| Build | Pass | `./gradlew build`: `mcremix-1.20.1-0.11.0.jar`, 1,039,650 bytes, SHA-256 `919e1407…b2ea7e` |
| The kiln's clay recipe is gone | Pass | The jar has no `data/mcremix/recipes/kiln.json`, and none exists in the source. `data/minecraft/recipes/furnace.json` and `assets/minecraft/lang/en_us.json` are in the jar |
| GameTests, plain | Pass | 102 of 102, among them every test in `KilnBasinTests`, `ChainDriveTests`, `FurnaceTests` and `WeatheringTests` (102 `@GameTest` methods in the source, all run) |
| GameTests, with companions | Pass | `-Pcompanions -Ppowergrid`: 109 of 109 |
| `BasinBlockEntityMixin` applies | Pass | No mixin errors in the dev runs or in the reobfuscated jar on the server below; `kilnHeatsTheBasinOnTop` and `kilnHeatedBasinMixesBrass` pass, and both depend on it |
| The jar on a real server | Pass | The Forge server earlier rounds used (Create, Power Grid 0.6.2, Immersive Weathering 2.0.5, Better Combat, WWOO, Moonlight) with the 0.11.0 jar in place of 0.8.0, on the world 0.8.0 made. It starts with no errors from this mod. The Furnace, a copper chain drive, an iron chain gearshift, a waxed rusted iron chain drive and a basin on a Furnace all place, and the Furnace under the basin saves `BasinHeat`. Forge adds the `weathering` config keys to the old world's server config at their defaults (`ironRustsWhenDry = false`) |

### Files changed

None. Nothing in the 0.9.0 to 0.11.0 code needed fixing.

## 0.8.2: Iron rusts only while wet

Taran chose on 2026-10-20 to have all iron rust only while wet. This covers the 0.8.2 changes to section 12a of
[BUILD_NOTES.md](BUILD_NOTES.md) and reruns every test. Tested on 2026-10-20 against the same versions as 0.8.1.

**Result:** 0.8.2 compiled with no errors, and both new Immersive Weathering checks pass. The plain run passed 93 of 93
GameTests. The first `-Pcompanions -Ppowergrid` run passed 99 of 100, because gravel from the test world's roof fell
on the older `drillOnAPistonStopsAtWhatItCannotCut` (see "Checks"); a rerun on a new world passed 100 of 100. The
jar loads on a Forge server with Create and Immersive Weathering, and a world that ran 0.8.1 gets the new default.
No world was played in a client.

**What changed:** with `fastWeathering` on, Immersive Weathering's iron (cut and plate iron, iron doors, trapdoors and
bars) now rusts only while wet, as this mod's iron parts always have: rained on, waterlogged or beside water. The
0.8.1 setting `dryIronRusts` (default true) is now `ironRustsWhenDry` (default false). The new name is for worlds
that ran 0.8.1: their server config holds `dryIronRusts = true`, which a changed default alone would keep.

**Built from a separate copy:** the 0.8.2 jar was built from the 0.8.1 copy this thread tested, not from the merged
0.11.0 source in this folder, which had not been compiled. The same change is in this source (the files below), so
the next build carries it. With the change, this folder's 0.11.0 source compiles (`compileJava` and
`compileGametestJava`); its tests were not run.

### Checks

| Check | Result | How |
| --- | --- | --- |
| Immersive Weathering's iron rusts while wet, each block to a stage of its own, and stays clean while dry | Pass | `immersiveWeatheringIronRustsOnlyWhileWet` (with IW): an iron door beside water, plate iron beside water, and a waterlogged cut iron slab, iron bars and iron trapdoor, 300 random ticks each, all at their final stage, the door's upper half with it and waterlogging kept; a dry iron door (both halves), dry plate iron and dry iron bars stay clean |
| `ironRustsWhenDry = true` | Pass | `dryIronRustsWhenSwitchedOn` (with IW, its own batch): a dry iron door and dry plate iron rust to their final stage, the door's upper half with it; a dry iron shaft of this mod stays clean; the setting is back off afterwards |
| A world that ran 0.8.1 gets the new default | Pass | The Forge server 0.8.1 was loaded on (Create 6.0.8, Immersive Weathering 2.0.5, Moonlight), on the world 0.8.1 made, whose server config held `dryIronRusts = true`. 0.8.2 loads with no errors; Forge logs that it removed `dryIronRusts` and set `ironRustsWhenDry` to its default, and the file now holds `ironRustsWhenDry = false` |
| Everything else | Pass, see note | Every other GameTest passes, the 0.8.1 weathering tests included. In the first companion run `drillOnAPistonStopsAtWhatItCannotCut` failed: the saved world showed a heap of gravel on its iron rig and a hole in the roof above it, the same cause as the `slagWashesIntoNuggets` failures in 0.8.1. Gravel in the roof of the space the tests are cut into can land on any test in a fresh world; a flat game-test world would stop it |

### Files changed

For the next build, against 0.8.1. All of them except `gradle.properties` are already changed in this folder.

- `gradle.properties`: `mod_version=0.8.2`, in the 0.8.1 copy only; this folder stays at 0.11.0.
- `src/main/java/com/mcremix/config/RemixConfig.java`: `IRON_RUSTS_WHEN_DRY` (`ironRustsWhenDry`, default false)
  replaces `DRY_IRON_RUSTS` (`dryIronRusts`, default true), and the `fastWeathering` comment.
- `src/main/java/com/mcremix/kinetics/Weathering.java`: `onlyWhenWet` reads the new setting; the class comment.
- `src/gametest/java/com/mcremix/gametest/CompanionTests.java`: `immersiveWeatheringIronRustsOnlyWhileWet` and
  `dryIronRustsWhenSwitchedOn` replace `immersiveWeatheringIronRustsToAStageOfItsOwn` and `dryIronCanBeKeptClean`,
  and their two registrations in `companionModTests`.
- `README.md` (the Weathering paragraph), `docs/DESIGN.md` (Weathering, the Iron point), `docs/BUILD_NOTES.md` (a
  0.8.2 status paragraph and section 12a) and `docs/TEST_REPORT.md` (this report).

The tested jar is `builds/mcremix-1.20.1-0.8.2.jar` (SHA-256
`78871815c160baac2242367f8f4822e5b1f76da09c294f8332f84f302013e8b5`); `builds/mcremix-1.20.1-0.8.2.zip` holds the same
jar for browsers that block .jar downloads.

## 0.8.1: Fast, varied weathering

Covers section 12a of [BUILD_NOTES.md](BUILD_NOTES.md), and reruns every earlier test. Tested on 2026-10-20 against
Forge 47.4.6, Create 6.0.8-291, Flywheel 1.0.6-beta-266 and, with `-Pcompanions`, Immersive Weathering 2.0.5.

**Result:** 0.8.1 compiled with no errors, every check in section 12a that a GameTest can make passes, and the jar
loads on a Forge server with Create and Immersive Weathering. The last plain run passed 93 of 93 GameTests and the
last `-Pcompanions -Ppowergrid` run 99 of 100. The one failure was the older `slagWashesIntoNuggets`, which gravel
falling into the test can break, as "Earlier tests" explains; an earlier companion run passed 100 of 100. No world
was played in a client this round.
0.8.1 is 0.8.0 plus weathering, because 0.9.0 to 0.11.0 are still waiting in the writer's copy; see "Files changed"
for merging.

### How it was tested

- **GameTests:** 6 new tests in `WeatheringTests` and 2 in `CompanionTests` that need Immersive Weathering. A random
  tick in them is one call to `BlockState.randomTick`, the call the world makes, except in
  `chunkRandomTicksWeatherCopper`, which ticks a whole chunk through `ServerLevel.tickChunk`. A day at the default
  random tick speed is about 18 random ticks per block (3 per 16x16x16 section per tick, 24,000 ticks). The counts
  below are from one run; each new test world has its own seed, which moves them a little.
- **A Forge 47.4.6 server** with the 0.8.1 jar, Create 6.0.8 (the Modrinth release), Immersive Weathering 2.0.5 and
  Moonlight, on a new world: it loads with no errors or mixin warnings, writes the new `weathering` section into the
  world's server config, and stops cleanly. Random ticks only happen near a player, so nothing weathered there.
- **A preview**, not a screenshot: [weathering-preview](../../builds/weathering-preview.png) draws a 20 by 9 copper
  wall as built, half a day later and a day later, with vanilla's textures, from the same final-stage formula (checked
  against Java's arithmetic) and the configured chance per random tick.

### Section 12a: fast, varied weathering

| Check | Result | How |
| --- | --- | --- |
| A packed copper build weathers within a day, a stage at a time, at the configured pace | Pass | `copperBuildsWeatherWithinADayIntoAMix`: a 14 by 14 floor of copper blocks, all side by side, which vanilla would hold back until its slowest block caught up. After 18 random ticks (a day) 192 of 196 are at their final stage (192 in the companion run too, whose spots differ); after 200 every block is, none skipped a stage, and a stage took 2.92 random ticks on average (a chance of 0.3420 a tick against 0.3413 configured) |
| Every copper family weathers and keeps its shape; waxed blocks do not | Pass | `everyCopperFamilyWeathersAndKeepsItsShape`: vanilla's cut copper, cut copper stairs (east, top half) and a waterlogged bottom slab; Create's copper shingles, tile stairs and a double shingle slab; a copper cogwheel (Z axis) and an exposed copper gearbox. Each ends at its spot's final stage in the same family with every property kept. Waxed copper, Create's waxed copper tiles and a waxed copper shaft stay as they are |
| Final stages: a third each, never fresh, no patches or stripes; fixed for the spot | Pass | `finalStagesAreFixedAndEvenlyMixed`: over 16,384 spots, 5,472 exposed, 5,444 weathered and 5,468 oxidized, none fresh; side-by-side neighbours alike 33.2% of the time (33.7% in the companion run; a third expected). A block at its final stage stays there for 500 more random ticks; scraped back a stage with an axe, it weathers to the same block again |
| This mod's iron parts rust only while wet, to their final stage | Pass | `ironRustsOnlyWhileWetToAStageOfItsOwn`: a dry iron shaft and gearbox stay clean after 300 random ticks; a waterlogged iron shaft and an iron large cogwheel beside water rust to their final stage, the shaft still waterlogged |
| Copper is not held back by dry iron beside it | Pass | `dryIronDoesNotHoldBackCopper` and `copperGearboxesWeatherIronOnlyWhenWet` (both older, now checking final stages) |
| The world's own random ticks do it | Pass | `chunkRandomTicksWeatherCopper`: 196 cut copper blocks in one chunk, 40 chunk ticks at random tick speed 4096 (one random tick per block each): all 196 at their final stage, none past it |
| Immersive Weathering's iron rusts within a day, wet or dry, each to a stage of its own | Pass | `immersiveWeatheringIronRustsToAStageOfItsOwn` (with IW): two iron doors (one beside water), plate iron (wet and dry), iron bars (waterlogged and dry), a waterlogged cut iron slab and a waterlogged iron trapdoor, 300 random ticks each: all at their final stage, each door's upper half the same block as its lower half, waterlogging kept |
| `dryIronRusts = false` | Pass | `dryIronCanBeKeptClean` (with IW, its own batch): a dry iron door and dry plate iron stay clean; plate iron beside water still rusts to its final stage; the setting is back on afterwards |
| `variedWeathering = false`, `fastWeathering = false` | Pass | `weatheringCanBeSwitchedBack` (its own batch): with variety off, 14 copper blocks all reach oxidized copper. With fast weathering off, vanilla's rule is back (an exposed copper block beside a fresh one and an exposed copper shaft stay exposed for 500 random ticks), an exposed copper block beside a dry iron shaft and gearbox still oxidizes, and a waterlogged iron shaft rusts all the way. Both settings are back on afterwards |
| Ponder text | Pass, see note | The kinetic materials scene's copper line now reads "Copper parts oxidize like copper blocks, within a day, each stopping at a stage of its own"; the lang line was regenerated with `gen_assets.py`. Not played in a client |
| A build in a played world, in rain and in the dry | Not checked | No world was played this round; the server above only loaded |

### Earlier tests on 0.8.1

All 87 earlier tests pass, and the 5 earlier companion tests, except that `slagWashesIntoNuggets` (BloomTests)
failed in 2 of the 7 runs that started from a fresh world, its slag never washed. To find out why, 40 copies of it ran
among the other tests: one failed the same way, and the saved world showed gravel in its water channel. The game-test
world is a normal world with a random seed, and the tests sit in a space cut out of deepslate; gravel in the roof
above that copy had fallen in, stopping the water and cutting the fan's air short of the slag. Nothing in that test
weathers, so the new rules play no part, and any test can be hit the same way. Not fixed here: a flat test world, or a
lid over the channel, should stop it. One companion run also failed to start (Registrate reported unused register
callbacks while loading); the next one started normally.

Two earlier tests checked weathering in ways the new rules change, and now check for each block's own final stage
instead:

- `dryIronDoesNotHoldBackCopper` (KineticTests) and the second half of `copperGearboxesWeatherIronOnlyWhenWet`
  (GearboxTests) started copper at exposed and expected it to move on, and expected wet iron to end fully rusted. A
  spot whose final stage is exposed now stops there, and the first run failed on exactly that: a copper block at such a
  spot stayed exposed. Vanilla's neighbour rule, which these tests were written for, is now checked with
  `fastWeathering` off in `weatheringCanBeSwitchedBack`.

### Running the tests

```
./gradlew runGameTestServer                            # 93 tests
./gradlew runGameTestServer -Pcompanions -Ppowergrid   # 100 tests, with IW, WWOO, Better Combat and Power Grid
```

Delete `run/gametest/world` first for a fresh world, as the last runs here did.

### Files changed

For merging into the 0.9.0 to 0.11.0 copy. Every change is listed; nothing else differs from the 0.8.0 copy this round
started from.

- `gradle.properties`: `mod_version=0.8.1`.
- `src/main/java/com/mcremix/kinetics/Weathering.java`: new `randomTick`, `stage`, `onlyWhenWet`, `next`,
  `finalStage` and `chancePerRandomTick`, three constants, the `RemixConfig` import and the class comment;
  `mayWeather`, `rust` and `isWet` are unchanged. It stays in `kinetics/`, so classes that already use it (the
  writer's chain drives, if they do) compile as before, and any `TieredKinetic` part weathers by the new rules.
- `src/main/java/com/mcremix/mixin/BlockStateBaseMixin.java`: new; `src/main/resources/mcremix.mixins.json` lists it.
- `src/main/java/com/mcremix/config/RemixConfig.java`: a `weathering` section between `kinetics` and `heat`, with
  `fastWeathering`, `weatheringDays`, `variedWeathering` and `dryIronRusts`.
- `src/main/java/com/mcremix/ponder/KineticScenes.java`: one text in the kinetic materials scene, and its line in
  `src/main/resources/assets/mcremix/lang/en_us.json` (`mcremix.ponder.kinetic_tiers.text_3`). `gen_assets.py`
  also rewrote all 130 PNGs with the same pixels in different bytes (this machine's Pillow encodes them differently);
  those were put back, so only the lang line changed.
- `src/gametest/java/com/mcremix/gametest/`: new `WeatheringTests.java`; `CompanionTests.java` (two IW tests and
  their helpers `ironBlock`, `ironDoor` and `boxedWater`, registered in `companionModTests`, and the `RemixConfig`,
  `RandomSource`, `ChangeOverTimeBlock`, `DoorBlock`, `BlockStateProperties` and `DoubleBlockHalf` imports);
  `KineticTests.java` and `GearboxTests.java` (final-stage checks, above).
- `README.md` (the 0.8.1 heading, a Weathering paragraph, one sentence in Shaft and cog materials, the layout),
  `docs/DESIGN.md` (a Weathering section, the copper and iron weathering cells in the materials table, and one clause
  each in the Create and Immersive Weathering integrations), `docs/BUILD_NOTES.md` (a 0.8.1 status paragraph and
  section 12a) and `docs/TEST_REPORT.md` (this report).

Not changed: the roadmap line in `docs/DESIGN.md`, which the writer's copy has moved on.

The tested jar is `builds/mcremix-1.20.1-0.8.1.jar` (SHA-256
`ae7b56b6b147700bf50648304bcb27dd61c0b8601e7bc93f818ee6381844ca40`); `builds/mcremix-1.20.1-0.8.1.zip` holds the same
jar for browsers that block .jar downloads.

### Not covered

- A played world: how a weathered build looks in game, rain on outdoor iron, and blocks far from players (random
  ticks only happen in chunks near a player, as in vanilla).
- Server performance with many copper blocks: not measured. Every random tick of every block now passes one
  `instanceof` check, and copper and IW iron one class-name comparison more.
- Mods that replace random ticking without calling `BlockState.randomTick`; their worlds would keep each block's own
  rules.


## 0.8.0: Slag roads, kiln boilers, ruined villages, saw blades, clutches and gearshifts

Covers sections 8, 9, 11 and 12 of [BUILD_NOTES.md](BUILD_NOTES.md), and reruns every earlier test. Tested on
2026-10-15 against Forge 47.4.6, Create 6.0.8-291, Flywheel 1.0.6-beta-266 and JEI 15.20.0.106.

**Result:** 0.5.0 to 0.8.0 compiled with no errors the first time, and everything checked in sections 8, 9, 11 and 12
works; four checks were not done (monsters in villages at night, WWOO villages, and a belt or a Mechanical Arm feeding
a saw). One bug turned up and is fixed in this copy: ruined villages hung cobwebs outside, under eaves and on porches.
Taran's "Fires only" answer to last round's question is in too. The new GameTests needed five fixes to their own setup
and two older tests were made steadier; none of those were bugs in the mod.

### How it was tested

- **GameTests:** 25 new tests for sections 8, 9, 11 and 12 (`RoadTests`, `BoilerTests`, `RuinTests`, `SawTests`,
  `ClutchTests`), and 3 more in `CompanionTests` that need Immersive Weathering or Power Grid.
  `./gradlew runGameTestServer` runs 87 and passes all of them; with `-Pcompanions -Ppowergrid` it runs 92 and passes
  all of them.
- **Dev client** with IW, WWOO and Better Combat (`./gradlew runClient -Pcompanions`), in last round's creative world
  upgraded to 0.8.0: rendering, goggles, tooltips, JEI, Ponder, saws on bearings and pistons, and a newly generated
  plains village, with Flywheel on and with its backend off (`/flywheel backend flywheel:off`). Screenshots are in
  [builds/screenshots](../../builds/screenshots).
- **A Forge server** with the 0.8.0 jar, every companion mod (IW, WWOO, Better Combat and Power Grid) and no JEI,
  upgrading last round's server world. It loads, and every console check passes: a golden saw, a wooden clutch, a
  waxed weathered iron gearshift, a slag road and a stone saw place. Eight draws from the plains house pool with
  `/place jigsaw` placed seven pieces; the other draw failed, as the command does when it picks the pool's empty entry
  (10 of the pool's 87 weight). None of the seven has a torch; six have mossy cobblestone, three have cobwebs (four in
  all), and the seventh is a small farm, which has nothing to ruin but its wheat. No villagers, zombie villagers or iron
  golems were around them. Forge logs that the world's server config lacked the new `ruinedVillages` entry and still had
  the removed `experience` section, and corrects both.

### Section 8: slag roads and kiln boilers

| Check | Result | How |
| --- | --- | --- |
| Slag on a dirt path makes a road and is used up (not in creative); on grass, crops and saplings it is still bone meal; a Deployer lays roads | Pass | GameTests `slagMetalsAPathIntoARoad` (one slag used, none in creative; grass takes it as bone meal; a sapling grows within 20 tries, since bone meal only sometimes grows one; plain dirt does nothing) and `deployerWithSlagLaysRoads`. The crunch, grit and ash were not seen or heard |
| Looks right at path height; not see-through; no suffocation | Pass | Client: a dark cinder top with glassy chips and rust specks, level with a dirt path beside it. GameTest `slagRoadBehavesLikeAPath` (path height, solid to light and to the view). [slag-road](../../builds/screenshots/slag-road.png) |
| A quarter faster for players and what they ride, through jumps, ending a step off the road; a riderless horse or a zombie gets nothing | Pass | GameTest `roadSpeedsUpPlayersAndWhatTheyRide`: speed × 1.25 exactly, kept through a jump, gone a step off the road; a ridden horse speeds up and loses it when left standing; a riderless horse and a zombie get nothing. The wider view was not looked at |
| A block on it turns it to dirt; hoes leave it; no mob spawns; drops | Pass | GameTest `slagRoadBehavesLikeAPath`: covered, or placed under a block, it becomes dirt; a hoe leaves it; zombies cannot spawn on it; a shovel drops dirt and one slag, Silk Touch the road |
| JEI pages | Pass | Client: the Slag Road has its info page. The slag page's new sentence ("Used on a dirt path, it makes a Slag Road") was read in the lang file. [jei-slag-road](../../builds/screenshots/jei-slag-road.png) |
| A kiln under a boiler with an engine: charcoal simmers it, coke or fanned charcoal boils it at heat 1, and fanned charcoal burns twice as fast; goggles | Pass, see note | GameTest `kilnBoilsABoilerLikeABlazeBurner`: coke heat 1, charcoal passive, fanned charcoal 1, with the kiln's goggle lines (boiling, simmering with the hint, none without an engine). `bellowsBlowingIntoAKiln`: in 40 ticks fanned charcoal burns 80 ticks of fuel, plain charcoal 40. Client goggles: "Boils the boiler above like a fed Blaze Burner" on coke, "Keeps the boiler above simmering, as a campfire would" and the hint on charcoal. The engine's speed was not watched: the client's boiler had no water pumped in, which Create's boiler needs to run an engine. [kiln-boiler-coke](../../builds/screenshots/kiln-boiler-coke.png), [kiln-boiler-charcoal](../../builds/screenshots/kiln-boiler-charcoal.png) |
| A soul campfire under the kiln only simmers it; a superheated blaze burner gives 2 | Pass | Same GameTest: passive and 2, the second with "Boils the boiler above like a superheated Blaze Burner" |
| Without an engine the kiln keeps its coke; with one it burns it with an empty chamber | Pass | Same GameTest: with an engine, burning with 3 coke left; without, 4 left and not burning. How fast the engine slows once the coke runs out was not watched |
| Chimney | Pass | GameTest `kilnChimneyFollowsTheBlockAbove`: gone under a stone block, a bottom slab and a fluid tank, and when the kiln is placed under a block; standing under a torch and a hopper; back when the block above is broken. Client: gone under the tank, standing on a bare kiln. [kiln-chimney](../../builds/screenshots/kiln-chimney.png) |
| Ponder | Pass | Client: "Slag roads" plays to the end with its text (the road is laid, then the two horses race). "Heating a steam boiler with a Kiln" plays with its text; the gauge is too small at this screen size to count its notches. [ponder-slag-roads](../../builds/screenshots/ponder-slag-roads.png), [ponder-kiln-boiler](../../builds/screenshots/ponder-kiln-boiler.png) |

### Section 9: ruined villages

| Check | Result | How |
| --- | --- | --- |
| About a third of the cobblestone mossy; stone bricks mossy or cracked | Pass | GameTest `villagePiecesGenerateAsRuins`: in 400 rolls, 142 cobblestone came out mossy, and of 400 stone bricks 142 mossy and 54 cracked. `plains_small_house_1` placed the way a village places it has 9 mossy of its 43 cobblestone. Client and server: mossy cobblestone through the plains houses. Only plains villages were generated |
| Glass panes: some windows empty | Pass | Same GameTest: 138 of 400 panes gone (four in ten expected). The arm a pane keeps into the hole was not looked at |
| No torches; bare lamp posts; cold campfires; carved pumpkins; half the lanterns and potted flowers gone | Pass | GameTests `ruinProcessorDecaysBlocks` (torches, wall torches and soul torches become cave air; a lit campfire goes cold and a jack o'lantern becomes a carved pumpkin, each facing the same way; planks, beds, doors, chests, bells, hay, composters, smithing tables and sandstone are left alone) and `villagePiecesGenerateAsRuins` (174 of 400 lanterns gone, 174 of 400 potted flowers dead, the lamp post's four torches gone). Server: no torch in seven houses |
| Cobwebs in the top corners of rooms, never outside | Pass after fix | Client: cobwebs hung outside a house, under its eaves (bug 1). New GameTest `cobwebsHangOnlyInsideRooms`: 73 cobwebs in 400 room corners and none on the porches; the old code put 17 on the porches. [village-cobweb-outside](../../builds/screenshots/village-cobweb-outside.png) shows the bug |
| Crops at mixed ages, about a quarter grass | Pass | `villagePiecesGenerateAsRuins`: wheat at every age from 0 to 7, 84 of 400 turned to grass. Bone meal on a ruined crop was not tried (they are ordinary crop blocks) |
| Beds, bells, doors, workstations, chests and hay intact; no villagers, zombie villagers or golems | Pass, see note | GameTests: the small house keeps its two doors, bed, planks, stairs and logs. Client: none of the three in the village, though a cat was there (below). Server: none around seven houses |
| A house cut by a chunk border decays the same on both sides | Pass | `villagePiecesGenerateAsRuins`: the same block in the same place always decays the same way, since every roll is seeded by position. Not looked for in the client |
| At night, monsters spawn in the dark streets | Not checked | |
| `ruinedVillages = false` | Pass | GameTest `ruinedVillagesCanBeSwitchedOff`: the small house keeps its three wall torches and 43 cobblestone |
| With WWOO, its biomes' villages are ruined the same way | Not checked | The client's village was in a vanilla biome |
| With IW, about one house in six burns; without IW none does | Pass | GameTest `someVillageHousesBurnedDown` (with IW): 16 charred logs, 33 charred planks and 15 charred stairs of the house's 50, so most of the roof has fallen in; no unburnt wood, glass, torches or cobwebs; doors, bed and cobblestone kept. The roll is skipped when IW is missing (`Charred.available()`). Client: one black, roofless house in the village. Bringing a charred block down by breaking one under it was not tried. [village-burnt-house](../../builds/screenshots/village-burnt-house.png), [village-ruins](../../builds/screenshots/village-ruins.png) |
| Charred wood burns Hot; the Kiln Heat tab lists it | Pass | GameTest `charredWoodBurnsHot` (log, planks, slab, stairs, fence and gate). Client: the charred log is under Hot in the Kiln Heat tab. [jei-charred-log](../../builds/screenshots/jei-charred-log.png) |
| Power Grid magnetizes slag; without it the recipe is skipped quietly | Pass, see note | GameTest `powerGridMagnetizesSlag`: the recipe loads as Power Grid's magnetization and gives iron nuggets. Without Power Grid the logs show no recipe error. Power Grid's JEI tab was not seen, since Power Grid does not run in the dev client |

### Section 11: saw blades

| Check | Result | How |
| --- | --- | --- |
| Render placed in every facing and in hand; blades turn with the shaft, Flywheel on and off; on a contraption | Pass, see note | Client: the three new saws and Create's render in every facing and in hand, and their blades turn with the shaft with Flywheel on and with its backend off. On a bearing and on a piston each saw keeps its own blade and cuts, with Flywheel on and off. Whether a trailing blade stands still was not watched. [saws-placed](../../builds/screenshots/saws-placed.png), [saws-on-a-bearing](../../builds/screenshots/saws-on-a-bearing.png) |
| Tooltip blade line on every saw; goggles show speed and stress | Pass | Client: a grey blade line on all four, Create's included ("Gold blade: cuts wood, too soft for stone"); goggles at 16 RPM show 32, 32, 64 and 128 su with the speed line. GameTest `sawBladesHaveTheirTiers`: 2, 2, 4 and 8 su per RPM, so 64, 64, 128 and 256 su at 32 RPM. [saw-tooltips](../../builds/screenshots/saw-tooltips.png), [goggles-saws](../../builds/screenshots/goggles-saws.png) |
| Recipes | Pass | GameTest `sawRecipes`: three cobblestone, stone or smooth stone slabs around andesite alloy; three golden sheets around an iron ingot; three diamonds around an iron sheet; andesite casing below each; Create's saw unchanged |
| Speeds at 32 RPM | Pass | GameTest `sawBladesCutAtTheirSpeeds`: a log takes 75, 50, 25 and 37.5 ticks on the stone, iron, golden and diamond saws, exactly two thirds, one, two and four thirds of Create's speed, and comes off at ticks 58, 39, 20 and 30. How the log slides was not watched |
| Smooth stone on a golden saw comes off uncut and the next log is cut; the other saws stonecut | Pass | GameTest `goldenSawSkatesStoneAndStripsLogs`: the smooth stone skates across the golden blade and comes off whole, then a log on the same saw comes off stripped; a stone saw cuts the smooth stone into two slabs. The squeal and sparks were not seen or heard, and the filter slot was not tried |
| A belt carrying logs and stone through a golden saw | Not checked | The GameTest feeds the saw the way a belt or funnel hands it items, but no belt was built |
| Horizontal saws fell trees at their blade's speed, fixed and on a bearing or piston; on a piston each saw cuts the log it is pushed into | Pass | GameTests `horizontalSawsFellTrees` (fixed saws at 32 RPM fell a small oak at ticks 37, 46, 64 and 91: gold, diamond, iron, stone) and `sawsOnAPistonCutWhatTheyArePushedInto` (each saw cuts both logs in its way and ends three blocks on). Client: the four saws on pistons pushed into small oaks fell them, gold first and stone last, with Flywheel on and off; on bearings they cut the logs above and beside them. The log under each saw on a bearing stayed, under Create's own saw too. [saws-on-pistons](../../builds/screenshots/saws-on-pistons.png) |
| A Mechanical Arm takes an upward new saw as a point | Pass, see note | GameTest `sawsWorkWithArmsControlsAndPlacement`: a turning upward golden saw is an arm point of the new type; a still one and a sideways one are not, as with Create's saw. No arm was built |
| Clicking a placed new saw with any saw places it alongside | Pass | Same GameTest (a diamond saw and Create's saw land beside a stone saw, facing up), and the client (the placement outline shows, and a click places the saw) |
| Contraption Controls; dropped with a pickaxe or an axe | Pass | Same GameTest: all four are in `#create:contraption_controlled`, and a golden saw drops itself to an iron pickaxe and to an iron axe |
| JEI catalysts | Pass | Client: the golden saw is a catalyst on Sawing only; the stone and diamond saws on Sawing and Block Cutting. [jei-saw-catalysts](../../builds/screenshots/jei-saw-catalysts.png) shows their tab rows (golden, stone, diamond) |
| Ponder | Pass | Client: "Saw blades" plays with its text, the golden saw's stone sparking and coming off; the new saws also have Create's saw scenes, with their text. [ponder-saw-blades](../../builds/screenshots/ponder-saw-blades.png) |

### Section 12: clutch and gearshift tiers

| Check | Result | How |
| --- | --- | --- |
| All 34 render placed and in hand: the material's casing, the red bands, bright when powered, the shaft halves | Pass | Client: wooden, copper and iron clutches and gearshifts in rows, powered ones under redstone blocks with their bands lit. GameTest `thirtyFourClutchesAndGearshifts`: 17 clutches and 17 gearshifts, each with its item and block entity. [clutches](../../builds/screenshots/clutches.png) |
| Powered, a clutch stops its far side and a gearshift reverses it, driven from either end | Pass | GameTest `poweredClutchesStopAndGearshiftsReverse`: every part driven from the west, then from the east. Client: a powered clutch's far shaft half stands still and a powered gearshift's keeps turning |
| Create's clutch and gearshift break at 192 RPM, copper and iron hold; a wooden one snaps first | Pass | GameTests `clutchesBreakAboveTheirSpeedLimit` (Create's clutch breaks at 192 and holds at 128, Create's gearshift breaks at 192, a copper clutch and an iron gearshift hold at 192, a wooden clutch breaks at 96 and a wooden gearshift holds at 64) and `woodenClutchAndGearshiftSnapFirstWhenOverstressed` |
| Goggles show material, top speed, load limit and stage | Pass | Client: wooden "Top speed 64 RPM, Load limit 2048 su, Snaps first when overstressed", copper "256 RPM, 4096 su, Bright, no friction", iron "256 RPM, Load limit none, Clean" and 2 su at 16 RPM. GameTest `clutchFrictionFollowsMaterialAndWeathering`: the friction of 11 kinds, from none for wood and bright copper to 2.125 su per RPM for rusted iron. [goggles-clutches](../../builds/screenshots/goggles-clutches.png) |
| Weathering, wax and scraping; a powered clutch that weathers stays powered | Pass | GameTest `clutchesWeatherWaxAndScrape`: a copper gearshift weathers and keeps its axis; a dry iron clutch stays clean while a wet iron gearshift rusts; a waxed copper clutch stays; honeycomb waxes (one used) and an axe scrapes; a powered copper clutch that weathers stays powered and its far shaft stays still |
| Recipes; wooden ones burn and break fastest with an axe | Pass | GameTests `clutchRecipes` (the table, waxing with honeycomb, Create's two unchanged) and `clutchesDropThemselves` (wooden ones to an axe, the rest to a pickaxe, each dropping itself; a wooden clutch burns, an iron one does not) |
| Ponder | Pass | Client: "Clutch and gearshift materials" plays with its text, and the tiered parts also have Create's clutch and gearshift scenes, with their text. [ponder-clutches](../../builds/screenshots/ponder-clutches.png) |

### Earlier tests on 0.8.0

All 62 pass, among them `XpTests` as rewritten for the held-back XP, and `drillOnAPistonStopsAtWhatItCannotCut` with
the movement checks now registered at common setup. Two were made steadier on the way:

- `fannedCampfireCooksTwiceAsFast` failed once: cooked beef can roll more than two blocks off a campfire before it
  settles. It now counts the beef within four blocks of either campfire.
- `ironDrillBitsCanBeSwitchedOff` switches the live server config off and back on. Forge saves each change to the file
  and reloads it a moment later, and once that reload landed after the test had switched it back on, leaving
  `ironDrillBits` off for the drill tests that ran next (`drillsOnlyCutWhatTheirBitsCan` failed). It now runs in a
  batch of its own with time between its steps, and checks the setting is back at the end. The new
  `ruinedVillagesCanBeSwitchedOff` works the same way.

### Bugs found and fixed

1. **Ruined villages hung cobwebs outside.** In the client, cobwebs hung under the eaves outside a plains house.
   `RuinProcessor.hangCobwebs` asked a corner for a roof, two walls and a floor below, and the corner under an eave, on
   a porch or under a well's roof has those too. A corner now also has to be closed in: looking along each of the four
   sides, a wall, door, trapdoor, iron bars or glass of the same piece must come within 16 blocks
   (`RuinProcessor.enclosed` and `closesRoom`). Run over every vanilla village template (the zombie ones aside), the old
   rule allowed cobwebs in 558 corners and 83 of them are outdoors (67 in houses, 16 in town centers); the new rule
   skips those 83 and keeps the 81 corners of rooms that open onto the street through a doorway with no door. The new
   GameTest `cobwebsHangOnlyInsideRooms` failed on the old code, with 17 cobwebs on the porches, and passes now. A
   freshly generated village was not looked at again in the client.

### "Fires only"

Taran answered last round's question: a bellows makes a kiln hotter only when the kiln has a fire. `KilnBlockEntity`
now fans the kiln's own fire (its fuel, or a fire underneath) by one level, while heat from an electric heater counts
but is never fanned. A kiln warmed only by a Power Grid heater stays as it is with a bellows blowing into it, the bellows
says "No fire to fan", and the kiln no longer flares; fuel burning in that kiln is still fanned, and the kiln burns it
when fanning it reaches work the heater alone cannot do. In `CompanionTests`' Power Grid test, an empty kiln on a
500 °C coil with a bellows blowing into it stays Hot and unfanned, and a kiln on the same coil burning charcoal with raw
iron in its chamber is Searing and fanned. DESIGN.md's bellows section says so in one sentence.

### Test setup fixes (not bugs in the mod)

The first run of the new tests had six failures, all in the tests themselves:

- Saw placement: Create takes any click by a fake player on a saw as setting the saw's filter, so the test now clicks
  the block directly.
- Slag as bone meal grows a sapling only some of the time, so the test tries up to 20 times.
- The ruined house test expected the template's own counts, but its two jigsaw blocks become a plank and a stair when a
  village places it (34 planks and 50 stairs, not 33 and 49).
- Two tests read the goggle lines of saws and clutches, which Create builds with client code, so they failed on the
  GameTest server; the goggles were checked in the client instead.
- The tree-felling test drove each saw from the side, but a horizontal saw only takes its shaft from behind.

### Differences from the notes (no change made)

- **Cats still come with villages.** A cat walked the generated village; it comes from the village's own cat pool. The
  design removes villagers, zombie villagers and golems and says nothing about cats.
- **Kiln Heat tab labels crowd.** On a page with all three slots, "Below" and "Fanned" run together ("BelowFann...").
  Cosmetic.

### Running the tests

```
./gradlew runGameTestServer                            # 87 tests
./gradlew runGameTestServer -Pcompanions -Ppowergrid   # 92 tests, with IW, WWOO, Better Combat and Power Grid
./gradlew runClient -Pcompanions                       # dev client with IW, WWOO and Better Combat
```

Each GameTest run here started from a fresh `run/gametest/world` (deleted first).

### Files changed

For merging into the 0.9.0 copy. Every change is listed; nothing else differs from the copy this round started from.

- `src/main/java/com/mcremix/kiln/KilnBlockEntity.java`: "Fires only" (the new `heatWith`, when fuel is worth
  burning, when the kiln flares, and the class comment).
- `src/main/java/com/mcremix/bellows/BellowsBlockEntity.java`: "No fire to fan" for a kiln warmed only by a heater
  (`describeEffect`).
- `src/main/java/com/mcremix/village/RuinProcessor.java`: cobwebs only in closed-in corners (`enclosed`,
  `closesRoom`, `ROOM_WIDTH` and four imports).
- `docs/DESIGN.md`: one sentence in the Bellows section (line 109).
- `src/gametest/java/com/mcremix/gametest/`: new `RoadTests.java`, `BoilerTests.java`, `RuinTests.java`,
  `SawTests.java` and `ClutchTests.java`; `CompanionTests.java` (the Power Grid test checks "Fires only", and three
  new tests for IW and Power Grid); `BellowsTests.java` (counts the cooked beef within four blocks);
  `DrillTests.java` (`ironDrillBitsCanBeSwitchedOff` runs in its own batch).
- `docs/TEST_REPORT.md` (this report) and `docs/BUILD_NOTES.md` (a 0.8.0 status paragraph, sections 8, 9, 11 and 12
  ticked, and the cobweb sentence in section 9).

Not changed: the status line and roadmap in `docs/DESIGN.md` still say 0.5.0 to 0.8.0 await a build. They are left for
the writer, who is editing DESIGN.md for 0.9.0.

The tested jar is `builds/mcremix-1.20.1-0.8.0.jar` (SHA-256
`cca45bc21ab74a6b76c7bb5189c85027a234695f965246ea9907c92ca68185d0`); `builds/mcremix-1.20.1-0.8.0.zip` holds the same
jar for browsers that block .jar downloads.

### Not covered

- Sounds (the test machine has no audio device): the road's crunch, the golden saw's squeal.
- A steam engine driven by a kiln-heated boiler: the boiler in the client had no water pumped in, so only its heat
  was checked.
- Taiga, desert, savanna, snowy and WWOO villages, monsters in villages at night, and a fresh village in the client
  after the cobweb fix.
- A belt or a Mechanical Arm feeding a saw (GameTests cover feeding the saw directly and the arm point).
- Power Grid in the dev client (it crashes there), so its magnetizing JEI tab was not seen.

## 0.4.0: Bellows, gearbox tiers, Stone Anvil, JEI tabs and drill bits

Covers sections 5, 6 and 7 of [BUILD_NOTES.md](BUILD_NOTES.md), and reruns every phase 1 test. Tested on 2026-10-14
and 2026-10-15 against Forge 47.4.6, Create 6.0.8-291 and JEI 15.20.0.106.

**Result:** 0.2.0 to 0.4.0 compiled with no errors the first time, and everything in sections 5 to 7 works. Two bugs
turned up and are fixed in this copy, plus the dev setup's Flywheel pin, which crashed the dev client whenever a
contraption moved (below). One design question, about bellows on Power Grid heaters, is at the end.

### How it was tested

- **GameTests:** 31 new tests for sections 5 to 7 (`BellowsTests`, `GearboxTests`, `StoneAnvilTests`, `DrillTests`)
  and a rewritten Power Grid test. `./gradlew runGameTestServer` runs 62 and passes all of them; with
  `-Pcompanions -Ppowergrid` it runs 64 and passes all of them.
- **Dev client** with IW, WWOO and Better Combat (`./gradlew runClient -Pcompanions`), in the phase 1 creative world
  upgraded to 0.4.0: rendering, animation, goggles, tooltips, JEI and Ponder, with Flywheel on and with its backend
  off (`/flywheel backend flywheel:off`). Screenshots are in [builds/screenshots](../../builds/screenshots).
- **A Forge server** with the 0.4.0 jar, every companion mod and no JEI, upgrading the phase 1 WWOO world. It loads,
  and 8 of 8 console checks pass: a golden drill leaves iron ore and cuts stone, Create's drill leaves obsidian and the
  diamond drill cuts it, Create's andesite shaft and gearbox break at 192 RPM while a copper shaft and a rusted iron
  gearbox hold. A campfire kiln fanned by a bellows reads Hot and fanned, and the bellows loads its network with
  64 su at 32 RPM.

### Section 5: the Bellows and Power Grid heat

| Check | Result | How |
| --- | --- | --- |
| Renders placed and in hand; points into what you click, where you look on the ground; sneaking turns it round; a shaft only connects at the back | Pass | Client: base, casing, copper nozzle, leather bag, lid, crank and yoke all render. GameTests `bellowsPointsIntoWhatYouClick` (the side of a kiln, the side of a campfire, sneaking, the ground) and `bellowsTakesAShaftOnlyAtTheBack` (the back turns it; the nozzle end and the sides do not connect). [bellows-tooltip](../../builds/screenshots/bellows-tooltip.png) |
| The lid pumps once per turn with the pin in the yoke; each downstroke puffs; a fire in front flares; Ponder | Pass | Client frames at 32 RPM: the lid rises and falls once per crank turn and the pin stays in the slot; air puffs from the nozzle and the campfire under a kiln flares. The Ponder scene plays to the end. Sound not checked (the test machine has no audio device). [bellows-pumping](../../builds/screenshots/bellows-pumping.png), [bellows-puff](../../builds/screenshots/bellows-puff.png), [bellows-fanning-kiln](../../builds/screenshots/bellows-fanning-kiln.png), [ponder-bellows](../../builds/screenshots/ponder-bellows.png) |
| Campfire kiln: Warm without, Hot with; goggles lines; stopping the shaft drops it to Warm | Pass | GameTest `bellowsFansACampfireKilnToHot`: the unfanned kiln stays Warm with its raw copper "too cold"; the fanned one is Hot, its goggles say "Fanned by bellows", the bellows says "Fanning the kiln", and the copper fires. With the motor stopped the kiln is Warm again and the bellows says "Not turning". The server shows the same (Heat 2, fanned) |
| Blowing into a kiln: charcoal goes Searing and burns twice as fast; coke stays Searing at its normal rate; a cold kiln stays cold | Pass | GameTest `bellowsBlowingIntoAKiln`: in 40 ticks, fanned charcoal used 80 ticks of fuel, plain charcoal 40, fanned coke 40. Coke never reaches Blazing ("The kiln cannot burn any hotter"); a cold kiln stays cold ("No fire to fan") |
| Raw beef on a fanned campfire: about 15 s instead of 30 | Pass | GameTest `fannedCampfireCooksTwiceAsFast`: 300 ticks fanned, 600 unfanned |
| 2 su per RPM | Pass | GameTest `bellowsStressAndGoggleLines`: impact 2, a 64 su load at 32 RPM, and the "Not turning", "Nothing to blow into", "No kiln above this fire" and "No fire to fan" lines. The client's goggles and item tooltip show it |
| Power Grid: a kiln follows the heater's temperature; a bellows facing a heater says "No fire to fan" | Pass | GameTest `powerGridHeaterHeatsAKiln` (rewritten, see below): the basin heater and the heating coil set from -1000 to 1500 °C read Cold below 200, Warm from 200, Hot from 400, Searing from 600 and Blazing from 1200, and neither counts as a fire. In place, a kiln on a basin heater held near 800 °C is Searing, one on a coil held near 500 °C is Hot, and a bellows aimed at that coil does not fan it. The fallback (Power Grid renamed) still reads the basin heater like a blaze burner. Power Grid's own goggles were not checked: Power Grid does not run in the dev client |

### Section 6: gearbox tiers

| Check | Result | How |
| --- | --- | --- |
| All 17 render placed and in hand; patina and rust by stage; waxed ones look like their stage | Pass | Client: all 17 on motors render and turn, each stage drawn, waxed ones like their stage. GameTest `seventeenGearboxesWithTheirOwnBlockEntity` (17 blocks, each with an item and the gearbox block entity). [gearboxes-gallery](../../builds/screenshots/gearboxes-gallery.png) |
| Upright on open ground, lying against a vertical shaft, turned to meet a shaft beside it; sneaking lays it down; a wrench turns it; it drops itself | Pass | GameTests `gearboxesStandUprightOrLieAgainstShafts` and `gearboxesDropThemselvesEitherWayUp` (four kinds on all three axes drop themselves; pick-block gives the gearbox; Create's gearbox on its side still drops Create's vertical gearbox) |
| Outputs turn like Create's gearbox, Flywheel on and off | Pass | GameTest `gearboxesTurnLikeCreates`: six gearboxes upright and three lying down give the same speed and direction on every output as Create's gearbox in the same spot. Client: they turn with Flywheel on and with its backend off |
| Create's gearbox breaks at 192 RPM, copper and iron hold; a wooden gearbox snaps first | Pass | GameTests `gearboxesBreakAboveTheirSpeedLimit` (Create's breaks at 192 and holds at 128; copper and iron hold at 192, waxed rusted iron at 256; wood breaks at 96 and holds at 64) and `woodenGearboxSnapsFirstWhenOverstressed`. Also on the server |
| Goggles show material, top speed, load limit and stage; a weathered copper gearbox adds 0.5 su per RPM | Pass | GameTest `gearboxFrictionFollowsMaterialAndWeathering` (11 kinds; 16 su at 32 RPM for weathered copper). Client goggles: "Oxidized, nearly seized" on oxidized copper, "Clean" on iron, "Clean, Waxed" on waxed iron. [goggles-gearbox](../../builds/screenshots/goggles-gearbox.png) |
| Copper oxidizes; iron rusts only when wet; honeycomb waxes, an axe scrapes; crafting with honeycomb waxes | Pass | GameTests `copperGearboxesWeatherIronOnlyWhenWet` (with the dry-iron check below), `gearboxesWaxAndScrapeLikeShafts` and `gearboxRecipes`. Rain was not rerun for gearboxes: they rust through the same `Weathering.rust` as the iron shafts, whose rain test passed in phase 1 |
| Recipes | Pass | GameTest `gearboxRecipes`: four wooden shafts around a stripped log, four copper cogwheels around copper casing, four iron cogwheels around an industrial iron block, Create's gearbox unchanged, and the industrial iron block still stonecut from iron |
| "Gearbox materials" Ponder from any tiered gearbox and from Create's | Pass | Client: plays to the end from a copper gearbox and from Create's gearbox. [ponder-gearboxes](../../builds/screenshots/ponder-gearboxes.png) |

**The iron gearboxes and the dry-iron fix:** `RustingGearboxBlock` is not a vanilla `WeatheringCopper`, so it does not
hold copper back. In `copperGearboxesWeatherIronOnlyWhenWet`, an exposed copper gearbox and an exposed copper block
beside a dry iron gearbox keep weathering, the dry gearbox stays clean, and a wet iron gearbox next to a dry one rusts
all the way.

### Section 6: the Stone Anvil

| Check | Result | How |
| --- | --- | --- |
| Recipe; places like a vanilla anvil; outline matches the model | Pass | GameTest `stoneAnvilRecipeAndPlacement` (three smooth stone, a stone, three cobblestone; faces the way a vanilla anvil placed the same way does, long top across the view, top at 14 px). Client: the outline follows the foot, waist and top. [stone-anvils](../../builds/screenshots/stone-anvils.png), [stone-anvil-strike](../../builds/screenshots/stone-anvil-strike.png) |
| Striking: sparks, a lava pop and grit on its top; four strikes give an ingot | Pass | GameTest `stoneAnvilBeatsOutABloom`. Client: the particles land on the stone anvil's lower top. The thud was not heard (no audio device). [stone-anvil-strike](../../builds/screenshots/stone-anvil-strike.png) |
| About one bloom in five cracks it: chipped, damaged, then three cobblestone; a pickaxe drops it at its stage | Pass | GameTest `stoneAnvilsCrackTwiceThenCrumble`: 12 anvils lasted 13.5 and 14.1 blooms each in two runs (the design says about 15), always stone, chipped, damaged, then exactly three cobblestone, facing kept. `stoneAnvilsDropThemselvesToAPickaxe`: each stage drops itself to a pickaxe and nothing by hand. Client: all three stages render |
| No menu; vanilla anvils still strike blooms | Pass | GameTest `stoneAnvilHasNoMenu` (empty hand, a stick, a sword); phase 1's `bloomFourStrikesOnAnvil` still passes |
| The bloom Ponder strikes on a stone anvil, cracks it twice, crumbles it, then drops an iron anvil; it opens from the stone anvils | Pass | Client. [ponder-bloom-stone-anvil](../../builds/screenshots/ponder-bloom-stone-anvil.png), [stone-anvil-tooltip](../../builds/screenshots/stone-anvil-tooltip.png) |

### Section 6: JEI tabs

| Check | Result | How |
| --- | --- | --- |
| Kiln tab | Pass | Client: 39 pages with the firing recipes and non-food smelting at Hot; crushed iron shows "Blazing: twice as many"; no food; logs show charcoal at Warm only. Hovering the heat line says "Each heat level above this halves the time". [jei-kiln](../../builds/screenshots/jei-kiln.png) |
| Kiln Heat tab | Pass | Client: four pages, Warm to Blazing; the fuel and "below" slots cycle as listed, Blazing has no fuel slot, Hot and Searing show the Bellows; catalysts Kiln and Bellows. Power Grid's heaters were not checked here (Power Grid does not run in the dev client). [jei-kiln-heat](../../builds/screenshots/jei-kiln-heat.png) |
| Striking Blooms tab | Pass | Client: bloom to ingot with slag at "50% chance"; catalysts Stone Anvil and Anvil. [jei-striking-blooms](../../builds/screenshots/jei-striking-blooms.png) |
| Furnace, smoker, blast furnace and fuel tabs hidden, back with `coldFurnaces = false`; info pages | Pass after fix | Client: the tabs are hidden, the furnace, smoker, blast furnace and slag have info pages, and with `coldFurnaces = false` the tabs come back. The "Furnaces have gone cold" pages stayed too; fixed (bug 2). [jei-cold-furnaces-off](../../builds/screenshots/jei-cold-furnaces-off.png) |
| Without JEI, nothing in `compat/jei` loads | Pass | The server runs without JEI and works (above). Only the four classes in `compat/jei` import JEI. A client without JEI was not run |

### Section 7: drill bits

| Check | Result | How |
| --- | --- | --- |
| The three drills render placed and in hand; heads spin, Flywheel on and off, placed and on a contraption | Pass | Client: all four drills render with their bits. Placed, every head turns with Flywheel on and with its backend off (two frames 0.35 s apart differ on each head and nowhere else). A golden drill on a piston and on a bearing renders and cuts with Flywheel on and off (after the Flywheel fix below). [drills-placed](../../builds/screenshots/drills-placed.png), [drill-bearing-and-piston](../../builds/screenshots/drill-bearing-and-piston.png) |
| Tooltip bits line on every drill; Shift summaries; stress impact | Pass | Client: a grey bits line under every drill's name, Create's included; Shift shows the summaries of ours; stress impact reads Low for stone and gold, Moderate for Create's, High for diamond. [drill-tooltip](../../builds/screenshots/drill-tooltip.png) |
| Recipes | Pass, see note | GameTest `drillRecipes`. They take three cobblestone, gold ingots or diamonds, in the shape of Create's drill, not four as the notes said |
| Speeds at 32 RPM on stone; goggles; 64, 64, 128 and 256 su | Pass, see note | GameTest `drillSpeedsFollowTheirBits`: gold 29 ticks, diamond 38, Create's 47, stone 74. GameTest `drillStressFollowsTheirBits`: 2, 2, 4 and 8 su per RPM, so 64, 64, 128 and 256 su at 32 RPM for stone, gold, Create's and diamond. Client goggles show the bits, speed and stress lines |
| Stuck drills: sparks, crits and grit every half second; the lines shown on looking at it; starting again | Pass | GameTests `drillsOnlyCutWhatTheirBitsCan` (16 drill and block pairs: gold on iron, copper and lapis ore; stone on gold, diamond, emerald and redstone ore; Create's on obsidian, crying obsidian and ancient debris; and pairs they do cut) and `stuckDrillStartsAgainWhenTheBlockChanges` (stone swapped in is cut within 2.5 s). Client: sparks and grit off the ore, "Too hard for gold bits" and "Iron Ore needs stone, iron or diamond bits". The squeal was not heard (no audio device). [drill-stuck](../../builds/screenshots/drill-stuck.png), [drill-sparks](../../builds/screenshots/drill-sparks.png) |
| The diamond drill cuts obsidian; `ironDrillBits = false` | Pass | GameTest `ironDrillBitsCanBeSwitchedOff` (fixed and on a contraption: Create's drill cuts obsidian again and loses its bits; the golden drill keeps its tier). The server check above |
| On a piston, an uncuttable block stops it; on a bearing, drills skip it | Pass after fix | GameTests `drillOnAPistonStopsAtWhatItCannotCut` (the golden drill cuts the stone and stops before the iron ore; Create's drill cuts through) and `drillOnABearingSkipsWhatItCannotCut`. Client: both rigs, Flywheel on and off. Fixed (bug 1) |
| Contraption Controls; a cobble generator feeds a hopper | Pass | GameTests `drillsAreContraptionControlledAndPlaceAlongside` (all four drills are in `#create:contraption_controlled`; a Contraption Controls block was not used by hand) and `goldenDrillFeedsAHopperFromACobbleGenerator` |
| Clicking a placed tiered drill with any drill places it alongside | Pass | Same test: a diamond drill and Create's drill clicked onto a golden drill each land beside it, facing the same way |
| "Drill bits" Ponder from all four drills; Kinetic Appliances and Contraption Actors | Pass | Client. [ponder-drill-bits](../../builds/screenshots/ponder-drill-bits.png) |

### Phase 1 tests on 0.4.0

All 31 still pass, and IW's soot test. The Power Grid test failed: it checked the old reading, Create's blaze heat
property on the basin heater, while 0.2.0 reads Power Grid's heaters by temperature. It now tests the temperatures
(section 5 table).

### Bugs found and fixed

1. **Tiered drills on a piston pushed blocks instead of cutting them.** Create decides which blocks a moving
   contraption shoves ahead of it, and it only treats its own Mechanical Drill as not holding up the block in front.
   A stone, golden or diamond drill on a piston therefore pushed the stone and the ore in front along instead of
   cutting them. `TieredDrillBlock.registerMovementChecks()` registers the same rule for tiered drills
   (`BlockMovementChecks.registerNotSupportiveCheck`), and `McRemix` calls it at common setup.
   `drillOnAPistonStopsAtWhatItCannotCut` failed before the fix (the golden drill reached the end of the piston with
   the stone pushed ahead of it) and passes now; in the client the drill cuts the stone and stops at the ore.
2. **With `coldFurnaces = false`, JEI still showed "Furnaces have gone cold"** on the furnace, smoker and blast
   furnace, which cook again in that case. `RemixJeiPlugin` now adds those pages only when cold furnaces are on, as it
   already did for hiding the tabs. Checked in the client: with cold furnaces off the pages are gone and slag keeps
   its page. [jei-cold-furnaces-off](../../builds/screenshots/jei-cold-furnaces-off.png) shows the furnace's tabs
   before the fix (left) and after it (right).
3. **Dev setup: the dev client crashed whenever a contraption moved.** `gradle.properties` pinned Flywheel 1.0.5,
   older than the 1.0.6-beta-266 that Create 6.0.8 is built with and bundles, so Create's contraption visual failed
   with `NoClassDefFoundError: dev/engine_room/flywheel/lib/model/baked/BlockMaterialFunction`. Now pinned to
   1.0.6-beta-266. Players were never affected, since Create's jar brings its own Flywheel. With the new pin every
   GameTest passes and the piston and bearing rigs render with Flywheel on and off.

### Differences from the notes (no change made)

- **Drill speeds land near the table, not on it.** The multipliers are applied to Create's break speed as written,
  but Create counts break progress in whole ticks, so the real gap depends on speed and hardness. At 32 RPM on
  stone, gold cuts 1.6 times as fast as Create's drill, diamond 1.24 times and stone 0.64 times. "Twice as fast" on
  the tooltip is fair as a rule of thumb.
- **Drill recipes take three of their material, not four**, like Create's drill (three andesite alloy around an iron
  ingot). BUILD_NOTES now says three.
- **A heating coil makes a kiln Searing only at its limit.** Power Grid's heating coil overheats at 600 °C, which is
  the Searing threshold, so in practice a coil gives Hot; the basin heater (overheats at 1600 °C) covers Searing and
  Blazing.

### Question for Taran

**Should a bellows blowing into a kiln that sits on a Power Grid heater make it hotter?** It does now: the kiln adds a
level to whatever heats it, so a kiln on a 450 °C coil goes from Hot to Searing with a bellows aimed at the kiln.
DESIGN.md says the bellows "fans fires, not electric heaters", and a bellows aimed at the heater itself does nothing,
as the tests confirm. If the answer is no, the boost should lift only fuel or a fire below the kiln, and the bellows
should say "No fire to fan". Found by reading `KilnBlockEntity.tick`; not tried in game.

### Running the tests

```
./gradlew runGameTestServer                            # 62 tests
./gradlew runGameTestServer -Pcompanions -Ppowergrid   # 64 tests, with IW, WWOO, Better Combat and Power Grid
./gradlew runClient -Pcompanions                       # dev client with IW, WWOO and Better Combat
```

### Files changed

For merging into the 0.5.0 copy. Every change is listed; nothing else differs from the copy this round started from.

- `gradle.properties`: `flywheel_version=1.0.6-beta-266` (was 1.0.5).
- `src/main/java/com/mcremix/drilling/TieredDrillBlock.java`: new `registerMovementChecks()` and its two imports.
- `src/main/java/com/mcremix/McRemix.java`: calls it at common setup (one listener and two imports).
- `src/main/java/com/mcremix/compat/jei/RemixJeiPlugin.java`: the furnace, smoker and blast furnace info pages are
  added only when `coldFurnaces` is on.
- `src/gametest/java/com/mcremix/gametest/`: new `BellowsTests.java`, `GearboxTests.java`, `StoneAnvilTests.java` and
  `DrillTests.java`; `GT.java` gains the `craft` and `breakWith` helpers; `CompanionTests.java` has the rewritten
  Power Grid test.
- `docs/TEST_REPORT.md` (this report) and `docs/BUILD_NOTES.md` (a 0.4.0 status paragraph, sections 5 to 7 ticked,
  the drill recipe count).

Not changed: `docs/DESIGN.md` still says 0.2.0 to 0.4.0 await a build (its status line and roadmap). That is left for
the writer, who is editing it for 0.5.0.

The tested jar is `builds/mcremix-1.20.1-0.4.0.jar` (SHA-256 `b363846a1bc6e575774defa7015d06c1baf7f0639a3f2d54f7625c8b313ff7c6`).

### Not covered

- Sounds (the test machine has no audio device).
- Power Grid in the dev client (it crashes there, as in phase 1), so its goggles and its heaters in the Kiln Heat tab
  were not seen; GameTests and the server cover its heat.
- A client without JEI, and multiplayer with more than one player.

## Phase 1 gameplay (0.1.0)

Covers every item in section 4 of [BUILD_NOTES.md](BUILD_NOTES.md), plus mob and discovery XP, and play alongside
Overhauled Overworld (WWOO), Better Combat, Immersive Weathering (IW) and Create: Power Grid. Tested on 2026-10-14
against Forge 47.4.6 and Create 6.0.8-291.

**Result:** everything in section 4 works. Two bugs turned up and are fixed in this copy (below). Nothing needs a
design call.

### How it was tested

- **GameTests** (`src/gametest`, 33 tests): `./gradlew runGameTestServer` runs 31 and passes all of them. With
  `-Pcompanions -Ppowergrid` it also loads IW, WWOO, Better Combat and Power Grid, adds the soot and Power Grid tests,
  and passes all 33. The tests are never packaged into the mod jar.
- **Dev client** with IW, WWOO and Better Combat loaded (`./gradlew runClient -Pcompanions`), in a creative copy of
  the server world below. Screenshots are in [builds/screenshots](../../builds/screenshots).
- **A Forge server** with the release jars of every companion mod (IW 2.0.5, Moonlight, WWOO 2.0.0, Better Combat
  1.9.0, Power Grid 0.6.2 and their libraries) and a fresh WWOO world, driven by console commands. The final jar
  loads there with every companion mod.

### Section 4, item by item

| Check | Result | How |
| --- | --- | --- |
| JEI lists every item; the 49 parts render and spin | Pass | `@mcremix` in JEI shows all 53 items. All 49 parts on creative motors render textured and turn (two frames 0.3 s apart differ). No missing model or texture lines in the log. [jei-mcremix](../../builds/screenshots/jei-mcremix.png), [parts-gallery](../../builds/screenshots/parts-gallery.png), [parts-spinning](../../builds/screenshots/parts-spinning.png) |
| Copper oxidizes, honeycomb waxes, axe scrapes | Pass | GameTests `copperWeathersIronOnlyWhenWet`, `honeycombWaxesAndAxesScrape` (axis kept, one honeycomb used, axe scrapes wax then a stage) |
| Iron rusts only in rain or by water | Pass | GameTests cover dry, waterlogged and touching water. Rain can only be tested in the client (GameTests run underground): after about 100 s of rain at `randomTickSpeed 1000`, iron under open sky was rusted and iron under a roof was still clean. [rain-rust](../../builds/screenshots/rain-rust.png) |
| Goggles show friction and limits | Pass | Weathered copper shows 16 su at 32 RPM, 256 RPM, 4096 su, "Weathered, heavy friction". Wood shows 64 RPM, 2048 su, "Snaps first when overstressed". Waxed iron shows 4 su, 256 RPM, no load limit, "Clean, Waxed". GameTest `partFrictionFollowsMaterialAndWeathering` checks friction for 11 variants. [goggles-parts](../../builds/screenshots/goggles-parts.png) |
| Speed limits | Pass | GameTest `partsBreakAboveTheirSpeedLimit`: wood breaks at 96 RPM but holds at 64; Create's andesite shaft breaks at 192 but holds at 128; copper, iron and a waxed weathered copper cog hold at 256 |
| Overstress snaps wood first | Pass | GameTest `woodenShaftSnapsFirstWhenOverstressed`: a hand crank driving three presses snaps the wooden shaft, and the crank side turns again. GameTest `copperBreaksUnderHeavyLoadIronDoesNot`: three presses at 256 RPM break a copper shaft but not an iron one; one press is fine on copper |
| Kiln: clicks, charcoal, hoppers, comparator, goggles | Pass | GameTests `kilnClicksLoadChamberAndFirebox` (logs in the chamber, planks in the firebox, charcoal out, sneak takes back unfired logs), `kilnHoppersInTopAndSidesOutBottom`, `kilnComparatorReadsHeat` (0 to 4), `kilnGogglesShowHeatFuelAndSlots`, `kilnRecipeRules`, `kilnOnLavaIsTooColdForIron`, `hotKilnBurnsWhatStandsOnIt`. In the client a kiln on a lit blaze burner shows "Heat: Searing, Raw Iron x3". [kiln-goggles](../../builds/screenshots/kiln-goggles.png) |
| Bloom on a blaze burner, anvil, press, slag | Pass | GameTests `kilnOnFedBlazeBurnerMakesBloom`, `kilnBlazingDoublesCrushedIron`, `bloomFourStrikesOnAnvil`, `bloomUnderMechanicalPress`, `slagFertilisesLikeBoneMeal`, `slagWashesIntoNuggets`. The client kiln above made a bloom from raw iron |
| Furnace message, ash, hopper-fed furnace stays cold | Pass | GameTests `furnaceMenusStayShut` (furnace, smoker and blast furnace, empty hand or holding an item; sneak-placing still builds) and `furnacesStayColdWhenHopperFed` (a lit furnace also goes out). The client shows the message and a puff of ash. [furnace-ash](../../builds/screenshots/furnace-ash.png) |
| Villages, chests, breeding, curing, WWOO zombie villagers | Pass | GameTests `structureSpawnsSkipVillagersAndGolems`, `villagePiecesPlaceWithoutVillagersOrGolems`, `villageChestsHoldNoContraband`, `villagersCannotBreed`, `zombieVillagersCanStillBeCured`, `zombieVillagersSpawnWhereverZombiesDo`. On the server, a plains, desert, savanna, taiga and snowy village each had no villagers, zombie villagers or golems (cats and farm animals were there), and 60 rolls of six village chest tables held no contraband. With WWOO all 51 biomes that spawn zombies also spawn zombie villagers. In the client a zombie villager spawned at night in the WWOO world and a golden apple cured it into a villager. [zombie-villager-cure](../../builds/screenshots/zombie-villager-cure.png) |
| Vanilla enchanting untouched | Pass | GameTest `vanillaEnchantingIsUntouched` (recipe, table enchants a sword for 1 level, anvil and grindstone open) |
| Ponder: five scenes, sentences, kiln faces the camera | Pass | Kiln intro, heating from below, bloom iron, materials and shear pins all play to the end with plain sentences and the kilns facing the camera. Hold W works from the item tooltip, and the iron bloom, copper and iron parts open their scenes. [ponder-kiln-intro](../../builds/screenshots/ponder-kiln-intro.png), [ponder-kiln-heat](../../builds/screenshots/ponder-kiln-heat.png), [ponder-bloom](../../builds/screenshots/ponder-bloom.png), [ponder-materials-scene](../../builds/screenshots/ponder-materials-scene.png), [ponder-shear-pin](../../builds/screenshots/ponder-shear-pin.png), [ponder-hold-w](../../builds/screenshots/ponder-hold-w.png) |
| IW: a wood-fired kiln lays soot | Pass | GameTest `woodFiredKilnLaysSoot` with IW loaded. On the server, a kiln burning planks under a brick chimney put soot on all five chimney blocks |

### Beyond section 4

| Check | Result | How |
| --- | --- | --- |
| Mobs drop half the XP | Pass | GameTest `mobsDropHalfTheXp` (10 becomes 5, 5 averages 2.5, players keep theirs, a real zombie kill drops 2 or 3). In the client with Better Combat, six sword kills of plain zombies gave 3, 2, 3, 2, 3 and 3 XP (vanilla gives 5) |
| First pickup of each item gives XP by rarity | Pass | GameTest `firstPickupOfEachItemGivesXp` (1, 3, 8 and 20 by rarity, once per item, kept in the data that survives death, none in creative) |
| Alongside WWOO, Better Combat, IW and Power Grid | Pass | All load together on the server and in GameTests; the client runs with all but Power Grid (see below). GameTest `powerGridHeaterHeatsAKiln`: a Power Grid basin heater heats a kiln like a blaze burner |

### Bugs found and fixed

1. **The game would not start with Immersive Weathering installed.** `mods.toml` asked for IW `[2.0,)`, but IW
   numbers its versions after Minecraft's (`1.20.1-2.0.5`), which Forge's version compare puts below `2.0`, so Forge
   refused to load. Reproduced on the server with the old jar. Now `[1.20.1-2.0,)`.
2. **A dry iron part stopped nearby copper from ever weathering.** Iron parts were copper blocks as far as vanilla's
   weathering was concerned, and vanilla holds a copper block back while anything of its family within 4 blocks is
   younger. A dry iron part never ages, so copper parts and plain copper blocks near it froze for good; a wet iron part
   next to a dry one froze too. Iron parts are now `RustingShaftBlock` and `RustingCogWheelBlock`: they rust only
   while wet, at the pace of a lone copper block, and copper does not count them. Waxing, scraping, friction and names
   are unchanged. GameTest `dryIronDoesNotHoldBackCopper` fails on the old code and passes now; in the client's rain
   test, an exposed copper shaft and an exposed copper block two blocks from dry iron both oxidized.

   The one visible difference: a cluster of wet iron parts now rusts as fast as a single part, where copper parts
   slow each other down. The design only says iron rusts when wet, with copper's stages and friction.

### Running the tests (phase 1)

```
./gradlew runGameTestServer                            # 31 tests at the time; see 0.4.0 above for today's counts
./gradlew runGameTestServer -Pcompanions -Ppowergrid   # 33 tests, with IW, WWOO, Better Combat and Power Grid
./gradlew runClient -Pcompanions                       # dev client with IW, WWOO and Better Combat
```

The companion mods come from Modrinth's Maven by version id (see `build.gradle`). Inside a dev client, `/test runall`
runs the same tests.

### Files changed (phase 1)

For merging into the phase 2 copy:

- `src/main/resources/META-INF/mods.toml`: the Immersive Weathering version range.
- `src/main/java/com/mcremix/kinetics/`: new `RustingShaftBlock` and `RustingCogWheelBlock`; `Weathering.rust`;
  `KineticFamilies` builds iron parts from the new classes; doc comments only in `WeatheringShaftBlock` and
  `WeatheringCogWheelBlock`.
- `build.gradle`: the `gametest` source set and `gameTestServer` run, the Modrinth repository, `-Pcompanions` and
  `-Ppowergrid`.
- `src/gametest/` (new), `README.md` (test commands), `docs/BUILD_NOTES.md` (section 4 ticked), this report.

### Not covered (phase 1)

- Power Grid in the dev client: dev runs do not apply its access transformer to the remapped jar, so the client
  crashes with it. That is a dev-environment limit; it loads on a real server.
- Multiplayer with more than one player, long play sessions and performance.
- Better Combat beyond a plain sword swing (dual wielding, other weapons).
- The kiln comparator, hoppers and the press were checked in GameTests only, not by hand in the client.

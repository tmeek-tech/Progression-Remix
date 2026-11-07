# Build notes: first compile

The 0.1.0 code was written without a compiler: the session that wrote it could not reach the Forge, Mojang or Create
mavens. Every Create, Ponder, Flywheel, Forge and vanilla API it calls was checked by hand against their sources
(Create 6.0.8, Ponder 1.0, Flywheel 1.0, Forge 47 / 1.20.1 with Parchment names), but nothing has been built or run.

**Status (2026-10-14):** 0.1.0 compiled cleanly against Create 6.0.8-291, Ponder 1.0.91, Flywheel 1.0.5 and
Forge 47.4.6. One load-time crash was fixed: `CStress.setNoImpact()` throws for blocks outside Create's own
namespace, so `KineticFamilies` no longer calls it (unregistered blocks already default to no impact; friction comes
from `TieredKineticBlockEntity.calculateStressApplied`). The jar then loaded on a plain Forge server with Create's
release jar, and a dev client rendered the parts and played the Ponder scenes. Sections 5 and 6 cover 0.2.0 (the
Bellows and Power Grid heat) and 0.3.0 (gearbox tiers, the Stone Anvil and JEI tabs), and section 7 covers 0.4.0
(drill bits); none of those has been built yet.

**Status (2026-10-14, gameplay):** every check in section 4 passes; [TEST_REPORT.md](TEST_REPORT.md) says how each
was checked. Two bugs were fixed on the way: the mod refused to load with Immersive Weathering installed (its
`mods.toml` version range), and a dry iron part stopped nearby copper from ever weathering (iron parts are now
`RustingShaftBlock` and `RustingCogWheelBlock`, and the 0.3.0 iron gearboxes `RustingGearboxBlock`). `./gradlew runGameTestServer` runs the 33 GameTests in `src/gametest`;
`-Pcompanions -Ppowergrid` loads the mods this one is played alongside.

**Status (2026-10-15, 0.4.0):** 0.2.0 to 0.4.0 compiled with no errors, and every check in sections 5 to 7 passes;
[TEST_REPORT.md](TEST_REPORT.md) says how each was checked. Two bugs were fixed: tiered drills on a piston pushed the
block in front of them instead of cutting it (Create only treats its own drill as not holding that block up, so
`TieredDrillBlock.registerMovementChecks` adds the same rule for ours), and JEI kept its "Furnaces have gone cold"
pages with `coldFurnaces = false`. `flywheel_version` is now 1.0.6-beta-266, the Flywheel that Create 6.0.8 is built
with: 1.0.5 crashed the dev client whenever a contraption moved. Drill speeds land near the table in section 7 rather
than on it (at 32 RPM on stone a golden drill cuts 1.6 times as fast as Create's), because Create counts break
progress in whole ticks. Not checked: sounds (no audio device), Power Grid in the dev client, and a client without JEI.
`./gradlew runGameTestServer` now runs 62 GameTests, 64 with `-Pcompanions -Ppowergrid`.

**0.5.0** (section 8) adds slag roads and kilns that heat Create steam boilers, **0.6.0** (section 9) makes villages
generate as ruins, **0.7.0** (section 11) gives Create's saw stone, gold and diamond siblings, and **0.8.0** (section
12) adds wooden, copper and iron clutches and gearshifts. XP is now held back along with enchanting:
`xp/ExperienceEvents` (halved mob XP, discovery XP), its two `experience` config entries and its lang lines are
gone, and `XpTests` instead checks that mobs and new items give vanilla XP.

**Status (2026-10-15, 0.8.0):** 0.5.0 to 0.8.0 compiled with no errors, and the checks in sections 8, 9, 11 and 12
pass apart from four still open below; [TEST_REPORT.md](TEST_REPORT.md) says how each was checked. One bug was fixed:
ruined villages hung cobwebs outside, under eaves, on porches and under well roofs, so `RuinProcessor` now hangs them
only in corners closed in on all four sides. Taran chose "Fires only" for bellows on Power Grid heaters: a kiln
warmed only by a heater is not fanned, while fuel burning in it still is (`KilnBlockEntity`, `BellowsBlockEntity` and
one sentence in DESIGN.md). Still open: monsters in villages at night, WWOO villages, and a belt or a Mechanical Arm
feeding a new saw. Not checked: sounds (no audio device), a steam engine's speed on a kiln-heated boiler (the test
boiler had no water supply), and Power Grid in the dev client. `./gradlew runGameTestServer` now runs 87 GameTests, 92
with `-Pcompanions -Ppowergrid`.

**Status (2026-10-20, 0.8.1):** 0.8.1 (section 12a) is 0.8.0 with copper and iron weathering within a day, each
block by itself and each to a stage of its own. It is numbered after 0.8.0 because 0.9.0 to 0.11.0 (kilns under
basins, chain drive tiers, the kiln as a Furnace) are written in the writer's copy and still wait for a build round;
that round has to merge these changes, which are listed under "Files changed" in [TEST_REPORT.md](TEST_REPORT.md).
0.8.1 compiled with no errors and every weathering GameTest passes; the report says how each check in section 12a was
made. The older `slagWashesIntoNuggets` sometimes fails in a fresh test world, when gravel from the roof of the space
the tests are cut into falls into it; the report has the details. Two older tests expected copper beside dry iron to
weather past exposed and wet iron to rust all the way, which a spot that stops earlier now never does, so they check
for each block's own final stage instead. The jar loads on a Forge server with Create and IW. Not checked: a played
world, so the look of a weathered build is only previewed from the same formula.
`./gradlew runGameTestServer` now runs 93 GameTests, 100 with `-Pcompanions -Ppowergrid`.

**Status (2026-10-20, 0.8.2):** Taran chose to have all iron rust only while wet, so 0.8.2 is 0.8.1 with Immersive
Weathering's iron rusting only in rain or water too, as this mod's iron parts always have. `dryIronRusts` (default
true) became `ironRustsWhenDry` (default false); with the new name, a world that ran 0.8.1 gets the new default
instead of keeping the old value in its server config. 0.8.2 was built from a separate copy of 0.8.1; the same change
is in this source (0.11.0), so the next build carries it. [TEST_REPORT.md](TEST_REPORT.md) has the 0.8.2 report.

**Status (2026-10-20, 0.11.0, compiled and tested the same day; [TEST_REPORT.md](TEST_REPORT.md) has the report):** **0.9.0** (section 13) lets a kiln heat a Create basin, **0.10.0**
(section 14) adds copper and iron chain drives, and **0.11.0** (section 15) names the kiln the Furnace in game, with
vanilla's furnace textures and recipe; the sections before it still call it the kiln, as the code does. They were
written while 0.5.0 to 0.8.1 were being built, and now carry every fix from those rounds and 0.8.1's weathering, so
copper and iron chain drives weather by section 12a's rules like every other part. Where the two met:
`KilnBlockEntity.tick` keeps "Fires only" and counts a basin holding ingredients, like a working boiler, as a reason
for a bellows to burn fuel twice as fast; `ChainDriveTests.chainDrivesAgeAndKeepTheirRow` now expects its copper
chain gearshift and wet iron chain drive at their own final stages, as `KineticTests` does for shafts. Sections 13 to
15 are what to check. `./gradlew runGameTestServer` should run 102 GameTests, 109 with
`-Pcompanions -Ppowergrid`.

**Status (2026-10-20, 0.11.1, compiled and tested the same day; [TEST_REPORT.md](TEST_REPORT.md) has the report):**
Taran asked for iron to rust when it has rusted iron above it, so 0.11.1 (section 15a) is 0.11.0 with rust running
down onto dry iron. It compiled with no errors, and `./gradlew runGameTestServer` passes all 103 GameTests, all 111
with `-Pcompanions -Ppowergrid`.

**Status (2026-10-20, 0.12.0, compiled and tested the same day; [TEST_REPORT.md](TEST_REPORT.md) has the report):**
0.12.0 (section 16) is stage one of building the idea bank: steel, made from a cementation pack, with steel parts to
512 RPM; the Steel Spring; and wooden parts in all eleven woods. It compiled with no errors, and
`./gradlew runGameTestServer` passes all 121 GameTests, all 129 with `-Pcompanions -Ppowergrid`. Run them in a fresh
test world (delete `run/gametest/world` first): entities saved in an old test world load after a test has cleared its
space, so cooked beef, XP orbs and logs from earlier runs pile up in the same tests and fail them.

**Status (2026-10-21, 0.13.0, compiled and tested the same day; [TEST_REPORT.md](TEST_REPORT.md) has the report):**
0.13.0 (section 17) is stage two of the idea bank: pipes, pumps and tanks rated for heat in wood, glass and steel,
Create's copper ones weathering for looks, steel pressure boilers, molten metal, and the crucible with sand molds.
It compiled with no errors, and `./gradlew runGameTestServer` runs 148 GameTests, 156 with
`-Pcompanions -Ppowergrid`.

**Status (2026-10-22, 0.15.0, compiled and tested the same day; [TEST_REPORT.md](TEST_REPORT.md) has the report):**
0.15.0 (section 19) is part one of Taran's friction points: bare shafts and cogs must be held by a friction point,
brackets and casings come in every material, brass is a tier, and parts under too much load wobble loose and break.
It also fixes a crash in 0.14.0 that stopped every client at start-up. It compiled with no errors,
`./gradlew runGameTestServer` passes all 196 GameTests, all 202 with `-Pcompanions` (Power Grid is always loaded now,
so `-Ppowergrid` is gone), and a dev client went through section 19's "In game" list. The dev client runs with Power
Grid now; section 19 says how, and how to run it with no screen.

**Status (2026-10-22, 0.15.1, compiled and tested the same day; [TEST_REPORT.md](TEST_REPORT.md) has the report):**
0.15.1 (section 19a) lets a bracket go down on its own, with a shaft or cog placed into it, and keeps Create's own rule
for cogs that clash. It compiled with no errors, `./gradlew runGameTestServer` passes all 199 GameTests, all 205 with
`-Pcompanions`, and a dev client went through section 19a's "In game" list.

**Status (2026-10-22, 0.16.0, compiled and tested the same day; [TEST_REPORT.md](TEST_REPORT.md) has the report):**
0.16.0 (section 20) is stage four of the idea bank: furnaces that must be lit, the Steel Furnace on piped fuel, the
Spark Plug, fermenting, vats of layered fluids with mixers on their sides, the Pressurizer and pressurised steam,
polyethylene, PVC face pipes and hydrochloric acid, and fixes a vat losing all but its tank's own fluid on reload. It
compiled with no errors, `./gradlew runGameTestServer` passes all 232 GameTests, all 238 with `-Pcompanions`, and a dev
client went through the part of section 20's "In game" list that only a client shows (the GameTests cover the rest).

**Status (2026-10-22, 0.17.0, compiled and tested the same day; [TEST_REPORT.md](TEST_REPORT.md) has the report):**
0.17.0 (section 21) is part two of Taran's friction points: machines in copper, iron, steel and aluminium that a casing
swaps in place, drill bits and saw blades fitted to any drill or saw, copper and steel vat mixers, and wooden movers. It
compiled with no errors, `./gradlew runGameTestServer` passes all 262 GameTests, all 268 with `-Pcompanions`, and a dev
client went through section 21's "In game" list.

**Status (2026-10-22, 0.18.0, compiled and tested the same day; [TEST_REPORT.md](TEST_REPORT.md) has the report):**
0.18.0 (section 22) is part three of Taran's friction points: aluminium friction points heat up as they turn and melt
when too hot, and heat sinks, fans, water coolers and radiator panels cool them, with cold water from snowy biomes and
coolant made from glycol. It also fixes aluminium casings and machines melting (in 0.17.0 they left no rough aluminium
and took the part inside with them). It compiled with no errors, `./gradlew runGameTestServer` passes all 273
GameTests, all 279 with `-Pcompanions`, and a dev client played both new Ponder scenes through.

**Status (2026-10-26, 0.20.0, compiled and tested the same day; [TEST_REPORT.md](TEST_REPORT.md) has the report):**
0.20.0 (section 25) is Taran's pipe and tank rework: wooden pipes, pumps and tanks, Create's mixer's vat form, the
Steel Spring and slag roads are taken out, and brass (thin, half the flow), iron (rusts, and leaks rusted through) and
aluminium (Warm, melts beside heat) pipes, pumps and tanks come in, with recipes for every pipe, pump and tank. It
compiled with no errors, `./gradlew runGameTestServer` passes all 291 GameTests, all 297 with `-Pcompanions`, and a
dev client drew every new part and played the changed vat mixer scene. The jar was not started on a real Forge server:
running the downloaded Forge installer was not allowed in the environment that built it.

**Status (2026-10-26, 0.21.0, compiled and tested the same day; [TEST_REPORT.md](TEST_REPORT.md) has the report):**
0.21.0 (section 26) is Taran's fermenter look: a standing vat is drawn like a brewery's conical fermenter in its tanks'
own metal, its scaffolding as a cone on braced legs and a manway door on each side of its lowest layer where nothing is
fitted, and its windows shut, so goggles say what it holds. It compiled with no errors, `./gradlew runGameTestServer`
passes all 292 GameTests, all 298 with `-Pcompanions`, and a dev client drew vats of every width, metal, patina and
rust stage and played both vat Ponder scenes. The jar was not started on a real Forge server, for the same reason as
0.20.0's.

**Status (2026-10-26, 0.22.1, compiled and tested the same day; [TEST_REPORT.md](TEST_REPORT.md) has the report):**
0.22.1 (section 28) makes rusted iron leaking a server config option, `rustedIronLeaks`, off by default (it only drips
then), and draws a vat's outlet only toward inventories it empties into. `./gradlew runGameTestServer` passes all 293
GameTests, all 299 with `-Pcompanions`, and a dev client drew the outlets.

This is the list of things to check, roughly in the order they will come up.

## 1. Dependencies resolve

`./gradlew build`. If a dependency is missing, check its coordinates on the maven before changing code:

| Property | Value | Fallback / where to look |
|---|---|---|
| `create_version` | `6.0.8-+` | `6.0.7-281`. Artifact `com.simibubi.create:create-1.20.1:<v>:slim` on maven.createmod.net |
| `ponder_version` | `1.0.91` | `net.createmod.ponder:Ponder-Forge-1.20.1`, use the version Create's own `gradle.properties` names |
| `flywheel_version` | `1.0.6-beta-266` | `dev.engine-room.flywheel:flywheel-forge-api-1.20.1` / `flywheel-forge-1.20.1`, same rule |
| `registrate_version` | `MC1.20-1.3.3` | `com.tterrag.registrate:Registrate` on maven.ithundxr.dev/mirror |
| `jei_version` | `15.20.0.106` | runtime only; any 15.x for 1.20.1 is fine |

PNG textures copied out of the shared project folder carry an extra `caBX` metadata chunk that the storage adds.
The pixels are unchanged and Minecraft skips the chunk; `python3 tools/gen_assets.py` rewrites clean copies anyway.

## 2. Compile errors most likely to show up

- **`ModBlockEntities.TIERED_KINETIC`**: `.visual(() -> TieredKineticVisual::create, false)` must match
  `SimpleBlockEntityVisualizer.Factory<TieredKineticBlockEntity>`, and `.validBlocks(KineticFamilies.allEntries())`
  passes a `BlockEntry<? extends Block>[]` as varargs. Create registers its own shaft and cogs the same way. If
  generics still complain, make `allEntries()` return `NonNullSupplier<? extends Block>[]`.
- **`TieredKineticVisual.LargeCog`** copies Create's `BracketedKineticBlockVisual.LargeCogVisual`; if a method was
  renamed between Create 6.0.7 and 6.0.8 (`rotationAxis()`, `relight`, `_delete`), mirror whatever Create's class does.
- **`KineticFamilies`** cogwheel item: `.item(CogwheelBlockItem::new)` needs the block type to be a `CogWheelBlock`
  (`TieredCogWheelBlock` extends it).
- **`McRemix.REGISTRATE`**: `setTooltipModifierFactory`, `ItemDescription.Modifier`, `KineticStats.create`,
  `FontHelper.Palette.STANDARD_CREATE` as in Create 6's own `Create.REGISTRATE`.
- **Ponder scenes** (`ponder/*Scenes.java`): `CreateSceneBuilder` wraps the plain `SceneBuilder`; method names follow
  Ponder 1.0 (`scene.world()`, `scene.overlay()`, `scene.effects()`).

## 3. Mixins

`mcremix.mixins.json` has two:

- `KineticBlockEntityMixin` (Create class, `remap = false`): injects at the tail of `tick` and shadows the `stress` field.
- `AbstractFurnaceBlockEntityMixin` (vanilla, remapped): static `serverTick` at HEAD.

The vanilla one needs `mcremix.refmap.json` in the jar. If the game logs "No refMap loaded" or an
injection fails in production but not in dev, check the `mixin { add sourceSets.main, ... }` block in `build.gradle`.

## 4. In game (dev client, a new creative world)

- [x] JEI shows every `mcremix` item; the 49 shafts and cogs render (not purple/black) in hand and placed, and spin.
- [x] Copper shafts oxidize (`/gamerule randomTickSpeed 1000`), honeycomb waxes them, an axe scrapes them back.
      Iron only rusts in rain or next to water. Goggles show friction and limits.
- [x] A creative motor at 96 RPM grinds a wooden shaft until it breaks; at 192 RPM Create's andesite shaft breaks;
      copper and iron hold to 256.
- [x] An overstressed network snaps its wooden shaft first and the source side starts turning again.
- [x] Kiln: click logs into the chamber and planks into the firebox, get charcoal. Hoppers in from the top and sides,
      out from the bottom. Comparator reads the heat (0 to 4). Goggles show heat, fuel and slot states.
- [x] Kiln on a lit blaze burner fires raw iron into a bloom; four anvil strikes give an ingot, the Mechanical Press
      does it in one. Slag works as bone meal; washing it gives nuggets.
- [x] Furnace, smoker and blast furnace: right-click shows the action-bar message and a puff of ash; a hopper-fed
      furnace does not smelt.
- [x] `/locate structure minecraft:village_plains` (and `village_desert`, a zombie village): no villagers, zombie
      villagers or golems, no diamonds or iron gear in chests. Villagers cannot breed; curing a zombie villager from a
      night spawn works. With WWOO installed, check zombie villagers still spawn at night in its biomes: they are now
      the only source of villagers.
- [x] Vanilla enchanting is untouched.
- [x] Ponder (hold W) on the kiln, iron bloom, any copper shaft, the wooden shaft: all five scenes play, text shows
      as sentences (not `mcremix.ponder...` keys), the kiln faces the camera.
- [x] With Immersive Weathering installed, a wood-fired kiln slowly lays soot on walls above it.

## 5. Phase 2: the Bellows and Power Grid heat (0.2.0)

Written after the first compile, so also not built yet. New files: `bellows/BellowsBlock.java`,
`bellows/BellowsBlockEntity.java`, `client/BellowsRenderer.java`, `client/BellowsEffects.java`,
`ponder/BellowsScenes.java`, `compat/PowerGridCompat.java`; the kiln now implements `heat/BellowsTarget`.

Most likely compile snags:

- **`ModBlocks.BELLOWS`** registers its stress with `BlockStressValues.IMPACTS.register(block, DoubleSupplier)` in
  `.onRegister`, because `CStress.setImpact` throws for blocks outside Create (the 0.1.0 crash).
- **`BellowsRenderer`** chains `SuperByteBuffer.translate(float, float, float)`, `.scale(float, float, float)` and
  `.light(int)` (Flywheel's `TransformStack`), and calls the static `getAngleForBe` and `kineticRotationTransform`.
- **`ModPartialModels`** now has three static `PartialModel.of(...)` fields; they load when `init()` first touches the
  class, before models bake.

In game:

- [x] The bellows renders in hand and placed (base, andesite casing with a copper nozzle, leather bag, plank lid,
      crank and yoke at the back). Clicking the side of a kiln or campfire points the nozzle into it; on the ground it
      points where you look; sneaking turns it round. A shaft only connects at the back.
- [x] Turning, the lid pumps once per revolution and the crank pin stays inside the yoke slot. Each downstroke puffs
      air from the nozzle with a soft breath; a fire in front flares. The Ponder scene plays (hold W on the bellows).
- [x] A kiln on a lit campfire with raw copper: Warm and "needs Hot" without the bellows, Hot with it (goggles on the
      kiln show "Fanned by bellows", goggles on the bellows show "Fanning the kiln"). Stop the shaft: back to Warm.
- [x] Blowing straight into a kiln burning charcoal: Searing, and the fuel counter on the goggles drops twice as fast.
      A kiln burning coke stays Searing (never Blazing) and its fuel burns at the normal rate. A cold kiln stays cold.
- [x] Raw beef on a fanned campfire cooks in about 15 seconds instead of 30.
- [x] Goggles and the item tooltip show 2 su per RPM (`bellowsStressImpact` in the server config).
- [x] With Create: Power Grid: a kiln on a heating coil or basin heater follows the heater's temperature (Warm from
      200 °C, Hot 400, Searing 600, Blazing 1200; the heater's goggles show °C). A bellows facing a heater says
      "No fire to fan". `compat/PowerGridCompat` finds the temperature by reflection; if Power Grid renames
      `ThermalBehaviour.getTemperature()` or its "thermal" behaviour type, heaters fall back to the blaze heat property.

## 6. Gearbox tiers and the Stone Anvil (0.3.0)

Also not built yet. New files: `kinetics/TieredGearboxBlock.java`, `kinetics/WeatheringGearboxBlock.java` (copper),
`kinetics/RustingGearboxBlock.java` (iron, following the tester's fix for shafts and cogs: it is not a
`WeatheringCopper`, so a dry iron gearbox does not hold back copper near it), `kinetics/TieredGearboxBlockEntity.java`,
`client/TieredGearboxRenderer.java`, `client/TieredGearboxVisual.java`; the gearbox scene is `KineticScenes.gearboxes`. `KineticPart.GEARBOX` makes `KineticFamilies` register a wooden gearbox
and 16 copper and iron ones (66 kinetic variants in all).

Most likely compile snags:

- **`TieredGearboxBlock`** extends Create's `GearboxBlock` (so Create's propagation treats it as a gearbox and it plays
  the cog ambience) and overrides `getBlockEntityType()` with `BlockEntityType<? extends GearboxBlockEntity>`.
  `getDrops` passes an upright state to `super` so a gearbox on its side drops itself rather than Create's vertical
  gearbox item.
- **`TieredGearboxVisual` / `TieredGearboxRenderer`** are Create's `GearboxVisual` / `GearboxRenderer` with the shaft
  half swapped for `ModPartialModels.shaftHalfOf(variant)`; the nine `*_shaft_half` partials are registered in
  `init()`.
- **`ModBlockEntities`**: `TIERED_KINETIC` now takes `KineticFamilies.entries(v -> v.part() != KineticPart.GEARBOX)`
  (it used to take every entry) and `TIERED_GEARBOX` takes the gearboxes. A gearbox on the wrong type would crash on
  placement with a block entity type mismatch.

In game:

- [x] All 17 gearboxes render placed and in hand (frame, sunken panel, bushings with material shaft halves turning in
      them). Copper ones show patina specks by stage, iron ones rust; waxed ones look like their stage.
- [x] Placed on open ground a gearbox stands upright (shafts on the four sides). Placed under or on top of a vertical
      shaft it lies on its side and connects to it, turning so it also meets a shaft beside it. Sneaking lays it down
      anywhere. A wrench rotates it; broken while lying down it drops itself, not Create's vertical gearbox.
- [x] Driven from one side, the outputs turn like Create's gearbox (straight through reversed, corners mirrored),
      both with Flywheel's instanced rendering and with its backend switched off (the renderer fallback).
- [x] Create's gearbox spun at 192 RPM grinds and breaks; a copper or iron gearbox at 192 RPM does not. A wooden
      gearbox snaps first when the network is overstressed.
- [x] Goggles on a tiered gearbox show material, top speed, load limit and stage like the shafts. A weathered copper
      gearbox adds 0.5 su per RPM to its network.
- [x] Copper gearboxes oxidize over time; iron ones rust only in the rain or next to water. Honeycomb waxes, an axe
      scrapes, exactly like the shafts. Crafting a gearbox with honeycomb gives the waxed one.
- [x] Recipes: four wooden shafts around a stripped log, four copper cogwheels around copper casing, four iron
      cogwheels around an industrial iron block (Create's, stonecut from iron).
- [x] The "Gearbox materials" Ponder scene plays from any tiered gearbox and from Create's gearbox (hold W).

### Stone Anvil

Bloom iron had a gap: a bloom needs an anvil (31 iron) or a Mechanical Press (an iron block), and villages no longer
hand out iron. New file `smithing/StoneAnvilBlock.java` (three blocks in `ModBlocks`: stone, chipped and damaged
stone anvil); `item/BloomHammering.java` now accepts any block in `#mcremix:bloom_anvils` (vanilla's `#minecraft:anvil`
plus the stone anvils) instead of `#minecraft:anvil`, so tests that strike blooms on a vanilla anvil still apply.

Most likely compile snags: `ModBlocks` refers to `ModBlocks.CHIPPED_STONE_ANVIL` inside a lambda before it is declared
(qualified on purpose; a bare name is an illegal forward reference); `BloomHammering` reads the anvil's height with
`state.getShape(level, pos).max(Direction.Axis.Y)`.

- [x] Recipe: three smooth stone over one stone over three cobblestone. It places with its long top across your view,
      like a vanilla anvil, and its outline matches the model (cobblestone foot, stone waist, smooth scorched top).
- [x] Striking a bloom on it throws sparks, a lava pop and stone grit with a dull thud; four strikes give an ingot.
      Particles sit on its top, which is two pixels lower than a vanilla anvil's.
- [x] About one finished bloom in five cracks it: chipped (a corner knocked off, a crack across the face), damaged (a
      second corner, more cracks), then it breaks with stone debris and drops three cobblestone. Mined with a pickaxe
      it drops itself at its current stage.
- [x] Right-clicking it with anything but a bloom does nothing (no menu). Vanilla anvils still strike blooms and still
      wear only rarely.
- [x] The bloom Ponder scene strikes on a stone anvil, cracks it twice, crumbles it, then drops an iron anvil in its
      place. It also opens from the stone anvils (hold W).

### JEI tabs

New package `compat/jei/`: `RemixJeiPlugin` (`@JeiPlugin`), `KilnCategory`, `KilnHeatCategory`, `BloomCategory`,
`JeiText`. `build.gradle` gains `modCompileOnly` on `jei-1.20.1-common-api` and `jei-1.20.1-forge-api` at the pinned
`jei_version` (15.20.0.106); the runtime jar was already there. Written against JEI's API at tag v15.20.0.

Most likely compile snags: the categories extend JEI's `AbstractRecipeCategory` (width and height in the constructor,
because `getBackground()` is deprecated for removal in 15.20); text and the arrow go through `createRecipeExtras`
(`addText`, `addAnimatedRecipeArrow`, `addRecipeArrow`); slot notes use `addRichTooltipCallback`. Recipes are read
from `Minecraft.getInstance().getConnection()`, which JEI only calls after login, tags and recipes have arrived.

- [x] A Kiln tab, catalyst Kiln: firing recipes (iron bloom at Searing with "Blazing: twice as many" for crushed
      iron, coke at Hot, charcoal and bricks at Warm, netherite scrap at Blazing) and non-food smelting at Hot (glass,
      copper, gold, smooth stone). No food, and no smelting recipe whose input a firing recipe already takes (logs
      show charcoal at Warm only). Times divide by the `kilnSpeed` config. Hovering the heat line explains that each
      level hotter halves the time.
- [x] A Kiln Heat tab, catalysts Kiln and Bellows: four pages (Warm to Blazing). Fuel cycles wood items, then coal and
      charcoal, then coke and blaze rods; Blazing has no fuel slot. Below cycles campfire and magma, lava bucket and
      flint and steel, soul campfire and soul soil, and the blaze burner (and blaze cake) with a note on how it is fed;
      Power Grid's heaters appear with their temperature when Power Grid is installed. Hot and Searing show the Bellows.
- [x] A Striking Blooms tab, catalysts Stone Anvil and Anvil: bloom to ingot, plus slag marked 50% chance.
- [x] The furnace, smoker, blast furnace and fuel tabs are gone; with `coldFurnaces = false` they come back. The
      furnace, smoker and blast furnace have info pages, and so does slag.
- [x] Without JEI installed, nothing in `compat/jei` loads (the game starts and the mod works).

## 7. Drill bits (0.4.0)

Create's Mechanical Drill becomes the iron member of a family of four. New package `drilling/`: `DrillBits` (the four
tiers: Forge tier, speed, stress; the grinding effect and the goggle and hover lines), `TieredDrillBlock` (extends
Create's `DrillBlock`), `TieredDrillBlockEntity` (extends `DrillBlockEntity`, scales `getBreakSpeed`),
`TieredDrillMovementBehaviour` (extends `DrillMovementBehaviour`). Client: `TieredDrillRenderer` (extends
`DrillRenderer`), `TieredDrillVisual` (the block entity visual, the contraption renderer and actor visual, copied from
Create's drill with the head as a parameter), `ClientEvents` (item tooltip line), three heads in `ModPartialModels`.
Mixins: `BlockBreakingKineticBlockEntityMixin` and `DrillMovementBehaviourMixin` (both `remap = false`) apply the tier
to fixed and moving drills, and `KineticBlockEntityMixin` gains two injections for the drill goggle and hover lines.
The tier test is `TierSortingRegistry.isCorrectTierForDrops(Tier, BlockState)` (Forge 1.20.1).

| Drill | Bits texture | Cuts like | Speed | Stress (su/RPM) | Recipe |
|---|---|---|---|---|---|
| Stone Drill | cobblestone | stone pickaxe | ×2/3 | 2 | cobblestone around andesite alloy, andesite casing below |
| Mechanical Drill (Create) | anvil (Create's own) | iron pickaxe | ×1 | 4 | unchanged |
| Golden Drill | gold block | golden pickaxe | ×2 | 2 | gold ingots around an iron ingot, andesite casing below |
| Diamond Drill | diamond block | diamond pickaxe | ×4/3 | 8 | diamonds around an iron ingot, andesite casing below |

Most likely compile snags:

- **`TieredDrillBlock`** overrides `getBlockEntityType()` with `BlockEntityType<? extends DrillBlockEntity>` and `use`
  with its own catnip placement helper, because Create's drill helper only matches Create's drill item.
- **`TieredDrillVisual.create`** returns `BlockEntityVisual<? super TieredDrillBlockEntity>` built from Create's
  `OrientedRotatingVisual` constructor (from `Direction.SOUTH` to the drill's facing), as Create's
  `OrientedRotatingVisual.of` does; `TieredDrillVisual.Actor` is Create's `DrillActorVisual` with the head passed in.
- **`ModBlocks.drill`** registers the movement behaviour with Create's `MovementBehaviour.movementBehaviour(...)` and
  the stress impact straight into `BlockStressValues.IMPACTS` (Create's `CStress` helpers only accept Create's blocks).
- **Mixins**: the canBreak injections check `getReturnValueZ()` and only ever turn true into false. The contraption
  one only acts when the behaviour's class is exactly `DrillMovementBehaviour`, so other mods' drill subclasses keep
  their own rules (as do their blocks: `DrillBits.of` returns null for any drill that is neither ours nor Create's).

In game:

- [x] The three drills render placed and in hand: Create's drill with cobblestone, gold or diamond bits on the head.
      The heads spin with Flywheel's instanced rendering and with its backend off, placed and on a contraption.
- [x] The item tooltip of every drill, Create's included, has a grey line under the name ("Iron bits: cut like an
      iron pickaxe"). Shift shows the summaries of ours; stress impact shows Low for stone and gold, High for diamond.
- [x] Recipes: three cobblestone around andesite alloy on andesite casing (no iron), three gold ingots or three
      diamonds around an iron ingot on andesite casing, the shape of Create's drill. Create's drill recipe is unchanged.
- [x] At 32 RPM on stone, a golden drill cuts about twice as fast as Create's, a diamond drill a third faster, a stone
      drill a third slower. Goggles show the bits, the speed line and the stress: 64, 64, 128 and 256 su at 32 RPM
      for stone, gold, iron and diamond.
- [x] A golden drill facing iron, copper or lapis ore does not cut it: every half second it throws sparks, crits and
      grit off the face with a high grinding squeal. Looking at it (no goggles needed) shows "Too hard for gold bits"
      and "Iron Ore needs stone, iron or diamond bits". A stone drill does the same on gold, diamond, emerald and
      redstone ore; Create's drill on obsidian, crying obsidian and ancient debris ("needs diamond bits"). Swapping
      the block for one it can cut starts it again within half a second.
- [x] The diamond drill cuts obsidian. With `ironDrillBits = false` in the server config, Create's drill cuts
      obsidian again and loses its bits line; the other three keep their tiers.
- [x] On a mechanical piston, a drill that meets a block its bits cannot cut stops the piston as bedrock would, with
      one burst of sparks. On a bearing, the drills skip such blocks.
- [x] Contraption Controls switch the new drills on and off (they are in `#create:contraption_controlled`). A cobble
      generator under a golden drill feeds a belt or hopper below as it does under Create's drill.
- [x] Clicking a placed tiered drill with any drill item places it alongside, like Create's drill helper.
- [x] The "Drill bits" Ponder scene (four drills on stone, then iron ore, diamond ore and obsidian) opens from all
      four drills, Create's included, under Kinetic Appliances; the three new drills are also under Contraption
      Actors.

## 8. Slag roads and kiln-heated boilers (0.5.0)

New package `road/`: `SlagRoadBlock` (extends vanilla's `DirtPathBlock`) and `RoadEvents` (a Forge-bus
`LivingTickEvent` handler that adds a transient `MOVEMENT_SPEED` modifier, +25% `MULTIPLY_BASE`, to players and to
anything a player rides while it stands on a road, and takes it off when it steps off). `SlagItem.useOn` metals a dirt
path into a road. The kiln is registered in Create's `BoilerHeater.REGISTRY`, which reads
`KilnBlockEntity.boilerHeat()`; the kiln tells the tank above to re-read its heaters whenever that value changes,
because a boiler only re-reads them when a block under it changes. Ponder: `RoadScenes` (`roads/slag_road`) and
`BoilerScenes` (`kiln/boiler`).

| Kiln's own fire (fuel, plus Bellows) | Boiler heat | The same as |
|---|---|---|
| none | what the block under the kiln would give the boiler | (passes through) |
| Warm or Hot | passive | a campfire under the boiler |
| Searing | 1 | a fed blaze burner |

The boiler gets the higher of the kiln's fire and the heat from underneath, so a superheated blaze burner under a kiln
still gives 2 and a soul campfire under one still gives passive. The kiln burns fuel when the chamber needs the heat
(as before) or when the boiler on top is active (an engine or whistle) and the fuel, fanned if a Bellows is blowing,
would give it more than the heat coming up from below.

Most likely compile snags:

- **`SlagRoadBlock`**: `DirtPathBlock`'s constructor is protected in 1.20.1, so the subclass has a public one that
  calls it. Registrate copies `Blocks.DIRT_PATH`'s properties, and a copy leaves out view blocking and suffocation, so
  `ModBlocks.SLAG_ROAD` sets both again.
- **`ModBlocks.KILN`** registers the heater in `.onRegister` with `BoilerHeater.REGISTRY.register(block,
  KilnBlock::boilerHeat)`, the way Power Grid registers its heaters. `KilnBlock.boilerHeat(Level, BlockPos,
  BlockState)` matches `BoilerHeater.getHeat`.
- **`KilnBlock.CHIMNEY`** (`chimney=true|false` in the blockstate, 40 variants) is set on placement and in
  `updateShape` from the shape of the block above (`Shapes.joinIsNotEmpty` against the chimney's 4×4×4 box), so a
  kiln placed under a block, or a block placed on a kiln, updates it. `ponder/kiln/boiler.nbt` stores the kiln with
  `chimney=false`.
- **`KilnBlockEntity`** uses `FluidTankBlockEntity.getControllerBE()`, its public `boiler` field
  (`BoilerData.isActive()`), `updateBoilerTemperature()`, and `BoilerHeater.findHeat`, `NO_HEAT` and `PASSIVE_HEAT`.
- **`RoadScenes`** moves the two horses with the seven-argument `Entity.lerpTo(x, y, z, yRot, xRot, steps,
  teleport)` of 1.20.1 and sets `Entity`'s public `xo`, `yo`, `zo`, `yRotO` and `LivingEntity`'s public `yBodyRot`,
  `yHeadRot` (and their `O` fields) directly.
- **`BoilerScenes`** edits the tank's `Boiler` tag (`PassiveHeat`, `ActiveHeat`, `Supply`) and the powered shaft's
  `EnginePower` with `modifyBlockEntityNBT`, then sets the shaft to 16 or 64 RPM, as Create's own steam engine scene
  does. The schematic `ponder/kiln/boiler.nbt` holds a 2×2 tank with its controller tag (`Size`, `Height`,
  `Window: 0`, a `Boiler` tag with `Engines: 1`), a steam engine on the tank's north wall and its powered shaft (with
  `EnginePos`). If the gauge or engine looks wrong in the scene, compare with Create's `steam_engine` schematic.

In game:

- [x] Slag used on a dirt path turns it into a Slag Road with a gravel crunch, grit and a little ash, and is used up
      (not in creative). On grass, crops and saplings slag still works as bone meal. A Deployer holding slag lays
      roads too.
- [x] The road looks right at path height: a dark cinder top with glassy chips and rust specks; the sides show dirt
      under a slag rim; it is not see-through and you cannot suffocate in it (the same as a path).
- [x] Walking on it is a quarter faster, and a walking player's view widens slightly, as with Soul Speed. A horse
      ridden onto it speeds up the same way. The boost lasts through jumps and stops as soon as you step off; a
      riderless horse or a zombie gets nothing.
- [x] A solid block put on the road turns it into dirt, like a path. Hoes do not till it. Mobs do not spawn on it.
- [x] Breaking it with a shovel drops dirt and one slag; Silk Touch drops the road, and the road placed under a
      solid block becomes dirt.
- [x] JEI has an info page for the Slag Road, and the slag page mentions roads.
- [x] A kiln under a 2×2 fluid tank holding water, with a steam engine on the tank: charcoal in the firebox gives
      passive heat (the engine turns at 16 RPM, an eighth of its power); goggles on the kiln say "Keeps the boiler
      above simmering, as a campfire would" and the hint below it. Coal Coke, or charcoal with a Bellows blowing into
      the kiln, gives heat 1: 64 RPM at full power, and "Boils the boiler above like a fed Blaze Burner". The fanned
      charcoal burns twice as fast.
- [x] A soul campfire under the kiln (Searing with no fuel) only simmers the boiler; a superheated blaze burner under
      the kiln gives it 2 ("like a superheated Blaze Burner").
- [x] Without an engine or whistle on the tank, an empty kiln keeps its coke. With one, the coke burns while the
      chamber is empty, and anything put in the chamber fires meanwhile. When the coke runs out the engine drops back
      within a second.
- [x] Under a tank, the kiln's chimney stub disappears and its smoke leaves from its back, just under the tank,
      instead of rising from the top. The same happens under any block that covers the chimney (a bottom slab, a
      solid block); a torch or hopper on top leaves it standing. Breaking the block above brings the chimney back.
- [x] Ponder: "Slag roads" opens from the slag road and from slag (the road is metalled block by block, then two
      horses race: the one on the road ends a quarter ahead, legs moving at their pace and still once they stop).
      "Heating a steam boiler with a Kiln" opens from the kiln, Coal Coke, and Create's steam engine and fluid tank;
      the gauge rises one notch on charcoal and fills on coke, and the engine speeds up.

GameTests worth adding: slag used on a path makes a road; a player on a road gets the speed modifier and loses it a
block away; a kiln burning coke under a tank with an engine gives boiler heat 1 and uses fuel with an empty chamber,
one on a soul campfire gives passive, and without an engine the coke stays in the firebox.

## 9. Ruined villages (0.6.0)

Village buildings generate as ruins. `village/RuinProcessor` is a structure processor (registered as `mcremix:ruin` in
`registry/ModStructureProcessors`); `mixin/SinglePoolElementMixin` adds it to the place settings of every jigsaw piece
whose template path starts with `village/`, after the piece's own processors (`RuinProcessor.HOUSE` for templates under
a `houses/` folder, `RuinProcessor.OTHER` for the rest). Per block it removes torches, half the lanterns and four glass
panes in ten (placing cave air, see below), turns lit campfires cold, jack o'lanterns into carved pumpkins, crops into a
random age (a quarter of them into grass), half the potted flowers into potted dead bushes, and cobblestone and stone
bricks (with their stairs, slabs and walls) mossy or cracked. Its `finalizeProcessing` then hangs cobwebs in the top
corners of rooms, only where the corner is closed in on all four sides (a wall, door or window of the piece within 16
blocks each way), so none hang under eaves, on porches or under a well's roof. Every roll is seeded by the block's
position, so a piece spanning two chunks decays the same in both.
`ruinedVillages` in the server config switches it off.

With Immersive Weathering installed, one house in six (seeded by the piece's position) burned down instead: logs,
planks and wooden stairs, slabs, fences and gates become IW's charred blocks (looked up by id, properties copied with
`withPropertiesOf`), glass, hay, wool, carpets and flowers go, and seven in ten of the topmost charred blocks of each
column (the roof) are removed. Burnt houses get no cobwebs.

Also in 0.6.0, both data only: IW's charred log, planks, slab, stairs, fence and fence gate are in
`#mcremix:kiln_fuel/hot` (optional entries), and with Power Grid, `recipes/magnetization/slag.json` (guarded by
`forge:mod_loaded`) turns slag under an electromagnet into one iron nugget plus a second half the time.

Most likely compile snags:

- **`SinglePoolElementMixin`** shadows `SinglePoolElement.template` (`Either<ResourceLocation, StructureTemplate>`)
  and injects at the return of `getSettings(Rotation, BoundingBox, boolean)`. `LegacySinglePoolElement` (most of
  vanilla's village pieces) overrides `getSettings` and calls super, then adds its air-skipping processor, so it is
  covered and the ruin processor runs first. The air-skipping processor drops plain air, which would leave whatever
  stood there before in place of a removed pane or torch, so removed blocks are placed as `Blocks.CAVE_AIR`
  (`RuinProcessor.GONE`), which it keeps.
- **`RuinProcessor`** overrides Forge's `process(LevelReader, BlockPos, BlockPos, StructureBlockInfo,
  StructureBlockInfo, StructurePlaceSettings, StructureTemplate)` (the form Create's `SchematicProcessor` uses) and
  vanilla's `finalizeProcessing(ServerLevelAccessor, BlockPos, BlockPos, List, List, StructurePlaceSettings)`. It uses
  `StructurePlaceSettings.getRandom(BlockPos)` and `getBoundingBox()`, `CropBlock.getStateForAge` and `getMaxAge`,
  `FlowerPotBlock.getContent`, `Block.withPropertiesOf`, `Blocks.GRASS` (the 1.20.1 name of short grass) and
  `Tags.Blocks.GLASS_PANES`.
- **`ModStructureProcessors`** registers the type with a `DeferredRegister` on `Registries.STRUCTURE_PROCESSOR`, as
  Create's `AllStructureProcessorTypes` does. The codec is `Codec.BOOL.fieldOf("houses").xmap(...).codec()`; it is
  never used, since the processor is only ever added in code.
- The burnt-house roll uses `Mth.getSeed(BlockPos)` and `RandomSource.create(long)`, the roof pass
  `BlockPos.asLong(int, int, int)`, and the IW lookup `ForgeRegistries.BLOCKS.getValue`, which returns air for an id
  that is not registered.

In game (new worlds; `/place structure minecraft:village_plains` and the desert, savanna, snowy and taiga ones build a
whole village where you stand):

- [x] Plains and taiga villages: about a third of the cobblestone (walls, stairs, slabs) is mossy; stone bricks are
      mossy or cracked. Desert and savanna villages, built of sandstone, terracotta and acacia, change less.
- [x] Glass panes: some windows are empty (open to the air, even in a house dug into a hillside), and a pane next to
      a missing one keeps an arm into the hole, like a shard.
- [x] No torches anywhere, lamp posts are bare, campfires are unlit (break one: two charcoal), jack o'lanterns are
      carved pumpkins, about half the lanterns and potted flowers are gone or dead.
- [x] Inside houses, cobwebs in some top corners of rooms (about one per room), never outside, in walls, under floors
      or in mid-air.
- [x] Fields: crops at mixed ages, about a quarter replaced by grass. Bone meal still grows the rest.
- [x] Beds, bells, doors, workstations, chests and hay are all intact. Still no villagers, zombie villagers or golems.
- [x] A house cut by a chunk border decays the same on both sides (no seams of mossy against clean blocks along the
      border).
- [ ] At night, monsters spawn in the dark streets.
- [x] With `ruinedVillages = false`, newly generated villages are intact (torches lit, no cobwebs).
- [ ] With WWOO, villages in its biomes are ruined the same way.
- [x] With Immersive Weathering: in a few villages, about one house in six is a black shell of charred logs and
      planks with most of its roof gone, no glass, hay or wool, and no cobwebs. Breaking a charred block under an
      unsupported one can bring it down (IW's own rule). Without IW, no house burns.
- [x] A charred log in the firebox burns Hot (goggles show "Hot"); the JEI Kiln Heat tab lists IW's charred blocks
      under Hot.
- [x] With Power Grid: slag under an electromagnet gives iron nuggets (JEI shows the recipe in Power Grid's
      magnetizing tab). Without Power Grid the recipe is skipped without errors in the log.

A GameTest can call `RuinProcessor.OTHER.process(...)` on block infos (torch to cave air, cobblestone sometimes
mossy, lit campfire cold) without generating a village.

## 10. Things that are design calls, not bugs

Netherite needs a Blazing furnace; smokers and blast furnaces are crafted around a Furnace (or a salvaged Cold
Furnace) and only serve as villager workstations; vanilla's furnace is called the Cold Furnace even on a server with
`coldFurnaces = false`, since names are the client's; Create's gearbox, clutch, gearshift, chain drive and chain
gearshift are limited to 128 RPM like its andesite shafts (its belts, speedometer and stressometer are not, so a fast
line can still be belted and measured); the stone anvil has no repair menu; Create's drill no longer cuts obsidian,
crying obsidian or ancient debris (it has iron bits); a golden saw does no stonecutting; a furnace boils a steam
boiler at most one heat level by itself, and heat under a furnace reaches its boiler unchanged; slag roads speed up
players and ridden mounts only, and a block put on a road turns it to dirt; villages generate as ruins with no light,
so they are dangerous at night. All of it is in `serverconfig/mcremix-server.toml` or data.

## 11. Saw blades (0.7.0)

Create's Mechanical Saw becomes the iron member of a family of four, like the drill in section 7. New package
`sawing/`: `SawBlades` (the four blades: Forge tier, speed, stress, whether it does stonecutting; the goggle line),
`TieredSawBlock` (extends Create's `SawBlock`), `TieredSawBlockEntity` (extends `SawBlockEntity`: scales
`getBreakSpeed` and the processing time, and holds and sparks an item its blade cannot cut), `TieredSawMovementBehaviour`
(extends `SawMovementBehaviour`) and `TieredSawArmPointType` (registered in `registry/ModArmPoints`). Client:
`TieredSawRenderer` (extends `SawRenderer` and draws our blade; the shaft is Create's `SawVisual`) and six blade
partials per saw in `ModPartialModels`. Mixins: `SawBlockEntityMixin` (`remap = false`), and `KineticBlockEntityMixin`
gains the saw goggle line. Ponder: `SawScenes.blades`; the new drills and saws also get Create's own drill and saw
scenes.

| Saw | Blade texture | Speed | Stress (su/RPM) | Stonecutting | Recipe (andesite casing below each) |
|---|---|---|---|---|---|
| Stone Saw | chipped grey stone | ×2/3 | 2 | yes | three cobblestone, stone or smooth stone slabs around andesite alloy |
| Mechanical Saw (Create) | Create's own | ×1 | 4 | yes | unchanged (iron sheets around an iron ingot) |
| Golden Saw | gold, hooked teeth | ×2 | 2 | no | three golden sheets around an iron ingot |
| Diamond Saw | steel set with diamond segments | ×4/3 | 8 | yes | three diamonds around an iron sheet |

Most likely compile snags:

- **`SawBlockEntityMixin`** injects at the return of Create's private `getRecipes()` (a `List<? extends Recipe<?>>`)
  and at the head of the private `applyRecipe()`, and shadows the private `playEvent` field. For a golden saw it drops
  `RecipeType.STONECUTTING` recipes; when that leaves none it calls `TieredSawBlockEntity.skate()`, and the skated item
  gets no cut event when it leaves (no chips or cutting sound).
- **`TieredSawBlockEntity.start(ItemStack)`** overrides Create's public `start` (the processing inventory's callback is
  `this::start`, so it reaches ours). It runs Create's, then divides `inventory.remainingTime` by the blade's speed and
  copies it into `recipeDuration` (both floats), on the logical side only (`!level.isClientSide || isVirtual()`, so
  Ponder runs at the blade's speed too). Create's `sendData` only marks the block changed and the packet is written at
  the end of the tick, so the client gets the scaled time.
- **`ModArmPoints`** uses `DeferredRegister.create(CreateRegistries.ARM_INTERACTION_POINT_TYPE, McRemix.ID)`. Create's
  registry is a vanilla `MappedRegistry` in `BuiltInRegistries`, so Forge fires `RegisterEvent` for it, and Create
  sorts the types when it freezes. Create's own `SawType` only matches `AllBlocks.MECHANICAL_SAW`, hence our type; it
  reuses Create's public `DepotPoint`.
- **`TieredSawBlock.registerMovementChecks`** adds a `BlockMovementChecks.registerNotSupportiveCheck` for our saws,
  because Create's fallback only names its own saw and drill: without it a piston drags the block in front of a saw
  along instead of cutting it. It runs at common setup beside the drills' check from the 0.4.0 fixes, on the main
  thread: calling it from the mod constructor loaded `TieredSawBlock` and Create's `SawBlock` while other mods were
  being built in parallel, and both register a placement helper with catnip's `PlacementHelpers.register`, which is not
  synchronised (Create's own movement check registration is).
- **`ModBlockEntities.TIERED_SAW`** uses Create's `SawVisual` (`.visual(() -> SawVisual::new)`, the shaft only) and
  `TieredSawRenderer`, which overrides `renderBlade` and has its own static `renderInContraption`, Create's with the
  blade passed in. `TieredSawMovementBehaviour.renderInContraption` is `@OnlyIn(Dist.CLIENT)`, like Create's.
- **Ponder text**: story boards our plugin registers read their text under our namespace, Create's own scenes
  included, so `gen_assets.py` copies the texts of Create's drill and saw scenes (`mcremix.ponder.mechanical_saw_*`,
  `mcremix.ponder.mechanical_drill*`, `mcremix.ponder.shared.*`) into our lang file, checked against Create's English.
- **JEI**: `RemixJeiPlugin.registerRecipeCatalysts` finds Create's categories by uid (`create:sawing`,
  `create:block_cutting`) through `getJeiHelpers().getAllRecipeTypes()`, so nothing of Create's JEI code is needed.

In game:

- [x] The three saws render placed (every facing) and in hand: Create's saw with a stone, gold or diamond blade. The
      blade turns both ways with the shaft and stops with it, with Flywheel's backend on and off, and on a
      contraption it turns while cutting and stands still while trailing.
- [x] Every saw's item tooltip, Create's included, has a grey blade line ("Gold blade: cuts wood, too soft for
      stone"). Goggles show the blade's speed and the stress: 64, 64, 128 and 256 su at 32 RPM for stone, gold, iron
      and diamond.
- [x] Recipes: three slabs (cobblestone, stone or smooth stone) around andesite alloy, three golden sheets around an
      iron ingot, three diamonds around an iron sheet, andesite casing below each. Create's saw recipe is unchanged.
- [x] At 32 RPM, a log on an upward golden saw is stripped about twice as fast as on Create's, on a diamond saw a
      third faster, on a stone saw a third slower. The log slides across at the matching pace, with no jump or pause
      at the end.
- [x] Smooth stone (or anything with only stonecutting recipes) on a golden saw: one high grinding squeal, sparks,
      crits and grit off the blade for about half a second, then the stone moves off uncut with no cutting sound or
      chips. The next log on the same saw is cut normally. The stone, iron and diamond saws stonecut as Create's
      does, the filter slot choosing the output.
- [ ] A belt carrying logs and stone through a golden saw: the logs come out stripped, the stone passes along.
- [x] Horizontal saws fell trees at their blade's speed, fixed and on a bearing or piston; every blade fells vanilla
      trees. On a mechanical piston, each of the four saws cuts the log it is pushed into instead of dragging it along.
- [ ] A Mechanical Arm accepts an upward new saw as a point (click it while placing the arm), feeds it and takes its
      results, as with Create's saw.
- [x] Clicking a placed new saw with any saw item places the new one alongside, facing the same way.
- [x] Contraption Controls switch the new saws (they are in `#create:contraption_controlled`); they break with a
      pickaxe or an axe and drop themselves.
- [x] JEI: the three new saws are catalysts on Create's Sawing tab; the stone and diamond saws (not the golden one)
      also on Block Cutting.
- [x] Ponder: "Saw blades" opens from all four saws under Kinetic Appliances (the new ones are also under Contraption
      Actors); in it the golden saw's smooth stone sparks and comes off uncut. The new saws' Ponder also has Create's
      processing, tree cutting and contraption scenes, and the new drills Create's drill scenes, all with their text
      rather than raw keys.

A GameTest can put smooth stone on a golden saw (`insertItem` or its item handler) at speed and check it comes off
uncut, and the same with a log that comes off stripped.

## 12. Clutch and gearshift tiers (0.8.0)

Create's clutch and gearshift join the andesite tier (`max_speed/128` now lists `create:clutch` and
`create:gearshift`), and wooden, copper and iron ones join the shaft, cog and gearbox families. `KineticPart` gains
`CLUTCH` and `GEARSHIFT` and a `cased` flag (gearboxes, clutches and gearshifts turn shaft halves of their own, so they
are kept off `TIERED_KINETIC`); `KineticFamilies` registers a wooden clutch and gearshift and 16 copper and iron ones
of each, 100 kinetic variants in all. New files in `kinetics/`: `TieredClutchBlock` and `TieredGearshiftBlock` (extend
Create's `ClutchBlock` and `GearshiftBlock`), `WeatheringClutchBlock` and `WeatheringGearshiftBlock` (copper),
`RustingClutchBlock` and `RustingGearshiftBlock` (iron), `TieredClutchBlockEntity` and `TieredGearshiftBlockEntity`
(extend `ClutchBlockEntity` and `GearshiftBlockEntity`, with the gearbox's friction and goggle lines). In `client/`:
`TieredSplitShaftVisual` and `TieredSplitShaftRenderer`, Create's `SplitShaftVisual` and `SplitShaftRenderer` with the
material's shaft half. Ponder: `KineticScenes.clutches`, and the new blocks also get Create's own clutch and gearshift
scenes.

| Part | Recipe (shapeless, each with redstone dust) |
|---|---|
| Wooden Clutch | stripped log, wooden shaft |
| Wooden Gearshift | wooden gearbox (wood has no cogwheels) |
| Copper Clutch / Gearshift | copper casing, and a copper shaft / copper cogwheel |
| Iron Clutch / Gearshift | industrial iron block, and an iron shaft / iron cogwheel |
| Create's Clutch / Gearshift | unchanged (andesite casing) |

Most likely compile snags:

- **`TieredClutchBlock` / `TieredGearshiftBlock`** override `getBlockEntityType()` with
  `BlockEntityType<? extends SplitShaftBlockEntity>`, as Create's do. `getBlockEntityClass()` stays Create's
  `SplitShaftBlockEntity.class`, which our block entities extend.
- **`ModBlockEntities.TIERED_CLUTCH` / `TIERED_GEARSHIFT`** register `TieredSplitShaftVisual::new` (a
  `KineticBlockEntityVisual<SplitShaftBlockEntity>`, so the factory accepts our subclasses) and
  `TieredSplitShaftRenderer::new`. `TIERED_KINETIC` now filters on `!v.part().cased`. A clutch on the wrong type would
  crash on placement with a block entity type mismatch.
- **Render layer**: the end frames are see-through around the shaft, so the new blocks call
  `.addLayer(() -> RenderType::cutoutMipped)`, as Create's clutch does.
- **Ponder text**: `gen_assets.py` copies the texts of Create's clutch and gearshift scenes into our lang, as for the
  drill and saw scenes, checked against Create's English.

In game:

- [x] All 34 render placed (on each axis) and in hand: the gearbox's casing in the material, a red band across a
      clutch's sides or two red strips along a gearshift's, bright while powered, and an open frame on each end around
      a turning shaft half of the material.
- [x] Powered, a tiered clutch stops its far side and a tiered gearshift reverses it, driven from either end, with
      Flywheel's backend on and off: the far shaft half stands still or turns backwards.
- [x] Create's clutch and gearshift spun at 192 RPM grind and break; copper and iron ones at 192 RPM do not. A wooden
      clutch snaps first when its network is overstressed.
- [x] Goggles on one show material, top speed, load limit and stage, as on the gearboxes.
- [x] Copper ones oxidize, iron ones rust only when wet, honeycomb waxes and an axe scrapes, like the shafts. A
      powered clutch that weathers stays powered.
- [x] Recipes as in the table; crafting one with honeycomb gives the waxed one. Wooden ones burn and break fastest
      with an axe, the rest with a pickaxe.
- [x] Ponder: "Clutch and gearshift materials" plays from every tiered clutch and gearshift and from Create's two
      (under Kinetic Materials; the tiered ones are also under Kinetic Relays). The tiered ones also have Create's
      clutch and gearshift scenes, with their text rather than raw keys.

## 12a. Fast, varied weathering (0.8.1, iron only while wet in 0.8.2)

Copper and iron weather within a day, each block by itself, and each stops at a stage of its own (DESIGN.md,
Weathering). `kinetics/Weathering` now covers every mod's blocks, and `mixin/BlockStateBaseMixin` sends every
block's random tick through `Weathering.randomTick` first. A block counts if its stage is vanilla's
`WeatheringCopper.WeatherState` (vanilla's copper, and Create's shingles and tiles, which Create adds to vanilla's stage
maps), Immersive Weathering's `Rustable.RustLevel` (matched by class name, so there is still no compile dependency on
IW), or if it is one of this mod's parts (anything `TieredKinetic`, so new copper or iron parts are covered without
changes). Waxed blocks have no stage and are left alone. The `Weathering*Block` and `Rusting*Block` classes are
unchanged: their own random ticks are what `fastWeathering = false` falls back to.

| Server config (`weathering`) | Default | Effect |
|---|---|---|
| `fastWeathering` | true | Every copper and iron block weathers by these rules. Off: each block's own mod's rules, as in 0.8.0 |
| `weatheringDays` | 0.5 (0.2 to 20) | Average days from fresh to the last stage: a chance of 3 / (days x 17.58) on each random tick, 0.341 by default, so a stage every 2.9 random ticks (about 3.3 minutes) |
| `variedWeathering` | true | Each spot's final stage is `1 + RandomSource.create(Mth.getSeed(pos) ^ worldSeed).nextInt(3)`: exposed, weathered or oxidized/rusted, a third each, fixed for the spot. Off: all the way |
| `ironRustsWhenDry` | false | Immersive Weathering's iron also rusts when dry, as copper oxidizes. Off: only while wet, as this mod's iron parts always do. Named `dryIronRusts` (default true) in 0.8.1; renamed in 0.8.2 so worlds that ran 0.8.1 get the new default instead of keeping the old value |

"Wet" is unchanged from 0.1.0: waterlogged, rain on the block or the block above it, or water beside it. A door weathers
by its lower half, which is wet if either half is; the upper half's own random tick does nothing, and IW's doors copy
the lower half's block to the upper half when it changes.

Most likely snags:

- **The mixin** injects at the head of `BlockBehaviour.BlockStateBase#randomTick` (`m_222972_` in the refmap) and
  cancels it for copper and iron. A mod that replaces random ticking without calling that method (some performance
  mods batch random ticks) would bypass it, and those blocks would keep their own rules.
- **Immersive Weathering renaming `Rustable$RustLevel`** would leave its iron on IW's own rules, silently.
  `CompanionTests.immersiveWeatheringIronRustsOnlyWhileWet` fails if that happens.
- **Cost**: every random tick of every block now costs one `instanceof` check, and copper and IW iron one class-name
  comparison more.

In game (by GameTest; see TEST_REPORT.md):

- [x] A packed floor of 196 copper blocks: nine in ten at their final stage after a day's worth of random ticks
      (18), one stage at a time, at the configured pace, about a third stopping at each stage.
- [x] Vanilla's cut copper, stairs and a waterlogged slab, Create's shingles, tile stairs and a double shingle slab,
      and a copper cogwheel and gearbox each weather to their final stage and keep their shape, facing and
      waterlogging. Waxed copper, Create's waxed tiles and a waxed shaft stay as they are.
- [x] Final stages: a third of spots each, none fresh, neighbours alike a third of the time (no patches or stripes);
      a block at its final stage goes no further, and scraped back with an axe it weathers to the same stage again.
- [x] This mod's iron parts rust only while wet, to their final stage; dry ones stay clean, and copper beside them
      is not held back.
- [x] The world's own random ticks (a chunk ticked at random tick speed 4096) weather copper the same way.
- [x] With IW: its plate iron, cut iron slab, iron bars, trapdoor and doors rust to their final stage while wet, a
      door's upper half with its lower half, waterlogging kept; dry ones stay clean. With `ironRustsWhenDry` on, dry
      ones rust too, and a dry iron shaft still does not.
- [x] `variedWeathering` off: copper goes all the way. `fastWeathering` off: vanilla's neighbour rule is back, dry
      iron parts still do not count as fresher copper, and wet iron parts rust all the way at the old pace.
- [ ] A build in a played world over a day, in rain and in the dry (not done this round).

## 13. Kiln-heated basins (0.9.0)

A Create basin standing on a kiln is heated by the kiln's own fire, as a blaze burner would heat it (DESIGN.md,
"Basins"). `KilnBlockEntity` gains `basinHeat`, a `BlazeBurnerBlock.HeatLevel` saved as `BasinHeat`: `basinHeatOf`
maps the fire (Cold to `NONE`, Warm and Hot to `SMOULDERING`, Searing to `KINDLED`, Blazing to `SEETHING`), and the
basin gets the higher of that and what `BasinBlockEntity.getHeatLevelOf` reads from the block under the kiln. The kiln
burns fuel for a basin only when the basin's input inventory or tank holds something and the fuel (fanned, if blown)
would give at least `FADING`, more than the heat from below. New files: `mixin/BasinBlockEntityMixin`,
`ponder/BasinScenes` (scene "kiln/basin") and the GameTests in `KilnBasinTests`.

Most likely compile snags:

- **`BasinBlockEntityMixin`** injects at the head of `BasinBlockEntity.getHeatLevel()`, which is package-private and
  `@NotNull`, with `remap = false` (Create is not obfuscated) and a
  `CallbackInfoReturnable<BlazeBurnerBlock.HeatLevel>`. With a kiln underneath it returns
  `KilnBlockEntity.basinHeat()` and skips Create's per-tick `cachedHeatLevel`; otherwise Create's lookup runs
  untouched, so a blaze burner or Power Grid basin heater under a basin works as before. The kiln's block state
  cannot be used instead: it shows the chamber's heat, which counts heat from below that the basin would then get
  twice. If the injection does not apply, loading stops with a mixin error naming it.
- **Create calls in `KilnBlockEntity`**: `BasinBlockEntity.getHeatLevelOf(BlockState)` (public static), the public
  fields `inputInventory` and `inputTank` and their `isEmpty()`, `notifyChangeOfContents()` (a mixer or press only
  looks for a recipe again when its basin's contents change, so the kiln calls it when the heat changes, as Create's
  blaze burner does when placed), and `BlazeBurnerBlock.HeatLevel.isAtLeast` and `byIndex`.
- **`HeatScenes.setKiln`** now leaves `BurnTime` at 0 when the kiln is Cold, so fuel waiting in a cold kiln is not
  drawn glowing. Only the new scene does that.
- **Ponder**: Ponder 1.0.91 applies `modifyBlockEntityNBT` through `load` (not as a client packet), which reads the
  basin's `InputItems` and `OutputItems` and the kiln's `BasinHeat`. The scene does not use the basin's
  `VisualizedItems` (only read from client packets, and not drawn for a basin without a spout anyway).

In game:

- [ ] A basin on a kiln holding copper and zinc, a mixer over it: charcoal in the firebox stays unburnt and nothing
      mixes. Goggles on the kiln say "Too cold to heat the basin above" and "A Searing fire would heat it: Coal Coke,
      or charcoal with a Bellows". Coal Coke lights it, small flames lick up around the basin's foot, the mixer
      starts on its own and makes brass, and the goggles say "Heats the basin above like a fed Blaze Burner".
- [ ] Charcoal also mixes brass with a running Bellows blowing into the kiln, and burns twice as fast.
- [ ] Once the ingredients are used up, the kiln finishes the fuel item it is burning and lights no more; new
      ingredients light it again. An empty basin never lights it. With raw copper in the chamber as well, it fires
      both.
- [ ] Heat from below passes up as it is: on a fed blaze burner the kiln mixes brass with an empty firebox; on a soul
      campfire (a Searing chamber) it only warms the basin ("Only warms the basin above, as a campfire would"); on a
      blaze burner fed Blaze Cake it superheats the basin (goggles: "like a superheated Blaze Burner"). Power Grid's
      basin heater under a kiln passes its heat up the same way.
- [ ] A Mechanical Press compacting a heated recipe over a kiln-heated basin works too.
- [ ] The basin caps the kiln's chimney: the smoke vents out of the back and no soot builds up.
- [ ] A basin straight on a blaze burner behaves exactly as before.
- [ ] Ponder: "Heating a basin with a Kiln" plays from the kiln, Coal Coke, Create's basin and Create's mixer (under
      Heat). The charcoal stays dark; the coke lights the kiln and flames appear around the basin's foot; the mixer's
      head comes down once, and as it rises the copper and zinc have become two brass ingots.

GameTests: `KilnBasinTests.kilnHeatsTheBasinOnTop` builds seven kilns with basins (coke, charcoal, an empty basin,
charcoal fanned by a Bellows, and a fed blaze burner, a soul campfire and a superheated burner underneath) and checks
after 60 ticks which burn fuel, the heat each gives its basin, whether Create's brass recipe matches
(`BasinRecipe.match`, which goes through the mixin) and the goggle lines. `kilnHeatedBasinMixesBrass` runs a real mixer
on a creative motor over a coke-fired kiln and waits for two brass ingots in the basin.

## 14. Chain drive tiers (0.10.0)

Create's encased chain drive and adjustable chain gearshift join the andesite tier (`max_speed/128` now lists
`create:encased_chain_drive` and `create:adjustable_chain_gearshift`), so a powered chain gearshift can no longer turn
128 RPM into 256 out of andesite parts. Copper and iron ones join the families (DESIGN.md, "Chain drives"): `KineticPart`
gains `CHAIN_DRIVE` and `CHAIN_GEARSHIFT` (both `cased`, so they stay off `TIERED_KINETIC`), and `KineticFamilies`
registers 16 of each, 132 kinetic variants in all. There is no wooden chain drive. New files in `kinetics/`:
`TieredChainDriveBlock` and `TieredChainGearshiftBlock` (extend Create's `ChainDriveBlock` and `ChainGearshiftBlock`),
`WeatheringChainDriveBlock` and `WeatheringChainGearshiftBlock` (copper), `RustingChainDriveBlock` and
`RustingChainGearshiftBlock` (iron), `TieredChainDriveBlockEntity` (extends `KineticBlockEntity`) and
`TieredChainGearshiftBlockEntity` (extends `ChainGearshiftBlockEntity`), both with the gearbox's friction and goggle
lines; a powered chain gearshift's goggles add how much faster the rest of its row turns. In `client/`:
`TieredKineticVisual.shaft` and `TieredShaftRenderer`, Create's shaft visual and renderer with the material's shaft.
Ponder: `KineticScenes.chainDrives`, and the new blocks also get Create's own chain drive and chain gearshift scenes.

| Part | Recipe (shapeless, like Create's) |
|---|---|
| Copper Chain Drive | copper casing, three copper nuggets |
| Iron Chain Drive | industrial iron block, three iron nuggets |
| Copper / Iron Chain Gearshift | a copper / iron chain drive, an electron tube |
| Create's Chain Drive / Chain Gearshift | unchanged (andesite casing) |

Most likely compile snags:

- **`TieredChainDriveBlock` / `TieredChainGearshiftBlock`** override `getBlockEntityType()` with
  `BlockEntityType<? extends KineticBlockEntity>`, as Create's do. `getBlockEntityClass()` stays Create's
  `KineticBlockEntity.class`. Create casts the chain gearshift's block entity to `ChainGearshiftBlockEntity` (in
  `ChainGearshiftBlock.neighborChanged` and `ChainDriveBlock.getRotationSpeedModifier`), which ours extends.
- **`ModBlockEntities.TIERED_CHAIN_DRIVE` / `TIERED_CHAIN_GEARSHIFT`** register `TieredKineticVisual::shaft` (a generic
  static method like Create's `SingleAxisRotatingVisual::shaft`) and `TieredShaftRenderer::new` (generic in its block
  entity, like Create's `ShaftRenderer`). The renderer uses catnip's `CachedBuffers.partialFacingVertical` with
  `ModPartialModels.shaftOf`.
- **Block properties**: `ChainDriveBlock.PART` ("part": start, middle, end, none),
  `ChainDriveBlock.CONNECTED_ALONG_FIRST_COORDINATE` ("axis_along_first") and `ChainGearshiftBlock.POWERED`.
  `gen_assets.py` ports Create's `ChainDriveGenerator`, and its blockstates match Create's generated ones variant for
  variant. The models are parented on Create's `block/encased_chain_drive/*` models, with our textures.
- **Ponder text**: `gen_assets.py` copies the texts of Create's chain drive and chain gearshift scenes and the shared
  RPM labels they use into our lang, checked against Create's English.

In game:

- [ ] All 32 render placed (on each axis) and in hand: the gearbox's casing in the material with a chain of its own
      links round the middle. In a row, the chain runs out of the shaft faces from behind the bushing to the next
      drive. A chain gearshift has a redstone window over its chain, bright while powered. The shaft through it turns
      in the material, with Flywheel's backend on and off.
- [ ] A row relays rotation between drives of any material, Create's included; a drive placed against a row joins it
      the way Create's does, and a wrench turns it as usual.
- [ ] Create's chain drives spun at 192 RPM grind and break; copper and iron ones at 192 and 256 RPM do not. A
      powered chain gearshift of any material at 128 RPM breaks Create's chain drives past it, but not copper or iron
      ones.
- [ ] Goggles on one show material, top speed, load limit and stage, as on the gearboxes. A powered chain gearshift
      adds "The rest of its row turns 2× as fast" (1.5× at signal strength 7); an unpowered one says nothing about it.
- [ ] Copper ones oxidize, and iron ones rust only when wet, each to its spot's own final stage as in section 12a;
      honeycomb waxes and an axe scrapes, like the shafts. A drive that weathers stays in its row, and a powered
      chain gearshift stays powered.
- [ ] Recipes as in the table; crafting one with honeycomb gives the waxed one. A pickaxe breaks them fastest.
- [ ] Ponder: "Chain drive materials" plays from every tiered chain drive and chain gearshift and from Create's two
      (under Kinetic Materials; the tiered ones are also under Kinetic Relays). The tiered ones also have Create's
      chain drive and chain gearshift scenes, with their text rather than raw keys.

GameTests in `ChainDriveTests`: `thirtyTwoChainDrivesWithTheirOwnBlockEntities` (each has its own block entity type,
item, and weathers or rusts as its name says); `chainDrivesBreakAboveTheirSpeedLimit` (rows of two on motors: Create's
break at 192 RPM and behind a powered chain gearshift at 128, copper and iron carry 192 and 256, and a row can mix
Create's with copper); `chainDriveFrictionFollowsMaterialAndWeathering`; `chainDrivesAgeAndKeepTheirRow` (a powered
copper chain gearshift oxidizes to its final stage, is waxed and scraped and keeps its row and power; wet iron
rusts to its final stage, dry does not);
`poweredChainGearshiftGogglesShowTheRatio`; `chainDriveRecipes`.

## 15. The kiln becomes the Furnace (0.11.0)

The kiln is called the Furnace in game and looks like vanilla's furnace with its front cut open; it works exactly as
before. Its id stays `mcremix:kiln`, so saved worlds keep their kilns, and so do the code names (`KilnBlock`,
`KilnBlockEntity`, the `kiln/*` Ponder schematics, the `kiln_fuel/*` tags, the `kilnSpeed` config entry).
`KilnBlockEntity` is untouched. What changed:

- **Names**: `block.mcremix.kiln` is "Furnace", and `assets/minecraft/lang/en_us.json` in our jar overrides
  `block.minecraft.furnace` so vanilla's furnace, which stays cold, is the "Cold Furnace". It has to be in the
  minecraft namespace: the game reads each namespace's lang files in turn, in no fixed order, so the key in our own
  file could lose to vanilla's, while within one namespace a mod's file is read after vanilla's and wins. Every
  text a player reads now says furnace: tooltips, goggle lines, the JEI tabs ("Furnace" and "Furnace Heat") and
  their notes, the cold furnace hints, and the Ponder titles and texts (in `HeatScenes`, `BoilerScenes`,
  `BasinScenes`, `BellowsScenes` and the Heat tag in `ModPonderPlugin`). No English text says kiln any more.
- **Model**: the same shape, with `minecraft:block/furnace_side` on the walls and the frame round the two openings and
  `minecraft:block/furnace_top` on top and bottom, so a resource pack that retextures the furnace retextures ours too.
  The lining (`kiln_inside`, `kiln_inside_lit`) is now sooty stone instead of brick. The chimney stub's sides have
  UVs of their own: worked out from its position they ran above the texture, which could show a strip of a
  neighbouring texture in the atlas on 0.1.0 to 0.10.0.
- **Block**: cobblestone's properties and a stone map colour instead of bricks' (same strength, tool and light).
- **Recipe**: vanilla's own, eight `#minecraft:stone_crafting_materials` (cobblestone, blackstone, cobbled
  deepslate) in a ring. `data/minecraft/recipes/furnace.json`, which used to be switched off, now makes
  `mcremix:kiln`; keeping vanilla's recipe id means vanilla's recipe book advancement offers the Furnace as soon as a
  player picks up cobblestone. The kiln's clay recipe (`mcremix:kiln`) is gone, and nothing makes a Cold Furnace.
  Smokers and blast furnaces still take ours or a Cold Furnace.

In game:

- [ ] Placed and in hand, the Furnace has vanilla's furnace walls and top, a front with a chamber opening over a grate
      and a firebox opening below, a dark stone lining that glows from the bottom when lit, and a stone chimney stub
      with the furnace's rim on its sides, not a stray texture. A basin or boiler on top still hides the chimney.
- [ ] It is "Furnace" in hand, in JEI and in Ponder; vanilla's is "Cold Furnace". Clicking a Cold Furnace says "This
      furnace is cracked and cold for good. Build a new Furnace to fire things in, and cook food on a Campfire."
- [ ] Eight cobblestone (or blackstone or cobbled deepslate) make a Furnace, and a new world's recipe book shows it
      once you pick up cobblestone; clay balls over a campfire make nothing. Logs round a Furnace make a smoker.
- [ ] Every Furnace Ponder scene, and the bellows one, says furnace throughout.
- [ ] A world saved with 0.10.0 keeps its kilns, as Furnaces, with what was in them.

GameTests: `FurnaceTests.theFurnaceIsCraftedLikeVanillas` checks that vanilla's recipe id makes it, crafts it from
cobblestone and from mixed stones, and checks that the old clay recipe makes nothing and that a smoker takes ours.
`KilnTests` no longer expects vanilla's furnace recipe to be gone.

## 15a. Rust runs down (0.11.1)

Taran asked on 2026-10-20 for iron to rust when it has rusted iron above it (DESIGN.md, Weathering). Iron that only
rusts while wet now also rusts while the iron on top of it is rustier than it is, even when dry, and only until it
has caught up: `Weathering.rustierAbove` compares the stage of the block above with its own. So rust rained on at the
top of a column or wall works its way down as far as the blocks above it got, each block still stopping at its own
final stage, and the rust fades downward wherever a block stopped early. "Iron" is what section 12a weathers as iron:
this mod's iron parts and Immersive Weathering's iron (`Weathering.isIron`). Waxed iron above counts for nothing (it
has no stage), nor does copper. A door goes by the block on its upper half, as it is wet if either half is.

The rule is in `Weathering.randomTick`, so it reaches Immersive Weathering's iron with `fastWeathering` on, and in
`Weathering.rust`, so this mod's iron parts follow it with `fastWeathering` off too; Immersive Weathering's iron then
keeps its own rules. With `ironRustsWhenDry` on, Immersive Weathering's iron rusts anyway, so the rule only changes
this mod's parts.

In game (by GameTest):

- [x] Under a rusted iron gearbox, a dry column of an iron gearbox, an upright shaft, an upright cogwheel and another
      gearbox rusts down, each block to its own final stage or the stage of the block above it, whichever comes
      first, and never past the block above at any point. A gearbox on a spot that would rust all the way stops at
      weathered under a weathered gearbox. Dry iron under fresh iron, waxed rusted iron or oxidized copper stays
      clean (`WeatheringTests.rustRunsDownOntoDryIron`).
- [x] `fastWeathering` off: a dry iron gearbox under a rusted one rusts all the way at the old pace
      (`WeatheringTests.weatheringCanBeSwitchedBack`).
- [x] With IW: dry plate iron under rusted plate iron, and a dry iron door with rusted plate iron on top of it, rust
      to their final stage, the door's upper half with it; dry plate iron under fresh plate iron stays clean
      (`CompanionTests.immersiveWeatheringRustRunsDown`).
- [ ] A rusty iron wall in a played world streaks down over a day (not done this round).

## 16. Idea bank stage one: steel, the Steel Spring and every wood (0.12.0)

Taran asked on 2026-10-20 for everything in the idea bank to be built, asking about each design fork rather than
picking defaults. DESIGN.md (Build phases, item 4) has the five stages and what is decided for the later ones; this is
the first. The design is in DESIGN.md under Materials ("Every wood", "Steel", "Steel Spring").

**Steel parts.** `KineticMaterial.STEEL`: 512 RPM, no load limit, 0.25 su per RPM of friction when clean, and it rusts
like iron (`rusts()` now stands for "only weathers when wet", and `Weathering.isIron` and the goggles' rust lines use
it, so steel rusts, runs rust down and is reported like iron). Every part comes in steel, in four stages and waxed,
on Steel Casing (`ModBlocks.STEEL_CASING`, a connected casing like Create's). `SpeedCap` raises Create's
`maxRotationSpeed` to 512 as a world loads (`raiseCreateSpeedCap`, on by default), and Create's shaft and cogs in
brass casing or a metal girder join the 128 RPM tag, since they are andesite parts too and would otherwise have been
free to run at 512. Items: coal coke as before, `cementation_pack`, `blister_steel`, `steel_ingot`, `steel_nugget`,
`steel_sheet`, with the steel ones under `forge:ingots/steel`, `forge:nuggets/steel` and `forge:plates/steel`.
Recipes: the pack is shapeless (four iron ingots, four coal coke, a clay ball), `firing/blister_steel` fires it at
Searing for 800 ticks into four blister steel, `pressing/steel_ingot` and `pressing/steel_sheet`, and
`item_application/steel_casing_from_log` and `_from_wood`.

**Every wood.** `WoodSpecies` names the eleven woods with their stripped log and whether they are fireproof, and
`KineticVariant` carries the wood of a wooden part, so the wooden shaft, gearbox, clutch and gearshift are registered
once per wood (`oak_shaft`, `crimson_gearbox` and so on) with the stripped log's sound and map colour; the tiered
blocks ask `KineticVariant.flammable()` whether they burn. `OldWoodenParts` remaps the old `wooden_*` blocks and items
to oak on load. `gen_assets.py` draws each wood's parts in colours taken from its planks (`PALETTES`), keeps oak's
textures pixel for pixel the old wooden ones, and keeps every older texture as it was (a texture's place in
`TEXTURE_STAGES` seeds its noise, so the new woods go at the end). The `mcremix:wooden_shafts` item tag lets the
bellows take any wood's shaft.

**Steel Spring.** `SteelSpringBlock` is an encased shaft with `WOUND` (0 to 4, the windows) and `POWERED` (let go);
`SteelSpringBlockEntity` is a `GeneratingKineticBlockEntity`:

- It counts its turns (`springTurns`, 64, fills it). Held, it winds while its shaft turns the way it was wound (or
  either way when slack), taking 8 su per RPM through `calculateStressApplied` and telling the network when that
  starts or stops; full or turned the other way it slips, with a click every four ticks, and takes nothing.
- Let go, it generates `-windDirection × releaseSpeed`, where `releaseSpeed` follows Hooke's law, the dial's value
  times how full it is, never below 1 RPM. It is worked out again every 10 ticks rather than every tick, so a big
  network is not redone each tick. Capacity is 8 su per RPM. Overstressed, it does not use up turns. While something
  else drives its shaft (`hasSource()`), it generates nothing, and it takes over once that stops.
- The dial is a Create value box on the four sides round its shaft (`SpringConstantBehaviour`, 1 to 512 RPM, default
  32). Comparators read 0 slack, 15 full, 1 to 14 between; the blockstate's `WOUND` steps at thirds.
- Rendering: a steel shaft through it (`SteelSpringRenderer`, and a Flywheel visual in `TieredKineticVisual`), in
  Create's gearshift model with the spring's side textures (`steel_spring_<0-4>_<off|on>`).

**Ponder.** `KineticScenes.steelSpring` (`kinetics/steel_spring`) and `HeatScenes.cementation` (`kiln/cementation`),
alongside `KineticScenes.steel` and `KineticScenes.woods` from earlier in this round. The spring's scene is under
Create's Kinetic Sources tag; the pack and blister steel are under Heat.

In game:

- [ ] Wooden parts in each wood look like that wood's planks, and cherry looks pink, not grey. Crimson and warped
      parts do not catch fire.
- [ ] A world from 0.11.1 with wooden parts in it loads them as oak parts, in place and in chests.
- [ ] A Steel Spring's windows show the coil moving in from the wall as it winds, and the redstone corners glow when
      it is let go. The dial shows on the sides round its shaft; the goggles show turns, state, constant, speed and
      time left.
- [ ] Wound by a hand crank, it clicks when full and the crank keeps turning. Let go with a lever it spins its shaft
      back, slowing; a stiff spring visibly drives a press faster than a soft one.
- [ ] The Ponder scenes for the spring, blister steel, steel parts and every wood play through with their text.

GameTests: `SteelTests` (6), `WoodTests` (4) and `SpringTests` (8) are new; the old count tests are now
`everyGearboxHasItsOwnBlockEntity` (35), `everyClutchAndGearshift` (35 and 35) and
`everyChainDriveHasItsOwnBlockEntity` (24 and 24), and the tests that used the wooden parts use a different wood each.

## 17. Idea bank stage two: heat-rated pipes, pressure boilers and the crucible (0.13.0)

Taran answered seventeen questions before this stage (project memory `idea-bank-stage2.md`); DESIGN.md has the design
under "Fluids, heat and the crucible".

**Heat ratings.** `FluidHeat` reads a fluid's heat from the `mcremix:heat/*` fluid tags (lava and the molten metals),
else from its temperature (up to 350 K Cold, 700 Warm, 1300 Hot, 1600 Searing, then Blazing), and a block's ceiling
from the `mcremix:fluid_ceiling/{cold,hot,searing}` block tags; an untagged block holds anything. `Overheating`
counts up while a part holds something hotter than its ceiling, shows glowing dust and smoke (flames for wood, cracks
for glass), and after 60 ticks breaks it as its `FluidPartMaterial.Failure` says: wood burns (crimson and warped char
and split), glass shatters, steel drops whole. `Spills` lets the contents out: a source per 1000 mB that has room, and
molten metal short of that as rough metal. The hooks are mixins on Create's `FluidTransportBehaviour.tick` (pipes,
pumps, valves), `SmartFluidTankBehaviour.tick` (spouts, drains) and `FluidTankBlockEntity.tick` (tanks, from the
controller; a bursting tank breaks every block).

**Pipes, pumps and tanks.** `ModFluidBlocks` registers per `FluidPartMaterial` (eleven woods, glass, steel)
`RatedPipeBlock` (Create's `FluidPipeBlock`), `RatedPumpBlock` (its `PumpBlock`) and `RatedTankBlock` (its
`FluidTankBlock`; each kind has its own block entity type so kinds never join: `VatBlockEntity` is never a boiler,
`SteelTankBlockEntity` boils as a `PressureBoilerData`). Glass pipes move a quarter as much (`FluidNetworkMixin`)
and creep a quarter as fast (`PipeConnectionMixin`), and `GlassPipeRenderer` shows the fluid on every open side.
`FluidPropagatorMixin` makes Create look for any `PumpBlock`, not only its own. Pump speeds are the usual
`mcremix:max_speed/*` tags (wooden pumps 64 with shear pins, `create:mechanical_pump` 256). Models are Create's:
blockstates point at Create's models and `FluidPartModels` wraps them in a `SpriteSwapModel` that moves each quad from
Create's copper sprite to the same spot on this mod's (`<material>_pipes`, `_pipes_connected`, `_pump`, and the eight
tank sprites per tank). `gen_assets.py` makes those by recolouring Create's textures, which it keeps as palettes in
`tools/create_fluid_textures.json` (Create is MIT licensed): each shade of Create's copper moves to the same place on
the material's palette, and glass pipes are clear between pale edges.

**Copper patina.** `CopperFluidBlockMixin` adds a `patina` property to exactly Create's `FluidPipeBlock`,
`GlassFluidPipeBlock`, `PumpBlock` and `FluidTankBlock`; `BlockMixin` gives them random ticks while they have further
to weather, and they weather by `Weathering`'s copper rules. Honeycomb waxes them (`HoneycombItemMixin`), an axe
scrapes (`PatinaEvents`), they drop as they were (`KeepPatinaModifier`), and a wrench swapping a pipe for a glass pipe
keeps the patina (`FluidPipeBlockMixin`, `GlassFluidPipeBlockMixin`). `FluidPartModels.weatheredCopper` swaps their
models onto `exposed_`, `weathered_` and `oxidized_` copies of Create's twelve copper sprites.

**Pressure boilers.** `PressureBoilerData` is Create's `BoilerData` at a level for every two tanks. Spare levels
(what heat, water and size allow beyond what the engines use) build pressure; a whistle vents it
(`WhistleBlockEntityMixin` makes it scream while venting), and without one the boiler warns and bursts at 1800 (one
spare level in 90 s): its tanks drop, the water goes, and `mcremix:scalding` damage hits everyone within four blocks.
`SteamEngineBlockEntityMixin` lets engines run on steel boilers.

**Molten metal.** `MoltenMetal` (seven metals, their heat, tint, ingot, nugget and block), `MoltenMetalType`,
`MoltenMetalFluid` (lava's pace outside the Nether) and `MoltenMetalBlock` (sets about fifteen seconds after it
lands), buckets, and `RoughMetalBlock` (`LAYERS` 1 to 8, its block entity holding the mB, dropping ingots and
nuggets). Water sets molten metal at once (`ModFluids.registerInteractions`). One grey animated texture is tinted per
metal. `steel_block` is new, with crafting both ways.

**Crucible.** `CrucibleBlock` (a `KineticBlock`, `FACING` towards the placer, `FORMED` for a 3x3x3) and
`CrucibleBlockEntity`; recipes `mcremix:melting` (`MeltingRecipe`: ingredient, fluid, heat defaulting to the fluid's,
time defaulting to 10 ticks per 9 mB) and `mcremix:alloying` (`AlloyingRecipe`: fluids and counted solids in, a fluid
out), cached per item in `CrucibleRecipes`. Every heat level above a recipe's halves its time; a big crucible works
every column at once. Tilt follows the shaft (a full turn is 90°, up to 105°), and it keeps
`retained(tilt)` of its capacity, pouring the rest at 15 mB a tick per column onto what is under the lip: another
crucible, a sand mold on a depot or belt (held there while it fills and sets), a fluid handler, or the ground through
`Spills`. `SandMoldItem` holds one metal; full, it sets for 40 ticks and breaks out (on a depot, a belt, in an
inventory or on the ground). `CrucibleRenderer` draws the tipping pot, the metals in layers, unmelted items and the
stream; its constants match the pot `gen_assets.py` models (`pot_elements`). `CrucibleArmPointType` lets the Mechanical
Arm drop things in. JEI: `MeltingCategory` and `AlloyingCategory`. Ponder: `CrucibleScenes.intro` (`crucible/intro`).

In game:

- [ ] Wooden, glass and steel pipes, pumps and tanks look like Create's in their material, join and rim as Create's
      do, and a vat or steel tank shows its connected texture when it grows. Glass pipes show the fluid inside.
- [ ] Lava in a wooden pipe glows, smokes and burns it out after about three seconds, spilling the lava; molten gold
      does the same to a glass or copper pipe but runs in steel. Crimson and warped pipes char without flames.
- [ ] Create's copper pipes, pumps and tanks weather to green over a day, take wax and lose it to an axe, and keep it
      when broken and placed again.
- [ ] A steel boiler with a whistle screams while it has spare steam; without one it hisses and then bursts.
- [ ] A crucible over a fed blaze burner melts copper and zinc into brass; a shaft turned a little pours an ingot's
      worth into a mold on a depot, which hisses and gives an ingot and sand. Spilt metal runs, burns and sets.
- [ ] 27 crucibles placed as a cube draw as one big pot and tip on the shafts at two side middles.
- [ ] JEI's Melting and Alloying tabs list the recipes; the crucible's Ponder scene plays through.

GameTests: `FluidPartTests` (9), `PatinaTests` (6), `PressureBoilerTests` (4) and `CrucibleTests` (8) are new.


## 18. Idea bank stage three: Power Grid, chemistry and aluminium (0.14.0)

Taran answered eighteen questions before this stage, then redesigned the electrolysis cell as an Electrolyser on a
basin or a vat (project memory `idea-bank-stage3.md` and `electrolysis-and-vats.md`); DESIGN.md has the design under
"Power Grid, chemistry and aluminium". Question 17, which aluminium parts arrive now, moved to another thread, so
0.14.0 keeps every kind.

**Power Grid is required.** `build.gradle` takes Create: Power Grid 0.6.2 and Architectury 9.2.14 from Modrinth as
`modImplementation` (no more `-Ppowergrid`), and `mods.toml` lists `powergrid` as mandatory. Power Grid's own recipes
are replaced under `data/powergrid/recipes`: steel in the motor (mechanical crafting) and the transformer core
(sequenced assembly), Bakelite in the blank circuit boards (cutting), the switches, button and socket, and ferric
chloride in place of its acid for etching. Its classes are compiled against directly now; the older heater lookup in
`PowerGridCompat` still reads temperatures at runtime.

**Chemicals.** `Chemical` registers coal tar, wood spirit, ferric chloride, caustic soda, bauxite liquor and salt water
as liquids (block, bucket, still and running textures) and chlorine and hydrogen as gases (no bucket; `GasBlock`
turns a placed gas into `Gases.escape` a tick later). `ChlorineCloud` is the entity let-out chlorine becomes (heavier
than air, two damage a second to anything that breathes, kills plants, `mcremix:chlorine` damage type). `SeaWater`
makes a bucket dipped in a biome tagged `mcremix:salt_water` (oceans and beaches) come up salt, and the
`OpenEndedPipeMixin` and `FluidDrainingBehaviourMixin` do the same for a pump through an open pipe end and a hose
pulley. Escaped gas from an open pipe end goes through `Gases` too.

**Furnace byproducts and hydrogen.** `FiringRecipe` takes an optional `byproduct` fluid (coking coal 50 mB of tar,
charring logs 50 mB of wood spirit), which goes into a 1,000 mB tank in `KilnBlockEntity`; what does not fit, or is the
other fluid, burns off with black smoke. Pipes take it from any side. The same pipe handler fills a 1,000 mB hydrogen
tank, burnt 100 mB per 160 ticks at Searing before any item fuel. `KilnCategory` shows the byproduct, `KilnHeatCategory`
lists hydrogen as Searing fuel.

**Aluminium.** `KineticMaterial.ALUMINIUM` (512 RPM, 8,192 su, no friction, never weathers) gives `KineticFamilies` one
of each part, with Aluminium Casing. `MoltenMetal.ALUMINIUM` melts Hot at 933 K. `AluminiumMelting` runs from the
`KineticBlockEntityMixin` tick beside `KineticLimits`: a part next to anything Hot (lava, fire, a lit Furnace, a Blaze
Burner, molten metal) counts up for 100 ticks, glowing and dripping, then melts and its metal runs down to set as rough
aluminium. The settling, firing and splitting chain is data: `splashing/red_sand` (bauxite 25%), `mixing/bauxite_liquor`
(heated), `settling/aluminium_hydroxide`, `firing/alumina` (Searing), `electrolysis/alumina` and
`electrolysis/salt_water`, and red mud bricks with stairs, slab and wall.

**Heat sinks.** `HeatSinkBlock` faces its base onto the block clicked; `HeatSinkBlockEntity` keeps its own
temperature (2 J per degree) and each tick moves 20 W per degree between itself and the device under its base,
through Power Grid's thermal behaviour (`ThermalBehaviourAccessor`), and sheds 2 W per degree above the air, more with
a fan blowing through it.

**Electrolysis.** `ElectrolyserBlock` (Power Grid's `DirectionalElectricBlock`, facing the vessel, its model drawn
facing down and turned by `DownModelShapes`) and `ElectrolyserBlockEntity`, a Power Grid electric block entity with two terminals (0 is +,
1 is -). `buildCircuit` puts a `Cell` between them: Power Grid's `PNJunctionWire` (saturation 1e-7 A, ideality 8.5,
so about 4 V to start) with 0.02 Ω of melt, and a contact resistance. `Cell` overrides `pnLim` with SPICE's junction
step limit; Power Grid's own holds the junction at its critical voltage whenever a Newton step crosses it, and two
cells in series then never settled (2 A, 37 A, 0.2 A, round every six ticks). The circuit closes only while both
electrodes are in and there is something to split with room for the results. Each tick its current goes as charge to
the recipe (`ElectrolysisRecipe`, a set charge a batch), and its power burns the electrodes (`ANODE_LIFE` 12 kJ, the
cathode a tenth as fast); `CarbonElectrodeItem` keeps its wear as a bar. Past `RATED_POWER` (600 W) it heats as Power
Grid devices do. On a basin, `BasinElectrolysis` runs the recipe and lets its gases go over the rim (`Gases`); on a
vat's side it asks the vat for work.

**Settling and vats.** `SettlingRecipe` (`mcremix:settling`) runs in a basin with no heat under it and no mixer or press
over it (`BasinSettling`, from `BasinBlockEntityMixin` at every return of the basin's tick, since the tick returns early
whenever its contents are unchanged). `Vat`, held by the controller of Create's tank through `FluidTankBlockEntityMixin`
(`VatHolder`), checks for a 1-3 wide, 1-5 high tank on a full layer of andesite scaffolding, keeps three product tanks
of 2,000 mB per tank block and nine item slots, runs up to 16 batches at once, hands gases out first, and pushes items
into inventories beside or under the scaffolding. `FluidTankBlockMixin` breaks the vat up before Create splits the
tank. Wooden tanks keep their `*_vat` ids but are named Oak Tank and so on (`WoodenTankBlockEntity`).

**JEI and Ponder.** `SettlingCategory` and `ElectrolysisCategory` lay basin recipes out with `BasinSlots`. Ponder:
`TarScenes.byproducts` and `.bakelite`, `HeatSinkScenes.intro`, `ElectrolysisScenes.basin` and `.vat` (Power Grid's
`PowerGridSceneBuilder` draws the wires). Nothing in the electrolysis scenes is simulated.

**Tools and armour in the crucible**, from the crucible thread's patch: `gen_assets.py` writes 40 melting recipes
(`gear_melting()`, 45 mB a crafted ingot), and the crucible's tooltip says so.

In game:

- [ ] Coking coal fills the Furnace's tank with tar; a pipe draws it off; a full tank smokes black at the chimney.
- [ ] A press over a heated basin with tar and wood spirit makes Bakelite; Power Grid's recipes in JEI show steel,
      Bakelite and ferric chloride.
- [ ] A bucket dipped in the sea comes up salt water, and inland plain water.
- [ ] An Electrolyser on a basin of salt water, wired through a transformer, fizzes, and a green cloud forms over the
      basin that hurts and kills grass; goggles show volts, amps, temperature and electrodes.
- [ ] Wired straight across a generator, an Electrolyser heats up and overheats.
- [ ] A 2x2 vat on scaffolding with two Electrolysers in series fills with chlorine and hydrogen for pipes, and settles
      liquor into aluminium hydroxide that comes out into a chest by the scaffolding.
- [ ] Hydrogen piped into a Furnace burns it Searing; hydrogen let out of an open pipe just hisses.
- [ ] Aluminium parts beside lava glow, drip and melt into rough aluminium; a heat sink on a hot Power Grid device
      cools it, more with a fan.
- [ ] The tar, Bakelite, heat sink and both electrolysis Ponder scenes play through; JEI shows the Settling and
      Electrolysis tabs.

GameTests: `CoalTarTests` (4), `PowerGridRecipeTests` (3), `ChlorineTests` (3), `SeaWaterTests` (5), `AluminaTests` (3),
`HeatSinkTests` (1) and `ElectrolysisTests` (13) are new, with `aluminiumCarries512WithNoDrag` and
`aluminiumBreaksUnderHeavyLoad` in `SteelTests` and `gearMeltsIntoHalfItsMetal` in `CrucibleTests`.

## 19. Friction points, part 1 (0.15.0)

Taran's redesign of Create's kinetics (project memory `friction-points.md`, `machine-tiers.md` and
`friction-cooling.md`), built in three parts: this one (friction points, brackets, casings, brass and load limits),
then machine tiers with fitted drill bits and saw blades, then heat and cooling. DESIGN.md has it under "Friction
points (0.15.0)".

**What holds what.** `FrictionPoints` decides. A bare shaft or cog (anything extending Create's
`AbstractSimpleShaftBlock`, from any mod) needs a friction point within its material's span along its axis
(`KineticMaterial.span`: wood and brass 1, andesite, copper and iron 2, steel and aluminium 3), counted through the bare
parts between. A friction point is anything that `hasShaftTowards` the part and is not bare itself: machines,
gearboxes, clutches, gearshifts, chain drives, encased parts, blocks in `mcremix:friction_points` (the Steel Spring),
and a bare part with a bracket bolted to a solid face. Power sources hold nothing (`GeneratingKineticBlockEntity`,
apart from mechanical bearings that are not windmills), and nor do belts, gauges and `mcremix:holds_no_shafts` (the
flywheel). The edge of the loaded world counts as held. `ShaftSupport` refuses a placement (`BlockItemMixin.canPlace`,
and `onPlace` for Create's placement helper), checks again whenever something beside a part changes
(`BlockStateBaseMixin`), and drops parts that lose what held them. `frictionPoints` in the server config turns it off.

**Brackets.** `TieredBracketBlock` is Create's bracket in every wood, brass, copper, iron, steel and aluminium; copper,
iron and steel come in every weathering stage, waxed or not. `Brackets` fits one only where its plate can bolt to a
solid face (`BracketBlockItemMixin`, and from the offhand as a part is placed), drops it when that block goes, and
weathers it on its part. `BracketedBlockEntityBehaviourMixin` tells the network whenever a bracket changes, and
`PartSwaps` (with `BlockStateBaseMixin` and `LevelMixin`) keeps a bracket on a part whose block only weathers or is
waxed or scraped.

**Casings.** For each `EncasingCasing` (andesite, brass, copper, Industrial Iron, steel, aluminium), `EncasedParts`
registers an encased shaft, cog and large cog (`TieredEncasedShaftBlock`, `TieredEncasedCogwheelBlock`) whose
`EncasedPartBlockEntity` keeps the part inside as `InnerPart`; Create's own andesite parts keep Create's encased blocks
in andesite and brass casing. The casing is never used up, breaking an encased part drops the part inside, and a
sneaking wrench takes the casing off. `EncasedPartRenderer` and `EncasedPartVisual` draw the inner part in its own
material, `EncasedPartTextures` joins casings up.

**Limits.** `KineticMaterials` gives every kinetic block a material: this mod's from the block, Create's and other
mods' from the `mcremix:kinetic_material/*` block tags (Create's andesite machines are andesite, its brass-cased blocks
brass, its pump copper). Top speed applies only at friction points (`FrictionPoints.limitsFor`: the lower of part and
bracket or casing, or a machine's own); load limits apply to every part, against its network's stress.
`KineticLimits.tick`, from `KineticBlockEntityMixin`, grinds a friction point past its top speed for
`overloadGraceTicks` (60) and wobbles a part past its load for `loadGraceTicks` (20), then breaks it (`snap`, or
`snapHolder` for a bracket or casing, leaving the part to stand or drop). The wobble is sent to clients
(`McRemixWobble`) and drawn as a tilted spin by `RotatingInstanceMixin` (Flywheel) and `KineticBlockEntityRendererMixin`
(Create's renderers, used with Flywheel off and in Ponder), both through `client/Wobble`. Weathered friction points draw
0.0625, 0.125 and 0.25 su per RPM by stage on top of their material's own friction.

**Brass** (96 RPM, 3,072 su, never tarnishes) has a shaft, cog, large cog, bracket, gearbox, clutch, gearshift and chain
drives. **Aluminium** friction points need `mcremix:polyethylene`, empty until stage 4: the bracket and Aluminium
Casing take it, and its gearbox, clutch, gearshift and chain drives are built on the casing.

**Goggles.** `FrictionGoggles` adds lines to every kinetic block with a material: material and weathering, what holds
it or what it holds, friction, top speed, load limit, and what is giving way.

**Ponder.** `FrictionPointScenes` (friction points, brackets, casings in every material, wood as a fuse), with
`PonderParts` showing brackets, casings, wobbles and drops through block entity data, since Ponder worlds run no game
rules. `KineticScenes` reworks the materials, steel, woods, gearbox, clutch and chain drive scenes; the shear pin scene
is gone; the Furnace basin and byproducts scenes hold their cogs with an encased cog and a bracket.

**0.14.0 crash.** `ChemicalFluidType.initializeClient` read its chemical before its constructor had set it (Forge calls
it from `FluidType`'s constructor), so every client crashed at start-up; it reads it only when asked now.

**Dev client.** Power Grid's `LevelEntitiesAccessor` mixin clashes with the official name of `Level.getEntities()` in a
dev environment only, so `runClient` crashed on joining a world with an `IllegalAccessError`. `dev/accesstransformer.cfg`
(dev only, nothing in the jar) makes both methods public. With no screen, run it under Xvfb:
`Xvfb :99 -screen 0 1280x720x24 &` and `DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe ./gradlew runClient`,
driven by xdotool (`apt-get update && apt-get install -y xdotool`) and screenshotted with ImageMagick's
`import -window root`. On that software renderer, Flywheel's default `indirect` backend sometimes leaves turning shafts
undrawn from some camera spots, Create's own shafts included; `/flywheel backend flywheel:instancing` draws everything.

In game:

- [x] A bare shaft out of reach of any friction point is refused with a message above the hotbar. With a bracket in the
      offhand it goes on with the bracket bolted to the floor; where that bracket has nothing to bolt to, the message
      says so.
- [x] Shafts in every material run in brackets of their own material, and copper and iron brackets show every
      weathering stage.
- [x] Casings in every material show the part inside; goggles name the casing, the part held, its friction, top speed
      and load limit.
- [x] Brass shafts and cogs look like clockwork, three small cogs meshing with a large one.
- [x] Five presses on oak shafts at 64 RPM (2,560 su against oak's 2,048): the oak shafts wobble, goggles say they are
      wobbling loose, and one breaks off as an item, which takes the load off the rest.
- [x] An oak bracket at 96 RPM grinds and smokes, cracks, then gives way, and the shaft it held drops; a copper one
      beside it at 96 RPM runs on.
- [x] The friction points, brackets, casings, wood fuse, materials, steel, Furnace basin and byproducts Ponder scenes
      play through.

GameTests: `FrictionPointTests` (11) is new. Tests that expected wood to snap when a network was overstressed now
expect it to give way under load (`woodIsTheFuseUnderLoad`, `woodenGearboxIsTheFuseUnderLoad`,
`woodenClutchAndGearshiftAreFusesUnderLoad`), speed limits to hold at friction points
(`frictionPointsGrindAboveTheirSpeedLimit`, `everyWoodenFrictionPointGrindsAbove64Rpm`), and Create's brass encased
shaft to be brass (`brassEncasedShaftsTakeTheBrassLimit`). Test lines are built with brackets (`GT.bracket`,
`GT.bracketedX`).

## 19a. Brackets on their own (0.15.1)

Taran asked on 2026-10-22 for brackets that go down on their own, with a shaft placed into one, instead of only
clipping onto a placed shaft. Clipping on still works, and so does a bracket in the offhand.

**Standing brackets.** Create never places its brackets as blocks: `BracketBlockItem.useOn` fails on anything without a
bracket behaviour. `BracketBlockItemMixin` now places the bracket as a block there (`Brackets.placeStanding`), and
`BlockItemMixin` gives it its state at `getPlacementState` (`Brackets.placementState`): the foot against the face
clicked, or the first solid side in the order the player looks; the shaft lined up with a part pointing at it
(Create's `RotatedPillarKineticBlock.getPreferredAxis`) or else the way the player looks. With nothing solid to bolt
to it is refused with the 0.15.0 message. `Brackets.shaftAxis` reads the shaft's axis back from Create's
`FACING` and `AXIS_ALONG_FIRST_COORDINATE`, and `Brackets.standing` makes a state from a foot and an axis.

`BlockStateBaseMixin` gives a standing bracket what a block needs and Create's bracket never had: `canSurvive` (bolted
to a sturdy centre face at its foot), `updateShape` (air once its foot's block goes, so it drops through its loot
table), `getShape` (its plate and upright, two of each for a cog bracket, built from Create's model in `Brackets`),
and `canBeReplaced` with a shaft or cog in hand. Create's brackets can occlude, so without the shape they would have
culled the faces of the blocks around them. `WrenchableDirectionalBlockMixin` makes a wrench turn a standing bracket a
quarter turn on its plate (Create's turn would swing its foot off the block). `Brackets.registerMovementChecks` makes
a standing bracket attached to its foot's block and brittle, like a torch, for contraptions.

**Parts into brackets.** A shaft or cog used on a standing bracket, or on the floor in front of one, replaces it.
`BlockItemMixin` lays the part along the bracket whatever way the player looks; `FrictionPoints.isHeld` counts a
bolted standing bracket at the spot as holding a part along its axis (`Brackets.holdsPlaced`), so the placement is
allowed; and `ShaftSupport.onPlace` sees the bracket in the replaced block and fits it to the new part
(`Brackets.fitFromStanding`, Create's `getSuitableBracket`, so a cog gets a cog's bracket). Anything else that ends
up replacing one drops it. Breaking a part in a bracket drops both, as in 0.15.0 (Taran's call on a card).

**Fixed from 0.15.0.** A held cog skipped Create's own rule against cogs that clash (no large cog beside another cog
in its plane): `canSurvive` for a bare part returned only whether it was held. Now an unheld part is refused and a
held one still asks its block, and the offhand-bracket allowance in `BlockItemMixin.canPlace` applies only where the
block itself would stand (`heldCogsKeepCreatesRules`).

**Ponder.** The brackets scene now puts two brackets down on the floor, places copper shafts into them, then shows the
rest of the line; clipping onto a placed part is shown on the post.

In game:

- [x] A copper bracket used on the floor stands on it, its shaft the way the player looks, with an outline that fits
      it; Create's metal bracket used on a wall stands against it, and the wall's face is still drawn around the plate.
- [x] A copper shaft and a copper cog placed into floor brackets lie along them, the cog in a cog's bracket.
- [x] A wrench turns the wall bracket on its plate, and a shaft placed into it stands upright.
- [x] The brackets Ponder scene plays through.

GameTests: `bracketsGoDownOnTheirOwn`, `aPartPlacedIntoABracketTakesIt` and `heldCogsKeepCreatesRules` are new in
`FrictionPointTests`.

## 20. Idea bank stage four: plastics, the Steel Furnace and PVC pipes (0.16.0)

Taran answered seventeen questions before this stage, then redesigned its chemistry in words and answered 27 more
cards on that, on mixers fitted to vats, on fluid fuel and on lighting furnaces (project memory `idea-bank-stage4.md`,
`stage4-redesign.md`, `vat-mixers-and-steel-furnace-fuel.md` and `furnace-lighting.md`; the numbers picked without a
card are in `stage4-build-choices.md`). DESIGN.md has the design under "Plastics, the Steel Furnace and PVC pipes".
Built in a copy of 0.15.0, then rebased onto the friction thread's 0.15.1; the one conflict was in `BlockItemMixin`,
where 0.15.1's bracket placement and this stage's vat form both hook block placement, and both are kept.

**Chemicals.** `Chemical` adds ethanol, ethylene dichloride, vinyl chloride and hydrochloric acid as liquids and
ethylene, hydrogen chloride and pressurised steam as gases (steam has no bucket). Escaped gas goes through `Gases`
to a `GasCloud`: the 0.14.0 `ChlorineCloud` now shares that base with `EthyleneCloud` (ripens each crop under it a
stage, once per cloud), `VinylChlorideHaze` (nausea for ten seconds after the last breath) and `HydrogenChlorideMist`
(weathers the iron and copper blocks under it a stage, once per mist, through `Weathering`). A cloud of the same gas
let loose within 3 blocks joins it.

**Lighting.** `KilnBlockEntity` keeps a `lit` flag (and the block state `lit`). `tickFire` burns the firebox only while
it is lit, whatever there is to fire and however hot the block below is, and `goOut` when the fuel is gone; the old
"burn only when it is worth it" checks for boilers, basins and bellows are gone. `KilnBlock.use` lights it with
anything in `mcremix:furnace_lighters` (flint and steel, fire charge) and puts it out with anything that can
`SHOVEL_DIG`, through `light()` and `douse()`, so Deployers do both. Its block state now changes through
`updateState`, with `UPDATE_CLIENTS` only: Create's pipes take a neighbour update to mean their pipework changed and
start their flows over, so every heat change used to cut off a pump feeding the furnace.

**The Steel Furnace.** `SteelFurnaceBlock` extends the Furnace's block; `SteelFurnaceBlock.upgrade` swaps a Furnace for
one when Steel Casing is used on it, keeping the block entity's data, then `cased()` throws the solid fuel out and puts
the fire out. `SteelFurnaceBlockEntity` extends `KilnBlockEntity` and turns off solid fuel, heat from below
(`sealed()`) and bellows. Fuel comes in through a fluid handler on its bottom face only: it counts what came over the
last `FLOW_WINDOW` (20) ticks, and `SteelFurnaceFuels` turns that flow into a heat (each fuel's `heatPerMb` and
ceiling; a level needs 1, 2, 4 or 8 heat a tick). Lit, it takes everything and loses what is past the ceiling; unlit,
gases are let go through `Gases` and liquids gather in a 1,000 mB pool that `Spills` pours out of the front as a
fluid block, and the pool refuses more (so the pipe backs up) when there is nowhere to pour. Its front, sides and top
take fluids for `SteelFurnaceRecipe` (`mcremix:steel_furnace`: an ingredient fluid, an optional catalyst item lying in
the chamber that wears by `catalyst_lasts` mB, a heat, and up to two fluid results) into a 4,000 mB feed tank, and
give its results from two 4,000 mB tanks. The client draws fuel escaping from the front and black smoke when it is
fed too fast, from the synced `Escaping` and `Flow`.

**The Spark Plug.** `SparkPlugBlock` is a Power Grid `DirectionalElectricBlock` that only stands against a Furnace's
side or back (`canSurvive`), drawn screwed into the block under it and turned by `DownModelShapes` as the Electrolyser
is. `SparkPlugBlockEntity` puts a switched 100 kΩ resistor across its two terminals: it closes at 900 V and opens below
800, and while closed it lights the furnace whenever `canLight()`. `client/SparkPlugEffects` plays Power Grid's spark
sound and throws sparks at the gap while it conducts.

**Basins and vats.** `BasinVent` (from `BasinBlockEntityMixin`) lets gas in a basin go into the air once a second,
and the basin's fluid handler takes gas only to let it go. `FermentingRecipe` (`mcremix:fermenting`) runs in
`BasinSettling` beside settling, and in a vat a batch for every bucket. `Vat` holds up to `LAYERS` (4) fluids: the
tank's own plus three more in its `Layers` list, sorted by density for the renderer (`VatLayersRenderer`, through
`FluidTankRendererMixin`, only for two or more). `Vat.mixing` runs `VatMixingRecipe` (`mcremix:vat_mixing`) and
Create's mixing recipes that are wet and need no heat while a fitted mixer turns, and nothing settles or ferments then.
The 0.14.0 basin recipe for ferric chloride is a vat mixing recipe now.

**Mixers on vats.** `VatMixerBlock` is a mixer's vat form, one block per mixer (`ModFluidBlocks.registerVatMixer`;
only Create's andesite mixer has one so far, fitting wooden tanks). `BlockItemMixin.mcremix$vatForm` places it when a
mixer is used on a vat's side and the vat is of the mixer's material, and tells the player which vats it fits when
not; sneaking skips it. It drops its mixer, takes the mixer's stress and minimum speed, and draws as a half slab with
a shaft (`VatMixerRenderer` draws the back half of the shaft). `VatMixerBlockEntity` stirs `BATCHES` (4) at a time,
each stir taking as long as Create's mixer over a basin at that speed, less its head's travel.

**The Pressurizer.** `PressurizerBlock` (a Create kinetic block, shaft on one horizontal axis) and
`PressurizerBlockEntity` (stress `IMPACT` 32 su per RPM, a 1,000 mB feed tank and a 1,000 mB output tank, items in an
output slot). `PressurizingRecipe` (`mcremix:pressurizing`) has a `min_speed`, a `heat` and a time at that speed,
halved for each doubling; pressurised steam runs every tick, as much as the speed allows. `heatUnder` reads the block
below through `HeatSources`, and a Furnace or Steel Furnace there by its chamber's heat. `PressureBoilerData` takes
steam piped into a steel boiler, samples it as Create samples water (every 5 ticks, the most of the last ten samples)
and turns every `STEAM_PER_LEVEL` (10) mB a tick into a level of both heat and water, up to 18.
`client/PressurizerRenderer` draws its shaft when Flywheel is off and in Ponder.

**PVC pipes.** `PvcPipeBlock` (with `PvcPipeBlockEntity` and its Create `FluidTransportBehaviour`) lies on one face of the
block it rests on; `PvcPipes` works out its joints (along the face, inside corners, outside corners through a
`PvcCornerBlock` placed in the air by the edge, into the block below if it holds fluid, and risers to pumps, tanks,
basins and Create's pipes) and wrench cuts. `PvcGeometry` builds the shapes, and `client/PvcPipeModel` the baked
model from the same boxes. `PumpBlockEntityMixin` and `FluidPropagatorMixin` count a pump's reach in half-blocks, a PVC
pipe one and any other pipe two, so pumps reach twice as far through PVC and exactly as far as before through
Create's. PVC is in `mcremix:fluid_ceiling/cold`, and past it `Overheating` makes it sag (`Failure.SAGS`).

**Acid.** `AcidBlock` is hydrochloric acid in the world: every weathered block it touches goes back to bare metal
(`Weathering.bare`: vanilla's, Create's and this mod's copper and iron, and Immersive Weathering's iron, waxed blocks
excepted), and it soaks away after `SOAK` (40) ticks. `gen_assets.py` writes a `create:filling` recipe for each
weathered item, 100 mB of acid back to its bare form.

**Items and recipes.** Polyethylene Pellets, Polyethylene Bushing (in `mcremix:polyethylene`), PVC Resin, PVC Sheet
(pressed from resin) and PVC Pipe (three sheets make eight); the Steel Furnace (a Furnace and Steel Casing), the Spark
Plug (copper ingot, Bakelite, iron ingot), the Pressurizer (steel shaft, two steel plates, a steel tank, steel casing),
`electrolysis/hydrogen_chloride`, and Power Grid's integrated circuit (`data/powergrid/recipes/mechanical_crafting`)
taking pellets. `PressurizingRecipe` and `SteelFurnaceRecipe` say they take a time (`canSpecifyDuration`) and
`VatMixingRecipe` up to `Vat.LAYERS` fluids, or Create warns about each of those recipes at start-up.

**JEI.** `StandingCategory` is the shared layout of `SettlingCategory` and the new `FermentingCategory`;
`SteelFurnaceCategory`, `SteelFurnaceFuelCategory` (mB a tick for each heat), `VatMixingCategory` and
`PressurizingCategory` are new, and `KilnHeatCategory` no longer lists hydrogen. A dev client found the fuel rates and
the steam line cut short, so the Steel Furnace Fuel and Pressurizing tabs are 140 wide, and the Vat Mixing note is
"Stirred in a vat", its tooltip saying the rest.

**Ponder.** `SparkPlugScenes`, `SteelFurnaceScenes`, `VatMixerScenes` and `PressurizerScenes` are new, with their
schematics written by `gen_stage4_ponder()` in `gen_assets.py`. The Furnace, boiler and basin scenes light their fuel
with flint and steel, and `HeatScenes.setKiln` draws a Ponder furnace burning its own fuel as lit. Nothing in the new
scenes is simulated: the furnaces, vat and Pressurizer are set through their saved data, and the spark plug's sparks
are particles, since its sound would loop in Ponder.

**Two older bugs, found in game.** Create's connected textures move a quad's corners onto the connected sprite but
leave the quad naming the plain one, so `SpriteSwapModel` (0.13.0) drew a wooden or steel tank joined into a bigger
one, and weathered copper ones, from whatever lay beside its sprite in the atlas: dark, or see-through. It goes by
where a quad's corners lie now. And `FluidTankBlockEntityMixin` (0.14.0) saved and loaded a vat at the tail of Create's
tank `write` and `read`, which return early unless they are writing for or reading from a client, so a vat's data only
ever went to players: a reload lost everything but the tank's own fluid (its other layers, what it had made, the items
waiting in it, its charge), and Ponder, which loads a block entity the same way, never showed a vat's layers. Both
hooks run at every return now.

In game, checked in a dev client (2026-10-22):

- [x] The Steel Furnace and its chimney, the Spark Plug, the Pressurizer and Create's mixer in its vat form look right
      placed in a world, and PVC pipes lie along a floor, up a wall round an inside corner and over a step's outside
      corner.
- [x] An Oak Tank vat joined from eight tanks draws its windows properly, and filled with ferric chloride, chlorine and
      ethylene (by `/data`) it shows them in layers through its windows, still there after the world is saved and
      loaded again.
- [x] The Furnace's lighting scene and the Spark Plug, Steel Furnace, vat mixer and Pressurizer scenes play through.
- [x] JEI shows the Fermenting, Steel Furnace, Steel Furnace Fuel (every fuel's rates in full), Vat Mixing and
      Pressurizing tabs.

Covered by GameTests, not played by hand:

- [ ] Fuel in a Furnace waits; flint and steel lights it; it burns with nothing to fire until the fuel is gone; a
      shovel puts it out and leaves the fuel; a Deployer with flint and steel lights it.
- [ ] Steel Casing used on a lit Furnace makes a Steel Furnace, its fuel popping out.
- [ ] Coal tar pumped into an unlit Steel Furnace pours out of the front; lit, the pump's speed sets its heat, and
      faster than Searing it smokes black.
- [ ] A Spark Plug on a Furnace's side, wired through a transformer to about 1,000 V, sparks and lights it.
- [ ] Sugar in water ferments in a basin; ethanol over alumina in a Steel Furnace gives ethylene; a Pressurizer at 512
      RPM over a blaze burner makes pellets; a press over a heated basin makes a bushing.
- [ ] Create's mixer used on a wooden vat stirs chlorine and ethylene into ethylene dichloride.
- [ ] PVC pipes rise to a pump and carry water further than Create's pipes; lava in them makes them sag and burst.
- [ ] Acid poured on rusted iron strips it; hydrogen chloride let loose rusts the iron under it.

GameTests: `SteelFurnaceTests` (7), `VatTests` (8, one of them `aVatKeepsItsLayersThroughASave` for the save bug), `PlasticsTests` (5), `PressurizerTests` (5), `PvcPipeTests` (3),
`SparkPlugTests` (3) and `AcidTests` (2) are new, with `aFurnaceBurnsOnlyOnceLit` in `KilnTests`.
`aluminiumFrictionPointsWaitForPolyethylene` became `aluminiumFrictionPointsTakePolyethylene`, crafting the casing
and bracket with a bushing, and `hydrogenIsFurnaceFuel` is gone. Tests that fuel a Furnace light it first
(`KilnTests.lit`), and the boiler and basin tests expect a lit furnace to burn whether or not there is work.

## 21. Friction points, part 2: machine tiers (0.17.0)

Part 2 of Taran's friction redesign (project memory `machine-tiers.md`, cards in `machine-tiers-cards.md`): machines
in copper, iron, steel and aluminium, fitted drill bits and saw blades, copper and steel vat mixers, and wooden movers.
DESIGN.md has it under "Machine tiers (0.17.0)", "Drill bits and saw blades" and "Wooden movers". Built on 0.16.0,
which the stage four thread wrote back first; part 3 (heat and cooling) is next.

**Machine tiers.** `MachineKind` names the nine machines with Create's id, base material (andesite, brass for the
crafter and arm) and Create's entry. `ModMachines` registers each kind in `MachineTiers.TIERS` (copper, iron, steel,
aluminium) as a subclass of Create's block (`TieredPressBlock`, `TieredMixerBlock`, `TieredMillstoneBlock`,
`TieredFanBlock`, `TieredSawBlock`, `TieredDrillBlock`, `TieredDeployerBlock`, `TieredCrafterBlock`, `TieredArmBlock`),
each adding `Patina.PROPERTY` and using one block entity type per kind with Create's block entity classes, renderers and
visuals. `MachineTiers` maps kind and material to a block (Create's for the base material) and back. A tier is a
`TieredMachine`, which is a `MachineBody` (material, friction, weathering) and a `CreateStandIn`.

**Create takes them for its own.** Create asks its registry entries whether a block or item is one of its machines
(`AllBlocks.X.has(state)`, `isIn(stack)`, `is(item)`) all over: chutes about fans, arms about what they reach,
crafter grids, covers and unpacking, deployers, placement helpers, contraptions and schematics. `BlockEntryMixin` and
`ItemProviderEntryMixin` answer yes for anything whose block is a `CreateStandIn` for that entry. That replaced this
mod's own saw arm point, drill and saw placement helpers and movement checks; `ModArmPoints` keeps only what Create
cannot know.

**Limits and weathering.** `KineticMaterials.of` reads a `MachineBody`'s material; Create's machines are andesite or
brass by tag. `FrictionPoints.limitsFor` caps a machine at its own material's top speed, and
`MachineTiers.isCrushingWheel` makes Create's crushing wheel no friction point (`mcremix:holds_no_shafts`) with no load
limit but still capped at andesite's 128 RPM. `FrictionPoints.friction` adds the material's friction and the weathering
stage's; `Weathering` and `Patina` weather, wax, scrape and drop machines as they do parts (iron and steel only when
wet).

**Casing swaps.** `CasingSwaps` takes a casing used on a placed machine (`PlayerInteractEvent.RightClickBlock`, which
Create's deployer fires too) and calls `swap`: the block entity's data minus its network keys is saved, the block entity
removed so nothing drops, the new tier set with the old state's properties (patina reset) and the data loaded back.
The old casing pops out of the clicked face. A deployer's front and a crafter's front are left to Create.

**Bits and blades.** `DrillBits` and `SawBlades` are items; `DrillBlockEntityMixin` and `SawBlockEntityMixin` give every
drill and saw, Create's included, a fitted one (`FittedBits`, `FittedBlade`, saved and synced through
`BlockBreakingKineticBlockEntityMixin`, iron by default and not written then). `Fittings` fits them on use (players and
deployers) and pops the old ones out. `KeepFittingsModifier`, a global loot modifier, puts non-iron bits or blades in the
dropped item's `BlockEntityTag`, and the tooltip names them. On contraptions, `BlockBreakingMovementBehaviourMixin`
cuts with the bits in the block's data and `ContraptionColliderMixin` stops a piston or gantry at a block they cannot
cut. `TieredDrillVisual`, `TieredDrillRenderer` and `TieredSawRenderer` (Create's, MIT, drawing the fitted head or
blade) stand in for Create's on every drill and saw; `Revisualize` rebuilds a Flywheel visual when only data changed.
The stone, golden and diamond drills and saws are gone, with no conversion (Taran starts a new world).

**Looks.** `tools/create_machine_textures.json` stores Create's andesite and brass machine textures as palettes; the
generator recolours them into each tier's casing and weathering stage, and writes Create's models with those textures,
Create's blockstates with a model per patina, names, loot and Create's recipes with the tier's casing. Partials that
move are tiered through `ModPartialModels.machinePart`: the arm (`ArmVisualMixin`, `ArmRendererMixin`), the crafter's
lid, arrow and belt (`MechanicalCrafterRendererMixin`), and the drill head's collar. `TieredCrafterCTBehaviour` joins a
row of tiered crafters as Create's join.

**Vat mixers.** Stage four's `VatMixerBlock` is a `MachineBody` of its mixer's material; `ModFluidBlocks.registerVatMixer`
adds copper and steel forms, which fit only copper and steel vats.

**Wooden movers.** `MoverKind` names the bearing, piston, sticky piston, pole, head, gantry carriage, gantry shaft and
rope pulley with Create's entries; `ModMovers` registers each in every `WoodSpecies` as a subclass of Create's block
(`WoodenBearingBlock` and so on), each a `WoodenMover` (a `MachineBody` of wood and a `CreateStandIn`), so Create's
`has` checks take them too. Those that turn take Create's stress impact and are wooden friction points. Create builds
some of their states itself, so `PistonContraptionMixin` swaps the head and pole a piston pushes out for its wood's,
`MechanicalPistonBlockMixin` makes slime give its wood's sticky piston, `WoodenPistonHeadBlock` picks its wood's pole,
and `WoodenGantryShaftBlock.onPlace` checks its own block where Create checks for its gantry shaft. Rendering swaps
Create's shaft, half shaft, bearing top, gantry cogs and gantry shaft models for the wood's
(`ModPartialModels.moverModel`): with Flywheel in `SingleAxisRotatingVisualMixin`, `OrientedRotatingVisualMixin`,
`BearingVisualMixin`, `GantryCarriageVisualMixin` and `StabilizedBearingVisualMixin`; without it in
`KineticBlockEntityRendererMixin`, `BearingRendererMixin`, `GantryCarriageRendererMixin` and
`StabilizedBearingMovementBehaviourMixin`. Two known gaps: a wooden piston's hidden pole (the one inside the piston
while extended) comes back in the piston's wood after a cycle, and picking Create's rope or magnet in creative gives
Create's rope pulley.

**Ponder and JEI.** `MachineScenes.tiers` (a press of each material: top speeds, the andesite one breaking, a casing
swap, weathering and wax) is on every machine of every material, Create's included; every tier also gets Create's own
scenes for its machine, and drills and saws the bits and blades scenes. Wooden movers get Create's mover scenes. JEI lists
every tier as a catalyst for its machine's tabs.

In game (dev client under Xvfb, section 19):

- [x] Wooden movers draw in their wood with Flywheel's instancing backend and with Flywheel off: an oak piston on
      spruce poles pushing a birch bearing (the bearing's top drawn in birch on the move), an oak carriage riding spruce
      and Create's gantry shafts with wooden cogs, a jungle rope pulley letting down Create's rope, and an oak bearing
      turning a plank once assembled. Placed with commands and kept through a save and reload.
- [x] Ponder opens for a wooden piston and gantry carriage with Create's scenes, the machine materials scene plays
      through for a copper press, and Create's press lists it after its own scenes.

Covered by GameTests, not played by hand:

- [ ] Tiered fans blow through chutes, crafters of any tier work together, arms reach every tier, tiered deployers reach
      and place alongside Create's.
- [ ] Machines weather by their material, wax and scrape, top out at their material and draw its friction; an andesite
      press breaks past 128 RPM.
- [ ] A casing used on a placed machine swaps it, keeping a drill's bits; a deployer holding a casing does it too.
- [ ] Bits and blades are fitted by using them on a drill or saw or from a deployer, and a broken drill or saw keeps
      them; gold bits feed a hopper from a cobblestone generator.
- [ ] Copper and steel mixers fit copper and steel vats, stir them, top out at their material and weather.
- [ ] Wooden pistons push their own head on any poles, slime makes them sticky, carriages ride any gantry shafts,
      pulleys let down Create's rope, and wooden movers break past 64 RPM.

GameTests: `MachineTierTests` (12) and `MoverTests` (8) are new, with three fitting tests in `DrillTests`, two in
`SawTests` and five vat mixer tests in `VatTests`. The stone, golden and diamond drill and saw tests became bits and
blades tests (`goldBitsFeedAHopperFromACobbleGenerator`, `sawStressFollowsTheirBlades`, `drillBitsRecipes`,
`sawBladeRecipes`).

## 22. Friction points, part 3: heat and cooling (0.18.0)

Part 3 of Taran's friction redesign (project memory `friction-cooling.md`, cards in `friction-heat-cards.md`): friction
heat in aluminium, heat sinks and fans on friction points, water coolers, radiator panels, cold water and coolant.
DESIGN.md has it under "Heat and cooling (0.18.0)". Built on 0.17.0. Titanium's heat waits for titanium in stage 5
(`KineticMaterial.heatsUp`).

**Friction heat.** `FrictionHeat.attach` gives every `KineticBlockEntity` a `FrictionHeatBehaviour`, a Power Grid
`ThermalBehaviour` (unless the block already keeps one), so Power Grid's thermometer, its fans and this mod's heat sinks
and water coolers find it as they find a Power Grid device. Only a friction point whose holder is aluminium heats
(`FrictionHeat.heatingMaterial`: the bracket or casing, else the block itself), at `WATTS_PER_RPM` (8.27 W per RPM),
shedding `DISSIPATION` (3.64 W per °C) with `THERMAL_MASS` 272 J per °C. `FrictionHeat.settlesAt`, `sinkCooling` and
`fanCooling` are the balance the GameTests check. Anything that does not heat sits at the air's temperature and keeps
nothing in its save. `FrictionGoggles` adds a temperature line, gold from 520 °C and red from 620. The server config's
`frictionHeat` turns it all off.

**Melting.** From 520 °C it glows and smokes, from 620 it drips (`AluminiumMelting.glow`), and at 660 `FrictionHeat.melt`
takes the aluminium off: a bracket off its part, a casing off the part inside (switched back to the bare part, which
then needs holding), a gearbox, clutch, gearshift or chain drive whole, and a machine whole, dropping what it held.
`AluminiumMelting.metalIn` counts a casing's or machine's body as a sheet, and `AluminiumMelting.melt` runs the metal off
to one side when the part it held still stands over the spot. Melting next to lava (0.14.0) goes the same way now: in
0.17.0 an aluminium casing or machine left no rough aluminium and took the part inside with it.

**Fans.** Power Grid's fans cool the thermal behaviours their air passes through. A friction point that is a whole block
stops the air instead, so `AirCurrentMixin` adds the fan's cooling to the friction point it runs into, weaker the
further off the fan is, and `FrictionHeatBehaviour` caps a fan blowing straight on it at one still heat sink
(`FrictionHeat.fanCooling`, full at 128 RPM right beside it). A fan through a heat sink's fins works as in 0.14.0.

**Heat sinks.** `HeatSinkBlockEntity.deviceAt` is what a heat sink or water cooler bolted to a block cools: a Power Grid
device, a friction point that heats (or is still warm), or another sink, never a radiator.

**Water coolers.** `WaterCoolerBlock` faces the block its base is bolted to; pipes join any other face.
`WaterCoolerBlockEntity` cools through Power Grid's model each tick: `Coolants.cooling` says, for what is in its feed tank,
the watts per degree it takes (water 15, cold water 30, coolant 45), the joules a millibucket takes up (134, 188, 188)
and what it comes out as (warm water, or hot coolant). It takes the lower of that rate times how far above the air the
device is and what the room in its output tank allows, so it spends fluid only as it cools, and stops when there is no
feed or the output is full. `CoolantTanks` is the pair of one-bucket tanks it works between, also used by radiators: one
pipe can fill and another drain, or one do both.

**Radiator panels.** `RadiatorBlock` is a pillar whose axis runs from its front plate to its back. `RadiatorBlockEntity.panel`
joins radiators reached side by side in the plane across that axis with the same axis (up to `MAX_PANEL`, 256), and
`PanelTanks` fills the emptiest block first and drains the fullest first, so the panel shares its fluid like a tank.
`Coolants.chilled` says what it gives out: any water (plain, warm or cold) becomes cold water and hot coolant coolant;
cold water and coolant pass through unchanged; salt water is refused. Each block chills `STILL_RATE` (0.5 mB a tick)
times its own Power Grid cooling multiplier, which fans blowing through it raise; radiators are
`create:fan_transparent`, so a fan's air goes on up a column.

**Cold water and pouring out.** `SeaWater` is now `WildWater`: water taken from the world by bucket, by an open pipe end
a pump draws through (`OpenEndedPipeMixin`) or by a hose pulley (`FluidDrainingBehaviourMixin`) is salt water in the
sea (`mcremix:salt_water`) and cold water wherever the biome is cold enough to snow at that height or is in
`mcremix:cold_water`. `Chemical.poursAs` makes warm and cold water and hot coolant `AmbientLiquidBlock`s, which become
plain water or coolant, as deep as they were, a tick after they are poured.

**Coolant.** `Chemical` gains ethylene glycol, coolant and hot coolant. Two mixing recipes: ethylene dichloride and
caustic soda, heated, make glycol and salt water (250 mB of each); glycol and water, unheated, make 500 mB of coolant.

**Ponder and JEI.** `FrictionHeatScenes.intro` (an aluminium gearbox warming, glowing, dripping and melting, then cooled
by a heat sink, a fan straight on it, a fan through the sink and a water cooler) is on the heat sink, the water cooler,
the aluminium casing and every aluminium part and machine; `WaterCoolingScenes.intro` (a water cooler on an aluminium
gearbox, a radiator panel with a fan under each column, the loop closed, then coolant) is on the water cooler, the
radiator and the buckets of the cooling fluids and glycol. The water cooler and radiator are under the Heat tag. JEI has
information pages for the water cooler, the radiator and the buckets of warm water, cold water, coolant and hot coolant.

In game (dev client under Xvfb, section 19):

- [x] Both Ponder scenes play through: friction heat in aluminium (opened from the heat sink) and cooling with water and
      coolant (from the water cooler), every text in turn, the fans, pumps and gearboxes turning and the radiators
      giving off cold mist.

Covered by GameTests, not played by hand:

- [ ] Only aluminium holders heat; uncooled aluminium settles whole up to about 256 RPM, a sink or a fan straight on it
      carries it to about 384, and a fan through the sink to 512; at 512 it melts about a minute from cold.
- [ ] Overheated aluminium melts into rough aluminium: a gearbox whole, a casing and a bracket off their steel shafts,
      which turn on, held by the gearbox beyond, at the air's temperature.
- [ ] Water coolers cool far faster than sinks, giving back as much warm water as the water they took; cold water and
      coolant cool faster; a full output or no water stops them; one cools a Power Grid resistor, and one on a steel
      gearbox has nothing to do.
- [ ] Pipes join a cooler on any face but its base and a radiator only at its front and back; each takes only what it
      works with.
- [ ] Radiators chill warm water slowly, faster with a fan; panels share their water and chill fast with a fan under
      each column; plain water turns cold, hot coolant turns back into coolant, and cold water passes through.
- [ ] A bucket dipped where it snows comes up cold and in a frozen ocean salt; a pump draws cold water from a frozen
      river; warm and cold water poured out become water, and hot coolant coolant.
- [ ] Glycol is mixed from ethylene dichloride and caustic soda over a blaze burner, and coolant from glycol and water.

GameTests: `FrictionHeatTests` (4) and `CoolingTests` (7) are new; `SeaWaterTests` calls `WildWater`.

## 23. Idea bank stage five: titanium (0.19.0)

Taran's ask of 2026-10-23 (titanium as the highest tier, faster than anything, needing more cooling than aluminium),
which is also idea bank stage 5. Cards in project memory `titanium-cards.md` and `titanium-chain-cards.md`, Claude's own
calls in `titanium-build-choices.md`; DESIGN.md has it under "Titanium (0.19.0)". Built on 0.18.0.

**The tier.** `KineticMaterial.TITANIUM`: 2,048 RPM, any load, reach 3, no drag, never weathers. `SpeedCap` raises
Create's `maxRotationSpeed` to it as a world loads. Every part, casing (`EncasingCasing.TITANIUM`) and machine
(`ModMachines`) comes in it; the casing is a titanium sheet, a stripped log and the `mcremix:polyethylene` tag, as
aluminium's is. `heatsUp()` is true for it, and `tints()` (only for it) says what holds its parts keeps temper colours.

**Temper colours.** `Tints` keeps them in the stage slot weathering metals use (`WeatherState`: unaffected, gold,
purple, blue, the `gold_tinted_` and so on prefixes), so brackets, gearboxes, clutches, gearshifts and chain drives
come as a block for each tint (`KineticFamilies`; bare shafts and cogs never heat, so they come plain), encased parts
as an encased block for each (`EncasedParts`, `TieredEncased.tint`) and machines in their `Patina`, which on titanium
never weathers. `Tints.heat`, called from `FrictionHeatBehaviour` each tick, swaps what holds the part for a deeper tint
past 400, 550 and 700 °C and never back. `KineticBlockMixin` keeps the block entity through a swap (cancelling
`KineticBlock.onRemove`) and, since a swap is a change of block, sets `preventSpeedUpdate` as Create does for a change
of state, or the kept block entity would read 0 RPM for a tick while it found its network again
(`aTintNeverStopsTheLine`; `titaniumCarries1024WhereSteelBreaks` failed on it once). `Weathering.bare`
strips a tint first, so acid (`AcidBlock`, which now also strips the bracket on a bare shaft or cog) takes it off.

**Heat and burning.** `FrictionHeat.wattsPerRpm` is 25.33 W per RPM for titanium (from twice aluminium's cooling need
scaled to its 1,000 °C), `limit` 1,000 °C and `glowsFrom` 800. `FrictionHeatBehaviour` burns titanium as it melts
aluminium, once it has gone on rising past its limit for a few ticks: `TitaniumBurning` flares 60 ticks (white sparks,
smoke, and `lightAround` setting fire in air beside it that touches something flammable, as lava does), then removes the
holder as `FrictionHeat.melt` does for aluminium and drops `powderFrom(metal)` titanium dioxide, one for each 90 mB.
Goggles give the temperature with its burning point. A heat sink now has an overheat callback: past 660 °C it melts into
rough aluminium (`HeatSinkBlockEntity.METAL`, 630 mB), so on titanium it holds only to about 150 RPM, or 225 with a fan
through it; water coolers have no such limit (`titaniumNeedsTwiceTheCooling` checks every rung against aluminium's).

**Ore and chemistry.** Black sand (`ModBlocks.BLACK_SAND`, a falling block) is crushed or milled from basalt, and Power
Grid's magnetization recipe turns it into rutile and iron nuggets. `Chemical` gains molten salt (1100 K, Hot, glowing),
molten sodium (400 K) and titanium tetrachloride (a gas): appended after hot coolant, since texture seeds follow the
enum's order, and `Chemical.light()` lights any Hot fluid. A new recipe type, `mcremix:boiling` (`BoilingRecipe`, a
basin recipe), is what `BasinSettling` looks for in a heated basin with nothing working it, where a cool one settles;
JEI shows it in a Boiling tab. Molten salt melts from salt in the crucible and an Electrolyser splits it into sodium and
chlorine. `SodiumBlock` is molten sodium's fluid block: a tick after it touches water on any side it goes off with a
strength-2 explosion that lights fires and breaks nothing (`ExplosionInteraction.NONE`); sodium and water in one vat do
nothing yet. `Gases` fumes escaped titanium tetrachloride into white smoke and hydrogen chloride mist. Rutile Briquettes
(rutile or titanium dioxide, coal coke and 50 mB coal tar, pressed) are the Steel Furnace's catalyst for chlorine to
titanium tetrachloride at Blazing, one per 200 mB, which spared the furnace a second catalyst slot. A steel vat with a
mixer stirs 100 mB of titanium tetrachloride and 200 mB of molten sodium into a sponge and four salt.

**The Arc Furnace.** `ArcFurnaceBlock` is a `HorizontalElectricBlock` with two connector terminals on its beam
(`horizontalNorthTerminals` turns them with its facing) and blockstate flags for each electrode, a charge and the arc.
`ArcFurnaceBlockEntity` builds its circuit as one switched 0.25 Ω wire across the terminals, closed while it has both
electrodes and room for what it makes, and heats by Power Grid's thermal model (`forMaxPower`, 150 W, overheating at
175 °C). An arc of 400 W or more adds its power to the energy for the recipe in hand (`ArcFurnaceRecipe`,
`mcremix:arc_furnace`, 16 kJ for a sponge); both electrodes wear by the joules through them (160 kJ each, the Carbon
Electrode's wear). Its item handler: two electrode holders and the charge take items, only the made slot gives them,
and a hand puts in one electrode a click. Goggles show the electrodes, volts, amps, watts, the hearth's temperature,
progress and why it is idle. JEI has an Arc Furnace tab.

**Plating.** `TitaniumPlatingBlock` holds its tint; `Tints.of` and `withTint` know it, so acid strips it. Each colour
fires from every lighter one (`mcremix:firing`, six recipes), and `KilnProcessing.find` now chains every firing recipe
an item matches by heat (`hotter`), the Furnace doing the hottest its heat reaches (`at`); no earlier item had two.

**Ponder.** `TitaniumScenes.heat` (a titanium gearbox tinting, stripped by acid, burning with fire across the gap,
water-cooled, then plating in each colour) is on every titanium part, the casing, every titanium machine, the plating,
ingots and dioxide; `TitaniumScenes.arcFurnace` is on the Arc Furnace, sponge and ingots, under Heat and Power Grid's
Electric Devices. `BasinScenes.kilnBasin` ends with salt water boiling down, and the tier, machine, friction point,
encasing and friction heat scenes name titanium.

**Cold water.** `mcremix:cold_water` now holds `#forge:is_snowy` as well as frozen rivers. WWOO gives snowy plains,
taigas and beaches the frozen ocean's temperature quirk, so it rains on patches of them, and makes ice spikes, groves
and frozen rivers too warm to snow; a bucket there came up plain, and the companion run failed whenever
`aBucketDippedWhereItSnowsComesUpCold` landed on such a patch. Now every snowy biome gives cold water, and the test
also checks height: windswept gravelly hills give plain water at y 90 and cold at y 250, where it snows. In 1.20.1
`GameTestHelper.relativePos` turns an unrotated test's positions half round, so build positions from `p(...)` and
never pass them through it.

Covered by GameTests, not played by hand:

- [ ] A titanium gearbox carries 1,024 RPM where a steel one grinds apart, tinting as it heats without stopping.
- [ ] Every cooling rung holds titanium at the speed twice aluminium's cooling gives, heat sinks melting past 660 °C.
- [ ] Heat tints a gearbox, a bracket, an encased shaft and a press for good; acid strips them, and plating.
- [ ] Overheated titanium flares, lights planks across a gap, and crumbles into titanium dioxide: a gearbox whole, a
      bracket and a casing off their steel shafts, which turn on, held by the gearbox beyond.
- [ ] The chain end to end: salt boiled in a fed-burner basin and not in a cool or stirred one; salt melted and rutile
      pressed; molten salt split; chlorine over briquettes in the Steel Furnace; sponge stirred in a steel vat; sponge
      melted in a cooled Arc Furnace (about 1 kW from 16 V), too weak at 8 V and hotter uncooled; electrodes wearing
      alike and burning out; sodium going off in water without breaking glass; titanium tetrachloride fuming.
- [ ] Plain plating fires gold, purple or blue in Hot, Searing and Blazing Furnaces; deeper colours wait for more heat.
- [ ] Water from any snowy biome is cold, WWOO's rainy patches too; elsewhere only as high as it snows.

GameTests: `TitaniumTests` (8) and `TitaniumChainTests` (9) are new; the gearbox, clutch and chain drive counts include
titanium's four tints.

## 24. The titanium Pressurizer (0.19.1)

Taran's stage 4 answer was a Pressurizer in steel now and titanium later; 0.19.0 left it steel, and a card after it
asked whether to build the titanium one. DESIGN.md has it under "Titanium (0.19.0)" and with the Pressurizer.

**The block.** `TitaniumPressurizerBlock` is the `PressurizerBlock` with a `Patina` property, and a `MachineBody` of
titanium, so `FrictionPoints` caps it at 2,048 RPM, `FrictionHeatBehaviour` heats it, `Tints` keeps its temper colour in
its patina and `TitaniumBurning` crumbles it, as for every titanium machine. The steel one is unchanged (its material
still comes from the `kinetic_material/steel` tag). Both share the `pressurizer` block entity type, whose jobs already
went faster with speed, so nothing in `PressurizerBlockEntity` changed: at 2,048 RPM every job runs four times as fast as
at 512 and steam comes at 320 mB a tick. `PressurizerRenderer.shaft(state)` picks the shaft drawn through it, which
`TieredKineticVisual.pressurizerShaft` uses too, so the titanium one shows a titanium shaft.

**Assets and recipe.** `gen_titanium_pressurizer()` draws the steel one's shapes in titanium, in each tint (the
`gold_tinted_` and so on prefixes), with a blockstate for each axis and patina. The recipe is the steel one's in titanium
round the same steel fluid tank: a titanium shaft, titanium plates either side of the tank, titanium casing under it.
The Pressurizer's Ponder scene and JEI's Pressurizing tab show both, and it is with the titanium machines in Ponder.

**Starting a test server.** One plain run stopped before any test with Create's own Registrate reporting "Found unused
register callbacks" and no entry named in the log; the same code started normally next time. 0.16.0 and 0.8.1 saw it
once each (TEST_REPORT). It only throws in a dev run, and since Registrate named no entry before throwing, its
callback map seems to have lost count rather than held a real unused callback: start the run again.

Covered by GameTests, not played by hand:

- [ ] A titanium Pressurizer at 1,024 RPM sets ethylene into pellets twice as fast as the steel one at 512, warming as
      it turns and carrying on through a change of temper colour.
- [ ] A titanium shaft, two titanium plates, a steel fluid tank and titanium casing craft one.

GameTests: `PressurizerTests.aTitaniumPressurizerWorksTwiceAsFastAt1024` is new, and `pressurizerAndCircuitRecipes`
crafts the titanium one too.

## 25. Pipe and tank metals (0.20.0)

Taran's ask of 2026-10-26: take out wooden pipes and tanks, the Steel Spring and slag roads, and add iron, brass and
aluminium pipes and tanks, the brass ones thinner and the iron ones rusting. Fourteen cards in project memory
`pipe-tank-rework-cards.md`; DESIGN.md has it under "Pipe and tank metals (0.20.0)". Built on 0.19.1.

**Taken out.** Wooden pipes, pumps and tanks in all eleven woods, Create's mixer's vat form (`andesite_vat_mixer`), the
Steel Spring (block, block entity, renderer, the `springTurns` setting, its Ponder scene) and slag roads (the `road/`
package, its Ponder scene and JEI page; slag still fertilises and washes into nuggets, but does nothing on a path).
`RemovedBlocks` answers Forge's `MissingMappingsEvent`: a slag road block or item becomes a dirt path, and the rest are
ignored, so a world from 0.19.1 opens without Forge's missing-blocks screen and simply loses them, what they held
included.

**The materials.** `FluidPartMaterial` is now `BRASS`, `IRON`, `GLASS`, `STEEL` and `ALUMINIUM`, and says how each gives
way (`Failure.MELTS` is new, for aluminium), whether it rusts, how much it slows a line (`slowdown`: brass 2, glass 4)
and whether it is thin. `RatedPipeBlock.create`, `RatedPumpBlock.create` and `RatedTankBlock.create` pick the class:
iron's are `Rusting` subclasses, which add `Patina.PROPERTY` (the states are made before the material is known, so the
property has to come with the class) and rust through `Weathering` as iron parts do, only while wet; the brass pipe is
`RatedPipeBlock.Thin`, with vanilla-style pipe shapes 6 px across. The aluminium tank's block entity,
`AluminiumTankBlockEntity` (renamed from `WoodenTankBlockEntity`), never updates a boiler, and
`SteamEngineBlockEntityMixin` lets an engine run on brass, iron or steel tanks. Heat ceilings stay data: the new
`fluid_ceiling/warm` tag (read by `FluidHeat`) holds the aluminium parts, `hot` brass, iron and glass with Create's,
`searing` steel. The pumps join `kinetic_material/brass`, `iron` and `aluminium`, so `FrictionPoints` caps them at 96,
256 and 512 RPM, and the aluminium one heats and melts as every aluminium friction point does.

**Thin brass pipes.** `ThinPipeModel` wraps the brass pipe's baked models: every vertex on the pipe's own sprites moves
toward the middle of the block, to 0.75 of its distance within 4 px of it and to 3 + 1.25 (d - 4) px beyond, so Create's
4 px wall lands at 3 px while the pipe still reaches its block's faces and its rims shrink with it. A bracket is left
as it is. The item model is three elements of `gen_assets.py`'s own.

**Half the flow.** `FluidNetworkMixin` divides Create's `transferSpeed` by the slowest pipe on the line, looked over
again whenever the line grows and once a second, and `PipeConnectionMixin` slows the flow drawn in each pipe the same
way, so what is seen matches what moves. Glass went the same way (it was a separate rule before).

**Rust and leaks.** `Leaks`: an unwaxed iron pipe, pump or tank block at its last stage (`Patina.OXIDIZED`) loses
`PER_TICK` (1) mB a tick. A line (`FluidNetworkMixin`) loses its rusted pipes' and pumps' share from where it draws,
while it moves fluid; a tank (`FluidTankBlockEntityMixin`) its rusted blocks' share while it holds anything, a vat its
heaviest liquid first (`Vat.leak`). Both look for rust once a second and drop a block from the count as soon as it is
scraped, cleaned or waxed. A liquid is simply lost, dripping on the client (`Leaks.drip`, from
`FluidTransportBehaviourMixin` and the tank mixin); a gas is held back and let go a second's worth at a time through
`Gases.escape`, from an open block beside the leak (from inside the tank it would come out in the tank). The acid
cleaning recipes (`gen_assets.py`, partial NBT on the item's patina) take rusted iron items back to bare iron.

**Aluminium.** With a Hot fluid inside, or beside anything Hot (lava, fire, a lit Furnace, a Blaze Burner, molten
metal), an aluminium pipe (`FluidTransportBehaviourMixin`) or tank (`FluidTankBlockEntityMixin`, any outer block of it,
`AluminiumMelting.besideHeat`) waits `AluminiumMelting.MELTS_AFTER` and melts. `AluminiumMelting.metalIn` gives what
it sets as: 60 mB a pipe, a pump that and a cog, 180 mB a tank block. A whole tank gives way at once
(`Overheating.giveWay`): what it held spills first and its metal sets after, on the spill or beside it; before this
fix the metal took the spot and the fluid had nowhere to go. A pipe's 60 mB is one layer of rough metal, which a
flowing fluid washes away like a snow layer: lava beside a melted pipe runs into the gap and takes it.

**Vat mixers.** `VatMixerBlock.Kind` is `COPPER`, `IRON`, `STEEL`, `ALUMINIUM` and `BRASS`, the last with no mixer.
`VatMixerBlock.formOf(Create's mixer)` is null, so Create's mixer used on a vat goes on as an ordinary mixer, and a
mixer used on a vat of another metal is refused ("mcremix.vat_mixer.fits"). Ponder's vat scene and JEI's Vat Mixing
tab show the copper one, with every mixer and tank that fits as catalysts.

**Recipes.** `gen_fluid_part_recipes` makes them as Create makes its copper ones, in each metal: a sheet either side of
an ingot makes four pipes, across or upright (`*_fluid_pipe_vertical`), a pipe and the metal's cog a pump (aluminium's
with the `mcremix:polyethylene` tag too), and a sheet above and below a barrel a tank. Glass pipes are panes either side
of a block of glass. Create's chute is iron sheet, ingot and sheet upright and its item vault iron sheet, barrel and
sheet upright, so iron pipes are made across only and an iron tank has its sheets either side of the barrel; the
first GameTest run caught the chute. Before 0.20.0 no recipe made any of this mod's pipes, pumps or tanks (since
0.13.0), so the Pressurizer, which takes a steel tank, could not be made in survival.

**Textures.** The brass, iron and aluminium pipes, pumps and tanks are Create's copper ones recoloured
(`tools/create_fluid_textures.json`), and the iron ones come in Immersive Weathering's rust stages (exposed, weathered,
rusted) as well. The project folder adds a C2PA `caBX` chunk to every PNG written into it (project memory
`build-routing.md`), about 1.6 kB each once zipped; a working copy taken from it built a 6.2 MB jar
where 0.19.1's was 4.6 MB. The chunks were stripped from the working copy before the 0.20.0 jar was built (the pixels
are untouched); strip them again whenever a jar is built from a copy of the project folder.

**Tests.** `SpringTests` held the cementation pack's two tests as well as the spring's; they are in `SteelTests` now.
A creative motor turns no faster than 256 RPM (`maxMotorSpeed`), so the vat mixer break test drives its copper mixer
at 512 through a steel large cog, as `SteelTests.doublingLane` does.

Covered by GameTests, not played by hand:

- [ ] Iron parts rust only while wet, each to a final stage of its own; rusted tanks leak 1 mB a tick a block, waxed
      and scraped ones nothing; chlorine leaking out of a tank hangs beside it as a cloud; a rusted line leaks where
      a waxed one does not.
- [ ] Aluminium tanks and pipes melt with lava inside, and beside lava, into rough aluminium; iron holds lava.
- [ ] Brass pumps pump and snap past 96 RPM; a line with a brass pipe carries half as much; iron pumps find new pipes.
- [ ] Iron and brass tanks make Create's boilers and aluminium never; steel boilers are compact.
- [ ] Each mixer fits a vat of its own metal, Create's mixer goes on as an ordinary mixer, no mixer fits a brass vat,
      and a copper vat mixer breaks at 512 RPM.
- [ ] Every pipe, pump and tank crafts; iron's upright grids still make Create's chute and item vault.
- [ ] Old worlds: slag roads become dirt paths and the rest are known as taken out.

In game (dev client): brass pipes draw thin, straight, bent and joined to Create's, iron parts in each stage of rust,
aluminium parts, the three new pumps turning, and a rusted tank dripping.

GameTests: `IronRustTests` (5) is new; `FluidPartTests`, `VatTests` and `PressureBoilerTests` trade their wood tests
for brass, iron and aluminium ones; `SpringTests` (8) and `RoadTests` (4) are gone, and `SteelTests` has the cementation
pack's checks from them as two tests of its own.

## 26. The fermenter look (0.21.0)

Taran's ask of 2026-10-26, with a photo of conical brewery fermenters: make vats look more like them. Five cards in
project memory `vat-fermenter-look.md`: the whole standing vat takes the look, its scaffolding becoming a cone on legs
(loose tanks and scaffolding are unchanged); each vat is in its own tanks' metal, so copper weathers and iron rusts;
square, Create's tank body with the cone and no dome; no window, goggles say what it holds; and a door on each side
(Taran's change from one door a wrench moves). DESIGN.md has it under "The fermenter look (0.21.0)". Built on 0.20.0.

**Drawn into the chunk.** Nothing about the look is a block or a block state: it is drawn as the vat's blocks are built
into their chunk. `FluidPartModels` wraps every tank model in `VatShellModel` inside its sprite swaps (Create's copper
tank under its patina swap, this mod's metals under theirs), so what it adds comes out in the tank's metal, patina or
rust, and Create's andesite scaffolding in `VatStandModel`. Their `getModelData` asks `VatLook`, which reads the level
being built (for a chunk, Minecraft's copy of the chunks round it, block entities included): a tank block in the same
layer as its controller, whose vat is formed (`standingOver`), gets `DOORS`, and a scaffolding block under one gets
`STAND` (the tank's state above it, the vat's width and where in it the block is). Scaffolding with `STAND` is drawn in
the tanks' layer (cutout mipped) and asks the tank model above it for its part of the stand, so the cone and legs take
the tank's metal too, as do its breaking particles.

**The parts.** `VatParts` lays them out in pixels across the whole vat and cuts each to the block that draws it, and
`VatShellModel` caches them by part, width and place. Under the vat: a cone of the tank's own `fluid_tank_inner` from
a pixel in from its edge down to an outlet in the middle (`CONE_END`, 10 px down the scaffolding), with a valve lever
pointing north, and four legs at its corners, braced together and standing on feet. In the middle of each side of the
lowest tank layer (`doorBlock`; on a vat two across the door spans two blocks): a manway door, a dark gasket ring with a
plate on it, a bright cross handle on a hub, and two hinges. A door is left off when the block in front of it (on a vat
two across, either block) is not air (`VatLook.clear`), so a vat mixer, a pipe or an Electrolyser goes on with no door
behind it. Vanilla's `setBlockDirty` builds the neighbouring sections again too, so a door goes and comes back as
things are placed in front of it and taken away.

**Drawing it again.** A chunk is only built again when told, so `Vat.redraw` (on a client) has every block the vat
could take up, its scaffolding too, built again whenever its look changes: when it forms, breaks up or changes size
(`drawn` holds the look last drawn). `FluidTankBlockEntityMixin` hooks Create's `invalidate`, which `setRemoved` always
calls (`remove()` only when the chunk is not unloading), so a controller going on a client calls `Vat.forget` and its
blocks are drawn without the look.

**Windows.** Each time the controller evaluates a standing vat, `Vat.closeWindows` shuts its tanks' windows (Create's
`setWindows(false)`) and remembers that they were open (`HadWindows`, saved); `openWindows` opens them again when it is
taken apart, unless the tanks are a boiler by then (Create's boiler shuts them itself). `FluidTankBlockMixin` returns
`PASS` from `onWrenched` on a standing vat, so a wrench does nothing to it. `VatLayersRenderer` and
`FluidTankRendererMixin`, which drew a vat's layers through its windows, are gone; goggles list the layers as before.

**Textures.** Everything but the cone is on a new sheet, `fluid_tank_vat`, drawn in Create's copper by `gen_assets.py`
(`vat_sheet`, in `OWN_COPPER_TEXTURES`): the door's plate, a disc ten pixels across lit from the top left with four
bolts; the legs' strip; the hub; and single colours, two pixels high, down the right. `TANK_SPRITES` gains `_vat`, so
the recolouring that makes each metal's tanks, Create's copper patina and iron's rust makes a sheet for each of them
as well, eleven in all (`*fluid_tank_vat.png`). The 1.20.1 block atlas takes in everything under `textures/block/`, so
no model has to name the sheets. F3+S writes the atlases, with where each sprite lies, to `run/screenshots/debug`;
that is how the bug below was found.

**Bug: stray textures under wider vats.** At first a vat two or three across drew a box of other sprites (gold-tinted
titanium, the dropper's front: the vat sheet's neighbours in the atlas) under its tanks. `AABB`'s constructor puts each
pair of corners in order, so `intersect` of two boxes that do not meet gives the gap between them, not nothing: a part
outside a block was cut to that gap and drawn there, its UVs running off its sprite. `VatParts.box` checks `intersects`
first now.

**Ponder.** A vat in a Ponder scene never evaluates, so the vat scene (`ElectrolysisScenes`) and the vat mixer scene
(`VatMixerScenes.setVat`) form it by setting `Vat.Formed` in its controller's NBT and drawing its sections again; their
schematics (`gen_assets.py`) have plain tanks with no windows. Both scenes point at the vat with Engineer's Goggles, and
the vat scene says its windows shut while it stands and open again once it is taken apart.

DESIGN.md also loses a stale clause calling dirt paths beds for slag roads, which 0.20.0 took out.

Covered by GameTests, not played by hand:

- [ ] A standing vat's windows shut as it forms and a wrench does nothing to them; taken off its scaffolding, they
      open again and a wrench works as on any tank.

In game (dev client): vats one, two and three across with their cones, legs and doors and nothing stray; copper vats in
each stage of patina, iron vats in each stage of rust, and brass, steel and aluminium vats; a vat mixer with no door
behind it; a block of scaffolding taken out (the vat breaks up, its windows open and the scaffolding is drawn as
itself) and put back (it forms again and shuts them); and both vat Ponder scenes.

GameTests: `VatTests` gains `aStandingVatKeepsItsWindowsShut`.

## 27. Texture changes (0.22.0)

Taran's ask of 2026-10-26: red mud bricks as vanilla's mud bricks with a red tint, steel darker (like Create's metal
girder or industrial iron block), titanium's tinting more subtle, heat sinks smaller, and more detailed heat sink,
radiator, water cooler and arc furnace textures. Built on 0.21.0. Looks only: no recipe, tag, config or behaviour
changed, and no block or item was added or taken out.

**Red mud bricks are vanilla's, tinted.** `RED_MUD_BRICK_COLOURS` and `brick_texture` are gone, and so is
`red_mud_bricks.png`. The four blocks' models now draw `minecraft:block/mud_bricks` with `tintindex: 0` on every face,
and `McRemixClient.onBlockColors`/`onItemColors` multiply that by `RED_MUD_TINT` (`0xE18778`), the way grass is
coloured. Vanilla's own models could not be used as parents: a child's `elements` replace a parent's, and vanilla's
have no tint index, so `tinted_templates()` in `gen_assets.py` keeps a copy of vanilla's elements with the index added,
for `cube_all`, `cube_north_west_mirrored_all`, the three stairs shapes, both slabs, the three wall pieces and
`wall_inventory`. The blockstate uses the mirrored model as vanilla's mud bricks do, so the brick courses still break
up across a wall. Two things fall out of this: the red follows any resource pack that retextures mud bricks, and the
jar carries no copy of a vanilla texture.

**Steel.** The `steel` palette went from a mid grey to a dark gunmetal, `#525357` over `#333338` with a `#7C7A7B`
highlight, read off Create's girder and industrial iron. `exposed_steel` and `weathered_steel` were warmed to match so
the rust stages still run from it, and `STEEL_ITEM` was redrawn around the new base. Blister steel was left alone: its
pitting reads against the darker ingot, and the two are still told apart at a glance.

**Titanium tints.** The gold, purple and blue tempers are now mixed two fifths of the way from titanium's own grey to
their old colours, picked from a 100/50/40/30% strip rendered side by side. Every tinted part regenerates from the
palette, so the casings, brackets, chain drives, gearboxes, platings and the rest all moved together.

**The heat sink.** `heat_sink.png` is gone, replaced by `heat_sink_base` and `heat_sink_fin`. The model is a 12x12x2
plate with six 1-px fins 6 px tall and 8 deep (`HEAT_SINK_FINS`), where it used to be a full-face block, and
`HeatSinkBlock`'s `VoxelShape` is built from the same numbers through `DownModelShapes.box` so the outline matches what
is drawn. The fin texture is ribbed down its length and the base has bolted ledges, a lit chamfer and a thermal pad;
the Ponder pointers that used to aim at the middle of the block would now float above it, so `HeatSinkScenes.side` and
`.top` give the two spots a sink is pointed at, and `FrictionHeatScenes` and `ElectrolysisScenes` use them too.

**More textures.** The Water Cooler's one texture became `water_cooler_base`, `_top` and `_side` (rim, screws, a
recessed serpentine channel, a lid edge, a seam and a round water port); the Arc Furnace's hearth became
`arc_furnace_shell`, `_lining`, `_rim` and `_post` (hammered copper dented and lit on the upper left, a water pipe
round the wall, seams and rivets, refractory brick sooted towards the middle); the Radiator gained
`radiator_fin` (louvres and a rippled edge) and its panel gained tube-end rings. Each new texture has a seed of its
own; 1300 and 1301 were already taken, so the arc rim and post use 1360 and 1361.

**Checked in a dev client.** A datapack sets out the cooling blocks, the steel parts beside `create:industrial_iron_block`,
the titanium parts beside untinted titanium, and red mud bricks beside vanilla's; screenshots are in
`/mnt/project-files/builds/screenshots/0.22.0`.

## 28. Rusted iron leaks as an option, and vat outlets (0.22.1)

Two asks from Taran on 2026-10-26: "Can you actually have rusted iron pipes and tanks not leak by default but have it as a
config option? Keep the drip animations though either way.", and "make it so that the small pipe at the bottom of vats
attaches to adjacent inventories instead of always being there". Built on 0.22.0.

**Leaks.** `RemixConfig.RUSTED_IRON_LEAKS` (`rustedIronLeaks`, under `weathering`, default false) gates the loss through
`Leaks.losesFluid`: `FluidTankBlockEntityMixin.mcremix$leak` and `FluidNetworkMixin.mcremix$leak` return early with it
off. The drips (`Leaks.drip`, on the client) only ask whether a block is rusted through, so they show either way, and
a waxed or scraped block still stops dripping.

**Outlets.** `VatLook.outlets` gives the stand a bitmask (`Stand.outlets`, one bit per `Direction.get3DDataValue`) of
the ways to inventories: an item handler under the middle block or blocks of the scaffolding (one on a vat one or three
across, the four round the middle on two), or beside the middle of a side. `VatParts.standBoxes` draws the outlet only
when there is one: down, it reaches the bottom of the scaffolding with a flange there and no mouth; sideways, a 4 px
pipe runs from it to the edge of the scaffolding with a rim against the inventory, one per way; the valve lever goes on
the first side, clockwise from north, with no pipe. With none, a plate closes the cone's bottom. `VatShellModel` caches
stand quads under a negative key with the mask in it, so they never meet the doors' keys. Placing or breaking an
inventory beside the scaffolding builds its section again (vanilla's `setBlockDirty` takes in the neighbours), so the
pipe comes and goes with it. Where the vat empties to is unchanged (`Vat.pushOutputs`: every inventory under or beside
its scaffolding).

**Tests.** `IronRustTests`' three leak tests run in a batch of their own, `rustedIronLeaks`, whose `@BeforeBatch` turns
the option on and `@AfterBatch` puts it back; `rustedIronOnlyDripsByDefault` is new. In game (dev client): the outlet
down into a chest and into a hopper, sideways to chests and a barrel on vats one, two and three across, none on a vat
with nothing round it, and a chest placed beside one and broken again.

## 29. Taran's list (0.23.0)

Taran's ask of 2026-10-26 17:53, a list of nineteen things, built in three jars: this one (looks, removals, supports,
tooltips, pipes, weathering, the Electrolyser and the vat mixer), 0.24.0 (the two-block Pressurizer, steam engines and
electric pumps in more metals) and 0.25.0 (structural formulas on tooltips and the JEI pages). Built on 0.22.1. The five
cards Taran answered and Claude's own readings are in project memory, `supports-list-cards.md`.

**Supports.** Friction points are called supports wherever a player reads it: lang, goggles, Ponder, tooltips, config
comments and docs. The code keeps its names (`FrictionPoints`, `FrictionPointTests`, the `friction_point` lang keys and
the `frictionPoints` config key), so nothing in a world or a config changes. The block tag is now `mcremix:supports`,
and it takes in the old `mcremix:friction_points`, so a pack that filled that still works.

**The speed controller's top slot.** `FrictionPoints.inSpeedControllerSlot`: a bare large cog right above a rotational
speed controller, turning across the controller's shaft (the one Create has it drive), is held there as by a bolted
bracket and holds the parts along its axis within their span. It stays a bare part with no top speed of its own, so the
controller can still drive it faster than the controller's own brass turns. Ponder: `kinetics/speed_controller`.

**Stability and Max Speed tooltips.** `KineticRatings` and `client/KineticTooltips`: every kinetic block made of a
material, Create's included, gets two lines under Create's stress lines, drawn as Create draws them, a bar and a word
(card A): Stability is the load it carries (Flimsy 2,048 su, Delicate 3,072, Light-duty 4,096, Sturdy 8,192,
Unbreakable for any load) and Max Speed its top speed at a support (Sluggish 64, Slow 96, Moderate 128, Fast 256, Very
Fast 512, Ludicrous 2,048). With goggles on they show the su and RPM instead. What holds no shafts has no Max Speed line,
but for the crushing wheel, which keeps andesite's.

**Brass.** No clutch, gearshift or chain gearshift any more (`KineticMaterial.comesAs`), and only brass makes the
mechanical crafter: `MachineKind.CRAFTER` is a kind with no tiers, and the copper, iron, steel, aluminium and titanium
crafters are gone with `TieredCrafterBlock`, its renderer mixin and its connected textures. `RemovedBlocks`: placed ones
go quietly from old worlds, and in inventories they become Create's clutch, gearshift, adjustable chain gearshift and
mechanical crafter. Brass's gearbox, chain drive and bracket textures are drawn afresh in `BRASS_SHINE`, seven shades
of polished brass from near-black amber to a pale glint (`brass_casing_texture`, `brass_chain_drive_texture`,
`brass_bracket_texture`): a bright bevel round a plate engraved with Geneva stripes and a fine inner line, under a
sheen, a rosette over each framed corner, a ruby jewel where a shaft comes out (`jewel`, as a clock's pivots run in),
and a fine fusee chain. `brass_clockwork_texture` takes the same shades, with a ruby at each hub's heart, and the large
brass cog gains a hoop across its spokes (`clockwork_wheel(hoop=6)`). Seeds are unchanged, so every other material's
textures come out as before.

**Titanium, shinier.** Its palette is brighter and wider (`#8E8A87` over `#4A4746` to `#E2DED9`), and each temper
colour stays two fifths of the way from it to the same full temper colour as 0.22.0's. `casing_texture` and
`material_texture` treat every titanium look as polished: brighter catch-lights and streaks, and a sheen across a
casing's panel; the titanium block and plating have a sheen across each plate.

**Iron Casing.** `ModBlocks.IRON_CASING`, a Create casing block with a connected texture drawn from iron's gearbox
casing: an iron sheet applied to a stripped log or wood. It takes over everything Create's Block of Industrial Iron did
for the iron tier: the iron gearbox, clutch, gearshift and chain drives are built on it, it encases shafts and cogs as
iron (`EncasingCasing.IRON`) and swaps a machine to iron (`CasingSwaps`). Parts encased in Industrial Iron before keep
working, and give back an Iron Casing when a wrench takes it off. It never rusts.

**Weathering stops late.** `Weathering.finalStage` now picks the second-to-last or the last stage, so copper and iron
always get at least to weathered (rusty), still into a mix.

**Pipes join only their own metal (cards B).** `PipeMetal` sorts a block into copper (Create's pipe, glass pipe and
encased pipe), brass, iron, glass, steel, aluminium or PVC, or none for everything else: pumps, valves, smart pipes,
tanks and machines join any metal. Two pipes of different metals never connect, at four levels:
`FluidPipeBlockMixin` (`canConnectTo` in `updateBlockState` and `shouldDrawRim`), so no arm is drawn between them;
`PipeTransportMixin` on Create's two pipe transport behaviours (`canHaveFlowToward` and `getRenderedRimAttachment`), so
no flow is offered across, which matters because Create re-opens a straight pipe's ends whatever its neighbours are, and
the end gets a rim (a partial rim on glass); `FluidPropagatorMixin.isOpenEnd`, so such an end does not spill; and
`FluidTransportBehaviourMixin.initialize`, which unjoins pipes of two metals in worlds from before (`PipeMetal.rejoin`).
PVC joins only PVC and fittings (`PvcPipes.port`).

**Copper weathers on coolers, radiators and arc furnaces.** `WaterCoolerBlock`, `RadiatorBlock` and `ArcFurnaceBlock`
have `Patina.PROPERTY`, so they weather, wax and scrape as any copper block does, for looks only. Their copper is drawn
at each stage (`copper_look`, with specks from `speckle`): `water_cooler_{top,side}`, `radiator_{panel,tube}`,
`arc_furnace_{shell,rim,post,terminals}`, each with a stage suffix and a model per stage.

**Electrolysers go on the side of a basin.** `ElectrolyserBlock.getStateForPlacement` faces the block clicked when it is
beside it, and the way the player looks when placed on top of something or under it. `ElectrolyserBlockEntity.vessel`
takes only a horizontal facing, into a basin or a vat. One from an older world sitting on a basin faces down, and is
fixed to nothing now (goggles: "Not on the side of a basin or a vat"); a wrench or a pickaxe takes it off to put back on
a side. The electrodes stand four pixels out of copper clamps on the terminals, so what is fitted shows. A test player
is a fake player, which Create's basin takes as a deployer setting its filter wherever its side is clicked, so
`basinsTakeElectrolysersOnTheirSide` places its side Electrolyser straight from the item.

**Vat mixers.** A vat mixer is a plate of its vat's own tank, six pixels thick (`vat_mixer_tank`: Create's tank for
copper, this mod's for the rest, at the mixer's own weathering stage; a steel one rusts though steel tanks do not, so
`{exposed,weathered,rusted}_steel_fluid_tank.png` exist for it alone), with a cog on its face in its mixer's metal
(`ModPartialModels.shaftlessCog`, drawn by `VatMixerRenderer` and its Flywheel visual). It is an `ICogWheel` with no
shaft, as Create's mixer is: it turns with the small cogs beside it and the large cogs diagonal to it, in the plane
across its facing. It is still a support of its mixer's material, holding nothing.

**Tests.** 300 GameTests; new: `speedControllersHoldTheCogOnTop`, `tooltipsRateEveryMaterial`,
`pipesJoinOnlyTheirOwnMetal`, `pvcJoinsOnlyPvcAndFittings`, `coolersRadiatorsAndArcFurnacesWeather`,
`basinsTakeElectrolysersOnTheirSide` and `aVatMixerTurnsByItsCogNotAShaft`. The machine tier, gearbox, clutch, chain
drive, weathering, Electrolyser and vat tests were changed to suit: Iron Casing for Industrial Iron, no brass clutch,
the later final stages, Electrolysers on basin sides, mixed pipe lanes made one metal, and vat mixers turned by a cog.

## 30. Taran's list, part two (0.24.0)

The second of the three jars for Taran's list of 2026-10-26: the Pressurizer in two blocks, steam engines in brass,
iron and steel, and electric pumps in every metal that has a pump. Built on 0.23.0. Taran's cards C and D settled the
engines; the rest are Claude's readings of the list, in project memory (`supports-list-cards.md`).

**The Pressurizer is two blocks.** `PressurizerBlock` is a `HorizontalKineticBlock` now: a riveted steel body with a
pressure dial on its front and a shaft coming out of its back only (`hasShaftTowards`), under a pressure vessel,
`PressurizerVesselBlock`, a riveted drum twelve pixels across with a safety valve on its crown. Placing a Pressurizer
needs room above and puts the vessel there (`VESSEL`, set in `setPlacedBy`); its back meets a shaft beside it if one is
there to drive it, as Create lines machines up, and otherwise its dial faces the player (sneaking, it always does). The
two go together as a door's halves do: breaking the vessel breaks the Pressurizer, which drops itself, and the vessel
picks as the Pressurizer and drops nothing of its own. A contraption carries them together
(`PressurizerVesselBlock.registerMovementChecks`). The titanium one's vessel shows its temper colours
(`PressurizerVesselBlock.Tinted`, `matching`). Items and fluids go in and out at its sides and its bottom, not its
front, back or top (`PressurizerBlockEntity.opensTo`). The dial reads the pressure, which is speed: `pressure()` is the
speed as a share of the top speed of its metal (512 for steel, 2,048 for titanium), and the needle sweeps 270 degrees
from nothing to 1.15 of top speed, into the red past 1, swinging after it and trembling while a job runs
(`moveNeedle`). `PressurizerVisual` (Flywheel) and `PressurizerRenderer` draw the half shaft at its back and the needle
(`block/pressurizer/needle`). One from before 0.24.0, saved without `TwoBlock`, turns on its first tick so its back
meets its drive, and gets its vessel if there is room above; with no room it does without, and its top still takes
pipes as it used to (`upgrade`). New textures: `pressurizer_front` (the dial), `pressurizer_needle`,
`pressurizer_valve`, `pressurizer_vessel_{side,top}`, and the titanium ones in each temper colour. The Ponder scene
shows the vessel and the dial.

**Steam engines in brass, iron and steel (cards C and D).** `MetalSteamEngineBlock` extends Create's
`SteamEngineBlock` and is a `CreateStandIn` for `AllBlocks.STEAM_ENGINE`, so Create's engine code, its renderer and
visual, its boiler and its powered shaft take it for Create's engine and it runs exactly as Create's does: the same
speed and capacity from each boiler level (card C). There is no aluminium engine, since aluminium tanks make no
boiler (card D); copper's is Create's own. Each goes only against a tank of its own metal: `canSurvive` on the metal
engines, `SteamEngineBlockMixin` keeping Create's engine to Create's copper tanks, and both refusing on placement with
"Brass Steam Engine goes only on Brass Fluid Tank boilers" over the hotbar (`SteamEngines.refuse`). An engine runs only
on its own metal's boiler (`SteamEngineBlockEntityMixin.isValid`), and a boiler counts only the engines that run on it
(`BoilerDataMixin`), so one left on another metal's tank by a command or a schematic takes no steam. Worlds from
before have Create's engine on brass, iron and steel boilers: on its first server tick it becomes that metal's engine,
keeping its facing and its scroll setting (`SteamEngines.match`). Each is made as Create's is, with its metal's block in
place of the copper block, and drawn from Create's engine texture in its metal. They do not weather. Ponder:
`steam_engine/metals`, on every engine, Create's engine and every tank.

**Electric pumps in every pump metal.** `MetalElectricPumpBlock` and `MetalElectricPumpBlockEntity` extend Power Grid's
electric pump, which is the copper one, in brass, iron, steel and aluminium. They draw as Power Grid's does (its
resistance, through a `ResistanceValues` provider registered at common setup) and go faster the more power they take,
as it does, but top out at their metal's mechanical pump speed instead of copper's: 96 RPM in brass, 256 in iron, 512
in steel and aluminium, scaled with Power Grid's `electricPumpMaxSpeed`. Power Grid rates its pump for a fifth more power
than its top speed needs, and it overheats on much more; each metal's rating is scaled with its top speed the same way,
so a steel pump runs on volts that would burn Power Grid's. They are fluid parts of their metal (`FluidPartMaterial.Part`): rated
for heat as its pipes are (brass and iron Hot, steel Searing, aluminium Warm), an aluminium one melts beside anything
Hot into a pipe's worth of rough aluminium, and an iron one rusts while wet, drips rusted through, leaks with
`rustedIronLeaks` on and is cleaned by a Spout of hydrochloric acid. Like every pump they join pipes of any metal. Each is
a Power Grid electric motor on a pipe of its metal, as Power Grid's is a motor on Create's copper pipe; the aluminium one
needs no polyethylene bushing, having no shaft. Power Grid's own pump is tagged Hot, as Create's copper pipe is. The
textures are Power Grid's pump in each metal with its grey motor kept (`tools/powergrid_textures.json`, Power Grid being
Apache-2.0), the iron one at each rust stage.

**Fixed:** iron pipe, pump and tank tooltips said rusted iron leaks; since 0.22.1 it only drips unless
`rustedIronLeaks` is on, and they say so.

**Tests.** 315 GameTests; new: `placingOnePutsItsVesselOnTop`, `itsTwoHalvesGoTogether`,
`onlyItsSidesAndUndersideTakePipes`, `itsShaftIsAtItsBackOnly`, `theDialReadsSpeedAsAShareOfTopSpeed`,
`oneFromBefore024TurnsToItsDrive`, `eachEngineRunsOnItsOwnMetalsBoilerAlone`,
`createsEngineOnAnotherMetalsBoilerBecomesThatMetals`, `everyMetalsEngineGivesWhatCreatesGives`,
`anEngineGoesOnlyAgainstItsOwnMetalsTank`, `enginesAreMadeAsCreatesIsOnTheirMetalsBlock`,
`eachMetalsElectricPumpTopsOutAtItsMetalsSpeed`, `anElectricPumpPumpsThroughPipesOfAnyMetal`,
`electricPumpsAreAMotorOnAPipe` and `electricPumpsHoldWhatTheirMetalHolds`. The electric pump tests wire a creative
voltage source to a device connector on the pump, as a player wires Power Grid's. The Pressurizer and steel boiler
tests place the vessel and each boiler's own engine, and `ironPartsRustOnlyWhileWet` takes the iron electric pump too.

## 31. Taran's list, part three (0.25.0)

The last of the three jars for Taran's list of 2026-10-26: structural formulas on the tooltips of compounds, and the
JEI pages for this mod's recipe types laid out and drawn as Create's own. Built on 0.24.0. Taran's card E settled when
formulas show; the rest are Claude's readings of the list, in project memory (`supports-list-build-choices.md`).

**Structural formulas (card E).** Every compound this mod makes, and those from Create and Minecraft, draws its
structural formula on its tooltip, under its name and lines, all the time rather than only with Shift (card E): its
atoms written in their element's colour as chemists colour them (carbon grey, hydrogen white, oxygen red, chlorine
green, sodium violet, iron orange, titanium silver, aluminium pink, silicon tan, calcium pale green, phosphorus
orange, lightened where they would vanish on a tooltip's dark ground), its bonds as lines, double bonds as two, faint
lines between neighbouring ions, and the formula written out underneath. Polymers show their repeat unit in brackets
with an n (polyethylene, PVC, Bakelite); ionic solids show their ions (salt as a patch of its lattice, bone meal as
calcium phosphate); sugars and rings are drawn skeletal, their carbons as corners; solutions show both parts ("NaCl in
H2O"). There are thirty: this mod's chemicals, as items, fluids and every bucket of those fluids; Create's limestone;
and Minecraft's water (with ice, packed and blue ice and snow), sugar, quartz, clay, calcite, dripstone and bone meal.
JEI's fluid tooltips show them too, the fluid known by its name.

`tools/gen_assets.py` lays each one out in bond lengths into `assets/mcremix/formulas/<name>.json`
(`StructuralFormula`): its atoms (label, place, charge, hydrogens, whether it is a hidden skeletal carbon, a count
before it and its share of an atom shared with the next unit on), its bonds (order, and the side a double bond's second
line goes), brackets and loose signs, and the items and fluids that show it. `FormulaTooltips` loads them as a client
resource reload listener, so a resource pack can change or add them, and `Formulas.Index` maps items, fluid types and
every bucket of those fluids to their formula. `RenderTooltipEvent.GatherComponents` adds a `FormulaTooltip` to the
tooltip, which `FormulaTooltipRenderer` draws: labels in Minecraft's font, subscripts, charges and counts in a font of
3 by 5 pixel digits and signs (`font/formula_small`) that stays crisp at every GUI scale, and bonds as one-pixel lines
stopped short of the labels. A drawing too wide for a tooltip has its bonds shortened to fit, down to 12 pixels.

**A start-up crash, and what it was.** The first client built with formulas crashed at start-up: "Found unused
register callbacks" from Create's Registrate, then "Registry Object not present: minecraft:item.bucket.fill_milk".
Bisecting found two listeners added to the mod bus as lambdas,
`modBus.addListener((RegisterClientTooltipComponentFactoriesEvent event) -> ...)`, in a `FormulaTooltips.register`.
Written as static methods and added by method reference from `McRemixClient`, as the mod's other client listeners
are, the client starts. Keep mod-bus listeners as method references.

**JEI pages as Create's.** All fourteen of this mod's tabs (Furnace, Furnace Heat, Striking Blooms, Melting, Alloying,
Settling, Fermenting, Boiling, Electrolysis, Steel Furnace, Steel Furnace Fuel, Vat Mixing, Pressurizing and Arc
Furnace) are laid out and drawn as Create lays out its basin pages, so they sit beside Create's tabs as one set
(`Page`): 177 by 103, what goes in along the bottom on the left in Create's slot frames (each item once, with how many
of it, as Create condenses them), what comes out on the right under Create's down arrow (a chance output in Create's
dotted frame with Create's chance tooltip), the machine in the middle (`Scene`) drawn from Create's angle, lit and
moving as Create draws its own and standing on Create's shadow or on the glow of what heats it, and a bar along the
bottom, Create's heat bar or plain bar, giving the heat, time, speed, charge or energy. A slot on the bar's end cycles
through what heats the machine that hot from below, each with a note on how. Hovering the bar says more: hotter is
faster, a big crucible is faster, the heat the Furnace's bonus starts at, the least speed a Pressurizer needs, what
charge is, the power the Arc Furnace needs to melt at all.

What is drawn: the lit Furnace and Steel Furnace, facing us; the crucible, its pot holding the recipe's molten metal and
its shaft turning, over the heat (a campfire for Warm, lava for Hot, Create's blaze burner, fed for Searing and
superheated for Blazing); a bloom jumping on the Stone Anvil; Create's basin filled nearly to its brim with the
recipe's fluid and its items floating on it, so they show over the rim, over a blaze burner for Boiling and with an
Electrolyser on its side, both electrodes in, for Electrolysis; a steel vat on its cone and legs with a door on its
side and a steel mixer's cog turning on its front; the Pressurizer under its vessel, its needle trembling at the
recipe's least speed; and the Arc Furnace melting, both electrodes in. Three blocks tall with its heat, a heated
Pressurizer stands a block higher than the rest so its fire sits on the bar rather than across the words on it, and
`Page.besideFire` keeps the words on other heated bars clear of the fire (Boiling's "Superheated" leaves its time to
the tooltip). A vat's doors and stand come from model data, which Create's GUI blocks do not pass, so
`Scene.block(state, ModelData, ...)` renders through `BlockRenderDispatcher.getModelRenderer().renderModel` with
`VatLook.doors` and `VatLook.stand`. `BasinSlots` is gone.

Some words changed with them: the Arc Furnace's bar reads "16 kJ: 10s at 1.6 kW", the time on its arc's 20 V, as
Electrolysis reads "100 C: 10s at 10 A", with the 400 W under which the arc only warms the hearth in its tooltip;
Vat Mixing reads "3.3s a batch at 64 RPM"; the Furnace Heat tab's notes say lava, fire and soul fire go "under it";
the Steel Furnace Fuel tab sets each fuel out as a table of heats against mB a tick beside the furnace, with the pump
speed that moves each in the line's tooltip.

**Tests.** 318 GameTests; new: `everyDrawingHoldsWhatItsFormulaSays`, `writtenFormulasCountTheirAtoms` and
`compoundsShowTheirFormulas`. No GameTest reaches JEI's screens, so the pages were checked in a dev client, every tab
in turn.

## 32. Taran's nine changes (0.26.0)

Taran's list of 2026-10-27 (13:01), nine changes, and a tenth at 13:08: a cog right-clicked into a casing goes in.
Built on 0.25.0. Taran answered three cards: the brass bracket stays the clock bridge already drawn (card A), the Brass
Mechanical Pump gets a handwheel valve (card B), and steel boilers run on piped steam only (card C). The rest are
Claude's readings of the list, in project memory (`nine-changes-build-choices.md`).

**Element symbols.** A material of one element shows just its symbol on its tooltip, in its element's colour, with
nothing written out under it (`StructuralFormula.isElement`; `FormulaTooltipRenderer` draws the symbol alone). Iron,
copper, gold, zinc, aluminium and titanium on their ingots, nuggets, sheets and blocks (titanium's sponge too) and on
their melts and every bucket of those; molten sodium; and carbon on diamond, the diamond block and the Carbon
Electrode. `tools/gen_assets.py` writes them as one-atom formulas. Copper is drawn copper, gold gold and zinc lavender.

**Brass, cleaner (Taran: no red, thicker shafts, no rings, a new bracket).** The brass clockwork texture, casing and
chain drive are smooth polished brass: a bright bevel round a plain plate with one fine inset line, plain bushings
where shafts come out, and no rubies, rosettes or other red. The arbor is 3 pixels across where it was 2
(`clockwork_arbor`), with no collars on it. The bracket is a clock bridge (`clock_bridge_model`, card A): a thin pierced
plate standing over the part on two legs, a bearing ring round the shaft at its heart, its feet screwed down by a
strip; a cog's has two bridges either side of the wheel. The old bracket textures are gone.

**The Electrolyser (Taran: fewer red, blue and brown pixels).** A moulded black Bakelite head on its steel plate, its
terminals Bakelite collars each marked by a single thin red or blue band, with tinned steel clamps
(`electrolyser_poles_texture`, `electrolyser_plate_texture`), where the copper clamps and coloured collars were.

**Ingots and sheets in Create's style.** This mod's ingots, nuggets and sheets (steel, aluminium, titanium, blister
steel, the PVC sheet and Bakelite) are drawn as Create draws its own (`create_item_texture`): Create's brass ingot,
nugget and sheet, each pixel numbered by the shade of brass it is in, repainted in the material's nine shades, cooler
or warmer in the shadows as Create's zinc is; blister steel has bumps raised over its lit faces.

**The Brass Mechanical Pump's handwheel (card B), and no Brass Electric Pump.** The Brass Electric Pump is gone
(`RemovedBlocks` turns placed ones, and those in inventories, into Power Grid's electric pump; one left standing keeps
its block entity, `MetalElectricPumpBlockEntity`, which is also valid on Power Grid's block so it still ticks there,
going as Power Grid's at most 256 RPM). The Brass Mechanical Pump (`RatedPumpBlock.Throttled`, its block entity
`RatedPumpBlockEntity`) has a handwheel on a stem rising out of its body, with Create's flanges and a shallower
impeller under it (`brass_pump_models`, from Create's pump models kept in `tools/create_pump_models.json`). Holding
right-click on the wheel opens Create's value board, a slider from 1 to 24 mB a tick: 24 is all a brass pump moves at
its 96 RPM top speed, since Create's pump moves half its speed in mB a tick and a line through brass goes at half pace
(`RatedPumpBlockEntity.topFlow`). `FluidNetworkMixin` caps a line's transfer each tick at the lowest setting of the
brass pumps in it. A pump placed before 0.26.0 lets all through until it is turned down. The value box sits on the
wheel's face whichever way the pump faces (`HandwheelBox`), and the pump's shape takes in the wheel.

**Steam engines and boilers.** An engine drives a shaft of any material where Create's engine turns its own: this
mod's shafts and cogs are built on Create's powered shaft block entity (`TieredKineticBlockEntity`), so Create's engine
drives them as they are, and Create's own andesite shaft is swapped for Create's powered shaft as before. An engine
holds nothing (Taran), so the shaft it turns needs a bracket or support within its span like any bare part
(`FrictionPoints.mustBeHeld`, which counts Create's powered shaft as bare, and `PartSwaps` keeps a held shaft held across
the swap). `ContraptionMixin` carries this mod's shafts on contraptions as themselves, where Create carries its powered
shaft as an andesite one. The boiler's gauge is drawn in its tanks' metal (`FluidTankRendererMixin`,
`ModPartialModels.boilerGauge`): brass, iron at each stage of rust, steel, and Create's copper weathered with its tank.
A steel boiler (card C) boils no water and takes no heat from below: pressurised steam piped in is all that runs it,
every 10 mB a tick a level, and its goggles show Size and Steam in place of Water and Heat (`PressureBoilerData`). The
steam engine Ponder scene says so, its steel boiler running on steam.

**Cogs into casings (13:08).** A cog or large cog used on a placed casing goes into it (`CogsIntoCasings`): the casing
becomes that casing's encased cog, turning across the clicked face, as the cog would placed against that face, and the
casing pops back out of that face, since Create never uses a casing up when it encases a part and an encased cog gives
back only the cog. Sneaking places the cog beside the casing as before; a deployer holding a cog does it too.

**The Pressurizer.** Taran: rotation into the bottom half's left or right side; fluids and items in and out at the top
half's sides but not its top, where the valve is; the model reaching out to pipes it joins; square, with no staggered
corners; and the bottom half recessed round its shaft faces as the chain drive is, so shafts do not z-fight.
- *The shaft.* Its axis runs across it, left to right of the dial (`getRotationAxis` is the facing turned clockwise),
  and it has a shaft on both those sides (`hasShaftTowards`). Placed, its dial faces the player; a shaft ahead or
  behind it turns it a quarter so a side meets that shaft (`getStateForPlacement`). `PressurizerRenderer` and
  `PressurizerVisual` draw a whole shaft through it.
- *The vessel takes everything.* The body takes no pipes or funnels at all (`PressurizerBlockEntity.opensTo`), but one
  from before 0.24.0 with no room for a vessel still takes them at its top. The vessel has a block entity of its own,
  `PressurizerVesselBlockEntity`, which hands pipes and funnels on its four sides the Pressurizer's feed and output and
  what it has made (`throughVessel`); its top takes nothing. An empty hand on either block takes out what it made.
- *Nozzles.* The vessel has a north, east, south and west property, true where a pipe beside it is open towards it
  (`PressurizerVesselBlock.piped`, kept up in `updateShape`); each draws an 8 pixel square nozzle from the tank out to
  the block's edge, where Create's pipe clamps its drain flange over its end, and its shape grows to match. The vessel's
  blockstates are multipart, the titanium one per patina.
- *The model.* The body is framed as Create's chain drive is (`pressurizer_body_elements`): slabs round the edge, and
  the two shaft faces sunk 0.95 pixels inside the frame, so the shaft's end does not flicker against them; the back is
  plain casing and the underside sooted casing, with no ports. The vessel is a square tank 12 pixels across
  (`vessel_elements`), its top texture drawn square to match.
- *Old ones.* `PressurizerBlockEntity` saves `SideShaft`; one without it (from 0.24.0 or 0.25.0, shaft at its back, or
  before 0.24.0) turns a quarter on its first tick so a side meets what drives it, keeping its vessel or putting one up
  if there is room, its vessel's nozzles set from the pipes beside it. Pipes and funnels on an old one's body are left
  with nothing to join and have to move up to its vessel.
- *Ponder, JEI and words.* The Ponder scene drives it from its west side through a small cog, a large cog behind and a
  gearbox, and shows a pipe coming in at the vessel's side as its nozzle comes out. JEI draws its shaft turning through
  its sides. The tooltip and Ponder words say where the shaft and pipes go.

**A test that went by where it stood.** `lavaMeltsAluminiumPipesButNotIron` expected the first of its two aluminium
pipes to melt. Which of two aluminium parts gives way first goes by their positions (each takes up to a second more,
`Overheating.check`), and once one melts the line stops and the other, with nothing running through it, cools; with the
new tests the test stood where the second went first. It now takes whichever melted.

**Found in game.** Two things a dev client showed that no test reaches, both fixed and looked at again:
- *Black shaft ends in the Pressurizer's sides.* The shaft drawn through it ends in the recesses inside the block, and
  an opaque block keeps no light inside it, so the shaft there was drawn unlit. Both Pressurizers now let light in
  (`noOcclusion`), as Create's chain drive and encased blocks do. One placed before 0.26.0 keeps the dark light saved
  in its chunk until something works it out again, so `PressurizerBlockEntity.upgrade`, which turns an old one, relights
  its block (`checkBlock`).
- *Every metal's gauge kept copper fittings.* Recolouring Create's boiler gauge moved its copper casing along the
  metal's palette, but the post holding its needle and the bands round its sides are brighter than any copper shade it
  maps, so they stayed copper on brass, iron and steel. They are moved along the light end of the metal's palette too
  (`GAUGE_FITTINGS` in `tools/gen_assets.py`), so only the glass and the dial face keep their own colours.

**Tests.** 324 GameTests; new: `cogsGoIntoPlacedCasings` (from the top, a side and the front, a large cog, Create's
cog into Create's casing, in creative, sneaking, and a deployer), `enginesDriveAnyShaftButHoldNone` (a brass shaft in
its bracket and a steel one a gearbox holds turn at Create's speed and stay themselves; Create's helper is refused
where only an engine is, and an engine's shaft drops when its gearbox goes), `brassPumpHandwheelsSetTheMostTheyLetThrough`,
`theBrassElectricPumpIsPowerGridsNow`, `steelBoilersRunOnPipedSteamAlone`, `itsShaftGoesInAtEitherSide` (in place of
`itsShaftIsAtItsBackOnly`), `onlyItsVesselsFourSidesTakePipes` (in place of `onlyItsSidesAndUndersideTakePipes`) and
`oneFrom025TurnsASideToItsShaft`; `compoundsShowTheirFormulas` also checks the symbols of one-element materials.
Textures, models, the gauge, the value board and the Ponder scenes were checked in a dev client.

## 33. Stability for supports, Strength for shafts and cogs (0.27.0)

Taran's ask of 2026-10-29 (19:45): only supports have a Stability, their top speed; shafts and cogs have a Strength,
their load; a shaft or cog in a support turns as fast as the support allows, not the lower of the two; and parts reach
further without a support. Built on 0.26.0. Taran answered three cards: parts reach twice as far (card A), only shafts
and cogs break from load (card B), and Stability is named in steadiness words (card C). The rest are Claude's readings,
in project memory (`stability-strength-cards.md`).

**The limits.** `FrictionPoints.Limits` says what each block's limits come from. `Limits.ofPart` is a shaft or cog:
its top speed is its bracket's or casing's alone where it has one (`speed()` takes the holder before the part), and its
load its own material's wherever it is, bare, bracketed or encased (`load()`). `Limits.ofBlock` is everything else of a
material, a gearbox, clutch, gearshift, chain drive, machine, pump, mover or Create's crushing wheel: a top speed of its
own where it is a support (and the crushing wheel), and no load limit at all (`load()` is null). `KineticMaterial`'s
`slower` and `weaker`, which picked the lower of part and holder, are gone. So `KineticLimits.exceeded` finds a load
breach only on a shaft or cog, which breaks itself, and a speed breach on a support, which grinds: a held part's bracket
comes off or its casing is lost (`snapHolder`), and anything else breaks and drops. A wooden shaft is still the fuse;
a wooden gearbox, clutch or gearshift no longer is. The speed controller's top-slot cog keeps no top speed of its own,
as before, so the controller still drives it past brass's 96 RPM, but it keeps its Strength. The goggle lines for a
bracket or casing wobbling loose are gone, since load no longer breaks one; goggles read "Stability: up to 256 RPM"
on a support or a part in a bracket or casing, and "Strength: up to 4,096 su" (or "takes any load") on a shaft or cog.

**Reach (card A).** `KineticMaterial.span` is doubled: wood and brass 2, andesite, copper and iron 4, steel, aluminium
and titanium 6, and `MAX_SPAN` is 6. `ShaftSupport.neighbourChanged` re-checks that far down a line, and the shaft
tooltips (`SPANS` in `tools/gen_assets.py`) and the action bar line say so.

**Tooltips (card C).** `KineticRatings.of` says what an item's tooltip rates, in common code so tests can reach it: a
shaft or cog its Strength; a support (gearbox, clutch, gearshift, chain drive, bracket, machine), Create's crushing
wheel and a casing (`EncasingCasing.of`, the Stability it gives what it encases) its Stability; anything else nothing.
`KineticTooltips` draws one line where it drew two. `Stability` runs Wobbly (wood's 64 RPM, red), Shaky (brass's 96,
gold), Steady (andesite's 128, yellow), Stable (copper's and iron's 256, green), Very Stable (steel's and aluminium's
512, aqua), Rock-solid (titanium's 2,048, light purple). `Strength` takes the old Stability words for loads, Flimsy to
Unbreakable. With goggles on they give the RPM or su. The lang keys are `mcremix.tooltip.stability.*` and
`mcremix.tooltip.strength.*`; `mcremix.tooltip.max_speed.*` is gone. Item summaries are split
(`part_summaries` and `support_summaries` in `tools/gen_assets.py`), so a shaft's speaks of its load and reach and a
gearbox's or bracket's of its top speed, and the ingots and the brass pump say which is which.

**Ponder.** The supports scene is on a 9 by 9 plate: a copper gearbox holds four copper shafts west, two oak shafts east
(the spot past them too far) and six steel shafts north, and they drop nearest first when it goes. In the brackets
scene the shafts stay when the oak bracket grinds off, since the copper bracket still holds the line within copper's
four; in the material tiers scene the oak shafts drop only where no oak bracket is left in reach. The texts of the
supports, brackets, casings, wood fuse and tiers scenes say what sets the top speed and what carries the load.

**Tests.** 324 GameTests, plain and 330 with `-Pcompanions`. Changed: `barePartsGoOnlyWhereSomethingHoldsThem` (copper
four out, birch and brass two, steel six; Create's helper runs Create's shaft on from a gearbox to the end of andesite's
reach and no further), `tooltipsRateEveryMaterial` (each level's words in the lang file; supports, casings and the crushing
wheel rated for Stability alone, shafts and cogs for Strength alone), `encasedPartsTakeTheirCasingsTopSpeed` (in place
of `encasedPartsTakeTheSlowerOfPartAndCasing`), `onlyShaftsAndCogsGiveWayUnderLoad` (in place of
`bracketsAndCasingsGiveWayUnderLoad`: a copper bracket or casing on an iron shaft carries a load past copper's, and
copper inside an iron casing breaks), `woodenGearboxesTakeAnyLoad` and `woodenClutchesAndGearshiftsTakeAnyLoad` (in
place of the fuse tests: they turn on under a load past wood's, and a birch shaft after the gearbox gives way), and
`frictionPointsGrindAboveTheirSpeedLimit` (Create's andesite shaft in a copper bracket carries 192 RPM). The machine,
mover and vat mixer tests check that those supports take any load.

## 34. Taran's six changes (0.28.0)

Taran's ask of 2026-11-02 (13:23), on top of 0.27.0: steel rusts, but only to the first level; vats connecting to
inventories is bugged, and they should connect to funnels; titanium Pressurizers turn random colours without heat;
Pressurizers have the wrong texture in the hand; metal casting recipes should be in JEI; and Ponder scenes need a pause
button. Taran answered two cards: steel rusts wherever iron does (parts, machines, pipes, pumps and tanks, and so their
vats and boilers, wet only, never past the first stage), and a funnel goes on the vat's bottom layer, its scaffolding.
What was wrong with vats and inventories was not answered, so Claude reproduced it and chose a reading (below). Both are
in project memory (`six-fixes-cards.md`); Claude's own calls are in `six-fixes-build-choices.md`.

**Steel to its first stage.** `KineticMaterial.lastStage()` and `FluidPartMaterial.lastStage()` are EXPOSED for steel and
OXIDIZED for everything else, and `FluidPartMaterial.STEEL` now weathers, so steel pipes, pumps, electric pumps and
tanks rust as iron's do, by `Patina` (their vats and boilers with them). `Weathering.lastStage(block)` says how far any
block goes; `Weathering.randomTick`, `next`, `KineticVariant.canWeather`, `Patina.ages` and the rusting parts all stop
there, so a steel block stops taking random ticks once rusted. `Weathering.pastLastStage` and `backToLastStage` find steel
that rusted further before 0.28.0 (its weathered and rusted part blocks, waxed or not, and pipes, pumps and tanks at
WEATHERED or OXIDIZED patina): `BlockMixin` and the `Rusting*Block`s keep it randomly ticking until its next random
tick turns it back to its first stage, keeping its facing, axis and waxing, and `Brackets.weather` does the same for a
steel bracket on a shaft. Those blocks stay registered so old worlds load, but `ModCreativeTab.shown` leaves them out of
the creative tab, which is also where JEI's list comes from. `tools/gen_assets.py` (`past_last_stage`) writes no waxing
recipes for them (the eighteen `waxing/waxed_{weathered,rusted}_steel_*` files are gone) and acid recipes for the exposed
stage only, steel pipes, pumps, electric pumps and tanks included; it draws exposed steel pipes, pumps, tanks (and vat
sides), the steel electric pump and the steel boiler gauge and dial with steel's rust spots. `Leaks.leaks` only lets a
part leak at its material's last stage when that is OXIDIZED, so steel never leaks or drips.

**The titanium Pressurizer's colours.** Its vessel, `PressurizerVesselBlock.Tinted`, carries `Patina.PROPERTY` for its
temper colour but is no `MachineBody`, so `Patina.has` counted it and `Weathering` aged it as copper: it went gold,
purple and blue with no heat. `Patina.has` now leaves it out, and `PressurizerBlockEntity.matchVessel` sets the vessel
back to its Pressurizer's colour the first time the Pressurizer ticks after loading, which mends vessels in worlds from
0.19.1 to 0.27.0.

**Pressurizers in the hand.** The item models (`pressurizer.json`, `titanium_pressurizer.json`) lift the vessel over the
base, above y 16, with no face UVs, so Minecraft worked them out from where the faces lay and reached off the sprites into
other textures on the atlas. `vessel_elements` now builds the vessel where the block has it and `lifted`
raises each element with the UV it had there (`auto_uv`).

**The vat bug, and Claude's reading.** On the server items came out as they should (a chest, a barrel and a depot beside
or under the scaffolding all filled), so the bug read as the look: the outlet pipe from 0.22.1 ran to one inventory
only, was drawn from the middle of one block of the scaffolding rather than under the middle of the cone on 2- and
3-wide vats, never ran to an inventory beside a corner block, and was partly stale after a block beside the scaffolding
changed, since Minecraft only rebuilds the sections next to a change. `Vat.ports(floor, width)` now lists every place an
item can come out, under each block of the scaffolding and beside each block on its outer sides (21 on a 3-wide vat),
each with a bit of its own; the server pushes into them in that order (`pushOutputs`, as before). `VatLook` works out
which connect on the client (an inventory with an item handler on that side, a belt that carries items, a funnel facing
away and extracting) and keys the stand's model by them (`VatShellModel`), and `VatParts.standBoxes` draws a pipe 4 px
across from the outlet to each, along the vat and then out to the side, or down into an inventory under a block, with a
rim against what it meets; the pipes are cut into boxes that do not overlap (`pipes`) so no faces fight. The valve's
lever went on a side of the outlet no pipe leaves from (gone since 0.28.1, section 35). `ClientLevelMixin` (client) redraws the whole vat when a block
beside or under its scaffolding changes (`VatLook.blockChanged`).

**Funnels on the scaffolding (Taran's card).** A funnel on a vat's scaffolding finds no block entity there, so
`CapManipulationBehaviourBaseMixin` (on Create's `findNewCapability`, at every return) gives a funnel's inventory
behaviour the vat's outlet when it found nothing (`Vat.standingOn`, `Vat.outletCapability`, `OutletHandler`): what the
vat made, which can only be taken out. It listens to each outlet once and finds it again when the vat re-forms. An
extracting funnel facing out pulls the items out one at a time, as one on a basin does; nothing goes into a vat that
way. The client pipes the outlet to it. The goggles line for waiting items mentions funnels.

**Casting in JEI.** `CastingCategory` is a JEI tab with no recipe data: every `MoltenMetal` into the Sand Ingot Mold and
the Sand Block Mold (90 and 810 mB). The page, laid out as the other tabs since 0.25.0, takes the mold and the metal and
gives the casting and the sand back, draws the crucible tipped 75° over a depot with the mold on it and the stream between,
and says the mold sets in two seconds. The crucible, the depot and the belt are its catalysts.

**Ponder pause.** `PonderUIMixin` (client) adds a button 28 px left of Replay, with the forward key (W unless it was moved)
as its shortcut, since Ponder's other buttons take the other movement keys. Paused, the scene is held as Identify holds
it, by the same checks: no ticks (`tick`), and the scene, its text and anything that asks Ponder for its frame time (Create's
turning shafts) drawn at the frame it stopped at (`renderWindow`, `renderOverlay`'s second check, `getPartialTicks`),
but its text stays up. Replay and moving to another scene play again. The button shows a pause sign while the scene plays and a
play sign, lit, while it is paused (`textures/gui/ponder_pause.png`, from `gen_ponder_pause`), and its name under it when hovered
(`mcremix.ponder.ui.pause` and `.play`).

**Ponder.** The vat scene's text says the outlet is piped to inventories, and a funnel on the scaffolding (on the
schematic since 0.28.0) pulls out an aluminium hydroxide.

**Tests.** 329 GameTests, plain, and 335 with `-Pcompanions`. New: `SteelTests.steelRustsOnlyToItsFirstStage` (a steel
shaft, gearbox, large cog, pipe, pump, tank, electric pump and press each beside water stop at their first stage after
600 random ticks and stop ticking, and old rusted and weathered steel, waxed or not, comes back to it in one random
tick), `PressurizerTests.aTitaniumVesselOnlyTakesItsPressurizersColour`, `VatTests.aFunnelOnAVatsScaffoldingPullsOutWhatItMakes`
(an andesite funnel facing out pulls out the four aluminium hydroxide settled from four buckets of liquor, into a hopper,
and nothing goes in by the outlet), `VatTests.aWideVatFillsAnInventoryBesideAnyBlockOfItsScaffolding` and
`VatTests.everyPlaceAVatPutsItemsHasItsOwnBit`. Changed: `PatinaTests` (steel is no longer among the metals with no
patina) and `MachineTierTests` (a wet steel fan stops at its first stage).

## 35. Ponder starts playing, and no valve lever (0.28.1)

Taran's follow-up of 2026-11-04 (16:12), on top of 0.28.0: the pause button works, but Ponder scenes now start out
paused, and they should start out playing; the vat reading of 0.28.0 (the outlet's pipes) was the right bug; and the
tiny pipe under the vat's cone, left from the original model, should come off (Taran's screenshot of a copper vat on a
chest showed the valve lever opposite the outlet's pipe).

**Why scenes started paused.** Ponder opens from an item's tooltip by holding W, and W is also the pause button's key
(section 34). Ponder's `PonderButton.keyPressed` runs its callback whenever its key matches, and Minecraft hands a
screen the key's repeats as well as its press, so the held W went on toggling the pause after the scene opened, and
Ponder came up paused (or flickering) until W was let go. `PonderPause.Button` now takes the key only when it is
pressed afresh: built while the key is down (as it is when Ponder opens on W, and again on a resize), it is unarmed and
swallows the key's presses until `PonderUIMixin`'s `tick` hook (`tickKey`) sees the key up; after each press that
toggles, it waits for the key to come up again, so holding W pauses once and no longer flickers. The button with the
mouse is unchanged, and a Ponder opened another way (JEI, `/ponder`) takes W at once.

**The valve lever.** `VatParts.standBoxes` no longer draws the lever on the outlet, and `route` no longer notes the side
each pipe leaves from, which only the lever used (nor the `LIGHT` skin). The outlet, its flanges and the pipes to each
inventory are unchanged; with nothing to empty into, the cone is still closed at the bottom.

**Tests.** No new GameTests: both are client-only, checked in a dev client (the test report).

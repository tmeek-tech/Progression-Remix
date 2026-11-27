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
`ruinedVillages` in the server config switches it off. Since 0.38.0 (section 51) houses also fall in by their
village's age, rubble lies where it fell, the ruins grow over by biome and about half the doors, beds, chests and
workstations are gone.

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

## 36. The new dye colours (0.29.0)

Taran's colours of 2026-11-14 (design/new-colours.md in the shared folder, from the distillation design's cards): the
coal tar dyes add colours vanilla lacks, **mauve, indigo, malachite green and maroon**, and a pale shade of each mixed
with white, **lilac, periwinkle, mint and rose**, on everything vanilla and Create dye, with glazed terracotta after
Victorian encaustic tiles. The four dyes' own recipes (aniline and the rest) belong to the distillation build's stage 5;
until it adds them they have **no way to be made** and are in creative only (Taran's card, 2026-11-15). The pale shades
mix from them with white dye, two at a time, as pink does from red.

**Eight more DyeColors.** Minecraft's sixteen colours are an enum, and vanilla and Create build their tables from it, so
the eight are added to it: `DyeColorMixin`, at the end of DyeColor's static initialiser, makes eight more constants
through its constructor (`DyeColorAccessor`, an invoker on `<init>`), with ids 16 to 23, puts them into the enum's own
array of values (found by type, as its name differs between mappings, and set through `Unsafe`, as it is final), and
makes `BY_ID`, `BY_FIREWORK_COLOR` and `CODEC` again from all 24. Each constant is named as its colour is
(`MALACHITE_GREEN`, serialised `malachite_green`), since Create names some models by the constant. Their values are in
`colour/NewColour.java`: the dye colour (what fleece, leather, banners, beacons and fireworks take), a map colour, a
firework colour, a sign text colour, and the vanilla colour each is nearest.

Because everything made from DyeColor after it starts has them, most of vanilla and Create needs nothing more: beds'
textures (`Sheets.BED_TEXTURES`), item-rendered shulker boxes, fleece colours, collars (wolves and cats), leather dyeing,
firework stars, sign text, banner patterns and the loom, the beacon, and Create's `DyedBlockList`s. Create registers its
seats, sails, valve handles, toolboxes, postboxes, table cloths and nixie tubes for every colour there is, so
`create:mauve_seat` and the rest exist with no code here; their files (below) are in Create's namespace.

**The places that hard-code sixteen**, each patched:
- `SheepMixin`: a sheep keeps its colour in the low four bits of a byte beside its sheared bit (16); a new colour also
  sets bit 32. Its loot table is `mcremix:entities/sheep/<colour>`. `SheepAccessor` adds each wool to the table of what a
  sheep is sheared for (`Colours.setup`). Sheep never spawn in the new colours.
- `ShulkerBoxBlockMixin`: `getBlockByColor`, for dyeing a shulker box. Vanilla asks it for every colour while it starts,
  before mods register blocks (dispenser behaviours), so then it gives the plain box, and `Colours.setup` registers the
  dispenser behaviour for ours.
- `BlockEntityTypeMixin`: beds, banners and shulker boxes in the new colours may hold vanilla's block entities.
- `SheetsMixin` (client): shulker boxes' textures are listed by name for sixteen; the list is made again from DyeColor.
- `LlamaDecorLayerMixin` (client): a llama's carpet texture, by colour id.
- `MapBannerMixin`: a banner on a map shows as the flag of its nearest vanilla colour.
- `TropicalFishMixin`: the one fish in ten that spawns in random colours picks from vanilla's sixteen, so nothing
  living has the new colours unless dyed.
- `FireBlockAccessor`: wool and carpets burn as vanilla's do. `DyeHelper.addDye` gives Create a light and dark of each.

**Blocks and items** (`colour/ColourBlocks.java`), for each colour, from vanilla's own classes and with properties
copied from vanilla's purple ones: dye (`DyeItem`), wool, carpet, concrete, concrete powder, terracotta, glazed
terracotta, stained glass and pane, candle and candle cake, bed, banner and wall banner, shulker box. They are in this
mod's tab and in vanilla's Colored Blocks, Functional Blocks and Ingredients tabs after pink's (`Colours.fillTabs`).

**Files** (`tools/gen_colours.py`, run last by `gen_assets.py`). It reads the Minecraft client jar, Forge's universal jar
and Create's jar from Gradle's caches, so `./gradlew compileJava` must have run first. Vanilla's and Create's purple
files are the templates: every blockstate, model, loot table, recipe, tag and name either has for purple is written
again for each colour, vanilla's in our namespace and Create's in its own. Glass, panes, candles and candle cakes say
their render type in their models. Wool, carpet and bed dyeing take any colour, so vanilla's 16 recipes for each are
written again (over vanilla's, in its namespace) to take ours too. Bed and shulker textures go in vanilla's namespace,
where `Sheets` looks them up by colour name.

Textures are vanilla's and Create's own, recoloured: each colour has a source (mauve and lilac from magenta, indigo from
blue, periwinkle from light blue, malachite green from cyan, mint from lime, maroon from red, rose from pink); the
pixels that differ between the vanilla colours of a texture (white left out, as its bed's pillow differs) are the dyed
ones, and each is moved in OKLab by the new colour's offset from the source's, keeping vanilla's shading, so a bed's
frame, a candle's wick and a postbox's metal stay as they are. Drawn here instead:
- the four coal tar dyes, as a heap of crystals with a lustre (aniline dyes crystallise), and the pale shades in
  vanilla's mixed-dye shape (pink's, lime's);
- the glazed terracotta, after Victorian encaustic tiles: each is a 32x32 figure symmetric under quarter turns, and the
  tile is its top left quarter, so four tiles turned as vanilla's (facing north, east / west, south) make the figure, and
  four figures' corners meet in a smaller one. Mauve a quatrefoil, lilac fleurs-de-lis, indigo an eight-pointed star,
  periwinkle interlaced rings, malachite green a Greek key, mint a four-petalled flower, maroon nested lozenges, rose a
  roundel; the ground is the colour, the inlays cream and a dark of the colour.

**Tests.** 337 GameTests, plain, and 343 with `-Pcompanions`. New, in `ColourTests`: the eight join DyeColor and are
found by id, name, firework colour, codec and `valueOf`, and each dye is its colour's `DyeItem`; Create has its seven
dyed blocks in each and dyes a white seat mauve; pale shades mix from their dye and white, and no recipe makes the four
dyes; wool, carpets, glass, candles, banners, beds and shulker boxes are made and dyed as vanilla's (mauve wool dyes red
and mint, red wool mauve); beds, banners and shulker boxes keep their block entities; concrete powder sets and wool
burns; a sheep keeps each colour through dyeing, saving, loading and shearing, is sheared for its wool and goes back to
red cleanly; an indigo collar loads, rose leather is rose, mint sign text comes back mint and a mauve banner shows on
maps as magenta. (This says more than 0.29.0's tests checked; section 38 has what they did, and what they check now.)

## 37. Catwalks, railings and decor blocks (0.30.0)

Stage 6 of the distillation build (`design/distillation.md`, "How the build splits", Taran, 2026-11-15): catwalks,
railings, caged ladders, creosote timber and argon tubes, decorative blocks that should feel real the way Immersive
Weathering's do. Taran answered three cards: catwalks, railings and caged ladders are steel only; a Spout pours creosote
on timber (250 mB a stripped log, 100 mB a plank); and creosote timber and argon tubes are built now with no recipes,
creative only, until creosote (stage 1) and argon (stage 4) exist. They are in project memory (`decor-blocks-build.md`).

**Where it lives.** Everything is apart from the files the other stages edit, so the build threads can work at once:
`registry/ModDecor.java` (registered from `McRemix` after `ModBlocks`), the `decor` package, `ponder/DecorPonderPlugin`
and `DecorScenes` (added in `McRemixClient`), `tools/gen_decor.py` and the `DecorTests` GameTests. `gen_assets.py` calls
`gen_decor.run` last, after `gen_colours` (section 36), passing itself; `gen_decor` uses its helpers and adds its lines
to the lang file and tags `gen_assets` wrote rather than owning them. Thin faces have their UVs kept half a pixel inside
the sprite (`inset`), since a face whose UVs sit on a sprite's edge picked up the next sprite on the atlas (orange
specks from Create's seat).

**Steel catwalks.** `CatwalkBlock`: bar grating 2 px deep in the top of the block, so it lies flush with a floor beside
it, banded by a 4 px flat bar along each side that does not run on into another catwalk, or into catwalk stairs
climbing along that side. Waterloggable, and the sky shows through it, but rain does not: its collision is just big
enough for the game to count it solid, so rain stops on it and what is under it stays dry, which its tooltip says
rather than claiming water passes through. Its side flags turn and mirror with a structure, as worldgen places one
without updating its shape. 3 from 3 steel sheets and 2 steel nuggets.

**Catwalk stairs.** `CatwalkStairsBlock`: two grating treads on sloped flat-bar stringers, climbing the way you face.
Each side with no flight beside it gets a handrail (`LEFT`, `RIGHT`: posts, a sloped rail and a knee rail), so stairs
side by side make one wide flight railed only outside. To collide with, both halves of a rail stand 2.75 blocks above
the stairs' foot (`RAIL_COLLISION`), for where one stairs meets the next up: there the lower one's rail has to stand
higher than a step up (0.6 of a block) from the upper one's lower tread, which is half a block above the lower one's top
tread. When it stood two blocks up, half a block above that tread, a player walking down at a slant stepped onto it and
off the side. 2 from 3 catwalks.

**Keeping them inside the rails.** The game looks for a block's collision only for what stands less than two blocks
above that block's foot, so a rail above that line stops a step but not a player who rises past it: one who jumps on the
stairs, or runs off a tread over the stairs below, leaves where the game looks for that stairs' rail, and it would carry
them into it and out through it (in a model of vanilla's movement, with the jump already held low, 12,866 of 102,960
runs down a flight of seven, all jumping or hopping). A step up cut short by a catwalk overhead does it too: the game
then tries the move again from the raised box, which looks for nothing one block under the feet, or, for one that steps
up a whole block as a mount or a player with step-up gear does, in the block the feet come back down into. Once a box is
in a shape the game lets it go on through. So `CatwalkStairsMovement` moves anything that lives back out of a rail it
has got into, across the rail to the side its middle is on, and stops it moving on into it (`keepOutOfRails`, looking at
the stairs in the feet's block and the two under it, and ignoring overlaps under a millionth of a block, as a box set
against a rail can be in it by rounding): a player on their own client as soon as they have moved (Forge's player
tick, ending, which comes before the client sends the server where they are, so the server never sees them in a rail),
and a mob, or a mount on its rider's client, before it moves again. Out of a rail of the stairs in the feet's block it
moves them only back into the flight: one standing across the join of a wide flight when the stairs beside goes, and
the other gains a rail round them, still stands on its treads, and moving them out over the gap would drop them. If one
so jumps, it first moves them through that rail into the flight (`intoFlight`, on Forge's jump event): from the upper
tread their feet rise into the block above, where the rail is one of the stairs under them. In the model that
leaves no run down a single-width flight going out through a rail, from those 12,866 and 1,091 of 65,520 down a flight
of three, and none walking down under a catwalk; on a single-width flight a push is 0.14 of a block at most, and comes
almost only while airborne.
Down a two-wide flight 12 of 140,400 runs still leave, every one a sprinting hop from its landing or its top stairs over
the top of a rail lower down, as they did before. And a jump from catwalk stairs, or from a tread's edge with the feet
already over the stairs below, starts at 0.23 a tick at most and rises 0.44 of a block, short of a rail's top beside it
(Forge's jump event). A block's jump factor would not do, as the game takes it from the block under the jumper's middle,
which at a joint is the air over the lower stairs. A jump from anywhere else, such as a landing, is not held.

**Railings.** `RailingBlock`: a railing for each side of the block (posts, handrail, knee rail, toe board), placed on
the edge nearest where you aim (within 0.3 of a block of it, else the side you face); another used on it adds a side,
and broken it drops one for each side, as many as a schematicannon spends printing it (`SpecialBlockItemRequirement`).
A placement asks `canBeReplaced` of the railing aimed at and, if that says no, of a railing in front of the face aimed
at, which takes the side against that face. The method tells which it is asked about by the context's clicked position,
which is the block aimed at the first time and the block in front of the face the second, not by the railing's state,
as two railings side by side can be in the same one. All the sides of a railing share their rust and wax, and a
railing in hand that has rusted or been waxed would give the one it joins its own, so it adds a side only to a railing
rusted and waxed as it is; a bare one, as railings are made, adds one to any, which keeps its own rust or wax. Refused,
a railing aimed at through its open side (at the floor in front of it) takes nothing, and nothing is placed. Set by a
command it is railed on the north, as a railing with no side draws nothing, and a railing placed fresh takes only the
side aimed at. Its collision is a block and a half high. Shapes for railings and stairs are worked out once, at class
load, as the client and server threads both ask for them. 2 from 5 steel nuggets.

**Caged ladders.** `CagedLadderBlock`, a ladder: steel stiles and rungs, and a cage of hoops and bars that starts two
blocks up, above a player's head, as a real one starts above head height (`LADDERS_BELOW`, the ladders of the same
flight under it counted to two, each from the one under it, so a change at the foot runs up). A cage on the second
ladder stood in the way of a player's head at the foot, so nobody could walk in. A ladder also counts again when it is
placed (`onPlace`), as a contraption sets its blocks down without their neighbours updating them, which left a moved
flight with its cages where they were. A climber off to one side of the second ladder would catch the bottom of the
cage's side bars with their head, so there they are steered across into it (`entityInside`): one rising against the
ladder, or whose head the bars have just stopped, as when they climb by jumping (the stop has already taken their upward
speed when the block is told), with their feet in the flight's own column, so one on a ladder or vines beside it is not
pulled across, and narrow enough to fit. Each tick their speed across is set to how far they are under the bar and 0.03
of a block more (`CLEARANCE`), up to 0.15, before drag takes some of it: a strafe key held toward that bar takes back
0.02 a tick in the air, and with less clearance they hung under it; now one far under the bar can be left just short of
it by the first steer, and the next takes them in. One whose keys push toward the side they are off to more than twice
as hard as into the ladder is left alone: steering them against their keys made them bob in and out, and one strafing
into a wall beside the foot is lifted by the wall, so moving them off it dropped them. That is unless the ladder has
stopped them this tick (a key into it, and no speed into it left), as it lifts them wherever they are across it. One who
holds jump and a strafe key toward the side they are off, and no key into the ladder, hangs under the side bar until
they press forward, as the jump key cannot be read there. 3 from 2 steel sheets and 5 steel nuggets.

**Steel's rust.** All four are `FluidPartMaterial.Part`s of steel with `Patina`: they rust only while wet, to the
exposed stage and no further, and wax and scrape as steel parts do. Their properties are set in `ModDecor.steel`, not
copied from iron bars: Immersive Weathering swaps vanilla's iron bars for a block that always takes random ticks, and a
copy carried that over, so rusted or waxed steel kept ticking with nothing left to do.

**Creosote timber.** `CreosoteBlocks`: a log (debarked, by axis), planks, stairs, slab, fence, fence gate and post, dark
and oily like railway sleepers, with the log's sawn end soaked black outside and brown in the heart. They burn as wood:
`CreosoteBlocks.setup` gives fire itself vanilla's odds for them (planks' for the sawn pieces, logs' for the log and
post), so none burns waterlogged. In a hot biome (base temperature over 1.0), by day (from the level's clock: a client's
`isDay` is worked out once, when it joins), under open sky and over air, each of them now and then drips a speck of
creosote from its underside (`weep`, at a spot from `dripSpot` under the timber's occlusion shape: a fence's or gate's
outline reaches the ground under its rails, so drips started in the air below them). Tags: planks, wooden stairs, slabs
and fences, fence gates, `mineable/axe` and `mcremix:creosote_timber`; the log's item is in `logs_that_burn`, for fuel
and charcoal (not the block, or Create's saw would fell a pile of logs as a tree), and the post's item burns as a log
does. `forge:stripped_logs` gets the vanilla stripped logs, which the Spout will creosote. The crafting from creosote
logs is there (4 planks a log; 4 posts from 2 logs; stairs, slabs, fence and gate from planks).

**Posts.** `CreosoteBlocks.Post`: a 6 px pole, standing or lying along either way like a log, waterloggable, for jetty
piles, telegraph poles and fence posts. Posts join where one points at another's side (`SIDES`): the one pointed at
grows a stub of the same pole out to it, so a crossarm meets its pole and a beam sits on a pole's top. A lantern hangs
under a standing post, and under a lying one on a short length of chain (`CHAIN`, set when a hanging lantern, hanging
sign, ceiling bell, spore blossom or chain is below; `getBlockSupportShape` gives a lying post a centre hold
underneath). A torch stands on a standing post only, as on a lying one it would float, and `Post.hookOnly` keeps
brackets and PVC face pipes off the hold for the same reason. Mobs path round posts, as round fences. Rendered cutout
for the chain. Other mods' things that hang from a centre hold, such as Create's blueprints and Immersive Weathering's
icicles, hang from a lying post too, but with no chain, 5 px under it: the chain is drawn only for the vanilla blocks
listed, and keeping the rest off would take a mixin into each mod for a rare look.

**Argon tubes.** `ArgonTubeBlock`, a Power Grid device: a glass tube lying along the face it is set against, with a
nickel cap and terminal at each end (- at the low end, + at the high end). Tubes end to end join, thin collars taking
the place of their caps, but each keeps its own two terminals, since Power Grid cannot link nodes across blocks, so a
line is wired in series with short jumpers. `ArgonTubeBlockEntity` puts a 40 kOhm switch between the terminals that
closes at 800 V and opens below 600 V; lit (`LIT`), the tube glows violet with light 10, and the goggles show its volts
and, lit, its milliamps. Whether it is lit is saved. A loaded tube is left as saved until its circuit has volts across
it again and for 5 ticks after, while it settles, and does so again whenever Power Grid takes it back into its circuit
(`unpaused`) and all the while its chunk or one beside it does not tick its block entities
(`ServerChunkCache.isPositionTicking`): its first ticks come before it has rejoined, and a supply that has not ticked
since its chunk loaded is out of the circuit, so the tube reads no volts. A chunk's block entities tick only while its
tickets make it block ticking, which for a player is within their view distance and a chunk beyond, and while it is
within their simulation distance and a chunk beyond, so within the nearer of the two; and round a force-loaded chunk and
the spawn, where both hold: a tube in a force-loaded chunk, or in the spawn chunks within nine chunks of the spawn's own,
never holds, while one in the spawn's outer ring, ten chunks out, holds as it was until a player comes near. Power Grid
takes a chunk's devices out of their circuits as its tickets stop it block ticking (Forge's ticket level event), so a
supply in the tube's chunk or one beside it is in the circuit whenever the tube is not holding; one two chunks away or
more need not be, and if it stops while the tube's
chunks still tick, as when a server restarts with the tube in the spawn chunks and its supply outside them, the tube has
no volts and goes dark once it has waited. It waits 30 seconds at most, so one saved lit with no supply goes dark in the
end. So a tube held lit between 600 and 800 V stays lit across a reload, and a lit one does not blink off as its chunk
loads. Once it changes it stays lit or dark for half a second (`DWELL_TICKS`), so one on a supply too weak to hold it,
which sags under 600 V as it strikes, blinks rather than flickering every tick. The empty Glass Tube is an item.

**Recipes to come.** `gen_decor.CREOSOTE_FLUID` and `ARGON_FLUID` are `None`. When stage 1 adds creosote it sets the
first to the fluid's id and regenerates: the Spout recipes for stripped logs (250 mB) and planks (100 mB) appear, for
any not already creosote timber. When stage 4 adds argon it sets the second: the Glass Tube's crafting (a glass pane
between 2 iron nuggets, 2 tubes) and the Spout's 50 mB of argon into a tube appear.

**Ponder.** `catwalk/intro` (a ring of catwalks round a steel column six high, railings, a flight of stairs onto the
ring, and a caged ladder from the ring to the column's top, rusting when wet) on the four steel blocks;
`argon_tube/intro` (one tube struck from a voltage source, then three joined and wired in series) on the tube and the
Glass Tube. The tube is also under Power Grid's electric devices tag (`DecorPonderPlugin.registerTags`). Loading a
scene's schematic updates every block's shape from its neighbours, which joined the three tubes before the first was
shown, so the scene unjoins them first.

**Tests.** 345 GameTests, plain, and 351 with `-Pcompanions`. New, in `DecorTests`: `railingsGoOnTheEdgeYouAimAt` (a
railing aimed at through the wall's face, two side by side, a fresh one railed only where aimed, a rusted one taking a
side from a bare one while a waxed one adds nothing, aimed at the floor as a player aims, and a schematicannon's cost),
`catwalksAndStairsJoinTheirNeighbours` (a flight climbing east; a player walking down a flight of two at a slant over
the joint, through `Entity.move`, stepping neither onto the rail nor past it; a jump from the stairs, and from a tread's
edge, held to 0.23, one from the floor not; one in a rail moved back out to the side they came from, either way, and one
clear of it left; and, moved as the game moves a player and then kept out of the rails as their client does, a jump down
a flight of three, a sprint down it with jump held, and walks down it at a slant under a catwalk over its foot, stepping
up 0.6 and a whole block, each kept inside the rail and each needing it; and one standing across the join of a two-wide
flight when one side is broken, left on the other), `aCagedLaddersCageStartsTwoBlocksUp` (a player's height of room to
walk in at the foot; through `Entity.move`, a climber jumping up off to one side stopped by the cage and steered more
than 0.085, one in the middle keeping its speed across, one standing at the foot and one on a vanilla ladder beside the
flight left alone; moved as the game moves a player, one holding forward and a strafe key toward the near bar reaching
the cage, one strafing into a wall beside the foot not moved off it, on a flight facing south and one facing east, and
one strafing so while looking a little toward the ladder, which stops them, steered in and reaching the cage; a ladder
set with a wrong count putting it right), `wetSteelWalkwaysRustOnlyToTheirFirstStage` (all four, through random ticks),
`aCrossarmJoinsItsPoleAndHoldsALantern` (a pole beside it unjoined, the chain going with the lantern, a bell, no bracket
on the hold), `creosoteBurnsAsWoodDoes`, `creosoteDripsFromUnderTheWood` (a fence's drips start under its post or its
rails, a gate's under its posts, lower in a wall) and `anArgonTubeStrikesAt800VoltsAndHoldsTo600` (25 mA at 1,000 V, as
clients are told; held lit at 700 V and saved lit, dark at 500 V; dark at 700 V from the start; one loaded lit with no
supply still lit 40 ticks on, then lit by its supply at 700 V, drawing 17.5 mA, once wired up).

Five reviews went over the change, area by area, each finding checked by a second reviewer. The first found the caged
ladder's cage, the railing's schematic cost and side-face placement, creosote's fire odds the wrong way round and the
argon scene's joins. The second found a tube loaded lit going dark before its supply's chunk ticked, a tube flickering
every tick on a weak supply, railings of different rust or wax merging, a moved flight keeping its old cages, the
off-centre climb, the stairs' jumpable rail, fence and gate drips from the air and the catwalk's tooltip saying water
passes through. The third ran vanilla's movement in a model and found that a climber who jumps still caught the cage,
that one on a ladder beside a flight was pulled into it, that walking down a flight at a slant stepped over the rail
where two stairs meet (the two-block rail had always allowed it), that a bare railing could no longer join a rusted one,
that a tube went dark when its supply's chunk stopped before its own, and two tests that did not check what they said.
The fourth and fifth ran the code itself in those models: jumps and hops still carried a player through a rail where the
game does not look for it, even with the jump held low, and so did a step up cut short by a catwalk overhead, through
the rail one block lower; a climber holding a strafe key toward the near bar hung under it, one steered against their
keys bobbed, one strafing into a wall beside the foot was dropped, and one pushing into the ladder at a slant was then
left hanging; the argon tube's notes had the spawn chunks wrong; and the notes and the Build row were out of date. The
GameTests then showed, through the server's own copy of a player, which steps up a whole block, that such a step under a
catwalk misses the rail of the stairs the feet come down into. Looks were checked in a dev client (the test report).

## 38. Fixes to the new colours (0.30.1)

A review of 0.29.0 (section 36) read Forge's, vanilla's and Create's code for every place a dyed thing is handled, and a
second reviewer checked each finding. Thirteen things the new colours did differently from vanilla's purple were
confirmed; all are fixed, on top of 0.30.0 (section 37), whose files they do not touch.

**Copied properties.** Forge 1.20.1's `Properties.copy` copies neither `isValidSpawn`, `isRedstoneConductor`,
`isSuffocating` nor `isViewBlocking`, and keeps the source's map colour function. So the new stained glass let mobs
spawn on it, carried redstone (and so stopped a chest under it opening), suffocated and blocked the view, and every new
bed showed purple on maps. `ColourBlocks.glass` sets the four again, as vanilla sets them for its own glass, and a bed's
foot shows its colour and its head wool, as vanilla's. Of the other blocks copied, only shulker boxes set any of the
four in vanilla, and the default (a full, solid shape) gives the same answer while closed and open.

**Lists vanilla keeps for its sixteen**, each now given the new colours' things:
- Villagers' homes: the home point of interest lists vanilla's beds' states, made before any mod's blocks exist.
  `PoiTypesMixin` answers `PoiTypes.forState` and `hasPoi` for the head of a new bed, which is where the world asks
  (placing and breaking blocks, loading chunks). Widening the list in place does not work: a point of interest type is
  a record, so changing its states changes its hash, and Forge's registry then loses it when it saves the world.
- Water cauldrons wash the last pattern off a banner and the colour off a shulker box from a table by item;
  `Colours.setup` adds the new banners and shulker boxes to it.
- Dispensers put carpets on llamas from behaviours vanilla registers for its sixteen carpets; the new ones get the same
  behaviour (`DispenserBlockAccessor`, an invoker on `getDispenseMethod`, finds purple's).

**Generated files** (`tools/gen_colours.py`):
- Where Create's tags list vanilla's purple shulker box, the new ones are now listed too, so they are in
  `create:simple_mounted_storage` and `create:single_block_inventories` and work as storage on contraptions and
  trains. Create's recipe-unlock advancements for its dyed blocks are written for the new colours (64 files).
- Vanilla's recipe-unlock advancements for its purple recipes are written for the new colours, and one for each pale
  dye like pink's from red and white (108 files), so the recipe book offers the recipes when you pick up their
  ingredients, as it does vanilla's.
- Carpet and bed dyeing (vanilla's 16 recipes for each, written again to take the new colours, and ours) had the
  category "decorations", which crafting does not have, so they sat in the recipe book's misc tab; they are "building",
  as vanilla's.
- Item models of glass, panes and candles say their render type too: a pane's item is flat, with no block model to take
  it from, and showed opaque.
- The Minecraft, Forge and Create jars are found from the versions `gradle.properties` names, taking the newest build
  when several are cached; before, they were named in the script and the first match taken. It works on Pillow before
  12.1 (`getdata` where `get_flattened_data` is missing), and `gen_assets.py` imports it before wiping the generated
  files, so a missing jar stops the run before anything is deleted.

**Tests.** 349 GameTests plain and 355 with `-Pcompanions`, four of them new. `bedsBannersAndShulkerBoxesKeepTheirBlockEntities` passed with `BlockEntityTypeMixin` removed; it now asks
each block entity type whether it takes each new bed, banner, wall banner and shulker box. Section 36 and the 0.29.0 test
report said more was tested than was: the tests mixed only lilac and rose, saved and sheared only malachite green, and
dyed white wool mauve. Now all four pale dyes are mixed (two each), every colour goes through dyeing, saving, loading,
shearing and loading shorn, and red wool is dyed mauve. New in `ColourTests`: `theNewStainedGlassIsGlass`,
`villagersTakeTheNewBedsAsHomes` (a placed bed's head becomes a home in the world's point of interest records),
`cauldronsAndDispensersTakeTheNewColours` and `theRecipeBookHasTheNewColoursAsItHasVanillas`; and Create's mounted storage
tag is checked with its dyed blocks.

## 39. Fixes to the decor blocks (0.30.2)

A last check of 0.30.0 (section 37), each finding checked by a second reviewer, found two things the blocks did wrong and
a note that said less than the code does, and a check of the two fixes found each one short; they are fixed on top of
0.30.1 (section 38), sharing no code with it, but for one rare case below. Section 37 describes the blocks as they now
are.

**Stair rails in a broken wide flight.** One standing across the join of a two-wide flight stands on both stairs. Break
one, and the other gains a rail on that side, round them; `CatwalkStairsMovement.keepOutOfRails` then moved them out
through it, over the gap, and they fell. Out of a rail of the stairs in the feet's own block it now moves them only back
toward that stairs' middle, where its treads still hold them up. The rails of stairs one and two blocks under the feet's
block push either way, as before: one in those has come down onto them from above. But one standing so on the upper
tread who jumped had their feet in the block above at once, and that rail then counted as a lower one's and moved them
out over the gap all the same; so on a jump `intoFlight` first moves one in such a rail through it into the flight, by
under two thirds of a block, and the jump goes on from there (on the side that moves them: a real player's copy on the
server jumps again as it takes their move, which already has it). Not fixed, as a rare case for decor: when two stairs
of the broken half, one above the other, are gone, the rail the stairs one lower in the remaining half gains can still
move one standing across the join out over the gap, as it is one of the stairs under their feet (in a model, about 8% of
such starts).

**Argon tubes hold by both distances.** A loaded tube held as it was while its chunk was out of every player's view
(below entity ticking), which goes by the view distance. Block entities, and so the supplies Power Grid ticks into
their circuits, also go by the simulation distance, which can be less. So a supply in the chunk next to a tube that a
player could see, past the simulation distance, loaded without ticking and stayed out of its circuit, and the tube went
dark. Holding while a chunk round the tube is past the simulation distance alone (`Level.shouldTickBlocksAt`) was not
enough either: with a simulation distance above the view distance, a tube a chunk past the view, next to a supply's
chunk that Power Grid had paused, went dark. It now holds while its chunk or any beside it does not tick its block
entities by both its tickets and the simulation distance (`ServerChunkCache.isPositionTicking`).

**The caged ladder's steer.** Its notes and `CLEARANCE`'s left out that the steer sets the climber's speed across, which
drag then takes some of: one far under the bar can be left just short of it by the first steer, and the next takes them
in. Only the notes changed.

**Two flaky tests.** `onlyShaftsAndCogsGiveWayUnderLoad` (section 33) failed in 4 of about 20 runs: the copper bracket it
checks weathers as it turns, at a random tick's odds (section 19), and the test wanted it bare. It now takes copper at
any stage. `bracketsStayOnThroughWaxAxeAndWeather` then failed once with companions the same way, its copper bracket
weathered in the three ticks before it is waxed, and takes copper at any stage too. Any test that wants an exact copper
block for more than a few ticks can fail so.

**Tests.** 349 GameTests plain and 355 with `-Pcompanions`, as 0.30.1. `catwalksAndStairsJoinTheirNeighbours` now ends
by standing a player across the join of a two-wide flight, breaking the stairs on one side and checking the other has
railed round them and leaves them where they are, and then by standing them so on its upper tread and jumping, moved as
the game moves a player, and checking they land on it again. The argon change is reasoned from how chunk tickets spread
and from Power Grid's code, not tested: every GameTest chunk is force-loaded, so it and the chunks beside it always tick.

## 40. The glass lab and the first chemistry (0.31.0)

Stage 1 of Taran's distillation design (design/distillation.md in the shared folder, 2026-11-10 to 14; the numbers it
left to the build are in design/glass-lab-build-numbers.md): the glass lab still and bench, wash, wood spirit and coal
tar split into their cuts, and Bakelite's new path from phenol and formaldehyde, all playable a bucket at a time.
Taran's cards of 2026-11-15: just the glassware, no bench block; the thermometer gives a pulse when a cut ends; CO2
sinks and smothers; rutile briquettes are bound with pitch, as electrodes are; about 5 Bakelite to a bucket of coal tar;
the same ethanol per sugar as before. The steel column and crude oil are later stages; creosote timber came with stage 6
(0.30.0) and now takes the still's creosote.

**Lab glass** (`lab/LabGlass`, `LabGlassBlock`). Eight blocks, each glass on an iron retort stand: round flask, glass
column, still head, coil condenser, cow receiver, hot tube, settling flask and separating funnel. Each takes its heat
from what is under it, as a basin does (`LabGlass.heatUnder`), but nothing past Hot: over Searing or hotter it cracks. A
hot piece (over Warm or hotter, or a column or head over a boiling flask) cracks when rain falls on it or water stands
beside it, so a lab needs a roof. Rain falls as vanilla's does (`Level.isRainingAt`), on the top of the stack of
glassware a piece is in: under open sky, through nothing that stops it, so a roof of glass, panes or iron bars keeps it
off as well as stone. The round flask, the bench pieces, the condenser and the cow receiver check themselves every
`CHECK_EVERY` (10) ticks; a glass column or still head stands on its flask, which checks them for water while it boils,
so set straight on heat neither works nor cracks. A cracked piece (`cracked` block state, its own cracked texture)
hisses, and `SHATTERS_AFTER` (100 ticks) later breaks, dropping nothing, and what it held spills as a broken tank's does
(gases escape): a bucket's worth or more makes a source block, so the receiver's flasks and the condenser's water spill
too. A cracked piece set down somewhere else, as a piston or contraption does, has its shatter scheduled again
(`LabGlassBlock.onPlace`), and the still head's pulse is timed by its block entity, so the block's one scheduled tick is
only ever its shatter. Every piece is solid to fluids (`forceSolidOn`), so running water or lava flows round it rather
than washing it away. Cool glass is safe, and glass pipes never crack. Each piece's crafting recipe takes an iron nugget
for its stand; its block model has `minecraft:block/block` as parent, for the display every block item has in hand, in
frames and in slots, and its collision box takes in its stand's rod, turned with the piece.

**The still** (`RoundFlaskBlockEntity`, `StillHeadBlockEntity`, `GlassColumnBlock`, `CoilCondenserBlockEntity`,
`CowReceiverBlockEntity`). A round flask over heat, up to three glass column pieces on it, a still head with its
thermometer on top, a coil condenser on the head's side arm sloping down, and a cow receiver under the condenser's low
end. A bucket of a mixture with a `mcremix:distilling` recipe (`DistillingRecipe`), over at least its heat, comes over a
cut at a time, lightest first, into the receiver's flask under the drip; the next cut waits until that flask will take
it (empty, or holding that cut with room), so two cuts never mix. A batch starts on a full bucket (`batch()`, the
recipe's mixture amount), or on what there is once nothing has come in for `SETTLE` (20) ticks, so a pipe filling the
flask a little at a time gives whole batches; the goggles say Filling meanwhile. A flask just placed or loaded waits
`SETTLE` ticks too (`initialize`), for its pipes to start again. A smaller batch's cuts are whole mB, and what each
falls short is carried to the next batch of that mixture (`owed`, saved with the flask), so lots of any size give a
bucket's cuts in the end. A mixture poured in after something else, so in the flask's second tank, moves to the first
once that is empty, and distils. As each cut ends the thermometer's reading climbs, the still turns the receiver to its
next flask itself, and then the head gives a redstone pulse (`StillHeadBlock.POWERED`, 4 ticks, timed by
`StillHeadBlockEntity`). The receiver takes no notice of a pulse within `STILL_TURN_HOLDS` (20) ticks of the still's own
turn, so a thermometer wired to it by redstone still turns it once a cut. What is left stays in the round flask. The
batch takes the recipe's time (600 ticks) at its own heat, half for each level hotter, half again with cold water in the
condenser and a third with coolant. Cuts marked `close` (acetone and methanol) come over at a quarter of the pace in a
bare still, half with one column piece and full pace with two; a third fits but adds nothing, as the design's numbers
say. The condenser warms a mB of water (or cold water) into warm water, or of coolant into hot coolant, for every 4 mB
of vapour; dry, or with nowhere for that to go, the still waits. The goggles show the thermometer only while something
boils, and an incomplete still names what it lacks: a head, a condenser, a receiver, or fewer column pieces when four
are stacked, which the head's goggles say too. The receiver turns by hand (empty hand) or a redstone pulse; a bucket on
a side takes from the flask on that side, sneaking with an empty hand tips that flask out, and pipes on any side draw
from every flask and the vent, so smart pipes can sort the cuts. Pipes never draw a flask's input back out, only what it
made. A bucket (`LabGlass.useContainer`) takes a whole bucket from the first tank that has one, what it made first, then
what went in while nothing is distilling or cooking it; a bucket of water fills the coil condenser, and an empty one
takes its warm water, then its water; sneaking with an empty hand (`tipOut`) pours out everything a bucket could take,
part-buckets and all, and marks the piece changed, so it is saved and every client sees it. So a still runs by hand
alone. A distilling recipe without exactly one fluid ingredient with an amount, without a result, or whose cuts come to
more than that amount is turned down as it loads (`DistillingRecipe.valid`, a `JsonSyntaxException` the recipe manager
logs), so a broken data pack never reaches a still, and a cut never pours more than the flask gave up.

| A bucket of | At | Cuts, lightest first (°C) | Left in the flask |
|---|---|---|---|
| Wash | Warm | 250 ethanol (78) | 750 warm water |
| Wood spirit | Warm | 100 acetone (56), 300 methanol (65) | 600 vinegar |
| Coal tar | Hot | 150 light oil (120), 250 carbolic oil (200), 250 creosote (250) | 350 pitch |

**The reflux flask.** A round flask with a coil condenser standing on it (the condenser's `upright` shape) cooks what
Create mixes in a heated basin from fluids alone (one or two in, one or two out, no items), a bucket at a time over Hot
heat, 100 ticks a batch, losing nothing: ethylene glycol, from ethylene dichloride and caustic soda, is one. Each result
goes into a tank already holding it with room for it, or into an empty one; with no room for a batch's results the flask
waits and its goggles say Full, rather than cooking and pouring them away. Taking the condenser off ends the batch it
was cooking, so a bucket can take what went in back out.

**Basin reflux** (`BasinReflux`, `BasinRecipeMixin` on `BasinRecipe.apply`). A heated basin recipe that takes ethanol,
methanol or ethylene dichloride loses a quarter as much again of it as vapour (at least 1 mB), in a puff of vapour, so
the same result costs more, unless a coil condenser is fitted to the basin's side (its `basin` shape) with water through
it, which condenses the vapour back for the condenser's usual warm water.

**The bench** (`BenchGlassBlockEntity`, `BenchGlassBlock`). Glassware that does one recipe a batch at a time, filled
and emptied by bucket or pipe (pipes draw only what it made; a bucket takes a whole bucket of what it made first, then
of what went in), and tipped out by sneaking with an empty hand:
- **Hot tube** (`HotTubeBlockEntity`), lying along its axis over a fire with a catalyst inside: it runs the Steel
  Furnace's catalyst jobs that need Hot or less, at the Steel Furnace's pace, and the catalyst wears by the fluid run
  over it. Methanol over **copper mesh** (four copper nuggets, by hand only: its recipe is
  `crafting/copper_mesh_manual_only`, so Create's presses never pack loose nuggets into it; lasts 16,000 mB) gives
  formaldehyde, 250 to 250, at Hot;
  ethanol over alumina gives ethylene, as in the Steel Furnace. The catalyst goes in by hand or funnel and comes out with
  an empty hand.
- **Settling flask** (`SettlingFlaskBlockEntity`): settling recipes with one fluid in and no items, cool (it stops while
  warm). Carbolic oil gives 2 naphthalene per 250 mB and leaves strained carbolic oil. What settles is taken by hand or
  by funnel or chute.
- **Separating funnel** (`SeparatingFunnelBlockEntity`): vat mixing recipes of liquids alone (one or two in, one or
  two out, no gases or items), 100 ticks, no heat. The layers sort by density: the heavier leaves by the tap at its
  foot (a pipe under it), the lighter from its neck (a pipe on its top or sides), and a bucket takes the heavier first.
  Strained carbolic oil and caustic soda, 250 each, give 250 sodium phenolate under 250 creosote.

**Phenol, lime and soda.** Sodium phenolate and CO2, 250 each, stirred in a vat give 250 phenol and a soda ash. CO2
(`mcremix:carbon_dioxide`, a gas) comes from fermenting (50 mB a batch, kept in a vat, lost from a basin) and from
lime: limestone or chalk fired at Searing gives quicklime and 50 mB of CO2 into the Furnace's tank, drawn off by pipe (a
gas has no bucket). What has no room goes up its chimney, and CO2 waiting there gives way to anything else the Furnace
gives off (`KilnBlockEntity.giveOff`): it goes up the chimney and the tar or spirit is kept. Quicklime washed by a fan
through water is slaked lime; soda ash, slaked lime and
250 water mixed in a basin give 250 caustic soda and a chalk. Soda ash also comes from kelp: fired at Warm it is kelp
ash, and washed, half of it gives soda ash.

**Bakelite** is pressed from 50 phenol and 50 formaldehyde over a heated basin, about 5 to a bucket of coal tar
(against 10 before). Coal tar itself no longer makes Bakelite, nor binds Carbon Electrodes or Rutile Briquettes: pitch
does (100 and 50 mB).

**Fermenting** now gives **wash**: 2 sugar and 1,000 water ferment into 1,000 wash and 50 CO2 (the old
`fermenting/ethanol` recipe is gone), and the still turns the wash into 250 ethanol, as much as before.

**CO2 let loose** (`CarbonDioxideCloud`, a `GasCloud`): an all but invisible haze, heavier than air, that sinks, pools in
low places and lingers, a bucket's worth for a minute (`TICKS_PER_BUCKET`). Anything inside that breathes (not the
undead or golems) runs out of air as under water, then smothers, 2 damage a second, with its own damage type
(`mcremix:carbon_dioxide`, bypasses armour) and death message.

**Vinegar** (`VinegarBlock`), poured in the world, takes the patina off every unwaxed copper block it runs onto or
against, and off a copper bracket on a shaft or cog, as hydrochloric acid takes off rust; it doesn't touch rust or
titanium's colours, and soaks away after two seconds. A Spout's 100 mB cleans a weathered copper part, Create's copper
pipes, pumps and tanks, and vanilla's and Create's copper blocks (`filling/vinegar`).

**Steel Furnace fuels.** Light oil (2 heat a mB), acetone (1) and creosote (2), each up to Searing.

**Creosote timber** (section 37) is no longer creative only: `CREOSOTE_FLUID` in `tools/gen_decor.py` is now
`mcremix:creosote`, so a Spout soaks a stripped log (250 mB) or planks (100 mB) into creosote timber
(`filling/creosote_log`, `filling/creosote_planks`). Argon tubes wait for argon (stage 4).

**Fluids and items.** New chemicals, each with a JEI entry, a bucket (all but carbon dioxide, a gas) and a structural
formula (all but pitch): wash, carbon dioxide, acetone, methanol, vinegar, formaldehyde, light oil, carbolic oil,
strained carbolic oil, creosote, pitch, sodium phenolate and phenol. The carbon dioxide cloud has its own name.
Solutions show their water ("CH3OH in H2O" for wood spirit), and oils that are mixtures show
what matters in them (phenol and naphthalene in carbolic oil). New items: copper mesh, naphthalene, kelp ash, soda ash,
quicklime, slaked lime and chalk.

**JEI and Ponder.** A Distilling tab (`DistillingCategory`) shows each mixture's cuts with their boiling points, the
close pair and the residue, over its heat. The hot tube joins the Steel Furnace tab, the settling flask the Settling tab
and the separating funnel the Vat Mixing tab. Ponder scenes `lab/still` (the still, cut by cut) and `lab/bench` (the
hot tube, reflux flask, settling flask and separating funnel). The Bakelite scene (`kiln/bakelite`) now presses phenol
and formaldehyde and shows on their buckets, and the Steel Furnace's lists the new fuels.

**Tests.** 30 GameTests in `GlassLabTests`, 16 of them added for what two reviews of the stage found, and one new in
`CoalTarTests`. The still: wood spirit distils into its cuts, the still turning the receiver between them and warming
the condenser's water; a pipe-fed still waits for a whole batch, and splits a half batch in the same proportions; two
25 mB lots give what 50 mB at once would, what the first lot's cuts fell short by carried to the second; a flask loaded
holding part of a batch waits for its pipes; a thermometer wired to the receiver doesn't turn it twice a cut; a dry
condenser waits; a still that loses its condenser, or has four column pieces, says what it lacks, on its head too; a
mixture poured in after water still distils; buckets take whole buckets, the receiver's from the flask on the side
clicked and the condenser's too, and sneaking with an empty hand tips glass out and marks it to be saved; wash stills
into a quarter ethanol; a distilling recipe without its one mixture, or cutting more than it holds, is turned down as it
loads; a cow receiver turns by hand or pulse. Lab glass: it cracks past Hot and when wet while hot, leaving a source of
what it held; the condenser and receiver crack too; a cracked piece set down as a piston sets one down still shatters,
and a cracked still head's pulse doesn't put that off; rain cracks hot glass unless it is roofed, with glass as well as
stone (in a batch of its own, `labGlassRain`, which turns the rain on and off); running water doesn't wash lab glass
away; cool glass takes water. The bench: the hot tube makes formaldehyde; carbolic oil settles naphthalene; the funnel
splits the phenol wash; a reflux flask cooks what a heated basin mixes, keeping the dichloride the batch didn't take,
puts each result in a tank that takes it all, and waits when none does; a heated basin loses what boils off unless
condensed. CO2 sinks into a pit and hurts a pig there as drowning does; vinegar takes the patina off copper; Spouts of
vinegar and of the still's creosote do their work on depots; every piece has its recipe, copper mesh by hand only; the
still's creosote soaks timber. `carbonDioxideWaitingGivesWayToTar` (`CoalTarTests`): CO2 a Furnace holds gives way to a
coal's tar, none of it burnt off.
Changed: the vat and basin fermenting tests (wash; a two-tank vat holds 4,000 mB of what it makes, so four buckets and
ten sugar ferment as four batches at once and leave two sugar, which a funnel can't take out), Bakelite from phenol and
formaldehyde, and electrodes and briquettes bound with pitch, each now checking the amounts too, since a fluid
ingredient's test ignores them.

## 41. The steel column (0.32.0)

Stage 2 of Taran's distillation design (design/distillation.md in the shared folder; the numbers it left to the build
are in design/steel-column-build-numbers.md): a steel fractionating column that splits stage 1's wash, wood spirit and
coal tar nonstop, every cut at once, with a steel condenser at its foot that also works inline on any pipe. Six cards
went to Taran on 2026-11-15, and on 2026-11-16 Taran chose every one as it was built: each heater under a column adds to
its pace (more heaters only add pace, none is required under every block), any pipe on a section draws that section's
cut, a close pair needs two more sections or reflux, one condenser serves a column and colder coolant makes it faster,
warm cuts cool only in a condenser, and what escapes is a flammable haze.
Crude oil (stage 3) and the liquid air column (stage 4) come later.

**The column** (`column/ColumnSectionBlock`, `ColumnSectionBlockEntity`, `Column`). Steel Column Sections join as
Create's fluid tanks do (`ConnectivityHandler`): one to three across, a square, and up to 16 high, each block layer a
section. The controller, the bottom north-west block, keeps the column, and a pipe on any block's side works with the
section that block is in; a flanged nozzle grows on each side a pipe joins (`NOZZLES`, the four side properties). When
the controller passes to another block, as when a column grows to the north or west or its controller is broken, what
it held goes with it (`absorb`, `bequeath`), each section's cut moving up or down with the column's new bottom. Create
tells a controller its column changed part way through forming it, before the new size is set, so `Column.resize` only
ever grows and sets capacities, and the next tick trims what no longer fits (`settle`). A bucket works on the section
it is used on: feed into the bottom, reflux into the top, or out with the cut a section holds. Sections and the
condenser are steel: Searing-rated (`fluid_ceiling/searing`), mined with a pickaxe, rusting where wet to the first
stage only, waxed with honeycomb. Four steel plates round a steel fluid tank make two sections; a steel fluid tank, two
steel pipes and four copper plates make a condenser.

**Running it.** Feed is piped into the bottom section (2,000 mB for each block of its floor): any mixture with a
`mcremix:distilling` recipe, one at a time, and the goggles say when a fluid won't split. Each block under the bottom
section at the mixture's least heat or hotter, read as a basin reads it, is a heater, adding 5 mB of feed a tick at
that heat (`PACE_PER_HEATER`, a bucket every ten seconds) and twice as much for each level hotter, as Create's boilers
count heaters. Every cut comes out at once, nonstop, each in its own section (`ColumnLayout`): the residue in the bottom
section and each cut above it in boiling order, the lightest in the top section, from which it goes over the vapour
line to the condenser and out of its ends, cold. A pipe on a section's side draws the cut that condenses there. A
section holds 1,000 mB of its cut for each block of its floor; a cut nobody draws fills its section and then holds the
whole column up until it is drawn, as a full condenser does. Without a condenser the column does not start, and too
cold it says what heat it needs. What each cut's share of the feed comes to past whole mB is carried, as the still
carries it (`owed`), so a column gives a bucket's cuts in the end; a new mixture, or the same recipe with other results
after a data pack reload or from an old save, starts that afresh.

**Sections.** Every cut needs a section of its own, the residue too: the wash needs two, coal tar four. A column too
short lets the cuts it has no room for escape over the top, lightest first. Acetone and methanol boil close together
(`close`), so between them they need two more sections that draw nothing (`EXTRA_FOR_CLOSE`), five in all for wood
spirit; a pair still short lets part of its lighter cut escape, half when one section short and two thirds when two
short, and the rest still comes over, so there is reflux to start with. Sections beyond what a mixture needs sit just
under the top and draw nothing, and a separating section reads the temperature between its neighbours. The goggles say
how many sections a column is short, and whether reflux could stand in for them.

**Reflux.** The top cut piped back into the top section's side is reflux; the top section takes no other fluid back,
up to 1,000 mB for each block of its floor. The column keeps its ratio over the last 200 ticks (`REFLUX_WINDOW`), what
came back for every mB of fresh top cut condensed. A ratio of 1 stands in for both of a close pair's sections and 1/2
for one (`ColumnLayout.credit`), so wood spirit splits clean in three sections at a ratio of 1. Reflux costs pace: at a
ratio r the column boils feed at 1/(1 + r) of its pace, half at 1, and more than 1 buys nothing. Reflux boils again on
the pace the feed leaves, only as fast as the condenser can still take it after the fresh top cut, and goes over to
the condenser again. Two pumps drawing from the condenser, one into the top section and one away, set the ratio by
their speeds.

**Cuts leave as hot as their section** (`fluids/WarmCuts`). Above 77 °C, Warm on the mod's fluid scale, a cut drawn from
a section or the bottom leaves as a warm form of itself, which no PVC pipe carries: hot carbolic oil (200 °C), hot
creosote (250 °C) and hot vinegar (100 °C). Pitch (360 °C) is always kept warm and has no plain form; methanol (65 °C)
and the wash's warm water are cool enough as they are, and the top cut comes through the condenser cold. Hot creosote
soaks timber in a Spout and burns in the Steel Furnace as creosote does, and hot vinegar cleans patina as vinegar does
(the Spout recipes now take the fluid tags `mcremix:creosote` and `mcremix:vinegar`, which hold both forms); carbolic
oil has to be cooled before it settles out its naphthalene. Poured out into the world, a warm form turns straight into
its plain one. Each has a bucket, a JEI entry and its plain form's structural formula.

**The steel condenser** (`SteelCondenserBlock`, `SteelCondenserBlockEntity`, `client/SteelCondenserRenderer`). A
riveted steel shell of copper tubes lying on its side, along the face it is placed against: its two ends are the process
side, and its top, bottom and other two sides the water jacket, which takes water, cold water or coolant and gives back
warm water or hot coolant, a mB for every 4 mB it condenses or cools, as the coil condenser does. It condenses or cools
10 mB a tick with water, 20 with cold water and 30 with coolant (`RATE`, scaled by the coolant's conductance,
`rateWith`), and holds 1,000 mB of what it made, which pipes on its ends draw. Beside a steel column's bottom section it
is that column's condenser, and its renderer draws the vapour line from it up the column's side and over into its top;
vapour past what it can take, or with nothing to cool it, escapes there. Anywhere else it is inline: a warm cut piped
into either end comes out of either end as its plain form, so a cut drawn hot can go on in PVC. It takes nothing else,
pitch least of all, which would set solid in its tubes, and while it serves a column it takes nothing piped in.

**Solvent vapour** (`fluids/SolventVapourCloud`, a `GasCloud`). What escapes over the top of a short column, or past a
condenser, hangs low as a faint haze, heavier than air, sinking, about 10 seconds a bucket (`TICKS_PER_BUCKET`). It does
nothing to what breathes it. If it reaches a flame (fire, a torch, a lantern, a lit campfire, furnace, smoker or
candle), lava, or a blaze burner or anything else Warm or hotter, it flashes into flame and is gone, setting alight
everything standing in it for 5 seconds and lighting fires on what burns under it. A lit redstone torch, lamp or ore,
or an argon tube's cold glow, is no flame. The column lets what escaped loose once a second (`VENT_EVERY`), and its
goggles say how much; so does the condenser's.

**Goggles.** A column's goggles give its size, what it is doing (idle, won't split, needs a condenser, too cold, held
up and by which section, or splitting), its heaters and pace, its reflux ratio, how many sections short it is and what
escapes, then each section from the top: its cut and temperature (the top's going over to the condenser, separating
sections between), what it holds, the feed and the reflux. A condenser's say which job it has, its rate with what cools
it, whether it is dry or its warm water has nowhere to go, whether it is full, and what escapes.

**JEI and Ponder.** The Distilling tab now gives the sections each mixture needs in a steel column, and how many with a
reflux ratio of 1 at half the pace, and on a close pair's cuts the two more sections they need between them
(`ColumnLayout.paired`). A new Cooling Hot Cuts tab (`CoolingCategory`) shows what the inline condenser cools, at its
rates with water, cold water and coolant. Ponder scenes `steel_column/intro` (a column on a blaze burner splitting coal
tar: heaters, the condenser, feed, cuts, warm cuts, a held-up column and venting), `steel_column/reflux` (wood spirit
at a ratio of 1) and `steel_condenser/inline`.

**Models and textures** (`tools/gen_column.py`, called from `gen_assets.py`): riveted steel sections with flange rings,
a nozzle model, the condenser's shell and heads, the vapour line and its elbow as partial models, each with an exposed
(rusted) form, and the three warm fluids. The nozzle is a bolted flange 14 pixels across and 2 deep: the rim Create draws
where its pipe joins a block is 10 across and 2 deep, and hid the first, smaller nozzle entirely.

**Review.** A review of the stage found ten things, all fixed but one: a too-short column's goggles counted too few
sections missing when a close pair's lighter cut had no section either; reflux boiled faster than the condenser could
take it, so much of it was vented; a data pack reload that changed a recipe's results could crash a column (`owed`
sized for the old ones); an argon tube set off vapour; JEI gave a close pair's extra sections on each of its cuts, and
on a cut flagged close alone; a column sent itself to clients every second, changed or not; the top section worked out
a whole layout for each mB of reflux it was offered; the cooling tab worked out the rate its own way; and dead state was
saved. Left: the condenser's jacket accounting repeats the glass coil condenser's rather than sharing it. A test also
found a column losing what it held when Create reported it changed part way through forming (fixed as above).

**Tests.** 16 GameTests in `ColumnTests`. A column one across and two high on a campfire splits wash into 250 ethanol
out of the condenser's end and 750 warm water from the bottom, warming 62 mB of the condenser's water; four high on a
fire it splits coal tar into light oil over the top, hot carbolic oil, hot creosote and pitch; three high, wood spirit
loses two thirds of its acetone over the top as a vapour, the goggles saying two sections short or reflux, and with
the top cut sent back at a ratio of 1 nothing escapes and it boils at half its pace. A cut nobody draws, or a full
condenser, holds a column up, and drawn off it runs again; a column waits for its condenser and finds one placed
beside it, and a condenser beside an idle column serves it; a column two across counts four heaters at a pace of 6
for wash over two campfires and two fires, and only the two fires for coal tar. Vapour a dry condenser can't take
escapes beside it; a condenser on its own cools hot cuts for PVC at 10, 20 and 30 mB a tick and refuses pitch, cold
creosote and water, and beside a column takes nothing in; solvent vapour flashes at a flame, setting an armour stand
alight and the planks under it, and hangs harmless with no flame; reflux re-boils only what the condenser can take; a
column saved with other results starts afresh; what a column holds stays as it grows, loses its bottom or gains a new
one; close cuts need two more sections or reflux; parts are steel and grow nozzles where pipes join; and every part
is crafted, and the hot cuts work as their plain forms.

## 42. Crude oil (0.33.0)

Stage 3 of Taran's distillation design (design/distillation.md in the shared folder; the numbers it left to the build
are in design/crude-oil-build-numbers.md): crude oil found only by digging, drawn slowly by a hose pulley that never
runs it dry, split into six cuts in the glass still and the steel column, and what those cuts become: ethylene and fuel
gas from naphtha cracked with steam, paraffin wax and fuel oil from heavy oil, petroleum coke from bitumen, three new
Steel Furnace fuels, and a diesel engine. Six cards went to Taran on 2026-11-16, and Taran chose every one as it was
built: medium pockets, one in about every 6 by 6 chunks; a hose pulley drawing a bucket every 10 seconds; an engine
worth a whole steam engine; an engine that stalls dry or overloaded and must be cranked again; sour gas that poisons
and flashes at a flame; and bitumen boiled down to coke in a basin over Searing heat. Cleaning sour gas is stage 5;
the liquid air column (stage 4) comes later.

**Oil pockets** (`oil/OilPocketFeature`, `registry/ModFeatures`, worldgen data). A rough blob 10 to 16 blocks across
and 6 to 9 tall, three ellipsoids run together, filled with crude oil where it was rock (stone, deepslate, tuff, and
ores in them). It is sealed: a block of oil goes in only where everything beside it is solid rock or more oil, so
where the blob would open onto a cave, water or anything else, the stone is left as a skin round it and there is no
sign of it until you dig in. Placed in the `underground_decoration` step under every Overworld biome (a Forge biome
modifier), one chunk in 36 (`rarity_filter`), between y -40 and -16.

**Crude oil in the world** (`fluids/CrudeOilBlock`). It creeps out as slowly as lava does outside the Nether. Standing
or wading in it slows you and mobs as a cobweb does (`makeStuckInBlock`), and it can't be swum in. It burns: it counts
as a burning block to fire, so fire beside it eats into it a block at a time and a lit spill burns itself out; lava
touching it sets it alight (`ModFluids.registerInteractions`), and so does anything on fire wading into it. It never
puts out what is alight in it.

**The hose pulley** (`fluids/OilWells`, `mixin/FluidDrainingBehaviourMixin`, `mixin/HosePulleyFluidHandlerMixin`). To
a hose pulley a pocket is endless however small it is, as a sea is, so drawing never takes a block away and several
pulleys can share one. Create decides a pool is endless in two places, the search and the check it makes again every
8 seconds (`continueSearch` and `continueValidation`, each testing the pool's size and `canDrainInfinitely`), and the
mixin answers yes for crude oil in both. The oil comes up 5 mB a tick however fast the pump turns
(`DRAW_PER_TICK`), a bucket every 10 seconds, one heater's worth for a steel column; what it could draw saves up while
nothing pulls on it, up to a second's worth (`SAVED_UP_TO`, 100 mB). Every other fluid comes as fast as before.

**The six cuts** (`fluids/Chemical`, `recipes/distilling/crude_oil.json`). A bucket of crude oil, Hot as coal tar is,
gives 50 sour gas (boiling at 20 °C), 250 naphtha (100), 150 kerosene (210), 200 diesel (300), 200 heavy oil (375) and
150 bitumen, the residue (400). In the glass still it takes 30 seconds; the sour gas leaves by the cow receiver's vent
and the bitumen stays in the flask. A steel column needs five sections for it, four cuts and the residue: sour gas
never condenses, so it takes no section and goes over the top with the naphtha (`ColumnLayout.gases`); there is no
close pair, so no reflux. Drawn from a section, kerosene, diesel and heavy oil leave hot (`WarmCuts`): hot kerosene
(210 °C), hot diesel (300 °C) and hot heavy oil (375 °C), which no PVC pipe carries until a steel condenser inline cools
them. Hot kerosene and hot diesel run the engine as they are (the fluid tags `mcremix:kerosene` and `mcremix:diesel`
hold both forms); hot heavy oil must be cooled before its wax settles out. Bitumen is always warm, as pitch is, and
goes into neither PVC nor a condenser. Crude oil, heavy oil, fuel oil and bitumen are mixtures and have no structural
formula; the rest do (sour gas as hydrogen sulfide, fuel gas as methane, naphtha as octane, kerosene as dodecane,
diesel as cetane), with sulfur drawn yellow.

**The condenser's gas outlet** (`column/SteelCondenserBlockEntity`). Beside a column, the condenser holds up to
1,000 mB of gas as well as what it condensed, and pipes on its ends draw both: a pipe that names no fluid gets the
condensed cut first and the gas once that is drawn off, and smart pipes can sort them. Gas with nowhere to go escapes
beside the condenser as a cloud rather than holding the column up, as the still's vent lets it go, and the goggles say
how much gas is escaping, apart from any vapour.

**Naphtha cracking** (`kiln/SteelFurnaceBlockEntity`, `SteelFurnaceRecipe`, `recipes/steel_furnace/naphtha_cracking.json`).
The Steel Furnace gains a second feed tank, so any of its recipes can now take a second fluid (`coFeed`). 250 naphtha
and 100 pressurised steam at Searing give 150 ethylene and 100 fuel gas in 5 seconds. Without its second fluid the
furnace waits and its goggles say for what. A second fluid that no longer fits the recipe its feed is for (steam left
in after the feed changed) can be pumped back out, so it never blocks the furnace.

**Paraffin wax and fuel oil** (`item/ParaffinWaxItem`, `recipes/settling/paraffin_wax.json`). 250 heavy oil left to
stand cool (a basin, vat or settling flask) drops 1 paraffin wax in 10 seconds and leaves 225 fuel oil. Paraffin wax
does what honeycomb does: used on a block it waxes whatever honeycomb waxes (vanilla's copper, Create's copper parts,
the mod's copper, iron and steel parts), it takes honeycomb's place in every waxing recipe (they now take the item tag
`mcremix:waxes`, which holds both, and vanilla's waxed copper recipes get paraffin wax twins), and string on paraffin
wax makes a candle.

**Petroleum coke** (`recipes/boiling/petroleum_coke.json`). 250 bitumen boiled down in a basin over Searing heat gives
2 petroleum coke in 10 seconds, as a coker drum works. It works wherever Coal Coke does: the item tag `mcremix:coke`
holds both, and steel alloying, cementation packs, Rutile Briquettes and Carbon Electrodes take the tag; it burns as
long as Coal Coke and is Searing furnace fuel.

**Steel Furnace fuels** (`kiln/SteelFurnaceFuels`). Fuel gas burns 2 heat a mB up to Blazing, as ethylene does; fuel oil
3 up to Searing, the thriftiest liquid fuel; sour gas 1 up to Searing, and it burns dirty, puffing out 1 mB of sulfur
dioxide from the chimney for every 2 mB burnt (`SULFUR_PER_SOUR_GAS`). Diesel and kerosene burn only in the engine, and
naphtha, heavy oil and bitumen aren't fuels.

**Gases let loose** (`fluids/FlammableGasCloud`, `SourGasCloud`, `FuelGasCloud`, `SulfurDioxideCloud`). Solvent vapour,
sour gas and fuel gas now share a base: at a flame, lava or anything Warm or hotter they flash into flame, setting alight
what stands in them and lighting fires on what burns under them. Sour gas is a faint yellowish haze as heavy as air,
about 20 seconds a bucket, whose hydrogen sulfide poisons what breathes it, 1 damage a second and nausea, with its own
death message. Fuel gas is lighter than air and hangs where it was let go, about 10 seconds a bucket, harmless to
breathe. Sulfur dioxide is a grey haze heavier than air, about 30 seconds a bucket, that never burns: plants under it
die as they do under chlorine (grass, mycelium, podzol and moss to dirt; leaves, flowers, crops, saplings, vines, cacti
and cane gone), and what breathes it chokes, 1 damage every 2 seconds and slowness. Both poisons go through armour
(`damage_type/sour_gas`, `sulfur_dioxide`).

**The diesel engine** (`oil/DieselEngineBlock`, `DieselEngineBlockEntity`, `DieselFuels`, `client/DieselEngineRenderer`).
An old stationary engine in one block: a horizontal cylinder out of its front, the crankcase at the back, an exhaust
stack that puffs smoke as it runs, and a heavy spoked flywheel on its right-hand side, on a shaft that comes out of both
sides. It holds 1,000 mB of one fuel, diesel or kerosene, hot or cold, piped into its back or bottom or poured in by
bucket. With fuel in it, its shaft turned at 8 RPM or more for 2 seconds, by a hand crank or any turning shaft, makes it
catch, and it runs the way it was cranked, so it never fights the crank. Running, it burns 1 mB a tick (a bucket every
50 seconds) and turns at 32 RPM with 512 su a RPM, 16,384 su, a whole steam engine's worth; on kerosene three quarters
of that, 12,288 su (`KEROSENE_POWER`), worked out from each mB it burns, so a tank changed from one fuel to the other
changes the power. It stalls when it runs dry, or when what it drives is too much for it for a second (`STALL_TICKS`),
and must be cranked again. Create's flywheel, a mechanical piston, a steel fluid tank and four steel plates make one.

**Goggles.** The engine's say whether it is running, stalled dry or overloaded, or waiting on fuel or a crank and how
far through the crank it is, its fuel and how long it lasts, its speed and power, and when kerosene runs it weaker. The
Steel Furnace's name the second fluid it waits for; the condenser's, the gas escaping.

**JEI and Ponder.** The Distilling tab has crude oil, its sour gas marked as leaving by the vent or the condenser's gas
outlet. The Steel Furnace tab shows a recipe's second fluid, the Steel Furnace Fuel tab the three new fuels (sour gas
with a line on its sulfur dioxide), and a new Diesel Engine tab (`DieselEngineCategory`) each fuel with its speed,
power, how long a bucket lasts and how it starts; the Settling and Boiling tabs show the wax and the coke. Ponder scenes
`oil/pocket` (finding a pocket, the hose pulley, the sticky spill and setting it alight), `diesel_engine/intro` (fuel,
cranking, running, kerosene and stalling) and `steel_furnace/cracking`.

**Models and textures** (`tools/gen_oil.py`, called last from `gen_assets.py`): the engine's body, cylinder, stack,
tank and flywheel (a partial model, `ModPartialModels`, turned by its renderer and Flywheel visual), the eleven new
fluids, paraffin wax and petroleum coke, and the five structural formulas.

**Review.** A review of the stage found ten things; five are fixed, each with a test that fails without it: Create's
check of a pool every 8 seconds took the endless flag off a small pocket, so a hose pulley's draw stalled and started
over (fixed by answering for crude in the check too); a pipe on the condenser locked onto the sour gas and never drew
the naphtha, holding the column up; an engine whose tank went from diesel to kerosene without running dry kept the old
power; steam left in the Steel Furnace's second tank after its feed changed blocked it for good; and the condenser's
goggles called escaping sour gas vapour. In a dev client, the flywheel sat on the left of the engine for some facings
(now always on its right) and the pocket's Ponder scene was cut off at the edges (now drawn at three quarters). Known
gaps: features placed after the pockets (a lava spring, or a dungeon or geode from a neighbouring chunk decorated later)
can rarely open one, as can a lush cave's clay pools; each fill of a Steel Furnace scans its recipes more than once; the
hose pulley mixin looks up the pool's fluid once more than Create does; the column works out a recipe's gases several
times a tick; the gas clouds each build their damage source the same way; and the Steel Furnace's first feed tank still
can't be pumped back out.

**Tests.** 14 GameTests in `CrudeOilTests`. A pocket placed in a stone box with a tunnel, water and planks in it fills
more than 60 blocks, every one sealed. A hose pulley draws crude oil at 5 mB a tick for 260 ticks, the pool untouched
and still endless after Create checks it again, saves up 100 mB while idle, and water still comes a bucket at once.
Crude oil slows an armour stand that stone doesn't, lava sets it alight, and a burning stand lights it. A bucket in the
glass still on a fire gives 250 naphtha, 150 kerosene, 200 diesel and 200 heavy oil in its flasks, 50 sour gas at the
vent and 150 bitumen left; the column's layout needs five sections, three for a too-short column, one for gas alone; a
column five high gives the cuts in its sections and 250 naphtha with 50 gas in the condenser, a plain drain taking the
naphtha first, and a full gas outlet lets sour gas loose with the goggles saying so. Naphtha and steam crack into 300
ethylene and 200 fuel gas, the furnace waits for steam without it, and stale steam drains back out. Heavy oil settles
into wax and 225 fuel oil, and bitumen over a fed blaze burner into 2 petroleum coke. Paraffin wax waxes copper and
makes waxed parts and candles, petroleum coke stands in for Coal Coke, and the engine is crafted. Sour gas puffs sulfur
dioxide from a burning Steel Furnace, which kills grass and leaves; sour gas poisons and flashes, fuel gas only
flashes. An engine catches after 2 seconds of cranking at 32 RPM and 16,384 su, runs about 100 ticks on 100 mB, stalls
dry, stays stalled, and catches again once refuelled; on kerosene it gives 12,288 su and says so, a 4 RPM crank never
starts it, one with no fuel says so, and a tank switched to diesel gives full power again; and overloaded, it stalls.

## 43. The liquid air plant (0.34.0)

Stage 4 of Taran's distillation design (design/distillation.md in the shared folder; the numbers it left to the build
are in design/liquid-air-build-numbers.md): a liquefier built from a fan, the Pressurizer, a steel condenser and a new
aluminium expansion valve; an aluminium air column that splits liquid air with no heat; cold lines that crack steel,
iron and PVC; and what the gases are for: oxygen for hotter fires, ammonia and urea, nitric acid over platinum, and
argon for the arc furnace and argon tubes. Nine cards went to Taran on 2026-11-16, and Taran chose seven as they were
built: a fifth liquefied a pass; four extra sections between argon and oxygen, seven in all; frost, then crack and
burst; a mB of oxygen for every 2 of fuel; iron catalyst pellets that wear; half again from argon; and oxygen that flares
fire while the rest smother. The other two changed a number: a bucket of liquid air splits as real air does, 780
nitrogen, 10 argon and 210 oxygen (the pick was 750, 50 and 200), and platinum turns up 1 time in 100 red sand washed
(the pick was 1 in 50). Stage 5 (sour gas cleaning, sulfuric acid and the coal tar dyes) comes next.

**Air in** (`pressurizer/PressurizerBlockEntity`). An encased fan of any metal against a Pressurizer's vessel, blowing
into it, feeds it air, 1 mB a tick for every 16 RPM, into its feed tank. A fan pulling, or set further off, does
nothing. Create's own encased fan is the andesite tier and breaks above 128 RPM, so a fast feed takes a steel fan.

**The liquefier** (`recipes/pressurizing/hot_compressed_air.json`, `hot_compressed_nitrogen.json`, `fluids/WarmCuts`). From 256 RPM the Pressurizer
squeezes 100 mB of air (or nitrogen) a batch into hot compressed air (or nitrogen), which leaves Warm, so no PVC carries
it. A steel condenser inline chills it into compressed air, but only on coolant: on water its jacket stays too warm,
and the Cooling Hot Cuts tab marks those two cuts coolant only (`WarmCuts.coolantOnly`). The **expansion valve**
(`air/ExpansionValveBlock`, `ExpansionValveBlockEntity`, `Liquefying`), a new aluminium block, takes the compressed gas
into any side, up to 100 mB a tick, and a fifth turns liquid (`LIQUID_SHARE`): liquid air out of its bottom, the rest
out of its top as air again, cold, to pipe back into the Pressurizer for another pass. With nothing drawing from its
top, that gas is let go; with nowhere for the liquid to go, it holds 1,000 mB and then stops taking more. It frosts
over while it works (`FROSTED`). Fed nitrogen, the same chain makes liquid nitrogen.

**The air column** (`air/AirColumnSectionBlock`, `AirColumnSectionBlockEntity`, `column/Column`, `ColumnLayout`).
Aluminium column sections join as steel ones do, one to three across and up to sixteen high, but never with steel. The
column runs in an air mode: it has no heater and no condenser, and liquid air piped into its bottom section boils in
the world's warmth at 5 mB a tick for every block of floor (`pace` = width²). A bucket gives 780 nitrogen from the top
section, 10 argon from the one below it and 210 oxygen from the bottom, all three gases, drawn by pipes on their
sections; the top cut gets its own section rather than a condenser. Argon and oxygen boil 3 °C apart, so the layout
gives them four extra sections between them (`ColumnLayout.credit`), seven in all; a column short of them lets part of
the argon go over the top, as the steel column's close pair does. Liquid nitrogen piped into the top section is reflux:
each half of the reflux ratio stands in for one extra section (`mostReflux` = extra / 2), so a ratio of 2 runs liquid air
on three sections; it boils back into nitrogen in the top section (`reboilAir`) and slows the column as reflux always
does. Cuts with no room, and gas the pipes don't draw, are let go beside the column every second (`Gases.escape`). Running,
it frosts after 10 seconds and grows icicles from its flange rings after 30 (`FROST`, looks only); it never rusts, and
melts beside anything Hot as an aluminium tank does. The glass still and the steel column don't take liquid air.

**Cold lines** (`fluids/FluidHeat`, `Overheating`, tags `mcremix:cryogenic` and `mcremix:cracks_in_cold`). Liquid air and
liquid nitrogen are cryogenic: below 150 K, or tagged. In a part tagged to crack in the cold, steel and iron pipes,
pumps, electric pumps and tanks and PVC pipe, they frost it white for about three seconds (60 ticks and up to 19 more)
with a creak of ice, and then it gives way as an overheated part does: steel and iron burst and drop as items, PVC
shatters. Copper, brass, aluminium and glass take the cold, as do Create's spouts, basins and drains, the column and the
valve. Tooltips on pipes, pumps and tanks say when a part cracks in the cold (`client/ClientEvents`).

**Spilt** (`fluids/CryogenicLiquidBlock`). Poured out, either boils away in about two seconds, smoking white, into its
gas. As soon as it lands it freezes water beside it to ice and puts out fire, and anything standing in it freezes as in
powder snow.

**Liquid nitrogen as a coolant** (`cooling/Coolants`). In a water cooler it cools six times as fast as water, and boils
off into nitrogen where warm water would come out, ready to pipe on. The glass coil condenser takes it too; the steel
condenser's jacket and the radiator never do.

**Oxygen** (`kiln/SteelFurnaceBlockEntity`). Piped into a lit Steel Furnace's front, sides or top, oxygen goes to the
fire. With a mB of oxygen for every 2 mB of fuel over the last second (`FUEL_PER_OXYGEN`), its fuel burns twice as hot
and its ceiling goes up a level: coal tar fed 2 mB a tick with 1 of oxygen burns Blazing. The furnace draws only what
its fuel needs, and unlit, the oxygen escapes out of its front. Oxygen a recipe takes as its second fluid goes to the
job first.

**Ammonia and urea** (`pressurizer/PressurizingRecipe`, `recipes/pressurizing/ammonia.json`, `urea.json`,
`item/UreaItem`). The Pressurizer gains a second feed tank and a catalyst slot, so its recipes can take a second fluid
and something to squeeze over. Four iron nuggets pressed with alumina over a basin make two iron catalyst pellets (`recipes/compacting/iron_catalyst.json`), dropped into the
vessel by hand or funnel. 100 nitrogen and 300 hydrogen squeezed Hot at 256 RPM over a pellet give 200 ammonia; a
pellet lasts 16 buckets of ammonia and then crumbles (`catalystLasts`). 100 ammonia and 50 CO2 squeezed Hot give 4 urea
and 50 water. Urea is bulk bone meal: on a crop it grows one stage every time; on anything else bone meal works on it
does what one bone meal does, and dispensers use it. The Pressurizer's second tank takes only a fluid some recipe wants
second, and once its feed is in, only one that goes with that feed (`takesSecond`).

**Nitric acid and sodium nitrite** (`recipes/steel_furnace/nitrogen_dioxide.json`, `recipes/vat_mixing/nitric_acid.json`,
`sodium_nitrite.json`). 100 ammonia and 175 oxygen burnt in a Steel Furnace at Searing over a platinum gauze give 100
nitrogen dioxide and 150 water in 5 seconds; the gauze lies in the chamber as copper mesh does and lasts 64 buckets.
Nitrogen dioxide stirred with water in a vat gives nitric acid, and with caustic soda, sodium nitrite crystals and water.

**Platinum** (Create's `recipes/splashing/red_sand.json`). Washing red sand turns up a platinum nugget 1 time in 100, beside its
gold and bauxite; four nuggets in a square make a gauze.

**Argon** (`arcfurnace/ArcFurnaceBlockEntity`). Piped into an Arc Furnace's sides, argon waits in a 1,000 mB tank. Each
sponge melted with 25 mB of argon there (`ARGON_PER_MELT`) gives half an ingot more, carried to the next, so two
sponges give three ingots. A Spout fills a glass tube with 50 mB of argon for stage 6's argon tube (`tools/gen_decor.py`
now names the argon fluid).

**Gases let loose** (`fluids/OxygenCloud`, `SmotheringGasCloud`, `NitrogenCloud`, `ArgonCloud`, `AmmoniaCloud`,
`NitrogenDioxideCloud`, `electrolysis/Gases`). Oxygen is an all but invisible haze as heavy as air, about 20 seconds a
bucket: what burns in it burns twice as long, and a fire under it spreads to what burns beside it. Nitrogen and argon
smother as CO2 does (CO2's cloud now shares their base): nitrogen thins in about 10 seconds a bucket, while argon sinks
and pools for about a minute. Ammonia is a sharp haze lighter than air, about 15 seconds a bucket, that stings, 1
damage every 2 seconds with blindness and slowness. Nitrogen dioxide is a brown haze heavier than air, about 30 seconds
a bucket, that poisons through armour, 1 damage a second and nausea. Both have their own death messages. Air,
compressed air and compressed nitrogen just hiss away.

**Goggles.** The valve's say whether it is liquefying, what it holds, and how much gas a second it lets go and why;
the air column's are the steel column's, with how fast it boils in the air's warmth in place of its heat; the
Pressurizer's name the second fluid it waits for, and its catalyst and how worn it is, or the catalyst it needs; the
Steel Furnace's say when oxygen burns it hotter and up to what; the Arc Furnace's its argon.

**JEI and Ponder.** A new Liquefying tab (`compat/jei/LiquefyingCategory`) shows each compressed gas through the valve,
a fifth liquid and the rest gas. The Distilling tab has liquid air in the air column, with no heat, its argon and
oxygen marked as needing seven sections bare or three at a reflux of 2. The Pressurizing tab shows a recipe's second
fluid and its catalyst with how long it lasts; Cooling Hot Cuts marks the coolant-only cuts; the Steel Furnace Fuel tab
says how oxygen helps; the Arc Furnace tab says what argon gives; and liquid nitrogen and liquid air have info pages.
Ponder scenes `liquefier/intro` (fan, Pressurizer, condenser, valve and the loop back), `air_column/intro` (the split,
reflux and frost) and `cold_lines/intro` (what cracks and what takes the cold), in their own plugin
(`ponder/AirPonderPlugin`, `AirScenes`).

**Models and textures** (`tools/gen_air.py` and `gen_air_art.py`, called from `gen_assets.py`): the valve and the
aluminium column section with their frost and icicle stages, the thirteen new fluids, iron catalyst, platinum nugget and
gauze, urea and sodium nitrite, the structural formulas (nitrogen drawn blue, argon pale cyan, platinum grey) and the
three Ponder schematics.

**Review.** A review of the stage found six things, all fixed; the four that change play each have a test, and with
them undone the oxygen, ammonia and arc furnace tests fail. Oxygen piped in before the fuel in a tick (whichever pipe ticks first, which can change on a reload) never
made the fire hotter, because the furnace counted the fuel it was owed over a window a tick behind the one it judged
by: until a tick's fuel is in, the tick before the window now stands in for it, and a fire a twentieth short of oxygen
still counts as fed. A Pressurizer with nitrogen in and its hydrogen run out squeezed the nitrogen on its own, sending
hot compressed nitrogen down the ammonia line: over an iron catalyst it now waits for hydrogen (`PressurizingRecipe.find`
ranks a recipe waiting over the catalyst in the vessel before one that takes nothing second). Taking a catalyst out and
putting it back made it new; its wear stays with the vessel now, as the Steel Furnace's does. An Arc Furnace with room
for one more ingot but not the half more argon owed stopped as full; it now melts that one without argon. Two only
touched goggles: argon piped into an idle Arc Furnace wasn't sent to clients, and an air column's venting figure counted
a joined column's pending gas twice. Two older tests were made sturdier, since the new tests shifted when each batch
starts: the rusted chlorine tank's cloud, let go a second's leak at a time and lasting 12 ticks of every 20, is looked
for every tick rather than at one, and the fan test sets its second fan turning only once it knows which way is in. In a
dev client the air column's Ponder scene was cut off at the top (now drawn at 0.6 and a block lower). Known gaps: gas an
air column or a valve is about to let go isn't saved, so up to a second of it is lost on unload; the valve's goggles
count any pipe above it as drawing its gas, joined or not; and a click that a full catalyst slot refuses still swings
the hand on the client.

**Tests.** 16 GameTests in `LiquidAirTests`. A fan at 128 RPM blowing into a Pressurizer's vessel at 256 RPM feeds it
air that it squeezes into 700 to 1,000 mB of hot compressed air by tick 120, and one blowing out feeds nothing. A steel
condenser chills hot compressed air only on coolant and never takes liquid nitrogen. The valve turns compressed air a
fifth liquid air out of its bottom and the rest air out of its top, frosting over, lets the air go with nothing on top,
and does the same for nitrogen. A column seven high splits a bucket into 780, 10 and 210, nothing lost, and steel and
aluminium sections don't join; three high it loses four fifths of its argon over the top as a cloud, and liquid
nitrogen reflux past a ratio of 2 stops that, boils back into nitrogen and slows it to a third, frosting it over; over
lava it melts. Liquid nitrogen bursts steel and iron tanks and cracks steel, iron and PVC pipes, but not copper,
aluminium or glass; spilt, it freezes water, puts out fire, freezes a pig and boils away into nitrogen; in a water
cooler it cools faster than coolant and boils off into nitrogen. Oxygen makes hydrogen burn Searing and wood spirit past
its ceiling, coming before the fuel or after it. Let loose, oxygen keeps a burning stand alight longer, nitrogen and
argon smother (argon sinking into a pit), ammonia stings and nitrogen dioxide poisons. Nitrogen and hydrogen over iron
give 600 ammonia, wearing the catalyst 600 whether it is taken out and put back or not; without a catalyst it waits, and
over iron without hydrogen too, while nitrogen alone is squeezed. Ammonia and CO2 give 8 urea and 100 water, and urea
grows wheat a stage. Ammonia and oxygen over platinum gauze give nitrogen dioxide and water. Argon gives an arc furnace
half again, and one nearly full melts without it. Red sand washes out platinum 1 time in 100, and four nuggets make a
gauze. `FormulaTests` covers the ten new formulas.

## 44. The sulfur works and the dye works (0.35.0)

Stage 5 of Taran's distillation design, its last stage (design/distillation.md in the shared folder; the numbers it left
to the build are in design/dyes-sulfur-build-numbers.md): sour gas cleaned two ways, molten sulfur, sulfuric acid over
the platinum gauze, nitrating and the amines, the diazo salts, gunpowder, and the coal tar dyes, which give mauve,
indigo, malachite green and maroon their first recipes. Nine cards went to Taran on 2026-11-16, and Taran chose the
pick on all nine: a sulfur for every 250 mB of sour gas; red mud that cleans 250 mB and airs back in 20 seconds;
hydrogen sulfide deadly, sinking and unseen when thick; sulfur trioxide fogging half off water but none off acid; diazo
salts kept on ice; sodamide tripling the indigo; ferric chloride for malachite green; 8 dyes for every 250 mB of
aniline; and 4 gunpowder a batch.

**Red mud** (`recipes/settling/red_mud_cleaning.json`, `electrolysis/Vat`). A vat of red mud, standing with no mixer,
cleans sour gas piped into it: each lump takes 250 mB in 5 seconds, leaves 150 mB of fuel gas and turns black, into
foul red mud, a batch for every bucket the vat holds. A vat keeps a gas only while it holds the items a recipe works it
with (`Vat.wants`), so once the mud has all gone foul, sour gas passes on to a pipe as anything it has no use for does.

**Airing** (`electrolysis/AiringRecipe`, `BasinSettling`, `registry/ModRecipes`, `recipes/airing/`). A new recipe type:
what an open basin turns in the air. A basin with air above it, no mixer or press over it and no heat under it airs a
batch at a time; a vat airs as it settles, with 50 mB of oxygen piped in for each batch (`AiringRecipe.inVat`, built
from the basin recipe with the oxygen added). Foul red mud airs into red mud and a sulfur in 20 seconds; indigo melt
into 8 indigo dye and its 250 mB of caustic soda in 10.

**The Claus works** (`recipes/vat_mixing/claus_wash.json`, `distilling/foul_soda.json`, `steel_furnace/claus.json`).
Soda ash mixed with water is soda ash solution; 250 sour gas and 150 of it stirred in a vat give 150 fuel gas and 250
foul soda. Foul soda boils Warm in the glass still or the steel column into 400 mB of hydrogen sulfide and 600 of soda
ash solution a bucket. Hydrogen sulfide run over alumina in a Steel Furnace, Hot, gives molten sulfur and water, 100 of
each for every 100 mB; the alumina lasts 16 buckets.

**Hydrogen sulfide as fuel** (`kiln/SteelFurnaceFuels`, `SteelFurnaceBlockEntity`). Piped into a Steel Furnace's bottom
it burns as sour gas does, a heat a mB up to Searing, but puffs out a mB of sulfur dioxide for every mB where sour gas
puffs one for every 2 (`Fuel.sulfurUnits`; the furnace now counts half-mB of sulfur dioxide).

**Molten sulfur** (`fluids/MoltenSulfurBlock`, `recipes/settling/molten_sulfur.json`, `registry/ModDyes`). Warm, so no PVC
carries it. In a basin or vat with no heat under it, 100 mB sets into a sulfur in a second. Poured out, a source sets
into a block of sulfur in 1.5 to 4.5 seconds and what ran from it into nothing, smoking; standing in it scalds. Nine
sulfur craft into a block and back.

**Sulfuric acid** (`kiln/FiringRecipe`, `KilnBlockEntity`, `recipes/firing/sulfur.json`, `steel_furnace/sulfur_trioxide.json`,
`electrolysis/VatMixingRecipe`, `vat_mixing/sulfuric_acid.json`, `sulfuric_acid_soaked.json`). A firing recipe may now
have no result: sulfur burns away in a Furnace, Warm, into 100 mB of sulfur dioxide kept in its tank, and what has no
room goes up the chimney as its choking cloud. Sulfur dioxide over the platinum gauze in a Steel Furnace, Hot, gives
sulfur trioxide, the gauze lasting 64 buckets. A vat mixing recipe can now let fumes go (`fumes`): 250 sulfur trioxide
and 250 water stirred in a vat give 250 sulfuric acid and let 125 mB of the trioxide go as an acid fog over the vat.
With 250 sulfuric acid piped in too they give 750 and nothing fogs, since a vat tries the recipe that takes the most
first (`Vat.mixing` now counts fluids as well as items). What the vat makes waits apart from what it was filled with, so
a plant pipes some acid back in. 250 nitric and 250 sulfuric acid mixed are 500 mixed acid; sulfuric and mixed acid
poured out strip rust and patina as the other acids do (`fluids/Chemical.acid`).

**Nitrating and the amines** (`recipes/mixing/`). Light oil and nitric acid give nitrobenzene slowly, in 400 ticks, or
with mixed acid in a quarter of the time and the sulfuric acid back; nitrobenzene, vinegar and iron nuggets, heated,
give aniline (Béchamp). Aniline and hydrochloric acid give aniline salt; aniline and vinegar, heated, acetanilide, which
mixed acid nitrates and hydrochloric acid frees into nitroaniline. Naphthalene nitrates into nitronaphthalene and
reduces into naphthylamine; with sulfuric acid, heated, it is naphthalenesulfonic acid, which caustic soda melts into
naphthol.

**The diazo salts** (`fluids/DiazoSalts`, `mixin/FluidTankBlockEntityMixin`, `BasinBlockEntityMixin`, `Vat`). Aniline,
hydrochloric acid, sodium nitrite and a block of ice (or packed ice) mixed in an unheated basin give 250 diazo salt; in
a vat, 250 cold water does the ice's job and comes out as water. Nitroaniline and naphthylamine make the nitro and
naphthyl salts the same way. A salt in a tank, basin or vat that no block tagged `mcremix:keeps_cold` (ice, packed ice,
blue ice, snow blocks, powder snow) touches, outside a snowy biome, loses a tenth every 10 seconds (`EVERY`, spread by
position), fizzing; in a heated basin it all goes within a second, plain diazo salt into half as much phenol. A basin
checks its output as well as its input. Goggles on a warming tank or vat say so.

**The dyes** (`recipes/mixing/*_dye.json`). 250 aniline and 250 bleach (chlorine stirred into caustic soda in a vat) give
8 mauve; aniline salt with iron and nitrobenzene, heated, 8 magenta; aniline salt with ferric chloride, 8 black. Diazo
salt with more aniline gives 16 yellow, and with sodium phenolate 8 orange; the nitro salt with 2 naphthol and caustic
soda gives 8 red, the naphthyl salt 8 maroon. Indigo: chloroacetic acid (vinegar and chlorine in a vat) and aniline,
heated, give phenylglycine; 12 phenylglycine, 3 sodamide (molten sodium and ammonia in a vat) and 750 caustic soda,
heated, give 750 indigo melt, and without sodamide 12 give only 250. The sodamide batch is three times the card's so
that Create, which tries the recipe with the most items first, takes it over the plain one. Aired, 250 mB of melt is 8
indigo dye. Malachite green: benzal chloride (light oil and chlorine in a vat) and water, heated, give benzaldehyde;
aniline and methanol over alumina in a Steel Furnace give dimethylaniline (a second fluid, as steam is for naphtha);
benzaldehyde, dimethylaniline and a little hydrochloric acid stirred in a vat give 4 leuco base, which 250 ferric
chloride turns into 8 malachite green.

**Gunpowder** (`recipes/mixing/saltpetre.json`, `gunpowder.json`). Soda ash and nitric acid give 2 saltpetre, water and
CO2; a sulfur, 2 saltpetre and a charcoal mixed give 4 gunpowder.

**Gases let loose** (`fluids/HydrogenSulfideCloud`, `SulfurTrioxideFog`, `GasCloud.shows`, `electrolysis/Gases`).
Hydrogen sulfide sinks and pools, about 30 seconds a bucket, and poisons through armour, 2 damage a second with nausea;
a cloud under about half a bucket shows a faint yellow haze and a bigger one nothing, as strong hydrogen sulfide numbs
the nose. At a flame it flashes as sour gas does and leaves sulfur dioxide. The acid fog is white, sinks, lasts about 20
seconds a bucket, hurts 1 a second through armour, and rusts or weathers the metal under it a stage, as hydrogen
chloride mist does. Both have death messages. Sulfur dioxide, hydrogen sulfide and sulfur trioxide escaping a vat,
basin or column now leave as their clouds.

**Goggles, tooltips, JEI and Ponder.** Vats say when they are airing, and tanks and vats when a diazo salt in them is
warming. Buckets of molten sulfur, indigo melt, the diazo salts, sulfuric and mixed acid say what they are for. JEI gets
an Airing tab (`compat/jei/AiringCategory`) with the basin, the tank and the scaffolding as its places; the Vat Mixing
tab shows fumes; the Furnace tab shows a firing with no result; the Steel Furnace Fuel tab marks hydrogen sulfide; and
the diazo salts, hydrogen sulfide and sulfur trioxide have info pages. Ponder scenes `sour_gas_cleaning/intro`,
`sulfuric_acid/intro` and `dye_works/intro`, in their own plugin (`ponder/DyePonderPlugin`, `DyeScenes`).

**Models and textures** (`tools/gen_dyes.py` and `gen_dyes_art.py`, called from `gen_assets.py`): sulfur and its block,
foul red mud, the twelve crystal, needle, granule and powder items, the twenty new fluids, the structural formulas
(sulfur drawn yellow) and the three Ponder schematics.

**Review.** A review of the stage found ten things; four are fixed, and the one that changes play has a test, which
fails with the fix undone. Oxygen piped into any formed vat stayed in it for good, taking its room, since airing indigo
melt in a vat takes no items and a gas waited wherever the vat held a recipe's items: a gas now waits only where the vat
holds the rest of what its recipe takes, fluids as well as items (`Vat.holdsTheRest`), so a pipe draws oxygen out of a
vat with nothing to air. The acid fog over a vat and the fizz of a warming diazo salt came out over the controller's
corner, about a block and a half off on a vat 3 wide, and now come out over its middle (`DiazoSalts.topOf`). A diazo
salt in a creative tank fizzed every ten seconds without ever running down; creative tanks are now left alone, goggles
too. And the tenth that breaks down was worked out apart for basins and for tanks, and is now one sum
(`DiazoSalts.part`). Six are left as they are: whether ice touches a tank checks every block of it, inside ones too, at
most 288 for Create's tanks, every ten seconds and on each look through goggles; a stirred vat sorts its mixing recipes
each time a pipe asks it, as it did before; a mix of many batches lets its fumes go a batch at a time, each looking for
a cloud to join; the acid fog's rusting repeats the hydrogen chloride mist's; `Chemical.light` names molten sulfur and
indigo melt rather than lighting a fluid by its heat; and molten sulfur burns with vanilla's hot floor damage, as molten
metal burns with lava's, since the mod's own scalding reads as a bursting boiler. In a dev client the sulfur block and
the new items draw, poured molten sulfur sets into a block of sulfur with what ran from it gone, and the three Ponder
scenes play to their last step. Known gaps: acid a vat has made doesn't soak up the fog, only acid piped in, since what
a vat makes is kept apart from what it was filled with; death in molten sulfur reads as the floor being lava; and no
GameTest runs the basin recipes from nitrating to the dyes, saltpetre and gunpowder, the vat recipes for the Claus wash,
bleach, chloroacetic acid, sodamide, benzal chloride and the leuco base, or dimethylaniline in the Steel Furnace, which
all load with no errors and use the basin, vat and furnace as the earlier recipes do.

**Tests.** 14 GameTests in `DyesSulfurTests`. All 53 new recipes load; nine sulfur craft a block and back; a batch makes
4 gunpowder; and foul soda boils Warm into 400 hydrogen sulfide and 600 soda ash solution. Two lumps of red mud in a vat
clean 500 mB of a bucket of sour gas into 300 of fuel gas and go foul; a pipe can't draw the sour gas off while red mud
is in, and can once it has all gone foul. Foul red mud airs into red mud and a sulfur in 20 seconds in an open basin but
not under a block, and in a vat on 50 mB of oxygen a lump; oxygen waits in a vat of indigo melt or foul red mud but not
in an empty one; and 250 mB of indigo melt airs into 8 indigo dye and gives its caustic soda back. Poured molten sulfur
sets, its source into a block of sulfur and what ran from it into nothing, and 300 mB in a basin sets into 3 sulfur. Two
sulfur in a Furnace on lava burn into 200 mB of sulfur dioxide, leaving nothing in the tray. In a Steel Furnace 300
hydrogen sulfide over alumina gives 300 molten sulfur and 300 water, and 300 sulfur dioxide over the gauze 300 sulfur
trioxide. Sulfur trioxide stirred with water gives 250 acid and a fog over the vat, and with acid piped in 750 and no
fog. A diazo salt loses a tenth in ten seconds in a warm tank but nothing in one touching packed ice, and goggles warn
of the first only; in a heated basin 500 mB of plain salt in the input breaks down into 250 phenol, and 500 of the nitro
salt in the output into nothing. Hydrogen sulfide burns as hot as sour gas, Searing at most, with twice its sulfur, and
a Steel Furnace on 4 mB a tick burns Searing and puffs sulfur dioxide; let loose it poisons a pig with nausea, it and
the acid fog hurt through armour, a cloud of it flashes at a campfire into sulfur dioxide, and 250 mB of it shows where
2 buckets don't. `ColourTests` now checks that mauve, indigo, malachite green and maroon each come from their dye works
recipe and only from it, and `FormulaTests` covers the 34 new formulas.

## 45. A new look for the engine, the valve, the condenser and the columns, and the steel flywheel (0.36.0)

Taran's ask of 2026-11-16, after 0.35.0: no stair-stepped edges on the expansion valve and the steel condenser, no red
handle on the valve, no green on the diesel engine, a steel flywheel in the engine's recipe, an engine that shows where
its pipe goes in, connected textures on the steel and aluminium columns that show when one forms, and a clean, shiny
aluminium for the aluminium column and the valve. Five cards went to Taran the same evening: the condenser's shell and
the valve's drum are square boxes shaded to look round, as Create draws its pipes and tanks; the valve is all polished
aluminium; the engine is bare dark steel with a brass tank and fittings; the steel flywheel is a block of its own, set
on a shaft line as Create's is; and a formed column has a flange at every section. Clean, shiny aluminium was read as
the mod's aluminium tanks and pipes are: smooth bright panels, a soft sheen, crisp seams, no mottle and no rivets. Frost
and icicles stay.

**The steel condenser** (`tools/gen_column.py`). One square shell, 12 pixels across, shaded round with a seam each side,
where the old one was stepped boxes; a flanged nozzle stands out of each of its four pipe faces. The rusted look is the
same shell in rusted steel.

**The expansion valve** (`tools/gen_air_art.py`). All polished aluminium: a square drum shaded round, a flat head with a
flange under it, the pipe stubs and port, and the handwheel, which was red. Frosted and frozen looks are drawn on the
new art as before.

**The diesel engine** (`tools/gen_oil.py`). Bare dark steel where it was painted green, its fins, stack, flywheel and
shaft in the same steel; the fuel tank is brass. Its fuel inlet now shows: a brass flange on its back with a bolt at
each corner and a dark bore in its middle, where a pipe joins, a brass line from it up to the tank, and a brass ring
round a bore under its base plate, where a pipe from below joins.

**The steel flywheel** (`kinetics/SteelFlywheelBlock`, `SteelFlywheelBlockEntity`, `client/SteelFlywheelRenderer`,
`SteelFlywheelVisual`, `registry/ModOil`, `client/ModPartialModels`). Create's flywheel in steel: eight steel ingots
round a steel shaft craft one, and it is set on a shaft line, turns with it and eases its speed up and down as Create's
does. It is drawn with Create's flywheel model and a steel texture drawn from Create's, shade for shade
(`tools/create_flywheel_texture.json`), with a steel shaft through it. Create keeps its flywheel's block entity,
renderer and visual to its own package and draws its own wheel, so these are the mod's own copies. Like the rest of the
mod's steel it rusts where it gets wet, only to its first stage, and honeycomb or paraffin wax keep it as it is; like
Create's it holds no shafts (`mcremix:holds_no_shafts`), takes no stress and is given no Stability or Strength. The
diesel engine's recipe takes it where it took Create's brass flywheel.

**Connected columns** (`client/ColumnCTBehaviour`, `client/ModSpriteShifts`, `registry/ModColumn`, `registry/ModAir`,
`column/ColumnSectionBlockEntity`). The sections of a formed steel or aluminium column join: their side plates run on
across the column's width, a flange band runs round it at every section, a cap rings its top and a skirt its foot, and
the plates on its top join into one. Only sections of the same formed column join (Create's
`ConnectivityHandler.isConnected`, as its tanks do), so two columns side by side keep their own edges, and a lone
section looks as it did. A section's mesh is drawn again on the client when its column forms, grows or splits, since
that changes nothing in its block state. Each look has its own sheet: fresh and rusted steel; plain, frosted and frozen
aluminium.

**Aluminium** (`tools/gen_air_art.py`). The aluminium column's plates, top, flanges and nozzles and the valve are drawn
in a new polished palette, bright and cool, with soft streaks and a rare glint on the top in place of the mottle and
rivets; the lips at a section's ends are slimmer.

**Review.** One review of the change found ten things; six are fixed. A rivet ring on a column's top doubled up at the
corners where the column ends, two rivets side by side, and now turns the corner on one, two pixels in from each end
(`gen_column.ring`). The diesel engine's shaft was still Create's andesite axis on an engine now all steel and built
round a steel flywheel, and is now steel. The generator's third copy of the patina list is gone (it reads
`gen_assets.PATINAS`), Create's flywheel texture is read when it is needed with the file closed after, and
`ModSpriteShifts.column` takes its metal first; and the docs had no word of 0.36.0 until this section, the report and
DESIGN.md. Four are left as they are: the steel flywheel takes its rust from the fluid part hook
(`FluidPartMaterial.Part`), as the decor blocks do, and every fluid check that reads it looks only at blocks in a fluid
network or at the tank under a steam engine, which a flywheel never is; a section's joined texture depends only on the
sections beside it, and any change between two of them either places or breaks a block, which redraws the blocks round
it, or changes one of their controllers, which redraws round that section, so a column that grows round the same
controller still redraws where it must; every section of a formed column marks its blocks for redrawing when a chunk
loads, which is cheap and idempotent, and skipping the first read would leave a newly placed section drawn unjoined; and
frost on a frosted column repeats from block to block as it did before, since each block was always one texture. In a
dev client the five old blocks and the steel flywheel draw as meant: formed steel and aluminium columns join with a band
at every section, the condenser is square, the valve polished with an aluminium handwheel, the engine dark steel with
its brass inlet in plain sight once the pipe behind it is taken away, and fresh and rusted flywheels turn on creative
motors. Known gaps: frosted and frozen columns were seen only in the generator's previews, since a column's frost
follows what it holds and setting it by hand does not last; and no GameTest covers the client side of a column redrawing
as it forms.

**Tests.** `aSteelFlywheelIsSteelAndTurnsOnItsShaftLine` in `CrudeOilTests`: eight steel ingots round a steel shaft
craft a steel flywheel; it rusts as steel does, only to its first stage; honeycomb waxes it; it holds no shafts and is
given no rating; and a creative motor at 32 RPM turns it at 32. `paraffinWaxAndPetroleumCokeStandInForHoneycombAndCoke`
now crafts the diesel engine from a steel flywheel. The rest is art and a client-side texture rule, checked in a dev
client.

## 46. A bigger engine, stacked columns, detailed aluminium and an upright condenser (0.36.1)

Taran's asks of 2026-11-16, after 0.36.0: the diesel engine still looked too small and weak, perhaps with a bigger
flywheel, and its yellow had to go; the aluminium column needed more detail; a steel or aluminium column section placed
on another should lose its bottom flange, so a stack looks joined up the column without losing the flange at each
section; the steel condenser should stand upright rather than on its side; and the part under its flange was
see-through. Three cards went to Taran that evening: one flywheel bigger than the block (it reaches into the ground
unless the engine stands a block up), dark steel all over where the brass was, and welds and bolted flanges on
aluminium. On the condenser Taran asked for it to stand upright only when placed against a column, with no gap between
the two.

**The diesel engine** (`tools/gen_oil.py`, `oil/DieselEngineBlockEntity`). A chunkier body that fills the block but for
its flywheel's side: a crankcase 10 pixels wide and 11 high, a bearing housing each side where the shaft comes out, a
bigger cylinder with three fins on the shaft's line and its head, the stack on the cylinder, the tank on the crankcase,
and a base plate bolted down at its corners. One heavy flywheel, 24 pixels across, turns on its right: a rim of sixteen
bars, each set at its angle by Minecraft's 22.5 degree turns and every other one a little thinner along the shaft, so
that where two overlap their faces never meet in one plane and flicker; eight spokes; and a hub. The wheel reaches a
quarter block past the engine on every side but the shaft's, so the engine's render box is that much bigger, and its
parts take the texture as if they stood in the block's middle, since a face's place on the texture, taken from where
it lies, would run off the texture past the block. The tank, the fittings, the inlet flange, the line up to the tank and
the ring under the base are dark steel, not brass; the inlet is now in the middle of the back face, where a pipe meets
it, and the exhaust smoke comes out of the stack where it now stands. The item is drawn smaller in the inventory and in
item frames to fit its wheel.

**Stacked sections** (`column/ColumnSectionBlock`, `ColumnSectionBlockEntity`, `tools/gen_column.py`,
`tools/gen_air_art.py`, `tools/gen_air.py`). A steel or aluminium column section on another of its own kind leaves its
bottom flange off (`down` in its block state, set as it is placed and kept as the block under it changes); on anything
else it keeps it, formed or not. A section from an older world checks the block under it once after it loads. The
Ponder structures carry the new state.

**Aluminium** (`tools/gen_air_art.py`). Each section's side has a girth weld round its middle and seams up its plates,
and each flange a ring of bolts. A frozen column's icicles now hang from the flanges, between the bolts, so they stay
inside the block and show at every joint.

**The steel condenser** (`column/SteelCondenserBlock`, `SteelCondenserBlockEntity`, `tools/gen_column.py`,
`ponder/ColumnScenes`). Its axis can now be vertical, and its block state says which side of it a steel column stands
on (`against`). Placed beside a steel column section it stands upright, flush against the first one of north, east,
south and west, with no water nozzle on that side; placed anywhere else it lies along the face it is placed against,
or along the way its placer faces when placed on top, as before. A wrench stands it up or lays it down, and an upright
one keeps flush with a column beside it as blocks round it change. Its ends are its top and bottom when upright, so the
column's vapour line comes down into its top and the cut leaves from its bottom, and water goes in and out of its other
sides. Each head's inner face, round the shell, was missing, which is what showed through under its flanges, and is
now drawn. The column scenes stand it upright against the column, its product pipe under the floor, and the tooltip
says how it stands. Condensers in an older world keep lying as they were until a wrench stands them up.

**Review.** One review of the change found nothing that needed fixing. Left as known gaps: the flywheel reaches four
pixels into whatever block is under the engine, as Taran chose, so it shows whole only with the engine on a post, a
wall or a slab, and in the engine's Ponder scene it dips into the floor; an upright condenser sits flush against the
first steel column section north, east, south or west of it, which with two columns beside it may not be the column it
serves; condensers placed before 0.36.1 keep lying until a wrench stands them up; the vapour line still rises from the
middle of the block, a pixel and a half from the middle of a flush condenser's top; and frosted and frozen aluminium,
with its new welds, bolts and icicles, was seen only in the generator's previews.

**Tests.** In `ColumnTests`: `aCondenserStandsUprightAgainstAColumnAndLiesElsewhere` (placed against a column it stands
upright against it; placed alone on top of something it lies the way its placer faces, and against a wall along that
face; upright, its top and bottom are its ends and its sides its jacket, which takes water through a side and not its
top; a wrench on a side lays it down and on the side again stands it up; with the column gone it stands on its own) and
`aSectionOnItsKindHidesItsBottomFlange` (steel on steel hides it, the bottom one shows it, aluminium on steel shows it,
and taking away the section under one shows it again). `aColumnSplitsWashIntoEthanolAndWarmWater` now stands its
condenser upright and draws the ethanol from its bottom. 443 of 443 GameTests pass plain and 449 of 449 with
`-Pcompanions`. In a dev client the engine, the columns and the condensers draw as meant; an older world's sections
took up their stacked look as they loaded.

## 47. The upright condenser kept centred (0.36.2)

Taran's ask of 2026-11-16, after 0.36.1: keep the condenser centred when it stands against a column, and just draw it
out towards the column. In 0.36.1 the whole condenser slid two pixels over to meet the column, so it sat off the middle
of its block.

**The condenser** (`tools/gen_column.py`, `column/SteelCondenserBlock`). Standing against a column it now keeps the
lone upright condenser's place, centred in its block, with its shell and both heads drawn out on the column's side to
the block's face, so it touches the column with no gap and no water nozzle that side. The water nozzles on its other
three sides are back in the middle of each face, and the vapour line comes down within half a pixel of the middle of
its top. Its outline and collision box reach the column's face and stop a pixel short of the other three.

**Tests.** No test reads the model; the condenser tests of 0.36.1 still cover where it stands. 443 of 443 GameTests
pass plain and 449 of 449 with `-Pcompanions`. In a dev client the condenser stands centred against its column.

## 48. Shiny aluminium and a condenser that runs into its column (0.36.3)

Taran's asks of 2026-11-17, after 0.36.2: take the 3D rivets off the aluminium column and make it read shiny rather
than as bricks; and have the steel condenser extrude into the steel column instead of its side just being drawn out to
it.

**Aluminium** (`tools/gen_air_art.py`, `tools/gen_air.py`). The bolt heads that stood out of each flange in 0.36.1 are
gone, and so are the girth weld round each section's middle and the staggered seams up its plates, which together read
as brickwork. The side is now one polished plate shaded round (`SHINE`): a crisp highlight a quarter of the way in from
its left, falling off to a broad, soft reflection near its right, with a little light at its edge. On a formed column
the connected texture's left end, middle and right end tiles carry that shading across the whole face, so a wide
column is shaded round as one, as Create shades its tanks. The seam under each flange, the lit cap band at the top and
the skirt at the foot follow the shading round, so they wrap the column as bands of polished metal. The top keeps its
groove and its ring of bolt heads, drawn flat in its texture; frost and icicles are as before. The tooltip calls the
section polished rather than riveted.

**The condenser** (`tools/gen_column.py`, `column/SteelCondenserBlock`). Standing against a column it now keeps the lone
upright condenser's shape, centred, with its flanged heads stopping a pixel short of the column as they do alone, and
only its shell carries on the last two pixels into the column, the shell's own shading running on to the column's face.
There is still no water nozzle on that side, and its outline and collision box are the lone upright one's with the
shell's run into the column added. A neck eight pixels square was tried first: with only two pixels between the
condenser and the column it showed in the game as a thin gap, which Taran had asked to avoid in 0.36.1, so the whole
shell runs in instead. Both were put to Taran on a card, with a picture of each
(`builds/screenshots/0.36.3/condenser_choices.png`).

**Review.** One review of the change. The lone upright and lying condenser models are unchanged byte for byte; the
shape added for the column side matches the model's; `UPRIGHT` is declared before the map that uses it; the stacked
aluminium models lost their bolt heads too; nothing else used the plate painters taken out. Left as known gaps: the
heads' pixel-wide gap to the column lets a sliver of light through when looking straight along the column's face, and
frosted and frozen aluminium was seen only in the generator's previews.

**Tests.** No test reads models or textures. 443 of 443 GameTests pass plain and 449 of 449 with `-Pcompanions`. In a
dev client the aluminium columns and the condenser draw as meant, with no errors from the mod in the log.


## 49. A neck into the column (0.36.4)

Taran's answer of 2026-11-17 to the card left in 0.36.3: the upright condenser should run into its column through a
neck, the first of the two tried, rather than with its whole shell.

**The condenser** (`tools/gen_column.py`, `column/SteelCondenserBlock`). Standing against a column it keeps the lone
upright condenser's shape whole, centred in its block, with its shell's face to the column drawn again and its flanged
heads a pixel short of the column as they are alone. Out of the middle of that face a neck of its shell, eight pixels
square, runs the last two pixels into the column. Its top and bottom take the shell's shading across them and its
sides the same shading turned to run down them, so it reads as the shell's own plate drawn out; its end is inside the
column and not drawn. There is still no water nozzle on that side. The outline and collision box are the lone upright
one's with the neck's last pixel added, turned for each side the column can be on. The tooltip says a neck of its
shell runs into the column.

**Review.** One review of the change. The flush model is the lone upright model with its column side's nozzle taken
off and the neck put on, nothing else; the lone upright and lying models are unchanged byte for byte; the neck is
centred on both of its axes, so the shape for each side needs no turning beyond its position; no face of the neck lies
in the plane of another face, so nothing flickers. Left as known gaps: with two pixels between the shell and the
column, the gap round the neck shows from most angles as a thin dark slit, and looking straight along the column's
face light shows through it, as the card's picture showed; Taran chose this over the shell running in.

**Tests.** No test reads models or the condenser's shape. 443 of 443 GameTests pass plain and 449 of 449 with
`-Pcompanions`. The neck was seen in a dev client when it was first tried for 0.36.3, against a column and cloned away
from one to see it whole; the model shipped is that one.


## 50. Coiling and vacuum sealing (0.37.0)

Taran's ask of 2026-11-17, "Can you add coiling and vacuum sealing as crafting processes?", with five cards answered
on 2026-11-18, all as Claude picked them: coiling makes Power Grid's coils, on a coil winder in every metal; vacuum
sealing makes Power Grid's tubes and bulbs, on the Pressurizer run backwards; and a deeper vacuum needs more speed and
Hot heat.

**The coil winder** (`registry/ModCoiling`, `coiling/`, `client/CoilWinderRenderer` and `CoilWinderVisual`,
`tools/gen_coiling.py`). Six blocks, `coil_winder` (andesite, unnamed by its andesite as Create's machines are) and
`copper_`, `iron_`, `steel_`, `aluminium_` and `titanium_coil_winder`, each a shaft over its casing over a cogwheel
of its metal. It is a machine of its metal (`MachineBody`): capped at its metal's top speed, weathering by its patina,
and a casing used on it makes it that metal (`CasingSwaps`), keeping what it holds. It loads its network 4 su per RPM,
as Create's millstone. Its front is open: a wooden bobbin on its shaft, drawn as octagons so it reads round as it
turns, fills in seven steps with the wire's colour (a tinted wire model, the colour given by the recipe), and a brass
guide runs along a rod over it as it winds. Its shaft goes in at either side; placed, its front faces the player, and
a shaft ahead or behind turns it a quarter, as the Pressurizer is placed. Its recipes (`mcremix:coiling`,
`coiling/CoilingRecipe`) take one item for each ingredient out of four slots that hold a kind of thing each
(`machines/KindSlots`, shared with the Pressurizer's vessel); a hand, funnel, chute or hopper puts in only what some
recipe winds, and only the coils come out. A Copper Coil (four copper wire, a stick, two cardboard) has a processing
time of 400 and a Resistive Coil (four iron wire and a stick) 300, counted down by a sixteenth of its speed a tick as
Create's millstone counts, so 2.5 and under 2 seconds at 128 RPM and half that at 256. Power Grid's two hand recipes
for them are switched off. JEI has a Coiling page (`CoilingCategory`) with every winder as its
catalyst, and Ponder a scene on every winder (`ponder/CoilingScenes`, registered by `CoilingPonderPlugin`).

**Vacuum sealing** (`pressurizer/VacuumSealingRecipe`, `PressurizerBlockEntity`, `tools/gen_vacuum.py`). The
Pressurizer now cares which way it turns: it squeezes when its shaft's top rolls away from its dial
(`PressurizerBlockEntity.squeezesAt`), and turned the other way its pump pulls the air out of its vessel. Then it seals
what lies in four new slots in its vessel, a kind of thing to each, over the heat each seal wants: Power Grid's four
bulbs from 128 RPM, its five tubes from 256 and the cathode ray tube from 512, all over Hot heat, in 100 ticks at that
speed (the CRT 200), twice as fast at twice the speed. Each takes its hand recipe's ingredients, with a glass pane for
the triode, thyratron, barretter and regulator tubes, whose hand recipes had none; the pentode is sealed from a triode.
Power Grid's hand recipes for all ten are switched off. With parts for more than one seal it seals the one that takes
the most; a hand or a funnel at the vessel's sides lays parts in, and an empty hand takes out what it has made, then
the last kind of part laid in, then its catalyst. The dial's needle drops below zero, up to 45 degrees at its metal's
top speed, into a new blue zone on the dial. New jobs for the goggles: sealing, pulling a vacuum on nothing, still
wanting parts (naming what the nearest seal lacks), and turned the wrong way for what it holds either way.

**Old Pressurizers.** One saved before 0.37.0 has no `Sense` tag. Until it turns it counts as squeezing; the first way
it turns becomes its squeezing way (saved as `Reversed`), so a build that already ran keeps running. A Pressurizer
placed new follows the rule above.

**Ponder and JEI.** The Pressurizer's intro scene now turns it forwards (its drive's signs flipped), and a second scene
on the same stage shows vacuum sealing; it is on both Pressurizers and on every bulb and tube it seals. JEI has a
Vacuum Sealing page (`VacuumSealingCategory`), drawn as the pressurizing page with the needle below zero and the shaft
turning back, the two Pressurizers its catalysts.

**Review.** One review round found the winder starting a part-wound coil over whenever its chunk loaded or its casing
was swapped, as the recipe it was winding was kept only in memory; it now saves the recipe's name (`Working`) beside
its progress, as the Pressurizer does. Three smaller changes came with it: what a winder winds past one coil goes on the
next rather than being lost, a winder tells clients what changed once a tick at most rather than at every slot it
changes, and the Pressurizer looks for what its vessel seals only when what the vessel holds has changed. The rest were
left: parts of different seals jamming four slots (a known gap below), a matching that takes the first slot an
ingredient fits with no second try (no recipe here has two ingredients one item fits), a recipe kept until the slots
change after `/reload`, and fan air still filling the feed tank while it pulls a vacuum.

**Tests.** `CoilingTests` (six) and `VacuumSealingTests` (five). `PressurizerTests.turned` now takes a signed speed and
gears up past 256 RPM with the motor turned back, so the Pressurizer still turns forwards.

## 51. Ruins that look ruined (0.38.0)

Taran's ask of 2026-11-18, "Can you make ruined villages look more ruined?", with five cards answered the same day:
each village its own age (a few nearly whole, a few mostly fallen, most in between, and its houses ranging from a
holed roof to wall stubs), rubble lying where it fell, moderate overgrowth by biome, vanilla blocks only, and less left
to salvage (half the doors gone and the rest hanging open, about half the beds, chests and workstations gone). Only
chunks generated from 0.38.0 on change; `ruinedVillages` still switches all of it off.

**Kinds and styles.** `RuinProcessor` now knows what a piece is (`Kind`: `HOUSE` under a `houses/` folder, farms and
animal pens included as villages file them; `STREET` under `streets/` or `terminators/`; `OTHER` for wells, meeting
points, lamp posts and decorations) and what grows back over it (`village/RuinStyle`: plains, taiga, savanna, desert,
snowy), both from the template's path `village/<style>/...`. A mod's village is matched by whole words of its folder's
name (`snowy_taiga` is snowy, `desert_oasis` desert, `rice_paddy` plains); where words disagree, dry beats cold beats
forest, and anything not recognised grows back as the plains. There is one processor for each kind and style
(`RuinProcessor.of`); its codec is `{kind, style}`, both optional, still never used since the processor is only added
in code. `RuinProcessor.HOUSE` and `OTHER` are the plains house and other.

**Age.** `RuinProcessor.villageAge(pivot)` is the mean of two rolls seeded by the pivot every piece of a village shares
(its first piece's), so a village's age runs 0 to 1, most villages near the middle. A house's decay
(`RuinProcessor.decay`) is its village's age give or take up to 0.35, seeded by the house's own position. Of 4,800
houses in 400 villages, a sixth came out nearly whole (decay under 0.2) and a sixth nearly gone (over 0.8).

**The collapse** (`village/Collapse`, run on houses in `finalizeProcessing`). It works on `village/PieceView`, the
piece's processed blocks by world position, which answers as a `BlockGetter` with each block turned the way the piece
is, and lets blocks be set, moved and removed (as cave air) before they are placed. A column is a room's when the run of
blocks at its top has two empty blocks under it and then a floor. A share of the house's columns, 0.1 + 0.9 x decay,
in patches about three blocks across (smooth noise over the world's x and z, seeded by the house), loses its roof over
rooms. Above decay 0.4 the tops of the walls go too, in those same patches: a column keeps less of its height as
((decay - 0.4) / 0.6)^1.5 grows, cut unevenly in two-block patches, and corners (walls meeting on both axes) are cut
0.7 as deep, so they stand longest. Then whatever was left hanging comes down: frame blocks (walls, floors, roofs,
posts, panes) must still reach the ground through other frame blocks, and anything that leans (ladders, signs, banners,
lanterns, torches, beds, carpets, plants) must still have what it leans on, a door or a tall plant both its halves.
Beds, chests, workstations and bells on a floor that gave way fall whole to the floor below, both halves of a bed or
double chest together, or are lost if there is no whole floor under them.

**Rubble.** Some of what fell lies where it landed: a third of the planks, wooden stairs and slabs as slabs or stairs
of the same wood, three logs in ten lying on their side (half of those a beam two or three blocks long), four stone
blocks in ten as cobblestone, mossy cobblestone, gravel or slabs of their family (stone bricks also cracked stone
bricks; sandstone sand and sandstone slabs; diorite, smooth stone and bricks their own slabs), a few terracotta blocks,
half the snow blocks as one to three snow layers, and wooden trapdoors lying flat. Off a roof over a room a block lands
on the floor under the hole; off a wall, an eave or a gable it tumbles one block to a side rolled for it, onto the floor
inside or the ground outside (up to a block outside the house), but never up on what is left of the wall beside it.
Rubble never lands on salvage, never fills a doorway or the way into one (a two-high gap between walls, or the spot in
front of a door), never puts snow on ice, and piles at most two deep.

**Salvage, doors and windows.** The new block tag `#mcremix:ruin_salvage` (beds, chests, trapped chests, barrels,
every workstation but the bell, cauldrons, brewing stands, furnaces, smokers and blast furnaces): 0.25 + 0.5 x decay of
them are gone from a house, so about half overall, a bed's or double chest's halves going together. Bells stay. Half
the doors are gone, both halves together (rolled by the lower half's position); the rest hang open. A house's windows
break 0.4 + 0.4 x decay of the time, the rest of a village's 0.4 as before. Doorways are found from the template's own
blocks (`originalInfos`, placed by `StructureTemplate.calculateRelativePosition`), so a doorway whose door was taken is
still kept clear.

**Overgrowth** (`village/Overgrowth`), more of it the more a house has decayed (growth 0.5 + 0.5 x decay). The floors
of rooms that lost their roofs grow over by style: in the plains moss blocks, moss carpets and mossy cobblestone, then
grass, tall grass, flowers, ferns and oak or birch saplings; in the taiga moss, podzol, ferns, large ferns, sweet berry
bushes and spruce saplings; in the savanna coarse and rooted dirt, grass, tall grass, dead bushes and acacia saplings;
in the desert sand over the floor and drifted two high against walls half the time, with dead bushes; in a snowy
village one to three snow layers (more against walls) on everything open to the sky, roofs and wall tops included,
never on ice or packed ice. Vines climb two to five blocks up walls from the bottom course (plains most, savanna
fewest, none in deserts or snow), and moss creeps over the lowest two courses of stone and over stone left open to the
sky. Fields run to weeds: 0.25 + 0.35 x decay of the farmland is back to grass or coarse dirt (sand in a desert, snowy
grass in the snow) with grass, ferns, flowers, dead bushes or snow where the crop was, and the rest grows its crop at
mixed ages. Saplings and berry bushes never grow in a doorway or the way into one. The old quarter of crops turned to
grass is gone; fields do that now.

**Streets** (`Kind.STREET`): a quarter of the dirt path is back to grass and a tenth more coarse dirt (not in the
snow), a desert street's smooth sandstone a fifth sand. Green villages grow weeds on a third of a street's grass, and a
snowy village's grass is snowy, with a snow layer on it.

**Same in every chunk.** A village piece is placed chunk by chunk, each chunk running the processors over the whole
piece. Every roll is seeded by a world position, and the world is only read inside the chunk being placed, which the
piece has not touched yet: `PieceView.free` and `worldAt` answer from the world there and, beyond it, take a spot
within the piece's extent for air the template left out and anything outside it as unknown. So a choice that needs the
world is made in the one chunk that places that block, and a house across a chunk border falls in the same way on
both sides.

**Burnt houses** (with Immersive Weathering) keep 0.6.0's look, and are stripped of salvage like the rest.

**Review.** One review round. Fixed, each with a test: ground or another piece reaching into a house where its
template has air was taken for air, so moss, plants, cobwebs or rubble could replace it (it is read from the world now,
inside the chunk being placed); a doorway whose door was taken was found by pairing the template's blocks with the
processed ones by index, which another processor's `finalizeProcessing` could upset (it is placed by position now); a
mod's village folder was matched by letters inside its name, so `rice_paddy` was icy (whole words now). Also tidied:
every position-seeded roll goes through `Collapse.roll`, the burnt-house salt named. The run with companions then
caught a tall grass in a savanna house left without its top half when the wall round it crumbled (vanilla's savanna
and plains houses keep tall grass, the taiga's large ferns): what kept a door's halves together now keeps anything in
two halves together, and `tallPlantsFallWhole` checks it. Left: rubble tumbling out of a
house can land in a neighbouring piece's space and stay there if that piece has air there (a known gap); burnt houses'
fallen roofs still leave what hung from them, as in 0.6.0; working out each house's decay once per pane and each
column's top again where it is needed, which costs little next to placing the piece. A vine said to be overwritten at
an inside corner is not: a spot holding a vine is not free.

**Tests.** `RuinTests` has nine new tests: `villagesAgeTogether`, `housesFallInWithDecay` (two plains houses, nearly
whole at the least decayed spot on the platform and stubs at the most), `ruinsAreTheSameAcrossChunks` (the plains
library placed whole and in four parts), `ruinsGrowOverByBiome`, `aboutHalfTheSalvageIsGone`,
`streetsGoBackToTheLand`, and from the review `ruinsLeaveTheGroundInThem`, `villageStylesComeFromFolderNames` and
`tallPlantsFallWhole`. A house is checked by placing it as built, then as a ruin on the same spot, and listing what is
wrong with the ruin that was not with the house: blocks that cannot survive where they are, sand or gravel with
nothing under it, solid blocks with nothing holding them, beds and doors missing a half, a doorway blocked. Of the four
it had, `ruinProcessorDecaysBlocks` now also counts doors gone whole or left open, and `ruinedVillagesCanBeSwitchedOff`
checks a door is left alone. Shares that hang on rolls are counted over spots far off, the same every run: one street
placed on the platform turned only one block of its 45 to sand in one layout, so `streetsGoBackToTheLand` checks its
street keeps its blocks and counts grass, coarse dirt and sand over a thousand spots.
`CompanionTests.someVillageHousesBurnedDown` now expects a burnt house's door and bed gone whole or left whole, the
door open.

In game (new worlds; `/place structure minecraft:village_plains` and the desert, savanna, snowy and taiga ones build a
whole village where you stand): shots in `builds/screenshots/0.38.0`.

## 52. The quest book (0.39.0)

Taran's ask of 2026-11-18, "Can you write a quest book that walks you through the progression but leaves room for
specific solutions to be determined by the player? Use FTB Quests.", with four cards answered the same day: nothing
but progress (no rewards, and no XP while enchanting is redesigned), show the whole road (every quest in a chapter
visible from the start, locked until the ones before it are done), inside the mod jar, and the idea, not the recipe
(each quest names a goal, why it matters and the rule behind it, and points to JEI and Ponder; never a layout or a
recipe).

**The book.** `tools/gen_quests.py` holds the whole book as data and writes it in FTB Quests' own SNBT (FTB Quests
2001.4.x for 1.20.1: file version 13, quest text inline, no lang files) into `src/main/resources/mcremix_questbook/`:
`data.snbt`, `chapter_groups.snbt`, one file per chapter under `chapters/`, and `index.txt`, which names the book's
version (16 hex digits of a SHA-1 over every file) and its files. It lives outside `assets` and `data`, so
`gen_assets.py` never wipes it. Run the script from the project root after any change. It refuses a dependency on a
quest not in the book, two quests on one spot, or a clash of ids, and warns of dependency lines that cross or run
through another quest. Ids are hashed from each chapter's and quest's key (`make_id`), so they never change between
versions and players keep their progress when the book updates; renaming a key loses that quest's progress.
`{Warm}`, `{Hot}`, `{Searing}`, `{Blazing}` and `{Cold}` in text become the heat words in colour (`&e`, `&6`, `&c`,
`&b`, `&3`), and a paragraph starting with `@` is a grey pointer to JEI or Ponder.

**Chapters.** Fifteen in order (The Hearth, Turning, Bloom Iron, Steel, Molten Metal, Tar Spirit and Glass,
Electricity, Aluminium, The Steel Furnace, Heat and Cooling, Titanium, The Steel Column, Crude Oil, Liquid Air, Sulfur
and Dyes) and a Side Roads group (Ruins, Decor): 130 quests, each chapter's turning points drawn as big hexagons.
A task is an item in the inventory (FTB Quests ticks it on any inventory change), a block seen working (an observation
task on a block state: a lit Furnace, a Searing Furnace, a Searing and a Blazing Steel Furnace, an Electrolyser with
both electrodes, a formed big crucible, an air column grown icicles; or on the new block tag `#mcremix:wooden_shafts`,
which `gen_assets.py` writes beside the item tag), the village structure tag for the ruins, or a checkmark where
there is nothing to hold (a gas, a column drawing every cut). Substitutes and side lines (slag, weathering, wood, the
big crucible, petroleum coke, paraffin wax, gunpowder, the pale shades and the like) are optional, so a chapter
completes without them. A quest that needs another chapter's step first waits on that quest: the first review found
eleven that opened before what their recipes take could be made. Seven wanted a pressed sheet, so they now wait on the
Mechanical Press: the goggles, Create's steam engine, the mechanical pump (fluid pipes), brass and the vat's copper
mixer (a whisk), caustic soda (a mixer and a fan) and iron casing. The press and the drill wanted andesite casing,
Power Grid's generator parts a brass Mechanical Crafter, and its heating coil a coil from the coil winder. The goggles
left the shafts quest for one of their own after the press.

**Second review** (the last round). Every quest's claims were checked against the code, and each finding was checked
again by a second reader before anything changed: 73 held up (many of them the same point found twice) and 12 did not.
What changed, in about forty quests:
- Recipe details out, since JEI has them: the furnace's eight cobblestone, two logs to six shafts, the iron anvil's 31
  iron, the stone anvil's stones, the cementation pack's clay, four blister steel, the casings as sheet on stripped
  log, the coil winder's stick and cardboard, two pellets to a bushing, four platinum nuggets to a gauze, the
  gunpowder ratio and the briquette's pitch.
- Claims the code contradicts: a Furnace heats a basin or boiler above it by the fuel burning in it, and heat from
  under it passes up as it is (a soul campfire beneath only warms or simmers); a part's own material sets how far it
  reaches from a support; raw iron, iron ore and ancient debris aren't fired at Hot ("Raw Iron Won't Smelt", since a
  crucible does melt it); the crucible melts iron, steel and netherite at any Blazing heat, Power Grid's basin heater
  included; lab glass cracks at Searing itself; the sea gives salt water even where it snows; only the Steel
  Condenser's jacket refuses liquid nitrogen (the still's coil condenser takes it); titanium needs twice aluminium's
  cooling (it makes about three times the heat); the air column draws argon from a section of its own between
  nitrogen and oxygen, spare sections sitting under the top; reflux stands in for the empty sections between close
  cuts but makes no room for a missing cut; titanium sponge needs no steel vat; a diazo salt keeps where it is cold
  enough to snow; wax waxes what honeycomb waxes. DESIGN.md said the same as the old text about titanium's heat, the
  crucible and the steel vat, so those three lines now follow the code too.
- Unlock order: charcoal opens straight off the Furnace (a campfire under it is Warm with nothing to light) and Hot
  waits on both charcoal and lighting. No dependency can gate the first lighter, since flint and steel takes iron and
  iron comes later, so the light quest says where one turns up: ruined portal chests hold flint and steel and fire
  charges, and mineshafts, dungeons and shipwrecks iron (only village chests lose theirs). Coal tar, wood spirit and
  salt water now wait on iron (a bucket or a pipe), weathering on iron (shears, for honeycomb), the golden apple on Hot
  (gold), salt on the heated basin, Blazing on chlorine (hydrogen), PVC on polyethylene (ethylene), naphtha cracking on
  the Steel Furnace's fuel quest, and sulfur on bauxite liquor (red mud).

**Installing it** (`compat/QuestBook`, called at common setup). Without FTB Quests loaded it does nothing. With it,
it reads the book from the jar and looks in `config/ftbquests/quests`. With no `chapters/*.snbt` there (FTB Quests'
own empty book has none) it writes the book and `mcremix_book.txt`, which holds the version and a SHA-1 of each file
written. With the marker there and the same version, nothing. With an earlier version, it replaces the book only if
every file it wrote still matches its hash and no chapter was added, deleting only the files it wrote; otherwise, and
with chapters but no marker, it leaves the book alone and logs that deleting the folder brings the mod's book back. A
marker naming a path outside the folder is refused. FTB Quests saves its book only when it is edited: in the dev
client, playing, completing quests, and saving and quitting left every file matching, and quest progress lives in the
world (`saves/<world>/ftbquests`). A restart with a newer book logged "Updated the Minecraft Remix quest book", and
the world's three completed quests were still complete.

**Seeing it.** `./gradlew runClient -Pquests` (or `runGameTestServer -Pquests`) also loads FTB Quests 2001.4.22, FTB
Library 2001.2.13 and FTB Teams 2001.3.2 from `maven.ftb.dev`; Architectury is already there for Power Grid. In game,
`/ftbquests open_book` opens the book.

**Tests.** `QuestBookTests.questBookNamesThingsThatExist` reads the book from the jar and checks that every item and
icon it names is registered, every block state it looks for parses against its block, every block tag it looks for
has blocks, the structure tag exists, no id repeats and every dependency is in the book.
`questBookInstallsOnlyOverItsOwnUnchangedBook` runs the installer in a temporary folder through each case above:
installing into an empty folder and into FTB Quests' empty book, leaving the same version be, updating an unchanged
earlier version (and deleting a chapter the new book drops), keeping a changed book, a book with a chapter added and
someone else's book, and refusing a marker that points out of the folder. FTB Quests' own detection was checked by
hand for one quest of each kind (a checkmark, an item, a look-at), not for every quest.

**Known gaps** (left, per the one-review rule): a crash partway through replacing the book leaves files that no
longer match the marker, so the next start keeps them as a changed book until the folder is deleted; a later FTB
Quests that rewrites older books on load would make every book look changed, so updates would stop until the folder
is deleted; the text is English only.

## 53. Shorter, livelier Ponder and quest text (0.39.1)

Taran's ask of 2026-11-22, "For ponders and quest descriptions, can you make explanation more consise, state the
obvious less, and make them sound fun without being cringe?", with one card answered the same day after samples:
more playful (more personality in the quests and a few more asides in Ponder, still no jokes for their own sake).
Nothing but text changed.

**Ponder.** 335 of the 373 text boxes in this mod's 54 scenes are reworded, 14% fewer words in all, and no box is
over 30 words now (41 were). Out went what the scene already shows, what JEI lists (recipe amounts, every fuel) and
lines that repeat the scene's title; in came a short aside here and there. Every scene keeps its boxes, timings and
keyframes, so the lang keys (`mcremix.ponder.<scene>.text_N`) are the same: only the strings in the `*Scenes.java`
classes and their values in `en_us.json` changed, and `ponder_lang()` in `gen_assets.py` gives the same values from
the Java. Create's own scenes copied into this mod are untouched. Two scenes were also put right: the Steel Furnace
scene now gives the fuel ladder as the code has it (fuel oil goes furthest, then coal tar, light oil, creosote,
ethylene and fuel gas; hydrogen, ethylene and fuel gas burn Blazing) and leaves the full list to JEI, and naphtha
cracking's pressurised steam comes from a Pressurizer, not a boiler.

**Quests.** 124 of the 130 quests' descriptions and three chapter subtitles (The Hearth, Turning, Electricity) are
reworded in the same voice, 5% fewer words. The quest text was already lean, so most of the change is voice, with
lines that repeat another quest or the chapter's subtitle taken out (Bakelite's list of Power Grid parts, the
transformer's steel core, steel as iron that soaked up carbon). Keys, titles, quest subtitles, tasks, dependencies and
layout are unchanged, so ids and progress carry over, and every number, heat word and JEI or Ponder pointer is kept.
The voice is written down at the top of `tools/gen_quests.py` for later edits. The book's version is now
`5646b40b394c4a29`, so `compat/QuestBook` replaces an installed 0.39.0 book that nobody has edited and leaves an
edited one alone.

**Known gaps:** the text is still English only, and none of it was read in game; the strings are the same keys and
the same SNBT fields as 0.39.0's, which were.

## 54. Power Grid's parts made as real ones are (0.40.0)

Taran's ask of 2026-11-25, "Can you make the recipes more realistic for Create: Power Grid?". Claude surveyed all 116
of Power Grid 0.6.2's recipes (21 already changed by 0.14.0, 0.16.0 and 0.37.0) and proposed about 40 more, row by
row, in the design folder's `power-grid-recipes.md`, then asked seven cards. Taran answered all seven the same day:
every part made as real ones are, with the fantasy gadgets left alone (the pick); silicon purified the real way (not
the pick, which was arc-furnace silicon alone); real gases in tubes, neon from the air column (the pick); wire drawn on
the Coil Winder (the pick); PVC insulation (the pick); capacitors of aluminium and waxed paper on the Coil Winder (the
pick); and the battery as Volta's pile in salt water (the pick), answered in words with a new ask: "also add a high
capacity battery that uses the sodium and sulfur recipe". That battery had four cards of its own, which Taran answered the same day:
heat, capacity and recipe as Claude picked them, and the look as Power Grid's Battery in dark steel, not the grey
painted cabinet Claude picked (below).

**Recipes** (`tools/gen_powergrid.py`, run last by `gen_assets.py`, and the motor and integrated circuit in
`gen_assets.py`). Power Grid's recipes are replaced by files of the same name under `data/powergrid/recipes`, and
switched off with a `forge:false` condition where this mod makes the part another way:

| Part | Now |
|---|---|
| Copper, iron and gold wire | drawn on the Coil Winder, an ingot into four (`coiling/*_wire`); the saw recipes are off |
| Insulated copper wire, copper cord | four wire and a PVC sheet make four; a cord is two in a PVC jacket |
| Magnet | a steel ingot magnetised (`magnetization/magnet`); andesite alloy's is off |
| Generator rotors, housing | steel plate laminations round copper coils; steel plate housing |
| Electric motor, commutator | coke brushes |
| Resistor, potentiometer, varistor | coke on Bakelite with copper leads, four at a time; a carbon track under a copper wiper; zinc oxide between two copper wire |
| Capacitor | two aluminium sheets, two paper and a wax rolled on the Coil Winder into four (`coiling/capacitor`) |
| Battery | three layers of copper and zinc plate wet with 250 mB of salt water; `mixing/acid` is off |
| Unetched circuit board | a wax resist (honeycomb or paraffin), deployed, in place of dough |
| Relays, gauges, multimeter, device and wire connectors, factory light | iron, Bakelite, brass, glass, the magnet and a vacuum-sealed light bulb; the multimeter needs no Gizmo |
| Diode, VFET, BJTs, integrated circuit, solar panel | made on a silicon wafer, the BJTs with gold wire, the IC with gold, copper and polyethylene, the panel in polyethylene and aluminium under glass |
| Tubes and bulbs | iron plate anodes, iron wire grids, quicklime cathodes, phosphor for the CRT, and a gas for five of them (below) |

The FE Inverter, Electrical Gizmo, Electro-Baton, Electro-Zapper and Nether Transformer keep Power Grid's own recipes
(card). A Resistive Coil is now wound on a Brick in place of a Stick.

**New things** (`registry/ModPowerGrid`, `fluids/Chemical`). Seven items: zinc oxide (zinc nuggets fired at Searing),
phosphor (two zinc oxide and quartz mixed superheated into willemite), the quartz briquette (quartz, coke and 50 mB of
pitch pressed), crude silicon, polysilicon, the silicon ingot and the silicon wafer. Three fluids: crude
trichlorosilane, trichlorosilane and neon (a gas). Each has a structural formula: ZnO, Zn2SiO4, SiHCl3, and Si and Ne
alone (`StructuralFormula` colours neon a paler blue than argon).

**The silicon chain.** The Arc Furnace reduces a briquette to crude silicon for 32 kJ and melts polysilicon into an
ingot for 16 kJ. Crude silicon is a catalyst in the Steel Furnace at Hot: 300 mB of hydrogen chloride over it gives
100 mB of crude trichlorosilane and 100 of hydrogen, and it wears away an item a batch (`catalyst_lasts` 300). The hot
tube runs it too: it now takes any Steel Furnace job with a catalyst at Hot or below that takes one fluid and lays
nothing down (`HotTubeBlockEntity.runs`). Crude trichlorosilane distils at Warm into 950 mB of trichlorosilane and 50
of ferric chloride. **Steel Furnace jobs may now lay a solid down** (`SteelFurnaceRecipe.deposit`, its one item
result): trichlorosilane with hydrogen piped in as its second feed lays a polysilicon in the tray at Blazing, a hopper
or hand taking it out as fired things are, and gives back 300 mB of hydrogen chloride; a full tray stops the job as a
full tank does. A saw cuts an ingot into eight wafers. One crude silicon makes about 0.95 polysilicon, so a briquette
is about seven and a half wafers, and the hydrogen chloride and hydrogen go round.

**Gas-filled tubes** (`PressurizerBlockEntity`, `VacuumSealingRecipe.gas`). The vessel has a gas tank of 100 mB. A
seal may take one fluid ingredient, its gas: the barretter and thyratron 10 mB of hydrogen, the neon bulb and regulator
tube 1 mB of neon, the growth lamp 5 mB of argon. Piped in, neon and argon go to the gas tank; hydrogen, which ammonia
takes as its second feed, goes there only while the vessel holds parts of a hydrogen-filled seal. Of the seals what it
holds is enough for, gas included, it seals the one that uses every kind it holds, then a gas-filled one, then the one
with the most ingredients. With nothing in the vessel that is sealed in the gas it holds, a pipe can draw the gas
off. Goggles show the gas held and, short of it, "Still wants 10 mB of Hydrogen to seal". A Steel Furnace whose tray
is full says to take out or draw off what it made.

**Neon from the air** (`Column`, `DistillingRecipe.trace`). A distilling recipe may carry a trace too little for a cut,
`"trace": {"fluid": ..., "every": ...}`: liquid air's is a mB of neon for every 55,000 mB split. The column counts what
it splits (`TraceOwed`, saved) and gathers the trace in a 100 mB tank at its very top; in the air column what has no
room there escapes over the top. Only a pipe on the top face of the top section draws it off
(`ColumnSectionBlockEntity`'s `UP` capability), and nothing goes in that way. Goggles show it, and JEI's liquid air
page shows it at the end of its bar with how many buckets make a mB.

**The Coil Winder** draws wire (an ingot, 160 ticks of work) and rolls capacitors (320). Of the recipes what it holds
is enough for, it winds the one that uses everything it holds (`KindSlots.usesEvery`), then the one with the most
ingredients, so the parts of a coil and an ingot wind the coil first. JEI hides Power Grid's Blazing Acid and its
bucket.

**Ponder and quests.** The coil winder scene gains wire drawing and capacitors and winds the Resistive Coil on a
Brick; the vacuum sealing scene gains a line on gas-filled tubes. The quest book gains four quests (crude silicon,
polysilicon and wafers in the titanium chapter, neon in the liquid air chapter), the Power Grid quest asks for the
Coil Winder and drawn wire, and the coils, salt water, vacuum and argon quests say what changed.

**The sodium-sulfur battery** (`battery/SodiumSulfurBatteryBlock`, `SodiumSulfurBatteryBlockEntity`, registered in
`ModPowerGrid`). It subclasses Power Grid's `BatteryBlock` and `MultiBlockBatteryEntity` with its own block entity
type and `SimpleBatterySpec`, so it is Power Grid's Battery in every other way (joining up to 3 by 3 by 5, the Device
Connector, its charge kept on the item, Display Links) and joins only its own kind. Its spec: four times Power Grid's
`acidBatteryCapacity` a block, and it comes full (the sodium is the charge); 12.45 to 12.6 V from full to 40%, then
down to 10.8 V empty; 0.05 Ω plus a term that only matters in the last tenth (Power Grid's is 0.1 Ω full and over
100 Ω at a fifth). Its heat settings (150 W, mass 3) come from a `ThermalValues.Provider` it registers, since Power
Grid's `CThermal` only knows Power Grid's blocks. The controller checks every 10 ticks that every block under its
bottom layer has Warm heat or hotter (`LabGlass.heatUnder`, so a Furnace below counts); `updateParameters` sets the
spec's voltage and resistance (divided by the battery's size) while hot and 10 kΩ, Power Grid's own highest, while
cold, even when empty, so a drained battery warmed again still charges. Goggles show its charge in Wh and whether it
is hot. The beta-alumina tube is two alumina and a soda ash compacted into an unfired tube, fired at Blazing for 400
ticks; the battery is a Steel Casing going round the assembly line twice: deploy a tube, fill 100 mB of molten sodium,
fill 250 mB of molten sulfur, press. The crucible melts a sulfur into 100 mB of molten sulfur (Warm, the heat of what it
melts into), the settling recipe backwards. It looks like Power Grid's Battery (Taran's look card): Power Grid's side and
top and their connected sheets, kept as palettes in `tools/powergrid_textures.json` (Power Grid is Apache-2.0), with
the copper ribs sulfur yellow shade for shade, the grey frame dark steel (`gen_decor.STEEL`) and the pale panels
beside the ribs as they are. They join across the battery with Create's RECTANGLE connected textures as Power Grid's
do (`ModSpriteShifts.SODIUM_SULFUR_BATTERY` through `ColumnCTBehaviour`). A Ponder
scene (`BatteryScenes`, a two-high battery on a magma block) and an optional quest in the sulfur chapter (135 in all)
go with it.

**A ruins test made sturdier.** With this version's tests added, `ruinsGrowOverByBiome` (0.38.0) sat, in a run with
companions, where its worst-off taiga house fell in almost entirely (a sixth left standing) and grew nothing of the
taiga kind, so it failed every such run.
Each kind of house is now placed at the two spots on the platform where it would be worst off, and what grew in both
is counted together (`RuinTests.worstOff`).

**Known gaps:** the sodium-sulfur battery's temperature, as Power Grid's thermometer reads it, is the heat of its own
current, not the heat under it; hydrogen left in the gas tank seals light bulb parts as barretters until it runs out (a pipe can draw
it off once no hydrogen tube's parts lie in the vessel); argon's half again in the Arc Furnace applies to silicon
melts as it does to sponge; and the Arc Furnace's charge is drawn as titanium sponge whatever it holds.

## 55. Flint tools and bread from the mill (0.41.0)

Taran's ask of 2026-11-26, "Can you add bread from the mill and flint tools to replace wood and stone?", two ideas
from the idea bank's third round. Claude checked Create's bread chain first (milling a wheat gives 1.5 flour on
average; dough is flour and a water bucket, a mixer's 1000 mB or a fan's splashing, all of which need iron; dough
bakes into bread on a campfire, smoking or smelting; Create's cake takes dough; the millstone is a cogwheel, andesite
casing and stone, so it comes with andesite alloy) and asked eight cards. This version was built on Claude's pick for
each while Taran's answers were pending, and Taran then chose seven as picked: bread, cake and cookies need flour; no
hand quern; a water bottle makes dough; flint is knapped on stone; a knife in place of the sword; sharp but brittle;
wooden and stone tools gone everywhere. The eighth went the other way (2026-11-27): rather than punching trees, logs
need a flint hatchet, so the version was changed before it merged (Logs need an axe, below).

**Knapping** (`flint/Knapping`, a `PlayerInteractEvent.RightClickBlock` handler). Flint used on a block in
`#mcremix:knapping_stones` (the overworld's and the nether's base stone, `#forge:cobblestone`, `#forge:stone`, smooth
stone and the three stone anvils) is struck on it: the click is taken on both sides, and the server counts strikes by
striker and block, starting again when the striker moves to another block. The fourth strike in a row
(`STRIKES_NEEDED`) uses up a flint and gives Knapped Flint (into the inventory, or dropped). Each strike throws flint
and block particles and plays stone and pot clicks that rise in pitch; the last breaks with a pot's crack and crits.
The event is cancelled and its use-block and use-item results are both denied, because Create's Deployer fires the
event but reads those results rather than the cancelling; so a Deployer holding flint strikes once a push and leaves
the flake in its overflow. JEI has a Knapping page (`KnappingCategory`, laid out as `BloomCategory`: flint jumping on
a stone block, "4 strikes on stone"), with the common stones as catalysts, and Ponder a scene (`FlintScenes.knapping`:
gravel dug for flint, four strikes, the flake) on flint, Knapped Flint and every flint tool.

**Flint tools** (`flint/FlintTools`). A tier registered with `TierSortingRegistry` after stone and before iron, so it
mines whatever stone did (iron ore needs only stone) and nothing more; its own tag, `mcremix:needs_flint_tool`, is
empty. Level 1, 65 uses (stone has 131), speed 6 (iron's), stone's attack bonus and enchantability, mended with
Knapped Flint. The pickaxe, hatchet (an `AxeItem`), shovel and hoe take stone's attack numbers; the knife is a
`SwordItem` of 2 and -2 (4 damage at 2 attacks a second, against the stone sword's 5 at 1.6) that cannot sweep
(`FlintKnifeItem` refuses `SWORD_SWEEP`). Recipes are vanilla's shapes in Knapped Flint and `#forge:rods/wooden`, but
the hatchet is 2x2 (`KK`, `KS`: three flakes and a stick) so that it crafts in the inventory, and the knife is a flake
on a stick; each has a recipe advancement that unlocks it with the first flake. They are in
`#minecraft:pickaxes`, `axes`, `shovels`, `hoes` and `swords` (so `#minecraft:tools` and decorated pots), the pickaxe
in `#minecraft:cluster_max_harvestables`, and `#mcremix:flint_tools`. Better Combat, where installed, gives the knife
its dagger preset, the hatchet its axe and the pickaxe its pickaxe (`data/mcremix/weapon_attributes`). Textures are
drawn in `tools/gen_early.py` from ASCII sprites: a dark glassy flint head lashed with cord to a stick.

**Wooden and stone tools gone** (`flint/OldTools`). Their ten recipes are off (`forge:false`) and the creative tabs
leave them out (`OldTools.fillTabs` puts the flint tools where the stone ones were in the tools and combat tabs), so
JEI, which builds its list from the tabs, does too; removing them from JEI as well only logged an error for each. `FlintTools.replace` gives the flint tool for one of them, keeping count, tag (name, enchantments) and
wear as the same share of its uses; it is used by a global loot modifier (`mcremix:swap_old_tools`,
`SwapItemsModifier`, last in the list) on every loot table, by `EntityJoinLevelEvent` on a mob's hands (a wither
skeleton's stone sword), and by `VillagerTradesEvent`, which wraps every trade so a toolsmith's stone tools come out
flint. Vanilla's `story/mine_stone` and `story/upgrade_tools` advancements show and ask for the flint pickaxe, and
`husbandry/wax_off` counts the flint hatchet.

**Logs need an axe** (Taran's card, `flint/LogsNeedAnAxe`). A `PlayerEvent.BreakSpeed` handler sets the dig speed to 0
on a block in `#mcremix:needs_an_axe` (which holds `#minecraft:logs`: logs, wood, stripped logs and wood, stems and
hyphae) unless the main hand can `AXE_DIG` or is the correct tool for the block, so hands and other tools make no
headway, on the client (no cracks) as on the server. Creative breaks as ever, never asking the dig speed. A Deployer's
player digs at the same speed, so it needs an axe in hand too, while a Mechanical Saw and explosions break logs with
none. Starting on such a block without an axe (`LeftClickBlock`, action START, on the server, a player in survival)
puts "That log needs an axe. A flint hatchet will do" on the action bar, once a click. Leaves give the hatchet its
stick: a global loot modifier (`mcremix:leaf_sticks`, `LeafSticksModifier`, `chance` 0.1, with `survives_explosion`)
adds a stick a tenth of the time to any `#minecraft:leaves` loot that holds no leaves block (shears and silk touch take
the block, and no sticks with it, as in vanilla), on top of vanilla's 1 in 50: about one leaf in eight. Vanilla's first
tutorial hint ("Punch it to collect wood") says "Its logs need a flint hatchet". Known gaps: a spawn far from gravel
means a walk before the first hatchet (a ruin's planks and the bonus chest are the other ways in), and logs placed as
decoration need an axe to take down too.

**Bread** (`tools/gen_early.py`, `food/KeepBottleRecipe`). Vanilla's `bread` and `cake` recipes are off and `cookie`
takes `#forge:flour/wheat` in place of wheat. Everything else is Create's: milling or crushing wheat, dough from a
water bucket, mixing or splashing, and dough baked on a campfire, smoked or smelted, and its cake made with dough.
A new recipe type, `mcremix:crafting_shapeless_keep_bottle`, is vanilla's shapeless crafting that hands back a glass
bottle for each potion it uses; flour and a water bottle (`forge:partial_nbt`, Potion `minecraft:water`) make dough
with it, so the first bread needs only glass.

**Quests.** The Hearth chapter opens with Flint Tools (a flint pickaxe, the hatchet first), which the campfire and Furnace quests now
follow, the rest of the chapter moved a column right; Turning gains Bread from the Mill (flour and bread) after
Andesite Alloy, and says a millstone grinds gravel into flint every time. 137 quests.

**Known gaps:** wooden and stone tools already in inventories, in chests opened before 0.41.0 or in item frames stay
as they are (they still work); a player holding flint over stone cannot place a torch from the offhand onto it with
that click; knapping progress never runs out, so three strikes now and one an hour later still make a flake.

## 56. Realistic ores (0.42.0)

Taran's ask of 2026-11-27, "Can you add realistic ores and coordinate it with my other mod Exploration Remix so that
they look good in each biome?", with nine cards (design/realistic-ores.md): replace vanilla's ores, process each
mineral its own way, a few big ore bodies, no hints at the surface (not Claude's pick: no gossan, no pan), both mods,
deeper is richer. Three cards (which minerals, their looks, the old sources) were still open when this was built, so
it follows Claude's pick for each.

**Minerals and their rocks** (`ores/Mineral`, `ores/Host`, `registry/ModOres`). Eleven minerals (bog iron, hematite,
magnetite, malachite, chalcopyrite, native copper, sphalerite, gold quartz, ilmenite, pyrite, native sulfur), each
with the list of rocks it occurs in. Every pair is an ore block, `<rock>_<mineral>_ore` (plain stone's is
`<mineral>_ore`), 111 in all over 31 rocks: vanilla's stones, both sandstones, seven terracottas, the soft ground bog
iron lies in, and Create's limestone, asurine, crimsite, ochrum, veridium, scorchia and scoria, which Exploration
Remix builds its layers from. An ore copies its rock's block (`Host.like`: for Create's stones the vanilla block
Create builds them on, with the stone's map colour), 1.5 harder (0.5 for soft ground), and needs the right tool;
basalt's ores are pillars and keep the basalt's axis. Bauxite, rock salt and ilmenite sand are rocks of their own
(`OwnRock`). `ModOres.ore(mineral, host)` gives the block or null. Everything else about them (textures drawn on each
rock's own texture in the mineral's habit, models, loot, tags, recipes, the biome tags and lang) is written by
`tools/gen_ores.py`, which reads the two Java enums so the lists live in one place; `gen_assets.py` runs it.

**Ore bodies** (`ores/Formation`, `ores/Body`, `ores/OreBodies`, `ores/OreBodiesFeature`). Each formation has a grid
(`cell`), and each cell holds one body or none (`chance`, by the biome on the ground over the cell's centre where the
formation asks for one, through `SurfaceClimate`, the overworld's climate read at depth 0, so a body under a mountain
belongs to the mountain whether or not that column is generated). A body's place, size, shape and twist are hashed
from the seed and its cell (`Hash`), so the feature, placed once per chunk at `underground_ores` with no placement
(`forge:add_features` on `#minecraft:is_overworld`), draws only the chunk's own blocks of every body that reaches it,
and a body comes out whole in any order of chunk generation. Plans are cached in a small lock-free table keyed by
dimension, seed and cell. `Body.at` says what a block is (nothing, the mineral, its accessory, the halo, the weathered
cap) and `Body.place` makes it the ore of the rock the block is, through `Rocks`, a block-to-rock table built the first
time it is asked; rock the mineral does not occur in is left alone. Shapes: lens, bed (wide, thin, waving), vein
(steep, pinching and swelling), stock (porphyry's tall web of veinlets in a pyrite halo, malachite within 18 blocks of
the ground, its last 3 leached bare), pods on a string, sheets, seams and blanket (bauxite, under the soil, never the
grass). A body set by depth comes up no higher than one block under the ground (`OreBodies.shallowest`), so a shallow
lens never lies open on flat ground (the no hints card); ilmenite sand, which lies on black beaches, is the one that
reaches the top block. Formations are kept out of chunks they cannot reach (`Mode.HEIGHT` bounds), and the deep ones
(`*_DEEP`, at the depths of Exploration Remix's layers) out of worlds that do not reach those depths. After the bodies: pyrite's few
small clusters of cubes in every chunk's rock, native sulfur crusting about a third of the rock round lava and magma
below y 0, and any vanilla iron, copper or gold ore (or raw iron or copper block, or Create's zinc ore) still in a
section, found cheaply with `PalettedContainer.maybeHas`, turned into the real mineral in the rock most of its
neighbours are (`OreBodies.realOre`).

**Vanilla's ores out.** `forge:remove_features` takes vanilla's iron, copper and gold ore features and Create's zinc
ore out of the overworld; the conversion pass catches the rest (vanilla's big noise veins, other mods' and Exploration
Remix's own veins). Coal, lapis, redstone, emeralds, diamonds and the nether's gold stay.

**Processing** (`tools/gen_ores.py` recipes and tags). Bog iron, hematite and magnetite are
`forge:raw_materials/iron` and fire Searing into a bloom; malachite and native copper are raw copper (malachite gives
off carbon dioxide); chalcopyrite, sphalerite and pyrite are roasted Hot first, into copper matte, zinc calcine and
hematite, giving off sulfur dioxide (which the Furnace's tank takes, or vents as the 0.35.0 cloud), and are kept out of
the raw-material tags so Create's crushing and the crucible cannot skip the roast. The iron, copper and gold minerals'
ore blocks are in vanilla's `minecraft:iron_ores`, `copper_ores` and `gold_ores` block tags (piglins guard gold by
them, and other mods key on them), but not the item tags, which would hand them to vanilla's and Create's raw ore
recipes. Gold quartz is raw gold and needs an
iron pickaxe; ilmenite crushes into black sand and ilmenite sand washes into it. Create crushes every ore block into
its mineral and three quarters of another. Create's zinc ore smelting is off. Washing red sand no longer gives bauxite
(the old sources card, Claude's pick); rutile from basalt and platinum from red sand stay.

**Exploration Remix.** Its deep layers (y -784 at the bottom) hold the deep formations: magnetite and porphyry in the
Blue Reaches, hematite and zinc in the Deep Gardens, gold veins through the Wasteland, native copper and ilmenite in
the Wasteland and the Galleries. Their rocks are hosts, scoria included (`Host.SCORIA`, added for the Wasteland), so a
deep body runs on through asurine, blackstone, scoria and scorchia. ER's own veins (vanilla ores in its palette) are
turned by the conversion pass; its pull request (0.23.3) has them write the real ores directly, each in the rock round
it: gold quartz in Lantern Lake, the Ember Gardens, the Wasteland and the Galleries, chalcopyrite in the Silver Fern
Rift, hematite (in limestone and calcite, which is why calcite takes hematite) in the Ivory Root Cavern, with vanilla's
ores in their place when Minecraft Remix is missing. Its `basalt_rock` and `cave_rock` tags take `#forge:ores`, so the
ores in its rock count as that rock. To see them in a dev
client, `-PexplorationRemix=<jar>` loads a jar zipped from ER's `build/classes/java/main` and `build/resources/main`
with its refmap (`build/tmp/compileJava`) and manifest, through a flat-dir repository as a plain runtime dependency: its
release jar's mixin shadows keep their SRG names when moddev remaps it, and its `rune.*` mixins trip Registrate's
unused-callback check at start-up in a dev client (only there: outside dev it is a log line), so the dev jar leaves
those out of `explorationremix.mixins.json`.

**Quests.** The Hearth gains Real Ores after Flint Tools, and the copper, iron, zinc, bauxite, black sand and sulfur
quests say where their ores come from now (`tools/gen_quests.py`): 138 quests.

**Tests** (`src/gametest/java/com/mcremix/ores/OreTests`, eight): every mineral has an ore in each of its rocks and
leaves other blocks be; each ore takes its tool (flint for most, iron for gold quartz, a shovel for bog iron, Silk
Touch keeps the block); every formation plans the same body every time and keeps it inside its bounds and its reach;
bodies set by depth stay at least a block under the ground, except ilmenite sand; vanilla's ores and Create's zinc
turn into the real minerals in the rock round them (iron ore in calcite as calcite's hematite, in diorite, which
hematite is not found in, as plain hematite); drawing a chunk far from the tests
turns only rock into ore (a magnetite lens takes its stone, glass through it stays glass, copper ore in granite becomes
granite's chalcopyrite); the bodies are in every overworld biome's features and vanilla's iron, copper, gold and
Create's zinc are not; and each mineral goes its own way to its metal, the sulfides giving off sulfur dioxide and
never melting or crushing straight into metal. `PowerGridRecipeTests` now checks that washing red sand turns up gold and
platinum but no bauxite.

**Known gaps:** worlds made before 0.42.0 keep their vanilla ores in chunks already generated; a body crossing rock
its mineral is not in (crimsite and ochrum take only hematite) leaves that rock bare; Exploration Remix's gilded
blackstone and redstone stay as they are; the conversion pass sees each chunk once, when its own features are placed,
so a vanilla ore that a neighbour's feature writes into it afterwards (a vein near a chunk edge, or another mod's ore
feature that runs after this one) stays vanilla.

## 57. Rotted wood and zombie villagers in ruins (0.43.0)

Taran's ask of 2026-11-27, "Can you add rotted wood for ruined villages and have ruined villages always spawn zombie
villagers at night?", with seven cards: every overworld wood (not Claude's pick, which was oak, spruce and acacia),
doors and trapdoors with boards missing, a stick now and then (not Claude's pick, which was the block itself, for
building only), floors that give way if you land on them, more rot the older and damper, a ruin's zombies are zombie
villagers, anywhere in the ruin. Said in the thread without a card: only ruins have rotted wood, mushrooms grow on it
as on podzol, burnt houses stay charred, and Silk Touch keeps the block whole, as it does glass.

**Blocks** (`village/RottedWood`, `registry/ModRottedWood`). `RottedWood.Wood` lists the eight overworld woods and
`Kind` the eleven blocks of each (log, wood, stripped log, stripped wood, planks, stairs, slab, fence, fence gate, door,
trapdoor), named `rotted_<vanilla id>`: 88 blocks. They are vanilla's blocks (`FenceBlock`, `DoorBlock` with the wood's
`BlockSetType`, and so on) with rotted wood's properties: a duller map colour than the wood's, hardness 0.6 (0.8 for doors and
trapdoors), wood's sounds quieter and lower. Logs and wood strip with an axe into the rotted stripped ones
(`RottedWood.Pillar`); planks, stairs and slabs give way (`Planks`, `Stairs`, `Slab`, through `RottedWood.giveWay`).
A door broken by its upper half (`RottedWood.Door`) drops what its lower half would with the same tool and takes that
half away quietly, so Silk Touch keeps it whichever half is hit: vanilla drops a door from its lower half's loot, and
breaks that half with no tool once the upper one is gone.
`RottedWood.rotted(state)` gives what a sound block becomes with its properties kept, from a table built the first time
it is asked, and `RottedWood.setup` sets fire's odds (vanilla's for the sound wood: doors and trapdoors do not burn) and
composting (one in two). Textures, models, loot, tags and names come from `tools/gen_rot.py`, which reads the two enums
from the Java: each texture is drawn from vanilla's for the sound wood (in 0.43.0 darkened, desaturated toward
grey-brown, mottled, seams opened, boards checked across the grain and split along it, soft spots, mycelium; bark fallen
away in patches on logs with a small bracket fungus; a log end's heart gone soft and cracked; doors and trapdoors with a
board missing and a rotted foot, their iron rusted; 0.43.1 drew them again more quietly, section 58), and the
blockstates and models are vanilla's own pointed at them. Loot
is a stick a third of the time, the block with Silk Touch (both halves of a double slab; a door from its lower half
only). Block tags: `mineable/axe`; `wooden_fences`, `fence_gates`, `wooden_doors` and `wooden_trapdoors`, so fences
join sound ones and villagers and zombies treat the doors as wooden; `mushroom_grow_block` for the whole blocks; and
`mcremix:rotted_wood` (blocks and items). No item tags, so nothing crafts from it and it is not fuel.

**Giving way.** `giveWay` is farmland's trampling rule at half the odds: something living of more than 0.512 cubic
blocks (a zombie, a player, not a chicken), a mob only if `mobGriefing` lets it, never a player in creative,
spectator or adventure mode, with a chance of (fall distance - 0.5) / 2. The block is destroyed with its drops; the `fallOn` that broke it
does not pass the fall on, so the landing does no harm and whatever landed keeps falling.

**Rot** (`village/Rot`, run by `RuinProcessor.finalizeProcessing` on every piece but burnt houses: a house after it has
collapsed, a street or anything else on its own). A piece's share is (0.18 + 0.3 x decay) x `RuinStyle.rot` (plains 1,
taiga 1.1, snowy 0.9, savanna 0.6, desert 0.3), decay being a house's own and the village's age for the rest. Each
block's odds are the share times its wetness (1.6 open to the sky, 1.4 on the ground, the piece's lowest two courses,
2.24 both, 0.55 under a roof) plus a patch of value noise, three blocks across in layers three high, worth up to a
quarter either way, so rot comes in patches along wet timber. A door goes by its lower half, so it rots whole. Rolls are
seeded by position (`Collapse.roll`) and the patch by the piece, so every chunk a piece crosses rots it the same.
Mushrooms then come up on one in twenty rotted logs or planks still under a roof, where there is room and not in a
doorway, a red one in four. `Overgrowth` counts rotted planks as boards, so a floor open to the sky still grows moss or
podzol after it has rotted.

**Zombie villagers** (`village/RuinSpawns`). On Forge's `LevelEvent.PotentialSpawns` for monsters, when the list has a
zombie or husk entry and the position is in a ruin, each such entry is replaced by a zombie villager entry of the same
weight and group size. The natural spawner asks for the list twice (to pick a spawn, then to check it is still allowed,
by identity), so the replacement for given numbers is always the same object. In a ruin is inside the box round all the
pieces of a structure that has a piece from a template under `village/` (read through the new
`SinglePoolElementAccessor`), found with the structure manager's references for the position's chunk. The spawner
asks at every try, so a structure is judged a village or not from the first of its starts looked at, and one that is
not (a mineshaft, a stronghold) is never loaded again to ask, as vanilla loads only structures with spawns of their own;
each village's box is worked out once per start. Both are held weakly. `zombieVillagersInRuins` (server config, on)
switches it off. `VillageEvents` still cancels the zombie villagers a
village places as it generates; these are natural spawns.

**Quests.** A Ruined Village says timber rots, rotted floors give way and the old locals turn up again as zombie
villagers after dark, and Bringing People Back that a ruin has plenty after dark (`tools/gen_quests.py`, keys and titles
unchanged).

**Tests** (`src/gametest/java/com/mcremix/gametest/RottedWoodTests`, five): a plank room with a door and a stair-edged
roof, run through the processor in young and old villages, rots more in the old, more on its roof and on the ground than
in walls under the roof, less in a savanna and more in a taiga, keeps every rotted block's shape, rots a door whole, the
same every time, and grows mushrooms only on rotted wood under the roof; real plains and taiga houses rot as they
generate with nothing left hanging; rotted floors give way as above (a zombie dropped onto rotted planks falls through;
only planks, stairs and slabs give way); rotted wood leaves a stick about a third of the time and nothing else, itself
with Silk Touch (a door from either half, once), is in no item tag recipes read and crafts nothing, joins sound fences,
grows mushrooms in full light, burns as the sound wood does, composts and strips; and inside a village's pieces the
monster list has zombie villagers in place of zombies and husks, as likely, and the same entries when asked again, while
outside it and for animals the list is as it was. `RuinTests.housesFallInWithDecay` counts rotted slabs, stairs and
logs as rubble, since rubble on the ground is the likeliest wood to rot.

**Known gaps:** villages generated before 0.43.0 keep their sound wood (rot happens as a village generates), though
their zombies come as zombie villagers like any village's; wood of other mods' kinds never rots; a structure is judged a
village from the first of its starts looked at, so a mod's structure that only sometimes holds a village piece may be
judged by one without; a village placed with `/place structure` or `/place jigsaw` rots but gets no zombie villagers,
as those commands leave no structure start in the world's chunks.

## 58. Quieter rotted wood textures (0.43.1)

Taran on 0.43.0, 2026-11-27: "Could you make the textures look better? It looks messy and the black lines have too much
contrast." Only the drawing in `tools/gen_rot.py` and the 72 textures it writes (64 block, 8 item) changed: no Java,
model, loot, tag or name.

**Why 0.43.0 looked messy.** Its cracks, checks and bracket fungus were painted in one flat colour, 0.45 of the
texture's average lightness, so on every wood they were near-black lines; and each texture carried a dozen small marks
(boards checked into cubes, soft spots, pale mycelium specks, a bracket fungus, ragged oval bark patches) that read as
noise from a few blocks away.

**How 0.43.1 draws rot.** Every mark darkens the pixels under it by a share of their own lightness (`Tex.scale`) and
never paints a colour, so it keeps the wood's grain and hue: a split 0.74 (0.87 at its ends), about as dark as vanilla's
own seams between boards; a joint opened 0.88; the rim of a hole 0.82. Few marks, each where the wood's shape puts it:

- `dull` as before (0.84 of the lightness plus 0.02, half the colour, pale wood over 0.7 pressed down) with a softer
  mottle, 0.05 where it was 0.08, mostly from the larger of its two noise scales.
- Damp patches (`stain`): ellipses that fade out as (1 - d²)², up to 0.2 of the lightness darker on planks and 0.12 to
  0.14 elsewhere, going greyer and a touch green toward their middles.
- Planks: two damp patches, each on one of the texture's four darkest rows (its joints) with that joint opened over five
  pixels, and one or two splits of 3 to 5 pixels along the grain two rows below a joint.
- Stripped logs and wood: two splits of 3 to 5 pixels up the grain, still never side by side at one height (0.43.0's
  lamp post faces), and a long damp streak.
- Logs and wood: one strip of bark peeled away along the grain, 5 to 7 pixels long and 2 wide (1 at its ends), its
  sides wandering a pixel now and then; under it the stripped wood, dulled and held to at most 0.14 above the bark's
  average lightness, with its broken sides 0.88. Round patches were tried first and looked stuck on, cherry's pink
  worst.
- Log ends: the heart darkened to 0.8 at its middle, easing out over 3.5 pixels, and two checks 4 to 5.5 pixels long
  running out from it at 0.76.
- Doors and trapdoors: as 0.43.0 (a board gone, the foot rotted away, the iron rusted, a softer rust now) with a damp
  patch over the rotted foot and lighter rims; the cracks down their boards are gone, and their marks no longer wrap from
  one edge to the other (`Tex(tiles=False)`), as a door does not tile.
- Gone: the flat crack colour, mycelium, bracket fungus, checking into cubes and soft spots.

**Measured** over the 64 block textures, against vanilla's for the same woods: the darkest 2% of each texture's pixels
sat at 0.52 of its average lightness in 0.43.0 and sit at 0.72 now (vanilla 0.78); its single darkest pixel averaged
0.42 and went as low as 0.20, now 0.62 and 0.35 (vanilla 0.74 and 0.34, both lows the dark bark round a cherry log's
end); the spread of lightness, standard deviation over the mean, was 0.20 and is 0.13 (vanilla 0.12). Before and after
sheets of every wood and dev client shots from 0.43.0's camera spots are in `builds/screenshots/0.43.1`.

## 59. Logs need a flint hatchet, on top of 0.43.1 (0.43.2)

Taran answered the eight flint and bread cards of section 55 on 2026-11-27: seven as built, and the fourth the other
way, so rather than punching trees, logs need a flint hatchet. The change was made on 0.41.0's branch before it merged
(PR #15, commit 82b21a9) and merged here onto 0.43.1, so this version is 0.43.1 with it; section 55's "Logs need an
axe" says how it works. In short: bare hands, and any tool but an axe, make no headway on a log
(`#mcremix:needs_an_axe`, which holds `#minecraft:logs`), and starting on one says so on the action bar; the flint
hatchet crafts in the inventory's 2x2 grid; leaves drop a stick about one time in eight. Rotted logs (section 57) are
not in `#minecraft:logs`, so they still break by hand in a moment, and the Real Ores quest (section 56) still follows
Flint Tools. In the merge only the generated quest book index conflicted (its book hash); every generated file came out
of `gen_assets.py` and `gen_quests.py` identical to the merge. 138 quests, book `4b24d6040403c8b9`.

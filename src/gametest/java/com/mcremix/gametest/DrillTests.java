package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mcremix.McRemix;
import com.mcremix.config.RemixConfig;
import com.mcremix.drilling.DrillBits;
import com.mcremix.drilling.FittedBits;
import com.mcremix.drilling.TieredDrillBlock;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.machines.Fittings;
import com.mcremix.machines.MachineKind;
import com.mcremix.registry.ModMachines;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.contraptions.piston.LinearActuatorBlockEntity;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.drill.DrillBlock;
import com.simibubi.create.content.kinetics.drill.DrillBlockEntity;
import com.simibubi.create.content.kinetics.drill.DrillMovementBehaviour;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Section 7 of BUILD_NOTES: drill bits, fitted to any drill. */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class DrillTests {

	/** A drill body: Create's own, or one of the tiers. Bits do not care which. */
	static Block body(KineticMaterial material) {
		return material == KineticMaterial.ANDESITE ? AllBlocks.MECHANICAL_DRILL.get()
			: ModMachines.get(MachineKind.DRILL, material).get();
	}

	static final KineticMaterial[] BODIES = { KineticMaterial.ANDESITE, KineticMaterial.COPPER, KineticMaterial.IRON,
		KineticMaterial.STEEL, KineticMaterial.ALUMINIUM };

	static DrillBlockEntity drillAt(GameTestHelper h, BlockPos pos) {
		if (h.getBlockEntity(pos) instanceof DrillBlockEntity be)
			return be;
		h.fail("no drill", pos);
		return null;
	}

	/** Fits bits to the drill at {@code pos} straight through its block entity, as setup for a test. */
	static void fit(GameTestHelper h, BlockPos pos, DrillBits bits) {
		((FittedBits) drillAt(h, pos)).mcremix$setBits(bits);
	}

	/** A drill at {@code pos} facing {@code facing} with {@code bits} fitted, driven by a creative motor behind it. */
	static void fixedDrill(GameTestHelper h, BlockPos pos, Block drill, DrillBits bits, Direction facing, int rpm) {
		h.setBlock(pos, drill.defaultBlockState().setValue(DrillBlock.FACING, facing));
		fit(h, pos, bits);
		GT.motor(h, pos.relative(facing.getOpposite()), facing, rpm);
	}

	static void fixedDrill(GameTestHelper h, BlockPos pos, DrillBits bits, Direction facing, int rpm) {
		fixedDrill(h, pos, AllBlocks.MECHANICAL_DRILL.get(), bits, facing, rpm);
	}

	record Lane(DrillBits bits, Block target, boolean cuts) {}

	/** Fixed drills at 32 rpm on ores their bits can and cannot cut, in every body. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void drillsOnlyCutWhatTheirBitsCan(GameTestHelper h) {
		List<Lane> lanes = List.of(
			new Lane(DrillBits.GOLD, Blocks.IRON_ORE, false), new Lane(DrillBits.GOLD, Blocks.COPPER_ORE, false),
			new Lane(DrillBits.GOLD, Blocks.LAPIS_ORE, false), new Lane(DrillBits.GOLD, Blocks.STONE, true),
			new Lane(DrillBits.GOLD, Blocks.COAL_ORE, true),
			new Lane(DrillBits.STONE, Blocks.GOLD_ORE, false), new Lane(DrillBits.STONE, Blocks.DIAMOND_ORE, false),
			new Lane(DrillBits.STONE, Blocks.EMERALD_ORE, false), new Lane(DrillBits.STONE, Blocks.REDSTONE_ORE, false),
			new Lane(DrillBits.STONE, Blocks.IRON_ORE, true),
			new Lane(DrillBits.IRON, Blocks.OBSIDIAN, false), new Lane(DrillBits.IRON, Blocks.CRYING_OBSIDIAN, false),
			new Lane(DrillBits.IRON, Blocks.ANCIENT_DEBRIS, false), new Lane(DrillBits.IRON, Blocks.DIAMOND_ORE, true),
			new Lane(DrillBits.DIAMOND, Blocks.DIAMOND_ORE, true), new Lane(DrillBits.DIAMOND, Blocks.IRON_ORE, true));
		for (int i = 0; i < lanes.size(); i++) {
			int x = i < 8 ? 1 : 9, z = 1 + (i % 8) * 2;
			fixedDrill(h, p(x + 1, 2, z), body(BODIES[i % BODIES.length]), lanes.get(i).bits(), Direction.EAST, 32);
			h.setBlock(p(x + 2, 2, z), lanes.get(i).target());
		}
		h.runAtTickTime(300, () -> {
			List<String> wrong = new ArrayList<>();
			for (int i = 0; i < lanes.size(); i++) {
				Lane lane = lanes.get(i);
				int x = i < 8 ? 1 : 9, z = 1 + (i % 8) * 2;
				BlockPos drillPos = p(x + 1, 2, z), target = p(x + 2, 2, z);
				String where = lane.bits() + " bits in a " + BODIES[i % BODIES.length] + " drill ";
				boolean standing = h.getBlockState(target).is(lane.target());
				if (standing == lane.cuts())
					wrong.add(where + (lane.cuts() ? "did not cut " : "cut ") + lane.target());
				// what someone looking at a stuck drill reads (no goggles needed)
				List<Component> hover = new ArrayList<>();
				drillAt(h, drillPos).addToTooltip(hover, false);
				String keys = Keys.of(hover);
				if (!lane.cuts() && !(keys.contains("[mcremix.tooltip.drill.too_hard]") && keys.contains("[mcremix.drill_bits.material." + lane.bits().id() + "]")))
					wrong.add(where + "stuck on " + lane.target() + " shows " + keys);
				if (lane.cuts() && keys.contains("too_hard"))
					wrong.add(where + "says it is stuck after cutting " + lane.target());
			}
			h.assertTrue(wrong.isEmpty(), "drill bits: " + wrong);

			// the hint names every bit that would cut it
			expectHint(h, DrillBits.GOLD, Blocks.IRON_ORE, "[mcremix.drill_bits.list.3]", "stone", "iron", "diamond");
			expectHint(h, DrillBits.IRON, Blocks.OBSIDIAN, "[mcremix.drill_bits.list.1]", "diamond");
			expectHint(h, DrillBits.STONE, Blocks.GOLD_ORE, "[mcremix.drill_bits.list.2]", "iron", "diamond");
			h.succeed();
		});
	}

	static void expectHint(GameTestHelper h, DrillBits bits, Block target, String list, String... needed) {
		BlockPos at = p(15, 2, 15);
		h.setBlock(p(15, 3, 15), target);
		BlockState drill = AllBlocks.MECHANICAL_DRILL.getDefaultState().setValue(DrillBlock.FACING, Direction.UP);
		List<Component> lines = new ArrayList<>();
		boolean stuck = bits.addStuckInfo(lines, h.getLevel(), h.absolutePos(at), drill);
		String keys = Keys.of(lines);
		h.assertTrue(stuck && keys.contains("[mcremix.tooltip.drill.needs]") && keys.contains(list), bits + " on " + target + " shows " + keys);
		for (String n : needed)
			h.assertTrue(keys.contains("[mcremix.drill_bits.material." + n + "]"), bits + " on " + target + " should name " + n + " bits: " + keys);
		h.setBlock(p(15, 3, 15), Blocks.AIR);
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void stuckDrillStartsAgainWhenTheBlockChanges(GameTestHelper h) {
		BlockPos drill = p(2, 2, 4), target = p(3, 2, 4);
		fixedDrill(h, drill, body(KineticMaterial.COPPER), DrillBits.GOLD, Direction.EAST, 32);
		h.setBlock(target, Blocks.IRON_ORE);
		h.startSequence()
			.thenExecuteAfter(80, () -> {
				GT.assertIs(h, target, Blocks.IRON_ORE);
				h.setBlock(target, Blocks.STONE);
			})
			.thenExecuteAfter(50, () -> h.assertTrue(h.getBlockState(target).isAir(),
				"a drill with gold bits should cut stone put in front of it within 2.5 s of being stuck"))
			.thenSucceed();
	}

	/** Ticks to cut one stone at 32 rpm with each bits. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void drillSpeedsFollowTheirBits(GameTestHelper h) {
		Map<DrillBits, Integer> took = new LinkedHashMap<>();
		DrillBits[] all = DrillBits.values();
		for (int i = 0; i < all.length; i++) {
			fixedDrill(h, p(2, 2, 2 + i * 3), all[i], Direction.EAST, 32);
			h.setBlock(p(3, 2, 2 + i * 3), Blocks.STONE);
		}
		h.onEachTick(() -> {
			for (int i = 0; i < all.length; i++)
				if (!took.containsKey(all[i]) && h.getBlockState(p(3, 2, 2 + i * 3)).isAir())
					took.put(all[i], (int) h.getTick());
		});
		h.succeedWhen(() -> {
			h.assertTrue(took.size() == all.length, "every drill should cut its stone; so far " + took);
			McRemix.LOGGER.info("[gametest] ticks to cut stone at 32 rpm: {}", took);
			int stone = took.get(DrillBits.STONE), iron = took.get(DrillBits.IRON), gold = took.get(DrillBits.GOLD),
				diamond = took.get(DrillBits.DIAMOND);
			h.assertTrue(gold < diamond && diamond < iron && iron < stone, "speed order should be gold, diamond, iron, stone: " + took);
			h.assertTrue(iron >= 1.5f * gold, "gold should cut about twice as fast as iron: " + took);
			h.assertTrue(iron >= 1.15f * diamond, "diamond should cut about a third faster than iron: " + took);
			h.assertTrue(stone >= 1.35f * iron, "stone should cut about a third slower than iron: " + took);
			for (DrillBits bits : all)
				h.assertTrue(DrillBits.of(drillAt(h, p(2, 2, 2 + bits.ordinal() * 3))) == bits, "the drill lost its " + bits + " bits");
		});
	}

	/**
	 * Bits set the stress, whatever the body: a tier draws what Create's drill does, through Create's config, and on top
	 * of that its material's friction (iron and steel draw some).
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void drillStressFollowsTheirBits(GameTestHelper h) {
		DrillBits[] all = DrillBits.values();
		float[] at32 = { 64, 128, 64, 256 };
		double create = BlockStressValues.getImpact(AllBlocks.MECHANICAL_DRILL.get());
		for (KineticMaterial material : BODIES) {
			double got = BlockStressValues.getImpact(body(material));
			h.assertTrue(got == create, material + " drill should draw what Create's does (" + create + " su per rpm), draws " + got);
		}
		for (int i = 0; i < all.length; i++)
			fixedDrill(h, p(2, 2, 2 + i * 3), body(BODIES[i + 1]), all[i], Direction.EAST, 32);
		h.runAtTickTime(20, () -> {
			for (int i = 0; i < all.length; i++) {
				float stress = GT.kinetic(h, p(2, 2, 2 + i * 3)).getOrCreateNetwork().calculateStress();
				float want = at32[i] + BODIES[i + 1].baseFriction * 32;
				h.assertTrue(stress == want, all[i] + " bits at 32 rpm on a " + BODIES[i + 1] + " drill should load " + want
					+ " su, load " + stress);
			}
			h.succeed();
		});
	}

	/** A drill as a contraption carries it: its block entity's data, bits and all. */
	static MovementContext moving(GameTestHelper h, DrillBlockEntity drill) {
		CompoundTag data = drill.saveWithFullMetadata();
		return new MovementContext(h.getLevel(), new StructureBlockInfo(drill.getBlockPos(), drill.getBlockState(), data), null);
	}

	/**
	 * Flips the live server config, so it runs in a batch of its own with time between the steps: Forge saves each
	 * change to the config file and reloads the file a moment later, and a reload that lands after the restore can
	 * leave the setting off for every other drill test.
	 */
	@GameTest(template = GT.PLATFORM, batch = "ironDrillBitsConfig", timeoutTicks = 200)
	public static void ironDrillBitsCanBeSwitchedOff(GameTestHelper h) {
		BlockPos iron = p(2, 2, 2), golden = p(2, 2, 6);
		h.setBlock(iron, AllBlocks.MECHANICAL_DRILL.getDefaultState().setValue(DrillBlock.FACING, Direction.EAST));
		h.setBlock(golden, body(KineticMaterial.STEEL).defaultBlockState().setValue(DrillBlock.FACING, Direction.EAST));
		fit(h, golden, DrillBits.GOLD);
		DrillBlockEntity ironBe = drillAt(h, iron), goldenBe = drillAt(h, golden);
		BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState(), ironOre = Blocks.IRON_ORE.defaultBlockState();
		DrillMovementBehaviour moving = new DrillMovementBehaviour();
		BlockPos somewhere = h.absolutePos(p(8, 2, 8));

		h.assertTrue(DrillBits.of(ironBe) == DrillBits.IRON, "a new drill should come with iron bits");
		h.assertFalse(ironBe.canBreak(obsidian, 50), "iron bits should not cut obsidian");
		h.assertFalse(Fittings.canBreak(moving, h.getLevel(), somewhere, obsidian, moving(h, ironBe)), "nor on a contraption");
		h.assertFalse(Fittings.canBreak(moving, h.getLevel(), somewhere, ironOre, moving(h, goldenBe)),
			"gold bits on a contraption should not cut iron ore");

		boolean before = RemixConfig.IRON_DRILL_BITS.get();
		h.startSequence()
			.thenExecute(() -> RemixConfig.IRON_DRILL_BITS.set(false))
			.thenIdle(40)
			.thenExecute(() -> {
				try {
					h.assertTrue(ironBe.canBreak(obsidian, 50), "with ironDrillBits off iron bits should cut obsidian");
					h.assertTrue(Fittings.canBreak(moving, h.getLevel(), somewhere, obsidian, moving(h, ironBe)), "and on a contraption");
					h.assertFalse(goldenBe.canBreak(ironOre, 3), "gold bits keep their tier");
					h.assertFalse(Fittings.canBreak(moving, h.getLevel(), somewhere, ironOre, moving(h, goldenBe)), "on a contraption too");
				} finally {
					RemixConfig.IRON_DRILL_BITS.set(before);
				}
			})
			.thenIdle(40)
			.thenExecute(() -> h.assertTrue(RemixConfig.IRON_DRILL_BITS.get() == before,
				"ironDrillBits should be back to " + before + " once the config file has reloaded"))
			.thenSucceed();
	}

	/**
	 * A piston pushing a drill forward: the drill cuts the stone in front, then meets a block its bits cannot cut and
	 * the piston stops as it would at bedrock. Iron bits on the same rig go on through.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void drillOnAPistonStopsAtWhatItCannotCut(GameTestHelper h) {
		BlockPos goldPiston = p(4, 2, 3), ironPiston = p(4, 2, 9);
		pistonRig(h, goldPiston, AllBlocks.MECHANICAL_DRILL.get(), DrillBits.GOLD);
		pistonRig(h, ironPiston, body(KineticMaterial.IRON), DrillBits.IRON);
		h.startSequence()
			.thenExecuteAfter(30, () -> {
				// the motors are set to extend; if this Create build turns the other way, flip them once
				for (BlockPos piston : new BlockPos[] { goldPiston, ironPiston }) {
					LinearActuatorBlockEntity be = (LinearActuatorBlockEntity) h.getBlockEntity(piston);
					if (be != null && !be.running && be.offset == 0) {
						CreativeMotorBlockEntity motor = (CreativeMotorBlockEntity) h.getBlockEntity(piston.north());
						motor.generatedSpeed.setValue(-motor.generatedSpeed.getValue());
					}
				}
			})
			.thenExecuteAfter(300, () -> {
				List<String> seen = new ArrayList<>();
				for (int x = 4; x <= 11; x++)
					seen.add(x + ":" + h.getBlockState(p(x, 2, 3)).getBlock().getDescriptionId() + "/"
						+ h.getBlockState(p(x, 2, 9)).getBlock().getDescriptionId());
				McRemix.LOGGER.info("[gametest] piston rigs after 330 ticks: {}", seen);
				h.assertTrue(h.getBlockState(p(7, 2, 3)).is(Blocks.IRON_ORE), "gold bits should leave the iron ore standing: " + seen);
				h.assertFalse(h.getBlockState(p(6, 2, 3)).is(Blocks.STONE) || h.getBlockState(p(8, 2, 3)).is(Blocks.STONE),
					"gold bits should cut the stone, not push it: " + seen);
				h.assertTrue(h.getBlockState(p(6, 2, 3)).is(AllBlocks.MECHANICAL_DRILL.get()), "the drill should stop right before the ore: " + seen);
				h.assertTrue(DrillBits.of(drillAt(h, p(6, 2, 3))) == DrillBits.GOLD, "the drill should come off the contraption with its gold bits");
				h.assertFalse(h.getBlockState(p(7, 2, 9)).is(Blocks.IRON_ORE), "iron bits in an iron drill should cut the iron ore: " + seen);
			})
			.thenSucceed();
	}

	/** Piston facing east at {@code piston}, three poles behind it, the drill in front, then stone and iron ore. */
	static void pistonRig(GameTestHelper h, BlockPos piston, Block drill, DrillBits bits) {
		h.setBlock(piston, AllBlocks.MECHANICAL_PISTON.getDefaultState().setValue(BlockStateProperties.FACING, Direction.EAST)
			.setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, false));
		for (int i = 1; i <= 3; i++)
			h.setBlock(piston.west(i), AllBlocks.PISTON_EXTENSION_POLE.getDefaultState().setValue(BlockStateProperties.FACING, Direction.EAST));
		h.setBlock(piston.east(), drill.defaultBlockState().setValue(DrillBlock.FACING, Direction.EAST));
		fit(h, piston.east(), bits);
		h.setBlock(piston.east(2), Blocks.STONE);
		h.setBlock(piston.east(3), Blocks.IRON_ORE);
		GT.motor(h, piston.north(), Direction.SOUTH, -32);
	}

	/** A steel drill with gold bits spun on a bearing cuts the stone around it and sweeps past the iron ore. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 500)
	public static void drillOnABearingSkipsWhatItCannotCut(GameTestHelper h) {
		BlockPos bearing = p(6, 4, 8), drill = bearing.east();
		h.setBlock(bearing, AllBlocks.MECHANICAL_BEARING.getDefaultState().setValue(BlockStateProperties.FACING, Direction.EAST));
		h.setBlock(drill, body(KineticMaterial.STEEL).defaultBlockState().setValue(DrillBlock.FACING, Direction.UP));
		fit(h, drill, DrillBits.GOLD);
		h.setBlock(drill.above(), Blocks.STONE);
		h.setBlock(drill.below(), Blocks.STONE);
		h.setBlock(drill.north(), Blocks.IRON_ORE);
		h.setBlock(drill.south(), Blocks.IRON_ORE);
		GT.motor(h, bearing.west(), Direction.EAST, 16);
		h.startSequence()
			.thenWaitUntil(() -> {
				MechanicalBearingBlockEntity be = (MechanicalBearingBlockEntity) h.getBlockEntity(bearing);
				h.assertTrue(be != null && be.isRunning(), "the bearing should be turning");
				h.assertTrue(h.getBlockState(drill.above()).isAir() && h.getBlockState(drill.below()).isAir(),
					"the drill should cut the stone above and below it");
			})
			// another full turn at 16 rpm, past both ores again
			.thenIdle(100)
			.thenExecute(() -> {
				GT.assertIs(h, drill.north(), Blocks.IRON_ORE);
				GT.assertIs(h, drill.south(), Blocks.IRON_ORE);
				h.assertTrue(((MechanicalBearingBlockEntity) h.getBlockEntity(bearing)).isRunning(), "the bearing should still be turning");
			})
			.thenSucceed();
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void goldBitsFeedAHopperFromACobbleGenerator(GameTestHelper h) {
		// a drill pointing down at the cobblestone spot; whatever it cuts falls into the hopper underneath
		BlockPos hopper = p(4, 2, 4), spot = p(4, 3, 4), drill = p(4, 4, 4);
		h.setBlock(hopper, Blocks.HOPPER);
		fixedDrill(h, drill, DrillBits.GOLD, Direction.DOWN, 64);
		h.onEachTick(() -> {
			if (h.getBlockState(spot).isAir())
				h.setBlock(spot, Blocks.COBBLESTONE);
		});
		h.succeedWhen(() -> {
			HopperBlockEntity be = (HopperBlockEntity) h.getBlockEntity(hopper);
			int cobble = 0;
			for (int i = 0; i < be.getContainerSize(); i++)
				if (be.getItem(i).is(Items.COBBLESTONE))
					cobble += be.getItem(i).getCount();
			h.assertTrue(cobble >= 3, "the hopper should collect cobblestone cut by gold bits, has " + cobble);
		});
	}

	@GameTest(template = GT.PLATFORM)
	public static void drillsAreContraptionControlledAndPlaceAlongside(GameTestHelper h) {
		for (KineticMaterial material : BODIES)
			h.assertTrue(AllTags.AllItemTags.CONTRAPTION_CONTROLLED.matches(new ItemStack(body(material))),
				body(material) + " should be in #create:contraption_controlled");

		// clicking a placed tiered drill with any drill places the new one beside it, facing the same way
		BlockPos placed = p(4, 2, 4);
		h.setBlock(placed, body(KineticMaterial.COPPER).defaultBlockState().setValue(DrillBlock.FACING, Direction.UP));
		TestPlayer player = GT.player(h, p(4, 2, 1));
		int before = countDrills(h);
		GT.click(h, player, placed, Direction.NORTH, .5, new ItemStack(body(KineticMaterial.STEEL)));
		GT.click(h, player, placed, Direction.NORTH, .5, new ItemStack(AllBlocks.MECHANICAL_DRILL.get()));
		List<String> found = new ArrayList<>();
		for (Direction d : Direction.Plane.HORIZONTAL) {
			BlockState s = h.getBlockState(placed.relative(d));
			if (s.getBlock() instanceof DrillBlock)
				found.add(d + ":" + s.getBlock().getDescriptionId() + "/" + s.getValue(DrillBlock.FACING));
		}
		h.assertTrue(countDrills(h) == before + 2, "two drills should have been placed beside it: " + found);
		h.assertTrue(found.stream().allMatch(f -> f.endsWith("/up")), "placed drills should face the same way: " + found);
		h.assertTrue(found.stream().anyMatch(f -> f.contains("steel_mechanical_drill")) && found.stream().anyMatch(f -> f.contains("create.mechanical_drill")),
			"both a steel drill and Create's drill should be beside it: " + found);
		h.assertTrue(h.getBlockState(placed).getBlock() instanceof TieredDrillBlock, "the clicked drill stays");
		h.succeed();
	}

	static int countDrills(GameTestHelper h) {
		int n = 0;
		for (int x = 0; x < 16; x++)
			for (int z = 0; z < 16; z++)
				for (int y = 2; y < 6; y++)
					if (h.getBlockState(p(x, y, z)).getBlock() instanceof DrillBlock)
						n++;
		return n;
	}

	static int itemsAround(GameTestHelper h, BlockPos pos, Item item) {
		AABB box = new AABB(h.absolutePos(pos)).inflate(3);
		return h.getLevel().getEntitiesOfClass(ItemEntity.class, box, e -> e.getItem().is(item)).stream()
			.mapToInt(e -> e.getItem().getCount()).sum();
	}

	/** Using bits on a drill fits them, pops the old ones out, and changes what the drill draws from the network. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void bitsAreFittedByUsingThemOnADrill(GameTestHelper h) {
		BlockPos drill = p(4, 2, 4);
		fixedDrill(h, drill, body(KineticMaterial.COPPER), DrillBits.IRON, Direction.UP, 32);
		TestPlayer player = GT.player(h, p(4, 2, 1));
		h.startSequence()
			.thenExecuteAfter(10, () -> {
				float stress = GT.kinetic(h, drill).getOrCreateNetwork().calculateStress();
				h.assertTrue(stress == 128, "iron bits at 32 rpm should load 128 su, load " + stress);
				ItemStack gold = new ItemStack(DrillBits.GOLD.item(), 2);
				GT.click(h, player, drill, Direction.NORTH, .5, gold);
				h.assertTrue(DrillBits.of(drillAt(h, drill)) == DrillBits.GOLD, "using gold bits on the drill should fit them");
				h.assertTrue(player.getMainHandItem().getCount() == 1, "fitting should use one set of bits, left " + player.getMainHandItem());
				h.assertTrue(itemsAround(h, drill, DrillBits.IRON.item()) == 1, "the iron bits should pop out");
				// the same bits again change nothing
				GT.click(h, player, drill, Direction.NORTH, .5, player.getMainHandItem());
				h.assertTrue(player.getMainHandItem().getCount() == 1, "using the bits it has should do nothing");
				h.assertTrue(itemsAround(h, drill, DrillBits.GOLD.item()) == 0, "and pop nothing out");
				float now = GT.kinetic(h, drill).getOrCreateNetwork().calculateStress();
				h.assertTrue(now == 64, "gold bits at 32 rpm should load 64 su, load " + now);
				GT.click(h, player, drill, Direction.NORTH, .5, new ItemStack(DrillBits.DIAMOND.item()));
				h.assertTrue(DrillBits.of(drillAt(h, drill)) == DrillBits.DIAMOND && player.getMainHandItem().isEmpty(),
					"diamond bits should go in, using the last of the stack");
				h.assertTrue(itemsAround(h, drill, DrillBits.GOLD.item()) == 1, "the gold bits should pop out");
				float diamond = GT.kinetic(h, drill).getOrCreateNetwork().calculateStress();
				h.assertTrue(diamond == 256, "diamond bits at 32 rpm should load 256 su, load " + diamond);
			})
			.thenSucceed();
	}

	/** A deployer holding bits fits them to the drill it faces. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void deployerFitsBitsToADrill(GameTestHelper h) {
		BlockPos drill = p(4, 2, 4), deployer = p(4, 4, 4);
		h.setBlock(drill, body(KineticMaterial.COPPER).defaultBlockState().setValue(DrillBlock.FACING, Direction.UP));
		h.setBlock(deployer, AllBlocks.DEPLOYER.getDefaultState().setValue(BlockStateProperties.FACING, Direction.DOWN)
			.setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true));
		GT.motor(h, deployer.east(), Direction.WEST, 32);
		h.startSequence()
			.thenExecuteAfter(5, () -> {
				ItemStack left = h.getBlockEntity(deployer).getCapability(ForgeCapabilities.ITEM_HANDLER)
					.map(inv -> inv.insertItem(0, new ItemStack(DrillBits.DIAMOND.item()), false)).orElse(null);
				h.assertTrue(left != null && left.isEmpty(), "the deployer should take the bits, left " + left);
			})
			.thenWaitUntil(() -> h.assertTrue(DrillBits.of(drillAt(h, drill)) == DrillBits.DIAMOND,
				"the deployer should fit its diamond bits to the drill"))
			.thenExecuteAfter(5, () -> h.assertTrue(itemsAround(h, drill, DrillBits.IRON.item()) == 1,
				"the drill's iron bits should pop out"))
			.thenSucceed();
	}

	/** A broken drill keeps its bits, and goes back down with them. Only drills with iron bits stack with new ones. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void brokenDrillKeepsItsBits(GameTestHelper h) {
		BlockPos golden = p(4, 2, 4), plain = p(8, 2, 4);
		Block steel = body(KineticMaterial.STEEL);
		h.setBlock(golden, steel.defaultBlockState().setValue(DrillBlock.FACING, Direction.UP));
		fit(h, golden, DrillBits.GOLD);
		h.setBlock(plain, steel.defaultBlockState().setValue(DrillBlock.FACING, Direction.UP));
		List<ItemStack> goldDrops = GT.breakWith(h, golden, new ItemStack(Items.IRON_PICKAXE));
		List<ItemStack> plainDrops = GT.breakWith(h, plain, new ItemStack(Items.IRON_PICKAXE));
		h.assertTrue(goldDrops.size() == 1 && goldDrops.get(0).is(steel.asItem()), "the drill should drop itself, dropped " + goldDrops);
		h.assertTrue(DrillBits.of(goldDrops.get(0)) == DrillBits.GOLD, "the dropped drill should keep its gold bits: " + goldDrops.get(0).getTag());
		h.assertTrue(plainDrops.size() == 1 && !plainDrops.get(0).hasTag(), "a drill with iron bits should drop plain: " + plainDrops);
		h.assertTrue(ItemStack.isSameItemSameTags(plainDrops.get(0), new ItemStack(steel)), "and stack with a new drill");

		BlockPos floor = p(4, 1, 10);
		TestPlayer player = GT.player(h, p(4, 2, 13));
		GT.click(h, player, floor, Direction.UP, 1, goldDrops.get(0));
		BlockPos down = floor.above();
		h.assertTrue(h.getBlockState(down).is(steel), "the drill should go back down, found " + h.getBlockState(down));
		h.assertTrue(DrillBits.of(drillAt(h, down)) == DrillBits.GOLD, "with its gold bits");
		h.succeed();
	}

	@GameTest(template = GT.PLATFORM)
	public static void drillBitsRecipes(GameTestHelper h) {
		Item[] materials = { Items.COBBLESTONE, Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND };
		DrillBits[] all = DrillBits.values();
		for (int i = 0; i < all.length; i++) {
			Item m = materials[i];
			GearboxTests.expectCraft(h, all[i].item(), null, m, null, m, Items.IRON_INGOT, m, null, null, null);
		}
		// Create's drill is still Create's recipe, and comes with iron bits
		var alloy = com.simibubi.create.AllItems.ANDESITE_ALLOY.get();
		GearboxTests.expectCraft(h, AllBlocks.MECHANICAL_DRILL.get(), null, alloy, null, alloy, Items.IRON_INGOT, alloy, null,
			AllBlocks.ANDESITE_CASING.get(), null);
		h.assertTrue(DrillBits.of(new ItemStack(AllBlocks.MECHANICAL_DRILL.get())) == DrillBits.IRON, "a new drill has iron bits");
		h.succeed();
	}
}

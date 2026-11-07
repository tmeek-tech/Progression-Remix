package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mcremix.McRemix;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.machines.MachineKind;
import com.mcremix.registry.ModMachines;
import com.mcremix.sawing.FittedBlade;
import com.mcremix.sawing.SawBlades;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.contraptions.piston.LinearActuatorBlockEntity;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Section 11 of BUILD_NOTES: saw blades, fitted to any saw. */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class SawTests {
	static final SawBlades[] ALL = { SawBlades.STONE, SawBlades.IRON, SawBlades.GOLD, SawBlades.DIAMOND };

	/** A saw body: Create's own, or one of the tiers. Blades do not care which. */
	static Block body(KineticMaterial material) {
		return material == KineticMaterial.ANDESITE ? AllBlocks.MECHANICAL_SAW.get()
			: ModMachines.get(MachineKind.SAW, material).get();
	}

	static final KineticMaterial[] BODIES = { KineticMaterial.ANDESITE, KineticMaterial.COPPER, KineticMaterial.IRON,
		KineticMaterial.STEEL, KineticMaterial.ALUMINIUM };

	/** Fits a blade to the saw at {@code pos} straight through its block entity, as setup for a test. */
	static void fit(GameTestHelper h, BlockPos pos, SawBlades blades) {
		((FittedBlade) sawAt(h, pos)).mcremix$setBlades(blades);
	}

	/** An upward saw with {@code blades} fitted, its shaft running east-west, driven by a creative motor on its west. */
	static void upwardSaw(GameTestHelper h, BlockPos pos, Block saw, SawBlades blades, int rpm) {
		h.setBlock(pos, saw.defaultBlockState().setValue(SawBlock.FACING, Direction.UP)
			.setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true));
		fit(h, pos, blades);
		if (rpm != 0)
			GT.motor(h, pos.west(), Direction.EAST, rpm);
	}

	static void upwardSaw(GameTestHelper h, BlockPos pos, SawBlades blades, int rpm) {
		upwardSaw(h, pos, AllBlocks.MECHANICAL_SAW.get(), blades, rpm);
	}

	/** A saw facing east, fitted with {@code blades}. */
	static void eastSaw(GameTestHelper h, BlockPos pos, Block saw, SawBlades blades) {
		h.setBlock(pos, saw.defaultBlockState().setValue(SawBlock.FACING, Direction.EAST)
			.setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, false));
		fit(h, pos, blades);
	}

	static SawBlockEntity sawAt(GameTestHelper h, BlockPos pos) {
		if (h.getBlockEntity(pos) instanceof SawBlockEntity be)
			return be;
		h.fail("no saw", pos);
		return null;
	}

	static boolean skating(GameTestHelper h, BlockPos pos) {
		return ((FittedBlade) sawAt(h, pos)).mcremix$isSkating();
	}

	/** Puts an item on the blade the way a funnel or hopper would. Returns what did not fit. */
	static ItemStack feed(GameTestHelper h, BlockPos saw, ItemStack stack) {
		return sawAt(h, saw).getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
			.map(inv -> inv.insertItem(0, stack, false)).orElse(stack);
	}

	static int itemsAround(GameTestHelper h, BlockPos pos, Item item) {
		AABB box = new AABB(h.absolutePos(pos)).inflate(3);
		return h.getLevel().getEntitiesOfClass(ItemEntity.class, box, e -> e.getItem().is(item)).stream()
			.mapToInt(e -> e.getItem().getCount()).sum();
	}

	/**
	 * Blades set the stress, whatever the body: a tier draws what Create's saw does, through Create's config, and on top
	 * of that its material's friction (iron and steel draw some).
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void sawStressFollowsTheirBlades(GameTestHelper h) {
		double create = BlockStressValues.getImpact(AllBlocks.MECHANICAL_SAW.get());
		for (KineticMaterial material : BODIES) {
			double impact = BlockStressValues.getImpact(body(material));
			h.assertTrue(impact == create, material + " saw should draw what Create's does (" + create + " su per rpm), draws " + impact);
		}
		for (int i = 0; i < ALL.length; i++) {
			BlockPos pos = p(2, 2, 2 + i * 3);
			upwardSaw(h, pos, body(BODIES[i + 1]), ALL[i], 32);
			h.assertTrue(SawBlades.of(sawAt(h, pos)) == ALL[i], "the saw should have the " + ALL[i] + " blade");
		}
		h.assertTrue(SawBlades.GOLD.canCut(Blocks.OAK_LOG.defaultBlockState()) && SawBlades.STONE.canCut(Blocks.OAK_LOG.defaultBlockState()),
			"every blade cuts logs");
		h.runAtTickTime(20, () -> {
			float[] want = { 64, 128, 64, 256 };
			for (int i = 0; i < ALL.length; i++) {
				BlockPos pos = p(2, 2, 2 + i * 3);
				float load = GT.kinetic(h, pos).getOrCreateNetwork().calculateStress();
				float expected = want[i] + BODIES[i + 1].baseFriction * 32;
				h.assertTrue(load == expected, ALL[i] + " blade at 32 rpm on a " + BODIES[i + 1] + " saw should load "
					+ expected + " su, loads " + load);
			}
			h.succeed();
		});
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void goldenSawSkatesStoneAndStripsLogs(GameTestHelper h) {
		BlockPos golden = p(4, 2, 4), stone = p(4, 2, 11);
		upwardSaw(h, golden, body(KineticMaterial.COPPER), SawBlades.GOLD, 32);
		upwardSaw(h, stone, SawBlades.STONE, 32);
		boolean[] skated = { false };
		h.onEachTick(() -> {
			if (skating(h, golden))
				skated[0] = true;
		});
		h.startSequence()
			.thenExecuteAfter(5, () -> {
				h.assertTrue(feed(h, golden, new ItemStack(Items.SMOOTH_STONE)).isEmpty(), "the golden saw should take the stone");
				h.assertTrue(feed(h, stone, new ItemStack(Items.SMOOTH_STONE)).isEmpty(), "the stone saw should take the stone");
			})
			.thenWaitUntil(() -> {
				h.assertTrue(sawAt(h, golden).inventory.isEmpty(), "the stone should leave the golden saw");
				h.assertTrue(itemsAround(h, stone, Items.SMOOTH_STONE_SLAB) == 2, "the stone saw should cut two slabs");
			})
			.thenExecute(() -> {
				h.assertTrue(skated[0], "the stone should skate across the golden blade");
				h.assertTrue(itemsAround(h, golden, Items.SMOOTH_STONE) == 1 && itemsAround(h, golden, Items.SMOOTH_STONE_SLAB) == 0,
					"the stone should come off the golden saw uncut");
				h.assertTrue(feed(h, golden, new ItemStack(Items.OAK_LOG)).isEmpty(), "the golden saw should take the log");
			})
			.thenWaitUntil(() -> h.assertTrue(itemsAround(h, golden, Items.STRIPPED_OAK_LOG) == 1,
				"the log should come off the golden saw stripped"))
			.thenExecute(() -> h.assertFalse(skating(h, golden), "a log does not skate"))
			.thenSucceed();
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void sawBladesCutAtTheirSpeeds(GameTestHelper h) {
		for (int i = 0; i < ALL.length; i++)
			upwardSaw(h, p(4, 2, 2 + i * 4), ALL[i], 32);
		float[] duration = new float[ALL.length];
		int[] cut = { -1, -1, -1, -1 };
		h.startSequence()
			.thenExecuteAfter(5, () -> {
				for (int i = 0; i < ALL.length; i++)
					h.assertTrue(feed(h, p(4, 2, 2 + i * 4), new ItemStack(Items.OAK_LOG)).isEmpty(), ALL[i] + " saw should take the log");
				for (int i = 0; i < ALL.length; i++)
					duration[i] = sawAt(h, p(4, 2, 2 + i * 4)).inventory.recipeDuration;
			})
			.thenWaitUntil(() -> {
				for (int i = 0; i < ALL.length; i++)
					if (cut[i] < 0 && sawAt(h, p(4, 2, 2 + i * 4)).inventory.appliedRecipe)
						cut[i] = (int) h.getTick();
				for (int i = 0; i < ALL.length; i++)
					h.assertTrue(cut[i] >= 0, ALL[i] + " saw has not cut its log yet");
			})
			.thenExecute(() -> {
				McRemix.LOGGER.info("[gametest] log on each saw at 32 rpm (stone, iron, gold, diamond): recipe time {}, cut at tick {}",
					java.util.Arrays.toString(duration), java.util.Arrays.toString(cut));
				float iron = duration[1];
				for (int i = 0; i < ALL.length; i++)
					h.assertTrue(Math.abs(iron / duration[i] - ALL[i].speed) < .01f,
						ALL[i] + " blade should take 1/" + ALL[i].speed + " of the iron blade's time: " + duration[i] + " vs " + iron);
				h.assertTrue(cut[2] < cut[3] && cut[3] < cut[1] && cut[1] < cut[0],
					"gold should cut first, then diamond, iron and stone: " + java.util.Arrays.toString(cut));
			})
			.thenSucceed();
	}

	/**
	 * Each blade on a mechanical piston, in a different body, pushed into two logs: it cuts them where they stand.
	 * Without the movement check a tiered saw would take the logs along with it.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 600)
	public static void sawsOnAPistonCutWhatTheyArePushedInto(GameTestHelper h) {
		int[] rows = { 1, 5, 9, 13 };
		for (int i = 0; i < ALL.length; i++) {
			BlockPos piston = p(4, 2, rows[i]);
			h.setBlock(piston, AllBlocks.MECHANICAL_PISTON.getDefaultState().setValue(BlockStateProperties.FACING, Direction.EAST)
				.setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, false));
			for (int pole = 1; pole <= 3; pole++)
				h.setBlock(piston.west(pole), AllBlocks.PISTON_EXTENSION_POLE.getDefaultState()
					.setValue(BlockStateProperties.FACING, Direction.EAST));
			eastSaw(h, piston.east(), body(BODIES[i + 1]), ALL[i]);
			h.setBlock(piston.east(2), Blocks.OAK_LOG);
			h.setBlock(piston.east(3), Blocks.OAK_LOG);
			GT.motor(h, piston.north(), Direction.SOUTH, -32);
		}
		h.startSequence()
			.thenExecuteAfter(30, () -> {
				// the motors are set to extend; if this Create build turns the other way, flip them once
				for (int row : rows) {
					LinearActuatorBlockEntity be = (LinearActuatorBlockEntity) h.getBlockEntity(p(4, 2, row));
					if (be != null && !be.running && be.offset == 0) {
						CreativeMotorBlockEntity motor = (CreativeMotorBlockEntity) h.getBlockEntity(p(4, 2, row - 1));
						motor.generatedSpeed.setValue(-motor.generatedSpeed.getValue());
					}
				}
			})
			.thenWaitUntil(() -> {
				for (int i = 0; i < ALL.length; i++) {
					List<String> row = new ArrayList<>();
					for (int x = 5; x <= 11; x++)
						row.add(x + ":" + h.getBlockState(p(x, 2, rows[i])).getBlock().getDescriptionId());
					for (int x = 5; x <= 11; x++)
						h.assertFalse(h.getBlockState(p(x, 2, rows[i])).is(Blocks.OAK_LOG), ALL[i] + " saw left a log standing: " + row);
					h.assertTrue(h.getBlockState(p(8, 2, rows[i])).getBlock() instanceof SawBlock,
						ALL[i] + " saw should end three blocks on: " + row);
				}
			})
			.thenExecute(() -> {
				for (int i = 0; i < ALL.length; i++)
					h.assertTrue(itemsAround(h, p(7, 2, rows[i]), Items.OAK_LOG) == 2, ALL[i] + " saw should drop both logs");
			})
			.thenSucceed();
	}

	/** A small oak: a trunk four logs high with leaves round the top two. */
	static void oak(GameTestHelper h, BlockPos base) {
		for (int y = 0; y < 4; y++)
			h.setBlock(base.above(y), Blocks.OAK_LOG);
		BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false)
			.setValue(LeavesBlock.DISTANCE, 1);
		for (int y = 2; y <= 3; y++)
			for (Direction d : Direction.Plane.HORIZONTAL)
				h.setBlock(base.above(y).relative(d), leaves);
		h.setBlock(base.above(4), leaves);
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void horizontalSawsFellTrees(GameTestHelper h) {
		int[] rows = { 2, 6, 10, 14 };
		for (int i = 0; i < ALL.length; i++) {
			BlockPos saw = p(3, 2, rows[i]);
			eastSaw(h, saw, body(BODIES[i]), ALL[i]);
			GT.motor(h, saw.west(), Direction.EAST, 32);
			oak(h, saw.east());
		}
		int[] felled = { -1, -1, -1, -1 };
		h.onEachTick(() -> {
			for (int i = 0; i < ALL.length; i++)
				if (felled[i] < 0 && !h.getBlockState(p(4, 5, rows[i])).is(Blocks.OAK_LOG))
					felled[i] = (int) h.getTick();
		});
		h.succeedWhen(() -> {
			for (int i = 0; i < ALL.length; i++) {
				if (felled[i] < 0) {
					List<String> column = new ArrayList<>();
					for (int y = 2; y <= 6; y++)
						column.add(y + ":" + h.getBlockState(p(4, y, rows[i])).getBlock().getDescriptionId());
					h.fail(ALL[i] + " saw has not felled its tree; saw speed " + GT.kinetic(h, p(3, 2, rows[i])).getSpeed()
						+ ", felled " + java.util.Arrays.toString(felled) + ", trunk " + column);
				}
				for (int y = 2; y <= 5; y++)
					h.assertFalse(h.getBlockState(p(4, y, rows[i])).is(Blocks.OAK_LOG), ALL[i] + " saw left a log at height " + y);
			}
			McRemix.LOGGER.info("[gametest] trees felled at 32 rpm (stone, iron, gold, diamond) at tick {}", java.util.Arrays.toString(felled));
			h.assertTrue(felled[2] < felled[3] && felled[3] < felled[1] && felled[1] < felled[0],
				"gold should fell its tree first, then diamond, iron and stone: " + java.util.Arrays.toString(felled));
		});
	}

	/**
	 * Right-clicks the side of a block straight through the block. Going through the Forge event would not do here:
	 * Create takes any click by a fake player on a saw as setting the saw's filter, wherever the click lands.
	 */
	static InteractionResult clickSide(GameTestHelper h, TestPlayer player, BlockPos pos, Direction face, ItemStack held) {
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		BlockPos abs = h.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).relative(face, .5), face, abs, false);
		return h.getBlockState(pos).use(h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void sawsWorkWithArmsControlsAndPlacement(GameTestHelper h) {
		for (KineticMaterial material : BODIES)
			h.assertTrue(AllTags.AllItemTags.CONTRAPTION_CONTROLLED.matches(new ItemStack(body(material))),
				body(material) + " should be in #create:contraption_controlled");
		BlockPos running = p(2, 2, 2), still = p(2, 2, 6), sideways = p(2, 2, 10);
		upwardSaw(h, running, body(KineticMaterial.COPPER), SawBlades.GOLD, 32);
		upwardSaw(h, still, body(KineticMaterial.STEEL), SawBlades.DIAMOND, 0);
		eastSaw(h, sideways, body(KineticMaterial.IRON), SawBlades.STONE);
		GT.motor(h, sideways.west(), Direction.EAST, 32);

		// clicking a placed tiered saw with any saw places the new one beside it, facing the same way
		BlockPos placed = p(10, 2, 8);
		upwardSaw(h, placed, body(KineticMaterial.IRON), SawBlades.IRON, 0);
		TestPlayer player = GT.player(h, p(10, 2, 4));
		var first = clickSide(h, player, placed, Direction.NORTH, new ItemStack(body(KineticMaterial.ALUMINIUM)));
		var second = clickSide(h, player, placed, Direction.NORTH, new ItemStack(AllBlocks.MECHANICAL_SAW.get()));
		Map<String, String> beside = new LinkedHashMap<>();
		for (Direction d : Direction.Plane.HORIZONTAL) {
			BlockState s = h.getBlockState(placed.relative(d));
			if (s.getBlock() instanceof SawBlock)
				beside.put(s.getBlock().getDescriptionId(), String.valueOf(s.getValue(SawBlock.FACING)));
		}
		h.assertTrue(beside.size() == 2 && beside.containsKey("block.mcremix.aluminium_mechanical_saw")
			&& beside.containsKey("block.create.mechanical_saw") && beside.values().stream().allMatch("up"::equals),
			"an aluminium saw and Create's saw should be placed beside it, facing up: " + beside + " (clicks " + first + ", " + second + ")");

		h.runAtTickTime(10, () -> {
			ArmInteractionPoint point = ArmInteractionPoint.create(h.getLevel(), h.absolutePos(running), h.getBlockState(running));
			h.assertTrue(point != null && point.getType() instanceof AllArmInteractionPointTypes.SawType,
				"a Mechanical Arm should take a running upward copper saw as a point, got " + (point == null ? null : point.getType()));
			h.assertTrue(ArmInteractionPoint.create(h.getLevel(), h.absolutePos(still), h.getBlockState(still)) == null,
				"a saw that is not turning is no arm point, as with Create's");
			h.assertTrue(GT.kinetic(h, sideways).getSpeed() != 0
				&& ArmInteractionPoint.create(h.getLevel(), h.absolutePos(sideways), h.getBlockState(sideways)) == null,
				"a sideways saw is no arm point, even turning");

			for (Item tool : new Item[] { Items.IRON_PICKAXE, Items.IRON_AXE }) {
				BlockPos pos = tool == Items.IRON_AXE ? p(13, 2, 13) : p(13, 2, 2);
				h.setBlock(pos, body(KineticMaterial.STEEL));
				List<ItemStack> drops = GT.breakWith(h, pos, new ItemStack(tool));
				h.assertTrue(drops.size() == 1 && drops.get(0).is(body(KineticMaterial.STEEL).asItem()),
					"a steel saw broken with " + tool + " should drop itself, dropped " + drops);
			}
			h.succeed();
		});
	}

	/** Using a blade on a saw fits it, pops the old one out, and changes what the saw draws from the network. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void bladesAreFittedByUsingThemOnASaw(GameTestHelper h) {
		BlockPos saw = p(4, 2, 4);
		upwardSaw(h, saw, body(KineticMaterial.COPPER), SawBlades.IRON, 32);
		TestPlayer player = GT.player(h, p(4, 2, 1));
		h.startSequence()
			.thenExecuteAfter(10, () -> {
				GT.click(h, player, saw, Direction.NORTH, .5, new ItemStack(SawBlades.GOLD.item(), 2));
				h.assertTrue(SawBlades.of(sawAt(h, saw)) == SawBlades.GOLD, "using a gold blade on the saw should fit it");
				h.assertTrue(player.getMainHandItem().getCount() == 1, "fitting should use one blade, left " + player.getMainHandItem());
				h.assertTrue(itemsAround(h, saw, SawBlades.IRON.item()) == 1, "the iron blade should pop out");
				h.assertTrue(BlockEntityBehaviour.get(sawAt(h, saw), FilteringBehaviour.TYPE).getFilter().isEmpty(), "the click should fit the blade, not set the saw's filter");
				float load = GT.kinetic(h, saw).getOrCreateNetwork().calculateStress();
				h.assertTrue(load == 64, "a gold blade at 32 rpm should load 64 su, loads " + load);
				GT.click(h, player, saw, Direction.NORTH, .5, new ItemStack(SawBlades.DIAMOND.item()));
				h.assertTrue(SawBlades.of(sawAt(h, saw)) == SawBlades.DIAMOND, "a diamond blade should go in");
				h.assertTrue(itemsAround(h, saw, SawBlades.GOLD.item()) == 1, "the gold blade should pop out");
			})
			.thenSucceed();
	}

	/** A deployer holding a blade fits it to the saw it faces; a broken saw keeps its blade. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void deployerFitsBladesAndBrokenSawsKeepThem(GameTestHelper h) {
		BlockPos saw = p(4, 2, 4), deployer = p(4, 4, 4);
		upwardSaw(h, saw, body(KineticMaterial.COPPER), SawBlades.IRON, 0);
		h.setBlock(deployer, AllBlocks.DEPLOYER.getDefaultState().setValue(BlockStateProperties.FACING, Direction.DOWN)
			.setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true));
		GT.motor(h, deployer.east(), Direction.WEST, 32);
		h.startSequence()
			.thenExecuteAfter(5, () -> {
				ItemStack left = h.getBlockEntity(deployer).getCapability(ForgeCapabilities.ITEM_HANDLER)
					.map(inv -> inv.insertItem(0, new ItemStack(SawBlades.GOLD.item()), false)).orElse(null);
				h.assertTrue(left != null && left.isEmpty(), "the deployer should take the blade, left " + left);
			})
			.thenWaitUntil(() -> h.assertTrue(SawBlades.of(sawAt(h, saw)) == SawBlades.GOLD,
				"the deployer should fit its gold blade to the saw"))
			.thenExecuteAfter(5, () -> {
				h.assertTrue(itemsAround(h, saw, SawBlades.IRON.item()) == 1, "the saw's iron blade should pop out");
				h.assertTrue(BlockEntityBehaviour.get(sawAt(h, saw), FilteringBehaviour.TYPE).getFilter().isEmpty(), "the deployer should fit the blade, not set the saw's filter");
				h.setBlock(deployer, Blocks.AIR);
				List<ItemStack> drops = GT.breakWith(h, saw, new ItemStack(Items.IRON_AXE));
				Block copper = body(KineticMaterial.COPPER);
				h.assertTrue(drops.size() == 1 && drops.get(0).is(copper.asItem()) && SawBlades.of(drops.get(0)) == SawBlades.GOLD,
					"the broken saw should drop with its gold blade: " + drops);
			})
			.thenSucceed();
	}

	@GameTest(template = GT.PLATFORM)
	public static void sawBladeRecipes(GameTestHelper h) {
		var ironSheet = AllItems.IRON_SHEET.get();
		Item[] materials = { Items.COBBLESTONE, Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND };
		for (int i = 0; i < ALL.length; i++) {
			Item m = materials[i];
			GearboxTests.expectCraft(h, ALL[i].item(), null, m, null, m, ironSheet, m, null, null, null);
		}
		// Create's saw is still Create's recipe, and comes with an iron blade
		GearboxTests.expectCraft(h, AllBlocks.MECHANICAL_SAW.get(), null, ironSheet, null, ironSheet, Items.IRON_INGOT, ironSheet,
			null, AllBlocks.ANDESITE_CASING.get(), null);
		h.assertTrue(SawBlades.of(new ItemStack(AllBlocks.MECHANICAL_SAW.get())) == SawBlades.IRON, "a new saw has an iron blade");
		h.succeed();
	}
}

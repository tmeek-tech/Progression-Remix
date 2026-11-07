package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.kinetics.FrictionPoints;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.kinetics.KineticPart;
import com.mcremix.kinetics.WoodSpecies;
import com.mcremix.movers.MoverKind;
import com.mcremix.registry.ModMovers;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.contraptions.gantry.GantryCarriageBlock;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.PistonState;
import com.simibubi.create.content.contraptions.piston.PistonExtensionPoleBlock;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Create's bearing, pistons, gantries and rope pulley in every wood (Taran's design, Part 2). Wherever Create's code asks
 * whether something is one of its movers, every wood's counts as Create's own of its kind; those that turn are wooden
 * friction points.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class MoverTests {

	static Block mover(MoverKind kind, WoodSpecies wood) {
		return ModMovers.get(kind, wood).get();
	}

	@GameTest(template = GT.PLATFORM)
	public static void createTakesEveryWoodenMoverForItsOwn(GameTestHelper h) {
		for (MoverKind kind : MoverKind.values())
			for (WoodSpecies wood : WoodSpecies.values()) {
				Block block = mover(kind, wood);
				BlockState state = block.defaultBlockState();
				h.assertTrue(kind.createEntry().has(state),
					"Create should take " + block.getDescriptionId() + " for its " + kind.createId);
				if (kind != MoverKind.HEAD)
					h.assertTrue(kind.createEntry().isIn(new ItemStack(block)),
						"and its item for its " + kind.createId + "'s: " + block.getDescriptionId());
				for (MoverKind other : MoverKind.values())
					h.assertFalse(other != kind && other.createEntry().has(state),
						block.getDescriptionId() + " should not count as Create's " + other.createId);
				h.assertTrue(state.is(BlockTags.MINEABLE_WITH_AXE), block.getDescriptionId() + " should be cut with an axe");
				boolean holdsWhatItMoves = kind == MoverKind.BEARING || kind == MoverKind.PISTON
					|| kind == MoverKind.STICKY_PISTON || kind == MoverKind.ROPE_PULLEY;
				h.assertTrue(AllTags.AllBlockTags.SAFE_NBT.matches(state) == holdsWhatItMoves,
					block.getDescriptionId() + " should be printed with its contents by a schematicannon only if Create's is");
			}
		BlockState oak = mover(MoverKind.STICKY_PISTON, WoodSpecies.OAK).defaultBlockState();
		h.assertTrue(MechanicalPistonBlock.isPiston(oak) && MechanicalPistonBlock.isStickyPiston(oak),
			"an oak sticky piston should be a sticky piston to Create");
		h.assertTrue(MechanicalPistonBlock.isExtensionPole(mover(MoverKind.POLE, WoodSpecies.CRIMSON).defaultBlockState()),
			"a crimson pole should be a pole to Create");
		h.assertFalse(AllBlocks.SHAFT.has(mover(MoverKind.BEARING, WoodSpecies.OAK).defaultBlockState()),
			"a wooden bearing is no shaft");
		h.succeed();
	}

	/**
	 * Create's recipes with the wood's parts (Taran): its stripped log for the andesite casing, its slab, its shaft for
	 * the bearing's and its gearbox for the gantry carriage's cog, a stick between planks for poles and redstone for gantry
	 * shafts, wool and a slab for the rope pulley. Crimson and warped take their stems.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void woodenMoversAreCraftedFromTheirWood(GameTestHelper h) {
		Block oakShaft = WoodTests.part(WoodSpecies.OAK, KineticPart.SHAFT);
		Block oakGearbox = WoodTests.part(WoodSpecies.OAK, KineticPart.GEARBOX);
		Block oakPole = mover(MoverKind.POLE, WoodSpecies.OAK), oakPiston = mover(MoverKind.PISTON, WoodSpecies.OAK);
		expect(h, GT.craft(h, Items.OAK_SLAB, null, null, Items.STRIPPED_OAK_LOG, null, null, oakShaft),
			mover(MoverKind.BEARING, WoodSpecies.OAK), 1);
		expect(h, GT.craft(h, null, Items.OAK_SLAB, null, null, Items.STRIPPED_OAK_WOOD, null, null, oakPole),
			oakPiston, 1);
		expect(h, GT.craft(h, Items.SLIME_BALL, null, null, oakPiston),
			mover(MoverKind.STICKY_PISTON, WoodSpecies.OAK), 1);
		expect(h, GT.craft(h, Items.OAK_PLANKS, null, null, Items.STICK, null, null, Items.OAK_PLANKS), oakPole, 8);
		expect(h, GT.craft(h, Items.OAK_SLAB, null, null, Items.STRIPPED_OAK_LOG, null, null, oakGearbox),
			mover(MoverKind.GANTRY_CARRIAGE, WoodSpecies.OAK), 1);
		expect(h, GT.craft(h, Items.OAK_PLANKS, null, null, Items.REDSTONE, null, null, Items.OAK_PLANKS),
			mover(MoverKind.GANTRY_SHAFT, WoodSpecies.OAK), 8);
		expect(h, GT.craft(h, Items.STRIPPED_OAK_LOG, null, null, Items.WHITE_WOOL, null, null, Items.OAK_SLAB),
			mover(MoverKind.ROPE_PULLEY, WoodSpecies.OAK), 1);
		expect(h, GT.craft(h, Items.CRIMSON_SLAB, null, null, Items.STRIPPED_CRIMSON_STEM, null, null,
			WoodTests.part(WoodSpecies.CRIMSON, KineticPart.SHAFT)), mover(MoverKind.BEARING, WoodSpecies.CRIMSON), 1);
		h.assertTrue(GT.craft(h, Items.SPRUCE_SLAB, null, null, Items.STRIPPED_OAK_LOG, null, null, oakShaft).isEmpty(),
			"a bearing should be all one wood");
		h.succeed();
	}

	private static void expect(GameTestHelper h, ItemStack made, Block block, int count) {
		h.assertTrue(made.is(block.asItem()) && made.getCount() == count, "expected " + count + " "
			+ block.getDescriptionId() + " but the recipe made " + made);
	}

	/**
	 * A wooden piston pushes out its own wood's head on whatever poles are behind it, which go as they are: an oak piston
	 * on spruce poles ends with an oak head on a spruce pole.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void woodenPistonsPushTheirOwnHeadOnAnyPoles(GameTestHelper h) {
		BlockPos piston = p(5, 3, 4);
		Block sprucePole = mover(MoverKind.POLE, WoodSpecies.SPRUCE);
		for (BlockPos pole : List.of(p(4, 3, 4), p(3, 3, 4)))
			h.setBlock(pole, sprucePole.defaultBlockState().setValue(PistonExtensionPoleBlock.FACING, Direction.EAST));
		h.setBlock(piston, mover(MoverKind.PISTON, WoodSpecies.OAK).defaultBlockState()
			.setValue(MechanicalPistonBlock.FACING, Direction.EAST)
			.setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true));
		GT.motor(h, piston.below(), Direction.UP, -32);
		h.succeedWhen(() -> {
			GT.assertIs(h, p(7, 3, 4), mover(MoverKind.HEAD, WoodSpecies.OAK));
			GT.assertIs(h, p(6, 3, 4), sprucePole);
			h.assertTrue(h.getBlockState(piston).getValue(MechanicalPistonBlock.STATE) == PistonState.EXTENDED,
				"the piston should be extended");
		});
	}

	/** A slimeball on a wooden piston's face makes it that wood's sticky piston, as on Create's. */
	@GameTest(template = GT.PLATFORM)
	public static void slimeMakesAWoodenPistonSticky(GameTestHelper h) {
		BlockPos piston = p(4, 2, 4);
		h.setBlock(piston, mover(MoverKind.PISTON, WoodSpecies.BIRCH).defaultBlockState()
			.setValue(MechanicalPistonBlock.FACING, Direction.UP));
		TestPlayer player = GT.player(h, p(4, 2, 6));
		GT.click(h, player, piston, Direction.UP, 0, new ItemStack(Items.SLIME_BALL));
		GT.assertIs(h, piston, mover(MoverKind.STICKY_PISTON, WoodSpecies.BIRCH));
		h.assertTrue(h.getBlockState(piston).getValue(MechanicalPistonBlock.FACING) == Direction.UP,
			"it should still face up");
		h.assertTrue(player.getMainHandItem().isEmpty(), "the slimeball should be used up");
		h.succeed();
	}

	/** A wooden piston's head drops, and is picked as, the pole of its wood, as Create's head is its pole. */
	@GameTest(template = GT.PLATFORM)
	public static void pistonHeadsDropTheirWoodsPole(GameTestHelper h) {
		BlockPos head = p(4, 2, 4);
		BlockState state = mover(MoverKind.HEAD, WoodSpecies.MANGROVE).defaultBlockState();
		h.setBlock(head, state);
		Block pole = mover(MoverKind.POLE, WoodSpecies.MANGROVE);
		ItemStack picked = state.getCloneItemStack(null, h.getLevel(), h.absolutePos(head), GT.player(h, p(4, 2, 6)));
		h.assertTrue(picked.is(pole.asItem()), "picking a mangrove head should give a mangrove pole, not " + picked);
		List<ItemStack> drops = GT.breakWith(h, head, ItemStack.EMPTY);
		h.assertTrue(drops.size() == 1 && drops.get(0).is(pole.asItem()),
			"a mangrove head should drop a mangrove pole: " + drops);
		h.succeed();
	}

	/**
	 * A wooden gantry carriage rides gantry shafts of any wood, Create's among them: an oak carriage on a row of spruce
	 * shafts with one of Create's in it runs to the end of the row.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void woodenCarriagesRideAnyGantryShafts(GameTestHelper h) {
		Block spruce = mover(MoverKind.GANTRY_SHAFT, WoodSpecies.SPRUCE);
		for (int x = 2; x <= 6; x++) {
			Block shaft = x == 4 ? AllBlocks.GANTRY_SHAFT.get() : spruce;
			GantryShaftBlock.Part part = x == 2 ? GantryShaftBlock.Part.START
				: x == 6 ? GantryShaftBlock.Part.END : GantryShaftBlock.Part.MIDDLE;
			h.setBlock(p(x, 3, 4), shaft.defaultBlockState().setValue(GantryShaftBlock.FACING, Direction.EAST)
				.setValue(GantryShaftBlock.PART, part));
		}
		Block carriage = mover(MoverKind.GANTRY_CARRIAGE, WoodSpecies.OAK);
		BlockState carriageState = carriage.defaultBlockState().setValue(GantryCarriageBlock.FACING, Direction.UP)
			.setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, false);
		h.assertTrue(GantryCarriageBlock.isValidGantryShaftAxis(carriageState, h.getBlockState(p(3, 3, 4))),
			"the carriage should sit across the row");
		h.setBlock(p(3, 4, 4), carriageState);
		GT.motor(h, p(1, 3, 4), Direction.EAST, 32);
		h.succeedWhen(() -> {
			h.assertFalse(h.getBlockState(p(3, 4, 4)).is(carriage), "the carriage should have left where it started");
			h.assertTrue(h.getBlockState(p(2, 4, 4)).is(carriage) || h.getBlockState(p(6, 4, 4)).is(carriage),
				"the carriage should have stopped at an end of the row");
		});
	}

	/** A wooden rope pulley lets down Create's rope and magnet with what hangs under it. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void woodenPulleysLetDownCreatesRope(GameTestHelper h) {
		BlockPos pulley = p(4, 5, 4);
		h.setBlock(pulley, mover(MoverKind.ROPE_PULLEY, WoodSpecies.JUNGLE).defaultBlockState()
			.setValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, Direction.Axis.X));
		h.setBlock(pulley.below(), Blocks.JUNGLE_PLANKS);
		GT.motor(h, pulley.west(), Direction.EAST, 32);
		h.succeedWhen(() -> {
			GT.assertIs(h, p(4, 2, 4), Blocks.JUNGLE_PLANKS);
			GT.assertIs(h, p(4, 3, 4), AllBlocks.PULLEY_MAGNET.get());
			GT.assertIs(h, p(4, 4, 4), AllBlocks.ROPE.get());
		});
	}

	/**
	 * Wooden movers are wooden friction points (card, 2026-10-22): they top out at 64 RPM, carry wood's load and draw
	 * wood's friction, which is none, on top of Create's stress. Create's own count as andesite: a bearing of Create's
	 * grinds apart at 256 RPM, an oak one at 128.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 160)
	public static void woodenMoversTopOutAt64Rpm(GameTestHelper h) {
		BlockPos oakFast = p(2, 3, 2), oakSlow = p(5, 3, 2), createFast = p(8, 3, 2), createSlow = p(11, 3, 2);
		Block oak = mover(MoverKind.BEARING, WoodSpecies.OAK);
		int[] speeds = { 128, 64, 256, 128 };
		BlockPos[] at = { oakFast, oakSlow, createFast, createSlow };
		Block[] bearings = { oak, oak, AllBlocks.MECHANICAL_BEARING.get(), AllBlocks.MECHANICAL_BEARING.get() };
		for (int i = 0; i < at.length; i++) {
			GT.motor(h, at[i].below(), Direction.UP, speeds[i]);
			h.setBlock(at[i], bearings[i].defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
		}
		FrictionPoints.Limits limits = FrictionPoints.limitsFor(GT.kinetic(h, oakSlow));
		h.assertTrue(limits.frictionPoint() && limits.speed() == KineticMaterial.WOOD && limits.load() == null,
			"an oak bearing should be a wooden friction point that takes any load: " + limits);
		limits = FrictionPoints.limitsFor(GT.kinetic(h, createSlow));
		h.assertTrue(limits.frictionPoint() && limits.speed() == KineticMaterial.ANDESITE,
			"Create's bearing should be an andesite friction point: " + limits);
		MachineTierTests.assertImpact(h, oakSlow,
			(float) BlockStressValues.getImpact(AllBlocks.MECHANICAL_BEARING.get()),
			"an oak bearing should draw Create's stress and no friction");
		h.runAtTickTime(140, () -> {
			h.assertTrue(h.getBlockState(oakFast).isAir(), "an oak bearing should break at 128 RPM");
			GT.assertIs(h, oakSlow, oak);
			h.assertTrue(h.getBlockState(createFast).isAir(), "Create's bearing should break at 256 RPM");
			GT.assertIs(h, createSlow, AllBlocks.MECHANICAL_BEARING.get());
			h.succeed();
		});
	}
}

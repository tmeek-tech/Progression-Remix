package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.kinetics.Brackets;
import com.mcremix.kinetics.EncasedPartBlockEntity;
import com.mcremix.kinetics.EncasedParts;
import com.mcremix.kinetics.EncasingCasing;
import com.mcremix.kinetics.FrictionPoints;
import com.mcremix.kinetics.KineticFamilies;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.kinetics.KineticPart;
import com.mcremix.kinetics.KineticRatings;
import com.mcremix.kinetics.KineticRatings.Stability;
import com.mcremix.kinetics.KineticRatings.Strength;
import com.mcremix.kinetics.ShaftSupport;
import com.mcremix.kinetics.TieredKinetic;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.decoration.bracket.BracketBlock;
import com.simibubi.create.content.decoration.bracket.BracketBlock.BracketType;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Friction points (Taran's design, 0.15.0): a bare shaft or cog goes only where a friction point holds it, within its
 * material's span, and pops off when that goes; brackets bolt only to solid blocks and stay on through weathering, wax
 * and the axe; casings of every material encase a part of any material and keep it, and since 0.26.0 a cog goes into a
 * placed casing; and a bracket or casing that gives way leaves the part it held.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class FrictionPointTests {

	static Block part(String name) {
		return KineticTests.part(name);
	}

	/** A survival player looking east, so a part it places with nothing beside it lies along x. */
	static TestPlayer playerLookingEast(GameTestHelper h, BlockPos standAt) {
		TestPlayer player = GT.player(h, standAt);
		player.setYRot(-90);
		player.setYHeadRot(-90);
		return player;
	}

	static boolean held(GameTestHelper h, BlockPos rel) {
		return h.getBlockEntity(rel) instanceof ShaftSupport.Holdable holdable && holdable.mcremix$wasHeld();
	}

	/** Whether the player was told above the hotbar that a part won't go there. */
	static boolean toldUnheld(TestPlayer player) {
		return player.actionBar.stream().anyMatch(m -> m.getContents() instanceof TranslatableContents t
			&& t.getKey().startsWith("mcremix.friction_point.unheld"));
	}

	static boolean isAir(GameTestHelper h, BlockPos rel) {
		return h.getBlockState(rel).isAir();
	}

	/** The kind of bare part a block is: a shaft, cog or large cog. */
	static KineticPart kind(Block part) {
		if (part instanceof TieredKinetic tiered)
			return tiered.variant().part();
		return part == AllBlocks.COGWHEEL.get() ? KineticPart.COGWHEEL
			: part == AllBlocks.LARGE_COGWHEEL.get() ? KineticPart.LARGE_COGWHEEL : KineticPart.SHAFT;
	}

	/** A shaft along x in a casing, as a casing used on it would leave it. */
	static void encased(GameTestHelper h, BlockPos rel, Block part, EncasingCasing casing) {
		h.setBlock(rel, EncasedParts.encased(casing, kind(part)).defaultBlockState()
			.setValue(RotatedPillarKineticBlock.AXIS, Axis.X));
		((EncasedPartBlockEntity) h.getBlockEntity(rel)).setInner(part);
	}

	static Block inner(GameTestHelper h, BlockPos rel) {
		return EncasedPartBlockEntity.innerPart(h.getBlockEntity(rel), h.getBlockState(rel)).getBlock();
	}

	static String name(Block block) {
		return BuiltInRegistries.BLOCK.getKey(block).getPath();
	}

	/**
	 * Like a torch: a bare part only goes where a friction point holds it, as far out as its material's span (since
	 * 0.27.0 wood and brass 2, copper and andesite 4, steel 6), counting through the parts between. Sources hold nothing.
	 * Create's helper that runs a line of its shafts on when one is clicked is held to the same rule.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 40)
	public static void barePartsGoOnlyWhereSomethingHoldsThem(GameTestHelper h) {
		Block copper = part("copper_shaft"), birch = part("birch_shaft"), brass = part("brass_shaft"),
			steel = part("steel_shaft");
		BlockPos gearbox = p(6, 2, 7);
		GearboxTests.gearbox(h, gearbox, part("copper_gearbox"), Axis.Y);
		TestPlayer player = playerLookingEast(h, p(14, 2, 14));

		// copper reaches four blocks west of the gearbox, not five
		ItemStack copperShafts = new ItemStack(copper, 8);
		GT.click(h, player, gearbox, Direction.WEST, .5, copperShafts);
		for (int x = 5; x >= 2; x--)
			GT.click(h, player, p(x, 2, 7), Direction.WEST, .5, copperShafts);
		for (int x = 5; x >= 2; x--)
			GT.assertIs(h, p(x, 2, 7), copper);
		h.assertTrue(isAir(h, p(1, 2, 7)), "a copper shaft five blocks from its gearbox should be refused");
		h.assertTrue(copperShafts.getCount() == 4,
			"only the four copper shafts that went down should be used, " + (8 - copperShafts.getCount()) + " were");
		h.assertTrue(toldUnheld(player), "the player should be told why the fifth shaft won't go");

		// wood two, east
		ItemStack birchShafts = new ItemStack(birch, 4);
		GT.click(h, player, gearbox, Direction.EAST, .5, birchShafts);
		GT.click(h, player, p(7, 2, 7), Direction.EAST, .5, birchShafts);
		GT.click(h, player, p(8, 2, 7), Direction.EAST, .5, birchShafts);
		GT.assertIs(h, p(7, 2, 7), birch);
		GT.assertIs(h, p(8, 2, 7), birch);
		h.assertTrue(isAir(h, p(9, 2, 7)), "a wooden shaft three blocks from its gearbox should be refused");

		// brass two too, south
		ItemStack brassShafts = new ItemStack(brass, 4);
		GT.click(h, player, gearbox, Direction.SOUTH, .5, brassShafts);
		GT.click(h, player, p(6, 2, 8), Direction.SOUTH, .5, brassShafts);
		GT.click(h, player, p(6, 2, 9), Direction.SOUTH, .5, brassShafts);
		GT.assertIs(h, p(6, 2, 8), brass);
		GT.assertIs(h, p(6, 2, 9), brass);
		h.assertTrue(isAir(h, p(6, 2, 10)), "a brass shaft three blocks from its gearbox should be refused");

		// steel six, north
		ItemStack steelShafts = new ItemStack(steel, 8);
		GT.click(h, player, gearbox, Direction.NORTH, .5, steelShafts);
		for (int z = 6; z >= 1; z--)
			GT.click(h, player, p(6, 2, z), Direction.NORTH, .5, steelShafts);
		for (int z = 6; z >= 1; z--)
			GT.assertIs(h, p(6, 2, z), steel);
		h.assertTrue(isAir(h, p(6, 2, 0)), "a steel shaft seven blocks from its gearbox should be refused");

		// a motor holds nothing
		GT.motor(h, p(10, 2, 10), Direction.EAST, 0);
		GT.click(h, player, p(10, 2, 10), Direction.EAST, .5, new ItemStack(copper));
		h.assertTrue(isAir(h, p(11, 2, 10)), "a creative motor should not hold a shaft");

		// Create's placement helper places by its own rules, but the shaft still has to be held: andesite reaches four
		BlockPos createBox = p(9, 2, 13);
		GearboxTests.gearbox(h, createBox, AllBlocks.GEARBOX.get(), Axis.Y);
		GT.shaftX(h, p(8, 2, 13), AllBlocks.SHAFT.get());
		ItemStack createShafts = AllBlocks.SHAFT.asStack(8);
		for (int x = 8; x >= 5; x--)
			GT.click(h, player, p(x, 2, 13), Direction.WEST, .5, createShafts);
		for (int x = 7; x >= 5; x--)
			GT.assertIs(h, p(x, 2, 13), AllBlocks.SHAFT.get());
		h.assertTrue(isAir(h, p(4, 2, 13)), "Create's placement helper should not run a line past the shaft's span");
		h.assertTrue(createShafts.getCount() == 5, "only the three Create shafts that went down should be used");

		h.runAtTickTime(5, () -> {
			for (BlockPos placed : new BlockPos[] { p(5, 2, 7), p(2, 2, 7), p(8, 2, 7), p(6, 2, 9), p(6, 2, 1), p(5, 2, 13) })
				h.assertTrue(held(h, placed), "the part at " + placed + " should count as held");
			h.succeed();
		});
	}

	/** With a bracket in the offhand a part can go where nothing holds it, if the bracket can bolt there: it goes on too. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 40)
	public static void aBracketInTheOffhandGoesOnAsThePartIsPlaced(GameTestHelper h) {
		TestPlayer player = playerLookingEast(h, p(14, 2, 14));
		player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(part("copper_bracket"), 2));
		ItemStack shafts = new ItemStack(part("iron_shaft"), 4);
		BlockPos placed = p(4, 2, 4);
		GT.click(h, player, placed.below(), Direction.UP, 0, shafts);
		GT.assertIs(h, placed, part("iron_shaft"));
		BlockState bracket = GT.bracketOn(h, placed);
		h.assertTrue(bracket != null && bracket.is(part("copper_bracket")),
			"the bracket in the offhand should go on with the shaft, it has " + bracket);
		h.assertTrue(Brackets.foot(bracket) == Direction.DOWN, "it should bolt to the floor");
		h.assertTrue(player.getOffhandItem().getCount() == 1, "fitting it should use one bracket");
		h.assertTrue(FrictionPoints.isFrictionPoint(h.getLevel(), h.absolutePos(placed), h.getBlockState(placed)),
			"a shaft with a bolted bracket should be a friction point");

		// out from the side of a post it has nothing to bolt to, since a bracket never bolts along its shaft
		h.setBlock(p(8, 2, 8), Blocks.STONE);
		h.setBlock(p(8, 3, 8), Blocks.STONE);
		GT.click(h, player, p(8, 3, 8), Direction.EAST, .5, shafts);
		h.assertTrue(isAir(h, p(9, 3, 8)), "with nothing for the bracket to bolt to the shaft should be refused");
		h.assertTrue(player.getOffhandItem().getCount() == 1, "the bracket should be kept");
		h.assertTrue(shafts.getCount() == 3, "only the first shaft should be used");

		h.runAtTickTime(5, () -> {
			GT.assertIs(h, placed, part("iron_shaft"));
			h.assertTrue(held(h, placed), "the placed shaft should count as held");
			h.succeed();
		});
	}

	/**
	 * A part pops off as an item once nothing holds it: shafts out from a gearbox that is taken away, all along the line,
	 * and a shaft whose bracket's block goes, the bracket first. A part with a bracket of its own stays, and holds the
	 * next one.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void partsPopOffWhenWhatHoldsThemGoes(GameTestHelper h) {
		Block copper = part("copper_shaft");
		BlockPos gearbox = p(8, 2, 4), near = p(7, 2, 4), far = p(6, 2, 4), bracketed = p(9, 2, 4), beyond = p(10, 2, 4);
		GearboxTests.gearbox(h, gearbox, part("copper_gearbox"), Axis.Y);
		GT.shaftX(h, near, copper);
		GT.shaftX(h, far, copper);
		GT.bracketedX(h, bracketed, copper, part("copper_bracket"));
		GT.shaftX(h, beyond, copper);
		// a shaft held up by its bracket on a block, with nothing else under it
		BlockPos post = p(4, 2, 10), perched = p(4, 3, 10);
		h.setBlock(post, Blocks.STONE);
		GT.bracketedX(h, perched, copper, part("iron_bracket"));

		h.runAtTickTime(5, () -> {
			for (BlockPos pos : new BlockPos[] { near, far, bracketed, beyond, perched })
				h.assertTrue(held(h, pos), "the shaft at " + pos + " should count as held once something holds it");
			h.setBlock(gearbox, Blocks.AIR);
			h.setBlock(post, Blocks.AIR);
		});
		h.runAtTickTime(15, () -> {
			GT.assertNotIs(h, near, copper);
			GT.assertNotIs(h, far, copper);
			h.assertItemEntityPresent(copper.asItem(), far, 1.5);
			GT.assertIs(h, bracketed, copper);
			GT.assertIs(h, beyond, copper);
			GT.assertNotIs(h, perched, copper);
			h.assertItemEntityPresent(part("iron_bracket").asItem(), perched, 2);
			h.succeed();
		});
	}

	/**
	 * Create's speed controller supports the large cog in its top slot (Taran, 0.23.0): the cog stays with nothing else
	 * holding it, holds the shafts along its axis as far as their span, has no top speed of its own, and pops off with
	 * them when the controller goes. A cog lying along the controller's shaft is not in the slot.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void speedControllersHoldTheCogOnTop(GameTestHelper h) {
		Block cog = part("steel_large_cogwheel"), steel = part("steel_shaft");
		BlockPos controller = p(4, 2, 4), top = p(4, 3, 4), across = p(10, 2, 4), along = p(10, 3, 4);
		BlockState rsc = AllBlocks.ROTATION_SPEED_CONTROLLER.getDefaultState()
			.setValue(SpeedControllerBlock.HORIZONTAL_AXIS, Axis.X);
		h.setBlock(controller, rsc);
		h.setBlock(top, cog.defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Axis.Z));
		for (int z = 5; z <= 11; z++)
			h.setBlock(p(4, 3, z), steel.defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Axis.Z));
		h.setBlock(across, rsc);
		h.setBlock(along, cog.defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Axis.X));

		h.runAtTickTime(5, () -> {
			h.assertTrue(FrictionPoints.inSpeedControllerSlot(h.getLevel(), h.absolutePos(top), h.getBlockState(top)),
				"a large cog across the controller's shaft should be in its top slot");
			h.assertFalse(FrictionPoints.inSpeedControllerSlot(h.getLevel(), h.absolutePos(along), h.getBlockState(along)),
				"a cog along the controller's shaft should not be in its slot");
			h.assertTrue(held(h, top), "the cog in the slot should count as held");
			for (int z = 5; z <= 10; z++)
				h.assertTrue(held(h, p(4, 3, z)), "a steel shaft " + (z - 4) + " along the cog should be held by it");
			h.assertTrue(FrictionPoints.distanceToHolder(h.getLevel(), h.absolutePos(p(4, 3, 11)), h.getBlockState(p(4, 3, 11))) == 0,
				"a steel shaft seven along should be past its reach");
			h.assertTrue(FrictionPoints.limitsFor(GT.kinetic(h, top)).speed() == null,
				"the cog in the slot should have no top speed of its own");
			h.setBlock(controller, Blocks.AIR);
		});
		h.runAtTickTime(20, () -> {
			GT.assertNotIs(h, top, cog);
			for (int z = 5; z <= 10; z++)
				GT.assertNotIs(h, p(4, 3, z), steel);
			h.assertItemEntityPresent(cog.asItem(), top, 2);
			h.succeed();
		});
	}

	/**
	 * Every material's Stability and Strength as the tooltips name them (Taran, 0.23.0, reworked in 0.27.0): a support's
	 * top speed from Wobbly to Rock-solid and a shaft or cog's load from Flimsy to Unbreakable, each level with its words
	 * in the lang file. Supports, casings and Create's crushing wheel are rated for Stability alone, and shafts and cogs
	 * for Strength alone.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void tooltipsRateEveryMaterial(GameTestHelper h) {
		Object[][] want = {
			{ KineticMaterial.WOOD, Stability.WOBBLY, Strength.FLIMSY },
			{ KineticMaterial.BRASS, Stability.SHAKY, Strength.DELICATE },
			{ KineticMaterial.ANDESITE, Stability.STEADY, Strength.UNBREAKABLE },
			{ KineticMaterial.COPPER, Stability.STABLE, Strength.LIGHT_DUTY },
			{ KineticMaterial.IRON, Stability.STABLE, Strength.UNBREAKABLE },
			{ KineticMaterial.STEEL, Stability.VERY_STABLE, Strength.UNBREAKABLE },
			{ KineticMaterial.ALUMINIUM, Stability.VERY_STABLE, Strength.STURDY },
			{ KineticMaterial.TITANIUM, Stability.ROCK_SOLID, Strength.UNBREAKABLE } };
		h.assertTrue(want.length == KineticMaterial.values().length, "every material should be rated");
		for (Object[] row : want) {
			KineticMaterial material = (KineticMaterial) row[0];
			h.assertTrue(Stability.of(material) == row[1],
				material + " should be " + row[1] + ", is " + Stability.of(material));
			h.assertTrue(Strength.of(material) == row[2],
				material + " should be " + row[2] + ", is " + Strength.of(material));
		}
		Language lang = Language.getInstance();
		for (String key : new String[] { "mcremix.tooltip.stability", "mcremix.tooltip.strength" })
			h.assertTrue(lang.has(key), "the lang file should name " + key);
		for (Stability level : Stability.values())
			h.assertTrue(lang.has(level.key()), "the lang file should name " + level.key());
		for (Strength level : Strength.values())
			h.assertTrue(lang.has(level.key()), "the lang file should name " + level.key());
		h.assertTrue(lang.getOrDefault(Stability.ROCK_SOLID.key()).equals("Rock-solid"), "titanium's supports are Rock-solid");

		// which each kind of block is rated for
		Object[][] kinds = { { part("copper_shaft"), true, KineticMaterial.COPPER },
			{ part("oak_shaft"), true, KineticMaterial.WOOD }, { part("brass_large_cogwheel"), true, KineticMaterial.BRASS },
			{ AllBlocks.SHAFT.get(), true, KineticMaterial.ANDESITE }, { AllBlocks.COGWHEEL.get(), true, KineticMaterial.ANDESITE },
			{ part("copper_gearbox"), false, KineticMaterial.COPPER }, { part("oak_bracket"), false, KineticMaterial.WOOD },
			{ part("steel_chain_drive"), false, KineticMaterial.STEEL }, { AllBlocks.GEARBOX.get(), false, KineticMaterial.ANDESITE },
			{ AllBlocks.MECHANICAL_PRESS.get(), false, KineticMaterial.ANDESITE },
			{ AllBlocks.CRUSHING_WHEEL.get(), false, KineticMaterial.ANDESITE },
			{ ModBlocks.STEEL_CASING.get(), false, KineticMaterial.STEEL },
			{ AllBlocks.BRASS_CASING.get(), false, KineticMaterial.BRASS } };
		List<String> wrong = new ArrayList<>();
		for (Object[] row : kinds) {
			Block block = (Block) row[0];
			KineticRatings.Rating rating = KineticRatings.of(new ItemStack(block), block.defaultBlockState());
			if (rating == null || rating.strength() != (boolean) row[1] || rating.material() != row[2])
				wrong.add(name(block) + " rated " + rating);
		}
		Block flywheel = AllBlocks.FLYWHEEL.get();
		if (KineticRatings.of(new ItemStack(flywheel), flywheel.defaultBlockState()) != null)
			wrong.add("the flywheel, which holds nothing and has no material, was rated");
		h.assertTrue(wrong.isEmpty(), "ratings: " + wrong);
		h.succeed();
	}

	/** A bracket goes on only where its plate is against a solid block, below, above or beside the part. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 40)
	public static void bracketsBoltOnlyToSolidBlocks(GameTestHelper h) {
		Block copper = part("copper_shaft"), bracket = part("iron_bracket");
		BlockPos high = p(4, 3, 4), low = p(8, 2, 4), byWall = p(12, 3, 4);
		GT.shaftX(h, high, copper);
		GT.shaftX(h, low, copper);
		h.setBlock(p(12, 3, 3), Blocks.STONE);
		GT.shaftX(h, byWall, copper);
		TestPlayer player = GT.player(h, p(1, 2, 14));

		ItemStack brackets = new ItemStack(bracket, 4);
		GT.click(h, player, high, Direction.UP, 0, brackets);
		h.assertTrue(GT.bracketOn(h, high) == null, "a bracket with nothing solid beside the shaft should not go on");
		h.assertTrue(brackets.getCount() == 4, "a bracket that did not go on should not be used");
		h.assertTrue(player.actionBar.stream().anyMatch(m -> m.getContents() instanceof TranslatableContents t
			&& t.getKey().equals("mcremix.bracket.nothing_to_bolt_to")), "the player should be told there is nothing to bolt to");
		h.assertFalse(FrictionPoints.isFrictionPoint(h.getLevel(), h.absolutePos(high), h.getBlockState(high)),
			"a bare shaft with no bracket is no friction point");

		GT.click(h, player, low, Direction.UP, 0, brackets);
		BlockState onFloor = GT.bracketOn(h, low);
		h.assertTrue(onFloor != null && onFloor.is(bracket) && Brackets.foot(onFloor) == Direction.DOWN,
			"clicked from above, the bracket should bolt to the floor, it is " + onFloor);
		h.assertTrue(brackets.getCount() == 3, "fitting a bracket should use one");

		GT.click(h, player, byWall, Direction.SOUTH, .5, brackets);
		BlockState onWall = GT.bracketOn(h, byWall);
		h.assertTrue(onWall != null && Brackets.foot(onWall) == Direction.NORTH,
			"clicked from the south, the bracket should bolt to the wall to the north, it is " + onWall);
		h.assertTrue(FrictionPoints.isFrictionPoint(h.getLevel(), h.absolutePos(byWall), h.getBlockState(byWall)),
			"a shaft with a bolted bracket should be a friction point");
		h.succeed();
	}

	/** Whether the player was told above the hotbar that a bracket has nothing to bolt to. */
	static boolean toldNothingToBoltTo(TestPlayer player) {
		return player.actionBar.stream().anyMatch(m -> m.getContents() instanceof TranslatableContents t
			&& t.getKey().equals("mcremix.bracket.nothing_to_bolt_to"));
	}

	/**
	 * A bracket goes down on its own (Taran, 0.15.1), bolted against the face clicked: its shaft runs the way the player
	 * looks, or lines up with a part pointing at it. Never where nothing solid is beside it, and it falls off when its
	 * block goes. It is no full block, and a wrench turns it a quarter turn on its plate. Create's own brackets too.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 40)
	public static void bracketsGoDownOnTheirOwn(GameTestHelper h) {
		Block bracket = part("copper_bracket"), metal = AllBlocks.METAL_BRACKET.get();
		TestPlayer player = playerLookingEast(h, p(14, 2, 14));
		ItemStack brackets = new ItemStack(bracket, 8);

		BlockPos onFloor = p(4, 2, 4);
		GT.click(h, player, onFloor.below(), Direction.UP, 0, brackets);
		BlockState floor = h.getBlockState(onFloor);
		h.assertTrue(floor.is(bracket), "a bracket used on the floor should go down on its own, found " + floor);
		h.assertTrue(Brackets.foot(floor) == Direction.DOWN, "it should bolt to the floor, its foot is " + Brackets.foot(floor));
		h.assertTrue(Brackets.shaftAxis(floor) == Axis.X,
			"looking east, its shaft should run east, it runs along " + Brackets.shaftAxis(floor));
		h.assertTrue(brackets.getCount() == 7, "it should use one bracket");
		BlockPos abs = h.absolutePos(onFloor);
		h.assertFalse(floor.isCollisionShapeFullBlock(h.getLevel(), abs), "a bracket should not be a full block");
		h.assertTrue(floor.getShape(h.getLevel(), abs).max(Axis.Y) < 1, "a bracket on the floor should stand less than a block high");
		h.assertFalse(floor.isFaceSturdy(h.getLevel(), abs, Direction.UP), "nothing should bolt to the top of a bracket");

		// on a wall, looking down at it: its shaft lines up with the shaft pointing at it
		BlockPos wall = p(8, 3, 8), onWall = p(8, 3, 9);
		h.setBlock(wall.below(), Blocks.STONE);
		h.setBlock(wall, Blocks.STONE);
		GT.shaftX(h, onWall.east(), part("copper_shaft"));
		player.setXRot(60);
		GT.click(h, player, wall, Direction.SOUTH, .5, brackets);
		player.setXRot(0);
		BlockState walled = h.getBlockState(onWall);
		h.assertTrue(walled.is(bracket) && Brackets.foot(walled) == Direction.NORTH,
			"used on the south face of a wall, the bracket should bolt to it, found " + walled);
		h.assertTrue(Brackets.shaftAxis(walled) == Axis.X,
			"its shaft should line up with the shaft beside it, along x, not " + Brackets.shaftAxis(walled));
		GT.click(h, player, onWall, Direction.SOUTH, .5, AllItems.WRENCH.asStack());
		walled = h.getBlockState(onWall);
		h.assertTrue(walled.is(bracket) && Brackets.foot(walled) == Direction.NORTH && Brackets.shaftAxis(walled) == Axis.Y,
			"a wrench should turn it on its plate to take a shaft up the wall, found " + walled);

		// out from the side of an end rod up on a post there is nothing solid to bolt to
		h.setBlock(p(12, 2, 4), Blocks.STONE);
		h.setBlock(p(12, 3, 4), Blocks.END_ROD);
		GT.click(h, player, p(12, 3, 4), Direction.EAST, .5, brackets);
		h.assertTrue(isAir(h, p(13, 3, 4)), "a bracket with nothing solid beside it should not go down");
		h.assertTrue(brackets.getCount() == 6, "a bracket that did not go down should not be used");
		h.assertTrue(toldNothingToBoltTo(player), "the player should be told there is nothing to bolt to");

		// Create's metal bracket goes down too, and drops when its wall goes
		BlockPos metalOnWall = p(8, 2, 9);
		GT.click(h, player, wall.below(), Direction.SOUTH, .5, new ItemStack(metal));
		GT.assertIs(h, metalOnWall, metal);
		h.setBlock(wall.below(), Blocks.AIR);
		h.runAtTickTime(3, () -> {
			h.assertTrue(isAir(h, metalOnWall), "a bracket whose wall goes should fall off");
			h.assertItemEntityPresent(metal.asItem(), metalOnWall, 1.5);
			GT.assertIs(h, onWall, bracket);
			h.succeed();
		});
	}

	/**
	 * A shaft or cog placed into a bracket standing on its own lies along the bracket, whichever way the player looks,
	 * and takes it (Taran, 0.15.1): it is held, with nothing else in reach, and the bracket is fitted for a cog if it is
	 * one. Breaking the part drops both, as before (Taran's call).
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 40)
	public static void aPartPlacedIntoABracketTakesIt(GameTestHelper h) {
		Block bracket = part("iron_bracket"), shaft = part("copper_shaft"), cog = part("copper_cogwheel");
		TestPlayer player = playerLookingEast(h, p(14, 2, 14));
		ItemStack brackets = new ItemStack(bracket, 4);
		BlockPos forShaft = p(4, 2, 4), forCog = p(4, 2, 8);
		GT.click(h, player, forShaft.below(), Direction.UP, 0, brackets);
		GT.click(h, player, forCog.below(), Direction.UP, 0, brackets);
		GT.assertIs(h, forShaft, bracket);
		GT.assertIs(h, forCog, bracket);

		// turned to look north, which would lay a shaft along z anywhere else
		player.setYRot(180);
		player.setYHeadRot(180);
		ItemStack shafts = new ItemStack(shaft, 4);
		GT.click(h, player, forShaft, Direction.UP, 0, shafts);
		BlockState placed = h.getBlockState(forShaft);
		h.assertTrue(placed.is(shaft), "a shaft used on a bracket should go into it, found " + placed);
		h.assertTrue(placed.getValue(RotatedPillarKineticBlock.AXIS) == Axis.X,
			"the shaft should lie along its bracket, along x, not " + placed.getValue(RotatedPillarKineticBlock.AXIS));
		BlockState fitted = GT.bracketOn(h, forShaft);
		h.assertTrue(fitted != null && fitted.is(bracket) && Brackets.foot(fitted) == Direction.DOWN
			&& fitted.getValue(BracketBlock.TYPE) == BracketType.SHAFT, "the shaft should take the bracket, it has " + fitted);
		h.assertTrue(shafts.getCount() == 3 && brackets.getCount() == 2, "placing it should use one shaft and no bracket");
		h.assertTrue(FrictionPoints.isFrictionPoint(h.getLevel(), h.absolutePos(forShaft), placed),
			"a shaft in a bracket should be a friction point");

		// a cog placed from the floor in front of the bracket goes into it too
		GT.click(h, player, forCog.below(), Direction.UP, 0, new ItemStack(cog, 2));
		BlockState cogPlaced = h.getBlockState(forCog);
		h.assertTrue(cogPlaced.is(cog) && cogPlaced.getValue(RotatedPillarKineticBlock.AXIS) == Axis.X,
			"a cog placed where a bracket stands should go into it, along it, found " + cogPlaced);
		BlockState cogBracket = GT.bracketOn(h, forCog);
		h.assertTrue(cogBracket != null && cogBracket.getValue(BracketBlock.TYPE) == BracketType.COG,
			"the cog should take the bracket fitted for a cog, it has " + cogBracket);

		h.runAtTickTime(3, () -> {
			h.assertTrue(held(h, forShaft) && held(h, forCog), "parts placed into brackets should count as held");
			h.getLevel().destroyBlock(h.absolutePos(forShaft), true);
		});
		h.runAtTickTime(6, () -> {
			h.assertTrue(isAir(h, forShaft), "a broken shaft should take its bracket with it");
			h.assertItemEntityPresent(shaft.asItem(), forShaft, 1.5);
			h.assertItemEntityPresent(bracket.asItem(), forShaft, 1.5);
			h.succeed();
		});
	}

	/**
	 * A held cog still answers to Create's own rule: no large cog beside another cog in its plane. Neither a bracket it
	 * goes into nor one in the offhand lets it in.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 20)
	public static void heldCogsKeepCreatesRules(GameTestHelper h) {
		Block large = part("copper_large_cogwheel"), bracket = part("copper_bracket");
		BlockPos first = p(4, 2, 4), beside = p(4, 2, 5);
		GT.bracketedX(h, first, large, bracket);
		h.setBlock(beside, Brackets.standing((BracketBlock) bracket, Direction.DOWN, Axis.X));
		BlockState largeX = large.defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Axis.X);
		h.assertTrue(FrictionPoints.isHeld(h.getLevel(), h.absolutePos(beside), largeX),
			"the bracket standing there would hold the cog");
		h.assertFalse(largeX.canSurvive(h.getLevel(), h.absolutePos(beside)),
			"a large cog beside another in its plane should be refused, held or not");

		// placed with a bracket in the offhand, sneaking to lay it along x
		BlockPos second = p(10, 2, 4), next = p(10, 2, 5);
		GT.bracketedX(h, second, large, bracket);
		h.setBlock(p(9, 2, 5), Blocks.STONE);
		TestPlayer player = playerLookingEast(h, p(14, 2, 14));
		player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(bracket, 2));
		player.setShiftKeyDown(true);
		ItemStack cogs = new ItemStack(large, 2);
		GT.click(h, player, p(9, 2, 5), Direction.EAST, .5, cogs);
		h.assertTrue(isAir(h, next), "a large cog that would clash should be refused even with a bracket in the offhand");
		h.assertTrue(cogs.getCount() == 2 && player.getOffhandItem().getCount() == 2, "nothing should be used");
		h.assertFalse(toldUnheld(player), "the player should not be told it needs holding, since that is not why");
		h.succeed();
	}

	/** A bracketed part that is waxed, scraped or weathers is the same part: it keeps its bracket, and stays held. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 40)
	public static void bracketsStayOnThroughWaxAxeAndWeather(GameTestHelper h) {
		BlockPos waxed = p(4, 2, 4), weathering = p(8, 2, 4);
		GT.bracketedX(h, waxed, part("copper_shaft"), part("copper_bracket"));
		GT.bracketedX(h, weathering, part("copper_shaft"), part("brass_bracket"));
		h.runAtTickTime(3, () -> {
			TestPlayer player = GT.player(h, p(6, 2, 10));
			GT.click(h, player, waxed, Direction.UP, 0, new ItemStack(Items.HONEYCOMB));
			GT.assertIs(h, waxed, part("waxed_copper_shaft"));
			BlockState bracket = GT.bracketOn(h, waxed);
			h.assertTrue(bracket != null && bracket.is(part("copper_bracket")), "waxing should keep the bracket on, it has " + bracket);
			h.assertTrue(held(h, waxed), "a waxed part should still count as held");
			GT.click(h, player, waxed, Direction.UP, 0, new ItemStack(Items.IRON_AXE));
			GT.assertIs(h, waxed, part("copper_shaft"));
			h.assertTrue(GT.bracketOn(h, waxed) != null, "scraping should keep the bracket on");

			KineticTests.randomTicks(h, weathering, RandomSource.create(3), 600);
			GT.assertNotIs(h, weathering, part("copper_shaft"));
			bracket = GT.bracketOn(h, weathering);
			h.assertTrue(bracket != null && bracket.is(part("brass_bracket")), "a part that weathers should keep its bracket, it has " + bracket);
			h.assertTrue(held(h, weathering), "a part that weathers should still count as held");
		});
		h.runAtTickTime(10, () -> {
			GT.assertIs(h, waxed, part("copper_shaft"));
			h.assertTrue(GT.bracketOn(h, waxed) != null, "the scraped shaft should keep its bracket once the change has settled");
			h.assertTrue(GT.bracketOn(h, weathering) != null, "the weathered shaft should keep its bracket once the change has settled");
			h.assertItemEntityNotPresent(part("copper_bracket").asItem(), waxed, 3);
			h.assertItemEntityNotPresent(part("brass_bracket").asItem(), weathering, 3);
			h.succeed();
		});
	}

	/**
	 * Every casing encases a shaft or cog of any material, Create's too: the part stays inside, comes out again when a
	 * wrench, sneaking, takes the casing off, and is what breaking the block drops. The casing is not used up, as with
	 * Create's own encasing. A part whose casing was all that held it drops off once the casing is gone.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 40)
	public static void casingsEncaseAnyPartAndKeepIt(GameTestHelper h) {
		EncasingCasing[] casings = { EncasingCasing.ANDESITE, EncasingCasing.BRASS, EncasingCasing.COPPER,
			EncasingCasing.IRON, EncasingCasing.STEEL, EncasingCasing.ALUMINIUM, EncasingCasing.COPPER };
		Block[] parts = { part("copper_shaft"), part("oak_shaft"), part("rusted_iron_cogwheel"),
			part("brass_large_cogwheel"), part("aluminium_shaft"), part("waxed_exposed_steel_cogwheel"),
			AllBlocks.SHAFT.get() };
		TestPlayer player = GT.player(h, p(8, 2, 14));
		List<String> wrong = new ArrayList<>();
		List<BlockPos> unwrappedAt = new ArrayList<>();
		for (int i = 0; i < casings.length; i++) {
			Block encased = EncasedParts.encased(casings[i], kind(parts[i]));
			String what = name(parts[i]) + " in " + casings[i].prefix() + " casing";
			BlockPos[] spots = { p(1 + i * 2, 2, 2), p(1 + i * 2, 2, 6), p(1 + i * 2, 2, 10) };
			int before = wrong.size();
			for (BlockPos at : spots) {
				GT.shaftX(h, at, parts[i]);
				ItemStack casing = new ItemStack(casings[i].casing());
				GT.click(h, player, at, Direction.UP, 0, casing);
				if (!h.getBlockState(at).is(encased))
					wrong.add(what + " made " + name(h.getBlockState(at).getBlock()));
				else if (inner(h, at) != parts[i])
					wrong.add(what + " holds " + name(inner(h, at)));
				if (casing.getCount() != 1)
					wrong.add(what + " used up the casing");
			}
			if (wrong.size() > before)
				continue;

			// a wrench, sneaking, takes the casing off and leaves the part
			player.setShiftKeyDown(true);
			GT.click(h, player, spots[1], Direction.UP, 0, AllItems.WRENCH.asStack());
			player.setShiftKeyDown(false);
			BlockState unwrapped = h.getBlockState(spots[1]);
			if (!unwrapped.is(parts[i]) || unwrapped.getValue(RotatedPillarKineticBlock.AXIS) != Axis.X)
				wrong.add(what + " wrenched open left " + unwrapped);
			else
				unwrappedAt.add(spots[1]);

			// breaking it drops the part inside
			List<ItemStack> drops = GT.breakWith(h, spots[2], new ItemStack(Items.IRON_PICKAXE));
			if (drops.size() != 1 || !drops.get(0).is(parts[i].asItem()) || drops.get(0).getCount() != 1)
				wrong.add(what + " broke into " + drops);
		}
		// Create's own andesite shaft keeps Create's own encased shaft, which comes open the same way
		BlockPos createCased = p(1, 2, 14);
		GT.shaftX(h, createCased, AllBlocks.SHAFT.get());
		GT.click(h, player, createCased, Direction.UP, 0, AllBlocks.ANDESITE_CASING.asStack());
		if (!h.getBlockState(createCased).is(AllBlocks.ANDESITE_ENCASED_SHAFT.get()))
			wrong.add("Create's shaft in andesite casing made " + name(h.getBlockState(createCased).getBlock()));
		// a part something else holds stays when its casing comes off
		BlockPos kept = p(14, 2, 13);
		GearboxTests.gearbox(h, p(15, 2, 13), part("copper_gearbox"), Axis.Y);
		GT.shaftX(h, kept, part("copper_shaft"));
		GT.click(h, player, kept, Direction.UP, 0, AllBlocks.COPPER_CASING.asStack());
		player.setShiftKeyDown(true);
		GT.click(h, player, createCased, Direction.UP, 0, AllItems.WRENCH.asStack());
		GT.click(h, player, kept, Direction.UP, 0, AllItems.WRENCH.asStack());
		player.setShiftKeyDown(false);
		GT.assertIs(h, createCased, AllBlocks.SHAFT.get());
		GT.assertIs(h, kept, part("copper_shaft"));
		h.assertTrue(wrong.isEmpty(), "encasing: " + wrong);

		h.runAtTickTime(5, () -> {
			for (BlockPos at : unwrappedAt)
				h.assertTrue(isAir(h, at), "the part at " + at + " should drop off once the casing that held it is gone");
			h.assertItemEntityPresent(part("copper_shaft").asItem(), unwrappedAt.get(0), 1.5);
			h.assertTrue(isAir(h, createCased), "Create's shaft should drop off once the casing that held it is gone");
			GT.assertIs(h, kept, part("copper_shaft"));
			h.succeed();
		});
	}

	/**
	 * A cog used on a placed casing goes into it (Taran, 0.26.0): the casing becomes its encased cog, turning across the
	 * face clicked, with the cog inside, a large cog as a large one and Create's cog into Create's own casing as Create's
	 * own encased cog. In survival the cog is used and the casing pops back out of that face, since the encased cog breaks
	 * into the cog alone; in creative neither. Sneaking, the cog goes beside the casing as before, where something holds
	 * it; a deployer holding a cog puts it in too.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void cogsGoIntoPlacedCasings(GameTestHelper h) {
		Block[] cogs = { part("copper_cogwheel"), part("brass_large_cogwheel"), AllBlocks.COGWHEEL.get(),
			AllBlocks.LARGE_COGWHEEL.get() };
		EncasingCasing[] casings = { EncasingCasing.STEEL, EncasingCasing.ANDESITE, EncasingCasing.BRASS,
			EncasingCasing.COPPER };
		Direction[] faces = { Direction.UP, Direction.EAST, Direction.NORTH, Direction.UP };
		Block[] made = { EncasedParts.encased(EncasingCasing.STEEL, KineticPart.COGWHEEL),
			EncasedParts.encased(EncasingCasing.ANDESITE, KineticPart.LARGE_COGWHEEL), AllBlocks.BRASS_ENCASED_COGWHEEL.get(),
			EncasedParts.encased(EncasingCasing.COPPER, KineticPart.LARGE_COGWHEEL) };
		TestPlayer player = GT.player(h, p(8, 2, 14));
		List<String> wrong = new ArrayList<>();
		for (int i = 0; i < cogs.length; i++) {
			BlockPos at = p(1 + i * 4, 2, 2);
			String what = name(cogs[i]) + " into " + casings[i].prefix() + " casing";
			h.setBlock(at, casings[i].casing());
			ItemStack held = new ItemStack(cogs[i], 4);
			GT.click(h, player, at, faces[i], .5, held);
			BlockState state = h.getBlockState(at);
			if (!state.is(made[i]))
				wrong.add(what + " made " + name(state.getBlock()));
			else if (state.getValue(RotatedPillarKineticBlock.AXIS) != faces[i].getAxis())
				wrong.add(what + " turns along " + state.getValue(RotatedPillarKineticBlock.AXIS));
			else if (made[i] != AllBlocks.BRASS_ENCASED_COGWHEEL.get() && inner(h, at) != cogs[i])
				wrong.add(what + " holds " + name(inner(h, at)));
			if (held.getCount() != 3)
				wrong.add(what + " used " + (4 - held.getCount()) + " cogs");
			h.assertItemEntityPresent(casings[i].casing().asItem(), at.relative(faces[i]), 1);
		}

		// in creative the cog is not used and no casing comes out
		BlockPos creative = p(1, 2, 7);
		h.setBlock(creative, ModBlocks.IRON_CASING.get());
		TestPlayer builder = GT.player(h, p(8, 2, 14));
		builder.getAbilities().instabuild = true;
		ItemStack ironCogs = new ItemStack(part("iron_cogwheel"), 4);
		GT.click(h, builder, creative, Direction.UP, 0, ironCogs);
		if (!h.getBlockState(creative).is(EncasedParts.encased(EncasingCasing.IRON, KineticPart.COGWHEEL)))
			wrong.add("in creative, an iron cog into iron casing made " + name(h.getBlockState(creative).getBlock()));
		if (ironCogs.getCount() != 4)
			wrong.add("in creative the cog should not be used");
		h.assertItemEntityNotPresent(ModBlocks.IRON_CASING.get().asItem(), creative.above(), 1.5);

		// sneaking, the cog goes beside the casing, where the gearbox past it holds it
		BlockPos casing = p(5, 2, 7);
		h.setBlock(casing, ModBlocks.ALUMINIUM_CASING.get());
		GearboxTests.gearbox(h, casing.east(2), part("copper_gearbox"), Axis.Y);
		player.setShiftKeyDown(true);
		GT.click(h, player, casing, Direction.EAST, .5, new ItemStack(part("copper_cogwheel"), 4));
		player.setShiftKeyDown(false);
		GT.assertIs(h, casing, ModBlocks.ALUMINIUM_CASING.get());
		GT.assertIs(h, casing.east(), part("copper_cogwheel"));
		h.assertTrue(wrong.isEmpty(), "cogs into casings: " + wrong);

		// a deployer above a titanium casing, holding a steel cog
		BlockPos deployed = p(11, 2, 8), deployer = deployed.above(2);
		h.setBlock(deployed, ModBlocks.TITANIUM_CASING.get());
		h.setBlock(deployer, AllBlocks.DEPLOYER.getDefaultState().setValue(BlockStateProperties.FACING, Direction.DOWN)
			.setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true));
		GT.motor(h, deployer.east(), Direction.WEST, 32);
		h.startSequence()
			.thenExecuteAfter(5, () -> {
				ItemStack left = h.getBlockEntity(deployer).getCapability(ForgeCapabilities.ITEM_HANDLER)
					.map(inv -> inv.insertItem(0, new ItemStack(part("steel_cogwheel")), false)).orElse(null);
				h.assertTrue(left != null && left.isEmpty(), "the deployer should take the cog, left " + left);
			})
			.thenWaitUntil(() -> GT.assertIs(h, deployed, EncasedParts.encased(EncasingCasing.TITANIUM, KineticPart.COGWHEEL)))
			.thenExecute(() -> {
				h.assertTrue(h.getBlockState(deployed).getValue(RotatedPillarKineticBlock.AXIS) == Axis.Y,
					"the deployer's cog should turn upright, across the top it went in through");
				h.assertTrue(inner(h, deployed) == part("steel_cogwheel"), "the titanium casing should hold the steel cog");
			})
			.thenSucceed();
	}

	/**
	 * An encased part's top speed is its casing's, whatever the part is made of (Taran, 0.27.0). Past it the casing gives
	 * way and leaves the part, which stays where something else holds it and pops off where nothing does.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void encasedPartsTakeTheirCasingsTopSpeed(GameTestHelper h) {
		Block copper = part("copper_shaft"), iron = part("iron_shaft"), aluminium = part("aluminium_shaft"),
			andesite = AllBlocks.SHAFT.get();
		GT.motor(h, p(1, 2, 1), Direction.EAST, 192);
		encased(h, p(2, 2, 1), copper, EncasingCasing.STEEL);
		GT.motor(h, p(1, 2, 3), Direction.EAST, 192);
		encased(h, p(2, 2, 3), copper, EncasingCasing.ANDESITE);
		GearboxTests.gearbox(h, p(3, 2, 3), part("copper_gearbox"), Axis.Y);
		GT.motor(h, p(1, 2, 5), Direction.EAST, 192);
		encased(h, p(2, 2, 5), andesite, EncasingCasing.STEEL);
		GT.motor(h, p(1, 2, 7), Direction.EAST, 128);
		encased(h, p(2, 2, 7), iron, EncasingCasing.BRASS);
		GT.motor(h, p(1, 2, 9), Direction.EAST, 256);
		encased(h, p(2, 2, 9), aluminium, EncasingCasing.ALUMINIUM);
		GT.motor(h, p(1, 2, 11), Direction.EAST, 192);
		encased(h, p(2, 2, 11), part("oak_shaft"), EncasingCasing.STEEL);

		h.runAtTickTime(200, () -> {
			GT.assertIs(h, p(2, 2, 1), EncasedParts.encased(EncasingCasing.STEEL, KineticPart.SHAFT));
			h.assertTrue(Math.abs(GT.kinetic(h, p(2, 2, 1)).getSpeed()) == 192, "copper in steel casing should carry 192 rpm");
			GT.assertIs(h, p(2, 2, 3), copper);
			h.assertTrue(Math.abs(GT.kinetic(h, p(2, 2, 3)).getSpeed()) == 192,
				"the copper shaft should turn on at 192 rpm once its andesite casing has given way, held by the gearbox");
			GT.assertIs(h, p(2, 2, 5), EncasedParts.encased(EncasingCasing.STEEL, KineticPart.SHAFT));
			h.assertTrue(Math.abs(GT.kinetic(h, p(2, 2, 5)).getSpeed()) == 192,
				"Create's andesite shaft in steel casing should carry 192 rpm, past andesite's 128");
			h.assertTrue(isAir(h, p(2, 2, 7)), "the iron shaft should pop off once its brass casing has given way");
			h.assertItemEntityPresent(iron.asItem(), p(2, 2, 7), 1.5);
			GT.assertIs(h, p(2, 2, 9), EncasedParts.encased(EncasingCasing.ALUMINIUM, KineticPart.SHAFT));
			h.assertTrue(Math.abs(GT.kinetic(h, p(2, 2, 9)).getSpeed()) == 256, "aluminium in aluminium casing should carry 256 rpm");
			GT.assertIs(h, p(2, 2, 11), EncasedParts.encased(EncasingCasing.STEEL, KineticPart.SHAFT));
			h.assertTrue(inner(h, p(2, 2, 11)) == part("oak_shaft") && Math.abs(GT.kinetic(h, p(2, 2, 11)).getSpeed()) == 192,
				"an oak shaft in steel casing should carry 192 rpm, three times wood's 64");
			h.succeed();
		});
	}

	/**
	 * Only shafts and cogs give way under load, their Strength (Taran, 0.27.0): a copper bracket or casing on an iron
	 * shaft carries a load past copper's and the iron turns on; copper inside an iron casing breaks, casing and all.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void onlyShaftsAndCogsGiveWayUnderLoad(GameTestHelper h) {
		Block iron = part("iron_shaft"), copper = part("copper_shaft");
		GT.motor(h, p(1, 2, 2), Direction.EAST, 256);
		GT.bracketedX(h, p(2, 2, 2), iron, part("copper_bracket"));
		KineticTests.presses(h, 3, 2, 3);
		GT.motor(h, p(1, 2, 6), Direction.EAST, 256);
		encased(h, p(2, 2, 6), iron, EncasingCasing.COPPER);
		KineticTests.presses(h, 3, 6, 3);
		GT.motor(h, p(1, 2, 10), Direction.EAST, 256);
		encased(h, p(2, 2, 10), copper, EncasingCasing.IRON);
		KineticTests.presses(h, 3, 10, 3);
		h.runAtTickTime(20, () -> {
			double impact = BlockStressValues.getImpact(AllBlocks.MECHANICAL_PRESS.get());
			h.assertTrue(impact * 3 * 256 > KineticMaterial.COPPER.maxLoad,
				"test setup assumes three presses at 256 rpm draw more than copper carries, they draw " + impact * 3 * 256);
		});
		h.runAtTickTime(200, () -> {
			GT.assertIs(h, p(2, 2, 2), iron);
			h.assertTrue(GT.bracketOn(h, p(2, 2, 2)) != null && GT.bracketOn(h, p(2, 2, 2)).is(part("copper_bracket")),
				"the copper bracket should stay on under the load");
			h.assertTrue(Math.abs(GT.kinetic(h, p(2, 2, 2)).getSpeed()) == 256, "the iron shaft in its bracket should turn on");
			GT.assertIs(h, p(2, 2, 6), EncasedParts.encased(EncasingCasing.COPPER, KineticPart.SHAFT));
			h.assertTrue(Math.abs(GT.kinetic(h, p(2, 2, 6)).getSpeed()) == 256, "the iron shaft in copper casing should turn on");
			h.assertTrue(isAir(h, p(2, 2, 10)), "copper in iron casing should break, casing and all");
			h.assertItemEntityPresent(copper.asItem(), p(2, 2, 10), 1.5);
			h.succeed();
		});
	}

	/** An encased part draws its casing's friction (iron and steel are heavy to turn) and a little for its weathering. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 40)
	public static void encasedFrictionComesFromTheCasing(GameTestHelper h) {
		Block[] parts = { part("copper_cogwheel"), part("copper_shaft"), part("rusted_iron_shaft"),
			part("exposed_copper_shaft"), part("oak_shaft"), AllBlocks.SHAFT.get() };
		EncasingCasing[] casings = { EncasingCasing.COPPER, EncasingCasing.IRON, EncasingCasing.STEEL,
			EncasingCasing.ALUMINIUM, EncasingCasing.BRASS, EncasingCasing.STEEL };
		float[] friction = { 0f, .125f, .5f, .0625f, 0f, .25f };
		for (int i = 0; i < parts.length; i++)
			encased(h, p(2 + (i % 2) * 6, 2, 1 + i * 2), parts[i], casings[i]);
		h.runAtTickTime(5, () -> {
			List<String> wrong = new ArrayList<>();
			for (int i = 0; i < parts.length; i++) {
				float applied = GT.kinetic(h, p(2 + (i % 2) * 6, 2, 1 + i * 2)).calculateStressApplied();
				if (applied != friction[i])
					wrong.add(name(parts[i]) + " in " + casings[i].prefix() + " casing " + applied + " (want " + friction[i] + ")");
			}
			h.assertTrue(wrong.isEmpty(), "encased friction in su per rpm: " + wrong);
			h.succeed();
		});
	}

	/**
	 * Brass sits just above wood: shafts, cogs, a gearbox, a chain drive and a bracket, but no clutch, gearshift or chain
	 * gearshift (Taran, 0.23.0); its friction points top out at 96 RPM, its parts carry 3072 su, and it never tarnishes.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void brassSitsJustAboveWood(GameTestHelper h) {
		List<String> wrong = new ArrayList<>();
		for (KineticPart kind : KineticPart.values()) {
			var brass = KineticFamilies.get("brass_" + kind.suffix);
			boolean wanted = kind != KineticPart.CLUTCH && kind != KineticPart.GEARSHIFT
				&& kind != KineticPart.CHAIN_GEARSHIFT;
			if (brass == null && wanted)
				wrong.add("brass_" + kind.suffix + " is missing");
			else if (brass != null && !wanted)
				wrong.add("brass_" + kind.suffix + " should be gone");
			else if (brass != null && brass.get().defaultBlockState().isRandomlyTicking())
				wrong.add("brass_" + kind.suffix + " weathers");
			if (KineticMaterial.BRASS.comesAs(kind) != wanted)
				wrong.add("KineticMaterial says brass " + (wanted ? "has no " : "has a ") + kind.suffix);
		}
		h.assertTrue(wrong.isEmpty(), "brass parts: " + wrong);

		GT.motor(h, p(1, 2, 1), Direction.EAST, 128);
		GearboxTests.gearbox(h, p(2, 2, 1), part("brass_gearbox"), Axis.Y);
		GT.motor(h, p(1, 2, 3), Direction.EAST, 96);
		GearboxTests.gearbox(h, p(2, 2, 3), part("brass_gearbox"), Axis.Y);
		// five presses at 96 rpm draw 3840 su: past brass's 3072, within copper's 4096
		GT.motor(h, p(1, 2, 6), Direction.EAST, 96);
		GT.bracketedX(h, p(2, 2, 6), part("brass_shaft"), part("copper_bracket"));
		KineticTests.presses(h, 3, 6, 5);
		GT.motor(h, p(1, 2, 9), Direction.EAST, 96);
		GT.bracketedX(h, p(2, 2, 9), part("copper_shaft"), part("copper_bracket"));
		KineticTests.presses(h, 3, 9, 5);
		h.runAtTickTime(200, () -> {
			GT.assertNotIs(h, p(2, 2, 1), part("brass_gearbox"));
			GT.assertIs(h, p(2, 2, 3), part("brass_gearbox"));
			GT.assertNotIs(h, p(2, 2, 6), part("brass_shaft"));
			GT.assertIs(h, p(2, 2, 9), part("copper_shaft"));
			h.assertTrue(Math.abs(GT.kinetic(h, p(2, 2, 9)).getSpeed()) == 96, "copper should carry the presses that broke brass");
			h.succeed();
		});
	}

	/**
	 * Aluminium friction points are made with polyethylene, a bushing: without one there is no casing or bracket. Aluminium
	 * shafts and cogs take none.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 20)
	public static void aluminiumFrictionPointsTakePolyethylene(GameTestHelper h) {
		var sheet = ModItems.ALUMINIUM_SHEET.get();
		var ingot = ModItems.ALUMINIUM_INGOT.get();
		var nugget = ModItems.ALUMINIUM_NUGGET.get();
		ItemStack casing = GT.craft(h, sheet, Items.STRIPPED_OAK_LOG);
		h.assertTrue(casing.isEmpty(), "aluminium casing should need polyethylene, but a sheet and a stripped log make " + casing);
		ItemStack brackets = GT.craft(h, nugget, nugget, nugget, ingot, sheet, ingot);
		h.assertTrue(brackets.isEmpty(), "aluminium brackets should need polyethylene, but nuggets, ingots and a sheet make " + brackets);
		ItemStack shafts = GT.craft(h, ingot, null, null, ingot);
		h.assertTrue(shafts.is(part("aluminium_shaft").asItem()), "two aluminium ingots should still make aluminium shafts, not " + shafts);

		var bushing = ModItems.POLYETHYLENE_BUSHING.get();
		casing = GT.craft(h, sheet, Items.STRIPPED_OAK_LOG, bushing);
		h.assertTrue(casing.is(ModBlocks.ALUMINIUM_CASING.get().asItem()),
			"a sheet, a stripped log and a polyethylene bushing should make aluminium casing, not " + casing);
		brackets = GT.craft(h, nugget, nugget, nugget, ingot, bushing, ingot);
		h.assertTrue(brackets.is(part("aluminium_bracket").asItem()) && brackets.getCount() == 4,
			"nuggets, ingots and a polyethylene bushing should make four aluminium brackets, not " + brackets);
		h.succeed();
	}
}

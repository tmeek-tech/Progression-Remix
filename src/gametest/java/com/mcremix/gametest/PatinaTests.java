package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import com.mcremix.McRemix;
import com.mcremix.cooling.RadiatorBlockEntity;
import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.Patina;
import com.mcremix.kinetics.Weathering;
import com.mcremix.registry.ModBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Stage 2 of the idea bank: Create's copper pipes, pumps and tanks weather, for looks only. */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class PatinaTests {

	static Patina patina(GameTestHelper h, BlockPos rel) {
		BlockState state = h.getBlockState(rel);
		h.assertTrue(Patina.has(state), "no patina on " + state);
		return state.getValue(Patina.PROPERTY);
	}

	static void setPatina(GameTestHelper h, BlockPos rel, Patina patina) {
		h.setBlock(rel, h.getBlockState(rel).setValue(Patina.PROPERTY, patina));
	}

	/** A player holding {@code stack}, using it on the top of the block at {@code rel}. */
	static UseOnContext use(GameTestHelper h, BlockPos rel, ItemStack stack) {
		Player player = h.makeMockPlayer();
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		BlockPos at = h.absolutePos(rel);
		return new UseOnContext(player, InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(at).add(0, .5, 0), Direction.UP, at, false));
	}

	/**
	 * Create's copper parts take a patina, and of this mod's only the iron and steel ones, which rust (IronRustTests, and
	 * SteelTests for steel, which only gets to its first stage).
	 */
	@GameTest(template = GT.PLATFORM)
	public static void onlyCreatesCopperPartsTakeAPatina(GameTestHelper h) {
		for (Block block : new Block[] { AllBlocks.FLUID_PIPE.get(), AllBlocks.GLASS_FLUID_PIPE.get(),
			AllBlocks.MECHANICAL_PUMP.get(), AllBlocks.FLUID_TANK.get() }) {
			BlockState fresh = block.defaultBlockState();
			h.assertTrue(Patina.has(fresh) && fresh.getValue(Patina.PROPERTY) == Patina.NONE,
				block + " should start fresh, with no wax");
			h.assertTrue(fresh.isRandomlyTicking(), block + " should get random ticks to weather");
			h.assertFalse(fresh.setValue(Patina.PROPERTY, Patina.OXIDIZED).isRandomlyTicking(),
				"an oxidized " + block + " has nowhere further to go");
			h.assertFalse(fresh.setValue(Patina.PROPERTY, Patina.WAXED_EXPOSED).isRandomlyTicking(),
				"a waxed " + block + " stays as it is");
		}
		for (Block block : new Block[] { AllBlocks.CREATIVE_FLUID_TANK.get(), AllBlocks.ENCASED_FLUID_PIPE.get(),
			FluidPartTests.pipe(FluidPartMaterial.BRASS), FluidPartTests.pipe(FluidPartMaterial.GLASS),
			FluidPartTests.pump(FluidPartMaterial.ALUMINIUM), FluidPartTests.tank(FluidPartMaterial.ALUMINIUM) })
			h.assertFalse(Patina.has(block.defaultBlockState()), block + " should not take a patina");
		h.succeed();
	}

	/** They weather by the same rules as copper blocks: within a day, each to a final stage of its own. */
	@GameTest(template = GT.PLATFORM)
	public static void copperPartsWeatherLikeCopper(GameTestHelper h) {
		BlockPos[] spots = new BlockPos[16];
		Block[] blocks = { AllBlocks.FLUID_PIPE.get(), AllBlocks.GLASS_FLUID_PIPE.get(), AllBlocks.MECHANICAL_PUMP.get(),
			AllBlocks.FLUID_TANK.get() };
		for (int i = 0; i < spots.length; i++) {
			spots[i] = p(2 + (i % 4) * 3, 2, 2 + (i / 4) * 3);
			h.setBlock(spots[i], blocks[i % 4]);
		}
		RandomSource random = RandomSource.create(7);
		for (int tick = 0; tick < 200; tick++)
			for (BlockPos spot : spots)
				h.getBlockState(spot).randomTick(h.getLevel(), h.absolutePos(spot), random);
		for (int i = 0; i < spots.length; i++) {
			int stage = patina(h, spots[i]).stage().ordinal();
			h.assertTrue(stage == Weathering.finalStage(h.getLevel(), h.absolutePos(spots[i])),
				h.getBlockState(spots[i]) + " should have weathered to its own final stage");
			GT.assertIs(h, spots[i], blocks[i % 4]);
		}
		h.succeed();
	}

	/**
	 * The copper on water coolers, radiators and arc furnaces weathers too, for looks only (Taran, 0.23.0): each to a
	 * final stage of its own, staying the same block, and an oxidized radiator still joins a fresh one's panel.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void coolersRadiatorsAndArcFurnacesWeather(GameTestHelper h) {
		Block[] blocks = { ModBlocks.WATER_COOLER.get(), ModBlocks.RADIATOR.get(), ModBlocks.ARC_FURNACE.get() };
		BlockPos[] spots = new BlockPos[9];
		for (int i = 0; i < spots.length; i++) {
			spots[i] = p(2 + (i % 3) * 3, 2, 2 + (i / 3) * 3);
			h.setBlock(spots[i], blocks[i % 3]);
			h.assertTrue(patina(h, spots[i]) == Patina.NONE && h.getBlockState(spots[i]).isRandomlyTicking(),
				blocks[i % 3] + " should start fresh and weather");
			h.assertFalse(Patina.rusts(h.getBlockState(spots[i])), blocks[i % 3] + " is copper, which does not rust");
		}
		RandomSource random = RandomSource.create(5);
		for (int tick = 0; tick < 200; tick++)
			for (BlockPos spot : spots)
				h.getBlockState(spot).randomTick(h.getLevel(), h.absolutePos(spot), random);
		for (int i = 0; i < spots.length; i++) {
			h.assertTrue(patina(h, spots[i]).stage().ordinal() == Weathering.finalStage(h.getLevel(),
				h.absolutePos(spots[i])), h.getBlockState(spots[i]) + " should have weathered to its own final stage");
			GT.assertIs(h, spots[i], blocks[i % 3]);
		}

		BlockPos fresh = p(4, 2, 12), oxidized = p(5, 2, 12);
		h.setBlock(fresh, ModBlocks.RADIATOR.get());
		h.setBlock(oxidized, ModBlocks.RADIATOR.getDefaultState().setValue(Patina.PROPERTY, Patina.OXIDIZED));
		h.assertTrue(((RadiatorBlockEntity) h.getBlockEntity(fresh)).panel().size() == 2,
			"an oxidized radiator should join a fresh one's panel");
		h.succeed();
	}

	@GameTest(template = GT.PLATFORM)
	public static void honeycombWaxesAndAnAxeScrapes(GameTestHelper h) {
		BlockPos tank = p(4, 2, 4);
		h.setBlock(tank, AllBlocks.FLUID_TANK.get());
		setPatina(h, tank, Patina.WEATHERED);
		Items.HONEYCOMB.useOn(use(h, tank, new ItemStack(Items.HONEYCOMB)));
		h.assertTrue(patina(h, tank) == Patina.WAXED_WEATHERED, "honeycomb should wax it");
		h.getBlockState(tank).randomTick(h.getLevel(), h.absolutePos(tank), RandomSource.create(1));
		h.assertTrue(patina(h, tank) == Patina.WAXED_WEATHERED, "waxed, it should stay as it is");
		Items.IRON_AXE.useOn(use(h, tank, new ItemStack(Items.IRON_AXE)));
		h.assertTrue(patina(h, tank) == Patina.WEATHERED, "an axe should take the wax off first");
		Items.IRON_AXE.useOn(use(h, tank, new ItemStack(Items.IRON_AXE)));
		h.assertTrue(patina(h, tank) == Patina.EXPOSED, "then scrape a stage off");
		Items.IRON_AXE.useOn(use(h, tank, new ItemStack(Items.IRON_AXE)));
		Items.IRON_AXE.useOn(use(h, tank, new ItemStack(Items.IRON_AXE)));
		h.assertTrue(patina(h, tank) == Patina.NONE, "down to fresh copper, and no further");
		h.succeed();
	}

	/** Broken, a weathered or waxed part drops as it was and goes back down the same; a fresh one drops plain. */
	@GameTest(template = GT.PLATFORM)
	public static void copperPartsDropAsTheyWere(GameTestHelper h) {
		BlockPos pump = p(4, 2, 4), pipe = p(10, 2, 10);
		h.setBlock(pump, AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(PumpBlock.FACING, Direction.EAST));
		setPatina(h, pump, Patina.WAXED_OXIDIZED);
		h.setBlock(pipe, AllBlocks.FLUID_PIPE.get());
		h.getLevel().destroyBlock(h.absolutePos(pump), true);
		h.getLevel().destroyBlock(h.absolutePos(pipe), true);
		ItemStack pumpItem = dropped(h, pump), pipeItem = dropped(h, pipe);
		h.assertTrue(pumpItem.is(AllBlocks.MECHANICAL_PUMP.asItem()) && Patina.of(pumpItem) == Patina.WAXED_OXIDIZED,
			"the pump should drop waxed and oxidized, not " + pumpItem + " " + pumpItem.getTag());
		h.assertTrue(pipeItem.is(AllBlocks.FLUID_PIPE.asItem()) && !pipeItem.hasTag(),
			"a fresh pipe should drop plain, to stack with new ones");
		BlockPos floor = p(7, 1, 7);
		pumpItem.useOn(use(h, floor, pumpItem.copy()));
		h.assertTrue(patina(h, floor.above()) == Patina.WAXED_OXIDIZED, "placed again, the pump should be as it was");
		h.succeed();
	}

	static ItemStack dropped(GameTestHelper h, BlockPos rel) {
		return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(rel)).inflate(1)).stream()
			.map(ItemEntity::getItem).findFirst().orElse(ItemStack.EMPTY);
	}

	/** A wrench turns a weathered pipe to glass and back without losing its patina. */
	@GameTest(template = GT.PLATFORM)
	public static void wrenchedPipesKeepTheirPatina(GameTestHelper h) {
		BlockPos at = p(4, 2, 4);
		FluidPipeBlock pipe = (FluidPipeBlock) AllBlocks.FLUID_PIPE.get();
		h.setBlock(at, pipe.getAxisState(Direction.Axis.X).setValue(Patina.PROPERTY, Patina.WEATHERED));
		// a pipe looks at its flows when wrenched, which it only has once it has ticked
		h.runAtTickTime(5, () -> {
			pipe.onWrenched(h.getBlockState(at), use(h, at, ItemStack.EMPTY));
			GT.assertIs(h, at, AllBlocks.GLASS_FLUID_PIPE.get());
			h.assertTrue(patina(h, at) == Patina.WEATHERED, "the glass pipe should be as weathered as the pipe was");
		});
		h.runAtTickTime(10, () -> {
			((GlassFluidPipeBlock) AllBlocks.GLASS_FLUID_PIPE.get()).onWrenched(h.getBlockState(at),
				use(h, at, ItemStack.EMPTY));
			GT.assertIs(h, at, pipe);
			h.assertTrue(patina(h, at) == Patina.WEATHERED, "and back again");
			h.succeed();
		});
	}

	/** Only for looks: oxidized pipes carry as much as fresh ones. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void weatheredPipesCarryAsMuch(GameTestHelper h) {
		Block pump = AllBlocks.MECHANICAL_PUMP.get(), cog = AllBlocks.COGWHEEL.get(), copper = AllBlocks.FLUID_PIPE.get();
		BlockPos fresh = p(3, 2, 4), oxidized = p(3, 2, 11);
		BlockPos freshEnd = FluidPartTests.line(h, fresh, Fluids.WATER, pump, cog, 64, AllBlocks.FLUID_TANK.get(), copper,
			copper, copper);
		BlockPos oxidizedEnd = FluidPartTests.line(h, oxidized, Fluids.WATER, pump, cog, 64, AllBlocks.FLUID_TANK.get(),
			copper, copper, copper);
		// the pump, the three pipes and the tank
		for (int i = 1; i <= 5; i++)
			setPatina(h, oxidized.east(i), i == 5 ? Patina.WAXED_OXIDIZED : Patina.OXIDIZED);
		h.runAtTickTime(180, () -> {
			int a = FluidPartTests.tankAt(h, freshEnd).getTankInventory().getFluidAmount();
			int b = FluidPartTests.tankAt(h, oxidizedEnd).getTankInventory().getFluidAmount();
			h.assertTrue(a > 0 && b > 0, "water should get through both lines");
			h.assertTrue(Math.abs(a - b) <= Math.max(a, b) / 4,
				"oxidized pipes should carry as much as fresh: " + b + " mB against " + a);
			h.succeed();
		});
	}
}

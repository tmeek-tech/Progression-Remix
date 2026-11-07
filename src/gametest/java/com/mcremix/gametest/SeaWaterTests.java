package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import com.mcremix.McRemix;
import com.mcremix.fluids.Chemical;
import com.mcremix.fluids.WildWater;
import com.mcremix.registry.ModFluids;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Water taken from the sea is salt water, by bucket, pump or hose pulley; water taken anywhere else is water. The test
 * world is flat plains, so each sea test makes its own patch of ocean with the fillbiome command. The game reads the
 * biome at a block from the 4x4x4 biome cells within two blocks of it, picked with a little randomness, so a patch is
 * filled two blocks round the spot the test needs to be sea; the inland tests have no patch at all.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class SeaWaterTests {
	/** Makes an ocean of the biome cells within two blocks of {@code centre}, and checks the game reads it as sea. */
	static void sea(GameTestHelper h, BlockPos centre) {
		BlockPos a = h.absolutePos(centre).offset(-2, -2, -2), b = h.absolutePos(centre).offset(2, 2, 2);
		MinecraftServer server = h.getLevel().getServer();
		CommandSourceStack source = server.createCommandSourceStack().withLevel(h.getLevel()).withSuppressedOutput();
		server.getCommands().performPrefixedCommand(source, String.format("fillbiome %d %d %d %d %d %d minecraft:ocean",
			a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ()));
		h.assertTrue(WildWater.isSea(h.getLevel(), h.absolutePos(centre)), "fillbiome should make " + centre + " sea");
	}

	/** A source of water in a glass box open at the top and on the side given. */
	static void pool(GameTestHelper h, BlockPos pos, Direction open) {
		for (Direction d : Direction.Plane.HORIZONTAL)
			if (d != open)
				h.setBlock(pos.relative(d), Blocks.GLASS);
		h.setBlock(pos, Blocks.WATER);
	}

	/** Dips an empty bucket into the water at {@code pos}, as a player standing over it and looking down. */
	static ItemStack dip(GameTestHelper h, BlockPos pos) {
		TestPlayer player = GT.player(h, pos.above());
		player.setXRot(90);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
		return Items.BUCKET.use(h.getLevel(), player, InteractionHand.MAIN_HAND).getObject();
	}

	@GameTest(template = GT.PLATFORM)
	public static void aBucketDippedInTheSeaComesUpSalt(GameTestHelper h) {
		BlockPos pos = p(8, 2, 8);
		sea(h, pos);
		pool(h, pos, Direction.UP);
		ItemStack bucket = dip(h, pos);
		h.assertTrue(bucket.is(ModFluids.chemical(Chemical.SALT_WATER).bucket().get()),
			"a bucket dipped in the sea should come up full of salt water, has " + bucket);
		h.assertBlockPresent(Blocks.AIR, pos);
		h.succeed();
	}

	@GameTest(template = GT.PLATFORM)
	public static void aBucketDippedInlandComesUpWater(GameTestHelper h) {
		BlockPos pos = p(8, 2, 8);
		h.assertFalse(WildWater.isSea(h.getLevel(), h.absolutePos(pos)), "the plains should not be sea");
		pool(h, pos, Direction.UP);
		ItemStack bucket = dip(h, pos);
		h.assertTrue(bucket.is(Items.WATER_BUCKET), "a bucket dipped inland should come up full of water, has " + bucket);
		h.succeed();
	}

	/** A pump drawing through an open pipe end from a pool in the sea fills a fluid tank with salt water. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aPumpDrawsSaltWaterFromTheSea(GameTestHelper h) {
		BlockPos pool = p(6, 2, 8);
		sea(h, pool);
		BlockPos tank = draw(h, pool);
		h.succeedWhen(() -> {
			FluidStack drawn = tank(h, tank);
			h.assertTrue(drawn.getFluid() == Chemical.SALT_WATER.fluid() && drawn.getAmount() > 0,
				"the pump in the sea should fill its tank with salt water, it holds " + describe(drawn));
		});
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aPumpDrawsWaterInland(GameTestHelper h) {
		BlockPos tank = draw(h, p(6, 2, 8));
		h.succeedWhen(() -> {
			FluidStack drawn = tank(h, tank);
			h.assertTrue(drawn.getFluid() == Fluids.WATER && drawn.getAmount() > 0,
				"the pump inland should fill its tank with water, it holds " + describe(drawn));
		});
	}

	/** A pool, a pipe east of it, a pump pushing east (driven from the south), and a fluid tank; returns the tank. */
	static BlockPos draw(GameTestHelper h, BlockPos pool) {
		pool(h, pool, Direction.EAST);
		BlockPos pipe = pool.east(), pump = pipe.east(), tank = pump.east();
		h.setBlock(pipe, ((FluidPipeBlock) AllBlocks.FLUID_PIPE.get()).getAxisState(Direction.Axis.X));
		h.setBlock(pump, AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(PumpBlock.FACING, Direction.EAST));
		h.setBlock(pump.south(), AllBlocks.COGWHEEL.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
		GT.motor(h, pipe.south(), Direction.EAST, 64);
		h.setBlock(tank, AllBlocks.FLUID_TANK.getDefaultState());
		return tank;
	}

	static FluidStack tank(GameTestHelper h, BlockPos pos) {
		return ((FluidTankBlockEntity) h.getBlockEntity(pos)).getTankInventory().getFluid();
	}

	static String describe(FluidStack stack) {
		return stack.isEmpty() ? "nothing" : stack.getAmount() + " mB of " + stack.getDisplayName().getString();
	}

	/**
	 * A hose pulley let down into a pool in the sea draws salt water. Its hose is let down by setting how far it reaches,
	 * not by turning it, so the test doesn't hang on which way a motor turns it.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aHosePulleyDrawsSaltWaterFromTheSea(GameTestHelper h) {
		BlockPos pulley = p(8, 5, 8), pool = pulley.below(3);
		sea(h, pool);
		for (int dx = -1; dx <= 1; dx++)
			for (int dz = -1; dz <= 1; dz++)
				h.setBlock(pool.offset(dx, 0, dz), Blocks.WATER);
		for (int i = -2; i <= 2; i++)
			for (Direction d : Direction.Plane.HORIZONTAL)
				h.setBlock(pool.relative(d, 2).relative(d.getClockWise(), i), Blocks.GLASS);
		h.setBlock(pulley, AllBlocks.HOSE_PULLEY.getDefaultState());
		BlockEntity be = h.getBlockEntity(pulley);
		CompoundTag tag = be.saveWithoutMetadata();
		CompoundTag offset = tag.getCompound("Offset");
		offset.putFloat("Value", 3);
		offset.putFloat("Target", 3);
		tag.put("Offset", offset);
		be.load(tag);
		IFluidHandler hose = be.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(IllegalStateException::new);
		h.succeedWhen(() -> {
			FluidStack drawn = hose.drain(1000, FluidAction.SIMULATE);
			h.assertTrue(drawn.getFluid() == Chemical.SALT_WATER.fluid(),
				"the hose in the sea should draw salt water, draws " + describe(drawn));
		});
	}
}

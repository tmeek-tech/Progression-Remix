package com.mcremix.fluids;

import java.util.ArrayList;
import java.util.List;

import com.mcremix.config.RemixConfig;
import com.mcremix.electrolysis.Gases;
import com.mcremix.electrolysis.Vat;
import com.simibubi.create.content.fluids.FluidFX;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * Iron rusted through. An iron pipe, pump or tank block at its last stage of rust, and not waxed, leaks: it loses
 * {@link #PER_TICK} mB a tick, a pipe or pump while fluid goes through it (taken from where the line draws), a tank
 * block while its tank holds anything (Taran's design). Liquids drip from it and are lost; gases get out as they do from
 * anything open ({@link Gases#escape}), so a rusted chlorine line poisons the plants round it. Scraping the rust back
 * with an axe, cleaning it with acid or waxing it stops the leak. All of that only with {@code rustedIronLeaks} on (off by
 * default, Taran's call of 2026-10-26); either way a rusted-through block drips.
 */
public class Leaks {
	/** Millibuckets a rusted-through block loses each tick. */
	public static final int PER_TICK = 1;
	/** How often, in ticks, gas that leaked is let go, all that leaked since at once, and the rust looked over again. */
	public static final int INTERVAL = 20;
	private static final Direction[] OUT = { Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST,
		Direction.WEST, Direction.DOWN };

	/** Whether rusted-through blocks lose what they hold or carry ({@code rustedIronLeaks}), not only drip. */
	public static boolean losesFluid() {
		return RemixConfig.RUSTED_IRON_LEAKS.get();
	}

	/** Whether a block is rusted through: iron, at its last stage of rust, and not waxed. Steel never gets there. */
	public static boolean leaks(BlockState state) {
		return state.getBlock() instanceof FluidPartMaterial.Part part && part.material().rusts()
			&& part.material().lastStage() == Patina.OXIDIZED.stage() && Patina.has(state)
			&& state.getValue(Patina.PROPERTY) == Patina.OXIDIZED;
	}

	/** Whether what leaks is a gas, which escapes, rather than a liquid, which drips away. */
	public static boolean isGas(FluidStack fluid) {
		return Chemical.isGas(fluid) || !fluid.isEmpty() && fluid.getFluid().getFluidType().isLighterThanAir();
	}

	/** The blocks of a tank {@code width} by {@code height} from {@code origin} that are rusted through. */
	public static List<BlockPos> leaking(Level level, BlockPos origin, int width, int height) {
		List<BlockPos> leaking = new ArrayList<>();
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++)
				for (int z = 0; z < width; z++) {
					BlockPos pos = origin.offset(x, y, z);
					if (leaks(level.getBlockState(pos)))
						leaking.add(pos);
				}
		return leaking;
	}

	/**
	 * A tank losing what leaks from its rusted blocks this tick: a vat from its heaviest liquid up, then its gases, as
	 * it would leak; any other tank its fluid.
	 */
	public static FluidStack drain(FluidTankBlockEntity controller, int amount) {
		Vat vat = Vat.of(controller);
		return vat != null ? vat.leak(amount) : controller.getTankInventory().drain(amount, FluidAction.EXECUTE);
	}

	/**
	 * Gas held back from what leaked, let go a second's worth at a time, shared between the spots it leaked from.
	 * Returns how much is still held back; a liquid is only lost, and nothing is held back for it.
	 */
	public static int escape(ServerLevel level, List<BlockPos> spots, FluidStack lost, int heldBack) {
		if (lost.isEmpty() || !isGas(lost))
			return 0;
		int held = heldBack + lost.getAmount();
		if (level.getGameTime() % INTERVAL != 0 || spots.isEmpty())
			return held;
		int each = Math.max(1, held / spots.size());
		for (BlockPos pos : spots)
			Gases.escape(level, outside(level, pos), List.of(new FluidStack(lost, each)));
		return 0;
	}

	/** Where gas leaking from {@code pos} gets out: the first open space beside it, above first, or else its middle. */
	private static Vec3 outside(ServerLevel level, BlockPos pos) {
		for (Direction side : OUT) {
			BlockPos next = pos.relative(side);
			if (level.getBlockState(next).getCollisionShape(level, next).isEmpty())
				return Vec3.atCenterOf(next);
		}
		return Vec3.atCenterOf(pos);
	}

	/** Client: a rusted-through pipe or pump drips what goes through it from underneath. */
	public static void drip(Level level, BlockPos pos, FluidTransportBehaviour pipe) {
		RandomSource random = level.random;
		if (random.nextInt(3) != 0)
			return;
		for (Direction side : Direction.values()) {
			PipeConnection.Flow flow = pipe.getFlow(side);
			if (flow == null || flow.fluid.isEmpty() || isGas(flow.fluid))
				continue;
			double x = pos.getX() + .4 + random.nextDouble() * .2, z = pos.getZ() + .4 + random.nextDouble() * .2;
			level.addParticle(FluidFX.getDrippingParticle(flow.fluid), x, pos.getY() + .24, z, 0, 0, 0);
			return;
		}
	}

	/** Client: a rusted-through tank block drips what its tank holds, down an outside wall or from underneath. */
	public static void drip(Level level, BlockPos pos, FluidTankBlockEntity controller) {
		RandomSource random = level.random;
		if (random.nextInt(4) != 0)
			return;
		FluidStack liquid = liquidIn(controller);
		if (liquid.isEmpty())
			return;
		Direction side = Direction.getRandom(random);
		if (side == Direction.UP || level.getBlockState(pos.relative(side)).getBlock() instanceof FluidTankBlock)
			return;
		double x = pos.getX() + .5 + side.getStepX() * .52 + (side.getStepX() == 0 ? random.nextDouble() - .5 : 0) * .8;
		double y = pos.getY() + (side == Direction.DOWN ? -.02 : .1 + random.nextDouble() * .8);
		double z = pos.getZ() + .5 + side.getStepZ() * .52 + (side.getStepZ() == 0 ? random.nextDouble() - .5 : 0) * .8;
		level.addParticle(FluidFX.getDrippingParticle(liquid), x, y, z, 0, 0, 0);
	}

	/** The liquid a tank holds, as the client sees it: a vat's heaviest. */
	private static FluidStack liquidIn(FluidTankBlockEntity controller) {
		Vat vat = Vat.of(controller);
		if (vat == null) {
			FluidStack fluid = controller.getTankInventory().getFluid();
			return isGas(fluid) ? FluidStack.EMPTY : fluid;
		}
		for (Vat.Layer layer : vat.layers())
			if (!layer.fromTop())
				return layer.fluid();
		return FluidStack.EMPTY;
	}
}

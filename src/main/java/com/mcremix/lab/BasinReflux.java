package com.mcremix.lab;

import java.util.Set;

import javax.annotation.Nullable;

import com.mcremix.fluids.Chemical;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.fluid.FluidIngredient;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Basin reflux (the distillation design, Taran: yes): what boils easily boils off a heated basin. A heated basin recipe
 * that takes ethanol, methanol or ethylene dichloride loses a quarter as much again of it as vapour, which costs more
 * for the same result, unless a coil condenser fitted to the basin's side, with water through it, runs the vapour back
 * down.
 */
public final class BasinReflux {
	public static final Set<Chemical> BOILS_OFF = Set.of(Chemical.ETHANOL, Chemical.METHANOL,
		Chemical.ETHYLENE_DICHLORIDE);
	/** How much is lost, as a share of what the recipe takes. */
	public static final int LOST_PART = 4;

	private BasinReflux() {}

	/** After a basin made {@code recipe}: what of it boiled off, if it was heated. */
	public static void after(BasinBlockEntity basin, Recipe<?> recipe) {
		if (!(recipe instanceof ProcessingRecipe<?> processing)
			|| processing.getRequiredHeat() == HeatCondition.NONE || !(basin.getLevel() instanceof ServerLevel level))
			return;
		IFluidHandler tanks = basin.getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(null);
		IFluidHandler inputs = basin.inputTank.getCapability().orElse(null);
		if (tanks == null || inputs == null)
			return;
		for (FluidIngredient ingredient : processing.getFluidIngredients()) {
			if (ingredient.getMatchingFluidStacks().stream().noneMatch(BasinReflux::boilsOff))
				continue;
			int lost = Math.max(1, ingredient.getRequiredAmount() / LOST_PART);
			CoilCondenserBlockEntity condenser = condenser(basin);
			if (condenser != null && condenser.condensable(lost) >= lost) {
				condenser.condense(lost);
				continue;
			}
			for (int i = 0; i < inputs.getTanks(); i++) {
				FluidStack left = inputs.getFluidInTank(i);
				if (left.isEmpty() || !ingredient.test(left) || !boilsOff(left))
					continue;
				tanks.drain(new FluidStack(left, Math.min(lost, left.getAmount())), IFluidHandler.FluidAction.EXECUTE);
				break;
			}
			BlockPos pos = basin.getBlockPos();
			level.sendParticles(ParticleTypes.CLOUD, pos.getX() + .5, pos.getY() + .9, pos.getZ() + .5, 3, .25, .05,
				.25, .01);
			level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.1f, 1.8f);
		}
	}

	private static boolean boilsOff(FluidStack fluid) {
		Chemical chemical = Chemical.of(fluid.getFluid());
		return chemical != null && BOILS_OFF.contains(chemical);
	}

	/** A coil condenser fitted to the basin's side, facing it. */
	@Nullable
	public static CoilCondenserBlockEntity condenser(BasinBlockEntity basin) {
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockPos at = basin.getBlockPos().relative(side);
			BlockState state = basin.getLevel().getBlockState(at);
			if (state.getBlock() instanceof CoilCondenserBlock && state.getValue(CoilCondenserBlock.FACING) == side.getOpposite()
				&& basin.getLevel().getBlockEntity(at) instanceof CoilCondenserBlockEntity condenser)
				return condenser;
		}
		return null;
	}
}

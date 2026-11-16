package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mcremix.fluids.OilWells;
import com.simibubi.create.content.fluids.hosePulley.HosePulleyFluidHandler;
import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * A hose pulley draws crude oil slowly, {@link OilWells#DRAW_PER_TICK} mB a tick however fast its pump turns: the oil
 * is thick and comes up no faster. What it can draw saves up for a second while nothing pulls on it.
 */
@Mixin(value = HosePulleyFluidHandler.class, remap = false)
public abstract class HosePulleyFluidHandlerMixin {
	@Shadow
	private SmartFluidTank internalTank;
	@Shadow
	private FluidDrainingBehaviour drainer;
	@Shadow
	private java.util.function.Supplier<BlockPos> rootPosGetter;

	@Unique
	private int mcremix$allowance;
	@Unique
	private long mcremix$countedTo = Long.MIN_VALUE;

	@ModifyVariable(method = "drainInternal", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int mcremix$slowOil(int maxDrain) {
		return mcremix$drawingCrude() ? Math.min(maxDrain, mcremix$allowance()) : maxDrain;
	}

	@ModifyReturnValue(method = "drainInternal", at = @At("RETURN"))
	private FluidStack mcremix$countOil(FluidStack drawn, @Local(argsOnly = true) FluidAction action) {
		if (action.execute() && !drawn.isEmpty() && OilWells.isCrude(drawn.getFluid()))
			mcremix$allowance = Math.max(0, mcremix$allowance() - drawn.getAmount());
		return drawn;
	}

	@Unique
	private boolean mcremix$drawingCrude() {
		if (!internalTank.isEmpty())
			return OilWells.isCrude(internalTank.getFluid().getFluid());
		BlockPos root = rootPosGetter.get();
		return root != null && OilWells.isCrude(drainer.getDrainableFluid(root).getFluid());
	}

	/** What it may still draw: {@link OilWells#DRAW_PER_TICK} more each tick, saved up to a second's worth. */
	@Unique
	private int mcremix$allowance() {
		Level level = drainer.getWorld();
		long now = level == null ? 0 : level.getGameTime();
		if (now != mcremix$countedTo) {
			long ticks = mcremix$countedTo == Long.MIN_VALUE ? 1 : Math.max(0, now - mcremix$countedTo);
			mcremix$allowance = (int) Math.min(OilWells.SAVED_UP_TO,
				mcremix$allowance + Math.min(ticks, OilWells.SAVED_UP_TO) * OilWells.DRAW_PER_TICK);
			mcremix$countedTo = now;
		}
		return mcremix$allowance;
	}
}

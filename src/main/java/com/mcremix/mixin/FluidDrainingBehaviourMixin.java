package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mcremix.fluids.OilWells;
import com.mcremix.fluids.WildWater;
import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 * A hose pulley lowered into the sea brings up salt water, and where it snows cold water. Lowered into an oil pocket,
 * it finds it endless at once, however small ({@link OilWells}).
 */
@Mixin(value = FluidDrainingBehaviour.class, remap = false)
public abstract class FluidDrainingBehaviourMixin {
	@Shadow
	Fluid fluid;

	/**
	 * Crude oil counts as endless from the first block searched, rather than past Create's threshold; and so it stays
	 * when Create checks the pool again, which would otherwise find it small and start the search over.
	 */
	@ModifyExpressionValue(method = { "continueSearch", "continueValidation" }, at = @At(value = "INVOKE",
		target = "Lcom/simibubi/create/content/fluids/transfer/FluidDrainingBehaviour;maxBlocks()I"))
	private int mcremix$endlessOil(int most) {
		return OilWells.isCrude(fluid) ? 0 : most;
	}

	/** ... whatever the server's bottomless fluid settings say. */
	@ModifyExpressionValue(method = { "continueSearch", "continueValidation" }, at = @At(value = "INVOKE",
		target = "Lcom/simibubi/create/content/fluids/transfer/FluidDrainingBehaviour;canDrainInfinitely(Lnet/minecraft/world/level/material/Fluid;)Z"))
	private boolean mcremix$oilBottomless(boolean bottomless) {
		return bottomless || OilWells.isCrude(fluid);
	}

	@ModifyReturnValue(method = "getDrainableFluid", at = @At("RETURN"))
	private FluidStack mcremix$wildWater(FluidStack drawn, @Local(argsOnly = true) BlockPos root) {
		return WildWater.drawn(((FluidDrainingBehaviour) (Object) this).getWorld(), root, drawn);
	}
}

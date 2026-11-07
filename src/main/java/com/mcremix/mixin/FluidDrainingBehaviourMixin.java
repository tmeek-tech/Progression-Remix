package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mcremix.fluids.WildWater;
import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraftforge.fluids.FluidStack;

/** A hose pulley lowered into the sea brings up salt water, and where it snows cold water. */
@Mixin(value = FluidDrainingBehaviour.class, remap = false)
public abstract class FluidDrainingBehaviourMixin {

	@ModifyReturnValue(method = "getDrainableFluid", at = @At("RETURN"))
	private FluidStack mcremix$wildWater(FluidStack drawn, @Local(argsOnly = true) BlockPos root) {
		return WildWater.drawn(((FluidDrainingBehaviour) (Object) this).getWorld(), root, drawn);
	}
}

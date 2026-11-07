package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mcremix.fluids.WildWater;
import com.simibubi.create.content.fluids.OpenEndedPipe;

import net.minecraftforge.fluids.FluidStack;

/** A pump drawing water through an open pipe end in the sea draws salt water, and where it snows cold water. */
@Mixin(value = OpenEndedPipe.class, remap = false)
public abstract class OpenEndedPipeMixin {

	@ModifyExpressionValue(method = "removeFluidFromSpace", at = @At(value = "NEW", target = "net/minecraftforge/fluids/FluidStack"))
	private FluidStack mcremix$wildWater(FluidStack drawn) {
		OpenEndedPipe pipe = (OpenEndedPipe) (Object) this;
		return WildWater.drawn(pipe.getWorld(), pipe.getOutputPos(), drawn);
	}
}

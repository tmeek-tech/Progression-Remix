package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.fluids.tank.FluidTankBlock;

/** Create's creative tank is a FluidTankBlock told it is creative; only this says which one it is. */
@Mixin(value = FluidTankBlock.class, remap = false)
public interface FluidTankBlockAccessor {
	@Accessor("creative")
	boolean mcremix$isCreative();
}

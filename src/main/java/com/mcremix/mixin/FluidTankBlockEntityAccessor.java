package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

@Mixin(value = FluidTankBlockEntity.class, remap = false)
public interface FluidTankBlockEntityAccessor {
	/** Hands out the tank's fluids afresh, so pipes find a vat's, or the tank's own again once it is no vat. */
	@Invoker("refreshCapability")
	void mcremix$refreshCapability();

	/** Whether its windows are open, so what it holds shows. */
	@Accessor("window")
	boolean mcremix$window();
}

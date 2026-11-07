package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;

@Mixin(value = BasinBlockEntity.class, remap = false)
public interface BasinBlockEntityAccessor {
	/** How hot the basin is kept from below, the Furnace included ({@link BasinBlockEntityMixin}). */
	@Invoker("getHeatLevel")
	HeatLevel mcremix$getHeatLevel();
}

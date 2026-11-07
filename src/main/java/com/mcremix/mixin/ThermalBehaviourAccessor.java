package com.mcremix.mixin;

import javax.annotation.Nullable;

import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.core.BlockPos;

/**
 * What a heat sink needs to know about the Power Grid device it is bolted to: how much heat it takes to warm it a
 * degree, and whether its temperature is really kept by another part of the same machine; and what a radiator or a
 * friction point that heats up needs: how hard the fans blowing on it are blowing.
 */
@Mixin(value = ThermalBehaviour.class, remap = false)
public interface ThermalBehaviourAccessor {
	@Accessor("thermalMass")
	float mcremix$getThermalMass();

	@Accessor("trackedBehaviour")
	@Nullable
	BlockPos mcremix$getTrackedBehaviour();

	/** How many times faster than in still air it sheds heat, for the fans blowing through it (1 with none). */
	@Accessor("totalCoolingFactorMultiplier")
	float mcremix$getCoolingMultiplier();
}

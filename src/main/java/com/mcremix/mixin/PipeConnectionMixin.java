package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;
import com.mcremix.fluids.FluidPartMaterial;
import com.simibubi.create.content.fluids.PipeConnection;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Fluid creeps through a brass pipe at half the pace and a glass one at a quarter, on both sides, so what is seen matches
 * what flows.
 */
@Mixin(value = PipeConnection.class, remap = false)
public abstract class PipeConnectionMixin {
	@ModifyVariable(method = "tickFlowProgress", at = @At("STORE"), ordinal = 0)
	private float mcremix$slowed(float flowSpeed, @Local(argsOnly = true) Level world,
		@Local(argsOnly = true) BlockPos pos) {
		FluidPartMaterial material = FluidPartMaterial.of(world.getBlockState(pos));
		return material == null ? flowSpeed : flowSpeed / material.slowdown();
	}
}

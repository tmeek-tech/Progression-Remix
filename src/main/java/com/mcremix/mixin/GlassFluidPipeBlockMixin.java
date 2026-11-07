package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.sugar.Local;
import com.mcremix.fluids.Patina;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;

import net.minecraft.world.level.block.state.BlockState;

/** A wrench turns a weathered glass pipe back into a copper pipe just as weathered. */
@Mixin(value = GlassFluidPipeBlock.class, remap = false)
public abstract class GlassFluidPipeBlockMixin {

	@ModifyArg(method = "onWrenched", at = @At(value = "INVOKE", remap = true,
		target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
		index = 1)
	private BlockState mcremix$keepPatina(BlockState pipe, @Local(argsOnly = true) BlockState glass) {
		return Patina.copy(glass, pipe);
	}
}

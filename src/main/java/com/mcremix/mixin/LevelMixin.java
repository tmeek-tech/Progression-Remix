package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.kinetics.PartSwaps;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** A shaft or cog that only weathered, or was waxed or scraped, gets its bracket back once its new block is set. */
@Mixin(Level.class)
public abstract class LevelMixin {

	@Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
		at = @At("RETURN"))
	private void mcremix$carryAcrossSwap(BlockPos pos, BlockState state, int flags, int recursionLeft,
		CallbackInfoReturnable<Boolean> cir) {
		PartSwaps.afterSet((Level) (Object) this, pos);
	}
}

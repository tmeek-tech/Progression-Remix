package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.fluids.Patina;
import com.mcremix.kinetics.Weathering;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Create's copper pipes, pumps and tanks get random ticks while they have further to weather, and so does steel that
 * rusted past its first stage before 0.28.0, waxed or not, until it is brought back to it ({@link Weathering}).
 */
@Mixin(Block.class)
public abstract class BlockMixin {

	@Inject(method = "isRandomlyTicking", at = @At("HEAD"), cancellable = true)
	private void mcremix$patinaAges(BlockState state, CallbackInfoReturnable<Boolean> cir) {
		if (Patina.ages(state) || Weathering.pastLastStage(state))
			cir.setReturnValue(true);
	}
}

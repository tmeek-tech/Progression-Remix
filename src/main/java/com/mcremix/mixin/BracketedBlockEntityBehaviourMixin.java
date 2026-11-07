package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.kinetics.Brackets;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;

import net.minecraft.world.level.block.state.BlockState;

/**
 * A bracket fitted, taken off or weathered changes a friction point: its friction, and whether the part it is on still
 * stands. Covers every way it happens, Create's wrench included.
 */
@Mixin(value = BracketedBlockEntityBehaviour.class, remap = false)
public abstract class BracketedBlockEntityBehaviourMixin {

	@Inject(method = "applyBracket", at = @At("TAIL"))
	private void mcremix$fitted(BlockState state, CallbackInfo ci) {
		Brackets.changed((BracketedBlockEntityBehaviour) (Object) this);
	}

	@Inject(method = "removeBracket", at = @At("RETURN"))
	private void mcremix$removed(boolean inOnReplacedContext, CallbackInfoReturnable<BlockState> cir) {
		if (cir.getReturnValue() != null && !inOnReplacedContext)
			Brackets.changed((BracketedBlockEntityBehaviour) (Object) this);
	}
}

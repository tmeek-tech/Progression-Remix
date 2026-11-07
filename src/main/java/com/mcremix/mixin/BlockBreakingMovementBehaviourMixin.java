package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.machines.Fittings;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A drill or saw on a contraption cuts with the bits or blade fitted to it, which come with its block entity's data:
 * a block they cannot cut is left standing, so a piston or gantry runs into it and stops, as it would at bedrock, and
 * what they cut goes at their speed. See {@link Fittings#canBreak}.
 */
@Mixin(value = BlockBreakingMovementBehaviour.class, remap = false)
public abstract class BlockBreakingMovementBehaviourMixin {
	private static final String CAN_BREAK = "Lcom/simibubi/create/content/kinetics/base/BlockBreakingMovementBehaviour;"
		+ "canBreak(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;"
		+ "Lnet/minecraft/world/level/block/state/BlockState;)Z";

	@Redirect(method = "visitNewPosition", at = @At(value = "INVOKE", target = CAN_BREAK))
	private boolean mcremix$fittedOnVisit(BlockBreakingMovementBehaviour self, Level world, BlockPos pos,
		BlockState state, MovementContext context, BlockPos visited) {
		return Fittings.canBreak(self, world, pos, state, context);
	}

	@Redirect(method = "tickBreaker", at = @At(value = "INVOKE", target = CAN_BREAK))
	private boolean mcremix$fittedWhileBreaking(BlockBreakingMovementBehaviour self, Level world, BlockPos pos,
		BlockState state, MovementContext context) {
		return Fittings.canBreak(self, world, pos, state, context);
	}

	@Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
	private void mcremix$fittedSpeed(MovementContext context, CallbackInfoReturnable<Float> cir) {
		float speed = Fittings.speed((BlockBreakingMovementBehaviour) (Object) this, context);
		if (speed != 1)
			cir.setReturnValue(cir.getReturnValueF() * speed);
	}
}

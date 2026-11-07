package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.fluids.PipeMetal;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.FluidTransportBehaviour.AttachmentTypes;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * No fluid goes between plain pipes of different metals ({@link PipeMetal}), however their ends lie: Create opens a
 * straight pipe's far end whatever is there, so an end can still point into a pipe of another metal. Such an end is
 * shut, and shows its rim as against a wall: Create's pipes, its windowed pipe and this mod's.
 */
@Mixin(targets = { "com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity$StandardPipeFluidTransportBehaviour",
	"com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity$StraightPipeFluidTransportBehaviour" },
	remap = false)
public abstract class PipeTransportMixin {
	@Inject(method = "canHaveFlowToward", at = @At("RETURN"), cancellable = true)
	private void mcremix$notIntoOtherMetal(BlockState state, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValue() && PipeMetal.clashToward(((FluidTransportBehaviour) (Object) this).blockEntity,
			state, direction))
			cir.setReturnValue(false);
	}

	@Inject(method = "getRenderedRimAttachment", at = @At("RETURN"), cancellable = true)
	private void mcremix$rimOnOtherMetal(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction direction,
		CallbackInfoReturnable<AttachmentTypes> cir) {
		if (PipeMetal.endsToward(state, direction) && PipeMetal.clash(state, world.getBlockState(pos.relative(direction))))
			cir.setReturnValue(state.getBlock() instanceof GlassFluidPipeBlock ? AttachmentTypes.PARTIAL_RIM
				: AttachmentTypes.RIM);
	}
}

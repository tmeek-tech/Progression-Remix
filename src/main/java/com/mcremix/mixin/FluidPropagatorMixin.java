package com.mcremix.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mcremix.fluids.PipeMetal;
import com.mcremix.fluids.PvcPipes;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * When a pipe changes, Create looks along it for pumps to push again, but only counts its own pump; wooden and steel
 * pumps must be found too, or they stop pushing into a line built onto them. And it looks as far as a pump reaches, in
 * half-blocks, as {@link PumpBlockEntityMixin} counts them: a PVC pipe one, any other pipe two. A pipe's end against a
 * plain pipe of another metal is closed, so nothing pours out of it there ({@link PipeMetal}).
 */
@Mixin(value = FluidPropagator.class, remap = false)
public abstract class FluidPropagatorMixin {
	@WrapOperation(method = "propagateChangedPipe", at = @At(value = "INVOKE",
		target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private static boolean mcremix$anyPump(BlockEntry<?> pump, BlockState state, Operation<Boolean> original) {
		return state.getBlock() instanceof PumpBlock || original.call(pump, state);
	}

	@SuppressWarnings("unchecked")
	@WrapOperation(method = "propagateChangedPipe", at = @At(value = "INVOKE",
		target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
	private static boolean mcremix$halfSteps(List<Object> frontier, Object entry, Operation<Boolean> original,
		@Local(argsOnly = true) LevelAccessor level) {
		Pair<Integer, BlockPos> step = (Pair<Integer, BlockPos>) entry;
		int distance = step.getFirst();
		// the changed pipe itself is where it starts, at nought
		if (distance > 0)
			distance += PvcPipes.reach(level, step.getSecond()) - 1;
		return original.call(frontier, Pair.of(distance, step.getSecond()));
	}

	@WrapOperation(method = "propagateChangedPipe", at = @At(value = "INVOKE",
		target = "Lcom/simibubi/create/content/fluids/FluidPropagator;getPumpRange()I"))
	private static int mcremix$inHalfBlocks(Operation<Integer> original) {
		return original.call() * 2;
	}

	@Inject(method = "isOpenEnd", at = @At("HEAD"), cancellable = true)
	private static void mcremix$closedOnOtherMetal(BlockGetter level, BlockPos pos, Direction side,
		CallbackInfoReturnable<Boolean> cir) {
		if (PipeMetal.clash(level.getBlockState(pos), level.getBlockState(pos.relative(side))))
			cir.setReturnValue(false);
	}
}

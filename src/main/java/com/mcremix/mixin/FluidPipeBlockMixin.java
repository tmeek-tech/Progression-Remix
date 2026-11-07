package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mcremix.fluids.Patina;
import com.mcremix.fluids.PipeMetal;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A pipe joins a plain pipe beside it only of its own metal, and shows its rim against one of another ({@link PipeMetal});
 * and a wrench turns a weathered copper pipe into a glass pipe just as weathered.
 */
@Mixin(value = FluidPipeBlock.class, remap = false)
public abstract class FluidPipeBlockMixin {

	@ModifyArg(method = "onWrenched", at = @At(value = "INVOKE", remap = true,
		target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"),
		index = 1)
	private BlockState mcremix$keepPatina(BlockState glass, @Local(argsOnly = true) BlockState pipe) {
		return Patina.copy(pipe, glass);
	}

	@WrapOperation(method = "updateBlockState", at = @At(value = "INVOKE",
		target = "Lcom/simibubi/create/content/fluids/pipes/FluidPipeBlock;canConnectTo(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"))
	private boolean mcremix$ownMetal(BlockAndTintGetter level, BlockPos at, BlockState neighbour, Direction side,
		Operation<Boolean> original, @Local(argsOnly = true) BlockState state) {
		return !PipeMetal.clash(state, neighbour) && original.call(level, at, neighbour, side);
	}

	@WrapOperation(method = "shouldDrawRim", at = @At(value = "INVOKE",
		target = "Lcom/simibubi/create/content/fluids/pipes/FluidPipeBlock;canConnectTo(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"))
	private static boolean mcremix$rimOnOtherMetal(BlockAndTintGetter level, BlockPos at, BlockState neighbour,
		Direction side, Operation<Boolean> original, @Local(argsOnly = true) BlockState state) {
		return !PipeMetal.clash(state, neighbour) && original.call(level, at, neighbour, side);
	}
}

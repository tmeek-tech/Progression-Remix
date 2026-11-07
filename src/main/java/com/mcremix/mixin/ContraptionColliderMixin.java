package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mcremix.machines.Fittings;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/**
 * A piston or gantry pushing a drill or saw into a block its bits or blade cannot cut stops there, as it would at
 * bedrock: Create asks the behaviour without the drill's data, so the fitted bits are read from the block's own.
 */
@Mixin(value = ContraptionCollider.class, remap = false)
public abstract class ContraptionColliderMixin {
	@WrapOperation(method = "isCollidingWithWorld", at = @At(value = "INVOKE",
		target = "Lcom/simibubi/create/content/kinetics/base/BlockBreakingMovementBehaviour;"
			+ "canBreak(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;"
			+ "Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private static boolean mcremix$fitted(BlockBreakingMovementBehaviour behaviour, Level world, BlockPos pos,
		BlockState state, Operation<Boolean> original, @Local StructureBlockInfo blockInfo) {
		return original.call(behaviour, world, pos, state) && Fittings.cuts(behaviour, blockInfo.nbt(), state);
	}
}

package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mcremix.client.ModPartialModels;
import com.simibubi.create.content.contraptions.bearing.StabilizedBearingMovementBehaviour;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.world.level.block.state.BlockState;

/** Without Flywheel, a wooden bearing carried on a contraption keeps its wood's top (ModPartialModels#machinePart). */
@Mixin(value = StabilizedBearingMovementBehaviour.class, remap = false)
public abstract class StabilizedBearingMovementBehaviourMixin {
	@WrapOperation(method = "renderInContraption", at = @At(value = "INVOKE",
		target = "Lnet/createmod/catnip/render/CachedBuffers;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"
			+ "Lnet/minecraft/world/level/block/state/BlockState;)Lnet/createmod/catnip/render/SuperByteBuffer;"))
	private SuperByteBuffer mcremix$woodenTop(PartialModel part, BlockState state, Operation<SuperByteBuffer> original) {
		return original.call(ModPartialModels.machinePart(part, state), state);
	}
}

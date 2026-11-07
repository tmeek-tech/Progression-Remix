package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mcremix.client.ModPartialModels;
import com.simibubi.create.content.contraptions.bearing.BearingRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Without Flywheel, a wooden bearing turns its top and shaft in its wood (ModPartialModels#machinePart). */
@Mixin(value = BearingRenderer.class, remap = false)
public abstract class BearingRendererMixin {
	private static final String PARTIAL = "Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"
		+ "Lnet/minecraft/world/level/block/state/BlockState;";

	@WrapOperation(method = "renderSafe", at = @At(value = "INVOKE",
		target = "Lnet/createmod/catnip/render/CachedBuffers;partial(" + PARTIAL
			+ ")Lnet/createmod/catnip/render/SuperByteBuffer;"))
	private SuperByteBuffer mcremix$woodenTop(PartialModel part, BlockState state, Operation<SuperByteBuffer> original) {
		return original.call(ModPartialModels.machinePart(part, state), state);
	}

	@WrapOperation(method = "getRotatedModel", at = @At(value = "INVOKE",
		target = "Lnet/createmod/catnip/render/CachedBuffers;partialFacing(" + PARTIAL
			+ "Lnet/minecraft/core/Direction;)Lnet/createmod/catnip/render/SuperByteBuffer;"))
	private SuperByteBuffer mcremix$woodenShaft(PartialModel part, BlockState state, Direction facing,
		Operation<SuperByteBuffer> original) {
		return original.call(ModPartialModels.machinePart(part, state), state, facing);
	}
}

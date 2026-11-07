package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mcremix.client.ModPartialModels;
import com.simibubi.create.content.fluids.tank.FluidTankRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A boiler's gauge is drawn in the metal of its tanks (Taran, 0.26.0), rusting or weathering with them
 * ({@link ModPartialModels#boilerGauge}). This mod's tanks are drawn by Create's renderer too.
 */
@Mixin(value = FluidTankRenderer.class, remap = false)
public abstract class FluidTankRendererMixin {

	@WrapOperation(method = "renderAsBoiler", at = @At(value = "INVOKE",
		target = "Lnet/createmod/catnip/render/CachedBuffers;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"
			+ "Lnet/minecraft/world/level/block/state/BlockState;)Lnet/createmod/catnip/render/SuperByteBuffer;"))
	private SuperByteBuffer mcremix$gaugeInTankMetal(PartialModel part, BlockState tank,
		Operation<SuperByteBuffer> original) {
		return original.call(ModPartialModels.boilerGauge(part, tank), tank);
	}
}

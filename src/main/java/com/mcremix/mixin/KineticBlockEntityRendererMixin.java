package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mcremix.client.ModPartialModels;
import com.mcremix.client.Wobble;
import com.mcremix.kinetics.KineticLimits;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.lib.transform.Affine;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A wobbling part ({@link KineticLimits}) drawn by Create's renderers, with Flywheel off and in Ponder, spins about the
 * same tilted axis as its Flywheel instance ({@link RotatingInstanceMixin}). And a wooden piston, gantry carriage or
 * rope pulley turns its wood's shaft where Create's turns Create's (ModPartialModels#moverShaft).
 */
@Mixin(value = KineticBlockEntityRenderer.class, remap = false)
public abstract class KineticBlockEntityRendererMixin {
	@WrapOperation(method = "kineticRotationTransform", at = @At(value = "INVOKE",
		target = "Lnet/createmod/catnip/render/SuperByteBuffer;rotateCentered(FLnet/minecraft/core/Direction;)Ldev/engine_room/flywheel/lib/transform/Affine;"))
	private static Affine<?> mcremix$wobble(SuperByteBuffer buffer, float angle, Direction direction,
		Operation<Affine<?>> original, @Local(argsOnly = true) KineticBlockEntity be) {
		if (!KineticLimits.wobbling(be))
			return original.call(buffer, angle, direction);
		return buffer.rotateCentered(angle, Wobble.axis(direction.getAxis(), be.getBlockPos()));
	}

	@WrapOperation(method = "renderSafe", at = @At(value = "INVOKE",
		target = "Lcom/simibubi/create/content/kinetics/base/KineticBlockEntityRenderer;getRenderedBlockState(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)Lnet/minecraft/world/level/block/state/BlockState;"))
	private BlockState mcremix$woodenShaft(KineticBlockEntityRenderer<?> renderer, KineticBlockEntity be,
		Operation<BlockState> original) {
		return ModPartialModels.moverShaft(original.call(renderer, be), be.getBlockState());
	}
}

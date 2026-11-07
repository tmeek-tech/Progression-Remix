package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mcremix.client.TieredSawRenderer;
import com.mcremix.sawing.SawBlades;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.kinetics.saw.SawMovementBehaviour;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import net.minecraft.client.renderer.MultiBufferSource;

/** A saw on a contraption shows its own blade. The shaft is Create's actor visual. Other mods' saws keep theirs. */
@Mixin(value = SawMovementBehaviour.class, remap = false)
public abstract class SawMovementBehaviourClientMixin {

	@Inject(method = "renderInContraption", at = @At("HEAD"), cancellable = true)
	private void mcremix$fittedBlade(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, MultiBufferSource buffer, CallbackInfo ci) {
		if (((Object) this).getClass() != SawMovementBehaviour.class)
			return;
		ci.cancel();
		TieredSawRenderer.renderInContraption(context, renderWorld, matrices, buffer,
			SawBlades.of(context.blockEntityData));
	}
}

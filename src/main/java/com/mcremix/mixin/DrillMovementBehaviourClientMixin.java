package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.client.ModPartialModels;
import com.mcremix.client.TieredDrillVisual;
import com.mcremix.drilling.DrillBits;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.kinetics.drill.DrillMovementBehaviour;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.renderer.MultiBufferSource;

/** A drill on a contraption spins its own bits, with Flywheel or without. Other mods' drills keep their heads. */
@Mixin(value = DrillMovementBehaviour.class, remap = false)
public abstract class DrillMovementBehaviourClientMixin {

	@Inject(method = "renderInContraption", at = @At("HEAD"), cancellable = true)
	private void mcremix$fittedHead(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, MultiBufferSource buffer, CallbackInfo ci) {
		if (((Object) this).getClass() != DrillMovementBehaviour.class)
			return;
		ci.cancel();
		if (!VisualizationManager.supportsVisualization(context.world))
			TieredDrillVisual.renderInContraption(context, renderWorld, matrices, buffer,
				ModPartialModels.drillHead(DrillBits.of(context.blockEntityData), context.state));
	}

	@Inject(method = "createVisual", at = @At("HEAD"), cancellable = true)
	private void mcremix$fittedVisual(VisualizationContext visualizationContext, VirtualRenderWorld simulationWorld,
		MovementContext movementContext, CallbackInfoReturnable<ActorVisual> cir) {
		if (((Object) this).getClass() == DrillMovementBehaviour.class)
			cir.setReturnValue(new TieredDrillVisual.Actor(visualizationContext, simulationWorld, movementContext,
				ModPartialModels.drillHead(DrillBits.of(movementContext.blockEntityData),
					movementContext.state)));
	}
}

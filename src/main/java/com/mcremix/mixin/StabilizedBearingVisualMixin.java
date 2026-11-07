package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mcremix.client.ModPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.bearing.StabilizedBearingVisual;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

/** A wooden bearing carried on a contraption keeps its wood's top and half shaft (ModPartialModels#machinePart). */
@Mixin(value = StabilizedBearingVisual.class, remap = false)
public abstract class StabilizedBearingVisualMixin {
	private static final String PART = ":Ldev/engine_room/flywheel/lib/model/baked/PartialModel;";

	@ModifyExpressionValue(method = "<init>", at = {
		@At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;BEARING_TOP" + PART),
		@At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;SHAFT_HALF" + PART) })
	private PartialModel mcremix$wooden(PartialModel part, @Local(argsOnly = true) MovementContext context) {
		return ModPartialModels.machinePart(part, context.state);
	}
}

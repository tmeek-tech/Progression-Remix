package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mcremix.client.ModPartialModels;
import com.simibubi.create.content.contraptions.bearing.BearingVisual;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;

import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.Direction;

/**
 * A wooden bearing's top is built in its wood (ModPartialModels#machinePart); its half shaft is swapped where every
 * turning model is set up (OrientedRotatingVisualMixin).
 */
@Mixin(value = BearingVisual.class, remap = false)
public abstract class BearingVisualMixin<B extends KineticBlockEntity> extends OrientedRotatingVisual<B> {
	private BearingVisualMixin(VisualizationContext context, B blockEntity, float partialTick, Direction from,
		Direction to, Model model) {
		super(context, blockEntity, partialTick, from, to, model);
	}

	@ModifyExpressionValue(method = "<init>", at = @At(value = "FIELD",
		target = "Lcom/simibubi/create/AllPartialModels;BEARING_TOP:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"))
	private PartialModel mcremix$woodenTop(PartialModel top) {
		return ModPartialModels.machinePart(top, blockEntity.getBlockState());
	}
}

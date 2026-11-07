package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mcremix.client.ModPartialModels;
import com.simibubi.create.content.contraptions.gantry.GantryCarriageBlockEntity;
import com.simibubi.create.content.contraptions.gantry.GantryCarriageVisual;
import com.simibubi.create.content.kinetics.base.ShaftVisual;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;

/**
 * A wooden gantry carriage's cogs are built in its wood (ModPartialModels#machinePart); its shaft is swapped where every
 * turning model is set up (SingleAxisRotatingVisualMixin).
 */
@Mixin(value = GantryCarriageVisual.class, remap = false)
public abstract class GantryCarriageVisualMixin extends ShaftVisual<GantryCarriageBlockEntity> {
	private GantryCarriageVisualMixin(VisualizationContext context, GantryCarriageBlockEntity blockEntity,
		float partialTick) {
		super(context, blockEntity, partialTick);
	}

	@ModifyExpressionValue(method = "<init>", at = @At(value = "FIELD",
		target = "Lcom/simibubi/create/AllPartialModels;GANTRY_COGS:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"))
	private PartialModel mcremix$woodenCogs(PartialModel cogs) {
		return ModPartialModels.machinePart(cogs, blockEntity.getBlockState());
	}
}

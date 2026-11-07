package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mcremix.client.ModPartialModels;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmVisual;

import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;

/**
 * An arm of a machine tier is built in its tier's look (ModPartialModels#machinePart); its cog stays Create's. A
 * weathered arm is built again, as Flywheel builds a block entity's visual again when its block changes.
 */
@Mixin(value = ArmVisual.class, remap = false)
public abstract class ArmVisualMixin extends SingleAxisRotatingVisual<ArmBlockEntity> {
	private static final String PART = ":Ldev/engine_room/flywheel/lib/model/baked/PartialModel;";

	private ArmVisualMixin(VisualizationContext context, ArmBlockEntity blockEntity, float partialTick, Model model) {
		super(context, blockEntity, partialTick, model);
	}

	@ModifyExpressionValue(method = { "<init>", "update" }, at = {
		@At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;ARM_BASE" + PART),
		@At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;ARM_LOWER_BODY" + PART),
		@At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;ARM_UPPER_BODY" + PART),
		@At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;ARM_CLAW_BASE" + PART),
		@At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;ARM_CLAW_BASE_GOGGLES" + PART),
		@At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;ARM_CLAW_GRIP_UPPER" + PART),
		@At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;ARM_CLAW_GRIP_LOWER" + PART) })
	private PartialModel mcremix$tier(PartialModel part) {
		return ModPartialModels.machinePart(part, blockEntity.getBlockState());
	}
}

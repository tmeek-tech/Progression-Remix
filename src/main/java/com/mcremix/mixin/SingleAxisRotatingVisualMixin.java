package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mcremix.client.ModPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;

import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.api.model.Model;

/**
 * A wooden piston, gantry carriage or rope pulley spins its wood's shaft where Create's visual spins Create's
 * (ModPartialModels#moverModel). Create builds the shaft before its own visual's code runs, so it is swapped here,
 * where every model that turns about one axis is set up.
 */
@Mixin(value = SingleAxisRotatingVisual.class, remap = false)
public abstract class SingleAxisRotatingVisualMixin {
	@WrapOperation(method = "<init>(Ldev/engine_room/flywheel/api/visualization/VisualizationContext;"
		+ "Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;FLnet/minecraft/core/Direction;"
		+ "Ldev/engine_room/flywheel/api/model/Model;)V", at = @At(value = "INVOKE",
		target = "Ldev/engine_room/flywheel/api/instance/InstancerProvider;instancer(Ldev/engine_room/flywheel/api/instance/InstanceType;Ldev/engine_room/flywheel/api/model/Model;)Ldev/engine_room/flywheel/api/instance/Instancer;"))
	private Instancer<?> mcremix$woodenShaft(InstancerProvider provider, InstanceType<?> type, Model model,
		Operation<Instancer<?>> original, @Local(argsOnly = true) KineticBlockEntity be) {
		return original.call(provider, type, ModPartialModels.moverModel(model, be.getBlockState()));
	}
}

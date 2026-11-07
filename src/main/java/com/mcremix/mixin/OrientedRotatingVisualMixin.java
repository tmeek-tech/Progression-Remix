package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mcremix.client.ModPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;

import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.api.model.Model;

/**
 * A wooden bearing spins its wood's half shaft where Create's visual spins Create's (ModPartialModels#moverModel), as
 * {@link SingleAxisRotatingVisualMixin} does for whole shafts.
 */
@Mixin(value = OrientedRotatingVisual.class, remap = false)
public abstract class OrientedRotatingVisualMixin {
	@WrapOperation(method = "<init>", at = @At(value = "INVOKE",
		target = "Ldev/engine_room/flywheel/api/instance/InstancerProvider;instancer(Ldev/engine_room/flywheel/api/instance/InstanceType;Ldev/engine_room/flywheel/api/model/Model;)Ldev/engine_room/flywheel/api/instance/Instancer;"))
	private Instancer<?> mcremix$woodenShaftHalf(InstancerProvider provider, InstanceType<?> type, Model model,
		Operation<Instancer<?>> original, @Local(argsOnly = true) KineticBlockEntity be) {
		return original.call(provider, type, ModPartialModels.moverModel(model, be.getBlockState()));
	}
}

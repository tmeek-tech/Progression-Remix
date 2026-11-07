package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.fluids.PressureBoilerData;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

/** A whistle on a steel boiler screams while it vents the boiler's spare steam, powered or not. */
@Mixin(value = WhistleBlockEntity.class, remap = false)
public abstract class WhistleBlockEntityMixin {

	@Inject(method = "isPowered", at = @At("HEAD"), cancellable = true)
	private void mcremix$venting(CallbackInfoReturnable<Boolean> cir) {
		FluidTankBlockEntity tank = ((WhistleBlockEntity) (Object) this).getTank();
		if (tank != null && tank.boiler instanceof PressureBoilerData boiler && boiler.venting)
			cir.setReturnValue(true);
	}
}

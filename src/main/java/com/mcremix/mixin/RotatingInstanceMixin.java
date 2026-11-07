package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.client.Wobble;
import com.mcremix.kinetics.KineticLimits;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatingInstance;

import net.minecraft.core.Direction;

/**
 * A part wobbling under more load than it carries ({@link KineticLimits}) spins about a tilted axis ({@link Wobble}).
 * Every turning part Create draws with Flywheel, shafts, cogs, gearboxes and the turning parts of machines alike, is
 * set up here.
 */
@Mixin(value = RotatingInstance.class, remap = false)
public abstract class RotatingInstanceMixin {
	@Inject(method = "setup(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;Lnet/minecraft/core/Direction$Axis;F)Lcom/simibubi/create/content/kinetics/base/RotatingInstance;",
		at = @At("RETURN"))
	private void mcremix$wobble(KineticBlockEntity be, Direction.Axis axis, float speed,
		CallbackInfoReturnable<RotatingInstance> cir) {
		if (KineticLimits.wobbling(be))
			((RotatingInstance) (Object) this).setRotationAxis(Wobble.axis(axis, be.getBlockPos()));
	}
}

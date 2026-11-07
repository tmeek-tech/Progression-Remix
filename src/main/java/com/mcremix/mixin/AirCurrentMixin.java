package com.mcremix.mixin;

import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mcremix.kinetics.FrictionHeatBehaviour;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Power Grid's fans cool what their air blows through. A gearbox, casing, clutch or any other friction point that is a
 * whole block stops the air instead, so the friction point the air runs into is cooled here, as by a fan blowing
 * straight on it ({@link com.mcremix.kinetics.FrictionHeat#fanCooling}), and less the further off the fan is.
 */
@Mixin(value = AirCurrent.class, remap = false)
public abstract class AirCurrentMixin {
	@Shadow
	@Final
	public IAirCurrentSource source;
	@Shadow
	public Direction direction;
	@Shadow
	public float maxDistance;
	@Unique
	private ThermalBehaviour mcremix$blownOn;

	@Inject(method = "rebuild()V", at = @At("HEAD"))
	private void mcremix$forgetBlownOn(CallbackInfo ci) {
		if (mcremix$blownOn != null)
			mcremix$blownOn.removeCoolingMultiplier((AirCurrent) (Object) this);
		mcremix$blownOn = null;
	}

	@Inject(method = "rebuild()V", at = @At("TAIL"))
	private void mcremix$coolBlownOn(CallbackInfo ci) {
		Level level = source.getAirCurrentWorld();
		if (level == null || level.isClientSide || direction == null)
			return;
		float range = source.getMaxDistance();
		int reach = (int) maxDistance;
		// only where a whole block stopped the air short of the fan's range; Power Grid cools anything it blows into
		if (reach != maxDistance || reach >= range)
			return;
		BlockPos hit = source.getAirCurrentPos().relative(direction, reach + 1);
		if (!(BlockEntityBehaviour.get(level, hit, ThermalBehaviour.TYPE) instanceof FrictionHeatBehaviour heat))
			return;
		float strength = Math.abs(source.getSpeed()) * ModdedConfigs.server().kinetics.encasedFanCoolingStrength.getF()
			* (1 - reach / range);
		if (strength <= 0)
			return;
		heat.addCoolingMultiplier((AirCurrent) (Object) this, strength);
		mcremix$blownOn = heat;
	}
}

package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mcremix.fluids.MetalSteamEngineBlock;
import com.mcremix.fluids.SteamEngines;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;

import net.minecraft.world.level.Level;

/**
 * Each steam engine runs on a boiler of its own metal alone ({@link SteamEngines}): Create's on Create's copper tanks,
 * as Create has it, and a brass, iron or steel one on tanks of its metal. Create's engine on another metal's boiler,
 * from before, becomes that metal's engine the first time it ticks.
 */
@Mixin(value = SteamEngineBlockEntity.class, remap = false)
public abstract class SteamEngineBlockEntityMixin {

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void mcremix$matchBoiler(CallbackInfo ci) {
		if (SteamEngines.match((SteamEngineBlockEntity) (Object) this))
			ci.cancel();
	}

	@ModifyReturnValue(method = "isValid", at = @At("RETURN"))
	private boolean mcremix$ownMetal(boolean valid) {
		SteamEngineBlockEntity engine = (SteamEngineBlockEntity) (Object) this;
		Level level = engine.getLevel();
		if (level == null || !(engine.getBlockState().getBlock() instanceof MetalSteamEngineBlock metal))
			return valid;
		return metal.runsOn(SteamEngines.boilerUnder(level, engine.getBlockPos(), engine.getBlockState()));
	}
}

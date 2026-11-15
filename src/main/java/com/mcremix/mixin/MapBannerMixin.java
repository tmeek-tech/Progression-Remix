package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.colour.NewColour;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.saveddata.maps.MapBanner;
import net.minecraft.world.level.saveddata.maps.MapDecoration;

/**
 * A banner marked on a map shows as a flag of its colour, from a fixed set of sixteen; a banner in a new colour shows
 * as the vanilla colour it is nearest (black otherwise).
 */
@Mixin(MapBanner.class)
public abstract class MapBannerMixin {
	@Shadow @Final private DyeColor color;

	@Inject(method = "getDecoration", at = @At("HEAD"), cancellable = true)
	private void mcremix$nearestFlag(CallbackInfoReturnable<MapDecoration.Type> cir) {
		NewColour c = NewColour.of(color);
		if (c != null)
			cir.setReturnValue(MapDecoration.Type.valueOf("BANNER_" + c.nearestVanilla().name()));
	}
}

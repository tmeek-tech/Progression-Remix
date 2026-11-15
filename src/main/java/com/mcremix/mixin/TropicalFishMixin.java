package com.mcremix.mixin;

import java.util.Arrays;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mcremix.colour.NewColour;

import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.item.DyeColor;

/**
 * One tropical fish in ten spawns in two random colours from every colour there is. The new colours come from coal tar
 * dyes, so nothing living has them unless dyed: the fish pick from vanilla's sixteen.
 */
@Mixin(TropicalFish.class)
public abstract class TropicalFishMixin {
	@Redirect(method = "finalizeSpawn",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/DyeColor;values()[Lnet/minecraft/world/item/DyeColor;"))
	private DyeColor[] mcremix$vanillaColours() {
		return Arrays.copyOf(DyeColor.values(), NewColour.FIRST_ID);
	}
}

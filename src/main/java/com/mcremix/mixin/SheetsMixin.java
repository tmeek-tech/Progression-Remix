package com.mcremix.mixin;

import java.util.Arrays;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.ImmutableList;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

/**
 * Shulker boxes' textures are listed by colour name, for vanilla's sixteen only (beds' are made from DyeColor, so have
 * the new colours already): the list is made again from DyeColor. The textures are in vanilla's namespace, as theirs.
 */
@Mixin(Sheets.class)
public abstract class SheetsMixin {
	@Shadow @Final @Mutable public static List<Material> SHULKER_TEXTURE_LOCATION;

	@Inject(method = "<clinit>", at = @At("RETURN"))
	private static void mcremix$newColours(CallbackInfo ci) {
		SHULKER_TEXTURE_LOCATION = Arrays.stream(DyeColor.values())
			.map(c -> new Material(Sheets.SHULKER_SHEET, new ResourceLocation("entity/shulker/shulker_" + c.getName())))
			.collect(ImmutableList.toImmutableList());
	}
}

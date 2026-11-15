package com.mcremix.mixin;

import java.util.Arrays;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mcremix.McRemix;
import com.mcremix.colour.NewColour;

import net.minecraft.client.renderer.entity.layers.LlamaDecorLayer;
import net.minecraft.resources.ResourceLocation;

/** A llama's carpet is drawn from a texture per colour, listed for vanilla's sixteen: ours follow. */
@Mixin(LlamaDecorLayer.class)
public abstract class LlamaDecorLayerMixin {
	@Shadow @Final @Mutable private static ResourceLocation[] TEXTURE_LOCATION;

	@Inject(method = "<clinit>", at = @At("RETURN"))
	private static void mcremix$newColours(CallbackInfo ci) {
		ResourceLocation[] all = Arrays.copyOf(TEXTURE_LOCATION, NewColour.FIRST_ID + NewColour.values().length);
		for (NewColour c : NewColour.values())
			all[NewColour.FIRST_ID + c.ordinal()] = McRemix.asResource("textures/entity/llama/decor/" + c.id + ".png");
		TEXTURE_LOCATION = all;
	}
}

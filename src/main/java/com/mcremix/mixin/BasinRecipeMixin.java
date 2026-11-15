package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.lab.BasinReflux;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;

import net.minecraft.world.item.crafting.Recipe;

/** A heated basin loses what boils easily, unless a coil condenser runs it back ({@link BasinReflux}). */
@Mixin(value = BasinRecipe.class, remap = false)
public class BasinRecipeMixin {
	@Inject(method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/minecraft/world/item/crafting/Recipe;)Z",
		at = @At("RETURN"))
	private static void mcremix$boilOff(BasinBlockEntity basin, Recipe<?> recipe, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ())
			BasinReflux.after(basin, recipe);
	}
}

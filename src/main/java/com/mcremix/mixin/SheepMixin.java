package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.McRemix;
import com.mcremix.colour.NewColour;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;

/**
 * A sheep keeps its colour in the low four bits of a byte, beside the bit that says it is sheared (16), which holds
 * vanilla's sixteen colours and no more. The eight new colours (ids 16 to 23) set the bit above that one (32) as well.
 * Sheep never spawn in them; they only take them from dye. Their loot tables are ours.
 */
@Mixin(Sheep.class)
public abstract class SheepMixin {
	private static final int MCREMIX_HIGH_COLOUR = 32;

	@Shadow @Final private static EntityDataAccessor<Byte> DATA_WOOL_ID;

	@Shadow public abstract DyeColor getColor();

	@Shadow public abstract boolean isSheared();

	@Inject(method = "getColor", at = @At("HEAD"), cancellable = true)
	private void mcremix$getColor(CallbackInfoReturnable<DyeColor> cir) {
		byte b = ((Sheep) (Object) this).getEntityData().get(DATA_WOOL_ID);
		if ((b & MCREMIX_HIGH_COLOUR) != 0)
			cir.setReturnValue(DyeColor.byId(NewColour.FIRST_ID + (b & 15)));
	}

	@Inject(method = "setColor", at = @At("HEAD"), cancellable = true)
	private void mcremix$setColor(DyeColor colour, CallbackInfo ci) {
		Sheep sheep = (Sheep) (Object) this;
		byte b = sheep.getEntityData().get(DATA_WOOL_ID);
		int id = colour.getId();
		byte rest = (byte) (b & ~(15 | MCREMIX_HIGH_COLOUR));
		sheep.getEntityData().set(DATA_WOOL_ID,
			(byte) (rest | (id & 15) | (id >= NewColour.FIRST_ID ? MCREMIX_HIGH_COLOUR : 0)));
		ci.cancel();
	}

	@Inject(method = "getDefaultLootTable", at = @At("HEAD"), cancellable = true)
	private void mcremix$lootTable(CallbackInfoReturnable<ResourceLocation> cir) {
		if (isSheared())
			return;
		NewColour colour = NewColour.of(getColor());
		if (colour != null)
			cir.setReturnValue(McRemix.asResource("entities/sheep/" + colour.id));
	}
}

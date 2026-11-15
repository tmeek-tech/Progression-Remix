package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;

/** What a dispenser does with an item, so the new colours' carpets can be given what vanilla gives its own. */
@Mixin(DispenserBlock.class)
public interface DispenserBlockAccessor {
	@Invoker("getDispenseMethod")
	DispenseItemBehavior mcremix$getDispenseMethod(ItemStack stack);
}

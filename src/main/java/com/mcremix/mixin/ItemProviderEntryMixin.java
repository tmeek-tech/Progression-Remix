package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mcremix.machines.CreateStandIn;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemProviderEntry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/**
 * The item side of {@link BlockEntryMixin}: a stand-in's item counts as Create's own, so holding a copper arm selects
 * what it will reach, a tiered drill's item places beside any drill, and a wooden pole or gantry shaft lengthens a row
 * of poles or shafts.
 */
@Mixin(value = ItemProviderEntry.class, remap = false)
public abstract class ItemProviderEntryMixin {
	@ModifyReturnValue(method = "is", at = @At("RETURN"))
	private boolean mcremix$standIns(boolean is, Item item) {
		return is || (item instanceof BlockItem block && (Object) this instanceof BlockEntry<?> entry
			&& CreateStandIn.standsFor(entry, block.getBlock()));
	}
}

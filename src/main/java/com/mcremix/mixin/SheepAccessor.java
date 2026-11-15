package com.mcremix.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ItemLike;

/** The wool a sheep of each colour is sheared for, which vanilla lists for its sixteen. */
@Mixin(Sheep.class)
public interface SheepAccessor {
	@Accessor("ITEM_BY_DYE")
	static Map<DyeColor, ItemLike> mcremix$woolByColour() {
		throw new AssertionError();
	}
}

package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.material.MapColor;

/** DyeColor's constructor, for the eight colours {@link DyeColorMixin} adds. */
@Mixin(DyeColor.class)
public interface DyeColorAccessor {
	@Invoker("<init>")
	static DyeColor mcremix$create(String constant, int ordinal, int id, String name, int textureDiffuseColor,
		MapColor mapColor, int fireworkColor, int textColor) {
		throw new AssertionError();
	}
}

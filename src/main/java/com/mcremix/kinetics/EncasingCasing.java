package com.mcremix.kinetics;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mcremix.registry.ModBlocks;
import com.simibubi.create.AllBlocks;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.material.MapColor;

/**
 * The casings a shaft or cog of any material can be encased in (Taran: every material's casing), each a friction point
 * in its own material: the encased part turns as fast as its casing allows, whatever the part is made of, and carries
 * what its own material carries (Taran, 0.27.0). Iron's is the Iron Casing
 * (Taran, 0.23.0), as for iron gearboxes and machines. Titanium's encased blocks come in each of its tints, as the
 * casing is what heats ({@link Tints}).
 */
public enum EncasingCasing {
	ANDESITE(KineticMaterial.ANDESITE, () -> AllBlocks.ANDESITE_CASING.get(), MapColor.PODZOL, SoundType.WOOD),
	BRASS(KineticMaterial.BRASS, () -> AllBlocks.BRASS_CASING.get(), MapColor.TERRACOTTA_BROWN, SoundType.WOOD),
	COPPER(KineticMaterial.COPPER, () -> AllBlocks.COPPER_CASING.get(), MapColor.COLOR_LIGHT_GRAY, SoundType.COPPER),
	IRON(KineticMaterial.IRON, () -> ModBlocks.IRON_CASING.get(), MapColor.METAL, SoundType.METAL),
	STEEL(KineticMaterial.STEEL, () -> ModBlocks.STEEL_CASING.get(), MapColor.COLOR_GRAY, SoundType.METAL),
	ALUMINIUM(KineticMaterial.ALUMINIUM, () -> ModBlocks.ALUMINIUM_CASING.get(), MapColor.CLAY, SoundType.METAL),
	TITANIUM(KineticMaterial.TITANIUM, () -> ModBlocks.TITANIUM_CASING.get(), MapColor.DEEPSLATE, SoundType.METAL);

	public final KineticMaterial material;
	private final Supplier<Block> casing;
	public final MapColor color;
	public final SoundType sound;

	EncasingCasing(KineticMaterial material, Supplier<Block> casing, MapColor color, SoundType sound) {
		this.material = material;
		this.casing = casing;
		this.color = color;
		this.sound = sound;
	}

	/** The block whose item encases a part, and whose look the encased block takes. */
	public Block casing() {
		return casing.get();
	}

	public String prefix() {
		return material.prefix;
	}

	/** The casing of a material, or null for wood, which has none. */
	@Nullable
	public static EncasingCasing of(KineticMaterial material) {
		for (EncasingCasing casing : values())
			if (casing.material == material)
				return casing;
		return null;
	}

	/** Which casing an item places, or null. */
	@Nullable
	public static EncasingCasing of(ItemStack stack) {
		for (EncasingCasing casing : values())
			if (stack.is(casing.casing().asItem()))
				return casing;
		return null;
	}

	/**
	 * The tints its encased blocks come in: titanium's four ({@link Tints}), since the casing is what heats; just the one
	 * for every other casing.
	 */
	public WeatherState[] tints() {
		return material.tints() ? WeatherState.values() : new WeatherState[] { WeatherState.UNAFFECTED };
	}

	/** Create encases its own shaft and cogs in andesite and brass casings with blocks of its own. */
	public boolean createHasOwn() {
		return this == ANDESITE || this == BRASS;
	}
}

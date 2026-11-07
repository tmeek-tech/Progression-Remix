package com.mcremix.fluids;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mcremix.heat.HeatLevel;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModFluids;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.fluids.FluidStack;

/**
 * The metals the crucible melts, each a fluid at its real melting heat: zinc and aluminium Hot; copper, gold and brass
 * Searing; iron, steel and netherite Blazing. Amounts go by the measure most mods with molten metal use: 90 mB an ingot, 10 a nugget,
 * 810 a block. Spilled, a metal runs like slow lava and sets into rough metal that mines back into ingots and nuggets.
 */
public enum MoltenMetal {
	IRON("iron", HeatLevel.BLAZING, 1850, 0xFFFFE7A3, 15, MapColor.METAL,
		() -> Items.IRON_INGOT, () -> Items.IRON_NUGGET, () -> Items.IRON_BLOCK),
	GOLD("gold", HeatLevel.SEARING, 1400, 0xFFFFB534, 15, MapColor.GOLD,
		() -> Items.GOLD_INGOT, () -> Items.GOLD_NUGGET, () -> Items.GOLD_BLOCK),
	COPPER("copper", HeatLevel.SEARING, 1450, 0xFFFF8546, 15, MapColor.COLOR_ORANGE,
		() -> Items.COPPER_INGOT, () -> AllItems.COPPER_NUGGET.get(), () -> Items.COPPER_BLOCK),
	/** Zinc melts at only 420 °C, so it pours as a silvery liquid with barely a glow. */
	ZINC("zinc", HeatLevel.HOT, 773, 0xFFC9C3CC, 7, MapColor.GLOW_LICHEN,
		() -> AllItems.ZINC_INGOT.get(), () -> AllItems.ZINC_NUGGET.get(), () -> AllBlocks.ZINC_BLOCK.get().asItem()),
	BRASS("brass", HeatLevel.SEARING, 1350, 0xFFFFA84A, 15, MapColor.TERRACOTTA_YELLOW,
		() -> AllItems.BRASS_INGOT.get(), () -> AllItems.BRASS_NUGGET.get(), () -> AllBlocks.BRASS_BLOCK.get().asItem()),
	STEEL("steel", HeatLevel.BLAZING, 1800, 0xFFFFF3C9, 15, MapColor.COLOR_GRAY,
		() -> ModItems.STEEL_INGOT.get(), () -> ModItems.STEEL_NUGGET.get(), () -> ModBlocks.STEEL_BLOCK.get().asItem()),
	/** There is no netherite nugget: what is short of an ingot is lost when netherite sets. */
	NETHERITE("netherite", HeatLevel.BLAZING, 2000, 0xFF9A4558, 15, MapColor.COLOR_BLACK,
		() -> Items.NETHERITE_INGOT, null, () -> Items.NETHERITE_BLOCK),
	/** Aluminium melts at 660 °C, Hot like zinc, and pours bright and silvery with only a dull glow. */
	ALUMINIUM("aluminium", HeatLevel.HOT, 933, 0xFFE4E8EE, 9, MapColor.CLAY,
		() -> ModItems.ALUMINIUM_INGOT.get(), () -> ModItems.ALUMINIUM_NUGGET.get(),
		() -> ModBlocks.ALUMINIUM_BLOCK.get().asItem());

	public static final int INGOT = 90;
	public static final int NUGGET = 10;
	public static final int BLOCK = 810;
	/** A source block of molten metal, as of any fluid. */
	public static final int SOURCE = 1000;

	public final String id;
	public final HeatLevel heat;
	/** In kelvin, for other mods that read fluid temperatures; {@link FluidHeat} reads the tags first. */
	public final int temperature;
	/** Multiplied over the one glowing molten texture every metal shares. */
	public final int tint;
	public final int light;
	public final MapColor mapColor;
	private final Supplier<Item> ingot;
	@Nullable
	private final Supplier<Item> nugget;
	private final Supplier<Item> block;

	MoltenMetal(String id, HeatLevel heat, int temperature, int tint, int light, MapColor mapColor, Supplier<Item> ingot,
		@Nullable Supplier<Item> nugget, Supplier<Item> block) {
		this.id = id;
		this.heat = heat;
		this.temperature = temperature;
		this.tint = tint;
		this.light = light;
		this.mapColor = mapColor;
		this.ingot = ingot;
		this.nugget = nugget;
		this.block = block;
	}

	public Item ingot() {
		return ingot.get();
	}

	@Nullable
	public Item nugget() {
		return nugget == null ? null : nugget.get();
	}

	public Item block() {
		return block.get();
	}

	public Fluid fluid() {
		return ModFluids.molten(this).source().get();
	}

	public FluidStack stack(int amount) {
		return new FluidStack(fluid(), amount);
	}

	/** The metal a fluid is, or null for anything else. */
	@Nullable
	public static MoltenMetal of(Fluid fluid) {
		for (MoltenMetal metal : values())
			if (ModFluids.molten(metal).source().get().isSame(fluid))
				return metal;
		return null;
	}

	@Nullable
	public static MoltenMetal of(FluidStack stack) {
		return stack.isEmpty() ? null : of(stack.getFluid());
	}

	@Nullable
	public static MoltenMetal byId(String id) {
		for (MoltenMetal metal : values())
			if (metal.id.equals(id))
				return metal;
		return null;
	}

	/** Ingots and nuggets holding as much metal as {@code amount} mB, rounded down to a nugget. */
	public List<ItemStack> asItems(int amount) {
		List<ItemStack> out = new ArrayList<>();
		int ingots = amount / INGOT;
		int nuggets = nugget == null ? 0 : (amount % INGOT) / NUGGET;
		while (ingots > 0) {
			int n = Math.min(64, ingots);
			out.add(new ItemStack(ingot(), n));
			ingots -= n;
		}
		if (nuggets > 0)
			out.add(new ItemStack(nugget(), nuggets));
		return out;
	}
}

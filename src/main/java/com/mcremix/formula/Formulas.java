package com.mcremix.formula;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Which structural formula each thing shows: the items each formula lists, the fluids, and every bucket of those
 * fluids, as the client loads them (and a GameTest reads them straight out of the mod).
 */
public final class Formulas {
	private Formulas() {}

	/** What shows each formula: items (buckets among them), fluid types, and fluids by name, as JEI writes them. */
	public record Index(Map<Item, StructuralFormula> items, Map<FluidType, StructuralFormula> fluids,
		Map<String, StructuralFormula> names) {
		public static final Index EMPTY = new Index(Map.of(), Map.of(), Map.of());

		public static Index of(Collection<StructuralFormula> formulas) {
			Map<Item, StructuralFormula> items = new HashMap<>();
			Map<FluidType, StructuralFormula> fluids = new HashMap<>();
			Map<String, StructuralFormula> names = new HashMap<>();
			for (StructuralFormula formula : formulas) {
				formula.items().forEach(id -> {
					Item item = ForgeRegistries.ITEMS.getValue(id);
					if (item != null && ForgeRegistries.ITEMS.containsKey(id))
						items.put(item, formula);
				});
				formula.fluids().forEach(id -> {
					Fluid fluid = ForgeRegistries.FLUIDS.getValue(id);
					if (fluid != null && fluid != Fluids.EMPTY && ForgeRegistries.FLUIDS.containsKey(id)) {
						fluids.put(fluid.getFluidType(), formula);
						names.put(fluid.getFluidType().getDescriptionId(), formula);
					}
				});
			}
			for (Item item : ForgeRegistries.ITEMS)
				if (item instanceof BucketItem bucket && fluids.containsKey(bucket.getFluid().getFluidType()))
					items.putIfAbsent(item, fluids.get(bucket.getFluid().getFluidType()));
			return new Index(Map.copyOf(items), Map.copyOf(fluids), Map.copyOf(names));
		}

		@Nullable
		public StructuralFormula of(ItemStack stack) {
			return stack.isEmpty() ? null : items.get(stack.getItem());
		}

		@Nullable
		public StructuralFormula of(Fluid fluid) {
			return fluids.get(fluid.getFluidType());
		}

		/** The formula of the fluid with this name (its type's description id), for tooltips JEI draws for fluids. */
		@Nullable
		public StructuralFormula ofName(String descriptionId) {
			return names.get(descriptionId);
		}
	}

	private static volatile Index index = Index.EMPTY;

	public static Index index() {
		return index;
	}

	public static void set(Collection<StructuralFormula> formulas) {
		index = Index.of(formulas);
	}
}

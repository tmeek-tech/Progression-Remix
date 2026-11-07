package com.mcremix.registry;

import java.util.EnumMap;
import java.util.Map;

import com.mcremix.McRemix;
import com.mcremix.kinetics.KineticMaterial;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public class ModTags {
	public static class Blocks {
		public static final TagKey<Block> HEAT_WARM = tag("heat_sources/warm");
		public static final TagKey<Block> HEAT_HOT = tag("heat_sources/hot");
		public static final TagKey<Block> HEAT_SEARING = tag("heat_sources/searing");
		public static final TagKey<Block> HEAT_BLAZING = tag("heat_sources/blazing");
		/** Electric heaters, read by their temperature instead of a fixed level. */
		public static final TagKey<Block> HEAT_ELECTRIC = tag("heat_sources/electric");

		/**
		 * What a kinetic block from another mod, Create's included, is made of, for its limits (see
		 * {@link com.mcremix.kinetics.KineticMaterials}). This mod's own parts know their material.
		 */
		private static final Map<KineticMaterial, TagKey<Block>> MATERIALS = new EnumMap<>(KineticMaterial.class);

		static {
			for (KineticMaterial material : KineticMaterial.values())
				MATERIALS.put(material, tag("kinetic_material/" + material.prefix));
		}

		public static TagKey<Block> material(KineticMaterial material) {
			return MATERIALS.get(material);
		}

		/**
		 * Kinetic blocks that hold shafts and cogs whatever their kind, supports as players know them (see
		 * {@link com.mcremix.kinetics.FrictionPoints}). Its old name, {@code mcremix:friction_points}, is part of it, so
		 * packs that used that still work.
		 */
		public static final TagKey<Block> FRICTION_POINTS = tag("supports");
		/** Kinetic blocks that pass rotation on but hold nothing: sources, belts, gauges. */
		public static final TagKey<Block> HOLDS_NO_SHAFTS = tag("holds_no_shafts");
		/** Furnaces that no longer cook; kilns and campfires do their jobs. */
		public static final TagKey<Block> COLD_FURNACES = tag("cold_furnaces");
		/** Blocks an iron bloom can be struck on: the vanilla anvils and the stone anvils. */
		public static final TagKey<Block> BLOOM_ANVILS = tag("bloom_anvils");

		/**
		 * The hottest fluid a pipe, pump or tank can hold, by its material (see {@link com.mcremix.fluids.FluidHeat}).
		 * Anything holding fluid that is in none of these carries any heat.
		 */
		public static final TagKey<Block> FLUID_CEILING_COLD = tag("fluid_ceiling/cold");
		public static final TagKey<Block> FLUID_CEILING_WARM = tag("fluid_ceiling/warm");
		public static final TagKey<Block> FLUID_CEILING_HOT = tag("fluid_ceiling/hot");
		public static final TagKey<Block> FLUID_CEILING_SEARING = tag("fluid_ceiling/searing");

		private static TagKey<Block> tag(String path) {
			return TagKey.create(Registries.BLOCK, McRemix.asResource(path));
		}
	}

	/** How hot a fluid is, on the heat scale. A fluid in none of these is rated by its temperature. */
	public static class Fluids {
		public static final TagKey<Fluid> HEAT_WARM = tag("heat/warm");
		public static final TagKey<Fluid> HEAT_HOT = tag("heat/hot");
		public static final TagKey<Fluid> HEAT_SEARING = tag("heat/searing");
		public static final TagKey<Fluid> HEAT_BLAZING = tag("heat/blazing");

		private static TagKey<Fluid> tag(String path) {
			return TagKey.create(Registries.FLUID, McRemix.asResource(path));
		}
	}

	public static class Items {
		public static final TagKey<Item> FUEL_WARM = tag("kiln_fuel/warm");
		public static final TagKey<Item> FUEL_HOT = tag("kiln_fuel/hot");
		public static final TagKey<Item> FUEL_SEARING = tag("kiln_fuel/searing");
		/** Polyethylene, which aluminium and titanium friction points are made with: the polyethylene bushing. */
		public static final TagKey<Item> POLYETHYLENE = tag("polyethylene");
		/** What lights a Furnace or Steel Furnace by hand or from a deployer: flint and steel, a fire charge. */
		public static final TagKey<Item> FURNACE_LIGHTERS = tag("furnace_lighters");
		/** Items stripped from village chest loot. */
		public static final TagKey<Item> VILLAGE_CONTRABAND = tag("village_contraband");

		private static TagKey<Item> tag(String path) {
			return TagKey.create(Registries.ITEM, McRemix.asResource(path));
		}
	}
}

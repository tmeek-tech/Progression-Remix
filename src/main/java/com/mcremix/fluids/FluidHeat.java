package com.mcremix.fluids;

import javax.annotation.Nullable;

import com.mcremix.heat.HeatLevel;
import com.mcremix.registry.ModTags;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 * How hot a fluid is, and how hot a fluid a pipe, pump or tank can hold, both on the Furnace's heat scale. Lava is Hot,
 * and molten metals go by their real melting points: zinc Hot; copper, gold and brass Searing; iron, steel and netherite
 * Blazing. PVC holds only cold fluids, aluminium up to Warm, glass, brass, iron and copper (Create's own fluid blocks) up
 * to Hot, steel up to Searing, and nothing is piped at Blazing: the crucible pours those. Both are tags, so packs can
 * rate other mods' fluids and blocks.
 */
public class FluidHeat {

	/**
	 * A fluid's heat: its {@code mcremix:heat/*} tag, or failing that its temperature in kelvin, so other mods' fluids
	 * are rated without a tag (water and Create's fluids are 300 K, lava 1300 K).
	 */
	public static HeatLevel of(Fluid fluid) {
		if (fluid.is(ModTags.Fluids.HEAT_BLAZING))
			return HeatLevel.BLAZING;
		if (fluid.is(ModTags.Fluids.HEAT_SEARING))
			return HeatLevel.SEARING;
		if (fluid.is(ModTags.Fluids.HEAT_HOT))
			return HeatLevel.HOT;
		if (fluid.is(ModTags.Fluids.HEAT_WARM))
			return HeatLevel.WARM;
		return fromKelvin(fluid.getFluidType().getTemperature());
	}

	public static HeatLevel of(FluidStack stack) {
		return stack.isEmpty() ? HeatLevel.COLD : of(stack.getFluid());
	}

	public static HeatLevel fromKelvin(int kelvin) {
		if (kelvin > 1600)
			return HeatLevel.BLAZING;
		if (kelvin > 1300)
			return HeatLevel.SEARING;
		if (kelvin > 700)
			return HeatLevel.HOT;
		if (kelvin > 350)
			return HeatLevel.WARM;
		return HeatLevel.COLD;
	}

	/** The hottest fluid a block can hold, or null if it is not rated and holds anything. */
	@Nullable
	public static HeatLevel ceiling(BlockState state) {
		if (state.is(ModTags.Blocks.FLUID_CEILING_COLD))
			return HeatLevel.COLD;
		if (state.is(ModTags.Blocks.FLUID_CEILING_WARM))
			return HeatLevel.WARM;
		if (state.is(ModTags.Blocks.FLUID_CEILING_HOT))
			return HeatLevel.HOT;
		if (state.is(ModTags.Blocks.FLUID_CEILING_SEARING))
			return HeatLevel.SEARING;
		return null;
	}

	/** Whether a block holding this fluid overheats. */
	public static boolean tooHot(BlockState holder, FluidStack fluid) {
		if (fluid.isEmpty())
			return false;
		HeatLevel ceiling = ceiling(holder);
		return ceiling != null && !ceiling.isAtLeast(of(fluid));
	}
}

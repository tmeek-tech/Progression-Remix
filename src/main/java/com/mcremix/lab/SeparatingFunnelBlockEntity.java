package com.mcremix.lab;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import com.mcremix.electrolysis.VatMixingRecipe;
import com.mcremix.fluids.Chemical;
import com.mcremix.registry.ModRecipes;
import com.simibubi.create.foundation.fluid.FluidIngredient;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/**
 * A separating funnel (the distillation design's bench): two liquids shaken together that settle into two layers, the
 * phenol wash among them (strained carbolic oil and caustic soda into sodium phenolate under creosote). It does what a
 * vat mixes from liquids alone, a batch at a time, with no heat. The heavier layer comes out of the tap at its foot,
 * to a pipe under it, and the lighter from its neck, to a pipe on its top or sides; a bucket takes the heavier first.
 */
public class SeparatingFunnelBlockEntity extends BenchGlassBlockEntity {
	/** How long a batch takes to shake and settle, when its recipe gives no time. */
	public static final int DEFAULT_TIME = 100;

	public SeparatingFunnelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state, 2, 2);
	}

	/** What a funnel can do of a vat's mixing: two liquids at most in and out, no gas, no items. */
	public static boolean separates(VatMixingRecipe recipe) {
		if (!recipe.getIngredients().isEmpty() || !recipe.getRollableResults().isEmpty()
			|| recipe.getFluidIngredients().isEmpty() || recipe.getFluidIngredients().size() > 2
			|| recipe.getFluidResults().isEmpty() || recipe.getFluidResults().size() > 2)
			return false;
		for (FluidIngredient ingredient : recipe.getFluidIngredients())
			for (FluidStack fluid : ingredient.getMatchingFluidStacks())
				if (Chemical.isGas(fluid))
					return false;
		for (FluidStack result : recipe.getFluidResults())
			if (Chemical.isGas(result))
				return false;
		return true;
	}

	@Nullable
	private VatMixingRecipe recipe() {
		if (level == null)
			return null;
		for (VatMixingRecipe r : level.getRecipeManager().getAllRecipesFor(ModRecipes.VAT_MIXING_TYPE.get()))
			if (separates(r) && holds(r.getFluidIngredients()))
				return r;
		return null;
	}

	@Override
	protected String work() {
		VatMixingRecipe r = recipe();
		if (r == null) {
			progress = 0;
			return in[0].isEmpty() && in[1].isEmpty() ? "idle" : "waiting";
		}
		if (!put(r.getFluidResults(), true))
			return "full";
		required = r.getProcessingDuration() > 0 ? r.getProcessingDuration() : DEFAULT_TIME;
		if (++progress < required)
			return "working";
		progress = 0;
		take(r.getFluidIngredients());
		put(r.getFluidResults(), false);
		level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.2f, 1.6f);
		return "working";
	}

	private static int density(FluidTank tank) {
		return tank.getFluid().getFluid().getFluidType().getDensity(tank.getFluid());
	}

	/** Its layers, heaviest first: what it made, which settles under what is still to shake. */
	private List<FluidTank> layers() {
		List<FluidTank> made = new ArrayList<>();
		for (FluidTank tank : out)
			if (!tank.isEmpty())
				made.add(tank);
		made.sort(Comparator.comparingInt(SeparatingFunnelBlockEntity::density).reversed());
		return made;
	}

	/** The tap at its foot gives the bottom layer; the neck, the top one. */
	@Override
	protected List<FluidTank> drains(@Nullable Direction side) {
		List<FluidTank> layers = layers();
		if (layers.isEmpty() || side == null)
			return layers;
		return List.of(side == Direction.DOWN ? layers.get(0) : layers.get(layers.size() - 1));
	}
}

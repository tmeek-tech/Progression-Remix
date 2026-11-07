package com.mcremix.kiln;

import java.util.Optional;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.mcremix.heat.HeatLevel;
import com.mcremix.registry.ModRecipes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;
import com.simibubi.create.foundation.fluid.FluidIngredient;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.wrapper.RecipeWrapper;

/**
 * What the Steel Furnace works from a fluid: a batch of it, held at least as hot as {@link #heat()}, gives the fluids
 * listed, in {@link #getProcessingDuration()} ticks at that heat and half as long for each level hotter. Some run over
 * a catalyst lying in the chamber, the recipe's one item ingredient, which wears away by {@link #catalystLasts()} mB
 * of the fluid run over it rather than by the batch.
 */
public class SteelFurnaceRecipe extends ProcessingRecipe<RecipeWrapper> {
	public static final int DEFAULT_CATALYST_LASTS = 16000;

	private HeatLevel heat = HeatLevel.HOT;
	private int catalystLasts = DEFAULT_CATALYST_LASTS;

	public SteelFurnaceRecipe(ProcessingRecipeParams params) {
		super(ModRecipes.STEEL_FURNACE, params);
	}

	/** The recipe that takes {@code fluid}, if any does. */
	public static Optional<SteelFurnaceRecipe> find(@Nullable Level level, FluidStack fluid) {
		if (level == null || fluid.isEmpty())
			return Optional.empty();
		return level.getRecipeManager().getAllRecipesFor(ModRecipes.STEEL_FURNACE_TYPE.get()).stream()
			.filter(r -> r.feed().test(fluid)).findFirst();
	}

	public FluidIngredient feed() {
		return getFluidIngredients().get(0);
	}

	/** What the fluid runs over, or null if it needs nothing. */
	@Nullable
	public Ingredient catalyst() {
		return getIngredients().isEmpty() ? null : getIngredients().get(0);
	}

	public HeatLevel heat() {
		return heat;
	}

	/** How much of the fluid one catalyst item works before it wears away, in mB. */
	public int catalystLasts() {
		return catalystLasts;
	}

	/** Ticks a batch takes at {@code at}: each level above the recipe's own halves the time, as in the Furnace. */
	public int timeAt(HeatLevel at, double speed) {
		int extra = Math.max(0, at.ordinal() - heat.ordinal());
		return Math.max(10, (int) (getProcessingDuration() / (double) (1 << extra) / speed));
	}

	/** Found by its fluid with {@link #find}, never as a crafting recipe is. */
	@Override
	public boolean matches(RecipeWrapper inv, Level level) {
		return false;
	}

	@Override
	protected int getMaxInputCount() {
		return 1;
	}

	@Override
	protected int getMaxOutputCount() {
		return 0;
	}

	@Override
	protected int getMaxFluidInputCount() {
		return 1;
	}

	/** Its time is how long a batch takes, at the least speed or heat it needs. */
	@Override
	protected boolean canSpecifyDuration() {
		return true;
	}

	@Override
	protected int getMaxFluidOutputCount() {
		return 2;
	}

	@Override
	public void readAdditional(JsonObject json) {
		heat = HeatLevel.byName(GsonHelper.getAsString(json, "heat", "hot"));
		catalystLasts = GsonHelper.getAsInt(json, "catalyst_lasts", DEFAULT_CATALYST_LASTS);
	}

	@Override
	public void readAdditional(FriendlyByteBuf buffer) {
		heat = HeatLevel.byId(buffer.readVarInt());
		catalystLasts = buffer.readVarInt();
	}

	@Override
	public void writeAdditional(JsonObject json) {
		json.addProperty("heat", heat.getSerializedName());
		json.addProperty("catalyst_lasts", catalystLasts);
	}

	@Override
	public void writeAdditional(FriendlyByteBuf buffer) {
		buffer.writeVarInt(heat.ordinal());
		buffer.writeVarInt(catalystLasts);
	}
}

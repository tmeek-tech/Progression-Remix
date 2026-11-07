package com.mcremix.pressurizer;

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
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.wrapper.RecipeWrapper;

/**
 * What a Pressurizer squeezes: a batch of one fluid gives an item or another fluid. Speed is pressure (Taran,
 * 2026-10-22): under {@link #minSpeed()} RPM nothing happens, and at the minimum a batch takes
 * {@link #getProcessingDuration()} ticks, half that at twice the speed. Some want heat underneath, at least
 * {@link #heat()}; the rest work cold or hot alike.
 */
public class PressurizingRecipe extends ProcessingRecipe<RecipeWrapper> {
	private HeatLevel heat = HeatLevel.COLD;
	private int minSpeed = 128;

	public PressurizingRecipe(ProcessingRecipeParams params) {
		super(ModRecipes.PRESSURIZING, params);
	}

	/** The recipe that takes {@code fluid}, if any does. */
	public static Optional<PressurizingRecipe> find(@Nullable Level level, FluidStack fluid) {
		if (level == null || fluid.isEmpty())
			return Optional.empty();
		return level.getRecipeManager().getAllRecipesFor(ModRecipes.PRESSURIZING_TYPE.get()).stream()
			.filter(r -> r.feed().test(fluid)).findFirst();
	}

	public FluidIngredient feed() {
		return getFluidIngredients().get(0);
	}

	/** The heat it wants underneath; cold for none. */
	public HeatLevel heat() {
		return heat;
	}

	/** The slowest it works at, in RPM. */
	public int minSpeed() {
		return minSpeed;
	}

	/** How much of a batch a tick at {@code rpm} gets through, once it is fast enough. */
	public float progressAt(float rpm) {
		return rpm / minSpeed / Math.max(1, getProcessingDuration());
	}

	/** Found by its fluid with {@link #find}, never as a crafting recipe is. */
	@Override
	public boolean matches(RecipeWrapper inv, Level level) {
		return false;
	}

	@Override
	protected int getMaxInputCount() {
		return 0;
	}

	@Override
	protected int getMaxOutputCount() {
		return 1;
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
		return 1;
	}

	@Override
	public void readAdditional(JsonObject json) {
		heat = HeatLevel.byName(GsonHelper.getAsString(json, "heat", "cold"));
		minSpeed = Math.max(1, GsonHelper.getAsInt(json, "min_speed", 128));
	}

	@Override
	public void readAdditional(FriendlyByteBuf buffer) {
		heat = HeatLevel.byId(buffer.readVarInt());
		minSpeed = buffer.readVarInt();
	}

	@Override
	public void writeAdditional(JsonObject json) {
		json.addProperty("heat", heat.getSerializedName());
		json.addProperty("min_speed", minSpeed);
	}

	@Override
	public void writeAdditional(FriendlyByteBuf buffer) {
		buffer.writeVarInt(heat.ordinal());
		buffer.writeVarInt(minSpeed);
	}
}

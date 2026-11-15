package com.mcremix.lab;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
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
 * A mixture a still splits by boiling point (the distillation design): its cuts, lightest first, each one pure fluid,
 * and last what is left behind. In the glass lab still, a bucket at a time, the cuts come over one after another
 * through the condenser into the cow receiver's flasks, and what is left stays in the round flask.
 * <pre>
 * {
 *   "type": "mcremix:distilling",
 *   "heat": "warm",                       // the least heat it boils at; each level hotter halves the time
 *   "ingredients": [{ "fluid": "mcremix:wood_spirit", "amount": 1000 }],
 *   "results": [{ "fluid": "mcremix:acetone", "amount": 100 }, { "fluid": "mcremix:methanol", "amount": 300 },
 *     { "fluid": "mcremix:vinegar", "amount": 600 }],   // the cuts, lightest first; the last is what is left
 *   "boiling_points": [56, 65, 100],      // in °C, for the thermometer and JEI; the residue's is where it starts
 *   "close": [true, true, false],         // cuts that boil close together come over slowly in a bare still
 *   "processingTime": 600                 // ticks the whole batch takes at its own heat
 * }
 * </pre>
 * A recipe without exactly one fluid to split, or without a result, is turned down as it loads (the game logs why), so
 * a broken data pack recipe never reaches a still.
 */
public class DistillingRecipe extends ProcessingRecipe<RecipeWrapper> {
	private HeatLevel heat = HeatLevel.WARM;
	private int[] boilingPoints = new int[0];
	private boolean[] close = new boolean[0];

	public DistillingRecipe(ProcessingRecipeParams params) {
		super(ModRecipes.DISTILLING, params);
	}

	/** The recipe that splits {@code fluid}, if any does. */
	public static Optional<DistillingRecipe> find(@Nullable Level level, FluidStack fluid) {
		if (level == null || fluid.isEmpty())
			return Optional.empty();
		return level.getRecipeManager().getAllRecipesFor(ModRecipes.DISTILLING_TYPE.get()).stream()
			.filter(r -> r.valid() && r.feed().test(fluid)).findFirst();
	}

	/** One fluid to split, in an amount, and at least a residue. */
	public boolean valid() {
		return getFluidIngredients().size() == 1 && getFluidIngredients().get(0).getRequiredAmount() > 0
			&& !getFluidResults().isEmpty();
	}

	public FluidIngredient feed() {
		return getFluidIngredients().get(0);
	}

	/** How much of the mixture the amounts given are for. */
	public int batch() {
		return feed().getRequiredAmount();
	}

	/** The cuts, lightest first: every result but the last. */
	public List<FluidStack> cuts() {
		List<FluidStack> all = getFluidResults();
		return all.subList(0, Math.max(0, all.size() - 1));
	}

	/** What is left behind in the flask: the last result. */
	public FluidStack residue() {
		List<FluidStack> all = getFluidResults();
		return all.isEmpty() ? FluidStack.EMPTY : all.get(all.size() - 1);
	}

	public HeatLevel heat() {
		return heat;
	}

	/** The boiling point in °C of result {@code i}, cuts and residue alike; 0 if the recipe gives none. */
	public int boilingPoint(int i) {
		return i >= 0 && i < boilingPoints.length ? boilingPoints[i] : 0;
	}

	/** Whether cut {@code i} boils close to its neighbour, and so comes over slowly in a bare still. */
	public boolean close(int i) {
		return i >= 0 && i < close.length && close[i];
	}

	/** Never found as a crafting recipe is, only by its fluid with {@link #find}. */
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
		return 0;
	}

	@Override
	protected int getMaxFluidInputCount() {
		return 1;
	}

	/** Up to five cuts (crude oil's) and what is left. */
	@Override
	protected int getMaxFluidOutputCount() {
		return 6;
	}

	@Override
	protected boolean canSpecifyDuration() {
		return true;
	}

	@Override
	public void readAdditional(JsonObject json) {
		if (!valid())
			throw new JsonSyntaxException("Distilling recipe " + getId()
				+ " needs exactly one fluid ingredient with an amount, and at least one fluid result");
		heat = HeatLevel.byName(GsonHelper.getAsString(json, "heat", "warm"));
		JsonArray points = GsonHelper.getAsJsonArray(json, "boiling_points", new JsonArray());
		boilingPoints = new int[points.size()];
		for (int i = 0; i < points.size(); i++)
			boilingPoints[i] = points.get(i).getAsInt();
		JsonArray closeness = GsonHelper.getAsJsonArray(json, "close", new JsonArray());
		close = new boolean[closeness.size()];
		for (int i = 0; i < closeness.size(); i++)
			close[i] = closeness.get(i).getAsBoolean();
	}

	@Override
	public void readAdditional(FriendlyByteBuf buffer) {
		heat = HeatLevel.byId(buffer.readVarInt());
		boilingPoints = buffer.readVarIntArray();
		close = new boolean[buffer.readVarInt()];
		for (int i = 0; i < close.length; i++)
			close[i] = buffer.readBoolean();
	}

	@Override
	public void writeAdditional(JsonObject json) {
		json.addProperty("heat", heat.getSerializedName());
		JsonArray points = new JsonArray();
		for (int point : boilingPoints)
			points.add(point);
		json.add("boiling_points", points);
		JsonArray closeness = new JsonArray();
		for (boolean c : close)
			closeness.add(c);
		json.add("close", closeness);
	}

	@Override
	public void writeAdditional(FriendlyByteBuf buffer) {
		buffer.writeVarInt(heat.ordinal());
		buffer.writeVarIntArray(boilingPoints);
		buffer.writeVarInt(close.length);
		for (boolean c : close)
			buffer.writeBoolean(c);
	}
}

package com.mcremix.pressurizer;

import java.util.Comparator;
import java.util.List;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.wrapper.RecipeWrapper;

/**
 * What a Pressurizer squeezes: a batch of one fluid gives an item or another fluid. Speed is pressure (Taran,
 * 2026-10-22): under {@link #minSpeed()} RPM nothing happens, and at the minimum a batch takes
 * {@link #getProcessingDuration()} ticks, half that at twice the speed. Some want heat underneath, at least
 * {@link #heat()}; the rest work cold or hot alike.
 * <p>
 * Since stage 4 of the distillation design some take a second fluid with the first, as nitrogen is squeezed with
 * hydrogen into ammonia ({@link #coFeed()}), which waits in a tank of its own, and some work over a catalyst dropped
 * into the vessel, the recipe's one item ingredient ({@link #catalyst()}), which crumbles after
 * {@link #catalystLasts()} mB of what it makes.
 */
public class PressurizingRecipe extends ProcessingRecipe<RecipeWrapper> {
	public static final int DEFAULT_CATALYST_LASTS = 16000;

	private HeatLevel heat = HeatLevel.COLD;
	private int minSpeed = 128;
	private int catalystLasts = DEFAULT_CATALYST_LASTS;

	public PressurizingRecipe(ProcessingRecipeParams params) {
		super(ModRecipes.PRESSURIZING, params);
	}

	/** The recipe that takes {@code fluid}, with nothing in the second feed tank. */
	public static Optional<PressurizingRecipe> find(@Nullable Level level, FluidStack fluid) {
		return find(level, fluid, FluidStack.EMPTY);
	}

	/** {@link #find(Level, FluidStack, FluidStack, ItemStack)} with nothing in the vessel. */
	public static Optional<PressurizingRecipe> find(@Nullable Level level, FluidStack fluid, FluidStack second) {
		return find(level, fluid, second, ItemStack.EMPTY);
	}

	/**
	 * The recipe that takes {@code fluid} first: one that also takes {@code second}, the second feed tank's fluid, if
	 * any does; failing that one waiting on a second fluid over the {@code catalyst} in the vessel, so nitrogen waiting
	 * for hydrogen over iron isn't squeezed on its own; failing that one that takes nothing second; failing that one
	 * waiting on a second fluid.
	 */
	public static Optional<PressurizingRecipe> find(@Nullable Level level, FluidStack fluid, FluidStack second,
		ItemStack catalyst) {
		if (level == null || fluid.isEmpty())
			return Optional.empty();
		return all(level).stream().filter(r -> r.feed().test(fluid)).min(Comparator.comparingInt(r -> {
			if (r.coFeed() == null)
				return 2;
			if (r.coFeed().test(second))
				return 0;
			return r.catalyst() != null && !catalyst.isEmpty() && r.catalyst().test(catalyst) ? 1 : 3;
		}));
	}

	private static List<PressurizingRecipe> all(Level level) {
		return level.getRecipeManager().getAllRecipesFor(ModRecipes.PRESSURIZING_TYPE.get());
	}

	/** Whether any recipe takes {@code fluid} as its second fluid, and none takes it first. */
	public static boolean onlyCoFeed(@Nullable Level level, FluidStack fluid) {
		if (level == null || fluid.isEmpty() || find(level, fluid).isPresent())
			return false;
		return all(level).stream().anyMatch(r -> r.coFeed() != null && r.coFeed().test(fluid));
	}

	/** Whether any recipe that takes {@code first} first takes {@code second} with it. */
	public static boolean takesSecond(@Nullable Level level, FluidStack first, FluidStack second) {
		if (level == null || first.isEmpty() || second.isEmpty())
			return false;
		return all(level).stream()
			.anyMatch(r -> r.feed().test(first) && r.coFeed() != null && r.coFeed().test(second));
	}

	/** Whether any recipe works over {@code stack} as its catalyst. */
	public static boolean anyCatalyst(@Nullable Level level, ItemStack stack) {
		if (level == null || stack.isEmpty())
			return false;
		return all(level).stream().anyMatch(r -> r.catalyst() != null && r.catalyst().test(stack));
	}

	public FluidIngredient feed() {
		return getFluidIngredients().get(0);
	}

	/** The second fluid it takes with the first, or null if it takes only one. */
	@Nullable
	public FluidIngredient coFeed() {
		return getFluidIngredients().size() > 1 ? getFluidIngredients().get(1) : null;
	}

	/** What it works over, dropped into the vessel, or null if it needs nothing. */
	@Nullable
	public Ingredient catalyst() {
		return getIngredients().isEmpty() ? null : getIngredients().get(0);
	}

	/** How many mB of what it makes one catalyst item lasts. */
	public int catalystLasts() {
		return catalystLasts;
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
		return 1;
	}

	@Override
	protected int getMaxOutputCount() {
		return 1;
	}

	@Override
	protected int getMaxFluidInputCount() {
		return 2;
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
		catalystLasts = Math.max(1, GsonHelper.getAsInt(json, "catalyst_lasts", DEFAULT_CATALYST_LASTS));
	}

	@Override
	public void readAdditional(FriendlyByteBuf buffer) {
		heat = HeatLevel.byId(buffer.readVarInt());
		minSpeed = buffer.readVarInt();
		catalystLasts = buffer.readVarInt();
	}

	@Override
	public void writeAdditional(JsonObject json) {
		json.addProperty("heat", heat.getSerializedName());
		json.addProperty("min_speed", minSpeed);
		json.addProperty("catalyst_lasts", catalystLasts);
	}

	@Override
	public void writeAdditional(FriendlyByteBuf buffer) {
		buffer.writeVarInt(heat.ordinal());
		buffer.writeVarInt(minSpeed);
		buffer.writeVarInt(catalystLasts);
	}
}

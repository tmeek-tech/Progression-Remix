package com.mcremix.pressurizer;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.mcremix.heat.HeatLevel;
import com.mcremix.machines.KindSlots;
import com.mcremix.registry.ModRecipes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;

/**
 * What a Pressurizer seals in a vacuum (Taran, 0.37.0: Power Grid's tubes and bulbs, sealed in place of being crafted
 * by hand). Turned the other way its pump pulls the air out of its vessel, and the fire under it melts the glass shut
 * round what lies inside: an ingredient for each item, put into the vessel a kind to a slot. The deeper the vacuum, the
 * faster it turns: under {@link #minSpeed()} RPM it is not deep enough, and at it a seal takes
 * {@link #getProcessingDuration()} ticks, half that at twice the speed. Bulbs seal from 128 RPM, tubes from 256 and the
 * cathode ray tube from 512, all over Hot heat ({@link #heat()}).
 * <pre>
 * {
 *   "type": "mcremix:vacuum_sealing",
 *   "ingredients": [{ "item": "minecraft:glass_pane" }, ... ],
 *   "results": [{ "item": "powergrid:light_bulb" }],
 *   "min_speed": 128,
 *   "heat": "hot",
 *   "processingTime": 100
 * }
 * </pre>
 */
public class VacuumSealingRecipe extends ProcessingRecipe<RecipeWrapper> {
	private HeatLevel heat = HeatLevel.HOT;
	private int minSpeed = 128;

	public VacuumSealingRecipe(ProcessingRecipeParams params) {
		super(ModRecipes.VACUUM_SEALING, params);
	}

	public static List<VacuumSealingRecipe> all(@Nullable Level level) {
		return level == null ? List.of()
			: level.getRecipeManager().getAllRecipesFor(ModRecipes.VACUUM_SEALING_TYPE.get());
	}

	/**
	 * The recipe what lies in the vessel seals into: of those it holds enough for, the one that takes the most, so a
	 * pentode's triode is not taken for something smaller; ties go by name.
	 */
	public static Optional<VacuumSealingRecipe> find(@Nullable Level level, IItemHandler held) {
		return all(level).stream().filter(r -> r.takeFrom(held, true))
			.max(Comparator.comparingInt((VacuumSealingRecipe r) -> r.getIngredients().size())
				.thenComparing(r -> r.getId().toString(), Comparator.reverseOrder()));
	}

	/** Whether any recipe takes {@code stack}. */
	public static boolean takes(@Nullable Level level, ItemStack stack) {
		for (VacuumSealingRecipe recipe : all(level))
			if (KindSlots.anyTakes(recipe.getIngredients(), stack))
				return true;
		return false;
	}

	/** Takes one item for each ingredient out of {@code held}, or with {@code simulate} only says whether it could. */
	public boolean takeFrom(IItemHandler held, boolean simulate) {
		return KindSlots.takeFrom(getIngredients(), held, simulate);
	}

	/** What it seals, its first result. */
	public ItemStack sealed() {
		return getRollableResults().isEmpty() ? ItemStack.EMPTY : getRollableResults().get(0).getStack().copy();
	}

	/** The heat it wants underneath, to melt the glass shut. */
	public HeatLevel heat() {
		return heat;
	}

	/** The slowest it seals at, in RPM: the vacuum it needs. */
	public int minSpeed() {
		return minSpeed;
	}

	/** How much of a seal a tick at {@code rpm} gets through, once it is fast enough. */
	public float progressAt(float rpm) {
		return rpm / minSpeed / Math.max(1, getProcessingDuration());
	}

	/** Found by what lies in the vessel with {@link #find}, never as a crafting recipe is. */
	@Override
	public boolean matches(RecipeWrapper inv, Level level) {
		return false;
	}

	@Override
	protected int getMaxInputCount() {
		return 12;
	}

	@Override
	protected int getMaxOutputCount() {
		return 1;
	}

	@Override
	protected boolean canSpecifyDuration() {
		return true;
	}

	@Override
	public void readAdditional(JsonObject json) {
		heat = HeatLevel.byName(GsonHelper.getAsString(json, "heat", "hot"));
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

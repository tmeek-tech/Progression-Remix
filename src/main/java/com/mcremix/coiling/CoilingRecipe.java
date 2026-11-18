package com.mcremix.coiling;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
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
 * What a coil winder winds (Taran, 0.37.0: Power Grid's coils, wound in place of being crafted by hand): wire, the
 * stick it goes round and anything else it takes, an ingredient for each item, so four copper wire are four entries.
 * At 16 RPM a coil takes {@link #getProcessingDuration()} ticks, a sixteenth of that at 256, as Create's millstone
 * grinds ({@link CoilWinderBlockEntity#progressPerTick}). {@code colour} is the wire's, which the bobbin fills with.
 * <pre>
 * {
 *   "type": "mcremix:coiling",
 *   "ingredients": [{ "tag": "forge:wires/iron" }, ... , { "item": "minecraft:stick" }],
 *   "results": [{ "item": "powergrid:resistive_coil" }],
 *   "processingTime": 300,
 *   "colour": "#8f9294"
 * }
 * </pre>
 */
public class CoilingRecipe extends ProcessingRecipe<RecipeWrapper> {
	/** Copper's, for a recipe that names no colour. */
	public static final int DEFAULT_COLOUR = 0xC8794A;

	private int colour = DEFAULT_COLOUR;

	public CoilingRecipe(ProcessingRecipeParams params) {
		super(ModRecipes.COILING, params);
	}

	public static List<CoilingRecipe> all(@Nullable Level level) {
		return level == null ? List.of() : level.getRecipeManager().getAllRecipesFor(ModRecipes.COILING_TYPE.get());
	}

	/** The first recipe everything in {@code inputs} is enough for. */
	public static Optional<CoilingRecipe> find(@Nullable Level level, IItemHandler inputs) {
		return all(level).stream().filter(r -> r.takeFrom(inputs, true)).findFirst();
	}

	/** Whether any recipe takes {@code stack}. */
	public static boolean takes(@Nullable Level level, ItemStack stack) {
		for (CoilingRecipe recipe : all(level))
			if (KindSlots.anyTakes(recipe.getIngredients(), stack))
				return true;
		return false;
	}

	/**
	 * Takes one item for each ingredient out of {@code inputs}, or with {@code simulate} only says whether it could:
	 * every ingredient finds an item of its own, no item counted twice.
	 */
	public boolean takeFrom(IItemHandler inputs, boolean simulate) {
		return KindSlots.takeFrom(getIngredients(), inputs, simulate);
	}

	/** What it winds, its first result. */
	public ItemStack coil() {
		return getRollableResults().isEmpty() ? ItemStack.EMPTY : getRollableResults().get(0).getStack().copy();
	}

	/** The colour of its wire, as 0xRRGGBB. */
	public int colour() {
		return colour;
	}

	/** Found by what lies in a winder with {@link #find}, never as a crafting recipe is. */
	@Override
	public boolean matches(RecipeWrapper inv, Level level) {
		return false;
	}

	@Override
	protected int getMaxInputCount() {
		return 9;
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
		String hex = GsonHelper.getAsString(json, "colour", "");
		colour = hex.isEmpty() ? DEFAULT_COLOUR : Integer.parseInt(hex.startsWith("#") ? hex.substring(1) : hex, 16);
	}

	@Override
	public void readAdditional(FriendlyByteBuf buffer) {
		colour = buffer.readInt();
	}

	@Override
	public void writeAdditional(JsonObject json) {
		json.addProperty("colour", String.format("#%06x", colour & 0xFFFFFF));
	}

	@Override
	public void writeAdditional(FriendlyByteBuf buffer) {
		buffer.writeInt(colour);
	}
}

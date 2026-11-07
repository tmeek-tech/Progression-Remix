package com.mcremix.crucible;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mcremix.heat.HeatLevel;
import com.mcremix.registry.ModRecipes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Finding the crucible's recipes, and what its two recipe types share. */
public final class CrucibleRecipes {
	/** Each recipe manager's melting recipe by item, the client's and the server's apart; a reload makes a new one. */
	private static final Map<RecipeManager, Map<Item, Optional<MeltingRecipe>>> MELTING =
		Collections.synchronizedMap(new WeakHashMap<>());

	private CrucibleRecipes() {}

	public static Optional<MeltingRecipe> melting(Level level, ItemStack stack) {
		if (level == null || stack.isEmpty())
			return Optional.empty();
		RecipeManager manager = level.getRecipeManager();
		return MELTING.computeIfAbsent(manager, m -> new ConcurrentHashMap<>()).computeIfAbsent(stack.getItem(),
			item -> manager.getAllRecipesFor(ModRecipes.MELTING_TYPE.get()).stream()
				.filter(recipe -> recipe.matches(stack)).findFirst());
	}

	public static List<AlloyingRecipe> alloying(Level level) {
		return level.getRecipeManager().getAllRecipesFor(ModRecipes.ALLOYING_TYPE.get());
	}

	/** Something that does not melt but is stirred into an alloy, like the coke that makes steel. */
	public static boolean isAlloyingSolid(Level level, ItemStack stack) {
		for (AlloyingRecipe recipe : alloying(level))
			if (recipe.usesItem(stack))
				return true;
		return false;
	}

	/** Every heat level above the least halves the time, as in the Furnace. */
	static int timeAt(int time, HeatLevel least, HeatLevel at) {
		int extra = Math.max(0, at.ordinal() - least.ordinal());
		return Math.max(1, time >> extra);
	}

	static FluidStack fluidFromJson(JsonObject json) {
		ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "fluid"));
		Fluid fluid = ForgeRegistries.FLUIDS.getValue(id);
		if (fluid == null || fluid == Fluids.EMPTY)
			throw new JsonSyntaxException("Unknown fluid '" + id + "'");
		return new FluidStack(fluid, GsonHelper.getAsInt(json, "amount"));
	}
}

package com.mcremix.machines;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Slots that hold a kind of thing each, as the coil winder's and the Pressurizer's vessel's do for what they work on
 * (0.37.0), and recipes that take one item from them for each of their ingredients.
 */
public final class KindSlots {
	private KindSlots() {}

	/** Puts what fits of {@code stack} in with more of its kind, or else into an empty slot; gives back the rest. */
	public static ItemStack insert(IItemHandler slots, ItemStack stack, boolean simulate) {
		if (stack.isEmpty())
			return stack;
		for (int slot = 0; slot < slots.getSlots(); slot++)
			if (ItemHandlerHelper.canItemStacksStack(slots.getStackInSlot(slot), stack))
				return slots.insertItem(slot, stack, simulate);
		for (int slot = 0; slot < slots.getSlots(); slot++)
			if (slots.getStackInSlot(slot).isEmpty())
				return slots.insertItem(slot, stack, simulate);
		return stack;
	}

	/** Whether the slots hold nothing. */
	public static boolean empty(IItemHandler slots) {
		for (int slot = 0; slot < slots.getSlots(); slot++)
			if (!slots.getStackInSlot(slot).isEmpty())
				return false;
		return true;
	}

	/** Takes out the last kind of thing put in, for a hand that finds nothing else to take. */
	public static ItemStack takeLast(IItemHandler slots) {
		for (int slot = slots.getSlots() - 1; slot >= 0; slot--)
			if (!slots.getStackInSlot(slot).isEmpty())
				return slots.extractItem(slot, 64, false);
		return ItemStack.EMPTY;
	}

	/**
	 * Takes one item for each of {@code ingredients} out of {@code slots}, or with {@code simulate} only says whether it
	 * could: every ingredient finds an item of its own, no item counted twice.
	 */
	public static boolean takeFrom(List<Ingredient> ingredients, IItemHandler slots, boolean simulate) {
		int[] left = counts(slots);
		List<Integer> taken = new ArrayList<>();
		for (Ingredient ingredient : ingredients) {
			int found = find(ingredient, slots, left);
			if (found < 0)
				return false;
			left[found]--;
			taken.add(found);
		}
		if (!simulate)
			for (int slot : taken)
				slots.extractItem(slot, 1, false);
		return true;
	}

	/** Those of {@code ingredients} the slots hold no item for, each item counted once. */
	public static List<Ingredient> unmet(List<Ingredient> ingredients, IItemHandler slots) {
		int[] left = counts(slots);
		List<Ingredient> unmet = new ArrayList<>();
		for (Ingredient ingredient : ingredients) {
			int found = find(ingredient, slots, left);
			if (found >= 0)
				left[found]--;
			else
				unmet.add(ingredient);
		}
		return unmet;
	}

	private static int[] counts(IItemHandler slots) {
		int[] left = new int[slots.getSlots()];
		for (int slot = 0; slot < left.length; slot++)
			left[slot] = slots.getStackInSlot(slot).getCount();
		return left;
	}

	/** The first slot with an item left that {@code ingredient} takes, or -1. */
	private static int find(Ingredient ingredient, IItemHandler slots, int[] left) {
		for (int slot = 0; slot < left.length; slot++)
			if (left[slot] > 0 && ingredient.test(slots.getStackInSlot(slot)))
				return slot;
		return -1;
	}

	/** Whether any of {@code ingredients} takes {@code stack}. */
	public static boolean anyTakes(List<Ingredient> ingredients, ItemStack stack) {
		if (stack.isEmpty())
			return false;
		for (Ingredient ingredient : ingredients)
			if (ingredient.test(stack))
				return true;
		return false;
	}

	/** Whether the slots hold anything one of {@code ingredients} takes. */
	public static boolean sharesAny(List<Ingredient> ingredients, IItemHandler slots) {
		for (int slot = 0; slot < slots.getSlots(); slot++)
			if (anyTakes(ingredients, slots.getStackInSlot(slot)))
				return true;
		return false;
	}

	/** What {@code ingredients} want that the slots do not hold, by name: "2 Cardboard and 1 Stick". */
	public static Component missing(List<Ingredient> ingredients, IItemHandler slots) {
		Map<String, Integer> wants = new LinkedHashMap<>();
		Map<String, Component> names = new LinkedHashMap<>();
		for (Ingredient ingredient : unmet(ingredients, slots)) {
			ItemStack[] items = ingredient.getItems();
			if (items.length == 0)
				continue;
			String id = items[0].getItem().getDescriptionId();
			wants.merge(id, 1, Integer::sum);
			names.putIfAbsent(id, items[0].getHoverName());
		}
		MutableComponent list = Component.empty();
		int i = 0;
		for (Map.Entry<String, Integer> want : wants.entrySet()) {
			if (i > 0)
				list.append(Component.translatable(i == wants.size() - 1 ? "mcremix.goggles.wants.and"
					: "mcremix.goggles.wants.comma"));
			list.append(Component.translatable("mcremix.goggles.wants.count", want.getValue(), names.get(want.getKey())));
			i++;
		}
		return list;
	}
}

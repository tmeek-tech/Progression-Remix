package com.mcremix.flint;

import static com.mcremix.McRemix.REGISTRATE;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.mcremix.McRemix;
import com.mcremix.registry.ModTags;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;

/**
 * Flint tools (0.41.0, Taran's ask): they take the place of wooden and stone ones, which are no longer made
 * ({@link OldTools}). Flint is knapped on stone into Knapped Flint ({@link Knapping}), which makes them in vanilla's
 * shapes. Sharp but brittle: they mine what a stone tool did, iron ore included, as fast as iron, but last half as
 * long as stone.
 */
public final class FlintTools {
	public static final int USES = 65;
	public static final float SPEED = 6f;

	/**
	 * Ranked after stone and before iron, so it mines whatever a stone tool could. Nothing needs flint in particular:
	 * its tag, {@code mcremix:needs_flint_tool}, is empty unless a pack fills it. Mended on an anvil with Knapped Flint,
	 * as stone tools were with what made them.
	 */
	public static final Tier FLINT = TierSortingRegistry.registerTier(
		new ForgeTier(1, USES, SPEED, 1f, 5, ModTags.Blocks.NEEDS_FLINT_TOOL,
			() -> Ingredient.of(FlintTools.KNAPPED_FLINT.get())),
		McRemix.asResource("flint"), List.of(Tiers.STONE), List.of(Tiers.IRON));

	/** Flint struck on stone until a sharp flake comes away: what flint tools are made of. */
	public static final ItemEntry<Item> KNAPPED_FLINT = REGISTRATE.item("knapped_flint", Item::new)
		.register();

	// stone's attack numbers, as the tools they replace
	public static final ItemEntry<PickaxeItem> PICKAXE = REGISTRATE
		.item("flint_pickaxe", p -> new PickaxeItem(FLINT, 1, -2.8f, p))
		.register();
	public static final ItemEntry<AxeItem> HATCHET = REGISTRATE
		.item("flint_hatchet", p -> new AxeItem(FLINT, 7f, -3.2f, p))
		.register();
	public static final ItemEntry<ShovelItem> SHOVEL = REGISTRATE
		.item("flint_shovel", p -> new ShovelItem(FLINT, 1.5f, -3f, p))
		.register();
	public static final ItemEntry<HoeItem> HOE = REGISTRATE
		.item("flint_hoe", p -> new HoeItem(FLINT, -1, -2f, p))
		.register();
	/** In place of the sword: a point softer than a stone sword's edge but quicker, and no sweep. */
	public static final ItemEntry<FlintKnifeItem> KNIFE = REGISTRATE
		.item("flint_knife", p -> new FlintKnifeItem(FLINT, 2, -2f, p))
		.register();

	private FlintTools() {}

	/** Each wooden and stone tool and the flint one that takes its place, in chests, on mobs and in trades. */
	public static Map<Item, Item> replacements() {
		return REPLACEMENTS.get();
	}

	private static final Supplier<Map<Item, Item>> REPLACEMENTS = Suppliers.memoize(() -> Map.of(
			Items.WOODEN_PICKAXE, PICKAXE.get(), Items.STONE_PICKAXE, PICKAXE.get(),
			Items.WOODEN_AXE, HATCHET.get(), Items.STONE_AXE, HATCHET.get(),
			Items.WOODEN_SHOVEL, SHOVEL.get(), Items.STONE_SHOVEL, SHOVEL.get(),
			Items.WOODEN_HOE, HOE.get(), Items.STONE_HOE, HOE.get(),
			Items.WOODEN_SWORD, KNIFE.get(), Items.STONE_SWORD, KNIFE.get()));

	/**
	 * The flint tool in place of a wooden or stone one, or {@code null} for anything else. Count, enchantments and
	 * name carry over, and wear as the same share of its uses.
	 */
	public static ItemStack replace(ItemStack stack) {
		Item flint = stack.isEmpty() ? null : replacements().get(stack.getItem());
		if (flint == null)
			return null;
		ItemStack swapped = new ItemStack(flint, stack.getCount());
		if (stack.getTag() != null)
			swapped.setTag(stack.getTag().copy());
		if (stack.isDamageableItem() && stack.getMaxDamage() > 0) {
			float worn = (float) stack.getDamageValue() / stack.getMaxDamage();
			swapped.setDamageValue(Math.min(swapped.getMaxDamage() - 1, Math.round(worn * swapped.getMaxDamage())));
		}
		return swapped;
	}

	public static List<ItemEntry<? extends Item>> tools() {
		return List.of(PICKAXE, HATCHET, SHOVEL, HOE, KNIFE);
	}

	public static void register() {}
}

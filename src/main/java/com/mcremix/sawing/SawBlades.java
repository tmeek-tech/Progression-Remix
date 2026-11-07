package com.mcremix.sawing;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import com.mcremix.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.TierSortingRegistry;

/**
 * The blade fitted to a Mechanical Saw, Create's own or any material's, in the drill bits' materials
 * ({@link com.mcremix.drilling.DrillBits}): the same speeds relative to iron and the same stress. Gold is too soft for
 * stone, so a gold blade only does the wood cutting; a stone item skates across it uncut, throwing sparks. A stone blade
 * grinds stone as well as wood. A new saw comes with an iron blade; using another blade on a placed saw fits it, the old
 * one popping out ({@link FittedBlade}), and a saw that is broken keeps its blade in the item. In the world every blade
 * fells trees, as any axe would.
 */
public enum SawBlades {
	STONE(Tiers.STONE, 2 / 3f, 2, true),
	IRON(Tiers.IRON, 1, 4, true),
	GOLD(Tiers.GOLD, 2, 2, false),
	DIAMOND(Tiers.DIAMOND, 4 / 3f, 8, true);

	/** Where a saw keeps its blade, on the block entity and in a dropped saw's {@code BlockEntityTag}. */
	public static final String TAG = "McRemixBlade";

	public final Tier tier;
	/** Cutting speed, as a multiple of an iron blade at the same RPM. */
	public final float speed;
	/** Stress impact per RPM. */
	public final double stress;
	/** Whether the blade does stonecutting recipes. */
	public final boolean cutsStone;

	SawBlades(Tier tier, float speed, double stress, boolean cutsStone) {
		this.tier = tier;
		this.speed = speed;
		this.stress = stress;
		this.cutsStone = cutsStone;
	}

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** The item's id: stone_saw_blade, iron_saw_blade, golden_saw_blade, diamond_saw_blade. */
	public String itemName() {
		return (this == GOLD ? "golden" : id()) + "_saw_blade";
	}

	public Item item() {
		return ModItems.SAW_BLADES.get(this).get();
	}

	/** A blade named by its id, or iron for anything else (a saw that never had another fitted). */
	public static SawBlades byId(String id) {
		for (SawBlades blades : values())
			if (blades.id().equals(id))
				return blades;
		return IRON;
	}

	/** The blade a block entity's data or a dropped saw's {@code BlockEntityTag} names: iron if none. */
	public static SawBlades of(@Nullable CompoundTag data) {
		return data == null ? IRON : byId(data.getString(TAG));
	}

	/** The blade a saw item goes down with: iron unless it was broken with another fitted. */
	@Nullable
	public static SawBlades of(ItemStack stack) {
		if (!(stack.getItem() instanceof BlockItem item) || !takes(item.getBlock().defaultBlockState()))
			return null;
		return of(stack.getTagElement("BlockEntityTag"));
	}

	/** The blade fitted to a saw. Null for other mods' saws, which keep their own rules. */
	@Nullable
	public static SawBlades of(SawBlockEntity saw) {
		return takes(saw.getBlockState()) ? ((FittedBlade) saw).mcremix$blades() : null;
	}

	/** Create's saw and this mod's take blades; other mods' saws, built on Create's, are left alone. */
	public static boolean takes(BlockState saw) {
		return AllBlocks.MECHANICAL_SAW.has(saw) || saw.getBlock() instanceof TieredSawBlock;
	}

	/** Whether an axe of this material would get drops from the block, by Forge's tier tags. */
	public boolean canCut(BlockState state) {
		return TierSortingRegistry.isCorrectTierForDrops(tier, state);
	}

	/** Whether the blade can do a recipe the saw found for the item on it. */
	public boolean canUse(Recipe<?> recipe) {
		return cutsStone || recipe.getType() != RecipeType.STONECUTTING;
	}

	/** "Gold blade: cuts wood, too soft for stone", for item tooltips and goggles. */
	public MutableComponent describe() {
		return Component.translatable("mcremix.saw_blade." + id());
	}

	/** Goggle lines: which blade, what it cuts, and how fast. */
	public void addGoggleInfo(List<Component> tooltip) {
		tooltip.add(indent().append(describe().withStyle(ChatFormatting.WHITE)));
		tooltip.add(indent().append(Component.translatable("mcremix.goggles.drill_bits.speed",
			Component.translatable("mcremix.drill_bits.speed." + id())).withStyle(ChatFormatting.GRAY)));
	}

	private static MutableComponent indent() {
		return Component.literal("    ");
	}
}

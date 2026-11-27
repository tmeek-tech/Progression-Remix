package com.mcremix.flint;

import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/**
 * Leaves drop a stick more often, {@code chance} of the time on top of vanilla's 1 in 50, since a stick is half the
 * first hatchet and logs need one ({@link LogsNeedAnAxe}). Leaves taken whole, by shears or silk touch, drop no
 * sticks, as in vanilla.
 */
public class LeafSticksModifier extends LootModifier {
	public static final Supplier<Codec<LeafSticksModifier>> CODEC = Suppliers.memoize(() ->
		RecordCodecBuilder.create(inst -> codecStart(inst)
			.and(Codec.floatRange(0, 1).fieldOf("chance").forGetter(m -> m.chance))
			.apply(inst, LeafSticksModifier::new)));

	private final float chance;

	public LeafSticksModifier(LootItemCondition[] conditions, float chance) {
		super(conditions);
		this.chance = chance;
	}

	@Override
	protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
		BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
		if (state == null || !state.is(BlockTags.LEAVES))
			return loot;
		Item leaves = state.getBlock().asItem();
		if (loot.stream().anyMatch(stack -> stack.is(leaves)))
			return loot;
		if (context.getRandom().nextFloat() < chance)
			loot.add(new ItemStack(Items.STICK));
		return loot;
	}

	@Override
	public Codec<? extends IGlobalLootModifier> codec() {
		return CODEC.get();
	}
}

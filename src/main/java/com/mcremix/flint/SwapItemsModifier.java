package com.mcremix.flint;

import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/**
 * Wooden and stone tools in any loot (the bonus chest, igloos, ocean ruins, suspicious sand, a hero's gifts) come out
 * as the flint ones ({@link FlintTools#replace}).
 */
public class SwapItemsModifier extends LootModifier {
	public static final Supplier<Codec<SwapItemsModifier>> CODEC = Suppliers.memoize(() ->
		RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, SwapItemsModifier::new)));

	public SwapItemsModifier(LootItemCondition[] conditions) {
		super(conditions);
	}

	@Override
	protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
		loot.replaceAll(stack -> {
			ItemStack swapped = FlintTools.replace(stack);
			return swapped == null ? stack : swapped;
		});
		return loot;
	}

	@Override
	public Codec<? extends IGlobalLootModifier> codec() {
		return CODEC.get();
	}
}

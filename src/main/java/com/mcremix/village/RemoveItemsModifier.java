package com.mcremix.village;

import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/** Strips a tag's items from every loot table whose id starts with a prefix (e.g. all village chests). */
public class RemoveItemsModifier extends LootModifier {
	public static final Supplier<Codec<RemoveItemsModifier>> CODEC = Suppliers.memoize(() ->
		RecordCodecBuilder.create(inst -> codecStart(inst)
			.and(Codec.STRING.fieldOf("table_prefix").forGetter(m -> m.tablePrefix))
			.and(TagKey.codec(Registries.ITEM).fieldOf("items").forGetter(m -> m.items))
			.apply(inst, RemoveItemsModifier::new)));

	private final String tablePrefix;
	private final TagKey<Item> items;

	public RemoveItemsModifier(LootItemCondition[] conditions, String tablePrefix, TagKey<Item> items) {
		super(conditions);
		this.tablePrefix = tablePrefix;
		this.items = items;
	}

	@Override
	protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
		if (!context.getQueriedLootTableId().toString().startsWith(tablePrefix))
			return loot;
		loot.removeIf(stack -> stack.is(items));
		return loot;
	}

	@Override
	public Codec<? extends IGlobalLootModifier> codec() {
		return CODEC.get();
	}
}

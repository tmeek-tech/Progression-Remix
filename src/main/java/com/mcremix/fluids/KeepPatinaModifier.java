package com.mcremix.fluids;

import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/**
 * A weathered or waxed copper pipe, pump or tank of Create's drops as it was, so it can go back down weathered or waxed;
 * see {@link Patina#mark}. Create's loot tables stay as they are. Machines keep their patina, or titanium's tint, too.
 */
public class KeepPatinaModifier extends LootModifier {
	public static final Supplier<Codec<KeepPatinaModifier>> CODEC = Suppliers.memoize(() ->
		RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, KeepPatinaModifier::new)));

	public KeepPatinaModifier(LootItemCondition[] conditions) {
		super(conditions);
	}

	@Override
	protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
		BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
		if (state == null || !Patina.kept(state))
			return loot;
		Patina patina = state.getValue(Patina.PROPERTY);
		for (ItemStack stack : loot)
			Patina.mark(stack, patina);
		return loot;
	}

	@Override
	public Codec<? extends IGlobalLootModifier> codec() {
		return CODEC.get();
	}
}

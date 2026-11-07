package com.mcremix.machines;

import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.mcremix.drilling.DrillBits;
import com.mcremix.sawing.SawBlades;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.kinetics.drill.DrillBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/**
 * A drill or saw broken with other bits or another blade than iron keeps them (Taran's call): the drill it drops carries
 * them in its {@code BlockEntityTag} and goes back down with them, so nothing is lost or handed out twice. One with iron
 * drops plain and stacks with new ones. Create's loot tables stay as they are.
 */
public class KeepFittingsModifier extends LootModifier {
	public static final Supplier<Codec<KeepFittingsModifier>> CODEC = Suppliers.memoize(() ->
		RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, KeepFittingsModifier::new)));

	public KeepFittingsModifier(LootItemCondition[] conditions) {
		super(conditions);
	}

	@Override
	protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
		BlockEntity be = context.getParamOrNull(LootContextParams.BLOCK_ENTITY);
		String key, id;
		if (be instanceof DrillBlockEntity drill && DrillBits.of(drill) != null && DrillBits.of(drill) != DrillBits.IRON) {
			key = DrillBits.TAG;
			id = DrillBits.of(drill).id();
		} else if (be instanceof SawBlockEntity saw && SawBlades.of(saw) != null
			&& SawBlades.of(saw) != SawBlades.IRON) {
			key = SawBlades.TAG;
			id = SawBlades.of(saw).id();
		} else
			return loot;
		for (ItemStack stack : loot)
			if (stack.getItem() instanceof BlockItem item && item.getBlock() == be.getBlockState().getBlock())
				stack.getOrCreateTagElement("BlockEntityTag").putString(key, id);
		return loot;
	}

	@Override
	public Codec<? extends IGlobalLootModifier> codec() {
		return CODEC.get();
	}
}

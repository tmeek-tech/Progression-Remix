package com.mcremix.registry;

import com.mcremix.McRemix;
import com.mcremix.flint.LeafSticksModifier;
import com.mcremix.flint.SwapItemsModifier;
import com.mcremix.fluids.KeepPatinaModifier;
import com.mcremix.machines.KeepFittingsModifier;
import com.mcremix.village.RemoveItemsModifier;
import com.mojang.serialization.Codec;

import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModLootModifiers {
	private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
		DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, McRemix.ID);

	public static final RegistryObject<Codec<RemoveItemsModifier>> REMOVE_ITEMS =
		SERIALIZERS.register("remove_items", RemoveItemsModifier.CODEC);
	public static final RegistryObject<Codec<KeepPatinaModifier>> KEEP_PATINA =
		SERIALIZERS.register("keep_patina", KeepPatinaModifier.CODEC);
	public static final RegistryObject<Codec<KeepFittingsModifier>> KEEP_FITTINGS =
		SERIALIZERS.register("keep_fittings", KeepFittingsModifier.CODEC);

	/** Wooden and stone tools in loot come out as flint ones (0.41.0). */
	public static final RegistryObject<Codec<SwapItemsModifier>> SWAP_OLD_TOOLS =
		SERIALIZERS.register("swap_old_tools", SwapItemsModifier.CODEC);
	/** Leaves drop sticks more often, since logs need a hatchet (0.41.0). */
	public static final RegistryObject<Codec<LeafSticksModifier>> LEAF_STICKS =
		SERIALIZERS.register("leaf_sticks", LeafSticksModifier.CODEC);

	public static void register(IEventBus modBus) {
		SERIALIZERS.register(modBus);
	}
}

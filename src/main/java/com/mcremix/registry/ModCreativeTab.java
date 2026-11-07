package com.mcremix.registry;

import com.mcremix.McRemix;
import com.mcremix.kinetics.Weathering;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTab {
	private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, McRemix.ID);

	public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
		.title(Component.translatable("itemGroup.mcremix.main"))
		.icon(() -> new ItemStack(ModBlocks.KILN.get()))
		.displayItems((params, output) -> {
			for (RegistryEntry<Item> entry : McRemix.REGISTRATE.getAll(Registries.ITEM))
				if (shown(entry.get()))
					output.accept(entry.get());
		})
		.build());

	/**
	 * Everything this mod adds, but steel parts rusted past their first stage: steel stops there since 0.28.0, and they
	 * are kept only so that worlds that have them still load. JEI builds its list from the creative tabs, so it leaves
	 * them out too.
	 */
	public static boolean shown(Item item) {
		return !(item instanceof BlockItem block && Weathering.pastLastStage(block.getBlock().defaultBlockState()));
	}

	public static void register(IEventBus modBus) {
		TABS.register(modBus);
	}
}

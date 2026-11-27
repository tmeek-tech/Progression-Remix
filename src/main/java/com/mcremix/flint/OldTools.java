package com.mcremix.flint;

import java.util.List;

import com.mcremix.McRemix;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Wooden and stone tools are gone (0.41.0, Taran's card): their recipes are off (data/minecraft/recipes), JEI and the
 * creative tabs leave them out, and wherever the game would still hand one out it hands out the flint one instead: in
 * chests and other loot ({@link SwapItemsModifier}), in a mob's hands, and in a villager's trades. The items stay
 * registered, as vanilla's, so a world that has them still loads.
 */
@Mod.EventBusSubscriber(modid = McRemix.ID)
public class OldTools {
	public static final List<Item> GONE = List.of(
		Items.WOODEN_SWORD, Items.WOODEN_SHOVEL, Items.WOODEN_PICKAXE, Items.WOODEN_AXE, Items.WOODEN_HOE,
		Items.STONE_SWORD, Items.STONE_SHOVEL, Items.STONE_PICKAXE, Items.STONE_AXE, Items.STONE_HOE);

	/** A wither skeleton's stone sword, or any mob's wooden or stone tool, is flint by the time it is in the world. */
	@SubscribeEvent
	public static void onJoin(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob))
			return;
		for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND }) {
			ItemStack swapped = FlintTools.replace(mob.getItemBySlot(slot));
			if (swapped != null)
				mob.setItemSlot(slot, swapped); // the slot keeps its drop chance
		}
	}

	/** A toolsmith's stone tools, and any other trade handing one out, hand out flint ones. */
	@SubscribeEvent
	public static void onTrades(VillagerTradesEvent event) {
		for (List<VillagerTrades.ItemListing> listings : event.getTrades().values())
			listings.replaceAll(OldTools::swapping);
	}

	private static VillagerTrades.ItemListing swapping(VillagerTrades.ItemListing listing) {
		return (trader, random) -> {
			MerchantOffer offer = listing.getOffer(trader, random);
			ItemStack swapped = offer == null ? null : FlintTools.replace(offer.getResult());
			if (swapped == null)
				return offer;
			return new MerchantOffer(offer.getBaseCostA(), offer.getCostB(), swapped, offer.getUses(), offer.getMaxUses(),
				offer.getXp(), offer.getPriceMultiplier(), offer.getDemand());
		};
	}

	/** Out of vanilla's tabs, with the flint tools where the stone ones were (they are in this mod's tab too). */
	public static void fillTabs(BuildCreativeModeTabContentsEvent event) {
		var tab = event.getTabKey();
		if (tab == CreativeModeTabs.TOOLS_AND_UTILITIES) {
			before(event, Items.IRON_SHOVEL, FlintTools.SHOVEL.get(), FlintTools.PICKAXE.get(), FlintTools.HATCHET.get(),
				FlintTools.HOE.get());
		} else if (tab == CreativeModeTabs.COMBAT) {
			before(event, Items.IRON_SWORD, FlintTools.KNIFE.get());
			before(event, Items.IRON_AXE, FlintTools.HATCHET.get());
		}
		for (Item item : GONE)
			event.getEntries().remove(new ItemStack(item));
	}

	private static void before(BuildCreativeModeTabContentsEvent event, ItemLike anchor, ItemLike... items) {
		ItemStack next = new ItemStack(anchor);
		if (!event.getEntries().contains(next))
			return;
		for (ItemLike item : items)
			event.getEntries().putBefore(next, new ItemStack(item), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
	}
}

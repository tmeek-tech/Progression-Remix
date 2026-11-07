package com.mcremix.client;

import com.mcremix.McRemix;
import com.mcremix.drilling.DrillBits;
import com.mcremix.fluids.FluidHeat;
import com.mcremix.fluids.Patina;
import com.mcremix.heat.HeatLevel;
import com.mcremix.sawing.SawBlades;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = McRemix.ID, value = Dist.CLIENT)
public class ClientEvents {

	/**
	 * Every drill names the bits it goes down with, and every saw its blade, under its name: iron for a new one, Create's
	 * own included, and whatever was fitted when it was broken. Pipes, pumps,
	 * tanks and whatever else holds fluids say how hot a fluid they take, and a weathered, rusted or waxed one says so.
	 */
	@SubscribeEvent
	public static void onItemTooltip(ItemTooltipEvent event) {
		if (!(event.getItemStack().getItem() instanceof BlockItem item) || event.getToolTip().isEmpty())
			return;
		BlockState state = item.getBlock().defaultBlockState();
		HeatLevel ceiling = FluidHeat.ceiling(state);
		if (ceiling != null)
			event.getToolTip().add(1, Component.translatable("mcremix.tooltip.fluid_ceiling", ceiling.displayName())
				.withStyle(ChatFormatting.GRAY));
		Patina patina = Patina.of(event.getItemStack());
		// iron and steel rust rather than oxidize; a titanium machine keeps its tint in the same place: gold, purple or
		// blue
		if (patina != Patina.NONE) {
			String kind = !Patina.has(state) ? "mcremix.tint." : Patina.rusts(state) ? "mcremix.rust." : "mcremix.patina.";
			event.getToolTip().add(1, Component.translatable(kind + patina.getSerializedName())
				.withStyle(ChatFormatting.GRAY));
		}
		DrillBits bits = DrillBits.of(event.getItemStack());
		if (bits != null)
			event.getToolTip().add(1, bits.describe().withStyle(ChatFormatting.GRAY));
		SawBlades blades = SawBlades.of(event.getItemStack());
		if (blades != null)
			event.getToolTip().add(1, blades.describe().withStyle(ChatFormatting.GRAY));
	}
}

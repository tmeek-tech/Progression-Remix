package com.mcremix.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mcremix.McRemix;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.kinetics.KineticRatings;
import com.mcremix.kinetics.KineticRatings.Stability;
import com.mcremix.kinetics.KineticRatings.Strength;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * A support's Stability or a shaft or cog's Strength on the tooltip of every kinetic block made of a material, Create's
 * own included, under Create's stress lines and drawn the way Create draws them (Taran, 0.23.0, reworked in 0.27.0): a
 * bar and a level word, or the exact RPM or su while wearing goggles. Stability is the top speed a support turns at: on
 * gearboxes, clutches, gearshifts, chain drives, brackets and machines, and on the casings that make an encased part a
 * support. Strength is the load a shaft or cog carries before it gives way. Create's crushing wheel holds no shafts but
 * keeps andesite's top speed, so it shows a Stability too.
 */
@Mod.EventBusSubscriber(modid = McRemix.ID, value = Dist.CLIENT)
public final class KineticTooltips {
	private KineticTooltips() {}

	/** After Create's own lines, so these come under its stress impact and capacity. */
	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onItemTooltip(ItemTooltipEvent event) {
		if (!(event.getItemStack().getItem() instanceof BlockItem item) || event.getToolTip().isEmpty())
			return;
		List<Component> lines = lines(event.getItemStack(), item.getBlock().defaultBlockState(), event.getEntity());
		if (lines.isEmpty())
			return;
		List<Component> tooltip = event.getToolTip();
		if (!hasCreateStats(tooltip))
			tooltip.add(CommonComponents.EMPTY);
		tooltip.addAll(lines);
	}

	public static List<Component> lines(ItemStack stack, BlockState state, @Nullable Player player) {
		List<Component> list = new ArrayList<>();
		KineticRatings.Rating rating = KineticRatings.of(stack, state);
		if (rating == null)
			return list;
		boolean goggles = player != null && GogglesItem.isWearingGoggles(player);
		if (rating.strength())
			strength(list, rating.material(), goggles);
		else
			stability(list, rating.material(), goggles);
		return list;
	}

	/** A support's Stability: the fastest it turns before it grinds itself apart. */
	private static void stability(List<Component> list, KineticMaterial material, boolean goggles) {
		Stability stability = Stability.of(material);
		list.add(Component.translatable("mcremix.tooltip.stability").withStyle(ChatFormatting.GRAY));
		LangBuilder line = bar(Stability.values().length, stability.ordinal() + 1, stability.color);
		if (goggles)
			line.add(CreateLang.number(material.maxSpeed)).text(" ").add(CreateLang.translate("generic.unit.rpm"));
		else
			line.add(Component.translatable(stability.key()));
		list.add(line.component());
	}

	/** A shaft or cog's Strength: the most stress its network may draw before it gives way. */
	private static void strength(List<Component> list, KineticMaterial material, boolean goggles) {
		Strength strength = Strength.of(material);
		list.add(Component.translatable("mcremix.tooltip.strength").withStyle(ChatFormatting.GRAY));
		LangBuilder line = bar(Strength.values().length, strength.ordinal() + 1, strength.color);
		if (goggles && material.maxLoad > 0)
			line.add(CreateLang.number(material.maxLoad)).text(" ").add(CreateLang.translate("generic.unit.stress"));
		else
			line.add(Component.translatable(strength.key()));
		list.add(line.component());
	}

	private static LangBuilder bar(int length, int filled, ChatFormatting color) {
		return CreateLang.builder().add(CreateLang.text(TooltipHelper.makeProgressBar(length, filled)).style(color));
	}

	/** Whether Create has put its stress impact or capacity on the tooltip, with the blank line before them. */
	private static boolean hasCreateStats(List<Component> tooltip) {
		for (Component line : tooltip)
			if (line.getContents() instanceof TranslatableContents t && (t.getKey().equals("create.tooltip.stressImpact")
				|| t.getKey().equals("create.tooltip.capacityProvided")))
				return true;
		return false;
	}
}

package com.mcremix.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mcremix.McRemix;
import com.mcremix.formula.FormulaTooltip;
import com.mcremix.formula.Formulas;
import com.mcremix.formula.StructuralFormula;
import com.mojang.datafixers.util.Either;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Structural formulas on tooltips (Taran, 0.25.0), always shown, under the name: of the items each formula lists, of
 * the buckets of its fluids, and of those fluids where JEI shows them, known there by their names.
 */
@Mod.EventBusSubscriber(modid = McRemix.ID, value = Dist.CLIENT)
public final class FormulaTooltips {
	private FormulaTooltips() {}

	/** On the mod bus. */
	public static void onRegisterTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
		event.register(FormulaTooltip.class, tooltip -> new FormulaTooltipRenderer(tooltip.formula()));
	}

	/** On the mod bus. */
	public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
		event.registerReloadListener(new Loader());
	}

	@SubscribeEvent
	public static void onGatherComponents(RenderTooltipEvent.GatherComponents event) {
		List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
		StructuralFormula formula = Formulas.index().of(event.getItemStack());
		if (formula == null && event.getItemStack().isEmpty() && !elements.isEmpty())
			formula = elements.get(0).left().map(FormulaTooltips::fluidNamed).orElse(null);
		if (formula != null)
			elements.add(Math.min(1, elements.size()), Either.right(new FormulaTooltip(formula)));
	}

	/** A tooltip with no item whose first line names a fluid, as JEI's tooltips for fluids do. */
	@Nullable
	private static StructuralFormula fluidNamed(FormattedText line) {
		return line instanceof Component component && component.getContents() instanceof TranslatableContents name
			? Formulas.index().ofName(name.getKey()) : null;
	}

	/** Reads assets/&lt;namespace&gt;/formulas with the other resources, so a resource pack can change them. */
	private static class Loader extends SimpleJsonResourceReloadListener {
		Loader() {
			super(new Gson(), "formulas");
		}

		@Override
		protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
			List<StructuralFormula> formulas = new ArrayList<>();
			files.forEach((id, json) -> {
				try {
					formulas.add(StructuralFormula.fromJson(GsonHelper.convertToJsonObject(json, id.toString())));
				} catch (RuntimeException e) {
					McRemix.LOGGER.error("Couldn't read structural formula {}", id, e);
				}
			});
			Formulas.set(formulas);
			FormulaTooltipRenderer.forget();
		}
	}
}

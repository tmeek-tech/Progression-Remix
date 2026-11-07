package com.mcremix.kinetics;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;

import com.mcremix.fluids.Patina;
import com.mcremix.machines.MachineBody;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Goggle lines for every kinetic block with a material, Create's own included: what it is made of and how weathered it
 * is (for an encased part, its casing and the part inside), whether it is a support or what holds it, the
 * friction it draws, the top speed where it has one, the load it carries, how hot it is where it heats up, and what is
 * giving way when it is past a limit.
 */
public final class FrictionGoggles {
	private FrictionGoggles() {}

	/** Adds the lines, returning whether there were any. */
	public static boolean add(List<Component> tooltip, KineticBlockEntity be) {
		Level level = be.getLevel();
		if (level == null)
			return false;
		BlockState state = be.getBlockState();
		BlockPos pos = be.getBlockPos();
		KineticMaterial material = KineticMaterials.of(state);
		boolean bare = FrictionPoints.isBare(state);
		BlockState inner = FrictionPoints.innerPart(be);
		if (material == null && !bare && inner == null)
			return false;

		FrictionPoints.Limits limits = FrictionPoints.limitsFor(be);
		String holder = null;
		if (inner != null) {
			holder = "casing";
			tooltip.add(line(material == null ? Component.translatable("mcremix.goggles.friction_point")
				: Component.translatable("mcremix.goggles.casing", material(material)), ChatFormatting.AQUA));
			addTint(tooltip, state, null);
			boolean weathered = FrictionPoints.weathering(inner) > 0;
			tooltip.add(line(Component.translatable("mcremix.goggles.inside", inner.getBlock().getName()),
				weathered ? ChatFormatting.GOLD : ChatFormatting.GRAY));
		} else {
			if (material != null)
				tooltip.add(line(material(material), ChatFormatting.WHITE));
			if (state.getBlock() instanceof TieredKinetic tiered)
				addWeathering(tooltip, tiered.variant(), null);
			else if (state.getBlock() instanceof MachineBody machine && Patina.has(state))
				addWeathering(tooltip, machine.material(), state.getValue(Patina.PROPERTY).stage(),
					state.getValue(Patina.PROPERTY).waxed(), null);
			addTint(tooltip, state, null);
		}

		if (bare) {
			BlockState bracket = FrictionPoints.bracket(level, pos);
			if (bracket != null && Brackets.isBolted(level, pos, bracket)) {
				holder = "bracket";
				KineticMaterial held = KineticMaterials.of(bracket);
				tooltip.add(line(held == null ? Component.translatable("mcremix.goggles.friction_point")
					: Component.translatable("mcremix.goggles.bracket", material(held)), ChatFormatting.AQUA));
				if (bracket.getBlock() instanceof TieredKinetic tieredBracket)
					addWeathering(tooltip, tieredBracket.variant(), "mcremix.goggles.bracket_weathering");
				addTint(tooltip, bracket, "mcremix.goggles.bracket_weathering");
			} else if (FrictionPoints.inSpeedControllerSlot(level, pos, state)) {
				tooltip.add(line(Component.translatable("mcremix.goggles.held.speed_controller"), ChatFormatting.GRAY));
			} else {
				int distance = FrictionPoints.distanceToHolder(level, pos, state);
				if (distance > 0)
					tooltip.add(line(distance == 1 ? Component.translatable("mcremix.goggles.held.1")
						: Component.translatable("mcremix.goggles.held", distance), ChatFormatting.GRAY));
				int span = FrictionPoints.span(state);
				tooltip.add(line(span == 1 ? Component.translatable("mcremix.goggles.span.1")
					: Component.translatable("mcremix.goggles.span", span), ChatFormatting.GRAY));
			}
		} else if (inner == null && limits.frictionPoint()) {
			tooltip.add(line(Component.translatable("mcremix.goggles.friction_point"), ChatFormatting.AQUA));
		}

		float friction = FrictionPoints.friction(be);
		if (friction > 0)
			tooltip.add(line(Component.translatable("mcremix.goggles.friction",
				new BigDecimal(friction).stripTrailingZeros().toPlainString()), ChatFormatting.GRAY));
		KineticMaterial speed = limits.speed();
		if (speed != null)
			tooltip.add(line(Component.translatable("mcremix.goggles.max_speed", speed.maxSpeed), ChatFormatting.GRAY));
		else if (bare)
			tooltip.add(line(Component.translatable("mcremix.goggles.no_top_speed"), ChatFormatting.GRAY));
		KineticMaterial load = limits.load();
		if (load != null)
			tooltip.add(line(load.maxLoad > 0 ? Component.translatable("mcremix.goggles.max_load", load.maxLoad)
				: Component.translatable("mcremix.goggles.max_load_unlimited"), ChatFormatting.GRAY));

		FrictionHeatBehaviour heat = FrictionHeat.of(be);
		if (heat != null && heat.getTemperature() > ThermalBehaviour.ABSOLUTE_ZERO && (heat.heats() || heat.warm())) {
			float temperature = heat.getTemperature();
			// aluminium melts, titanium burns; what has stopped heating cools as the aluminium it most likely is
			KineticMaterial hot = heat.material() == null ? KineticMaterial.ALUMINIUM : heat.material();
			boolean burns = hot == KineticMaterial.TITANIUM;
			float limit = FrictionHeat.limit(hot), glows = FrictionHeat.glowsFrom(hot);
			float warning = burns ? limit - 50 : FrictionHeat.DRIPS_FROM;
			tooltip.add(line(Component.translatable(burns ? "mcremix.goggles.temperature.burns" : "mcremix.goggles.temperature",
				Math.round(temperature), Math.round(limit)), temperature >= warning ? ChatFormatting.RED
					: temperature >= glows ? ChatFormatting.GOLD : ChatFormatting.GRAY));
		}

		KineticLimits.Breach breach = KineticLimits.exceeded(be, stressOf(be));
		if (breach != null) {
			String key = breach.cause() == KineticLimits.Cause.LOAD ? "mcremix.goggles.wobbling" : "mcremix.goggles.grinding";
			tooltip.add(line(Component.translatable(breach.holder() && holder != null ? key + "." + holder : key),
				ChatFormatting.RED));
		}
		return true;
	}

	/** The stress the part's network draws, as the client last heard it. */
	private static float stressOf(KineticBlockEntity be) {
		return be instanceof KineticLimits.Strained strained ? strained.mcremix$networkStress() : 0;
	}

	/** Its weathering stage and wax, for parts and brackets that weather; {@code wrapKey} puts "Bracket: " before it. */
	private static void addWeathering(List<Component> tooltip, KineticVariant v, @Nullable String wrapKey) {
		addWeathering(tooltip, v.material(), v.stage(), v.waxed(), wrapKey);
	}

	/** The temper colour heat has left on titanium, if it has any; {@code wrapKey} puts "Bracket: " before it. */
	private static void addTint(List<Component> tooltip, BlockState state, @Nullable String wrapKey) {
		WeatherState tint = Tints.of(state);
		if (tint == WeatherState.UNAFFECTED)
			return;
		Component text = Component.translatable("mcremix.goggles.tint." + tint.name().toLowerCase(Locale.ROOT));
		tooltip.add(line(wrapKey == null ? text : Component.translatable(wrapKey, text), ChatFormatting.LIGHT_PURPLE));
	}

	/** The same for anything of a material that weathers: a part, a bracket or a machine. */
	private static void addWeathering(List<Component> tooltip, KineticMaterial material, WeatherState stage,
		boolean waxed, @Nullable String wrapKey) {
		if (!material.weathers)
			return;
		String stageKey = material.rusts() ? "rust" : "patina";
		Component text = Component.translatable("mcremix.goggles." + stageKey + "." + stage.name().toLowerCase(Locale.ROOT));
		tooltip.add(line(wrapKey == null ? text : Component.translatable(wrapKey, text),
			KineticMaterial.weatheringFriction(stage) > 0 ? ChatFormatting.GOLD : ChatFormatting.GREEN));
		if (waxed)
			tooltip.add(line(Component.translatable("mcremix.goggles.waxed"), ChatFormatting.YELLOW));
	}

	public static MutableComponent material(KineticMaterial material) {
		return Component.translatable("mcremix.goggles.material." + material.prefix);
	}

	private static MutableComponent line(Component text, ChatFormatting style) {
		return Component.literal("    ").append(text.copy().withStyle(style));
	}
}

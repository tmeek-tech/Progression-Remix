package com.mcremix.kinetics;

import java.util.Locale;

import javax.annotation.Nullable;

import com.mcremix.machines.MachineTiers;
import com.mcremix.registry.ModTags;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The levels item tooltips rate a material at, beside Create's stress impact and capacity (Taran, 0.23.0, reworked in
 * 0.27.0): a support's Stability, the top speed it turns at, and a shaft or cog's Strength, the load it carries before
 * it gives way.
 */
public final class KineticRatings {
	private KineticRatings() {}

	/** What a tooltip rates a block for: a support's Stability or a shaft or cog's Strength, in a material. */
	public record Rating(boolean strength, KineticMaterial material) {}

	/**
	 * What the tooltip of an item placing {@code state} rates: a shaft or cog its Strength; a support (a gearbox,
	 * clutch, gearshift, chain drive, bracket or machine) its Stability, and Create's crushing wheel too, which holds no
	 * shafts but keeps a top speed; a casing the Stability it gives what it encases. Null for anything else.
	 */
	@Nullable
	public static Rating of(ItemStack stack, BlockState state) {
		KineticMaterial material = KineticMaterials.of(state);
		if (material == null) {
			EncasingCasing casing = EncasingCasing.of(stack);
			return casing == null ? null : new Rating(false, casing.material);
		}
		if (FrictionPoints.isBare(state))
			return new Rating(true, material);
		if (!state.is(ModTags.Blocks.HOLDS_NO_SHAFTS) || MachineTiers.isCrushingWheel(state))
			return new Rating(false, material);
		return null;
	}

	/** How much load a shaft or cog carries: wood least, the unlimited ones never give way under load. */
	public enum Strength {
		FLIMSY(2048, ChatFormatting.RED),
		DELICATE(3072, ChatFormatting.GOLD),
		LIGHT_DUTY(4096, ChatFormatting.YELLOW),
		STURDY(Integer.MAX_VALUE, ChatFormatting.GREEN),
		UNBREAKABLE(0, ChatFormatting.AQUA);

		/** The most su a level covers; unbreakable ones have no limit. */
		final int upTo;
		public final ChatFormatting color;

		Strength(int upTo, ChatFormatting color) {
			this.upTo = upTo;
			this.color = color;
		}

		public static Strength of(KineticMaterial material) {
			if (material.maxLoad <= 0)
				return UNBREAKABLE;
			for (Strength level : values())
				if (level != UNBREAKABLE && material.maxLoad <= level.upTo)
					return level;
			return STURDY;
		}

		public String key() {
			return "mcremix.tooltip.strength." + name().toLowerCase(Locale.ROOT);
		}
	}

	/** How fast a support turns before it grinds itself apart: wood least, titanium past anything else. */
	public enum Stability {
		WOBBLY(64, ChatFormatting.RED),
		SHAKY(96, ChatFormatting.GOLD),
		STEADY(128, ChatFormatting.YELLOW),
		STABLE(256, ChatFormatting.GREEN),
		VERY_STABLE(512, ChatFormatting.AQUA),
		ROCK_SOLID(Integer.MAX_VALUE, ChatFormatting.LIGHT_PURPLE);

		/** The fastest RPM a level covers. */
		final int upTo;
		public final ChatFormatting color;

		Stability(int upTo, ChatFormatting color) {
			this.upTo = upTo;
			this.color = color;
		}

		public static Stability of(KineticMaterial material) {
			for (Stability level : values())
				if (material.maxSpeed <= level.upTo)
					return level;
			return ROCK_SOLID;
		}

		public String key() {
			return "mcremix.tooltip.stability." + name().toLowerCase(Locale.ROOT);
		}
	}
}

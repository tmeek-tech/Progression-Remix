package com.mcremix.kinetics;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

import com.simibubi.create.content.kinetics.chainDrive.ChainGearshiftBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the chain gearshift tiers: Create's adjustable chain gearshift, with friction from weathering and,
 * while powered, how much faster it drives the rest of its row.
 */
public class TieredChainGearshiftBlockEntity extends ChainGearshiftBlockEntity {
	private static final DecimalFormat RATIO = new DecimalFormat("0.###", DecimalFormatSymbols.getInstance(Locale.ROOT));

	public TieredChainGearshiftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public float calculateStressApplied() {
		if (!(getBlockState().getBlock() instanceof TieredKinetic tiered))
			return super.calculateStressApplied();
		float impact = tiered.variant().friction();
		this.lastStressApplied = impact;
		return impact;
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		// the rest of the row turns this many times as fast as this gearshift, whichever end the drive comes in at
		float ratio = getModifier();
		if (ratio > 1)
			tooltip.add(Component.literal("    ").append(Component.translatable("mcremix.goggles.chain_gearshift",
				RATIO.format(ratio)).withStyle(ChatFormatting.GOLD)));
		return true;
	}
}

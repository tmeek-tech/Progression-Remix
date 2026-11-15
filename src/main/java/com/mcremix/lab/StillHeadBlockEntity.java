package com.mcremix.lab;

import java.util.List;

import javax.annotation.Nullable;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** A still head shows, through goggles, what the round flask it stands on is doing: the thermometer's reading. */
public class StillHeadBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
	public StillHeadBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	/** The round flask under it, through any column pieces, or null. */
	@Nullable
	public RoundFlaskBlockEntity flask() {
		if (level == null)
			return null;
		BlockPos at = worldPosition.below();
		for (int i = 0; i <= RoundFlaskBlockEntity.MAX_COLUMNS; i++) {
			if (level.getBlockEntity(at) instanceof RoundFlaskBlockEntity flask)
				return flask;
			if (!(level.getBlockState(at).getBlock() instanceof GlassColumnBlock))
				return null;
			at = at.below();
		}
		return null;
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		RoundFlaskBlockEntity flask = flask();
		if (flask == null) {
			tooltip.add(CoilCondenserBlockEntity.line(getBlockState().getBlock().getName()));
			tooltip.add(CoilCondenserBlockEntity.line(Component.translatable("mcremix.goggles.still.no_flask")
				.withStyle(net.minecraft.ChatFormatting.GOLD)));
			return true;
		}
		return flask.addToGoggleTooltip(tooltip, isPlayerSneaking);
	}
}

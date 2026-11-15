package com.mcremix.lab;

import java.util.List;

import javax.annotation.Nullable;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A still head shows, through goggles, what the round flask it stands on is doing: the thermometer's reading. It also
 * times the thermometer's pulse ({@link StillHeadBlock#pulse}), turning it off when it is over.
 */
public class StillHeadBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
	/** The game time the pulse ends. */
	private long pulseEnds;

	public StillHeadBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	void pulseUntil(long time) {
		pulseEnds = time;
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;
		BlockState state = getBlockState();
		if (state.getValue(StillHeadBlock.POWERED) && level.getGameTime() >= pulseEnds)
			level.setBlock(worldPosition, state.setValue(StillHeadBlock.POWERED, false), Block.UPDATE_ALL);
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

	/** More glass column pieces under it than a still takes, so no flask is found through them. */
	private boolean tooManyColumns() {
		BlockPos at = worldPosition.below();
		for (int i = 0; i <= RoundFlaskBlockEntity.MAX_COLUMNS; i++, at = at.below())
			if (!(level.getBlockState(at).getBlock() instanceof GlassColumnBlock))
				return false;
		return true;
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		RoundFlaskBlockEntity flask = flask();
		if (flask == null) {
			tooltip.add(CoilCondenserBlockEntity.line(getBlockState().getBlock().getName()));
			String why = level != null && tooManyColumns() ? "mcremix.goggles.still.incomplete.columns"
				: "mcremix.goggles.still.no_flask";
			tooltip.add(CoilCondenserBlockEntity.line(Component.translatable(why)
				.withStyle(net.minecraft.ChatFormatting.GOLD)));
			return true;
		}
		return flask.addToGoggleTooltip(tooltip, isPlayerSneaking);
	}
}

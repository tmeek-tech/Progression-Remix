package com.mcremix.fluids;

import java.util.List;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A PVC pipe or corner piece in Create's fluid network: it carries fluid toward whichever sides its block state has
 * open, and a pipe remembers which of its joints a wrench has cut.
 */
public class PvcPipeBlockEntity extends SmartBlockEntity {
	private int cuts;

	public PvcPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(new Transport(this));
		registerAwardables(behaviours, FluidPropagator.getSharedTriggers());
	}

	public boolean isCut(Direction side) {
		return (cuts & 1 << side.ordinal()) != 0;
	}

	public void setCut(Direction side, boolean cut) {
		int was = cuts;
		cuts = cut ? cuts | 1 << side.ordinal() : cuts & ~(1 << side.ordinal());
		if (cuts != was)
			notifyUpdate();
	}

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		if (cuts != 0)
			tag.putByte("Cuts", (byte) cuts);
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		cuts = tag.getByte("Cuts");
	}

	static class Transport extends FluidTransportBehaviour {
		Transport(SmartBlockEntity be) {
			super(be);
		}

		@Override
		public boolean canHaveFlowToward(BlockState state, Direction direction) {
			return (PvcPipes.isPipe(state) || PvcPipes.isCorner(state))
				&& state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction))
				&& !PipeMetal.clashToward(blockEntity, state, direction);
		}
	}
}

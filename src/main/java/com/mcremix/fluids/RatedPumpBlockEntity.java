package com.mcremix.fluids;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A metal pump's block entity, pumping as Create's. A brass one has a handwheel setting the most it lets through
 * (Taran, 0.26.0): from a millibucket a tick up to all it can move, which is 24 at its top speed, since Create's pump
 * moves half its speed in millibuckets a tick and a line through brass goes at half pace. Holding right-click on the
 * handwheel sets it, as on Create's own value boxes, and a line through the pump moves no more a tick than it is set to
 * ({@code FluidNetworkMixin}).
 */
public class RatedPumpBlockEntity extends PumpBlockEntity {
	/** A brass pump's handwheel. It is made while the block entity is, before its own fields are set: none is given here. */
	@Nullable
	private ScrollValueBehaviour handwheel;

	public RatedPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		if (getBlockState().getBlock() instanceof RatedPumpBlock.Throttled throttled) {
			handwheel = new Handwheel(this, topFlow(throttled.material()));
			behaviours.add(handwheel);
		}
	}

	/** All a pump of {@code material} can move a tick, at its top speed through a line of its own metal. */
	public static int topFlow(FluidPartMaterial material) {
		return Math.max(1, material.kinetic().maxSpeed / 2 / material.slowdown());
	}

	/** The most a line through this pump moves a tick: as a brass pump's handwheel is set, or no limit. */
	public int maxFlow() {
		return handwheel == null ? Integer.MAX_VALUE : handwheel.getValue();
	}

	/** Sets a brass pump's handwheel, to between one and all it can move; nothing on any other pump. */
	public void setMaxFlow(int flow) {
		if (handwheel != null)
			handwheel.setValue(flow);
	}

	/** The handwheel: lets all through until it is turned down. */
	private static class Handwheel extends ScrollValueBehaviour {
		Handwheel(SmartBlockEntity be, int top) {
			super(Component.translatable("mcremix.pump.max_flow"), be, new HandwheelBox());
			between(1, top);
			value = top;
		}

		@Override
		public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
			return new ValueSettingsBoard(label, max, 4,
				ImmutableList.of(Component.translatable("mcremix.pump.max_flow.row")), new ValueSettingsFormatter(
					settings -> Component.translatable("mcremix.pump.max_flow.value", Math.max(1, settings.value()))));
		}

		/** A brass pump placed before it had a handwheel lets all through, as it did. */
		@Override
		public void read(CompoundTag nbt, boolean clientPacket) {
			if (nbt.contains("ScrollValue"))
				super.read(nbt, clientPacket);
			value = Mth.clamp(value, 1, max);
		}
	}

	/** Where the handwheel is: only on its own side, wherever the pump faces ({@link RatedPumpBlock.Throttled}). */
	private static class HandwheelBox extends ValueBoxTransform.Sided {
		@Override
		protected Vec3 getSouthLocation() {
			return Vec3.ZERO;
		}

		@Override
		public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
			return RatedPumpBlock.Throttled.wheel(state.getValue(PumpBlock.FACING));
		}

		@Override
		public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
			super.rotate(level, pos, state, ms);
			// on a pump lying down, the box reads across the pump, as on Create's creative motor
			if (getSide() == Direction.UP)
				TransformStack.of(ms)
					.rotateZDegrees(-AngleHelper.horizontalAngle(state.getValue(PumpBlock.FACING)) + 180);
		}

		@Override
		protected boolean isSideActive(BlockState state, Direction direction) {
			return state.hasProperty(PumpBlock.FACING)
				&& direction == RatedPumpBlock.Throttled.wheelSide(state.getValue(PumpBlock.FACING));
		}
	}
}

package com.mcremix.kinetics;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * A steel flywheel's block entity: Create's flywheel's, which keeps where the wheel has turned to on the client, easing
 * its speed up and down as a heavy wheel does. Create keeps those to its own package, so this is its own.
 */
public class SteelFlywheelBlockEntity extends KineticBlockEntity {
	private final LerpedFloat visualSpeed = LerpedFloat.linear();
	private float angle;

	public SteelFlywheelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected AABB createRenderBoundingBox() {
		return super.createRenderBoundingBox().inflate(2);
	}

	@Override
	protected void read(CompoundTag compound, boolean clientPacket) {
		super.read(compound, clientPacket);
		if (clientPacket)
			visualSpeed.chase(getGeneratedSpeed(), 1 / 64f, Chaser.EXP);
	}

	@Override
	public void tick() {
		super.tick();
		if (!level.isClientSide)
			return;
		visualSpeed.updateChaseTarget(getSpeed());
		visualSpeed.tickChaser();
		angle += visualSpeed.getValue() * 3 / 10f;
		angle %= 360;
	}

	/** Where the wheel has turned to, in degrees, {@code partialTicks} on from the last tick. */
	public float angle(float partialTicks) {
		return angle + visualSpeed.getValue(partialTicks) * 3 / 10f * partialTicks;
	}
}

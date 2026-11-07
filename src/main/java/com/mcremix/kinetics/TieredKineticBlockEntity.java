package com.mcremix.kinetics;

import java.util.List;

import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.AbstractSimpleShaftBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Block entity for every mod shaft and cog. A bare part turns freely; with a bracket it is a friction point and draws
 * the bracket's friction, and a little more for its own weathering ({@link FrictionPoints#bracketFriction}). Goggle
 * lines come from {@link FrictionGoggles}, as for every kinetic block.
 * <p>
 * It is built on Create's powered shaft so that a steam engine drives a shaft of any material where it stands (Taran,
 * 0.26.0): Create's engine turns the shaft two blocks out from it, which it swaps for its own powered shaft when that is
 * Create's andesite one, and this mod's shafts it drives as they are. The shaft stays a bare part, since an engine holds
 * nothing: it needs a bracket or a support within its span like any other. Undriven, it turns as Create's simple shafts
 * and cogs do.
 */
public class TieredKineticBlockEntity extends PoweredShaftBlockEntity implements TransformableBlockEntity {

	public TieredKineticBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public KineticVariant variant() {
		return ((TieredKinetic) getBlockState().getBlock()).variant();
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(new BracketedBlockEntityBehaviour(this, state -> state.getBlock() instanceof AbstractSimpleShaftBlock));
		super.addBehaviours(behaviours);
	}

	@Override
	public void transform(BlockEntity be, StructureTransform transform) {
		BracketedBlockEntityBehaviour bracket = getBehaviour(BracketedBlockEntityBehaviour.TYPE);
		if (bracket != null)
			bracket.transformBracket(transform);
	}

	@Override
	public float calculateStressApplied() {
		if (!(getBlockState().getBlock() instanceof TieredKinetic))
			return super.calculateStressApplied();
		float impact = level == null ? 0 : FrictionPoints.bracketFriction(level, worldPosition, getBlockState());
		this.lastStressApplied = impact;
		return impact;
	}

	/** Only a shaft, standing where an engine turns one and across it, as Create's engine takes its own. */
	@Override
	public boolean canBePoweredBy(BlockPos enginePos) {
		return super.canBePoweredBy(enginePos) && drivenFrom(enginePos);
	}

	private boolean drivenFrom(BlockPos enginePos) {
		BlockState state = getBlockState();
		if (level == null || !(state.getBlock() instanceof TieredShaftBlock))
			return false;
		BlockState engine = level.getBlockState(enginePos);
		return engine.getBlock() instanceof SteamEngineBlock
			&& SteamEngineBlock.getShaftPos(engine, enginePos).equals(worldPosition)
			&& state.getValue(RotatedPillarKineticBlock.AXIS) != SteamEngineBlock.getFacing(engine).getAxis();
	}

	/**
	 * Create's powered shaft turns back into a plain one once its engine has gone; this one stays itself, so it lets go
	 * of an engine that is no longer there to drive it, taken away or left behind by a contraption.
	 */
	@Override
	public void lazyTick() {
		super.lazyTick();
		if (level == null || level.isClientSide || enginePos == null)
			return;
		BlockPos engine = worldPosition.subtract(enginePos);
		if (level.isLoaded(engine) && !drivenFrom(engine))
			remove(engine);
	}

	/** It tells goggles what it is made of, as every part does; Create's powered shaft leaves its lines to its engine. */
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		return addToEngineTooltip(tooltip, isPlayerSneaking);
	}

	/**
	 * A shaft takes the half turn Create gives its powered shafts on every other block, so engines along one line beat
	 * in turn as on Create's; a cog does not, since its teeth would no longer mesh.
	 */
	@Override
	public int getRotationAngleOffset(Axis axis) {
		return getBlockState().getBlock() instanceof TieredShaftBlock ? super.getRotationAngleOffset(axis) : 0;
	}

	// the rest as Create's simple shafts and cogs: a large cog meshes with cogs a diagonal away
	@Override
	protected AABB createRenderBoundingBox() {
		return new AABB(worldPosition).inflate(1);
	}

	@Override
	public List<BlockPos> addPropagationLocations(IRotate block, BlockState state, List<BlockPos> neighbours) {
		if (!ICogWheel.isLargeCog(state))
			return super.addPropagationLocations(block, state, neighbours);
		BlockPos.betweenClosedStream(new BlockPos(-1, -1, -1), new BlockPos(1, 1, 1))
			.forEach(offset -> {
				if (offset.distSqr(BlockPos.ZERO) == 2)
					neighbours.add(worldPosition.offset(offset));
			});
		return neighbours;
	}

	@Override
	protected boolean isNoisy() {
		return false;
	}
}

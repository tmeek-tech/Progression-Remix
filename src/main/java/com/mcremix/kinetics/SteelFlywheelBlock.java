package com.mcremix.kinetics;

import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.Patina;
import com.mcremix.registry.ModOil;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The steel flywheel (Taran, 0.36.0): Create's flywheel in steel, set on a shaft line and spun, and what a diesel
 * engine is built round. It turns as Create's does, its shaft running through it and on out of both sides, and like
 * Create's it holds no shafts ({@code mcremix:holds_no_shafts}). Like the rest of this mod's steel it rusts where it
 * gets wet, only to its first stage ({@link Patina}); wax keeps it as it is. Its block entity's renderer and visual
 * turn the wheel and a steel shaft through it (client/SteelFlywheelVisual).
 */
public class SteelFlywheelBlock extends RotatedPillarKineticBlock implements IBE<SteelFlywheelBlockEntity>,
	FluidPartMaterial.Part {

	public SteelFlywheelBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(Patina.PROPERTY, Patina.NONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(Patina.PROPERTY));
	}

	@Override
	public FluidPartMaterial material() {
		return FluidPartMaterial.STEEL;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return AllShapes.LARGE_GEAR.get(state.getValue(AXIS));
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
		return face.getAxis() == getRotationAxis(state);
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.getValue(AXIS);
	}

	@Override
	public float getParticleTargetRadius() {
		return 2f;
	}

	@Override
	public float getParticleInitialRadius() {
		return 1.75f;
	}

	@Override
	public Class<SteelFlywheelBlockEntity> getBlockEntityClass() {
		return SteelFlywheelBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SteelFlywheelBlockEntity> getBlockEntityType() {
		return ModOil.STEEL_FLYWHEEL_BE.get();
	}
}

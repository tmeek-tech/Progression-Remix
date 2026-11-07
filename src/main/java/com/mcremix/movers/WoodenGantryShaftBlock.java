package com.mcremix.movers;

import com.mcremix.kinetics.WoodSpecies;
import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlock;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** A Gantry Shaft in a wood: Create's. It joins any gantry shafts in line with it, and carries any carriage. */
public class WoodenGantryShaftBlock extends GantryShaftBlock implements WoodenMover {
	private final WoodSpecies wood;

	public WoodenGantryShaftBlock(Properties properties, WoodSpecies wood) {
		super(properties);
		this.wood = wood;
	}

	@Override
	public MoverKind kind() {
		return MoverKind.GANTRY_SHAFT;
	}

	@Override
	public WoodSpecies wood() {
		return wood;
	}

	/** As Create's does when it becomes part of a longer row, which Create checks for its own block alone. */
	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onPlace(state, level, pos, oldState, isMoving);
		if (level.isClientSide() || !oldState.is(this))
			return;
		Part oldPart = oldState.getValue(PART), part = state.getValue(PART);
		if (((oldPart != Part.MIDDLE && part == Part.MIDDLE) || (oldPart == Part.SINGLE && part != Part.SINGLE))
			&& level.getBlockEntity(pos) instanceof GantryShaftBlockEntity shaft)
			shaft.checkAttachedCarriageBlocks();
	}

	@Override
	public BlockEntityType<? extends GantryShaftBlockEntity> getBlockEntityType() {
		return ModBlockEntities.WOODEN_GANTRY_SHAFT.get();
	}
}

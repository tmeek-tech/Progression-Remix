package com.mcremix.air;

import com.mcremix.column.ColumnSectionBlock;
import com.mcremix.column.ColumnSectionBlockEntity;
import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.registry.ModAir;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * An aluminium column section (the distillation design, stage 4): bare aluminium with flange rings and rivets, joining
 * as steel sections do, one to three across and up to sixteen high, into the air column that splits liquid air
 * ({@link AirColumnSectionBlockEntity}). Like other aluminium it never rusts, and it melts with anything Hot beside it.
 * Running, it frosts over and then grows icicles from its flange rings ({@link #FROST}); that is only looks.
 */
public class AirColumnSectionBlock extends ColumnSectionBlock {
	/** 0 bare, 1 frosted white, 2 frosted with icicles hanging from its flange rings. */
	public static final IntegerProperty FROST = IntegerProperty.create("frost", 0, 2);

	public AirColumnSectionBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FROST, 0));
	}

	@Override
	protected boolean rusts() {
		return false;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FROST));
	}

	@Override
	public FluidPartMaterial material() {
		return FluidPartMaterial.ALUMINIUM;
	}

	@Override
	public BlockEntityType<? extends ColumnSectionBlockEntity> getBlockEntityType() {
		return ModAir.AIR_COLUMN_SECTION_BE.get();
	}
}

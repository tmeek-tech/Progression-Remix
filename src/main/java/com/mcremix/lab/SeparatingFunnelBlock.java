package com.mcremix.lab;

import com.mcremix.registry.ModBlockEntities;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * A separating funnel hung in a ring on its stand, its tap pointing down to whatever is under it. See
 * {@link SeparatingFunnelBlockEntity}.
 */
public class SeparatingFunnelBlock extends BenchGlassBlock<SeparatingFunnelBlockEntity> {
	public SeparatingFunnelBlock(Properties properties) {
		super(properties, Shapes.or(Block.box(4, 4, 4, 12, 14, 12), Block.box(7, 0, 7, 9, 4, 9),
			Block.box(6.5, 14, 6.5, 9.5, 16, 9.5), Block.box(13, 0, 13, 15, 16, 15)));
	}

	@Override
	public Class<SeparatingFunnelBlockEntity> getBlockEntityClass() {
		return SeparatingFunnelBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SeparatingFunnelBlockEntity> getBlockEntityType() {
		return ModBlockEntities.SEPARATING_FUNNEL.get();
	}
}

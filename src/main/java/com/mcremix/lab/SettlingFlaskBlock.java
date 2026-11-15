package com.mcremix.lab;

import com.mcremix.registry.ModBlockEntities;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.Shapes;

/** A settling flask: a wide-bottomed flask on its stand, left to stand cool. See {@link SettlingFlaskBlockEntity}. */
public class SettlingFlaskBlock extends BenchGlassBlock<SettlingFlaskBlockEntity> {
	public SettlingFlaskBlock(Properties properties) {
		super(properties, Shapes.or(Block.box(2, 0, 2, 14, 7, 14), Block.box(5, 7, 5, 11, 13, 11),
			Block.box(6, 13, 6, 10, 16, 10), rod(Direction.NORTH)));
	}

	@Override
	public Class<SettlingFlaskBlockEntity> getBlockEntityClass() {
		return SettlingFlaskBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SettlingFlaskBlockEntity> getBlockEntityType() {
		return ModBlockEntities.SETTLING_FLASK.get();
	}
}

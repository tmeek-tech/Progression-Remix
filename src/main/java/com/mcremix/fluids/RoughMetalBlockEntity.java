package com.mcremix.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** How much metal, in mB, set into a block of rough metal. */
public class RoughMetalBlockEntity extends BlockEntity {
	public int amount;

	public RoughMetalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("Amount", amount);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		amount = tag.getInt("Amount");
	}
}

package com.mcremix.kiln;

import com.mcremix.registry.ModBlockEntities;
import com.mcremix.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Steel Furnace: a Furnace cased in steel. It fires, smelts and bakes as a Furnace does, heats a boiler or basin on
 * top the same way, and works fluids too, but burns only fuel piped in from underneath ({@link SteelFurnaceBlockEntity}).
 * Steel casing used on a Furnace makes one, keeping all it held but its solid fuel, which pops out, and its fire, which
 * goes out.
 */
public class SteelFurnaceBlock extends KilnBlock {
	public SteelFurnaceBlock(Properties properties) {
		super(properties);
	}

	/** Cases the Furnace at {@code pos} in steel, keeping what it holds and which way it faces; its fire goes out. */
	public static void upgrade(Level level, BlockPos pos, BlockState furnace) {
		BlockEntity old = level.getBlockEntity(pos);
		CompoundTag kept = old == null ? null : old.saveWithoutMetadata();
		// taken away first, so the Furnace doesn't spill what it holds as it goes
		level.removeBlockEntity(pos);
		level.setBlock(pos, ModBlocks.STEEL_FURNACE.getDefaultState().setValue(FACING, furnace.getValue(FACING))
			.setValue(CHIMNEY, furnace.getValue(CHIMNEY)), 3);
		if (kept != null && level.getBlockEntity(pos) instanceof SteelFurnaceBlockEntity steel) {
			steel.load(kept);
			steel.cased();
			steel.notifyUpdate();
		}
		level.playSound(null, pos, SoundEvents.NETHERITE_BLOCK_PLACE, SoundSource.BLOCKS, 1, 0.8f);
	}

	@Override
	public BlockEntityType<? extends KilnBlockEntity> getBlockEntityType() {
		return ModBlockEntities.STEEL_FURNACE.get();
	}
}

package com.mcremix.fluids;

import javax.annotation.Nullable;

import com.mcremix.registry.ModFluidBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Which steam engine goes on which boiler (Taran, 0.24.0): each metal's engine on its own metal's tanks alone, and
 * Create's copper one on Create's copper tanks alone (SteamEngineBlockMixin, SteamEngineBlockEntityMixin). Create's
 * engine stood on a brass, iron or steel boiler from before becomes that metal's engine, so the boiler keeps running.
 */
public final class SteamEngines {
	private SteamEngines() {}

	/** The tank {@code engine} goes on: its metal's, or Create's for Create's own engine. */
	public static Block boilerFor(Block engine) {
		return engine instanceof MetalSteamEngineBlock metal ? ModFluidBlocks.tank(metal.material()).get()
			: AllBlocks.FLUID_TANK.get();
	}

	/** The engine that goes on {@code tank}, or null if it is no tank of a metal with an engine of its own. */
	@Nullable
	public static MetalSteamEngineBlock engineFor(BlockState tank) {
		FluidPartMaterial material = FluidPartMaterial.of(tank);
		if (!(tank.getBlock() instanceof FluidTankBlock) || material == null || !material.hasEngine())
			return null;
		return ModFluidBlocks.engine(material).get();
	}

	/** The tank {@code engine} stands on. */
	public static BlockState boilerUnder(Level level, BlockPos pos, BlockState engine) {
		return level.getBlockState(pos.relative(SteamEngineBlock.getConnectedDirection(engine).getOpposite()));
	}

	/**
	 * Create's own engine on a boiler of another metal becomes that metal's engine, standing and turning as it did.
	 * Returns whether it did.
	 */
	public static boolean match(SteamEngineBlockEntity engine) {
		Level level = engine.getLevel();
		BlockState state = engine.getBlockState();
		if (level == null || level.isClientSide || !state.is(AllBlocks.STEAM_ENGINE.get()))
			return false;
		BlockPos pos = engine.getBlockPos();
		MetalSteamEngineBlock metal = engineFor(boilerUnder(level, pos, state));
		if (metal == null)
			return false;
		CompoundTag saved = engine.saveWithoutMetadata();
		level.setBlock(pos, metal.defaultBlockState()
			.setValue(SteamEngineBlock.FACE, state.getValue(SteamEngineBlock.FACE))
			.setValue(SteamEngineBlock.FACING, state.getValue(SteamEngineBlock.FACING))
			.setValue(BlockStateProperties.WATERLOGGED, state.getValue(BlockStateProperties.WATERLOGGED)), Block.UPDATE_ALL);
		BlockEntity replaced = level.getBlockEntity(pos);
		if (replaced != null)
			replaced.load(saved);
		return true;
	}

	/** Tells whoever tried to put {@code engine} against a tank it does not go on which tank it does go on. */
	public static void refuse(BlockPlaceContext context, Block engine) {
		Player player = context.getPlayer();
		Level level = context.getLevel();
		if (player == null || level.isClientSide)
			return;
		BlockState against = level.getBlockState(context.getClickedPos().relative(context.getClickedFace().getOpposite()));
		if (!(against.getBlock() instanceof FluidTankBlock))
			return;
		player.displayClientMessage(Component.translatable("mcremix.steam_engine.fits", engine.getName(),
			boilerFor(engine).getName()), true);
	}
}

package com.mcremix.fluids;

import com.mcremix.machines.CreateStandIn;
import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Create's steam engine in brass, iron or steel (Taran, 0.24.0): it runs as Create's copper one does, as fast and as
 * strong from each level of its boiler, but goes only on a tank of its own metal, as Create's goes only on Create's
 * copper tanks ({@link SteamEngines}). Create takes it for its own engine ({@link CreateStandIn}), so boilers count it
 * and Create's renderer draws its piston and linkage; its body is in its metal.
 */
public class MetalSteamEngineBlock extends SteamEngineBlock implements CreateStandIn {
	private final FluidPartMaterial material;

	public MetalSteamEngineBlock(Properties properties, FluidPartMaterial material) {
		super(properties);
		this.material = material;
	}

	public FluidPartMaterial material() {
		return material;
	}

	/** Whether it runs on {@code tank}: a tank of its own metal, and nothing else. */
	public boolean runsOn(BlockState tank) {
		return tank.getBlock() instanceof FluidTankBlock && FluidPartMaterial.of(tank) == material;
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return runsOn(level.getBlockState(pos.relative(getConnectedDirection(state).getOpposite())));
	}

	@Override
	public BlockEntityType<? extends SteamEngineBlockEntity> getBlockEntityType() {
		return ModBlockEntities.METAL_STEAM_ENGINE.get();
	}

	@Override
	public BlockEntry<? extends Block> createEntry() {
		return AllBlocks.STEAM_ENGINE;
	}
}

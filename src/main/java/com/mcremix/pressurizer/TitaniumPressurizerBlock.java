package com.mcremix.pressurizer;

import com.mcremix.fluids.Patina;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.machines.MachineBody;
import com.mcremix.registry.ModBlocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * The titanium Pressurizer (Taran, stage 4: steel now, titanium later): the same machine in titanium, so it takes up to
 * 2,048 RPM where the steel one stops at 512, and squeezes four times as fast at full speed. As a titanium machine it
 * warms as it turns and burns if nothing cools it, and keeps the temper colours heat gives it in its {@link Patina}
 * ({@link com.mcremix.kinetics.Tints}). Its vessel is titanium too, and takes the same colours.
 */
public class TitaniumPressurizerBlock extends PressurizerBlock implements MachineBody {
	public TitaniumPressurizerBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(Patina.PROPERTY, Patina.NONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(Patina.PROPERTY));
	}

	@Override
	public BlockState vessel(BlockState pressurizer) {
		return PressurizerVesselBlock.matching(ModBlocks.TITANIUM_PRESSURIZER_VESSEL.getDefaultState(), pressurizer);
	}

	@Override
	public KineticMaterial material() {
		return KineticMaterial.TITANIUM;
	}
}

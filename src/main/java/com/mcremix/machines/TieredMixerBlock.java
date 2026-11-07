package com.mcremix.machines;

import com.mcremix.fluids.Patina;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlock;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** A Mechanical Mixer in copper, iron, steel or aluminium: Create's, with its material and a patina. */
public class TieredMixerBlock extends MechanicalMixerBlock implements TieredMachine {
	private final KineticMaterial material;

	public TieredMixerBlock(Properties properties, KineticMaterial material) {
		super(properties);
		this.material = material;
	}

	@Override
	public MachineKind kind() {
		return MachineKind.MIXER;
	}

	@Override
	public KineticMaterial material() {
		return material;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(Patina.PROPERTY);
	}

	@Override
	public BlockEntityType<? extends MechanicalMixerBlockEntity> getBlockEntityType() {
		return ModBlockEntities.TIERED_MIXER.get();
	}
}

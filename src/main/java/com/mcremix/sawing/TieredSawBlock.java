package com.mcremix.sawing;

import com.mcremix.fluids.Patina;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.machines.MachineKind;
import com.mcremix.machines.TieredMachine;
import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * A Mechanical Saw in copper, iron, steel or aluminium: Create's, with its material and a patina. Its body sets its top
 * speed; the blade fitted to it decides what it cuts ({@link SawBlades}). Create's code takes it for its own saw
 * (placing beside any saw, arms using an upward one as a depot, contraptions): see BlockEntryMixin.
 */
public class TieredSawBlock extends SawBlock implements TieredMachine {
	private final KineticMaterial material;

	public TieredSawBlock(Properties properties, KineticMaterial material) {
		super(properties);
		this.material = material;
	}

	@Override
	public MachineKind kind() {
		return MachineKind.SAW;
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
	public BlockEntityType<? extends SawBlockEntity> getBlockEntityType() {
		return ModBlockEntities.TIERED_SAW.get();
	}
}

package com.mcremix.drilling;

import com.mcremix.fluids.Patina;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.machines.MachineKind;
import com.mcremix.machines.TieredMachine;
import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.drill.DrillBlock;
import com.simibubi.create.content.kinetics.drill.DrillBlockEntity;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * A Mechanical Drill in copper, iron, steel or aluminium: Create's, with its material and a patina. Its body sets its
 * top speed; the bits fitted to it decide what it cuts ({@link DrillBits}). Create's code takes it for its own drill
 * (placing beside any drill, contraptions leaving what is in front of it to be cut): see BlockEntryMixin.
 */
public class TieredDrillBlock extends DrillBlock implements TieredMachine {
	private final KineticMaterial material;

	public TieredDrillBlock(Properties properties, KineticMaterial material) {
		super(properties);
		this.material = material;
	}

	@Override
	public MachineKind kind() {
		return MachineKind.DRILL;
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
	public BlockEntityType<? extends DrillBlockEntity> getBlockEntityType() {
		return ModBlockEntities.TIERED_DRILL.get();
	}
}

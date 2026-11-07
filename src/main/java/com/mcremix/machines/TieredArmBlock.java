package com.mcremix.machines;

import com.mcremix.fluids.Patina;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlock;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** A Mechanical Arm in copper, iron, steel or aluminium: Create's, with its material and a patina. */
public class TieredArmBlock extends ArmBlock implements TieredMachine {
	private final KineticMaterial material;

	public TieredArmBlock(Properties properties, KineticMaterial material) {
		super(properties);
		this.material = material;
	}

	@Override
	public MachineKind kind() {
		return MachineKind.ARM;
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
	public BlockEntityType<? extends ArmBlockEntity> getBlockEntityType() {
		return ModBlockEntities.TIERED_ARM.get();
	}
}

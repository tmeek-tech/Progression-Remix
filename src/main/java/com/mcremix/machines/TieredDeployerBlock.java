package com.mcremix.machines;

import com.mcremix.fluids.Patina;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.deployer.DeployerBlock;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** A Deployer in copper, iron, steel or aluminium: Create's, with its material and a patina. */
public class TieredDeployerBlock extends DeployerBlock implements TieredMachine {
	private final KineticMaterial material;

	public TieredDeployerBlock(Properties properties, KineticMaterial material) {
		super(properties);
		this.material = material;
	}

	@Override
	public MachineKind kind() {
		return MachineKind.DEPLOYER;
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
	public BlockEntityType<? extends DeployerBlockEntity> getBlockEntityType() {
		return ModBlockEntities.TIERED_DEPLOYER.get();
	}
}

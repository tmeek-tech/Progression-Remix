package com.mcremix.fluids;

import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * A fluid tank in brass, iron, steel or aluminium. Each kind is its own block entity type, so tanks only join their own
 * kind: an iron tank never grows into a brass one or into Create's copper tanks. Brass and iron tanks are boilers as
 * Create's are, steel ones pressure boilers, and aluminium ones never boilers. Iron ones rust and, rusted through, leak
 * ({@link Rusting}, {@link Leaks}); steel ones rust too, but only to their first stage.
 */
public class RatedTankBlock extends FluidTankBlock implements FluidPartMaterial.Part {
	private final FluidPartMaterial material;

	protected RatedTankBlock(Properties properties, FluidPartMaterial material) {
		super(properties, false);
		this.material = material;
	}

	/** The tank for a material: iron and steel ones rust. */
	public static RatedTankBlock create(Properties properties, FluidPartMaterial material) {
		return material.rusts() ? new Rusting(properties, material) : new RatedTankBlock(properties, material);
	}

	@Override
	public FluidPartMaterial material() {
		return material;
	}

	@Override
	public BlockEntityType<? extends FluidTankBlockEntity> getBlockEntityType() {
		return ModBlockEntities.tank(material).get();
	}

	/** An iron or steel tank, every block of it rusting by its own {@link Patina} as its pipe does. */
	public static class Rusting extends RatedTankBlock {
		protected Rusting(Properties properties, FluidPartMaterial material) {
			super(properties, material);
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			super.createBlockStateDefinition(builder);
			builder.add(Patina.PROPERTY);
		}
	}
}

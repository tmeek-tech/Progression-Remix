package com.mcremix.fluids;

import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.config.CElectricity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.pump.ElectricPumpBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Power Grid's electric pump, going as fast as its metal allows. Power Grid runs its copper pump faster the more power
 * it takes, up to 256 RPM (its {@code electricPumpMaxSpeed}), and rates it for a fifth more power than that speed
 * needs, overheating on much more. A metal pump does the same up to its mechanical pump's top speed instead: 256 RPM
 * in iron, 512 in steel and aluminium, scaled with that setting. On any other block it is Power Grid's own: a Brass
 * Electric Pump placed before 0.26.0 is Power Grid's pump now, keeping this block entity.
 */
public class MetalElectricPumpBlockEntity extends ElectricPumpBlockEntity {
	/** The top speed Power Grid gives its copper pump by default, which its settings are reckoned against. */
	private static final float COPPER_TOP_SPEED = 256;

	public MetalElectricPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** The speed a pump of {@code material} goes at most, as its mechanical pump's top speed. */
	public static float topSpeed(FluidPartMaterial material) {
		return material.kinetic().maxSpeed;
	}

	private float topSpeed() {
		return getBlockState().getBlock() instanceof FluidPartMaterial.Part part ? topSpeed(part.material())
			: COPPER_TOP_SPEED;
	}

	@Override
	public int getSpeed() {
		CElectricity settings = ModdedConfigs.server().electricity;
		float cap = settings.electricPumpMaxSpeed.getF() * topSpeed() / COPPER_TOP_SPEED;
		return (int) Math.min(pumpElement.power() * settings.electricPumpPower.getF(), cap);
	}

	@Override
	public ThermalBehaviour specifyThermalBehaviour() {
		return ThermalBehaviour.forMaxPower(this, 1f,
			topSpeed() / ModdedConfigs.server().electricity.electricPumpPower.getF() * 1.2f);
	}
}

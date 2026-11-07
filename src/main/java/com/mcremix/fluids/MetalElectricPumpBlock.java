package com.mcremix.fluids;

import java.util.List;
import java.util.function.DoubleSupplier;

import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.config.ResistanceValues;
import org.patryk3211.powergrid.electricity.info.Resistance;
import org.patryk3211.powergrid.electricity.info.Voltage;
import org.patryk3211.powergrid.electricity.pump.ElectricPumpBlock;
import org.patryk3211.powergrid.electricity.pump.ElectricPumpBlockEntity;

import com.mcremix.registry.ModBlockEntities;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Power Grid's electric pump in iron, steel or aluminium (Taran, 0.24.0; the brass one went in 0.26.0). Power Grid's
 * own is the copper one; these pump as it does and draw as much, but go only as fast as their metal's mechanical pump
 * turns, 256 RPM in iron and 512 in steel and aluminium ({@link MetalElectricPumpBlockEntity}), and hold fluids as hot
 * as their metal's other parts do. An iron or steel one rusts as its pipes do ({@link Rusting}); an aluminium one melts beside anything
 * Hot. Like every pump, it joins pipes of any metal.
 */
public class MetalElectricPumpBlock extends ElectricPumpBlock implements FluidPartMaterial.Part {
	private final FluidPartMaterial material;

	protected MetalElectricPumpBlock(Properties properties, FluidPartMaterial material) {
		super(properties);
		this.material = material;
	}

	/** The electric pump for a material: iron and steel ones rust. */
	public static MetalElectricPumpBlock create(Properties properties, FluidPartMaterial material) {
		return material.rusts() ? new Rusting(properties, material) : new MetalElectricPumpBlock(properties, material);
	}

	@Override
	public FluidPartMaterial material() {
		return material;
	}

	@Override
	public BlockEntityType<? extends ElectricPumpBlockEntity> getBlockEntityType() {
		return ModBlockEntities.METAL_ELECTRIC_PUMP.get();
	}

	/** Power Grid's figures for its pump: its resistance, and the voltage that runs it at its top speed, its metal's. */
	@Override
	public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
		Resistance.series(resistance(), player, tooltip);
		float power = MetalElectricPumpBlockEntity.topSpeed(material)
			/ ModdedConfigs.server().electricity.electricPumpPower.getF();
		Voltage.max(Math.round(Math.sqrt(power * resistance())), player, tooltip);
	}

	/** The metal pumps take the resistance Power Grid's config gives its own. */
	public static void registerResistance() {
		ResistanceValues.register(new ResistanceValues.Provider() {
			@Override
			public DoubleSupplier get(Block block) {
				return block instanceof MetalElectricPumpBlock ? () -> ResistanceValues.get(ModdedBlocks.ELECTRIC_PUMP.get())
					: null;
			}

			@Override
			public DoubleSupplier get(Block block, String name) {
				return null;
			}
		});
	}

	/** An iron or steel electric pump, rusting by its {@link Patina} as its pipes do. */
	public static class Rusting extends MetalElectricPumpBlock {
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

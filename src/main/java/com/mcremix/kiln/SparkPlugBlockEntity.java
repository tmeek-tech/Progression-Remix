package com.mcremix.kiln;

import java.util.List;

import javax.annotation.Nullable;

import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.particles.SparkSoundOwner;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

import com.mcremix.client.SparkPlugEffects;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * A spark plug as a circuit: a gap between its + (the copper core, on its cap) and its - (the iron shell). Nothing
 * crosses it until about 1,000 volts are across it, so it wants a transformer stepping the voltage up (Taran,
 * 2026-10-22). Then it sparks, and goes on sparking as long as the voltage is there, lit furnace or not, drawing a few
 * milliamps: a switch or relay in its circuit decides when. While it sparks it lights the furnace it is screwed into as
 * soon as there is fuel in it to catch. It never wears out.
 */
public class SparkPlugBlockEntity extends ElectricBlockEntity implements IHaveGoggleInformation, SparkSoundOwner {
	/**
	 * Volts across it that make it spark: about 1,000, as a real plug needs, set a little under so that a 1,000 volt
	 * supply does it with what its wires lose on the way.
	 */
	public static final double SPARK_VOLTS = 900;
	/** Once it sparks it goes on until the voltage falls below this, so a voltage hovering at the mark does not flicker. */
	public static final double HOLD_VOLTS = 800;
	/** Ohms of the spark: at 1,000 volts, 10 milliamps, 10 watts. */
	static final float SPARK_RESISTANCE = 100_000;

	// set while the circuit is built, during construction, so it has no initial value here
	private SwitchedWire gap;
	private boolean sparking;
	private float volts, amps;
	private int syncIn;
	/** Client: whether it was sparking last tick, to start the sound of the spark when it begins. */
	private boolean wasSparking;

	public SparkPlugBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void buildCircuit(IElectricEntity.CircuitBuilder builder) {
		builder.setTerminalCount(2);
		gap = builder.connectSwitch(SPARK_RESISTANCE, builder.terminalNode(0), builder.terminalNode(1), false);
	}

	@Override
	public void electricalTick() {
		if (level == null || level.isClientSide)
			return;
		double across = Math.abs(gap.potentialDifference());
		boolean spark = across >= (sparking ? HOLD_VOLTS : SPARK_VOLTS);
		if (gap.getState() != spark)
			gap.setState(spark);
		applyPower(gap);
		float newAmps = spark ? (float) Math.abs(gap.current()) : 0;
		boolean changed = spark != sparking;
		sparking = spark;
		if (changed || (Math.abs(across - volts) > 1 || Math.abs(newAmps - amps) > 1e-4) && --syncIn <= 0) {
			syncIn = 10;
			volts = (float) across;
			amps = newAmps;
			notifyUpdate();
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || !sparking)
			return;
		if (!level.isClientSide) {
			KilnBlockEntity furnace = furnace();
			if (furnace != null && furnace.canLight())
				furnace.light();
		} else {
			boolean started = !wasSparking;
			wasSparking = true;
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SparkPlugEffects.tick(this, started));
		}
	}

	/** The furnace it is screwed into, if it still is. */
	@Nullable
	public KilnBlockEntity furnace() {
		if (level == null)
			return null;
		return level.getBlockEntity(worldPosition.relative(getBlockState().getValue(SparkPlugBlock.FACING)))
			instanceof KilnBlockEntity furnace ? furnace : null;
	}

	@Override
	public boolean isSparking() {
		return sparking && !isRemoved();
	}

	/** Volts across it, as last told to clients. */
	public float volts() {
		return volts;
	}

	/** Amps through it while it sparks. */
	public float amps() {
		return amps;
	}

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		if (clientPacket) {
			tag.putBoolean("Sparking", sparking);
			tag.putFloat("Volts", volts);
			tag.putFloat("Amps", amps);
		}
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		if (clientPacket) {
			sparking = tag.getBoolean("Sparking");
			if (!sparking)
				wasSparking = false;
			volts = tag.getFloat("Volts");
			amps = tag.getFloat("Amps");
		}
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		tooltip.add(line(Component.translatable("block.mcremix.spark_plug")));
		tooltip.add(line(Component.translatable("mcremix.goggles.spark_plug.volts", Math.round(volts))
			.withStyle(ChatFormatting.GRAY)));
		if (sparking)
			tooltip.add(line(Component.translatable("mcremix.goggles.spark_plug.sparking",
				String.format("%.1f", amps * 1000)).withStyle(ChatFormatting.GOLD)));
		else
			tooltip.add(line(Component.translatable("mcremix.goggles.spark_plug.too_low", Math.round(SPARK_VOLTS))
				.withStyle(ChatFormatting.DARK_GRAY)));
		KilnBlockEntity furnace = furnace();
		String state = furnace == null ? "none" : furnace.isLit() ? "lit" : "out";
		tooltip.add(line(Component.translatable("mcremix.goggles.spark_plug.furnace." + state)
			.withStyle(ChatFormatting.GRAY)));
		return true;
	}

	private static Component line(Component text) {
		return Component.literal("    ").append(text);
	}
}

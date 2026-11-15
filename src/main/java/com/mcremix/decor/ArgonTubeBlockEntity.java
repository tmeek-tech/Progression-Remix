package com.mcremix.decor;

import java.util.List;

import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An argon tube as a circuit: a gap of gas between its two electrodes. Nothing crosses it until about 800 volts are
 * across it, which a transformer stepping up the voltage gives; then the gas strikes and glows, and goes on glowing
 * down to 600 volts, drawing about twenty milliamps. Tubes in series need that much for each, as real sign tubes do.
 * It never wears out. Whether it is lit is saved, so one held lit below the strike stays lit across a reload.
 */
public class ArgonTubeBlockEntity extends ElectricBlockEntity implements IHaveGoggleInformation {
	/** Volts across it that strike the gas. */
	public static final double STRIKE_VOLTS = 800;
	/** Once lit it stays lit until the voltage falls below this, so a voltage at the mark does not flicker. */
	public static final double HOLD_VOLTS = 600;
	/** Ohms of the glowing gas: at 800 volts, 20 milliamps, 16 watts. */
	static final float GLOW_RESISTANCE = 40_000;
	/**
	 * Ticks a tube is left as it was saved, lit or dark, when its chunk loads: its first tick comes before it has
	 * rejoined its circuit, which then has to settle, and until then it reads no volts.
	 */
	private static final int SETTLE_TICKS = 5;

	// set while the circuit is built, during construction, so it has no initial value here
	private SwitchedWire gas;
	private boolean lit;
	private float volts, amps;
	private int syncIn, settling = SETTLE_TICKS;

	public ArgonTubeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void buildCircuit(IElectricEntity.CircuitBuilder builder) {
		builder.setTerminalCount(2);
		gas = builder.connectSwitch(GLOW_RESISTANCE, builder.terminalNode(0), builder.terminalNode(1), false);
	}

	@Override
	public void electricalTick() {
		if (level == null || level.isClientSide || electricBehaviour.isPaused())
			return;
		if (settling > 0) {
			settling--;
			if (gas.getState() != lit)
				gas.setState(lit);
			return;
		}
		double across = Math.abs(gas.potentialDifference());
		boolean glow = across >= (lit ? HOLD_VOLTS : STRIKE_VOLTS);
		if (gas.getState() != glow)
			gas.setState(glow);
		applyPower(gas);
		float newAmps = glow ? (float) Math.abs(gas.current()) : 0;
		boolean changed = glow != lit;
		lit = glow;
		// against the block's state rather than the last tick, so a tube saved lit goes dark when loaded with no power
		BlockState state = getBlockState();
		if (state.hasProperty(ArgonTubeBlock.LIT) && state.getValue(ArgonTubeBlock.LIT) != glow)
			level.setBlock(worldPosition, state.setValue(ArgonTubeBlock.LIT, glow), 3);
		if (changed || (Math.abs(across - volts) > 1 || Math.abs(newAmps - amps) > 1e-4) && --syncIn <= 0) {
			syncIn = 10;
			volts = (float) across;
			amps = newAmps;
			notifyUpdate();
		}
	}

	public boolean isLit() {
		return lit;
	}

	/** Amps through it while it glows, as last told to clients. */
	public float amps() {
		return amps;
	}

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		// saved too, so a tube held lit below the strike keeps glowing when its chunk loads again
		tag.putBoolean("Lit", lit);
		if (clientPacket) {
			tag.putFloat("Volts", volts);
			tag.putFloat("Amps", amps);
		}
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		lit = tag.getBoolean("Lit");
		if (clientPacket) {
			volts = tag.getFloat("Volts");
			amps = tag.getFloat("Amps");
		}
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		tooltip.add(line(Component.translatable("block.mcremix.argon_tube")));
		tooltip.add(line(Component.translatable("mcremix.goggles.argon_tube.volts", Math.round(volts))
			.withStyle(ChatFormatting.GRAY)));
		tooltip.add(line(lit ? Component.translatable("mcremix.goggles.argon_tube.lit", Math.round(amps * 1000))
			.withStyle(ChatFormatting.LIGHT_PURPLE)
			: Component.translatable("mcremix.goggles.argon_tube.too_low", Math.round(STRIKE_VOLTS))
				.withStyle(ChatFormatting.DARK_GRAY)));
		return true;
	}

	private static Component line(Component text) {
		return Component.literal("    ").append(text);
	}
}

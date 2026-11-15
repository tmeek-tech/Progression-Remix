package com.mcremix.decor;

import java.util.List;

import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An argon tube as a circuit: a gap of gas between its two electrodes. Nothing crosses it until about 800 volts are
 * across it, which a transformer stepping up the voltage gives; then the gas strikes and glows, and goes on glowing
 * down to 600 volts, drawing about twenty milliamps. Tubes in series need that much for each, as real sign tubes do.
 * It never wears out. Whether it is lit is saved, so one held lit below the strike stays lit across a reload: after a
 * load, and while its chunk or one beside it does not tick its block entities, it holds as it was, and then waits until
 * its supply is back in its circuit. A chunk's block entities tick only while its tickets make it block ticking, which
 * for a player is within their view distance and a chunk beyond, and while it is within their simulation distance and a
 * chunk beyond, so within the nearer of the two; and round a force-loaded chunk and the spawn, where both hold. So a
 * tube in a force-loaded chunk, or in the spawn chunks within nine chunks of the spawn's own, never holds, while one in
 * the spawn's outer ring, ten chunks out, holds as it was until a player comes near. Power Grid takes a chunk's devices
 * out of their circuits as its tickets stop it block ticking, so a supply in the tube's chunk or one beside it is in the
 * circuit whenever the tube is not holding. One two chunks away or more need not be: if it stops while the tube's
 * chunks still tick, as when a player walks off or the server restarts with the tube in the spawn chunks and its supply
 * outside them, the tube has no volts and goes dark, and one held below the strike then stays dark until its supply
 * reaches 800 volts. On a supply too weak to hold it, which sags below the hold as it strikes, it blinks.
 */
public class ArgonTubeBlockEntity extends ElectricBlockEntity implements IHaveGoggleInformation {
	/** Volts across it that strike the gas. */
	public static final double STRIKE_VOLTS = 800;
	/** Once lit it stays lit until the voltage falls below this, so a voltage at the mark does not flicker. */
	public static final double HOLD_VOLTS = 600;
	/** Ohms of the glowing gas: at 800 volts, 20 milliamps, 16 watts. */
	static final float GLOW_RESISTANCE = 40_000;
	/**
	 * Ticks a tube is left as it was saved, lit or dark, once its circuit has a supply again after its chunk loads: its
	 * first tick comes before it has rejoined its circuit, which then has to settle.
	 */
	private static final int SETTLE_TICKS = 5;
	/**
	 * The most ticks a tube waits, as it was saved, for a supply in its circuit after its chunk loads: a supply in a
	 * chunk that starts ticking later is out of the circuit until then, and the tube reads no volts.
	 */
	private static final int PATIENCE_TICKS = 600;
	/**
	 * Ticks a tube stays lit or dark after it changes, so one on a supply too weak to hold it, which sags below the hold
	 * as it strikes, blinks rather than flickering every tick.
	 */
	private static final int DWELL_TICKS = 10;

	// set while the circuit is built, during construction, so it has no initial value here
	private SwitchedWire gas;
	private boolean lit;
	private float volts, amps;
	private int syncIn, settling = SETTLE_TICKS, patience = PATIENCE_TICKS, dwell;

	public ArgonTubeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void buildCircuit(IElectricEntity.CircuitBuilder builder) {
		builder.setTerminalCount(2);
		gas = builder.connectSwitch(GLOW_RESISTANCE, builder.terminalNode(0), builder.terminalNode(1), false);
	}

	/** Back in its circuit after its chunk loads, or starts ticking again: it holds as it was until a supply is back. */
	@Override
	public void unpaused() {
		super.unpaused();
		settling = SETTLE_TICKS;
		patience = PATIENCE_TICKS;
	}

	/**
	 * Whether its chunk and each chunk beside it tick their block entities, by both the chunk's own tickets (block
	 * ticking, as Power Grid goes by) and the simulation distance ({@link ServerChunkCache#isPositionTicking}): a
	 * device's first tick puts it in its circuit, and Power Grid takes a chunk's devices out as its tickets stop it block
	 * ticking. The simulation distance alone would not do, as it can be more than the view distance.
	 */
	private boolean neighboursTick() {
		if (!(level instanceof ServerLevel server))
			return true;
		ServerChunkCache chunks = server.getChunkSource();
		int x = SectionPos.blockToSectionCoord(worldPosition.getX());
		int z = SectionPos.blockToSectionCoord(worldPosition.getZ());
		for (int dx = -1; dx <= 1; dx++)
			for (int dz = -1; dz <= 1; dz++)
				if (!chunks.isPositionTicking(ChunkPos.asLong(x + dx, z + dz)))
					return false;
		return true;
	}

	@Override
	public void electricalTick() {
		if (level == null || level.isClientSide || electricBehaviour.isPaused())
			return;
		// a supply beside it whose block entity does not tick may be out of the circuit, so it would read no volts: it
		// holds as it was, and waits for its supply once they tick
		if (!neighboursTick()) {
			settling = SETTLE_TICKS;
			patience = PATIENCE_TICKS;
		}
		if (settling > 0) {
			if (gas.getState() != lit)
				gas.setState(lit);
			// no volts at all: its supply is not back in the circuit yet
			if (gas.potentialDifference() != 0 || --patience <= 0)
				settling--;
			return;
		}
		double across = Math.abs(gas.potentialDifference());
		boolean glow = dwell > 0 ? lit : across >= (lit ? HOLD_VOLTS : STRIKE_VOLTS);
		if (dwell > 0)
			dwell--;
		if (gas.getState() != glow)
			gas.setState(glow);
		applyPower(gas);
		float newAmps = glow ? (float) Math.abs(gas.current()) : 0;
		boolean changed = glow != lit;
		if (changed)
			dwell = DWELL_TICKS;
		lit = glow;
		// against the block's state rather than the last tick, so a tube saved lit with no supply goes dark once it has
		// waited for one
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

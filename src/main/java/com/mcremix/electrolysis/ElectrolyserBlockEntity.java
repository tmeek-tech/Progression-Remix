package com.mcremix.electrolysis;

import java.util.List;

import javax.annotation.Nullable;

import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;
import org.patryk3211.powergrid.electricity.sim.special.PNJunctionWire;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

/**
 * An Electrolyser passes a current through what it is fixed to, the side of a basin or of a vat, from its anode
 * (the + side) to its cathode (the - side), and the current splits what is there ({@link ElectrolysisRecipe}). As a
 * circuit it is a real cell: next to nothing flows until about 4 volts are across it, + to -, and past that the current
 * climbs steeply, so a generator straight across one drives a huge current and cooks it. It wants a transformer
 * stepping the voltage down, or several Electrolysers in series. Wired the wrong way round, nothing flows. It only
 * closes its circuit while both electrodes are in and there is something to split, with room for what it makes.
 * <p>
 * Each batch takes a set charge, so the more current, the faster it splits. The anode burns away by the power through
 * it, whatever it splits: pushing more current through raises the voltage too, so a hard-driven Electrolyser costs more
 * anode for each batch than a gently driven one. The cathode burns away a tenth as fast. A burnt-out electrode is gone,
 * and the Electrolyser stops until a new one goes in, by hand, funnel or arm. Past its rating it overheats, as any
 * Power Grid device does.
 */
public class ElectrolyserBlockEntity extends ElectricBlockEntity implements IHaveGoggleInformation {
	/** Joules an anode passes before it has burnt away: about four aluminium ingots' worth. */
	public static final float ANODE_LIFE = 12_000;
	/** How much of the anode's wear the cathode takes. */
	public static final float CATHODE_SHARE = 0.1f;
	/**
	 * The cell's junction: its reverse saturation current in amps and its ideality factor, which put about an amp
	 * through it at 3.5 volts and a hundred at 4.5, and next to nothing below 3.
	 */
	static final double SATURATION = 1e-7, IDEALITY = 8.5;
	/** Ohms of the brine or melt between the electrodes, once current flows. */
	static final double CELL_RESISTANCE = 0.02;
	/** Ohms of the clamps the electrodes sit in. */
	static final float CONTACT_RESISTANCE = 0.005f;
	/** Watts it can take for good, about 100 amps; past that it heats up until it overheats. */
	public static final float RATED_POWER = 600;
	static final float THERMAL_MASS = 4;
	/** Less current than this counts as none. */
	static final double MIN_CURRENT = 0.05;

	public enum Status {
		WORKING, NO_ELECTRODES, NO_VESSEL, NOTHING_TO_SPLIT, NO_CURRENT
	}

	// set while the circuit is built, during construction, so they have no initial values here
	private SwitchedWire contact;
	private Cell cell;

	private ItemStack anode = ItemStack.EMPTY;
	private ItemStack cathode = ItemStack.EMPTY;
	private final LazyOptional<IItemHandler> electrodeSlots = LazyOptional.of(ElectrodeSlots::new);
	/** What the basin it is fixed to is splitting, and the charge towards its next batch. */
	@Nullable
	private ElectrolysisRecipe basinRecipe;
	private double basinCharge;
	private int recheck;
	private float amps, volts;
	private Status status = Status.NO_ELECTRODES;
	private int syncIn;

	public ElectrolyserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void buildCircuit(IElectricEntity.CircuitBuilder builder) {
		builder.setTerminalCount(2);
		FloatingNode inside = builder.addInternalNode();
		contact = builder.connectSwitch(CONTACT_RESISTANCE, builder.terminalNode(0), inside, false);
		cell = new Cell(inside, builder.terminalNode(1));
		builder.add(cell);
	}

	@Nullable
	@Override
	public ThermalBehaviour specifyThermalBehaviour() {
		return ThermalBehaviour.forMaxPower(this, THERMAL_MASS, RATED_POWER);
	}

	public ItemStack anode() {
		return anode;
	}

	public ItemStack cathode() {
		return cathode;
	}

	public float amps() {
		return amps;
	}

	/** Volts across it, + to -. */
	public float volts() {
		return volts;
	}

	public Status status() {
		return status;
	}

	@Override
	public void electricalTick() {
		if (level == null || level.isClientSide)
			return;
		Status was = status;
		boolean electrodes = !anode.isEmpty() && !cathode.isEmpty();
		boolean wanted = electrodes && wantsCurrent();
		contact.setState(wanted);
		applyPower(contact);
		applyPower(cell);
		double current = wanted ? cell.current() : 0;
		if (!Double.isFinite(current) || current < MIN_CURRENT)
			current = 0;
		double power = current == 0 ? 0 : Math.max(0, contact.power() + cell.power());
		amps = (float) current;
		OwnedFloatingNode plus = electricBehaviour.getTerminal(0), minus = electricBehaviour.getTerminal(1);
		volts = plus == null || minus == null ? 0 : (float) (plus.getVoltage() - minus.getVoltage());
		status = !electrodes ? Status.NO_ELECTRODES
			: vessel() == null ? Status.NO_VESSEL
			: !wanted ? Status.NOTHING_TO_SPLIT
			: current == 0 ? Status.NO_CURRENT
			: Status.WORKING;
		if (current > 0) {
			burn(power / 20);
			deliver(current / 20);
		}
		if (status != was || (status == Status.WORKING || volts != 0) && --syncIn <= 0) {
			syncIn = 10;
			sendData();
		}
	}

	/** The basin or vat it is fixed to the side of; null if neither. */
	@Nullable
	private Object vessel() {
		Direction facing = getBlockState().getValue(ElectrolyserBlock.FACING);
		if (!facing.getAxis().isHorizontal())
			return null;
		BlockEntity be = level.getBlockEntity(worldPosition.relative(facing));
		return be instanceof BasinBlockEntity basin ? basin : Vat.of(be);
	}

	private boolean wantsCurrent() {
		Object vessel = vessel();
		if (vessel instanceof BasinBlockEntity basin) {
			if (--recheck <= 0) {
				recheck = 10;
				ElectrolysisRecipe found = BasinElectrolysis.find(basin);
				if (found == null || basinRecipe == null || !found.getId().equals(basinRecipe.getId()))
					basinCharge = 0;
				basinRecipe = found;
			}
			return basinRecipe != null;
		}
		basinRecipe = null;
		return vessel instanceof Vat vat && vat.wantsCurrent();
	}

	/** This tick's charge, into the basin's next batch or the vat's. */
	private void deliver(double coulombs) {
		Object vessel = vessel();
		if (vessel instanceof Vat vat) {
			vat.addCharge(coulombs);
			return;
		}
		if (!(vessel instanceof BasinBlockEntity basin) || basinRecipe == null)
			return;
		basinCharge += coulombs;
		if (basinCharge < basinRecipe.charge())
			return;
		if (BasinElectrolysis.run(basin, basinRecipe))
			basinCharge -= basinRecipe.charge();
		recheck = 0;
	}

	/** The electrodes burn away by the power through them, the cathode a tenth as fast as the anode. */
	private void burn(double joules) {
		ItemStack anodeWas = anode, cathodeWas = cathode;
		anode = wear(anode, joules / ANODE_LIFE);
		cathode = wear(cathode, joules * CATHODE_SHARE / ANODE_LIFE);
		if (anode != anodeWas || cathode != cathodeWas)
			electrodesChanged();
		else
			setChanged();
	}

	/** The electrode a little more worn, or nothing once it has burnt away. */
	private ItemStack wear(ItemStack electrode, double by) {
		if (electrode.isEmpty())
			return electrode;
		float worn = CarbonElectrodeItem.wear(electrode) + (float) by;
		if (worn < 1) {
			CarbonElectrodeItem.setWear(electrode, worn);
			return electrode;
		}
		if (level instanceof ServerLevel server) {
			Vec3 at = Vec3.atCenterOf(worldPosition);
			server.playSound(null, worldPosition, SoundEvents.BASALT_BREAK, SoundSource.BLOCKS, 0.8f, 0.7f);
			server.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 6, 0.2, 0.2, 0.2, 0.01);
		}
		return ItemStack.EMPTY;
	}

	InteractionResult fit(Player player, ItemStack held) {
		if (!anode.isEmpty() && !cathode.isEmpty())
			return InteractionResult.PASS;
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		ItemStack one = held.copyWithCount(1);
		if (anode.isEmpty())
			anode = one;
		else
			cathode = one;
		if (!player.getAbilities().instabuild)
			held.shrink(1);
		level.playSound(null, worldPosition, SoundEvents.DEEPSLATE_PLACE, SoundSource.BLOCKS, 0.8f, 1.2f);
		electrodesChanged();
		return InteractionResult.SUCCESS;
	}

	InteractionResult takeOut(Player player) {
		boolean fromAnode = !anode.isEmpty();
		ItemStack out = fromAnode ? anode : cathode;
		if (out.isEmpty())
			return InteractionResult.PASS;
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		if (fromAnode)
			anode = ItemStack.EMPTY;
		else
			cathode = ItemStack.EMPTY;
		player.getInventory().placeItemBackInInventory(out);
		level.playSound(null, worldPosition, SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 0.8f, 1.2f);
		electrodesChanged();
		return InteractionResult.SUCCESS;
	}

	/** Shows which electrodes are in, and saves and sends them. */
	private void electrodesChanged() {
		BlockState state = getBlockState();
		BlockState shown = state.setValue(ElectrolyserBlock.ANODE, !anode.isEmpty())
			.setValue(ElectrolyserBlock.CATHODE, !cathode.isEmpty());
		if (shown != state)
			level.setBlock(worldPosition, shown, 3);
		setChanged();
		sendData();
	}

	@Override
	public void destroy() {
		super.destroy();
		Vec3 at = Vec3.atCenterOf(worldPosition);
		Containers.dropItemStack(level, at.x, at.y, at.z, anode);
		Containers.dropItemStack(level, at.x, at.y, at.z, cathode);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER)
			return electrodeSlots.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidate() {
		super.invalidate();
		electrodeSlots.invalidate();
	}

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		tag.put("Anode", anode.save(new CompoundTag()));
		tag.put("Cathode", cathode.save(new CompoundTag()));
		tag.putDouble("Charge", basinCharge);
		if (clientPacket) {
			tag.putFloat("Amps", amps);
			tag.putFloat("Volts", volts);
			tag.putInt("Status", status.ordinal());
		}
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		anode = ItemStack.of(tag.getCompound("Anode"));
		cathode = ItemStack.of(tag.getCompound("Cathode"));
		basinCharge = tag.getDouble("Charge");
		if (clientPacket) {
			amps = tag.getFloat("Amps");
			volts = tag.getFloat("Volts");
			status = Status.values()[Math.floorMod(tag.getInt("Status"), Status.values().length)];
		}
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		tooltip.add(line(Component.translatable("block.mcremix.electrolyser")));
		tooltip.add(line(electrodeLine("anode", anode)));
		tooltip.add(line(electrodeLine("cathode", cathode)));
		tooltip.add(line(Component.translatable("mcremix.goggles.electrolyser.reading", String.format("%.1f", volts),
			String.format("%.1f", amps)).withStyle(ChatFormatting.GRAY)));
		if (thermalBehaviour != null)
			tooltip.add(line(Component.translatable("mcremix.goggles.electrolyser.temperature",
				Math.round(thermalBehaviour.getTemperature())).withStyle(ChatFormatting.GRAY)));
		if (status != Status.WORKING)
			tooltip.add(line(Component.translatable("mcremix.goggles.electrolyser." + status.name().toLowerCase())
				.withStyle(ChatFormatting.DARK_GRAY)));
		return true;
	}

	private static Component line(Component text) {
		return Component.literal("    ").append(text);
	}

	private static Component electrodeLine(String side, ItemStack electrode) {
		if (electrode.isEmpty())
			return Component.translatable("mcremix.goggles.electrolyser.no_" + side).withStyle(ChatFormatting.RED);
		return Component.translatable("mcremix.goggles.electrolyser." + side,
			Math.round(100 * (1 - CarbonElectrodeItem.wear(electrode)))).withStyle(ChatFormatting.GRAY);
	}

	/**
	 * The cell as a circuit: Power Grid's p-n junction, with the step limit circuit simulators use (SPICE's) in place
	 * of its own. Its own holds a junction at its critical voltage whenever a step crosses it, and then two cells in
	 * series never settle: their current jumps between a fifth of an amp and tens of amps every few ticks.
	 */
	private static class Cell extends PNJunctionWire {
		Cell(IElectricNode plus, IElectricNode minus) {
			super(SATURATION, CELL_RESISTANCE, ThermalBehaviour.STANDARD_TEMPERATURE, IDEALITY, plus, minus);
		}

		@Override
		public double pnLim(double volts, double previous, double critical, double thermal) {
			double step = IDEALITY * thermal;
			if (volts <= critical || Math.abs(volts - previous) <= 2 * step)
				return volts;
			if (previous <= 0)
				return step * Math.log(volts / step);
			double arg = 1 + (volts - previous) / step;
			return arg > 0 ? previous + step * Math.log(arg) : critical;
		}
	}

	/** Funnels, chutes and arms can put Carbon Electrodes into an empty side, anode first; nothing comes out. */
	private class ElectrodeSlots implements IItemHandler {
		@Override
		public int getSlots() {
			return 2;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return slot == 0 ? anode : cathode;
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (!isItemValid(slot, stack) || !getStackInSlot(slot).isEmpty())
				return stack;
			if (!simulate) {
				if (slot == 0)
					anode = stack.copyWithCount(1);
				else
					cathode = stack.copyWithCount(1);
				electrodesChanged();
			}
			return stack.copyWithCount(stack.getCount() - 1);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			return 1;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return stack.getItem() instanceof CarbonElectrodeItem;
		}
	}
}

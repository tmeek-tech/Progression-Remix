package com.mcremix.arcfurnace;

import java.util.List;

import javax.annotation.Nullable;

import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;

import com.mcremix.electrolysis.CarbonElectrodeItem;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * An Arc Furnace strikes an arc from one Carbon Electrode down through what lies in its copper hearth and back up the
 * other, and melts it down ({@link ArcFurnaceRecipe}): titanium sponge into ingots. Molten titanium dissolves any other
 * crucible, so the hearth is copper, kept cool by water, and the titanium freezes against it into ingots.
 * <p>
 * As a circuit the arc is a heavy load, a quarter of an ohm, with no + or -, so 20 volts across it drive 80 amps,
 * 1.6 kW. It only closes its circuit while both electrodes are in and there is something to melt with room for what
 * it makes. Under {@link #MELTS_FROM} watts the arc is too weak to melt titanium and only warms the hearth. All its
 * power heats the hearth, which sheds only {@link #RATED_POWER} watts on its own and overheats, as any Power Grid device
 * does, unless a water cooler takes the rest ({@link com.mcremix.cooling.WaterCoolerBlockEntity}): one on water holds
 * it at about 2.5 kW, one on cold water about twice that.
 * <p>
 * Each melt takes a set energy, so the more power, the faster it melts. The electrodes burn away by the energy through
 * them, both alike, and the furnace stops when one is gone until a new one goes in, by hand, funnel or arm.
 */
public class ArcFurnaceBlockEntity extends ElectricBlockEntity implements IHaveGoggleInformation {
	/** Ohms of the arc with the clamps the electrodes sit in. */
	public static final float ARC_RESISTANCE = 0.25f;
	/** Watts under which the arc only warms the hearth: too weak to melt titanium. */
	public static final float MELTS_FROM = 400;
	/** Watts the hearth sheds on its own, with nothing cooling it. */
	public static final float RATED_POWER = 150;
	static final float THERMAL_MASS = 60;
	/** Joules each electrode passes before it has burnt away: ten sponges' worth. */
	public static final float ELECTRODE_LIFE = 160_000;
	/** How many it holds still to melt, and how many it has made before they must be taken out. */
	public static final int HOLDS = 16;
	/** Less current than this counts as none. */
	static final double MIN_CURRENT = 0.05;

	public enum Status {
		WORKING, NO_ELECTRODES, NOTHING_TO_MELT, FULL, NO_CURRENT, TOO_WEAK
	}

	// set while the circuit is built, during construction, so it has no initial value here
	private SwitchedWire arc;

	private ItemStack left = ItemStack.EMPTY;
	private ItemStack right = ItemStack.EMPTY;
	/** What is still to melt, and what it has made. */
	private ItemStack charge = ItemStack.EMPTY;
	private ItemStack made = ItemStack.EMPTY;
	/** How far through melting the next one it is, as a client is told. */
	private float shownProgress;
	private final LazyOptional<IItemHandler> slots = LazyOptional.of(Slots::new);
	@Nullable
	private ArcFurnaceRecipe recipe;
	/** Joules put into what is melting now. */
	private double energy;
	private float amps, volts, watts;
	private Status status = Status.NO_ELECTRODES;
	private int syncIn;

	public ArcFurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void buildCircuit(IElectricEntity.CircuitBuilder builder) {
		builder.setTerminalCount(2);
		arc = builder.connectSwitch(ARC_RESISTANCE, builder.terminalNode(0), builder.terminalNode(1), false);
	}

	@Nullable
	@Override
	public ThermalBehaviour specifyThermalBehaviour() {
		return ThermalBehaviour.forMaxPower(this, THERMAL_MASS, RATED_POWER);
	}

	public ItemStack left() {
		return left;
	}

	public ItemStack right() {
		return right;
	}

	public ItemStack charge() {
		return charge;
	}

	public ItemStack made() {
		return made;
	}

	public float amps() {
		return amps;
	}

	public float volts() {
		return volts;
	}

	public float watts() {
		return watts;
	}

	public Status status() {
		return status;
	}

	/** How far through melting the next one it is, from 0 to 1. */
	public float progress() {
		if (level != null && level.isClientSide)
			return shownProgress;
		return recipe == null ? 0 : (float) Math.min(1, energy / recipe.energy());
	}

	@Override
	public void electricalTick() {
		if (level == null || level.isClientSide)
			return;
		Status was = status;
		boolean electrodes = !left.isEmpty() && !right.isEmpty();
		ArcFurnaceRecipe found = ArcFurnaceRecipe.find(level, charge).orElse(null);
		// what it melts changed; not when it has only just loaded, which keeps how far it had got
		if (recipe != null && (found == null || !found.getId().equals(recipe.getId())))
			energy = 0;
		recipe = found;
		boolean room = recipe != null && fits(recipe.result());
		boolean wanted = electrodes && room;
		arc.setState(wanted);
		applyPower(arc);
		double current = wanted ? Math.abs(arc.current()) : 0;
		if (!Double.isFinite(current) || current < MIN_CURRENT)
			current = 0;
		double power = current == 0 ? 0 : Math.max(0, arc.power());
		amps = (float) current;
		watts = (float) power;
		OwnedFloatingNode a = electricBehaviour.getTerminal(0), b = electricBehaviour.getTerminal(1);
		volts = a == null || b == null ? 0 : (float) Math.abs(a.getVoltage() - b.getVoltage());
		status = !electrodes ? Status.NO_ELECTRODES
			: recipe == null ? Status.NOTHING_TO_MELT
			: !room ? Status.FULL
			: current == 0 ? Status.NO_CURRENT
			: power < MELTS_FROM ? Status.TOO_WEAK
			: Status.WORKING;
		if (current > 0)
			burn(power / 20);
		if (status == Status.WORKING) {
			energy += power / 20;
			if (energy >= recipe.energy()) {
				energy -= recipe.energy();
				melted();
			}
			effects();
		}
		showState();
		if (status != was || (status == Status.WORKING || volts != 0) && --syncIn <= 0) {
			syncIn = 10;
			sendData();
		}
	}

	/** One has melted down: it freezes against the cold copper into what it makes. */
	private void melted() {
		ItemStack result = recipe.result();
		charge.shrink(1);
		if (charge.isEmpty())
			charge = ItemStack.EMPTY;
		if (made.isEmpty())
			made = result;
		else
			made.grow(result.getCount());
		level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.4f);
		setChanged();
	}

	private boolean fits(ItemStack result) {
		if (made.isEmpty())
			return result.getCount() <= HOLDS;
		return ItemHandlerHelper.canItemStacksStack(made, result) && made.getCount() + result.getCount() <= HOLDS;
	}

	/** A white-hot glare and a spray of sparks under the electrodes, and the arc's roar. */
	private void effects() {
		if (!(level instanceof ServerLevel server) || level.getGameTime() % 4 != 0)
			return;
		Vec3 at = Vec3.atBottomCenterOf(worldPosition).add(0, 0.3, 0);
		server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 3, 0.25, 0.05, 0.25, 0.15);
		server.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 1, 0.2, 0.05, 0.2, 0.03);
		if (level.getGameTime() % 20 == 0)
			level.playSound(null, worldPosition, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.6f, 1.8f);
	}

	/** The electrodes burn away by the energy through them, both alike. */
	private void burn(double joules) {
		ItemStack leftWas = left, rightWas = right;
		left = wear(left, joules / ELECTRODE_LIFE);
		right = wear(right, joules / ELECTRODE_LIFE);
		if (left != leftWas || right != rightWas)
			changed();
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

	InteractionResult putIn(Player player, ItemStack held) {
		// one electrode a click, as much to melt as the hearth takes
		ItemStack offered = ArcFurnaceBlock.isElectrode(held) ? held.copyWithCount(1) : held.copy();
		Slots slots = new Slots();
		if (slots.insertItem(-1, offered, true).getCount() == offered.getCount())
			return InteractionResult.PASS;
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		ItemStack rest = slots.insertItem(-1, offered, false);
		if (!player.getAbilities().instabuild)
			held.shrink(offered.getCount() - rest.getCount());
		level.playSound(null, worldPosition, SoundEvents.DEEPSLATE_PLACE, SoundSource.BLOCKS, 0.8f, 1.2f);
		return InteractionResult.SUCCESS;
	}

	InteractionResult takeOut(Player player) {
		ItemStack out = !made.isEmpty() ? made : !charge.isEmpty() ? charge : !left.isEmpty() ? left : right;
		if (out.isEmpty())
			return InteractionResult.PASS;
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		if (out == made)
			made = ItemStack.EMPTY;
		else if (out == charge)
			charge = ItemStack.EMPTY;
		else if (out == left)
			left = ItemStack.EMPTY;
		else
			right = ItemStack.EMPTY;
		player.getInventory().placeItemBackInInventory(out);
		level.playSound(null, worldPosition, SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 0.8f, 1.2f);
		changed();
		return InteractionResult.SUCCESS;
	}

	/** Shows which electrodes are in, whether it holds something to melt and whether the arc burns. */
	private void showState() {
		BlockState state = getBlockState();
		BlockState shown = state.setValue(ArcFurnaceBlock.LEFT, !left.isEmpty())
			.setValue(ArcFurnaceBlock.RIGHT, !right.isEmpty())
			.setValue(ArcFurnaceBlock.LOADED, !charge.isEmpty())
			.setValue(ArcFurnaceBlock.LIT, status == Status.WORKING);
		if (shown != state)
			level.setBlock(worldPosition, shown, 3);
	}

	/** Shows what it holds now, and saves and sends it. */
	private void changed() {
		if (level != null && !level.isClientSide)
			showState();
		setChanged();
		sendData();
	}

	@Override
	public void destroy() {
		super.destroy();
		Vec3 at = Vec3.atCenterOf(worldPosition);
		for (ItemStack stack : new ItemStack[] { left, right, charge, made })
			Containers.dropItemStack(level, at.x, at.y, at.z, stack);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER)
			return slots.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidate() {
		super.invalidate();
		slots.invalidate();
	}

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		tag.put("Left", left.save(new CompoundTag()));
		tag.put("Right", right.save(new CompoundTag()));
		tag.put("Charge", charge.save(new CompoundTag()));
		tag.put("Made", made.save(new CompoundTag()));
		tag.putDouble("Energy", energy);
		if (clientPacket) {
			tag.putFloat("Amps", amps);
			tag.putFloat("Volts", volts);
			tag.putFloat("Watts", watts);
			tag.putFloat("Progress", progress());
			tag.putInt("Status", status.ordinal());
		}
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		left = ItemStack.of(tag.getCompound("Left"));
		right = ItemStack.of(tag.getCompound("Right"));
		charge = ItemStack.of(tag.getCompound("Charge"));
		made = ItemStack.of(tag.getCompound("Made"));
		energy = tag.getDouble("Energy");
		if (clientPacket) {
			amps = tag.getFloat("Amps");
			volts = tag.getFloat("Volts");
			watts = tag.getFloat("Watts");
			shownProgress = tag.getFloat("Progress");
			status = Status.values()[Math.floorMod(tag.getInt("Status"), Status.values().length)];
		}
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		tooltip.add(line(Component.translatable("block.mcremix.arc_furnace")));
		tooltip.add(line(electrodeLine(left)));
		tooltip.add(line(electrodeLine(right)));
		tooltip.add(line(Component.translatable("mcremix.goggles.arc_furnace.reading", String.format("%.1f", volts),
			String.format("%.1f", amps), Math.round(watts)).withStyle(ChatFormatting.GRAY)));
		if (thermalBehaviour != null)
			tooltip.add(line(Component.translatable("mcremix.goggles.arc_furnace.temperature",
				Math.round(thermalBehaviour.getTemperature())).withStyle(ChatFormatting.GRAY)));
		if (!charge.isEmpty())
			tooltip.add(line(Component.translatable("mcremix.goggles.arc_furnace.melting", charge.getCount(),
				charge.getHoverName(), Math.round(100 * progress())).withStyle(ChatFormatting.GRAY)));
		if (!made.isEmpty())
			tooltip.add(line(Component.translatable("mcremix.goggles.arc_furnace.made", made.getCount(),
				made.getHoverName()).withStyle(ChatFormatting.GRAY)));
		if (status != Status.WORKING)
			tooltip.add(line(Component.translatable("mcremix.goggles.arc_furnace." + status.name().toLowerCase(),
				Math.round(MELTS_FROM)).withStyle(ChatFormatting.DARK_GRAY)));
		return true;
	}

	private static Component line(Component text) {
		return Component.literal("    ").append(text);
	}

	private static Component electrodeLine(ItemStack electrode) {
		if (electrode.isEmpty())
			return Component.translatable("mcremix.goggles.arc_furnace.no_electrode").withStyle(ChatFormatting.RED);
		return Component.translatable("mcremix.goggles.arc_furnace.electrode",
			Math.round(100 * (1 - CarbonElectrodeItem.wear(electrode)))).withStyle(ChatFormatting.GRAY);
	}

	/**
	 * Funnels, chutes and arms put Carbon Electrodes into an empty holder, west first, and what it melts into the
	 * hearth; hoppers, funnels and arms take out only what it has made. The slots: the two holders, the hearth, what it
	 * has made. Inserting at slot -1 puts a stack wherever it goes.
	 */
	private class Slots implements IItemHandler {
		@Override
		public int getSlots() {
			return 4;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return switch (slot) {
				case 0 -> left;
				case 1 -> right;
				case 2 -> charge;
				case 3 -> made;
				default -> ItemStack.EMPTY;
			};
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (stack.isEmpty())
				return stack;
			if (slot < 0) {
				ItemStack rest = stack;
				for (int s = 0; s < 3 && !rest.isEmpty(); s++)
					rest = insertItem(s, rest, simulate);
				return rest;
			}
			if (!isItemValid(slot, stack))
				return stack;
			if (slot < 2) {
				if (!getStackInSlot(slot).isEmpty())
					return stack;
				if (!simulate) {
					if (slot == 0)
						left = stack.copyWithCount(1);
					else
						right = stack.copyWithCount(1);
					changed();
				}
				return stack.copyWithCount(stack.getCount() - 1);
			}
			if (!charge.isEmpty() && !ItemHandlerHelper.canItemStacksStack(charge, stack))
				return stack;
			int room = HOLDS - charge.getCount();
			int taken = Math.min(room, stack.getCount());
			if (taken <= 0)
				return stack;
			if (!simulate) {
				if (charge.isEmpty())
					charge = stack.copyWithCount(taken);
				else
					charge.grow(taken);
				changed();
			}
			return stack.copyWithCount(stack.getCount() - taken);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			if (slot != 3 || made.isEmpty() || amount <= 0)
				return ItemStack.EMPTY;
			ItemStack out = made.copyWithCount(Math.min(amount, made.getCount()));
			if (!simulate) {
				made.shrink(out.getCount());
				if (made.isEmpty())
					made = ItemStack.EMPTY;
				changed();
			}
			return out;
		}

		@Override
		public int getSlotLimit(int slot) {
			return slot < 2 ? 1 : HOLDS;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			if (slot < 2)
				return ArcFurnaceBlock.isElectrode(stack);
			return slot == 2 && !ArcFurnaceBlock.isElectrode(stack) && ArcFurnaceRecipe.find(level, stack).isPresent();
		}
	}
}

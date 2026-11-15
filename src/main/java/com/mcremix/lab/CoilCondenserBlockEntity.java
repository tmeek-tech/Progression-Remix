package com.mcremix.lab;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mcremix.cooling.CoolantTanks;
import com.mcremix.cooling.Coolants;
import com.mcremix.heat.HeatLevel;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * A coil condenser's water jacket (the distillation design: water in, warm water out). Every {@link #VAPOUR_PER_MB}
 * mB of vapour it turns back into liquid warms a mB of what it cools with, which comes out as a water cooler's does:
 * water and cold water as warm water, coolant as hot coolant ({@link Coolants}). Cold water condenses twice as fast as
 * water and coolant three times, as they take heat in a water cooler. With nothing to cool with, or what it gives back
 * nowhere to go, it condenses nothing, and what it serves waits.
 */
public class CoilCondenserBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
	/** mB of vapour a mB of water takes the heat of. */
	public static final int VAPOUR_PER_MB = 4;

	private final CoolantTanks tanks = new CoolantTanks(CoilCondenserBlockEntity::takes, this::changed);
	private final LazyOptional<IFluidHandler> pipes = LazyOptional.of(() -> tanks);
	/** Water warmed towards the next whole mB. */
	private float owed;
	/** Condensed something in the last second, for the goggles. */
	private long lastWorked = -100;
	private boolean syncQueued;

	public CoilCondenserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public static boolean takes(FluidStack stack) {
		return Coolants.cooling(stack) != null;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	public CoolantTanks tanks() {
		return tanks;
	}

	private void changed() {
		setChanged();
		syncQueued = true;
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;
		// lab glass: over heat, it cracks past Hot, and hot, if water meets it
		if (level.getGameTime() % LabGlass.CHECK_EVERY == 0) {
			HeatLevel heat = LabGlass.heatUnder(level, worldPosition);
			LabGlass.check(level, worldPosition, heat.isAtLeast(HeatLevel.WARM), LabGlass.tooHot(heat));
		}
		if (syncQueued && level.getGameTime() % 10 == 0) {
			syncQueued = false;
			sendData();
		}
	}

	/** What its jacket holds, to spill when it shatters. */
	public List<FluidStack> contents() {
		return List.of(tanks.feed.getFluid().copy(), tanks.output.getFluid().copy());
	}

	/** How much faster than plain water what it holds condenses: cold water twice, coolant three times; 0 if dry. */
	public float speed() {
		Coolants.Cooling with = Coolants.cooling(tanks.feed.getFluid());
		return with == null ? 0 : with.conductance() / Coolants.WATER.conductance();
	}

	/** How much of {@code want} mB of vapour it can condense now, with the water it has and room for what comes out. */
	public int condensable(int want) {
		Coolants.Cooling with = Coolants.cooling(tanks.feed.getFluid());
		if (with == null || want <= 0)
			return 0;
		int room = tanks.workable(with.spent().stack(1));
		return Math.max(0, Math.min(want, (int) ((room - owed) * VAPOUR_PER_MB + 1e-4)));
	}

	/** Condenses {@code vapour} mB, warming the water it takes; call {@link #condensable} first. */
	public void condense(int vapour) {
		Coolants.Cooling with = Coolants.cooling(tanks.feed.getFluid());
		if (with == null || vapour <= 0)
			return;
		owed += vapour / (float) VAPOUR_PER_MB;
		int whole = (int) owed;
		if (whole > 0) {
			tanks.turn(whole, with.spent().stack(1));
			owed -= whole;
		}
		if (level != null)
			lastWorked = level.getGameTime();
	}

	/** What it serves: the block it faces. */
	public Direction facing() {
		return getBlockState().getValue(CoilCondenserBlock.FACING);
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER && side != facing())
			return pipes.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		pipes.invalidate();
	}

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		tanks.write(tag);
		tag.putFloat("Owed", owed);
		tag.putLong("LastWorked", lastWorked);
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		tanks.read(tag);
		owed = tag.getFloat("Owed");
		lastWorked = tag.getLong("LastWorked");
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		tooltip.add(line(getBlockState().getBlock().getName()));
		String state;
		if (Coolants.cooling(tanks.feed.getFluid()) == null)
			state = "dry";
		else if (condensable(VAPOUR_PER_MB) == 0)
			state = "full";
		else if (level != null && level.getGameTime() - lastWorked < 20)
			state = "condensing";
		else
			state = "ready";
		tooltip.add(line(Component.translatable("mcremix.goggles.coil_condenser." + state)
			.withStyle(state.equals("condensing") || state.equals("ready") ? ChatFormatting.GREEN : ChatFormatting.GOLD)));
		tanks.addGoggleLines(tooltip);
		return true;
	}

	static Component line(Component text) {
		return Component.literal("    ").append(text);
	}
}

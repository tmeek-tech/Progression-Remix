package com.mcremix.cooling;

import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/**
 * The two tanks a water cooler or radiator works between: what pipes fill it with, and what it gives back for pipes to
 * draw off. Nothing else goes in or comes out, so one pipe can feed it and another drain it, or one do both.
 */
public class CoolantTanks implements IFluidHandler {
	public static final int CAPACITY = 1000;

	public final FluidTank feed;
	public final FluidTank output;

	public CoolantTanks(Predicate<FluidStack> takes, Runnable changed) {
		feed = new FluidTank(CAPACITY, takes) {
			@Override
			protected void onContentsChanged() {
				changed.run();
			}
		};
		output = new FluidTank(CAPACITY) {
			@Override
			protected void onContentsChanged() {
				changed.run();
			}
		};
	}

	/**
	 * How many millibuckets of the feed it can turn into {@code made} now: as much as it has, and has room for, which is
	 * none while the output still holds something else.
	 */
	public int workable(FluidStack made) {
		if (!output.isEmpty() && !output.getFluid().isFluidEqual(made))
			return 0;
		return Math.min(feed.getFluidAmount(), output.getSpace());
	}

	/** Turns {@code amount} of the feed into as much of {@code made}, or as much as the output has room for. */
	public void turn(int amount, FluidStack made) {
		int room = amount <= 0 ? 0 : output.fill(new FluidStack(made, amount), FluidAction.SIMULATE);
		if (room <= 0)
			return;
		feed.drain(room, FluidAction.EXECUTE);
		output.fill(new FluidStack(made, room), FluidAction.EXECUTE);
	}

	public void write(CompoundTag tag) {
		tag.put("Feed", feed.writeToNBT(new CompoundTag()));
		tag.put("Output", output.writeToNBT(new CompoundTag()));
	}

	public void read(CompoundTag tag) {
		feed.readFromNBT(tag.getCompound("Feed"));
		output.readFromNBT(tag.getCompound("Output"));
	}

	/** Goggle lines for what is in each tank. */
	public void addGoggleLines(List<Component> tooltip) {
		for (FluidTank tank : new FluidTank[] {feed, output})
			if (!tank.isEmpty())
				tooltip.add(Component.literal("    ").append(Component.translatable("mcremix.goggles.coolant",
					tank.getFluid().getDisplayName(), tank.getFluidAmount(), tank.getCapacity())
					.withStyle(ChatFormatting.GRAY)));
	}

	@Override
	public int getTanks() {
		return 2;
	}

	@Nonnull
	@Override
	public FluidStack getFluidInTank(int tank) {
		return (tank == 0 ? feed : output).getFluid();
	}

	@Override
	public int getTankCapacity(int tank) {
		return CAPACITY;
	}

	@Override
	public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
		return tank == 0 && feed.isFluidValid(stack);
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		return feed.fill(resource, action);
	}

	@Nonnull
	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		return output.drain(resource, action);
	}

	@Nonnull
	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		return output.drain(maxDrain, action);
	}
}

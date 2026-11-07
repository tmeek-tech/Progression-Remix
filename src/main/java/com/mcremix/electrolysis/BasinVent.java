package com.mcremix.electrolysis;

import java.util.ArrayList;
import java.util.List;

import com.mcremix.fluids.Chemical;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * A basin is open, so it cannot hold a gas. Gas piped into one goes off into the air over it ({@link Gases#escape}),
 * gathered and let go once a second, and any it finds in its own tanks, made there or kept from a world before basins
 * let it go, goes the same way. A recipe that takes a gas runs in a vat instead.
 */
public class BasinVent implements IFluidHandler {
	private final BasinBlockEntity basin;
	private final LazyOptional<IFluidHandler> tanks;
	private final List<FluidStack> rising = new ArrayList<>();

	public BasinVent(BasinBlockEntity basin, LazyOptional<IFluidHandler> tanks) {
		this.basin = basin;
		this.tanks = tanks;
	}

	/** The basin's own tanks, as Create offers them to pipes. */
	private IFluidHandler tanks() {
		return tanks.orElseThrow(IllegalStateException::new);
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		if (!Chemical.isGas(resource))
			return tanks().fill(resource, action);
		if (action.execute())
			rise(resource);
		return resource.getAmount();
	}

	private void rise(FluidStack gas) {
		if (gas.isEmpty())
			return;
		for (FluidStack going : rising)
			if (going.isFluidEqual(gas)) {
				going.grow(gas.getAmount());
				return;
			}
		rising.add(gas.copy());
	}

	/** Once a second, from the basin's tick: lets go of what came in since, and of any gas standing in its tanks. */
	public void tick() {
		if (!(basin.getLevel() instanceof ServerLevel level))
			return;
		for (SmartFluidTankBehaviour behaviour : basin.getTanks()) {
			IFluidHandler handler = behaviour.getCapability().orElse(null);
			if (handler == null)
				continue;
			for (int i = 0; i < handler.getTanks(); i++) {
				FluidStack held = handler.getFluidInTank(i);
				if (Chemical.isGas(held))
					rise(handler.drain(held.copy(), FluidAction.EXECUTE));
			}
		}
		if (rising.isEmpty())
			return;
		Gases.escape(level, Vec3.atBottomCenterOf(basin.getBlockPos().above()), rising);
		rising.clear();
	}

	@Override
	public int getTanks() {
		return tanks().getTanks();
	}

	@Override
	public FluidStack getFluidInTank(int tank) {
		return tanks().getFluidInTank(tank);
	}

	@Override
	public int getTankCapacity(int tank) {
		return tanks().getTankCapacity(tank);
	}

	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		return Chemical.isGas(stack) || tanks().isFluidValid(tank, stack);
	}

	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		return tanks().drain(resource, action);
	}

	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		return tanks().drain(maxDrain, action);
	}
}

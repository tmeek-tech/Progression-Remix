package com.mcremix.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.electrolysis.Vat;
import com.mcremix.electrolysis.VatHolder;
import com.mcremix.fluids.FluidHeat;
import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.FluidPartMaterial.Failure;
import com.mcremix.fluids.Leaks;
import com.mcremix.fluids.Overheating;
import com.mcremix.kinetics.AluminiumMelting;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * A tank holding a fluid too hot for it overheats as one, from its controller, and when it gives way every block of it
 * goes and what it held spills where it stood (see {@link Overheating}). An aluminium tank beside anything Hot melts the
 * same way. Every rusted-through block of an iron tank leaks, the controller losing what leaks, and on the client
 * drips it ({@link Leaks}).
 * <p>
 * And tanks standing on andesite scaffolding make a vat, kept by their controller as a boiler is ({@link Vat}): it
 * forms and breaks up as the controller checks now and then, works on the controller's tick, hands out its own fluids
 * and items from every tank of it, is saved and sent with the controller, and shows itself through goggles. On a client
 * its blocks are drawn again when the controller goes.
 */
@Mixin(value = FluidTankBlockEntity.class, remap = false)
public abstract class FluidTankBlockEntityMixin implements VatHolder {
	@Unique
	private int mcremix$overheat;
	@Unique
	private Vat mcremix$vat;
	@Unique
	private List<BlockPos> mcremix$leaking = List.of();
	@Unique
	private int mcremix$gasHeld;

	@Override
	public Vat mcremix$vat() {
		if (mcremix$vat == null)
			mcremix$vat = new Vat((FluidTankBlockEntity) (Object) this);
		return mcremix$vat;
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void mcremix$tick(CallbackInfo ci) {
		FluidTankBlockEntity self = (FluidTankBlockEntity) (Object) this;
		Level level = self.getLevel();
		if (level == null)
			return;
		BlockState state = self.getBlockState();
		if (level.isClientSide) {
			FluidTankBlockEntity controller = Leaks.leaks(state) ? self.getControllerBE() : null;
			if (controller != null)
				Leaks.drip(level, self.getBlockPos(), controller);
			return;
		}
		if (!self.isController() || self.isVirtual())
			return;
		mcremix$vat().tick();
		BlockPos pos = self.getBlockPos();
		mcremix$leak((ServerLevel) level, self, state);
		if (!Overheating.due(level, pos))
			return;
		boolean tooHot = FluidHeat.tooHot(state, self.getTankInventory().getFluid()) || mcremix$vat().tooHot(state)
			|| FluidPartMaterial.failureOf(state) == Failure.MELTS
				&& AluminiumMelting.besideHeat(level, pos, self.getWidth(), self.getHeight());
		ServerLevel server = (ServerLevel) level;
		mcremix$overheat = Overheating.check(server, pos, state, self.getWidth(), self.getHeight(), tooHot,
			mcremix$overheat, () -> Overheating.giveWay(server, self));
	}

	/** An iron tank's rusted-through blocks, looked over now and then, each losing its share of what it holds. */
	@Unique
	private void mcremix$leak(ServerLevel level, FluidTankBlockEntity self, BlockState state) {
		FluidPartMaterial material = FluidPartMaterial.of(state);
		if (material == null || !material.rusts() || !Leaks.losesFluid())
			return;
		BlockPos pos = self.getBlockPos();
		if ((level.getGameTime() + pos.asLong()) % Leaks.INTERVAL == 0)
			mcremix$leaking = Leaks.leaking(level, pos, self.getWidth(), self.getHeight());
		else if (!mcremix$leaking.isEmpty())
			// scraped, cleaned or waxed since it was looked over, a block stops at once
			mcremix$leaking.removeIf(spot -> !Leaks.leaks(level.getBlockState(spot)));
		if (mcremix$leaking.isEmpty())
			return;
		FluidStack lost = Leaks.drain(self, mcremix$leaking.size() * Leaks.PER_TICK);
		mcremix$gasHeld = Leaks.escape(level, mcremix$leaking, lost, mcremix$gasHeld);
	}

	@Inject(method = "lazyTick", at = @At("TAIL"))
	private void mcremix$formVat(CallbackInfo ci) {
		FluidTankBlockEntity self = (FluidTankBlockEntity) (Object) this;
		if (self.getLevel() != null && !self.getLevel().isClientSide && self.isController() && !self.isVirtual())
			mcremix$vat().evaluate();
	}

	@Inject(method = "removeController", at = @At("HEAD"))
	private void mcremix$breakUpVat(boolean keepFluids, CallbackInfo ci) {
		FluidTankBlockEntity self = (FluidTankBlockEntity) (Object) this;
		if (self.getLevel() != null && !self.getLevel().isClientSide && self.isController())
			mcremix$vat().breakUp();
	}

	// on a client, a vat's controller going takes its look with it, so its blocks and scaffolding are drawn again
	@Inject(method = "invalidate", at = @At("HEAD"))
	private void mcremix$forgetVat(CallbackInfo ci) {
		Level level = ((FluidTankBlockEntity) (Object) this).getLevel();
		if (mcremix$vat != null && level != null && level.isClientSide)
			mcremix$vat.forget();
	}

	@Inject(method = "handlerForCapability", at = @At("HEAD"), cancellable = true)
	private void mcremix$vatFluids(CallbackInfoReturnable<IFluidHandler> cir) {
		FluidTankBlockEntity self = (FluidTankBlockEntity) (Object) this;
		if (self.isController() && mcremix$vat().formed())
			cir.setReturnValue(mcremix$vat().fluidHandler());
	}

	@Inject(method = "getCapability", at = @At("HEAD"), cancellable = true)
	private <T> void mcremix$vatItems(Capability<T> cap, net.minecraft.core.Direction side,
		CallbackInfoReturnable<LazyOptional<T>> cir) {
		if (cap != ForgeCapabilities.ITEM_HANDLER)
			return;
		Vat vat = Vat.of((FluidTankBlockEntity) (Object) this);
		if (vat != null)
			cir.setReturnValue(vat.itemCapability().cast());
	}

	// at every return, not the tail: Create's tank stops early when it is not writing for or reading from a client, so
	// at the tail the vat was only ever synced, never saved, and lost all but the tank's own fluid when its chunk loaded
	@Inject(method = "write", at = @At("RETURN"))
	private void mcremix$writeVat(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
		if (((FluidTankBlockEntity) (Object) this).isController())
			compound.put("Vat", mcremix$vat().write());
	}

	@Inject(method = "read", at = @At("RETURN"))
	private void mcremix$readVat(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
		if (((FluidTankBlockEntity) (Object) this).isController())
			mcremix$vat().read(compound.getCompound("Vat"), clientPacket);
	}

	@Inject(method = "addToGoggleTooltip", at = @At("HEAD"), cancellable = true)
	private void mcremix$vatGoggles(List<Component> tooltip, boolean isPlayerSneaking,
		CallbackInfoReturnable<Boolean> cir) {
		FluidTankBlockEntity self = (FluidTankBlockEntity) (Object) this;
		Vat vat = Vat.of(self);
		if (vat == null)
			return;
		vat.addToGoggleTooltip(tooltip);
		self.containedFluidTooltip(tooltip, isPlayerSneaking, LazyOptional.of(vat::fluidHandler));
		cir.setReturnValue(true);
	}
}

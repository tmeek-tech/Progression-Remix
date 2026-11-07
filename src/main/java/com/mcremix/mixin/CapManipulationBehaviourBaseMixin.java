package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mcremix.electrolysis.Vat;
import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.CapManipulationBehaviourBase;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

/**
 * A funnel on a vat's scaffolding pulls out what the vat made (Taran, 0.28.0), as one on a basin does: the
 * scaffolding has no inventory of its own, so where a funnel finds none on it, it is given the vat's outlet
 * ({@link Vat#outletCapability}), which only gives out. The vat's outlet is piped to the funnel
 * ({@code com.mcremix.client.VatLook}).
 */
@Mixin(value = CapManipulationBehaviourBase.class, remap = false)
public abstract class CapManipulationBehaviourBaseMixin {
	@Shadow
	protected LazyOptional<?> targetCapability;

	@Shadow
	protected abstract Capability<?> capability();

	@Shadow
	public abstract BlockFace getTarget();

	@Shadow
	protected abstract void onHandlerInvalidated(LazyOptional<?> handler);

	/** The outlet it last listened to, so it listens once, though it looks again every few seconds. */
	@Unique
	private LazyOptional<?> mcremix$heard;

	@Inject(method = "findNewCapability", at = @At("RETURN"))
	private void mcremix$vatOutlet(CallbackInfo ci) {
		BlockEntityBehaviour self = (BlockEntityBehaviour) (Object) this;
		if (targetCapability.isPresent() || !(self.blockEntity instanceof FunnelBlockEntity)
			|| capability() != ForgeCapabilities.ITEM_HANDLER)
			return;
		Level level = self.getWorld();
		if (level == null || level.isClientSide)
			return;
		Vat vat = Vat.standingOn(level, getTarget().getOpposite().getPos());
		if (vat == null)
			return;
		LazyOptional<IItemHandler> outlet = vat.outletCapability();
		if (!outlet.isPresent())
			return;
		targetCapability = outlet;
		if (outlet != mcremix$heard)
			outlet.addListener(this::onHandlerInvalidated);
		mcremix$heard = outlet;
	}
}

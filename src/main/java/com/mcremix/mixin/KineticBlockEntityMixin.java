package com.mcremix.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.drilling.DrillBits;
import com.mcremix.kinetics.AluminiumMelting;
import com.mcremix.kinetics.Brackets;
import com.mcremix.kinetics.FrictionGoggles;
import com.mcremix.kinetics.FrictionPoints;
import com.mcremix.kinetics.KineticLimits;
import com.mcremix.kinetics.ShaftSupport;
import com.mcremix.kinetics.TieredKineticBlockEntity;
import com.mcremix.machines.Fittings;
import com.mcremix.machines.MachineBody;
import com.mcremix.sawing.SawBlades;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.drill.DrillBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lets every kinetic part grind or wobble and break when spun or loaded past its material's limits
 * ({@link KineticLimits}), clients seeing the wobble, and aluminium ones melt beside anything Hot
 * ({@link AluminiumMelting}); weathers the brackets on Create's shafts and cogs and counts their friction, as this mod's
 * parts do; gives a drill or saw the stress of the bits or blade fitted to it; and tells goggles what every part is made
 * of and what holds it ({@link FrictionGoggles}), which bits a drill has (and, when it is stuck, which bits would cut
 * the block in front of it) and which blade a saw has.
 */
@Mixin(value = KineticBlockEntity.class, remap = false)
public abstract class KineticBlockEntityMixin implements KineticLimits.Strained, ShaftSupport.Holdable {
	@Shadow
	protected float stress;
	@Shadow
	protected float lastStressApplied;

	@Unique
	private final KineticLimits.Strain mcremix$strain = new KineticLimits.Strain();
	@Unique
	private int mcremix$meltTicks;
	@Unique
	private boolean mcremix$held;

	@Override
	public boolean mcremix$wasHeld() {
		return mcremix$held;
	}

	@Override
	public void mcremix$setHeld(boolean held) {
		mcremix$held = held;
	}

	@Override
	public KineticLimits.Strain mcremix$strain() {
		return mcremix$strain;
	}

	@Override
	public float mcremix$networkStress() {
		return stress;
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void mcremix$checkLimits(CallbackInfo ci) {
		KineticBlockEntity self = (KineticBlockEntity) (Object) this;
		KineticLimits.tick(self, mcremix$strain, stress);
		mcremix$meltTicks = AluminiumMelting.tick(self, mcremix$meltTicks);
		Brackets.tick(self);
	}

	/**
	 * A drill or saw draws the stress of the bits or blade fitted to it, in place of its block's; a machine tier or vat
	 * form draws its material's friction and its weathering's on top, and a bracket on one of Create's shafts or cogs
	 * draws friction as one on this mod's does.
	 */
	@Inject(method = "calculateStressApplied", at = @At("RETURN"), cancellable = true)
	private void mcremix$fittedAndFriction(CallbackInfoReturnable<Float> cir) {
		KineticBlockEntity self = (KineticBlockEntity) (Object) this;
		if (self instanceof TieredKineticBlockEntity)
			return;
		BlockState state = self.getBlockState();
		float fitted = Fittings.stress(self);
		float friction = 0;
		if (state.getBlock() instanceof MachineBody)
			friction = FrictionPoints.friction(self);
		else if (self.getLevel() != null && FrictionPoints.isBare(state))
			friction = FrictionPoints.bracketFriction(self.getLevel(), self.getBlockPos(), state);
		if (fitted < 0 && friction <= 0)
			return;
		float impact = (fitted >= 0 ? fitted : cir.getReturnValueF()) + friction;
		lastStressApplied = impact;
		cir.setReturnValue(impact);
	}

	@Inject(method = "write", at = @At("TAIL"))
	private void mcremix$writeStrainAndHold(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
		if (clientPacket && mcremix$strain.wobbling)
			compound.putBoolean("McRemixWobble", true);
		if (mcremix$held)
			compound.putBoolean("McRemixHeld", true);
	}

	@Inject(method = "read", at = @At("TAIL"))
	private void mcremix$readStrainAndHold(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
		// sent to clients only, or set by a Ponder scene
		if (clientPacket || compound.contains("McRemixWobble"))
			mcremix$strain.wobbling = compound.getBoolean("McRemixWobble");
		mcremix$held = compound.getBoolean("McRemixHeld");
	}

	@Inject(method = "addToGoggleTooltip", at = @At("RETURN"), cancellable = true)
	private void mcremix$materialInfo(List<Component> tooltip, boolean isPlayerSneaking,
		CallbackInfoReturnable<Boolean> cir) {
		if (FrictionGoggles.add(tooltip, (KineticBlockEntity) (Object) this))
			cir.setReturnValue(true);
	}

	@Inject(method = "addToGoggleTooltip", at = @At("RETURN"), cancellable = true)
	private void mcremix$drillBits(List<Component> tooltip, boolean isPlayerSneaking,
		CallbackInfoReturnable<Boolean> cir) {
		if (!((Object) this instanceof DrillBlockEntity drill))
			return;
		DrillBits bits = DrillBits.of(drill);
		if (bits == null)
			return;
		bits.addGoggleInfo(tooltip);
		cir.setReturnValue(true);
	}

	@Inject(method = "addToGoggleTooltip", at = @At("RETURN"), cancellable = true)
	private void mcremix$sawBlade(List<Component> tooltip, boolean isPlayerSneaking,
		CallbackInfoReturnable<Boolean> cir) {
		if (!((Object) this instanceof SawBlockEntity saw))
			return;
		SawBlades blades = SawBlades.of(saw);
		if (blades == null)
			return;
		blades.addGoggleInfo(tooltip);
		cir.setReturnValue(true);
	}

	@Inject(method = "addToTooltip", at = @At("RETURN"), cancellable = true)
	private void mcremix$stuckDrill(List<Component> tooltip, boolean isPlayerSneaking,
		CallbackInfoReturnable<Boolean> cir) {
		if (!((Object) this instanceof DrillBlockEntity drill) || drill.getLevel() == null)
			return;
		DrillBits bits = DrillBits.of(drill);
		if (bits != null && bits.addStuckInfo(tooltip, drill.getLevel(), drill.getBlockPos(), drill.getBlockState()))
			cir.setReturnValue(true);
	}
}

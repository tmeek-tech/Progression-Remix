package com.mcremix.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.machines.Fittings;
import com.mcremix.sawing.FittedBlade;
import com.mcremix.sawing.SawBlades;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.phys.Vec3;

/**
 * Every saw keeps the blade fitted to it, iron until another is fitted, and cuts at that blade's speed. A gold blade does
 * not do stonecutting: of the recipes Create's saw finds for the item on it, the blade keeps those it can use, and when
 * that leaves none the item skates across the blade uncut for half a second at any speed, squealing and throwing sparks,
 * then moves on. The blade is saved and synced with the rest of the saw ({@code BlockBreakingKineticBlockEntityMixin}).
 */
@Mixin(value = SawBlockEntity.class, remap = false)
public abstract class SawBlockEntityMixin implements FittedBlade {
	/** How long an item skates across a blade too soft for it, in ticks. */
	@Unique
	private static final int SKATE_TICKS = 10;

	@Shadow
	public ProcessingInventory inventory;
	@Shadow
	private ItemStack playEvent;

	@Shadow
	protected abstract boolean canProcess();

	@Shadow
	public abstract Vec3 getItemMovementVec();

	@Unique
	private SawBlades mcremix$blades = SawBlades.IRON;
	@Unique
	private boolean mcremix$skating;

	@Override
	public SawBlades mcremix$blades() {
		return mcremix$blades;
	}

	@Override
	public void mcremix$setBlades(SawBlades blades) {
		mcremix$blades = blades;
	}

	@Override
	public boolean mcremix$isSkating() {
		return mcremix$skating;
	}

	@Unique
	private SawBlockEntity mcremix$self() {
		return (SawBlockEntity) (Object) this;
	}

	/** Whether this saw's work is done here: on the server, or in a Ponder scene. */
	@Unique
	private boolean mcremix$logical() {
		SawBlockEntity self = mcremix$self();
		return self.getLevel() != null && (!self.getLevel().isClientSide || self.isVirtual());
	}

	@Inject(method = "getRecipes", at = @At("RETURN"), cancellable = true)
	private void mcremix$bladeHardness(CallbackInfoReturnable<List<? extends Recipe<?>>> cir) {
		SawBlades blades = SawBlades.of(mcremix$self());
		List<? extends Recipe<?>> recipes = cir.getReturnValue();
		if (blades == null || blades.cutsStone || recipes.isEmpty())
			return;
		List<Recipe<?>> usable = new ArrayList<>();
		for (Recipe<?> recipe : recipes)
			if (blades.canUse(recipe))
				usable.add(recipe);
		if (usable.size() == recipes.size())
			return;
		cir.setReturnValue(usable);
		if (usable.isEmpty())
			mcremix$skate();
	}

	/** The item on the blade is too hard for it; called while the saw looks for recipes. */
	@Unique
	private void mcremix$skate() {
		if (mcremix$skating)
			return;
		mcremix$skating = true;
		SawBlockEntity self = mcremix$self();
		if (self.getLevel() != null && !self.getLevel().isClientSide)
			self.getLevel().playSound(null, self.getBlockPos(), SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, .5f,
				1.5f + self.getLevel().random.nextFloat() * .3f);
	}

	/** Nothing was cut when an item skated across: no chips or cutting sound as it leaves the blade. */
	@Inject(method = "applyRecipe", at = @At("HEAD"))
	private void mcremix$skated(CallbackInfo ci) {
		if (mcremix$skating)
			playEvent = ItemStack.EMPTY;
	}

	@Inject(method = "start", at = @At("HEAD"))
	private void mcremix$startFresh(ItemStack inserted, CallbackInfo ci) {
		if (mcremix$logical())
			mcremix$skating = false;
	}

	/** A new item on the blade: the cut takes Create's time divided by the blade's speed. */
	@Inject(method = "start", at = @At("RETURN"))
	private void mcremix$bladeSpeed(ItemStack inserted, CallbackInfo ci) {
		SawBlades blades = SawBlades.of(mcremix$self());
		if (blades == null || !mcremix$logical() || inventory.isEmpty() || !canProcess())
			return;
		if (mcremix$skating) {
			// Create passes an item it cannot cut in a few ticks, or one at high speed; hold it long enough to see
			float perTick = Mth.clamp(Math.abs(mcremix$self().getSpeed()) / 24, 1, 128);
			inventory.remainingTime = inventory.recipeDuration = 5 + SKATE_TICKS * perTick;
			return;
		}
		inventory.remainingTime /= blades.speed;
		inventory.recipeDuration = inventory.remainingTime;
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void mcremix$skatingParticles(CallbackInfo ci) {
		SawBlockEntity self = mcremix$self();
		if (self.getLevel() != null && self.getLevel().isClientSide && mcremix$skating && self.getSpeed() != 0
			&& !inventory.isEmpty() && !inventory.appliedRecipe)
			mcremix$spawnSkatingParticles(inventory.getStackInSlot(0));
	}

	/** Sparks and grit off the top of the blade, thrown the way the item travels. */
	@Unique
	private void mcremix$spawnSkatingParticles(ItemStack stack) {
		SawBlockEntity self = mcremix$self();
		RandomSource r = self.getLevel().random;
		Vec3 top = VecHelper.getCenterOf(self.getBlockPos()).add(0, .55, 0);
		Vec3 along = getItemMovementVec();
		double x = top.x + (r.nextDouble() - .5) * .25, z = top.z + (r.nextDouble() - .5) * .25;
		self.getLevel().addParticle(ParticleTypes.ELECTRIC_SPARK, x, top.y, z, along.x * .3 + (r.nextDouble() - .5) * .2,
			.15 + r.nextDouble() * .2, along.z * .3 + (r.nextDouble() - .5) * .2);
		if (r.nextInt(3) == 0)
			self.getLevel().addParticle(ParticleTypes.CRIT, x, top.y, z, along.x * .4, .2, along.z * .4);
		if (!stack.isEmpty() && r.nextInt(3) == 0)
			self.getLevel().addParticle(new ItemParticleOption(ParticleTypes.ITEM, stack), x, top.y, z, along.x * .1,
				.1, along.z * .1);
	}

	@Inject(method = "write", at = @At("TAIL"))
	private void mcremix$writeBlade(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
		if (mcremix$blades != SawBlades.IRON)
			compound.putString(SawBlades.TAG, mcremix$blades.id());
		if (mcremix$skating)
			compound.putBoolean("McRemixSkating", true);
	}

	@Inject(method = "read", at = @At("TAIL"))
	private void mcremix$readBlade(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
		SawBlades before = mcremix$blades;
		mcremix$blades = SawBlades.of(compound);
		mcremix$skating = compound.getBoolean("McRemixSkating");
		if (clientPacket && before != mcremix$blades)
			Fittings.refitted(mcremix$self());
	}
}

package com.mcremix.electrolysis;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * While its cog turns fast enough for its mixer, a mixer on a vat's side stirs the vat, if it fits that vat: each
 * stir mixes up to {@link #BATCHES} batches of what the vat holds, whatever the mixer's tier. A stir takes as long as
 * Create's Mechanical Mixer takes over a basin at the same speed, less its head going up and down, so a faster (and
 * so better) mixer is quicker, and a big vat wants more than one.
 */
public class VatMixerBlockEntity extends KineticBlockEntity {
	public static final int BATCHES = 4;

	/** What it is doing, for the goggles. */
	public enum Doing {
		STILL(ChatFormatting.DARK_GRAY),
		TOO_SLOW(ChatFormatting.GOLD),
		NO_VAT(ChatFormatting.GRAY),
		OTHER_MATERIAL(ChatFormatting.GOLD),
		IDLE(ChatFormatting.GRAY),
		MIXING(ChatFormatting.GREEN);

		final ChatFormatting color;

		Doing(ChatFormatting color) {
			this.color = color;
		}
	}

	private Doing doing = Doing.STILL;
	/** Ticks into this stir, and what it is stirring. */
	private int stirred;
	@Nullable
	private ResourceLocation stirring;

	public VatMixerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** Ticks a stir takes: Create's mixer's time at that speed for that recipe, and a second. */
	public static int stirTicks(BasinRecipe recipe, float speed) {
		int duration = recipe.getProcessingDuration();
		float recipeSpeed = duration == 0 ? 1 : duration / 100f;
		int rpm = Math.max(1, (int) Math.abs(speed));
		return 20 + Mth.clamp(Mth.log2(Math.max(1, 512 / rpm)) * Mth.ceil(recipeSpeed * 15) + 1, 1, 512);
	}

	/** The vat it is fixed to, whatever it is made of; null if none. */
	@Nullable
	private Vat vat() {
		return level == null ? null
			: Vat.at(level, worldPosition.relative(getBlockState().getValue(VatMixerBlock.HORIZONTAL_FACING)));
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;
		Vat vat = vat();
		BasinRecipe recipe = null;
		Doing now;
		if (getSpeed() == 0)
			now = Doing.STILL;
		else if (!isSpeedRequirementFulfilled())
			now = Doing.TOO_SLOW;
		else if (vat == null)
			now = Doing.NO_VAT;
		else if (!((VatMixerBlock) getBlockState().getBlock()).stirs(vat.tank().getBlockState()))
			now = Doing.OTHER_MATERIAL;
		else {
			recipe = vat.stir();
			now = recipe == null ? Doing.IDLE : Doing.MIXING;
		}
		if (recipe == null || !recipe.getId().equals(stirring))
			stirred = 0;
		stirring = recipe == null ? null : recipe.getId();
		if (now != doing) {
			doing = now;
			sendData();
		}
		if (recipe == null || ++stirred < stirTicks(recipe, getSpeed()))
			return;
		stirred = 0;
		vat.mix(BATCHES);
		AllSoundEvents.MIXING.playOnServer(level, worldPosition, .5f, 1);
	}

	public Doing doing() {
		return doing;
	}

	@Override
	protected void write(CompoundTag compound, boolean clientPacket) {
		super.write(compound, clientPacket);
		compound.putString("Doing", doing.name());
		compound.putInt("Stirred", stirred);
	}

	@Override
	protected void read(CompoundTag compound, boolean clientPacket) {
		super.read(compound, clientPacket);
		try {
			doing = Doing.valueOf(compound.getString("Doing"));
		} catch (IllegalArgumentException e) {
			doing = Doing.STILL;
		}
		stirred = compound.getInt("Stirred");
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()));
		tooltip.add(Component.literal("    ").append(Component.translatable(
			"mcremix.goggles.vat_mixer." + doing.name().toLowerCase(Locale.ROOT)).withStyle(doing.color)));
		return true;
	}
}

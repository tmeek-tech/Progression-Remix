package com.mcremix.bellows;

import java.util.List;

import javax.annotation.Nullable;

import com.mcremix.client.BellowsEffects;
import com.mcremix.heat.BellowsTarget;
import com.mcremix.heat.HeatLevel;
import com.mcremix.heat.HeatSources;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * While it turns, the bellows blows into the block in front of it, at any speed:
 * <ul>
 * <li>a kiln there, or a kiln standing on a fire there, burns one heat level hotter (never past Searing), and
 * fuel that is being fanned burns twice as fast;</li>
 * <li>a lit campfire there cooks twice as fast.</li>
 * </ul>
 * One stroke of the lid per turn of the shaft; faster shafts only cost more stress.
 */
public class BellowsBlockEntity extends KineticBlockEntity {
	/** How far the lid travels, in blocks. The crank pin sits half this far from the shaft. */
	public static final float STROKE = 5 / 16f;
	/** Bottom and resting height of the leather bag, in blocks. */
	public static final float BAG_BOTTOM = 2 / 16f;
	public static final float BAG_HEIGHT = 11 / 16f;

	/** Client only: when the last puff was heard, so a fast bellows does not drone. */
	public long lastPuffSound;

	public BellowsBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** How far down the lid is, 0 (open) to 1 (squeezed shut), for a crank angle in radians. */
	public static float compression(float crankAngle) {
		return (1 - Mth.cos(crankAngle)) / 2;
	}

	public Direction getFacing() {
		return getBlockState().getValue(BellowsBlock.HORIZONTAL_FACING);
	}

	public BlockPos getNozzleTarget() {
		return worldPosition.relative(getFacing());
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || getSpeed() == 0)
			return;
		if (level.isClientSide) {
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BellowsEffects.tick(this));
			return;
		}

		BlockPos front = getNozzleTarget();
		BlockState frontState = level.getBlockState(front);
		BellowsTarget device = deviceFed();
		if (device != null && device.takesAir())
			device.blow(level.getGameTime());
		if (frontState.getBlock() instanceof CampfireBlock && frontState.getValue(CampfireBlock.LIT)
			&& level.getBlockEntity(front) instanceof CampfireBlockEntity campfire)
			// an extra cooking step every tick: twice as fast
			CampfireBlockEntity.cookTick(level, front, frontState, campfire);
	}

	/** The heated device this bellows feeds: one right in front of it, or one standing on a fire in front of it. */
	@Nullable
	public BellowsTarget deviceFed() {
		if (level == null)
			return null;
		BlockPos front = getNozzleTarget();
		if (level.getBlockEntity(front) instanceof BellowsTarget device)
			return device;
		if (HeatSources.fireAt(level, front) != HeatLevel.COLD
			&& level.getBlockEntity(front.above()) instanceof BellowsTarget device)
			return device;
		return null;
	}

	/** What the bellows is doing, worded for goggles. Works on either side from what is in the world. */
	public MutableComponent describeEffect() {
		if (getSpeed() == 0)
			return line("still", ChatFormatting.DARK_GRAY);
		BlockPos front = getNozzleTarget();
		BlockState frontState = level.getBlockState(front);
		BellowsTarget device = deviceFed();
		if (device != null) {
			if (!device.takesAir())
				return line("sealed", ChatFormatting.GRAY);
			if (device.isBoostedByBellows())
				return line("fanning", ChatFormatting.GOLD);
			if (device.getHeat() == HeatLevel.COLD)
				return line("no_fire", ChatFormatting.GRAY);
			if (device.getHeat().isAtLeast(HeatLevel.SEARING))
				return line("max_heat", ChatFormatting.GRAY);
			// warm but not fanned: its heat comes from an electric heater underneath, and air only cools those
			return line("no_fire", ChatFormatting.GRAY);
		}
		if (frontState.getBlock() instanceof CampfireBlock)
			return frontState.getValue(CampfireBlock.LIT) ? line("campfire", ChatFormatting.GOLD)
				: line("no_fire", ChatFormatting.GRAY);
		if (HeatSources.fireAt(level, front) != HeatLevel.COLD)
			return line("nothing_above", ChatFormatting.GRAY);
		if (HeatSources.heatAt(level, front) != HeatLevel.COLD)
			return line("no_fire", ChatFormatting.GRAY); // an electric heater: air only cools it
		return line("nothing", ChatFormatting.DARK_GRAY);
	}

	private static MutableComponent line(String key, ChatFormatting color) {
		return Component.translatable("mcremix.goggles.bellows." + key).withStyle(color);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		tooltip.add(Component.literal("    ").append(Component.translatable("block.mcremix.bellows")));
		tooltip.add(Component.literal("    ").append(describeEffect()));
		return true;
	}
}

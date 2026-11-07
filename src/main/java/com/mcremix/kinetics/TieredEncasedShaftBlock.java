package com.mcremix.kinetics;

import java.util.List;

import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedShaftBlock;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A shaft of any material in a casing of any material: Create's encased shaft, except that it keeps the shaft that went
 * in ({@link EncasedPartBlockEntity}) and gives that back when wrenched open or broken. As on Create's, the casing is
 * not used up.
 */
public class TieredEncasedShaftBlock extends EncasedShaftBlock implements TieredEncased {
	private final EncasingCasing casing;
	private final WeatherState tint;

	public TieredEncasedShaftBlock(Properties properties, EncasingCasing casing, WeatherState tint) {
		super(properties, casing::casing);
		this.casing = casing;
		this.tint = tint;
	}

	@Override
	public EncasingCasing casing() {
		return casing;
	}

	@Override
	public KineticPart part() {
		return KineticPart.SHAFT;
	}

	@Override
	public WeatherState tint() {
		return tint;
	}

	@Override
	public void handleEncasing(BlockState state, Level level, BlockPos pos, ItemStack heldItem, Player player,
		InteractionHand hand, BlockHitResult ray) {
		super.handleEncasing(state, level, pos, heldItem, player, hand, ray);
		if (level.getBlockEntity(pos) instanceof EncasedPartBlockEntity encased)
			encased.setInner(state.getBlock());
	}

	@Override
	public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		BlockPos pos = context.getClickedPos();
		level.levelEvent(2001, pos, getId(state));
		KineticBlockEntity.switchToBlockState(level, pos, EncasedPartBlockEntity.innerPart(level, pos, state));
		return InteractionResult.SUCCESS;
	}

	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos,
		Player player) {
		if (target instanceof BlockHitResult hit && hit.getDirection().getAxis() != getRotationAxis(state))
			return getCasing().asItem().getDefaultInstance();
		return new ItemStack(EncasedPartBlockEntity.innerPart(level, pos, state).getBlock());
	}

	@Override
	@SuppressWarnings("deprecation")
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		return EncasedParts.drops(state, params);
	}

	@Override
	public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
		return EncasedParts.requiredItems(state, be);
	}

	@Override
	public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
		return ModBlockEntities.ENCASED_PART.get();
	}
}

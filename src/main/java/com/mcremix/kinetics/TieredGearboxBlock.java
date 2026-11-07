package com.mcremix.kinetics;

import java.util.List;

import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.gearbox.GearboxBlock;
import com.simibubi.create.content.kinetics.gearbox.GearboxBlockEntity;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;

/**
 * A gearbox in one of the shaft materials: it relays and mirrors rotation exactly like Create's, with its material's
 * top speed, load limit and weathering. There is no separate vertical item; see {@link #getStateForPlacement}.
 */
public class TieredGearboxBlock extends GearboxBlock implements TieredKinetic {
	private final KineticVariant variant;

	public TieredGearboxBlock(Properties properties, KineticVariant variant) {
		super(properties);
		this.variant = variant;
	}

	@Override
	public KineticVariant variant() {
		return variant;
	}

	/**
	 * Upright like Create's gearbox, so it meets shafts all around it. When a shaft above or below points into it,
	 * or the player sneaks, it lies on its side instead and turns to meet a shaft beside it, like Create's vertical
	 * gearbox does.
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		boolean onItsSide = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
		for (Direction d : new Direction[] { Direction.UP, Direction.DOWN })
			if (shaftPointsAt(level, pos, d))
				onItsSide = true;
		if (!onItsSide)
			return defaultBlockState().setValue(AXIS, Axis.Y);

		// lying down, its closed sides face along one horizontal line; keep that line free of shafts
		Axis meets = null;
		for (Direction side : Iterate.horizontalDirections) {
			if (!shaftPointsAt(level, pos, side))
				continue;
			if (meets != null && meets != side.getAxis()) {
				meets = null;
				break;
			}
			meets = side.getAxis();
		}
		Axis closed = meets == null ? context.getHorizontalDirection().getClockWise().getAxis()
			: meets == Axis.X ? Axis.Z : Axis.X;
		return defaultBlockState().setValue(AXIS, closed);
	}

	private static boolean shaftPointsAt(Level level, BlockPos pos, Direction side) {
		BlockPos neighbour = pos.relative(side);
		BlockState state = level.getBlockState(neighbour);
		return state.getBlock() instanceof IRotate rotate
			&& rotate.hasShaftTowards(level, neighbour, state, side.getOpposite());
	}

	/** Create's gearbox drops its vertical item when it lies on its side; these have one item either way up. */
	@SuppressWarnings("deprecation")
	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		return super.getDrops(state.setValue(AXIS, Axis.Y), builder);
	}

	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos,
		Player player) {
		return new ItemStack(this);
	}

	@Override
	public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return variant.flammable() ? 5 : 0;
	}

	@Override
	public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return variant.flammable() ? 5 : 0;
	}

	@Override
	public BlockEntityType<? extends GearboxBlockEntity> getBlockEntityType() {
		return ModBlockEntities.TIERED_GEARBOX.get();
	}
}

package com.mcremix.lab;

import java.util.List;

import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;

/**
 * A round flask on its retort stand, standing on a heat source as a basin does: the bottom of a lab still, with a still
 * head on it (and column pieces between), or a reflux flask with a coil condenser standing on it. It holds a bucket of
 * what goes in, filled by bucket or pipe. A bucket takes back a whole bucket of what it made, or of what went in while
 * nothing is working it; sneaking with an empty hand tips out what a bucket could take, part-buckets and all. See
 * {@link RoundFlaskBlockEntity}.
 */
public class RoundFlaskBlock extends LabGlassBlock implements IBE<RoundFlaskBlockEntity> {
	private static final VoxelShape SHAPE = Shapes.or(Block.box(3, 0, 3, 13, 11, 13), Block.box(6, 11, 6, 10, 16, 10),
		Block.box(13, 0, 13, 15, 16, 15));

	public RoundFlaskBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(LabGlass.CRACKED, false));
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		ItemStack held = player.getItemInHand(hand);
		RoundFlaskBlockEntity flask = getBlockEntity(level, pos);
		if (flask == null)
			return InteractionResult.PASS;
		if (held.isEmpty() && player.isShiftKeyDown()) {
			if (!level.isClientSide)
				flask.tipOut();
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		if (!held.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent())
			return InteractionResult.PASS;
		if (!level.isClientSide)
			LabGlass.useContainer(player, hand, flask.handler(), flask.handTanks());
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected List<FluidStack> held(Level level, BlockPos pos) {
		RoundFlaskBlockEntity flask = getBlockEntity(level, pos);
		return flask == null ? List.of() : flask.contents();
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public Class<RoundFlaskBlockEntity> getBlockEntityClass() {
		return RoundFlaskBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends RoundFlaskBlockEntity> getBlockEntityType() {
		return ModBlockEntities.ROUND_FLASK.get();
	}
}

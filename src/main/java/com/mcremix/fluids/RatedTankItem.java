package com.mcremix.fluids;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryWandItem;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Places a brass, iron, steel or aluminium tank, and like Create's tank item, puts down a whole layer when placed on top
 * of (or under) a wide tank of the same kind. Create's item only looks for its own copper tanks, so the layer is placed
 * here.
 */
public class RatedTankItem extends FluidTankItem {
	private final RatedTankBlock tank;

	public RatedTankItem(RatedTankBlock tank, Properties properties) {
		super(tank, properties);
		this.tank = tank;
	}

	@Override
	public InteractionResult place(BlockPlaceContext ctx) {
		InteractionResult result = super.place(ctx);
		if (result.consumesAction())
			placeLayer(ctx);
		return result;
	}

	private void placeLayer(BlockPlaceContext ctx) {
		Player player = ctx.getPlayer();
		if (player == null || player.isShiftKeyDown())
			return;
		Direction face = ctx.getClickedFace();
		if (!face.getAxis().isVertical())
			return;
		ItemStack stack = ctx.getItemInHand();
		Level level = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		BlockPos placedOn = pos.relative(face.getOpposite());
		if (!FluidTankBlock.isTank(level.getBlockState(placedOn)) || SymmetryWandItem.presentInHotbar(player))
			return;
		FluidTankBlockEntity part = ConnectivityHandler.partAt(tank.getBlockEntityType(), level, placedOn);
		FluidTankBlockEntity controller = part == null ? null : part.getControllerBE();
		if (controller == null)
			return;
		int width = controller.getWidth();
		if (width == 1)
			return;
		BlockPos start = face == Direction.DOWN ? controller.getBlockPos().below()
			: controller.getBlockPos().above(controller.getHeight());
		if (start.getY() != pos.getY())
			return;

		int needed = 0;
		for (int x = 0; x < width; x++)
			for (int z = 0; z < width; z++) {
				BlockState there = level.getBlockState(start.offset(x, 0, z));
				if (FluidTankBlock.isTank(there))
					continue;
				if (!there.canBeReplaced())
					return;
				needed++;
			}
		if (!player.isCreative() && stack.getCount() < needed)
			return;

		for (int x = 0; x < width; x++)
			for (int z = 0; z < width; z++) {
				BlockPos at = start.offset(x, 0, z);
				if (FluidTankBlock.isTank(level.getBlockState(at)))
					continue;
				player.getPersistentData().putBoolean("SilenceTankSound", true);
				super.place(BlockPlaceContext.at(ctx, at, face));
				player.getPersistentData().remove("SilenceTankSound");
			}
	}
}

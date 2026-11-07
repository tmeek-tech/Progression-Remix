package com.mcremix.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mcremix.electrolysis.Vat;
import com.mcremix.electrolysis.VatHolder;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.logistics.funnel.AbstractFunnelBlock;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

/**
 * Where a block of a standing vat lies in it, for its model ({@link VatShellModel}, {@link VatStandModel}): worked
 * out from the level as its chunk is built, from the controller of the tank it is or stands under. A vat's blocks are
 * built again whenever it forms, breaks up or changes size, and a door's whenever something goes in front of it.
 */
public final class VatLook {
	/**
	 * A block of a vat's scaffolding: the tank over it, how wide the vat is, where in it the block is, and where round
	 * the scaffolding its outlet is piped to ({@link #ports}).
	 */
	record Stand(BlockState tank, int width, int x, int z, int ports) {}

	/** A block of a vat's lowest tank layer that draws doors: how wide the vat is, and the sides they are on. */
	record Doors(int width, List<Direction> sides) {}

	static final ModelProperty<Stand> STAND = new ModelProperty<>();
	static final ModelProperty<Doors> DOORS = new ModelProperty<>();

	private VatLook() {}

	/** For a picture of a vat one block across, away from any level, as JEI draws one: its tank with doors on {@code sides}. */
	public static ModelData doors(Direction... sides) {
		return ModelData.builder().with(DOORS, new Doors(1, List.of(sides))).build();
	}

	/** And its cone and legs, which {@code tank}'s model draws in place of the scaffolding under it. */
	public static ModelData stand(BlockState tank) {
		return ModelData.builder().with(STAND, new Stand(tank, 1, 0, 0, 0)).build();
	}

	@Nullable
	static Doors doorsAt(BlockAndTintGetter level, BlockPos pos) {
		FluidTankBlockEntity controller = standingOver(level, pos);
		if (controller == null)
			return null;
		int width = controller.getWidth();
		BlockPos corner = controller.getBlockPos();
		List<Direction> sides = new ArrayList<>(4);
		for (Direction side : Direction.Plane.HORIZONTAL) {
			int[] at = VatParts.doorBlock(width, side);
			if (pos.getX() - corner.getX() == at[0] && pos.getZ() - corner.getZ() == at[1]
				&& clear(level, pos.relative(side), width == 2 ? side.getClockWise() : null))
				sides.add(side);
		}
		return sides.isEmpty() ? null : new Doors(width, List.copyOf(sides));
	}

	/**
	 * Whether a door has room to stand out from its vat: nothing, like a mixer, a pipe or an Electrolyser, is fitted
	 * where it would be. On a vat two across it stands out over the block {@code along} of its own as well.
	 */
	private static boolean clear(BlockAndTintGetter level, BlockPos front, @Nullable Direction along) {
		return level.getBlockState(front).isAir() && (along == null || level.getBlockState(front.relative(along)).isAir());
	}

	@Nullable
	static Stand standAt(BlockAndTintGetter level, BlockPos pos) {
		BlockPos above = pos.above();
		FluidTankBlockEntity controller = standingOver(level, above);
		if (controller == null)
			return null;
		int width = controller.getWidth();
		BlockPos corner = controller.getBlockPos();
		int x = above.getX() - corner.getX(), z = above.getZ() - corner.getZ();
		if (x < 0 || z < 0 || x >= width || z >= width)
			return null;
		return new Stand(level.getBlockState(above), width, x, z, ports(level, corner.below(), width));
	}

	/**
	 * Where round a vat's scaffolding its outlet is piped to, a bit for each {@link Vat.Port}: every inventory under or
	 * beside it, which the vat empties into, and every funnel on it pulling out, which empties it. Until 0.28.0 the
	 * outlet only ran to the middle of each side and under the middle, so on a vat two across it came out between two
	 * blocks, and an inventory anywhere else filled with no pipe to it (Taran). With none, it has no outlet.
	 */
	private static int ports(BlockAndTintGetter level, BlockPos floor, int width) {
		int ports = 0;
		for (Vat.Port port : Vat.ports(floor, width))
			if (connects(level, port))
				ports |= 1 << port.bit();
		return ports;
	}

	/** Whether there is an inventory at a port, or a funnel on the scaffolding there pulling out. */
	private static boolean connects(BlockAndTintGetter level, Vat.Port port) {
		BlockState state = level.getBlockState(port.target());
		if (state.getBlock() instanceof FunnelBlock && AbstractFunnelBlock.getFunnelFacing(state) == port.side())
			return state.getValue(FunnelBlock.EXTRACTING);
		BlockEntity be = level.getBlockEntity(port.target());
		if (be == null)
			return false;
		// a belt only makes its inventory on the server
		if (be instanceof BeltBlockEntity)
			return BeltBlock.canTransportObjects(state);
		return be.getCapability(ForgeCapabilities.ITEM_HANDLER, port.side().getOpposite()).isPresent();
	}

	/**
	 * A block changed on a client: if it is beside or under a block of a standing vat's scaffolding, the whole vat is
	 * drawn again ({@code ClientLevelMixin}), as its outlet's pipes run across it.
	 */
	public static void blockChanged(Level level, BlockPos pos) {
		for (Direction toScaffolding : new Direction[] { Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST,
			Direction.EAST }) {
			BlockPos scaffolding = pos.relative(toScaffolding);
			if (!AllBlocks.ANDESITE_SCAFFOLD.has(level.getBlockState(scaffolding)))
				continue;
			FluidTankBlockEntity controller = standingOver(level, scaffolding.above());
			if (controller != null)
				redraw(level, controller.getBlockPos());
		}
	}

	/**
	 * The controller of a standing vat whose lowest tank layer {@code tank} is in, or null. Read from the level being
	 * drawn, which for a chunk being built is a copy of the blocks round it.
	 */
	@Nullable
	private static FluidTankBlockEntity standingOver(BlockAndTintGetter level, BlockPos tank) {
		if (!(level.getBlockEntity(tank) instanceof FluidTankBlockEntity part))
			return null;
		BlockPos at = part.getController();
		if (at == null || at.getY() != tank.getY())
			return null;
		if (!(level.getBlockEntity(at) instanceof FluidTankBlockEntity controller) || !controller.isController())
			return null;
		return ((VatHolder) controller).mcremix$vat().formed() ? controller : null;
	}

	/** Has every block a vat with its controller at {@code controller} could take up, its scaffolding too, built again. */
	public static void redraw(Level level, BlockPos controller) {
		Minecraft minecraft = Minecraft.getInstance();
		if (level != minecraft.level)
			return;
		minecraft.levelRenderer.setBlocksDirty(controller.getX(), controller.getY() - 1, controller.getZ(),
			controller.getX() + Vat.MAX_WIDTH - 1, controller.getY() + Vat.MAX_HEIGHT - 1,
			controller.getZ() + Vat.MAX_WIDTH - 1);
	}
}

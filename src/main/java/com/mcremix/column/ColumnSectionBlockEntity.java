package com.mcremix.column;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.EmptyFluidHandler;

/**
 * One block of a steel column. The blocks join as Create's fluid tanks do ({@link ConnectivityHandler}): one to
 * {@link Column#MAX_WIDTH} across, a square, and up to {@link Column#MAX_HEIGHT} high, each block layer a section. The
 * controller, the column's bottom north-west block, keeps the column ({@link Column}); a pipe on any block's side works
 * with the section that block is in. When the controller passes to another block, as when the column grows to the
 * north or west or its controller is broken, what it held goes with it.
 */
public class ColumnSectionBlockEntity extends SmartBlockEntity implements IMultiBlockEntityContainer, IHaveGoggleInformation {
	@Nullable
	protected BlockPos controller;
	@Nullable
	protected BlockPos lastKnownPos;
	/** True until it has looked for a column to join: set fresh, so a column placed from a structure forms too. */
	protected boolean updateConnectivity = true;
	protected int width = 1;
	protected int height = 1;

	private final Column column = new Column(this);
	/** What a pipe on its side works with: its section of the column, whichever block keeps it now. */
	private final LazyOptional<IFluidHandler> port = LazyOptional.of(Port::new);

	public ColumnSectionBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	/** Whether it is a section of the aluminium air column (stage 4), which works {@link Column} its own way. */
	public boolean airColumn() {
		return false;
	}

	/** The column it keeps, if it is the controller; a part's is empty and idle. */
	public Column column() {
		return column;
	}

	// ------------------------------------------------------------------------------------------------ joining

	public void updateConnectivity() {
		updateConnectivity = false;
		if (level == null || level.isClientSide || !isController())
			return;
		ConnectivityHandler.formMulti(this);
	}

	@Override
	public void tick() {
		super.tick();
		if (lastKnownPos == null)
			lastKnownPos = getBlockPos();
		else if (!lastKnownPos.equals(worldPosition)) {
			// carried by a contraption: it joins again where it lands
			removeController(true);
			lastKnownPos = worldPosition;
			return;
		}
		if (updateConnectivity)
			updateConnectivity();
		if (level == null || level.isClientSide || !isController())
			return;
		if (column.width() != width || column.height() != height)
			column.resize(width, height);
		column.settle();
		column.tick(level);
	}

	@Override
	public BlockPos getController() {
		return isController() ? worldPosition : controller;
	}

	@Override
	public boolean isController() {
		return controller == null || controller.equals(worldPosition);
	}

	@Nullable
	@SuppressWarnings("unchecked")
	@Override
	public ColumnSectionBlockEntity getControllerBE() {
		if (isController())
			return this;
		if (level == null)
			return null;
		BlockEntity be = level.getBlockEntity(controller);
		return be instanceof ColumnSectionBlockEntity section ? section : null;
	}

	@Override
	public void setController(BlockPos pos) {
		if (level == null || level.isClientSide && !isVirtual())
			return;
		if (pos.equals(controller))
			return;
		// a controller joining another column hands on what it kept
		if (isController() && !pos.equals(worldPosition) && column.holdsAnything()
			&& level.getBlockEntity(pos) instanceof ColumnSectionBlockEntity to) {
			to.column.absorb(column, worldPosition.getY() - pos.getY());
			column.clear();
		}
		controller = pos;
		setChanged();
		sendData();
	}

	@Override
	public void removeController(boolean keepContents) {
		if (level == null || level.isClientSide)
			return;
		// what it keeps stays, for the column it forms again: the column trims it to the size it comes to
		updateConnectivity = true;
		controller = null;
		width = 1;
		height = 1;
		setChanged();
		sendData();
	}

	/**
	 * Before a controller is broken: what it kept goes to a block of its column left standing, the lowest first, which
	 * forms the column again from what is left.
	 */
	void bequeath() {
		if (level == null || level.isClientSide || !isController() || !column.holdsAnything())
			return;
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++)
				for (int z = 0; z < width; z++) {
					BlockPos at = worldPosition.offset(x, y, z);
					if (!at.equals(worldPosition) && level.getBlockEntity(at) instanceof ColumnSectionBlockEntity heir
						&& worldPosition.equals(heir.controller)) {
						heir.column.absorb(column, -y);
						column.clear();
						return;
					}
				}
	}

	@Override
	public BlockPos getLastKnownPos() {
		return lastKnownPos;
	}

	@Override
	public void preventConnectivityUpdate() {
		updateConnectivity = false;
	}

	/**
	 * Create calls this on the controller part way through forming a column too, before its new size is set; the column
	 * cuts back what no longer fits only on its next tick ({@link Column#settle}).
	 */
	@Override
	public void notifyMultiUpdated() {
		if (isController())
			column.resize(width, height);
		setChanged();
		sendData();
	}

	@Override
	public Direction.Axis getMainConnectionAxis() {
		return Direction.Axis.Y;
	}

	@Override
	public int getMaxLength(Direction.Axis longAxis, int width) {
		return longAxis == Direction.Axis.Y ? Column.MAX_HEIGHT : Column.MAX_WIDTH;
	}

	@Override
	public int getMaxWidth() {
		return Column.MAX_WIDTH;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public void setHeight(int height) {
		this.height = height;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public void setWidth(int width) {
		this.width = width;
	}

	// ------------------------------------------------------------------------------------------------ pipes

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER && (side == null || side.getAxis().isHorizontal()))
			return port.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		port.invalidate();
	}

	/** Its section of the column, found afresh each time: the column may have grown or split since. */
	private class Port implements IFluidHandler {
		private IFluidHandler section() {
			ColumnSectionBlockEntity c = getControllerBE();
			if (c == null)
				return EmptyFluidHandler.INSTANCE;
			int layer = worldPosition.getY() - c.worldPosition.getY();
			return layer < 0 || layer >= c.height ? EmptyFluidHandler.INSTANCE : c.column.handler(layer);
		}

		@Override
		public int getTanks() {
			return section().getTanks();
		}

		@Nonnull
		@Override
		public FluidStack getFluidInTank(int tank) {
			return section().getFluidInTank(tank);
		}

		@Override
		public int getTankCapacity(int tank) {
			return section().getTankCapacity(tank);
		}

		@Override
		public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
			return section().isFluidValid(tank, stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return section().fill(resource, action);
		}

		@Nonnull
		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return section().drain(resource, action);
		}

		@Nonnull
		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return section().drain(maxDrain, action);
		}
	}

	// ------------------------------------------------------------------------------------------------ saving

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		if (updateConnectivity)
			tag.putBoolean("Uninitialized", true);
		if (lastKnownPos != null)
			tag.put("LastKnownPos", NbtUtils.writeBlockPos(lastKnownPos));
		if (!isController())
			tag.put("Controller", NbtUtils.writeBlockPos(controller));
		else {
			tag.putInt("Size", width);
			tag.putInt("Height", height);
			CompoundTag kept = new CompoundTag();
			column.write(kept);
			tag.put("Column", kept);
		}
		super.write(tag, clientPacket);
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		BlockPos controllerBefore = controller;
		int widthBefore = width;
		int heightBefore = height;
		updateConnectivity = tag.contains("Uninitialized");
		controller = null;
		lastKnownPos = null;
		if (tag.contains("LastKnownPos"))
			lastKnownPos = NbtUtils.readBlockPos(tag.getCompound("LastKnownPos"));
		if (tag.contains("Controller"))
			controller = NbtUtils.readBlockPos(tag.getCompound("Controller"));
		if (isController()) {
			width = Math.max(1, Math.min(Column.MAX_WIDTH, tag.getInt("Size")));
			height = Math.max(1, Math.min(Column.MAX_HEIGHT, tag.getInt("Height")));
			column.sizeForRead(width, height);
			column.read(tag.getCompound("Column"));
		}
		// the client draws a formed column's sides and top joined up (client/ColumnCTBehaviour): when the column this
		// section is in changes, its blocks are drawn again, as Create's tanks are
		if (clientPacket && level != null && (!Objects.equals(controllerBefore, controller) || width != widthBefore
			|| height != heightBefore))
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 16);
	}

	// ------------------------------------------------------------------------------------------------ goggles

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		ColumnSectionBlockEntity c = getControllerBE();
		if (c == null)
			return false;
		tooltip.add(Column.line(getBlockState().getBlock().getName()));
		c.column.addGoggleLines(tooltip, worldPosition.getY() - c.worldPosition.getY());
		return true;
	}
}

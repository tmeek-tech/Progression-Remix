package com.mcremix.air;

import java.util.ArrayList;
import java.util.List;

import com.mcremix.column.Column;
import com.mcremix.column.ColumnSectionBlockEntity;
import com.mcremix.fluids.Overheating;
import com.mcremix.fluids.Spills;
import com.mcremix.kinetics.AluminiumMelting;
import com.mcremix.kinetics.FrictionHeat;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;

/**
 * One block of the aluminium air column (stage 4): a {@link ColumnSectionBlockEntity} whose column works as the air
 * column ({@link Column}). Its controller also keeps the column's frost, and melts the whole column, as an aluminium
 * tank melts, with anything Hot beside it.
 * <ul>
 * <li>Running, the sections frost over after {@link #FROSTS_AFTER} ticks and grow icicles after {@link #ICICLES_AFTER}.
 * Stopped, the icicles drip away after {@link #ICICLES_GO_AFTER} ticks and the frost after {@link #FROST_GOES_AFTER}.
 * It is only looks.</li>
 * </ul>
 */
public class AirColumnSectionBlockEntity extends ColumnSectionBlockEntity {
	public static final int FROSTS_AFTER = 200;
	public static final int ICICLES_AFTER = 600;
	public static final int ICICLES_GO_AFTER = 400;
	public static final int FROST_GOES_AFTER = 1200;
	/** The aluminium in a section, as its recipe puts it in: four sheets round a tank of two make two. */
	public static final int METAL = 3 * FrictionHeat.CASING_METAL;

	private int frost;
	private int runningFor;
	private int stoppedFor;
	private int melting;

	public AirColumnSectionBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public boolean airColumn() {
		return true;
	}

	/** 0 bare, 1 frosted, 2 with icicles, as its controller keeps it. */
	public int frost() {
		return frost;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level instanceof ServerLevel server) || !isController() || isRemoved())
			return;
		frost(server);
		if (Overheating.due(server, worldPosition)) {
			boolean hot = AluminiumMelting.besideHeat(server, worldPosition, width, height);
			melting = Overheating.check(server, worldPosition, getBlockState(), width, height, hot, melting,
				() -> melt(server));
		}
	}

	private void frost(ServerLevel server) {
		boolean running = column().job() == Column.Job.RUNNING;
		if (running) {
			runningFor = Math.min(runningFor + 1, ICICLES_AFTER);
			stoppedFor = 0;
		} else {
			stoppedFor = Math.min(stoppedFor + 1, FROST_GOES_AFTER);
			runningFor = 0;
		}
		int want = running
			? Math.max(frost, runningFor >= ICICLES_AFTER ? 2 : runningFor >= FROSTS_AFTER ? 1 : 0)
			: Math.min(frost, stoppedFor >= FROST_GOES_AFTER ? 0 : stoppedFor >= ICICLES_GO_AFTER ? 1 : 2);
		boolean changed = want != frost;
		if (changed) {
			frost = want;
			setChanged();
		}
		// every block of it shows it, those joined since too
		if (changed || server.getGameTime() % 20 == 0)
			showFrost(server);
	}

	private void showFrost(ServerLevel server) {
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++)
				for (int z = 0; z < width; z++) {
					BlockPos at = worldPosition.offset(x, y, z);
					BlockState state = server.getBlockState(at);
					if (state.getBlock() instanceof AirColumnSectionBlock && state.getValue(AirColumnSectionBlock.FROST) != frost)
						server.setBlock(at, state.setValue(AirColumnSectionBlock.FROST, frost), Block.UPDATE_CLIENTS);
				}
	}

	/** The whole column melts: what it held spills or escapes, and its metal runs down and sets as rough aluminium. */
	private void melt(ServerLevel server) {
		List<FluidStack> held = column().empty();
		List<BlockPos> spots = new ArrayList<>();
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++)
				for (int z = 0; z < width; z++)
					spots.add(worldPosition.offset(x, y, z));
		List<BlockPos> melted = new ArrayList<>();
		for (BlockPos at : spots)
			if (server.getBlockState(at).getBlock() instanceof AirColumnSectionBlock) {
				server.removeBlock(at, false);
				melted.add(at);
			}
		for (FluidStack fluid : held)
			Spills.spill(server, spots, fluid);
		for (BlockPos at : melted)
			AluminiumMelting.melt(server, at, METAL);
	}

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		if (isController()) {
			tag.putInt("Frost", frost);
			tag.putInt("RunningFor", runningFor);
			tag.putInt("StoppedFor", stoppedFor);
		}
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		frost = tag.getInt("Frost");
		runningFor = tag.getInt("RunningFor");
		stoppedFor = tag.getInt("StoppedFor");
	}
}

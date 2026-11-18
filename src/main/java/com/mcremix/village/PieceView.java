package com.mcremix.village;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.material.FluidState;

/**
 * One village piece's blocks as {@link RuinProcessor} reworks them, before they are placed. Positions are world
 * positions; states are as the template holds them, unrotated, since the template turns each state only as it places it.
 * {@link #worldState} gives a block as it will stand in the world, and as a {@link BlockGetter} the view answers with
 * those, so shapes and faces can be asked about. A position the piece holds nothing for is left to the world: within the
 * piece's extent mostly air the template had (old-style pieces drop it, and the village has cleared the ground there),
 * outside it the ground below and around the piece.
 * <p>
 * Everything decided from the view alone comes out the same for every chunk the piece crosses. The world is only read
 * inside the chunk being placed ({@link #free}, {@link #worldAt}), which the piece has not touched yet, so a choice that
 * needs it is made in the one chunk that places that block.
 */
final class PieceView implements BlockGetter {
	/** What a missing block is placed as; see {@link RuinProcessor}. */
	static final BlockState GONE = Blocks.CAVE_AIR.defaultBlockState();

	@Nullable
	final ServerLevelAccessor level;
	@Nullable
	final BoundingBox chunk;
	final Rotation rotation;
	final Mirror mirror;
	private final Rotation unrotation;
	private final Map<BlockPos, StructureBlockInfo> blocks = new LinkedHashMap<>();
	/** Where the template had doors, gone or not. */
	private final Set<BlockPos> doorways = new HashSet<>();
	/** The piece's extent, from the blocks it had when the view was made. */
	final int minX, minY, minZ, maxX, maxY, maxZ;

	/**
	 * @param originals the template's own blocks, at their places in the template (as a processor's
	 *        {@code finalizeProcessing} gets them), so the view knows where the doors were before any were taken; null,
	 *        and only doors still in {@code infos} count
	 * @param origin where the template is placed, which places {@code originals} in the world
	 */
	PieceView(@Nullable ServerLevelAccessor level, List<StructureBlockInfo> infos,
		@Nullable List<StructureBlockInfo> originals, BlockPos origin, StructurePlaceSettings settings) {
		this.level = level;
		this.chunk = settings.getBoundingBox();
		this.rotation = settings.getRotation();
		this.mirror = settings.getMirror();
		this.unrotation = switch (rotation) {
			case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
			case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
			default -> rotation;
		};
		int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE;
		int x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
		if (originals != null)
			for (StructureBlockInfo original : originals)
				if (original.state().getBlock() instanceof DoorBlock)
					doorways.add(StructureTemplate.calculateRelativePosition(settings, original.pos()).offset(origin));
		for (StructureBlockInfo info : infos) {
			blocks.put(info.pos(), info);
			if (info.state().getBlock() instanceof DoorBlock)
				doorways.add(info.pos());
			BlockPos p = info.pos();
			x0 = Math.min(x0, p.getX());
			y0 = Math.min(y0, p.getY());
			z0 = Math.min(z0, p.getZ());
			x1 = Math.max(x1, p.getX());
			y1 = Math.max(y1, p.getY());
			z1 = Math.max(z1, p.getZ());
		}
		minX = x0;
		minY = y0;
		minZ = z0;
		maxX = x1;
		maxY = y1;
		maxZ = z1;
	}

	boolean isEmpty() {
		return blocks.isEmpty();
	}

	/** Whether the piece holds anything for this position, air included. */
	boolean has(BlockPos pos) {
		return blocks.containsKey(pos);
	}

	/** The block the piece will place here as the template holds it, or air where the piece holds nothing. */
	BlockState state(BlockPos pos) {
		StructureBlockInfo info = blocks.get(pos);
		return info == null ? Blocks.AIR.defaultBlockState() : info.state();
	}

	@Nullable
	StructureBlockInfo info(BlockPos pos) {
		return blocks.get(pos);
	}

	/** The block here as it will stand in the world, turned the way the piece is. */
	BlockState worldState(BlockPos pos) {
		return toWorld(state(pos));
	}

	BlockState toWorld(BlockState templateState) {
		return templateState.mirror(mirror).rotate(rotation);
	}

	/** A state meant to stand in the world this way round, as the template must hold it to be placed so. */
	BlockState toTemplate(BlockState worldState) {
		return worldState.rotate(unrotation).mirror(mirror);
	}

	/** Nothing here in the piece: no block, or air (removed blocks are cave air). */
	boolean empty(BlockPos pos) {
		return state(pos).isAir();
	}

	/** Whether the template had a door here, whether or not it is still there. */
	boolean doorway(BlockPos pos) {
		return doorways.contains(pos);
	}

	boolean inChunk(BlockPos pos) {
		return chunk == null || chunk.isInside(pos);
	}

	/** Within the piece's extent, sideways. */
	boolean inColumns(int x, int z) {
		return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
	}

	/** Within the piece's extent, where a position it holds nothing for is mostly air it left out. */
	boolean inside(BlockPos pos) {
		return inColumns(pos.getX(), pos.getZ()) && pos.getY() >= minY && pos.getY() <= maxY;
	}

	/**
	 * Whether a block can be put here: the piece has nothing here, and neither has the world, ground or another piece
	 * reaching in. The world is only read inside the chunk being placed; beyond it, a position within the piece's extent
	 * counts as the air the template left out, and one outside it as taken.
	 */
	boolean free(BlockPos pos) {
		StructureBlockInfo info = blocks.get(pos);
		if (info != null)
			return info.state().isAir();
		if (inChunk(pos) && level != null)
			return level.getBlockState(pos).isAir();
		return inside(pos);
	}

	/**
	 * The block the world will have here once the piece is placed, as far as is known: the piece's own block where it
	 * has one, the world's elsewhere inside the chunk being placed, air elsewhere within the piece's extent, and null
	 * (unknown) anywhere else.
	 */
	@Nullable
	BlockState worldAt(BlockPos pos) {
		StructureBlockInfo info = blocks.get(pos);
		if (info != null)
			return toWorld(info.state());
		if (inChunk(pos) && level != null)
			return level.getBlockState(pos);
		return inside(pos) ? Blocks.AIR.defaultBlockState() : null;
	}

	/** Places a block, given as the template holds it. A block entity's data stays only on the same block. */
	void set(BlockPos pos, BlockState templateState) {
		StructureBlockInfo old = blocks.get(pos);
		boolean same = old != null && old.state().getBlock() == templateState.getBlock();
		blocks.put(pos, new StructureBlockInfo(pos, templateState,
			same && templateState.hasBlockEntity() ? old.nbt() : null));
	}

	/** Places a block given as it should stand in the world. */
	void setWorld(BlockPos pos, BlockState worldState) {
		set(pos, toTemplate(worldState));
	}

	/** Takes a block out: it is placed as cave air, so nothing the world had there shows through. */
	void remove(BlockPos pos) {
		if (blocks.containsKey(pos))
			blocks.put(pos, new StructureBlockInfo(pos, GONE, null));
	}

	/** Moves a block, data and all, leaving cave air behind. */
	void move(BlockPos from, BlockPos to) {
		StructureBlockInfo info = blocks.get(from);
		if (info == null)
			return;
		remove(from);
		blocks.put(to, new StructureBlockInfo(to, info.state(), info.nbt()));
	}

	/** Every position the piece holds anything for, in the order the template lists them, new ones last. */
	List<BlockPos> positions() {
		return new ArrayList<>(blocks.keySet());
	}

	List<StructureBlockInfo> list() {
		return new ArrayList<>(blocks.values());
	}

	/** The highest position in the column the piece holds a block (not air) for, or {@code minY - 1}. */
	int top(int x, int z) {
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, maxY + 2, z);
		for (int y = maxY + 2; y >= minY; y--)
			if (!empty(at.setY(y)))
				return y;
		return minY - 1;
	}

	/** Whether nothing in the piece stands over this position. */
	boolean openAbove(BlockPos pos) {
		return top(pos.getX(), pos.getZ()) <= pos.getY();
	}

	/** Whether the face of the block at {@code pos} toward {@code side} is whole, as the block stands in the world. */
	boolean fullFace(BlockPos pos, Direction side) {
		BlockState state = worldState(pos);
		return !state.isAir() && state.isFaceSturdy(this, pos, side);
	}

	// BlockGetter, for shapes: the piece's blocks as they will stand, air where it has none

	@Nullable
	@Override
	public BlockEntity getBlockEntity(BlockPos pos) {
		return null;
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		return worldState(pos);
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return getBlockState(pos).getFluidState();
	}

	@Override
	public int getHeight() {
		return level == null ? 384 : level.getHeight();
	}

	@Override
	public int getMinBuildHeight() {
		return level == null ? -64 : level.getMinBuildHeight();
	}
}

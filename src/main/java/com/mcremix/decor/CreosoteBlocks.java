package com.mcremix.decor;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mcremix.mixin.FireBlockAccessor;
import com.mcremix.registry.ModDecor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SporeBlossomBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Creosote timber: wood soaked in creosote, dark and oily like railway sleepers, dock piles and telegraph poles. It
 * burns as wood does. Left in hot sun (a hot biome, by day, under open sky) it weeps a little: specks of creosote drip
 * from its underside, as real treated timber bleeds in summer ({@link #weep}).
 */
public class CreosoteBlocks {

	/**
	 * Fire's odds for creosote timber, as vanilla's for wood: planks' for everything sawn, logs' for logs and posts.
	 * Given to fire itself, as vanilla's own wood is, so that waterlogged timber never burns. On the common setup's
	 * main-thread work.
	 */
	public static void setup() {
		FireBlockAccessor fire = (FireBlockAccessor) Blocks.FIRE;
		fire.mcremix$setFlammable(ModDecor.CREOSOTE_LOG.get(), 5, 5);
		fire.mcremix$setFlammable(ModDecor.CREOSOTE_POST.get(), 5, 5);
		for (Block sawn : new Block[] { ModDecor.CREOSOTE_PLANKS.get(), ModDecor.CREOSOTE_STAIRS.get(),
			ModDecor.CREOSOTE_SLAB.get(), ModDecor.CREOSOTE_FENCE.get(), ModDecor.CREOSOTE_FENCE_GATE.get() })
			fire.mcremix$setFlammable(sawn, 5, 20);
	}

	/**
	 * Whether the sun is up, from the level's clock, turning where {@link Level#isDay} does in clear weather. Not
	 * isDay itself, which a client works out once, when it joins, and never again.
	 */
	static boolean sunUp(Level level) {
		return !level.dimensionType().hasFixedTime() && Mth.cos(level.getTimeOfDay(1f) * Mth.TWO_PI) > 0.068f;
	}

	/** Now and then, in hot sun, a speck of creosote drips from under {@code pos}. */
	static void weep(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(24) != 0 || !sunUp(level) || !level.canSeeSky(pos.above()))
			return;
		if (level.getBiome(pos).value().getBaseTemperature() <= 1.0f)
			return;
		BlockPos below = pos.below();
		if (!level.getBlockState(below).isAir())
			return;
		Vec3 spot = dripSpot(state, level, pos, random);
		if (spot != null)
			level.addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, state), pos.getX() + spot.x,
				pos.getY() + spot.y - 0.05, pos.getZ() + spot.z, 0, 0, 0);
	}

	/**
	 * Where in its block a drip starts: a box of the timber's shape picked by how much of the ground it covers, a spot
	 * under it, and the underside of the timber there. The shape is the occlusion shape, which is the outline but for
	 * fences and gates, whose outlines reach the ground under their rails. Null for an empty shape.
	 */
	@Nullable
	public static Vec3 dripSpot(BlockState state, BlockGetter level, BlockPos pos, RandomSource random) {
		VoxelShape shape = state.getOcclusionShape(level, pos);
		if (shape.isEmpty())
			return null;
		List<AABB> boxes = shape.toAabbs();
		double pick = 0;
		for (AABB b : boxes)
			pick += (b.maxX - b.minX) * (b.maxZ - b.minZ);
		pick *= random.nextDouble();
		AABB box = boxes.get(boxes.size() - 1);
		for (AABB b : boxes)
			if ((pick -= (b.maxX - b.minX) * (b.maxZ - b.minZ)) <= 0) {
				box = b;
				break;
			}
		double x = box.minX + random.nextDouble() * (box.maxX - box.minX);
		double z = box.minZ + random.nextDouble() * (box.maxZ - box.minZ), y = box.minY;
		for (AABB b : boxes)
			if (x >= b.minX && x <= b.maxX && z >= b.minZ && z <= b.maxZ)
				y = Math.min(y, b.minY);
		return new Vec3(x, y, z);
	}

	/** A creosoted log: a debarked round timber, as poles and piles are. */
	public static class Log extends RotatedPillarBlock {
		public Log(Properties properties) {
			super(properties);
		}

		@Override
		public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
			weep(state, level, pos, random);
		}
	}

	public static class Planks extends Block {
		public Planks(Properties properties) {
			super(properties);
		}

		@Override
		public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
			weep(state, level, pos, random);
		}
	}

	public static class Stairs extends StairBlock {
		public Stairs(Supplier<BlockState> base, Properties properties) {
			super(base, properties);
		}

		@Override
		public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
			weep(state, level, pos, random);
		}
	}

	public static class Slab extends SlabBlock {
		public Slab(Properties properties) {
			super(properties);
		}

		@Override
		public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
			weep(state, level, pos, random);
		}
	}

	public static class Fence extends FenceBlock {
		public Fence(Properties properties) {
			super(properties);
		}

		@Override
		public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
			weep(state, level, pos, random);
		}
	}

	public static class Gate extends FenceGateBlock {
		public Gate(Properties properties) {
			super(properties, WoodType.OAK);
		}

		@Override
		public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
			weep(state, level, pos, random);
		}
	}

	/**
	 * A creosoted post: a pole a little over a third of a block thick, standing up or lying along either way like a
	 * log, as for telegraph poles, jetty piles and fence posts. Posts join where one points at another's side, so a
	 * crossarm meets its pole and a pole stands on a beam: a short stub of the same pole fills the gap ({@link #SIDES}).
	 * A lantern, hanging sign or bell hangs under any post, under a lying one on a short length of chain ({@link #CHAIN}),
	 * and a torch stands on a standing one. Mobs path round posts, as round fences.
	 */
	public static class Post extends Log implements SimpleWaterloggedBlock {
		public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
		/** A stub toward the post on that side, which points at this one. Never set along its own axis. */
		public static final Map<Direction, BooleanProperty> SIDES = PipeBlock.PROPERTY_BY_DIRECTION;
		/** A lying post with something hanging under it ({@link #hangsFrom}), which a short length of chain joins to it. */
		public static final BooleanProperty CHAIN = BooleanProperty.create("chain");
		private static final VoxelShape X = Block.box(0, 5, 5, 16, 11, 11), Y = Block.box(5, 0, 5, 11, 16, 11),
			Z = Block.box(5, 5, 0, 11, 11, 16);
		/** The chain's hold on a lying post's underside, which a hanging lantern needs to stay up. */
		private static final VoxelShape HOOK = Block.box(7, 0, 7, 9, 5, 9);
		/** Shapes by axis and by the stubs it has, a bit for each direction by its ordinal. */
		private static final VoxelShape[] SHAPES = new VoxelShape[3 * 64];

		static {
			for (Direction.Axis axis : Direction.Axis.values())
				for (int stubs = 0; stubs < 64; stubs++) {
					VoxelShape shape = switch (axis) {
						case X -> X;
						case Y -> Y;
						case Z -> Z;
					};
					for (Direction side : Direction.values())
						if ((stubs & 1 << side.ordinal()) != 0)
							shape = Shapes.or(shape, stub(side));
					SHAPES[axis.ordinal() * 64 + stubs] = shape.optimize();
				}
		}

		/** The pole's own width, from where it would cross the middle of the block to the face on {@code side}. */
		private static VoxelShape stub(Direction side) {
			double[] min = {5, 5, 5}, max = {11, 11, 11};
			int i = side.getAxis().ordinal();
			if (side.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
				min[i] = 11;
				max[i] = 16;
			} else {
				min[i] = 0;
				max[i] = 5;
			}
			return Block.box(min[0], min[1], min[2], max[0], max[1], max[2]);
		}

		public Post(Properties properties) {
			super(properties);
			BlockState state = defaultBlockState().setValue(WATERLOGGED, false).setValue(CHAIN, false);
			for (BooleanProperty side : SIDES.values())
				state = state.setValue(side, false);
			registerDefaultState(state);
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			super.createBlockStateDefinition(builder);
			builder.add(WATERLOGGED, CHAIN);
			SIDES.values().forEach(builder::add);
		}

		/** Whether the post on {@code side} points at this one, so they join. */
		static boolean joins(BlockState neighbour, Direction side) {
			return neighbour.getBlock() instanceof Post && neighbour.getValue(AXIS) == side.getAxis();
		}

		/**
		 * Whether a lantern, a hanging sign, a bell or a spore blossom hangs, or a chain runs down, from the block above
		 * {@code below}: what holds on by the middle of the face above it, as the chain does.
		 */
		static boolean hangsFrom(BlockState below) {
			if (below.hasProperty(BlockStateProperties.HANGING))
				return below.getValue(BlockStateProperties.HANGING);
			if (below.getBlock() instanceof CeilingHangingSignBlock || below.getBlock() instanceof SporeBlossomBlock)
				return true;
			if (below.getBlock() instanceof BellBlock)
				return below.getValue(BellBlock.ATTACHMENT) == BellAttachType.CEILING;
			return below.getBlock() instanceof ChainBlock && below.getValue(ChainBlock.AXIS) == Direction.Axis.Y;
		}

		/**
		 * Whether {@code face} of {@code state} holds only by the hook a chain hangs from: a lying post's underside, which
		 * is a third of a block above the face. What bolts or lies flat against a face, as a bracket or a pipe on a face
		 * does, would hang in the air there.
		 */
		public static boolean hookOnly(BlockState state, Direction face) {
			return face == Direction.DOWN && state.getBlock() instanceof Post && state.getValue(AXIS) != Direction.Axis.Y;
		}

		@Nullable
		@Override
		public BlockState getStateForPlacement(BlockPlaceContext context) {
			BlockState state = super.getStateForPlacement(context);
			if (state == null)
				return null;
			LevelAccessor level = context.getLevel();
			BlockPos pos = context.getClickedPos();
			state = state.setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
			for (Direction side : Direction.values())
				state = joined(state, side, level.getBlockState(pos.relative(side)));
			return state;
		}

		/** {@code state} with its stub on {@code side}, and its chain if that is down, as {@code neighbour} wants. */
		private static BlockState joined(BlockState state, Direction side, BlockState neighbour) {
			Direction.Axis axis = state.getValue(AXIS);
			if (side.getAxis() != axis)
				state = state.setValue(SIDES.get(side), joins(neighbour, side));
			if (side == Direction.DOWN)
				state = state.setValue(CHAIN, axis != Direction.Axis.Y && hangsFrom(neighbour));
			return state;
		}

		@Override
		public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
			BlockPos pos, BlockPos neighbourPos) {
			if (state.getValue(WATERLOGGED))
				level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
			return joined(state, direction, neighbour);
		}

		@Override
		public BlockState rotate(BlockState state, Rotation rotation) {
			BlockState turned = super.rotate(state, rotation);
			for (Direction side : Direction.Plane.HORIZONTAL)
				turned = turned.setValue(SIDES.get(rotation.rotate(side)), state.getValue(SIDES.get(side)));
			return turned;
		}

		@Override
		public BlockState mirror(BlockState state, Mirror mirror) {
			BlockState turned = state;
			for (Direction side : Direction.Plane.HORIZONTAL)
				turned = turned.setValue(SIDES.get(mirror.mirror(side)), state.getValue(SIDES.get(side)));
			return turned;
		}

		@Override
		public FluidState getFluidState(BlockState state) {
			return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
		}

		@Override
		public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
			int stubs = 0;
			for (Direction side : Direction.values())
				if (state.getValue(SIDES.get(side)))
					stubs |= 1 << side.ordinal();
			return SHAPES[state.getValue(AXIS).ordinal() * 64 + stubs];
		}

		/** A lying post holds a lantern up by its middle underneath, where the chain hangs from it ({@link #hookOnly}). */
		@Override
		public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
			VoxelShape shape = getShape(state, level, pos, CollisionContext.empty());
			return state.getValue(AXIS) == Direction.Axis.Y ? shape : Shapes.or(shape, HOOK);
		}

		@Override
		public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
			return false;
		}
	}

	/** A post's item, which burns in a furnace as a log does. */
	public static class PostItem extends BlockItem {
		public PostItem(Block block, Properties properties) {
			super(block, properties);
		}

		@Override
		public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
			return 300;
		}
	}
}

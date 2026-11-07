package com.mcremix.fluids;

import java.util.EnumMap;
import java.util.Map;

import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A mechanical pump in brass, iron, steel or aluminium, pumping exactly as Create's. Its limits come from its material
 * like any part's (the {@code mcremix:kinetic_material/*} tags): brass pumps run to 96 RPM, Create's copper one and iron
 * ones to 256, steel and aluminium ones to 512, and an aluminium one heats as it turns, as aluminium friction points
 * do. An iron or steel one rusts as their pipes do ({@link Rusting}), and a brass one has a handwheel setting the most
 * it lets through ({@link Throttled}).
 */
public class RatedPumpBlock extends PumpBlock implements FluidPartMaterial.Part {
	private final FluidPartMaterial material;

	protected RatedPumpBlock(Properties properties, FluidPartMaterial material) {
		super(properties);
		this.material = material;
	}

	/** The pump for a material: iron and steel ones rust, and brass ones have a handwheel. */
	public static RatedPumpBlock create(Properties properties, FluidPartMaterial material) {
		if (material.rusts())
			return new Rusting(properties, material);
		return material == FluidPartMaterial.BRASS ? new Throttled(properties, material)
			: new RatedPumpBlock(properties, material);
	}

	@Override
	public FluidPartMaterial material() {
		return material;
	}

	@Override
	public BlockEntityType<? extends PumpBlockEntity> getBlockEntityType() {
		return ModBlockEntities.RATED_PUMP.get();
	}

	/** An iron or steel pump, rusting by its {@link Patina} as its pipe does. */
	public static class Rusting extends RatedPumpBlock {
		protected Rusting(Properties properties, FluidPartMaterial material) {
			super(properties, material);
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			super.createBlockStateDefinition(builder);
			builder.add(Patina.PROPERTY);
		}
	}

	/**
	 * A brass pump, with a handwheel on a stem like a throttle valve's (Taran, 0.26.0), which sets the most it lets
	 * through ({@link RatedPumpBlockEntity}). The models are drawn facing up, as Create's are, with the handwheel to the
	 * south over the pump's front half; the blockstate turns them, so it sits on top of a pump lying down, south of one
	 * facing up and north of one facing down. What can be clicked turns with it.
	 */
	public static class Throttled extends RatedPumpBlock {
		/** The middle of the handwheel's face, in pixels, as the models are drawn. */
		private static final Vec3 WHEEL = new Vec3(8, 12, 16);
		private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

		static {
			for (Direction facing : Direction.values())
				SHAPES.put(facing, Shapes.or(box(facing, 2, 0, 2, 14, 16, 14), box(facing, 4, 8, 14, 12, 16, 16)));
		}

		protected Throttled(Properties properties, FluidPartMaterial material) {
			super(properties, material);
		}

		@Override
		public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
			return SHAPES.get(state.getValue(FACING));
		}

		/** The side of a pump facing {@code facing} that its handwheel is on. */
		public static Direction wheelSide(Direction facing) {
			return switch (facing) {
				case UP -> Direction.SOUTH;
				case DOWN -> Direction.NORTH;
				default -> Direction.UP;
			};
		}

		/** The middle of the handwheel's face on a pump facing {@code facing}, within its block. */
		public static Vec3 wheel(Direction facing) {
			return turned(facing, WHEEL.scale(1 / 16d));
		}

		/** A point of the models, drawn facing up, where the blockstate puts it on a pump facing {@code facing}. */
		static Vec3 turned(Direction facing, Vec3 p) {
			return switch (facing) {
				case UP -> p;
				case DOWN -> new Vec3(p.x, 1 - p.y, 1 - p.z);
				case NORTH -> new Vec3(p.x, p.z, 1 - p.y);
				case SOUTH -> new Vec3(1 - p.x, p.z, p.y);
				case EAST -> new Vec3(p.y, p.z, p.x);
				case WEST -> new Vec3(1 - p.y, p.z, 1 - p.x);
			};
		}

		private static VoxelShape box(Direction facing, double x0, double y0, double z0, double x1, double y1,
			double z1) {
			Vec3 a = turned(facing, new Vec3(x0, y0, z0).scale(1 / 16d));
			Vec3 b = turned(facing, new Vec3(x1, y1, z1).scale(1 / 16d));
			return Shapes.box(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z), Math.max(a.x, b.x),
				Math.max(a.y, b.y), Math.max(a.z, b.z));
		}
	}
}

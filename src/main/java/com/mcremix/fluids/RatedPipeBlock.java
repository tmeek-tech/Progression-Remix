package com.mcremix.fluids;

import java.util.Optional;

import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A fluid pipe in brass, iron, steel, aluminium or glass. It joins and carries fluid exactly as Create's copper pipe,
 * but joins only pipes of its own metal, and fittings of any ({@link PipeMetal}). What differs is how hot a fluid it
 * takes (see {@link FluidHeat}) and what its material does ({@link FluidPartMaterial}): a line through brass carries
 * half as much and through glass a quarter, brass pipes are thinner, and iron and steel ones rust ({@link Rusting}). A wrench
 * takes off a bracket but makes no window: a glass pipe is all window already.
 */
public class RatedPipeBlock extends FluidPipeBlock implements FluidPartMaterial.Part {
	private final FluidPartMaterial material;

	protected RatedPipeBlock(Properties properties, FluidPartMaterial material) {
		super(properties);
		this.material = material;
	}

	/** The pipe for a material: iron and steel ones rust, brass ones are thin. */
	public static RatedPipeBlock create(Properties properties, FluidPartMaterial material) {
		if (material.rusts())
			return new Rusting(properties, material);
		if (material.thin())
			return new Thin(properties, material);
		return new RatedPipeBlock(properties, material);
	}

	@Override
	public FluidPartMaterial material() {
		return material;
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		return tryRemoveBracket(context) ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}

	/** As Create's, which only puts its own pipe's sides back right once the bracket is off. */
	@Override
	public boolean tryRemoveBracket(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Optional<ItemStack> bracket = removeBracket(level, pos, false);
		if (bracket.isEmpty())
			return false;
		Player player = context.getPlayer();
		if (!level.isClientSide && player != null && !player.isCreative())
			player.getInventory().placeItemBackInInventory(bracket.get());
		if (!level.isClientSide) {
			BlockState state = level.getBlockState(pos);
			Axis preferred = FluidPropagator.getStraightPipeAxis(state);
			Direction direction = preferred == null ? Direction.UP : Direction.get(AxisDirection.POSITIVE, preferred);
			BlockState updated = updateBlockState(state, direction, null, level, pos);
			if (updated != state)
				level.setBlockAndUpdate(pos, updated);
		}
		return true;
	}

	@Override
	public BlockEntityType<? extends FluidPipeBlockEntity> getBlockEntityType() {
		return material == FluidPartMaterial.GLASS ? ModBlockEntities.GLASS_PIPE.get() : ModBlockEntities.RATED_PIPE.get();
	}

	/**
	 * An iron or steel pipe: it rusts by its {@link Patina} as iron parts do, only where it gets wet, steel only to its
	 * first stage, and iron rusted through leaks ({@link Leaks}). Its states are made before it knows its material, so the
	 * patina comes with the class.
	 */
	public static class Rusting extends RatedPipeBlock {
		protected Rusting(Properties properties, FluidPartMaterial material) {
			super(properties, material);
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			super.createBlockStateDefinition(builder);
			builder.add(Patina.PROPERTY);
		}
	}

	/** A brass pipe, six pixels across to Create's eight; its model is thinned to match ({@code ThinPipeModel}). */
	public static class Thin extends RatedPipeBlock {
		/** Half its width, as vanilla's pipes measure it. */
		public static final float APOTHEM = 3 / 16f;
		private static final VoxelShape[] SHAPES = shapes(APOTHEM);

		protected Thin(Properties properties, FluidPartMaterial material) {
			super(properties, material);
		}

		@Override
		public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
			return SHAPES[getAABBIndex(state)];
		}

		/** Vanilla's pipe shapes, which it only makes for the width it is built with: a core, and an arm to each side. */
		private static VoxelShape[] shapes(float apothem) {
			Direction[] directions = Direction.values();
			VoxelShape core = Shapes.box(.5 - apothem, .5 - apothem, .5 - apothem, .5 + apothem, .5 + apothem,
				.5 + apothem);
			VoxelShape[] arms = new VoxelShape[directions.length];
			for (int i = 0; i < directions.length; i++) {
				Direction d = directions[i];
				arms[i] = Shapes.box(.5 + Math.min(-apothem, d.getStepX() * .5), .5 + Math.min(-apothem, d.getStepY() * .5),
					.5 + Math.min(-apothem, d.getStepZ() * .5), .5 + Math.max(apothem, d.getStepX() * .5),
					.5 + Math.max(apothem, d.getStepY() * .5), .5 + Math.max(apothem, d.getStepZ() * .5));
			}
			VoxelShape[] shapes = new VoxelShape[1 << directions.length];
			for (int index = 0; index < shapes.length; index++) {
				VoxelShape shape = core;
				for (int i = 0; i < directions.length; i++)
					if ((index & 1 << i) != 0)
						shape = Shapes.or(shape, arms[i]);
				shapes[index] = shape.optimize();
			}
			return shapes;
		}
	}
}

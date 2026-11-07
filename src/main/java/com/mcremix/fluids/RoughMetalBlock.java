package com.mcremix.fluids;

import java.util.List;

import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Spilled molten metal, set. It keeps the height the metal had when it stopped, in eighths like snow layers, so a puddle
 * hardens in steps down from where it landed, and it holds the metal that went into it: mined with a pickaxe, it gives
 * that back as ingots and nuggets.
 */
public class RoughMetalBlock extends Block implements IBE<RoughMetalBlockEntity> {
	public static final IntegerProperty LAYERS = BlockStateProperties.LAYERS;
	/** A full block's worth of molten metal is one source block, 1000 mB. */
	public static final int MB_PER_LAYER = MoltenMetal.SOURCE / 8;
	private static final VoxelShape[] SHAPES = new VoxelShape[9];

	static {
		for (int i = 1; i <= 8; i++)
			SHAPES[i] = Block.box(0, 0, 0, 16, i * 2, 16);
	}

	public final MoltenMetal metal;

	public RoughMetalBlock(Properties properties, MoltenMetal metal) {
		super(properties);
		this.metal = metal;
		registerDefaultState(defaultBlockState().setValue(LAYERS, 8));
	}

	/** How many layers a lump of {@code amount} mB stands, at least one. */
	public static int layersFor(int amount) {
		return Mth.clamp(Mth.ceil(amount / (float) MB_PER_LAYER), 1, 8);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LAYERS);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(LAYERS)];
	}

	@Override
	public boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return type == PathComputationType.LAND && state.getValue(LAYERS) < 5;
	}

	/** Its metal, as ingots and nuggets; it has no item of its own. */
	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		int amount = be instanceof RoughMetalBlockEntity rough ? rough.amount
			: state.getValue(LAYERS) * MB_PER_LAYER;
		return metal.asItems(amount);
	}

	@Override
	public Class<RoughMetalBlockEntity> getBlockEntityClass() {
		return RoughMetalBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends RoughMetalBlockEntity> getBlockEntityType() {
		return ModBlockEntities.ROUGH_METAL.get();
	}
}

package com.mcremix.kiln;

import com.mcremix.heat.HeatLevel;
import com.mcremix.registry.ModBlockEntities;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModTags;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.fluids.FluidUtil;

/**
 * Brick kiln. Front face has two openings: the low firebox (fuel) and the chamber above it (things to fire).
 * Clicking the firebox opening loads fuel, anywhere else loads the chamber; an empty hand takes finished items,
 * and sneaking with an empty hand takes back unfired ones. Flint and steel or a fire charge lights it, and a shovel
 * puts it out, as with a campfire ({@link KilnBlockEntity#light()}).
 */
public class KilnBlock extends HorizontalDirectionalBlock implements IBE<KilnBlockEntity> {
	public static final IntegerProperty HEAT = IntegerProperty.create("heat", 0, 4);
	/** Its own fire is burning: lit, and burning its fuel ({@link KilnBlockEntity#isLit()}). */
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	/**
	 * Whether the chimney on top stands clear. A boiler or any block resting where it would be caps it: the model
	 * leaves it out and the smoke vents out of the back instead.
	 */
	public static final BooleanProperty CHIMNEY = BooleanProperty.create("chimney");
	/** Height of the top of the firebox opening, in pixels. */
	private static final double FIREBOX_TOP = 6 / 16.0;

	public KilnBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(HEAT, 0)
			.setValue(CHIMNEY, true).setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, HEAT, CHIMNEY, LIT);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		Direction facing = context.getHorizontalDirection().getOpposite();
		BlockPos above = context.getClickedPos().above();
		return defaultBlockState().setValue(FACING, facing)
			.setValue(CHIMNEY, chimneyClear(level, above, level.getBlockState(above), facing));
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
		BlockPos pos, BlockPos neighbourPos) {
		if (direction == Direction.UP)
			return state.setValue(CHIMNEY, chimneyClear(level, neighbourPos, neighbour, state.getValue(FACING)));
		return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
	}

	/**
	 * Whether the block above leaves room for the chimney: a 4 px square stub, 4 px high, centred 4 px behind the
	 * middle of the top (the model's chimney).
	 */
	public static boolean chimneyClear(BlockGetter level, BlockPos above, BlockState aboveState, Direction facing) {
		if (aboveState.isAir())
			return true;
		Direction back = facing.getOpposite();
		double x = 8 + 4 * back.getStepX(), z = 8 + 4 * back.getStepZ();
		return !Shapes.joinIsNotEmpty(aboveState.getShape(level, above), box(x - 2, 0, z - 2, x + 2, 4, z + 2),
			BooleanOp.AND);
	}

	public static int lightLevel(BlockState state) {
		return HeatLevel.byId(state.getValue(HEAT)).lightLevel();
	}

	/** Create's boilers read a kiln's heat through this; see {@link KilnBlockEntity#boilerHeat()}. */
	public static float boilerHeat(Level level, BlockPos pos, BlockState state) {
		return level.getBlockEntity(pos) instanceof KilnBlockEntity kiln ? kiln.boilerHeat() : BoilerHeater.NO_HEAT;
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		ItemStack held = player.getItemInHand(hand);
		if (level.isClientSide)
			return InteractionResult.SUCCESS;

		// steel casing used on a Furnace cases it in steel
		if (ModBlocks.STEEL_CASING.isIn(held) && state.is(ModBlocks.KILN.get())) {
			SteelFurnaceBlock.upgrade(level, pos, state);
			if (!player.isCreative())
				held.shrink(1);
			return InteractionResult.SUCCESS;
		}

		return onBlockEntityUse(level, pos, kiln -> {
			// flint and steel or a fire charge lights it, if there is fuel in it to catch
			if (held.is(ModTags.Items.FURNACE_LIGHTERS)) {
				if (kiln.isLit())
					return InteractionResult.SUCCESS;
				boolean flint = held.isDamageableItem();
				level.playSound(null, pos, flint ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.FIRECHARGE_USE,
					SoundSource.BLOCKS, 1, level.random.nextFloat() * 0.4f + 0.8f);
				if (!kiln.light()) {
					player.displayClientMessage(Component.translatable(kiln.burnsSolidFuel()
						? "mcremix.furnace.nothing_to_light" : "mcremix.steel_furnace.nothing_to_light"), true);
					return InteractionResult.SUCCESS;
				}
				if (flint)
					held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
				else if (!player.isCreative())
					held.shrink(1);
				return InteractionResult.SUCCESS;
			}
			// a shovel puts it out, leaving its fuel in
			if (kiln.isLit() && held.canPerformAction(ToolActions.SHOVEL_DIG)) {
				kiln.douse();
				held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
				return InteractionResult.SUCCESS;
			}

			if (held.isEmpty()) {
				ItemStack taken = player.isShiftKeyDown() ? kiln.takeInput() : kiln.takeOutput();
				if (taken.isEmpty())
					return InteractionResult.PASS;
				player.getInventory().placeItemBackInInventory(taken);
				level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.3f, 1.2f);
				return InteractionResult.SUCCESS;
			}

			// a bucket draws off what firing gave off, coal's tar among it; a lava bucket is fuel, so it goes in the fire
			if (!KilnFuels.isFuel(held) && FluidUtil.getFluidHandler(held).isPresent()
				&& FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection()))
				return InteractionResult.SUCCESS;

			// a Steel Furnace has no firebox to feed: whatever it can fire goes in the chamber, wherever it is clicked
			boolean solidFuel = kiln.burnsSolidFuel();
			boolean clickedFirebox = solidFuel && hit.getDirection() == state.getValue(FACING)
				&& hit.getLocation().y - pos.getY() < FIREBOX_TOP;
			boolean fireable = kiln.canFire(held);
			boolean fuel = solidFuel && KilnFuels.isFuel(held);

			ItemStack rest = held;
			if ((clickedFirebox || !fireable) && fuel) {
				rest = kiln.insertFuel(held.copy());
				if (rest.getCount() != held.getCount())
					level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.25f, 1.6f);
			} else if (fireable) {
				rest = kiln.insertIntoChamber(held.copy());
				if (rest.getCount() != held.getCount())
					level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.6f, 0.8f);
			} else {
				if (!solidFuel && KilnFuels.isFuel(held))
					player.displayClientMessage(Component.translatable("mcremix.steel_furnace.piped_fuel_only"), true);
				return InteractionResult.PASS;
			}

			if (rest.getCount() == held.getCount())
				return InteractionResult.PASS;
			if (!player.isCreative())
				player.setItemInHand(hand, rest);
			return InteractionResult.SUCCESS;
		});
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		if (state.getValue(HEAT) >= HeatLevel.HOT.ordinal() && !entity.isSteppingCarefully()
			&& entity instanceof LivingEntity)
			entity.hurt(level.damageSources().hotFloor(), 1.0F);
		super.stepOn(level, pos, state, entity);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.is(newState.getBlock()))
			withBlockEntityDo(level, pos, KilnBlockEntity::dropContents);
		IBE.onRemove(state, level, pos, newState);
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	/** Comparators read how hot the kiln is (0 to 4). */
	@Override
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return state.getValue(HEAT);
	}

	/** World position of a chamber slot, used by the renderer and particles. */
	public static Vec3 chamberSlotPosition(BlockPos pos, Direction facing, int slot) {
		Vec3 front = Vec3.atLowerCornerOf(facing.getNormal());
		Vec3 right = Vec3.atLowerCornerOf(facing.getClockWise().getNormal());
		double sideways = slot % 2 == 0 ? -0.17 : 0.17;
		double forward = slot < 2 ? 0.12 : -0.14;
		return Vec3.atCenterOf(pos)
			.add(front.scale(forward))
			.add(right.scale(sideways))
			.add(0, 7 / 16.0 - 0.5, 0);
	}

	@Override
	public Class<KilnBlockEntity> getBlockEntityClass() {
		return KilnBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends KilnBlockEntity> getBlockEntityType() {
		return ModBlockEntities.KILN.get();
	}
}

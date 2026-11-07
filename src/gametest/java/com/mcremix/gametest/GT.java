package com.mcremix.gametest;

import com.mcremix.kinetics.FrictionPoints;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.bracket.BracketBlock;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Shared helpers. Every test uses the "platform" template: a 16x16 stone floor at y=1, so things are built from y=2.
 */
public final class GT {
	public static final String PLATFORM = "platform";

	private GT() {}

	public static BlockPos p(int x, int y, int z) {
		return new BlockPos(x, y, z);
	}

	public static TestPlayer player(GameTestHelper h, BlockPos standAt) {
		TestPlayer player = new TestPlayer(h.getLevel());
		Vec3 at = h.absoluteVec(Vec3.atBottomCenterOf(standAt));
		player.moveTo(at.x, at.y, at.z, 0, 0);
		return player;
	}

	/**
	 * Right-clicks a block face the way a real click reaches the server, Forge events included.
	 *
	 * @param height for a side face, how far up the face the click lands (0 to 1)
	 */
	public static InteractionResult click(GameTestHelper h, ServerPlayer player, BlockPos rel, Direction face,
		double height, ItemStack held) {
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		BlockPos abs = h.absolutePos(rel);
		double x = abs.getX() + .5 + face.getStepX() * .5;
		double z = abs.getZ() + .5 + face.getStepZ() * .5;
		double y = face == Direction.UP ? abs.getY() + 1 : face == Direction.DOWN ? abs.getY() : abs.getY() + height;
		BlockHitResult hit = new BlockHitResult(new Vec3(x, y, z), face, abs, false);
		return player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	public static int count(ServerPlayer player, net.minecraft.world.item.Item item) {
		return player.getInventory().countItem(item);
	}

	/** A creative motor whose shaft points {@code facing}, spinning at {@code rpm}. */
	public static void motor(GameTestHelper h, BlockPos rel, Direction facing, int rpm) {
		h.setBlock(rel, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(BlockStateProperties.FACING, facing));
		CreativeMotorBlockEntity motor = (CreativeMotorBlockEntity) h.getBlockEntity(rel);
		motor.generatedSpeed.setValue(rpm);
	}

	public static void shaftX(GameTestHelper h, BlockPos rel, Block shaft) {
		h.setBlock(rel, shaft.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
	}

	/** Bolts a bracket of {@code bracket}'s kind onto the shaft or cog at {@code rel}, its plate on the block below. */
	public static void bracket(GameTestHelper h, BlockPos rel, Block bracket) {
		BlockState part = h.getBlockState(rel);
		Optional<BlockState> fitted = ((BracketBlock) bracket).getSuitableBracket(part, Direction.UP);
		BracketedBlockEntityBehaviour behaviour = FrictionPoints.brackets(h.getLevel(), h.absolutePos(rel));
		if (fitted.isEmpty() || behaviour == null || !behaviour.canHaveBracket())
			h.fail("a " + bracket.getDescriptionId() + " cannot go on " + part, rel);
		behaviour.applyBracket(fitted.get());
	}

	/** A shaft or cog along x with a bracket bolted to the floor under it. */
	public static void bracketedX(GameTestHelper h, BlockPos rel, Block part, Block bracket) {
		shaftX(h, rel, part);
		bracket(h, rel, bracket);
	}

	/** The bracket on the part at {@code rel}, or null. */
	@Nullable
	public static BlockState bracketOn(GameTestHelper h, BlockPos rel) {
		return FrictionPoints.bracket(h.getLevel(), h.absolutePos(rel));
	}

	public static KineticBlockEntity kinetic(GameTestHelper h, BlockPos rel) {
		if (h.getBlockEntity(rel) instanceof KineticBlockEntity kbe)
			return kbe;
		h.fail("no kinetic block entity", rel);
		return null;
	}

	public static void assertIs(GameTestHelper h, BlockPos rel, Block block) {
		BlockState state = h.getBlockState(rel);
		h.assertTrue(state.is(block), "expected " + block.getDescriptionId() + " at " + rel + " but found "
			+ state.getBlock().getDescriptionId());
	}

	public static void assertNotIs(GameTestHelper h, BlockPos rel, Block block) {
		BlockState state = h.getBlockState(rel);
		h.assertFalse(state.is(block), "expected " + block.getDescriptionId() + " at " + rel + " to be gone");
	}

	/** What a 3x3 crafting grid makes, or an empty stack. Cells go row by row; null is an empty cell. */
	public static ItemStack craft(GameTestHelper h, ItemLike... grid) {
		CraftingContainer container = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
			@Override
			public ItemStack quickMoveStack(Player player, int slot) {
				return ItemStack.EMPTY;
			}

			@Override
			public boolean stillValid(Player player) {
				return true;
			}
		}, 3, 3);
		for (int i = 0; i < grid.length; i++)
			if (grid[i] != null)
				container.setItem(i, new ItemStack(grid[i]));
		Level level = h.getLevel();
		return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, container, level)
			.map(recipe -> recipe.assemble(container, level.registryAccess()))
			.orElse(ItemStack.EMPTY);
	}

	/** Breaks a block the way a survival player holding {@code tool} does, and returns (and removes) what it dropped. */
	public static List<ItemStack> breakWith(GameTestHelper h, BlockPos rel, ItemStack tool) {
		TestPlayer player = player(h, rel);
		player.setItemInHand(InteractionHand.MAIN_HAND, tool);
		AABB box = new AABB(h.absolutePos(rel)).inflate(1.5);
		Set<ItemEntity> before = new HashSet<>(h.getLevel().getEntitiesOfClass(ItemEntity.class, box));
		player.gameMode.destroyBlock(h.absolutePos(rel));
		List<ItemStack> drops = new ArrayList<>();
		for (ItemEntity item : h.getLevel().getEntitiesOfClass(ItemEntity.class, box))
			if (!before.contains(item)) {
				drops.add(item.getItem().copy());
				item.discard();
			}
		return drops;
	}
}

package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.item.IronBloomItem;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class BloomTests {

	@GameTest(template = GT.PLATFORM)
	public static void bloomFourStrikesOnAnvil(GameTestHelper h) {
		BlockPos anvil = p(4, 2, 4);
		h.setBlock(anvil, Blocks.ANVIL);
		TestPlayer player = GT.player(h, p(4, 2, 2));

		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.IRON_BLOOM.get(), 2));
		for (int strike = 1; strike <= 3; strike++) {
			GT.click(h, player, anvil, Direction.UP, 0, player.getMainHandItem());
			ItemStack hand = player.getMainHandItem();
			h.assertTrue(hand.is(ModItems.IRON_BLOOM.get()) && hand.getCount() == 1 && IronBloomItem.getStrikes(hand) == strike,
				"after strike " + strike + " the hand should hold one bloom struck " + strike + " times, holds " + hand + " " + hand.getTag());
			h.assertTrue(GT.count(player, ModItems.IRON_BLOOM.get()) == 2, "the other bloom should wait in the inventory");
		}
		GT.click(h, player, anvil, Direction.UP, 0, player.getMainHandItem());
		h.assertTrue(GT.count(player, Items.IRON_INGOT) == 1, "the fourth strike should beat out an ingot");
		h.assertTrue(GT.count(player, ModItems.IRON_BLOOM.get()) == 1, "one unworked bloom should be left");
		h.assertTrue(GT.count(player, ModItems.SLAG.get()) <= 1, "at most one slag per bloom");

		// over many blooms about half leave slag, and the anvil only wears now and then
		int slag = 0, ingots = 0;
		for (int bloom = 0; bloom < 60; bloom++) {
			h.setBlock(anvil, Blocks.ANVIL);
			TestPlayer smith = GT.player(h, p(4, 2, 2));
			smith.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.IRON_BLOOM.get()));
			for (int strike = 0; strike < 4; strike++)
				GT.click(h, smith, anvil, Direction.UP, 0, smith.getMainHandItem());
			ingots += GT.count(smith, Items.IRON_INGOT);
			slag += GT.count(smith, ModItems.SLAG.get());
			h.assertTrue(h.getBlockState(anvil).is(BlockTags.ANVIL) || h.getBlockState(anvil).isAir(), "anvil state");
		}
		h.assertTrue(ingots == 60, "every bloom should give one ingot, got " + ingots + " from 60");
		h.assertTrue(slag >= 15 && slag <= 45, "about half the blooms should leave slag, got " + slag + " of 60");
		h.succeed();
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void bloomUnderMechanicalPress(GameTestHelper h) {
		// the press sits two blocks above the depot, its head reaches down into the gap
		BlockPos depot = p(4, 2, 4), press = p(4, 4, 4);
		h.setBlock(depot, AllBlocks.DEPOT.getDefaultState());
		h.setBlock(press, AllBlocks.MECHANICAL_PRESS.getDefaultState()
			.setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.EAST));
		GT.motor(h, p(3, 4, 4), Direction.EAST, 64);
		DepotBlockEntity d = (DepotBlockEntity) h.getBlockEntity(depot);
		d.setHeldItem(new ItemStack(ModItems.IRON_BLOOM.get()));
		h.succeedWhen(() -> h.assertTrue(d.getHeldItem().is(Items.IRON_INGOT),
			"waiting for the press to beat the bloom into an ingot; depot holds " + d.getHeldItem() + ", press speed "
				+ GT.kinetic(h, press).getSpeed()));
	}

	@GameTest(template = GT.PLATFORM)
	public static void slagFertilisesLikeBoneMeal(GameTestHelper h) {
		BlockPos crop = p(4, 2, 4);
		h.setBlock(p(4, 1, 4), Blocks.FARMLAND);
		h.setBlock(crop, Blocks.WHEAT);
		TestPlayer player = GT.player(h, p(4, 2, 2));
		GT.click(h, player, crop, Direction.UP, 0, new ItemStack(ModItems.SLAG.get(), 3));
		int age = h.getBlockState(crop).getValue(CropBlock.AGE);
		h.assertTrue(age > 0, "slag should make the wheat grow, age is " + age);
		h.assertTrue(player.getMainHandItem().getCount() == 2, "one slag should be used up, left " + player.getMainHandItem());

		BlockPos sapling = p(10, 2, 10);
		h.setBlock(p(10, 1, 10), Blocks.DIRT);
		h.setBlock(sapling, Blocks.OAK_SAPLING);
		for (int i = 0; i < 20 && h.getBlockState(sapling).is(Blocks.OAK_SAPLING); i++)
			GT.click(h, player, sapling, Direction.UP, 0, new ItemStack(ModItems.SLAG.get(), 1));
		h.assertFalse(h.getBlockState(sapling).is(Blocks.OAK_SAPLING), "slag should grow a sapling into a tree");
		// slag roads were taken out in 0.20.0: on a path it does nothing
		BlockPos path = p(4, 2, 10);
		h.setBlock(path, Blocks.DIRT_PATH);
		GT.click(h, player, path, Direction.UP, 0, new ItemStack(ModItems.SLAG.get(), 2));
		GT.assertIs(h, path, Blocks.DIRT_PATH);
		h.assertTrue(player.getMainHandItem().getCount() == 2, "slag on a path should do nothing");
		h.succeed();
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 600)
	public static void slagWashesIntoNuggets(GameTestHelper h) {
		BlockPos motor = p(1, 2, 4), fan = p(2, 2, 4);
		GT.motor(h, motor, Direction.EAST, 128);
		h.setBlock(fan, AllBlocks.ENCASED_FAN.getDefaultState().setValue(BlockStateProperties.FACING, Direction.EAST));
		// a waterlogged sign lets the air through and makes it a washing current
		h.setBlock(p(3, 2, 4), Blocks.OAK_SIGN.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
		for (int x = 3; x <= 10; x++) {
			h.setBlock(p(x, 2, 3), Blocks.GLASS);
			h.setBlock(p(x, 2, 5), Blocks.GLASS);
		}
		h.setBlock(p(10, 2, 4), Blocks.GLASS);

		h.startSequence()
			.thenExecuteAfter(10, () -> {
				EncasedFanBlockEntity be = (EncasedFanBlockEntity) h.getBlockEntity(fan);
				if (be.getAirFlowDirection() == Direction.WEST)
					((CreativeMotorBlockEntity) h.getBlockEntity(motor)).generatedSpeed.setValue(-128);
			})
			.thenExecuteAfter(10, () -> {
				EncasedFanBlockEntity be = (EncasedFanBlockEntity) h.getBlockEntity(fan);
				h.assertTrue(be.getAirFlowDirection() == Direction.EAST, "fan should blow east, blows " + be.getAirFlowDirection());
				Vec3 at = h.absoluteVec(new Vec3(6.5, 2.2, 4.5));
				ItemEntity slag = new ItemEntity(h.getLevel(), at.x, at.y, at.z, new ItemStack(ModItems.SLAG.get(), 16));
				slag.setDeltaMovement(Vec3.ZERO);
				h.getLevel().addFreshEntity(slag);
			})
			.thenWaitUntil(() -> {
				AABB area = new AABB(h.absolutePos(p(3, 2, 3)), h.absolutePos(p(11, 4, 6)));
				List<ItemEntity> items = h.getLevel().getEntitiesOfClass(ItemEntity.class, area);
				boolean nugget = items.stream().anyMatch(e -> e.getItem().is(Items.IRON_NUGGET));
				h.assertTrue(nugget, "waiting for iron nuggets from washed slag; items in the current: "
					+ items.stream().map(e -> e.getItem().toString()).toList());
			})
			.thenSucceed();
	}
}

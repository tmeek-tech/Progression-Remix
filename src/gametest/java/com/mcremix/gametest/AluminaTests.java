package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mcremix.McRemix;
import com.mcremix.fluids.Chemical;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kiln.KilnBlockEntity;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** The Bayer process, from bauxite to alumina. */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class AluminaTests {
	/** Two bauxite in hot caustic soda, under a running mixer over a fed blaze burner, dissolve into liquor and red mud. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void bauxiteDissolvesInHotCausticSoda(GameTestHelper h) {
		BlockPos pos = p(4, 3, 4);
		KilnTests.fedBurner(h, pos.below(), new ItemStack(Items.COAL));
		h.setBlock(pos, AllBlocks.BASIN.getDefaultState());
		BasinBlockEntity basin = (BasinBlockEntity) h.getBlockEntity(pos);
		basin.getInputInventory().insertItem(0, ModItems.BAUXITE.asStack(2), false);
		IFluidHandler tanks = basin.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(IllegalStateException::new);
		tanks.fill(Chemical.CAUSTIC_SODA.stack(250), FluidAction.EXECUTE);
		// the mixer two above the basin, turned by a cogwheel beside it on a creative motor
		GT.motor(h, p(5, 6, 4), Direction.DOWN, 64);
		h.setBlock(p(5, 5, 4), AllBlocks.COGWHEEL.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
		h.setBlock(p(4, 5, 4), AllBlocks.MECHANICAL_MIXER.getDefaultState());
		h.succeedWhen(() -> {
			FluidStack liquor = tanks.drain(Chemical.BAUXITE_LIQUOR.stack(1000), FluidAction.SIMULATE);
			h.assertTrue(liquor.getAmount() == 250, "the basin should hold 250 mB of liquor, holds " + liquor.getAmount());
			h.assertTrue(KilnBasinTests.count(basin.getInputInventory(), ModItems.BAUXITE.get()) == 0,
				"the bauxite should be used up");
			h.assertTrue(KilnBasinTests.count(basin.getOutputInventory(), ModItems.RED_MUD.get()) == 1,
				"the bauxite should leave red mud behind");
		});
	}

	/**
	 * Red mud fires Warm into red mud bricks. Four make a block, which crafts and cuts into stairs, slabs and walls, and
	 * a double slab breaks into two slabs.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void redMudFiresIntoBricks(GameTestHelper h) {
		KilnTests.expect(h, ModItems.RED_MUD.get(), ModItems.RED_MUD_BRICK.get(), HeatLevel.WARM);
		Item brick = ModItems.RED_MUD_BRICK.get();
		Block bricks = ModBlocks.RED_MUD_BRICKS.get(), slab = ModBlocks.RED_MUD_BRICK_SLAB.get();
		Item stairs = ModBlocks.RED_MUD_BRICK_STAIRS.get().asItem(), wall = ModBlocks.RED_MUD_BRICK_WALL.get().asItem();
		ItemStack made = GT.craft(h, brick, brick, null, brick, brick);
		h.assertTrue(made.is(bricks.asItem()), "four red mud bricks should make a block, made " + made);
		made = GT.craft(h, bricks, null, null, bricks, bricks, null, bricks, bricks, bricks);
		h.assertTrue(made.is(stairs) && made.getCount() == 4, "six blocks should make four stairs, made " + made);
		made = GT.craft(h, bricks, bricks, bricks);
		h.assertTrue(made.is(slab.asItem()) && made.getCount() == 6, "three blocks should make six slabs, made " + made);
		made = GT.craft(h, bricks, bricks, bricks, bricks, bricks, bricks);
		h.assertTrue(made.is(wall) && made.getCount() == 6, "six blocks should make six walls, made " + made);

		Set<Item> cut = new HashSet<>();
		for (StonecutterRecipe recipe : h.getLevel().getRecipeManager().getRecipesFor(RecipeType.STONECUTTING,
			new SimpleContainer(new ItemStack(bricks)), h.getLevel()))
			cut.add(recipe.getResultItem(h.getLevel().registryAccess()).getItem());
		h.assertTrue(cut.containsAll(List.of(stairs, slab.asItem(), wall)),
			"a stonecutter should cut the block into stairs, slabs and walls, cuts " + cut);

		// a few ticks in: on a test's first tick its chunk's entities may not be loaded yet, and a search finds no drops
		h.runAtTickTime(10, () -> {
			BlockPos pos = p(4, 2, 4);
			h.setBlock(pos, slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE));
			List<ItemStack> drops = GT.breakWith(h, pos, new ItemStack(Items.WOODEN_PICKAXE));
			h.assertTrue(drops.stream().allMatch(d -> d.is(slab.asItem()))
				&& drops.stream().mapToInt(ItemStack::getCount).sum() == 2, "a double slab should drop two slabs, dropped " + drops);
			h.succeed();
		});
	}

	/** Aluminium hydroxide fires into alumina in a Furnace made Searing by a soul campfire under it. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 700)
	public static void hydroxideFiresIntoAlumina(GameTestHelper h) {
		BlockPos pos = p(4, 3, 4);
		h.setBlock(pos.below(), Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
		h.setBlock(pos, ModBlocks.KILN.getDefaultState());
		KilnBlockEntity kiln = KilnTests.kiln(h, pos);
		kiln.insertIntoChamber(ModItems.ALUMINIUM_HYDROXIDE.asStack(2));
		h.succeedWhen(() -> h.assertTrue(KilnTests.trayCount(kiln, ModItems.ALUMINA.get()) == 2,
			"both should fire into alumina, the tray has " + KilnTests.trayCount(kiln, ModItems.ALUMINA.get())));
	}
}

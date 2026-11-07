package com.mcremix.gametest;

import static com.mcremix.gametest.ElectrolysisTests.amount;
import static com.mcremix.gametest.ElectrolysisTests.fluids;
import static com.mcremix.gametest.ElectrolysisTests.smallVat;
import static com.mcremix.gametest.ElectrolysisTests.vat;
import static com.mcremix.gametest.GT.p;
import static com.mcremix.gametest.MachineTierTests.tier;

import java.util.List;
import java.util.Map;

import com.mcremix.McRemix;
import com.mcremix.electrolysis.Vat;
import com.mcremix.electrolysis.VatMixerBlock;
import com.mcremix.electrolysis.VatMixerBlockEntity;
import com.mcremix.electrolysis.VatMixerBlockEntity.Doing;
import com.mcremix.fluids.Chemical;
import com.mcremix.fluids.ChlorineCloud;
import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.HydrogenChlorideMist;
import com.mcremix.fluids.Patina;
import com.mcremix.kinetics.FrictionPoints;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.machines.MachineKind;
import com.mcremix.machines.MachineTiers;
import com.mcremix.registry.ModFluidBlocks;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlock.Shape;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Basins are open and vats are closed: gas piped into a basin goes off into the air, while a vat holds several fluids
 * at once in layers, and a mixer on its side, in its vat form, stirs them.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class VatTests {
	/** Chlorine piped into a basin rises off it as a cloud; gas already standing in it goes too; water stays. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void aBasinLetsGasGo(GameTestHelper h) {
		BlockPos at = p(4, 2, 4);
		BasinBlockEntity basin = ElectrolysisTests.basin(h, at, new FluidStack(Fluids.WATER, 500));
		IFluidHandler pipe = ElectrolysisTests.fluids(basin);
		h.assertTrue(pipe.fill(Chemical.CHLORINE.stack(500), FluidAction.EXECUTE) == 500,
			"a basin should take the gas a pipe pushes into it");
		h.assertTrue(ElectrolysisTests.amount(pipe, Chemical.CHLORINE) == 0, "a basin should never hold chlorine");
		// as if kept from a world before basins let gas go
		basin.inputTank.getCapability().ifPresent(t -> t.fill(Chemical.HYDROGEN_CHLORIDE.stack(250), FluidAction.EXECUTE));
		h.assertTrue(ElectrolysisTests.amount(pipe, Chemical.HYDROGEN_CHLORIDE) == 250, "the old gas should be in at first");
		h.runAtTickTime(30, () -> {
			h.assertTrue(ElectrolysisTests.amount(pipe, Chemical.HYDROGEN_CHLORIDE) == 0,
				"gas standing in a basin should go off into the air");
			h.assertTrue(ElectrolysisTests.amount(pipe, new FluidStack(Fluids.WATER, 1)) == 500, "the water should stay");
			AABB over = new AABB(h.absolutePos(at)).inflate(3);
			h.assertFalse(h.getLevel().getEntitiesOfClass(ChlorineCloud.class, over).isEmpty(),
				"the chlorine should hang over the basin as a cloud");
			h.assertFalse(h.getLevel().getEntitiesOfClass(HydrogenChlorideMist.class, over).isEmpty(),
				"the hydrogen chloride should hang over the basin as a mist");
			h.succeed();
		});
	}

	static FluidStack water(int amount) {
		return new FluidStack(Fluids.WATER, amount);
	}

	/**
	 * A vat holds four fluids at once in layers sharing its room, and no fifth: drawn with the heaviest liquid lowest
	 * and the lightest gas highest. A pipe draws off one it has no use for.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void aVatHoldsFourFluidsInLayers(GameTestHelper h) {
		BlockPos tank = p(4, 3, 4);
		smallVat(h, tank.below());
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(vat(h, tank) != null, "the vat should form"))
			.thenExecute(() -> {
				IFluidHandler pipes = fluids(h.getBlockEntity(tank));
				h.assertTrue(pipes.fill(water(2000), FluidAction.EXECUTE) == 2000, "it should take water");
				h.assertTrue(pipes.fill(Chemical.CHLORINE.stack(1000), FluidAction.EXECUTE) == 1000, "and chlorine");
				h.assertTrue(pipes.fill(Chemical.FERRIC_CHLORIDE.stack(500), FluidAction.EXECUTE) == 500, "and ferric chloride");
				h.assertTrue(pipes.fill(Chemical.ETHYLENE.stack(1000), FluidAction.EXECUTE) == 1000, "and ethylene");
				h.assertTrue(pipes.fill(Chemical.ETHANOL.stack(500), FluidAction.EXECUTE) == 0, "but not a fifth fluid");
				int room = pipes.getTankCapacity(0) - 4500;
				h.assertTrue(pipes.fill(water(8000), FluidAction.EXECUTE) == room,
					"more water should only fill the room the four leave, " + room + " mB");
				List<Vat.Layer> layers = vat(h, tank).layers();
				List<FluidStack> order = layers.stream().map(Vat.Layer::fluid).toList();
				h.assertTrue(order.size() == 4 && order.get(0).isFluidEqual(Chemical.FERRIC_CHLORIDE.stack(1))
					&& order.get(1).isFluidEqual(water(1)) && order.get(2).isFluidEqual(Chemical.ETHYLENE.stack(1))
					&& order.get(3).isFluidEqual(Chemical.CHLORINE.stack(1)),
					"ferric chloride should lie under the water, and ethylene hang over the chlorine, drawn as "
						+ order.stream().map(f -> f.getDisplayName().getString()).toList());
				h.assertTrue(!layers.get(1).fromTop() && layers.get(2).fromTop(), "liquids rest, gases hang");
				FluidStack drawn = pipes.drain(Chemical.CHLORINE.stack(1000), FluidAction.EXECUTE);
				h.assertTrue(drawn.getAmount() == 1000, "a pipe should draw off the chlorine the vat has no use for");
				h.assertTrue(pipes.fill(Chemical.ETHANOL.stack(500), FluidAction.EXECUTE) == 500,
					"with a layer free it should take another fluid");
			})
			.thenSucceed();
	}

	/**
	 * A vat saves everything it holds with its tank: loaded again, it has back the layers past the tank's own fluid.
	 * Until 0.16.0 it only ever sent them to players, so a reload lost all but the tank's own fluid.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void aVatKeepsItsLayersThroughASave(GameTestHelper h) {
		BlockPos tank = p(4, 3, 4);
		smallVat(h, tank.below());
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(vat(h, tank) != null, "the vat should form"))
			.thenExecute(() -> {
				BlockEntity be = h.getBlockEntity(tank);
				IFluidHandler pipes = fluids(be);
				pipes.fill(water(2000), FluidAction.EXECUTE);
				pipes.fill(Chemical.CHLORINE.stack(1000), FluidAction.EXECUTE);
				pipes.fill(Chemical.ETHYLENE.stack(500), FluidAction.EXECUTE);
				CompoundTag saved = be.saveWithoutMetadata();
				h.assertTrue(saved.getCompound("Vat").contains("Layers"), "the vat's layers should be saved");
				pipes.drain(Chemical.CHLORINE.stack(1000), FluidAction.EXECUTE);
				pipes.drain(Chemical.ETHYLENE.stack(500), FluidAction.EXECUTE);
				be.load(saved);
				IFluidHandler loaded = fluids(h.getBlockEntity(tank));
				h.assertTrue(amount(loaded, Chemical.CHLORINE.stack(1)) == 1000
					&& amount(loaded, Chemical.ETHYLENE.stack(1)) == 500 && amount(loaded, water(1)) == 2000,
					"loaded again, it should hold its water, chlorine and ethylene");
			})
			.thenSucceed();
	}

	/**
	 * A standing vat is closed: its tank's windows shut as it forms, and a wrench does not open them. Taken off its
	 * scaffolding it opens them again, and a wrench shuts them as on any tank.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aStandingVatKeepsItsWindowsShut(GameTestHelper h) {
		BlockPos tank = p(4, 3, 4);
		smallVat(h, tank.below());
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(vat(h, tank) != null, "the vat should form"))
			.thenExecute(() -> {
				expectShape(h, tank, Shape.PLAIN, "a standing vat's windows should shut");
				h.assertTrue(wrench(h, tank) == InteractionResult.PASS, "a wrench should do nothing to a standing vat");
				expectShape(h, tank, Shape.PLAIN, "a wrench should not open a standing vat's windows");
				h.setBlock(tank.below(), Blocks.AIR);
			})
			.thenWaitUntil(() -> h.assertTrue(vat(h, tank) == null, "the vat should break up off its scaffolding"))
			.thenExecute(() -> {
				expectShape(h, tank, Shape.WINDOW, "taken apart, its windows should open again");
				wrench(h, tank);
				expectShape(h, tank, Shape.PLAIN, "a wrench should shut a tank's windows once it is no vat");
			})
			.thenSucceed();
	}

	/** A player using a wrench on the north side of the tank at {@code tank}. */
	static InteractionResult wrench(GameTestHelper h, BlockPos tank) {
		Player player = h.makeMockPlayer();
		player.setItemInHand(InteractionHand.MAIN_HAND, AllItems.WRENCH.asStack());
		BlockPos at = h.absolutePos(tank);
		return player.getMainHandItem().getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(at).add(0, 0, -.5), Direction.NORTH, at, false)));
	}

	static void expectShape(GameTestHelper h, BlockPos tank, Shape shape, String message) {
		Shape found = h.getBlockState(tank).getValue(FluidTankBlock.SHAPE);
		h.assertTrue(found == shape, message + ": its windows are " + found);
	}

	/** One of this mod's tanks on a block of andesite scaffolding: a small vat of its metal. */
	static void metalVat(GameTestHelper h, BlockPos scaffolding, FluidPartMaterial material) {
		h.setBlock(scaffolding, AllBlocks.ANDESITE_SCAFFOLD.getDefaultState());
		h.setBlock(scaffolding.above(), ModFluidBlocks.tank(material).get());
	}

	/** A Steel Fluid Tank on a block of andesite scaffolding: a small steel vat. */
	static void steelVat(GameTestHelper h, BlockPos scaffolding) {
		metalVat(h, scaffolding, FluidPartMaterial.STEEL);
	}

	/**
	 * A vat form fixed to the west of the tank, its cog turned at {@code rpm} by a cogwheel beside it to the north, on a
	 * motor.
	 */
	static VatMixerBlockEntity mixer(GameTestHelper h, BlockPos tank, Block form, int rpm) {
		BlockPos at = tank.west();
		h.setBlock(at, facingEast(form));
		GT.shaftX(h, at.north(), AllBlocks.COGWHEEL.get());
		GT.motor(h, at.north().west(), Direction.EAST, rpm);
		return (VatMixerBlockEntity) h.getBlockEntity(at);
	}

	/** A copper mixer in its vat form, on a copper vat, turned at {@code rpm} (copper tops out at 256). */
	static VatMixerBlockEntity mixer(GameTestHelper h, BlockPos tank, int rpm) {
		return mixer(h, tank, vatForm(KineticMaterial.COPPER), rpm);
	}

	static BlockState facingEast(Block form) {
		return form.defaultBlockState().setValue(VatMixerBlock.HORIZONTAL_FACING, Direction.EAST);
	}

	/** The vat form of the mixer of a material, or null if it has none. */
	static VatMixerBlock vatForm(KineticMaterial material) {
		return VatMixerBlock.formOf(MachineTiers.block(MachineKind.MIXER, material));
	}

	/** A player holding {@code mixer}, using it on the west side of the tank at {@code tank}. */
	static InteractionResult useMixerOn(GameTestHelper h, BlockPos tank, ItemStack mixer, boolean sneaking) {
		Player player = h.makeMockPlayer();
		player.setShiftKeyDown(sneaking);
		player.setItemInHand(InteractionHand.MAIN_HAND, mixer);
		BlockPos at = h.absolutePos(tank);
		return player.getMainHandItem().getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(at).add(-.5, 0, 0), Direction.WEST, at, false)));
	}

	/** A player holding Create's mixer, using it on the west side of the tank at {@code tank}. */
	static InteractionResult useMixerOn(GameTestHelper h, BlockPos tank, boolean sneaking) {
		return useMixerOn(h, tank, AllBlocks.MECHANICAL_MIXER.asStack(), sneaking);
	}

	/**
	 * A copper mixer used on the side of a copper tank goes on as its vat form, facing into the tank, called after the
	 * mixer and breaking back into it.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void aMixerUsedOnAVatGoesOnAsItsVatForm(GameTestHelper h) {
		BlockPos tank = p(6, 3, 4);
		smallVat(h, tank.below());
		Block copperMixer = tier(MachineKind.MIXER, KineticMaterial.COPPER);
		h.assertTrue(useMixerOn(h, tank, new ItemStack(copperMixer), false).consumesAction(), "the mixer should go on");
		BlockState placed = h.getBlockState(tank.west());
		h.assertTrue(placed.is(vatForm(KineticMaterial.COPPER))
			&& placed.getValue(VatMixerBlock.HORIZONTAL_FACING) == Direction.EAST,
			"it should go on as its vat form, facing the tank, not " + placed);
		h.assertTrue(placed.getBlock().getName().getString().equals(copperMixer.getName().getString()),
			"it should be called after the mixer, is " + placed.getBlock().getName().getString());
		List<ItemStack> drops = Block.getDrops(placed, h.getLevel(), h.absolutePos(tank.west()),
			h.getBlockEntity(tank.west()));
		h.assertTrue(drops.size() == 1 && drops.get(0).is(copperMixer.asItem()),
			"broken, it should give the mixer back, gives " + drops);
		h.succeed();
	}

	/** Create's own mixer has no vat form (Taran): used on a vat's side it goes down as an ordinary mixer. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void createsMixerHasNoVatForm(GameTestHelper h) {
		BlockPos tank = p(6, 3, 4);
		smallVat(h, tank.below());
		h.assertTrue(VatMixerBlock.formOf(AllBlocks.MECHANICAL_MIXER.get()) == null, "Create's mixer has no vat form");
		h.assertTrue(useMixerOn(h, tank, false).consumesAction(), "the mixer should go down");
		h.assertBlockPresent(AllBlocks.MECHANICAL_MIXER.get(), tank.west());
		h.succeed();
	}

	/**
	 * Brass tanks make vats as any tank does, but no mixer fits one (Taran): used on one a copper mixer does not go
	 * on, sneaking puts an ordinary mixer there, and one put on a brass vat anyway stirs nothing and says so.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void noMixerFitsABrassVat(GameTestHelper h) {
		BlockPos brass = p(6, 3, 3), other = p(6, 3, 9);
		metalVat(h, brass.below(), FluidPartMaterial.BRASS);
		metalVat(h, other.below(), FluidPartMaterial.BRASS);
		ItemStack copperMixer = new ItemStack(tier(MachineKind.MIXER, KineticMaterial.COPPER));
		h.assertFalse(useMixerOn(h, brass, copperMixer.copy(), false).consumesAction(),
			"the mixer should not go on a brass vat");
		h.assertBlockPresent(Blocks.AIR, brass.west());
		h.assertTrue(useMixerOn(h, brass, copperMixer.copy(), true).consumesAction(),
			"sneaking, it should go on as an ordinary mixer");
		GT.assertIs(h, brass.west(), tier(MachineKind.MIXER, KineticMaterial.COPPER));
		VatMixerBlockEntity wrong = mixer(h, other, 128);
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(vat(h, other) != null, "the brass vat should form"))
			.thenExecute(() -> {
				fluids(h.getBlockEntity(other)).fill(Chemical.HYDROGEN_CHLORIDE.stack(250), FluidAction.EXECUTE);
				fluids(h.getBlockEntity(other)).fill(water(250), FluidAction.EXECUTE);
			})
			.thenWaitUntil(() -> h.assertTrue(wrong.doing() == Doing.OTHER_MATERIAL, "it should say it doesn't fit, says "
				+ wrong.doing()))
			.thenIdle(40)
			.thenExecute(() -> h.assertTrue(amount(fluids(h.getBlockEntity(other)), Chemical.HYDROCHLORIC_ACID) == 0,
				"a copper mixer should not stir a brass vat"))
			.thenSucceed();
	}

	/** Hydrogen chloride and water stirred in a vat make hydrochloric acid, four batches a stir. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void aVatMixerMakesHydrochloricAcid(GameTestHelper h) {
		BlockPos tank = p(6, 3, 4);
		smallVat(h, tank.below());
		VatMixerBlockEntity mixer = mixer(h, tank, 128);
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(vat(h, tank) != null, "the vat should form"))
			.thenExecute(() -> {
				IFluidHandler pipes = fluids(h.getBlockEntity(tank));
				pipes.fill(Chemical.HYDROGEN_CHLORIDE.stack(1000), FluidAction.EXECUTE);
				pipes.fill(water(1000), FluidAction.EXECUTE);
			})
			.thenWaitUntil(() -> h.assertTrue(amount(fluids(h.getBlockEntity(tank)), Chemical.HYDROCHLORIC_ACID) == 2000,
				"four batches should make 2000 mB of acid, there is "
					+ amount(fluids(h.getBlockEntity(tank)), Chemical.HYDROCHLORIC_ACID)))
			.thenExecute(() -> {
				IFluidHandler pipes = fluids(h.getBlockEntity(tank));
				h.assertTrue(amount(pipes, Chemical.HYDROGEN_CHLORIDE) == 0 && amount(pipes, water(1)) == 0,
					"the hydrogen chloride and water should be used up");
				h.assertTrue(pipes.drain(1000, FluidAction.EXECUTE).isFluidEqual(Chemical.HYDROCHLORIC_ACID.stack(1)),
					"a pipe should draw off the acid");
			})
			.thenWaitUntil(() -> h.assertTrue(mixer.doing() == Doing.IDLE, "with nothing left it should idle"))
			.thenSucceed();
	}

	/** Chlorine and ethylene join over ferric chloride into ethylene dichloride, using it up only slowly. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void chlorineAndEthyleneJoinOverFerricChloride(GameTestHelper h) {
		BlockPos tank = p(6, 3, 4);
		smallVat(h, tank.below());
		mixer(h, tank, 128);
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(vat(h, tank) != null, "the vat should form"))
			.thenExecute(() -> {
				IFluidHandler pipes = fluids(h.getBlockEntity(tank));
				pipes.fill(Chemical.CHLORINE.stack(2000), FluidAction.EXECUTE);
				pipes.fill(Chemical.ETHYLENE.stack(2000), FluidAction.EXECUTE);
				pipes.fill(Chemical.FERRIC_CHLORIDE.stack(100), FluidAction.EXECUTE);
			})
			.thenWaitUntil(() -> h.assertTrue(
				amount(fluids(h.getBlockEntity(tank)), Chemical.ETHYLENE_DICHLORIDE) == 2000,
				"two batches should make 2000 mB of ethylene dichloride"))
			.thenExecute(() -> {
				IFluidHandler pipes = fluids(h.getBlockEntity(tank));
				h.assertTrue(amount(pipes, Chemical.FERRIC_CHLORIDE) == 50, "two batches should use 50 mB of ferric chloride");
				h.assertTrue(amount(pipes, Chemical.CHLORINE) == 0 && amount(pipes, Chemical.ETHYLENE) == 0,
					"the chlorine and ethylene should be used up");
			})
			.thenSucceed();
	}

	/**
	 * A vat stirs Create's own mixing that is wet and needs no heat: wheat flour and water into dough, which comes out
	 * of its scaffolding into a chest.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void aVatMixerKneadsDough(GameTestHelper h) {
		BlockPos tank = p(6, 3, 4), chestAt = p(6, 2, 5);
		smallVat(h, tank.below());
		h.setBlock(chestAt, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
		VatMixerBlockEntity mixer = mixer(h, tank, 128);
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(vat(h, tank) != null, "the vat should form"))
			.thenWaitUntil(() -> h.assertTrue(mixer.doing() == Doing.IDLE, "the mixer should be turning, is " + mixer.doing()))
			.thenExecute(() -> {
				fluids(h.getBlockEntity(tank)).fill(water(4000), FluidAction.EXECUTE);
				IItemHandler slots = h.getBlockEntity(tank).getCapability(ForgeCapabilities.ITEM_HANDLER)
					.orElseThrow(IllegalStateException::new);
				ItemStack left = ItemHandlerHelper.insertItem(slots, AllItems.WHEAT_FLOUR.asStack(4), false);
				h.assertTrue(left.isEmpty(), "a stirred vat should take flour");
			})
			.thenWaitUntil(() -> h.assertTrue(
				((Container) h.getBlockEntity(chestAt)).countItem(AllItems.DOUGH.get()) == 4,
				"four batches of dough should come out into the chest"))
			.thenSucceed();
	}

	/**
	 * A funnel on a vat's scaffolding, facing out, pulls out what the vat makes (Taran, 0.28.0), as one on a basin
	 * does: the four aluminium hydroxide settled from four buckets of bauxite liquor drop out of it one by one, here
	 * into a hopper, and none is left waiting. Nothing goes into a vat that way.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void aFunnelOnAVatsScaffoldingPullsOutWhatItMakes(GameTestHelper h) {
		BlockPos tank = p(4, 3, 4), funnelAt = p(3, 2, 4), hopperAt = p(3, 1, 4);
		smallVat(h, tank.below());
		h.setBlock(hopperAt, Blocks.HOPPER);
		h.setBlock(funnelAt, AllBlocks.ANDESITE_FUNNEL.getDefaultState().setValue(FunnelBlock.FACING, Direction.WEST)
			.setValue(FunnelBlock.EXTRACTING, true));
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(vat(h, tank) != null, "the vat should form"))
			.thenExecute(() -> {
				int in = fluids(h.getBlockEntity(tank)).fill(Chemical.BAUXITE_LIQUOR.stack(4000), FluidAction.EXECUTE);
				h.assertTrue(in == 4000, "a pipe should fill the vat with 4000 mB of liquor, filled " + in);
			})
			.thenWaitUntil(() -> {
				int pulled = ((Container) h.getBlockEntity(hopperAt)).countItem(ModItems.ALUMINIUM_HYDROXIDE.get());
				h.assertTrue(pulled == 4, "the funnel should pull out all four batches, has pulled " + pulled);
			})
			.thenExecute(() -> {
				IItemHandler outlet = vat(h, tank).outletCapability().orElseThrow(IllegalStateException::new);
				for (int slot = 0; slot < outlet.getSlots(); slot++)
					h.assertTrue(outlet.getStackInSlot(slot).isEmpty(), "nothing should be left waiting in the vat");
				h.assertTrue(ItemHandlerHelper.insertItem(outlet, AllItems.WHEAT_FLOUR.asStack(), false).getCount() == 1,
					"nothing should go into the vat by its outlet");
			})
			.thenSucceed();
	}

	/**
	 * A vat two blocks across fills an inventory beside any block of its scaffolding, not only the first: a chest
	 * east of its north-east block takes the four aluminium hydroxide it settles.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 360)
	public static void aWideVatFillsAnInventoryBesideAnyBlockOfItsScaffolding(GameTestHelper h) {
		BlockPos tank = p(4, 3, 4), chestAt = p(6, 2, 4);
		for (int x = 0; x < 2; x++)
			for (int z = 0; z < 2; z++)
				smallVat(h, tank.offset(x, -1, z));
		h.setBlock(chestAt, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST));
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(vat(h, tank) != null && vat(h, tank) == vat(h, tank.offset(1, 0, 1)),
				"the four tanks should form one vat"))
			.thenExecute(() -> {
				int in = fluids(h.getBlockEntity(tank)).fill(Chemical.BAUXITE_LIQUOR.stack(4000), FluidAction.EXECUTE);
				h.assertTrue(in == 4000, "a pipe should fill the vat with 4000 mB of liquor, filled " + in);
			})
			.thenWaitUntil(() -> {
				int hydroxide = ((Container) h.getBlockEntity(chestAt)).countItem(ModItems.ALUMINIUM_HYDROXIDE.get());
				h.assertTrue(hydroxide == 4, "four batches should settle into the chest, there are " + hydroxide);
			})
			.thenSucceed();
	}

	/**
	 * Every place round a vat's scaffolding where what it makes can come out has a bit of its own, as its look keeps
	 * them (0.28.0): one under each block of scaffolding and one beside each block on its outer sides.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 20)
	public static void everyPlaceAVatPutsItemsHasItsOwnBit(GameTestHelper h) {
		BlockPos floor = p(4, 2, 4);
		for (int width = 1; width <= Vat.MAX_WIDTH; width++) {
			List<Vat.Port> ports = Vat.ports(floor, width);
			h.assertTrue(ports.size() == width * width + 4 * width,
				"a vat " + width + " across should have " + (width * width + 4 * width) + " places, has " + ports.size());
			long bits = ports.stream().mapToInt(Vat.Port::bit).distinct().count();
			h.assertTrue(bits == ports.size(), "each place should have a bit of its own");
			h.assertTrue(ports.stream().allMatch(port -> port.bit() >= 0 && port.bit() < 21),
				"every bit should fit in the 21 its look keeps");
			for (Vat.Port port : ports) {
				BlockPos scaffolding = floor.offset(port.x(), 0, port.z());
				h.assertTrue(port.target().equals(scaffolding.relative(port.side())),
					"each place should be on its side of its block of scaffolding");
			}
		}
		h.succeed();
	}

	/**
	 * Copper, iron, steel and aluminium mixers each go on a vat of their own metal, as their vat forms (Taran), and not
	 * on another metal's.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void eachMixerFitsAVatOfItsOwnMetal(GameTestHelper h) {
		BlockPos copper = p(4, 3, 3), steel = p(4, 3, 9), iron = p(10, 3, 3), aluminium = p(10, 3, 9);
		smallVat(h, copper.below());
		metalVat(h, steel.below(), FluidPartMaterial.STEEL);
		metalVat(h, iron.below(), FluidPartMaterial.IRON);
		metalVat(h, aluminium.below(), FluidPartMaterial.ALUMINIUM);
		h.assertFalse(useMixerOn(h, iron, new ItemStack(tier(MachineKind.MIXER, KineticMaterial.COPPER)), false)
			.consumesAction(), "a copper mixer should not go on an iron vat");
		h.assertBlockPresent(Blocks.AIR, iron.west());
		Map<BlockPos, KineticMaterial> vats = Map.of(copper, KineticMaterial.COPPER, steel, KineticMaterial.STEEL, iron,
			KineticMaterial.IRON, aluminium, KineticMaterial.ALUMINIUM);
		vats.forEach((tank, material) -> {
			h.assertTrue(useMixerOn(h, tank, new ItemStack(tier(MachineKind.MIXER, material)), false).consumesAction(),
				"a " + material + " mixer should go on a " + material + " vat");
			h.assertTrue(h.getBlockState(tank.west()).is(vatForm(material)),
				"as its vat form, not " + h.getBlockState(tank.west()));
		});
		h.succeed();
	}

	/** Copper, iron, steel and aluminium vat mixers stir vats of their metal at 256 RPM, past andesite's 128. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void vatMixersStirTheirVats(GameTestHelper h) {
		BlockPos copper = p(5, 3, 3), steel = p(5, 3, 9), iron = p(11, 3, 3), aluminium = p(11, 3, 9);
		smallVat(h, copper.below());
		metalVat(h, steel.below(), FluidPartMaterial.STEEL);
		metalVat(h, iron.below(), FluidPartMaterial.IRON);
		metalVat(h, aluminium.below(), FluidPartMaterial.ALUMINIUM);
		mixer(h, copper, vatForm(KineticMaterial.COPPER), 256);
		mixer(h, steel, vatForm(KineticMaterial.STEEL), 256);
		mixer(h, iron, vatForm(KineticMaterial.IRON), 256);
		mixer(h, aluminium, vatForm(KineticMaterial.ALUMINIUM), 256);
		List<BlockPos> tanks = List.of(copper, steel, iron, aluminium);
		h.startSequence()
			.thenWaitUntil(() -> tanks.forEach(tank -> h.assertTrue(vat(h, tank) != null, "the vat should form")))
			.thenExecute(() -> tanks.forEach(tank -> {
				IFluidHandler pipes = fluids(h.getBlockEntity(tank));
				pipes.fill(Chemical.HYDROGEN_CHLORIDE.stack(250), FluidAction.EXECUTE);
				pipes.fill(water(250), FluidAction.EXECUTE);
			}))
			.thenWaitUntil(() -> tanks.forEach(tank -> h.assertTrue(
				amount(fluids(h.getBlockEntity(tank)), Chemical.HYDROCHLORIC_ACID) == 500,
				"the vat at " + tank + " should be stirred into 500 mB of acid, has "
					+ amount(fluids(h.getBlockEntity(tank)), Chemical.HYDROCHLORIC_ACID))))
			.thenExecute(() -> tanks.forEach(tank -> GT.assertNotIs(h, tank.west(), Blocks.AIR)))
			.thenSucceed();
	}

	/**
	 * A vat mixer turns by the cog on its face, with the cogs beside it, as Create's mixer does (Taran, 0.23.0): a motor
	 * behind it, where its shaft went in before, turns nothing.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void aVatMixerTurnsByItsCogNotAShaft(GameTestHelper h) {
		BlockPos behind = p(6, 3, 4), beside = p(6, 3, 10);
		smallVat(h, behind.below());
		smallVat(h, beside.below());
		h.setBlock(behind.west(), facingEast(vatForm(KineticMaterial.COPPER)));
		GT.motor(h, behind.west(2), Direction.EAST, 64);
		VatMixerBlockEntity cogged = mixer(h, beside, 64);
		h.runAtTickTime(40, () -> {
			h.assertTrue(GT.kinetic(h, behind.west()).getSpeed() == 0,
				"a motor behind a vat mixer should not turn it, turns at " + GT.kinetic(h, behind.west()).getSpeed());
			h.assertTrue(cogged.getSpeed() == -64, "a cog beside it should turn it the other way, at -64 RPM, not "
				+ cogged.getSpeed());
			h.succeed();
		});
	}

	/**
	 * A vat mixer is its mixer used another way, so it is a friction point capped at its mixer's material and draws its
	 * friction: a copper or iron one tops out at 256 RPM and a steel or aluminium one at 512; iron and steel ones draw
	 * their metal's friction, and a copper one more once oxidized.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void vatMixersTopOutAtTheirMixersMaterial(GameTestHelper h) {
		BlockPos copper = p(3, 2, 3), iron = p(7, 2, 3), steel = p(11, 2, 3), aluminium = p(3, 2, 9);
		Map<BlockPos, KineticMaterial> expected = Map.of(copper, KineticMaterial.COPPER, iron, KineticMaterial.IRON,
			steel, KineticMaterial.STEEL, aluminium, KineticMaterial.ALUMINIUM);
		expected.forEach((pos, material) -> h.setBlock(pos, facingEast(vatForm(material))));
		expected.forEach((pos, material) -> {
			FrictionPoints.Limits limits = FrictionPoints.limitsFor(GT.kinetic(h, pos));
			h.assertTrue(limits.frictionPoint() && limits.speed() == material && limits.load() == null,
				"the " + material + " vat mixer should be a friction point of " + material + " that takes any load, got "
					+ limits);
		});
		float base = (float) BlockStressValues.getImpact(AllBlocks.MECHANICAL_MIXER.get());
		MachineTierTests.assertImpact(h, copper, base, "a fresh copper vat mixer draws none");
		MachineTierTests.assertImpact(h, iron, base + .125f, "an iron vat mixer draws iron's friction");
		MachineTierTests.assertImpact(h, steel, base + .25f, "a steel vat mixer draws steel's friction");
		MachineTierTests.assertImpact(h, aluminium, base, "an aluminium vat mixer draws none");
		PatinaTests.setPatina(h, copper, Patina.OXIDIZED);
		MachineTierTests.assertImpact(h, copper, base + .25f, "an oxidized copper vat mixer draws a little more");
		h.succeed();
	}

	/**
	 * A copper mixer on a vat's side grinds itself apart at 512 RPM, past copper's 256, and gives the mixer back. A motor
	 * turns no faster than 256, so a steel large cog meshing with the mixer's cog, below it and to the side, doubles it.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 160)
	public static void aVatMixerBreaksPastItsMetalsTopSpeed(GameTestHelper h) {
		BlockPos tank = p(5, 3, 4);
		smallVat(h, tank.below());
		GT.motor(h, p(1, 2, 3), Direction.EAST, 256);
		GT.shaftX(h, p(2, 2, 3), KineticTests.part("steel_shaft"));
		GT.shaftX(h, p(3, 2, 3), KineticTests.part("steel_shaft"));
		GT.shaftX(h, p(4, 2, 3), KineticTests.part("steel_large_cogwheel"));
		h.setBlock(tank.west(), facingEast(vatForm(KineticMaterial.COPPER)));
		h.runAtTickTime(140, () -> {
			h.assertTrue(h.getBlockState(tank.west()).isAir(),
				"a copper vat mixer should break at 512 RPM: " + h.getBlockState(tank.west()));
			h.assertTrue(MachineTierTests.itemsNear(h, tank.west(),
				tier(MachineKind.MIXER, KineticMaterial.COPPER).asItem()) == 1, "and give its mixer back");
			h.succeed();
		});
	}

	/**
	 * A copper vat mixer weathers anywhere and a steel one only where it gets wet, as their mixers do, while an
	 * aluminium one never does. Broken, a weathered one gives back its mixer as weathered, which goes back on a vat so.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void copperAndSteelVatMixersWeatherAsTheirMixersDo(GameTestHelper h) {
		BlockPos copper = p(3, 2, 3), steel = p(7, 2, 3), aluminium = p(11, 2, 3), tank = p(5, 3, 9);
		h.setBlock(copper, facingEast(vatForm(KineticMaterial.COPPER)));
		h.setBlock(steel, facingEast(vatForm(KineticMaterial.STEEL)));
		h.setBlock(aluminium, facingEast(vatForm(KineticMaterial.ALUMINIUM)));
		h.assertFalse(Patina.has(h.getBlockState(aluminium)) || h.getBlockState(aluminium).isRandomlyTicking(),
			"an aluminium vat mixer should never weather");
		h.assertTrue(h.getBlockState(steel).isRandomlyTicking(), "a steel vat mixer should random-tick to rust by");
		RandomSource random = RandomSource.create(17);
		for (BlockPos pos : List.of(copper, steel))
			KineticTests.randomTicks(h, pos, random, 300);
		int stage = MachineTierTests.stageOf(h, copper);
		h.assertTrue(stage > 0 && stage == WeatheringTests.finalStage(h, copper),
			"a copper vat mixer should weather to its final stage anywhere, got " + h.getBlockState(copper));
		h.assertTrue(MachineTierTests.stageOf(h, steel) == 0, "a dry steel vat mixer should not rust, got "
			+ h.getBlockState(steel));

		Patina weathered = PatinaTests.patina(h, copper);
		List<ItemStack> drops = GT.breakWith(h, copper, new ItemStack(Items.IRON_PICKAXE));
		h.assertTrue(drops.size() == 1 && drops.get(0).is(tier(MachineKind.MIXER, KineticMaterial.COPPER).asItem())
			&& Patina.of(drops.get(0)) == weathered, "it should give back a " + weathered + " copper mixer: " + drops);
		smallVat(h, tank.below());
		h.assertTrue(useMixerOn(h, tank, drops.get(0), false).consumesAction(), "the mixer should go back on a vat");
		BlockState back = h.getBlockState(tank.west());
		h.assertTrue(back.is(vatForm(KineticMaterial.COPPER)) && PatinaTests.patina(h, tank.west()) == weathered,
			"as weathered as it was: " + back);
		h.succeed();
	}
}

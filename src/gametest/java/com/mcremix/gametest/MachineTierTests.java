package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mcremix.McRemix;
import com.mcremix.drilling.DrillBits;
import com.mcremix.drilling.FittedBits;
import com.mcremix.fluids.Patina;
import com.mcremix.kinetics.FrictionPoints;
import com.mcremix.kinetics.KineticLimits;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.kinetics.Weathering;
import com.mcremix.machines.MachineKind;
import com.mcremix.machines.MachineTiers;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModMachines;
import com.mcremix.registry.RemovedBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.kinetics.deployer.DeployerBlock;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.drill.DrillBlock;
import com.simibubi.create.content.kinetics.drill.DrillBlockEntity;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Machines in copper, iron, steel and aluminium (Taran's design, Part 2). Wherever Create's code asks whether something
 * is one of its machines, every tier counts as Create's own of its kind.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class MachineTierTests {

	static Block tier(MachineKind kind, KineticMaterial material) {
		return ModMachines.get(kind, material).get();
	}

	@GameTest(template = GT.PLATFORM)
	public static void createTakesEveryTierForItsOwn(GameTestHelper h) {
		for (MachineKind kind : MachineKind.values())
			for (KineticMaterial material : kind.tiered() ? MachineTiers.TIERS : List.<KineticMaterial>of()) {
				Block block = tier(kind, material);
				h.assertTrue(kind.createEntry().has(block.defaultBlockState()),
					"Create should take " + block.getDescriptionId() + " for its " + kind.createId);
				h.assertTrue(kind.createEntry().isIn(new ItemStack(block)),
					"and its item for its " + kind.createId + "'s: " + block.getDescriptionId());
				for (MachineKind other : MachineKind.values())
					h.assertFalse(other != kind && other.createEntry().has(block.defaultBlockState()),
						block.getDescriptionId() + " should not count as Create's " + other.createId);
			}
		h.assertFalse(AllBlocks.SHAFT.has(tier(MachineKind.PRESS, KineticMaterial.COPPER).defaultBlockState()),
			"a tiered machine is no shaft");
		h.assertFalse(AllBlocks.MECHANICAL_DRILL.has(AllBlocks.MECHANICAL_SAW.getDefaultState()),
			"Create's entries still tell its own machines apart");
		h.succeed();
	}

	/** A chute is pushed by a copper fan under it just as by Create's. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void tieredFansBlowThroughChutes(GameTestHelper h) {
		BlockPos create = p(3, 2, 4), copper = p(7, 2, 4), none = p(11, 2, 4);
		Block[] fans = { AllBlocks.ENCASED_FAN.get(), tier(MachineKind.FAN, KineticMaterial.COPPER) };
		BlockPos[] at = { create, copper };
		for (int i = 0; i < fans.length; i++) {
			GT.motor(h, at[i].below(), Direction.UP, 32);
			h.setBlock(at[i], fans[i].defaultBlockState().setValue(EncasedFanBlock.FACING, Direction.UP));
			h.setBlock(at[i].above(), AllBlocks.CHUTE.getDefaultState());
		}
		h.setBlock(none.above(), AllBlocks.CHUTE.getDefaultState());
		h.runAtTickTime(20, () -> {
			float byCreate = ((ChuteBlockEntity) h.getBlockEntity(create.above())).getItemMotion();
			float byCopper = ((ChuteBlockEntity) h.getBlockEntity(copper.above())).getItemMotion();
			float alone = ((ChuteBlockEntity) h.getBlockEntity(none.above())).getItemMotion();
			h.assertTrue(byCreate != alone, "Create's fan should move items in the chute above it (" + byCreate + ")");
			h.assertTrue(byCopper == byCreate,
				"a copper fan should move them as Create's does: " + byCopper + ", Create's " + byCreate);
			h.succeed();
		});
	}

	/**
	 * The mechanical crafter comes in Create's brass alone (Taran, 0.23.0): no tier of it is registered, and the ones
	 * that were go quietly from old worlds.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void craftersComeInBrassAlone(GameTestHelper h) {
		h.assertTrue(ModMachines.of(MachineKind.CRAFTER).length == 0, "no crafter should have tiers");
		h.assertFalse(MachineKind.CRAFTER.tiered(), "the crafter should not be a tiered kind");
		for (KineticMaterial material : MachineTiers.TIERS) {
			String name = material.prefix + "_mechanical_crafter";
			h.assertTrue(MachineTiers.block(MachineKind.CRAFTER, material) == null, "there should be no " + name);
			h.assertFalse(ForgeRegistries.BLOCKS.containsKey(McRemix.asResource(name)), name + " should be gone");
			h.assertTrue(RemovedBlocks.removed(name), name + " should be known as taken out");
		}
		h.assertTrue(RemovedBlocks.removed("tiered_crafter"), "the tiered crafters' block entity should be known as gone");
		h.assertTrue(MachineTiers.block(MachineKind.CRAFTER, KineticMaterial.BRASS) == AllBlocks.MECHANICAL_CRAFTER.get(),
			"Create's crafter is the brass one");
		h.succeed();
	}

	/** Arms take tiered deployers and millstones as points, as Create's own. */
	@GameTest(template = GT.PLATFORM)
	public static void armsReachEveryTier(GameTestHelper h) {
		Map<BlockPos, Class<? extends ArmInteractionPointType>> points = new LinkedHashMap<>();
		BlockPos deployer = p(5, 2, 2), millstone = p(8, 2, 2);
		h.setBlock(deployer, tier(MachineKind.DEPLOYER, KineticMaterial.STEEL));
		h.setBlock(millstone, tier(MachineKind.MILLSTONE, KineticMaterial.IRON));
		points.put(deployer, AllArmInteractionPointTypes.DeployerType.class);
		points.put(millstone, AllArmInteractionPointTypes.MillstoneType.class);
		points.forEach((pos, type) -> {
			ArmInteractionPoint point = ArmInteractionPoint.create(h.getLevel(), h.absolutePos(pos), h.getBlockState(pos));
			h.assertTrue(point != null && type.isInstance(point.getType()), "an arm should take "
				+ h.getBlockState(pos).getBlock().getDescriptionId() + " as a " + type.getSimpleName() + ", got "
				+ (point == null ? null : point.getType()));
		});
		h.succeed();
	}

	/** A tiered deployer reaches out like Create's, and places beside any deployer when clicked with one. */
	@GameTest(template = GT.PLATFORM)
	public static void tieredDeployersReachAndPlaceAlongside(GameTestHelper h) {
		BlockPos steel = p(4, 2, 4);
		h.setBlock(steel, tier(MachineKind.DEPLOYER, KineticMaterial.STEEL).defaultBlockState()
			.setValue(DeployerBlock.FACING, Direction.UP));
		Vec3 reach;
		try {
			Method vector = DeployerBlockEntity.class.getDeclaredMethod("getMovementVector");
			vector.setAccessible(true);
			reach = (Vec3) vector.invoke(h.getBlockEntity(steel));
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
		h.assertTrue(reach.equals(new Vec3(0, 1, 0)), "a steel deployer facing up should reach up, got " + reach);

		TestPlayer player = GT.player(h, p(4, 2, 1));
		SawTests.clickSide(h, player, steel, Direction.NORTH, new ItemStack(AllBlocks.DEPLOYER.get()));
		SawTests.clickSide(h, player, steel, Direction.NORTH, new ItemStack(tier(MachineKind.DEPLOYER, KineticMaterial.COPPER)));
		Map<String, String> beside = new LinkedHashMap<>();
		for (Direction d : Direction.Plane.HORIZONTAL) {
			BlockState s = h.getBlockState(steel.relative(d));
			if (s.getBlock() instanceof DeployerBlock)
				beside.put(s.getBlock().getDescriptionId(), String.valueOf(s.getValue(DeployerBlock.FACING)));
		}
		h.assertTrue(beside.size() == 2 && beside.containsKey("block.create.deployer")
			&& beside.containsKey("block.mcremix.copper_deployer") && beside.values().stream().allMatch("up"::equals),
			"Create's deployer and a copper one should be placed beside the steel deployer, facing up: " + beside);
		h.succeed();
	}

	static int stageOf(GameTestHelper h, BlockPos rel) {
		Enum<?> stage = Weathering.stage(h.getBlockState(rel));
		return stage == null ? -1 : stage.ordinal();
	}

	/**
	 * Copper machines weather anywhere, iron and steel ones only where they get wet, steel only to its first stage
	 * (0.28.0), aluminium ones never.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void machinesWeatherByTheirMaterial(GameTestHelper h) {
		BlockPos copper = p(2, 2, 2), dryIron = p(5, 2, 2), wetSteel = p(8, 2, 2), aluminium = p(13, 2, 2);
		h.setBlock(copper, tier(MachineKind.PRESS, KineticMaterial.COPPER));
		h.setBlock(dryIron, tier(MachineKind.MILLSTONE, KineticMaterial.IRON));
		h.setBlock(wetSteel, tier(MachineKind.FAN, KineticMaterial.STEEL));
		h.setBlock(wetSteel.east(), Blocks.WATER);
		for (BlockPos glass : new BlockPos[] { wetSteel.east(2), wetSteel.east().north(), wetSteel.east().south() })
			h.setBlock(glass, Blocks.GLASS);
		h.setBlock(aluminium, tier(MachineKind.MIXER, KineticMaterial.ALUMINIUM));
		h.assertFalse(Patina.has(h.getBlockState(aluminium)) || h.getBlockState(aluminium).isRandomlyTicking(),
			"aluminium machines never weather");
		h.assertTrue(h.getBlockState(dryIron).isRandomlyTicking(), "iron machines take random ticks to rust by");

		RandomSource random = RandomSource.create(17);
		for (BlockPos pos : new BlockPos[] { copper, dryIron, wetSteel, aluminium })
			KineticTests.randomTicks(h, pos, random, 300);
		h.assertTrue(stageOf(h, copper) == WeatheringTests.finalStage(h, copper), "a copper press should weather to its own "
			+ "final stage anywhere, got " + h.getBlockState(copper));
		h.assertTrue(stageOf(h, dryIron) == 0, "a dry iron millstone should not rust, got " + h.getBlockState(dryIron));
		h.assertTrue(stageOf(h, wetSteel) == 1, "a steel fan beside water should rust to its first stage and no further, got "
			+ h.getBlockState(wetSteel));
		h.assertFalse(h.getBlockState(wetSteel).isRandomlyTicking(), "a rusty steel fan has nowhere further to go");
		h.succeed();
	}

	/** Honeycomb waxes a machine and an axe scrapes it, and broken it drops as it was. */
	@GameTest(template = GT.PLATFORM)
	public static void machinesAreWaxedScrapedAndDropAsTheyWere(GameTestHelper h) {
		BlockPos press = p(4, 2, 4), fresh = p(8, 2, 4);
		h.setBlock(press, tier(MachineKind.PRESS, KineticMaterial.COPPER));
		PatinaTests.setPatina(h, press, Patina.WEATHERED);
		Items.HONEYCOMB.useOn(PatinaTests.use(h, press, new ItemStack(Items.HONEYCOMB)));
		h.assertTrue(PatinaTests.patina(h, press) == Patina.WAXED_WEATHERED, "honeycomb should wax a copper press");
		Items.IRON_AXE.useOn(PatinaTests.use(h, press, new ItemStack(Items.IRON_AXE)));
		Items.IRON_AXE.useOn(PatinaTests.use(h, press, new ItemStack(Items.IRON_AXE)));
		h.assertTrue(PatinaTests.patina(h, press) == Patina.EXPOSED, "an axe should take off the wax, then a stage");

		h.setBlock(fresh, tier(MachineKind.PRESS, KineticMaterial.COPPER));
		List<ItemStack> weathered = GT.breakWith(h, press, new ItemStack(Items.IRON_PICKAXE));
		List<ItemStack> plain = GT.breakWith(h, fresh, new ItemStack(Items.IRON_PICKAXE));
		h.assertTrue(weathered.size() == 1 && Patina.of(weathered.get(0)) == Patina.EXPOSED,
			"an exposed copper press should drop as it was: " + weathered);
		h.assertTrue(plain.size() == 1 && !plain.get(0).hasTag(), "a fresh one should drop plain: " + plain);
		h.succeed();
	}

	/**
	 * Every machine is a friction point capped at its material's top speed and carrying its load (Create's own count as
	 * andesite, the crafter and arm as brass), and draws its material's friction, more once weathered. Create's crushing
	 * wheel holds no shafts and carries any load, but turns no faster than andesite's top speed.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void machinesTopOutAtTheirMaterialAndDrawItsFriction(GameTestHelper h) {
		Map<BlockPos, KineticMaterial> expected = new LinkedHashMap<>();
		Map<BlockPos, Block> blocks = new LinkedHashMap<>();
		blocks.put(p(2, 2, 2), AllBlocks.MECHANICAL_PRESS.get());
		expected.put(p(2, 2, 2), KineticMaterial.ANDESITE);
		blocks.put(p(12, 2, 2), AllBlocks.MECHANICAL_CRAFTER.get());
		expected.put(p(12, 2, 2), KineticMaterial.BRASS);
		int x = 2;
		for (KineticMaterial material : MachineTiers.TIERS) {
			BlockPos pos = p(x, 2, 5);
			blocks.put(pos, tier(MachineKind.PRESS, material));
			expected.put(pos, material);
			x += 3;
		}
		blocks.forEach(h::setBlock);
		expected.forEach((pos, material) -> {
			FrictionPoints.Limits limits = FrictionPoints.limitsFor(GT.kinetic(h, pos));
			h.assertTrue(limits.frictionPoint() && limits.speed() == material && limits.load() == null,
				blocks.get(pos).getDescriptionId() + " should be a support of " + material + " that takes any load, got "
					+ limits);
		});

		BlockPos wheel = p(4, 2, 9);
		h.setBlock(wheel, AllBlocks.CRUSHING_WHEEL.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
		FrictionPoints.Limits limits = FrictionPoints.limitsFor(GT.kinetic(h, wheel));
		h.assertTrue(!limits.frictionPoint() && limits.speed() == KineticMaterial.ANDESITE && limits.load() == null,
			"a crushing wheel should be no friction point, top out at andesite's speed and carry any load: " + limits);
		h.assertTrue(FrictionPoints.distanceToHolder(h.getLevel(), h.absolutePos(wheel.east()),
			AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.X)) == 0,
			"a crushing wheel should hold no shaft");

		float base = (float) BlockStressValues.getImpact(AllBlocks.MECHANICAL_PRESS.get());
		BlockPos copper = p(2, 2, 5), iron = p(5, 2, 5), steel = p(8, 2, 5), aluminium = p(11, 2, 5);
		assertImpact(h, copper, base, "a fresh copper press draws no friction");
		assertImpact(h, iron, base + .125f, "an iron press draws iron's friction");
		assertImpact(h, steel, base + .25f, "a steel press draws steel's friction");
		assertImpact(h, aluminium, base, "an aluminium press draws none");
		PatinaTests.setPatina(h, copper, Patina.OXIDIZED);
		assertImpact(h, copper, base + .25f, "an oxidized copper press draws a little more");
		PatinaTests.setPatina(h, copper, Patina.WAXED_OXIDIZED);
		assertImpact(h, copper, base + .25f, "and as much waxed");
		PatinaTests.setPatina(h, iron, Patina.WEATHERED);
		assertImpact(h, iron, base + .25f, "a weathered iron press draws iron's friction and its rust's");
		h.succeed();
	}

	static void assertImpact(GameTestHelper h, BlockPos pos, float impact, String message) {
		float actual = GT.kinetic(h, pos).calculateStressApplied();
		h.assertTrue(Math.abs(actual - impact) < 1e-4, message + ": expected " + impact + " su per RPM, got " + actual);
	}

	/** Create's millstone, andesite, grinds itself apart at 256 RPM; an iron one runs on, and rusted draws more. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 160)
	public static void andesiteMachinesBreakPastTheirTopSpeed(GameTestHelper h) {
		BlockPos create = p(3, 2, 4), iron = p(9, 2, 4);
		GT.motor(h, create.below(), Direction.UP, 256);
		h.setBlock(create, AllBlocks.MILLSTONE.get());
		GT.motor(h, iron.below(), Direction.UP, 256);
		h.setBlock(iron, tier(MachineKind.MILLSTONE, KineticMaterial.IRON));
		float[] fresh = new float[1];
		h.runAtTickTime(20, () -> {
			fresh[0] = ((KineticLimits.Strained) GT.kinetic(h, iron)).mcremix$networkStress();
			PatinaTests.setPatina(h, iron, Patina.OXIDIZED);
		});
		h.runAtTickTime(25, () -> {
			float rusted = ((KineticLimits.Strained) GT.kinetic(h, iron)).mcremix$networkStress();
			h.assertTrue(Math.abs(rusted - fresh[0] - .25f * 256) < .01f,
				"rusted, the iron millstone should draw 64 su more at 256 RPM: " + fresh[0] + " then " + rusted);
		});
		h.runAtTickTime(140, () -> {
			h.assertTrue(h.getBlockState(create).isAir(),
				"Create's millstone should break at 256 RPM, past andesite's 128: " + h.getBlockState(create));
			GT.assertIs(h, iron, tier(MachineKind.MILLSTONE, KineticMaterial.IRON));
			h.succeed();
		});
	}

	static int itemsNear(GameTestHelper h, BlockPos rel, Item item) {
		return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(rel)).inflate(1.5),
			e -> e.getItem().is(item)).stream().mapToInt(e -> e.getItem().getCount()).sum();
	}

	/**
	 * A casing used on a placed machine swaps its material, up or down: what it holds stays, the casing it had pops out,
	 * and it finds its speed again. A machine that does not come in that material is left alone.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void casingsSwapAPlacedMachine(GameTestHelper h) {
		BlockPos mill = p(4, 2, 4);
		GT.motor(h, mill.below(), Direction.UP, 32);
		h.setBlock(mill, AllBlocks.MILLSTONE.get());
		TestPlayer player = GT.player(h, p(4, 2, 1));
		h.startSequence()
			.thenExecuteAfter(5, () -> {
				((MillstoneBlockEntity) h.getBlockEntity(mill)).inputInv.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 5));
				ItemStack copper = new ItemStack(AllBlocks.COPPER_CASING.get(), 3);
				GT.click(h, player, mill, Direction.NORTH, .5, copper);
				GT.assertIs(h, mill, tier(MachineKind.MILLSTONE, KineticMaterial.COPPER));
				h.assertTrue(player.getMainHandItem().getCount() == 2, "the swap should use one casing");
				h.assertTrue(itemsNear(h, mill, AllBlocks.ANDESITE_CASING.asItem()) == 1, "the andesite casing should pop out");
				ItemStack kept = ((MillstoneBlockEntity) h.getBlockEntity(mill)).inputInv.getStackInSlot(0);
				h.assertTrue(kept.is(Items.COBBLESTONE) && kept.getCount() == 5, "the millstone should keep what it held, has " + kept);
			})
			.thenWaitUntil(() -> h.assertTrue(GT.kinetic(h, mill).getSpeed() == 32, "the copper millstone should turn again"))
			.thenExecute(() -> {
				GT.click(h, player, mill, Direction.NORTH, .5, new ItemStack(AllBlocks.BRASS_CASING.get()));
				GT.assertIs(h, mill, tier(MachineKind.MILLSTONE, KineticMaterial.COPPER));
				PatinaTests.setPatina(h, mill, Patina.WAXED_WEATHERED);
				GT.click(h, player, mill, Direction.NORTH, .5, new ItemStack(ModBlocks.STEEL_CASING.get()));
				GT.assertIs(h, mill, tier(MachineKind.MILLSTONE, KineticMaterial.STEEL));
				h.assertTrue(PatinaTests.patina(h, mill) == Patina.NONE, "a swapped machine comes out fresh");
				h.assertTrue(itemsNear(h, mill, AllBlocks.COPPER_CASING.asItem()) == 1, "the copper casing should pop out");
				GT.click(h, player, mill, Direction.NORTH, .5, new ItemStack(AllBlocks.ANDESITE_CASING.get()));
				GT.assertIs(h, mill, AllBlocks.MILLSTONE.get());
				h.assertTrue(itemsNear(h, mill, ModBlocks.STEEL_CASING.get().asItem()) == 1,
					"stepping down to andesite should give the steel casing back");
			})
			.thenWaitUntil(() -> h.assertTrue(GT.kinetic(h, mill).getSpeed() == 32, "Create's millstone should turn again"))
			.thenSucceed();
	}

	/**
	 * A drill keeps its bits through a swap; a deployer's and a crafter's fronts still take the casing in hand, and a
	 * crafter, brass alone, takes no other casing.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void swapsKeepBitsAndLeaveFrontsToCreate(GameTestHelper h) {
		BlockPos drill = p(2, 2, 4), deployer = p(6, 2, 4), crafter = p(10, 2, 4);
		h.setBlock(drill, tier(MachineKind.DRILL, KineticMaterial.COPPER).defaultBlockState()
			.setValue(DrillBlock.FACING, Direction.UP));
		((FittedBits) h.getBlockEntity(drill)).mcremix$setBits(DrillBits.DIAMOND);
		h.setBlock(deployer, AllBlocks.DEPLOYER.getDefaultState().setValue(DeployerBlock.FACING, Direction.NORTH));
		h.setBlock(crafter, AllBlocks.MECHANICAL_CRAFTER.getDefaultState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
		TestPlayer player = GT.player(h, p(6, 2, 1));
		// a deployer makes its hand on its first tick
		h.startSequence()
			.thenExecuteAfter(5, () -> {
				GT.click(h, player, drill, Direction.NORTH, .5, new ItemStack(ModBlocks.IRON_CASING.get()));
				GT.assertIs(h, drill, tier(MachineKind.DRILL, KineticMaterial.IRON));
				h.assertTrue(DrillBits.of((DrillBlockEntity) h.getBlockEntity(drill)) == DrillBits.DIAMOND,
					"the iron drill should keep its diamond bits");

				GT.click(h, player, deployer, Direction.NORTH, .5, new ItemStack(AllBlocks.COPPER_CASING.get()));
				GT.assertIs(h, deployer, AllBlocks.DEPLOYER.get());
				h.assertTrue(((DeployerBlockEntity) h.getBlockEntity(deployer)).getPlayer().getMainHandItem()
					.is(AllBlocks.COPPER_CASING.asItem()), "a deployer's front should take the casing into its hand");
				GT.click(h, player, deployer, Direction.EAST, .5, new ItemStack(AllBlocks.COPPER_CASING.get()));
				GT.assertIs(h, deployer, tier(MachineKind.DEPLOYER, KineticMaterial.COPPER));

				GT.click(h, player, crafter, Direction.NORTH, .5, new ItemStack(ModBlocks.STEEL_CASING.get()));
				GT.assertIs(h, crafter, AllBlocks.MECHANICAL_CRAFTER.get());
				GT.click(h, player, crafter, Direction.EAST, .5, new ItemStack(ModBlocks.STEEL_CASING.get()));
				GT.assertIs(h, crafter, AllBlocks.MECHANICAL_CRAFTER.get());
				ItemStack slot = ((MechanicalCrafterBlockEntity) h.getBlockEntity(crafter)).getInventory().getStackInSlot(0);
				h.assertTrue(slot.is(ModBlocks.STEEL_CASING.get().asItem()),
					"a crafter's front should take the casing into its slot, has " + slot);
				h.assertTrue(itemsNear(h, crafter, AllBlocks.BRASS_CASING.asItem()) == 0,
					"a crafter should keep its brass casing");
			})
			.thenExecuteAfter(5, () -> h.assertTrue(((DeployerBlockEntity) h.getBlockEntity(deployer)).getPlayer()
				.getMainHandItem().is(AllBlocks.COPPER_CASING.asItem()), "the swapped deployer should keep what it held"))
			.thenSucceed();
	}

	/** A deployer holding a casing swaps the machine it faces. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void deployersSwapCasingsToo(GameTestHelper h) {
		BlockPos press = p(4, 2, 4), deployer = p(4, 4, 4);
		h.setBlock(press, AllBlocks.MECHANICAL_PRESS.get());
		h.setBlock(deployer, AllBlocks.DEPLOYER.getDefaultState().setValue(BlockStateProperties.FACING, Direction.DOWN)
			.setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true));
		GT.motor(h, deployer.east(), Direction.WEST, 32);
		h.startSequence()
			.thenExecuteAfter(5, () -> {
				ItemStack left = h.getBlockEntity(deployer).getCapability(ForgeCapabilities.ITEM_HANDLER)
					.map(inv -> inv.insertItem(0, new ItemStack(ModBlocks.STEEL_CASING.get()), false)).orElse(null);
				h.assertTrue(left != null && left.isEmpty(), "the deployer should take the casing, left " + left);
			})
			.thenWaitUntil(() -> GT.assertIs(h, press, tier(MachineKind.PRESS, KineticMaterial.STEEL)))
			.thenExecuteAfter(5, () -> h.assertTrue(itemsNear(h, press, AllBlocks.ANDESITE_CASING.asItem()) == 1,
				"the andesite casing should pop out"))
			.thenSucceed();
	}
}

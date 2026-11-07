package com.mcremix.client;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import com.mcremix.McRemix;
import com.mcremix.drilling.DrillBits;
import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.Patina;
import com.mcremix.kinetics.KineticFamilies;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.kinetics.KineticPart;
import com.mcremix.kinetics.KineticVariant;
import com.mcremix.kinetics.TieredKinetic;
import com.mcremix.machines.MachineTiers;
import com.mcremix.sawing.SawBlades;
import com.mcremix.kinetics.WoodSpecies;
import com.mcremix.movers.WoodenMover;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;

import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Moving models: the shaft and cog tiers (with cogs also drawn without their shaft, as in a casing) and the shaft halves
 * that turn in each gearbox, clutch and gearshift tier
 * (waxed and unwaxed parts share a model per weathering stage), the bellows, a drill head for each kind of bits and a
 * saw blade for each blade, the moving parts of each machine tier, and those of each wood's movers. Partials must exist
 * before models bake, so {@link #init()} runs from the client constructor.
 */
public class ModPartialModels {
	private static final Map<String, PartialModel> MODELS = new HashMap<>();

	/** Bellows parts, modelled with the nozzle to the north. The lid carries the yoke the crank pin rides in. */
	public static final PartialModel BELLOWS_LID = PartialModel.of(McRemix.asResource("block/bellows/lid"));
	public static final PartialModel BELLOWS_BAG = PartialModel.of(McRemix.asResource("block/bellows/bag"));
	public static final PartialModel BELLOWS_CRANK = PartialModel.of(McRemix.asResource("block/bellows/crank"));

	/**
	 * The crucible's pot, which tips, modelled with its lip to the south; a big crucible's pot and frame, modelled
	 * round its middle block; and the end of the drive shaft where it goes into a post, pointing south.
	 */
	public static final PartialModel CRUCIBLE_POT = PartialModel.of(McRemix.asResource("block/crucible/pot"));
	public static final PartialModel CRUCIBLE_POT_LARGE = PartialModel.of(McRemix.asResource("block/crucible/pot_large"));
	public static final PartialModel CRUCIBLE_FRAME_LARGE =
		PartialModel.of(McRemix.asResource("block/crucible/frame_large"));
	public static final PartialModel CRUCIBLE_SHAFT_STUB = PartialModel.of(McRemix.asResource("block/crucible/shaft"));

	/** The needle of a Pressurizer's dial, on a dial facing south, pointing straight up from the middle of the face. */
	public static final PartialModel PRESSURIZER_NEEDLE = PartialModel.of(McRemix.asResource("block/pressurizer/needle"));

	/** Create's drill head with other bits; iron bits are Create's own head. */
	private static final Map<DrillBits, PartialModel> DRILL_HEADS = new EnumMap<>(DrillBits.class);

	/** How Create's saw blades are kept: lying flat (horizontal saws) or upright, each turning, reversed or still. */
	private static final String[] SAW_BLADE_MODELS = { "horizontal_active", "horizontal_reversed",
		"horizontal_inactive", "vertical_active", "vertical_reversed", "vertical_inactive" };
	/** Create's saw blade in other materials, in {@link #SAW_BLADE_MODELS} order; iron is Create's own. */
	private static final Map<SawBlades, PartialModel[]> SAW_BLADES = new EnumMap<>(SawBlades.class);

	/**
	 * Create's arm parts in each machine tier's look ({@link MachineTiers#look}), by the part of Create's
	 * they stand for; the arm's cog stays Create's. And the drill head with each kind of bits, its collar in the tier's
	 * colours.
	 */
	private static final Map<String, Map<PartialModel, PartialModel>> MACHINE_PARTS = new HashMap<>();
	private static final Map<String, Map<DrillBits, PartialModel>> MACHINE_DRILL_HEADS = new HashMap<>();

	/**
	 * What turns in each wood's movers, by the part of Create's it stands for: the wood's shaft and half shaft, and
	 * Create's bearing top, gantry cogs and gantry shafts in the wood's colours.
	 */
	private static final Map<WoodSpecies, Map<PartialModel, PartialModel>> MOVER_PARTS = new EnumMap<>(WoodSpecies.class);

	/** Create's pump cog in this mod's metals, modelled facing south. */
	private static final Map<FluidPartMaterial, PartialModel> PUMP_COGS = new EnumMap<>(FluidPartMaterial.class);

	/**
	 * A boiler's gauge and its needle in the metal of its tanks (Taran, 0.26.0), by their models' names: each engine
	 * metal's, iron's at each stage of rust, steel's at its one, and Create's copper weathered.
	 */
	private static final Map<String, PartialModel> BOILER_GAUGES = new HashMap<>();

	public static void init() {
		for (DrillBits bits : DrillBits.values())
			DRILL_HEADS.put(bits, bits == DrillBits.IRON ? AllPartialModels.DRILL_HEAD
				: PartialModel.of(McRemix.asResource("block/" + bits.itemName() + "/head")));
		SAW_BLADES.put(SawBlades.IRON, new PartialModel[] { AllPartialModels.SAW_BLADE_HORIZONTAL_ACTIVE,
			AllPartialModels.SAW_BLADE_HORIZONTAL_REVERSED, AllPartialModels.SAW_BLADE_HORIZONTAL_INACTIVE,
			AllPartialModels.SAW_BLADE_VERTICAL_ACTIVE, AllPartialModels.SAW_BLADE_VERTICAL_REVERSED,
			AllPartialModels.SAW_BLADE_VERTICAL_INACTIVE });
		for (SawBlades blades : SawBlades.values()) {
			if (blades == SawBlades.IRON)
				continue;
			PartialModel[] models = new PartialModel[SAW_BLADE_MODELS.length];
			for (int i = 0; i < models.length; i++)
				models[i] = PartialModel.of(McRemix.asResource("block/" + blades.itemName() + "/blade_"
					+ SAW_BLADE_MODELS[i]));
			SAW_BLADES.put(blades, models);
		}
		for (FluidPartMaterial material : FluidPartMaterial.values())
			if (material.hasPumpAndTank())
				PUMP_COGS.put(material, PartialModel.of(McRemix.asResource("block/" + material.pumpName() + "/cog")));
		for (WeatherState stage : WeatherState.values()) {
			List<String> prefixes = new ArrayList<>(List.of(gaugePrefix(null, stage)));
			for (FluidPartMaterial material : FluidPartMaterial.values())
				if (material.hasEngine() && (stage == WeatherState.UNAFFECTED
					|| material.rusts() && stage.ordinal() <= material.lastStage().ordinal()))
					prefixes.add(gaugePrefix(material, stage));
			for (String prefix : prefixes)
				if (!prefix.isEmpty())
					for (String part : List.of("gauge", "gauge_dial"))
						BOILER_GAUGES.put(prefix + part,
							PartialModel.of(McRemix.asResource("block/steam_engine/" + prefix + part)));
		}
		Map<PartialModel, String> parts = new HashMap<>();
		parts.put(AllPartialModels.ARM_BASE, "mechanical_arm/base");
		parts.put(AllPartialModels.ARM_LOWER_BODY, "mechanical_arm/lower_body");
		parts.put(AllPartialModels.ARM_UPPER_BODY, "mechanical_arm/upper_body");
		parts.put(AllPartialModels.ARM_CLAW_BASE, "mechanical_arm/claw_base");
		parts.put(AllPartialModels.ARM_CLAW_BASE_GOGGLES, "mechanical_arm/claw_base_goggles");
		parts.put(AllPartialModels.ARM_CLAW_GRIP_UPPER, "mechanical_arm/upper_claw_grip");
		parts.put(AllPartialModels.ARM_CLAW_GRIP_LOWER, "mechanical_arm/lower_claw_grip");
		for (String look : MachineTiers.looks()) {
			Map<PartialModel, PartialModel> tiered = new HashMap<>();
			parts.forEach((part, name) -> tiered.put(part,
				PartialModel.of(McRemix.asResource("block/" + look + "_" + name))));
			MACHINE_PARTS.put(look, tiered);
			Map<DrillBits, PartialModel> heads = new EnumMap<>(DrillBits.class);
			for (DrillBits bits : DrillBits.values())
				heads.put(bits, PartialModel.of(McRemix.asResource("block/" + look + "_mechanical_drill/"
					+ bits.itemName())));
			MACHINE_DRILL_HEADS.put(look, heads);
		}
		for (KineticVariant v : KineticFamilies.variants()) {
			String stage = turning(v);
			if (MODELS.containsKey(stage + "_shaft"))
				continue;
			put(stage + "_shaft");
			put(stage + "_shaft_half");
			if (v.material() == KineticMaterial.WOOD)
				continue;
			put(stage + "_cogwheel");
			put(stage + "_cogwheel_shaftless");
			put(stage + "_large_cogwheel_shaftless");
		}
		for (WoodSpecies wood : WoodSpecies.values()) {
			KineticVariant shaft = KineticVariant.wooden(wood, KineticPart.SHAFT);
			Map<PartialModel, PartialModel> mover = new HashMap<>();
			mover.put(AllPartialModels.SHAFT, shaftOf(shaft));
			mover.put(AllPartialModels.SHAFT_HALF, shaftHalfOf(shaft));
			mover.put(AllPartialModels.BEARING_TOP, moverPart(wood, AllPartialModels.BEARING_TOP));
			mover.put(AllPartialModels.GANTRY_COGS, moverPart(wood, AllPartialModels.GANTRY_COGS));
			for (PartialModel gantryShaft : AllPartialModels.GANTRY_SHAFTS.values())
				mover.put(gantryShaft, moverPart(wood, gantryShaft));
			MOVER_PARTS.put(wood, mover);
		}
	}

	/** Create's model {@code create:block/<path>} in a wood's colours: {@code mcremix:block/<wood>_<path>}. */
	private static PartialModel moverPart(WoodSpecies wood, PartialModel create) {
		String path = create.modelLocation().getPath().substring("block/".length());
		return PartialModel.of(McRemix.asResource("block/" + wood.id + "_" + path));
	}

	/**
	 * The name what turns in a part goes by, the part's own textures: titanium's shafts and cogs never tint, only what
	 * holds them does, so a blue gearbox turns plain titanium shafts.
	 */
	private static String turning(KineticVariant v) {
		return v.material().tints() ? v.withStage(WeatherState.UNAFFECTED).textureStageName() : v.textureStageName();
	}

	private static void put(String name) {
		MODELS.put(name, PartialModel.of(McRemix.asResource("block/" + name)));
	}

	/** The model that spins: the whole part for shafts and small cogs, the toothed ring alone for large cogs. */
	public static PartialModel main(KineticVariant v) {
		String suffix = v.part() == KineticPart.LARGE_COGWHEEL ? "_large_cogwheel_shaftless" : "_" + v.part().suffix;
		return MODELS.get(turning(v) + suffix);
	}

	/** The shaft running through a large cog, in the same material. */
	public static PartialModel shaftOf(KineticVariant v) {
		return MODELS.get(turning(v) + "_shaft");
	}

	/** Half a shaft in the same material, pointing south from the middle; one per open side of a gearbox or clutch. */
	public static PartialModel shaftHalfOf(KineticVariant v) {
		return MODELS.get(turning(v) + "_shaft_half");
	}

	/** The shaft in a casing, in its own material: Create's for Create's shaft. */
	public static PartialModel innerShaft(BlockState inner) {
		return inner.getBlock() instanceof TieredKinetic tiered ? shaftOf(tiered.variant()) : AllPartialModels.SHAFT;
	}

	/** A half of the shaft in a casing, for the sides of an encased cog a shaft meets. */
	public static PartialModel innerShaftHalf(BlockState inner) {
		return inner.getBlock() instanceof TieredKinetic tiered ? shaftHalfOf(tiered.variant())
			: AllPartialModels.SHAFT_HALF;
	}

	/** The cog in a casing without its shaft, in its own material: Create's for Create's cogs. */
	public static PartialModel innerCog(BlockState inner, boolean large) {
		if (inner.getBlock() instanceof TieredKinetic tiered)
			return MODELS.get(turning(tiered.variant()) + (large ? "_large_cogwheel_shaftless" : "_cogwheel_shaftless"));
		return large ? AllPartialModels.SHAFTLESS_LARGE_COGWHEEL : AllPartialModels.SHAFTLESS_COGWHEEL;
	}

	/** A small cog without its shaft, in a metal at a weathering stage: what turns on a vat mixer's plate. */
	public static PartialModel shaftlessCog(KineticMaterial material, WeatherState stage) {
		PartialModel cog = MODELS.get(turning(new KineticVariant(material, KineticPart.COGWHEEL, stage, false))
			+ "_cogwheel_shaftless");
		return cog != null ? cog : AllPartialModels.SHAFTLESS_COGWHEEL;
	}

	public static PartialModel pumpCog(FluidPartMaterial material) {
		return PUMP_COGS.get(material);
	}

	/**
	 * Create's boiler gauge or its needle ({@code create}) as a boiler of {@code tank} shows it: in the tank's metal and
	 * its stage of rust, or Create's copper as weathered as the tank. Create's own on a copper tank that has not
	 * weathered, and on anything else.
	 */
	public static PartialModel boilerGauge(PartialModel create, BlockState tank) {
		String part = create == AllPartialModels.BOILER_GAUGE ? "gauge"
			: create == AllPartialModels.BOILER_GAUGE_DIAL ? "gauge_dial" : null;
		FluidPartMaterial material = FluidPartMaterial.of(tank);
		if (part == null || material == null && !AllBlocks.FLUID_TANK.has(tank))
			return create;
		WeatherState stage = Patina.has(tank) ? tank.getValue(Patina.PROPERTY).stage() : WeatherState.UNAFFECTED;
		// steel that rusted further before 0.28.0 looks as rusted as steel gets now
		if (material != null && stage.ordinal() > material.lastStage().ordinal())
			stage = material.lastStage();
		PartialModel gauge = BOILER_GAUGES.get(gaugePrefix(material, stage) + part);
		return gauge != null ? gauge : create;
	}

	/**
	 * How the gauges are named: {@code brass_}, {@code rusted_iron_}; {@code weathered_} for Create's copper, and
	 * nothing for Create's copper as it comes.
	 */
	private static String gaugePrefix(@Nullable FluidPartMaterial material, WeatherState stage) {
		String weathered = stage == WeatherState.UNAFFECTED ? ""
			: (stage == WeatherState.OXIDIZED && material != null ? "rusted" : stage.name().toLowerCase(Locale.ROOT)) + "_";
		return material == null ? weathered : weathered + material.id + "_";
	}

	/** The head of a drill in {@code state} with these bits: in its tier's look, or Create's own drill's. */
	public static PartialModel drillHead(DrillBits bits, BlockState state) {
		String look = MachineTiers.look(state);
		return look == null ? DRILL_HEADS.get(bits) : MACHINE_DRILL_HEADS.get(look).get(bits);
	}

	/**
	 * Create's moving part {@code part} of the machine or mover in {@code state}: in its tier's look or its wood, or
	 * Create's own.
	 */
	public static PartialModel machinePart(PartialModel part, BlockState state) {
		if (state.getBlock() instanceof WoodenMover mover)
			return MOVER_PARTS.get(mover.wood()).getOrDefault(part, part);
		String look = MachineTiers.look(state);
		return look == null ? part : MACHINE_PARTS.get(look).getOrDefault(part, part);
	}

	/**
	 * What a wooden mover in {@code state} spins where Create's visual spins {@code model}: its wood's shaft, half shaft
	 * or gantry shaft for Create's. Anything else spins what Create gave it.
	 */
	public static Model moverModel(Model model, BlockState state) {
		if (state.getBlock() instanceof WoodenMover mover)
			for (Map.Entry<PartialModel, PartialModel> part : MOVER_PARTS.get(mover.wood()).entrySet())
				if (model == Models.partial(part.getKey()))
					return Models.partial(part.getValue());
		return model;
	}

	/**
	 * What Create's renderers draw turning through a wooden mover in {@code state}, where they would draw its block
	 * {@code rendered}: its wood's shaft for Create's shaft.
	 */
	public static BlockState moverShaft(BlockState rendered, BlockState state) {
		if (!(state.getBlock() instanceof WoodenMover mover) || !AllBlocks.SHAFT.has(rendered))
			return rendered;
		return KineticFamilies.get(KineticVariant.wooden(mover.wood(), KineticPart.SHAFT).name()).getDefaultState()
			.setValue(RotatedPillarKineticBlock.AXIS, rendered.getValue(RotatedPillarKineticBlock.AXIS));
	}

	/** The blade of a saw turning at {@code speed}: as Create picks, turning one way, the other, or still. */
	public static PartialModel sawBlade(SawBlades blades, boolean horizontal, float speed) {
		int spin = speed > 0 ? 0 : speed < 0 ? 1 : 2;
		return SAW_BLADES.get(blades)[(horizontal ? 0 : 3) + spin];
	}
}

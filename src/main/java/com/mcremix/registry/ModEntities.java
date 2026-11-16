package com.mcremix.registry;

import static com.mcremix.McRemix.REGISTRATE;

import com.mcremix.fluids.AmmoniaCloud;
import com.mcremix.fluids.ArgonCloud;
import com.mcremix.fluids.CarbonDioxideCloud;
import com.mcremix.fluids.ChlorineCloud;
import com.mcremix.fluids.EthyleneCloud;
import com.mcremix.fluids.FuelGasCloud;
import com.mcremix.fluids.GasCloud;
import com.mcremix.fluids.HydrogenChlorideMist;
import com.mcremix.fluids.NitrogenCloud;
import com.mcremix.fluids.NitrogenDioxideCloud;
import com.mcremix.fluids.OxygenCloud;
import com.mcremix.fluids.SolventVapourCloud;
import com.mcremix.fluids.SourGasCloud;
import com.mcremix.fluids.SulfurDioxideCloud;
import com.mcremix.fluids.VinylChlorideHaze;
import com.tterrag.registrate.util.entry.EntityEntry;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {
	// gas clouds, drawn only by their own particles
	public static final EntityEntry<ChlorineCloud> CHLORINE_CLOUD = REGISTRATE
		.<ChlorineCloud>entity("chlorine_cloud", ChlorineCloud::new, MobCategory.MISC)
		.properties(b -> b.sized(0.5f, 0.5f).clientTrackingRange(10).updateInterval(5).fireImmune())
		.renderer(() -> NoopRenderer::new)
		.register();
	public static final EntityEntry<EthyleneCloud> ETHYLENE_CLOUD = REGISTRATE
		.<EthyleneCloud>entity("ethylene_cloud", EthyleneCloud::new, MobCategory.MISC)
		.properties(b -> b.sized(0.5f, 0.5f).clientTrackingRange(10).updateInterval(5).fireImmune())
		.renderer(() -> NoopRenderer::new)
		.register();
	/** Solvent vapour escaping a steel column or its condenser: it flashes into flame at a fire. */
	public static final EntityEntry<SolventVapourCloud> SOLVENT_VAPOUR = REGISTRATE
		.<SolventVapourCloud>entity("solvent_vapour", SolventVapourCloud::new, MobCategory.MISC)
		.properties(b -> b.sized(0.5f, 0.5f).clientTrackingRange(10).updateInterval(5).fireImmune())
		.renderer(() -> NoopRenderer::new)
		.register();
	public static final EntityEntry<VinylChlorideHaze> VINYL_CHLORIDE_HAZE = REGISTRATE
		.<VinylChlorideHaze>entity("vinyl_chloride_haze", VinylChlorideHaze::new, MobCategory.MISC)
		.properties(b -> b.sized(0.5f, 0.5f).clientTrackingRange(10).updateInterval(5).fireImmune())
		.renderer(() -> NoopRenderer::new)
		.register();
	public static final EntityEntry<HydrogenChlorideMist> HYDROGEN_CHLORIDE_MIST = REGISTRATE
		.<HydrogenChlorideMist>entity("hydrogen_chloride_mist", HydrogenChlorideMist::new, MobCategory.MISC)
		.properties(b -> b.sized(0.5f, 0.5f).clientTrackingRange(10).updateInterval(5).fireImmune())
		.renderer(() -> NoopRenderer::new)
		.register();

	public static final EntityEntry<CarbonDioxideCloud> CARBON_DIOXIDE_CLOUD = REGISTRATE
		.<CarbonDioxideCloud>entity("carbon_dioxide_cloud", CarbonDioxideCloud::new, MobCategory.MISC)
		.properties(b -> b.sized(0.5f, 0.5f).clientTrackingRange(10).updateInterval(5).fireImmune())
		.renderer(() -> NoopRenderer::new)
		.register();

	// crude oil's gases (stage 3): sour gas poisons and flashes, fuel gas only flashes, and the sulfur dioxide burning
	// sour gas gives off chokes and kills plants
	public static final EntityEntry<SourGasCloud> SOUR_GAS_CLOUD = cloud("sour_gas_cloud", SourGasCloud::new);
	public static final EntityEntry<FuelGasCloud> FUEL_GAS_CLOUD = cloud("fuel_gas_cloud", FuelGasCloud::new);
	public static final EntityEntry<SulfurDioxideCloud> SULFUR_DIOXIDE_CLOUD = cloud("sulfur_dioxide_cloud",
		SulfurDioxideCloud::new);

	// the air plant's gases (stage 4): oxygen makes fire flare, nitrogen and argon smother, ammonia stings and nitrogen
	// dioxide poisons
	public static final EntityEntry<OxygenCloud> OXYGEN_CLOUD = cloud("oxygen_cloud", OxygenCloud::new);
	public static final EntityEntry<NitrogenCloud> NITROGEN_CLOUD = cloud("nitrogen_cloud", NitrogenCloud::new);
	public static final EntityEntry<ArgonCloud> ARGON_CLOUD = cloud("argon_cloud", ArgonCloud::new);
	public static final EntityEntry<AmmoniaCloud> AMMONIA_CLOUD = cloud("ammonia_cloud", AmmoniaCloud::new);
	public static final EntityEntry<NitrogenDioxideCloud> NITROGEN_DIOXIDE_CLOUD = cloud("nitrogen_dioxide_cloud",
		NitrogenDioxideCloud::new);

	private static <T extends GasCloud> EntityEntry<T> cloud(String name, EntityType.EntityFactory<T> factory) {
		return REGISTRATE.<T>entity(name, factory, MobCategory.MISC)
			.properties(b -> b.sized(0.5f, 0.5f).clientTrackingRange(10).updateInterval(5).fireImmune())
			.renderer(() -> NoopRenderer::new)
			.register();
	}

	public static void register() {}
}

package com.mcremix.registry;

import static com.mcremix.McRemix.REGISTRATE;

import com.mcremix.fluids.ChlorineCloud;
import com.mcremix.fluids.EthyleneCloud;
import com.mcremix.fluids.HydrogenChlorideMist;
import com.mcremix.fluids.VinylChlorideHaze;
import com.tterrag.registrate.util.entry.EntityEntry;

import net.minecraft.client.renderer.entity.NoopRenderer;
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

	public static void register() {}
}

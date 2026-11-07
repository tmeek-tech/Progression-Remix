package com.mcremix.fluids;

import java.util.function.Consumer;

import org.joml.Vector3f;

import com.mcremix.McRemix;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;

/** One of the chemical works' liquids ({@link Chemical}), drawn with its own textures. */
public class ChemicalFluidType extends FluidType {
	public final Chemical chemical;

	public ChemicalFluidType(Chemical chemical) {
		super(Properties.create()
			.descriptionId("fluid." + McRemix.ID + "." + chemical.id)
			.canSwim(!chemical.thick())
			.canExtinguish(true)
			.canConvertToSource(false)
			.supportsBoating(!chemical.thick())
			.motionScale(chemical.thick() ? 0.0023333333333333335D : 0.014D)
			.fallDistanceModifier(chemical.thick() ? 0.5f : 0f)
			.density(chemical.density)
			.viscosity(chemical.viscosity)
			.temperature(chemical.temperature)
			.lightLevel(chemical.light())
			.sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
			.sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY));
		this.chemical = chemical;
	}

	/**
	 * Forge calls this from {@link FluidType}'s constructor, before {@link #chemical} is set, so the chemical is only
	 * read once something asks (reading it here crashed every client at start-up in 0.14.0).
	 */
	@Override
	public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
		consumer.accept(new IClientFluidTypeExtensions() {
			@Override
			public ResourceLocation getStillTexture() {
				return McRemix.asResource("block/" + chemical.id + "_still");
			}

			@Override
			public ResourceLocation getFlowingTexture() {
				return McRemix.asResource("block/" + chemical.id + "_flow");
			}

			@Override
			public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance,
				float darkenWorldAmount, Vector3f fluidFogColor) {
				return new Vector3f((chemical.color >> 16 & 255) / 255f, (chemical.color >> 8 & 255) / 255f,
					(chemical.color & 255) / 255f);
			}

			/** A thick liquid is opaque a hand's breadth in; a thin one lets you see a few blocks. */
			@Override
			public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance, float partialTick,
				float nearDistance, float farDistance, FogShape shape) {
				RenderSystem.setShaderFogStart(chemical.thick() ? 0.25f : 0f);
				RenderSystem.setShaderFogEnd(chemical.thick() ? 1f : 6f);
			}
		});
	}
}

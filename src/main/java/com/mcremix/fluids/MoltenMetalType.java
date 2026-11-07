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
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;

/**
 * A molten metal: lava's weight and pace for anything wading in it, its own glow, and one shared glowing texture
 * tinted per metal.
 */
public class MoltenMetalType extends FluidType {
	private static final ResourceLocation STILL = McRemix.asResource("block/molten_metal_still");
	private static final ResourceLocation FLOW = McRemix.asResource("block/molten_metal_flow");

	public final MoltenMetal metal;

	public MoltenMetalType(MoltenMetal metal) {
		super(Properties.create()
			.descriptionId("fluid." + McRemix.ID + ".molten_" + metal.id)
			.canSwim(false)
			.canDrown(false)
			.canExtinguish(false)
			.supportsBoating(false)
			.canHydrate(false)
			.canConvertToSource(false)
			.pathType(BlockPathTypes.LAVA)
			.adjacentPathType(null)
			.motionScale(0.0023333333333333335D)
			.fallDistanceModifier(0.5f)
			.lightLevel(metal.light)
			.density(7000)
			.viscosity(6000)
			.temperature(metal.temperature)
			.sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
			.sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA));
		this.metal = metal;
	}

	@Override
	public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
		consumer.accept(new IClientFluidTypeExtensions() {
			@Override
			public ResourceLocation getStillTexture() {
				return STILL;
			}

			@Override
			public ResourceLocation getFlowingTexture() {
				return FLOW;
			}

			@Override
			public int getTintColor() {
				return metal.tint;
			}

			@Override
			public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance,
				float darkenWorldAmount, Vector3f fluidFogColor) {
				return new Vector3f((metal.tint >> 16 & 255) / 255f, (metal.tint >> 8 & 255) / 255f,
					(metal.tint & 255) / 255f);
			}

			/** As thick as lava: a quarter of a block in front of the eyes. */
			@Override
			public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance, float partialTick,
				float nearDistance, float farDistance, FogShape shape) {
				RenderSystem.setShaderFogStart(0.25f);
				RenderSystem.setShaderFogEnd(1f);
			}
		});
	}
}

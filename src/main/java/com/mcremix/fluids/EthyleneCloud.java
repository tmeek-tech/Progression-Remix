package com.mcremix.fluids;

import java.util.HashSet;
import java.util.Set;

import org.joml.Vector3f;

import com.mcremix.registry.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;

/**
 * Ethylene let loose: a faint warm haze, about as heavy as air, so it stays where it was let go, and gone again in a few
 * seconds. It is the gas that ripens fruit, so every crop under it grows a stage: wheat and the like, stems, cocoa and
 * sweet berries. Each grows once for each cloud, however long the cloud hangs over it.
 */
public class EthyleneCloud extends GasCloud {
	public static final DustParticleOptions HAZE = new DustParticleOptions(new Vector3f(0.94f, 0.9f, 0.84f), 1.5f);
	/** How long a bucket's worth lingers: not long. */
	public static final int TICKS_PER_BUCKET = 100;

	/** What it has ripened already. */
	private final Set<BlockPos> ripened = new HashSet<>();

	public EthyleneCloud(EntityType<? extends EthyleneCloud> type, Level level) {
		super(type, level);
	}

	/** {@code amount} mB of ethylene let loose at {@code at}: it feeds a cloud already there, or starts one. */
	public static void release(ServerLevel level, Vec3 at, int amount) {
		release(level, at, amount, ModEntities.ETHYLENE_CLOUD.get());
	}

	@Override
	protected int ticksPerBucket() {
		return TICKS_PER_BUCKET;
	}

	@Override
	protected boolean sinks() {
		return false;
	}

	@Override
	protected ParticleOptions haze() {
		return HAZE;
	}

	@Override
	protected void act(float radius) {
		for (BlockPos pos : under(radius))
			if (!ripened.contains(pos) && ripen(level(), pos))
				ripened.add(pos);
	}

	/** Grows a crop one stage, if it is one and not yet ripe. */
	public static boolean ripen(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		// the gas doesn't reach under water
		if (!state.getFluidState().isEmpty())
			return false;
		Block block = state.getBlock();
		BlockState grown;
		IntegerProperty age = ageOf(state);
		if (age == null)
			grown = null;
		else if (block instanceof CropBlock crop)
			grown = crop.isMaxAge(state) ? null : crop.getStateForAge(state.getValue(age) + 1);
		else if (block instanceof StemBlock || block instanceof CocoaBlock || block instanceof SweetBerryBushBlock)
			grown = older(state, age);
		else
			grown = null;
		if (grown == null)
			return false;
		level.setBlock(pos, grown, Block.UPDATE_CLIENTS);
		ForgeHooks.onCropsGrowPost(level, pos, state);
		return true;
	}

	/** The state a stage older, or null if it is as old as it gets. */
	private static BlockState older(BlockState state, IntegerProperty age) {
		int next = state.getValue(age) + 1;
		return age.getPossibleValues().contains(next) ? state.setValue(age, next) : null;
	}

	private static IntegerProperty ageOf(BlockState state) {
		for (Property<?> property : state.getProperties())
			if (property instanceof IntegerProperty age && age.getName().equals("age"))
				return age;
		return null;
	}
}

package com.mcremix.client;

import org.patryk3211.powergrid.electricity.particles.HvSparkSoundInstance;

import com.mcremix.kiln.KilnBlock;
import com.mcremix.kiln.SparkPlugBlock;
import com.mcremix.kiln.SparkPlugBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Client side of a sparking spark plug: Power Grid's crackle of an arc for as long as it sparks, sparks where it goes
 * into its furnace, and sparks in the furnace's mouth, where the spark lights the firebox.
 */
public class SparkPlugEffects {

	/** Every tick it sparks; {@code started} on the first. */
	public static void tick(SparkPlugBlockEntity be, boolean started) {
		Level level = be.getLevel();
		if (started)
			Minecraft.getInstance().getSoundManager().play(new HvSparkSoundInstance(be));
		RandomSource r = level.random;
		Direction toFurnace = be.getBlockState().getValue(SparkPlugBlock.FACING);
		Vec3 base = Vec3.atCenterOf(be.getBlockPos()).add(Vec3.atLowerCornerOf(toFurnace.getNormal()).scale(0.45))
			.add(0, -0.25, 0);
		if (r.nextInt(3) == 0)
			level.addParticle(ParticleTypes.ELECTRIC_SPARK, base.x + (r.nextDouble() - .5) * .25, base.y,
				base.z + (r.nextDouble() - .5) * .25, 0, 0, 0);
		BlockPos furnacePos = be.getBlockPos().relative(toFurnace);
		BlockState furnace = level.getBlockState(furnacePos);
		if (furnace.getBlock() instanceof KilnBlock && r.nextInt(2) == 0) {
			Vec3 mouth = Vec3.atCenterOf(furnacePos)
				.add(Vec3.atLowerCornerOf(furnace.getValue(KilnBlock.FACING).getNormal()).scale(0.4)).add(0, -0.3, 0);
			level.addParticle(ParticleTypes.ELECTRIC_SPARK, mouth.x + (r.nextDouble() - .5) * .4,
				mouth.y + r.nextDouble() * .15, mouth.z + (r.nextDouble() - .5) * .4, 0, 0, 0);
		}
	}
}

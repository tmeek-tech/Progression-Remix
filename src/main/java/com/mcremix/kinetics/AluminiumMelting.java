package com.mcremix.kinetics;

import org.joml.Vector3f;

import com.mcremix.fluids.FluidHeat;
import com.mcremix.fluids.MetalElectricPumpBlock;
import com.mcremix.fluids.MoltenMetal;
import com.mcremix.fluids.RatedPipeBlock;
import com.mcremix.fluids.RatedPumpBlock;
import com.mcremix.fluids.RatedTankBlock;
import com.mcremix.fluids.Spills;
import com.mcremix.heat.HeatLevel;
import com.mcremix.heat.HeatSources;
import com.mcremix.kiln.KilnBlock;
import com.mcremix.machines.MachineBody;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Aluminium melts low, so its parts do not last beside anything Hot on the Furnace's scale: lava, fire, a lit Furnace,
 * a Blaze Burner, or molten metal. One left beside the heat glows and drips for about five seconds, then melts, and the
 * aluminium in it runs down and sets as rough aluminium where it lands. An aluminium bracket melts off whatever shaft or
 * cog it holds the same way, and an aluminium casing off the part inside; an aluminium machine melts whole and drops
 * what it held. Runs from the same mixin as {@link KineticLimits}. Turning too fast heats them too
 * ({@link FrictionHeat}). Aluminium pipes and tanks melt beside the heat the same way, and with anything Hot inside
 * them, as {@link com.mcremix.fluids.Overheating} has them give way.
 */
public class AluminiumMelting {
	private static final int CHECK_INTERVAL = 5;
	/** About five seconds beside the heat, and up to a second more, so a line of parts goes one by one. */
	public static final int MELTS_AFTER = 100;
	/** A pipe's share of three ingots made into four, to the nugget. */
	private static final int PIPE_METAL = 60;
	/** How far the melt runs down to find something to set on. */
	private static final int MAX_DROP = 16;

	private static final DustParticleOptions GLOW = new DustParticleOptions(new Vector3f(1f, .42f, .12f), 1.2f);

	/** Called every tick on the server. Returns the new count of ticks it has spent beside the heat. */
	public static int tick(KineticBlockEntity be, int heatTicks) {
		Level level = be.getLevel();
		if (!(level instanceof ServerLevel server) || be.isVirtual() || be.isRemoved())
			return 0;
		BlockPos pos = be.getBlockPos();
		if ((level.getGameTime() + pos.asLong()) % CHECK_INTERVAL != 0)
			return heatTicks;
		BlockState state = be.getBlockState();
		boolean partMelts = KineticMaterials.of(state) == KineticMaterial.ALUMINIUM;
		BlockState bracket = FrictionPoints.isBare(state) ? FrictionPoints.bracket(level, pos) : null;
		boolean bracketMelts = bracket != null && KineticMaterials.of(bracket) == KineticMaterial.ALUMINIUM;
		if (!partMelts && !bracketMelts)
			return 0;
		if (!besideHeat(level, pos))
			return Math.max(0, heatTicks - CHECK_INTERVAL * 2);

		heatTicks += CHECK_INTERVAL;
		int limit = MELTS_AFTER + (int) Math.floorMod(pos.asLong() * 31, 20L);
		float urgency = heatTicks / (float) limit;
		glow(server, pos, urgency, urgency > .5f);
		if (heatTicks < limit)
			return heatTicks;
		int metal = 0;
		if (bracketMelts) {
			BracketedBlockEntityBehaviour behaviour = FrictionPoints.brackets(level, pos);
			if (behaviour != null && behaviour.removeBracket(false) != null)
				metal += metalIn(KineticPart.BRACKET);
		}
		if (partMelts) {
			metal += metalIn(state);
			// an aluminium casing melts off the part inside, which stays unless it is aluminium too; a machine drops
			// what it held
			BlockState inner = FrictionPoints.innerPart(be);
			if (inner != null && KineticMaterials.of(inner) != KineticMaterial.ALUMINIUM) {
				KineticBlockEntity.switchToBlockState(level, pos, inner);
			} else {
				if (inner != null)
					metal += metalIn(inner);
				level.removeBlock(pos, false);
			}
		}
		melt(server, pos, metal);
		return 0;
	}

	/** Whether anything beside {@code pos} is Hot or hotter: a heat source, a lit Furnace, or a hot fluid. */
	public static boolean besideHeat(Level level, BlockPos pos) {
		for (Direction side : Direction.values())
			if (hot(level, pos.relative(side)))
				return true;
		return false;
	}

	/**
	 * Whether anything round a tank {@code width} by {@code height} blocks from {@code origin} is Hot or hotter: an
	 * aluminium tank on a lit Blaze Burner, or beside lava, melts.
	 */
	public static boolean besideHeat(Level level, BlockPos origin, int width, int height) {
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++)
				for (int z = 0; z < width; z++) {
					if (y > 0 && y < height - 1 && x > 0 && x < width - 1 && z > 0 && z < width - 1)
						continue;
					BlockPos pos = origin.offset(x, y, z);
					for (Direction side : Direction.values()) {
						BlockPos next = pos.relative(side);
						int dx = next.getX() - origin.getX(), dy = next.getY() - origin.getY(),
							dz = next.getZ() - origin.getZ();
						boolean inside = dx >= 0 && dx < width && dy >= 0 && dy < height && dz >= 0 && dz < width;
						if (!inside && hot(level, next))
							return true;
					}
				}
		return false;
	}

	private static boolean hot(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		HeatLevel heat = HeatSources.heatAt(level, pos);
		if (state.getBlock() instanceof KilnBlock)
			heat = HeatLevel.max(heat, HeatLevel.byId(state.getValue(KilnBlock.HEAT)));
		FluidState fluid = state.getFluidState();
		if (!fluid.isEmpty())
			heat = HeatLevel.max(heat, FluidHeat.of(fluid.getType()));
		return heat.isAtLeast(HeatLevel.HOT);
	}

	/**
	 * The aluminium in a block that melts: a part's as below, a casing's (or a machine's, whose body is its casing) a
	 * sheet, and a pipe's, pump's or tank's as their recipes put it in: two sheets and an ingot to four pipes, a pipe
	 * and a cog to a pump, a pipe to an electric pump (with a motor), and two sheets to a tank.
	 */
	public static int metalIn(BlockState state) {
		if (state.getBlock() instanceof TieredKinetic tiered)
			return metalIn(tiered.variant().part());
		if (state.getBlock() instanceof TieredEncased || state.getBlock() instanceof MachineBody)
			return FrictionHeat.CASING_METAL;
		if (state.getBlock() instanceof RatedPipeBlock)
			return PIPE_METAL;
		if (state.getBlock() instanceof RatedPumpBlock)
			return PIPE_METAL + metalIn(KineticPart.COGWHEEL);
		if (state.getBlock() instanceof MetalElectricPumpBlock)
			return PIPE_METAL;
		if (state.getBlock() instanceof RatedTankBlock)
			return 2 * FrictionHeat.CASING_METAL;
		return 0;
	}

	/** The aluminium in a part, as its recipes put it in: six shafts to two ingots, a cog a shaft and an ingot more. */
	public static int metalIn(KineticPart part) {
		return switch (part) {
			case SHAFT, BRACKET -> 30;
			case COGWHEEL, CLUTCH, CHAIN_DRIVE, CHAIN_GEARSHIFT -> 120;
			case LARGE_COGWHEEL, GEARSHIFT -> 210;
			case GEARBOX -> 570;
		};
	}

	/** Glowing orange, the more the closer it is to melting, and dripping when {@code drips}. */
	static void glow(ServerLevel level, BlockPos pos, float urgency, boolean drips) {
		double x = pos.getX() + .5, y = pos.getY() + .5, z = pos.getZ() + .5;
		level.sendParticles(GLOW, x, y, z, 2 + (int) (urgency * 6), .3, .3, .3, 0);
		level.sendParticles(ParticleTypes.SMOKE, x, y, z, 1 + (int) (urgency * 2), .3, .3, .3, .01);
		if (drips)
			level.sendParticles(ParticleTypes.FALLING_LAVA, x, pos.getY() + .1, z, 1, .2, 0, .2, 0);
		level.playSound(null, pos, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, .3f + urgency * .4f,
			.8f + urgency * .6f);
	}

	/**
	 * The melt runs down from {@code pos} and sets as rough aluminium where it lands. Where a part still stands at
	 * {@code pos} with nothing open below it (the shaft a bracket bolted to the floor held), it runs off to one side.
	 */
	public static void melt(ServerLevel level, BlockPos pos, int metal) {
		double x = pos.getX() + .5, y = pos.getY() + .5, z = pos.getZ() + .5;
		level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 6, .2, .2, .2, .02);
		level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, .6f, .8f);
		if (metal <= 0 || Spills.addRough(level, runsDownFrom(level, pos), MoltenMetal.ALUMINIUM, metal) > 0)
			return;
		for (Direction side : Direction.Plane.HORIZONTAL)
			if (Spills.hasRoom(level, pos.relative(side))
				&& Spills.addRough(level, runsDownFrom(level, pos.relative(side)), MoltenMetal.ALUMINIUM, metal) > 0)
				return;
	}

	/** As far down from {@code pos} as melt falls before it finds something to set on. */
	private static BlockPos runsDownFrom(ServerLevel level, BlockPos pos) {
		BlockPos drop = pos;
		for (int i = 0; i < MAX_DROP && !level.isOutsideBuildHeight(drop.below()) && Spills.hasRoom(level, drop.below()); i++)
			drop = drop.below();
		return drop;
	}
}

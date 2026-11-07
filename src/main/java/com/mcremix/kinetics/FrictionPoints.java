package com.mcremix.kinetics;

import javax.annotation.Nullable;

import com.mcremix.fluids.Patina;
import com.mcremix.machines.MachineTiers;
import com.mcremix.machines.MachineBody;
import com.mcremix.registry.ModTags;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.gauge.GaugeBlock;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlock;
import com.simibubi.create.content.kinetics.simpleRelays.AbstractSimpleShaftBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedShaftBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Friction points (Taran's design, 0.15.0), called supports in game since 0.23.0: the blocks a shaft or cog turns in.
 * Machines that do work, gearboxes, clutches, gearshifts, chain drives, encased shafts and cogs, and shafts and cogs
 * with a bracket bolted to a solid block; a speed controller also holds the large cog in its top slot.
 * Sources (water wheels, windmills, engines, cranks, motors), belts and gauges pass rotation on but hold nothing.
 * <ul>
 * <li>A bare shaft or cog must be held: a friction point that connects to it along its axis must be within its
 * material's span, counting through the bare parts between them, like scaffolding reaching out from a post.</li>
 * <li>Only friction points have a top speed, their Stability (Taran, 0.27.0): a held part's is its bracket's or
 * casing's alone, whatever the part is made of, and a machine's or gearbox's its own material's.</li>
 * <li>Only shafts and cogs carry a limited load, their Strength: their own material's, bare, bracketed or encased.
 * Supports never give way under load.</li>
 * <li>Only friction points draw friction from the network.</li>
 * </ul>
 */
public final class FrictionPoints {
	private FrictionPoints() {}

	/** A bare shaft or cog, Create's or this mod's or anything built on Create's simple shaft. */
	public static boolean isBare(BlockState state) {
		return state.getBlock() instanceof AbstractSimpleShaftBlock;
	}

	/**
	 * A part that must be held: a bare one, or Create's andesite shaft while a steam engine drives it, which Create swaps
	 * for a powered shaft of its own. An engine holds nothing (Taran, 0.26.0), so that shaft is held as a bare one is,
	 * though like Create's it takes no bracket.
	 */
	public static boolean mustBeHeld(BlockState state) {
		return isBare(state) || AllBlocks.POWERED_SHAFT.has(state);
	}

	@Nullable
	public static BracketedBlockEntityBehaviour brackets(BlockGetter level, BlockPos pos) {
		return BlockEntityBehaviour.get(level, pos, BracketedBlockEntityBehaviour.TYPE);
	}

	/** The bracket on a part, or null. */
	@Nullable
	public static BlockState bracket(BlockGetter level, BlockPos pos) {
		BracketedBlockEntityBehaviour behaviour = brackets(level, pos);
		return behaviour == null ? null : behaviour.getBracket();
	}

	/** Whether the part here has a bracket bolted to a solid block, which makes it a friction point. */
	public static boolean hasBoltedBracket(BlockGetter level, BlockPos pos) {
		BlockState bracket = bracket(level, pos);
		return bracket != null && Brackets.isBolted(level, pos, bracket);
	}

	/**
	 * Whether a bare part sits in the top slot of a rotation speed controller, which supports it (Taran, 0.23.0): a large
	 * cog right above the controller, turning across its shaft, the one Create has the controller drive. It is held
	 * there as by a bolted bracket, and holds the parts along its axis within their span, but stays a bare part with no
	 * top speed of its own, so the controller can still drive it faster than the controller's own brass turns.
	 */
	public static boolean inSpeedControllerSlot(BlockGetter level, BlockPos pos, BlockState state) {
		if (!isBare(state) || !ICogWheel.isLargeCog(state))
			return false;
		BlockState below = level.getBlockState(pos.below());
		if (!AllBlocks.ROTATION_SPEED_CONTROLLER.has(below))
			return false;
		Axis axis = state.getValue(RotatedPillarKineticBlock.AXIS);
		return axis.isHorizontal() && axis != below.getValue(SpeedControllerBlock.HORIZONTAL_AXIS);
	}

	/** Whether this block is a friction point at all (whether it holds a given part is {@link #holds}). */
	public static boolean isFrictionPoint(BlockGetter level, BlockPos pos, BlockState state) {
		if (state.is(ModTags.Blocks.FRICTION_POINTS))
			return true;
		if (state.is(ModTags.Blocks.HOLDS_NO_SHAFTS))
			return false;
		if (isBare(state))
			return hasBoltedBracket(level, pos);
		if (!(state.getBlock() instanceof IRotate))
			return false;
		if (state.getBlock() instanceof BeltBlock || state.getBlock() instanceof GaugeBlock)
			return false;
		// sources hold nothing; a mechanical bearing is one only as a windmill
		BlockEntity be = level.getBlockEntity(pos);
		if (be instanceof GeneratingKineticBlockEntity
			&& (!(be instanceof MechanicalBearingBlockEntity) || be instanceof WindmillBearingBlockEntity))
			return false;
		return true;
	}

	/** Whether the friction point at {@code pos} takes a shaft on its side facing {@code towardPart}. */
	public static boolean holds(LevelReader level, BlockPos pos, BlockState state, Direction towardPart) {
		return state.getBlock() instanceof IRotate rotate && rotate.hasShaftTowards(level, pos, state, towardPart);
	}

	/** How far a bare part may reach from the friction point holding it: its material's span. */
	public static int span(BlockState state) {
		KineticMaterial material = KineticMaterials.of(state);
		return material != null ? material.span : KineticMaterial.ANDESITE.span;
	}

	/**
	 * Whether a part may stand here: anything that is not a bare shaft or cog may, and a bare one if it has a bolted
	 * bracket, is going into a bracket standing here, sits in a speed controller's top slot, or a friction point holds it
	 * within its span.
	 */
	public static boolean isHeld(LevelReader level, BlockPos pos, BlockState state) {
		if (!mustBeHeld(state))
			return true;
		if (hasBoltedBracket(level, pos) || Brackets.holdsPlaced(level, pos, state)
			|| inSpeedControllerSlot(level, pos, state))
			return true;
		return distanceToHolder(level, pos, state) > 0;
	}

	/**
	 * How far along its axis the nearest friction point that holds this bare part is, counting through the bare parts
	 * between them and only as far as its span; 0 if none is. A part at the edge of what is loaded counts as held, so
	 * lines do not fall apart as chunks unload around them.
	 */
	public static int distanceToHolder(LevelReader level, BlockPos pos, BlockState state) {
		Axis axis = state.getValue(RotatedPillarKineticBlock.AXIS);
		int span = span(state);
		int best = 0;
		for (AxisDirection sign : AxisDirection.values()) {
			Direction dir = Direction.fromAxisAndDirection(axis, sign);
			BlockPos.MutableBlockPos at = pos.mutable();
			for (int k = 1; k <= span; k++) {
				if (best > 0 && k >= best)
					break;
				at.move(dir);
				if (!level.hasChunkAt(at))
					return k;
				BlockState there = level.getBlockState(at);
				if (mustBeHeld(there)) {
					if (there.getValue(RotatedPillarKineticBlock.AXIS) != axis)
						break;
					if (hasBoltedBracket(level, at) || inSpeedControllerSlot(level, at, there)) {
						best = k;
						break;
					}
					continue;
				}
				if (isFrictionPoint(level, at, there) && holds(level, at, there, dir.getOpposite()))
					best = k;
				break;
			}
		}
		return best;
	}

	/**
	 * What a kinetic block's limits come from (Taran, 0.27.0): the material of what turns ({@code part}: a shaft or cog,
	 * or a gearbox's or machine's own) and, for a bracketed or encased part, of what holds it ({@code holder}). A support
	 * has a Stability, its top speed, which for a held part is its holder's alone; a shaft or cog has a Strength, the load
	 * it carries, which is its own wherever it is ({@code strength}). Nothing else has either.
	 */
	public record Limits(@Nullable KineticMaterial part, @Nullable KineticMaterial holder, boolean frictionPoint,
		boolean capped, boolean strength) {
		public static final Limits NONE = new Limits(null, null, false, false, false);

		/** A shaft or cog, bare or held by {@code holder}: its top speed its support's, if it is one, and its load its own. */
		static Limits ofPart(@Nullable KineticMaterial part, @Nullable KineticMaterial holder, boolean frictionPoint) {
			return new Limits(part, holder, frictionPoint, frictionPoint, true);
		}

		/** Anything else of a material: a top speed of its own where {@code capped}, and any load. */
		static Limits ofBlock(KineticMaterial own, boolean frictionPoint, boolean capped) {
			return new Limits(own, null, frictionPoint, capped, false);
		}

		/** The material whose top speed binds: a held part's bracket or casing, or the block's own; null where none does. */
		@Nullable
		public KineticMaterial speed() {
			return !capped ? null : holder != null ? holder : part;
		}

		/** The material whose load limit binds: a shaft or cog's own; null for anything else, which takes any load. */
		@Nullable
		public KineticMaterial load() {
			return strength ? part : null;
		}
	}

	public static Limits limitsFor(KineticBlockEntity be) {
		BlockState state = be.getBlockState();
		BlockPos pos = be.getBlockPos();
		KineticMaterial own = KineticMaterials.of(state);
		if (isBare(state)) {
			BlockState bracket = bracket(be.getLevel(), pos);
			if (bracket == null)
				return Limits.ofPart(own, null, false);
			return Limits.ofPart(own, KineticMaterials.of(bracket), Brackets.isBolted(be.getLevel(), pos, bracket));
		}
		BlockState inner = innerPart(be);
		if (inner != null) {
			KineticMaterial part = KineticMaterials.of(inner);
			return Limits.ofPart(part != null ? part : KineticMaterial.ANDESITE, own, true);
		}
		if (own == null)
			return Limits.NONE;
		// Create's crushing wheel holds no shafts, so it is no friction point, but turns no faster than its material
		if (MachineTiers.isCrushingWheel(state))
			return Limits.ofBlock(own, false, true);
		boolean support = isFrictionPoint(be.getLevel(), pos, state);
		return Limits.ofBlock(own, support, support);
	}

	/**
	 * The bare shaft or cog in a casing, this mod's encased blocks' or Create's (always Create's own part), or null
	 * if the block is not an encased part.
	 */
	@Nullable
	public static BlockState innerPart(KineticBlockEntity be) {
		if (be instanceof EncasedPart encased)
			return encased.innerPart();
		BlockState state = be.getBlockState();
		Block part = state.getBlock() instanceof EncasedCogwheelBlock cog
			? (cog.isLargeCog() ? AllBlocks.LARGE_COGWHEEL.get() : AllBlocks.COGWHEEL.get())
			: state.getBlock() instanceof EncasedShaftBlock ? AllBlocks.SHAFT.get() : null;
		return part == null ? null
			: part.defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, state.getValue(RotatedPillarKineticBlock.AXIS));
	}

	/**
	 * Friction a bracketed bare part draws, in stress per RPM: its bracket's material (iron and steel are heavy to turn)
	 * and a little more for each weathered part, bracket or shaft. A bare part with no bracket turns freely.
	 */
	public static float bracketFriction(BlockGetter level, BlockPos pos, BlockState part) {
		BlockState bracket = bracket(level, pos);
		if (bracket == null)
			return 0;
		float friction = weathering(part);
		if (bracket.getBlock() instanceof TieredKinetic tiered)
			friction += tiered.variant().friction();
		return friction;
	}

	/**
	 * The friction this mod has a block draw, in stress per RPM: a friction point's material and weathering (a bracket
	 * and the part in it, a casing and the part inside, a gearbox, clutch, gearshift or chain drive, a machine tier, a
	 * mixer's vat form), and nothing for anything else.
	 */
	public static float friction(KineticBlockEntity be) {
		BlockState state = be.getBlockState();
		if (isBare(state))
			return be.getLevel() == null ? 0 : bracketFriction(be.getLevel(), be.getBlockPos(), state);
		if (state.getBlock() instanceof TieredEncased encased && be instanceof EncasedPart part)
			return encased.casing().material.baseFriction + weathering(part.innerPart());
		if (state.getBlock() instanceof TieredKinetic tiered && tiered.variant().part().cased)
			return tiered.variant().friction();
		if (state.getBlock() instanceof MachineBody machine)
			return machine.material().baseFriction + weathering(state);
		return 0;
	}

	/** The friction a part's or machine's weathering adds where it turns in a friction point. */
	public static float weathering(BlockState state) {
		if (state.getBlock() instanceof TieredKinetic tiered)
			return tiered.variant().weatheringFriction();
		if (Patina.has(state) && state.getBlock() instanceof MachineBody)
			return KineticMaterial.weatheringFriction(state.getValue(Patina.PROPERTY).stage());
		return 0;
	}

	/** Tells the network a friction point's friction changed (a bracket fitted, removed or weathered, a machine weathered). */
	public static void frictionChanged(KineticBlockEntity be) {
		if (be.getLevel() == null || be.getLevel().isClientSide || !be.hasNetwork())
			return;
		be.getOrCreateNetwork().updateStressFor(be, be.calculateStressApplied());
	}
}

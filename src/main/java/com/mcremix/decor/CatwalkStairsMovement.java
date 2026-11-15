package com.mcremix.decor;

import com.mcremix.McRemix;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeps players and mobs inside the handrails of catwalk stairs where the game's own collision cannot. The game looks for
 * a block's collision only for what stands less than two blocks above that block's foot, and a rail stands 2.75 blocks
 * up ({@link CatwalkStairsBlock#shape}). Where one stairs meets the next down, a player on the upper one stands two
 * blocks above the lower one's foot, and one running down stands two and a half above the foot of the stairs two
 * below; once they rise or run off a tread over that stairs the game no longer sees its rail. And when a catwalk or
 * anything else overhead cuts a step up short, the game tries the move again from the raised box, and misses the rail
 * of the stairs one block under them, or, for one that steps up a whole block, as a mount or a player with step-up gear
 * does, the rail of the stairs they come back down into. Either way it would carry them into the rail and out through
 * it. So each tick anything that lives is moved back out of a rail it has got into ({@link #keepOutOfRails}): a player
 * as soon as they have moved, a mob before it moves again. And a jump from the stairs rises 0.44 of a block at most
 * ({@link #MAX_JUMP}), short of a rail's top beside it. Done on the jump rather than by the block's jump factor, which
 * the game takes from the block under the jumper's middle, and at a joint that is the air over the lower stairs. A jump
 * from anywhere else is not changed.
 */
@Mod.EventBusSubscriber(modid = McRemix.ID)
public class CatwalkStairsMovement {
	/** The upward speed a jump from catwalk stairs starts at, at most: it rises 0.44 of a block. */
	public static final double MAX_JUMP = 0.23;
	/** How high a rail stands above its stairs' foot to collide with, in blocks. */
	private static final double RAIL_TOP = CatwalkStairsBlock.RAIL_COLLISION / 16.0;
	/** How far into a rail a box must be to be moved out: a box set against one can be in it by a hundred-millionth. */
	private static final double OVERLAP = 1e-6;

	@SubscribeEvent
	public static void onJump(LivingEvent.LivingJumpEvent event) {
		LivingEntity jumper = event.getEntity();
		Vec3 motion = jumper.getDeltaMovement();
		if (motion.y > MAX_JUMP && onStairs(jumper))
			jumper.setDeltaMovement(motion.x, MAX_JUMP, motion.z);
	}

	/**
	 * Whether catwalk stairs are in the block its feet are in, or in the block under any part of them: the one it
	 * stands on, or the one under it as it leaves a tread's edge, whose tread is a block down.
	 */
	static boolean onStairs(Entity entity) {
		AABB box = entity.getBoundingBox();
		BlockPos from = BlockPos.containing(box.minX, box.minY - 1, box.minZ),
			to = BlockPos.containing(box.maxX - 1e-7, box.minY, box.maxZ - 1e-7);
		for (BlockPos pos : BlockPos.betweenClosed(from, to))
			if (entity.level().getBlockState(pos).getBlock() instanceof CatwalkStairsBlock)
				return true;
		return false;
	}

	/** A player, on the client that moves them, just after they move, so they are never seen in a rail. */
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END && event.player.isLocalPlayer() && !event.player.noPhysics)
			keepOutOfRails(event.player);
	}

	/** Anything else that lives, each tick before it moves, on the side that moves it: the server, or a rider's client. */
	@SubscribeEvent
	public static void onTick(LivingEvent.LivingTickEvent event) {
		LivingEntity entity = event.getEntity();
		if (!(entity instanceof Player) && !entity.noPhysics && entity.isControlledByLocalInstance())
			keepOutOfRails(entity);
	}

	/**
	 * Moves an entity out of a rail of catwalk stairs it has got into, to the side of the rail its middle is on, and stops
	 * it moving on into the rail. Only stairs at most two blocks under the block its feet are in can have let it in: the
	 * game looks for the collision of stairs two under only below two blocks above their foot, and when something
	 * overhead cuts a step up short it tries the move again from the raised box, which leaves out stairs one under too,
	 * and for one that steps up a whole block, the stairs its feet are in once it is let back down. It is left where it
	 * is if moving it would put it in something else, or if it is in a rail by less than {@link #OVERLAP}, as a box set
	 * against one can be by rounding.
	 *
	 * @return whether it was moved
	 */
	public static boolean keepOutOfRails(Entity entity) {
		AABB box = entity.getBoundingBox();
		int feet = Mth.floor(box.minY - 1e-7);
		Level level = entity.level();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		double dx = 0, dz = 0;
		for (int y = feet - 2; y <= feet; y++) {
			if (box.minY >= y + RAIL_TOP)
				continue;
			for (int x = Mth.floor(box.minX); x <= Mth.floor(box.maxX); x++)
				for (int z = Mth.floor(box.minZ); z <= Mth.floor(box.maxZ); z++) {
					BlockState state = level.getBlockState(pos.set(x, y, z));
					if (!(state.getBlock() instanceof CatwalkStairsBlock))
						continue;
					for (AABB rail : CatwalkStairsBlock.railBoxes(state)) {
						rail = rail.move(x, y, z);
						if (!box.intersects(rail))
							continue;
						// a rail is thin across the flight: out across it, to the side the entity's middle is on
						boolean acrossX = rail.getXsize() < rail.getZsize();
						double move = acrossX
							? box.getCenter().x < rail.getCenter().x ? rail.minX - box.maxX : rail.maxX - box.minX
							: box.getCenter().z < rail.getCenter().z ? rail.minZ - box.maxZ : rail.maxZ - box.minZ;
						if (Math.abs(move) < OVERLAP)
							continue;
						if (acrossX)
							dx = farther(dx, move);
						else
							dz = farther(dz, move);
					}
				}
		}
		if (dx == 0 && dz == 0 || Double.isNaN(dx) || Double.isNaN(dz) || !level.noCollision(entity, box.move(dx, 0, dz)))
			return false;
		Vec3 motion = entity.getDeltaMovement();
		entity.setPos(entity.getX() + dx, entity.getY(), entity.getZ() + dz);
		entity.setDeltaMovement(dx * motion.x < 0 ? 0 : motion.x, motion.y, dz * motion.z < 0 ? 0 : motion.z);
		return true;
	}

	/** The farther of two moves along one axis, or NaN for two that go opposite ways. */
	private static double farther(double a, double b) {
		if (a * b < 0 || Double.isNaN(a))
			return Double.NaN;
		return Math.abs(b) > Math.abs(a) ? b : a;
	}
}

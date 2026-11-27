package com.mcremix.village;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.mcremix.McRemix;
import com.mcremix.config.RemixConfig;
import com.mcremix.mixin.SinglePoolElementAccessor;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The dead walk their village (0.43.0, Taran's ask: ruined villages always spawn zombie villagers at night). Anywhere in
 * a village, every zombie or husk that would spawn is a zombie villager instead, as many and as often, while skeletons,
 * spiders and creepers come as they always do. It is vanilla's own spawning, as a witch hut gets its witches: monsters
 * need the dark, so it happens at night or under what is left of a roof, a ruin lit up stays clear, and difficulty and
 * mob caps work as ever. A zombie villager takes its kind from the biome, so a desert village's are desert villagers.
 * <p>
 * A village is any structure built of jigsaw pieces from templates under {@code village/}, the same ones that generate
 * as ruins ({@link RuinProcessor#forTemplate}), and anywhere in it is anywhere inside the box round all its pieces.
 * {@code zombieVillagersInRuins} in the server config switches it off.
 */
@Mod.EventBusSubscriber(modid = McRemix.ID)
public final class RuinSpawns {
	/**
	 * The zombie villager entry that takes the place of a zombie or husk entry, by weight and group size: always the
	 * same object for the same numbers, since a spawn is checked against the list where it lands by identity.
	 */
	private static final Map<List<Integer>, SpawnerData> VILLAGERS = new ConcurrentHashMap<>();
	/**
	 * Whether each structure is a village, from the first of its starts looked at (a world's structures are its
	 * registry's, so weakly held: a server started again has new ones).
	 */
	private static final Map<Structure, Boolean> VILLAGE_STRUCTURES = Collections.synchronizedMap(new WeakHashMap<>());
	/** Each village's box round its pieces, by its start, while the start is loaded. */
	private static final Map<StructureStart, BoundingBox> BOXES = Collections.synchronizedMap(new WeakHashMap<>());

	private RuinSpawns() {}

	@SubscribeEvent
	public static void onPotentialSpawns(LevelEvent.PotentialSpawns event) {
		if (event.getMobCategory() != MobCategory.MONSTER || !(event.getLevel() instanceof ServerLevel level)
			|| !RemixConfig.get(RemixConfig.ZOMBIE_VILLAGERS_IN_RUINS))
			return;
		List<SpawnerData> zombies = new ArrayList<>();
		for (SpawnerData data : event.getSpawnerDataList())
			if (data.type == EntityType.ZOMBIE || data.type == EntityType.HUSK)
				zombies.add(data);
		if (zombies.isEmpty() || !inRuin(level, event.getPos()))
			return;
		for (SpawnerData zombie : zombies) {
			event.removeSpawnerData(zombie);
			event.addSpawnerData(villager(zombie));
		}
	}

	/** A zombie villager entry as likely and in groups as big as a zombie or husk one. */
	static SpawnerData villager(SpawnerData zombie) {
		return VILLAGERS.computeIfAbsent(List.of(zombie.getWeight().asInt(), zombie.minCount, zombie.maxCount),
			key -> new SpawnerData(EntityType.ZOMBIE_VILLAGER, key.get(0), key.get(1), key.get(2)));
	}

	/**
	 * Whether a position is in a village: inside the box round all of a village's pieces. The natural spawner asks this
	 * at every try, so a structure found not to be a village (a mineshaft, a stronghold) is never loaded again to ask,
	 * as vanilla loads only the structures that have spawns of their own, and each village's box is worked out once.
	 */
	public static boolean inRuin(ServerLevel level, BlockPos pos) {
		StructureManager structures = level.structureManager();
		for (Map.Entry<Structure, LongSet> entry : structures.getAllStructuresAt(pos).entrySet()) {
			Structure structure = entry.getKey();
			if (Boolean.FALSE.equals(VILLAGE_STRUCTURES.get(structure)))
				continue;
			boolean[] found = { false };
			structures.fillStartsForStructure(structure, entry.getValue(), start -> {
				if (!found[0] && VILLAGE_STRUCTURES.computeIfAbsent(structure, key -> village(start))
					&& start.getBoundingBox().isInside(pos) && BOXES.computeIfAbsent(start, RuinSpawns::pieces).isInside(pos))
					found[0] = true;
			});
			if (found[0])
				return true;
		}
		return false;
	}

	/** Whether a structure is a village: one of its pieces comes from a template under {@code village/}. */
	static boolean village(StructureStart start) {
		for (StructurePiece piece : start.getPieces())
			if (piece instanceof PoolElementStructurePiece pool && pool.getElement() instanceof SinglePoolElement single
				&& RuinProcessor.forTemplate(((SinglePoolElementAccessor) single).mcremix$template()) != null)
				return true;
		return false;
	}

	/**
	 * The box round all a structure's pieces. Not its own bounding box, which a village's terrain adaptation widens by
	 * twelve blocks every way, caves below included.
	 */
	static BoundingBox pieces(StructureStart start) {
		List<StructurePiece> pieces = start.getPieces();
		BoundingBox box = pieces.get(0).getBoundingBox();
		int x0 = box.minX(), y0 = box.minY(), z0 = box.minZ(), x1 = box.maxX(), y1 = box.maxY(), z1 = box.maxZ();
		for (StructurePiece piece : pieces) {
			BoundingBox b = piece.getBoundingBox();
			x0 = Math.min(x0, b.minX());
			y0 = Math.min(y0, b.minY());
			z0 = Math.min(z0, b.minZ());
			x1 = Math.max(x1, b.maxX());
			y1 = Math.max(y1, b.maxY());
			z1 = Math.max(z1, b.maxZ());
		}
		return new BoundingBox(x0, y0, z0, x1, y1, z1);
	}
}

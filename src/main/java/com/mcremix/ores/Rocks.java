package com.mcremix.ores;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;

import com.mcremix.registry.ModOres;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Which rock a block is, and what a mineral's ore in it is: the lookup an ore body uses for every block it passes
 * through. Built once, the first time it is asked, when every block (Create's stones too) is registered.
 */
public final class Rocks {
	private static volatile Map<Block, Host> hosts;
	private static volatile Map<Mineral, Map<Host, BlockState>> ores;

	private Rocks() {}

	/** The rock a block is, or null if no ore sits in it. */
	public static Host host(BlockState state) {
		return hosts().get(state.getBlock());
	}

	/**
	 * The ore {@code mineral} makes of {@code rock}, keeping a pillar's axis, or null where the block is not a rock the
	 * mineral occurs in (air, water, another ore, or a rock of the wrong kind).
	 */
	public static BlockState ore(Mineral mineral, BlockState rock) {
		Host host = host(rock);
		if (host == null)
			return null;
		BlockState ore = ores().get(mineral).get(host);
		if (ore == null)
			return null;
		if (host.pillar() && rock.hasProperty(RotatedPillarBlock.AXIS) && ore.hasProperty(RotatedPillarBlock.AXIS))
			ore = ore.setValue(RotatedPillarBlock.AXIS, rock.getValue(RotatedPillarBlock.AXIS));
		return ore;
	}

	/** The ore {@code mineral} makes of {@code host}'s own block, or null where it does not occur in that rock. */
	public static BlockState ore(Mineral mineral, Host host) {
		return ores().get(mineral).get(host);
	}

	private static Map<Block, Host> hosts() {
		Map<Block, Host> map = hosts;
		if (map == null) {
			map = new IdentityHashMap<>();
			for (Host host : Host.values()) {
				Block block = ForgeRegistries.BLOCKS.getValue(host.block);
				if (block != null && block != net.minecraft.world.level.block.Blocks.AIR)
					map.put(block, host);
			}
			hosts = map;
		}
		return map;
	}

	private static Map<Mineral, Map<Host, BlockState>> ores() {
		Map<Mineral, Map<Host, BlockState>> map = ores;
		if (map == null) {
			map = new EnumMap<>(Mineral.class);
			for (Mineral mineral : Mineral.values()) {
				Map<Host, BlockState> byHost = new EnumMap<>(Host.class);
				for (Host host : mineral.hosts)
					byHost.put(host, ModOres.ore(mineral, host).defaultBlockState());
				map.put(mineral, byHost);
			}
			ores = map;
		}
		return map;
	}
}

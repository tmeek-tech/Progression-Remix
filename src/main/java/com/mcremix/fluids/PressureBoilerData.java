package com.mcremix.fluids;

import java.util.Arrays;
import java.util.List;

import com.mcremix.McRemix;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.fluids.tank.BoilerData;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * A steel tank's boiler. It is compact, a boiler level for every two tanks where Create's copper boiler needs four. It
 * boils no water and takes no heat from what is under it (Taran, 0.26.0): pressurised steam piped in from a Pressurizer
 * is all that runs it, every 10 mB a tick a level, and its goggles show its size and that steam. Steam its engines do
 * not use builds pressure: a steam whistle on it vents the spare steam, screaming while it does, and without one the
 * boiler warns, then bursts. Its tanks fall as items, what they held is gone, and the steam scalds whoever is near;
 * nothing around it is broken.
 */
public class PressureBoilerData extends BoilerData {
	/** Pressure is spare boiler levels over time: one spare level bursts it in 90 seconds, nine in 10. */
	public static final int BURST_AT = 1800;
	/** How fast pressure falls once nothing is spare, or while a whistle vents. */
	private static final int RELIEF_PER_TICK = 6;
	/** How far from the boiler's sides bursting steam scalds. */
	private static final int SCALD_REACH = 4;
	public static final ResourceKey<DamageType> SCALDING = ResourceKey.create(Registries.DAMAGE_TYPE,
		McRemix.asResource("scalding"));

	/** mB a tick of pressurised steam that makes a boiler level, of heat and water alike. */
	public static final int STEAM_PER_LEVEL = 10;
	/** Ticks between samples of the steam coming in, as Create samples water. */
	private static final int STEAM_SAMPLE_RATE = 5;
	/** The most steam counted in a sample, so a flood cannot overflow the sum. */
	private static final int MOST_GATHERED = 1_000_000;

	public int pressure;
	public boolean venting;
	/** Pressurised steam piped in since the last sample, and mB a tick of it in each of the last ten samples. */
	private int gatheredSteam;
	private final float[] steamOverTime = new float[10];
	private int steamTicks, steamIndex;
	/** mB a tick of steam coming in: the most of the last ten samples, so a pump's pulses do not flicker. */
	public float steamSupply;
	private int steamLevels;

	/** The boiler levels the steam piped in gives: its heat and its water both. */
	public int steamLevels() {
		return steamLevels;
	}

	/** It boils no water: the steam piped in stands for it. */
	@Override
	public int getMaxHeatLevelForWaterSupply() {
		return steamLevels;
	}

	/** Nor does a fire under it count for anything: the steam piped in is its heat as well. */
	@Override
	public boolean updateTemperature(FluidTankBlockEntity controller) {
		needsHeatLevelUpdate = false;
		int before = activeHeat;
		boolean passiveBefore = passiveHeat;
		activeHeat = steamLevels;
		passiveHeat = false;
		return activeHeat != before || passiveHeat != passiveBefore;
	}

	/** Every few ticks, how much steam came in, and whether that changes its levels. */
	private void sampleSteam() {
		if (--steamTicks > 0)
			return;
		steamTicks = STEAM_SAMPLE_RATE;
		steamOverTime[steamIndex] = gatheredSteam / (float) STEAM_SAMPLE_RATE;
		steamIndex = (steamIndex + 1) % steamOverTime.length;
		gatheredSteam = 0;
		float most = 0;
		for (float sample : steamOverTime)
			most = Math.max(most, sample);
		steamSupply = most;
		int levels = Math.min(18, Mth.ceil(steamSupply) / STEAM_PER_LEVEL);
		if (levels != steamLevels) {
			steamLevels = levels;
			needsHeatLevelUpdate = true;
		}
	}

	static boolean isSteam(FluidStack stack) {
		return Chemical.of(stack.getFluid()) == Chemical.PRESSURIZED_STEAM;
	}

	@Override
	public BoilerFluidHandler createHandler() {
		return new SteamHandler();
	}

	/** What pipes fill a working steel boiler through: pressurised steam alone, where Create's takes water. */
	public class SteamHandler extends BoilerFluidHandler {
		@Override
		public boolean isFluidValid(int tank, FluidStack stack) {
			return isSteam(stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			if (!isSteam(resource))
				return 0;
			if (action.execute())
				gatheredSteam = Math.min(MOST_GATHERED, gatheredSteam + resource.getAmount());
			return resource.getAmount();
		}
	}

	@Override
	public int getMaxHeatLevelForBoilerSize(int boilerSize) {
		return Math.min(18, boilerSize / 2);
	}

	/** The boiler levels of steam this boiler makes that its engines do not use. */
	public int spare(int boilerSize) {
		if (!isActive() || activeHeat == 0)
			return 0;
		int level = Math.min(activeHeat, Math.min(getMaxHeatLevelForWaterSupply(), getMaxHeatLevelForBoilerSize(boilerSize)));
		return Math.max(0, level - attachedEngines);
	}

	@Override
	public void tick(FluidTankBlockEntity controller) {
		if (isActive() && !controller.getLevel().isClientSide)
			sampleSteam();
		super.tick(controller);
		if (!(controller.getLevel() instanceof ServerLevel level))
			return;
		int spare = spare(controller.getTotalTankSize());
		boolean wasVenting = venting;
		venting = spare > 0 && attachedWhistles > 0;
		if (spare > 0 && !venting)
			pressure += spare;
		else
			pressure = Math.max(0, pressure - RELIEF_PER_TICK);
		if (venting != wasVenting)
			controller.notifyUpdate();
		if (pressure >= BURST_AT)
			burst(level, controller);
		else
			warn(level, controller);
	}

	/** Steam hissing from its seams, more and more often, and the steel groaning once it is near bursting. */
	private void warn(ServerLevel level, FluidTankBlockEntity controller) {
		float fill = pressure / (float) BURST_AT;
		if (fill < .3f)
			return;
		int every = fill < .6f ? 30 : fill < .85f ? 12 : 4;
		if (level.getGameTime() % every != 0)
			return;
		Vec3 seam = seam(controller, level.random);
		level.sendParticles(ParticleTypes.CLOUD, seam.x, seam.y, seam.z, 2 + (int) (fill * 6), .1, .1, .1, .02);
		AllSoundEvents.STEAM.playOnServer(level, BlockPos.containing(seam), .4f + fill, 1.2f + fill * .4f);
		if (fill >= .6f && level.random.nextInt(3) == 0)
			level.playSound(null, seam.x, seam.y, seam.z, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, .2f + fill * .3f,
				.5f);
	}

	/** A random spot on the boiler's outside. */
	private static Vec3 seam(FluidTankBlockEntity controller, RandomSource random) {
		int width = controller.getWidth(), height = controller.getHeight();
		double along = random.nextDouble() * width, up = random.nextDouble() * height;
		Vec3 corner = Vec3.atLowerCornerOf(controller.getBlockPos());
		return switch (random.nextInt(4)) {
			case 0 -> corner.add(along, up, -.05);
			case 1 -> corner.add(along, up, width + .05);
			case 2 -> corner.add(-.05, up, along);
			default -> corner.add(width + .05, up, along);
		};
	}

	/** The water flashes to steam and every tank of it falls as an item. */
	public void burst(ServerLevel level, FluidTankBlockEntity controller) {
		BlockPos origin = controller.getBlockPos();
		int width = controller.getWidth(), height = controller.getHeight();
		Vec3 centre = Vec3.atLowerCornerOf(origin).add(width / 2.0, height / 2.0, width / 2.0);
		pressure = 0;
		venting = false;
		controller.getTankInventory().drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
		level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 3, .6f);
		AllSoundEvents.STEAM.playOnServer(level, BlockPos.containing(centre), 3, .6f);
		level.sendParticles(ParticleTypes.EXPLOSION, centre.x, centre.y, centre.z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.CLOUD, centre.x, centre.y, centre.z, 40 * width * height, width / 2.0 + 1,
			height / 2.0 + 1, width / 2.0 + 1, .15);
		scald(level, origin, width, height, centre);
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++)
				for (int z = 0; z < width; z++) {
					BlockPos pos = origin.offset(x, y, z);
					if (FluidTankBlock.isTank(level.getBlockState(pos)))
						level.destroyBlock(pos, true);
				}
	}

	/** Bursting steam scalds what is near, worst against the boiler's sides. */
	private static void scald(ServerLevel level, BlockPos origin, int width, int height, Vec3 centre) {
		DamageSource steam = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
			.getHolderOrThrow(SCALDING));
		AABB boiler = new AABB(origin, origin.offset(width, height, width));
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, boiler.inflate(SCALD_REACH))) {
			double away = Math.max(0, Math.sqrt(boiler.distanceToSqr(entity.position())));
			entity.hurt(steam, (float) Math.max(2, 12 - 2.5 * away));
		}
	}

	/**
	 * Create's goggle lines with Steam in place of Water and Heat (Taran, 0.26.0), since the steam is both, and how near
	 * it is to bursting.
	 */
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking, int boilerSize) {
		if (!isActive())
			return false;
		calcMinMaxForSize(boilerSize);
		CreateLang.translate("boiler.status", getHeatLevelTextComponent().withStyle(ChatFormatting.GREEN))
			.forGoggles(tooltip);
		CreateLang.builder().add(getSizeComponent(true, false)).forGoggles(tooltip, 1);
		// Create draws water's bar from the levels of water, which here are the levels of steam
		CreateLang.builder().add(Component.translatable("mcremix.boiler.steam").withStyle(ChatFormatting.GRAY)
			.append(Component.translatable("mcremix.boiler.steam_dots").withStyle(ChatFormatting.DARK_GRAY))
			.append(getWaterComponent(false, false))).forGoggles(tooltip, 1);
		float fill = Math.min(1, pressure / (float) BURST_AT);
		ChatFormatting colour = fill < .3f ? ChatFormatting.GREEN : fill < .6f ? ChatFormatting.YELLOW
			: ChatFormatting.RED;
		CreateLang.builder().add(Component.translatable("mcremix.boiler.pressure").withStyle(ChatFormatting.GRAY)
			.append(Component.literal(" " + Math.round(fill * 100) + "%").withStyle(colour))).forGoggles(tooltip, 1);

		if (attachedEngines > 0) {
			tooltip.add(CommonComponents.EMPTY);
			int forSize = getMaxHeatLevelForBoilerSize(boilerSize);
			if (forSize > 0 && steamLevels == 0) {
				CreateLang.builder().add(Component.translatable("mcremix.boiler.steam_input_rate"))
					.style(ChatFormatting.GRAY)
					.forGoggles(tooltip);
				CreateLang.number(steamSupply)
					.style(ChatFormatting.BLUE)
					.add(CreateLang.translate("generic.unit.millibuckets"))
					.add(CreateLang.text(" / ").style(ChatFormatting.GRAY))
					.add(CreateLang.translate("boiler.per_tick", CreateLang.number(STEAM_PER_LEVEL)
						.add(CreateLang.translate("generic.unit.millibuckets"))).style(ChatFormatting.DARK_GRAY))
					.forGoggles(tooltip, 1);
			} else {
				int level = Math.min(steamLevels, forSize);
				double totalSU = getEngineEfficiency(boilerSize) * 16 * Math.max(level, attachedEngines)
					* BlockStressValues.getCapacity(AllBlocks.STEAM_ENGINE.get());
				CreateLang.translate("tooltip.capacityProvided").style(ChatFormatting.GRAY).forGoggles(tooltip);
				CreateLang.number(totalSU)
					.translate("generic.unit.stress")
					.style(ChatFormatting.AQUA)
					.space()
					.add((attachedEngines == 1 ? CreateLang.translate("boiler.via_one_engine")
						: CreateLang.translate("boiler.via_engines", attachedEngines)).style(ChatFormatting.DARK_GRAY))
					.forGoggles(tooltip, 1);
			}
		}

		int spare = spare(boilerSize);
		if (venting)
			tooltip.add(Component.literal("    ").append(Component.translatable("mcremix.boiler.venting")
				.withStyle(ChatFormatting.DARK_GRAY)));
		else if (spare > 0)
			tooltip.add(Component.literal("    ").append(Component.translatable("mcremix.boiler.building", spare)
				.withStyle(ChatFormatting.GOLD)));
		return true;
	}

	@Override
	public void clear() {
		super.clear();
		pressure = 0;
		venting = false;
		gatheredSteam = 0;
		Arrays.fill(steamOverTime, 0);
		steamSupply = 0;
		steamLevels = 0;
	}

	@Override
	public CompoundTag write() {
		CompoundTag tag = super.write();
		tag.putInt("Pressure", pressure);
		tag.putBoolean("Venting", venting);
		tag.putFloat("Steam", steamSupply);
		tag.putInt("SteamLevels", steamLevels);
		return tag;
	}

	@Override
	public void read(CompoundTag tag, int boilerSize) {
		// the steam first: Create's read works out the gauge from the levels, steam's among them
		steamSupply = tag.getFloat("Steam");
		steamLevels = tag.getInt("SteamLevels");
		Arrays.fill(steamOverTime, steamSupply);
		super.read(tag, boilerSize);
		pressure = tag.getInt("Pressure");
		venting = tag.getBoolean("Venting");
	}
}

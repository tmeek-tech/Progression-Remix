package com.mcremix.compat;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Optional Create: Power Grid touches, looked up at runtime so the mod is not needed at compile time.
 * Power Grid's heaters keep their temperature, in degrees Celsius, in a Create block entity behaviour named
 * "thermal" with a {@code getTemperature()} getter.
 */
public class PowerGridCompat {
	private static final String THERMAL = "thermal";
	private static final Map<Class<?>, Optional<Method>> GETTERS = new ConcurrentHashMap<>();

	/** The device's temperature in degrees Celsius, or NaN if it does not have one. */
	public static float temperature(@Nullable BlockEntity be) {
		if (!(be instanceof SmartBlockEntity smart))
			return Float.NaN;
		for (BlockEntityBehaviour behaviour : smart.getAllBehaviours()) {
			if (!THERMAL.equals(behaviour.getType().getName()))
				continue;
			Optional<Method> getter = GETTERS.computeIfAbsent(behaviour.getClass(), PowerGridCompat::findGetter);
			if (getter.isEmpty())
				return Float.NaN;
			try {
				return ((Number) getter.get().invoke(behaviour)).floatValue();
			} catch (ReflectiveOperationException | ClassCastException e) {
				return Float.NaN;
			}
		}
		return Float.NaN;
	}

	private static Optional<Method> findGetter(Class<?> type) {
		try {
			return Optional.of(type.getMethod("getTemperature"));
		} catch (NoSuchMethodException e) {
			return Optional.empty();
		}
	}
}

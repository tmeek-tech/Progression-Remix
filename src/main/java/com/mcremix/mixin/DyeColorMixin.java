package com.mcremix.mixin;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mcremix.colour.NewColour;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;

/**
 * Adds the eight coal tar colours to vanilla's list of dye colours, after its sixteen (ids 16 to 23), as soon as the
 * list is made, so that everything built from the list afterwards (vanilla's bed and shulker box textures, sheep fleece
 * colours, Create's dyed seats, valve handles, toolboxes and the rest) has them as it has vanilla's. Each constant is
 * named as its colour is (MALACHITE_GREEN is malachite_green), as Create names some of its models by the constant.
 */
@Mixin(DyeColor.class)
public abstract class DyeColorMixin {
	@Shadow @Final @Mutable private static IntFunction<DyeColor> BY_ID;
	@Shadow @Final @Mutable private static Int2ObjectOpenHashMap<DyeColor> BY_FIREWORK_COLOR;
	@Shadow @Final @Mutable public static StringRepresentable.EnumCodec<DyeColor> CODEC;

	@Inject(method = "<clinit>", at = @At("RETURN"))
	private static void mcremix$addColours(CallbackInfo ci) {
		DyeColor[] old = DyeColor.values();
		NewColour[] added = NewColour.values();
		DyeColor[] all = Arrays.copyOf(old, old.length + added.length);
		for (int i = 0; i < added.length; i++) {
			NewColour c = added[i];
			all[old.length + i] = DyeColorAccessor.mcremix$create(c.name(), old.length + i,
				old.length + i, c.id, c.diffuse, c.mapColor, c.firework, c.text);
		}
		setValues(all);
		BY_ID = ByIdMap.continuous(DyeColor::getId, all, ByIdMap.OutOfBoundsStrategy.ZERO);
		BY_FIREWORK_COLOR = new Int2ObjectOpenHashMap<>(Arrays.stream(all)
			.collect(Collectors.toMap(DyeColor::getFireworkColor, c -> c)));
		CODEC = StringRepresentable.fromEnum(DyeColor::values);
	}

	/** The enum's own array of its constants, which values() copies; found by type, as its name differs by mappings. */
	private static void setValues(DyeColor[] all) {
		try {
			Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
			unsafeField.setAccessible(true);
			sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
			for (Field f : DyeColor.class.getDeclaredFields()) {
				if (Modifier.isStatic(f.getModifiers()) && f.isSynthetic() && f.getType() == DyeColor[].class) {
					unsafe.putObjectVolatile(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f), all);
					return;
				}
			}
			throw new IllegalStateException("DyeColor has no array of its values");
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Could not add Minecraft Remix's dye colours", e);
		}
	}
}

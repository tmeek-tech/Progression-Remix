package com.mcremix.gametest;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/** Flattens tooltip lines to their translation keys and literal text, for asserting on. */
public final class Keys {
	private Keys() {}

	public static String of(List<Component> lines) {
		StringBuilder out = new StringBuilder();
		for (Component line : lines)
			collect(line, out);
		return out.toString();
	}

	public static String of(Component line) {
		StringBuilder out = new StringBuilder();
		collect(line, out);
		return out.toString();
	}

	private static void collect(Component c, StringBuilder out) {
		if (c.getContents() instanceof TranslatableContents t) {
			out.append('[').append(t.getKey()).append(']');
			for (Object arg : t.getArgs())
				if (arg instanceof Component inner)
					collect(inner, out);
				else
					out.append(arg);
		} else {
			out.append(c.getContents().toString());
		}
		for (Component sibling : c.getSiblings())
			collect(sibling, out);
	}
}

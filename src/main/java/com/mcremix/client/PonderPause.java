package com.mcremix.client;

import java.util.function.BooleanSupplier;

import org.lwjgl.glfw.GLFW;

import com.mcremix.McRemix;
import com.mojang.blaze3d.platform.InputConstants;

import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.ponder.foundation.ui.PonderButton;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Ponder's scenes can be paused (Taran, 0.28.0). A button beside Replay, or the forward key (W unless it was moved: Ponder's
 * own buttons take the other movement keys), stops the scene where it is, as Identify does, and leaves its text up to
 * read. Pressed again, the scene plays on; Replay and going to another scene play it too. The screen's side of it is
 * {@code com.mcremix.mixin.PonderUIMixin}.
 * <p>
 * Holding W over an item is also how Ponder opens, so the key is still down when the scene starts. Its repeats must not
 * pause it (0.28.0 did, so scenes opened that way started paused), so the key toggles only when pressed afresh.
 */
public final class PonderPause {
	/** The pause sign and the play sign, side by side, drawn as Ponder draws its own buttons (tools/gen_assets.py). */
	private static final ResourceLocation ICONS = McRemix.asResource("textures/gui/ponder_pause.png");
	public static final String PAUSE = "mcremix.ponder.ui.pause";
	public static final String PLAY = "mcremix.ponder.ui.play";

	private PonderPause() {}

	/** What Ponder's scene screen gains here. */
	public interface Pausable {
		boolean mcremix$paused();
	}

	/** The pause button: its key toggles it only when pressed afresh, not while held from opening Ponder, nor repeating. */
	public static class Button extends PonderButton {
		private boolean armed;

		public Button(int x, int y) {
			super(x, y);
			armed = !held();
		}

		/** Every tick of the screen: once the key is up, the next press counts. */
		public void tickKey() {
			if (!held())
				armed = true;
		}

		@Override
		public boolean keyPressed(int key, int scan, int modifiers) {
			if (shortcut != null && shortcut.matches(key, scan)) {
				if (!armed)
					return true;
				armed = false;
			}
			return super.keyPressed(key, scan, modifiers);
		}

		private static boolean held() {
			KeyMapping forward = Minecraft.getInstance().options.keyUp;
			InputConstants.Key bound = forward.getKey();
			long window = Minecraft.getInstance().getWindow().getWindow();
			return switch (bound.getType()) {
				case KEYSYM -> InputConstants.isKeyDown(window, bound.getValue());
				case MOUSE -> GLFW.glfwGetMouseButton(window, bound.getValue()) == GLFW.GLFW_PRESS;
				default -> false;
			};
		}
	}

	/** The button's icon: the pause sign while the scene plays, the play sign while it is paused. */
	public static ScreenElement icon(BooleanSupplier paused) {
		return (graphics, x, y) -> graphics.blit(ICONS, x, y, 0, paused.getAsBoolean() ? 16 : 0, 0, 16, 16, 32, 16);
	}
}

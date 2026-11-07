package com.mcremix.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mcremix.client.PonderPause;

import net.createmod.ponder.foundation.ui.AbstractPonderScreen;
import net.createmod.ponder.foundation.ui.PonderButton;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * A pause button on Ponder's scenes ({@link PonderPause}). Paused, the screen holds its scene the way Identify does:
 * the scene gets no ticks, and it, its text and everything in it that animates are drawn at the frame time it stopped
 * at ({@code PonderUI.ponderPartialTicksPaused}). Nothing else of Identify comes with it, so the text stays up.
 */
@Mixin(value = PonderUI.class, remap = false)
public abstract class PonderUIMixin extends AbstractPonderScreen implements PonderPause.Pausable {
	private static final String IDENTIFY_MODE = "Lnet/createmod/ponder/foundation/ui/PonderUI;identifyMode:Z";

	@Shadow
	private PonderButton replay;

	@Unique
	private boolean mcremix$paused;
	@Unique
	private PonderPause.Button mcremix$pause;

	@Override
	public boolean mcremix$paused() {
		return mcremix$paused;
	}

	/**
	 * Between Next and Replay, so the buttons that start and stop a scene sit together; there is room there on the
	 * narrowest screen. Its key is the forward key, as Ponder's own buttons take the other movement keys.
	 */
	@Inject(method = "init()V", at = @At("TAIL"), remap = true)
	private void mcremix$addPause(CallbackInfo ci) {
		mcremix$pause = new PonderPause.Button(replay.getX() - 28, replay.getY());
		mcremix$pause.withShortcut(Minecraft.getInstance().options.keyUp)
			.showing(PonderPause.icon(this::mcremix$paused))
			.enableFade(0, 5)
			.withCallback(this::mcremix$toggle);
		addRenderableWidget(mcremix$pause);
	}

	/** Ponder opens on a held W, so its key counts again only once it has been let go ({@link PonderPause.Button}). */
	@Inject(method = "tick()V", at = @At("HEAD"), remap = true)
	private void mcremix$watchKey(CallbackInfo ci) {
		if (mcremix$pause != null)
			mcremix$pause.tickKey();
	}

	/** It stops at the frame on screen: Identify's frozen frame time, if Identify has the scene held already. */
	@Unique
	private void mcremix$toggle() {
		if (!mcremix$paused)
			PonderUI.ponderPartialTicksPaused = PonderUI.getPartialTicks();
		mcremix$paused = !mcremix$paused;
	}

	@Inject(method = "replay", at = @At("HEAD"))
	private void mcremix$playOnReplay(CallbackInfo ci) {
		mcremix$paused = false;
	}

	/** Going to another scene plays it, as it turns Identify off. */
	@Inject(method = "scroll", at = @At("RETURN"))
	private void mcremix$playOnOtherScene(boolean forward, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ())
			mcremix$paused = false;
	}

	/** Paused, the scene gets no ticks... */
	@WrapOperation(method = "tick()V", remap = true,
		at = @At(value = "FIELD", target = IDENTIFY_MODE, opcode = Opcodes.GETFIELD, remap = false))
	private boolean mcremix$holdTicks(PonderUI ui, Operation<Boolean> original) {
		return original.call(ui) || mcremix$paused;
	}

	/** ...and is drawn at the frame time it stopped at, as are the buttons and bar under it... */
	@WrapOperation(method = "renderWindow", at = @At(value = "FIELD", target = IDENTIFY_MODE, opcode = Opcodes.GETFIELD))
	private boolean mcremix$holdFrame(PonderUI ui, Operation<Boolean> original) {
		return original.call(ui) || mcremix$paused;
	}

	/** ...and its text, which stays up: the first check, which hides it in Identify, is left alone... */
	@WrapOperation(method = "renderOverlay",
		at = @At(value = "FIELD", target = IDENTIFY_MODE, opcode = Opcodes.GETFIELD, ordinal = 1))
	private boolean mcremix$holdText(PonderUI ui, Operation<Boolean> original) {
		return original.call(ui) || mcremix$paused;
	}

	/** ...and whatever else asks Ponder for its frame time, such as Create's shafts turning in the scene. */
	@WrapOperation(method = "getPartialTicks",
		at = @At(value = "FIELD", target = IDENTIFY_MODE, opcode = Opcodes.GETFIELD))
	private static boolean mcremix$holdAnimations(PonderUI ui, Operation<Boolean> original) {
		return original.call(ui) || ((PonderPause.Pausable) ui).mcremix$paused();
	}

	/** Lit while paused, as Identify's and Slow Text's buttons are while they are on. */
	@Inject(method = "renderWidgets", at = @At("HEAD"))
	private void mcremix$lightPause(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
		if (mcremix$pause == null)
			return;
		if (mcremix$paused)
			mcremix$pause.flash();
		else
			mcremix$pause.dim();
	}

	/** Its name under it when hovered, where Ponder writes its own buttons' names. */
	@Inject(method = "renderHoverTooltips", at = @At("TAIL"))
	private void mcremix$pauseLabel(GuiGraphics graphics, int color, CallbackInfo ci) {
		if (mcremix$pause == null || !mcremix$pause.isHoveredOrFocused())
			return;
		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, 500);
		graphics.drawCenteredString(font, Component.translatable(mcremix$paused ? PonderPause.PLAY : PonderPause.PAUSE),
			mcremix$pause.getX() + 10, height - 16, color);
		graphics.pose().popPose();
	}
}

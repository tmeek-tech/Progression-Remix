package com.mcremix.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.util.FakePlayer;

/** A survival-mode server player that is not in the world, and remembers the action-bar messages it was sent. */
public class TestPlayer extends FakePlayer {
	public final List<Component> actionBar = new ArrayList<>();

	public TestPlayer(ServerLevel level) {
		super(level, new GameProfile(UUID.randomUUID(), "mcremix_test"));
	}

	@Override
	public void displayClientMessage(Component message, boolean actionBar) {
		if (actionBar)
			this.actionBar.add(message);
	}
}

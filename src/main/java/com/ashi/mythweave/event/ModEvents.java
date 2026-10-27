package com.ashi.mythweave.event;

import com.ashi.mythweave.Mythweave;
import com.ashi.mythweave.command.MythweaveCommands;
import com.ashi.mythweave.evolution.KindleEvents;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/**
 * Hooks Mythweave into game events, in one place: every Fabric event listener and the command
 * registration is wired up here, so it's easy to see what the mod reacts to.
 */
public final class ModEvents {
	private ModEvents() {
	}

	/** Called from {@link Mythweave#onInitialize()}, after all registries. */
	public static void register() {
		// Kindle from fights near Mythlings (hostile deaths, survived hits).
		KindleEvents.register();
		// /mythweave commands.
		CommandRegistrationCallback.EVENT.register(
				(dispatcher, buildContext, selection) -> MythweaveCommands.register(dispatcher));
	}
}

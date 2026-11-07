package com.mythweave.client;

import com.mythweave.Mythweave;
import com.mythweave.client.journal.MythlingJournalScreen;
import com.mythweave.client.registry.ModModelLayers;
import com.mythweave.client.registry.ModParticleProviders;
import com.mythweave.client.registry.ModRenderers;
import com.mythweave.network.OpenJournalPayload;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

/** Client entry point: client-only registrations. Never loaded on a dedicated server. */
public class MythweaveClient implements ClientModInitializer {
	private static final KeyMapping.Category KEY_CATEGORY =
			KeyMapping.Category.register(Mythweave.id("main"));

	private static final KeyMapping OPEN_JOURNAL = KeyMappingHelper.registerKeyMapping(
			new KeyMapping(
					"key.mythweave.journal",
					InputConstants.Type.KEYBOARD,
					InputConstants.KEY_J,
					KEY_CATEGORY
			)
	);

	@Override
	public void onInitializeClient() {
		// Model layers first: renderers bake them.
		ModModelLayers.register();
		ModRenderers.register();
		ModParticleProviders.register();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_JOURNAL.consumeClick()) {
				openJournal(client);
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(OpenJournalPayload.TYPE, (payload, context) ->
				openJournal(context.client()));
	}

	private static void openJournal(Minecraft client) {
		if (client.player == null || client.gui.screen() != null) {
			return;
		}
		client.gui.setScreen(new MythlingJournalScreen());
	}

	/** Whether this key event is the journal key, respecting the player's rebinds. */
	public static boolean isJournalKey(KeyEvent event) {
		return OPEN_JOURNAL.matches(event);
	}
}

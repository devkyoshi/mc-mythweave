package com.mythweave.journal;

import com.mythweave.entity.base.MythlingEntity;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * Wires the journal into the server: periodic discovery/tamed refresh, death cleanup, and
 * making sure a joining player has a journal attachment to sync.
 */
public final class JournalEvents {
	private JournalEvents() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(MythlingJournalService::tick);

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof MythlingEntity mythling) {
				MythlingJournalService.onDeath(mythling);
			}
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				handler.player.getAttachedOrCreate(JournalAttachments.JOURNAL));
	}
}

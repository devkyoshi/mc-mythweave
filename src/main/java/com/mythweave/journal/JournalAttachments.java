package com.mythweave.journal;

import com.mythweave.Mythweave;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * The player's Mythweave journal, stored with Fabric's data attachment API.
 *
 * <p>It is persistent (saved in the player file), survives death, and is synced only to the
 * player it belongs to. The client screen reads it directly from the local player.
 */
public final class JournalAttachments {
	public static final AttachmentType<JournalData> JOURNAL = AttachmentRegistry.create(
			Mythweave.id("journal"),
			builder -> builder
					.persistent(JournalData.CODEC)
					.copyOnDeath()
					.initializer(JournalData::empty)
					.syncWith(JournalData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	private JournalAttachments() {
	}

	/** Called from {@link Mythweave#onInitialize()}; loading this class registers the attachment. */
	public static void initialize() {
	}
}

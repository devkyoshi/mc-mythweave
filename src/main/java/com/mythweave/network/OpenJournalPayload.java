package com.mythweave.network;

import com.mythweave.Mythweave;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Tells the client to open the journal. Commands run on the server, so this tiny packet is
 * the bridge from {@code /mythweave journal} to the client screen.
 */
public record OpenJournalPayload() implements CustomPacketPayload {
	public static final OpenJournalPayload INSTANCE = new OpenJournalPayload();
	public static final Type<OpenJournalPayload> TYPE = new Type<>(Mythweave.id("open_journal"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenJournalPayload> STREAM_CODEC =
			StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

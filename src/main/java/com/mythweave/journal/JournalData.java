package com.mythweave.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Everything the journal shows for one player: which forms they have discovered, and a
 * snapshot of every Mythling they have bonded with.
 *
 * <p>Stored as a persistent, player-only Fabric data attachment. Immutable, so every change
 * replaces the whole record and the attachment API can sync it safely.
 */
public record JournalData(Set<String> discoveredForms, List<TamedMythling> tamed) {
	public static final JournalData EMPTY = new JournalData(Set.of(), List.of());

	public static final Codec<JournalData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.listOf()
					.optionalFieldOf("discovered", List.of())
					.forGetter(data -> List.copyOf(data.discoveredForms())),
			TamedMythling.CODEC.listOf()
					.optionalFieldOf("tamed", List.of())
					.forGetter(JournalData::tamed)
	).apply(instance, (discovered, tamed) -> new JournalData(new LinkedHashSet<>(discovered), List.copyOf(tamed))));

	public static final StreamCodec<RegistryFriendlyByteBuf, JournalData> STREAM_CODEC = StreamCodec.of(
			(buf, data) -> {
				buf.writeVarInt(data.discoveredForms().size());
				for (String form : data.discoveredForms()) {
					buf.writeUtf(form);
				}
				buf.writeVarInt(data.tamed().size());
				for (TamedMythling mythling : data.tamed()) {
					TamedMythling.STREAM_CODEC.encode(buf, mythling);
				}
			},
			buf -> {
				int discoveredCount = buf.readVarInt();
				Set<String> discovered = new LinkedHashSet<>();
				for (int i = 0; i < discoveredCount; i++) {
					discovered.add(buf.readUtf());
				}
				int tamedCount = buf.readVarInt();
				List<TamedMythling> tamed = new java.util.ArrayList<>(tamedCount);
				for (int i = 0; i < tamedCount; i++) {
					tamed.add(TamedMythling.STREAM_CODEC.decode(buf));
				}
				return new JournalData(discovered, tamed);
			}
	);

	public JournalData {
		discoveredForms = Set.copyOf(discoveredForms);
		tamed = List.copyOf(tamed);
	}

	public static JournalData empty() {
		return EMPTY;
	}

	public boolean isDiscovered(String formId) {
		return this.discoveredForms.contains(formId);
	}

	public JournalData withDiscovered(String formId) {
		if (isDiscovered(formId)) {
			return this;
		}
		Set<String> discovered = new LinkedHashSet<>(this.discoveredForms);
		discovered.add(formId);
		return new JournalData(discovered, this.tamed);
	}

	public JournalData withTamed(List<TamedMythling> tamed) {
		return new JournalData(this.discoveredForms, tamed);
	}
}

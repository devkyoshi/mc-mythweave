package com.mythweave.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/**
 * A small, immutable snapshot of one of a player's bonded Mythlings, as shown in the journal.
 *
 * <p>The entity itself remains the source of truth. This record exists so the journal can
 * still list a companion that is in an unloaded chunk (or another dimension) using its
 * last-known name, form, Kindle and health.
 *
 * @param id          the Mythling's UUID
 * @param formId      its entity id path, e.g. "cindercub"
 * @param name        its custom name, or "" if it has none
 * @param kindle      total Kindle
 * @param combat      Kindle earned by fighting (combat branch affinity)
 * @param exploration Kindle earned by exploring (exploration branch affinity)
 * @param sitting     whether it is ordered to sit
 * @param evolving    whether it is currently evolving
 * @param health      current health
 * @param maxHealth   maximum health
 */
public record TamedMythling(
		UUID id,
		String formId,
		String name,
		int kindle,
		int combat,
		int exploration,
		boolean sitting,
		boolean evolving,
		float health,
		float maxHealth
) {
	public static final Codec<TamedMythling> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.fieldOf("id").forGetter(TamedMythling::id),
			Codec.STRING.fieldOf("form").forGetter(TamedMythling::formId),
			Codec.STRING.optionalFieldOf("name", "").forGetter(TamedMythling::name),
			Codec.INT.optionalFieldOf("kindle", 0).forGetter(TamedMythling::kindle),
			Codec.INT.optionalFieldOf("combat", 0).forGetter(TamedMythling::combat),
			Codec.INT.optionalFieldOf("exploration", 0).forGetter(TamedMythling::exploration),
			Codec.BOOL.optionalFieldOf("sitting", false).forGetter(TamedMythling::sitting),
			Codec.BOOL.optionalFieldOf("evolving", false).forGetter(TamedMythling::evolving),
			Codec.FLOAT.optionalFieldOf("health", 0.0F).forGetter(TamedMythling::health),
			Codec.FLOAT.optionalFieldOf("max_health", 0.0F).forGetter(TamedMythling::maxHealth)
	).apply(instance, TamedMythling::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, TamedMythling> STREAM_CODEC = StreamCodec.of(
			(buf, value) -> {
				buf.writeUUID(value.id());
				buf.writeUtf(value.formId());
				buf.writeUtf(value.name());
				buf.writeVarInt(value.kindle());
				buf.writeVarInt(value.combat());
				buf.writeVarInt(value.exploration());
				buf.writeBoolean(value.sitting());
				buf.writeBoolean(value.evolving());
				buf.writeFloat(value.health());
				buf.writeFloat(value.maxHealth());
			},
			buf -> new TamedMythling(
					buf.readUUID(),
					buf.readUtf(),
					buf.readUtf(),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readBoolean(),
					buf.readBoolean(),
					buf.readFloat(),
					buf.readFloat()
			)
	);
}

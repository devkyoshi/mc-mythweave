package com.mythweave.registry;

import com.mythweave.Mythweave;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Mythweave sound events. What each one plays is defined in
 * {@code assets/mythweave/sounds.json}: for now they reuse vanilla fox sounds (by event, so
 * no audio files are shipped), with Mythweave's own subtitles. Cindercub raises the pitch.
 */
public final class ModSounds {
	public static final SoundEvent CINDERCUB_AMBIENT = register("entity.cindercub.ambient");
	public static final SoundEvent CINDERCUB_HURT = register("entity.cindercub.hurt");
	public static final SoundEvent CINDERCUB_DEATH = register("entity.cindercub.death");
	public static final SoundEvent EMBERLYNX_AMBIENT = register("entity.emberlynx.ambient");
	public static final SoundEvent EMBERLYNX_HURT = register("entity.emberlynx.hurt");
	public static final SoundEvent EMBERLYNX_DEATH = register("entity.emberlynx.death");
	public static final SoundEvent ASHENOX_AMBIENT = register("entity.ashenox.ambient");
	public static final SoundEvent ASHENOX_HURT = register("entity.ashenox.hurt");
	public static final SoundEvent ASHENOX_DEATH = register("entity.ashenox.death");
	public static final SoundEvent SOLFLARE_AMBIENT = register("entity.solflare.ambient");
	public static final SoundEvent SOLFLARE_HURT = register("entity.solflare.hurt");
	public static final SoundEvent SOLFLARE_DEATH = register("entity.solflare.death");

	private ModSounds() {
	}

	private static SoundEvent register(String name) {
		Identifier id = Mythweave.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	/** Called from {@link Mythweave#onInitialize()}; loading the class runs the registrations. */
	public static void initialize() {
	}
}

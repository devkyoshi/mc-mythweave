package com.mythweave.evolution;

import com.mythweave.Mythweave;

import net.minecraft.network.chat.Component;

/**
 * One species' evolution line: its three forms, in order. Each species has one in
 * {@link EvolutionLines} (e.g. {@code EvolutionLines.CINDERCUB}); every form of that species
 * returns it from {@link com.mythweave.entity.base.Mythling#evolutionLine()}.
 *
 * <p>Values are entity ids in the mythweave namespace (e.g. "ashenox"), so a form's name
 * is its entity name ({@code entity.mythweave.<id>} in the lang file).
 *
 * @param base   stage 1, e.g. "cindercub"
 * @param stage2 stage 2, e.g. "ashenox"
 * @param stage3 stage 3, the final form, e.g. "emberlynx"
 */
public record EvolutionLine(String base, String stage2, String stage3) {
	/** The display name of one of this line's forms. */
	public static Component formName(String formId) {
		return Component.translatable("entity." + Mythweave.MOD_ID + "." + formId);
	}
}

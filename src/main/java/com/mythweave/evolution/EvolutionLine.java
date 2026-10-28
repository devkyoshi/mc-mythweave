package com.mythweave.evolution;

import com.mythweave.Mythweave;

import net.minecraft.network.chat.Component;

/**
 * One species' evolution line: its base form, its stage-2 form, and the two stage-3 forms it
 * branches into. Each species has one in {@link EvolutionLines} (e.g. {@code EvolutionLines.CINDERCUB}); every
 * form of that species returns it from {@link com.mythweave.entity.base.Mythling#evolutionLine()}.
 *
 * <p>Values are entity ids in the mythweave namespace (e.g. "emberlynx"), so a form's name
 * is its entity name ({@code entity.mythweave.<id>} in the lang file).
 *
 * @param base            stage 1, e.g. "cindercub"
 * @param stage2          stage 2, e.g. "emberlynx"
 * @param combatForm      stage 3 reached by fighting together ({@link EvolutionRules.Branch#COMBAT})
 * @param explorationForm stage 3 reached by exploring together ({@link EvolutionRules.Branch#EXPLORATION})
 */
public record EvolutionLine(String base, String stage2, String combatForm, String explorationForm) {
	/** The display name of one of this line's forms. */
	public static Component formName(String formId) {
		return Component.translatable("entity." + Mythweave.MOD_ID + "." + formId);
	}

	/** The stage-3 form a branch leads to. Only meaningful for COMBAT and EXPLORATION. */
	public String formFor(EvolutionRules.Branch branch) {
		return branch == EvolutionRules.Branch.COMBAT ? this.combatForm : this.explorationForm;
	}
}

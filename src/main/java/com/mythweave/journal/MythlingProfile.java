package com.mythweave.journal;

import com.mythweave.evolution.EvolutionLine;

import net.minecraft.world.item.Item;

import java.util.List;

/**
 * The journal's static information about one Mythling form. Stats and evolution rules are
 * derived from the entity type and {@link com.mythweave.evolution.EvolutionRules}; only the
 * descriptive fields live here.
 *
 * @param formId         entity id path, e.g. "cindercub"
 * @param line           the evolution line this form belongs to
 * @param spawnEgg       item shown as the small journal icon
 * @param bondingItem    item that bonds a wild one
 * @param elementKeys    translation keys for the form's elements, e.g. fire or earth
 * @param rarityKey      translation key for the rarity line
 * @param habitatKey     translation key for the habitat line
 * @param descriptionKey translation key for the description
 * @param abilityKeys    translation keys for this form's abilities/traits
 */
public record MythlingProfile(
		String formId,
		EvolutionLine line,
		Item spawnEgg,
		Item bondingItem,
		List<String> elementKeys,
		String rarityKey,
		String habitatKey,
		String descriptionKey,
		List<String> abilityKeys
) {
	public MythlingProfile {
		elementKeys = List.copyOf(elementKeys);
		abilityKeys = List.copyOf(abilityKeys);
	}
}

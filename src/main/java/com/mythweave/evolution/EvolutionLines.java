package com.mythweave.evolution;

import java.util.List;

/**
 * Every species' evolution line, in one place (like a registry). Each form of a species
 * returns its line from {@code Mythling#evolutionLine()}. See docs/design/evolution.md.
 */
public final class EvolutionLines {
	/** Cindercub -> Ashenox -> Emberlynx. */
	public static final EvolutionLine CINDERCUB = new EvolutionLine("cindercub", "ashenox", "emberlynx");

	/** Every line in the mod, in the order the journal shows them. */
	public static final List<EvolutionLine> ALL = List.of(CINDERCUB);

	private EvolutionLines() {
	}
}

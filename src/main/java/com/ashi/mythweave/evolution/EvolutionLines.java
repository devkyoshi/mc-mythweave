package com.ashi.mythweave.evolution;

/**
 * Every species' evolution line, in one place (like a registry). Each form of a species
 * returns its line from {@code Mythling#evolutionLine()}. See docs/design/evolution.md.
 */
public final class EvolutionLines {
	/** Cindercub -> Emberlynx -> Ashenox (combat) or Solflare (exploration). */
	public static final EvolutionLine CINDERCUB = new EvolutionLine("cindercub", "emberlynx", "ashenox", "solflare");

	private EvolutionLines() {
	}
}

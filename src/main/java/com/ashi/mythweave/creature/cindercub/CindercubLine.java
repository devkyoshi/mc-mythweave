package com.ashi.mythweave.creature.cindercub;

import com.ashi.mythweave.mythling.evolution.EvolutionLine;

/**
 * The Cindercub line (docs/design/evolution.md):
 * Cindercub -> Emberlynx -> Ashenox (combat) or Solflare (exploration).
 */
public final class CindercubLine {
	public static final EvolutionLine LINE = new EvolutionLine("cindercub", "emberlynx", "ashenox", "solflare");

	private CindercubLine() {
	}
}

package com.ashi.mythweave.entity.base;

import com.ashi.mythweave.evolution.EvolutionLine;
import com.ashi.mythweave.evolution.Kindle;

/**
 * A creature that can bond and evolve. Every Mythling form extends {@link MythlingEntity},
 * which implements this, so evolution code works the same for all of them.
 */
public interface Mythling {
	/** This Mythling's Kindle (evolution progress). Server-side state. */
	Kindle kindle();

	/** The evolution line this form belongs to (shared by every form of the species). */
	EvolutionLine evolutionLine();
}

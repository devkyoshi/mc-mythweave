package com.ashi.mythweave.mythling;

import com.ashi.mythweave.mythling.evolution.EvolutionLine;
import com.ashi.mythweave.mythling.evolution.Kindle;

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

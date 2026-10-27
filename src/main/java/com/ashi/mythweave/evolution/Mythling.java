package com.ashi.mythweave.evolution;

/**
 * A creature that can bond and evolve. Implemented by Mythling entities (all of which are
 * {@link net.minecraft.world.entity.TamableAnimal}s) so evolution code works the same for
 * every form.
 */
public interface Mythling {
	/** This Mythling's Kindle (evolution progress). Server-side state. */
	Kindle kindle();
}

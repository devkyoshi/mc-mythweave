package com.ashi.mythweave.client.registry;

import com.ashi.mythweave.client.particle.EmberParticle;
import com.ashi.mythweave.registry.ModParticles;

import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;

/**
 * Tells the client how to draw each Mythweave particle type. Sprites come from
 * {@code assets/mythweave/particles/<name>.json}.
 */
public final class ModParticleProviders {
	private ModParticleProviders() {
	}

	public static void register() {
		ParticleProviderRegistry.getInstance().register(ModParticles.EMBER, EmberParticle.Provider::new);
	}
}

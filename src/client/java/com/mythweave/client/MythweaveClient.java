package com.mythweave.client;

import com.mythweave.client.registry.ModModelLayers;
import com.mythweave.client.registry.ModParticleProviders;
import com.mythweave.client.registry.ModRenderers;

import net.fabricmc.api.ClientModInitializer;

/** Client entry point: client-only registrations. Never loaded on a dedicated server. */
public class MythweaveClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Model layers first: renderers bake them.
		ModModelLayers.register();
		ModRenderers.register();
		ModParticleProviders.register();
	}
}

package com.ashi.mythweave.client;

import com.ashi.mythweave.registry.ModEntityTypes;

import net.fabricmc.api.ClientModInitializer;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;

public class MythweaveClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Every entity type needs a renderer on the client, or spawning one crashes the game.
		// Fabric's EntityRendererRegistry is deprecated in 26.3; vanilla's register is public.
		// Phase 1 placeholder: NoopRenderer draws nothing (use F3+B to see the hitbox).
		// Phase 2 replaces this with CindercubRenderer and a real model.
		EntityRenderers.register(ModEntityTypes.CINDERCUB, NoopRenderer::new);
	}
}
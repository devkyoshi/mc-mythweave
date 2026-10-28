package com.mythweave.client.registry;

import com.mythweave.client.MythweaveClient;
import com.mythweave.client.render.entity.AshenoxRenderer;
import com.mythweave.client.render.entity.CindercubRenderer;
import com.mythweave.client.render.entity.EmberlynxRenderer;
import com.mythweave.client.render.entity.SolflareRenderer;
import com.mythweave.registry.ModEntities;

import net.minecraft.client.renderer.entity.EntityRenderers;

/**
 * Every entity renderer, in one place. Each entity type needs a renderer on the client, or
 * spawning one crashes the game.
 */
public final class ModRenderers {
	private ModRenderers() {
	}

	/** Called from {@link MythweaveClient#onInitializeClient()}, after {@link ModModelLayers}. */
	public static void register() {
		// Fabric's EntityRendererRegistry is deprecated in 26.3; vanilla's register is public.
		EntityRenderers.register(ModEntities.CINDERCUB, CindercubRenderer::new);
		EntityRenderers.register(ModEntities.EMBERLYNX, EmberlynxRenderer::new);
		EntityRenderers.register(ModEntities.ASHENOX, AshenoxRenderer::new);
		EntityRenderers.register(ModEntities.SOLFLARE, SolflareRenderer::new);
	}
}

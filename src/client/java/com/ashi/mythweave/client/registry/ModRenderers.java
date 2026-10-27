package com.ashi.mythweave.client.registry;

import com.ashi.mythweave.client.MythweaveClient;
import com.ashi.mythweave.client.render.entity.AshenoxRenderer;
import com.ashi.mythweave.client.render.entity.CindercubRenderer;
import com.ashi.mythweave.client.render.entity.EmberlynxRenderer;
import com.ashi.mythweave.registry.ModEntities;

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
	}
}

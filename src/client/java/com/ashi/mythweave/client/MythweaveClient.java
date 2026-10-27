package com.ashi.mythweave.client;

import com.ashi.mythweave.client.model.CindercubModel;
import com.ashi.mythweave.client.render.CindercubRenderer;
import com.ashi.mythweave.registry.ModEntityTypes;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

import net.minecraft.client.renderer.entity.EntityRenderers;

public class MythweaveClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// The model's cube layout must be registered as a layer so the renderer can "bake" it.
		ModelLayerRegistry.registerModelLayer(CindercubModel.LAYER, CindercubModel::createBodyLayer);

		// Every entity type needs a renderer on the client, or spawning one crashes the game.
		// Fabric's EntityRendererRegistry is deprecated in 26.3; vanilla's register is public.
		EntityRenderers.register(ModEntityTypes.CINDERCUB, CindercubRenderer::new);
	}
}

package com.ashi.mythweave.client.registry;

import com.ashi.mythweave.Mythweave;
import com.ashi.mythweave.client.MythweaveClient;
import com.ashi.mythweave.client.model.entity.CindercubModel;

import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

import net.minecraft.client.model.geom.ModelLayerLocation;

/**
 * Every entity model layer, in one place. A layer is a model's cube layout, registered so a
 * renderer can "bake" it ({@code context.bakeLayer(ModModelLayers.CINDERCUB)}).
 */
public final class ModModelLayers {
	public static final ModelLayerLocation CINDERCUB = new ModelLayerLocation(Mythweave.id("cindercub"), "main");

	private ModModelLayers() {
	}

	/** Called from {@link MythweaveClient#onInitializeClient()}, before {@link ModRenderers}. */
	public static void register() {
		ModelLayerRegistry.registerModelLayer(CINDERCUB, CindercubModel::createBodyLayer);
	}
}

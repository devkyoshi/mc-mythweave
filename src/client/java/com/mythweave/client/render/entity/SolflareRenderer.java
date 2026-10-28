package com.mythweave.client.render.entity;

import com.mythweave.Mythweave;
import com.mythweave.client.model.entity.SolflareModel;
import com.mythweave.client.registry.ModModelLayers;
import com.mythweave.client.render.entity.layer.PulsingGlowLayer;
import com.mythweave.client.render.entity.state.SolflareRenderState;
import com.mythweave.entity.solflare.SolflareEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/** Draws a Solflare with {@link SolflareModel} and its pulsing glow (eyes, crest, wings, tail, sun markings). Appearance only. */
public class SolflareRenderer extends MobRenderer<SolflareEntity, SolflareRenderState, SolflareModel> {
	private static final Identifier TEXTURE = Mythweave.id("textures/entity/solflare/solflare.png");
	/** Transparent except the parts that glow (eyes, crest, wings, tail, sun markings). */
	private static final Identifier GLOW_TEXTURE = Mythweave.id("textures/entity/solflare/solflare_glow.png");

	public SolflareRenderer(EntityRendererProvider.Context context) {
		// 0.5 = shadow radius in blocks at scale 1 (vanilla multiplies it by the SCALE attribute).
		super(context, new SolflareModel(context.bakeLayer(ModModelLayers.SOLFLARE)), 0.5F);
		this.addLayer(new PulsingGlowLayer<>(this, GLOW_TEXTURE));
	}

	@Override
	public SolflareRenderState createRenderState() {
		return new SolflareRenderState();
	}

	@Override
	public void extractRenderState(SolflareEntity entity, SolflareRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.isSitting = entity.isInSittingPose();
	}

	@Override
	public Identifier getTextureLocation(SolflareRenderState state) {
		return TEXTURE;
	}
}

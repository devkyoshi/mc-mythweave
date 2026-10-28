package com.ashi.mythweave.client.render.entity;

import com.ashi.mythweave.Mythweave;
import com.ashi.mythweave.client.model.entity.CindercubModel;
import com.ashi.mythweave.client.registry.ModModelLayers;
import com.ashi.mythweave.client.render.entity.layer.PulsingGlowLayer;
import com.ashi.mythweave.client.render.entity.state.CindercubRenderState;
import com.ashi.mythweave.entity.cindercub.CindercubEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Draws a Cindercub with {@link CindercubModel}. Appearance only, no gameplay logic.
 *
 * <p>26.3 renderers don't read the entity while drawing: each frame,
 * {@link #extractRenderState} copies what's needed into a {@link CindercubRenderState},
 * and the model animates from that.
 */
public class CindercubRenderer extends MobRenderer<CindercubEntity, CindercubRenderState, CindercubModel> {
	private static final Identifier TEXTURE = Mythweave.id("textures/entity/cindercub/cindercub.png");
	/** Transparent except the parts that glow (the ember tail and the eye shine). */
	private static final Identifier GLOW_TEXTURE = Mythweave.id("textures/entity/cindercub/cindercub_glow.png");

	public CindercubRenderer(EntityRendererProvider.Context context) {
		// 0.35 = shadow radius in blocks, sized for the 0.6-wide hitbox.
		super(context, new CindercubModel(context.bakeLayer(ModModelLayers.CINDERCUB)), 0.35F);
		this.addLayer(new PulsingGlowLayer<>(this, GLOW_TEXTURE));
	}

	@Override
	public CindercubRenderState createRenderState() {
		return new CindercubRenderState();
	}

	@Override
	public void extractRenderState(CindercubEntity entity, CindercubRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.isSitting = entity.isInSittingPose();
	}

	@Override
	public Identifier getTextureLocation(CindercubRenderState state) {
		return TEXTURE;
	}
}

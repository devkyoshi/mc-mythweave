package com.ashi.mythweave.client.render;

import com.ashi.mythweave.Mythweave;
import com.ashi.mythweave.client.model.CindercubModel;
import com.ashi.mythweave.entity.CindercubEntity;

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
	private static final Identifier TEXTURE = Mythweave.id("textures/entity/cindercub.png");

	public CindercubRenderer(EntityRendererProvider.Context context) {
		// 0.3 = shadow radius in blocks, sized for the 0.5-wide hitbox.
		super(context, new CindercubModel(context.bakeLayer(CindercubModel.LAYER)), 0.3F);
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

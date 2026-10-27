package com.ashi.mythweave.client.render;

import com.ashi.mythweave.Mythweave;
import com.ashi.mythweave.client.model.CindercubModel;
import com.ashi.mythweave.entity.CindercubEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Draws a Cindercub with {@link CindercubModel}. Appearance only, no gameplay logic.
 *
 * <p>26.3 renderers don't read the entity while drawing: each frame,
 * {@code extractRenderState} copies what's needed into a render state object, and the
 * model animates from that. The vanilla {@link LivingEntityRenderState} covers walking
 * and head rotation; a custom state is only needed once the model shows extra state
 * (e.g. sitting, in Phase 4).
 */
public class CindercubRenderer extends MobRenderer<CindercubEntity, LivingEntityRenderState, CindercubModel> {
	private static final Identifier TEXTURE = Mythweave.id("textures/entity/cindercub.png");

	public CindercubRenderer(EntityRendererProvider.Context context) {
		// 0.3 = shadow radius in blocks, sized for the 0.5-wide hitbox.
		super(context, new CindercubModel(context.bakeLayer(CindercubModel.LAYER)), 0.3F);
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return TEXTURE;
	}
}

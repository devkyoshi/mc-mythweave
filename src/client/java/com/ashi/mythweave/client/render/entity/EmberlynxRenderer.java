package com.ashi.mythweave.client.render.entity;

import com.ashi.mythweave.Mythweave;
import com.ashi.mythweave.client.model.entity.EmberlynxModel;
import com.ashi.mythweave.client.registry.ModModelLayers;
import com.ashi.mythweave.client.render.entity.layer.PulsingGlowLayer;
import com.ashi.mythweave.client.render.entity.state.EmberlynxRenderState;
import com.ashi.mythweave.entity.emberlynx.EmberlynxEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/** Draws an Emberlynx with {@link EmberlynxModel} and its pulsing glow. Appearance only. */
public class EmberlynxRenderer extends MobRenderer<EmberlynxEntity, EmberlynxRenderState, EmberlynxModel> {
	private static final Identifier TEXTURE = Mythweave.id("textures/entity/emberlynx/emberlynx.png");
	/** Transparent except the parts that glow (runes, eyes, crystals, flames). */
	private static final Identifier GLOW_TEXTURE = Mythweave.id("textures/entity/emberlynx/emberlynx_glow.png");

	public EmberlynxRenderer(EntityRendererProvider.Context context) {
		// 0.4 = shadow radius in blocks, sized for the 0.6-wide hitbox.
		super(context, new EmberlynxModel(context.bakeLayer(ModModelLayers.EMBERLYNX)), 0.4F);
		this.addLayer(new PulsingGlowLayer<>(this, GLOW_TEXTURE));
	}

	@Override
	public EmberlynxRenderState createRenderState() {
		return new EmberlynxRenderState();
	}

	@Override
	public void extractRenderState(EmberlynxEntity entity, EmberlynxRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.isSitting = entity.isInSittingPose();
	}

	@Override
	public Identifier getTextureLocation(EmberlynxRenderState state) {
		return TEXTURE;
	}
}

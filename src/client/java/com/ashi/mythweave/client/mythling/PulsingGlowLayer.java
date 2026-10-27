package com.ashi.mythweave.client.mythling;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * A Mythling's magic glow, for any creature: draws the model a second time with a glow map
 * (a texture that is transparent except the parts that should glow: runes, eyes, crystals,
 * flames), using the full-bright "eyes" render type (the one spider and enderman eyes use),
 * so those parts stay lit in the dark.
 *
 * <p>The glow "breathes": its opacity rises and falls slowly, like embers. Purely visual.
 * Add it in a renderer's constructor: {@code this.addLayer(new PulsingGlowLayer<>(this, GLOW_TEXTURE));}
 */
public class PulsingGlowLayer<S extends EntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
	private final RenderType glow;

	public PulsingGlowLayer(RenderLayerParent<S, M> renderer, Identifier glowTexture) {
		super(renderer);
		this.glow = RenderTypes.eyes(glowTexture);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
			S state, float yRot, float xRot) {
		// Opacity between 0.6 and 1.0, one breath about every 3 seconds (the eyes render type
		// blends by alpha, so this fades the glow over the painted texture below it).
		float breath = 0.8F + 0.2F * Mth.sin(state.ageInTicks * 0.1F);
		collector.order(1).submitModel(this.getParentModel(), state, poseStack, this.glow, lightCoords,
				OverlayTexture.NO_OVERLAY, ARGB.white(breath), null, state.outlineColor);
	}
}

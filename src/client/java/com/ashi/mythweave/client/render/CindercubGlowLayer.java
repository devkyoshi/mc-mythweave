package com.ashi.mythweave.client.render;

import com.ashi.mythweave.Mythweave;
import com.ashi.mythweave.client.model.CindercubModel;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * Makes Cindercub's magic glow: draws the model a second time with a texture that is
 * transparent except for the rune markings, eye shine, crystals, ember ear tips and tail
 * flame, using the full-bright "eyes" render type (the one spider and enderman eyes use), so
 * those parts stay lit in the dark.
 *
 * <p>The glow "breathes": its opacity rises and falls slowly, like embers. Purely visual;
 * the glow map is painted by {@code tools/generate_cindercub_texture.ps1}.
 */
public class CindercubGlowLayer extends RenderLayer<CindercubRenderState, CindercubModel> {
	private static final RenderType GLOW = RenderTypes.eyes(Mythweave.id("textures/entity/cindercub_glow.png"));

	public CindercubGlowLayer(RenderLayerParent<CindercubRenderState, CindercubModel> renderer) {
		super(renderer);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
			CindercubRenderState state, float yRot, float xRot) {
		// Opacity between 0.6 and 1.0, one breath about every 3 seconds (the eyes render type
		// blends by alpha, so this fades the glow over the painted texture below it).
		float breath = 0.8F + 0.2F * Mth.sin(state.ageInTicks * 0.1F);
		collector.order(1).submitModel(this.getParentModel(), state, poseStack, GLOW, lightCoords,
				OverlayTexture.NO_OVERLAY, ARGB.white(breath), null, state.outlineColor);
	}
}

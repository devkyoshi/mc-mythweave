package com.ashi.mythweave.client.render;

import com.ashi.mythweave.Mythweave;
import com.ashi.mythweave.client.model.CindercubModel;

import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * Makes the ember tail tip glow: draws the model a second time with a texture that is
 * transparent except for the tip, using the full-bright "eyes" render type (the same one
 * spider and enderman eyes use), so the tip stays lit in the dark.
 *
 * <p>Purely visual; the glow map is painted by {@code tools/generate_cindercub_texture.ps1}.
 */
public class CindercubGlowLayer extends EyesLayer<CindercubRenderState, CindercubModel> {
	private static final RenderType GLOW = RenderTypes.eyes(Mythweave.id("textures/entity/cindercub_glow.png"));

	public CindercubGlowLayer(RenderLayerParent<CindercubRenderState, CindercubModel> renderer) {
		super(renderer);
	}

	@Override
	public RenderType renderType() {
		return GLOW;
	}
}

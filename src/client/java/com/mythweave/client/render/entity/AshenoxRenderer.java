package com.mythweave.client.render.entity;

import com.mythweave.Mythweave;
import com.mythweave.client.model.entity.AshenoxModel;
import com.mythweave.client.registry.ModModelLayers;
import com.mythweave.client.render.entity.layer.PulsingGlowLayer;
import com.mythweave.client.render.entity.state.AshenoxRenderState;
import com.mythweave.entity.ashenox.AshenoxEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/** Draws an Ashenox with {@link AshenoxModel} and its pulsing glow (magma veins, eyes, horn tips, fire). Appearance only. */
public class AshenoxRenderer extends MobRenderer<AshenoxEntity, AshenoxRenderState, AshenoxModel> {
	private static final Identifier TEXTURE = Mythweave.id("textures/entity/ashenox/ashenox.png");
	/** Transparent except the parts that glow (magma veins, eyes, horn tips, paws, fire). */
	private static final Identifier GLOW_TEXTURE = Mythweave.id("textures/entity/ashenox/ashenox_glow.png");

	public AshenoxRenderer(EntityRendererProvider.Context context) {
		// 0.55 = shadow radius in blocks, sized for the 0.8-wide hitbox.
		super(context, new AshenoxModel(context.bakeLayer(ModModelLayers.ASHENOX)), 0.55F);
		this.addLayer(new PulsingGlowLayer<>(this, GLOW_TEXTURE));
	}

	@Override
	public AshenoxRenderState createRenderState() {
		return new AshenoxRenderState();
	}

	@Override
	public void extractRenderState(AshenoxEntity entity, AshenoxRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.isSitting = entity.isInSittingPose();
	}

	@Override
	public Identifier getTextureLocation(AshenoxRenderState state) {
		return TEXTURE;
	}
}

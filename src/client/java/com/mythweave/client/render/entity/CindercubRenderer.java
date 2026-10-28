package com.mythweave.client.render.entity;

import com.mythweave.Mythweave;
import com.mythweave.client.model.entity.CindercubModel;
import com.mythweave.client.registry.ModModelLayers;
import com.mythweave.client.render.entity.layer.PulsingGlowLayer;
import com.mythweave.client.render.entity.state.CindercubRenderState;
import com.mythweave.entity.cindercub.CindercubEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
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
	/** Minimum block light the body is drawn with (0-15; a torch gives 14). */
	private static final int EMBER_LIGHT = 9;

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
		state.sleepAmount = entity.getSleepAmount(partialTicks);
		state.happyTime = entity.getHappyTime(partialTicks);
		state.hurtTime = entity.hurtTime > 0 ? Math.max(0.0F, entity.hurtTime - partialTicks) : 0.0F;
		state.idleSeed = entity.getId();
	}

	/**
	 * Its own embers light it: never darker than block light {@link #EMBER_LIGHT}, so at night
	 * the whole fox stays visible in a warm glow instead of only its tail.
	 */
	@Override
	protected int getBlockLightLevel(CindercubEntity entity, BlockPos pos) {
		return Math.max(EMBER_LIGHT, super.getBlockLightLevel(entity, pos));
	}

	@Override
	public Identifier getTextureLocation(CindercubRenderState state) {
		return TEXTURE;
	}
}

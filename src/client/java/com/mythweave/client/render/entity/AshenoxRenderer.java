package com.mythweave.client.render.entity;

import com.mythweave.Mythweave;
import com.mythweave.client.model.entity.AshenoxModel;
import com.mythweave.client.registry.ModModelLayers;
import com.mythweave.client.render.entity.layer.PulsingGlowLayer;
import com.mythweave.client.render.entity.state.AshenoxRenderState;
import com.mythweave.entity.ashenox.AshenoxEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * Draws an Ashenox with {@link AshenoxModel} and its pulsing glow (eyes, flank spirals, ear
 * tips, tail tips and flames). Appearance only.
 */
public class AshenoxRenderer extends MobRenderer<AshenoxEntity, AshenoxRenderState, AshenoxModel> {
	private static final Identifier TEXTURE = Mythweave.id("textures/entity/ashenox/ashenox.png");
	/** Transparent except the parts that glow (eyes, spirals, ear tips, tail tips, flames). */
	private static final Identifier GLOW_TEXTURE = Mythweave.id("textures/entity/ashenox/ashenox_glow.png");
	/** Minimum block light the body is drawn with (0-15; a torch gives 14). */
	private static final int EMBER_LIGHT = 7;
	/** Above this walk speed (0..1) a moving Ashenox runs instead of walking. */
	private static final float RUN_SPEED = 0.8F;

	public AshenoxRenderer(EntityRendererProvider.Context context) {
		// 0.7 = shadow radius in blocks, sized for the horse-sized body.
		super(context, new AshenoxModel(context.bakeLayer(ModModelLayers.ASHENOX)), 0.7F);
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
		state.sleepAmount = entity.getSleepAmount(partialTicks);
		state.happyTime = entity.getHappyTime(partialTicks);
		state.alertAmount = entity.getAlertAmount(partialTicks);
		state.isRidden = entity.isVehicle();
		state.isRunning = entity.isSprinting() || state.walkAnimationSpeed > RUN_SPEED;
	}

	/** Its three burning tails light it: never darker than block light {@link #EMBER_LIGHT}. */
	@Override
	protected int getBlockLightLevel(AshenoxEntity entity, BlockPos pos) {
		return Math.max(EMBER_LIGHT, super.getBlockLightLevel(entity, pos));
	}

	@Override
	public Identifier getTextureLocation(AshenoxRenderState state) {
		return TEXTURE;
	}
}

package com.mythweave.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A Mythweave ember ({@link com.mythweave.registry.ModParticles#EMBER}), as in the
 * Cindercub concept art: a tiny glowing square that drifts up and sideways, slowly spinning
 * and flickering, cooling from gold through orange to red as it fades out. Always drawn
 * full-bright, so embers glow at night.
 *
 * <p>The sprite ({@code textures/particle/ember.png}) is a white square; the color comes from
 * the tint here.
 */
public class EmberParticle extends SingleQuadParticle {
	/** Start colors (r, g, b): mostly gold and orange, some red-orange. */
	private static final float[][] COLORS = {
			{1.0F, 0.82F, 0.35F},
			{1.0F, 0.66F, 0.22F},
			{1.0F, 0.52F, 0.16F},
			{0.95F, 0.36F, 0.1F},
	};

	private final float startR;
	private final float startG;
	private final float startB;
	private final float spin;
	private final float flickerPhase;
	private final float baseSize;

	protected EmberParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
		super(level, x, y, z, sprites.get(level.getRandom()));
		this.hasPhysics = false;
		this.gravity = -0.02F;               // a gentle lift: embers rise
		this.friction = 0.94F;
		// Keep the spawner's push and add a little random drift.
		this.xd = xd + (this.random.nextFloat() - 0.5F) * 0.02F;
		this.yd = yd + this.random.nextFloat() * 0.01F;
		this.zd = zd + (this.random.nextFloat() - 0.5F) * 0.02F;
		this.lifetime = 24 + this.random.nextInt(24);
		float[] c = COLORS[this.random.nextInt(COLORS.length)];
		this.startR = c[0];
		this.startG = c[1];
		this.startB = c[2];
		this.setColor(c[0], c[1], c[2]);
		this.baseSize = 0.035F + this.random.nextFloat() * 0.035F;
		this.quadSize = this.baseSize;
		this.roll = this.random.nextFloat() * Mth.TWO_PI;
		this.oRoll = this.roll;
		this.spin = (this.random.nextFloat() - 0.5F) * 0.2F;
		this.flickerPhase = this.random.nextFloat() * Mth.TWO_PI;
	}

	@Override
	public void tick() {
		super.tick();
		this.oRoll = this.roll;
		this.roll += this.spin;
		// Wander sideways a little, like a spark caught in warm air.
		this.xd += (this.random.nextFloat() - 0.5F) * 0.004F;
		this.zd += (this.random.nextFloat() - 0.5F) * 0.004F;

		float life = (float) this.age / this.lifetime;
		// Cool down: toward deep red over the second half of its life.
		float cool = Math.max(0.0F, life * 2.0F - 1.0F);
		this.setColor(Mth.lerp(cool, this.startR, 0.8F), Mth.lerp(cool, this.startG, 0.22F), Mth.lerp(cool, this.startB, 0.08F));
		// Flicker, and fade and shrink over the last 40%.
		float fade = life < 0.6F ? 1.0F : 1.0F - (life - 0.6F) / 0.4F;
		float flicker = 0.75F + 0.25F * Mth.sin(this.age * 0.9F + this.flickerPhase);
		this.setAlpha(fade * flicker);
		this.quadSize = this.baseSize * (0.6F + 0.4F * fade);
	}

	@Override
	protected SingleQuadParticle.Layer getLayer() {
		return SingleQuadParticle.Layer.TRANSLUCENT;
	}

	@Override
	protected int getLightCoords(float partialTicks) {
		return LightCoordsUtil.withBlock(super.getLightCoords(partialTicks), 15);
	}

	/** Creates embers; registered in {@code ModParticleProviders}. */
	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z,
				double xd, double yd, double zd, RandomSource random) {
			return new EmberParticle(level, x, y, z, xd, yd, zd, this.sprites);
		}
	}
}

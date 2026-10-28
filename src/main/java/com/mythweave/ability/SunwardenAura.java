package com.mythweave.ability;

import com.mythweave.entity.base.MythlingEntity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * Sunwarden aura, Solflare's passive ability: while its owner stays close, they get Fire
 * Resistance and Speed I, refreshed every couple of seconds the way a beacon does it.
 *
 * <p>Passive, so it isn't an {@link Ability} (nothing to trigger, no cooldown). The owning
 * entity calls {@link #tick} every server tick. Server-side only.
 *
 * <p>Bounded (spec §5): only the Solflare's own owner is affected, never other players or mobs,
 * and the effects are short (they wear off a few seconds after the owner walks away). It never
 * replaces a stronger or longer effect the owner already has: vanilla keeps the better one.
 */
public final class SunwardenAura {
	/** How close the owner must be, in blocks. */
	public static final double RANGE = 8.0;
	/** How often the effects are refreshed (2 s). */
	private static final int INTERVAL_TICKS = 40;
	/** How long each refresh lasts (5 s), so there's no gap between refreshes. */
	private static final int EFFECT_TICKS = 100;

	private SunwardenAura() {
	}

	/** Server-side, every tick: refreshes the owner's effects if they're close enough. */
	public static void tick(MythlingEntity solflare, ServerLevel level) {
		if (solflare.tickCount % INTERVAL_TICKS != 0 || !solflare.isTame() || solflare.isEvolving()) {
			return;
		}
		if (!(solflare.getOwner() instanceof Player owner) || !owner.isAlive() || owner.level() != level
				|| owner.distanceToSqr(solflare) > RANGE * RANGE) {
			return;
		}
		// Ambient (like a beacon's): faint swirls, and the icon shows it comes from nearby.
		owner.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, EFFECT_TICKS, 0, true, true, true), solflare);
		owner.addEffect(new MobEffectInstance(MobEffects.SPEED, EFFECT_TICKS, 0, true, true, true), solflare);
		level.sendParticles(ParticleTypes.WAX_ON, owner.getX(), owner.getY(0.6), owner.getZ(), 3, 0.3, 0.4, 0.3, 0.0);
	}
}

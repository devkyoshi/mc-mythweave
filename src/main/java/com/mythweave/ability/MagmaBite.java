package com.mythweave.ability;

import com.mythweave.entity.base.MythlingEntity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Magma Bite, Ashenox's passive ability: its bites set their target on fire, and a mob that
 * hits it in melee gets burned by its magma plates.
 *
 * <p>Passive, so it isn't an {@link Ability} (nothing to trigger, no cooldown). The owning
 * entity calls {@link #onBite} from {@code doHurtTarget} and {@link #onHurt} from
 * {@code hurtServer}. Server-side only.
 *
 * <p>Bounded (spec §5): it only ignites mobs. It never ignites players, so a bite in PvP
 * doesn't burn anyone, and it never places fire blocks; a burning mob doesn't spread fire.
 */
public final class MagmaBite {
	/** How long a bite burns its target. */
	public static final float BITE_FIRE_SECONDS = 4.0F;
	/** How long a mob burns after hitting the Ashenox in melee. */
	public static final float RETALIATION_FIRE_SECONDS = 3.0F;

	private MagmaBite() {
	}

	/** After a successful bite: set the target on fire (mobs only) with a puff of flame. */
	public static void onBite(MythlingEntity biter, ServerLevel level, Entity target) {
		if (target instanceof LivingEntity living && !(target instanceof Player) && !living.fireImmune()) {
			living.igniteForSeconds(BITE_FIRE_SECONDS);
			level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY(0.5), target.getZ(), 8, 0.2, 0.3, 0.2, 0.02);
		}
	}

	/**
	 * After it took damage: a mob that hit it directly (in melee, not with an arrow) is burned.
	 * Players are never burned.
	 */
	public static void onHurt(MythlingEntity victim, ServerLevel level, DamageSource source) {
		Entity attacker = source.getEntity();
		if (attacker instanceof LivingEntity living && attacker == source.getDirectEntity()
				&& !(attacker instanceof Player) && !living.fireImmune() && attacker != victim) {
			living.igniteForSeconds(RETALIATION_FIRE_SECONDS);
			level.sendParticles(ParticleTypes.LAVA, attacker.getX(), attacker.getY(0.5), attacker.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
		}
	}
}

package com.ashi.mythweave.evolution;

import com.ashi.mythweave.entity.base.MythlingEntity;
import com.ashi.mythweave.evolution.EvolutionRules.KindleSource;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Kindle earned from things that happen around a Mythling rather than to its own state:
 * hostile mobs defeated near it, and hits from hostile mobs it survives. Uses Fabric's
 * server-side entity events, so it runs only on the server, for every Mythling species.
 */
public final class KindleEvents {
	private KindleEvents() {
	}

	/** Called from {@link com.ashi.mythweave.event.ModEvents#register()}. */
	public static void register() {
		// A hostile mob died: every bonded Mythling near it that's with its owner shares the win.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(entity instanceof Enemy) || !(entity.level() instanceof ServerLevel level)) {
				return;
			}
			for (MythlingEntity mythling : level.getEntitiesOfClass(MythlingEntity.class,
					entity.getBoundingBox().inflate(EvolutionRules.HOSTILE_RANGE), Kindle::isWithOwner)) {
				mythling.kindle().gain(mythling, KindleSource.HOSTILE_DEFEATED);
			}
		});

		// A bonded Mythling took damage from a hostile mob and is still alive.
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (entity instanceof MythlingEntity mythling && mythling.isTame() && mythling.isAlive()
					&& damageTaken > 0.0F && source.getEntity() instanceof Enemy) {
				mythling.kindle().survivedHit(mythling, mythling.level().getGameTime());
			}
		});
	}
}

package com.ashi.mythweave.evolution;

import com.ashi.mythweave.evolution.EvolutionRules.KindleSource;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Kindle earned from things that happen around a Mythling rather than to its own state:
 * hostile mobs defeated near it, and hits from hostile mobs it survives. Uses Fabric's
 * server-side entity events, so it runs only on the server.
 */
public final class KindleEvents {
	private KindleEvents() {
	}

	/** Called from {@link com.ashi.mythweave.Mythweave#onInitialize()}. */
	public static void register() {
		// A hostile mob died: every bonded Mythling near it that's with its owner shares the win.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(entity instanceof Enemy) || !(entity.level() instanceof ServerLevel level)) {
				return;
			}
			for (TamableAnimal mythling : level.getEntitiesOfClass(TamableAnimal.class,
					entity.getBoundingBox().inflate(EvolutionRules.HOSTILE_RANGE),
					candidate -> candidate instanceof Mythling && Kindle.isWithOwner(candidate))) {
				((Mythling) mythling).kindle().gain(mythling, KindleSource.HOSTILE_DEFEATED);
			}
		});

		// A bonded Mythling took damage from a hostile mob and is still alive.
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (entity instanceof TamableAnimal mythling && entity instanceof Mythling holder
					&& mythling.isTame() && mythling.isAlive() && damageTaken > 0.0F
					&& source.getEntity() instanceof Enemy) {
				holder.kindle().survivedHit(mythling, mythling.level().getGameTime());
			}
		});
	}
}

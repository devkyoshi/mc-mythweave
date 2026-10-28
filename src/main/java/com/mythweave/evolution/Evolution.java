package com.mythweave.evolution;

import com.mythweave.Mythweave;
import com.mythweave.entity.base.MythlingEntity;
import com.mythweave.registry.ModEntities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

/**
 * Evolving: when a bonded Mythling has enough Kindle ({@link EvolutionRules}), it transforms
 * into the next form of its {@link EvolutionLine}. Server-side only.
 *
 * <p>The transformation lasts {@link EvolutionRules#EVOLUTION_TICKS}: the Mythling becomes
 * invulnerable, floats up spinning inside a spiral of flame, then bursts and is replaced by
 * the new form. The new entity keeps its owner, sit order, Kindle, name and leash (vanilla's
 * {@code Mob#convertTo} copies the last two), and starts at full health.
 *
 * <p>Every stage is bigger than the last, so while it spins the Mythling also grows until it's
 * as tall as its new form, and it only starts where the new form has room.
 */
public final class Evolution {
	/** Upward speed (blocks per tick) for the first two thirds of the transformation. */
	private static final double RISE_SPEED = 0.025;
	/** How high it rises in total, so the room check covers the whole transformation. */
	private static final double RISE_HEIGHT = RISE_SPEED * EvolutionRules.EVOLUTION_TICKS * 0.66;
	/** The temporary SCALE modifier that grows it during the transformation (never saved). */
	private static final Identifier GROWTH = Mythweave.id("evolution_growth");

	private Evolution() {
	}

	/** Server-side, once a second for a bonded Mythling: starts evolving if it's ready. */
	public static void checkReady(MythlingEntity mythling, ServerLevel level) {
		String next = nextFormId(mythling);
		if (next == null) {
			return;
		}
		Optional<EntityType<? extends MythlingEntity>> type = ModEntities.mythlingForm(next);
		if (type.isEmpty()) {
			// Ready, but that form isn't in the game yet: say so once, then keep earning Kindle.
			if (!mythling.readyAnnounced()) {
				mythling.setReadyAnnounced(true);
				Kindle.tellOwner(mythling, Component.translatable("message.mythweave.evolution.ready_later",
						mythling.getDisplayName(), EvolutionLine.formName(next)));
			}
			return;
		}
		// Only start somewhere sensible: standing on the ground, not swimming or riding.
		if (!mythling.onGround() || mythling.isInWater() || mythling.isPassenger()) {
			return;
		}
		// The new form is bigger: wait until it fits here, rise included (say so once).
		AABB room = sizeOf(type.get()).makeBoundingBox(mythling.position()).expandTowards(0.0, RISE_HEIGHT, 0.0);
		if (!level.noCollision(mythling, room)) {
			if (!mythling.roomAnnounced()) {
				mythling.setRoomAnnounced(true);
				Kindle.tellOwner(mythling, Component.translatable("message.mythweave.evolution.needs_room",
						mythling.getDisplayName(), EvolutionLine.formName(next)));
			}
			return;
		}
		start(mythling, level);
	}

	/** The form it's ready to become right now, or null. */
	private static @Nullable String nextFormId(MythlingEntity mythling) {
		Kindle kindle = mythling.kindle();
		return EvolutionRules.nextForm(mythling.evolutionLine(), mythling.formId(), kindle.points(), kindle.leaning());
	}

	/** A form's real size: its model-sized hitbox times its SCALE. */
	private static EntityDimensions sizeOf(EntityType<?> type) {
		return type.getDimensions().scale(MythlingEntity.formScale(type));
	}

	private static void start(MythlingEntity mythling, ServerLevel level) {
		mythling.setEvolvingTicks(EvolutionRules.EVOLUTION_TICKS);
		mythling.setPermanentlyInvulnerable(true);
		mythling.setNoGravity(true);
		mythling.getNavigation().stop();
		mythling.setTarget(null);
		level.playSound(null, mythling.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 1.0F, 1.4F);
		Kindle.tellOwner(mythling, Component.translatable("message.mythweave.evolution.started", mythling.getDisplayName()));
	}

	/** Server-side, every tick while evolving: rise, spin, flame spiral; finish at the end. */
	public static void tick(MythlingEntity mythling, ServerLevel level) {
		int left = mythling.evolvingTicks() - 1;
		mythling.setEvolvingTicks(left);
		int elapsed = EvolutionRules.EVOLUTION_TICKS - left;
		float progress = (float) elapsed / EvolutionRules.EVOLUTION_TICKS;

		// Hold still apart from the rise; ignore AI movement.
		mythling.getNavigation().stop();
		mythling.setDeltaMovement(0.0, progress < 0.66F ? RISE_SPEED : 0.0, 0.0);

		// Spin faster and faster (degrees per tick 6 -> 36). Rotation syncs to clients.
		float spin = mythling.getYRot() + 6.0F + 30.0F * progress;
		mythling.setYRot(spin);
		mythling.setYBodyRot(spin);
		mythling.setYHeadRot(spin);

		grow(mythling, progress);

		// Two flames orbiting and climbing, plus a few embers; denser as it builds.
		double radius = mythling.getBbWidth() * (1.4 - 0.6 * progress); // wider for bigger forms
		for (int i = 0; i < 2; i++) {
			double angle = elapsed * 0.5 + i * Math.PI;
			level.sendParticles(ParticleTypes.FLAME,
					mythling.getX() + Math.cos(angle) * radius, mythling.getY() + 0.2 + progress * mythling.getBbHeight(),
					mythling.getZ() + Math.sin(angle) * radius, 1, 0.0, 0.0, 0.0, 0.0);
		}
		if (elapsed % 4 == 0) {
			level.sendParticles(ParticleTypes.SMALL_FLAME, mythling.getX(), mythling.getY(0.5), mythling.getZ(),
					2 + (int) (progress * 6), 0.3, 0.4, 0.3, 0.02);
		}
		if (elapsed % 20 == 0) {
			level.playSound(null, mythling.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL,
					0.4F, 0.8F + progress);
		}

		if (left <= 0) {
			finish(mythling, level);
		}
	}

	/**
	 * Grows it smoothly from its own height to its new form's, so the swap at the end doesn't
	 * jump in size. A transient SCALE modifier: synced to clients, never saved (a reload
	 * mid-transformation re-applies it on the next tick).
	 */
	private static void grow(MythlingEntity mythling, float progress) {
		AttributeInstance scale = mythling.getAttribute(Attributes.SCALE);
		String next = nextFormId(mythling);
		Optional<EntityType<? extends MythlingEntity>> type = next == null ? Optional.empty() : ModEntities.mythlingForm(next);
		if (scale == null || type.isEmpty()) {
			return;
		}
		float ratio = sizeOf(type.get()).height() / sizeOf(mythling.getType()).height();
		scale.addOrUpdateTransientModifier(new AttributeModifier(GROWTH, (ratio - 1.0F) * progress,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	}

	private static void finish(MythlingEntity mythling, ServerLevel level) {
		mythling.setNoGravity(false); // convertTo copies NoGravity, so clear it first
		mythling.setPermanentlyInvulnerable(false); // also copied by convertTo
		AttributeInstance scale = mythling.getAttribute(Attributes.SCALE);
		if (scale != null) {
			scale.removeModifier(GROWTH); // in case it lands as it was
		}
		String next = nextFormId(mythling);
		Optional<EntityType<? extends MythlingEntity>> type = next == null ? Optional.empty() : ModEntities.mythlingForm(next);
		if (type.isEmpty()) {
			return; // can't happen normally (checked at start); just land as it was
		}

		Component oldName = mythling.getDisplayName();
		MythlingEntity evolved = mythling.convertTo(type.get(), ConversionParams.single(mythling, false, false),
				newForm -> copyBond(mythling, newForm));
		if (evolved == null) {
			return;
		}

		level.sendParticles(ParticleTypes.EXPLOSION, evolved.getX(), evolved.getY(0.5), evolved.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.FLAME, evolved.getX(), evolved.getY(0.5), evolved.getZ(), 60, 0.4, 0.5, 0.4, 0.12);
		level.sendParticles(ParticleTypes.LAVA, evolved.getX(), evolved.getY(0.5), evolved.getZ(), 12, 0.4, 0.3, 0.4, 0.0);
		level.playSound(null, evolved.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1.0F, 0.9F);
		level.playSound(null, evolved.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.NEUTRAL, 0.8F, 0.7F);
		Kindle.tellOwner(evolved, Component.translatable("message.mythweave.evolution.finished",
				oldName, EvolutionLine.formName(next)));
	}

	/** The bond carries over: owner, sit order, Kindle; and it arrives at full health. */
	private static void copyBond(MythlingEntity from, MythlingEntity to) {
		to.setOwnerReference(from.getOwnerReference());
		to.setTame(true, false);
		to.setOrderedToSit(from.isOrderedToSit());
		to.setInSittingPose(from.isInSittingPose());
		to.kindle().copyFrom(from.kindle());
		to.setHealth(to.getMaxHealth());
		to.setYRot(from.getYRot());
		to.setYBodyRot(from.getYRot());
		to.setYHeadRot(from.getYRot());
	}
}

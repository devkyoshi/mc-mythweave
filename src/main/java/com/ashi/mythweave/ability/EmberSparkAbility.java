package com.ashi.mythweave.ability;

import com.ashi.mythweave.block.EmberBrazierBlock;
import com.ashi.mythweave.entity.base.MythlingEntity;
import com.ashi.mythweave.evolution.EvolutionRules;
import com.ashi.mythweave.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * Ember Spark, the Cindercub line's signature ability: the Mythling flicks a spark at the
 * nearest unlit {@link EmberBrazierBlock} it can see and lights it. It grows with evolution,
 * so each form has its own constant: {@link #CINDERCUB} (8 blocks, 15 s) and {@link #EMBERLYNX}
 * (12 blocks, 10 s).
 *
 * <p>Triggered by the owner using Blaze Powder on their Mythling. Everything here runs on the
 * server, which also owns the cooldown, so a client can't skip it. The effect is bounded: it
 * only ever changes an Ember Brazier's LIT state (never places fire), only within range, and
 * only in line of sight.
 */
public final class EmberSparkAbility implements Ability<MythlingEntity> {
	public static final EmberSparkAbility CINDERCUB = new EmberSparkAbility(8, 15 * 20);
	public static final EmberSparkAbility EMBERLYNX = new EmberSparkAbility(12, 10 * 20);

	/** How far up/down from the Mythling the search reaches. */
	private static final int VERTICAL_RANGE = 4;

	/** Search radius in blocks (spherical). */
	private final int range;
	private final int cooldownTicks;

	private EmberSparkAbility(int range, int cooldownTicks) {
		this.range = range;
		this.cooldownTicks = cooldownTicks;
	}

	@Override
	public String id() {
		return "ember_spark";
	}

	@Override
	public int cooldownTicks() {
		return this.cooldownTicks;
	}

	public int range() {
		return this.range;
	}

	@Override
	public boolean isTriggeredBy(ItemStack stack) {
		return stack.is(Items.BLAZE_POWDER);
	}

	@Override
	public void use(MythlingEntity mythling, AbilityCooldown cooldown, ServerLevel level, Player owner) {
		if (!cooldown.isReady()) {
			owner.sendOverlayMessage(Component.translatable("message.mythweave.ember_spark.recharging",
					mythling.getDisplayName(), cooldown.remainingSeconds()));
			return;
		}

		BlockPos target = this.findTarget(mythling, level);
		if (target == null) {
			// A little puff so it's clear the Mythling heard you.
			level.sendParticles(ParticleTypes.SMOKE, mythling.getX(), mythling.getEyeY(), mythling.getZ(),
					4, 0.1, 0.1, 0.1, 0.01);
			owner.sendOverlayMessage(Component.translatable("message.mythweave.ember_spark.no_brazier",
					mythling.getDisplayName(), this.range));
			return;
		}

		Vec3 from = mythling.getEyePosition();
		Vec3 to = Vec3.atCenterOf(target);
		mythling.getLookControl().setLookAt(to);
		sparkTrail(level, from, to);
		mythling.playSound(SoundEvents.BLAZE_SHOOT, 0.4F, 1.6F);
		EmberBrazierBlock.setLit(level, target, level.getBlockState(target), true, mythling);
		level.sendParticles(ParticleTypes.LAVA, to.x, to.y + 0.3, to.z, 6, 0.2, 0.1, 0.2, 0.0);

		cooldown.start(this.cooldownTicks);
		mythling.kindle().gain(mythling, EvolutionRules.KindleSource.EMBER_SPARK);
		owner.sendOverlayMessage(Component.translatable("message.mythweave.ember_spark.lit", mythling.getDisplayName()));
	}

	/**
	 * The closest unlit Ember Brazier within range that the Mythling can see, or null. Scans a
	 * (2r+1) x 9 x (2r+1) box (about 2,600 lookups at 8 blocks, 5,600 at 12), once per use, in
	 * chunks that are already loaded around the Mythling.
	 */
	private @Nullable BlockPos findTarget(MythlingEntity mythling, ServerLevel level) {
		BlockPos origin = mythling.blockPosition();
		Vec3 eye = mythling.getEyePosition();
		BlockPos best = null;
		double bestDistSqr = (double) this.range * this.range;
		for (BlockPos pos : BlockPos.betweenClosed(
				origin.offset(-this.range, -VERTICAL_RANGE, -this.range), origin.offset(this.range, VERTICAL_RANGE, this.range))) {
			BlockState state = level.getBlockState(pos);
			if (!state.is(ModBlocks.EMBER_BRAZIER) || state.getValue(EmberBrazierBlock.LIT)) {
				continue;
			}
			double distSqr = pos.distToCenterSqr(eye);
			if (distSqr < bestDistSqr && canSee(mythling, level, eye, pos)) {
				best = pos.immutable(); // betweenClosed reuses one mutable position
				bestDistSqr = distSqr;
			}
		}
		return best;
	}

	/** True if nothing solid sits between the Mythling's eyes and the brazier. */
	private static boolean canSee(MythlingEntity mythling, ServerLevel level, Vec3 eye, BlockPos pos) {
		BlockHitResult hit = level.clip(new ClipContext(
				eye, Vec3.atCenterOf(pos), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mythling));
		return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
	}

	/** A short line of flame particles from the Mythling to the brazier (at most 12). */
	private static void sparkTrail(ServerLevel level, Vec3 from, Vec3 to) {
		int steps = Math.min(12, Math.max(3, (int) (from.distanceTo(to) * 2)));
		for (int i = 1; i <= steps; i++) {
			Vec3 p = from.lerp(to, (double) i / steps);
			level.sendParticles(ParticleTypes.SMALL_FLAME, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}
}

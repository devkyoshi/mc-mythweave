package com.ashi.mythweave.ability;

import com.ashi.mythweave.block.EmberBrazierBlock;
import com.ashi.mythweave.entity.CindercubEntity;
import com.ashi.mythweave.evolution.EvolutionRules;
import com.ashi.mythweave.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * Ember Spark, Cindercub's signature ability: it flicks a spark at the nearest unlit
 * {@link EmberBrazierBlock} it can see and lights it.
 *
 * <p>Triggered by the owner using {@link CindercubEntity#BONDING_ITEM} on their Cindercub.
 * Everything here runs on the server, which also owns the cooldown, so a client can't
 * skip it. The effect is bounded: it only ever changes an Ember Brazier's LIT state (never
 * places fire), only within {@link #RANGE} blocks, and only in line of sight.
 */
public final class EmberSpark {
	/** 15 seconds between sparks. */
	public static final int COOLDOWN_TICKS = 15 * 20;
	/** Search radius in blocks (spherical), and how far up/down the search box reaches. */
	public static final int RANGE = 8;
	private static final int VERTICAL_RANGE = 4;

	private EmberSpark() {
	}

	/** Server-side. Tries the ability and tells the owner how it went. */
	public static void use(CindercubEntity cub, ServerLevel level, Player owner) {
		int cooldown = cub.getEmberCooldown();
		if (cooldown > 0) {
			int seconds = (cooldown + 19) / 20;
			owner.sendOverlayMessage(Component.translatable("message.mythweave.cindercub.ember_recharging", seconds));
			return;
		}

		BlockPos target = findTarget(cub, level);
		if (target == null) {
			// A little puff so it's clear the Cindercub heard you.
			level.sendParticles(ParticleTypes.SMOKE, cub.getX(), cub.getEyeY(), cub.getZ(), 4, 0.1, 0.1, 0.1, 0.01);
			owner.sendOverlayMessage(Component.translatable("message.mythweave.cindercub.ember_no_brazier", RANGE));
			return;
		}

		Vec3 from = cub.getEyePosition();
		Vec3 to = Vec3.atCenterOf(target);
		cub.getLookControl().setLookAt(to);
		sparkTrail(level, from, to);
		cub.playSound(SoundEvents.BLAZE_SHOOT, 0.4F, 1.6F);
		EmberBrazierBlock.setLit(level, target, level.getBlockState(target), true, cub);
		level.sendParticles(ParticleTypes.LAVA, to.x, to.y + 0.3, to.z, 6, 0.2, 0.1, 0.2, 0.0);

		cub.setEmberCooldown(COOLDOWN_TICKS);
		cub.kindle().gain(cub, EvolutionRules.KindleSource.EMBER_SPARK);
		owner.sendOverlayMessage(Component.translatable("message.mythweave.cindercub.ember_lit"));
	}

	/**
	 * The closest unlit Ember Brazier within {@link #RANGE} that the Cindercub can see, or
	 * null. Scans a 17 x 9 x 17 box: about 2,600 block lookups, once per use, all in chunks
	 * that are already loaded around the Cindercub.
	 */
	private static @Nullable BlockPos findTarget(CindercubEntity cub, ServerLevel level) {
		BlockPos origin = cub.blockPosition();
		Vec3 eye = cub.getEyePosition();
		BlockPos best = null;
		double bestDistSqr = (double) RANGE * RANGE;
		for (BlockPos pos : BlockPos.betweenClosed(
				origin.offset(-RANGE, -VERTICAL_RANGE, -RANGE), origin.offset(RANGE, VERTICAL_RANGE, RANGE))) {
			BlockState state = level.getBlockState(pos);
			if (!state.is(ModBlocks.EMBER_BRAZIER) || state.getValue(EmberBrazierBlock.LIT)) {
				continue;
			}
			double distSqr = pos.distToCenterSqr(eye);
			if (distSqr < bestDistSqr && canSee(cub, level, eye, pos)) {
				best = pos.immutable(); // betweenClosed reuses one mutable position
				bestDistSqr = distSqr;
			}
		}
		return best;
	}

	/** True if nothing solid sits between the Cindercub's eyes and the brazier. */
	private static boolean canSee(CindercubEntity cub, ServerLevel level, Vec3 eye, BlockPos pos) {
		BlockHitResult hit = level.clip(new ClipContext(
				eye, Vec3.atCenterOf(pos), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, cub));
		return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
	}

	/** A short line of flame particles from the Cindercub to the brazier (at most 12). */
	private static void sparkTrail(ServerLevel level, Vec3 from, Vec3 to) {
		int steps = Math.min(12, Math.max(3, (int) (from.distanceTo(to) * 2)));
		for (int i = 1; i <= steps; i++) {
			Vec3 p = from.lerp(to, (double) i / steps);
			level.sendParticles(ParticleTypes.SMALL_FLAME, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}
}

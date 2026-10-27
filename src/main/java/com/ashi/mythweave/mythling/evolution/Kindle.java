package com.ashi.mythweave.mythling.evolution;

import com.ashi.mythweave.mythling.MythlingEntity;
import com.ashi.mythweave.mythling.evolution.EvolutionRules.Branch;
import com.ashi.mythweave.mythling.evolution.EvolutionRules.KindleSource;

import com.mojang.serialization.Codec;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One Mythling's Kindle: the bond XP that drives evolution, plus how much of it leans toward
 * each stage-3 branch. Server-side only: it is never synced to clients (players see it via
 * the status message), and it is saved with the entity under "Kindle".
 *
 * <p>Only a bonded Mythling earns Kindle. Numbers and rules come from {@link EvolutionRules}.
 */
public final class Kindle {
	private int points;
	private int combat;
	private int exploration;
	private final Set<String> visitedBiomes = new HashSet<>();
	private int ticksWithOwner;
	private double blocksTravelled;
	private long lastSurvivedHitTick = Long.MIN_VALUE;

	// Last position, to measure walking; not saved (the first tick after loading just sets it).
	private double lastX = Double.NaN;
	private double lastZ = Double.NaN;

	public int points() {
		return this.points;
	}

	public Branch leaning() {
		return EvolutionRules.leaning(this.combat, this.exploration);
	}

	/** True if the owner is alive, in the same dimension, and within {@link EvolutionRules#OWNER_RANGE}. */
	public static boolean isWithOwner(MythlingEntity mythling) {
		LivingEntity owner = mythling.getOwner();
		return mythling.isTame() && owner != null && owner.isAlive() && owner.level() == mythling.level()
				&& mythling.distanceToSqr(owner) <= EvolutionRules.OWNER_RANGE * EvolutionRules.OWNER_RANGE;
	}

	/** Server-side, every tick: time together, distance travelled together, new biomes. */
	public void tick(MythlingEntity mythling, ServerLevel level) {
		boolean withOwner = isWithOwner(mythling);

		// Walking distance. Jumps over 2 blocks in one tick are teleports (e.g. catching up
		// with the owner) and don't count.
		double x = mythling.getX();
		double z = mythling.getZ();
		if (withOwner && !Double.isNaN(this.lastX)) {
			double step = Math.sqrt((x - this.lastX) * (x - this.lastX) + (z - this.lastZ) * (z - this.lastZ));
			if (step < 2.0) {
				this.blocksTravelled += step;
			}
		}
		this.lastX = x;
		this.lastZ = z;

		if (!withOwner) {
			return;
		}
		if (++this.ticksWithOwner >= EvolutionRules.TIME_INTERVAL_TICKS) {
			this.ticksWithOwner = 0;
			this.gain(mythling, KindleSource.TIME_WITH_OWNER);
		}
		while (this.blocksTravelled >= EvolutionRules.TRAVEL_BLOCKS) {
			this.blocksTravelled -= EvolutionRules.TRAVEL_BLOCKS;
			this.gain(mythling, KindleSource.TRAVEL);
		}
		if (mythling.tickCount % 20 == 0) {
			level.getBiome(mythling.blockPosition()).unwrapKey().ifPresent(key -> {
				if (this.visitedBiomes.add(key.identifier().toString())) {
					this.gain(mythling, KindleSource.NEW_BIOME);
					tellOwner(mythling, Component.translatable("message.mythweave.kindle.new_biome",
							mythling.getDisplayName(), KindleSource.NEW_BIOME.kindle));
				}
			});
		}
	}

	/** Records the current biome without a reward (used on bonding, so the home biome is free). */
	public void markBiomeVisited(MythlingEntity mythling, ServerLevel level) {
		level.getBiome(mythling.blockPosition()).unwrapKey()
				.ifPresent(key -> this.visitedBiomes.add(key.identifier().toString()));
	}

	/** Server-side. The Mythling was hurt by a hostile mob and is still standing. */
	public void survivedHit(MythlingEntity mythling, long gameTime) {
		if (gameTime - this.lastSurvivedHitTick >= EvolutionRules.SURVIVED_HIT_COOLDOWN_TICKS) {
			this.lastSurvivedHitTick = gameTime;
			this.gain(mythling, KindleSource.SURVIVED_HIT);
		}
	}

	/** Server-side. Adds Kindle from a source, if the Mythling is bonded. */
	public void gain(MythlingEntity mythling, KindleSource source) {
		this.add(mythling, source.kindle, source.affinity);
	}

	/** Server-side. Adds raw Kindle (and affinity toward a branch); also used by the test command. */
	public void add(MythlingEntity mythling, int amount, Branch affinity) {
		if (!mythling.isTame() || amount <= 0) {
			return;
		}
		int before = this.points;
		this.points += amount;
		switch (affinity) {
			case COMBAT -> this.combat += amount;
			case EXPLORATION -> this.exploration += amount;
			case NONE -> { }
		}
		if (before < EvolutionRules.STAGE_2_KINDLE && this.points >= EvolutionRules.STAGE_2_KINDLE) {
			this.celebrate(mythling);
		}
	}

	/** Reached the stage-2 goal: a flare of flame and a message (evolving itself comes in E2). */
	private void celebrate(MythlingEntity mythling) {
		if (mythling.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.FLAME, mythling.getX(), mythling.getY() + 0.5, mythling.getZ(),
					30, 0.3, 0.4, 0.3, 0.05);
		}
		tellOwner(mythling, Component.translatable("message.mythweave.kindle.ready", mythling.getDisplayName()));
	}

	/** The one-line status shown to the owner: name, Kindle toward the next goal, leaning. */
	public Component status(MythlingEntity mythling) {
		// "leaning toward <form> (battle)": the form's name comes from this species' line.
		Branch leaning = this.leaning();
		Component leaningText = switch (leaning) {
			case NONE -> Component.translatable("message.mythweave.kindle.leaning.none");
			case COMBAT -> Component.translatable("message.mythweave.kindle.leaning.combat",
					EvolutionLine.formName(mythling.evolutionLine().formFor(leaning)));
			case EXPLORATION -> Component.translatable("message.mythweave.kindle.leaning.exploration",
					EvolutionLine.formName(mythling.evolutionLine().formFor(leaning)));
		};
		return Component.translatable("message.mythweave.kindle.status", mythling.getDisplayName(),
				this.points, EvolutionRules.nextGoal(this.points), leaningText);
	}

	private static void tellOwner(MythlingEntity mythling, Component message) {
		if (mythling.getOwner() instanceof Player player) {
			player.sendOverlayMessage(message);
		}
	}

	public void save(ValueOutput output) {
		ValueOutput out = output.child("Kindle");
		out.putInt("Points", this.points);
		out.putInt("Combat", this.combat);
		out.putInt("Exploration", this.exploration);
		out.putInt("TicksWithOwner", this.ticksWithOwner);
		out.putInt("BlocksTravelled", (int) this.blocksTravelled);
		out.store("Biomes", Codec.STRING.listOf(), List.copyOf(this.visitedBiomes));
	}

	public void load(ValueInput input) {
		ValueInput in = input.childOrEmpty("Kindle");
		// Clamp so hand-edited or corrupt data can't go negative.
		this.points = Math.max(0, in.getIntOr("Points", 0));
		this.combat = Math.max(0, in.getIntOr("Combat", 0));
		this.exploration = Math.max(0, in.getIntOr("Exploration", 0));
		this.ticksWithOwner = Math.max(0, in.getIntOr("TicksWithOwner", 0));
		this.blocksTravelled = Math.max(0, in.getIntOr("BlocksTravelled", 0));
		this.visitedBiomes.clear();
		in.read("Biomes", Codec.STRING.listOf()).ifPresent(this.visitedBiomes::addAll);
	}
}

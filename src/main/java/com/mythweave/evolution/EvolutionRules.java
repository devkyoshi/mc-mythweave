package com.mythweave.evolution;

import org.jspecify.annotations.Nullable;

/**
 * Every evolution number and rule, in one place (spec: evolution conditions must not be
 * scattered through entity code). Entities only report what happened; {@link Kindle} applies
 * these rules. See docs/design/evolution.md.
 *
 * <p>These rules are shared by every species. Which forms a species evolves into is its
 * {@link EvolutionLine} (all in {@link EvolutionLines}): stage 1 -> stage 2 -> stage 3.
 */
public final class EvolutionRules {
	/** Kindle needed for stage 2, and for stage 3 (counted from zero, not on top of stage 2). */
	public static final int STAGE_2_KINDLE = 150;
	public static final int STAGE_3_KINDLE = 400;
	/** How long the transformation takes: 3 seconds of rising, spinning flame. */
	public static final int EVOLUTION_TICKS = 60;

	/** "With its owner" means the owner is alive, in the same dimension, within this range. */
	public static final double OWNER_RANGE = 16.0;
	/** Time with the owner pays out once per this many ticks (1 minute). */
	public static final int TIME_INTERVAL_TICKS = 60 * 20;
	/** Travelling with the owner pays out once per this many blocks walked. */
	public static final double TRAVEL_BLOCKS = 100.0;
	/** Hostile mobs that die within this many blocks of the Mythling count as defeated together. */
	public static final double HOSTILE_RANGE = 12.0;
	/** Surviving hits pays out at most once per this many ticks (10 seconds), so it can't be farmed. */
	public static final int SURVIVED_HIT_COOLDOWN_TICKS = 10 * 20;

	/** Everything that earns Kindle, and how much. */
	public enum KindleSource {
		TIME_WITH_OWNER(1),
		EMBER_SPARK(5),
		TRAVEL(3),
		NEW_BIOME(10),
		HOSTILE_DEFEATED(8),
		SURVIVED_HIT(2);

		public final int kindle;

		KindleSource(int kindle) {
			this.kindle = kindle;
		}
	}

	private EvolutionRules() {
	}

	/** A form's stage in its line: 1 (base), 2 or 3; 0 if it isn't in the line. */
	public static int stageOf(EvolutionLine line, String formId) {
		if (formId.equals(line.base())) {
			return 1;
		}
		if (formId.equals(line.stage2())) {
			return 2;
		}
		return formId.equals(line.stage3()) ? 3 : 0;
	}

	/** Kindle needed to evolve out of a stage, or -1 if it's the final stage. */
	public static int kindleToEvolveFrom(int stage) {
		return switch (stage) {
			case 1 -> STAGE_2_KINDLE;
			case 2 -> STAGE_3_KINDLE;
			default -> -1;
		};
	}

	/**
	 * The form a Mythling should evolve into right now, or null if it isn't ready: not enough
	 * Kindle, or already final.
	 */
	public static @Nullable String nextForm(EvolutionLine line, String formId, int kindle) {
		int stage = stageOf(line, formId);
		int needed = kindleToEvolveFrom(stage);
		if (needed < 0 || kindle < needed) {
			return null;
		}
		return stage == 1 ? line.stage2() : line.stage3();
	}
}

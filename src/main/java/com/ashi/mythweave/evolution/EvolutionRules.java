package com.ashi.mythweave.evolution;

/**
 * Every evolution number and rule, in one place (spec: evolution conditions must not be
 * scattered through entity code). Entities only report what happened; {@link Kindle} applies
 * these rules. See docs/EVOLUTION_PLAN.md.
 *
 * <p>Line: Cindercub -> Emberlynx (stage 2) -> Ashenox (combat) or Solflare (exploration).
 */
public final class EvolutionRules {
	/** Kindle needed for stage 2, and for stage 3 (counted from zero, not on top of stage 2). */
	public static final int STAGE_2_KINDLE = 150;
	public static final int STAGE_3_KINDLE = 400;

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

	/** Which stage-3 form the Mythling is drawn toward. */
	public enum Branch {
		NONE,
		/** Ashenox. */
		COMBAT,
		/** Solflare. */
		EXPLORATION
	}

	/** Everything that earns Kindle: how much, and which branch it pulls toward. */
	public enum KindleSource {
		TIME_WITH_OWNER(1, Branch.NONE),
		EMBER_SPARK(5, Branch.EXPLORATION),
		TRAVEL(3, Branch.EXPLORATION),
		NEW_BIOME(10, Branch.EXPLORATION),
		HOSTILE_DEFEATED(8, Branch.COMBAT),
		SURVIVED_HIT(2, Branch.COMBAT);

		public final int kindle;
		public final Branch affinity;

		KindleSource(int kindle, Branch affinity) {
			this.kindle = kindle;
			this.affinity = affinity;
		}
	}

	private EvolutionRules() {
	}

	/** The next Kindle goal to show the player: stage 2's until reached, then stage 3's. */
	public static int nextGoal(int kindle) {
		return kindle < STAGE_2_KINDLE ? STAGE_2_KINDLE : STAGE_3_KINDLE;
	}

	/** The branch with more affinity; a tie (including 0 - 0) leans nowhere yet. */
	public static Branch leaning(int combat, int exploration) {
		if (combat == exploration) {
			return Branch.NONE;
		}
		return combat > exploration ? Branch.COMBAT : Branch.EXPLORATION;
	}
}

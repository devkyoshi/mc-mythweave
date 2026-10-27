package com.ashi.mythweave.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

/**
 * Cindercub, the first Mythling: a small ember fox.
 *
 * <p>Extends vanilla {@link TamableAnimal} because it already stores the owner UUID,
 * the sitting state, and saves both to disk. Bonding and following (Phase 4) will
 * build on that instead of reinventing it.
 *
 * <p>Wild behavior (Phase 3) is a passive animal's: wander, look around, flee when hurt.
 */
public class CindercubEntity extends TamableAnimal {
	public CindercubEntity(EntityType<? extends CindercubEntity> type, Level level) {
		super(type, level);
	}

	/** Base stats. A little sturdier than a fox (10 HP), slightly slower. */
	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
				.add(Attributes.MAX_HEALTH, 12.0)
				.add(Attributes.MOVEMENT_SPEED, 0.28);
	}

	/**
	 * AI goals, run server-side. Lower number = higher priority; goals that use the same
	 * controls (moving, looking) can't run together, so a higher-priority one interrupts.
	 * Gaps are left for Phase 4: sit when ordered (2) and follow owner (6), matching the
	 * vanilla wolf's order.
	 */
	@Override
	protected void registerGoals() {
		// Swim instead of sinking.
		this.goalSelector.addGoal(1, new FloatGoal(this));
		// Run away at 1.4x speed for a few seconds when hurt. The tamable version also
		// teleports back to a far-away owner while panicking (once bonding exists).
		this.goalSelector.addGoal(1, new TamableAnimalPanicGoal(1.4));
		// Wander at normal speed (1.0 x MOVEMENT_SPEED), avoiding water.
		this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0));
		// Idle: glance at nearby players, otherwise look around.
		this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
	}

	// Breeding is out of scope (spec §5), so nothing counts as food and no offspring is made.
	@Override
	public boolean isFood(ItemStack stack) {
		return false;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}
}

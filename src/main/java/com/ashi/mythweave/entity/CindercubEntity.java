package com.ashi.mythweave.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
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
 * <p>Phase 1 only: no AI goals yet, so a Cindercub stands still. Idle and wander
 * behavior comes in Phase 3.
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

package com.ashi.mythweave.entity.emberlynx;

import com.ashi.mythweave.ability.AbilityCooldown;
import com.ashi.mythweave.ability.EmberSparkAbility;
import com.ashi.mythweave.entity.base.MythlingEntity;
import com.ashi.mythweave.evolution.EvolutionLine;
import com.ashi.mythweave.evolution.EvolutionLines;
import com.ashi.mythweave.registry.ModSounds;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

/**
 * Emberlynx, stage 2 of the Cindercub line: a taller, leaner ember lynx with a fire mane,
 * flame-tufted ears, longer crystal horns and a long fire tail. Cindercubs become one at
 * {@link com.ashi.mythweave.evolution.EvolutionRules#STAGE_2_KINDLE} Kindle.
 *
 * <ul>
 *   <li>Sturdier (20 HP) and quicker than a Cindercub, and it hits back (4 damage).</li>
 *   <li><b>Defends its owner:</b> attacks whatever hurts its owner, and whatever hurts it,
 *       but never creepers, ghasts, armor stands, tamed animals, or players its owner can't
 *       harm (the vanilla wolf's rules). It doesn't panic when hurt.</li>
 *   <li>Stronger Ember Spark: {@link EmberSparkAbility#EMBERLYNX} (12 blocks, 10 s).</li>
 * </ul>
 */
public class EmberlynxEntity extends MythlingEntity {
	/** Ember Spark's cooldown; same save key as the Cindercub's, so it carries the same meaning. */
	private final AbilityCooldown emberSparkCooldown = new AbilityCooldown("EmberCooldown");

	public EmberlynxEntity(EntityType<? extends EmberlynxEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, 4.0);
	}

	/** Companion goals from the base class, plus fighting: bite, defend the owner, hit back. */
	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.2, true));
		this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
	}

	@Override
	protected boolean fleesWhenHurt() {
		return false;
	}

	/** Who it's allowed to fight when defending its owner (same rules as a vanilla wolf). */
	@Override
	public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
		if (target instanceof Creeper || target instanceof Ghast || target instanceof ArmorStand) {
			return false;
		}
		if (target instanceof Player targetPlayer && owner instanceof Player ownerPlayer
				&& !ownerPlayer.canHarmPlayer(targetPlayer)) {
			return false;
		}
		if (target instanceof AbstractHorse horse && horse.isTamed()) {
			return false;
		}
		return !(target instanceof TamableAnimal animal && animal.isTame());
	}

	@Override
	public Item bondingItem() {
		return Items.BLAZE_POWDER;
	}

	@Override
	public EvolutionLine evolutionLine() {
		return EvolutionLines.CINDERCUB;
	}

	/** Owner + Blaze Powder: its stronger Ember Spark. */
	@Override
	protected boolean onOwnerUseItem(Player owner, ItemStack stack) {
		if (!EmberSparkAbility.EMBERLYNX.isTriggeredBy(stack)) {
			return false;
		}
		if (this.level() instanceof ServerLevel serverLevel) {
			EmberSparkAbility.EMBERLYNX.use(this, this.emberSparkCooldown, serverLevel, owner);
		}
		return true;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return ModSounds.EMBERLYNX_AMBIENT;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.EMBERLYNX_HURT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return ModSounds.EMBERLYNX_DEATH;
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide()) {
			this.emberSparkCooldown.tick();
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		// Embers from the long fire tail (behind, rising) and the fire mane (at the shoulders).
		if (this.level().isClientSide()) {
			if (this.isInSittingPose()) {
				this.emitEmbers(0.45, 0.4, 0.35, 0.35, 3, 2);
			} else {
				this.emitEmbers(0.4, 0.5, 0.8, 0.35, 3, 2);
			}
			this.emitEmbers(-0.3, 0.0, this.isInSittingPose() ? 0.85 : 1.0, 0.1, 8, 5);
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		this.emberSparkCooldown.save(output);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.emberSparkCooldown.load(input, EmberSparkAbility.EMBERLYNX.cooldownTicks());
	}
}

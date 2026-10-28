package com.mythweave.entity.ashenox;

import com.mythweave.ability.AbilityCooldown;
import com.mythweave.ability.EmberSparkAbility;
import com.mythweave.ability.MagmaBite;
import com.mythweave.entity.base.MythlingEntity;
import com.mythweave.evolution.EvolutionLine;
import com.mythweave.evolution.EvolutionLines;
import com.mythweave.registry.ModSounds;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

/**
 * Ashenox, the combat stage-3 form of the Cindercub line: a heavy volcanic fox armored in
 * obsidian plates with glowing magma veins, big curved horns, a magma mane and a thick fire
 * tail. An Emberlynx becomes one at {@link com.mythweave.evolution.EvolutionRules#STAGE_3_KINDLE}
 * Kindle if it leans toward battle (fighting together).
 *
 * <ul>
 *   <li>Tough: 32 HP, 4 armor, resists knockback, immune to fire and lava (the entity type is
 *       fire-immune), and bites hard (6 damage).</li>
 *   <li><b>Magma Bite</b> ({@link MagmaBite}): its bites set mobs on fire, and mobs that hit it
 *       in melee get burned. Never players, never blocks.</li>
 *   <li>Defends its owner like the Emberlynx; Ember Spark as strong as the Emberlynx's.</li>
 *   <li>Final form: its Kindle keeps counting, but it doesn't evolve further.</li>
 * </ul>
 */
public class AshenoxEntity extends MythlingEntity {
	/** Ember Spark's cooldown; same save key as the earlier forms. */
	private final AbilityCooldown emberSparkCooldown = new AbilityCooldown("EmberCooldown");

	public AshenoxEntity(EntityType<? extends AshenoxEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
				.add(Attributes.MAX_HEALTH, 32.0)
				.add(Attributes.MOVEMENT_SPEED, 0.28)
				.add(Attributes.ATTACK_DAMAGE, 6.0)
				.add(Attributes.ARMOR, 4.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
				// 30% bigger than its model: about 1.4 blocks tall to the top of its head.
				.add(Attributes.SCALE, 1.3);
	}

	@Override
	protected boolean defendsOwner() {
		return true;
	}

	@Override
	public Item bondingItem() {
		return Items.BLAZE_POWDER;
	}

	@Override
	public EvolutionLine evolutionLine() {
		return EvolutionLines.CINDERCUB;
	}

	/** Owner + Blaze Powder: Ember Spark (same strength as the Emberlynx's). */
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

	/** Magma Bite, part 1: a successful bite sets the target on fire. */
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit) {
			MagmaBite.onBite(this, level, target);
		}
		return hit;
	}

	/** Magma Bite, part 2: a mob that hits it in melee is burned by its magma plates. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt) {
			MagmaBite.onHurt(this, level, source);
		}
		return hurt;
	}

	// Sounds: Mythweave's own events, pitched down because it's big.
	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return ModSounds.ASHENOX_AMBIENT;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.ASHENOX_HURT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return ModSounds.ASHENOX_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.75F;
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
		// Embers from the fire tail and the magma mane, and now and then a drip of lava from
		// the cracks in its plates (client-side visuals only).
		if (this.level().isClientSide()) {
			boolean sitting = this.isInSittingPose();
			this.emitEmbers(0.55, 0.6, sitting ? 0.35 : 0.85, 0.4, 3, 2);
			this.emitEmbers(-0.35, 0.0, sitting ? 0.95 : 1.1, 0.1, 6, 4);
			if (this.random.nextInt(40) == 0 && !this.isInWaterOrRain()) {
				this.level().addParticle(ParticleTypes.DRIPPING_LAVA, this.getRandomX(0.4), this.getY(0.6),
						this.getRandomZ(0.4), 0.0, 0.0, 0.0);
			}
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

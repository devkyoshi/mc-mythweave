package com.ashi.mythweave.entity.solflare;

import com.ashi.mythweave.ability.AbilityCooldown;
import com.ashi.mythweave.ability.EmberSparkAbility;
import com.ashi.mythweave.ability.SunwardenAura;
import com.ashi.mythweave.entity.base.MythlingEntity;
import com.ashi.mythweave.evolution.EvolutionLine;
import com.ashi.mythweave.evolution.EvolutionLines;
import com.ashi.mythweave.registry.ModSounds;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
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
 * Solflare, the exploration stage-3 form of the Cindercub line: a radiant guardian fox. A lean,
 * long-legged body in cream and sun-gold, two fans of flame wings at the shoulders, a flame
 * crest, tall ears and a long comet tail. An Emberlynx becomes one at
 * {@link com.ashi.mythweave.evolution.EvolutionRules#STAGE_3_KINDLE} Kindle if it leans toward
 * exploring (travelling, new biomes, Ember Spark).
 *
 * <ul>
 *   <li>Swift (speed 0.36) rather than tough: 24 HP, 4 damage, immune to fire and lava (the
 *       entity type is fire-immune).</li>
 *   <li><b>Sunwarden aura</b> ({@link SunwardenAura}): its owner gets Fire Resistance and Speed I
 *       while within 8 blocks.</li>
 *   <li>The strongest Ember Spark: {@link EmberSparkAbility#SOLFLARE} (16 blocks, 6 s, up to
 *       three braziers at once).</li>
 *   <li>Defends its owner like the Emberlynx. Final form: its Kindle keeps counting, but it
 *       doesn't evolve further.</li>
 * </ul>
 */
public class SolflareEntity extends MythlingEntity {
	/** Ember Spark's cooldown; same save key as the earlier forms. */
	private final AbilityCooldown emberSparkCooldown = new AbilityCooldown("EmberCooldown");

	public SolflareEntity(EntityType<? extends SolflareEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
				.add(Attributes.MAX_HEALTH, 24.0)
				.add(Attributes.MOVEMENT_SPEED, 0.36)
				.add(Attributes.ATTACK_DAMAGE, 4.0)
				// 30% bigger than its model, like the Ashenox: about 1.4 blocks tall to the top of its head.
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

	/** Owner + Blaze Powder: its Ember Spark, which can light several braziers at once. */
	@Override
	protected boolean onOwnerUseItem(Player owner, ItemStack stack) {
		if (!EmberSparkAbility.SOLFLARE.isTriggeredBy(stack)) {
			return false;
		}
		if (this.level() instanceof ServerLevel serverLevel) {
			EmberSparkAbility.SOLFLARE.use(this, this.emberSparkCooldown, serverLevel, owner);
		}
		return true;
	}

	// Sounds: Mythweave's own events, pitched a little down (bigger than an Emberlynx, lighter than an Ashenox).
	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return ModSounds.SOLFLARE_AMBIENT;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.SOLFLARE_HURT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return ModSounds.SOLFLARE_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.9F;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel serverLevel) {
			this.emberSparkCooldown.tick();
			SunwardenAura.tick(this, serverLevel);
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		// Embers from the flame wings and the comet tail, and now and then a mote of sunlight
		// drifting off its crest (client-side visuals only).
		if (this.level().isClientSide()) {
			boolean sitting = this.isInSittingPose();
			this.emitEmbers(-0.05, 0.4, sitting ? 0.95 : 1.15, 0.35, 3, 2);
			this.emitEmbers(0.45, 0.8, sitting ? 0.45 : 0.85, 0.35, 4, 2);
			if (this.random.nextInt(30) == 0 && !this.isInWaterOrRain()) {
				this.level().addParticle(ParticleTypes.END_ROD, this.getRandomX(0.3), this.getY(1.0),
						this.getRandomZ(0.3), 0.0, 0.02, 0.0);
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
		this.emberSparkCooldown.load(input, EmberSparkAbility.SOLFLARE.cooldownTicks());
	}
}

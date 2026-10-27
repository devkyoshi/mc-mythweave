package com.ashi.mythweave.entity.cindercub;

import com.ashi.mythweave.ability.AbilityCooldown;
import com.ashi.mythweave.ability.EmberSparkAbility;
import com.ashi.mythweave.entity.base.MythlingEntity;
import com.ashi.mythweave.evolution.EvolutionLine;
import com.ashi.mythweave.evolution.EvolutionLines;
import com.ashi.mythweave.registry.ModSounds;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
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
 * Cindercub, the first Mythling: a small ember-spirit fox kit. Bonding, following, sitting
 * and Kindle come from {@link MythlingEntity}; this class adds what makes it a Cindercub.
 *
 * <ul>
 *   <li>Bonds with {@link #BONDING_ITEM} (Blaze Powder); its tail ignites when it does.</li>
 *   <li>The owner uses Blaze Powder on it again to trigger {@link EmberSparkAbility}.</li>
 *   <li>Once bonded its tail is fire, and embers stream from it (client-side particles).</li>
 * </ul>
 */
public class CindercubEntity extends MythlingEntity {
	/** Prototype bonding item (spec §5). A custom Aether Shard can replace it later. */
	public static final Item BONDING_ITEM = Items.BLAZE_POWDER;

	/** Ember Spark's cooldown; saved as "EmberCooldown" (the key existing worlds already use). */
	private final AbilityCooldown emberSparkCooldown = new AbilityCooldown("EmberCooldown");

	public CindercubEntity(EntityType<? extends CindercubEntity> type, Level level) {
		super(type, level);
	}

	/** Base stats. A little sturdier than a fox (10 HP), slightly slower. */
	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
				.add(Attributes.MAX_HEALTH, 12.0)
				.add(Attributes.MOVEMENT_SPEED, 0.28);
	}

	@Override
	public Item bondingItem() {
		return BONDING_ITEM;
	}

	@Override
	public EvolutionLine evolutionLine() {
		return EvolutionLines.CINDERCUB;
	}

	/** Owner + Blaze Powder: Ember Spark (works sitting or standing; no powder is used). */
	@Override
	protected boolean onOwnerUseItem(Player owner, ItemStack stack) {
		if (!EmberSparkAbility.CINDERCUB.isTriggeredBy(stack)) {
			return false;
		}
		if (this.level() instanceof ServerLevel serverLevel) {
			EmberSparkAbility.CINDERCUB.use(this, this.emberSparkCooldown, serverLevel, owner);
		}
		return true;
	}

	/** The tail ignites (clients switch to the fire tail as the tame flag syncs): flame burst and whoosh. */
	@Override
	protected void onBonded(ServerLevel level, Player owner) {
		float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
		level.sendParticles(ParticleTypes.FLAME, this.getX() + Mth.sin(yaw) * 0.45, this.getY() + 0.6,
				this.getZ() - Mth.cos(yaw) * 0.45, 24, 0.15, 0.15, 0.15, 0.04);
		this.playSound(SoundEvents.FIRECHARGE_USE, 0.6F, 1.4F);
	}

	// Sounds: Mythweave's own events (see ModSounds), pitched up because it's small.
	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return ModSounds.CINDERCUB_AMBIENT;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.CINDERCUB_HURT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return ModSounds.CINDERCUB_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 1.25F;
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
		// Only a bonded Cindercub's tail is fire; a wild one has a plain fur tail.
		// Embers stream from the fire tail (lower when sitting). Only a bonded Cindercub's tail
		// is fire; a wild one has a plain fur tail.
		if (this.level().isClientSide() && this.isTame()) {
			if (this.isInSittingPose()) {
				this.emitEmbers(0.35, 0.3, 0.3, 0.2, 4, 2);
			} else {
				this.emitEmbers(0.3, 0.3, 0.55, 0.25, 4, 2);
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
		this.emberSparkCooldown.load(input, EmberSparkAbility.CINDERCUB.cooldownTicks());
	}
}

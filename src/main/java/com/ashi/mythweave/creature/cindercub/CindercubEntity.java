package com.ashi.mythweave.creature.cindercub;

import com.ashi.mythweave.mythling.MythlingEntity;
import com.ashi.mythweave.mythling.evolution.EvolutionLine;
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
 *   <li>The owner uses Blaze Powder on it again to trigger {@link EmberSpark}.</li>
 *   <li>Once bonded its tail is fire, and embers stream from it (client-side particles).</li>
 * </ul>
 */
public class CindercubEntity extends MythlingEntity {
	/** Prototype bonding item (spec §5). A custom Aether Shard can replace it later. */
	public static final Item BONDING_ITEM = Items.BLAZE_POWDER;

	/**
	 * Ticks until Ember Spark can be used again. Server-only (never synced, so a client
	 * can't change it) and saved with the entity, so relogging doesn't reset it.
	 */
	private int emberCooldown;

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
		return CindercubLine.LINE;
	}

	/** Owner + Blaze Powder: Ember Spark (works sitting or standing; no powder is used). */
	@Override
	protected boolean onOwnerUseItem(Player owner, ItemStack stack) {
		if (!stack.is(BONDING_ITEM)) {
			return false;
		}
		if (this.level() instanceof ServerLevel serverLevel) {
			EmberSpark.use(this, serverLevel, owner);
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

	public int getEmberCooldown() {
		return this.emberCooldown;
	}

	public void setEmberCooldown(int ticks) {
		this.emberCooldown = ticks;
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide() && this.emberCooldown > 0) {
			this.emberCooldown--;
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		// Only a bonded Cindercub's tail is fire; a wild one has a plain fur tail.
		if (this.level().isClientSide() && this.isTame()) {
			this.spawnTailEmbers();
		}
	}

	/**
	 * Client-side visuals only: embers stream up from the fire tail, more while running. In
	 * rain or water they turn into puffs of steam instead. Each client spawns its own, so this
	 * sends nothing over the network.
	 */
	private void spawnTailEmbers() {
		boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4;
		if (this.random.nextInt(moving ? 2 : 4) != 0) {
			return;
		}
		// A random point along the fire tail: behind the body and up (lower when sitting).
		// The entity faces (-sin(yaw), cos(yaw)), so "behind" is the opposite.
		boolean sitting = this.isInSittingPose();
		double along = this.random.nextDouble();
		double back = (sitting ? 0.35 : 0.3) + along * 0.3;
		double up = (sitting ? 0.3 : 0.55) + along * (sitting ? 0.2 : 0.25);
		float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
		double x = this.getX() + Mth.sin(yaw) * back + (this.random.nextDouble() - 0.5) * 0.15;
		double y = this.getY() + up;
		double z = this.getZ() - Mth.cos(yaw) * back + (this.random.nextDouble() - 0.5) * 0.15;
		if (this.isInWaterOrRain()) {
			this.level().addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.04, 0.0);
		} else {
			this.level().addParticle(this.random.nextInt(4) == 0 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME,
					x, y, z, 0.0, 0.015 + this.random.nextDouble() * 0.02, 0.0);
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("EmberCooldown", this.emberCooldown);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		// Clamp so a hand-edited or corrupt value can't lock the ability for ages.
		this.emberCooldown = Math.clamp(input.getIntOr("EmberCooldown", 0), 0, EmberSpark.COOLDOWN_TICKS);
	}
}

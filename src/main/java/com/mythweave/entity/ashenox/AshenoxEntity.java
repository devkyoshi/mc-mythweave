package com.mythweave.entity.ashenox;

import com.mythweave.ability.AbilityCooldown;
import com.mythweave.ability.EmberSparkAbility;
import com.mythweave.ability.MagmaBite;
import com.mythweave.entity.base.MythlingEntity;
import com.mythweave.evolution.EvolutionLine;
import com.mythweave.evolution.EvolutionLines;
import com.mythweave.registry.ModParticles;
import com.mythweave.registry.ModSounds;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * Ashenox, stage 2 of the Cindercub line: a horse-sized fire fox with three blazing tails,
 * and its owner's mount. A Cindercub becomes one at
 * {@link com.mythweave.evolution.EvolutionRules#STAGE_2_KINDLE} Kindle; it evolves into an
 * Emberlynx at {@link com.mythweave.evolution.EvolutionRules#STAGE_3_KINDLE}.
 *
 * <ul>
 *   <li><b>Rideable</b> by its owner (empty-handed click; sneak-click to sit). No saddle needed.
 *       Walks, sprints when the rider sprints, and jumps like a horse (hold jump to charge).
 *       One rider at a time; only the owner can steer it.</li>
 *   <li>Tough: 32 HP, 4 armor, resists knockback, immune to fire and lava (the entity type is
 *       fire-immune), and bites hard (6 damage).</li>
 *   <li><b>Magma Bite</b> ({@link MagmaBite}): its bites set mobs on fire, and mobs that hit it
 *       in melee get burned. Never players, never blocks.</li>
 *   <li>Defends its owner; Ember Spark as strong as the Emberlynx's.</li>
 *   <li>Sleeps lying down (sitting at night, or after sitting a long time), stands alert when
 *       hostile mobs are near, and is happy when its owner climbs on.</li>
 * </ul>
 */
public class AshenoxEntity extends MythlingEntity implements PlayerRideableJumping {
	/**
	 * Where the rider sits, relative to the Ashenox's feet (blocks; +z is forward): above the
	 * middle of its back, just behind the shoulders, clear of the neck and the tails. Tune here
	 * after testing in game.
	 */
	public static final Vec3 RIDER_SEAT = new Vec3(0.0, 0.98, -0.05);
	/** Ridden speed multiplier while the rider sprints. */
	private static final float SPRINT_MULTIPLIER = 1.4F;
	/** How long the happy animation lasts, in ticks. */
	public static final int HAPPY_TICKS = 30;

	/** Whether it's asleep. Decided on the server, synced so every client draws it lying down. */
	private static final EntityDataAccessor<Boolean> DATA_SLEEPING =
			SynchedEntityData.defineId(AshenoxEntity.class, EntityDataSerializers.BOOLEAN);
	/** Whether it's alert (a target, or hostile mobs nearby). Decided on the server, synced. */
	private static final EntityDataAccessor<Boolean> DATA_ALERT =
			SynchedEntityData.defineId(AshenoxEntity.class, EntityDataSerializers.BOOLEAN);
	/** Entity event: play the happy animation (handled in {@link #handleEntityEvent}). */
	private static final byte EVENT_HAPPY = 100;
	/** Sitting this long (2 minutes) makes it lie down to sleep even in daylight. */
	private static final int DOZE_AFTER_TICKS = 2400;
	/** After being woken it stays awake this long (30 seconds). */
	private static final int STAY_AWAKE_TICKS = 600;
	/** Hostile mobs within this many blocks make it alert. */
	private static final double ALERT_RANGE = 8.0;

	/** Ember Spark's cooldown; same save key as the other forms. */
	private final AbilityCooldown emberSparkCooldown = new AbilityCooldown("EmberCooldown");
	/** Server-side, not saved: how long it has been sitting, and how long it must stay awake. */
	private int sitTicks;
	private int awakeTicks;
	/** The rider's charged jump (0..1), waiting for it to be on the ground. */
	private float jumpPendingScale;
	/** Client-side: ticks left of the happy animation; how asleep (0..1) and alert (0..1) it is. */
	private int happyTicks;
	private float sleepAmount;
	private float prevSleepAmount;
	private float alertAmount;
	private float prevAlertAmount;

	public AshenoxEntity(EntityType<? extends AshenoxEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
				.add(Attributes.MAX_HEALTH, 32.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, 6.0)
				.add(Attributes.ARMOR, 4.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
				// A rider's charged jump clears about two blocks; it steps up full blocks like a horse.
				.add(Attributes.JUMP_STRENGTH, 0.6)
				.add(Attributes.STEP_HEIGHT, 1.0)
				// Its model is built at full horse size, so no extra scaling.
				.add(Attributes.SCALE, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_SLEEPING, false);
		builder.define(DATA_ALERT, false);
	}

	public boolean isSleepingDown() {
		return this.entityData.get(DATA_SLEEPING);
	}

	/** Client-side, for the renderer: 0 awake, 1 lying down asleep, smoothed between ticks. */
	public float getSleepAmount(float partialTicks) {
		return Mth.lerp(partialTicks, this.prevSleepAmount, this.sleepAmount);
	}

	/** Client-side, for the renderer: 0 relaxed, 1 fully alert, smoothed between ticks. */
	public float getAlertAmount(float partialTicks) {
		return Mth.lerp(partialTicks, this.prevAlertAmount, this.alertAmount);
	}

	/** Client-side, for the renderer: ticks into the happy animation, or -1 when not playing. */
	public float getHappyTime(float partialTicks) {
		return this.happyTicks > 0 ? HAPPY_TICKS - this.happyTicks + partialTicks : -1.0F;
	}

	/** Server-side: everyone nearby sees it bounce and wag its tails. */
	private void cheer() {
		this.level().broadcastEntityEvent(this, EVENT_HAPPY);
	}

	/** Server-side: wake up (if asleep) and stay awake for a while. */
	private void wake() {
		this.sitTicks = 0;
		this.awakeTicks = STAY_AWAKE_TICKS;
		this.entityData.set(DATA_SLEEPING, false);
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == EVENT_HAPPY) {
			this.happyTicks = HAPPY_TICKS;
		} else {
			super.handleEntityEvent(id);
		}
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
			this.wake();
			this.cheer();
			EmberSparkAbility.EMBERLYNX.use(this, this.emberSparkCooldown, serverLevel, owner);
		}
		return true;
	}

	@Override
	protected void onOwnerToggledSit(Player owner, boolean sit) {
		this.wake();
	}

	// ---- Riding ----

	@Override
	protected boolean isRideable() {
		return true;
	}

	/** Its owner climbed on: it wakes, perks up, and is happy about it. */
	@Override
	protected void onOwnerMounted(Player owner) {
		this.wake();
		this.cheer();
	}

	/** Only its owner steers it (only the owner can climb on, but a command could seat anyone). */
	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		return this.getFirstPassenger() instanceof Player rider && this.isOwnedBy(rider)
				? rider : super.getControllingPassenger();
	}

	/** While carrying someone, its own goals don't move, jump or turn it; the rider does. */
	@Override
	protected void updateControlFlags() {
		super.updateControlFlags();
		boolean free = !this.isVehicle();
		this.goalSelector.setControlFlag(Goal.Flag.MOVE, free);
		this.goalSelector.setControlFlag(Goal.Flag.JUMP, free);
		this.goalSelector.setControlFlag(Goal.Flag.LOOK, free);
	}

	/** Faces where the rider looks, sprints when they sprint, and jumps when they release jump. */
	@Override
	protected void tickRidden(Player rider, Vec3 riddenInput) {
		super.tickRidden(rider, riddenInput);
		this.setRot(rider.getYRot(), rider.getXRot() * 0.5F);
		this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
		this.setSprinting(rider.isSprinting() && riddenInput.z > 0.0);
		if (this.isLocalInstanceAuthoritative() && this.onGround()) {
			if (this.jumpPendingScale > 0.0F && !this.isJumping()) {
				this.executeRidersJump(this.jumpPendingScale, riddenInput);
			}
			this.jumpPendingScale = 0.0F;
		}
	}

	/** Forward and strafe from the rider; backing up and strafing are slower. */
	@Override
	protected Vec3 getRiddenInput(Player rider, Vec3 selfInput) {
		float sideways = rider.xxa * 0.5F;
		float forward = rider.zza;
		if (forward <= 0.0F) {
			forward *= 0.25F;
		}
		return new Vec3(sideways, 0.0, forward);
	}

	@Override
	protected float getRiddenSpeed(Player rider) {
		float speed = (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
		return rider.isSprinting() ? speed * SPRINT_MULTIPLIER : speed;
	}

	/** Lets the rider sprint (vanilla only allows it on vehicles that say so). */
	@Override
	public boolean canSprint() {
		return true;
	}

	/** A horse-style leap: up by the charged amount, and forward if the rider is moving forward. */
	private void executeRidersJump(float amount, Vec3 input) {
		Vec3 movement = this.getDeltaMovement();
		this.setDeltaMovement(movement.x, this.getJumpPower(amount), movement.z);
		this.needsSync = true;
		if (input.z > 0.0) {
			float yaw = this.getYRot() * Mth.DEG_TO_RAD;
			this.setDeltaMovement(this.getDeltaMovement().add(-0.4F * Mth.sin(yaw) * amount, 0.0, 0.4F * Mth.cos(yaw) * amount));
		}
	}

	@Override
	public void onPlayerJump(int jumpAmount) {
		this.jumpPendingScale = jumpAmount <= 0 ? 0.0F : this.getPlayerJumpPendingScale(jumpAmount);
	}

	@Override
	public boolean canJump() {
		return true;
	}

	@Override
	public void handleStartJump(int jumpScale) {
		this.playSound(SoundEvents.FOX_AGGRO, 0.5F, 0.7F);
	}

	@Override
	public void handleStopJump() {
	}

	// ---- Combat ----

	/** Magma Bite, part 1: a successful bite sets the target on fire. */
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit) {
			MagmaBite.onBite(this, level, target);
		}
		return hit;
	}

	/** Magma Bite, part 2: a mob that hits it in melee is burned. Getting hurt wakes it up. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		this.wake();
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt) {
			MagmaBite.onHurt(this, level, source);
		}
		return hurt;
	}

	// Sounds: Mythweave's own events, pitched down because it's big.
	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return this.isSleepingDown() ? null : ModSounds.ASHENOX_AMBIENT;
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

	// ---- Ticking ----

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide()) {
			this.emberSparkCooldown.tick();
			this.updateSleep();
			// Asleep: keep the head straight on the body, so it doesn't swing the body round.
			if (this.isSleepingDown()) {
				this.setYHeadRot(this.yBodyRot);
			}
			if (this.tickCount % 10 == 0) {
				this.entityData.set(DATA_ALERT, !this.isSleepingDown() && this.senseDanger());
			}
			// Now and then, with its owner close by, a happy tail wag.
			LivingEntity owner = this.getOwner();
			if (owner != null && !this.isInSittingPose() && !this.isVehicle() && this.distanceToSqr(owner) < 25.0
					&& this.random.nextInt(1200) == 0) {
				this.cheer();
			}
		} else {
			if (this.happyTicks > 0) {
				this.happyTicks--;
			}
			this.prevSleepAmount = this.sleepAmount;
			float sleepTarget = this.isSleepingDown() ? 1.0F : 0.0F;
			this.sleepAmount += Mth.clamp(sleepTarget - this.sleepAmount, -0.1F, 0.04F); // lies down slowly, gets up fast
			this.prevAlertAmount = this.alertAmount;
			float alertTarget = this.entityData.get(DATA_ALERT) ? 1.0F : 0.0F;
			this.alertAmount += Mth.clamp(alertTarget - this.alertAmount, -0.05F, 0.15F);
		}
	}

	/** Server-side: a target, or a hostile mob within {@link #ALERT_RANGE}. */
	private boolean senseDanger() {
		if (this.getTarget() != null) {
			return true;
		}
		return !this.level().getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(ALERT_RANGE),
				mob -> mob instanceof Enemy && mob.isAlive()).isEmpty();
	}

	/** Asleep, it doesn't turn to look at players or glance around; ridden, the rider steers. */
	@Override
	protected boolean canLookAround() {
		return !this.isSleepingDown() && !this.isVehicle();
	}

	/** Server-side: sitting at night, or sitting a long time, sends it to sleep. */
	private void updateSleep() {
		if (this.awakeTicks > 0) {
			this.awakeTicks--;
		}
		if (!this.isInSittingPose()) {
			this.sitTicks = 0;
			this.entityData.set(DATA_SLEEPING, false);
			return;
		}
		this.sitTicks++;
		boolean drowsy = this.level().isDarkOutside() || this.sitTicks > DOZE_AFTER_TICKS;
		this.entityData.set(DATA_SLEEPING, drowsy && this.awakeTicks == 0 && this.sitTicks > 100);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		// Embers drift from the three tail tips (more while running, few while asleep), and a
		// few float up around it while it stands still. Client-side visuals only.
		if (this.level().isClientSide()) {
			boolean asleep = this.isSleepingDown();
			boolean sprinting = this.isSprinting();
			int idle = asleep ? 16 : 5;
			int moving = sprinting ? 1 : 2;
			double up = this.isInSittingPose() ? 0.75 : 1.25;
			this.emitEmbers(0.0, 0.65, 0.2, up + 0.25, 0.15, idle, moving);
			this.emitEmbers(0.4, 0.75, 0.2, up, 0.15, idle + 2, moving + 1);
			this.emitEmbers(-0.4, 0.75, 0.2, up, 0.15, idle + 2, moving + 1);
			if (!asleep) {
				this.emitEmbers(0.0, -0.4, 0.8, 0.6, 0.6, 14, 1000);
			}
		}
	}

	/** Its own embers (the Cindercub line's custom particle) instead of vanilla flames. */
	@Override
	protected ParticleOptions emberParticle() {
		return ModParticles.EMBER;
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

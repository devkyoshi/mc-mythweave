package com.mythweave.entity.cindercub;

import com.mythweave.ability.AbilityCooldown;
import com.mythweave.ability.EmberSparkAbility;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
 *   <li>Bonds with {@link #BONDING_ITEM} (Blaze Powder), with a burst of flame.</li>
 *   <li>The owner uses Blaze Powder on it again to trigger {@link EmberSparkAbility}.</li>
 *   <li>Its tail is made of glowing embers, and embers stream from it (client-side particles).</li>
 *   <li>Told to sit, it curls up and sleeps at night or after sitting a while; it wakes when
 *       hurt or when its owner calls it.</li>
 *   <li>It's happy (a hop and a fast tail wag) when bonded, fed Blaze Powder, called by its
 *       owner, and now and then when its owner is close.</li>
 * </ul>
 */
public class CindercubEntity extends MythlingEntity {
	/** Prototype bonding item (spec §5). A custom Aether Shard can replace it later. */
	public static final Item BONDING_ITEM = Items.BLAZE_POWDER;

	/** Whether it's asleep. Decided on the server, synced so every client draws the sleep pose. */
	private static final EntityDataAccessor<Boolean> DATA_SLEEPING =
			SynchedEntityData.defineId(CindercubEntity.class, EntityDataSerializers.BOOLEAN);
	/** Entity event: play the happy animation (handled in {@link #handleEntityEvent}). */
	private static final byte EVENT_HAPPY = 100;
	/** How long the happy animation lasts, in ticks. */
	public static final int HAPPY_TICKS = 30;
	/** Sitting this long (2 minutes) makes it doze off even in daylight. */
	private static final int DOZE_AFTER_TICKS = 2400;
	/** After being woken it stays awake this long (30 seconds). */
	private static final int STAY_AWAKE_TICKS = 600;

	/** Ember Spark's cooldown; saved as "EmberCooldown" (the key existing worlds already use). */
	private final AbilityCooldown emberSparkCooldown = new AbilityCooldown("EmberCooldown");
	/** Server-side, not saved: how long it has been sitting, and how long it must stay awake. */
	private int sitTicks;
	private int awakeTicks;
	/** Client-side: ticks left of the happy animation, and how far into sleep it is (0..1). */
	private int happyTicks;
	private float sleepAmount;
	private float prevSleepAmount;

	public CindercubEntity(EntityType<? extends CindercubEntity> type, Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_SLEEPING, false);
	}

	public boolean isSleepingCurled() {
		return this.entityData.get(DATA_SLEEPING);
	}

	/** Client-side, for the renderer: 0 awake, 1 fully curled up asleep, smoothed between ticks. */
	public float getSleepAmount(float partialTicks) {
		return Mth.lerp(partialTicks, this.prevSleepAmount, this.sleepAmount);
	}

	/** Client-side, for the renderer: ticks into the happy animation, or -1 when not playing. */
	public float getHappyTime(float partialTicks) {
		return this.happyTicks > 0 ? HAPPY_TICKS - this.happyTicks + partialTicks : -1.0F;
	}

	/** Server-side: everyone nearby sees it do a happy hop. */
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
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		this.wake();
		return super.hurtServer(level, source, damage);
	}

	/** Called by its owner (sit or follow): it wakes up and is happy to be noticed. */
	@Override
	protected void onOwnerToggledSit(Player owner, boolean sit) {
		this.wake();
		this.cheer();
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
			this.wake();
			this.cheer();
			EmberSparkAbility.CINDERCUB.use(this, this.emberSparkCooldown, serverLevel, owner);
		}
		return true;
	}

	/** Bonding flares the ember tail: a burst of embers and a whoosh. */
	@Override
	protected void onBonded(ServerLevel level, Player owner) {
		float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
		level.sendParticles(ModParticles.EMBER, this.getX() + Mth.sin(yaw) * 0.45, this.getY() + 0.8,
				this.getZ() - Mth.cos(yaw) * 0.45, 30, 0.25, 0.25, 0.25, 0.05);
		this.playSound(SoundEvents.FIRECHARGE_USE, 0.6F, 1.4F);
		this.cheer();
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
			this.updateSleep();
			// Asleep: keep the head straight on the body, so a head turned just before it dozed
			// off can't swing the curled-up body round.
			if (this.isSleepingCurled()) {
				this.setYHeadRot(this.yBodyRot);
			}
			// Now and then, with its owner close by, a little happy hop.
			LivingEntity owner = this.getOwner();
			if (owner != null && !this.isInSittingPose() && this.distanceToSqr(owner) < 16.0
					&& this.random.nextInt(900) == 0) {
				this.cheer();
			}
		} else {
			if (this.happyTicks > 0) {
				this.happyTicks--;
			}
			this.prevSleepAmount = this.sleepAmount;
			float target = this.isSleepingCurled() ? 1.0F : 0.0F;
			this.sleepAmount += Mth.clamp(target - this.sleepAmount, -0.1F, 0.05F); // dozes off slowly, wakes fast
		}
	}

	/** Asleep, it doesn't turn to look at players or glance around. */
	@Override
	protected boolean canLookAround() {
		return !this.isSleepingCurled();
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
		// Embers stream from the ember tail, which rises tall behind the body (it stands
		// up even taller when sitting). Asleep, its embers smoulder more slowly.
		if (this.level().isClientSide() && this.isSleepingCurled()) {
			this.emitEmbers(0.3, 0.2, 0.2, 0.2, 8, 8);
		} else if (this.level().isClientSide()) {
			if (this.isInSittingPose()) {
				this.emitEmbers(0.35, 0.25, 0.5, 0.5, 2, 1);
			} else {
				this.emitEmbers(0.35, 0.35, 0.6, 0.45, 2, 1);
			}
		}
	}

	/** Glowing square embers, like the concept art, instead of vanilla flames. */
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
		this.emberSparkCooldown.load(input, EmberSparkAbility.CINDERCUB.cooldownTicks());
	}
}

package com.ashi.mythweave.entity;

import com.ashi.mythweave.ability.EmberSpark;
import com.ashi.mythweave.registry.ModSounds;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
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
 * Cindercub, the first Mythling: a small ember fox.
 *
 * <p>Extends vanilla {@link TamableAnimal}, which already provides what bonding needs:
 * the owner is stored as a UUID ("Owner") and the sit order as "Sitting", both saved
 * with the entity and restored on load, and both synced to clients automatically.
 *
 * <p>Wild: wanders, looks around, flees when hurt. Bond by using {@link #BONDING_ITEM}
 * on it (always succeeds). Bonded: follows its owner; the owner toggles sit/stand by
 * using it with an empty hand (or any item that has no other use on it), and uses
 * {@link #BONDING_ITEM} on it again to trigger {@link EmberSpark}.
 *
 * <p>Owner offline or in another dimension: vanilla's {@link SitWhenOrderedToGoal}
 * makes it sit and wait, and it stands up again when the owner is back. Owner death:
 * it keeps the bond and follows the respawned player, like a vanilla wolf.
 */
public class CindercubEntity extends TamableAnimal {
	/** Prototype bonding item (spec §5). A custom Aether Shard can replace it later. */
	public static final Item BONDING_ITEM = Items.BLAZE_POWDER;

	// Entity event ids handled by TamableAnimal#handleEntityEvent on the client.
	private static final byte EVENT_HEART_PARTICLES = 7;

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

	/**
	 * AI goals, run server-side. Lower number = higher priority; goals that use the same
	 * controls (moving, looking) can't run together, so a higher-priority one interrupts.
	 * The order matches the vanilla wolf's.
	 */
	@Override
	protected void registerGoals() {
		// Swim instead of sinking.
		this.goalSelector.addGoal(1, new FloatGoal(this));
		// Run away at 1.4x speed for a few seconds when hurt. The tamable version also
		// teleports back to a far-away owner while panicking.
		this.goalSelector.addGoal(1, new TamableAnimalPanicGoal(1.4));
		// Stay put while ordered to sit (or while the owner is offline / in another dimension).
		this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
		// Follow the owner once more than 8 blocks away, stopping within 2 blocks. 1.2x speed
		// so it keeps up with a walking player. If the owner gets more than 12 blocks away it
		// teleports next to them (vanilla rule), so it never gets left far behind.
		this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.2, 8.0F, 2.0F));
		// Wander at normal speed (1.0 x MOVEMENT_SPEED), avoiding water.
		this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0));
		// Idle: glance at nearby players, otherwise look around.
		this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
	}

	/**
	 * Right-click handling. Minecraft calls this on both the client and the server; the
	 * client call only predicts the arm swing, so every state change and message below is
	 * guarded by {@code !isClientSide()} and decided by the server.
	 *
	 * <p>Priority: bonding a wild Cindercub; for the owner, {@link #BONDING_ITEM} triggers
	 * Ember Spark, then vanilla interactions (leads, etc.), then the sit toggle. A non-owner
	 * can never change an owned Cindercub.
	 */
	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		boolean server = !this.level().isClientSide();

		if (!this.isTame()) {
			if (stack.is(BONDING_ITEM)) {
				if (server) {
					stack.consume(1, player);
					this.bondWith(player);
				}
				return InteractionResult.SUCCESS;
			}
			// Empty-handed on a wild one: hint at what it wants. Main hand only, so the
			// hint doesn't show twice (the game tries the off hand too).
			if (stack.isEmpty() && hand == InteractionHand.MAIN_HAND) {
				if (server) {
					player.sendOverlayMessage(Component.translatable(
							"message.mythweave.cindercub.wants_item", BONDING_ITEM.getName(new ItemStack(BONDING_ITEM))));
				}
				return InteractionResult.SUCCESS;
			}
			return super.mobInteract(player, hand);
		}

		if (!this.isOwnedBy(player)) {
			// Someone else's: refuse bonding attempts and empty-handed clicks with a clear
			// message, but let other items (e.g. a lead) behave normally.
			if (stack.is(BONDING_ITEM) || (stack.isEmpty() && hand == InteractionHand.MAIN_HAND)) {
				if (server) {
					player.sendOverlayMessage(Component.translatable("message.mythweave.cindercub.not_yours"));
				}
				return InteractionResult.SUCCESS.withoutItem();
			}
			return super.mobInteract(player, hand);
		}

		// Owner + Blaze Powder: Ember Spark (works sitting or standing; no powder is used).
		if (stack.is(BONDING_ITEM)) {
			if (this.level() instanceof ServerLevel serverLevel) {
				EmberSpark.use(this, serverLevel, player);
			}
			return InteractionResult.SUCCESS.withoutItem();
		}

		// Owner: vanilla interactions first; if nothing used the click, toggle sit/stand.
		InteractionResult result = super.mobInteract(player, hand);
		if (result.consumesAction()) {
			return result;
		}
		if (server) {
			boolean sit = !this.isOrderedToSit();
			this.setOrderedToSit(sit);
			this.setJumping(false);
			this.getNavigation().stop();
			this.setTarget(null);
			player.sendOverlayMessage(Component.translatable(
					sit ? "message.mythweave.cindercub.sitting" : "message.mythweave.cindercub.following"));
		}
		return InteractionResult.SUCCESS.withoutItem();
	}

	/** Server-side. Records the owner (UUID), shows hearts, and starts following right away. */
	private void bondWith(Player player) {
		this.tame(player);
		this.getNavigation().stop();
		this.setTarget(null);
		this.setOrderedToSit(false);
		this.level().broadcastEntityEvent(this, EVENT_HEART_PARTICLES);
		player.sendOverlayMessage(Component.translatable("message.mythweave.cindercub.bonded"));
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

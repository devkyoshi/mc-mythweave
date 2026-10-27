package com.ashi.mythweave.entity.base;

import com.ashi.mythweave.evolution.Kindle;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

/**
 * Base class for every Mythling: everything a bonded companion does that isn't specific to
 * one creature. Subclasses (one per creature or evolution form, in {@code entity/<name>/})
 * add their stats, sounds, abilities and visuals through the hooks below.
 *
 * <p>Builds on vanilla {@link TamableAnimal}, which already stores the owner as a UUID
 * ("Owner") and the sit order ("Sitting"), saves both, and syncs them to clients. On top of
 * that this class provides:
 * <ul>
 *   <li>bonding with {@link #bondingItem()} (always succeeds), with a hint for empty hands;</li>
 *   <li>refusing other players, so nobody can claim or command someone else's Mythling;</li>
 *   <li>the owner's controls: sneak + empty hand shows Kindle status, any item a subclass
 *       handles in {@link #onOwnerUseItem}, otherwise vanilla interactions, then sit/stand;</li>
 *   <li>the companion AI goals (follow, sit, wander, flee when hurt);</li>
 *   <li>{@link Kindle}: evolution progress, ticked, saved under "Kindle";</li>
 *   <li>no breeding (out of scope, spec §5).</li>
 * </ul>
 *
 * <p>Owner offline or in another dimension: {@link SitWhenOrderedToGoal} makes it sit and
 * wait. Owner death: the bond stays, like a vanilla wolf.
 */
public abstract class MythlingEntity extends TamableAnimal implements Mythling {
	// Entity event ids handled by TamableAnimal#handleEntityEvent on the client.
	private static final byte EVENT_HEART_PARTICLES = 7;

	/** Evolution progress; server-side, saved with the entity (see {@link Kindle}). */
	private final Kindle kindle = new Kindle();

	protected MythlingEntity(EntityType<? extends MythlingEntity> type, Level level) {
		super(type, level);
	}

	/** The item that bonds a wild one (consumed on use). */
	public abstract Item bondingItem();

	/**
	 * Owner-only item uses (abilities and the like). Called on both sides; do state changes
	 * only on the server. Return true if this item did something, so the click is consumed.
	 */
	protected boolean onOwnerUseItem(Player owner, ItemStack stack) {
		return false;
	}

	/** Server-side, right after bonding (hearts and message are already handled). */
	protected void onBonded(ServerLevel level, Player owner) {
	}

	@Override
	public Kindle kindle() {
		return this.kindle;
	}

	/**
	 * Companion AI, run server-side. Lower number = higher priority; goals that use the same
	 * controls (moving, looking) can't run together, so a higher-priority one interrupts.
	 * The order matches the vanilla wolf's. Override to add creature-specific goals.
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
	 * decided by the server.
	 *
	 * <p>Priority: bonding a wild one; refusing non-owners; for the owner, sneaking with an
	 * empty hand shows Kindle status, then {@link #onOwnerUseItem}, then vanilla interactions
	 * (leads, etc.), then the sit toggle.
	 */
	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		boolean server = !this.level().isClientSide();
		Item bondingItem = this.bondingItem();

		if (!this.isTame()) {
			if (stack.is(bondingItem)) {
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
					player.sendOverlayMessage(Component.translatable("message.mythweave.mythling.wants_item",
							this.getDisplayName(), bondingItem.getName(new ItemStack(bondingItem))));
				}
				return InteractionResult.SUCCESS;
			}
			return super.mobInteract(player, hand);
		}

		if (!this.isOwnedBy(player)) {
			// Someone else's: refuse bonding attempts and empty-handed clicks with a clear
			// message, but let other items (e.g. a lead) behave normally.
			if (stack.is(bondingItem) || (stack.isEmpty() && hand == InteractionHand.MAIN_HAND)) {
				if (server) {
					player.sendOverlayMessage(Component.translatable("message.mythweave.mythling.not_yours",
							this.getDisplayName()));
				}
				return InteractionResult.SUCCESS.withoutItem();
			}
			return super.mobInteract(player, hand);
		}

		// Owner, sneaking, empty main hand: show Kindle (evolution progress) instead of sitting.
		if (player.isSecondaryUseActive() && stack.isEmpty() && hand == InteractionHand.MAIN_HAND) {
			if (server) {
				player.sendOverlayMessage(this.kindle.status(this));
			}
			return InteractionResult.SUCCESS.withoutItem();
		}

		// Owner + an item the creature reacts to (e.g. Cindercub's Ember Spark).
		if (!stack.isEmpty() && this.onOwnerUseItem(player, stack)) {
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
					sit ? "message.mythweave.mythling.sitting" : "message.mythweave.mythling.following",
					this.getDisplayName()));
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
		if (this.level() instanceof ServerLevel serverLevel) {
			this.kindle.markBiomeVisited(this, serverLevel); // its home biome doesn't pay out
			this.onBonded(serverLevel, player);
		}
		player.sendOverlayMessage(Component.translatable("message.mythweave.mythling.bonded", this.getDisplayName()));
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel serverLevel && this.isTame()) {
			this.kindle.tick(this, serverLevel);
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		this.kindle.save(output);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.kindle.load(input);
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

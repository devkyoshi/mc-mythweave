package com.ashi.mythweave.entity.base;

import com.ashi.mythweave.evolution.Evolution;
import com.ashi.mythweave.evolution.EvolutionRules;
import com.ashi.mythweave.evolution.Kindle;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
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
 *   <li>each form's size, its {@code SCALE} attribute ({@link #formScale}), so later stages
 *       are bigger;</li>
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
	/** Ticks left in an evolution in progress; saved so a reload mid-transformation resumes it. */
	private int evolvingTicks;
	/** Not saved: the "ready, next form not in the game yet" message shows once per load. */
	private boolean readyAnnounced;
	/** Not saved: the "needs more room to evolve" message shows once per load. */
	private boolean roomAnnounced;

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

	/**
	 * True for forms that fight: they bite whatever hurts their owner or themselves (see
	 * {@link #wantsToAttack}) instead of running away. Such forms need an ATTACK_DAMAGE
	 * attribute. Default false: the creature panics when hurt.
	 */
	protected boolean defendsOwner() {
		return false;
	}

	/**
	 * Who a defending Mythling may fight: the vanilla wolf's rules. Never creepers, ghasts or
	 * armor stands, never tamed animals or horses, and never a player its owner can't harm.
	 */
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
	public Kindle kindle() {
		return this.kindle;
	}

	/** This form's id in its {@link com.ashi.mythweave.evolution.EvolutionLine}: its entity id path, e.g. "cindercub". */
	public String formId() {
		return BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()).getPath();
	}

	/** Ticks left in an evolution in progress (0 = not evolving). Driven by {@link Evolution}. */
	public int evolvingTicks() {
		return this.evolvingTicks;
	}

	public void setEvolvingTicks(int ticks) {
		this.evolvingTicks = ticks;
	}

	public boolean isEvolving() {
		return this.evolvingTicks > 0;
	}

	/** Whether the owner has been told it's ready but its next form isn't in the game yet (once per load). */
	public boolean readyAnnounced() {
		return this.readyAnnounced;
	}

	public void setReadyAnnounced(boolean announced) {
		this.readyAnnounced = announced;
	}

	/** Whether the owner has been told it needs more room to evolve (once per load). */
	public boolean roomAnnounced() {
		return this.roomAnnounced;
	}

	public void setRoomAnnounced(boolean announced) {
		this.roomAnnounced = announced;
	}

	/**
	 * How big a form is drawn and collides, relative to its model: the {@code SCALE} base value
	 * from its {@code createAttributes()}. Vanilla multiplies the hitbox, eye height, model,
	 * shadow and leash point by it, so later stages are visibly bigger with one number per form.
	 */
	@SuppressWarnings("unchecked")
	public static float formScale(EntityType<?> type) {
		return (float) DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type).getBaseValue(Attributes.SCALE);
	}

	/**
	 * Client-side visuals only: little flames drift up from a point behind and above the
	 * body (e.g. a fire tail), more often while it runs. In rain or water they turn into puffs
	 * of steam. Each client spawns its own, so this sends nothing over the network.
	 *
	 * @param back     how far behind the centre the effect starts (blocks)
	 * @param backSpan how much further back it can reach (picked at random along the span)
	 * @param up       height above the feet where it starts (blocks)
	 * @param upSpan   how much higher it can reach along the same span
	 * @param oneInIdle   spawn chance per tick while standing still (1 in n)
	 * @param oneInMoving spawn chance per tick while moving (1 in n)
	 */
	protected void emitEmbers(double back, double backSpan, double up, double upSpan, int oneInIdle, int oneInMoving) {
		boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4;
		if (this.random.nextInt(moving ? oneInMoving : oneInIdle) != 0) {
			return;
		}
		// The entity faces (-sin(yaw), cos(yaw)), so "behind" is the opposite. Offsets are for
		// the model at scale 1 and grow with the form's size.
		float scale = this.getScale();
		double along = this.random.nextDouble();
		double b = (back + along * backSpan) * scale;
		float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
		double x = this.getX() + Mth.sin(yaw) * b + (this.random.nextDouble() - 0.5) * 0.15;
		double y = this.getY() + (up + along * upSpan) * scale;
		double z = this.getZ() - Mth.cos(yaw) * b + (this.random.nextDouble() - 0.5) * 0.15;
		if (this.isInWaterOrRain()) {
			this.level().addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.04, 0.0);
		} else {
			this.level().addParticle(this.random.nextInt(4) == 0 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME,
					x, y, z, 0.0, 0.015 + this.random.nextDouble() * 0.02, 0.0);
		}
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
		if (this.defendsOwner()) {
			// Fighters: bite the target (chasing it even out of sight), choosing as targets
			// whatever hurts the owner, then whatever hurts the Mythling itself.
			this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.2, true));
			this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
			this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
		} else {
			// Run away at 1.4x speed for a few seconds when hurt. The tamable version also
			// teleports back to a far-away owner while panicking.
			this.goalSelector.addGoal(1, new TamableAnimalPanicGoal(1.4));
		}
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

		// Mid-evolution it ignores everyone (the transformation takes three seconds).
		if (this.isEvolving()) {
			return InteractionResult.PASS;
		}

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
		if (this.level() instanceof ServerLevel serverLevel) {
			if (this.isEvolving()) {
				Evolution.tick(this, serverLevel);
			} else if (this.isTame()) {
				this.kindle.tick(this, serverLevel);
				if (this.tickCount % 20 == 0) {
					Evolution.checkReady(this, serverLevel);
				}
			}
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		this.kindle.save(output);
		if (this.evolvingTicks > 0) {
			output.putInt("EvolvingTicks", this.evolvingTicks);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.kindle.load(input);
		this.evolvingTicks = Math.clamp(input.getIntOr("EvolvingTicks", 0), 0, EvolutionRules.EVOLUTION_TICKS);
		// Size belongs to the form, not the save: one saved before a size change (or edited
		// with /attribute) loads at its form's current size.
		AttributeInstance scale = this.getAttribute(Attributes.SCALE);
		if (scale != null) {
			scale.setBaseValue(formScale(this.getType()));
		}
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

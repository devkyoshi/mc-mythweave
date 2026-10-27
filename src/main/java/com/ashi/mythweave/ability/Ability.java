package com.ashi.mythweave.ability;

import com.ashi.mythweave.entity.base.MythlingEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A Mythling ability: something the owner triggers by using an item on their Mythling.
 *
 * <p>Abilities are stateless constants (e.g. {@link EmberSparkAbility#CINDERCUB}); per-creature
 * state such as the cooldown lives on the entity in an {@link AbilityCooldown}, which the
 * entity passes in. A Mythling triggers its abilities from {@code MythlingEntity#onOwnerUseItem}.
 *
 * <p>Rules (spec §5, §9): {@link #use} runs only on the server, checks the cooldown itself,
 * tells the owner what happened, and keeps its effect bounded (no uncontrolled fire, no
 * griefing).
 *
 * @param <E> the Mythling type that has this ability
 */
public interface Ability<E extends MythlingEntity> {
	/** Short id, e.g. "ember_spark" (for logs, commands and future data). */
	String id();

	/** Ticks between uses. */
	int cooldownTicks();

	/** True if using this item on the Mythling should trigger the ability. */
	boolean isTriggeredBy(ItemStack stack);

	/**
	 * Server-side. Tries the ability (respecting {@code cooldown}, which it also starts on
	 * success) and tells the owner how it went.
	 */
	void use(E mythling, AbilityCooldown cooldown, ServerLevel level, Player owner);
}

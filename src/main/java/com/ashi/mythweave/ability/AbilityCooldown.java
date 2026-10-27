package com.ashi.mythweave.ability;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * One ability's cooldown on one Mythling. Server-only: it is never synced, so a client can't
 * change it, and it's saved with the entity so relogging doesn't reset it.
 *
 * <p>The owning entity calls {@link #tick()} every server tick and {@link #save}/{@link #load}
 * from its own save methods.
 */
public final class AbilityCooldown {
	private final String saveKey;
	private int remainingTicks;

	/** @param saveKey the NBT key it's saved under. Never change it once released (breaks saves). */
	public AbilityCooldown(String saveKey) {
		this.saveKey = saveKey;
	}

	/** Server-side, once per tick. */
	public void tick() {
		if (this.remainingTicks > 0) {
			this.remainingTicks--;
		}
	}

	public boolean isReady() {
		return this.remainingTicks <= 0;
	}

	public int remainingTicks() {
		return this.remainingTicks;
	}

	/** Remaining time rounded up to whole seconds, for messages. */
	public int remainingSeconds() {
		return (this.remainingTicks + 19) / 20;
	}

	public void start(int ticks) {
		this.remainingTicks = ticks;
	}

	public void save(ValueOutput output) {
		output.putInt(this.saveKey, this.remainingTicks);
	}

	/** Clamped to 0..max, so a hand-edited or corrupt value can't lock the ability for ages. */
	public void load(ValueInput input, int max) {
		this.remainingTicks = Math.clamp(input.getIntOr(this.saveKey, 0), 0, max);
	}
}

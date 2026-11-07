package com.mythweave.client.render.entity.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Per-frame snapshot of what the Ashenox model needs beyond the vanilla living-entity data. */
public class AshenoxRenderState extends LivingEntityRenderState {
	/** From {@code TamableAnimal#isInSittingPose()}, which the server syncs to clients. */
	public boolean isSitting;
	/** 0 awake, 1 lying down asleep, smoothed between ticks. */
	public float sleepAmount;
	/** Ticks into the happy animation, or -1 when not playing. */
	public float happyTime = -1.0F;
	/** 0 relaxed, 1 fully alert (head up, ears up, tails raised), smoothed between ticks. */
	public float alertAmount;
	/** A player is riding it. */
	public boolean isRidden;
	/** Sprinting (its rider sprints): the bounding run instead of the walk. */
	public boolean isRunning;
}

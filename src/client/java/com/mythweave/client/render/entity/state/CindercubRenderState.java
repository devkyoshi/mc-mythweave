package com.mythweave.client.render.entity.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Per-frame snapshot of what the Cindercub model needs beyond the vanilla living-entity data. */
public class CindercubRenderState extends LivingEntityRenderState {
	/** From {@code TamableAnimal#isInSittingPose()}, which the server syncs to clients. */
	public boolean isSitting;
	/** 0 awake, 1 curled up asleep, smoothed so it settles down and wakes gradually. */
	public float sleepAmount;
	/** Ticks into the happy animation, or -1 when it isn't playing. */
	public float happyTime = -1.0F;
	/** Ticks left of the hurt flinch (10 when just hit, 0 when over). */
	public float hurtTime;
	/** A per-entity number so a group of Cindercubs don't sniff the air in sync. */
	public int idleSeed;
}

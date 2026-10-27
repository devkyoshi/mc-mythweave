package com.ashi.mythweave.client.creature.cindercub;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Per-frame snapshot of what the Cindercub model needs beyond the vanilla living-entity data. */
public class CindercubRenderState extends LivingEntityRenderState {
	/** From {@code TamableAnimal#isInSittingPose()}, which the server syncs to clients. */
	public boolean isSitting;
	/** From {@code TamableAnimal#isTame()} (also synced): bonded Cindercubs have a fire tail. */
	public boolean isTame;
}
